package com.astor.oplusconfighook;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed 入口类，负责系统进程 Hook 与策略注入。
 */
public class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "OplusConfigHook";
    private static final String MODULE_PKG = "com.astor.oplusconfighook";

    private static final String TARGET_TOMBSTONE_ABS = "/data/oplus/os/bpm/sys_elsa_config_list.xml";
    private static final String KEY_AUTOSTART = "startup/autostart_white_list.txt";
    private static final String PKG_ATHENA = "com.oplus.athena";
    private static final String PKG_BATTERY = "com.oplus.battery";
    private static final String PKG_ANDROID = "android";
    private static final String PKG_PHONEMANAGER = "com.oplus.phonemanager";
    private static final String PKG_SAFECENTER = "com.oplus.safecenter";
    private static final String RUNTIME_FLAGS = "runtime_flags.properties";
    private static final String KEY_DEBUG_FORCE_MASTER = "debug_force_master";
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

    private static volatile XSharedPreferences xpref;
    private static volatile long lastPrefReload = 0L;
    private static volatile long lastXPrefEnsureMs = 0L;
    private static final long PREF_RELOAD_MS = 2000L;
    private static final long XPREF_RECHECK_MS = 10000L;
    private static final long AUTOSTART_PREPARE_RETRY_MS = 15000L;
    private static final long TOMBSTONE_PREPARE_RETRY_MS = 15000L;
    private static final long ATHENA_INIT_WINDOW_MS = 15000L;
    private static final long LOG_THROTTLE_MS = 5000L;

    private static final AtomicBoolean FS_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean ATHENA_ON_BIND_HOOKED = new AtomicBoolean(false);
    private static final ThreadLocal<Boolean> IN_HOOK = ThreadLocal.withInitial(() -> false);

    private static final ConcurrentHashMap<String, Long> lastLog = new ConcurrentHashMap<>();
    private static final Set<String> hookedReadConfigClasses = ConcurrentHashMap.newKeySet();
    private static final Map<FileInputStream, TombstoneState> tombstoneStreams = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean cachedMaster = true;
    private static volatile boolean cachedAutostart = true;
    private static volatile boolean cachedTombstone = true;
    private static volatile boolean cachedLogOnlyMatches = false;
    private static volatile boolean cachedAllowAndroidFsHook = true;
    private static volatile boolean cachedFsEnabledForProcess = false;
    private static volatile boolean cachedDebugForceMaster = false;
    private static volatile boolean cachedPhonemanagerReadConfigObserve = false;
    private static volatile boolean cachedPhonemanagerReadConfigModify = false;
    private static volatile boolean cachedPhonemanagerFsHook = false;
    private static volatile boolean cachedSafecenterReadConfigObserve = false;
    private static volatile boolean cachedSafecenterReadConfigModify = false;
    private static volatile boolean cachedSafecenterFsHook = false;
    private static volatile String cachedConfigSource = "default";

    private static volatile String prefSourceType = "fallback";
    private static volatile String prefSourcePath = "";
    private static volatile boolean prefSourceExists = false;
    private static volatile boolean prefSourceReadable = false;
    private static final AtomicBoolean prefSourceLogged = new AtomicBoolean(false);
    private static volatile Method sysPropGetMethod;
    private static volatile long lastAutostartPrepareMs = 0L;
    private static volatile long lastTombstonePrepareMs = 0L;
    private static volatile PolicyPayload sAutostartPayload;
    private static volatile PolicyPayload sTombstonePayload;
    private static volatile long sAthenaInitWindowUntilMs = 0L;
    private static volatile String sProcessPkg = "";

    private static final class HookPolicy {
        final boolean allowReadConfigObserve;
        final boolean allowReadConfigModify;
        final boolean allowFsHook;

        HookPolicy(boolean allowReadConfigObserve, boolean allowReadConfigModify, boolean allowFsHook) {
            this.allowReadConfigObserve = allowReadConfigObserve;
            this.allowReadConfigModify = allowReadConfigModify;
            this.allowFsHook = allowFsHook;
        }
    }

    private static final class TombstoneState {
        final byte[] src;
        int pos = 0;

        TombstoneState(byte[] src) {
            this.src = src;
        }
    }

    private static final class PolicyPayload {
        final byte[] bytes;
        final String source;
        final boolean providerUsed;
        final String path;
        final long mtime;
        final String sha256;
        final String fallbackReason;

        PolicyPayload(byte[] bytes, String source, boolean providerUsed, String path, long mtime, String fallbackReason) {
            this.bytes = bytes == null ? new byte[0] : bytes;
            this.source = source;
            this.providerUsed = providerUsed;
            this.path = path;
            this.mtime = mtime;
            this.fallbackReason = fallbackReason == null ? "" : fallbackReason;
            this.sha256 = sha256Hex(this.bytes);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam != null && "com.astor.oplusconfighook".equals(lpparam.packageName)) return;
        if (lpparam != null) {
            sProcessPkg = lpparam.packageName;
        }
        refreshPrefsCacheIfNeeded();
        if (lpparam != null) {
            HookPolicy policy = policyForPackage(lpparam.packageName);
            cachedFsEnabledForProcess = policy.allowFsHook;
            logOnce("PKG_POLICY|" + lpparam.packageName, TAG + " [PKG_POLICY] pkg=" + lpparam.packageName
                    + " rcObserve=" + policy.allowReadConfigObserve
                    + " rcModify=" + policy.allowReadConfigModify
                    + " fs=" + policy.allowFsHook + " source=" + cachedConfigSource);
            if (PKG_ATHENA.equals(lpparam.packageName)) {
                hookAthenaInitWindow(lpparam);
            }
        }
        hookReadConfig(lpparam, "android.provider.OplusSettings");
        hookReadConfig(lpparam, "com.oplus.settings.OplusSettings");
        hookFileInputStreamHooks();
    }

    private void hookAthenaInitWindow(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !ATHENA_ON_BIND_HOOKED.compareAndSet(false, true)) return;
        try {
            XposedHelpers.findAndHookMethod(
                    "com.oplus.athena.client.action.oplusguardelf.RemoteGuardElfService",
                    lpparam.classLoader,
                    "onBind",
                    android.content.Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            long until = System.currentTimeMillis() + ATHENA_INIT_WINDOW_MS;
                            sAthenaInitWindowUntilMs = until;
                            logOnce("ATHENA_WINDOW|" + PKG_ATHENA,
                                    TAG + " [ATHENA_WINDOW] start until=" + until + " pkg=" + PKG_ATHENA);
                        }
                    }
            );
        } catch (Throwable ignored) {
            ATHENA_ON_BIND_HOOKED.set(false);
        }
    }

    private void hookReadConfig(XC_LoadPackage.LoadPackageParam lpparam, String className) {
        if (!hookedReadConfigClasses.add(className)) return;
        try {
            Class<?> cls = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (cls == null) {
                hookedReadConfigClasses.remove(className);
                return;
            }
            XposedHelpers.findAndHookMethod(cls, "readConfig", Context.class, String.class, int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (IN_HOOK.get()) return;
                    IN_HOOK.set(true);
                    try {
                        refreshPrefsCacheIfNeeded();
                        String raw = (String) param.args[1];
                        if (raw == null) return;
                        String key = normalizeKey(raw);
                        String packageName = lpparam == null ? "" : lpparam.packageName;
                        HookPolicy policy = policyForPackage(packageName);
                        boolean hit = KEY_AUTOSTART.equals(key) || raw.contains(KEY_AUTOSTART);

                        if (isObserveCapablePackage(packageName) && !policy.allowReadConfigObserve) {
                            if (hit) {
                                logOnce("RC_OBSERVE_DISABLED|" + packageName + "|" + key,
                                        TAG + " [RC] SKIP pkg=" + packageName + " key=" + key + " hit=true reason=observe_disabled");
                            }
                            return;
                        }

                        if (!cachedLogOnlyMatches || hit) {
                            logOnce("RC|" + packageName + "|" + key, TAG + " [RC] pkg=" + packageName + " key=" + key + " hit=" + hit
                                    + " master=" + cachedMaster + " autostart=" + cachedAutostart + " observe=" + policy.allowReadConfigObserve
                                    + " modify=" + policy.allowReadConfigModify + " source=" + cachedConfigSource);
                        }
                        if (!hit) {
                            logOnce("RC_SKIP|" + packageName + "|" + key + "|hit_false", TAG + " [RC] SKIP pkg=" + packageName + " key=" + key + " reason=hit_false source=" + cachedConfigSource);
                            return;
                        }
                        if (!cachedMaster) {
                            logOnce("RC_SKIP|" + packageName + "|" + key + "|master_off", TAG + " [RC] SKIP pkg=" + packageName + " key=" + key + " reason=master_off source=" + cachedConfigSource);
                            return;
                        }
                        if (!cachedAutostart) {
                            logOnce("RC_SKIP|" + packageName + "|" + key + "|autostart_off", TAG + " [RC] SKIP pkg=" + packageName + " key=" + key + " reason=autostart_off source=" + cachedConfigSource);
                            return;
                        }
                        if (!policy.allowReadConfigModify) {
                            logOnce("RC_OBSERVE_ONLY|" + packageName + "|" + key,
                                    TAG + " [RC] OBSERVE_ONLY pkg=" + packageName + " key=" + key + " hit=true reason=modify_disabled source=" + cachedConfigSource);
                            return;
                        }

                        maybePrepareAutostartPolicy();
                        PolicyPayload payload = sAutostartPayload;
                        byte[] bytes = payload == null ? null : payload.bytes;
                        if (bytes == null || bytes.length == 0) {
                            logOnce("RC_SKIP|" + packageName + "|" + key + "|bytes_empty", TAG + " [RC] SKIP pkg=" + packageName + " key=" + key + " reason=bytes_empty source=" + (payload == null ? "NONE" : payload.source));
                            return;
                        }
                        param.setResult(new ByteArrayInputStream(bytes));
                        logOnce("RC_REPLACED|" + packageName + "|" + key, TAG + " [RC] REPLACED pkg=" + packageName + " key=" + key + " bytes=" + bytes.length + " source=" + payload.source + " provider=" + payload.providerUsed + " sha256=" + payload.sha256);
                    } catch (Throwable t) {
                        logOnce("RC_SKIP|EX|" + className, TAG + " [RC] SKIP key=startup/autostart_white_list.txt reason=exception:" + t.getClass().getSimpleName());
                    } finally {
                        IN_HOOK.set(false);
                    }
                }
            });
        } catch (Throwable ignored) {
            hookedReadConfigClasses.remove(className);
        }
    }

    private void hookFileInputStreamHooks() {
        if (!FS_HOOKED.compareAndSet(false, true)) return;
        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, File.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (IN_HOOK.get()) return;
                    IN_HOOK.set(true);
                    try {
                        refreshPrefsCacheIfNeeded();
                        File file = (File) param.args[0];
                        String pathPreview = "";
                        if (file != null) {
                            try {
                                pathPreview = file.getAbsolutePath();
                            } catch (Throwable ignored) {
                                pathPreview = "<path_error>";
                            }
                        }
                        String packageName = resolvePackageName();
                        boolean matchedPreview = isTombstoneTargetPath(pathPreview);
                        boolean athenaInitWindow = PKG_ATHENA.equals(packageName) && isAthenaInitWindow();
                        if (!cachedLogOnlyMatches) {
                            if (pathPreview.contains("oplus") || pathPreview.contains("elsa") || pathPreview.contains("xml")) {
                                logOnce("FS_ENTER|" + packageName + "|" + pathPreview,
                                        TAG + " [FS_ENTER] pkg=" + packageName + " ctor=FileInputStream(File) path=" + pathPreview);
                            }
                        } else if (matchedPreview) {
                            logOnce("FS_ENTER|" + packageName + "|" + pathPreview,
                                    TAG + " [FS_ENTER] pkg=" + packageName + " ctor=FileInputStream(File) path=" + pathPreview);
                        }
                        if (!(cachedMaster && cachedTombstone && cachedFsEnabledForProcess)) {
                            if (matchedPreview) {
                                HookPolicy p = policyForPackage(packageName);
                                logOnce("FS_SKIP|" + packageName + "|" + pathPreview,
                                        TAG + " [FS] pkg=" + packageName + " hit path=" + pathPreview + " enabled=false reason=fs_hook_disabled"
                                                + " master=" + cachedMaster + " tombstone=" + cachedTombstone + " policyFs=" + p.allowFsHook);
                            }
                            return;
                        }

                        if (PKG_ATHENA.equals(packageName) && !athenaInitWindow && !pathPreview.contains("sys_elsa_config_list.xml") && !matchedPreview) {
                            return;
                        }

                        if (file == null) return;
                        String path;
                        try {
                            path = file.getAbsolutePath();
                        } catch (Throwable t) {
                            logOnce("FS_ERR|path|" + packageName, TAG + " [FS_ERR] stage=ctor ex=" + t.getClass().getSimpleName() + ":" + t.getMessage());
                            return;
                        }
                        boolean matched = isTombstoneTargetPath(path);
                        if (!cachedLogOnlyMatches) {
                            if (path.contains("sys_elsa") || matched) {
                                logOnce("FS_MATCH|" + packageName + "|" + path + "|" + matched,
                                        TAG + " [FS_MATCH] path=" + path + " matched=" + matched);
                            }
                        } else if (matched) {
                            logOnce("FS_MATCH|" + packageName + "|" + path + "|" + matched,
                                    TAG + " [FS_MATCH] path=" + path + " matched=" + matched);
                        }
                        if (!matched) return;

                        HookPolicy p = policyForPackage(packageName);
                        logOnce("FS_HIT_PRE|" + packageName + "|" + path,
                                TAG + " [FS] pkg=" + packageName + " hit path=" + path
                                        + " master=" + cachedMaster + " tombstone=" + cachedTombstone + " policyFs=" + p.allowFsHook);

                        maybePrepareTombstonePolicy();
                        PolicyPayload payload = sTombstonePayload;
                        byte[] src = payload == null ? null : payload.bytes;
                        if (src == null || src.length == 0) {
                            logOnce("FS_SKIP|" + packageName + "|" + path + "|policy_empty",
                                    TAG + " [FS] pkg=" + packageName + " hit path=" + path + " enabled=true reason=policy_empty");
                            return;
                        }
                        if (param.thisObject instanceof FileInputStream) {
                            tombstoneStreams.put((FileInputStream) param.thisObject, new TombstoneState(src));
                            logOnce("FS_HIT|" + path, TAG + " [FS] pkg=" + packageName + " hit path=" + path + " replacedBytes=" + src.length + " source=" + payload.source + " provider=" + payload.providerUsed + " sha256=" + payload.sha256);
                        }
                    } catch (Throwable t) {
                        String packageName = resolvePackageName();
                        logOnce("FS_ERR|ctor|" + packageName, TAG + " [FS_ERR] stage=ctor ex=" + t.getClass().getSimpleName() + ":" + t.getMessage());
                        if (param.thisObject instanceof FileInputStream) {
                            tombstoneStreams.remove((FileInputStream) param.thisObject);
                        }
                    } finally {
                        IN_HOOK.set(false);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(FileInputStream.class, "read", byte[].class, int.class, int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (IN_HOOK.get()) return;
                    IN_HOOK.set(true);
                    try {
                        handleTombstoneRead(param, (byte[]) param.args[0], (int) param.args[1], (int) param.args[2]);
                    } finally {
                        IN_HOOK.set(false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(FileInputStream.class, "read", byte[].class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (IN_HOOK.get()) return;
                    IN_HOOK.set(true);
                    try {
                        byte[] out = (byte[]) param.args[0];
                        handleTombstoneRead(param, out, 0, out.length);
                    } finally {
                        IN_HOOK.set(false);
                    }
                }
            });
        } catch (Throwable ignored) {
            FS_HOOKED.set(false);
        }
    }

    private static void handleTombstoneRead(XC_MethodHook.MethodHookParam param, byte[] out, int off, int len) {
        if (!(param.thisObject instanceof FileInputStream)) return;
        FileInputStream fis = (FileInputStream) param.thisObject;
        TombstoneState st = tombstoneStreams.get(fis);
        if (st == null) return;

        try {
            if (out == null) return;
            if (off < 0 || len < 0 || off > out.length || off + len > out.length) {
                tombstoneStreams.remove(fis);
                return;
            }
            // 与 InputStream.read 语义保持一致：len=0 时直接返回 0，不触发底层读取。
            if (len == 0) {
                param.setResult(0);
                return;
            }
            if (!(cachedMaster && cachedTombstone && cachedFsEnabledForProcess)) {
                tombstoneStreams.remove(fis);
                return;
            }

            byte[] src = st.src;
            if (src == null) {
                tombstoneStreams.remove(fis);
                return;
            }

            synchronized (st) {
                if (st.pos >= src.length) {
                    tombstoneStreams.remove(fis);
                    param.setResult(-1);
                    return;
                }
                int n = Math.min(len, src.length - st.pos);
                if (n <= 0) {
                    tombstoneStreams.remove(fis);
                    param.setResult(-1);
                    return;
                }
                System.arraycopy(src, st.pos, out, off, n);
                st.pos += n;
                param.setResult(n);
            }
        } catch (Throwable t) {
            logOnce("FS_ERR|read|" + resolvePackageName(),
                    TAG + " [FS_ERR] stage=read ex=" + t.getClass().getSimpleName() + ":" + t.getMessage());
            tombstoneStreams.remove(fis);
        }
    }

    private static void refreshPrefsCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastPrefReload < PREF_RELOAD_MS) return;
        lastPrefReload = now;
        try {
            boolean loaded = loadSystemPropertiesIfPresent();
            if (!loaded) {
                loaded = loadRuntimeFlagsIfPresent();
            }
            if (!loaded) {
                XSharedPreferences pref = ensureXPrefReady();
                if (pref == null) return;
                pref.reload();
                cachedMaster = pref.getBoolean(Prefs.KEY_ENABLED_MASTER, true);
                cachedAutostart = pref.getBoolean(Prefs.KEY_ENABLED_AUTOSTART, true);
                cachedTombstone = pref.getBoolean(Prefs.KEY_ENABLED_TOMBSTONE, true);
                cachedLogOnlyMatches = pref.getBoolean(Prefs.KEY_LOG_ONLY_MATCHED, false);
                cachedAllowAndroidFsHook = pref.getBoolean(Prefs.KEY_ALLOW_ANDROID_FS_HOOK, true);
                cachedDebugForceMaster = pref.getBoolean(KEY_DEBUG_FORCE_MASTER, false);
                cachedPhonemanagerReadConfigObserve = pref.getBoolean(Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, false);
                cachedPhonemanagerReadConfigModify = pref.getBoolean(Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, false);
                cachedPhonemanagerFsHook = pref.getBoolean(Prefs.KEY_ALLOW_PHONEMANAGER_FS_HOOK, false);
                cachedSafecenterReadConfigObserve = pref.getBoolean(Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, false);
                cachedSafecenterReadConfigModify = pref.getBoolean(Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, false);
                cachedSafecenterFsHook = pref.getBoolean(Prefs.KEY_ALLOW_SAFECENTER_FS_HOOK, false);
                cachedConfigSource = prefSourceType;
            }
            if (cachedDebugForceMaster) {
                cachedMaster = true;
                logOnce("PREF_DEBUG_FORCE", TAG + " [PREF] debug force master=true applied source=" + cachedConfigSource);
            }
            if (prefSourceLogged.compareAndSet(false, true)) {
                XposedBridge.log(TAG + " [PREF] source=" + prefSourceType
                        + " path=" + prefSourcePath
                        + " exists=" + prefSourceExists
                        + " readable=" + prefSourceReadable
                        + " master=" + cachedMaster
                        + " autostart=" + cachedAutostart
                        + " tombstone=" + cachedTombstone
                        + " pmRcObs=" + cachedPhonemanagerReadConfigObserve + " pmRcMod=" + cachedPhonemanagerReadConfigModify + " pmFs=" + cachedPhonemanagerFsHook
                        + " scRcObs=" + cachedSafecenterReadConfigObserve + " scRcMod=" + cachedSafecenterReadConfigModify + " scFs=" + cachedSafecenterFsHook);
            }
            logOnce("PREF_RELOAD_SOURCE", TAG + " [PREF] reload source=" + cachedConfigSource + " master=" + cachedMaster
                    + " autostart=" + cachedAutostart + " tombstone=" + cachedTombstone
                    + " pmRcObs=" + cachedPhonemanagerReadConfigObserve + " pmRcMod=" + cachedPhonemanagerReadConfigModify + " pmFs=" + cachedPhonemanagerFsHook
                    + " scRcObs=" + cachedSafecenterReadConfigObserve + " scRcMod=" + cachedSafecenterReadConfigModify + " scFs=" + cachedSafecenterFsHook);
        } catch (Throwable t) {
            logOnce("PREF_RELOAD_ERR", TAG + " [PREF] reload failed: " + t);
        }
    }

    private static boolean loadSystemPropertiesIfPresent() {
        try {
            String rawMaster = getSysProp(PROP_MASTER, "");
            String rawAutostart = getSysProp(PROP_AUTOSTART, "");
            String rawTombstone = getSysProp(PROP_TOMBSTONE, "");
            String rawLogOnly = getSysProp(PROP_LOG_ONLY, "");
            String rawAllowAndroid = getSysProp(PROP_ALLOW_ANDROID_FS, "");
            String rawDebugForce = getSysProp(PROP_DEBUG_FORCE_MASTER, "");
            String rawPmRcObs = getSysProp(PROP_PM_RC_OBS, "");
            String rawPmRcMod = getSysProp(PROP_PM_RC_MOD, "");
            String rawPmFs = getSysProp(PROP_PM_FS, "");
            String rawScRcObs = getSysProp(PROP_SC_RC_OBS, "");
            String rawScRcMod = getSysProp(PROP_SC_RC_MOD, "");
            String rawScFs = getSysProp(PROP_SC_FS, "");
            boolean hasAny = !(isEmpty(rawMaster) && isEmpty(rawAutostart) && isEmpty(rawTombstone)
                    && isEmpty(rawLogOnly) && isEmpty(rawAllowAndroid) && isEmpty(rawDebugForce)
                    && isEmpty(rawPmRcObs) && isEmpty(rawPmRcMod) && isEmpty(rawPmFs)
                    && isEmpty(rawScRcObs) && isEmpty(rawScRcMod) && isEmpty(rawScFs));
            if (!hasAny) {
                logOnce("SYS_PROP_EMPTY", TAG + " [PREF] source=system_property unavailable (all keys empty)");
                return false;
            }
            cachedMaster = parseBool(rawMaster, true);
            cachedAutostart = parseBool(rawAutostart, true);
            cachedTombstone = parseBool(rawTombstone, true);
            cachedLogOnlyMatches = parseBool(rawLogOnly, false);
            cachedAllowAndroidFsHook = parseBool(rawAllowAndroid, true);
            cachedDebugForceMaster = parseBool(rawDebugForce, false);
            cachedPhonemanagerReadConfigObserve = parseBool(rawPmRcObs, false);
            cachedPhonemanagerReadConfigModify = parseBool(rawPmRcMod, false);
            cachedPhonemanagerFsHook = parseBool(rawPmFs, false);
            cachedSafecenterReadConfigObserve = parseBool(rawScRcObs, false);
            cachedSafecenterReadConfigModify = parseBool(rawScRcMod, false);
            cachedSafecenterFsHook = parseBool(rawScFs, false);
            cachedConfigSource = "system_property";
            logOnce("SYS_PROP_USED", TAG + " [PREF] source=system_property master=" + cachedMaster
                    + " autostart=" + cachedAutostart + " tombstone=" + cachedTombstone
                    + " allowAndroidFs=" + cachedAllowAndroidFsHook + " debugForce=" + cachedDebugForceMaster
                    + " pmRcObs=" + cachedPhonemanagerReadConfigObserve + " pmRcMod=" + cachedPhonemanagerReadConfigModify + " pmFs=" + cachedPhonemanagerFsHook
                    + " scRcObs=" + cachedSafecenterReadConfigObserve + " scRcMod=" + cachedSafecenterReadConfigModify + " scFs=" + cachedSafecenterFsHook);
            return true;
        } catch (Throwable t) {
            logOnce("SYS_PROP_ERR", TAG + " [PREF] system_property read failed err=" + t);
            return false;
        }
    }

    private static boolean loadRuntimeFlagsIfPresent() {
        String[] paths = new String[] {
                "/data/local/tmp/oplusconfighook/" + RUNTIME_FLAGS,
                "/data/user_de/0/" + MODULE_PKG + "/files/" + RUNTIME_FLAGS,
                "/data/user/0/" + MODULE_PKG + "/files/" + RUNTIME_FLAGS,
                "/data/data/" + MODULE_PKG + "/files/" + RUNTIME_FLAGS
        };
        for (String path : paths) {
            File f = new File(path);
            boolean exists = false;
            boolean isFile = false;
            boolean canRead = false;
            try {
                exists = f.exists();
                isFile = f.isFile();
                canRead = f.canRead();
            } catch (Throwable ignored) {
            }
            logOnce("RUNTIME_FLAGS_TRY|" + path, TAG + " [PREF] try source=runtime_flags path=" + path + " exists=" + exists + " isFile=" + isFile + " canRead=" + canRead);
            if (!exists || !isFile) continue;
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
                cachedMaster = parseBool(p, Prefs.KEY_ENABLED_MASTER, true);
                cachedAutostart = parseBool(p, Prefs.KEY_ENABLED_AUTOSTART, true);
                cachedTombstone = parseBool(p, Prefs.KEY_ENABLED_TOMBSTONE, true);
                cachedLogOnlyMatches = parseBool(p, Prefs.KEY_LOG_ONLY_MATCHED, false);
                cachedAllowAndroidFsHook = parseBool(p, Prefs.KEY_ALLOW_ANDROID_FS_HOOK, true);
                cachedDebugForceMaster = parseBool(p, KEY_DEBUG_FORCE_MASTER, false);
                cachedPhonemanagerReadConfigObserve = parseBool(p, Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, false);
                cachedPhonemanagerReadConfigModify = parseBool(p, Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, false);
                cachedPhonemanagerFsHook = parseBool(p, Prefs.KEY_ALLOW_PHONEMANAGER_FS_HOOK, false);
                cachedSafecenterReadConfigObserve = parseBool(p, Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, false);
                cachedSafecenterReadConfigModify = parseBool(p, Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, false);
                cachedSafecenterFsHook = parseBool(p, Prefs.KEY_ALLOW_SAFECENTER_FS_HOOK, false);
                cachedConfigSource = "runtime_flags:" + path;
                logOnce("RUNTIME_FLAGS_USED|" + path, TAG + " [PREF] source=runtime_flags path=" + path + " master=" + cachedMaster + " autostart=" + cachedAutostart + " tombstone=" + cachedTombstone + " debugForce=" + cachedDebugForceMaster
                        + " pmRcObs=" + cachedPhonemanagerReadConfigObserve + " pmRcMod=" + cachedPhonemanagerReadConfigModify + " pmFs=" + cachedPhonemanagerFsHook
                        + " scRcObs=" + cachedSafecenterReadConfigObserve + " scRcMod=" + cachedSafecenterReadConfigModify + " scFs=" + cachedSafecenterFsHook);
                return true;
            } catch (Throwable t) {
                logOnce("RUNTIME_FLAGS_ERR|" + path, TAG + " [PREF] runtime_flags read failed path=" + path + " err=" + t);
            }
        }
        return false;
    }

    private static boolean parseBool(Properties p, String key, boolean def) {
        try {
            String v = p.getProperty(key);
            if (v == null) return def;
            return "1".equals(v) || "true".equalsIgnoreCase(v.trim());
        } catch (Throwable ignored) {
            return def;
        }
    }

    private static boolean parseBool(String v, boolean def) {
        if (v == null) return def;
        String s = v.trim();
        if (s.isEmpty()) return def;
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String getSysProp(String key, String def) {
        try {
            Method m = sysPropGetMethod;
            if (m == null) {
                Class<?> sp = Class.forName("android.os.SystemProperties");
                m = sp.getMethod("get", String.class, String.class);
                m.setAccessible(true);
                sysPropGetMethod = m;
            }
            Object out = m.invoke(null, key, def);
            return out instanceof String ? (String) out : def;
        } catch (Throwable ignored) {
            return def;
        }
    }

    private static void maybePrepareAutostartPolicy() {
        long now = System.currentTimeMillis();
        if (sAutostartPayload != null && now - lastAutostartPrepareMs < AUTOSTART_PREPARE_RETRY_MS) return;
        lastAutostartPrepareMs = now;
        try {
            Context moduleCtx = resolveModuleContext();
            if (moduleCtx == null) {
                logOnce("POLICY_PREPARE_SKIP", TAG + " [POLICY] autostart prepare skipped: currentApplication null");
                return;
            }
            sAutostartPayload = loadViaProviderOrFallback(moduleCtx, "autostart_white_list", "autostart_white_list.txt", "autostart");
        } catch (Throwable t) {
            logOnce("POLICY_PREPARE_ERR", TAG + " [POLICY] autostart prepare failed err=" + t.getClass().getSimpleName());
        }
    }

    private static void maybePrepareTombstonePolicy() {
        long now = System.currentTimeMillis();
        if (sTombstonePayload != null && now - lastTombstonePrepareMs < TOMBSTONE_PREPARE_RETRY_MS) return;
        lastTombstonePrepareMs = now;
        try {
            Context moduleCtx = resolveModuleContext();
            if (moduleCtx == null) {
                logOnce("POLICY_TOMBSTONE_PREPARE_SKIP", TAG + " [POLICY] tombstone prepare skipped: currentApplication null");
                return;
            }
            sTombstonePayload = loadViaProviderOrFallback(moduleCtx, "tombstone", "sys_elsa_config_list.xml", "tombstone");
        } catch (Throwable t) {
            logOnce("POLICY_TOMBSTONE_PREPARE_ERR", TAG + " [POLICY] tombstone prepare failed err=" + t.getClass().getSimpleName());
        }
    }

    private static Context resolveModuleContext() {
        try {
            Class<?> helper = XposedHelpers.findClass("android.app.AndroidAppHelper", null);
            Object app = XposedHelpers.callStaticMethod(helper, "currentApplication");
            if (!(app instanceof android.app.Application)) return null;
            Context host = ((android.app.Application) app).getApplicationContext();
            try {
                return host.createPackageContext(MODULE_PKG, Context.CONTEXT_IGNORE_SECURITY);
            } catch (Throwable ignored) {
                return host;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PolicyPayload loadViaProviderOrFallback(Context moduleCtx, String kind, String assetName, String logLabel) {
        String reason = "";
        try (InputStream in = moduleCtx.getContentResolver().openInputStream(Uri.parse("content://" + MODULE_PKG + ".policy/" + kind))) {
            if (in != null) {
                byte[] data = readAll(in);
                String src = detectSource(moduleCtx, assetName, data);
                String path = "";
                long mtime = -1L;
                if ("USER".equals(src)) {
                    File f = new File(PolicyStore.userDir(moduleCtx), assetName);
                    path = f.getAbsolutePath();
                    mtime = f.exists() ? f.lastModified() : -1L;
                }
                PolicyPayload p = new PolicyPayload(data, src, true, path, mtime, "");
                XposedBridge.log(TAG + " [POLICY] " + logLabel + " source=" + p.source + " provider=true len=" + p.bytes.length + " sha256=" + p.sha256 + (p.path.isEmpty() ? "" : " path=" + p.path + " mtime=" + p.mtime));
                return p;
            }
            reason = "NullInputStream";
        } catch (Throwable t) {
            reason = t.getClass().getSimpleName() + ":" + t.getMessage();
        }
        byte[] fallback = readAsset(moduleCtx, assetName);
        PolicyPayload p = new PolicyPayload(fallback, "ASSET", false, "", -1L, reason);
        XposedBridge.log(TAG + " [POLICY] " + logLabel + " provider=false fallback=ASSET reason=" + reason + " len=" + p.bytes.length + " sha256=" + p.sha256);
        return p;
    }

    private static String detectSource(Context c, String assetName, byte[] providerBytes) {
        byte[] asset = readAsset(c, assetName);
        if (providerBytes == null || providerBytes.length == 0) return "ASSET";
        if (asset != null && java.util.Arrays.equals(asset, providerBytes)) return "ASSET";
        return "USER";
    }

    private static byte[] readAsset(Context c, String assetName) {
        try (InputStream in = c.getAssets().open(assetName)) {
            return readAll(in);
        } catch (Throwable ignored) {
            return new byte[0];
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
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

    private static XSharedPreferences ensureXPrefReady() {
        long now = System.currentTimeMillis();
        boolean shouldCheck = xpref == null || now - lastXPrefEnsureMs >= XPREF_RECHECK_MS || "PACKAGE".equals(prefSourceType);
        if (!shouldCheck) return xpref;
        lastXPrefEnsureMs = now;
        try {
            XSharedPreferences candidate = createXPref();
            if (candidate != null) xpref = candidate;
        } catch (Throwable t) {
            logOnce("PREF_ENSURE_ERR", TAG + " [PREF] ensure failed: " + t);
        }
        return xpref;
    }

    private static XSharedPreferences createXPref() {
        String[] paths = new String[] {
                "/data/user_de/0/" + MODULE_PKG + "/shared_prefs/" + Prefs.PREF_NAME + ".xml",
                "/data/user/0/" + MODULE_PKG + "/shared_prefs/" + Prefs.PREF_NAME + ".xml",
                "/data/data/" + MODULE_PKG + "/shared_prefs/" + Prefs.PREF_NAME + ".xml"
        };
        String[] tags = new String[] {"DE", "CE", "LEGACY"};
        for (int i = 0; i < paths.length; i++) {
            File file = new File(paths[i]);
            boolean exists = false;
            boolean isFile = false;
            boolean readable = false;
            try {
                exists = file.exists();
                isFile = file.isFile();
                readable = file.canRead();
            } catch (Throwable ignored) {
            }
            logOnce("PREF_TRY|" + tags[i] + "|" + exists + "|" + isFile,
                    TAG + " [PREF] try source=" + tags[i].toLowerCase()
                            + " path=" + file.getAbsolutePath()
                            + " exists=" + exists
                            + " isFile=" + isFile
                            + " canRead=" + readable);
            if (!exists || !isFile) continue;
            XSharedPreferences pref = createXPrefByFile(file);
            if (pref != null) {
                prefSourceType = tags[i];
                prefSourcePath = file.getAbsolutePath();
                prefSourceExists = exists;
                prefSourceReadable = readable;
                XposedBridge.log(TAG + " [PREF] using FILE source=" + tags[i].toLowerCase() + " path=" + prefSourcePath + " exists=" + exists + " isFile=" + isFile + " canRead=" + readable);
                return pref;
            }
            logOnce("PREF_TRY_FAIL|" + tags[i], TAG + " [PREF] file source construct failed source=" + tags[i].toLowerCase() + " path=" + file.getAbsolutePath());
        }
        try {
            XSharedPreferences fallback = new XSharedPreferences(MODULE_PKG, Prefs.PREF_NAME);
            prefSourceType = "PACKAGE";
            prefSourcePath = MODULE_PKG + "/" + Prefs.PREF_NAME;
            prefSourceExists = false;
            prefSourceReadable = false;
            XposedBridge.log(TAG + " [PREF] using PACKAGE fallback=" + prefSourcePath);
            return fallback;
        } catch (Throwable t) {
            prefSourceType = "FAILED_FALLBACK";
            prefSourcePath = "";
            prefSourceExists = false;
            prefSourceReadable = false;
            XposedBridge.log(TAG + " [PREF] fallback create failed: " + t);
            return new XSharedPreferences(MODULE_PKG, Prefs.PREF_NAME);
        }
    }

    private static XSharedPreferences createXPrefByFile(File file) {
        try {
            Constructor<XSharedPreferences> c = XSharedPreferences.class.getConstructor(File.class);
            c.setAccessible(true);
            return c.newInstance(file);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean shouldEnableFsHookForPackage(String packageName) {
        return policyForPackage(packageName).allowFsHook;
    }

    private static boolean isObserveCapablePackage(String packageName) {
        return PKG_PHONEMANAGER.equals(packageName) || PKG_SAFECENTER.equals(packageName);
    }

    private static HookPolicy policyForPackage(String packageName) {
        if (PKG_ATHENA.equals(packageName) || PKG_BATTERY.equals(packageName)) {
            return new HookPolicy(true, true, true);
        }
        if (PKG_ANDROID.equals(packageName)) {
            return new HookPolicy(true, true, cachedAllowAndroidFsHook);
        }
        if (PKG_PHONEMANAGER.equals(packageName)) {
            return new HookPolicy(cachedPhonemanagerReadConfigObserve,
                    cachedPhonemanagerReadConfigObserve && cachedPhonemanagerReadConfigModify,
                    cachedPhonemanagerFsHook);
        }
        if (PKG_SAFECENTER.equals(packageName)) {
            return new HookPolicy(cachedSafecenterReadConfigObserve,
                    cachedSafecenterReadConfigObserve && cachedSafecenterReadConfigModify,
                    cachedSafecenterFsHook);
        }
        return new HookPolicy(false, false, false);
    }

    private static boolean isTombstoneTargetPath(String path) {
        return path != null && (TARGET_TOMBSTONE_ABS.equals(path) || path.contains("sys_elsa_config_list.xml"));
    }

    private static boolean isAthenaInitWindow() {
        return System.currentTimeMillis() <= sAthenaInitWindowUntilMs;
    }

    private static String resolvePackageName() {
        String packageName = sProcessPkg;
        if (packageName != null && !packageName.isEmpty()) return packageName;
        return android.app.AndroidAppHelper.currentPackageName();
    }

    private static void logOnce(String key, String line) {
        long now = System.currentTimeMillis();
        if (lastLog.size() > 2000) lastLog.clear();
        Long last = lastLog.get(key);
        if (last != null && now - last < LOG_THROTTLE_MS) return;
        lastLog.put(key, now);
        XposedBridge.log(line);
    }

    private static String normalizeKey(String raw) {
        String s = raw.trim().replace('\\', '/');
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        while (s.contains("//")) s = s.replace("//", "/");
        String[] parts = s.split("/");
        if (parts.length >= 2) return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        return s;
    }
}
