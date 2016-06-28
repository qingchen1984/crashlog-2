package simen.crashlogsample;

import android.app.Application;

import com.orhanobut.logger.CrashHandler;

/**
 * Created by zhangming on 16/6/28.
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
