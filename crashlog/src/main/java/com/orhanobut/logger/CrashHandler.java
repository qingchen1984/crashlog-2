package com.orhanobut.logger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录错误报告.
 * 为了要在程序启动器就监控整个程序,需要在Application中注册。
 */
public class CrashHandler implements UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    private static final String ClassName = "com.orhanobut.logger.CrashHandler";
    // 系统默认的UncaughtException处理类
    private UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandler instance;
    // 程序的Context对象s
    private Context mContext;

    private boolean showToast = true;
    private boolean auto_open = false;

    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<>();
    // 用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * 初始化
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public boolean isInited() {
        return !(mContext == null || mDefaultHandler == null);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (mContext != null) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences(ClassName, Context
                    .MODE_PRIVATE);
            sharedPreferences.edit().putBoolean(ClassName, true).apply(); //记录崩溃
            sharedPreferences.edit().putInt(ClassName + "_hashcode", mContext.hashCode()).apply(); //记录崩溃
        }

        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 退出程序
            ex.printStackTrace();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    public boolean handleException(Throwable ex) {
        if (!isInited()) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
            return false;
        }
        if (ex == null) {
            return false;
        }

        ex.printStackTrace();

        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        final String logFilePath = saveCatchInfo2File(ex);
        if (auto_open && mContext != null && shouldShowCrash()) {//自动打开日志文件

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(Uri.fromFile(new File(logFilePath)), "text/plain");
            if (viewIntent.resolveActivity(mContext.getPackageManager()) != null) {
                viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(viewIntent);
            }
        }

        if (showToast) {// 使用Toast来显示异常信息
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(mContext, "error accour",
                            Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }.start();
        }
        return true;
    }

    private boolean shouldShowCrash() {
        if (mContext == null) {
            return false;
        }

        SharedPreferences preferences = mContext.getSharedPreferences(ClassName, Context
                .MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long time = preferences.getLong(ClassName + "_time", now);
        preferences.edit().putLong(ClassName + "_time", now).commit();
        return now - time >= 5000;
    }

    /**
     * 是否在发生crash时,自动打开日志文件
     *
     * @param auto_open true,自动打开;false,不打开
     * @return
     */
    public CrashHandler AutoOpenCrash(boolean auto_open) {
        if (!isInited()) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
            throw new RuntimeException("CrashHandler has not been inited!!!");
        }
        this.auto_open = auto_open;
        return this;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null"
                        : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    public boolean isDebugable() {
        if (mContext == null) {
            return false;
        }

        try {
            return (mContext.getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)
                    != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否打开报错时提示
     *
     * @param showToast
     */
    public CrashHandler enanbleToast(boolean showToast) {
        if (!isInited()) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
            throw new RuntimeException("CrashHandler has not been inited!!!");
        }

        this.showToast = showToast;
        return this;
    }

    private String getFilePath() {
        String file_dir = "";
        boolean isSDCardExist = Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()); // SD卡是否存在
        boolean isRootDirExist = Environment.getExternalStorageDirectory()
                .exists();
        boolean hasWritePermissions = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//6.0及以后版本系统,检查是否有磁盘读写权限
            hasWritePermissions = mContext.checkSelfPermission(Manifest.permission
                    .WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasWritePermissions && isSDCardExist && isRootDirExist) {
            file_dir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/crashlog/" + getApplicationName()
                    + "/" + getVersion() + "/";
        } else {
            if (isSDCardExist && isRootDirExist) {
                file_dir = mContext.getExternalCacheDir().getAbsolutePath() + "/crashlog/"
                        + getApplicationName() + "/" + getVersion() + "/";
            } else {
                file_dir = mContext.getFilesDir().getAbsolutePath() + "/crashlog/"
                        + getApplicationName() + "/" + getVersion() + "/";
            }
        }
        return file_dir;
    }

    /**
     * 获取APP名字
     *
     * @return
     */
    public String getApplicationName() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = mContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(
                    mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) packageManager
                .getApplicationLabel(applicationInfo);
        return applicationName;
    }

    /**
     * 获取版本号
     *
     * @return
     */
    public String getVersion() {
        PackageManager manager;

        PackageInfo info = null;

        manager = mContext.getPackageManager();

        try {
            info = manager.getPackageInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return info.versionCode + "_" + info.versionName;
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    public String saveCatchInfo2File(Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);

        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }

        printWriter.close();
        return saveCatchInfo2File(writer.toString(), true);
    }


    /**
     * @param result    需要保存的结果
     * @param exception true,app异常;false,普通日志
     */
    public String saveCatchInfo2File(String result, boolean exception) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        sb.append(result);

        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash" + (exception ? "" : "log") + "-" + time + "-" + timestamp + ".txt";
            String file_dir = getFilePath();

            File dir = new File(file_dir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(file_dir + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            // TODO: 16/8/1 在这里可以将错误报告发给开发者
            fos.close();
            // }
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

}