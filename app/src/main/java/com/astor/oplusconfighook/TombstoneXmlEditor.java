package com.astor.oplusconfighook;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * 墓碑策略 XML 解析与写回工具，负责节点读取、修改与序列化。
 */
public final class TombstoneXmlEditor {
    private static final String TAG = "TombstoneXmlEditor";
    public static final String GROUP_POLICY = "SysBlackPolicy";
    public static final String GROUP_RESTRICT = "SysBlackRestrictMask";
    public static final List<String> BLACKLIST_SCENES = Arrays.asList("lcdon", "lcdoff", "night");

    public static final class PolicyItem {
        public final String groupName;
        public final String name;
        public final String keyScene;
        public final String scene;
        public final String mask;
        public final String quota;
        public final String bgMode;
        public final String fgsExempt;
        public final boolean hasScene;
        public final boolean hasMask;
        public final boolean hasQuota;
        public final boolean hasBgMode;
        public final boolean hasFgsExempt;

        PolicyItem(String groupName, String name, String keyScene, String scene, String mask, String quota, String bgMode, String fgsExempt,
                   boolean hasScene, boolean hasMask, boolean hasQuota, boolean hasBgMode, boolean hasFgsExempt) {
            this.groupName = groupName;
            this.name = name;
            this.keyScene = keyScene == null ? "" : keyScene;
            this.scene = scene;
            this.mask = mask;
            this.quota = quota;
            this.bgMode = bgMode;
            this.fgsExempt = fgsExempt;
            this.hasScene = hasScene;
            this.hasMask = hasMask;
            this.hasQuota = hasQuota;
            this.hasBgMode = hasBgMode;
            this.hasFgsExempt = hasFgsExempt;
        }

