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

public class ProxyBinderEditorBottomSheet extends BottomSheetDialogFragment {
    private static final Pattern PKG = Pattern.compile("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+");
    public interface Host {
        void onProxyBinderEditorConfirmed(boolean hasEnable, boolean enable, ArrayList<TombstoneXmlEditor.BinderDescRule> descRules);
    }

    private static final String ARG_ENABLE = "enable";
    private static final String ARG_DESC = "desc";

    public static ProxyBinderEditorBottomSheet newInstance(TombstoneXmlEditor.ProxyBinderConfig cfg) {
        ProxyBinderEditorBottomSheet sheet = new ProxyBinderEditorBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_ENABLE, cfg.enable);
        ArrayList<String> lines = new ArrayList<>();
        for (TombstoneXmlEditor.BinderDescRule r : cfg.descConfigs) {
            lines.add((r.desc == null ? "" : r.desc) + "=" + TextUtils.join("|", r.pkgTokens));
        }
        b.putStringArrayList(ARG_DESC, lines);
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

        MaterialSwitch sw = new MaterialSwitch(requireContext());
        sw.setText(R.string.proxy_config_enable);
        sw.setChecked(requireArguments().getBoolean(ARG_ENABLE));
        root.addView(sw);

        TextView t = new TextView(requireContext());
        t.setText(R.string.proxy_binder_desc_help);
        root.addView(t);

        TextInputEditText et = new TextInputEditText(requireContext());
        et.setMinLines(8);
        et.setText(TextUtils.join("\n", requireArguments().getStringArrayList(ARG_DESC)));
        root.addView(et);

        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(R.string.confirm_stage);
        btn.setOnClickListener(v -> {
            Host host = (Host) getParentFragment();
            if (host == null) return;
            ArrayList<TombstoneXmlEditor.BinderDescRule> out = new ArrayList<>();
            for (String line : linesOf(et.getText() == null ? "" : et.getText().toString())) {
                int idx = line.indexOf('=');
                if (idx <= 0) continue;
                TombstoneXmlEditor.BinderDescRule r = new TombstoneXmlEditor.BinderDescRule();
                r.desc = line.substring(0, idx).trim();
                r.pkgTokens = splitTokens(line.substring(idx + 1));
                for (String pkg : r.pkgTokens) {
                    if (!isPkgTokenOrWildcard(pkg)) {
                        UiNotifier.showMessage(this, getString(R.string.proxy_binder_invalid_pkg, pkg));
                        return;
                    }
                }
                out.add(r);
            }
            host.onProxyBinderEditorConfirmed(true, sw.isChecked(), out);
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
        for (String p : (raw == null ? "" : raw).split("[|#,\\s]+", -1)) {
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
