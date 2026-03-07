package com.astor.oplusconfighook;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 策略存储层，负责策略对象与持久化文本之间转换。
 */
public final class PolicyStore {
    private static final String TAG = "PolicyStore";
    public enum Source { USER, ASSET, NONE }

    private static final String USER_DIR = "user_config";
    private static final String USER_AUTOSTART = "autostart_white_list.txt";
    private static final String USER_TOMBSTONE = "sys_elsa_config_list.xml";
    private static final String USER_ASSOCIATE = "associate_white_list.txt";

    private static volatile byte[] autostartBytesEffective;
    private static volatile byte[] tombstoneXmlBytesEffective;
    private static volatile Source autostartSource = Source.NONE;
    private static volatile Source tombstoneSource = Source.NONE;
    private static volatile long autostartMtime = Long.MIN_VALUE;
    private static volatile long tombstoneMtime = Long.MIN_VALUE;
    private static volatile long lastAutostartFastLogMs = 0L;
    private static final long FAST_LOG_THROTTLE_MS = 5000L;

    private PolicyStore() {}

    public static byte[] autostartBytesFast() {
        long now = System.currentTimeMillis();
        if (now - lastAutostartFastLogMs > FAST_LOG_THROTTLE_MS) {
            lastAutostartFastLogMs = now;
            int len = autostartBytesEffective == null ? -1 : autostartBytesEffective.length;
            Log.i(TAG, "[POLICY] autostartBytesFast null=" + (autostartBytesEffective == null) + " len=" + len + " source=" + autostartSource + " mtime=" + autostartMtime);
        }
        return autostartBytesEffective;
    }

    public static byte[] tombstoneBytesFast() {
        return tombstoneXmlBytesEffective;
    }

    public static Source autostartSource() { return autostartSource; }
    public static Source tombstoneSource() { return tombstoneSource; }

    public static String autostartDebugSummary() {
        int len = autostartBytesEffective == null ? -1 : autostartBytesEffective.length;
        return "source=" + autostartSource + ",len=" + len + ",mtime=" + autostartMtime;
    }

    public static synchronized void refresh(Context context) {
        if (context == null) return;
        migrateUserDirCeToDpIfNeeded(context);
        Log.i(TAG, "[POLICY] refresh start");
        refreshAutostart(context);
        refreshTombstone(context);
        Log.i(TAG, "[POLICY] refresh done autostart=" + autostartDebugSummary() + " tombstoneSource=" + tombstoneSource + " tombstoneLen=" + (tombstoneXmlBytesEffective == null ? -1 : tombstoneXmlBytesEffective.length));
    }

    public static File userAutostartFile(Context c) { return new File(userDir(c), USER_AUTOSTART); }
    public static File userTombstoneFile(Context c) { return new File(userDir(c), USER_TOMBSTONE); }

    public static synchronized void saveAutostartPackages(Context context, Set<String> packages) throws IOException {
        migrateUserDirCeToDpIfNeeded(context);
        List<String> sorted = new ArrayList<>(packages);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (String p : sorted) {
            String t = p == null ? "" : p.trim();
            if (!t.isEmpty()) sb.append(t).append('\n');
        }
        writeFile(userAutostartFile(context), sb.toString().getBytes(StandardCharsets.UTF_8));
        refresh(context);
    }

    public static synchronized void saveAutostartRaw(Context context, byte[] data) throws IOException {
        migrateUserDirCeToDpIfNeeded(context);
        writeFile(userAutostartFile(context), data);
        refresh(context);
    }

    public static synchronized void saveTombstoneRaw(Context context, byte[] data) throws IOException {
        migrateUserDirCeToDpIfNeeded(context);
        byte[] normalized = normalizeTombstoneXml(data);
        writeFile(userTombstoneFile(context), normalized);
        refresh(context);
    }

    public static synchronized void deleteAutostartUser(Context context) {
        File file = userAutostartFile(context);
        if (file.exists()) file.delete();
        autostartMtime = Long.MIN_VALUE;
        refresh(context);
    }

    public static synchronized void deleteTombstoneUser(Context context) {
        File file = userTombstoneFile(context);
        if (file.exists()) file.delete();
        tombstoneMtime = Long.MIN_VALUE;
        refresh(context);
    }

    public static Set<String> parseAutostartPackages(byte[] data) {
        if (data == null) return Collections.emptySet();
        String text = new String(data, StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            out.add(t);
        }
        return out;
    }

    private static void refreshAutostart(Context context) {
        File user = userAutostartFile(context);
        long mtime = user.exists() ? user.lastModified() : -1L;
        if (autostartBytesEffective != null && autostartMtime == mtime) return;
        autostartMtime = mtime;

        Log.i(TAG, "[POLICY] autostart try=file path=" + user.getAbsolutePath() + " exists=" + user.exists());
        byte[] userBytes = readFileLogged(user, "autostart_user_file");
        if (userBytes != null && userBytes.length > 0) {
            autostartBytesEffective = userBytes;
            autostartSource = Source.USER;
            Log.i(TAG, "[POLICY] autostart loaded source=USER len=" + userBytes.length);
            return;
        }
        Log.i(TAG, "[POLICY] autostart try=asset name=" + USER_AUTOSTART);
        autostartBytesEffective = readAssetLogged(context, USER_AUTOSTART, "autostart_asset");
        autostartSource = autostartBytesEffective == null ? Source.NONE : Source.ASSET;
        Log.i(TAG, "[POLICY] autostart loaded source=" + autostartSource + " len=" + (autostartBytesEffective == null ? -1 : autostartBytesEffective.length));
    }

