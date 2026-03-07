package com.astor.oplusconfighook;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 统一日志入口，负责日志落盘、级别控制与调试信息输出。
 */
public final class AppLogger {
    private static final String TAG = "AppLogger";
    private static final long MAX_LOG_BYTES = 512 * 1024;
    private static final String LOG_DIR = "logs";
    private static final String APP_LOG = "app.log";
    private static final String CRASH_LOG = "crash.log";

    private static volatile Context appContext;
    private static volatile Thread.UncaughtExceptionHandler previousHandler;
    private static volatile boolean crashHandlerInstalled;

    private AppLogger() {}

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
    }

    public static File logsDir() {
        if (appContext == null) return null;
        File d = new File(appContext.getFilesDir(), LOG_DIR);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File appLogFile() {
        File d = logsDir();
        return d == null ? null : new File(d, APP_LOG);
    }

    public static File crashLogFile() {
        File d = logsDir();
        return d == null ? null : new File(d, CRASH_LOG);
    }

    public static synchronized void i(String tag, String message) {
        write("I", tag, message, null, false);
    }

    public static synchronized void e(String tag, String message, Throwable throwable) {
        write("E", tag, message, throwable, false);
    }

    public static synchronized void writeCrash(String tag, String message, Throwable throwable) {
        write("E", tag, message, throwable, true);
    }

    private static void write(String level, String tag, String message, Throwable throwable, boolean crashFile) {
        File target = crashFile ? crashLogFile() : appLogFile();
        if (target == null) return;
        ensureSize(target);
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        String line = ts + " [" + Thread.currentThread().getName() + "] " + level + "/" + tag + ": " + (message == null ? "" : message);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(target, true))) {
            w.write(line);
            w.newLine();
            if (throwable != null) {
                w.write(Log.getStackTraceString(throwable));
                w.newLine();
            }
        } catch (IOException e) {
            Log.e(TAG, "write log failed", e);
        }
    }

    private static void ensureSize(File f) {
        if (!f.exists()) return;
        if (f.length() < MAX_LOG_BYTES) return;
        File old = new File(f.getParentFile(), f.getName() + ".1");
        if (old.exists()) old.delete();
        f.renameTo(old);
    }

    public static synchronized void installCrashHandler() {
        if (crashHandlerInstalled) return;
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            writeCrash("Uncaught", "Thread=" + (t == null ? "null" : t.getName()), e);
            if (previousHandler != null) {
                previousHandler.uncaughtException(t, e);
            }
        });
        crashHandlerInstalled = true;
    }
}
