package com.astor.oplusconfighook;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 墓碑页状态管理 ViewModel，负责数据加载与界面状态同步。
 */
public class TombstoneViewModel extends ViewModel {
    private boolean initialized;

    public byte[] baseXmlBytes;
    public final Map<String, Set<String>> stagedBlacklists = new LinkedHashMap<>();
    public Set<String> stagedAllow = new LinkedHashSet<>();
    public Set<String> stagedDoNot = new LinkedHashSet<>();
    public final Map<String, TombstoneXmlEditor.PolicyItem> sourcePolicies = new LinkedHashMap<>();
    public final Map<String, TombstoneXmlEditor.PolicyItem> policyDrafts = new LinkedHashMap<>();
    public final Map<String, Set<String>> baselineBlacklists = new LinkedHashMap<>();
    public Set<String> baselineAllow = new LinkedHashSet<>();
    public Set<String> baselineDoNot = new LinkedHashSet<>();
    public final Map<String, TombstoneXmlEditor.PolicyItem> baselinePolicyDrafts = new LinkedHashMap<>();
    public final Map<String, String> baselineTimingValues = new LinkedHashMap<>();
    public final Map<String, String> stagedTimingValues = new LinkedHashMap<>();
    public final Map<String, TombstoneXmlEditor.TimingItem> timingItemsByKey = new LinkedHashMap<>();
    public Set<String> stagedImPkg = new LinkedHashSet<>();
    public Set<String> stagedCpuCtlWhiteList = new LinkedHashSet<>();
    public Set<String> stagedAppCardIgnore = new LinkedHashSet<>();
    public final Map<String, String> stagedEnableConfigAttrs = new LinkedHashMap<>();
    public Set<String> stagedGms = new LinkedHashSet<>();
    public boolean stagedBlackHansEnabled;
    public List<String> stagedBlackHansTokens = new ArrayList<>();
    public Set<String> stagedFfSkipFastFreeze = new LinkedHashSet<>();
    public final Map<String, Set<String>> stagedWhitePkgByCategory = new LinkedHashMap<>();
    public final TombstoneXmlEditor.ProxyGlobalConfig stagedProxyGlobalConfig = new TombstoneXmlEditor.ProxyGlobalConfig();
    public List<String> stagedDefaultProxyBcActions = new ArrayList<>();
    public List<TombstoneXmlEditor.ProxyBcExcludeRule> stagedProxyBcExcludeRules = new ArrayList<>();
    public final TombstoneXmlEditor.ProxyWakeLockConfig stagedProxyWakeLockConfig = new TombstoneXmlEditor.ProxyWakeLockConfig();
    public final TombstoneXmlEditor.ProxyGpsConfig stagedProxyGpsConfig = new TombstoneXmlEditor.ProxyGpsConfig();
    public final TombstoneXmlEditor.ProxyBtScanConfig stagedProxyBtScanConfig = new TombstoneXmlEditor.ProxyBtScanConfig();
    public final TombstoneXmlEditor.ProxyCpnExtensionConfig stagedProxyCpnExtensionConfig = new TombstoneXmlEditor.ProxyCpnExtensionConfig();
    public final TombstoneXmlEditor.ProxySensorConfig stagedProxySensorConfig = new TombstoneXmlEditor.ProxySensorConfig();
    public final TombstoneXmlEditor.ProxyBinderConfig stagedProxyBinderConfig = new TombstoneXmlEditor.ProxyBinderConfig();
    public Set<String> stagedServiceBumpReasons = new LinkedHashSet<>();
    public List<TombstoneXmlEditor.SceneRule> stagedPreventRules = new ArrayList<>();
    public List<TombstoneXmlEditor.SceneRule> stagedAllowRules = new ArrayList<>();
    public final TombstoneXmlEditor.CpuCtlRusConfig stagedCpuCtlRus = new TombstoneXmlEditor.CpuCtlRusConfig();
    public final List<TombstoneXmlEditor.HighLoadingConfig> stagedHighLoadingConfigs = new ArrayList<>();
    public final TombstoneXmlEditor.StrictModeSwitches stagedStrictModeSwitches = new TombstoneXmlEditor.StrictModeSwitches();
    public final TombstoneXmlEditor.AndroidFreezeConfig stagedAndroidFreezeConfig = new TombstoneXmlEditor.AndroidFreezeConfig();
    public final TombstoneXmlEditor.AppQuotaConfig stagedAppQuotaConfig = new TombstoneXmlEditor.AppQuotaConfig();
    public final Map<String, List<String>> stagedPkgConfigTokens = new LinkedHashMap<>();
    public final List<TombstoneXmlEditor.BinderRuleItem> stagedBinderRules = new java.util.ArrayList<>();
    public Set<String> baselineImPkg = new LinkedHashSet<>();
    public Set<String> baselineCpuCtlWhiteList = new LinkedHashSet<>();
    public Set<String> baselineAppCardIgnore = new LinkedHashSet<>();
    public final Map<String, String> baselineEnableConfigAttrs = new LinkedHashMap<>();
    public Set<String> baselineGms = new LinkedHashSet<>();
    public boolean baselineBlackHansEnabled;
    public List<String> baselineBlackHansTokens = new ArrayList<>();
    public Set<String> baselineFfSkipFastFreeze = new LinkedHashSet<>();
    public final Map<String, Set<String>> baselineWhitePkgByCategory = new LinkedHashMap<>();
    public final TombstoneXmlEditor.ProxyGlobalConfig baselineProxyGlobalConfig = new TombstoneXmlEditor.ProxyGlobalConfig();
    public List<String> baselineDefaultProxyBcActions = new ArrayList<>();
    public List<TombstoneXmlEditor.ProxyBcExcludeRule> baselineProxyBcExcludeRules = new ArrayList<>();
    public final TombstoneXmlEditor.ProxyWakeLockConfig baselineProxyWakeLockConfig = new TombstoneXmlEditor.ProxyWakeLockConfig();
    public final TombstoneXmlEditor.ProxyGpsConfig baselineProxyGpsConfig = new TombstoneXmlEditor.ProxyGpsConfig();
    public final TombstoneXmlEditor.ProxyBtScanConfig baselineProxyBtScanConfig = new TombstoneXmlEditor.ProxyBtScanConfig();
    public final TombstoneXmlEditor.ProxyCpnExtensionConfig baselineProxyCpnExtensionConfig = new TombstoneXmlEditor.ProxyCpnExtensionConfig();
    public final TombstoneXmlEditor.ProxySensorConfig baselineProxySensorConfig = new TombstoneXmlEditor.ProxySensorConfig();
    public final TombstoneXmlEditor.ProxyBinderConfig baselineProxyBinderConfig = new TombstoneXmlEditor.ProxyBinderConfig();
    public Set<String> baselineServiceBumpReasons = new LinkedHashSet<>();
    public List<TombstoneXmlEditor.SceneRule> baselinePreventRules = new ArrayList<>();
    public List<TombstoneXmlEditor.SceneRule> baselineAllowRules = new ArrayList<>();
    public final TombstoneXmlEditor.CpuCtlRusConfig baselineCpuCtlRus = new TombstoneXmlEditor.CpuCtlRusConfig();
    public final List<TombstoneXmlEditor.HighLoadingConfig> baselineHighLoadingConfigs = new ArrayList<>();
    public final TombstoneXmlEditor.StrictModeSwitches baselineStrictModeSwitches = new TombstoneXmlEditor.StrictModeSwitches();
    public final TombstoneXmlEditor.AndroidFreezeConfig baselineAndroidFreezeConfig = new TombstoneXmlEditor.AndroidFreezeConfig();
    public final TombstoneXmlEditor.AppQuotaConfig baselineAppQuotaConfig = new TombstoneXmlEditor.AppQuotaConfig();
    public final Map<String, List<String>> baselinePkgConfigTokens = new LinkedHashMap<>();
    public final List<TombstoneXmlEditor.BinderRuleItem> baselineBinderRules = new java.util.ArrayList<>();
    public boolean hasFfPkgBlack;
    public boolean hasWhitePkg;
    public boolean hasEnableConfig;
    public boolean hasGms;
    public boolean hasBlackHansPkg;
    public String inspectorQuery = "";
    public int tombstoneTabIndex = 0;
    public boolean uiExpandPkgConfig;
    public boolean uiExpandBinder;
    public int recommendedScrollY;
    public int advancedScrollY;

