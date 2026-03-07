package com.astor.oplusconfighook;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 日志导出工具，负责组装并分享诊断日志文件。
 */
public final class LogExporter {
    private LogExporter() {}

    public static void exportLogsZip(Context context, Uri uri) throws Exception {
        if (context == null || uri == null) throw new IllegalArgumentException("context/uri null");
        File logsDir = AppLogger.logsDir();
        if (logsDir == null || !logsDir.exists()) throw new IllegalStateException("logs dir missing");

        try (OutputStream out = context.getContentResolver().openOutputStream(uri);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            File[] files = logsDir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f == null || !f.isFile()) continue;
                ZipEntry entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                try (FileInputStream in = new FileInputStream(f)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) > 0) zos.write(buf, 0, read);
                }
                zos.closeEntry();
            }
        }
    }

    public static String readLastCrashText() {
        File crash = AppLogger.crashLogFile();
        if (crash == null || !crash.exists()) return "";
        try {
            byte[] data = java.nio.file.Files.readAllBytes(crash.toPath());
            return new String(data);
        } catch (Exception ignored) {
            return "";
        }
    }
}
