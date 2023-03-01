package com.perry.core.util;

import android.app.ActivityManager;
import android.app.Application;

import com.perry.App;

import java.util.List;

public class Utils {

    public static boolean isAppRunningForeground() {
        ActivityManager activityManager =
                (ActivityManager) App.getInstance().getSystemService(Application.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : runningAppProcesses) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(App.getInstance().getApplicationInfo().processName))
                    return true;
            }
        }
        return false;
    }
}