    private static void refreshTombstone(Context context) {
        File user = userTombstoneFile(context);
        long mtime = user.exists() ? user.lastModified() : -1L;
        if (tombstoneXmlBytesEffective != null && tombstoneMtime == mtime) return;
        tombstoneMtime = mtime;

        byte[] userBytes = readFile(user);
        if (userBytes != null && userBytes.length > 0) {
            tombstoneXmlBytesEffective = normalizeTombstoneXml(userBytes);
            tombstoneSource = Source.USER;
            return;
        }
        byte[] assetBytes = readAsset(context, USER_TOMBSTONE);
        tombstoneXmlBytesEffective = normalizeTombstoneXml(assetBytes);
        tombstoneSource = tombstoneXmlBytesEffective == null ? Source.NONE : Source.ASSET;
    }

    private static byte[] normalizeTombstoneXml(byte[] raw) {
        if (raw == null || raw.length == 0) return raw;
        String text = new String(raw, StandardCharsets.UTF_8);
        int first = text.indexOf("<?xml");
        int second = first < 0 ? -1 : text.indexOf("<?xml", first + 1);
        byte[] candidate = raw;
        if (second > 0) {
            candidate = text.substring(second).getBytes(StandardCharsets.UTF_8);
            Log.w(TAG, "Detected duplicated XML declaration, keeping last segment for tombstone XML");
        }
        try {
            TombstoneXmlEditor.parse(candidate);
            return candidate;
        } catch (Exception e) {
            Log.e(TAG, "normalize tombstone xml failed, keep original", e);
            return raw;
        }
    }

    private static File dpUserDir(Context context) {
        Context dp = context.createDeviceProtectedStorageContext();
        return new File(dp.getFilesDir(), USER_DIR);
    }

    private static File ceUserDir(Context context) {
        return new File(context.getFilesDir(), USER_DIR);
    }

    public static File userDir(Context context) {
        return dpUserDir(context);
    }

    public static synchronized void migrateUserDirCeToDpIfNeeded(Context context) {
        if (context == null) return;
        File ce = ceUserDir(context);
        File dp = dpUserDir(context);
        if (!ce.exists()) return;
        if (!dp.exists() && !dp.mkdirs()) {
            Log.w(TAG, "[POLICY] migrate skipped: cannot create dp dir path=" + dp.getAbsolutePath());
            return;
        }
        boolean migrated = false;
        migrated |= copyIfMissing(new File(ce, USER_TOMBSTONE), new File(dp, USER_TOMBSTONE));
        migrated |= copyIfMissing(new File(ce, USER_AUTOSTART), new File(dp, USER_AUTOSTART));
        migrated |= copyIfMissing(new File(ce, USER_ASSOCIATE), new File(dp, USER_ASSOCIATE));
        Log.i(TAG, "[POLICY] migrate ce_to_dp migrated=" + migrated + " ce=" + ce.getAbsolutePath() + " dp=" + dp.getAbsolutePath());
    }

    private static boolean copyIfMissing(File src, File dst) {
        if (src == null || dst == null || !src.exists() || !src.isFile() || src.length() <= 0) return false;
        if (dst.exists() && dst.length() > 0) return false;
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
        File tmp = new File(dst.getParentFile(), dst.getName() + ".tmp");
        try (InputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(tmp, false)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
            out.getFD().sync();
            if (dst.exists() && !dst.delete()) return false;
            if (!tmp.renameTo(dst)) return false;
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "[POLICY] copyIfMissing failed src=" + src.getAbsolutePath() + " dst=" + dst.getAbsolutePath() + " err=" + t.getClass().getSimpleName());
            return false;
        } finally {
            if (tmp.exists()) tmp.delete();
        }
    }

    private static void writeFile(File f, byte[] data) throws IOException {
        if (data == null) throw new IOException("data is null");
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(data);
        }
    }

    private static byte[] readFile(File file) {
        if (file == null || !file.exists()) return null;
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readFileLogged(File file, String stage) {
        if (file == null || !file.exists()) {
            Log.i(TAG, "[POLICY] " + stage + " result=not_found");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] out = readAll(in);
            Log.i(TAG, "[POLICY] " + stage + " result=ok len=" + (out == null ? -1 : out.length));
            return out;
        } catch (Exception e) {
            Log.e(TAG, "[POLICY] " + stage + " result=exception:" + e.getClass().getSimpleName(), e);
            return null;
        }
    }

    private static byte[] readAsset(Context context, String name) {
        try (InputStream in = context.getAssets().open(name)) {
            return readAll(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAssetLogged(Context context, String name, String stage) {
        try (InputStream in = context.getAssets().open(name)) {
            byte[] out = readAll(in);
            Log.i(TAG, "[POLICY] " + stage + " result=ok len=" + (out == null ? -1 : out.length));
            return out;
        } catch (Exception e) {
            Log.e(TAG, "[POLICY] " + stage + " result=exception:" + e.getClass().getSimpleName(), e);
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) > 0) bos.write(buf, 0, read);
        return bos.toByteArray();
    }
}
