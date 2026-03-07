package com.astor.oplusconfighook;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SharedPreferences 访问封装，集中管理本地配置项读写。
 */
public final class Prefs {
    public static final String PREF_NAME = "oplus_config_hook";
    private static final String PREF_ONBOARDING = "oplus_config_onboarding";
    public static final String KEY_FREE_NOTICE_SHOWN = "free_notice_shown";
    public static final String KEY_CONFIG_INITIALIZED = "config_initialized";

    public static final String KEY_ENABLED_MASTER = "enabled_master";
    public static final String KEY_ENABLED_AUTOSTART = "enabled_autostart";
    public static final String KEY_ENABLED_TOMBSTONE = "enabled_tombstone";
    public static final String KEY_LOG_ONLY_MATCHED = "log_only_matched_keys";
    public static final String KEY_LOG_STACKTRACE = "log_stacktrace";
    public static final String KEY_TOMBSTONE_ADVANCED_EDIT = "tombstone_advanced_edit";
    public static final String KEY_ALLOW_ANDROID_FS_HOOK = "allow_android_fs_hook";
    public static final String KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE = "allow_phonemanager_readconfig_observe";
    public static final String KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY = "allow_phonemanager_readconfig_modify";
    public static final String KEY_ALLOW_PHONEMANAGER_FS_HOOK = "allow_phonemanager_fs_hook";
    public static final String KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE = "allow_safecenter_readconfig_observe";
    public static final String KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY = "allow_safecenter_readconfig_modify";
    public static final String KEY_ALLOW_SAFECENTER_FS_HOOK = "allow_safecenter_fs_hook";

    private static final String TAG = "Prefs";
    private static final String RUNTIME_FLAGS = "runtime_flags.properties";
    public static final String KEY_DEBUG_FORCE_MASTER = "debug_force_master";
    private static final String PROP_MASTER = "persist.opch.enabled_master";
    private static final String PROP_AUTOSTART = "persist.opch.enabled_autostart";
    private static final String PROP_TOMBSTONE = "persist.opch.enabled_tombstone";
    private static final String PROP_LOG_ONLY = "persist.opch.log_only_matched";
    private static final String PROP_ALLOW_ANDROID_FS = "persist.opch.allow_android_fs_hook";
    private static final String PROP_DEBUG_FORCE_MASTER = "persist.opch.debug_force_master";
    private static final String PROP_PM_RC_OBS = "persist.opch.allow_pm_rc_observe";
    private static final String PROP_PM_RC_MOD = "persist.opch.allow_pm_rc_modify";
    private static final String PROP_PM_FS = "persist.opch.allow_pm_fs_hook";
    private static final String PROP_SC_RC_OBS = "persist.opch.allow_sc_rc_observe";
    private static final String PROP_SC_RC_MOD = "persist.opch.allow_sc_rc_modify";
    private static final String PROP_SC_FS = "persist.opch.allow_sc_fs_hook";
    private static final String PROP_ONBOARDING_SHOWN = "persist.opch.onboarding_shown";
    private static volatile long lastReadableLogMs = 0L;
    private static final long READABLE_LOG_MS = 10_000L;
    private static volatile long lastMasterDiagMs = 0L;
    private static final long MASTER_DIAG_MS = 10_000L;
    private static volatile boolean defaultsInitLogged = false;
    private static volatile boolean startupDiagLogged = false;
    private static final ExecutorService sIo = Executors.newSingleThreadExecutor();
    private static final Handler sMain = new Handler(Looper.getMainLooper());
    private static final Object sFlushLock = new Object();
    private static volatile boolean sInSystemCleanup = false;
    private static Context sFlushContext;
    private static String sLastChangedKey;
    private static boolean sLastChangedValue;
    private static final Runnable sFlushTask = new Runnable() {
        @Override
        public void run() {
            final Context flushContext;
            final String changedKey;
            final boolean changedValue;
            synchronized (sFlushLock) {
                flushContext = sFlushContext;
                changedKey = sLastChangedKey;
                changedValue = sLastChangedValue;
            }
            if (flushContext == null) return;
            if (sInSystemCleanup) {
                logLine("[FLUSH] skipped reason=in_system_cleanup");
                return;
            }
            sIo.execute(new Runnable() {
                @Override
                public void run() {
                    if (sInSystemCleanup) return;
                    boolean runtimeFlagsWriteOk = writeRuntimeFlags(flushContext, changedKey, changedValue);
                    boolean sysPropWriteOk = writeSystemProperties(flushContext);
                    ensurePrefsReadable(flushContext);
                    logLine("[CONFIG_COMMIT] key=" + changedKey
                            + " master=" + enabledMaster(flushContext)
                            + " autostart=" + enabledAutostart(flushContext)
                            + " tombstone=" + enabledTombstone(flushContext)
                            + " allowAndroidFsHook=" + allowAndroidFsHook(flushContext)
                            + " onboardingShown=" + onboardingShown(flushContext)
                            + " prefsCeWriteOk=true"
                            + " prefsDeWriteOk=true"
                            + " sysPropWriteOk=" + sysPropWriteOk
                            + " runtimeFlagsWriteOk=" + runtimeFlagsWriteOk
                            + " snapshotWriteOk=n/a"
                            + " timestamp=" + System.currentTimeMillis());
                }
            });
        }
    };
    private static final class BoolStoreRead {
        final String key;
        final boolean ceContains;
        final boolean ceValue;
        final boolean deContains;
        final boolean deValue;
        final boolean finalValue;
        final String source;
        final boolean hasUserFact;
        final String ceError;
        final String deError;

        BoolStoreRead(String key, boolean ceContains, boolean ceValue, boolean deContains, boolean deValue, boolean finalValue, String source,
                      boolean hasUserFact, String ceError, String deError) {
            this.key = key;
            this.ceContains = ceContains;
            this.ceValue = ceValue;
            this.deContains = deContains;
            this.deValue = deValue;
            this.finalValue = finalValue;
            this.source = source;
            this.hasUserFact = hasUserFact;
            this.ceError = ceError;
            this.deError = deError;
        }
    }
    private static final String[] DEFAULT_BOOL_KEYS = new String[] {
            KEY_ENABLED_MASTER,
            KEY_ENABLED_AUTOSTART,
            KEY_ENABLED_TOMBSTONE,
            KEY_ALLOW_ANDROID_FS_HOOK,
            KEY_LOG_ONLY_MATCHED,
            KEY_LOG_STACKTRACE,
            KEY_TOMBSTONE_ADVANCED_EDIT,
            KEY_DEBUG_FORCE_MASTER,
            KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE,
            KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY,
            KEY_ALLOW_PHONEMANAGER_FS_HOOK,
            KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE,
            KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY,
            KEY_ALLOW_SAFECENTER_FS_HOOK
    };

