package com.astor.oplusconfighook;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ProxySensorEditorBottomSheet extends BottomSheetDialogFragment {
    private static final Pattern PKG = Pattern.compile("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+");
    public interface Host {
        void onProxySensorEditorConfirmed(boolean hasEnable, boolean enable, boolean hasProxyType, boolean proxyType,
                                          String proxyPkgName, ArrayList<String> proxyTypeNames,
                                          ArrayList<TombstoneXmlEditor.WhiteTypePkgRule> whiteTypeRules);
    }

    private static final String ARG_HAS_ENABLE = "hasEnable";
    private static final String ARG_ENABLE = "enable";
    private static final String ARG_HAS_PROXY_TYPE = "hasProxyType";
    private static final String ARG_PROXY_TYPE = "proxyType";
    private static final String ARG_PROXY_PKG = "proxyPkg";
    private static final String ARG_PROXY_TYPES = "proxyTypes";
    private static final String ARG_WHITE_RULES = "whiteRules";

    public static ProxySensorEditorBottomSheet newInstance(TombstoneXmlEditor.ProxySensorConfig cfg) {
        ProxySensorEditorBottomSheet sheet = new ProxySensorEditorBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_HAS_ENABLE, cfg.hasEnable);
        b.putBoolean(ARG_ENABLE, cfg.enable);
        b.putBoolean(ARG_HAS_PROXY_TYPE, cfg.hasProxyType);
        b.putBoolean(ARG_PROXY_TYPE, cfg.proxyType);
        b.putString(ARG_PROXY_PKG, cfg.proxyPkgName);
        b.putStringArrayList(ARG_PROXY_TYPES, new ArrayList<>(cfg.proxyTypeNames));
        ArrayList<String> rules = new ArrayList<>();
        for (TombstoneXmlEditor.WhiteTypePkgRule r : cfg.whiteTypePkgs) {
            rules.add((r.type == null ? "" : r.type) + "=" + TextUtils.join("|", r.pkgTokens));
        }
        b.putStringArrayList(ARG_WHITE_RULES, rules);
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = 24;
        root.setPadding(p, p, p, p);

        Bundle args = requireArguments();
        MaterialSwitch swEnable = new MaterialSwitch(requireContext());
        swEnable.setText(R.string.proxy_config_enable);
        swEnable.setChecked(args.getBoolean(ARG_ENABLE));
        root.addView(swEnable);

        MaterialSwitch swProxyType = new MaterialSwitch(requireContext());
        swProxyType.setText("config.proxyType");
        swProxyType.setChecked(args.getBoolean(ARG_PROXY_TYPE));
        root.addView(swProxyType);

        TextView t1 = new TextView(requireContext());
        t1.setText(R.string.proxy_sensor_proxy_pkg_hint);
        root.addView(t1);
        TextInputEditText etPkg = new TextInputEditText(requireContext());
        etPkg.setMinLines(3);
        etPkg.setText(TextUtils.join("\n", splitTokens(args.getString(ARG_PROXY_PKG, ""))));
        root.addView(etPkg);

        TextView t2 = new TextView(requireContext());
        t2.setText(R.string.proxy_sensor_proxy_type_hint);
        root.addView(t2);
        TextInputEditText etTypes = new TextInputEditText(requireContext());
        etTypes.setMinLines(4);
        etTypes.setText(TextUtils.join("\n", args.getStringArrayList(ARG_PROXY_TYPES)));
        root.addView(etTypes);

        TextView t3 = new TextView(requireContext());
        t3.setText(R.string.proxy_sensor_white_type_pkg_hint);
        root.addView(t3);
        TextInputEditText etWhite = new TextInputEditText(requireContext());
        etWhite.setMinLines(5);
        etWhite.setText(TextUtils.join("\n", args.getStringArrayList(ARG_WHITE_RULES)));
        root.addView(etWhite);

        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(R.string.confirm_stage);
        btn.setOnClickListener(v -> {
            Host host = (Host) getParentFragment();
            if (host == null) return;
            ArrayList<String> pkgLines = linesOf(etPkg.getText() == null ? "" : etPkg.getText().toString());
            ArrayList<String> typeLines = linesOf(etTypes.getText() == null ? "" : etTypes.getText().toString());
            ArrayList<String> whiteLines = linesOf(etWhite.getText() == null ? "" : etWhite.getText().toString());
            ArrayList<TombstoneXmlEditor.WhiteTypePkgRule> rules = new ArrayList<>();
            for (String pkg : pkgLines) {
                if (!isPkgTokenOrWildcard(pkg)) {
                    UiNotifier.showMessage(this, getString(R.string.proxy_sensor_invalid_proxy_pkg, pkg));
                    return;
                }
            }
            for (String line : whiteLines) {
                int idx = line.indexOf('=');
                TombstoneXmlEditor.WhiteTypePkgRule r = new TombstoneXmlEditor.WhiteTypePkgRule();
                if (idx >= 0) {
                    r.type = line.substring(0, idx).trim();
                    r.pkgTokens = splitTokens(line.substring(idx + 1));
                    for (String pkg : r.pkgTokens) { if (!isPkgTokenOrWildcard(pkg)) { UiNotifier.showMessage(this, getString(R.string.proxy_sensor_invalid_white_type_pkg, pkg)); return; } }
                } else {
                    r.type = "";
                    r.pkgTokens = splitTokens(line);
                    for (String pkg : r.pkgTokens) { if (!isPkgTokenOrWildcard(pkg)) { UiNotifier.showMessage(this, getString(R.string.proxy_sensor_invalid_white_type_pkg, pkg)); return; } }
                }
                rules.add(r);
            }
            host.onProxySensorEditorConfirmed(
                    true, swEnable.isChecked(),
                    true, swProxyType.isChecked(),
                    TextUtils.join("|", pkgLines),
                    new ArrayList<>(new LinkedHashSet<>(typeLines)),
                    rules
            );
            dismiss();
        });
        root.addView(btn);
        scroll.addView(root);
        return scroll;
    }

    private static ArrayList<String> linesOf(String text) {
        ArrayList<String> out = new ArrayList<>();
        for (String line : text.split("\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static ArrayList<String> splitTokens(String raw) {
        Set<String> set = new LinkedHashSet<>();
        String[] parts = raw == null ? new String[0] : raw.split("[|#,\\s]+", -1);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return new ArrayList<>(set);
    }

    private static boolean isPkgTokenOrWildcard(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.equals("*")) return true;
        return PKG.matcher(s).matches();
    }
}
