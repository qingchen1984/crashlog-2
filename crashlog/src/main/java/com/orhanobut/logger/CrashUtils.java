package com.orhanobut.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by zhangming on 16/8/8.
 */
public class CrashUtils {
    private static final String TAG = "CrashUtils";
    private static final String ClassName = "com.orhanobut.logger.CrashHandler";

    public static void clearCrashlog(Context context) {
        if (!CrashHandler.getInstance().isInited()) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(ClassName, Context
                .MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(ClassName, false).apply();
        sharedPreferences.edit().putInt(ClassName + "_hashcode", 0).apply();
    }

    //判断本次启动是否从崩溃中恢复启动
    public static boolean isStartfromCrash(Context context) {
        if (!CrashHandler.getInstance().isInited()) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
            return false;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(ClassName, Context
                .MODE_PRIVATE);
        int hashCode = sharedPreferences.getInt(ClassName + "_hashcode", 0);
        return sharedPreferences.getBoolean(ClassName, false) && (hashCode != 0 && hashCode != context
                .getApplicationContext().hashCode());
    }
}
