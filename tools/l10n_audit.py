#!/usr/bin/env python3
import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

BASE_FILE = Path("app/src/main/res/values/strings.xml")
LOCALES = {
    "en": Path("app/src/main/res/values-en/strings.xml"),
    "ja": Path("app/src/main/res/values-ja/strings.xml"),
    "ko": Path("app/src/main/res/values-ko/strings.xml"),
    "zh-rCN": Path("app/src/main/res/values-zh-rCN/strings.xml"),
}
SCAN_DIRS = [Path("app/src/main/java"), Path("app/src/main/res/layout")]

# Allowed technical tokens that may contain Chinese-origin terms in identifiers.
ALLOWLIST_TOKENS = {
    "hans", "gms", "lcdon", "lcdoff", "night", "proxyPkg", "proxyType", "whiteTypePkg",
    "SysBlackPolicy", "SysBlackApp", "defaultProxyBc", "serviceBumpUnFzReasonList", "ffPkg.black"
}

CJK_RE = re.compile(r"[\u4e00-\u9fff]")
ZH_STYLE_PUNCT_RE = re.compile(r"[；“”《》]")

FALLBACK_PREFIXES = (
    "proxy_",
    "module_proxy",
    "module_menu_proxy",
    "module_detail_proxy",
    "section_",
    "policy_",
    "scenario_",
    "quick_",
)
FALLBACK_ALLOW_KEYS = {
    "scene_token_lcdon",
    "scene_token_lcdoff",
    "scene_token_night",
    "quick_suppress_timing_profile_l1_short",
    "quick_suppress_timing_profile_l2_short",
    "quick_suppress_timing_profile_l3_short",
    "policy_technical_detail",
    "proxy_config_enable",
}


def load_strings(path: Path) -> dict[str, str]:
    root = ET.parse(path).getroot()
    return {n.attrib["name"]: (n.text or "").strip() for n in root.findall("string") if "name" in n.attrib}


def check_missing_keys(base: dict[str, str], locale_values: dict[str, dict[str, str]]) -> list[tuple[str, str]]:
    misses: list[tuple[str, str]] = []
    base_keys = set(base)
    for locale, values in locale_values.items():
        for key in sorted(base_keys - set(values)):
            misses.append((locale, key))
    return misses


def is_token_allowed(text: str) -> bool:
    compact = re.sub(r"[\s=|,.:;_\-()/\[\]]+", "", text)
    if not compact:
        return True
    for tok in ALLOWLIST_TOKENS:
        compact = compact.replace(re.sub(r"[\s=|,.:;_\-()/\[\]]+", "", tok), "")
    return compact == ""


def check_cjk_leakage(locale_values: dict[str, dict[str, str]]) -> list[tuple[str, str, str]]:
    leaks: list[tuple[str, str, str]] = []
    zh_values = locale_values["zh-rCN"]
    for locale in ("en", "ja", "ko"):
        for key, value in locale_values[locale].items():
            if not value:
                continue
            # en: any CJK is suspicious unless token-only allowlist.
            if locale == "en":
                if CJK_RE.search(value) and not is_token_allowed(value):
                    leaks.append((locale, key, value))
            else:
                # ja/ko: flag likely Chinese fallback by exact zh copy or zh-style punctuation.
                if ((key in zh_values and value == zh_values[key] and CJK_RE.search(value) and len(value) >= 4)
                        or ZH_STYLE_PUNCT_RE.search(value)):
                    leaks.append((locale, key, value))
    return leaks


def scan_hardcoded_user_strings() -> list[tuple[str, int, str]]:
    issues: list[tuple[str, int, str]] = []
    java_literal = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')
    layout_text = re.compile(r'android:text\s*=\s*"([^"]+)"')
    java_ui_line = re.compile(r"Toast\.makeText|UiNotifier\.showMessage|\.setText\(|\.setTitle\(|\.setMessage\(")

    for base in SCAN_DIRS:
        for path in sorted(base.rglob("*.java" if "java" in str(base) else "*.xml")):
            text = path.read_text(encoding="utf-8")
            lines = text.splitlines()
            if path.suffix == ".java":
                for i, line in enumerate(lines, 1):
                    if not java_ui_line.search(line):
                        continue
                    for m in java_literal.finditer(line):
                        lit = m.group(1).strip()
                        if not lit:
                            continue
                        if "http" in lit or lit.startswith("TAG"):
                            continue
                        if CJK_RE.search(lit):
                            issues.append((path.as_posix(), i, lit))
            else:
                for i, line in enumerate(lines, 1):
                    m = layout_text.search(line)
                    if not m:
                        continue
                    lit = m.group(1).strip()
                    if lit.startswith("@string/"):
                        continue
                    if CJK_RE.search(lit):
                        issues.append((path.as_posix(), i, lit))
    return issues


def check_english_fallback(locale_values: dict[str, dict[str, str]], en_values: dict[str, str]) -> list[tuple[str, str, str, str]]:
    falls: list[tuple[str, str, str, str]] = []
    for locale in ("ja", "ko"):
        values = locale_values[locale]
        for key, value in values.items():
            if key in FALLBACK_ALLOW_KEYS:
                continue
            if not key.startswith(FALLBACK_PREFIXES):
                continue
            en_value = en_values.get(key)
            if not en_value:
                continue
            if value.strip() == en_value.strip():
                falls.append((locale, key, value, en_value))
    return falls


def write_report(path: Path, missing, leakage, english_fallback, hardcoded) -> None:
    out = [
        "# L10n Audit Report",
        "",
        "## Missing keys",
    ]
    if not missing:
        out.append("- None")
    else:
        out.extend([f"- [{loc}] {key}" for loc, key in missing])

    out += ["", "## CJK leakage", ""]
    if not leakage:
        out.append("- None")
    else:
        out.extend([f"- [{loc}] {key}: {val}" for loc, key, val in leakage])

    out += ["", "## English fallback in localized files", ""]
    if not english_fallback:
        out.append("- None")
    else:
        out.extend([f"- [{loc}] {key}: current={cur} | en={en}" for loc, key, cur, en in english_fallback])

    out += ["", "## Hardcoded user-visible strings", ""]
    if not hardcoded:
        out.append("- None")
    else:
        out.extend([f"- {p}:{ln}: {lit}" for p, ln, lit in hardcoded])

    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", default="tools/l10n_report.md")
    args = parser.parse_args()

    base = load_strings(BASE_FILE)
    locale_values = {locale: load_strings(path) for locale, path in LOCALES.items()}

    missing = check_missing_keys(base, locale_values)
    leakage = check_cjk_leakage(locale_values)
    english_fallback = check_english_fallback(locale_values, locale_values["en"])
    hardcoded = scan_hardcoded_user_strings()

    write_report(Path(args.report), missing, leakage, english_fallback, hardcoded)

    if missing:
        print("[FAIL] missing keys detected")
    if leakage:
        print("[FAIL] CJK leakage detected")
    if english_fallback:
        print("[FAIL] English fallback detected in ja/ko")
    if hardcoded:
        print("[FAIL] hardcoded user-visible strings detected")
    if not (missing or leakage or english_fallback or hardcoded):
        print("[PASS] l10n audit passed")

    return 1 if (missing or leakage or english_fallback or hardcoded) else 0


if __name__ == "__main__":
    sys.exit(main())
