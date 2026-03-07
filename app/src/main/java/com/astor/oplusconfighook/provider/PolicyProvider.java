package com.astor.oplusconfighook.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astor.oplusconfighook.PolicyStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PolicyProvider extends ContentProvider {
    private static final String TAG = "PolicyProvider";
    private static final String USER_TOMBSTONE = "sys_elsa_config_list.xml";
    private static final String USER_AUTOSTART = "autostart_white_list.txt";

    private static final class PolicyBytes {
        final byte[] data;
        final String source;
        final String path;
        final long mtime;
        final String sha256;

        PolicyBytes(byte[] data, String source, String path, long mtime) {
            this.data = data;
            this.source = source;
            this.path = path;
            this.mtime = mtime;
            this.sha256 = sha256Hex(data);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) {
        enforceCallerAllowed();
        Context c = getContext();
        if (c == null) throw new IllegalStateException("null context");

        String last = uri.getLastPathSegment();
        final PolicyBytes pb;
        if ("tombstone".equals(last)) {
            pb = loadPolicy(c, USER_TOMBSTONE);
            Log.i(TAG, "[POLICY] tombstone source=" + pb.source + " provider=true len=" + pb.data.length + " sha256=" + pb.sha256 + (pb.path.isEmpty() ? "" : " path=" + pb.path + " mtime=" + pb.mtime));
        } else if ("autostart_white_list".equals(last)) {
            pb = loadPolicy(c, USER_AUTOSTART);
            Log.i(TAG, "[POLICY] autostart source=" + pb.source + " provider=true len=" + pb.data.length + " sha256=" + pb.sha256 + (pb.path.isEmpty() ? "" : " path=" + pb.path + " mtime=" + pb.mtime));
        } else {
            throw new IllegalArgumentException("Unsupported path: " + uri);
        }

        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            new Thread(() -> {
                try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    out.write(pb.data);
                    out.flush();
                } catch (Throwable t) {
                    Log.w(TAG, "pipe write failed: " + t.getClass().getSimpleName() + ":" + t.getMessage());
                }
            }, "PolicyProviderPipe").start();
            return pipe[0];
        } catch (IOException e) {
            throw new IllegalStateException("createPipe failed", e);
        }
    }

    private PolicyBytes loadPolicy(Context c, String fileName) {
        PolicyStore.migrateUserDirCeToDpIfNeeded(c);
        File user = new File(PolicyStore.userDir(c), fileName);
        if (user.exists() && user.canRead() && user.length() > 0) {
            byte[] bytes = readFile(user);
            if (bytes != null && bytes.length > 0) {
                return new PolicyBytes(bytes, "USER", user.getAbsolutePath(), user.lastModified());
            }
        }
        byte[] asset = readAsset(c, fileName);
        if (asset == null) asset = new byte[0];
        return new PolicyBytes(asset, "ASSET", "", -1L);
    }

    private void enforceCallerAllowed() {
        Context context = getContext();
        if (context == null) throw new SecurityException("Context unavailable");
        int uid = Binder.getCallingUid();
        PackageManager pm = context.getPackageManager();
        String[] pkgs = pm.getPackagesForUid(uid);
        Set<String> allowed = new HashSet<>(Arrays.asList(
                "com.oplus.athena",
                "com.oplus.battery",
                context.getPackageName()
        ));
        if (pkgs != null) {
            for (String p : pkgs) {
                if (allowed.contains(p)) return;
            }
        }
        throw new SecurityException("caller not allowed uid=" + uid + " pkgs=" + Arrays.toString(pkgs));
    }

    private static byte[] readFile(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (Throwable e) {
            return null;
        }
    }

    private static byte[] readAsset(Context context, String name) {
        try (InputStream in = context.getAssets().open(name)) {
            return readAll(in);
        } catch (Throwable e) {
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

    private static String sha256Hex(byte[] data) {
        if (data == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(data);
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    @Nullable @Override public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) { return null; }
    @Nullable @Override public String getType(@NonNull Uri uri) { return "application/octet-stream"; }
    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
