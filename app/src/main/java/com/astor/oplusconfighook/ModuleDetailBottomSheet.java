package com.astor.oplusconfighook;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ModuleDetailBottomSheet extends BottomSheetDialogFragment {
    public static final String ARG_MODULE = "arg_module";

    private Host host;

    public interface Host {
        void onModuleEditAction(String actionId);
        void onOpenAdvancedAnchor(int anchorViewId);
        String getModuleSummary(String moduleId);
        boolean isAdvancedEditEnabled();
    }

    private static final class ActionItem {
        final int titleRes;
        final int bodyRes;
        final String actionId;
        final boolean advancedRequired;

        ActionItem(int titleRes, int bodyRes, String actionId, boolean advancedRequired) {
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
            this.actionId = actionId;
            this.advancedRequired = advancedRequired;
        }
    }

    public static ModuleDetailBottomSheet newInstance(String moduleId) {
        ModuleDetailBottomSheet sheet = new ModuleDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MODULE, moduleId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) return;
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;
        ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(lp);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_module_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!(getParentFragment() instanceof Host)) {
            throw new IllegalStateException("ModuleDetailBottomSheet parent must implement Host");
        }
        host = (Host) getParentFragment();
        String moduleId = requireArguments().getString(ARG_MODULE, "");

        TextView tvTitle = view.findViewById(R.id.tvModuleDetailTitle);
        TextView tvDesc = view.findViewById(R.id.tvModuleDetailDesc);
        TextView tvSummary = view.findViewById(R.id.tvModuleDetailSummary);
        LinearLayout actionContainer = view.findViewById(R.id.moduleDetailActionContainer);
        MaterialButton btnOpenAdvanced = view.findViewById(R.id.btnModuleDetailOpenAdvanced);

        int titleRes = resolveModuleTitle(moduleId);
        int descRes = resolveModuleDesc(moduleId);
        tvTitle.setText(titleRes);
        tvDesc.setText(descRes);
        tvSummary.setText(host.getModuleSummary(moduleId));

        List<ActionItem> actions = buildActions(moduleId);
        for (ActionItem item : actions) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, 20);
            TextView t = new TextView(requireContext());
            t.setText(item.titleRes);
            t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            TextView body = new TextView(requireContext());
            body.setText(item.bodyRes);
            row.addView(t);
            row.addView(body);
            MaterialButton editButton = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            editButton.setText(R.string.edit);
            editButton.setOnClickListener(v -> {
                if (item.advancedRequired && !host.isAdvancedEditEnabled()) {
                    host.onModuleEditAction(TombstoneFragment.ACTION_ADVANCED_REQUIRED_TIP);
                    return;
                }
                dismiss();
                host.onModuleEditAction(item.actionId);
            });
            if (item.advancedRequired && !host.isAdvancedEditEnabled()) {
                editButton.setEnabled(false);
                editButton.setOnClickListener(v -> host.onModuleEditAction(TombstoneFragment.ACTION_ADVANCED_REQUIRED_TIP));
            }
            row.addView(editButton);
            actionContainer.addView(row);
        }

        btnOpenAdvanced.setOnClickListener(v -> {
            dismiss();
            host.onOpenAdvancedAnchor(resolveAnchor(moduleId));
        });
        view.findViewById(R.id.btnModuleDetailClose).setOnClickListener(v -> dismiss());
    }

    private List<ActionItem> buildActions(String moduleId) {
        List<ActionItem> items = new ArrayList<>();
        switch (moduleId) {
            case "FAST_FREEZE":
                items.add(new ActionItem(R.string.module_menu_fast_freeze_skip, R.string.module_detail_fast_freeze_skip_desc, TombstoneFragment.EDIT_FF_SKIP, false));
                break;
            case "WHITELIST":
                items.add(new ActionItem(R.string.module_menu_whitelist_donot, R.string.module_detail_whitelist_donot_desc, TombstoneFragment.EDIT_DONOT, false));
                items.add(new ActionItem(R.string.module_menu_whitelist_white100, R.string.module_detail_whitelist_white100_desc, TombstoneFragment.EDIT_WHITE_100, false));
                items.add(new ActionItem(R.string.module_menu_whitelist_im, R.string.module_detail_whitelist_im_desc, TombstoneFragment.EDIT_IMPK, false));
                items.add(new ActionItem(R.string.module_menu_whitelist_cpuctl, R.string.module_detail_whitelist_cpuctl_desc, TombstoneFragment.EDIT_CPUCTL, false));
                items.add(new ActionItem(R.string.module_menu_whitelist_appcard, R.string.module_detail_whitelist_appcard_desc, TombstoneFragment.EDIT_APPCARD, false));
                items.add(new ActionItem(R.string.module_menu_whitelist_keep_alive, R.string.module_detail_whitelist_keepalive_desc, TombstoneFragment.ACTION_KEEPALIVE, false));
                break;
            case "SYS_BLACK":
                items.add(new ActionItem(R.string.module_menu_sys_black_quick, R.string.module_detail_sys_black_quick_desc, TombstoneFragment.ACTION_SUPPRESS, false));
                items.add(new ActionItem(R.string.module_menu_sys_black_lcdon, R.string.module_detail_sys_black_lcdon_desc, TombstoneFragment.EDIT_LCDON, false));
                items.add(new ActionItem(R.string.module_menu_sys_black_lcdoff, R.string.module_detail_sys_black_lcdoff_desc, TombstoneFragment.EDIT_LCDOFF, false));
                items.add(new ActionItem(R.string.module_menu_sys_black_night, R.string.module_detail_sys_black_night_desc, TombstoneFragment.EDIT_NIGHT, false));
                items.add(new ActionItem(R.string.module_menu_sys_black_policy, R.string.module_detail_sys_black_policy_desc, TombstoneFragment.OPEN_POLICY_PARAMS, false));
                break;
            case "PROXY":
                items.add(new ActionItem(R.string.module_menu_proxy_global, R.string.module_detail_proxy_global_desc, TombstoneFragment.OPEN_PROXY_ADVANCED, true));
                items.add(new ActionItem(R.string.module_menu_proxy_default_bc, R.string.module_detail_proxy_default_bc_desc, TombstoneFragment.OPEN_PROXY_ADVANCED, true));
                items.add(new ActionItem(R.string.module_menu_proxy_bundle, R.string.module_detail_proxy_bundle_desc, TombstoneFragment.OPEN_PROXY_ADVANCED, true));
                break;
            case "BUMP":
                items.add(new ActionItem(R.string.module_menu_bump_service, R.string.module_detail_bump_service_desc, TombstoneFragment.EDIT_SERVICE_BUMP, true));
                items.add(new ActionItem(R.string.module_menu_bump_rules, R.string.module_detail_bump_prevent_desc, TombstoneFragment.EDIT_PREVENT, true));
                items.add(new ActionItem(R.string.module_detail_bump_allow_title, R.string.module_detail_bump_allow_desc, TombstoneFragment.EDIT_ALLOW, true));
                items.add(new ActionItem(R.string.module_menu_bump_traffic, R.string.module_detail_bump_traffic_desc, TombstoneFragment.OPEN_BUMP_ADVANCED, true));
                break;
            case "HIGH_LOAD":
                items.add(new ActionItem(R.string.module_menu_highload_loading, R.string.module_detail_high_load_loading_desc, TombstoneFragment.EDIT_HIGH_LOADING, true));
                items.add(new ActionItem(R.string.module_menu_highload_cpuctl, R.string.module_detail_high_load_cpuctl_desc, TombstoneFragment.EDIT_CPU_CTL_RUS, true));
                items.add(new ActionItem(R.string.module_menu_highload_android_freeze, R.string.module_detail_high_load_android_freeze_desc, TombstoneFragment.EDIT_ANDROID_FREEZE, true));
                items.add(new ActionItem(R.string.module_menu_highload_quota, R.string.module_detail_high_load_quota_desc, TombstoneFragment.EDIT_APP_QUOTA, true));
                items.add(new ActionItem(R.string.module_menu_highload_strict, R.string.module_detail_high_load_strict_desc, TombstoneFragment.EDIT_STRICT_MODE_SWITCHES, true));
                break;
        }
        return items;
    }

    private int resolveAnchor(String moduleId) {
        if ("SYS_BLACK".equals(moduleId)) return R.id.policyContainer;
        if ("PROXY".equals(moduleId)) return R.id.proxyContainer;
        if ("BUMP".equals(moduleId) || "HIGH_LOAD".equals(moduleId)) return R.id.specialHighContainer;
        return R.id.specialLowContainer;
    }

    private int resolveModuleTitle(String moduleId) {
        switch (moduleId) {
            case "FAST_FREEZE": return R.string.module_fast_freeze_title;
            case "WHITELIST": return R.string.module_whitelist_title;
            case "SYS_BLACK": return R.string.module_sys_black_title;
            case "PROXY": return R.string.module_proxy_title;
            case "BUMP": return R.string.module_bump_title;
            default: return R.string.module_high_load_title;
        }
    }

    private int resolveModuleDesc(String moduleId) {
        switch (moduleId) {
            case "FAST_FREEZE": return R.string.module_fast_freeze_desc;
            case "WHITELIST": return R.string.module_whitelist_desc;
            case "SYS_BLACK": return R.string.module_sys_black_desc;
            case "PROXY": return R.string.module_proxy_desc;
            case "BUMP": return R.string.module_bump_desc;
            default: return R.string.module_high_load_desc;
        }
    }
}
