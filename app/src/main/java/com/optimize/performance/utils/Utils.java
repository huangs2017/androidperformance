package com.optimize.performance.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class Utils {

    private static String sCurProcessName;

    public static boolean isMainProcess(Context context) {
        String processName = getCurProcessName(context);
        if (processName != null && processName.contains(":")) {
            return false;
        }
        return (processName != null && processName.equals(context.getPackageName()));
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            // 获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // 获取NetworkInfo对象
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            //判断NetworkInfo对象是否为空
            if (networkInfo != null)
                return networkInfo.isAvailable();
        }
        return false;
    }

    public static String getCurProcessName(Context context) {
        try {
            int pid = android.os.Process.myPid();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    sCurProcessName = appProcess.processName;
                    return sCurProcessName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sCurProcessName = getCurProcessNameFromProc();
        return sCurProcessName;
    }

    private static String getCurProcessNameFromProc() {
        BufferedReader bufferedReader = null;
        try {
            FileInputStream fileInputStream = new FileInputStream("/proc/" + android.os.Process.myPid() + "/cmdline");
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, "iso-8859-1"));
            int c;
            StringBuilder processName = new StringBuilder();
            while ((c = bufferedReader.read()) > 0) {
                processName.append((char) c);
            }
            return processName.toString();
        } catch (Throwable e) {

        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

}
