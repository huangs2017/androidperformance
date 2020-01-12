package com.optimize.performance;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AsyncLayoutInflater;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import com.alibaba.fastjson.JSON;
import com.optimize.performance.net.JobSchedulerService;
import com.optimize.performance.net.RetrofitNewsUtils;
import com.optimize.performance.utils.ExceptionMonitor;
import com.optimize.performance.utils.LaunchTimer;
import com.optimize.performance.utils.LogUtils;
import com.zhangyue.we.x2c.X2C;
import com.zhangyue.we.x2c.ano.Xml;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Xml(layouts = "activity_main")
public class MainActivity extends AppCompatActivity {

    private AlphaAnimation alphaAnimation;
    private RecyclerView mRecyclerView;
    private NewsAdapter mNewsAdapter;
    private String mStringIds = "20190220005233,20190220005171,20190220005160,20190220005146,20190220001228,20190220001227," +
            "20190219006994,20190219006839,20190219005350,20190219005343,20190219004522,20190219004520,20190219000132,20190219000118," +
            "20190219000119,20190218009367,20190218009078,20190218009075,20190218008572,20190218008496,20190218006078,20190218006156," +
            "20190218006190,20190218006572,20190218006235,20190218006284,20190218006571,20190218006283,20190218006191,20190218005733," +
            "20190217004740,20190218001891,20190218001889,20190217004183,20190217004019,20190217004011,20190217003152,20190217002757," +
            "20190217002249,20190217000954,20190217000957,20190217000953,20190216004269,20190216003721,20190216003720,20190216003351,";

    private long mStartFrameTime = 0;
    private int mFrameCount = 0;
    private static final long MONITOR_INTERVAL = 160L; //单次计算FPS使用160毫秒
    private static final long MONITOR_INTERVAL_NANOS = MONITOR_INTERVAL * 1000L * 1000L;
    private static final long MAX_INTERVAL = 1000L; //设置计算fps的单位时间间隔1000ms,即fps/s;

    public List<NewsItem> mItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 以下代码是为了演示Msg导致的主线程卡顿
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                LogUtils.i("Msg 执行");
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        });

        // 必须在 super.onCreate(savedInstanceState) 之前
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {

                if (TextUtils.equals(name, "TextView")) {
                    // 生成自定义TextView
                }
                long time = System.currentTimeMillis();
                View view = getDelegate().createView(parent, name, context, attrs);
                LogUtils.i(name + " cost " + (System.currentTimeMillis() - time)); // 获取任一控件加载耗时
                return view;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return null;
            }
        });

        new AsyncLayoutInflater(MainActivity.this).inflate(R.layout.activity_main, null, new AsyncLayoutInflater.OnInflateFinishedListener() {
            @Override
            public void onInflateFinished(@NonNull View view, int i, @Nullable ViewGroup viewGroup) {
                setContentView(view);
                mRecyclerView = findViewById(R.id.recycler_view);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                mRecyclerView.setAdapter(mNewsAdapter);
            }
        });

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState); //----------------------------------------
        X2C.setContentView(MainActivity.this, R.layout.activity_main);
        mNewsAdapter = new NewsAdapter(mItems);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, filter);
        LogUtils.i("battery " + intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));

        getNews();
        getFPS();


        // 以下代码是为了演示业务不正常场景下的监控
        try {
            // 一些业务处理
            Log.i("", "");
        } catch (Exception e) {
            ExceptionMonitor.monitor(Log.getStackTraceString(e));
        }

        boolean flag = true;
        if (flag) {
            // 正常，继续执行流程
        } else {
            ExceptionMonitor.monitor("");
        }
    }

    /**
     * 演示JobScheduler的使用
     */
    private void startJobScheduler() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(getPackageName(), JobSchedulerService.class.getName()));
            builder.setRequiresCharging(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
            jobScheduler.schedule(builder.build());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getFPS() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (mStartFrameTime == 0) {
                    mStartFrameTime = frameTimeNanos;
                }
                long interval = frameTimeNanos - mStartFrameTime;
                if (interval > MONITOR_INTERVAL_NANOS) {
                    double fps = (((double) (mFrameCount * 1000L * 1000L)) / interval) * MAX_INTERVAL;
                    mFrameCount = 0;
                    mStartFrameTime = 0;
                } else {
                    ++mFrameCount;
                }

                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    private void getNews() {
        RetrofitNewsUtils.getApiService().getNBANews("banner", mStringIds)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String json = response.body().string();
                            JSONObject jsonObject = new JSONObject(json);
                            JSONObject data = jsonObject.getJSONObject("data");
                            Iterator<String> keys = data.keys();
                            while (keys.hasNext()) {
                                String next = keys.next();
                                JSONObject itemJO = data.getJSONObject(next);
                                NewsItem newsItem = JSON.parseObject(itemJO.toString(), NewsItem.class);
                                mItems.add(newsItem);
                            }
                            mNewsAdapter.setItems(mItems);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 以下代码是为了演示电量优化中对动画的处理
//      alphaAnimation.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 以下代码是为了演示电量优化中对动画的处理
//        alphaAnimation.cancel();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        LaunchTimer.endRecord("onWindowFocusChanged");
    }
}
