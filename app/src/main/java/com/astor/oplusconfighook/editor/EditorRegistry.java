package com.astor.oplusconfighook.editor;

import com.astor.oplusconfighook.EditableListEditorBottomSheet;
import com.astor.oplusconfighook.TombstoneViewModel;
import com.astor.oplusconfighook.TombstoneXmlEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EditorRegistry {
    public static final String RK_SERVICE_BUMP = "V3C:SERVICE_BUMP_REASONS";
    public static final String RK_PREVENT = "V3C:SCENE_PREVENT";
    public static final String RK_ALLOW = "V3C:SCENE_ALLOW";
    public static final String RK_ANDROID_FREEZE = "V3C:ANDROID_FREEZE";
    public static final String RK_APP_QUOTA = "V3C:APP_QUOTA";
    public static final String RK_HIGH_LOADING = "V3C:HIGH_LOADING";
    public static final String RK_CPU_CTL_RUS = "V3C:CPU_CTL_RUS";
    public static final String RK_PROXY_DEFAULT_BC = "PROXY:DEFAULT_BC";
    public static final String RK_PROXY_SENSOR_TYPES = "PROXY:SENSOR_TYPES";
    public static final String RK_STRICT_SWITCH = "V3C:STRICT_SWITCH";
    public static final String RK_PKGCONFIG_PREFIX = "PKGCONFIG:";
    public static final String RK_BINDER_PREFIX = "BINDER:";

    private static final Map<String, EditorSpec> EXACT = new LinkedHashMap<>();

    static {
        EXACT.put(RK_SERVICE_BUMP, new PlainListSpec("每行一个原因（如 bind/create/start），可自定义"));
        EXACT.put(RK_PROXY_DEFAULT_BC, new PlainListSpec("每行一个 token；支持粘贴多行；会去重"));
        EXACT.put(RK_PROXY_SENSOR_TYPES, new PlainListSpec("每行一个 token；支持粘贴多行；会去重"));
        EXACT.put(RK_PREVENT, new SceneRuleSpec("必须形如 scene,pkg,mask,version,code（pkg 必填）。示例：scene,com.example.app,mask,version,code", "prevent"));
        EXACT.put(RK_ALLOW, new SceneRuleSpec("必须形如 scene,pkg,mask,version,code（pkg 必填）。示例：scene,com.example.app,mask,version,code", "allow"));
        EXACT.put(RK_CPU_CTL_RUS, new CpuCtlSpec());
        EXACT.put(RK_HIGH_LOADING, new HighLoadingSpec());
        EXACT.put(RK_APP_QUOTA, new AppQuotaSpec());
        EXACT.put(RK_STRICT_SWITCH, new StrictSwitchSpec());
        EXACT.put(RK_ANDROID_FREEZE, new AndroidFreezeSpec());
    }

    private EditorRegistry() {}

    public static EditorSpec get(String requestKey) {
        EditorSpec exact = EXACT.get(requestKey);
        if (exact != null) return exact;
        if (requestKey != null && requestKey.startsWith(RK_PKGCONFIG_PREFIX)) {
            return new PlainPkgTokenSpec("每行一个 token；支持粘贴多行；会去重");
        }
        if (requestKey != null && requestKey.startsWith(RK_BINDER_PREFIX)) {
            return new BinderTokenSpec("每行一个 token；支持粘贴多行；会去重");
        }
        return new PlainListSpec(null);
    }

    private static class PlainListSpec extends EditorSpec.Base {
        private final String desc;

        private PlainListSpec(String desc) {
            this.desc = desc;
        }

        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_PLAIN_LIST; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) { return desc; }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            if (RK_SERVICE_BUMP.equals(requestKey)) return new ArrayList<>(vm.stagedServiceBumpReasons);
            if (RK_PROXY_DEFAULT_BC.equals(requestKey)) return new ArrayList<>(vm.stagedDefaultProxyBcActions);
            if (RK_PROXY_SENSOR_TYPES.equals(requestKey)) return new ArrayList<>(vm.stagedProxySensorConfig.proxyTypeNames);
            return new ArrayList<>();
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) { return ValidationResult.ok(); }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            List<String> cleaned = clean(lines);
            if (RK_SERVICE_BUMP.equals(requestKey)) {
                vm.stagedServiceBumpReasons = new LinkedHashSet<>(cleaned);
                return ApplyResult.ok();
            }
            if (RK_PROXY_DEFAULT_BC.equals(requestKey)) {
                vm.stagedDefaultProxyBcActions = new ArrayList<>(new LinkedHashSet<>(cleaned));
                return ApplyResult.ok();
            }
            if (RK_PROXY_SENSOR_TYPES.equals(requestKey)) {
                vm.stagedProxySensorConfig.proxyTypeNames.clear();
                vm.stagedProxySensorConfig.proxyTypeNames.addAll(new LinkedHashSet<>(cleaned));
                return ApplyResult.ok();
            }
            return ApplyResult.fail("unsupported requestKey=" + requestKey);
        }
    }

    private static final class PlainPkgTokenSpec extends PlainListSpec {
        private PlainPkgTokenSpec(String desc) { super(desc); }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            String category = requestKey.substring(RK_PKGCONFIG_PREFIX.length());
            List<String> old = vm.stagedPkgConfigTokens.get(category);
            return old == null ? new ArrayList<>() : new ArrayList<>(old);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            String category = requestKey.substring(RK_PKGCONFIG_PREFIX.length());
            vm.stagedPkgConfigTokens.put(category, new ArrayList<>(new LinkedHashSet<>(clean(lines))));
            return ApplyResult.ok();
        }
    }

    private static final class BinderTokenSpec extends PlainListSpec {
        private BinderTokenSpec(String desc) { super(desc); }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            int idx = Integer.parseInt(requestKey.substring(RK_BINDER_PREFIX.length()));
            for (TombstoneXmlEditor.BinderRuleItem item : vm.stagedBinderRules) {
                if (item.index == idx) return new ArrayList<>(item.pkgTokens);
            }
            return new ArrayList<>();
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            int idx = Integer.parseInt(requestKey.substring(RK_BINDER_PREFIX.length()));
            List<String> tokens = new ArrayList<>(new LinkedHashSet<>(clean(lines)));
            for (int i = 0; i < vm.stagedBinderRules.size(); i++) {
                TombstoneXmlEditor.BinderRuleItem item = vm.stagedBinderRules.get(i);
                if (item.index == idx) {
                    vm.stagedBinderRules.set(i, item.copyWithPkgTokens(tokens));
                    return ApplyResult.ok();
                }
            }
            return ApplyResult.fail("binder rule not found idx=" + idx);
        }
    }

    private static final class SceneRuleSpec extends EditorSpec.Base {
        private final String desc;
        private final String tag;

        private SceneRuleSpec(String desc, String tag) {
            this.desc = desc;
            this.tag = tag;
        }

        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_CSV_RULE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) { return desc; }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            List<TombstoneXmlEditor.SceneRule> list = "prevent".equals(tag) ? vm.stagedPreventRules : vm.stagedAllowRules;
            List<String> out = new ArrayList<>();
            for (TombstoneXmlEditor.SceneRule r : list) out.add(r.scene + "," + r.pkg + "," + r.mask + "," + r.version + "," + r.code);
            return out;
        }

        private ParseSceneResult parse(List<String> lines) {
            List<String> errors = new ArrayList<>();
            List<TombstoneXmlEditor.SceneRule> out = new ArrayList<>();
            List<String> cleaned = clean(lines);
            for (int i = 0; i < cleaned.size(); i++) {
                String line = cleaned.get(i);
                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    errors.add("第" + (i + 1) + "行 CSV 列数不足（至少2列）：" + line);
                    continue;
                }
                String pkg = parts[1].trim();
                if (pkg.isEmpty()) {
                    errors.add("第" + (i + 1) + "行 pkg 不能为空：" + line);
                    continue;
                }
                String scene = parts.length > 0 ? parts[0].trim() : "";
                String mask = parts.length > 2 ? parts[2].trim() : "";
                String version = parts.length > 3 ? parts[3].trim() : "";
                String code = parts.length > 4 ? parts[4].trim() : "";
                out.add(new TombstoneXmlEditor.SceneRule(tag, i, scene, pkg, mask, version, code, ""));
            }
            return new ParseSceneResult(out, errors);
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseSceneResult r = parse(lines);
            return r.errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(r.errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ParseSceneResult r = parse(lines);
            if (!r.errors.isEmpty()) return ApplyResult.fail(r.errors.get(0));
            if ("prevent".equals(tag)) vm.stagedPreventRules = r.rules; else vm.stagedAllowRules = r.rules;
            return ApplyResult.ok();
        }

        private static final class ParseSceneResult {
            final List<TombstoneXmlEditor.SceneRule> rules;
            final List<String> errors;

            private ParseSceneResult(List<TombstoneXmlEditor.SceneRule> rules, List<String> errors) {
                this.rules = rules;
                this.errors = errors;
            }
        }
    }

    private static final class CpuCtlSpec extends EditorSpec.Base {
        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_KEY_VALUE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) {
            String sample = vm.stagedCpuCtlRus.attrs.isEmpty()
                    ? "shortCommCpuRateKill=80"
                    : vm.stagedCpuCtlRus.attrs.entrySet().iterator().next().getKey() + "=" + vm.stagedCpuCtlRus.attrs.entrySet().iterator().next().getValue();
            return "必须形如 key=value（示例：" + sample + "）";
        }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, String> e : vm.stagedCpuCtlRus.attrs.entrySet()) out.add(e.getKey() + "=" + e.getValue());
            return out;
        }

        private ParseKeyValueResult parse(List<String> lines) {
            Map<String, String> attrs = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            List<String> cleaned = clean(lines);
            for (int i = 0; i < cleaned.size(); i++) {
                String line = cleaned.get(i);
                int idx = line.indexOf('=');
                if (idx < 0) {
                    errors.add("第" + (i + 1) + "行缺少 =：" + line);
                    continue;
                }
                String key = line.substring(0, idx).trim();
                if (key.isEmpty()) {
                    errors.add("第" + (i + 1) + "行 key 为空：" + line);
                    continue;
                }
                attrs.put(key, line.substring(idx + 1).trim());
            }
            return new ParseKeyValueResult(attrs, errors);
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            return r.errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(r.errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            if (!r.errors.isEmpty()) return ApplyResult.fail(r.errors.get(0));
            vm.stagedCpuCtlRus.attrs.clear();
            vm.stagedCpuCtlRus.attrs.putAll(r.attrs);
            return ApplyResult.ok();
        }
    }

    private static final class HighLoadingSpec extends EditorSpec.Base {
        private static final Set<String> KEYS = new LinkedHashSet<>(Arrays.asList("skipEnable", "skipDuration", "skipCount", "cpuEnable", "cpuTopCnt", "total", "littleCoreAvg", "bigCoreAvg"));

        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_KEY_VALUE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) { return "high-loading 映射：skip* -> <skipFrames/config>; cpuEnable/cpuTopCnt -> <cpuloading/config>; total/littleCoreAvg/bigCoreAvg -> <cpuloading/threshold>"; }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            TombstoneXmlEditor.HighLoadingConfig cfg = vm.stagedHighLoadingConfigs.isEmpty() ? new TombstoneXmlEditor.HighLoadingConfig() : vm.stagedHighLoadingConfigs.get(0);
            List<String> lines = new ArrayList<>();
            lines.add("skipEnable=" + cfg.skipFramesEnable);
            lines.add("skipDuration=" + cfg.skipFramesDuration);
            lines.add("skipCount=" + cfg.skipFramesCount);
            lines.add("cpuEnable=" + cfg.cpuLoadingEnable);
            lines.add("cpuTopCnt=" + cfg.cpuLoadingTopCnt);
            lines.add("total=" + cfg.thresholdTotal);
            lines.add("littleCoreAvg=" + cfg.thresholdLittleCoreAvg);
            lines.add("bigCoreAvg=" + cfg.thresholdBigCoreAvg);
            return lines;
        }

        private ParseKeyValueResult parse(List<String> lines) {
            ParseKeyValueResult base = parseKeyValue(lines);
            List<String> errors = new ArrayList<>(base.errors);
            for (String key : base.attrs.keySet()) {
                if (!KEYS.contains(key)) errors.add("未知 key: " + key);
            }
            return new ParseKeyValueResult(base.attrs, errors);
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            return r.errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(r.errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            if (!r.errors.isEmpty()) return ApplyResult.fail(r.errors.get(0));
            TombstoneXmlEditor.HighLoadingConfig base = vm.stagedHighLoadingConfigs.isEmpty() ? new TombstoneXmlEditor.HighLoadingConfig() : vm.stagedHighLoadingConfigs.get(0);
            TombstoneXmlEditor.HighLoadingConfig out = new TombstoneXmlEditor.HighLoadingConfig();
            out.skipFramesEnable = getBool(r.attrs, "skipEnable", base.skipFramesEnable);
            out.skipFramesDuration = getString(r.attrs, "skipDuration", base.skipFramesDuration);
            out.skipFramesCount = getString(r.attrs, "skipCount", base.skipFramesCount);
            out.cpuLoadingEnable = getBool(r.attrs, "cpuEnable", base.cpuLoadingEnable);
            out.cpuLoadingTopCnt = getString(r.attrs, "cpuTopCnt", base.cpuLoadingTopCnt);
            out.thresholdTotal = getString(r.attrs, "total", base.thresholdTotal);
            out.thresholdLittleCoreAvg = getString(r.attrs, "littleCoreAvg", base.thresholdLittleCoreAvg);
            out.thresholdBigCoreAvg = getString(r.attrs, "bigCoreAvg", base.thresholdBigCoreAvg);
            vm.stagedHighLoadingConfigs.clear();
            vm.stagedHighLoadingConfigs.add(out);
            return ApplyResult.ok();
        }
    }

    private static final class AppQuotaSpec extends EditorSpec.Base {
        private static final Set<String> KEYS = new LinkedHashSet<>(Arrays.asList("maxDuration", "protectTimeAfterExceed"));

        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_KEY_VALUE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) {
            return "必须形如 key=value（示例：maxDuration=" + vm.stagedAppQuotaConfig.maxDuration + "）";
        }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            List<String> lines = new ArrayList<>();
            lines.add("maxDuration=" + vm.stagedAppQuotaConfig.maxDuration);
            lines.add("protectTimeAfterExceed=" + vm.stagedAppQuotaConfig.protectTimeAfterExceed);
            return lines;
        }

        private ParseKeyValueResult parse(List<String> lines) {
            ParseKeyValueResult base = parseKeyValue(lines);
            List<String> errors = new ArrayList<>(base.errors);
            for (String key : base.attrs.keySet()) if (!KEYS.contains(key)) errors.add("未知 key: " + key);
            return new ParseKeyValueResult(base.attrs, errors);
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            return r.errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(r.errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ParseKeyValueResult r = parse(lines);
            if (!r.errors.isEmpty()) return ApplyResult.fail(r.errors.get(0));
            vm.stagedAppQuotaConfig.maxDuration = getString(r.attrs, "maxDuration", vm.stagedAppQuotaConfig.maxDuration);
            vm.stagedAppQuotaConfig.protectTimeAfterExceed = getString(r.attrs, "protectTimeAfterExceed", vm.stagedAppQuotaConfig.protectTimeAfterExceed);
            return ApplyResult.ok();
        }
    }

    private static final class StrictSwitchSpec extends EditorSpec.Base {
        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_KEY_VALUE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) { return "必须形如 key=value（示例：strictEnable=true）"; }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            return new ArrayList<>(Arrays.asList("strictEnable=" + vm.stagedStrictModeSwitches.strictEnable));
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseKeyValueResult kv = parseKeyValue(lines);
            List<String> errors = new ArrayList<>(kv.errors);
            for (Map.Entry<String, String> e : kv.attrs.entrySet()) {
                if (!"strictEnable".equals(e.getKey())) errors.add("未知 key: " + e.getKey());
                String v = e.getValue().toLowerCase(Locale.ROOT);
                if (!"true".equals(v) && !"false".equals(v)) errors.add("strictEnable 必须为 true/false");
            }
            return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ValidationResult vr = validate(lines, requestKey);
            if (!vr.ok) return ApplyResult.fail(vr.errors.get(0));
            ParseKeyValueResult kv = parseKeyValue(lines);
            if (kv.attrs.containsKey("strictEnable")) {
                vm.stagedStrictModeSwitches.strictEnable = "true".equalsIgnoreCase(kv.attrs.get("strictEnable"));
            }
            return ApplyResult.ok();
        }
    }

    private static final class AndroidFreezeSpec extends EditorSpec.Base {
        @Override
        public String mode() { return EditableListEditorBottomSheet.MODE_ANDROID_FREEZE; }

        @Override
        public String description(TombstoneViewModel vm, String requestKey) {
            return "本页支持两种格式：\n1) 开关键：enable=true（支持 enable/whiteEnable/killWhiteEnable/sysAppExemptEnable）\n2) 豁免配额：pkg 或 pkg,bgMode";
        }

        @Override
        public List<String> serialize(TombstoneViewModel vm, String requestKey) {
            List<String> lines = new ArrayList<>();
            lines.add("enable=" + vm.stagedAndroidFreezeConfig.enable);
            lines.add("whiteEnable=" + vm.stagedAndroidFreezeConfig.whiteEnable);
            lines.add("killWhiteEnable=" + vm.stagedAndroidFreezeConfig.killWhiteEnable);
            lines.add("sysAppExemptEnable=" + vm.stagedAndroidFreezeConfig.sysAppExemptEnable);
            for (TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota ex : vm.stagedAndroidFreezeConfig.exemptQuotas) {
                lines.add(ex.pkg + (ex.bgMode == null || ex.bgMode.trim().isEmpty() ? "" : "," + ex.bgMode));
            }
            return lines;
        }

        @Override
        public ValidationResult validate(List<String> lines, String requestKey) {
            ParseFreezeResult parsed = parse(lines);
            return parsed.errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(parsed.errors);
        }

        @Override
        public ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey) {
            ParseFreezeResult p = parse(lines);
            if (!p.errors.isEmpty()) return ApplyResult.fail(p.errors.get(0));
            vm.stagedAndroidFreezeConfig.enable = p.enable != null ? p.enable : vm.stagedAndroidFreezeConfig.enable;
            vm.stagedAndroidFreezeConfig.whiteEnable = p.whiteEnable != null ? p.whiteEnable : vm.stagedAndroidFreezeConfig.whiteEnable;
            vm.stagedAndroidFreezeConfig.killWhiteEnable = p.killWhiteEnable != null ? p.killWhiteEnable : vm.stagedAndroidFreezeConfig.killWhiteEnable;
            vm.stagedAndroidFreezeConfig.sysAppExemptEnable = p.sysAppExemptEnable != null ? p.sysAppExemptEnable : vm.stagedAndroidFreezeConfig.sysAppExemptEnable;
            vm.stagedAndroidFreezeConfig.exemptQuotas.clear();
            vm.stagedAndroidFreezeConfig.exemptQuotas.addAll(p.exemptQuotas);
            return ApplyResult.ok();
        }

        private ParseFreezeResult parse(List<String> lines) {
            ParseFreezeResult out = new ParseFreezeResult();
            List<String> cleaned = clean(lines);
            for (int i = 0; i < cleaned.size(); i++) {
                String line = cleaned.get(i);
                int lineNo = i + 1;
                if (line.contains("=")) {
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim().toLowerCase(Locale.ROOT);
                    if (!"enable".equals(key)
                            && !"whiteEnable".equals(key)
                            && !"killWhiteEnable".equals(key)
                            && !"sysAppExemptEnable".equals(key)) {
                        out.errors.add("第" + lineNo + "行：未知开关键（" + key + "）");
                        continue;
                    }
                    if (!"true".equals(value) && !"false".equals(value)) {
                        out.errors.add("第" + lineNo + "行：开关值必须为 true/false（当前：" + value + "）");
                        continue;
                    }
                    boolean bool = "true".equals(value);
                    if ("enable".equals(key)) out.enable = bool;
                    if ("whiteEnable".equals(key)) out.whiteEnable = bool;
                    if ("killWhiteEnable".equals(key)) out.killWhiteEnable = bool;
                    if ("sysAppExemptEnable".equals(key)) out.sysAppExemptEnable = bool;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length > 2) {
                    out.errors.add("第" + lineNo + "行：豁免配额格式错误（仅支持 pkg 或 pkg,bgMode）：" + line);
                    continue;
                }
                String pkg = parts[0].trim();
                if (pkg.isEmpty() || pkg.contains("=") || pkg.contains(" ") || !pkg.contains(".")) {
                    out.errors.add("第" + lineNo + "行：豁免配额行 pkg 非法（当前：" + pkg + "）");
                    continue;
                }
                String bgMode = parts.length == 2 ? parts[1].trim() : "";
                if (!bgMode.isEmpty() && !bgMode.matches("^[0-9]+$")) {
                    out.errors.add("第" + lineNo + "行：豁免配额行 bgMode 必须为数字（当前：" + bgMode + "）");
                    continue;
                }
                TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota ex = new TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota();
                ex.pkg = pkg;
                ex.bgMode = bgMode;
                out.exemptQuotas.add(ex);
            }
            return out;
        }

        private static final class ParseFreezeResult {
            final List<String> errors = new ArrayList<>();
            final List<TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota> exemptQuotas = new ArrayList<>();
            Boolean enable;
            Boolean whiteEnable;
            Boolean killWhiteEnable;
            Boolean sysAppExemptEnable;
        }
    }

    private static final class ParseKeyValueResult {
        final Map<String, String> attrs;
        final List<String> errors;

        private ParseKeyValueResult(Map<String, String> attrs, List<String> errors) {
            this.attrs = attrs;
            this.errors = errors;
        }
    }

    private static ParseKeyValueResult parseKeyValue(List<String> lines) {
        List<String> cleaned = EditorSpec.Base.clean(lines);
        Map<String, String> attrs = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            String line = cleaned.get(i);
            int idx = line.indexOf('=');
            if (idx < 0) {
                errors.add("第" + (i + 1) + "行缺少 =：" + line);
                continue;
            }
            String key = line.substring(0, idx).trim();
            if (key.isEmpty()) {
                errors.add("第" + (i + 1) + "行 key 为空：" + line);
                continue;
            }
            attrs.put(key, line.substring(idx + 1).trim());
        }
        return new ParseKeyValueResult(attrs, errors);
    }

    private static String getString(Map<String, String> attrs, String key, String fallback) {
        return attrs.containsKey(key) ? attrs.get(key) : fallback;
    }

    private static boolean getBool(Map<String, String> attrs, String key, boolean fallback) {
        if (!attrs.containsKey(key)) return fallback;
        return "true".equalsIgnoreCase(attrs.get(key));
    }
}