        String id() {
            return policyKey(groupName, name, keyScene);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PolicyItem)) return false;
            PolicyItem that = (PolicyItem) o;
            return hasScene == that.hasScene
                    && hasMask == that.hasMask
                    && hasQuota == that.hasQuota
                    && hasBgMode == that.hasBgMode
                    && hasFgsExempt == that.hasFgsExempt
                    && java.util.Objects.equals(groupName, that.groupName)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(keyScene, that.keyScene)
                    && java.util.Objects.equals(scene, that.scene)
                    && java.util.Objects.equals(mask, that.mask)
                    && java.util.Objects.equals(quota, that.quota)
                    && java.util.Objects.equals(bgMode, that.bgMode)
                    && java.util.Objects.equals(fgsExempt, that.fgsExempt);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(groupName, name, keyScene, scene, mask, quota, bgMode, fgsExempt,
                    hasScene, hasMask, hasQuota, hasBgMode, hasFgsExempt);
        }
    }

    public static final class TimingItem {
        public final String key;
        public final String nodeTag;
        public final int nodeIndex;
        public final String nodeQualifier;
        public final String modeTag;
        public final String modeLevel;
        public final String attrName;
        public final String value;

        TimingItem(String key, String nodeTag, int nodeIndex, String nodeQualifier, String modeTag, String modeLevel, String attrName, String value) {
            this.key = key;
            this.nodeTag = nodeTag;
            this.nodeIndex = nodeIndex;
            this.nodeQualifier = nodeQualifier;
            this.modeTag = modeTag;
            this.modeLevel = modeLevel;
            this.attrName = attrName;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TimingItem)) return false;
            TimingItem that = (TimingItem) o;
            return nodeIndex == that.nodeIndex
                    && java.util.Objects.equals(key, that.key)
                    && java.util.Objects.equals(nodeTag, that.nodeTag)
                    && java.util.Objects.equals(nodeQualifier, that.nodeQualifier)
                    && java.util.Objects.equals(modeTag, that.modeTag)
                    && java.util.Objects.equals(modeLevel, that.modeLevel)
                    && java.util.Objects.equals(attrName, that.attrName)
                    && java.util.Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(key, nodeTag, nodeIndex, nodeQualifier, modeTag, modeLevel, attrName, value);
        }
    }

    public static final class ProxyGlobalConfig {
        public boolean hasAny;
        public boolean alarm;
        public boolean service;
        public boolean job;
        public boolean broadcast;
        public String proxyBCmax = "";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProxyGlobalConfig)) return false;
            ProxyGlobalConfig that = (ProxyGlobalConfig) o;
            return hasAny == that.hasAny
                    && alarm == that.alarm
                    && service == that.service
                    && job == that.job
                    && broadcast == that.broadcast
                    && java.util.Objects.equals(proxyBCmax, that.proxyBCmax);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(hasAny, alarm, service, job, broadcast, proxyBCmax);
        }
    }

    public static final class ProxyBcExcludeRule {
        public String action = "";
        public List<String> appTypeTokens = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProxyBcExcludeRule)) return false;
            ProxyBcExcludeRule that = (ProxyBcExcludeRule) o;
            return java.util.Objects.equals(action, that.action)
                    && java.util.Objects.equals(appTypeTokens, that.appTypeTokens);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(action, appTypeTokens);
        }
    }

    public static final class ProxyWakeLockConfig {
        public String defaultWorkSourceType = "";
        public String supportWorkSourceTags = "";
        public String unSupportedWlTypes = "";
        public String proxyButDropTypes = "";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProxyWakeLockConfig)) return false;
            ProxyWakeLockConfig that = (ProxyWakeLockConfig) o;
            return java.util.Objects.equals(defaultWorkSourceType, that.defaultWorkSourceType)
                    && java.util.Objects.equals(supportWorkSourceTags, that.supportWorkSourceTags)
                    && java.util.Objects.equals(unSupportedWlTypes, that.unSupportedWlTypes)
                    && java.util.Objects.equals(proxyButDropTypes, that.proxyButDropTypes);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(defaultWorkSourceType, supportWorkSourceTags, unSupportedWlTypes, proxyButDropTypes);
        }
    }

    public static final class ProxyGpsItem {
        public String type = "";
        public String appType = "";
        public List<String> pkgTokens = new ArrayList<>();

        public String key() { return type + "|" + appType; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProxyGpsItem)) return false;
            ProxyGpsItem that = (ProxyGpsItem) o;
            return java.util.Objects.equals(type, that.type)
                    && java.util.Objects.equals(appType, that.appType)
                    && java.util.Objects.equals(pkgTokens, that.pkgTokens);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(type, appType, pkgTokens);
        }
    }

    public static final class ProxyGpsConfig {
        public boolean hasEnable;
        public boolean enable;
        public final List<ProxyGpsItem> items = new ArrayList<>();
    }

    public static final class ProxyBtScanConfig {
        public boolean hasEnable;
        public boolean enable;
    }

    public static final class ProxyCpnExtensionConfig {
        public boolean hasEnable;
        public boolean enable;
        public String cpnMask = "";
    }

    public static final class WhiteTypePkgRule {
        public String type = "";
        public List<String> pkgTokens = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WhiteTypePkgRule)) return false;
            WhiteTypePkgRule that = (WhiteTypePkgRule) o;
            return java.util.Objects.equals(type, that.type) && java.util.Objects.equals(pkgTokens, that.pkgTokens);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(type, pkgTokens);
        }
    }

    public static final class ProxySensorConfig {
        public boolean hasEnable;
        public boolean enable;
        public boolean hasProxyType;
        public boolean proxyType;
        public String proxyPkgName = "";
        public final List<String> proxyTypeNames = new ArrayList<>();
        public final List<WhiteTypePkgRule> whiteTypePkgs = new ArrayList<>();
    }

    public static final class BinderDescRule {
        public String desc = "";
        public List<String> pkgTokens = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BinderDescRule)) return false;
            BinderDescRule that = (BinderDescRule) o;
            return java.util.Objects.equals(desc, that.desc) && java.util.Objects.equals(pkgTokens, that.pkgTokens);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(desc, pkgTokens);
        }
    }

    public static final class ProxyBinderConfig {
        public boolean hasEnable;
        public boolean enable;
        public final List<BinderDescRule> descConfigs = new ArrayList<>();
    }

    public static final class SceneRule {
        public final String tag;
        public final int index;
        public final String scene;
        public final String pkg;
        public final String mask;
        public final String version;
        public final String code;
        public final String parentPath;

        public SceneRule(String tag, int index, String scene, String pkg, String mask, String version, String code, String parentPath) {
            this.tag = tag == null ? "" : tag;
            this.index = index;
            this.scene = scene == null ? "" : scene;
            this.pkg = pkg == null ? "" : pkg;
            this.mask = mask == null ? "" : mask;
            this.version = version == null ? "" : version;
            this.code = code == null ? "" : code;
            this.parentPath = parentPath == null ? "" : parentPath;
        }

        public String key() {
            return tag + ":" + parentPath + ":" + scene + ":" + pkg + ":" + version + ":" + code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SceneRule)) return false;
            SceneRule sceneRule = (SceneRule) o;
            return java.util.Objects.equals(tag, sceneRule.tag)
                    && java.util.Objects.equals(scene, sceneRule.scene)
                    && java.util.Objects.equals(pkg, sceneRule.pkg)
                    && java.util.Objects.equals(mask, sceneRule.mask)
                    && java.util.Objects.equals(version, sceneRule.version)
                    && java.util.Objects.equals(code, sceneRule.code)
                    && java.util.Objects.equals(parentPath, sceneRule.parentPath);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tag, scene, pkg, mask, version, code, parentPath);
        }
    }

    public static final class CpuCtlRusConfig {
        public final Map<String, String> attrs = new LinkedHashMap<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CpuCtlRusConfig)) return false;
            return java.util.Objects.equals(attrs, ((CpuCtlRusConfig) o).attrs);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(attrs); }
    }

    public static final class HighLoadingConfig {
        public boolean skipFramesEnable;
        public String skipFramesDuration = "";
        public String skipFramesCount = "";
        public boolean cpuLoadingEnable;
        public String cpuLoadingTopCnt = "";
        public String thresholdTotal = "";
        public String thresholdLittleCoreAvg = "";
        public String thresholdBigCoreAvg = "";
        public String parentPath = "";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HighLoadingConfig)) return false;
            HighLoadingConfig that = (HighLoadingConfig) o;
            return skipFramesEnable == that.skipFramesEnable
                    && cpuLoadingEnable == that.cpuLoadingEnable
                    && java.util.Objects.equals(skipFramesDuration, that.skipFramesDuration)
                    && java.util.Objects.equals(skipFramesCount, that.skipFramesCount)
                    && java.util.Objects.equals(cpuLoadingTopCnt, that.cpuLoadingTopCnt)
                    && java.util.Objects.equals(thresholdTotal, that.thresholdTotal)
                    && java.util.Objects.equals(thresholdLittleCoreAvg, that.thresholdLittleCoreAvg)
                    && java.util.Objects.equals(thresholdBigCoreAvg, that.thresholdBigCoreAvg)
                    && java.util.Objects.equals(parentPath, that.parentPath);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(skipFramesEnable, skipFramesDuration, skipFramesCount, cpuLoadingEnable,
                    cpuLoadingTopCnt, thresholdTotal, thresholdLittleCoreAvg, thresholdBigCoreAvg, parentPath);
        }
    }

    public static final class StrictModeSwitches {
        public boolean strictEnable;
        public String version = "";
        public final List<ModeSwitch> modes = new ArrayList<>();

        public static final class ModeSwitch {
            public String tag = "";
            public String level = "";
            public boolean enable;
            public String parentPath = "";

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ModeSwitch)) return false;
                ModeSwitch that = (ModeSwitch) o;
                return enable == that.enable
                        && java.util.Objects.equals(tag, that.tag)
                        && java.util.Objects.equals(level, that.level)
                        && java.util.Objects.equals(parentPath, that.parentPath);
            }

            @Override
            public int hashCode() { return java.util.Objects.hash(tag, level, enable, parentPath); }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StrictModeSwitches)) return false;
            StrictModeSwitches that = (StrictModeSwitches) o;
            return strictEnable == that.strictEnable
                    && java.util.Objects.equals(version, that.version)
                    && java.util.Objects.equals(modes, that.modes);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(strictEnable, version, modes); }
    }

    public static final class AndroidFreezeConfig {
        public boolean enable;
        public boolean whiteEnable;
        public boolean killWhiteEnable;
        public boolean sysAppExemptEnable;
        public final List<ExemptQuota> exemptQuotas = new ArrayList<>();

        public static final class ExemptQuota {
            public String pkg = "";
            public String bgMode = "";
            public String parentPath = "";

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ExemptQuota)) return false;
                ExemptQuota that = (ExemptQuota) o;
                return java.util.Objects.equals(pkg, that.pkg)
                        && java.util.Objects.equals(bgMode, that.bgMode)
                        && java.util.Objects.equals(parentPath, that.parentPath);
            }

            @Override
            public int hashCode() { return java.util.Objects.hash(pkg, bgMode, parentPath); }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AndroidFreezeConfig)) return false;
            AndroidFreezeConfig that = (AndroidFreezeConfig) o;
            return enable == that.enable && whiteEnable == that.whiteEnable && killWhiteEnable == that.killWhiteEnable
                    && sysAppExemptEnable == that.sysAppExemptEnable && java.util.Objects.equals(exemptQuotas, that.exemptQuotas);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(enable, whiteEnable, killWhiteEnable, sysAppExemptEnable, exemptQuotas); }
    }

    public static final class AppQuotaConfig {
        public String maxDuration = "";
        public String protectTimeAfterExceed = "";
        public String parentPath = "";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AppQuotaConfig)) return false;
            AppQuotaConfig that = (AppQuotaConfig) o;
            return java.util.Objects.equals(maxDuration, that.maxDuration)
                    && java.util.Objects.equals(protectTimeAfterExceed, that.protectTimeAfterExceed)
                    && java.util.Objects.equals(parentPath, that.parentPath);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(maxDuration, protectTimeAfterExceed, parentPath); }
    }

    public static final class OverlayV3C {
        public Set<String> serviceBumpReasons = new LinkedHashSet<>();
        public List<SceneRule> preventRules = new ArrayList<>();
        public List<SceneRule> allowRules = new ArrayList<>();
        public CpuCtlRusConfig cpuCtlRus = new CpuCtlRusConfig();
        public List<HighLoadingConfig> highLoadingConfigs = new ArrayList<>();
        public StrictModeSwitches strictModeSwitches = new StrictModeSwitches();
        public AndroidFreezeConfig androidFreezeConfig = new AndroidFreezeConfig();
        public AppQuotaConfig appQuotaConfig = new AppQuotaConfig();
    }

    public static final class Parsed {
        public final List<PolicyItem> policyItems = new ArrayList<>();
        public final Map<String, Set<String>> blacklistByInherit = new LinkedHashMap<>();
        public final Set<String> freezeAllowList = new LinkedHashSet<>();
        public final Set<String> doNotFreezeList = new LinkedHashSet<>();
        public final List<TimingItem> timingItems = new ArrayList<>();
        public final Set<String> imPkgList = new LinkedHashSet<>();
        public final Set<String> cpuCtlWhiteList = new LinkedHashSet<>();
        public final Set<String> appCardIgnoreList = new LinkedHashSet<>();
        // Never Silent Partial Edits: repeated same-tag nodes must be read/write/inspect all-occurrences.
        public final Map<String, String> enableConfigAttrs = new LinkedHashMap<>();
        public final Set<String> gmsList = new LinkedHashSet<>();
        public boolean blackHansEnabled;
        public final List<String> blackHansPkgTokens = new ArrayList<>();
        public final Set<String> ffSkipFastFreeze = new LinkedHashSet<>();
        public final Map<String, Set<String>> whitePkgByCategory = new LinkedHashMap<>();
        public boolean hasFfPkgBlack;
        public boolean hasWhitePkg;
        public boolean hasEnableConfig;
        public boolean hasGms;
        public boolean hasBlackHansPkg;

        public final ProxyGlobalConfig proxyGlobalConfig = new ProxyGlobalConfig();
        public final List<String> defaultProxyBcActions = new ArrayList<>();
        public final List<ProxyBcExcludeRule> proxyBcExcludeRules = new ArrayList<>();
        public final ProxyWakeLockConfig proxyWakeLockConfig = new ProxyWakeLockConfig();
        public final ProxyGpsConfig proxyGpsConfig = new ProxyGpsConfig();
        public final ProxyBtScanConfig proxyBtScanConfig = new ProxyBtScanConfig();
        public final ProxyCpnExtensionConfig proxyCpnExtensionConfig = new ProxyCpnExtensionConfig();
        public final ProxySensorConfig proxySensorConfig = new ProxySensorConfig();
        public final ProxyBinderConfig proxyBinderConfig = new ProxyBinderConfig();

        public final Map<String, List<String>> pkgConfigTokensByCategory = new LinkedHashMap<>();
        public final List<BinderRuleItem> binderRuleItems = new ArrayList<>();

        public final Set<String> serviceBumpReasons = new LinkedHashSet<>();
        public final List<SceneRule> preventRules = new ArrayList<>();
        public final List<SceneRule> allowRules = new ArrayList<>();
        public final CpuCtlRusConfig cpuCtlRusConfig = new CpuCtlRusConfig();
        public final List<HighLoadingConfig> highLoadingConfigs = new ArrayList<>();
        public final StrictModeSwitches strictModeSwitches = new StrictModeSwitches();
        public final AndroidFreezeConfig androidFreezeConfig = new AndroidFreezeConfig();
        public final AppQuotaConfig appQuotaConfig = new AppQuotaConfig();
    }

    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public enum EditableCapability { NONE, PKG_TOKENS_ONLY, FULL_LIST }

    public static final class InspectorHit {
        public final String sectionTag;
        public final String nodePath;
        public final RiskLevel riskLevel;
        public final EditableCapability editableCapability;
        public final String summary;

        InspectorHit(String sectionTag, String nodePath, RiskLevel riskLevel, EditableCapability editableCapability, String summary) {
            this.sectionTag = sectionTag;
            this.nodePath = nodePath;
            this.riskLevel = riskLevel;
            this.editableCapability = editableCapability;
            this.summary = summary;
        }
    }

    public static final class BinderRuleItem {
        public final int index;
        public final String type;
        public final String name;
        public final String aid;
        public final String rawPkg;
        public final List<String> pkgTokens;

        BinderRuleItem(int index, String type, String name, String aid, String rawPkg, List<String> pkgTokens) {
            this.index = index;
            this.type = type;
            this.name = name;
            this.aid = aid;
            this.rawPkg = rawPkg;
            this.pkgTokens = pkgTokens;
        }

        public BinderRuleItem copyWithPkgTokens(List<String> nextTokens) {
            return new BinderRuleItem(index, type, name, aid, rawPkg, new ArrayList<>(nextTokens));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BinderRuleItem)) return false;
            BinderRuleItem that = (BinderRuleItem) o;
            return index == that.index
                    && java.util.Objects.equals(type, that.type)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(aid, that.aid)
                    && java.util.Objects.equals(rawPkg, that.rawPkg)
                    && java.util.Objects.equals(pkgTokens, that.pkgTokens);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(index, type, name, aid, rawPkg, pkgTokens);
        }
    }

    private static final class ConcatenatedXmlParts {
        final byte[] prefixBytes;
        final byte[] activeBytes;

        ConcatenatedXmlParts(byte[] prefixBytes, byte[] activeBytes) {
            this.prefixBytes = prefixBytes;
            this.activeBytes = activeBytes;
        }
    }

    private static final String[] LCD_OFF_ATTRS = {"ffInterval", "interval", "ffTotal"};
    private static final String[] LCD_ON_ATTRS = {"RToM", "MToF", "checkImportance"};
    private static final String[] FF_ATTRS = {"enterTimeout", "interval", "maxFzNum"};
    private static final String[] POWER_SAVE_ATTRS = {"repeatTime"};
    private static final String[] TRAFFIC_ATTRS = {"interval", "trafficUnfreezeTime"};
    private static final String[] API_TIMEOUT_ATTRS = {"upperTimeout", "floorTimeout"};
    private static final String[] QUERY_USAGE_ATTRS = {"queryIntervalTime", "messageDelayTime", "messageRepeateCount"};
    private static final String[] SCREEN_OFF_STILL_FORCE_PROXY_ATTRS = {"gpsInterval", "defaultInterval"};
    private static final String[] FREEZE_INTERVAL_ATTRS = {"guardTime", "RToM", "RToMST", "MToF", "MToFST", "checkImportance"};

    private TombstoneXmlEditor() {}

    public static Parsed parse(byte[] xmlBytes) throws Exception {
        Document doc = parseDoc(splitConcatenated(xmlBytes).activeBytes);
        Parsed out = new Parsed();
        for (String scene : BLACKLIST_SCENES) out.blacklistByInherit.put(scene, new LinkedHashSet<>());

        readPolicyGroup(doc, GROUP_POLICY, out.policyItems);
        readPolicyGroup(doc, GROUP_RESTRICT, out.policyItems);
        warnCrossSectionNameCollision(out.policyItems);
        readTimingItems(doc, out.timingItems);

        NodeList sbas = doc.getElementsByTagName("SysBlackApp");
        for (int i = 0; i < sbas.getLength(); i++) {
            Element e = (Element) sbas.item(i);
            String inherit = attr(e, "inherit");
            String name = attr(e, "name");
            if (out.blacklistByInherit.containsKey(inherit) && !name.isEmpty()) out.blacklistByInherit.get(inherit).add(name);
        }

        readItemList(doc, "freeze_allow_list", out.freezeAllowList);
        readItemList(doc, "donot_freeze_app_list", out.doNotFreezeList);
        readAttrList(doc, "imPkg", "pkg", out.imPkgList);
        readAttrList(doc, "cpuCtlWhiteList", "pkg", out.cpuCtlWhiteList);
        readAttrList(doc, "ignoreApp", "pkg", out.appCardIgnoreList);
        readEnableConfig(doc, out.enableConfigAttrs);
        readAttrList(doc, "gms", "name", out.gmsList);
        readBlackHansPkg(doc, out);
        readFfPkgType(doc, "black", out.ffSkipFastFreeze);
        readWhitePkgByCategory(doc, out.whitePkgByCategory);
        out.hasFfPkgBlack = hasAnyAttrValue(doc, "ffPkg", "type", "black");
        out.hasWhitePkg = !elementsByTag(doc, "whitePkg").isEmpty();
        out.hasEnableConfig = !elementsByTag(doc, "enableConfig").isEmpty();
        out.hasGms = !elementsByTag(doc, "gms").isEmpty();
        out.hasBlackHansPkg = !elementsByTag(doc, "blackHansPkg").isEmpty();
        readProxyGlobalConfig(doc, out.proxyGlobalConfig);
        readDefaultProxyBc(doc, out.defaultProxyBcActions);
        readProxyBcExcludeRules(doc, out.proxyBcExcludeRules);
        readProxyWakeLock(doc, out.proxyWakeLockConfig);
        readProxyGps(doc, out.proxyGpsConfig);
        readProxyBtScan(doc, out.proxyBtScanConfig);
        readProxyCpnExtension(doc, out.proxyCpnExtensionConfig);
        readProxySensor(doc, out.proxySensorConfig);
        readProxyBinder(doc, out.proxyBinderConfig);
        readPkgConfig(doc, out.pkgConfigTokensByCategory);
        readBinderRules(doc, out.binderRuleItems);
        readServiceBumpReasons(doc, out.serviceBumpReasons);
        readSceneRules(doc, "prevent", out.preventRules);
        readSceneRules(doc, "allow", out.allowRules);
        readCpuCtlRus(doc, out.cpuCtlRusConfig);
        readHighLoading(doc, out.highLoadingConfigs);
        readStrictModeSwitches(doc, out.strictModeSwitches);
        readAndroidFreeze(doc, out.androidFreezeConfig);
        readAppQuotaConfig(doc, out.appQuotaConfig);
        return out;
    }

    public static byte[] applyOverlay(byte[] baseXmlBytes,
                                      Map<String, Set<String>> blacklists,
                                      Set<String> freezeAllow,
                                      Set<String> doNotFreeze,
                                      Map<String, PolicyItem> policyDrafts,
                                      Map<String, String> timingDelta,
                                      Map<String, String> enableConfigAttrs,
                                      Set<String> gmsList,
                                      boolean blackHansEnabled,
                                      List<String> blackHansPkgTokens,
                                      Set<String> imPkg,
                                      Set<String> cpuCtlWhiteList,
                                      Set<String> appCardIgnore,
                                      Set<String> ffSkipFastFreeze,
                                      Map<String, Set<String>> whitePkgByCategory,
                                      ProxyGlobalConfig proxyGlobalConfig,
                                      List<String> defaultProxyBcActions,
                                      List<ProxyBcExcludeRule> proxyBcExcludeRules,
                                      ProxyWakeLockConfig proxyWakeLockConfig,
                                      ProxyGpsConfig proxyGpsConfig,
                                      ProxyBtScanConfig proxyBtScanConfig,
                                      ProxyCpnExtensionConfig proxyCpnExtensionConfig,
                                      ProxySensorConfig proxySensorConfig,
                                      ProxyBinderConfig proxyBinderConfig,
                                      Map<String, List<String>> pkgConfigTokensByCategory,
                                      List<BinderRuleItem> binderRuleItems,
                                      OverlayV3C v3c) throws Exception {
        ConcatenatedXmlParts parts = splitConcatenated(baseXmlBytes);
        Document doc = parseDoc(parts.activeBytes);
        updateBlacklists(doc, blacklists);
        rewriteItemList(doc, "freeze_allow_list", freezeAllow);
        rewriteItemList(doc, "donot_freeze_app_list", doNotFreeze);
        applyPolicyGroupDraft(doc, GROUP_POLICY, policyDrafts);
        applyPolicyGroupDraft(doc, GROUP_RESTRICT, policyDrafts);
        applyTimingDelta(doc, timingDelta);
        rewriteEnableConfig(doc, enableConfigAttrs);
        rewriteAttrList(doc, "gms", "name", gmsList);
        rewriteBlackHansPkg(doc, blackHansEnabled, blackHansPkgTokens);
        rewriteAttrList(doc, "imPkg", "pkg", imPkg);
        rewriteAttrList(doc, "cpuCtlWhiteList", "pkg", cpuCtlWhiteList);
        rewriteAttrList(doc, "ignoreApp", "pkg", appCardIgnore);
        rewriteFfPkgBlack(doc, ffSkipFastFreeze);
        rewriteWhitePkg(doc, whitePkgByCategory);
        rewriteProxyGlobalConfig(doc, proxyGlobalConfig);
        rewriteDefaultProxyBc(doc, defaultProxyBcActions);
        rewriteProxyBcExcludeRules(doc, proxyBcExcludeRules);
        rewriteProxyWakeLock(doc, proxyWakeLockConfig);
        rewriteProxyGps(doc, proxyGpsConfig);
        rewriteProxyBtScan(doc, proxyBtScanConfig);
        rewriteProxyCpnExtension(doc, proxyCpnExtensionConfig);
        rewriteProxySensor(doc, proxySensorConfig);
        rewriteProxyBinder(doc, proxyBinderConfig);
        rewritePkgConfig(doc, pkgConfigTokensByCategory);
        rewriteBinderRules(doc, binderRuleItems);
        applyV3COverlay(doc, v3c);
        return mergeConcatenated(parts, toBytes(doc));
    }

    public static List<InspectorHit> inspectPackage(byte[] xmlBytes, String packageName) throws Exception {
        String pkg = packageName == null ? "" : packageName.trim();
        if (pkg.isEmpty()) return Collections.emptyList();
        Document doc = parseDoc(splitConcatenated(xmlBytes).activeBytes);
        List<InspectorHit> hits = new ArrayList<>();
        scanAttrHits(doc, hits, "imPkg", "pkg", pkg, "imPkg", RiskLevel.LOW, EditableCapability.FULL_LIST);
        scanAttrHits(doc, hits, "cpuCtlWhiteList", "pkg", pkg, "cpuCtlWhiteList", RiskLevel.LOW, EditableCapability.FULL_LIST);
        scanAttrHits(doc, hits, "ignoreApp", "pkg", pkg, "appCard.ignoreApp", RiskLevel.LOW, EditableCapability.FULL_LIST);
        scanAttrHits(doc, hits, "SysBlackApp", "name", pkg, "SysBlackApp", RiskLevel.LOW, EditableCapability.FULL_LIST);
        scanAttrHits(doc, hits, "sysBlack", "name", pkg, "sysBlack", RiskLevel.LOW, EditableCapability.FULL_LIST);
        scanAttrHits(doc, hits, "gms", "name", pkg, "gms", RiskLevel.MEDIUM, EditableCapability.FULL_LIST);
        scanItemListHits(doc, hits, "freeze_allow_list", pkg, "freeze_allow_list");
        scanItemListHits(doc, hits, "donot_freeze_app_list", pkg, "donot_freeze_app_list");
        scanEnableConfigHits(doc, hits);
        scanBlackHansHits(doc, hits, pkg);
        scanFfPkgHits(doc, hits, pkg);
        scanWhitePkgHits(doc, hits, pkg);
        scanProxyGpsHits(doc, hits, pkg);
        scanProxySensorHits(doc, hits, pkg);
        scanProxyBinderHits(doc, hits, pkg);
        scanSceneRuleHits(doc, hits, "prevent", pkg, RiskLevel.HIGH);
        scanSceneRuleHits(doc, hits, "allow", pkg, RiskLevel.HIGH);
        scanAndroidFreezeQuotaHits(doc, hits, pkg);
        scanPkgConfigHits(doc, hits, pkg);
        scanBinderHits(doc, hits, pkg);
        return hits;
    }

    private static void applyTimingDelta(Document doc, Map<String, String> timingDelta) {
        if (timingDelta == null || timingDelta.isEmpty()) return;
        List<TimingItem> source = new ArrayList<>();
        readTimingItems(doc, source);
        for (TimingItem timingItem : source) {
            String next = timingDelta.get(timingItem.key);
            if (next == null) continue;
            Element target = elementAt(doc, timingItem.nodeTag, timingItem.nodeIndex);
            if (target != null && target.hasAttribute(timingItem.attrName)) {
                target.setAttribute(timingItem.attrName, next.trim());
            }
        }
    }

    private static void readTimingItems(Document doc, List<TimingItem> out) {
        appendTimedAttrs(doc, out, "lcdOffConfig", LCD_OFF_ATTRS);
        appendTimedAttrs(doc, out, "lcdOnConfig", LCD_ON_ATTRS);
        appendTimedAttrs(doc, out, "ffConfig", FF_ATTRS);
        appendTimedAttrs(doc, out, "powerSaveConfig", POWER_SAVE_ATTRS);
        appendTimedAttrs(doc, out, "trafficConfig", TRAFFIC_ATTRS);
        appendTimedAttrs(doc, out, "ApiTimeoutConfig", API_TIMEOUT_ATTRS);
        appendTimedAttrs(doc, out, "queryUsageStatsConfig", QUERY_USAGE_ATTRS);
        appendTimedAttrs(doc, out, "screenOffStillForceProxyConfig", SCREEN_OFF_STILL_FORCE_PROXY_ATTRS);

        NodeList freezeIntervals = doc.getElementsByTagName("freezeInterval");
        for (int i = 0; i < freezeIntervals.getLength(); i++) {
            Element e = (Element) freezeIntervals.item(i);
            String appType = attr(e, "appType");
            for (String attr : FREEZE_INTERVAL_ATTRS) {
                if (!e.hasAttribute(attr)) continue;
                Element parent = e.getParentNode() instanceof Element ? (Element) e.getParentNode() : null;
                String modeTag = parent == null ? "" : parent.getTagName();
                String modeLevel = parent == null ? "" : attr(parent, "level");
                String key = "freezeInterval{" + modeTag + ":" + modeLevel + ":" + appType + "}." + attr;
                out.add(new TimingItem(key, "freezeInterval", i, appType, modeTag, modeLevel, attr, attr(e, attr)));
            }
        }
    }

    private static void appendTimedAttrs(Document doc, List<TimingItem> out, String tag, String[] attrs) {
        NodeList nodes = doc.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            for (String attr : attrs) {
                if (!e.hasAttribute(attr)) continue;
                String key = tag + (nodes.getLength() > 1 ? "[" + i + "]" : "") + "." + attr;
                out.add(new TimingItem(key, tag, i, "", "", "", attr, attr(e, attr)));
            }
        }
    }

    private static Element elementAt(Document doc, String tagName, int index) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (index < 0 || index >= nodes.getLength()) return null;
        return (Element) nodes.item(index);
    }

    private static void applyPolicyGroupDraft(Document doc, String groupName, Map<String, PolicyItem> drafts) {
        if (drafts == null || drafts.isEmpty()) return;
        NodeList groups = doc.getElementsByTagName(groupName);
        if (groups.getLength() == 0) return;
        if (groups.getLength() != 1) {
            throw new IllegalStateException("Multiple <" + groupName + "> nodes found in active segment (" + groups.getLength()
                    + "). To avoid silent partial edits, this structure is not supported.");
        }
        Element root = (Element) groups.item(0);
        NodeList nodes = root.getElementsByTagName("policy");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String name = attr(e, "name");
            String scene = attr(e, "scene");
            PolicyItem draft = drafts.get(policyKey(groupName, name, scene));
            if (draft == null) continue;
            String nodePath = groupName + "/policy[@name='" + name + "'][@scene='" + scene + "']";
            Log.d(TAG, "ApplyOverlayTarget=" + nodePath);
            writeAttrIfExists(e, "scene", draft.scene, draft.hasScene);
            writeAttrIfExists(e, "mask", draft.mask, draft.hasMask);
            writeAttrIfExists(e, "quota", draft.quota, draft.hasQuota);
            writeAttrIfExists(e, "bgMode", draft.bgMode, draft.hasBgMode);
            if (draft.hasFgsExempt) {
                String text = draft.fgsExempt == null ? "" : draft.fgsExempt.trim();
                if (text.isEmpty()) {
                    e.removeAttribute("fgsExempt");
                } else {
                    e.setAttribute("fgsExempt", parseBooleanText(text) ? "true" : "false");
                }
            }
        }
    }

    private static void readPolicyGroup(Document doc, String groupName, List<PolicyItem> out) {
        NodeList groups = doc.getElementsByTagName(groupName);
        if (groups.getLength() == 0) return;
        if (groups.getLength() != 1) {
            throw new IllegalStateException("Multiple <" + groupName + "> nodes found in active segment (" + groups.getLength()
                    + "). To avoid silent partial edits, this structure is not supported.");
        }
        Element root = (Element) groups.item(0);
        NodeList policies = root.getElementsByTagName("policy");
        for (int i = 0; i < policies.getLength(); i++) {
            Element e = (Element) policies.item(i);
            String scene = attr(e, "scene");
            out.add(new PolicyItem(
                    groupName,
                    attr(e, "name"),
                    scene,
                    scene,
                    attr(e, "mask"),
                    attr(e, "quota"),
                    attr(e, "bgMode"),
                    attr(e, "fgsExempt"),
                    e.hasAttribute("scene"),
                    e.hasAttribute("mask"),
                    e.hasAttribute("quota"),
                    e.hasAttribute("bgMode"),
                    e.hasAttribute("fgsExempt")
            ));
        }
    }

    private static String policyKey(String sectionTag, String policyName, String sceneString) {
        String section = sectionTag == null ? "" : sectionTag.trim();
        String name = policyName == null ? "" : policyName.trim();
        String scene = sceneString == null ? "" : sceneString.trim();
        return section + ":" + name + ":" + scene;
    }

    private static void warnCrossSectionNameCollision(List<PolicyItem> items) {
        Map<String, Set<String>> sectionsByName = new LinkedHashMap<>();
        for (PolicyItem item : items) {
            Set<String> sections = sectionsByName.computeIfAbsent(item.name, ignored -> new LinkedHashSet<>());
            sections.add(item.groupName);
        }
        for (Map.Entry<String, Set<String>> entry : sectionsByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                Log.w(TAG, "Policy name appears in multiple sections: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }

    private static void writeAttrIfExists(Element e, String name, String value, boolean hasAttrInSource) {
        if (!hasAttrInSource) return;
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            e.removeAttribute(name);
        } else {
            e.setAttribute(name, trimmed);
        }
    }

    private static boolean parseBooleanText(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized);
    }

    private static void updateBlacklists(Document doc, Map<String, Set<String>> selectedByScene) {
        NodeList sbas = doc.getElementsByTagName("SysBlackApp");
        Map<String, List<Element>> byScene = new HashMap<>();
        for (String s : BLACKLIST_SCENES) byScene.put(s, new ArrayList<>());
        for (int i = 0; i < sbas.getLength(); i++) {
            Element e = (Element) sbas.item(i);
            String inherit = attr(e, "inherit");
            if (byScene.containsKey(inherit)) byScene.get(inherit).add(e);
        }

        for (String scene : BLACKLIST_SCENES) {
            Set<String> selected = selectedByScene.get(scene);
            if (selected == null) selected = Collections.emptySet();
            List<Element> existing = byScene.get(scene);
            Set<String> remain = new LinkedHashSet<>(selected);

            String templateQuota = "500000";
            String templateBgMode = "0";
            if (!existing.isEmpty()) {
                templateQuota = attr(existing.get(0), "quota");
                templateBgMode = attr(existing.get(0), "bgMode");
                if (templateQuota.isEmpty()) templateQuota = "500000";
                if (templateBgMode.isEmpty()) templateBgMode = "0";
            }

            Node parent = null;
            Node insertAfter = null;
            for (Element e : existing) {
                parent = e.getParentNode();
                insertAfter = e;
                String pkg = attr(e, "name");
                if (!selected.contains(pkg)) {
                    e.getParentNode().removeChild(e);
                } else {
                    remain.remove(pkg);
                }
            }
            if (parent == null) parent = doc.getDocumentElement();

            for (String pkg : remain) {
                Element n = doc.createElement("SysBlackApp");
                n.setAttribute("inherit", scene);
                n.setAttribute("name", pkg);
                n.setAttribute("minVer", "");
                n.setAttribute("maxVer", "");
                n.setAttribute("quota", templateQuota);
                n.setAttribute("bgMode", templateBgMode);
                if (insertAfter != null && insertAfter.getParentNode() == parent) {
                    Node next = insertAfter.getNextSibling();
                    parent.insertBefore(n, next);
                } else {
                    parent.appendChild(n);
                }
                insertAfter = n;
            }
        }
    }

    private static List<Element> elementsByTag(Document doc, String tagName) {
        NodeList nl = doc.getElementsByTagName(tagName);
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            out.add((Element) nl.item(i));
        }
        return out;
    }

    private static int siblingIndex(Element e) {
        if (e == null || e.getParentNode() == null) return 0;
        NodeList children = e.getParentNode().getChildNodes();
        int index = 0;
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            if (!e.getTagName().equals(n.getNodeName())) continue;
            if (n == e) return index;
            index++;
        }
        return index;
    }

    private static String shortContextPath(Node node) {
        if (!(node instanceof Element)) return "";
        Element current = (Element) node;
        List<String> segments = new ArrayList<>();
        while (current != null) {
            StringBuilder segment = new StringBuilder(current.getTagName());
            segment.append("[").append(siblingIndex(current)).append("]");
            segments.add(segment.toString());
            Node parent = current.getParentNode();
            if (!(parent instanceof Element)) break;
            current = (Element) parent;
        }
        Collections.reverse(segments);
        return String.join("/", segments);
    }

    private static void readItemList(Document doc, String tagName, Set<String> out) {
        List<Element> roots = elementsByTag(doc, tagName);
        for (Element root : roots) {
            NodeList children = root.getElementsByTagName("item");
            for (int i = 0; i < children.getLength(); i++) {
                String text = children.item(i).getTextContent();
                if (text != null && !text.trim().isEmpty()) out.add(text.trim());
            }
        }
    }

    private static void rewriteItemList(Document doc, String tagName, Set<String> values) {
        List<Element> roots = elementsByTag(doc, tagName);
        if (roots.isEmpty()) return;
        List<String> sorted = new ArrayList<>(values == null ? Collections.emptySet() : values);
        Collections.sort(sorted);
        for (Element root : roots) {
            List<Node> toDelete = new ArrayList<>();
            Node child = root.getFirstChild();
            while (child != null) {
                Node next = child.getNextSibling();
                if (child.getNodeType() == Node.ELEMENT_NODE && "item".equals(child.getNodeName())) toDelete.add(child);
                child = next;
            }
            for (Node n : toDelete) root.removeChild(n);
            for (String value : sorted) {
                Element item = doc.createElement("item");
                item.setTextContent(value);
                root.appendChild(item);
            }
        }
    }

    private static boolean hasAnyAttrValue(Document doc, String tagName, String attrName, String value) {
        for (Element e : elementsByTag(doc, tagName)) {
            if (value.equals(attr(e, attrName))) return true;
        }
        return false;
    }

    private static void readFfPkgType(Document doc, String type, Set<String> out) {
        for (Element e : elementsByTag(doc, "ffPkg")) {
            if (!type.equals(attr(e, "type"))) continue;
            String pkg = attr(e, "pkg").trim();
            if (!pkg.isEmpty()) out.add(pkg);
        }
    }

    private static void readWhitePkgByCategory(Document doc, Map<String, Set<String>> out) {
        for (Element e : elementsByTag(doc, "whitePkg")) {
            String category = attr(e, "category").trim();
            String name = attr(e, "name").trim();
            if (category.isEmpty() || name.isEmpty()) continue;
            out.computeIfAbsent(category, ignored -> new LinkedHashSet<>()).add(name);
        }
    }

    private static void readEnableConfig(Document doc, Map<String, String> out) {
        List<Element> nodes = elementsByTag(doc, "enableConfig");
        if (nodes.size() > 1) {
            Log.w(TAG, "Multiple enableConfig nodes detected. Merge rule: last occurrence wins for duplicate attrs.");
        }
        for (Element e : nodes) {
            org.w3c.dom.NamedNodeMap attrs = e.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                out.put(attr.getNodeName(), attr.getNodeValue());
            }
        }
    }

    private static void readBlackHansPkg(Document doc, Parsed out) {
        List<Element> nodes = elementsByTag(doc, "blackHansPkg");
        if (nodes.size() > 1) {
            Log.w(TAG, "Multiple blackHansPkg nodes detected. Merge rule: last occurrence wins.");
        }
        for (Element e : nodes) {
            out.blackHansEnabled = parseBooleanText(attr(e, "enable"));
            out.blackHansPkgTokens.clear();
            out.blackHansPkgTokens.addAll(splitPkgTokens(attr(e, "pkg")));
        }
    }

    private static void rewriteEnableConfig(Document doc, Map<String, String> attrs) {
        List<Element> nodes = elementsByTag(doc, "enableConfig");
        if (nodes.isEmpty() || attrs == null) return;
        for (Element e : nodes) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue().trim();
                if (value.isEmpty()) e.removeAttribute(key);
                else e.setAttribute(key, value);
            }
        }
    }

    private static void rewriteBlackHansPkg(Document doc, boolean enabled, List<String> tokens) {
        List<Element> nodes = elementsByTag(doc, "blackHansPkg");
        if (nodes.isEmpty()) return;
        String joined = joinTokensPreserveOrder(tokens == null ? Collections.emptyList() : tokens);
        for (Element e : nodes) {
            e.setAttribute("enable", enabled ? "true" : "false");
            e.setAttribute("pkg", joined);
        }
    }

    private static void rewriteFfPkgBlack(Document doc, Set<String> pkgs) {
        List<Element> all = elementsByTag(doc, "ffPkg");
        List<Element> blackNodes = new ArrayList<>();
        for (Element e : all) if ("black".equals(attr(e, "type"))) blackNodes.add(e);
        Node insertAfter = !blackNodes.isEmpty() ? blackNodes.get(blackNodes.size() - 1) : (!all.isEmpty() ? all.get(all.size() - 1) : null);
        Node parent = insertAfter == null ? doc.getDocumentElement() : insertAfter.getParentNode();
        Set<String> remain = new LinkedHashSet<>(pkgs == null ? Collections.emptySet() : pkgs);
        for (Element e : blackNodes) {
            String pkg = attr(e, "pkg").trim();
            if (!remain.contains(pkg)) e.getParentNode().removeChild(e);
            else remain.remove(pkg);
        }
        List<String> sorted = new ArrayList<>(remain);
        Collections.sort(sorted);
        for (String pkg : sorted) {
            Element n = doc.createElement("ffPkg");
            n.setAttribute("type", "black");
            n.setAttribute("pkg", pkg);
            if (insertAfter != null && insertAfter.getParentNode() == parent) {
                parent.insertBefore(n, insertAfter.getNextSibling());
            } else {
                parent.appendChild(n);
            }
            insertAfter = n;
        }
    }

    private static void rewriteWhitePkg(Document doc, Map<String, Set<String>> byCategory) {
        if (byCategory == null) return;
        List<Element> all = elementsByTag(doc, "whitePkg");
        Map<String, List<Element>> existingByCat = new LinkedHashMap<>();
        for (Element e : all) {
            String cat = attr(e, "category").trim();
            existingByCat.computeIfAbsent(cat, ignored -> new ArrayList<>()).add(e);
        }
        Node globalInsertAfter = all.isEmpty() ? null : all.get(all.size() - 1);
        Node defaultParent = globalInsertAfter == null ? doc.getDocumentElement() : globalInsertAfter.getParentNode();
        for (Map.Entry<String, Set<String>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            if (cat == null || cat.trim().isEmpty()) continue;
            Set<String> selected = new LinkedHashSet<>(entry.getValue() == null ? Collections.emptySet() : entry.getValue());
            List<Element> existing = existingByCat.getOrDefault(cat, Collections.emptyList());
            Node insertAfter = !existing.isEmpty() ? existing.get(existing.size() - 1) : globalInsertAfter;
            Node parent = insertAfter == null ? defaultParent : insertAfter.getParentNode();
            for (Element e : existing) {
                String name = attr(e, "name").trim();
                if (!selected.contains(name)) e.getParentNode().removeChild(e);
                else selected.remove(name);
            }
            List<String> sorted = new ArrayList<>(selected);
            Collections.sort(sorted);
            for (String name : sorted) {
                Element n = doc.createElement("whitePkg");
                n.setAttribute("name", name);
                n.setAttribute("category", cat);
                if (insertAfter != null && insertAfter.getParentNode() == parent) {
                    parent.insertBefore(n, insertAfter.getNextSibling());
                } else {
                    parent.appendChild(n);
                }
                insertAfter = n;
                globalInsertAfter = n;
            }
        }
    }

    private static boolean isProxyGlobalConfig(Element e) {
        return e.hasAttribute("alarm") || e.hasAttribute("serivce") || e.hasAttribute("job")
                || e.hasAttribute("broadcast") || e.hasAttribute("proxyBCmax");
    }

    private static List<Element> findProxyGlobalConfigNodes(Document doc) {
        List<Element> out = new ArrayList<>();
        for (Element e : elementsByTag(doc, "proxyConfig")) {
            if (isProxyGlobalConfig(e)) out.add(e);
        }
        return out;
    }

    private static void readProxyGlobalConfig(Document doc, ProxyGlobalConfig out) {
        List<Element> nodes = findProxyGlobalConfigNodes(doc);
        if (nodes.isEmpty()) return;
        if (nodes.size() > 1) Log.w(TAG, "Multiple global proxyConfig nodes detected.");
        Element e = nodes.get(0);
        out.hasAny = true;
        out.alarm = parseBooleanText(attr(e, "alarm"));
        out.service = parseBooleanText(attr(e, "serivce"));
        out.job = parseBooleanText(attr(e, "job"));
        out.broadcast = parseBooleanText(attr(e, "broadcast"));
        out.proxyBCmax = attr(e, "proxyBCmax");
    }

    private static void readDefaultProxyBc(Document doc, List<String> out) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (Element root : elementsByTag(doc, "defaultProxyBc")) {
            NodeList children = root.getElementsByTagName("item");
            for (int i = 0; i < children.getLength(); i++) {
                String t = children.item(i).getTextContent();
                if (t != null && !t.trim().isEmpty()) ordered.add(t.trim());
            }
        }
        out.clear();
        out.addAll(ordered);
    }

    private static void readProxyBcExcludeRules(Document doc, List<ProxyBcExcludeRule> out) {
        out.clear();
        for (Element e : elementsByTag(doc, "proxyBcExcludeAction")) {
            ProxyBcExcludeRule r = new ProxyBcExcludeRule();
            r.action = attr(e, "action");
            r.appTypeTokens = splitPkgTokens(attr(e, "appType"));
            out.add(r);
        }
    }

    private static void readProxyWakeLock(Document doc, ProxyWakeLockConfig out) {
        List<Element> defaults = elementsByTag(doc, "defaultProxyWorkSourceTag");
        if (!defaults.isEmpty()) out.defaultWorkSourceType = attr(defaults.get(defaults.size() - 1), "defaultWorkSourceType");
        for (Element e : elementsByTag(doc, "proxyWakeLock")) {
            if (!(e.hasAttribute("supportWorkSourceTags") || e.hasAttribute("unSupportedWlTypes") || e.hasAttribute("proxyButDropTypes"))) continue;
            out.supportWorkSourceTags = attr(e, "supportWorkSourceTags");
            out.unSupportedWlTypes = attr(e, "unSupportedWlTypes");
            out.proxyButDropTypes = attr(e, "proxyButDropTypes");
        }
    }

    private static void readProxyGps(Document doc, ProxyGpsConfig out) {
        List<Element> roots = elementsByTag(doc, "proxyGps");
        if (roots.isEmpty()) return;
        Element root = roots.get(0);
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if ("config".equals(e.getTagName())) {
                out.hasEnable = e.hasAttribute("enable");
                out.enable = parseBooleanText(attr(e, "enable"));
            } else if ("item".equals(e.getTagName())) {
                ProxyGpsItem item = new ProxyGpsItem();
                item.type = attr(e, "type");
                item.appType = attr(e, "appType");
                item.pkgTokens = splitPkgTokens(attr(e, "pkg"));
                out.items.add(item);
            }
        }
    }

    private static void readProxyBtScan(Document doc, ProxyBtScanConfig out) {
        List<Element> roots = elementsByTag(doc, "proxyBtScan");
        if (roots.isEmpty()) return;
        Element root = roots.get(0);
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node n = root.getChildNodes().item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if ("config".equals(e.getTagName())) {
                out.hasEnable = e.hasAttribute("enable");
                out.enable = parseBooleanText(attr(e, "enable"));
            }
        }
    }

    private static void readProxyCpnExtension(Document doc, ProxyCpnExtensionConfig out) {
        List<Element> roots = elementsByTag(doc, "proxyCpnExtension");
        if (roots.isEmpty()) return;
        Element root = roots.get(0);
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node n = root.getChildNodes().item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if ("config".equals(e.getTagName())) {
                out.hasEnable = e.hasAttribute("enable");
                out.enable = parseBooleanText(attr(e, "enable"));
                out.cpnMask = attr(e, "cpnMask");
            }
        }
    }

    private static void readProxySensor(Document doc, ProxySensorConfig out) {
        List<Element> roots = elementsByTag(doc, "proxySensor");
        if (roots.isEmpty()) return;
        Element root = roots.get(0);
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node n = root.getChildNodes().item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if ("config".equals(e.getTagName())) {
                out.hasEnable = e.hasAttribute("enable");
                out.enable = parseBooleanText(attr(e, "enable"));
                out.hasProxyType = e.hasAttribute("proxyType");
                out.proxyType = parseBooleanText(attr(e, "proxyType"));
            } else if ("proxyPkg".equals(e.getTagName())) {
                out.proxyPkgName = attr(e, "name");
            } else if ("proxyType".equals(e.getTagName())) {
                String name = attr(e, "name");
                if (!name.isEmpty() && !out.proxyTypeNames.contains(name)) out.proxyTypeNames.add(name);
            } else if ("whiteTypePkg".equals(e.getTagName())) {
                WhiteTypePkgRule rule = new WhiteTypePkgRule();
                rule.type = attr(e, "type");
                rule.pkgTokens = splitPkgTokens(attr(e, "pkg"));
                out.whiteTypePkgs.add(rule);
            }
        }
    }

    private static void readProxyBinder(Document doc, ProxyBinderConfig out) {
        List<Element> roots = elementsByTag(doc, "proxyBinder");
        if (roots.isEmpty()) return;
        Element root = roots.get(0);
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node n = root.getChildNodes().item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            if ("config".equals(e.getTagName())) {
                out.hasEnable = e.hasAttribute("enable");
                out.enable = parseBooleanText(attr(e, "enable"));
            } else if ("descConfig".equals(e.getTagName())) {
                BinderDescRule rule = new BinderDescRule();
                rule.desc = attr(e, "desc");
                rule.pkgTokens = splitPkgTokens(attr(e, "pkg"));
                out.descConfigs.add(rule);
            }
        }
    }

    private static void rewriteProxyGlobalConfig(Document doc, ProxyGlobalConfig cfg) {
        if (cfg == null || !cfg.hasAny) return;
        for (Element e : findProxyGlobalConfigNodes(doc)) {
            e.setAttribute("alarm", cfg.alarm ? "true" : "false");
            e.setAttribute("serivce", cfg.service ? "true" : "false");
            e.setAttribute("job", cfg.job ? "true" : "false");
            e.setAttribute("broadcast", cfg.broadcast ? "true" : "false");
            if (cfg.proxyBCmax != null && !cfg.proxyBCmax.trim().isEmpty()) e.setAttribute("proxyBCmax", cfg.proxyBCmax.trim());
        }
    }

    private static void rewriteDefaultProxyBc(Document doc, List<String> actions) {
        List<Element> roots = elementsByTag(doc, "defaultProxyBc");
        if (roots.isEmpty()) {
            Element n = doc.createElement("defaultProxyBc");
            n.setAttribute("version", "1.0");
            doc.getDocumentElement().appendChild(n);
            roots = new ArrayList<>();
            roots.add(n);
        }
        List<String> ordered = new ArrayList<>(new LinkedHashSet<>(actions == null ? Collections.emptyList() : actions));
        for (Element root : roots) {
            List<Node> del = new ArrayList<>();
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                Node child = root.getChildNodes().item(i);
                if (child instanceof Element && "item".equals(child.getNodeName())) del.add(child);
            }
            for (Node d : del) root.removeChild(d);
            for (String a : ordered) {
                if (a == null || a.trim().isEmpty()) continue;
                Element item = doc.createElement("item");
                item.setTextContent(a.trim());
                root.appendChild(item);
            }
        }
    }

    private static void rewriteProxyBcExcludeRules(Document doc, List<ProxyBcExcludeRule> rules) {
        for (Element e : new ArrayList<>(elementsByTag(doc, "proxyBcExcludeAction"))) {
            if (e.getParentNode() != null) e.getParentNode().removeChild(e);
        }
        Node parent = doc.getDocumentElement();
        if (rules == null) return;
        for (ProxyBcExcludeRule rule : rules) {
            if (rule == null || rule.action == null || rule.action.trim().isEmpty()) continue;
            Element n = doc.createElement("proxyBcExcludeAction");
            n.setAttribute("action", rule.action.trim());
            n.setAttribute("appType", joinTokensPreserveOrder(rule.appTypeTokens == null ? Collections.emptyList() : rule.appTypeTokens));
            parent.appendChild(n);
        }
    }

    private static void rewriteProxyWakeLock(Document doc, ProxyWakeLockConfig cfg) {
        if (cfg == null) return;
        List<Element> defaultNodes = elementsByTag(doc, "defaultProxyWorkSourceTag");
        if (!defaultNodes.isEmpty()) {
            for (Element e : defaultNodes) e.setAttribute("defaultWorkSourceType", cfg.defaultWorkSourceType == null ? "" : cfg.defaultWorkSourceType);
        }
        for (Element e : elementsByTag(doc, "proxyWakeLock")) {
            if (!(e.hasAttribute("supportWorkSourceTags") || e.hasAttribute("unSupportedWlTypes") || e.hasAttribute("proxyButDropTypes"))) continue;
            e.setAttribute("supportWorkSourceTags", cfg.supportWorkSourceTags == null ? "" : cfg.supportWorkSourceTags);
            e.setAttribute("unSupportedWlTypes", cfg.unSupportedWlTypes == null ? "" : cfg.unSupportedWlTypes);
            e.setAttribute("proxyButDropTypes", cfg.proxyButDropTypes == null ? "" : cfg.proxyButDropTypes);
        }
    }

    private static Element firstChildByTag(Element parent, String tag) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node n = parent.getChildNodes().item(i);
            if (n instanceof Element && tag.equals(((Element) n).getTagName())) return (Element) n;
        }
        return null;
    }

    private static List<Element> childElementsByTag(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node n = parent.getChildNodes().item(i);
            if (n instanceof Element && tag.equals(((Element) n).getTagName())) out.add((Element) n);
        }
        return out;
    }

    private static void rewriteProxyGps(Document doc, ProxyGpsConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "proxyGps");
        if (roots.isEmpty()) {
            Element root = doc.createElement("proxyGps");
            doc.getDocumentElement().appendChild(root);
            roots = new ArrayList<>();
            roots.add(root);
        }
        for (Element root : roots) {
            Element config = firstChildByTag(root, "config");
            if (config == null) {
                config = doc.createElement("config");
                root.insertBefore(config, root.getFirstChild());
            }
            if (cfg.hasEnable) config.setAttribute("enable", cfg.enable ? "true" : "false");
            for (Element it : childElementsByTag(root, "item")) root.removeChild(it);
            for (ProxyGpsItem item : cfg.items) {
                Element e = doc.createElement("item");
                e.setAttribute("type", item.type == null ? "" : item.type);
                e.setAttribute("appType", item.appType == null ? "" : item.appType);
                e.setAttribute("pkg", joinTokensPreserveOrder(item.pkgTokens == null ? Collections.emptyList() : item.pkgTokens));
                root.appendChild(e);
            }
        }
    }

    private static void rewriteProxyBtScan(Document doc, ProxyBtScanConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "proxyBtScan");
        if (roots.isEmpty()) return;
        for (Element root : roots) {
            Element config = firstChildByTag(root, "config");
            if (config == null) { config = doc.createElement("config"); root.appendChild(config); }
            if (cfg.hasEnable) config.setAttribute("enable", cfg.enable ? "true" : "false");
        }
    }

    private static void rewriteProxyCpnExtension(Document doc, ProxyCpnExtensionConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "proxyCpnExtension");
        if (roots.isEmpty()) return;
        for (Element root : roots) {
            Element config = firstChildByTag(root, "config");
            if (config == null) { config = doc.createElement("config"); root.appendChild(config); }
            if (cfg.hasEnable) config.setAttribute("enable", cfg.enable ? "true" : "false");
            if (cfg.cpnMask != null) config.setAttribute("cpnMask", cfg.cpnMask);
        }
    }

    private static void rewriteProxySensor(Document doc, ProxySensorConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "proxySensor");
        if (roots.isEmpty()) return;
        for (Element root : roots) {
            Element config = firstChildByTag(root, "config");
            if (config == null) { config = doc.createElement("config"); root.insertBefore(config, root.getFirstChild()); }
            if (cfg.hasEnable) config.setAttribute("enable", cfg.enable ? "true" : "false");
            if (cfg.hasProxyType) config.setAttribute("proxyType", cfg.proxyType ? "true" : "false");
            Element proxyPkg = firstChildByTag(root, "proxyPkg");
            if (proxyPkg == null) { proxyPkg = doc.createElement("proxyPkg"); root.appendChild(proxyPkg); }
            proxyPkg.setAttribute("name", cfg.proxyPkgName == null ? "" : cfg.proxyPkgName);
            for (Element e : childElementsByTag(root, "proxyType")) root.removeChild(e);
            for (String name : cfg.proxyTypeNames) {
                if (name == null || name.trim().isEmpty()) continue;
                Element e = doc.createElement("proxyType");
                e.setAttribute("name", name.trim());
                root.appendChild(e);
            }
            for (Element e : childElementsByTag(root, "whiteTypePkg")) root.removeChild(e);
            for (WhiteTypePkgRule rule : cfg.whiteTypePkgs) {
                Element e = doc.createElement("whiteTypePkg");
                e.setAttribute("type", rule.type == null ? "" : rule.type);
                e.setAttribute("pkg", joinTokensPreserveOrder(rule.pkgTokens == null ? Collections.emptyList() : rule.pkgTokens));
                root.appendChild(e);
            }
        }
    }

    private static void rewriteProxyBinder(Document doc, ProxyBinderConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "proxyBinder");
        if (roots.isEmpty()) return;
        for (Element root : roots) {
            Element config = firstChildByTag(root, "config");
            if (config == null) { config = doc.createElement("config"); root.insertBefore(config, root.getFirstChild()); }
            if (cfg.hasEnable) config.setAttribute("enable", cfg.enable ? "true" : "false");
            for (Element e : childElementsByTag(root, "descConfig")) root.removeChild(e);
            for (BinderDescRule rule : cfg.descConfigs) {
                if (rule.desc == null || rule.desc.trim().isEmpty()) continue;
                Element e = doc.createElement("descConfig");
                e.setAttribute("desc", rule.desc.trim());
                e.setAttribute("pkg", joinTokensPreserveOrder(rule.pkgTokens == null ? Collections.emptyList() : rule.pkgTokens));
                root.appendChild(e);
            }
        }
    }

    private static void readAttrList(Document doc, String tagName, String attrName, Set<String> out) {
        NodeList nl = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String value = attr(e, attrName).trim();
            if (!value.isEmpty()) out.add(value);
        }
    }

    private static void rewriteAttrList(Document doc, String tagName, String attrName, Set<String> values) {
        NodeList nl = doc.getElementsByTagName(tagName);
        Set<String> remain = new LinkedHashSet<>(values == null ? Collections.emptySet() : values);
        List<Element> existing = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) existing.add((Element) nl.item(i));
        Node parent = existing.isEmpty() ? doc.getDocumentElement() : existing.get(0).getParentNode();
        Node insertAfter = existing.isEmpty() ? null : existing.get(existing.size() - 1);
        for (Element e : existing) {
            String v = attr(e, attrName).trim();
            if (!remain.contains(v)) e.getParentNode().removeChild(e);
            else remain.remove(v);
        }
        for (String value : remain) {
            Element n = doc.createElement(tagName);
            n.setAttribute(attrName, value);
            if (insertAfter != null && insertAfter.getParentNode() == parent) {
                Node next = insertAfter.getNextSibling();
                parent.insertBefore(n, next);
            } else parent.appendChild(n);
            insertAfter = n;
        }
    }

    private static List<String> splitPkgTokens(String raw) {
        List<String> out = new ArrayList<>();
        String[] arr = (raw == null ? "" : raw).split("#");
        for (String token : arr) {
            String t = token.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static void readPkgConfig(Document doc, Map<String, List<String>> out) {
        NodeList nl = doc.getElementsByTagName("pkgConfig");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String category = attr(e, "category");
            out.put(category, splitPkgTokens(attr(e, "name")));
        }
    }

    private static String joinTokensPreserveOrder(List<String> tokens) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String token : tokens) {
            String t = token == null ? "" : token.trim();
            if (!t.isEmpty()) ordered.add(t);
        }
        StringBuilder sb = new StringBuilder();
        for (String token : ordered) {
            if (sb.length() > 0) sb.append('#');
            sb.append(token);
        }
        return sb.toString();
    }

    private static void rewritePkgConfig(Document doc, Map<String, List<String>> byCategory) {
        if (byCategory == null) return;
        NodeList nl = doc.getElementsByTagName("pkgConfig");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String category = attr(e, "category");
            List<String> tokens = byCategory.get(category);
            if (tokens == null) continue;
            e.setAttribute("name", joinTokensPreserveOrder(tokens));
        }
    }

    private static void readBinderRules(Document doc, List<BinderRuleItem> out) {
        NodeList nl = doc.getElementsByTagName("cpnAsyncBinder");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            out.add(new BinderRuleItem(i, attr(e, "type"), attr(e, "name"), attr(e, "aid"), attr(e, "pkg"), splitPkgTokens(attr(e, "pkg"))));
        }
    }

    private static void rewriteBinderRules(Document doc, List<BinderRuleItem> rules) {
        if (rules == null) return;
        Map<Integer, BinderRuleItem> map = new LinkedHashMap<>();
        for (BinderRuleItem item : rules) map.put(item.index, item);
        NodeList nl = doc.getElementsByTagName("cpnAsyncBinder");
        for (int i = 0; i < nl.getLength(); i++) {
            BinderRuleItem item = map.get(i);
            if (item == null) continue;
            Element e = (Element) nl.item(i);
            e.setAttribute("pkg", joinTokensPreserveOrder(item.pkgTokens));
        }
    }

    private static void scanAttrHits(Document doc, List<InspectorHit> out, String tagName, String attrName, String pkg,
                                     String sectionTag, RiskLevel riskLevel, EditableCapability editableCapability) {
        NodeList nl = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String value = attrName == null ? e.getTextContent() : attr(e, attrName);
            if (!containsPkg(value, pkg)) continue;
            String path = tagName + "[" + i + "]";
            String summary = (attrName == null ? "text" : attrName) + "=" + value;
            out.add(new InspectorHit(sectionTag, path, riskLevel, editableCapability, summary));
        }
    }

    private static boolean containsPkg(String raw, String pkg) {
        if (raw == null || pkg == null) return false;
        String text = raw.trim();
        if (text.equals(pkg)) return true;
        for (String token : splitPkgTokens(text)) {
            if (pkg.equals(token)) return true;
        }
        return false;
    }

    private static void scanPkgConfigHits(Document doc, List<InspectorHit> out, String pkg) {
        NodeList nl = doc.getElementsByTagName("pkgConfig");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String name = attr(e, "name");
            if (!containsPkg(name, pkg)) continue;
            String path = "pkgConfig[" + i + "]";
            out.add(new InspectorHit("pkgConfig", path, RiskLevel.MEDIUM, EditableCapability.PKG_TOKENS_ONLY,
                    "category=" + attr(e, "category") + ", name includes " + pkg));
        }
    }

    private static void scanItemListHits(Document doc, List<InspectorHit> out, String rootTag, String pkg, String sectionTag) {
        List<Element> roots = elementsByTag(doc, rootTag);
        for (Element root : roots) {
            int rootIndex = siblingIndex(root);
            NodeList children = root.getElementsByTagName("item");
            for (int i = 0; i < children.getLength(); i++) {
                String text = children.item(i).getTextContent();
                if (text == null || !pkg.equals(text.trim())) continue;
                Node parent = root.getParentNode();
                String parentTag = parent instanceof Element ? ((Element) parent).getTagName() : "";
                String path = (parentTag.isEmpty() ? "" : parentTag + "/") + rootTag + "[" + rootIndex + "]/item[" + i + "]";
                out.add(new InspectorHit(sectionTag, path, RiskLevel.LOW, EditableCapability.FULL_LIST, "item=" + pkg));
            }
        }
    }

    private static void scanFfPkgHits(Document doc, List<InspectorHit> out, String pkg) {
        int idx = 0;
        for (Element e : elementsByTag(doc, "ffPkg")) {
            if (!"black".equals(attr(e, "type"))) continue;
            String currentPkg = attr(e, "pkg").trim();
            if (!pkg.equals(currentPkg)) {
                idx++;
                continue;
            }
            String path = shortContextPath(e) + "/ffPkg[type=black][" + idx + "]";
            out.add(new InspectorHit("ffPkg.black", path, RiskLevel.LOW, EditableCapability.FULL_LIST, "type=black pkg=" + currentPkg));
            idx++;
        }
    }

    private static void scanWhitePkgHits(Document doc, List<InspectorHit> out, String pkg) {
        int idx = 0;
        for (Element e : elementsByTag(doc, "whitePkg")) {
            String name = attr(e, "name").trim();
            String category = attr(e, "category").trim();
            if (!pkg.equals(name)) {
                idx++;
                continue;
            }
            String path = shortContextPath(e) + "/whitePkg[cat=" + category + "][" + idx + "]";
            out.add(new InspectorHit("whitePkg." + category, path, RiskLevel.LOW, EditableCapability.FULL_LIST,
                    "category=" + category + ", name=" + name));
            idx++;
        }
    }

    private static void scanBlackHansHits(Document doc, List<InspectorHit> out, String pkg) {
        List<Element> nodes = elementsByTag(doc, "blackHansPkg");
        for (int i = 0; i < nodes.size(); i++) {
            Element e = nodes.get(i);
            String rawPkg = attr(e, "pkg");
            if (!containsPkg(rawPkg, pkg)) continue;
            out.add(new InspectorHit("blackHansPkg", "blackHansPkg[" + i + "]", RiskLevel.MEDIUM,
                    EditableCapability.FULL_LIST, "enable=" + attr(e, "enable") + ", pkg includes " + pkg));
        }
    }

    private static void scanEnableConfigHits(Document doc, List<InspectorHit> out) {
        List<Element> nodes = elementsByTag(doc, "enableConfig");
        for (int i = 0; i < nodes.size(); i++) {
            Element e = nodes.get(i);
            String summary = "hansEnable=" + attr(e, "hansEnable")
                    + ", gmsEnable=" + attr(e, "gmsEnable")
                    + ", skipToast=" + attr(e, "skipToast")
                    + ", releaseStatistic=" + attr(e, "releaseStatistic")
                    + ", cgp_v2=" + attr(e, "cgp_v2");
            out.add(new InspectorHit("enableConfig", "enableConfig[" + i + "]", RiskLevel.LOW,
                    EditableCapability.NONE, summary));
        }
    }

    private static void scanProxyGpsHits(Document doc, List<InspectorHit> out, String pkg) {
        for (Element root : elementsByTag(doc, "proxyGps")) {
            List<Element> items = childElementsByTag(root, "item");
            for (int i = 0; i < items.size(); i++) {
                Element e = items.get(i);
                String raw = attr(e, "pkg");
                if (!containsPkg(raw, pkg)) continue;
                String path = "proxyGps/item[type=" + attr(e, "type") + "][appType=" + attr(e, "appType") + "][" + i + "]";
                out.add(new InspectorHit("proxy.gps", path, RiskLevel.MEDIUM, EditableCapability.FULL_LIST,
                        "type=" + attr(e, "type") + ", appType=" + attr(e, "appType") + ", pkg includes " + pkg));
            }
        }
    }

    private static void scanProxySensorHits(Document doc, List<InspectorHit> out, String pkg) {
        for (Element root : elementsByTag(doc, "proxySensor")) {
            List<Element> items = childElementsByTag(root, "whiteTypePkg");
            for (int i = 0; i < items.size(); i++) {
                Element e = items.get(i);
                String raw = attr(e, "pkg");
                if (!containsPkg(raw, pkg)) continue;
                String path = "proxySensor/whiteTypePkg[type=" + attr(e, "type") + "][" + i + "]";
                out.add(new InspectorHit("proxy.sensor", path, RiskLevel.MEDIUM, EditableCapability.FULL_LIST,
                        "type=" + attr(e, "type") + ", pkg includes " + pkg));
            }
        }
    }

    private static void scanProxyBinderHits(Document doc, List<InspectorHit> out, String pkg) {
        for (Element root : elementsByTag(doc, "proxyBinder")) {
            List<Element> items = childElementsByTag(root, "descConfig");
            for (int i = 0; i < items.size(); i++) {
                Element e = items.get(i);
                String raw = attr(e, "pkg");
                if (!containsPkg(raw, pkg)) continue;
                String path = "proxyBinder/descConfig[desc=" + attr(e, "desc") + "][" + i + "]";
                out.add(new InspectorHit("proxy.binder", path, RiskLevel.MEDIUM, EditableCapability.FULL_LIST,
                        "desc=" + attr(e, "desc") + ", pkg includes " + pkg));
            }
        }
    }

    private static void scanSceneRuleHits(Document doc, List<InspectorHit> out, String tag, String pkg, RiskLevel risk) {
        List<Element> nodes = elementsByTag(doc, tag);
        for (int i = 0; i < nodes.size(); i++) {
            Element e = nodes.get(i);
            if (!pkg.equals(attr(e, "pkg"))) continue;
            String parentPath = shortContextPath(e.getParentNode());
            String path = tag + "[parentPath=" + parentPath + "][i=" + i + "]";
            String summary = "scene=" + attr(e, "scene") + ", mask=" + attr(e, "mask")
                    + ", version=" + attr(e, "version") + ", code=" + attr(e, "code");
            out.add(new InspectorHit(tag, path, risk, EditableCapability.FULL_LIST, summary));
        }
    }

    private static void scanAndroidFreezeQuotaHits(Document doc, List<InspectorHit> out, String pkg) {
        for (Element root : elementsByTag(doc, "android-freeze")) {
            String st = "enable=" + attr(root, "enable") + ", whiteEnable=" + attr(root, "whiteEnable")
                    + ", killWhiteEnable=" + attr(root, "killWhiteEnable");
            for (Element sys : childElementsByTag(root, "SysAppExemptAndroidFreezeConfig")) {
                for (Element q : childElementsByTag(sys, "ExemptQuotaConfig")) {
                    if (!pkg.equals(attr(q, "pkg"))) continue;
                    String path = "android-freeze/ExemptQuotaConfig[parentPath=" + shortContextPath(q.getParentNode()) + "]";
                    out.add(new InspectorHit("android-freeze.exemptQuota", path, RiskLevel.MEDIUM,
                            EditableCapability.FULL_LIST, "bgMode=" + attr(q, "bgMode") + ", " + st));
                }
            }
        }
    }

    private static void scanBinderHits(Document doc, List<InspectorHit> out, String pkg) {
        NodeList nl = doc.getElementsByTagName("cpnAsyncBinder");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            String rawPkg = attr(e, "pkg");
            if (!containsPkg(rawPkg, pkg)) continue;
            String path = "cpnAsyncBinder[" + i + "]";
            String summary = "type=" + attr(e, "type") + ", name=" + attr(e, "name") + ", aid=" + attr(e, "aid");
            out.add(new InspectorHit("cpnAsyncBinder", path, RiskLevel.HIGH, EditableCapability.PKG_TOKENS_ONLY, summary));
        }
    }

    private static void readServiceBumpReasons(Document doc, Set<String> out) {
        for (Element e : elementsByTag(doc, "serviceBumpUnFzReasonList")) {
            String why = attr(e, "why").trim();
            if (!why.isEmpty()) out.add(why);
        }
    }

    private static void readSceneRules(Document doc, String tag, List<SceneRule> out) {
        List<Element> nodes = elementsByTag(doc, tag);
        for (int i = 0; i < nodes.size(); i++) {
            Element e = nodes.get(i);
            out.add(new SceneRule(tag, i, attr(e, "scene"), attr(e, "pkg"), attr(e, "mask"),
                    attr(e, "version"), attr(e, "code"), shortContextPath(e.getParentNode())));
        }
    }

    private static void readCpuCtlRus(Document doc, CpuCtlRusConfig out) {
        List<Element> nodes = elementsByTag(doc, "cpuCtlRus");
        if (nodes.isEmpty()) return;
        Element e = nodes.get(nodes.size() - 1);
        org.w3c.dom.NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node n = attrs.item(i);
            out.attrs.put(n.getNodeName(), n.getNodeValue());
        }
    }

    private static void readHighLoading(Document doc, List<HighLoadingConfig> out) {
        for (Element root : elementsByTag(doc, "high-loading")) {
            HighLoadingConfig cfg = new HighLoadingConfig();
            cfg.parentPath = shortContextPath(root.getParentNode());
            Element skipWrap = firstChildByTag(root, "skipFrames");
            Element skip = skipWrap == null ? null : firstChildByTag(skipWrap, "config");
            if (skip != null) {
                cfg.skipFramesEnable = parseBooleanText(attr(skip, "enable"));
                cfg.skipFramesDuration = attr(skip, "duration");
                cfg.skipFramesCount = attr(skip, "count");
            }
            Element cpuWrap = firstChildByTag(root, "cpuloading");
            Element cpu = cpuWrap == null ? null : firstChildByTag(cpuWrap, "config");
            if (cpu != null) {
                cfg.cpuLoadingEnable = parseBooleanText(attr(cpu, "enable"));
                cfg.cpuLoadingTopCnt = attr(cpu, "loadingTopCnt");
            }
            Element th = cpuWrap == null ? null : firstChildByTag(cpuWrap, "threshold");
            if (th != null) {
                cfg.thresholdTotal = attr(th, "total");
                cfg.thresholdLittleCoreAvg = attr(th, "littleCoreAvg");
                cfg.thresholdBigCoreAvg = attr(th, "bigCoreAvg");
            }
            out.add(cfg);
        }
    }

    private static void readStrictModeSwitches(Document doc, StrictModeSwitches out) {
        List<Element> roots = elementsByTag(doc, "strictMode");
        if (roots.isEmpty()) return;
        Element root = roots.get(roots.size() - 1);
        out.strictEnable = parseBooleanText(attr(root, "enable"));
        out.version = attr(root, "version");
        for (String tag : Arrays.asList("highExtremeMode", "extremeMode", "highPerfMode", "highLoadMode")) {
            for (Element e : childElementsByTag(root, tag)) {
                StrictModeSwitches.ModeSwitch m = new StrictModeSwitches.ModeSwitch();
                m.tag = tag;
                m.level = attr(e, "level");
                m.enable = parseBooleanText(attr(e, "enable"));
                m.parentPath = shortContextPath(e.getParentNode());
                out.modes.add(m);
            }
        }
    }

    private static void readAndroidFreeze(Document doc, AndroidFreezeConfig out) {
        List<Element> roots = elementsByTag(doc, "android-freeze");
        if (roots.isEmpty()) return;
        Element root = roots.get(roots.size() - 1);
        out.enable = parseBooleanText(attr(root, "enable"));
        out.whiteEnable = parseBooleanText(attr(root, "whiteEnable"));
        out.killWhiteEnable = parseBooleanText(attr(root, "killWhiteEnable"));
        List<Element> sysNodes = childElementsByTag(root, "SysAppExemptAndroidFreezeConfig");
        if (!sysNodes.isEmpty()) {
            Element sys = sysNodes.get(0);
            out.sysAppExemptEnable = parseBooleanText(attr(sys, "enable"));
            for (Element q : childElementsByTag(sys, "ExemptQuotaConfig")) {
                AndroidFreezeConfig.ExemptQuota ex = new AndroidFreezeConfig.ExemptQuota();
                ex.pkg = attr(q, "pkg");
                ex.bgMode = attr(q, "bgMode");
                ex.parentPath = shortContextPath(q.getParentNode());
                out.exemptQuotas.add(ex);
            }
        }
    }

    private static void readAppQuotaConfig(Document doc, AppQuotaConfig out) {
        for (Element root : elementsByTag(doc, "appQuotaConfig")) {
            Element c = firstChildByTag(root, "continuousTaskCfg");
            if (c == null) continue;
            out.maxDuration = attr(c, "maxDuration");
            out.protectTimeAfterExceed = attr(c, "protectTimeAfterExceed");
            out.parentPath = shortContextPath(root.getParentNode());
        }
    }

    private static void applyV3COverlay(Document doc, OverlayV3C v3c) {
        if (v3c == null) return;
        rewriteServiceBumpReasons(doc, v3c.serviceBumpReasons);
        rewriteSceneRules(doc, "prevent", v3c.preventRules);
        rewriteSceneRules(doc, "allow", v3c.allowRules);
        rewriteCpuCtlRus(doc, v3c.cpuCtlRus);
        rewriteHighLoading(doc, v3c.highLoadingConfigs);
        rewriteStrictModeSwitches(doc, v3c.strictModeSwitches);
        rewriteAndroidFreeze(doc, v3c.androidFreezeConfig);
        rewriteAppQuotaConfig(doc, v3c.appQuotaConfig);
    }

    private static void rewriteServiceBumpReasons(Document doc, Set<String> reasons) {
        List<Element> nodes = elementsByTag(doc, "serviceBumpUnFzReasonList");
        List<Node> parents = new ArrayList<>();
        for (Element e : nodes) {
            if (!parents.contains(e.getParentNode())) parents.add(e.getParentNode());
            e.getParentNode().removeChild(e);
        }
        if (parents.isEmpty()) parents.add(doc.getDocumentElement());
        List<String> sorted = new ArrayList<>(reasons == null ? Collections.emptySet() : reasons);
        Collections.sort(sorted);
        for (Node p : parents) {
            for (String why : sorted) {
                Element n = doc.createElement("serviceBumpUnFzReasonList");
                n.setAttribute("why", why);
                p.appendChild(n);
            }
        }
    }

    private static void rewriteSceneRules(Document doc, String tag, List<SceneRule> rules) {
        List<Element> existing = elementsByTag(doc, tag);
        List<Element> parents = new ArrayList<>();
        for (Element e : existing) {
            Element p = (Element) e.getParentNode();
            if (!parents.contains(p)) parents.add(p);
        }
        Map<String, List<SceneRule>> grouped = new LinkedHashMap<>();
        for (SceneRule r : rules == null ? Collections.<SceneRule>emptyList() : rules) {
            String key = r.parentPath == null ? "" : r.parentPath;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        if (parents.isEmpty()) parents.add(doc.getDocumentElement());
        for (Element parent : parents) {
            for (Element e : new ArrayList<>(childElementsByTag(parent, tag))) parent.removeChild(e);
            String path = shortContextPath(parent);
            List<SceneRule> use = grouped.get(path);
            if (use == null) use = grouped.get("");
            if (use == null) use = Collections.emptyList();
            for (SceneRule r : use) {
                Element n = doc.createElement(tag);
                setOrRemove(n, "scene", r.scene);
                setOrRemove(n, "pkg", r.pkg);
                setOrRemove(n, "mask", r.mask);
                setOrRemove(n, "version", r.version);
                setOrRemove(n, "code", r.code);
                parent.appendChild(n);
            }
        }
    }

    private static void rewriteCpuCtlRus(Document doc, CpuCtlRusConfig cfg) {
        if (cfg == null) return;
        List<Element> nodes = elementsByTag(doc, "cpuCtlRus");
        if (nodes.isEmpty()) {
            Element n = doc.createElement("cpuCtlRus");
            doc.getDocumentElement().appendChild(n);
            nodes = elementsByTag(doc, "cpuCtlRus");
        }
        for (Element e : nodes) {
            for (Map.Entry<String, String> entry : cfg.attrs.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) continue;
                if (entry.getValue() == null) continue;
                e.setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void rewriteHighLoading(Document doc, List<HighLoadingConfig> cfgs) {
        List<Element> roots = elementsByTag(doc, "high-loading");
        if (roots.isEmpty()) {
            Element n = doc.createElement("high-loading");
            doc.getDocumentElement().appendChild(n);
            roots = elementsByTag(doc, "high-loading");
        }
        HighLoadingConfig fallback = (cfgs == null || cfgs.isEmpty()) ? new HighLoadingConfig() : cfgs.get(cfgs.size() - 1);
        for (Element root : roots) {
            String path = shortContextPath(root.getParentNode());
            HighLoadingConfig use = fallback;
            if (cfgs != null) for (HighLoadingConfig c : cfgs) if (path.equals(c.parentPath)) { use = c; break; }
            Element skipFrames = ensureChild(root, "skipFrames");
            Element skipCfg = ensureChild(skipFrames, "config");
            skipCfg.setAttribute("enable", use.skipFramesEnable ? "true" : "false");
            setOrRemove(skipCfg, "duration", use.skipFramesDuration);
            setOrRemove(skipCfg, "count", use.skipFramesCount);
            Element cpu = ensureChild(root, "cpuloading");
            Element cpuCfg = ensureChild(cpu, "config");
            cpuCfg.setAttribute("enable", use.cpuLoadingEnable ? "true" : "false");
            setOrRemove(cpuCfg, "loadingTopCnt", use.cpuLoadingTopCnt);
            Element th = ensureChild(cpu, "threshold");
            setOrRemove(th, "total", use.thresholdTotal);
            setOrRemove(th, "littleCoreAvg", use.thresholdLittleCoreAvg);
            setOrRemove(th, "bigCoreAvg", use.thresholdBigCoreAvg);
        }
    }

    private static void rewriteStrictModeSwitches(Document doc, StrictModeSwitches cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "strictMode");
        if (roots.isEmpty()) return;
        for (Element root : roots) {
            root.setAttribute("enable", cfg.strictEnable ? "true" : "false");
            setOrRemove(root, "version", cfg.version);
            for (String tag : Arrays.asList("highExtremeMode", "extremeMode", "highPerfMode", "highLoadMode")) {
                List<Element> modes = childElementsByTag(root, tag);
                for (Element e : modes) {
                    String nodeLevel = attr(e, "level");
                    StrictModeSwitches.ModeSwitch pick = null;
                    for (StrictModeSwitches.ModeSwitch m : cfg.modes) {
                        if (tag.equals(m.tag) && nodeLevel.equals(m.level)) { pick = m; break; }
                    }
                    if (pick == null) {
                        for (StrictModeSwitches.ModeSwitch m : cfg.modes) {
                            if (tag.equals(m.tag)) { pick = m; break; }
                        }
                    }
                    if (pick != null) {
                        e.setAttribute("enable", pick.enable ? "true" : "false");
                    }
                }
            }
        }
    }

    private static void rewriteAndroidFreeze(Document doc, AndroidFreezeConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "android-freeze");
        if (roots.isEmpty()) {
            Element n = doc.createElement("android-freeze");
            doc.getDocumentElement().appendChild(n);
            roots = elementsByTag(doc, "android-freeze");
        }
        for (Element root : roots) {
            root.setAttribute("enable", cfg.enable ? "true" : "false");
            root.setAttribute("whiteEnable", cfg.whiteEnable ? "true" : "false");
            root.setAttribute("killWhiteEnable", cfg.killWhiteEnable ? "true" : "false");
            Element sys = firstChildByTag(root, "SysAppExemptAndroidFreezeConfig");
            if (sys == null) { sys = doc.createElement("SysAppExemptAndroidFreezeConfig"); root.appendChild(sys); }
            sys.setAttribute("enable", cfg.sysAppExemptEnable ? "true" : "false");
            for (Element q : new ArrayList<>(childElementsByTag(sys, "ExemptQuotaConfig"))) sys.removeChild(q);
            List<AndroidFreezeConfig.ExemptQuota> list = new ArrayList<>(cfg.exemptQuotas);
            list.sort((a,b)->a.pkg.compareToIgnoreCase(b.pkg));
            for (AndroidFreezeConfig.ExemptQuota q : list) {
                if (q.pkg == null || q.pkg.trim().isEmpty()) continue;
                Element e = doc.createElement("ExemptQuotaConfig");
                e.setAttribute("pkg", q.pkg.trim());
                setOrRemove(e, "bgMode", q.bgMode);
                sys.appendChild(e);
            }
        }
    }

    private static void rewriteAppQuotaConfig(Document doc, AppQuotaConfig cfg) {
        if (cfg == null) return;
        List<Element> roots = elementsByTag(doc, "appQuotaConfig");
        if (roots.isEmpty()) {
            Element n = doc.createElement("appQuotaConfig");
            doc.getDocumentElement().appendChild(n);
            roots = elementsByTag(doc, "appQuotaConfig");
        }
        for (Element root : roots) {
            Element c = firstChildByTag(root, "continuousTaskCfg");
            if (c == null) { c = doc.createElement("continuousTaskCfg"); root.appendChild(c); }
            setOrRemove(c, "maxDuration", cfg.maxDuration);
            setOrRemove(c, "protectTimeAfterExceed", cfg.protectTimeAfterExceed);
        }
    }

    private static void setOrRemove(Element e, String name, String value) {
        if (value == null || value.trim().isEmpty()) e.removeAttribute(name);
        else e.setAttribute(name, value.trim());
    }

    private static Element ensureChild(Element parent, String tag) {
        Element child = firstChildByTag(parent, tag);
        if (child != null) return child;
        child = parent.getOwnerDocument().createElement(tag);
        parent.appendChild(child);
        return child;
    }

    private static ConcatenatedXmlParts splitConcatenated(byte[] raw) {
        if (raw == null || raw.length == 0) return new ConcatenatedXmlParts(null, raw);
        String text = new String(raw, StandardCharsets.UTF_8);
        int idx = text.lastIndexOf("<?xml");
        if (idx <= 0) idx = text.lastIndexOf("<filter-conf");
        if (idx <= 0) return new ConcatenatedXmlParts(null, raw);
        byte[] prefix = text.substring(0, idx).getBytes(StandardCharsets.UTF_8);
        byte[] active = text.substring(idx).getBytes(StandardCharsets.UTF_8);
        return new ConcatenatedXmlParts(prefix, active);
    }

    private static byte[] mergeConcatenated(ConcatenatedXmlParts parts, byte[] editedActive) {
        if (parts == null || parts.prefixBytes == null || parts.prefixBytes.length == 0) return editedActive;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(parts.prefixBytes);
            if (parts.prefixBytes[parts.prefixBytes.length - 1] != '\n') bos.write('\n');
            bos.write(editedActive);
        } catch (Exception ignored) {
            return editedActive;
        }
        return bos.toByteArray();
    }

    private static String attr(Element e, String n) {
        return e.hasAttribute(n) ? e.getAttribute(n) : "";
    }

    private static Document parseDoc(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(false);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
    }

    private static byte[] toBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer tr = tf.newTransformer();
        tr.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        tr.transform(new DOMSource(doc), new StreamResult(bos));
        return bos.toByteArray();
    }
}
