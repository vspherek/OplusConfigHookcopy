package com.astor.oplusconfighook;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用列表仓库，负责异步预加载、缓存与监听分发。
 */
public final class AppListRepository {
    public enum PreloadState { IDLE, LOADING, READY, ERROR }

    public static final class Snapshot {
        public final PreloadState state;
        public final List<AppInfo> apps;
        public final String error;

        Snapshot(PreloadState state, List<AppInfo> apps, String error) {
            this.state = state;
            this.apps = apps;
            this.error = error;
        }
    }

    public interface Listener {
        void onChanged(Snapshot snapshot);
    }

    private static final String TAG = "AppListFlow";
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<Listener> listeners = new LinkedHashSet<>();
    private static volatile List<AppInfo> cache;
    private static volatile PreloadState preloadState = PreloadState.IDLE;
    private static volatile String preloadError;
    private static volatile boolean loading;

    private AppListRepository() {}

    /**
     * 已废弃：请改用 {@link #tryGetCachedApps()}，该接口当前为非阻塞读取。
     */
    @Deprecated
    public static List<AppInfo> getInstalledApps(Context context) {
        ensurePreloaded(context);
        List<AppInfo> local = tryGetCachedApps();
        return local == null ? new ArrayList<>() : local;
    }

    public static List<AppInfo> tryGetCachedApps() {
        synchronized (LOCK) {
            return cache == null ? null : new ArrayList<>(cache);
        }
    }

    public static void ensurePreloaded(Context context) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();
        boolean shouldStart = false;
        synchronized (LOCK) {
            if (cache != null || loading) return;
            loading = true;
            preloadState = PreloadState.LOADING;
            preloadError = null;
            shouldStart = true;
        }
        if (!shouldStart) return;
        notifyListeners(snapshot());
        AppLogger.i(TAG, "appList load start");

        Thread worker = new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                List<AppInfo> loaded = loadApps(appCtx);
                synchronized (LOCK) {
                    cache = loaded;
                    preloadState = PreloadState.READY;
                    loading = false;
                    preloadError = null;
                    LOCK.notifyAll();
                }
                AppLogger.i(TAG, "appList load success count=" + loaded.size() + " costMs=" + (System.currentTimeMillis() - start));
            } catch (Throwable t) {
                synchronized (LOCK) {
                    preloadState = PreloadState.ERROR;
                    preloadError = t.getMessage();
                    loading = false;
                    LOCK.notifyAll();
                }
                AppLogger.e(TAG, "appList load failed", t);
            }
            notifyListeners(snapshot());
        }, "app-list-preload");
        worker.setDaemon(true);
        worker.start();
    }

    public static Snapshot snapshot() {
        synchronized (LOCK) {
            List<AppInfo> apps = cache == null ? null : new ArrayList<>(cache);
            return new Snapshot(preloadState, apps, preloadError);
        }
    }

    public static void registerListener(Listener listener) {
        if (listener == null) return;
        Snapshot snap;
        synchronized (LOCK) {
            listeners.add(listener);
            snap = snapshot();
        }
        listener.onChanged(snap);
    }

    public static void unregisterListener(Listener listener) {
        if (listener == null) return;
        synchronized (LOCK) {
            listeners.remove(listener);
        }
    }

    public static PreloadState preloadState() {
        return preloadState;
    }

    public static String preloadError() {
        return preloadError;
    }

    private static void notifyListeners(Snapshot snapshot) {
        List<Listener> copy;
        synchronized (LOCK) {
            copy = new ArrayList<>(listeners);
        }
        for (Listener listener : copy) {
            MAIN_HANDLER.post(() -> {
                try {
                    listener.onChanged(snapshot);
                } catch (Throwable t) {
                    AppLogger.e(TAG, "listener failed", t);
                }
            });
        }
    }

    private static List<AppInfo> loadApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, AppInfo> out = new LinkedHashMap<>();
        Set<String> launchablePkgs = new LinkedHashSet<>();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL);
        for (ResolveInfo ri : launchables) {
            ApplicationInfo ai = ri.activityInfo == null ? null : ri.activityInfo.applicationInfo;
            if (ai != null) launchablePkgs.add(ai.packageName);
        }

        List<PackageInfo> installed = pm.getInstalledPackages(PackageManager.MATCH_ALL);
        for (PackageInfo pi : installed) {
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) continue;
            out.put(ai.packageName, toAppInfo(pm, ai, pi, launchablePkgs.contains(ai.packageName)));
        }

        return new ArrayList<>(out.values());
    }

    private static AppInfo toAppInfo(PackageManager pm, ApplicationInfo ai, PackageInfo pi, boolean launchable) {
        String label = String.valueOf(ai.loadLabel(pm));
        boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        String effectiveLabel = (label == null || label.trim().isEmpty()) ? ai.packageName : label;
        if (launchable && effectiveLabel.equals(ai.packageName)) {
            effectiveLabel = ai.packageName;
        }
        return new AppInfo(ai.packageName, effectiveLabel, system, pi.firstInstallTime, pi.lastUpdateTime);
    }
}
