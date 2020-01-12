package com.optimize.performance;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import com.github.moduth.blockcanary.BlockCanary;
import com.optimize.performance.block.AppBlockCanaryContext;
import com.optimize.performance.utils.LaunchTimer;
import com.tencent.mmkv.MMKV;

public class MyApplication extends Application {

    private static Application mApplication;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        LaunchTimer.startRecord();
        MultiDex.install(this);
//        DexposedBridge.hookAllConstructors(Thread.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//                Thread thread = (Thread) param.thisObject;
//                LogUtils.i(thread.getName() + " stack " + Log.getStackTraceString(new Throwable()));
//            }
//        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MMKV.initialize(MyApplication.this);
        MMKV.defaultMMKV().encode("times", 100);
        int times = MMKV.defaultMMKV().decodeInt("times");


        LaunchTimer.startRecord();
        mApplication = this;

        LaunchTimer.endRecord();

//        DexposedBridge.hookAllConstructors(ImageView.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//                DexposedBridge.findAndHookMethod(ImageView.class, "setImageBitmap", Bitmap.class, new ImageHook());
//            }
//        });
//
//        try {
//            DexposedBridge.findAndHookMethod(Class.forName("android.os.BinderProxy"), "transact", int.class, Parcel.class, Parcel.class, int.class, new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            LogUtils.i( "BinderProxy beforeHookedMethod " + param.thisObject.getClass().getSimpleName() + "\n" + Log.getStackTraceString(new Throwable()) );
//                            super.beforeHookedMethod(param);
//                        }
//                    });
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

        BlockCanary.install(this, new AppBlockCanaryContext()).start(); // AndroidPerformanceMonitor

//        new ANRWatchDog().start();
    }

    public static Application getApplication() {
        return mApplication;
    }

}
