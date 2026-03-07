# 六模块“混合结构”适配审计报告（v3.4.8）

> 数据来源：assets XML + TombstoneFragment 入口扫描 + TombstoneXmlEditor 读写策略静态判读。

## 表 A：六模块 → 节点结构判定

| 模块 | XML tag/path | 是否混合结构 | 是否多处出现 | 当前 UI 入口/EditorSpec | 风险/缺口 |
| --- | --- | ---: | ---: | --- | --- |
| High Load | android-freeze | 是（attrs+子节点列表） | 否 | EditorSpec ANDROID_FREEZE | ✅ 已适配 |
| Proxy | proxySensor | 是（config + proxyPkg/proxyType/whiteTypePkg） | 否 | ProxySensorEditorBottomSheet | ✅ v3.4.8 分区编辑 |
| Proxy | proxyBinder | 是（config + descConfig） | 否 | ProxyBinderEditorBottomSheet | ✅ v3.4.8 分区编辑 |
| High Load | high-loading | 是（skipFrames/cpuloading 子节点） | 否 | EditorSpec KEY_VALUE(high-loading) | ✅ 补齐 littleCoreAvg/bigCoreAvg + 落点说明 |
| SysBlack | SysBlackPolicy/RestrictMask | 混合（模板参数+挂载） | 是 | 独立 editor | ⚠️ 需持续提示生效段 |
| Fast Freeze | ffPkg[type=black] | 否（列表） | 是 | PLAIN_LIST | ✅ |
| Whitelist | doNotFreeze/whitePkg/imPkg/cpuCtl/appCard | 否（列表） | 是 | PLAIN_LIST/AppPicker | ✅ |
| Bump | prevent/allow | 规则列表（CSV） | 是 | CSV_RULE | ✅（parentPath 分组写回） |

## 表 B：多处出现节点清单（最危险）

| XML tag | occurrences | 当前读写策略（first/last/all） | 潜在问题 | 建议 |
| --- | ---: | --- | --- | --- |
| proxyConfig | 2 | 读取第一个并 warn；写回匹配全局语义节点(findProxyGlobalConfigNodes) | occurrences>1 时存在误判风险 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |
| prevent | 4 | 按 parentPath 分组写回（rewriteSceneRules） | occurrences>1 时存在误判风险 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |
| allow | 0 | 按 parentPath 分组写回（rewriteSceneRules） | 低 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |
| proxySensor | 1 | 读取第一个（readProxySensor）；写回全部同类节点（rewriteProxySensor） | 低 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |
| proxyBinder | 1 | 读取第一个（readProxyBinder）；写回全部同类节点（rewriteProxyBinder） | 低 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |
| high-loading | 1 | 读取全部并保留 parentPath；写回按 parentPath 优先匹配 | 低 | 优先按 parentPath 精确更新，缺失时全量更新并 warn |

## 代码入口扫描（TombstoneFragment openEditableListSheet 节选）

```text
RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null), false);
"prevent".equals(tag) ? RK_PREVENT : RK_ALLOW, tag, null, false, null);
RK_ANDROID_FREEZE, getString(R.string.v3c_android_freeze_title), null, false, null);
RK_APP_QUOTA, getString(R.string.v3c_app_quota_title), null, false, null);
RK_HIGH_LOADING, getString(R.string.v3c_high_loading_title), null, false, null);
RK_CPU_CTL_RUS, getString(R.string.v3c_cpu_ctl_rus_title), null, false, null);
RK_PKGCONFIG_PREFIX + category,
RK_BINDER_PREFIX + ruleIndex,
resolveProxyRequestKey(titleText), titleText, explain, false, null));
String requestKey, String title, @Nullable String description, boolean enablePickApps, @Nullable String pickAppsLabel) {
RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null));
RK_PROXY_DEFAULT_BC, getString(R.string.proxy_default_bc_title), getString(R.string.proxy_default_bc_explain), false, null)));
RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null));
RK_STRICT_SWITCH, getString(R.string.module_menu_highload_strict), null, false, null);
```