    private Prefs() {}

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences spDe(Context c) {
        Context de = c.createDeviceProtectedStorageContext();
        return de.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences spOnboardingCe(Context c) {
        return c.getSharedPreferences(PREF_ONBOARDING, Context.MODE_PRIVATE);
    }

    public static void putBoolean(Context c, String key, boolean value) {
        if (c == null) return;
        Context appContext = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        SharedPreferences ce = sp(c);
        SharedPreferences de = spDe(c);
        boolean beforeCe = ce.getBoolean(key, defaultForKey(key));
        boolean beforeDe = de.getBoolean(key, defaultForKey(key));
        boolean forceCommit = KEY_ENABLED_MASTER.equals(key)
                || KEY_LOG_STACKTRACE.equals(key)
                || KEY_TOMBSTONE_ADVANCED_EDIT.equals(key);

        boolean prefsCeWriteOk;
        boolean prefsDeWriteOk;
        if (forceCommit) {
            prefsCeWriteOk = ce.edit().putBoolean(key, value).commit();
            prefsDeWriteOk = de.edit().putBoolean(key, value).commit();
        } else {
            ce.edit().putBoolean(key, value).apply();
            de.edit().putBoolean(key, value).apply();
            prefsCeWriteOk = true;
            prefsDeWriteOk = true;
        }

        logMasterWrite(c, key, beforeCe, beforeDe, value, "putBoolean", forceCommit ? "commit_ce_de" : "apply_ce_de");
        logStore("ui_write", "CE+DE", key, value);
        boolean ceNow = ce.getBoolean(key, defaultForKey(key));
        boolean deNow = de.getBoolean(key, defaultForKey(key));
        logLine("[PREF_WRITE_VERIFY] key=" + key + " ceNow=" + ceNow + " deNow=" + deNow
                + " mode=" + (forceCommit ? "commit" : "apply"));

        synchronized (sFlushLock) {
            sFlushContext = appContext;
            sLastChangedKey = key;
            sLastChangedValue = value;
        }
        sMain.removeCallbacks(sFlushTask);
        sMain.postDelayed(sFlushTask, 300L);

        logLine("[CONFIG_COMMIT] key=" + key
                + " master=" + enabledMaster(c)
                + " autostart=" + enabledAutostart(c)
                + " tombstone=" + enabledTombstone(c)
                + " allowAndroidFsHook=" + allowAndroidFsHook(c)
                + " onboardingShown=" + onboardingShown(c)
                + " prefsCeWriteOk=" + prefsCeWriteOk
                + " prefsDeWriteOk=" + prefsDeWriteOk
                + " sysPropWriteOk=deferred"
                + " runtimeFlagsWriteOk=deferred"
                + " snapshotWriteOk=n/a"
                + " timestamp=" + System.currentTimeMillis());
    }

    public static boolean enabledMaster(Context c) {
        if (c == null) return true;
        return readBoolWithStores(c, KEY_ENABLED_MASTER, true).finalValue;
    }

    public static String enabledMasterSource(Context c) {
        if (c == null) return "default";
        return readBoolWithStores(c, KEY_ENABLED_MASTER, true).source;
    }

    public static String enabledAutostartSource(Context c) {
        if (c == null) return "default";
        return readBoolWithStores(c, KEY_ENABLED_AUTOSTART, true).source;
    }

    public static String enabledTombstoneSource(Context c) {
        if (c == null) return "default";
        return readBoolWithStores(c, KEY_ENABLED_TOMBSTONE, true).source;
    }

    public static String hookConfigSourceSummary(Context c) {
        if (c == null) return "default";
        String m = enabledMasterSource(c);
        String a = enabledAutostartSource(c);
        String t = enabledTombstoneSource(c);
        return (m.equals(a) && a.equals(t)) ? m : ("mixed(m=" + m + ",a=" + a + ",t=" + t + ")");
    }
    public static boolean enabledAutostart(Context c) {
        if (c == null) return true;
        return readBoolWithStores(c, KEY_ENABLED_AUTOSTART, true).finalValue;
    }
    public static boolean enabledTombstone(Context c) {
        if (c == null) return true;
        return readBoolWithStores(c, KEY_ENABLED_TOMBSTONE, true).finalValue;
    }
    public static boolean logOnlyMatches(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_LOG_ONLY_MATCHED, false).finalValue;
    }
    public static boolean logStack(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_LOG_STACKTRACE, false).finalValue;
    }

    public static boolean tombstoneAdvancedEdit(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_TOMBSTONE_ADVANCED_EDIT, false).finalValue;
    }
    public static boolean allowAndroidFsHook(Context c) {
        if (c == null) return true;
        return readBoolWithStores(c, KEY_ALLOW_ANDROID_FS_HOOK, true).finalValue;
    }
    public static boolean debugForceMaster(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_DEBUG_FORCE_MASTER, false).finalValue;
    }

    public static boolean allowPhonemanagerReadConfigObserve(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, false).finalValue;
    }
    public static boolean allowPhonemanagerReadConfigModify(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, false).finalValue;
    }
    public static boolean allowPhonemanagerFsHook(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_PHONEMANAGER_FS_HOOK, false).finalValue;
    }
    public static boolean allowSafecenterReadConfigObserve(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, false).finalValue;
    }
    public static boolean allowSafecenterReadConfigModify(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, false).finalValue;
    }
    public static boolean allowSafecenterFsHook(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_ALLOW_SAFECENTER_FS_HOOK, false).finalValue;
    }


    public static boolean configInitialized(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_CONFIG_INITIALIZED, false).finalValue;
    }

    public static boolean onboardingShown(Context c) {
        if (c == null) return false;
        return readBoolWithStores(c, KEY_FREE_NOTICE_SHOWN, false).finalValue;
    }

    public static boolean onboardingShownPrefsOnly(Context c) {
        if (c == null) return false;
        boolean ceContains = false;
        String ceValue = "null";
        boolean resolved = false;
        try {
            SharedPreferences ce = spOnboardingCe(c);
            ceContains = ce.contains(KEY_FREE_NOTICE_SHOWN);
            if (ceContains) {
                resolved = ce.getBoolean(KEY_FREE_NOTICE_SHOWN, false);
                ceValue = String.valueOf(resolved);
            }
        } catch (Throwable t) {
            logLine("[ONBOARDING_PREFS_ONLY] store=onboarding ceContains=" + ceContains
                    + " ceValue=" + ceValue
                    + " final=false err=" + t.getClass().getSimpleName() + ":" + t.getMessage());
            return false;
        }

        logLine("[ONBOARDING_PREFS_ONLY] store=onboarding ceContains=" + ceContains
                + " ceValue=" + ceValue
                + " final=" + resolved);
        return resolved;
    }

    public static boolean onboardingShownStable(Context c) {
        String p = getSysProp(PROP_ONBOARDING_SHOWN, "");
        if (p != null) {
            String s = p.trim();
            if ("1".equals(s)
                    || "true".equalsIgnoreCase(s)
                    || "y".equalsIgnoreCase(s)
                    || "yes".equalsIgnoreCase(s)
                    || "on".equalsIgnoreCase(s)) {
                return true;
            }
        }
        return onboardingShownPrefsOnly(c);
    }

    public static void markOnboardingShownStable(Context c, String source) {
        if (c == null) return;
        markOnboardingShown(c, source + ":prefs");
        boolean ok = writeSysProp(PROP_ONBOARDING_SHOWN, "1");
        logLine("[ONBOARDING_SYS] source=" + source + " writePropKey=" + PROP_ONBOARDING_SHOWN + " ok=" + ok);
    }

    public static String onboardingShownStableSource(Context c) {
        String p = getSysProp(PROP_ONBOARDING_SHOWN, "");
        if (p != null) {
            String s = p.trim();
            if ("1".equals(s)
                    || "true".equalsIgnoreCase(s)
                    || "y".equalsIgnoreCase(s)
                    || "yes".equalsIgnoreCase(s)
                    || "on".equalsIgnoreCase(s)) {
                return "system_property";
            }
            if (!s.isEmpty()) return "system_property_invalid:" + s;
        }
        try {
            if (c != null) {
                SharedPreferences ce = spOnboardingCe(c);
                if (ce.contains(KEY_FREE_NOTICE_SHOWN)) return "prefs_onboarding_ce";
            }
        } catch (Throwable ignored) {
        }
        return "default";
    }

    public static String onboardingShownReason(Context c) {
        if (c == null) return "default";
        return readBoolWithStores(c, KEY_FREE_NOTICE_SHOWN, false).source;
    }

    public static boolean hasUserConfigurationFact(Context c) {
        if (c == null) return false;
        BoolStoreRead m = readBoolWithStores(c, KEY_ENABLED_MASTER, true);
        BoolStoreRead a = readBoolWithStores(c, KEY_ENABLED_AUTOSTART, true);
        BoolStoreRead t = readBoolWithStores(c, KEY_ENABLED_TOMBSTONE, true);
        BoolStoreRead fs = readBoolWithStores(c, KEY_ALLOW_ANDROID_FS_HOOK, true);
        return m.hasUserFact || a.hasUserFact || t.hasUserFact || fs.hasUserFact;
    }

    public static String userConfigurationFactSummary(Context c) {
        if (c == null) return "none";
        return "master=" + enabledMasterSource(c)
                + ",autostart=" + enabledAutostartSource(c)
                + ",tombstone=" + enabledTombstoneSource(c)
                + ",allowAndroidFs=" + readBoolWithStores(c, KEY_ALLOW_ANDROID_FS_HOOK, true).source;
    }

    public static void logOnboardingRead(Context c, String source, String showDecision, String decisionReason) {
        if (c == null) return;
        BoolStoreRead read = readBoolWithStores(c, KEY_FREE_NOTICE_SHOWN, false);
        logLine("[ONBOARDING_READ] source=" + source
                + " key=" + read.key
                + " ceContains=" + read.ceContains
                + " ceValue=" + (read.ceContains ? read.ceValue : "null")
                + " deContains=" + read.deContains
                + " deValue=" + (read.deContains ? read.deValue : "null")
                + " finalValue=" + read.finalValue
                + " finalSource=" + read.source
                + " ceErr=" + read.ceError
                + " deErr=" + read.deError
                + " showDecision=" + showDecision
                + " decisionReason=" + decisionReason);
    }

    public static void markOnboardingShown(Context c, String source) {
        if (c == null) return;
        boolean ceOk = spOnboardingCe(c).edit().putBoolean(KEY_FREE_NOTICE_SHOWN, true).commit();
        boolean ceNow = spOnboardingCe(c).getBoolean(KEY_FREE_NOTICE_SHOWN, false);
        logLine("[ONBOARDING_WRITE_VERIFY] store=onboarding ceOk=" + ceOk + " ceNow=" + ceNow + " source=" + source);
        logLine("[ONBOARDING] mark_shown source=" + source + " store=onboarding");
    }

    public static final class CleanupResult {
        public final boolean success;
        public final String message;

        CleanupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static CleanupResult cleanSystemPersistentState(Context c) {
        if (c == null) return new CleanupResult(false, "context null");

        sInSystemCleanup = true;
        sMain.removeCallbacks(sFlushTask);

        int ok = 0;
        int total = 0;

        String[] keys = new String[] {
                PROP_MASTER,
                PROP_AUTOSTART,
                PROP_TOMBSTONE,
                PROP_LOG_ONLY,
                PROP_ALLOW_ANDROID_FS,
                PROP_DEBUG_FORCE_MASTER,
                PROP_PM_RC_OBS,
                PROP_PM_RC_MOD,
                PROP_PM_FS,
                PROP_SC_RC_OBS,
                PROP_SC_RC_MOD,
                PROP_SC_FS,
                PROP_ONBOARDING_SHOWN
        };
        for (String key : keys) {
            total++;
            if (clearSysProp(key)) ok++;
        }

        total++;
        boolean rmOk = runShell("su", "-c", "rm -f /data/local/tmp/oplusconfighook/runtime_flags.properties");
        if (!rmOk) {
            runShell("su", "-c", "rmdir /data/local/tmp/oplusconfighook 2>/dev/null");
        }
        if (rmOk) ok++;

        sInSystemCleanup = false;

        String msg = c.getString(R.string.cleanup_system_state_result, ok, total);
        logLine("[CLEANUP] result ok=" + ok + " total=" + total + " rmOk=" + rmOk);
        return new CleanupResult(ok >= (total - 1), msg);
    }
    public static void ensureDefaults(Context c, String source) {
        if (c == null) return;
        SharedPreferences ce = sp(c);
        SharedPreferences de = spDe(c);
        Map<String, BoolStoreRead> initCheckReads = new HashMap<>();
        for (String key : DEFAULT_BOOL_KEYS) {
            initCheckReads.put(key, readBoolWithStores(c, key, defaultForKey(key)));
        }

        BoolStoreRead gateRead = initCheckReads.get(KEY_ENABLED_MASTER);
        boolean shouldInit = !gateRead.hasUserFact;
        String initReason = shouldInit ? "both_missing" : "user_fact_present";
        logLine("[INIT_CHECK] key=" + KEY_ENABLED_MASTER
                + " ceContains=" + gateRead.ceContains
                + " ceValue=" + (gateRead.ceContains ? gateRead.ceValue : "null")
                + " deContains=" + gateRead.deContains
                + " deValue=" + (gateRead.deContains ? gateRead.deValue : "null")
                + " finalValue=" + gateRead.finalValue
                + " finalSource=" + gateRead.source
                + " hasUserFact=" + gateRead.hasUserFact
                + " shouldInit=" + shouldInit
                + " reason=" + initReason
                + " strategy=user_fact_priority"
                + " ceErr=" + gateRead.ceError
                + " deErr=" + gateRead.deError
                + " thread=" + Thread.currentThread().getName()
                + " source=" + source);
        if (!gateRead.ceContains && !gateRead.deContains && gateRead.hasUserFact && !"default".equals(gateRead.source)) {
            logLine("[INIT_CHECK_NOTE] file_absent_but_user_fact_present key=" + KEY_ENABLED_MASTER + " source=" + gateRead.source);
        }

        boolean initialized = readBoolWithStores(c, KEY_CONFIG_INITIALIZED, false).finalValue;
        String missingKey = firstMissingDefaultKey(c);

        if (!shouldInit && missingKey == null) {
            if (!defaultsInitLogged) {
                defaultsInitLogged = true;
                logLine("[INIT_DEFAULTS] skip reason=already_initialized source=" + source);
            }
            return;
        }

        if (!shouldInit && missingKey != null) {
            String repairReason = !ce.contains(missingKey) && de.contains(missingKey)
                    ? "store_repair_ce_missing"
                    : (ce.contains(missingKey) && !de.contains(missingKey)
                    ? "store_repair_de_missing"
                    : "store_repair_mixed_missing");
            logLine("[INIT_DEFAULTS] run mode=store_repair reason=" + repairReason + " key=" + missingKey + " source=" + source);
            ensureDefaultKeys(c, ce, de, source, initCheckReads);
            if (!initialized) {
                ce.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).commit();
                de.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).commit();
                logLine("[INIT_DEFAULTS] done mark_initialized=true source=" + source + " triggerSource=" + source + " gateKeySummary=ceContains=" + gateRead.ceContains + ",deContains=" + gateRead.deContains + " reason=" + repairReason);
            } else {
                logLine("[INIT_DEFAULTS] skip mark_initialized reason=already_initialized source=" + source);
            }
            defaultsInitLogged = true;
            return;
        }

        if (!shouldInit) {
            if (!defaultsInitLogged) {
                defaultsInitLogged = true;
                logLine("[INIT_DEFAULTS] skip reason=gate_key_exists source=" + source);
            }
            return;
        }

        if (missingKey == null) {
            ce.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).apply();
            de.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).apply();
            logLine("[INIT_DEFAULTS] skip reason=no_missing_keys mark_initialized=true source=" + source);
            defaultsInitLogged = true;
            return;
        }

        logLine("[INIT_DEFAULTS] run mode=first_init reason=missing_key key=" + missingKey + " source=" + source);
        ensureDefaultKeys(c, ce, de, source, initCheckReads);

        ce.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).commit();
        de.edit().putBoolean(KEY_CONFIG_INITIALIZED, true).commit();
        logLine("[INIT_DEFAULTS] done mark_initialized=true source=" + source + " triggerSource=" + source + " gateKeySummary=ceContains=" + gateRead.ceContains + ",deContains=" + gateRead.deContains);
        defaultsInitLogged = true;
        logLine("[PREF_STORE] defaults source=" + source + " master=" + enabledMaster(c)
                + " autostart=" + enabledAutostart(c)
                + " tombstone=" + enabledTombstone(c)
                + " allowAndroidFs=" + allowAndroidFsHook(c)
                + " logOnlyMatched=" + logOnlyMatches(c)
                + " debugForceMaster=" + debugForceMaster(c)
                + " pmRcObs=" + allowPhonemanagerReadConfigObserve(c)
                + " pmRcMod=" + allowPhonemanagerReadConfigModify(c)
                + " pmFs=" + allowPhonemanagerFsHook(c)
                + " scRcObs=" + allowSafecenterReadConfigObserve(c)
                + " scRcMod=" + allowSafecenterReadConfigModify(c)
                + " scFs=" + allowSafecenterFsHook(c));
    }

    private static void ensureDefaultKeys(Context c, SharedPreferences ce, SharedPreferences de,
                                          String source, Map<String, BoolStoreRead> initCheckReads) {
        for (String key : DEFAULT_BOOL_KEYS) {
            BoolStoreRead read = initCheckReads.get(key);
            boolean writeValue = read != null ? read.finalValue : defaultForKey(key);
            ensureDefaultKey(c, ce, de, key, writeValue, source, initCheckReads);
        }
    }

    private static boolean ensureDefaultKey(Context c, SharedPreferences ce, SharedPreferences de,
                                            String key, boolean value, String source,
                                            Map<String, BoolStoreRead> initCheckReads) {
        boolean changed = false;
        BoolStoreRead read = readBoolWithStores(c, key, value);
        maybeLogInitInconsistent(key, "ce", initCheckReads.get(key), read.ceContains, source);
        maybeLogInitInconsistent(key, "de", initCheckReads.get(key), read.deContains, source);
        if (!read.ceContains) {
            ce.edit().putBoolean(key, value).commit();
            changed = true;
            logLine("[INIT_DEFAULTS] write key=" + key + " value=" + value + " store=prefs_ce source=" + source
                    + " existsCheckSource=readBoolWithStores valueBefore=" + (read.ceContains ? read.ceValue : "null"));
            if (KEY_ENABLED_MASTER.equals(key)) {
                logLine("[MASTER_WRITE] source=" + source + " before=<absent-ce> after=" + value + " reason=init_defaults store=prefs_ce");
            }
        } else {
            logLine("[INIT_DEFAULTS] skip key=" + key + " reason=exists store=prefs_ce source=" + source
                    + " existsCheckSource=readBoolWithStores valueBefore=" + read.ceValue);
        }
        if (!read.deContains) {
            de.edit().putBoolean(key, value).commit();
            changed = true;
            logLine("[INIT_DEFAULTS] write key=" + key + " value=" + value + " store=prefs_de source=" + source
                    + " existsCheckSource=readBoolWithStores valueBefore=" + (read.deContains ? read.deValue : "null"));
            if (KEY_ENABLED_MASTER.equals(key)) {
                logLine("[MASTER_WRITE] source=" + source + " before=<absent-de> after=" + value + " reason=init_defaults store=prefs_de");
            }
        } else {
            logLine("[INIT_DEFAULTS] skip key=" + key + " reason=exists store=prefs_de source=" + source
                    + " existsCheckSource=readBoolWithStores valueBefore=" + read.deValue);
        }
        return changed;
    }

    private static String firstMissingDefaultKey(Context c) {
        for (String key : DEFAULT_BOOL_KEYS) {
            BoolStoreRead read = readBoolWithStores(c, key, defaultForKey(key));
            if (!read.ceContains || !read.deContains) return key;
        }
        return null;
    }

    private static void maybeLogInitInconsistent(String key, String store, BoolStoreRead initRead,
                                                 boolean defaultsExists, String triggerSource) {
        if (initRead == null) return;
        boolean initContains = "ce".equals(store) ? initRead.ceContains : initRead.deContains;
        if (initContains != defaultsExists) {
            logLine("[INIT_INCONSISTENT] key=" + key
                    + " store=" + store
                    + " initCheckContains=" + initContains
                    + " defaultsExists=" + defaultsExists
                    + " phase=init_defaults"
                    + " trigger=" + triggerSource);
        }
    }

    private static BoolStoreRead readBoolWithStores(Context c, String key, boolean defaultValue) {
        SharedPreferences ce = sp(c);
        SharedPreferences de = spDe(c);
        return readBoolWithStores(c, ce, de, key, defaultValue);
    }

    private static BoolStoreRead readBoolWithStores(Context c, SharedPreferences ce, SharedPreferences de, String key, boolean defaultValue) {
        boolean ceContains = false;
        boolean deContains = false;
        boolean ceValue = defaultValue;
        boolean deValue = defaultValue;
        boolean runtimeContains = false;
        boolean runtimeValue = defaultValue;
        boolean propContains = false;
        boolean propValue = defaultValue;
        String ceErr = "null";
        String deErr = "null";
        try {
            ceContains = ce.contains(key);
            ceValue = ce.getBoolean(key, defaultValue);
        } catch (Throwable t) {
            ceErr = t.getClass().getSimpleName() + ":" + t.getMessage();
        }
        try {
            deContains = de.contains(key);
            deValue = de.getBoolean(key, defaultValue);
        } catch (Throwable t) {
            deErr = t.getClass().getSimpleName() + ":" + t.getMessage();
        }
        if (c != null) {
            Boolean runtime = readRuntimeFlagValue(c, key);
            if (runtime != null) {
                runtimeContains = true;
                runtimeValue = runtime;
            }
            Boolean prop = readSystemPropertyValueForKey(key);
            if (prop != null) {
                propContains = true;
                propValue = prop;
            }
        }
        boolean finalValue;
        String source;
        if (ceContains) {
            finalValue = ceValue;
            source = "ce";
        } else if (deContains) {
            finalValue = deValue;
            source = "de_fallback";
        } else if (runtimeContains) {
            finalValue = runtimeValue;
            source = "runtime_flags";
        } else if (propContains) {
            finalValue = propValue;
            source = "system_property";
        } else {
            finalValue = defaultValue;
            source = "default";
        }
        boolean hasUserFact = propContains || runtimeContains || ceContains || deContains;
        return new BoolStoreRead(key, ceContains, ceValue, deContains, deValue, finalValue, source, hasUserFact, ceErr, deErr);
    }

    private static Boolean readRuntimeFlagValue(Context c, String key) {
        Properties p = readRuntimeFlagProperties(c);
        if (p == null) return null;
        return parseBoolValue(p.getProperty(key));
    }

    private static Properties readRuntimeFlagProperties(Context c) {
        File[] candidates = new File[] {
                new File(c.createDeviceProtectedStorageContext().getFilesDir(), RUNTIME_FLAGS),
                new File(c.getFilesDir(), RUNTIME_FLAGS),
                new File("/data/local/tmp/oplusconfighook/" + RUNTIME_FLAGS)
        };
        for (File file : candidates) {
            if (file == null || !file.exists() || !file.canRead()) continue;
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties p = new Properties();
                p.load(fis);
                return p;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Boolean readSystemPropertyValueForKey(String key) {
        String propKey = propKeyForConfigKey(key);
        if (propKey == null) return null;
        return parseBoolValue(readSystemProperty(propKey));
    }

    private static String propKeyForConfigKey(String key) {
        if (KEY_ENABLED_MASTER.equals(key)) return PROP_MASTER;
        if (KEY_ENABLED_AUTOSTART.equals(key)) return PROP_AUTOSTART;
        if (KEY_ENABLED_TOMBSTONE.equals(key)) return PROP_TOMBSTONE;
        if (KEY_LOG_ONLY_MATCHED.equals(key)) return PROP_LOG_ONLY;
        if (KEY_ALLOW_ANDROID_FS_HOOK.equals(key)) return PROP_ALLOW_ANDROID_FS;
        if (KEY_DEBUG_FORCE_MASTER.equals(key)) return PROP_DEBUG_FORCE_MASTER;
        if (KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE.equals(key)) return PROP_PM_RC_OBS;
        if (KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY.equals(key)) return PROP_PM_RC_MOD;
        if (KEY_ALLOW_PHONEMANAGER_FS_HOOK.equals(key)) return PROP_PM_FS;
        if (KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE.equals(key)) return PROP_SC_RC_OBS;
        if (KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY.equals(key)) return PROP_SC_RC_MOD;
        if (KEY_ALLOW_SAFECENTER_FS_HOOK.equals(key)) return PROP_SC_FS;
        return null;
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            return (String) clz.getMethod("get", String.class, String.class).invoke(null, key, "");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String getSysProp(String key, String def) {
        String v = readSystemProperty(key);
        if (v == null) return def;
        String t = v.trim();
        return t.isEmpty() ? def : t;
    }

    private static Boolean parseBoolValue(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) return null;
        if ("1".equals(v) || "true".equals(v) || "y".equals(v) || "yes".equals(v) || "on".equals(v)) return true;
        if ("0".equals(v) || "false".equals(v) || "n".equals(v) || "no".equals(v) || "off".equals(v)) return false;
        return null;
    }

    public static void logMasterReadUi(Context c, String source) {
        if (c == null) return;
        boolean ce = sp(c).getBoolean(KEY_ENABLED_MASTER, false);
        boolean de = spDe(c).getBoolean(KEY_ENABLED_MASTER, false);
        logLine("[MASTER_READ_UI] source=" + source + " value=" + ce + " store=CE deValue=" + de);
    }

    public static void logUiBoolDiag(Context c, String source, String key, boolean def) {
        if (c == null) return;
        BoolStoreRead read = readBoolWithStores(c, key, def);
        logLine("[UI_BOOL_DIAG] source=" + source
                + " key=" + read.key
                + " ceContains=" + read.ceContains
                + " ceValue=" + (read.ceContains ? read.ceValue : "null")
                + " deContains=" + read.deContains
                + " deValue=" + (read.deContains ? read.deValue : "null")
                + " finalValue=" + read.finalValue
                + " finalSource=" + read.source
                + " ceErr=" + read.ceError
                + " deErr=" + read.deError);
    }

    public static void logMasterDiag(Context c, String source) {
        if (c == null) return;
        long now = System.currentTimeMillis();
        if (now - lastMasterDiagMs < MASTER_DIAG_MS) return;
        lastMasterDiagMs = now;
        String pkg = c.getPackageName();
        String name = PREF_NAME + ".xml";
        String[] tags = new String[] {"de", "ce", "legacy"};
        String[] paths = new String[] {
                "/data/user_de/0/" + pkg + "/shared_prefs/" + name,
                "/data/user/0/" + pkg + "/shared_prefs/" + name,
                "/data/data/" + pkg + "/shared_prefs/" + name
        };
        for (int i = 0; i < paths.length; i++) {
            File f = new File(paths[i]);
            boolean exists = f.exists() && f.isFile();
            long lastMod = exists ? f.lastModified() : 0L;
            String val = "n/a";
            if ("ce".equals(tags[i])) val = String.valueOf(sp(c).getBoolean(KEY_ENABLED_MASTER, false));
            if ("de".equals(tags[i])) val = String.valueOf(spDe(c).getBoolean(KEY_ENABLED_MASTER, false));
            logLine("[MASTER_DIAG] source=" + source + " store=" + tags[i] + " path=" + paths[i] + " exists=" + exists + " master=" + val + " lastModified=" + lastMod);
        }
    }


    public static void logStartupDiagnostics(Context c, String source) {
        if (c == null || startupDiagLogged) return;
        startupDiagLogged = true;
        try {
            PackageManager pm = c.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(c.getPackageName(), 0);
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pi.getLongVersionCode() : pi.versionCode;
            int uid = pi.applicationInfo != null ? pi.applicationInfo.uid : android.os.Process.myUid();
            int userId = uid / 100000;
            String processName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? Application.getProcessName() : "unknown";
            String apkPath = pi.applicationInfo != null ? pi.applicationInfo.sourceDir : "unknown";
            logLine("[APP_INSTALL] source=" + source
                    + " pkg=" + c.getPackageName()
                    + " ver=" + pi.versionName + "(" + versionCode + ")"
                    + " firstInstall=" + pi.firstInstallTime
                    + " lastUpdate=" + pi.lastUpdateTime
                    + " uid=" + uid
                    + " userId=" + userId
                    + " process=" + processName
                    + " apk=" + apkPath);
        } catch (Throwable t) {
            logLine("[APP_INSTALL] source=" + source + " err=" + t.getClass().getSimpleName() + ":" + t.getMessage());
        }

        try {
            Context de = c.createDeviceProtectedStorageContext();
            String deFiles = de.getFilesDir() != null ? de.getFilesDir().getAbsolutePath() : "null";
            String deDataDir = de.getDataDir() != null ? de.getDataDir().getAbsolutePath() : "null";
            logLine("[APP_ENV] source=" + source
                    + " packageName=" + c.getPackageName()
                    + " dataDir=" + safePath(c.getDataDir())
                    + " deviceProtectedDataDir=" + deDataDir
                    + " filesDir=" + safePath(c.getFilesDir())
                    + " deFilesDir=" + deFiles
                    + " isDeviceProtectedStorage=" + c.isDeviceProtectedStorage());
            logPrefPathsSnapshot(c, source);
        } catch (Throwable t) {
            logLine("[APP_ENV] source=" + source + " err=" + t.getClass().getSimpleName() + ":" + t.getMessage());
        }
    }

    public static void logPrefPathsSnapshot(Context c, String source) {
        if (c == null) return;
        try {
            Context de = c.createDeviceProtectedStorageContext();
            File ceFile = new File(safePath(c.getDataDir()) + "/shared_prefs/" + PREF_NAME + ".xml");
            File deFile = new File(safePath(de.getDataDir()) + "/shared_prefs/" + PREF_NAME + ".xml");
            logLine("[PREF_PATHS] source=" + source + " store=ce " + describeFile(ceFile));
            logLine("[PREF_PATHS] source=" + source + " store=de " + describeFile(deFile));
        } catch (Throwable t) {
            logLine("[PREF_PATHS] source=" + source + " err=" + t.getClass().getSimpleName() + ":" + t.getMessage());
        }
    }

    private static String describeFile(File file) {
        if (file == null) return "path=null exists=false isFile=false canRead=false len=0 mtime=0 parentExists=false parentCanRead=false";
        File parent = file.getParentFile();
        return "path=" + file.getAbsolutePath()
                + " exists=" + file.exists()
                + " isFile=" + file.isFile()
                + " canRead=" + file.canRead()
                + " len=" + (file.exists() ? file.length() : 0L)
                + " mtime=" + (file.exists() ? file.lastModified() : 0L)
                + " parentExists=" + (parent != null && parent.exists())
                + " parentCanRead=" + (parent != null && parent.canRead());
    }

    private static String safePath(File file) {
        return file != null ? file.getAbsolutePath() : "null";
    }

    public static void ensurePrefsReadable(Context c) {
        if (c == null) return;
        String pkg = c.getPackageName();
        String fileName = PREF_NAME + ".xml";
        String[] paths = new String[] {
                "/data/user_de/0/" + pkg + "/shared_prefs/" + fileName,
                "/data/user/0/" + pkg + "/shared_prefs/" + fileName,
                "/data/data/" + pkg + "/shared_prefs/" + fileName
        };
        String[] tags = new String[] {"de", "ce", "legacy"};
        StringBuilder summary = new StringBuilder("[PREF_WRITE] summary ");
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            String result = fixReadableForPath(path);
            summary.append(tags[i]).append("=").append(result).append(" ");
        }
        ensureRuntimeFlagsReadable(c);
        long now = System.currentTimeMillis();
        if (now - lastReadableLogMs > READABLE_LOG_MS) {
            lastReadableLogMs = now;
            AppLogger.i(TAG, summary.toString());
            Log.i(TAG, summary.toString());
        }
    }

    private static String fixReadableForPath(String path) {
        File f = new File(path);
        boolean exists;
        try {
            exists = f.exists() && f.isFile();
        } catch (Throwable t) {
            String res = "exception:" + t.getClass().getSimpleName();
            logWrite(path, "probe", res, false);
            return res;
        }
        if (!exists) {
            logWrite(path, "probe", "not_found", false);
            return "not_found";
        }

        String fileRes;
        try {
            boolean ok = f.setReadable(true, false);
            fileRes = ok ? "setReadable_ok" : "setReadable_fail";
            logWrite(path, "setReadable", fileRes, true);
        } catch (Throwable t) {
            fileRes = "exception:" + t.getClass().getSimpleName();
            logWrite(path, "setReadable", fileRes, true);
        }

        try {
            File parent = f.getParentFile();
            if (parent != null && parent.exists()) {
                boolean dirR = parent.setReadable(true, false);
                boolean dirX = parent.setExecutable(true, false);
                logWrite(parent.getAbsolutePath(), "setDirReadableExecutable", "readable=" + dirR + ",executable=" + dirX, true);
            }
        } catch (Throwable t) {
            logWrite(path, "setDirReadableExecutable", "exception:" + t.getClass().getSimpleName(), true);
        }

        String chmodRes = tryChmod644(path);
        logWrite(path, "chmod", chmodRes, true);
        logWrite(path, "copy", "not_performed", true);
        return fileRes + "+" + chmodRes;
    }

    private static String tryChmod644(String path) {
        java.lang.Process p = null;
        try {
            p = new ProcessBuilder("sh", "-c", "chmod 644 '" + path.replace("'", "'\\''") + "'").start();
            int code = p.waitFor();
            return code == 0 ? "chmod_ok" : "chmod_fail";
        } catch (Throwable t) {
            return "exception:" + t.getClass().getSimpleName();
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void logWrite(String path, String action, String result, boolean exists) {
        String line = "[PREF_WRITE] path=" + path + " exists=" + exists + " action=" + action + " result=" + result;
        logLine(line);
    }

    private static boolean writeRuntimeFlags(Context c) {
        return writeRuntimeFlags(c, null, null);
    }

    private static boolean writeRuntimeFlags(Context c, @Nullable String changedKey, @Nullable Boolean changedValue) {
        if (c == null) return false;
        boolean master = enabledMaster(c);
        boolean autostart = enabledAutostart(c);
        boolean tombstone = enabledTombstone(c);
        boolean logOnly = logOnlyMatches(c);
        boolean logStacktrace;
        String logStackSource;
        if (KEY_LOG_STACKTRACE.equals(changedKey) && changedValue != null) {
            logStacktrace = changedValue;
            logStackSource = "override";
        } else {
            logStacktrace = logStack(c);
            logStackSource = "getter";
        }
        boolean advancedEdit;
        String advancedEditSource;
        if (KEY_TOMBSTONE_ADVANCED_EDIT.equals(changedKey) && changedValue != null) {
            advancedEdit = changedValue;
            advancedEditSource = "override";
        } else {
            advancedEdit = tombstoneAdvancedEdit(c);
            advancedEditSource = "getter";
        }
        logLine("[RUNTIME_FLAGS_BUILD] key=" + KEY_LOG_STACKTRACE + " source=" + logStackSource + " value=" + logStacktrace);
        logLine("[RUNTIME_FLAGS_BUILD] key=" + KEY_TOMBSTONE_ADVANCED_EDIT + " source=" + advancedEditSource + " value=" + advancedEdit);
        boolean allowAndroid = allowAndroidFsHook(c);
        boolean debugForce = debugForceMaster(c);
        boolean pmRcObs = allowPhonemanagerReadConfigObserve(c);
        boolean pmRcMod = allowPhonemanagerReadConfigModify(c);
        boolean pmFs = allowPhonemanagerFsHook(c);
        boolean scRcObs = allowSafecenterReadConfigObserve(c);
        boolean scRcMod = allowSafecenterReadConfigModify(c);
        boolean scFs = allowSafecenterFsHook(c);

        Properties p = new Properties();
        p.setProperty(KEY_ENABLED_MASTER, String.valueOf(master));
        p.setProperty(KEY_ENABLED_AUTOSTART, String.valueOf(autostart));
        p.setProperty(KEY_ENABLED_TOMBSTONE, String.valueOf(tombstone));
        p.setProperty(KEY_LOG_ONLY_MATCHED, String.valueOf(logOnly));
        p.setProperty(KEY_LOG_STACKTRACE, String.valueOf(logStacktrace));
        p.setProperty(KEY_TOMBSTONE_ADVANCED_EDIT, String.valueOf(advancedEdit));
        p.setProperty(KEY_ALLOW_ANDROID_FS_HOOK, String.valueOf(allowAndroid));
        p.setProperty(KEY_DEBUG_FORCE_MASTER, String.valueOf(debugForce));
        p.setProperty(KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, String.valueOf(pmRcObs));
        p.setProperty(KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, String.valueOf(pmRcMod));
        p.setProperty(KEY_ALLOW_PHONEMANAGER_FS_HOOK, String.valueOf(pmFs));
        p.setProperty(KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, String.valueOf(scRcObs));
        p.setProperty(KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, String.valueOf(scRcMod));
        p.setProperty(KEY_ALLOW_SAFECENTER_FS_HOOK, String.valueOf(scFs));

        File[] candidates = new File[] {
                new File(c.createDeviceProtectedStorageContext().getFilesDir(), RUNTIME_FLAGS),
                new File(c.getFilesDir(), RUNTIME_FLAGS),
                new File("/data/local/tmp/oplusconfighook/" + RUNTIME_FLAGS)
        };
        boolean ok = false;
        for (File out : candidates) {
            try {
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out, false)) {
                    p.store(fos, "OplusConfigHook runtime flags");
                }
                boolean readable = out.setReadable(true, false);
                String chmod = tryChmod644(out.getAbsolutePath());
                ok = true;
                logLine("[RUNTIME_FLAGS_WRITE] path=" + out.getAbsolutePath() + " ok=true readable=" + readable + " chmod=" + chmod
                        + " master=" + master + " autostart=" + autostart + " tombstone=" + tombstone
                        + " logStacktrace=" + logStacktrace + " advancedEdit=" + advancedEdit + " debugForce=" + debugForce
                        + " pmRcObs=" + pmRcObs + " pmRcMod=" + pmRcMod + " pmFs=" + pmFs
                        + " scRcObs=" + scRcObs + " scRcMod=" + scRcMod + " scFs=" + scFs);
            } catch (Throwable t) {
                logLine("[RUNTIME_FLAGS_WRITE] path=" + out.getAbsolutePath() + " ok=false err=" + t.getClass().getSimpleName());
            }
        }
        return ok;
    }

    private static void ensureRuntimeFlagsReadable(Context c) {
        File[] candidates = new File[] {
                new File(c.createDeviceProtectedStorageContext().getFilesDir(), RUNTIME_FLAGS),
                new File(c.getFilesDir(), RUNTIME_FLAGS),
                new File("/data/local/tmp/oplusconfighook/" + RUNTIME_FLAGS)
        };
        for (File f : candidates) {
            if (!f.exists()) continue;
            boolean readable = false;
            try {
                readable = f.setReadable(true, false);
            } catch (Throwable ignored) {
            }
            String chmod = tryChmod644(f.getAbsolutePath());
            logLine("[RUNTIME_FLAGS_WRITE] path=" + f.getAbsolutePath() + " action=ensure_readable readable=" + readable + " chmod=" + chmod);
        }
    }

    private static boolean writeSystemProperties(Context c) {
        boolean master = getPrefFirstValue(c, KEY_ENABLED_MASTER, true);
        boolean autostart = getPrefFirstValue(c, KEY_ENABLED_AUTOSTART, true);
        boolean tombstone = getPrefFirstValue(c, KEY_ENABLED_TOMBSTONE, true);
        boolean logOnly = getPrefFirstValue(c, KEY_LOG_ONLY_MATCHED, false);
        boolean allowAndroid = getPrefFirstValue(c, KEY_ALLOW_ANDROID_FS_HOOK, true);
        boolean debugForce = getPrefFirstValue(c, KEY_DEBUG_FORCE_MASTER, false);
        boolean pmRcObs = getPrefFirstValue(c, KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, false);
        boolean pmRcMod = getPrefFirstValue(c, KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, false);
        boolean pmFs = getPrefFirstValue(c, KEY_ALLOW_PHONEMANAGER_FS_HOOK, false);
        boolean scRcObs = getPrefFirstValue(c, KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, false);
        boolean scRcMod = getPrefFirstValue(c, KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, false);
        boolean scFs = getPrefFirstValue(c, KEY_ALLOW_SAFECENTER_FS_HOOK, false);

        int okCount = 0;
        int total = 13;
        if (writeSysProp(PROP_MASTER, bool01(master))) okCount++;
        if (writeSysProp(PROP_AUTOSTART, bool01(autostart))) okCount++;
        if (writeSysProp(PROP_TOMBSTONE, bool01(tombstone))) okCount++;
        if (writeSysProp(PROP_LOG_ONLY, bool01(logOnly))) okCount++;
        if (writeSysProp(PROP_ALLOW_ANDROID_FS, bool01(allowAndroid))) okCount++;
        if (writeSysProp(PROP_DEBUG_FORCE_MASTER, bool01(debugForce))) okCount++;
        if (writeSysProp(PROP_PM_RC_OBS, bool01(pmRcObs))) okCount++;
        if (writeSysProp(PROP_PM_RC_MOD, bool01(pmRcMod))) okCount++;
        if (writeSysProp(PROP_PM_FS, bool01(pmFs))) okCount++;
        if (writeSysProp(PROP_SC_RC_OBS, bool01(scRcObs))) okCount++;
        if (writeSysProp(PROP_SC_RC_MOD, bool01(scRcMod))) okCount++;
        if (writeSysProp(PROP_SC_FS, bool01(scFs))) okCount++;
        logLine("[SYS_PROP_WRITE] summary master=" + bool01(master)
                + " autostart=" + bool01(autostart)
                + " tombstone=" + bool01(tombstone)
                + " logOnly=" + bool01(logOnly)
                + " allowAndroidFs=" + bool01(allowAndroid)
                + " debugForce=" + bool01(debugForce)
                + " pmRcObs=" + bool01(pmRcObs) + " pmRcMod=" + bool01(pmRcMod) + " pmFs=" + bool01(pmFs)
                + " scRcObs=" + bool01(scRcObs) + " scRcMod=" + bool01(scRcMod) + " scFs=" + bool01(scFs)
                + " okCount=" + okCount + "/" + total);
        return okCount == total;
    }

    private static String bool01(boolean b) {
        return b ? "1" : "0";
    }

    private static boolean getPrefFirstValue(Context c, String key, boolean def) {
        if (c == null) return def;
        SharedPreferences ce = sp(c);
        if (ce.contains(key)) return ce.getBoolean(key, def);
        SharedPreferences de = spDe(c);
        if (de.contains(key)) return de.getBoolean(key, def);
        return def;
    }

    private static boolean writeSysProp(String key, String value) {
        String cmd = "setprop " + key + " " + value;
        boolean ok = runShell("su", "-c", cmd);
        String mode = "root";
        if (!ok) {
            ok = runShell("sh", "-c", cmd);
            mode = ok ? "shell" : "root+shell_failed";
        }
        logLine("[SYS_PROP_WRITE] key=" + key + " value=" + value + " result=" + (ok ? "ok" : "fail") + " via=" + mode);
        return ok;
    }

    private static boolean clearSysProp(String key) {
        String cmd = "setprop " + key + " \"\"";
        boolean ok = runShell("su", "-c", cmd);
        logLine("[SYS_PROP_CLEAR] key=" + key + " result=" + (ok ? "ok" : "fail"));
        return ok;
    }

    private static boolean runShell(String... args) {
        java.lang.Process p = null;
        try {
            p = new ProcessBuilder(args).start();
            int code = p.waitFor();
            return code == 0;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean defaultForKey(String key) {
        if (KEY_ENABLED_MASTER.equals(key)) return true;
        if (KEY_ENABLED_AUTOSTART.equals(key)) return true;
        if (KEY_ENABLED_TOMBSTONE.equals(key)) return true;
        if (KEY_ALLOW_ANDROID_FS_HOOK.equals(key)) return true;
        if (KEY_LOG_ONLY_MATCHED.equals(key)) return false;
        if (KEY_LOG_STACKTRACE.equals(key)) return false;
        if (KEY_TOMBSTONE_ADVANCED_EDIT.equals(key)) return false;
        if (KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE.equals(key)) return false;
        if (KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY.equals(key)) return false;
        if (KEY_ALLOW_PHONEMANAGER_FS_HOOK.equals(key)) return false;
        if (KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE.equals(key)) return false;
        if (KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY.equals(key)) return false;
        if (KEY_ALLOW_SAFECENTER_FS_HOOK.equals(key)) return false;
        return false;
    }

    private static void logMasterWrite(Context c, String key, boolean beforeCe, boolean beforeDe, boolean after, String source, String reason) {
        if (!KEY_ENABLED_MASTER.equals(key)) return;
        String pkg = c.getPackageName();
        String cePath = "/data/user/0/" + pkg + "/shared_prefs/" + PREF_NAME + ".xml";
        String dePath = "/data/user_de/0/" + pkg + "/shared_prefs/" + PREF_NAME + ".xml";
        logLine("[MASTER_WRITE] source=" + source + " reason=" + reason
                + " beforeCe=" + beforeCe + " beforeDe=" + beforeDe + " after=" + after
                + " store=CE+DE cePath=" + cePath + " dePath=" + dePath);
    }

    private static void logStore(String action, String target, String key, boolean value) {
        logLine("[PREF_STORE] action=" + action + " target=" + target + " name=" + PREF_NAME + " key=" + key + " value=" + value);
    }

    private static void logLine(String line) {
        AppLogger.i(TAG, line);
        Log.i(TAG, line);
    }
}
