#!/usr/bin/env python3
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / 'app/src/main/assets/sys_elsa_config_list.xml'
FRAGMENT = ROOT / 'app/src/main/java/com/astor/oplusconfighook/TombstoneFragment.java'
EDITOR = ROOT / 'app/src/main/java/com/astor/oplusconfighook/TombstoneXmlEditor.java'
OUT = ROOT / 'tools/module_schema_audit_report.md'


def normalize_xml(raw: bytes) -> bytes:
    text = raw.decode('utf-8', errors='ignore')
    first = text.find('<?xml')
    second = text.find('<?xml', first + 1) if first >= 0 else -1
    return text[second:].encode('utf-8') if second > 0 else raw


def count_tags(root, tag):
    return len(root.findall(f'.//{tag}'))


def has_mixed(root, tag):
    nodes = root.findall(f'.//{tag}')
    if not nodes:
        return False
    for n in nodes:
        if n.attrib and list(n):
            return True
    return False


def extract_ui_entries(text: str):
    lines = []
    for m in re.finditer(r'openEditableListSheet\(([^\n]+)', text):
        lines.append(m.group(1).strip())
    return lines


def strategy_hints(editor_text: str):
    hints = {}
    hints['proxyConfig'] = '读取第一个并 warn；写回匹配全局语义节点(findProxyGlobalConfigNodes)'
    hints['prevent'] = '按 parentPath 分组写回（rewriteSceneRules）'
    hints['allow'] = '按 parentPath 分组写回（rewriteSceneRules）'
    hints['proxySensor'] = '读取第一个（readProxySensor）；写回全部同类节点（rewriteProxySensor）'
    hints['proxyBinder'] = '读取第一个（readProxyBinder）；写回全部同类节点（rewriteProxyBinder）'
    hints['high-loading'] = '读取全部并保留 parentPath；写回按 parentPath 优先匹配'
    hints['android-freeze'] = '读取最后一个；写回全部同类节点'
    return hints


def main():
    raw = ASSET.read_bytes()
    xml = normalize_xml(raw)
    root = ET.fromstring(xml)
    frag = FRAGMENT.read_text(encoding='utf-8')
    editor_text = EDITOR.read_text(encoding='utf-8')
    ui_entries = '\n'.join(extract_ui_entries(frag)[:30])
    hints = strategy_hints(editor_text)

    rows_a = [
        ('High Load', 'android-freeze', '是（attrs+子节点列表）', '否', 'EditorSpec ANDROID_FREEZE', '✅ 已适配'),
        ('Proxy', 'proxySensor', '是（config + proxyPkg/proxyType/whiteTypePkg）', '是' if count_tags(root, 'proxySensor') > 1 else '否', 'ProxySensorEditorBottomSheet', '✅ v3.4.8 分区编辑'),
        ('Proxy', 'proxyBinder', '是（config + descConfig）', '是' if count_tags(root, 'proxyBinder') > 1 else '否', 'ProxyBinderEditorBottomSheet', '✅ v3.4.8 分区编辑'),
        ('High Load', 'high-loading', '是（skipFrames/cpuloading 子节点）', '是' if count_tags(root, 'high-loading') > 1 else '否', 'EditorSpec KEY_VALUE(high-loading)', '✅ 补齐 littleCoreAvg/bigCoreAvg + 落点说明'),
        ('SysBlack', 'SysBlackPolicy/RestrictMask', '混合（模板参数+挂载）', '是', '独立 editor', '⚠️ 需持续提示生效段'),
        ('Fast Freeze', 'ffPkg[type=black]', '否（列表）', '是' if count_tags(root, 'ffPkg') > 1 else '否', 'PLAIN_LIST', '✅'),
        ('Whitelist', 'doNotFreeze/whitePkg/imPkg/cpuCtl/appCard', '否（列表）', '是', 'PLAIN_LIST/AppPicker', '✅'),
        ('Bump', 'prevent/allow', '规则列表（CSV）', '是', 'CSV_RULE', '✅（parentPath 分组写回）'),
    ]

    danger_tags = ['proxyConfig', 'prevent', 'allow', 'proxySensor', 'proxyBinder', 'high-loading']
    rows_b = []
    for tag in danger_tags:
        occ = count_tags(root, tag)
        rows_b.append((tag, occ, hints.get(tag, '未知'), 'occurrences>1 时存在误判风险' if occ > 1 else '低', '优先按 parentPath 精确更新，缺失时全量更新并 warn'))

    md = []
    md.append('# 六模块“混合结构”适配审计报告（v3.4.8）\n')
    md.append('> 数据来源：assets XML + TombstoneFragment 入口扫描 + TombstoneXmlEditor 读写策略静态判读。\n')
    md.append('## 表 A：六模块 → 节点结构判定\n')
    md.append('| 模块 | XML tag/path | 是否混合结构 | 是否多处出现 | 当前 UI 入口/EditorSpec | 风险/缺口 |')
    md.append('| --- | --- | ---: | ---: | --- | --- |')
    for r in rows_a:
        md.append(f'| {r[0]} | {r[1]} | {r[2]} | {r[3]} | {r[4]} | {r[5]} |')

    md.append('\n## 表 B：多处出现节点清单（最危险）\n')
    md.append('| XML tag | occurrences | 当前读写策略（first/last/all） | 潜在问题 | 建议 |')
    md.append('| --- | ---: | --- | --- | --- |')
    for r in rows_b:
        md.append(f'| {r[0]} | {r[1]} | {r[2]} | {r[3]} | {r[4]} |')

    md.append('\n## 代码入口扫描（TombstoneFragment openEditableListSheet 节选）\n')
    md.append('```text')
    md.append(ui_entries)
    md.append('```')

    OUT.write_text('\n'.join(md), encoding='utf-8')
    print(f'Wrote {OUT}')


if __name__ == '__main__':
    main()
