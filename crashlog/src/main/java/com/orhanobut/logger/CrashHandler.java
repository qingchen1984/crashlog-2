package com.orhanobut.logger;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 * 需要在Application中注册，为了要在程序启动器就监控整个程序。
 */
public class CrashHandler implements UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    // 系统默认的UncaughtException处理类
    private UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandler instance;
    // 程序的Context对象s
    private Context mContext;

    private boolean showToast = true;

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

    /**
     * 初始化
     */
    public void init(Context context) {
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);

        enanbleToast(isDebugable());
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

    public boolean isInited() {
        if (mContext == null || mDefaultHandler == null) {
            return false;
        }

        return true;
    }

    /**
     * 是否打开报错时提示
     *
     * @param showToast
     */
    public void enanbleToast(boolean showToast) {
        this.showToast = showToast;
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
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
//			Log.e(TAG, "程序异常退出");
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
        if (ex == null) {
            return false;
        }

//        if (ex instanceof SQLiteFullException) {
//            //数据库或磁盘已满异常
//
//        }

        ex.printStackTrace();

        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        saveCatchInfo2File(ex);

        // 使用Toast来显示异常信息
        if (showToast) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(mContext, "error accur",
                            Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }.start();
        }
        return true;
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

    private String getFilePath() {
        String file_dir = "";
        // SD卡是否存在
        boolean isSDCardExist = Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
        // Environment.getExternalStorageDirectory()相当于File file=new
        // File("/sdcard")
        boolean isRootDirExist = Environment.getExternalStorageDirectory()
                .exists();
        if (isSDCardExist && isRootDirExist) {
            file_dir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + "/crashlog/"
                    + getApplicationName()
                    + "/" + getVersion() + "/";
        } else {
            // MyApplication.getInstance().getFilesDir()返回的路劲为/data/data/PACKAGE_NAME/files，其中的包就是我们建立的主Activity所在的包
            file_dir = mContext.getFilesDir().getAbsolutePath() + "/crashlog/"
                    + getApplicationName() + "/" + getVersion() + "/";
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

    public Context getContext() {
        return mContext;
    }

    //保存日志信息到本地文件
    public static void saveLogInfo2File(String log) {
        if (CrashHandler.getInstance().isInited() == false) {
            Log.d(TAG, "CrashHandler has not been inited!!!");
            CrashHandler.getInstance().saveCatchInfo2File("CrashHandler has not been inited!!!",
                    false);
            return;
        }

        Log.d(TAG, log);
        CrashHandler.getInstance().collectDeviceInfo(CrashHandler.getInstance().getContext());
        CrashHandler.getInstance().saveCatchInfo2File(log, false);
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
            String fileName = "crash" + (exception ? "" : "log") + "-" + time + "-" + timestamp + ".log";
            String file_dir = getFilePath();
            // if
            // (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            // {
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
            // 发送给开发人员
            sendCrashLog2PM(file_dir + fileName);
            fos.close();
            // }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    /**
     * 将捕获的导致崩溃的错误信息发送给开发人员 目前只将log日志保存在sdcard 和输出到LogCat中，并未发送给后台。
     */
    private void sendCrashLog2PM(String fileName) {
        if (!new File(fileName).exists()) {
            Toast.makeText(mContext, "日志文件不存在！", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    //停止使用
//    class sdLogThread extends Thread {
//        public final String filename;
//        private OutputStream outputStream;
//
//        public sdLogThread(String filename) {
//            this.filename = filename;
//        }
//
//        @Override
//        public void run() {
//            InetSocketAddress target = new InetSocketAddress("172.27.29.1",
//                    6789);
//            InetSocketAddress target2 = new InetSocketAddress("192.168.0.112",
//                    6789);
//            Socket socket = new Socket();
//            try {
//                socket.connect(target);
//                outputStream = socket.getOutputStream();
//            } catch (IOException e) {
//                try {
//                    socket.connect(target2);
//                    outputStream = socket.getOutputStream();
//                } catch (IOException e1) {
//                    return;
//                }
//            }
//
//            FileInputStream fis = null;
//            BufferedReader reader = null;
//            String s = null;
//            try {
//                fis = new FileInputStream(filename);
//                reader = new BufferedReader(new InputStreamReader(fis, "GBK"));
//                while (true) {
//                    s = reader.readLine();
//                    if (s == null)
//                        break;
//                    // 由于目前尚未确定以何种方式发送，所以先打出log日志。
//                    Log.i("info", s.toString());
//                    sendDataAtTime(s.getBytes());
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally { // 关闭流
//                try {
//                    reader.close();
//                    fis.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    socket.close();
//                    socket = null;
//
//                    if (outputStream != null) {
//                        outputStream.close();
//                        outputStream = null;
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        // 立即发送消息
//        private boolean sendDataAtTime(byte[] data) {
//            if (outputStream != null) {
//                try {
//                    outputStream.write(data, 0, data.length);
//                    return true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            return false;
//        }
//
//    }
}