    public boolean initialized() {
        return initialized;
    }

    public void markInitialized() {
        initialized = true;
    }

    public boolean isDirty() {
        return !stagedBlacklists.equals(baselineBlacklists)
                || !stagedAllow.equals(baselineAllow)
                || !stagedDoNot.equals(baselineDoNot)
                || !policyDrafts.equals(baselinePolicyDrafts)
                || !stagedTimingValues.equals(baselineTimingValues)
                || !stagedImPkg.equals(baselineImPkg)
                || !stagedCpuCtlWhiteList.equals(baselineCpuCtlWhiteList)
                || !stagedAppCardIgnore.equals(baselineAppCardIgnore)
                || !stagedEnableConfigAttrs.equals(baselineEnableConfigAttrs)
                || !stagedGms.equals(baselineGms)
                || stagedBlackHansEnabled != baselineBlackHansEnabled
                || !stagedBlackHansTokens.equals(baselineBlackHansTokens)
                || !stagedFfSkipFastFreeze.equals(baselineFfSkipFastFreeze)
                || !stagedWhitePkgByCategory.equals(baselineWhitePkgByCategory)
                || !stagedProxyGlobalConfig.equals(baselineProxyGlobalConfig)
                || !stagedDefaultProxyBcActions.equals(baselineDefaultProxyBcActions)
                || !stagedProxyBcExcludeRules.equals(baselineProxyBcExcludeRules)
                || !stagedProxyWakeLockConfig.equals(baselineProxyWakeLockConfig)
                || !stagedProxyGpsConfig.items.equals(baselineProxyGpsConfig.items)
                || stagedProxyGpsConfig.enable != baselineProxyGpsConfig.enable
                || stagedProxyBtScanConfig.enable != baselineProxyBtScanConfig.enable
                || stagedProxyCpnExtensionConfig.enable != baselineProxyCpnExtensionConfig.enable
                || !stagedProxyCpnExtensionConfig.cpnMask.equals(baselineProxyCpnExtensionConfig.cpnMask)
                || !stagedProxySensorConfig.proxyTypeNames.equals(baselineProxySensorConfig.proxyTypeNames)
                || !stagedProxySensorConfig.whiteTypePkgs.equals(baselineProxySensorConfig.whiteTypePkgs)
                || stagedProxySensorConfig.enable != baselineProxySensorConfig.enable
                || stagedProxySensorConfig.proxyType != baselineProxySensorConfig.proxyType
                || !stagedProxySensorConfig.proxyPkgName.equals(baselineProxySensorConfig.proxyPkgName)
                || !stagedProxyBinderConfig.descConfigs.equals(baselineProxyBinderConfig.descConfigs)
                || stagedProxyBinderConfig.enable != baselineProxyBinderConfig.enable
                || !stagedServiceBumpReasons.equals(baselineServiceBumpReasons)
                || !stagedPreventRules.equals(baselinePreventRules)
                || !stagedAllowRules.equals(baselineAllowRules)
                || !stagedCpuCtlRus.equals(baselineCpuCtlRus)
                || !stagedHighLoadingConfigs.equals(baselineHighLoadingConfigs)
                || !stagedStrictModeSwitches.equals(baselineStrictModeSwitches)
                || !stagedAndroidFreezeConfig.equals(baselineAndroidFreezeConfig)
                || !stagedAppQuotaConfig.equals(baselineAppQuotaConfig)
                || !stagedPkgConfigTokens.equals(baselinePkgConfigTokens)
                || !stagedBinderRules.equals(baselineBinderRules);
    }
}
