package simen.crashlogsample;

import android.app.Activity;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            String ttm = null;
            ttm.length();
        } catch (NullPointerException e) {
            reportError(this, e);
        }

        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApplicationErrorReport errorReport;
            }
        });
        findViewById(R.id.click2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printStrack();
                Thread.dumpStack();
            }
        });

        printStrack();
    }

    private void printStrack() {
        int count = Thread.activeCount();
        Thread[] cc = new Thread[count];
        Thread.enumerate(cc);

        StringBuilder builder = new StringBuilder();
        for (Thread item : cc) {
            Log.d(TAG, "onCreate: " + item.toString());
            try {
                printStackTrace(builder, "StackTrace", item.getStackTrace());
            } catch (IOException e) {
            }
        }
        System.out.println(builder.toString());

    }

    private void printStackTrace(Appendable msg, String indent, StackTraceElement[] stack)
            throws IOException {
        if (stack == null) return;
        msg.append("\n");
        for (int i = 0; i < stack.length; i++) {
            msg.append(indent);
            msg.append("\tat ");
            msg.append(stack[i].toString());
            msg.append("\n");
        }
    }

    public static void reportError(Context context, Throwable e) {
        ApplicationErrorReport report = new ApplicationErrorReport();
        report.packageName = report.processName = context.getPackageName();
        report.time = System.currentTimeMillis();
        report.type = ApplicationErrorReport.TYPE_CRASH;
        report.systemApp = false;

        ApplicationErrorReport.CrashInfo crash = new ApplicationErrorReport.CrashInfo();
        crash.exceptionClassName = e.getClass().getSimpleName();
        crash.exceptionMessage = e.getMessage();

        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        e.printStackTrace(printer);

        crash.stackTrace = writer.toString();

        StackTraceElement stack = e.getStackTrace()[0];
        crash.throwClassName = stack.getClassName();
        crash.throwFileName = stack.getFileName();
        crash.throwLineNumber = stack.getLineNumber();
        crash.throwMethodName = stack.getMethodName();

        report.crashInfo = crash;

        Intent intent = new Intent(Intent.ACTION_APP_ERROR);
        intent.putExtra(Intent.EXTRA_BUG_REPORT, report);
        context.startActivity(intent);
    }
}
