package simen.crashlogsample;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import com.orhanobut.logger.CrashHandler;

import java.util.List;

/**
 * Created by zhangming on 16/6/28.
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (isNamedProcess(this, "simen.crashlogsample")) {
            CrashHandler instance = CrashHandler.getInstance();
            instance.init(this);
            instance.enanbleToast(false);
            instance.AutoOpenCrash(true);
        }
    }

    public static boolean isNamedProcess(Context context, String processName) {
        if (context == null) {
            return false;
        }

        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = manager.getRunningAppProcesses();
        if (processInfoList == null || processInfoList.isEmpty()) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo processInfo : processInfoList) {
            if (processInfo != null && processInfo.pid == pid
                    && isEquals(processName, processInfo.processName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEquals(Object actual, Object expected) {
        return actual == expected || (actual == null ? expected == null : actual.equals(expected));
    }
}
