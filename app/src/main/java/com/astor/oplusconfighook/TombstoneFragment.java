package com.astor.oplusconfighook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Paint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.tabs.TabLayout;

import com.astor.oplusconfighook.editor.EditorRegistry;
import com.astor.oplusconfighook.editor.EditorSpec;
import com.astor.oplusconfighook.editor.ValidationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * 墓碑策略主页面，管理规则编辑、校验和提交流程。
 */
public class TombstoneFragment extends Fragment implements WhyAffectBottomSheetFragment.Actions, ModuleDetailBottomSheet.Host, EditableListEditorBottomSheet.Host, ProxySensorEditorBottomSheet.Host, ProxyBinderEditorBottomSheet.Host {
    private static final int REQ_LCDON = 1;
    private static final int REQ_LCDOFF = 2;
    private static final int REQ_NIGHT = 3;
    private static final int REQ_ALLOW = 4;
    private static final int REQ_DONOT = 5;
    private static final int REQ_IMPK = 6;
    private static final int REQ_CPUCTL = 7;
    private static final int REQ_APPCARD = 8;
    private static final int REQ_GMS = 9;
    private static final int REQ_FF_SKIP = 10;
    private static final int REQ_WHITE_100 = 11;
    private static final int REQ_WHITE_010 = 12;
    private static final int REQ_WHITE_001 = 13;
    private static final int REQ_BLACK_HANS = 14;
    private static final int REQ_INSPECTOR_PICK = 100;
    private static final int REQ_QUICK_SUPPRESS_PICK = 101;
    private static final int REQ_QUICK_KEEP_PICK = 102;
    private static final int REQ_PKGCONFIG_PICK = 201;
    private static final int REQ_BINDER_PICK = 202;

    public static final String EDIT_FF_SKIP = "EDIT_FF_SKIP";
    public static final String EDIT_DONOT = "EDIT_DONOT";
    public static final String EDIT_WHITE_100 = "EDIT_WHITE_100";
    public static final String EDIT_IMPK = "EDIT_IMPK";
    public static final String EDIT_CPUCTL = "EDIT_CPUCTL";
    public static final String EDIT_APPCARD = "EDIT_APPCARD";
    public static final String ACTION_KEEPALIVE = "ACTION_KEEPALIVE";
    public static final String ACTION_SUPPRESS = "ACTION_SUPPRESS";
    public static final String EDIT_LCDON = "EDIT_LCDON";
    public static final String EDIT_LCDOFF = "EDIT_LCDOFF";
    public static final String EDIT_NIGHT = "EDIT_NIGHT";
    public static final String OPEN_POLICY_PARAMS = "OPEN_POLICY_PARAMS";
    public static final String OPEN_PROXY_ADVANCED = "OPEN_PROXY_ADVANCED";
    public static final String EDIT_SERVICE_BUMP = "EDIT_SERVICE_BUMP";
    public static final String EDIT_PREVENT = "EDIT_PREVENT";
    public static final String EDIT_ALLOW = "EDIT_ALLOW";
    public static final String OPEN_BUMP_ADVANCED = "OPEN_BUMP_ADVANCED";
    public static final String EDIT_HIGH_LOADING = "EDIT_HIGH_LOADING";
    public static final String EDIT_CPU_CTL_RUS = "EDIT_CPU_CTL_RUS";
    public static final String EDIT_ANDROID_FREEZE = "EDIT_ANDROID_FREEZE";
    public static final String EDIT_APP_QUOTA = "EDIT_APP_QUOTA";
    public static final String EDIT_STRICT_MODE_SWITCHES = "EDIT_STRICT_MODE_SWITCHES";
    public static final String ACTION_ADVANCED_REQUIRED_TIP = "ACTION_ADVANCED_REQUIRED_TIP";

    private static final long WARN_UPPER_BOUND = 86_400_000L;
    private static final String FLOW_TAG = "AppListFlow";
    private static final int TIMING_PROFILE_NONE = 0;
    private static final int TIMING_PROFILE_L1 = 1;
    private static final int TIMING_PROFILE_L2 = 2;
    private static final int TIMING_PROFILE_L3 = 3;

    private static final String RK_SERVICE_BUMP = "V3C:SERVICE_BUMP_REASONS";
    private static final String RK_PREVENT = "V3C:SCENE_PREVENT";
    private static final String RK_ALLOW = "V3C:SCENE_ALLOW";
    private static final String RK_ANDROID_FREEZE = "V3C:ANDROID_FREEZE";
    private static final String RK_APP_QUOTA = "V3C:APP_QUOTA";
    private static final String RK_HIGH_LOADING = "V3C:HIGH_LOADING";
    private static final String RK_CPU_CTL_RUS = "V3C:CPU_CTL_RUS";
    private static final String RK_PROXY_DEFAULT_BC = "PROXY:DEFAULT_BC";
    private static final String RK_PROXY_SENSOR_TYPES = "PROXY:SENSOR_TYPES";
    private static final String RK_STRICT_SWITCH = "V3C:STRICT_SWITCH";
    private static final String RK_PKGCONFIG_PREFIX = "PKGCONFIG:";
    private static final String RK_BINDER_PREFIX = "BINDER:";

    private enum Module {
        FAST_FREEZE, WHITELIST, SYS_BLACK, PROXY, BUMP, HIGH_LOAD
    }

    private static final class FieldMeta {
        final int nameRes;
        final int tooltipRes;
        final boolean isTimeLike;

        FieldMeta(int nameRes, int tooltipRes, boolean isTimeLike) {
            this.nameRes = nameRes;
            this.tooltipRes = tooltipRes;
            this.isTimeLike = isTimeLike;
        }
    }

    private TombstoneViewModel vm;

    private TextView tvSource;
    private TextView tvDirtyState;
    private LinearLayout policyContainer;
    private LinearLayout timeParamContainer;
    private LinearLayout sectionRecommended;
    private LinearLayout sectionAdvanced;
    private LinearLayout specialLowContainer;
    private LinearLayout specialMediumContainer;
    private LinearLayout specialHighContainer;
    private LinearLayout proxyContainer;
    private TabLayout tabLayoutTombstone;
    private ScrollView scrollRecommended;
    private ScrollView scrollAdvanced;
    private TextView tvFastFreezeSummary;
    private TextView tvWhitelistSummary;
    private TextView tvSysBlackSummary;
    private TextView tvProxySummary;
    private TextView tvBumpSummary;
    private TextView tvHighLoadSummary;
    private boolean bindingPolicyViews;
    private boolean bindingUiState;
    private boolean lastAdvancedEditEnabled;
    private AlertDialog inspectorDialog;
    private TextView tvInspectorPkg;
    private MaterialSwitch swInspectorEditableOnly;
    private RecyclerView rvInspectorHits;
    private InspectorAdapter inspectorAdapter;
    private final Set<String> inspectorRemovedKeys = new LinkedHashSet<>();
    private Set<String> quickPendingPackages;
    private boolean quickPendingSuppress;
    private String pendingPkgConfigCategory;
    private int pendingBinderIndex = -1;

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                int req = result.getData().getIntExtra(AppPickerActivity.EXTRA_REQ, -1);
                ArrayList<String> selected = result.getData().getStringArrayListExtra(AppPickerActivity.EXTRA_SELECTED);
                Set<String> set = new LinkedHashSet<>(selected == null ? new ArrayList<>() : selected);
                if (req == REQ_LCDON) vm.stagedBlacklists.put("lcdon", set);
                else if (req == REQ_LCDOFF) vm.stagedBlacklists.put("lcdoff", set);
                else if (req == REQ_NIGHT) vm.stagedBlacklists.put("night", set);
                else if (req == REQ_ALLOW) vm.stagedAllow = set;
                else if (req == REQ_DONOT) vm.stagedDoNot = set;
                else if (req == REQ_IMPK) vm.stagedImPkg = set;
                else if (req == REQ_CPUCTL) vm.stagedCpuCtlWhiteList = set;
                else if (req == REQ_APPCARD) vm.stagedAppCardIgnore = set;
                else if (req == REQ_GMS) vm.stagedGms = set;
                else if (req == REQ_FF_SKIP) vm.stagedFfSkipFastFreeze = set;
                else if (req == REQ_WHITE_100) vm.stagedWhitePkgByCategory.put("100", set);
                else if (req == REQ_WHITE_010) vm.stagedWhitePkgByCategory.put("010", set);
                else if (req == REQ_WHITE_001) vm.stagedWhitePkgByCategory.put("001", set);
                else if (req == REQ_BLACK_HANS) vm.stagedBlackHansTokens = new ArrayList<>(set);
                else if (req == REQ_INSPECTOR_PICK && !set.isEmpty()) {
                    vm.inspectorQuery = set.iterator().next();
                    if (tvInspectorPkg != null) tvInspectorPkg.setText(vm.inspectorQuery);
                    runInspector(vm.inspectorQuery, swInspectorEditableOnly != null && swInspectorEditableOnly.isChecked());
                    if (inspectorDialog != null && !inspectorDialog.isShowing()) showInspectorDialog();
                    return;
                } else if (req == REQ_QUICK_SUPPRESS_PICK || req == REQ_QUICK_KEEP_PICK) {
                    quickPendingPackages = set;
                    quickPendingSuppress = req == REQ_QUICK_SUPPRESS_PICK;
                    showQuickActionScopeSheet();
                    return;
                } else if (req == REQ_PKGCONFIG_PICK && pendingPkgConfigCategory != null) {
                    String category = pendingPkgConfigCategory;
                    mergePkgConfigTokens(category, set);
                    pendingPkgConfigCategory = null;
                    showPkgConfigEditor(category);
                } else if (req == REQ_BINDER_PICK && pendingBinderIndex >= 0) {
                    int idx = pendingBinderIndex;
                    mergeBinderTokens(idx, set);
                    pendingBinderIndex = -1;
                    showBinderEditor(idx);
                }
                updateCounts();
                renderSpecialHandling();
                updateDirtyBadge();
                if (!shouldSuppressUnappliedToast()) {
                    Toast.makeText(requireContext(), R.string.staged_not_applied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    byte[] data = in == null ? null : in.readAllBytes();
                    TombstoneXmlEditor.parse(data);
                    PolicyStore.saveTombstoneRaw(requireContext(), data);
                    reloadFromPolicy();
                    updateSource();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), getString(R.string.import_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/xml"), uri -> {
                if (uri == null) return;
                try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    PolicyStore.refresh(requireContext());
                    byte[] bytes = PolicyStore.tombstoneBytesFast();
                    if (bytes != null) out.write(bytes);
                    Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), getString(R.string.export_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.i(FLOW_TAG, "TombstoneFragment onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tombstone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppLogger.i(FLOW_TAG, "TombstoneFragment onViewCreated");
        vm = new ViewModelProvider(requireActivity()).get(TombstoneViewModel.class);
        tvSource = view.findViewById(R.id.tvTombstoneSource);
        tvDirtyState = view.findViewById(R.id.tvDirtyState);
        policyContainer = view.findViewById(R.id.policyContainer);
        timeParamContainer = view.findViewById(R.id.timeParamContainer);
        sectionRecommended = view.findViewById(R.id.sectionRecommended);
        sectionAdvanced = view.findViewById(R.id.sectionAdvanced);
        specialLowContainer = view.findViewById(R.id.specialLowContainer);
        specialMediumContainer = view.findViewById(R.id.specialMediumContainer);
        specialHighContainer = view.findViewById(R.id.specialHighContainer);
        proxyContainer = view.findViewById(R.id.proxyContainer);

        tabLayoutTombstone = view.findViewById(R.id.tabLayoutTombstone);
        scrollRecommended = view.findViewById(R.id.scrollRecommended);
        scrollAdvanced = view.findViewById(R.id.scrollAdvanced);
        if (scrollRecommended != null) {
            scrollRecommended.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (vm == null) return;
                vm.recommendedScrollY = Math.max(scrollY, 0);
            });
        }
        if (scrollAdvanced != null) {
            scrollAdvanced.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (vm == null) return;
                vm.advancedScrollY = Math.max(scrollY, 0);
            });
        }
        tvFastFreezeSummary = view.findViewById(R.id.tvFastFreezeSummary);
        tvWhitelistSummary = view.findViewById(R.id.tvWhitelistSummary);
        tvSysBlackSummary = view.findViewById(R.id.tvSysBlackSummary);
        tvProxySummary = view.findViewById(R.id.tvProxySummary);
        tvBumpSummary = view.findViewById(R.id.tvBumpSummary);
        tvHighLoadSummary = view.findViewById(R.id.tvHighLoadSummary);

        tabLayoutTombstone.addTab(tabLayoutTombstone.newTab().setText(R.string.tab_recommended));
        tabLayoutTombstone.addTab(tabLayoutTombstone.newTab().setText(R.string.tab_advanced));
        tabLayoutTombstone.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int idx = (tab == null ? 0 : tab.getPosition());
                if (idx == vm.tombstoneTabIndex) return;
                stopScroll(vm.tombstoneTabIndex == 0 ? scrollRecommended : scrollAdvanced);
                vm.tombstoneTabIndex = idx;
                applyTab(idx);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        int initIdx = vm.tombstoneTabIndex;
        if (initIdx < 0 || initIdx >= tabLayoutTombstone.getTabCount()) {
            initIdx = 0;
            vm.tombstoneTabIndex = 0;
        }
        if (scrollRecommended != null) {
            scrollRecommended.post(() -> scrollRecommended.scrollTo(0, Math.max(vm.recommendedScrollY, 0)));
        }
        if (scrollAdvanced != null) {
            scrollAdvanced.post(() -> scrollAdvanced.scrollTo(0, Math.max(vm.advancedScrollY, 0)));
        }
        applyTab(initIdx);
        TabLayout.Tab initTab = tabLayoutTombstone.getTabAt(initIdx);
        if (initTab != null) {
            tabLayoutTombstone.selectTab(initTab);
        }

        view.<MaterialButton>findViewById(R.id.btnEditLcdOn).setOnClickListener(v -> openPicker(REQ_LCDON, vm.stagedBlacklists.get("lcdon")));
        view.<MaterialButton>findViewById(R.id.btnEditLcdOff).setOnClickListener(v -> openPicker(REQ_LCDOFF, vm.stagedBlacklists.get("lcdoff")));
        view.<MaterialButton>findViewById(R.id.btnEditNight).setOnClickListener(v -> openPicker(REQ_NIGHT, vm.stagedBlacklists.get("night")));
        view.<MaterialButton>findViewById(R.id.btnEditAllow).setOnClickListener(v -> openPicker(REQ_ALLOW, vm.stagedAllow));
        view.<MaterialButton>findViewById(R.id.btnEditDoNotFreeze).setOnClickListener(v -> openPicker(REQ_DONOT, vm.stagedDoNot));
        view.<MaterialButton>findViewById(R.id.btnInspector).setOnClickListener(v -> showInspectorDialog());
        view.<MaterialButton>findViewById(R.id.btnQuickSuppress).setOnClickListener(v ->
                openPicker(REQ_QUICK_SUPPRESS_PICK, new LinkedHashSet<>()));
        view.<MaterialButton>findViewById(R.id.btnQuickKeepAlive).setOnClickListener(v ->
                openPicker(REQ_QUICK_KEEP_PICK, new LinkedHashSet<>()));
        view.<MaterialButton>findViewById(R.id.btnFastFreezeConfig).setOnClickListener(v -> openModuleDetail(Module.FAST_FREEZE));
        view.<MaterialButton>findViewById(R.id.btnFastFreezeAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.specialLowContainer));
        view.<MaterialButton>findViewById(R.id.btnWhitelistConfig).setOnClickListener(v -> openModuleDetail(Module.WHITELIST));
        view.<MaterialButton>findViewById(R.id.btnWhitelistAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.specialLowContainer));
        view.<MaterialButton>findViewById(R.id.btnSysBlackConfig).setOnClickListener(v -> openModuleDetail(Module.SYS_BLACK));
        view.<MaterialButton>findViewById(R.id.btnSysBlackAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.policyContainer));
        view.<MaterialButton>findViewById(R.id.btnProxyConfig).setOnClickListener(v -> openModuleDetail(Module.PROXY));
        view.<MaterialButton>findViewById(R.id.btnProxyAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.proxyContainer));
        view.<MaterialButton>findViewById(R.id.btnBumpConfig).setOnClickListener(v -> openModuleDetail(Module.BUMP));
        view.<MaterialButton>findViewById(R.id.btnBumpAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.specialHighContainer));
        view.<MaterialButton>findViewById(R.id.btnHighLoadConfig).setOnClickListener(v -> openModuleDetail(Module.HIGH_LOAD));
        view.<MaterialButton>findViewById(R.id.btnHighLoadAdvanced).setOnClickListener(v -> openAdvancedAndScrollTo(R.id.specialHighContainer));
        view.<MaterialButton>findViewById(R.id.btnApplyTombstone).setOnClickListener(v -> apply());
        view.<MaterialButton>findViewById(R.id.btnRestoreTombstone).setOnClickListener(v -> {
            PolicyStore.deleteTombstoneUser(requireContext());
            reloadFromPolicy();
            updateSource();
        });
        view.<MaterialButton>findViewById(R.id.btnImportTombstone).setOnClickListener(v -> importLauncher.launch(new String[]{"text/xml", "application/xml"}));
        view.<MaterialButton>findViewById(R.id.btnExportTombstone).setOnClickListener(v -> exportLauncher.launch("sys_elsa_config_list.xml"));

        bindingUiState = true;
        try {
            if (!vm.initialized()) {
                reloadFromPolicy();
                vm.markInitialized();
            } else {
                renderTimeParams();
                renderPolicies();
                renderSpecialHandling();
                renderProxySection();
                updateCounts();
            }
        } finally {
            bindingUiState = false;
        }
        lastAdvancedEditEnabled = advancedEditEnabled();
        updateSource();
        updateDirtyBadge();
    }

    @Override
    public void onStart() {
        super.onStart();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onResume");
        syncAdvancedEditUiIfChanged();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            AppLogger.i(FLOW_TAG, "TombstoneFragment onHiddenChanged -> visible");
            syncAdvancedEditUiIfChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onStop");
        if (shouldSuppressDirtyNotice()) return;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLogger.i(FLOW_TAG, "TombstoneFragment onDestroy");
    }

    private boolean shouldSuppressDirtyNotice() {
        if (!isAdded() || getView() == null || bindingUiState || bindingPolicyViews) return true;
        Activity activity = getActivity();
        return activity == null
                || activity.isChangingConfigurations()
                || activity.isFinishing()
                || isRemoving();
    }

    private boolean shouldSuppressUnappliedToast() {
        if (!isAdded() || getView() == null || bindingUiState || bindingPolicyViews) return true;
        Activity activity = getActivity();
        return activity == null
                || activity.isChangingConfigurations()
                || activity.isFinishing()
                || isRemoving();
    }

    private boolean advancedEditEnabled() {
        return Prefs.tombstoneAdvancedEdit(requireContext());
    }

    private void syncAdvancedEditUiIfChanged() {
        if (!isAdded() || getView() == null) return;
        boolean advancedEnabled = advancedEditEnabled();
        if (advancedEnabled == lastAdvancedEditEnabled) return;
        lastAdvancedEditEnabled = advancedEnabled;
        bindingUiState = true;
        try {
            renderTimeParams();
            renderPolicies();
            renderSpecialHandling();
            renderProxySection();
            updateCounts();
        } finally {
            bindingUiState = false;
        }
    }

    private void openPicker(int req, Set<String> preselected) {
        AppLogger.e(FLOW_TAG, "navigate openPicker req=" + req, new Exception("openPicker trace"));
        Intent i = new Intent(requireContext(), AppPickerActivity.class);
        i.putExtra(AppPickerActivity.EXTRA_MODE, "TOMBSTONE");
        i.putExtra(AppPickerActivity.EXTRA_REQ, req);
        i.putStringArrayListExtra(AppPickerActivity.EXTRA_PRESELECTED, new ArrayList<>(preselected == null ? new LinkedHashSet<>() : preselected));
        pickerLauncher.launch(i);
    }

    private void reloadFromPolicy() {
        try {
            PolicyStore.refresh(requireContext());
            vm.baseXmlBytes = PolicyStore.tombstoneBytesFast();
            TombstoneXmlEditor.Parsed p = TombstoneXmlEditor.parse(vm.baseXmlBytes);
            vm.stagedBlacklists.clear();
            for (String s : TombstoneXmlEditor.BLACKLIST_SCENES) vm.stagedBlacklists.put(s, new LinkedHashSet<>(p.blacklistByInherit.get(s)));
            vm.stagedAllow = new LinkedHashSet<>(p.freezeAllowList);
            vm.stagedDoNot = new LinkedHashSet<>(p.doNotFreezeList);
            vm.sourcePolicies.clear();
            vm.policyDrafts.clear();
            for (TombstoneXmlEditor.PolicyItem item : p.policyItems) {
                vm.sourcePolicies.put(item.id(), item);
                vm.policyDrafts.put(item.id(), clonePolicy(item));
            }
            vm.stagedTimingValues.clear();
            vm.baselineTimingValues.clear();
            vm.timingItemsByKey.clear();
            for (TombstoneXmlEditor.TimingItem timingItem : p.timingItems) {
                vm.stagedTimingValues.put(timingItem.key, timingItem.value);
                vm.baselineTimingValues.put(timingItem.key, timingItem.value);
                vm.timingItemsByKey.put(timingItem.key, timingItem);
            }
            vm.stagedImPkg = new LinkedHashSet<>(p.imPkgList);
            vm.stagedCpuCtlWhiteList = new LinkedHashSet<>(p.cpuCtlWhiteList);
            vm.stagedAppCardIgnore = new LinkedHashSet<>(p.appCardIgnoreList);
            vm.stagedEnableConfigAttrs.clear();
            vm.stagedEnableConfigAttrs.putAll(p.enableConfigAttrs);
            vm.stagedGms = new LinkedHashSet<>(p.gmsList);
            vm.stagedBlackHansEnabled = p.blackHansEnabled;
            vm.stagedBlackHansTokens = new ArrayList<>(p.blackHansPkgTokens);
            vm.stagedFfSkipFastFreeze = new LinkedHashSet<>(p.ffSkipFastFreeze);
            vm.stagedWhitePkgByCategory.clear();
            for (Map.Entry<String, Set<String>> entry : p.whitePkgByCategory.entrySet()) {
                vm.stagedWhitePkgByCategory.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
            }
            vm.hasFfPkgBlack = p.hasFfPkgBlack;
            vm.hasWhitePkg = p.hasWhitePkg;
            vm.hasEnableConfig = p.hasEnableConfig;
            vm.hasGms = p.hasGms;
            vm.hasBlackHansPkg = p.hasBlackHansPkg;
            vm.stagedPkgConfigTokens.clear();
            vm.stagedPkgConfigTokens.putAll(p.pkgConfigTokensByCategory);
            vm.stagedBinderRules.clear();
            vm.stagedBinderRules.addAll(p.binderRuleItems);
            vm.stagedProxyGlobalConfig.hasAny = p.proxyGlobalConfig.hasAny;
            vm.stagedProxyGlobalConfig.alarm = p.proxyGlobalConfig.alarm;
            vm.stagedProxyGlobalConfig.service = p.proxyGlobalConfig.service;
            vm.stagedProxyGlobalConfig.job = p.proxyGlobalConfig.job;
            vm.stagedProxyGlobalConfig.broadcast = p.proxyGlobalConfig.broadcast;
            vm.stagedProxyGlobalConfig.proxyBCmax = p.proxyGlobalConfig.proxyBCmax;
            vm.stagedDefaultProxyBcActions = new ArrayList<>(p.defaultProxyBcActions);
            vm.stagedProxyBcExcludeRules = new ArrayList<>(p.proxyBcExcludeRules);
            vm.stagedProxyWakeLockConfig.defaultWorkSourceType = p.proxyWakeLockConfig.defaultWorkSourceType;
            vm.stagedProxyWakeLockConfig.supportWorkSourceTags = p.proxyWakeLockConfig.supportWorkSourceTags;
            vm.stagedProxyWakeLockConfig.unSupportedWlTypes = p.proxyWakeLockConfig.unSupportedWlTypes;
            vm.stagedProxyWakeLockConfig.proxyButDropTypes = p.proxyWakeLockConfig.proxyButDropTypes;
            vm.stagedProxyGpsConfig.hasEnable = p.proxyGpsConfig.hasEnable;
            vm.stagedProxyGpsConfig.enable = p.proxyGpsConfig.enable;
            vm.stagedProxyGpsConfig.items.clear(); vm.stagedProxyGpsConfig.items.addAll(p.proxyGpsConfig.items);
            vm.stagedProxyBtScanConfig.hasEnable = p.proxyBtScanConfig.hasEnable;
            vm.stagedProxyBtScanConfig.enable = p.proxyBtScanConfig.enable;
            vm.stagedProxyCpnExtensionConfig.hasEnable = p.proxyCpnExtensionConfig.hasEnable;
            vm.stagedProxyCpnExtensionConfig.enable = p.proxyCpnExtensionConfig.enable;
            vm.stagedProxyCpnExtensionConfig.cpnMask = p.proxyCpnExtensionConfig.cpnMask;
            vm.stagedProxySensorConfig.hasEnable = p.proxySensorConfig.hasEnable;
            vm.stagedProxySensorConfig.enable = p.proxySensorConfig.enable;
            vm.stagedProxySensorConfig.hasProxyType = p.proxySensorConfig.hasProxyType;
            vm.stagedProxySensorConfig.proxyType = p.proxySensorConfig.proxyType;
            vm.stagedProxySensorConfig.proxyPkgName = p.proxySensorConfig.proxyPkgName;
            vm.stagedProxySensorConfig.proxyTypeNames.clear(); vm.stagedProxySensorConfig.proxyTypeNames.addAll(p.proxySensorConfig.proxyTypeNames);
            vm.stagedProxySensorConfig.whiteTypePkgs.clear(); vm.stagedProxySensorConfig.whiteTypePkgs.addAll(p.proxySensorConfig.whiteTypePkgs);
            vm.stagedProxyBinderConfig.hasEnable = p.proxyBinderConfig.hasEnable;
            vm.stagedProxyBinderConfig.enable = p.proxyBinderConfig.enable;
            vm.stagedProxyBinderConfig.descConfigs.clear(); vm.stagedProxyBinderConfig.descConfigs.addAll(p.proxyBinderConfig.descConfigs);
            vm.stagedServiceBumpReasons = new LinkedHashSet<>(p.serviceBumpReasons);
            vm.stagedPreventRules = new ArrayList<>(p.preventRules);
            vm.stagedAllowRules = new ArrayList<>(p.allowRules);
            vm.stagedCpuCtlRus.attrs.clear(); vm.stagedCpuCtlRus.attrs.putAll(p.cpuCtlRusConfig.attrs);
            vm.stagedHighLoadingConfigs.clear(); vm.stagedHighLoadingConfigs.addAll(p.highLoadingConfigs);
            vm.stagedStrictModeSwitches.strictEnable = p.strictModeSwitches.strictEnable;
            vm.stagedStrictModeSwitches.version = p.strictModeSwitches.version;
            vm.stagedStrictModeSwitches.modes.clear(); vm.stagedStrictModeSwitches.modes.addAll(p.strictModeSwitches.modes);
            vm.stagedAndroidFreezeConfig.enable = p.androidFreezeConfig.enable;
            vm.stagedAndroidFreezeConfig.whiteEnable = p.androidFreezeConfig.whiteEnable;
            vm.stagedAndroidFreezeConfig.killWhiteEnable = p.androidFreezeConfig.killWhiteEnable;
            vm.stagedAndroidFreezeConfig.sysAppExemptEnable = p.androidFreezeConfig.sysAppExemptEnable;
            vm.stagedAndroidFreezeConfig.exemptQuotas.clear(); vm.stagedAndroidFreezeConfig.exemptQuotas.addAll(p.androidFreezeConfig.exemptQuotas);
            vm.stagedAppQuotaConfig.maxDuration = p.appQuotaConfig.maxDuration;
            vm.stagedAppQuotaConfig.protectTimeAfterExceed = p.appQuotaConfig.protectTimeAfterExceed;
            vm.stagedAppQuotaConfig.parentPath = p.appQuotaConfig.parentPath;
            refreshBaselineFromDraft();
            runProxyRoundTripSelfCheck(vm.baseXmlBytes);
            renderTimeParams();
            renderPolicies();
            renderSpecialHandling();
            renderProxySection();
            updateCounts();
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void runProxyRoundTripSelfCheck(byte[] baseXmlBytes) {
        if (baseXmlBytes == null || baseXmlBytes.length == 0) return;
        try {
            TombstoneXmlEditor.Parsed parsed = TombstoneXmlEditor.parse(baseXmlBytes);
            if (parsed == null) return;
        } catch (Exception e) {
            Log.w(FLOW_TAG, "Proxy round-trip self-check skipped: " + e.getMessage());
        }
    }

    private void refreshBaselineFromDraft() {
        vm.baselineBlacklists.clear();
        for (Map.Entry<String, Set<String>> entry : vm.stagedBlacklists.entrySet()) {
            vm.baselineBlacklists.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        vm.baselineAllow = new LinkedHashSet<>(vm.stagedAllow);
        vm.baselineDoNot = new LinkedHashSet<>(vm.stagedDoNot);
        vm.baselinePolicyDrafts.clear();
        for (Map.Entry<String, TombstoneXmlEditor.PolicyItem> entry : vm.policyDrafts.entrySet()) {
            vm.baselinePolicyDrafts.put(entry.getKey(), clonePolicy(entry.getValue()));
        }
        vm.baselineImPkg = new LinkedHashSet<>(vm.stagedImPkg);
        vm.baselineCpuCtlWhiteList = new LinkedHashSet<>(vm.stagedCpuCtlWhiteList);
        vm.baselineAppCardIgnore = new LinkedHashSet<>(vm.stagedAppCardIgnore);
        vm.baselineEnableConfigAttrs.clear();
        vm.baselineEnableConfigAttrs.putAll(vm.stagedEnableConfigAttrs);
        vm.baselineGms = new LinkedHashSet<>(vm.stagedGms);
        vm.baselineBlackHansEnabled = vm.stagedBlackHansEnabled;
        vm.baselineBlackHansTokens = new ArrayList<>(vm.stagedBlackHansTokens);
        vm.baselineFfSkipFastFreeze = new LinkedHashSet<>(vm.stagedFfSkipFastFreeze);
        vm.baselineWhitePkgByCategory.clear();
        for (Map.Entry<String, Set<String>> entry : vm.stagedWhitePkgByCategory.entrySet()) {
            vm.baselineWhitePkgByCategory.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        vm.baselinePkgConfigTokens.clear();
        vm.baselinePkgConfigTokens.putAll(vm.stagedPkgConfigTokens);
        vm.baselineBinderRules.clear();
        vm.baselineBinderRules.addAll(vm.stagedBinderRules);
        vm.baselineProxyGlobalConfig.hasAny = vm.stagedProxyGlobalConfig.hasAny;
        vm.baselineProxyGlobalConfig.alarm = vm.stagedProxyGlobalConfig.alarm;
        vm.baselineProxyGlobalConfig.service = vm.stagedProxyGlobalConfig.service;
        vm.baselineProxyGlobalConfig.job = vm.stagedProxyGlobalConfig.job;
        vm.baselineProxyGlobalConfig.broadcast = vm.stagedProxyGlobalConfig.broadcast;
        vm.baselineProxyGlobalConfig.proxyBCmax = vm.stagedProxyGlobalConfig.proxyBCmax;
        vm.baselineDefaultProxyBcActions = new ArrayList<>(vm.stagedDefaultProxyBcActions);
        vm.baselineProxyBcExcludeRules = new ArrayList<>(vm.stagedProxyBcExcludeRules);
        vm.baselineProxyWakeLockConfig.defaultWorkSourceType = vm.stagedProxyWakeLockConfig.defaultWorkSourceType;
        vm.baselineProxyWakeLockConfig.supportWorkSourceTags = vm.stagedProxyWakeLockConfig.supportWorkSourceTags;
        vm.baselineProxyWakeLockConfig.unSupportedWlTypes = vm.stagedProxyWakeLockConfig.unSupportedWlTypes;
        vm.baselineProxyWakeLockConfig.proxyButDropTypes = vm.stagedProxyWakeLockConfig.proxyButDropTypes;
        vm.baselineProxyGpsConfig.hasEnable = vm.stagedProxyGpsConfig.hasEnable;
        vm.baselineProxyGpsConfig.enable = vm.stagedProxyGpsConfig.enable;
        vm.baselineProxyGpsConfig.items.clear(); vm.baselineProxyGpsConfig.items.addAll(vm.stagedProxyGpsConfig.items);
        vm.baselineProxyBtScanConfig.hasEnable = vm.stagedProxyBtScanConfig.hasEnable;
        vm.baselineProxyBtScanConfig.enable = vm.stagedProxyBtScanConfig.enable;
        vm.baselineProxyCpnExtensionConfig.hasEnable = vm.stagedProxyCpnExtensionConfig.hasEnable;
        vm.baselineProxyCpnExtensionConfig.enable = vm.stagedProxyCpnExtensionConfig.enable;
        vm.baselineProxyCpnExtensionConfig.cpnMask = vm.stagedProxyCpnExtensionConfig.cpnMask;
        vm.baselineProxySensorConfig.hasEnable = vm.stagedProxySensorConfig.hasEnable;
        vm.baselineProxySensorConfig.enable = vm.stagedProxySensorConfig.enable;
        vm.baselineProxySensorConfig.hasProxyType = vm.stagedProxySensorConfig.hasProxyType;
        vm.baselineProxySensorConfig.proxyType = vm.stagedProxySensorConfig.proxyType;
        vm.baselineProxySensorConfig.proxyPkgName = vm.stagedProxySensorConfig.proxyPkgName;
        vm.baselineProxySensorConfig.proxyTypeNames.clear(); vm.baselineProxySensorConfig.proxyTypeNames.addAll(vm.stagedProxySensorConfig.proxyTypeNames);
        vm.baselineProxySensorConfig.whiteTypePkgs.clear(); vm.baselineProxySensorConfig.whiteTypePkgs.addAll(vm.stagedProxySensorConfig.whiteTypePkgs);
        vm.baselineProxyBinderConfig.hasEnable = vm.stagedProxyBinderConfig.hasEnable;
        vm.baselineProxyBinderConfig.enable = vm.stagedProxyBinderConfig.enable;
        vm.baselineProxyBinderConfig.descConfigs.clear(); vm.baselineProxyBinderConfig.descConfigs.addAll(vm.stagedProxyBinderConfig.descConfigs);
        vm.baselineServiceBumpReasons = new LinkedHashSet<>(vm.stagedServiceBumpReasons);
        vm.baselinePreventRules = new ArrayList<>(vm.stagedPreventRules);
        vm.baselineAllowRules = new ArrayList<>(vm.stagedAllowRules);
        vm.baselineCpuCtlRus.attrs.clear(); vm.baselineCpuCtlRus.attrs.putAll(vm.stagedCpuCtlRus.attrs);
        vm.baselineHighLoadingConfigs.clear(); vm.baselineHighLoadingConfigs.addAll(vm.stagedHighLoadingConfigs);
        vm.baselineStrictModeSwitches.strictEnable = vm.stagedStrictModeSwitches.strictEnable;
        vm.baselineStrictModeSwitches.version = vm.stagedStrictModeSwitches.version;
        vm.baselineStrictModeSwitches.modes.clear(); vm.baselineStrictModeSwitches.modes.addAll(vm.stagedStrictModeSwitches.modes);
        vm.baselineAndroidFreezeConfig.enable = vm.stagedAndroidFreezeConfig.enable;
        vm.baselineAndroidFreezeConfig.whiteEnable = vm.stagedAndroidFreezeConfig.whiteEnable;
        vm.baselineAndroidFreezeConfig.killWhiteEnable = vm.stagedAndroidFreezeConfig.killWhiteEnable;
        vm.baselineAndroidFreezeConfig.sysAppExemptEnable = vm.stagedAndroidFreezeConfig.sysAppExemptEnable;
        vm.baselineAndroidFreezeConfig.exemptQuotas.clear(); vm.baselineAndroidFreezeConfig.exemptQuotas.addAll(vm.stagedAndroidFreezeConfig.exemptQuotas);
        vm.baselineAppQuotaConfig.maxDuration = vm.stagedAppQuotaConfig.maxDuration;
        vm.baselineAppQuotaConfig.protectTimeAfterExceed = vm.stagedAppQuotaConfig.protectTimeAfterExceed;
        vm.baselineAppQuotaConfig.parentPath = vm.stagedAppQuotaConfig.parentPath;
    }

    private TombstoneXmlEditor.PolicyItem clonePolicy(TombstoneXmlEditor.PolicyItem item) {
        return new TombstoneXmlEditor.PolicyItem(item.groupName, item.name, item.keyScene, item.scene, item.mask, item.quota, item.bgMode, item.fgsExempt,
                item.hasScene, item.hasMask, item.hasQuota, item.hasBgMode, item.hasFgsExempt);
    }

    private void renderTimeParams() {
        timeParamContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        Map<String, List<TombstoneXmlEditor.TimingItem>> byGroup = new LinkedHashMap<>();
        for (TombstoneXmlEditor.TimingItem item : vm.timingItemsByKey.values()) {
            if (!"freezeInterval".equals(item.nodeTag)) continue;
            String gk = item.modeTag + "|" + item.modeLevel;
            byGroup.computeIfAbsent(gk, ignored -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<TombstoneXmlEditor.TimingItem>> ge : byGroup.entrySet()) {
            List<TombstoneXmlEditor.TimingItem> modeItems = ge.getValue();
            if (modeItems.isEmpty()) continue;
            TombstoneXmlEditor.TimingItem firstMode = modeItems.get(0);
            TextView modeTitle = new TextView(requireContext());
            modeTitle.setText(modeDisplayName(firstMode.modeTag, firstMode.modeLevel));
            modeTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
            timeParamContainer.addView(modeTitle);

            Map<String, List<TombstoneXmlEditor.TimingItem>> byAppType = new LinkedHashMap<>();
            for (TombstoneXmlEditor.TimingItem item : modeItems) {
                byAppType.computeIfAbsent(item.nodeQualifier, ignored -> new ArrayList<>()).add(item);
            }

            for (Map.Entry<String, List<TombstoneXmlEditor.TimingItem>> ae : byAppType.entrySet()) {
                String appType = ae.getKey();
                List<TombstoneXmlEditor.TimingItem> items = ae.getValue();

                TextView appTypeTitle = new TextView(requireContext());
                appTypeTitle.setText(appTypeDisplayName(appType));
                appTypeTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
                timeParamContainer.addView(appTypeTitle);

                for (TombstoneXmlEditor.TimingItem item : items) {
                    if (isAdvancedFreezeField(item.attrName)) continue;
                    String key = item.key;
                    View row = inflater.inflate(R.layout.item_timing_param, timeParamContainer, false);
                    TextView tvName = row.findViewById(R.id.tvTimingName);
                    TextInputEditText etValue = row.findViewById(R.id.etTimingValue);
                    etValue.setSaveEnabled(false);
                    etValue.setFreezesText(false);
                    etValue.setId(View.generateViewId());
                    TextView tvUnit = row.findViewById(R.id.tvTimingUnit);
                    TextView tvHint = row.findViewById(R.id.tvTimingHint);
                    tvName.setText(freezeIntervalFieldTitle(item.attrName));
                    row.findViewById(R.id.btnTimingInfo).setOnClickListener(v -> showFreezeTimingInfo());
                    etValue.setText(vm.stagedTimingValues.get(key));
                    boolean editable = advancedEditEnabled();
                    etValue.setEnabled(editable);
                    if (!editable) etValue.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.advanced_editing_required, Toast.LENGTH_SHORT).show());
                    else etValue.setOnClickListener(null);
                    tvUnit.setVisibility(View.GONE);
                    tvHint.setVisibility(View.GONE);
                    etValue.addTextChangedListener(new SimpleTextWatcher(s2 -> {
                        vm.stagedTimingValues.put(key, String.valueOf(s2).trim());
                        updateDirtyBadge();
                    }));
                    timeParamContainer.addView(row);
                }

                LinearLayout advancedWrap = new LinearLayout(requireContext());
                advancedWrap.setOrientation(LinearLayout.VERTICAL);
                advancedWrap.setVisibility(View.GONE);
                for (TombstoneXmlEditor.TimingItem item : items) {
                    if (!isAdvancedFreezeField(item.attrName)) continue;
                    TextView v = new TextView(requireContext());
                    v.setText(item.attrName + " = " + vm.stagedTimingValues.get(item.key));
                    advancedWrap.addView(v);
                }
                if (advancedWrap.getChildCount() > 0) {
                    MaterialButton toggle = new MaterialButton(requireContext());
                    toggle.setText(R.string.advanced_fields);
                    toggle.setOnClickListener(v -> advancedWrap.setVisibility(advancedWrap.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
                    timeParamContainer.addView(toggle);
                    timeParamContainer.addView(advancedWrap);
                }

                if (shouldShowWhyAffectCta(firstMode.modeTag, firstMode.modeLevel, appType)) {
                    MaterialButton why = new MaterialButton(requireContext());
                    why.setText(R.string.why_affect_my_app);
                    why.setOnClickListener(v -> showWhyAffectDialog());
                    timeParamContainer.addView(why);
                }
            }
        }
    }

    private boolean shouldShowWhyAffectCta(String modeTag, String modeLevel, String appType) {
        return ("highExtremeMode".equals(modeTag) && "4".equals(appType))
                || ("extremeMode".equals(modeTag) && "4".equals(appType))
                || ("highLoadMode".equals(modeTag) && "1".equals(modeLevel) && "4#7".equals(appType));
    }

    private boolean isAdvancedFreezeField(String attr) {
        return !("guardTime".equals(attr) || "RToM".equals(attr) || "RToMST".equals(attr)
                || "MToF".equals(attr) || "MToFST".equals(attr) || "checkImportance".equals(attr));
    }

    private String modeDisplayName(String tag, String level) {
        if ("highExtremeMode".equals(tag)) return getString(R.string.mode_high_extreme);
        if ("extremeMode".equals(tag)) return getString(R.string.mode_extreme);
        if ("highPerfMode".equals(tag)) return getString(R.string.mode_high_perf);
        if ("highLoadMode".equals(tag) && "3".equals(level)) return getString(R.string.mode_high_load_l3);
        if ("highLoadMode".equals(tag) && "1".equals(level)) return getString(R.string.mode_high_load_l1);
        return tag + " (level=" + level + ")";
    }

    private String appTypeDisplayName(String appType) {
        if ("1000".equals(appType)) return getString(R.string.app_type_general);
        if ("4".equals(appType)) return getString(R.string.app_type_net);
        if ("4#7".equals(appType)) return getString(R.string.app_type_net_combo);
        return "appType=" + appType;
    }

    private void renderPolicies() {
        bindingPolicyViews = true;
        policyContainer.removeAllViews();
        addSectionHeader(policyContainer, R.string.policy_section_title, R.string.policy_section_body);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        addGroup(inflater, TombstoneXmlEditor.GROUP_POLICY, R.string.policy_group_sys_black_policy);
        addGroup(inflater, TombstoneXmlEditor.GROUP_RESTRICT, R.string.policy_group_sys_black_restrict);
        bindingPolicyViews = false;
    }

    private void addGroup(LayoutInflater inflater, String groupName, int titleRes) {
        TextView title = new TextView(requireContext());
        title.setText(titleRes);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        policyContainer.addView(title);

        for (TombstoneXmlEditor.PolicyItem draft : vm.policyDrafts.values()) {
            if (!groupName.equals(draft.groupName)) continue;
            TombstoneXmlEditor.PolicyItem source = vm.sourcePolicies.get(draft.id());
            if (source == null) continue;
            View card = inflater.inflate(R.layout.item_policy_editor_card, policyContainer, false);
            String scene = (draft.keyScene == null || draft.keyScene.isEmpty() ? "-" : draft.keyScene);
            String policyTitle = source.groupName.equals(TombstoneXmlEditor.GROUP_POLICY)
                    ? getString(R.string.policy_template_title, draft.name, scene)
                    : getString(R.string.policy_mask_title, draft.name);
            ((TextView) card.findViewById(R.id.tvPolicySceneTitle)).setText(policyTitle);
            ((TextView) card.findViewById(R.id.tvPolicyNameValue)).setText("group=" + draft.groupName);
            setSceneInfo(card, draft.name);

            card.findViewById(R.id.btnResetPolicyScene).setOnClickListener(v -> {
                vm.policyDrafts.put(draft.id(), clonePolicy(source));
                renderPolicies();
                Toast.makeText(requireContext(), R.string.modified_not_applied, Toast.LENGTH_SHORT).show();
            });

            bindAttr(card, R.id.rowScene, R.id.etSceneValue, source.hasScene, draft.scene, source.groupName.equals(TombstoneXmlEditor.GROUP_POLICY) && advancedEditEnabled(), value -> updateDraft(draft.id(), "scene", value));
            bindAttr(card, R.id.rowMask, R.id.etMaskValue, source.hasMask, draft.mask, true, value -> updateDraft(draft.id(), "mask", value));
            bindAttr(card, R.id.rowQuota, R.id.etQuotaValue, source.hasQuota, draft.quota, source.groupName.equals(TombstoneXmlEditor.GROUP_POLICY) && advancedEditEnabled(), value -> updateDraft(draft.id(), "quota", value));
            bindAttr(card, R.id.rowBgMode, R.id.etBgModeValue, source.hasBgMode, draft.bgMode, source.groupName.equals(TombstoneXmlEditor.GROUP_POLICY) && advancedEditEnabled(), value -> updateDraft(draft.id(), "bgMode", value));

            setInfo(card, R.id.btnSceneInfo, R.string.tombstone_param_scene_title, R.string.tombstone_param_scene_body);
            setInfo(card, R.id.btnMaskInfo, R.string.tombstone_param_mask_title, R.string.tombstone_param_mask_body);
            setInfo(card, R.id.btnQuotaInfo, R.string.tombstone_param_quota_title, R.string.tombstone_param_quota_body);
            setInfo(card, R.id.btnBgModeInfo, R.string.tombstone_param_bgmode_title, R.string.tombstone_param_bgmode_body);

            View fgsRow = card.findViewById(R.id.rowFgsExempt);
            if (source.hasFgsExempt) {
                fgsRow.setVisibility(View.VISIBLE);
                MaterialSwitch fgsSwitch = card.findViewById(R.id.switchFgsExempt);
                fgsSwitch.setEnabled(source.groupName.equals(TombstoneXmlEditor.GROUP_POLICY) && advancedEditEnabled());
                fgsSwitch.setChecked("1".equals(draft.fgsExempt) || "true".equalsIgnoreCase(draft.fgsExempt));
                fgsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateDraft(draft.id(), "fgsExempt", Boolean.toString(isChecked)));
                setInfo(card, R.id.btnFgsInfo, R.string.tombstone_param_fgsexempt_title, R.string.tombstone_param_fgsexempt_body);
            } else {
                fgsRow.setVisibility(View.GONE);
            }

            policyContainer.addView(card);
        }
    }

    private interface ValueCallback { void onValue(String value); }

    private void bindAttr(View root, int rowId, int fieldId, boolean visible, String value, boolean editable, ValueCallback callback) {
        View row = root.findViewById(rowId);
        if (!visible) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        TextInputEditText et = root.findViewById(fieldId);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setText(value);
        et.setEnabled(editable);
        if (!editable) {
            et.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.advanced_editing_required, Toast.LENGTH_SHORT).show());
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.clearFocus();
                    Toast.makeText(requireContext(), R.string.advanced_editing_required, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            et.setOnClickListener(null);
            et.setOnFocusChangeListener(null);
        }
        et.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (bindingPolicyViews) return;
            callback.onValue(String.valueOf(s));
        }));
    }

    private void updateDraft(String id, String field, String value) {
        if (bindingPolicyViews) return;
        TombstoneXmlEditor.PolicyItem old = vm.policyDrafts.get(id);
        if (old == null) return;
        TombstoneXmlEditor.PolicyItem next;
        if ("scene".equals(field)) next = new TombstoneXmlEditor.PolicyItem(old.groupName, old.name, old.keyScene, value, old.mask, old.quota, old.bgMode, old.fgsExempt, old.hasScene, old.hasMask, old.hasQuota, old.hasBgMode, old.hasFgsExempt);
        else if ("mask".equals(field)) next = new TombstoneXmlEditor.PolicyItem(old.groupName, old.name, old.keyScene, old.scene, value, old.quota, old.bgMode, old.fgsExempt, old.hasScene, old.hasMask, old.hasQuota, old.hasBgMode, old.hasFgsExempt);
        else if ("quota".equals(field)) next = new TombstoneXmlEditor.PolicyItem(old.groupName, old.name, old.keyScene, old.scene, old.mask, value, old.bgMode, old.fgsExempt, old.hasScene, old.hasMask, old.hasQuota, old.hasBgMode, old.hasFgsExempt);
        else if ("bgMode".equals(field)) next = new TombstoneXmlEditor.PolicyItem(old.groupName, old.name, old.keyScene, old.scene, old.mask, old.quota, value, old.fgsExempt, old.hasScene, old.hasMask, old.hasQuota, old.hasBgMode, old.hasFgsExempt);
        else next = new TombstoneXmlEditor.PolicyItem(old.groupName, old.name, old.keyScene, old.scene, old.mask, old.quota, old.bgMode, value, old.hasScene, old.hasMask, old.hasQuota, old.hasBgMode, old.hasFgsExempt);
        vm.policyDrafts.put(id, next);
        updateDirtyBadge();
    }

    private boolean isDirty() { return vm.isDirty(); }

    private void setSceneInfo(View root, String sceneName) {
        int titleRes;
        int bodyRes;
        if ("lcdon".equals(sceneName)) {
            titleRes = R.string.scene_info_lcdon_title;
            bodyRes = R.string.hint_lcdon;
        } else if ("lcdoff".equals(sceneName)) {
            titleRes = R.string.scene_info_lcdoff_title;
            bodyRes = R.string.hint_lcdoff;
        } else if ("night".equals(sceneName)) {
            titleRes = R.string.scene_info_night_title;
            bodyRes = R.string.hint_night;
        } else if ("charging".equals(sceneName)) {
            titleRes = R.string.scene_info_charging_title;
            bodyRes = R.string.hint_charging;
        } else if ("extremeFg".equals(sceneName)) {
            titleRes = R.string.scene_info_extremefg_title;
            bodyRes = R.string.hint_extremefg;
        } else if ("fastFreeze".equals(sceneName)) {
            titleRes = R.string.scene_info_fastfreeze_title;
            bodyRes = R.string.hint_fastfreeze;
        } else if ("minSystem".equals(sceneName)) {
            titleRes = R.string.scene_info_minsystem_title;
            bodyRes = R.string.hint_minsystem;
        } else {
            titleRes = R.string.tombstone_param_scene_title;
            bodyRes = R.string.tombstone_param_scene_body;
        }
        root.findViewById(R.id.btnPolicyNameInfo).setOnClickListener(v -> showParamInfoDialog(titleRes, bodyRes));
    }

    private void setInfo(View root, int buttonId, int titleRes, int bodyRes) {
        root.findViewById(buttonId).setOnClickListener(v -> showParamInfoDialog(titleRes, bodyRes));
    }

    private void showParamInfoDialog(int titleResId, int bodyResId) {
        String body = getString(bodyResId) + "\n\n" + getString(R.string.hint_inference_disclaimer);
        new MaterialAlertDialogBuilder(requireContext()).setTitle(titleResId).setMessage(body).setPositiveButton(R.string.tombstone_param_dialog_ok, null).show();
    }

    private void renderSpecialHandling() {
        if (specialLowContainer == null) return;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        specialLowContainer.removeAllViews();
        specialMediumContainer.removeAllViews();
        specialHighContainer.removeAllViews();

        addSectionHeader(specialLowContainer, R.string.section_enable_config_title, R.string.section_enable_config_body);
        addEnableConfigCard();

        addSectionHeader(specialLowContainer, R.string.section_whitelist_title, R.string.section_whitelist_body);
        if (vm.hasGms) addLowCard(inflater, R.string.special_gms_title, vm.stagedGms, REQ_GMS);
        if (vm.hasFfPkgBlack) addLowCard(inflater, R.string.special_ff_skip_title, vm.stagedFfSkipFastFreeze, REQ_FF_SKIP);
        if (vm.hasWhitePkg) {
            addLowCard(inflater, R.string.special_white_100_title, vm.stagedWhitePkgByCategory.getOrDefault("100", new LinkedHashSet<>()), REQ_WHITE_100);
            if (advancedEditEnabled()) {
                addLowCard(inflater, R.string.special_white_010_title, vm.stagedWhitePkgByCategory.getOrDefault("010", new LinkedHashSet<>()), REQ_WHITE_010);
                addLowCard(inflater, R.string.special_white_001_title, vm.stagedWhitePkgByCategory.getOrDefault("001", new LinkedHashSet<>()), REQ_WHITE_001);
            }
        }
        if (vm.hasBlackHansPkg) addBlackHansCard();
        addLowCard(inflater, R.string.special_im_pkg_title, vm.stagedImPkg, REQ_IMPK);
        addLowCard(inflater, R.string.special_cpu_ctl_title, vm.stagedCpuCtlWhiteList, REQ_CPUCTL);
        addLowCard(inflater, R.string.special_app_card_title, vm.stagedAppCardIgnore, REQ_APPCARD);

        LinearLayout pkgConfigContainer = addCollapsibleGroup(
                specialMediumContainer,
                R.string.section_pkg_config_title,
                R.string.section_pkg_config_body,
                vm.uiExpandPkgConfig,
                expanded -> vm.uiExpandPkgConfig = expanded);
        addPkgConfigCard(pkgConfigContainer);

        LinearLayout binderContainer = addCollapsibleGroup(
                specialHighContainer,
                R.string.section_binder_title,
                R.string.section_binder_body,
                vm.uiExpandBinder,
                expanded -> vm.uiExpandBinder = expanded);
        addBinderCard(binderContainer);

        addSectionHeader(specialHighContainer, R.string.section_extreme_title, R.string.section_extreme_body);
        addV3CCards();
    }

    private interface ExpandSetter { void set(boolean expanded); }

    private void addSectionHeader(ViewGroup parent, int titleRes, int bodyRes) {
        TextView title = new TextView(requireContext());
        title.setText(titleRes);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        parent.addView(title);
        TextView body = new TextView(requireContext());
        body.setText(bodyRes);
        body.setPadding(0, 6, 0, 12);
        parent.addView(body);
    }

    private LinearLayout addCollapsibleGroup(ViewGroup parent, int titleRes, int bodyRes, boolean defaultExpanded, ExpandSetter setter) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText(titleRes);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        wrap.addView(title);
        TextView body = new TextView(requireContext());
        body.setText(bodyRes);
        wrap.addView(body);
        MaterialButton toggle = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(defaultExpanded ? View.VISIBLE : View.GONE);
        toggle.setText(defaultExpanded ? R.string.collapse : R.string.expand);
        toggle.setOnClickListener(v -> {
            boolean expanded = content.getVisibility() != View.VISIBLE;
            content.setVisibility(expanded ? View.VISIBLE : View.GONE);
            toggle.setText(expanded ? R.string.collapse : R.string.expand);
            setter.set(expanded);
        });
        wrap.addView(toggle);
        wrap.addView(content);
        parent.addView(wrap);
        return content;
    }

    private void addEnableConfigCard() {
        if (!vm.hasEnableConfig) return;
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 12;
        card.setLayoutParams(cardLp);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);
        TextView title = new TextView(requireContext());
        title.setText(R.string.special_enable_config_title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        content.addView(title);
        String[] keys = new String[]{"hansEnable", "gmsEnable", "skipToast", "releaseStatistic", "cgp_v2"};
        for (String key : keys) {
            if (!vm.stagedEnableConfigAttrs.containsKey(key)) continue;
            MaterialSwitch sw = new MaterialSwitch(requireContext());
            sw.setText(key);
            sw.setChecked("true".equalsIgnoreCase(vm.stagedEnableConfigAttrs.get(key)) || "1".equals(vm.stagedEnableConfigAttrs.get(key)));
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vm.stagedEnableConfigAttrs.put(key, isChecked ? "true" : "false");
                updateDirtyBadge();
            });
            content.addView(sw);
        }
        card.addView(content);
        specialLowContainer.addView(card);
    }

    private void addBlackHansCard() {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 12;
        card.setLayoutParams(cardLp);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);
        TextView title = new TextView(requireContext());
        title.setText(R.string.special_black_hans_title);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        content.addView(title);
        MaterialSwitch sw = new MaterialSwitch(requireContext());
        sw.setText("enable");
        sw.setChecked(vm.stagedBlackHansEnabled);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> { vm.stagedBlackHansEnabled = isChecked; updateDirtyBadge(); });
        content.addView(sw);
        TextView summary = new TextView(requireContext());
        summary.setText(vm.stagedBlackHansTokens.isEmpty() ? getString(R.string.not_configured) : String.join("\n", vm.stagedBlackHansTokens));
        content.addView(summary);
        MaterialButton pick = new MaterialButton(requireContext());
        pick.setText(R.string.select_app);
        pick.setOnClickListener(v -> openPicker(REQ_BLACK_HANS, new LinkedHashSet<>(vm.stagedBlackHansTokens)));
        content.addView(pick);
        card.addView(content);
        specialLowContainer.addView(card);
    }

    private void addLowCard(LayoutInflater inflater, int titleRes, Set<String> values, int req) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 12;
        card.setLayoutParams(cardLp);
        card.setUseCompatPadding(true);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(requireContext());
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        title.setText(titleRes);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        title.setLineSpacing(0f, 1.15f);

        ImageButton info = new ImageButton(requireContext());
        int infoSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, requireContext().getResources().getDisplayMetrics());
        info.setLayoutParams(new LinearLayout.LayoutParams(infoSize, infoSize));
        info.setBackgroundColor(0x00000000);
        info.setImageResource(R.drawable.ic_info_md);
        info.setOnClickListener(v -> showSectionExplain(titleRes));

        TextView summary = new TextView(requireContext());
        summary.setPadding(0, 10, 0, 0);
        summary.setLineSpacing(0f, 1.18f);
        summary.setText(values.isEmpty() ? getString(R.string.not_configured) : String.join("\n", values));

        MaterialButton pick = new MaterialButton(requireContext());
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pickLp.topMargin = 10;
        pick.setLayoutParams(pickLp);
        pick.setMaxLines(2);
        pick.setText(R.string.select_app);
        pick.setOnClickListener(v -> openPicker(req, values));

        titleRow.addView(title);
        titleRow.addView(info);
        content.addView(titleRow);
        content.addView(summary);
        content.addView(pick);
        card.addView(content);
        specialLowContainer.addView(card);
    }

    private void addV3CCards() {
        addSimpleTokenCard(specialMediumContainer, getString(R.string.v3c_service_bump_title), vm.stagedServiceBumpReasons,
                () -> openEditableListSheet(RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null), false);

        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_prevent_title), "rules=" + vm.stagedPreventRules.size(),
                () -> showSceneRuleEditor("prevent", vm.stagedPreventRules), true);
        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_allow_title), "rules=" + vm.stagedAllowRules.size(),
                () -> showSceneRuleEditor("allow", vm.stagedAllowRules), true);
        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_android_freeze_title),
                "enable=" + vm.stagedAndroidFreezeConfig.enable + ", white=" + vm.stagedAndroidFreezeConfig.whiteEnable
                        + ", killWhite=" + vm.stagedAndroidFreezeConfig.killWhiteEnable + ", exempt=" + vm.stagedAndroidFreezeConfig.exemptQuotas.size(),
                this::showAndroidFreezeEditor, true);
        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_app_quota_title),
                "maxDuration=" + vm.stagedAppQuotaConfig.maxDuration + ", protect=" + vm.stagedAppQuotaConfig.protectTimeAfterExceed,
                this::showAppQuotaEditor, true);
        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_high_loading_title),
                vm.stagedHighLoadingConfigs.isEmpty() ? getString(R.string.not_configured) : "configs=" + vm.stagedHighLoadingConfigs.size(),
                this::showHighLoadingEditor, true);
        addSimpleSummaryCard(specialHighContainer, getString(R.string.v3c_cpu_ctl_rus_title),
                vm.stagedCpuCtlRus.attrs.isEmpty() ? getString(R.string.not_configured) : vm.stagedCpuCtlRus.attrs.toString(),
                this::showCpuCtlRusEditor, true);
    }

    private void addSimpleTokenCard(LinearLayout container, String title, Set<String> values, Runnable onEdit, boolean highRisk) {
        addSimpleSummaryCard(container, title, values == null || values.isEmpty() ? getString(R.string.not_configured) : String.join("\n", values), onEdit, highRisk);
    }

    private void addSimpleSummaryCard(LinearLayout container, String title, String summary, Runnable onEdit, boolean highRisk) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = 12;
        card.setLayoutParams(cardLp);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        content.addView(tvTitle);
        TextView tv = new TextView(requireContext());
        tv.setText(summary);
        content.addView(tv);
        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(R.string.edit_section_list);
        btn.setOnClickListener(v -> {
            if (highRisk && !advancedEditEnabled()) {
                Toast.makeText(requireContext(), R.string.advanced_editing_required, Toast.LENGTH_SHORT).show();
                return;
            }
            onEdit.run();
        });
        content.addView(btn);
        card.addView(content);
        container.addView(card);
    }

    private void showSceneRuleEditor(String tag, List<TombstoneXmlEditor.SceneRule> current) {
        List<String> lines = new ArrayList<>();
        for (TombstoneXmlEditor.SceneRule r : current) lines.add(r.scene + "," + r.pkg + "," + r.mask + "," + r.version + "," + r.code);
        openEditableListSheet("prevent".equals(tag) ? RK_PREVENT : RK_ALLOW, tag, null, false, null);
    }

    private void applySceneRuleEdit(String tag, ArrayList<String> edited) {
        List<TombstoneXmlEditor.SceneRule> out = new ArrayList<>();
        for (int i = 0; i < edited.size(); i++) {
            String[] parts = edited.get(i).split(",", -1);
            String scene = parts.length > 0 ? parts[0].trim() : "";
            String pkg = parts.length > 1 ? parts[1].trim() : "";
            String mask = parts.length > 2 ? parts[2].trim() : "";
            String version = parts.length > 3 ? parts[3].trim() : "";
            String code = parts.length > 4 ? parts[4].trim() : "";
            if (pkg.isEmpty()) continue;
            out.add(new TombstoneXmlEditor.SceneRule(tag, i, scene, pkg, mask, version, code, ""));
        }
        if (out.isEmpty() && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        if ("prevent".equals(tag)) vm.stagedPreventRules = out; else vm.stagedAllowRules = out;
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }

    private void showAndroidFreezeEditor() {
        List<String> lines = new ArrayList<>();
        lines.add("enable=" + vm.stagedAndroidFreezeConfig.enable);
        lines.add("whiteEnable=" + vm.stagedAndroidFreezeConfig.whiteEnable);
        lines.add("killWhiteEnable=" + vm.stagedAndroidFreezeConfig.killWhiteEnable);
        lines.add("sysAppExemptEnable=" + vm.stagedAndroidFreezeConfig.sysAppExemptEnable);
        for (TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota ex : vm.stagedAndroidFreezeConfig.exemptQuotas) lines.add(ex.pkg + "," + ex.bgMode);
        openEditableListSheet(RK_ANDROID_FREEZE, getString(R.string.v3c_android_freeze_title), null, false, null);
    }

    private void applyAndroidFreezeEdit(ArrayList<String> edited) {
        TombstoneXmlEditor.AndroidFreezeConfig next = new TombstoneXmlEditor.AndroidFreezeConfig();
        next.enable = vm.stagedAndroidFreezeConfig.enable;
        next.whiteEnable = vm.stagedAndroidFreezeConfig.whiteEnable;
        next.killWhiteEnable = vm.stagedAndroidFreezeConfig.killWhiteEnable;
        next.sysAppExemptEnable = vm.stagedAndroidFreezeConfig.sysAppExemptEnable;
        boolean hasValid = false;
        for (String rawLine : edited) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("enable=")) {
                next.enable = "true".equalsIgnoreCase(line.substring("enable=".length()).trim());
                hasValid = true;
            } else if (line.startsWith("whiteEnable=")) {
                next.whiteEnable = "true".equalsIgnoreCase(line.substring("whiteEnable=".length()).trim());
                hasValid = true;
            } else if (line.startsWith("killWhiteEnable=")) {
                next.killWhiteEnable = "true".equalsIgnoreCase(line.substring("killWhiteEnable=".length()).trim());
                hasValid = true;
            } else if (line.startsWith("sysAppExemptEnable=")) {
                next.sysAppExemptEnable = "true".equalsIgnoreCase(line.substring("sysAppExemptEnable=".length()).trim());
                hasValid = true;
            } else {
                if (line.contains("=")) continue;
                String[] p = line.split(",", -1);
                if (p.length == 0 || p.length > 2) continue;
                String pkg = p[0].trim();
                if (pkg.isEmpty() || pkg.contains("=") || pkg.contains(" ") || !pkg.contains(".")) continue;
                String bgMode = p.length > 1 ? p[1].trim() : "";
                if (!bgMode.isEmpty() && !bgMode.matches("^[0-9]+$")) continue;
                TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota ex = new TombstoneXmlEditor.AndroidFreezeConfig.ExemptQuota();
                ex.pkg = pkg;
                ex.bgMode = bgMode;
                next.exemptQuotas.add(ex);
                hasValid = true;
            }
        }
        if (!hasValid && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        vm.stagedAndroidFreezeConfig.enable = next.enable;
        vm.stagedAndroidFreezeConfig.whiteEnable = next.whiteEnable;
        vm.stagedAndroidFreezeConfig.killWhiteEnable = next.killWhiteEnable;
        vm.stagedAndroidFreezeConfig.sysAppExemptEnable = next.sysAppExemptEnable;
        vm.stagedAndroidFreezeConfig.exemptQuotas.clear();
        vm.stagedAndroidFreezeConfig.exemptQuotas.addAll(next.exemptQuotas);
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }

    private void showAppQuotaEditor() {
        List<String> lines = new ArrayList<>();
        lines.add("maxDuration=" + vm.stagedAppQuotaConfig.maxDuration);
        lines.add("protectTimeAfterExceed=" + vm.stagedAppQuotaConfig.protectTimeAfterExceed);
        openEditableListSheet(RK_APP_QUOTA, getString(R.string.v3c_app_quota_title), null, false, null);
    }

    private void applyAppQuotaEdit(ArrayList<String> edited) {
        String maxDuration = vm.stagedAppQuotaConfig.maxDuration;
        String protectTimeAfterExceed = vm.stagedAppQuotaConfig.protectTimeAfterExceed;
        boolean hasValid = false;
        for (String line : edited) {
            if (line.startsWith("maxDuration=")) {
                maxDuration = line.substring("maxDuration=".length()).trim();
                hasValid = true;
            }
            if (line.startsWith("protectTimeAfterExceed=")) {
                protectTimeAfterExceed = line.substring("protectTimeAfterExceed=".length()).trim();
                hasValid = true;
            }
        }
        if (!hasValid && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        vm.stagedAppQuotaConfig.maxDuration = maxDuration;
        vm.stagedAppQuotaConfig.protectTimeAfterExceed = protectTimeAfterExceed;
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }

    private void showHighLoadingEditor() {
        TombstoneXmlEditor.HighLoadingConfig cfg = vm.stagedHighLoadingConfigs.isEmpty() ? new TombstoneXmlEditor.HighLoadingConfig() : vm.stagedHighLoadingConfigs.get(0);
        List<String> lines = new ArrayList<>();
        lines.add("skipEnable=" + cfg.skipFramesEnable);
        lines.add("skipDuration=" + cfg.skipFramesDuration);
        lines.add("skipCount=" + cfg.skipFramesCount);
        lines.add("cpuEnable=" + cfg.cpuLoadingEnable);
        lines.add("cpuTopCnt=" + cfg.cpuLoadingTopCnt);
        lines.add("total=" + cfg.thresholdTotal);
        openEditableListSheet(RK_HIGH_LOADING, getString(R.string.v3c_high_loading_title), null, false, null);
    }

    private void applyHighLoadingEdit(ArrayList<String> edited) {
        TombstoneXmlEditor.HighLoadingConfig base = vm.stagedHighLoadingConfigs.isEmpty() ? new TombstoneXmlEditor.HighLoadingConfig() : vm.stagedHighLoadingConfigs.get(0);
        TombstoneXmlEditor.HighLoadingConfig out = new TombstoneXmlEditor.HighLoadingConfig();
        out.skipFramesEnable = base.skipFramesEnable;
        out.skipFramesDuration = base.skipFramesDuration;
        out.skipFramesCount = base.skipFramesCount;
        out.cpuLoadingEnable = base.cpuLoadingEnable;
        out.cpuLoadingTopCnt = base.cpuLoadingTopCnt;
        out.thresholdTotal = base.thresholdTotal;
        boolean hasValid = false;
        for (String line : edited) {
            if (line.startsWith("skipEnable=")) {
                out.skipFramesEnable = line.endsWith("true");
                hasValid = true;
            } else if (line.startsWith("skipDuration=")) {
                out.skipFramesDuration = line.substring("skipDuration=".length()).trim();
                hasValid = true;
            } else if (line.startsWith("skipCount=")) {
                out.skipFramesCount = line.substring("skipCount=".length()).trim();
                hasValid = true;
            } else if (line.startsWith("cpuEnable=")) {
                out.cpuLoadingEnable = line.endsWith("true");
                hasValid = true;
            } else if (line.startsWith("cpuTopCnt=")) {
                out.cpuLoadingTopCnt = line.substring("cpuTopCnt=".length()).trim();
                hasValid = true;
            } else if (line.startsWith("total=")) {
                out.thresholdTotal = line.substring("total=".length()).trim();
                hasValid = true;
            }
        }
        if (!hasValid && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        vm.stagedHighLoadingConfigs.clear();
        vm.stagedHighLoadingConfigs.add(out);
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }

    private void showCpuCtlRusEditor() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : vm.stagedCpuCtlRus.attrs.entrySet()) lines.add(e.getKey() + "=" + e.getValue());
        openEditableListSheet(RK_CPU_CTL_RUS, getString(R.string.v3c_cpu_ctl_rus_title), null, false, null);
    }

    private void applyCpuCtlRusEdit(ArrayList<String> edited) {
        LinkedHashMap<String, String> next = new LinkedHashMap<>();
        for (String line : edited) {
            int idx = line.indexOf('=');
            if (idx <= 0) continue;
            String key = line.substring(0, idx).trim();
            if (key.isEmpty()) continue;
            next.put(key, line.substring(idx + 1).trim());
        }
        if (next.isEmpty() && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        vm.stagedCpuCtlRus.attrs.clear();
        vm.stagedCpuCtlRus.attrs.putAll(next);
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }

    private void addPkgConfigCard(LinearLayout container) {
        for (Map.Entry<String, java.util.List<String>> e : vm.stagedPkgConfigTokens.entrySet()) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setUseCompatPadding(true);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = 12;
            card.setLayoutParams(cardLp);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(20, 18, 20, 18);

            TextView tv = new TextView(requireContext());
            tv.setLineSpacing(0f, 1.18f);
            tv.setText(getString(R.string.special_pkg_config_title) + "\ncategory=" + e.getKey() + " · " + e.getValue().size() + " tokens");
            content.addView(tv);
            MaterialButton pick = new MaterialButton(requireContext());
            pick.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pick.setMaxLines(2);
            pick.setPadding(0, 10, 0, 0);
            pick.setText(R.string.select_app);
            pick.setOnClickListener(v -> showPkgConfigEditor(e.getKey()));
            content.addView(pick);
            card.addView(content);
            container.addView(card);
        }
    }

    private void addBinderCard(LinearLayout container) {
        for (TombstoneXmlEditor.BinderRuleItem item : vm.stagedBinderRules) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setUseCompatPadding(true);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = 12;
            card.setLayoutParams(cardLp);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(20, 18, 20, 18);

            TextView tv = new TextView(requireContext());
            tv.setLineSpacing(0f, 1.18f);
            tv.setText(getString(R.string.special_cpn_title) + "\n" + item.type + " / " + item.name + " / aid=" + item.aid);
            content.addView(tv);
            MaterialButton pick = new MaterialButton(requireContext());
            pick.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pick.setMaxLines(2);
            pick.setPadding(0, 10, 0, 0);
            pick.setText(R.string.select_app);
            pick.setOnClickListener(v -> showBinderEditor(item.index));
            content.addView(pick);
            card.addView(content);
            container.addView(card);
        }
    }

    private void showPkgConfigEditor(String category) {
        List<String> old = vm.stagedPkgConfigTokens.get(category);
        if (old == null) old = new ArrayList<>();
        List<String> tokens = new ArrayList<>(old);
        openEditableListSheet(RK_PKGCONFIG_PREFIX + category,
                getString(R.string.special_pkg_config_title) + " #" + category,
                null,
                true,
                getString(R.string.select_app));
    }

    private void showBinderEditor(int ruleIndex) {
        TombstoneXmlEditor.BinderRuleItem found = null;
        for (TombstoneXmlEditor.BinderRuleItem item : vm.stagedBinderRules) if (item.index == ruleIndex) { found = item; break; }
        if (found == null) return;
        List<String> tokens = new ArrayList<>(found.pkgTokens);
        openEditableListSheet(RK_BINDER_PREFIX + ruleIndex,
                getString(R.string.special_cpn_title) + " #" + ruleIndex,
                null,
                true,
                getString(R.string.select_app));
    }

    private void mergePkgConfigTokens(String category, Set<String> selected) {
        List<String> old = vm.stagedPkgConfigTokens.get(category);
        List<String> keepNonPkg = new ArrayList<>();
        if (old != null) {
            for (String token : old) if (!isPkgToken(token)) keepNonPkg.add(token);
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(keepNonPkg);
        for (String pkg : selected) merged.add(pkg);
        vm.stagedPkgConfigTokens.put(category, new ArrayList<>(merged));
        updateDirtyBadge();
        renderSpecialHandling();
    }

    private void mergeBinderTokens(int ruleIndex, Set<String> selected) {
        for (int i = 0; i < vm.stagedBinderRules.size(); i++) {
            TombstoneXmlEditor.BinderRuleItem item = vm.stagedBinderRules.get(i);
            if (item.index != ruleIndex) continue;
            vm.stagedBinderRules.set(i, item.copyWithPkgTokens(new ArrayList<>(new LinkedHashSet<>(selected))));
            updateDirtyBadge();
            renderSpecialHandling();
            return;
        }
    }

    private boolean isPkgToken(String t) {
        if (t == null) return false;
        String s = t.trim();
        return s.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+");
    }

    private void showProxySensorEditor() {
        ProxySensorEditorBottomSheet.newInstance(vm.stagedProxySensorConfig)
                .show(getChildFragmentManager(), "proxy_sensor_editor");
    }

    private void showProxyBinderEditor() {
        ProxyBinderEditorBottomSheet.newInstance(vm.stagedProxyBinderConfig)
                .show(getChildFragmentManager(), "proxy_binder_editor");
    }

    @Override
    public void onProxySensorEditorConfirmed(boolean hasEnable, boolean enable, boolean hasProxyType, boolean proxyType, String proxyPkgName, ArrayList<String> proxyTypeNames, ArrayList<TombstoneXmlEditor.WhiteTypePkgRule> whiteTypeRules) {
        vm.stagedProxySensorConfig.hasEnable = hasEnable;
        vm.stagedProxySensorConfig.enable = enable;
        vm.stagedProxySensorConfig.hasProxyType = hasProxyType;
        vm.stagedProxySensorConfig.proxyType = proxyType;
        vm.stagedProxySensorConfig.proxyPkgName = proxyPkgName == null ? "" : proxyPkgName.trim();
        vm.stagedProxySensorConfig.proxyTypeNames.clear();
        vm.stagedProxySensorConfig.proxyTypeNames.addAll(proxyTypeNames);
        vm.stagedProxySensorConfig.whiteTypePkgs.clear();
        vm.stagedProxySensorConfig.whiteTypePkgs.addAll(whiteTypeRules);
        updateDirtyBadge();
        renderProxySection();
    }

    @Override
    public void onProxyBinderEditorConfirmed(boolean hasEnable, boolean enable, ArrayList<TombstoneXmlEditor.BinderDescRule> descRules) {
        vm.stagedProxyBinderConfig.hasEnable = hasEnable;
        vm.stagedProxyBinderConfig.enable = enable;
        vm.stagedProxyBinderConfig.descConfigs.clear();
        vm.stagedProxyBinderConfig.descConfigs.addAll(descRules);
        updateDirtyBadge();
        renderProxySection();
    }

    private void renderProxySection() {
        if (proxyContainer == null) return;
        proxyContainer.removeAllViews();
        addSectionHeader(proxyContainer, R.string.proxy_semantic_title, R.string.proxy_semantic_body);
        addProxyGlobalCard();
        addProxyStringListCard(getString(R.string.proxy_default_bc_title), getString(R.string.proxy_default_bc_explain), vm.stagedDefaultProxyBcActions);
        addProxySensorCard();
        addProxySimpleToggleCard(getString(R.string.proxy_gps_title), getString(R.string.proxy_gps_explain), vm.stagedProxyGpsConfig.enable, v -> vm.stagedProxyGpsConfig.enable = v);
        addProxySimpleToggleCard(getString(R.string.proxy_bt_scan_title), getString(R.string.proxy_bt_scan_explain), vm.stagedProxyBtScanConfig.enable, v -> vm.stagedProxyBtScanConfig.enable = v);
        addProxyBinderCard();
    }

    private void addProxySensorCard() {
        int occ = countOccurrences("proxySensor");
        String suffix = getString(R.string.proxy_occurrence_suffix, occ, Math.max(occ, 1));
        String summary = getString(
                R.string.proxy_sensor_summary,
                String.valueOf(vm.stagedProxySensorConfig.enable),
                String.valueOf(vm.stagedProxySensorConfig.proxyType),
                vm.stagedProxySensorConfig.proxyPkgName,
                vm.stagedProxySensorConfig.proxyTypeNames.size(),
                vm.stagedProxySensorConfig.whiteTypePkgs.size(),
                suffix
        );
        addSimpleSummaryCard(proxyContainer, getString(R.string.proxy_sensor_title), summary, this::showProxySensorEditor, false);
    }

    private void addProxyBinderCard() {
        int occ = countOccurrences("proxyBinder");
        String suffix = getString(R.string.proxy_occurrence_suffix, occ, Math.max(occ, 1));
        String summary = getString(
                R.string.proxy_binder_summary,
                String.valueOf(vm.stagedProxyBinderConfig.enable),
                vm.stagedProxyBinderConfig.descConfigs.size(),
                suffix
        );
        addSimpleSummaryCard(proxyContainer, getString(R.string.proxy_binder_title), summary, this::showProxyBinderEditor, false);
    }

    private interface BoolSetter { void set(boolean v); }

    private void addProxySimpleToggleCard(String titleText, String explain, boolean checked, BoolSetter setter) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(requireContext());
        t.setText(titleText);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(t);
        ImageButton info = buildInlineInfoButton(() -> showProxyInfoDialog(titleText, explain));
        titleRow.addView(info);
        content.addView(titleRow);

        TextView e = new TextView(requireContext());
        e.setText(explain);
        content.addView(e);
        MaterialSwitch sw = new MaterialSwitch(requireContext());
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((b, isChecked) -> { setter.set(isChecked); updateDirtyBadge(); });
        content.addView(sw);
        card.addView(content);
        proxyContainer.addView(card);
    }

    private void addProxyStringListCard(String titleText, String explain, List<String> values) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(requireContext());
        t.setText(titleText);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(t);
        ImageButton info = buildInlineInfoButton(() -> showProxyInfoDialog(titleText, explain));
        titleRow.addView(info);
        content.addView(titleRow);

        TextView e = new TextView(requireContext());
        e.setText(explain + "\n" + (values == null ? "" : String.join("\n", values)));
        content.addView(e);
        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(R.string.edit_section_list);
        btn.setSingleLine(true);
        btn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        btn.setOnClickListener(v -> openEditableListSheet(resolveProxyRequestKey(titleText), titleText, explain, false, null));
        content.addView(btn);
        card.addView(content);
        proxyContainer.addView(card);
    }

    private ImageButton buildInlineInfoButton(Runnable onClick) {
        ImageButton info = new ImageButton(requireContext());
        int infoSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, requireContext().getResources().getDisplayMetrics());
        info.setLayoutParams(new LinearLayout.LayoutParams(infoSize, infoSize));
        info.setBackgroundColor(0x00000000);
        info.setImageResource(R.drawable.ic_info_md);
        info.setOnClickListener(v -> onClick.run());
        return info;
    }

    private void showProxyInfoDialog(String titleText, String explain) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleText)
                .setMessage(explain)
                .setPositiveButton(R.string.tombstone_param_dialog_ok, null)
                .show();
    }

    private void addProxyGlobalCard() {
        if (!vm.stagedProxyGlobalConfig.hasAny) return;
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setUseCompatPadding(true);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);
        TextView t = new TextView(requireContext());
        t.setText(R.string.proxy_global_title);
        content.addView(t);
        MaterialSwitch s1 = new MaterialSwitch(requireContext()); s1.setText("alarm"); s1.setChecked(vm.stagedProxyGlobalConfig.alarm); s1.setOnCheckedChangeListener((b,c)->{vm.stagedProxyGlobalConfig.alarm=c;updateDirtyBadge();});
        MaterialSwitch s2 = new MaterialSwitch(requireContext()); s2.setText("service"); s2.setChecked(vm.stagedProxyGlobalConfig.service); s2.setOnCheckedChangeListener((b,c)->{vm.stagedProxyGlobalConfig.service=c;updateDirtyBadge();});
        MaterialSwitch s3 = new MaterialSwitch(requireContext()); s3.setText("job"); s3.setChecked(vm.stagedProxyGlobalConfig.job); s3.setOnCheckedChangeListener((b,c)->{vm.stagedProxyGlobalConfig.job=c;updateDirtyBadge();});
        MaterialSwitch s4 = new MaterialSwitch(requireContext()); s4.setText("broadcast"); s4.setChecked(vm.stagedProxyGlobalConfig.broadcast); s4.setOnCheckedChangeListener((b,c)->{vm.stagedProxyGlobalConfig.broadcast=c;updateDirtyBadge();});
        content.addView(s1);content.addView(s2);content.addView(s3);content.addView(s4);
        TextInputLayout til = new TextInputLayout(requireContext());
        TextInputEditText et = new TextInputEditText(requireContext());
        et.setText(vm.stagedProxyGlobalConfig.proxyBCmax);
        et.addTextChangedListener(new SimpleTextWatcher(v -> { vm.stagedProxyGlobalConfig.proxyBCmax = String.valueOf(v); updateDirtyBadge(); }));
        til.addView(et);content.addView(til);
        card.addView(content);
        proxyContainer.addView(card);
    }

    private void openEditableListSheet(String requestKey, String title, @Nullable String description, boolean enablePickApps, @Nullable String pickAppsLabel) {
        String tag = "editable_list_" + requestKey;
        Fragment prev = getChildFragmentManager().findFragmentByTag(tag);
        if (prev instanceof DialogFragment) {
            ((DialogFragment) prev).dismissAllowingStateLoss();
        }
        EditorSpec spec = EditorRegistry.get(requestKey);
        ArrayList<String> items = new ArrayList<>(spec.serialize(vm, requestKey));
        ValidationResult selfCheck = spec.validate(items, requestKey);
        if (!selfCheck.ok) {
            Log.e("EditorSpec", "serialize invalid key=" + requestKey + " errors=" + selfCheck.errors + " lines=" + items);
            if (advancedEditEnabled()) {
                UiNotifier.showMessage(this, getString(R.string.editor_spec_self_check_warning));
            }
        }
        String mode = spec.mode();
        String desc2 = description != null ? description : spec.description(vm, requestKey);
        EditableListEditorBottomSheet.newInstance(requestKey, title, desc2, items, mode, enablePickApps, pickAppsLabel)
                .show(getChildFragmentManager(), tag);
    }

    private String resolveProxyRequestKey(String titleText) {
        if (titleText.equals(getString(R.string.proxy_default_bc_title))) return RK_PROXY_DEFAULT_BC;
        if (titleText.equals(getString(R.string.proxy_sensor_type_title))) return RK_PROXY_SENSOR_TYPES;
        return titleText;
    }

    @Override
    public void onListEditorConfirmed(String requestKey, ArrayList<String> edited) {
        AppLogger.i("ListEditor", "confirmed key=" + requestKey + " size=" + edited.size());
        EditorSpec spec = EditorRegistry.get(requestKey);
        EditorSpec.ApplyResult result = spec.apply(vm, edited, requestKey);
        if (!result.ok) {
            UiNotifier.showMessage(this, result.message == null ? getString(R.string.edit_invalid_all_ignored) : result.message);
            return;
        }
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (RK_PROXY_DEFAULT_BC.equals(requestKey) || RK_PROXY_SENSOR_TYPES.equals(requestKey)) {
            if (root != null) root.post(this::renderProxySection); else renderProxySection();
        } else {
            if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        }
        updateDirtyBadge();
    }



    @Override
    public void onListEditorCancelled(String requestKey) {
        // no-op
    }

    @Override
    public void onListEditorPickAppsRequested(String requestKey, ArrayList<String> current) {
        LinkedHashSet<String> pkgSet = new LinkedHashSet<>(current);
        if (requestKey.startsWith(RK_PKGCONFIG_PREFIX)) {
            pendingPkgConfigCategory = requestKey.substring(RK_PKGCONFIG_PREFIX.length());
            openPicker(REQ_PKGCONFIG_PICK, pkgSet);
            return;
        }
        if (requestKey.startsWith(RK_BINDER_PREFIX)) {
            try {
                pendingBinderIndex = Integer.parseInt(requestKey.substring(RK_BINDER_PREFIX.length()));
                openPicker(REQ_BINDER_PICK, pkgSet);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics());
    }

    private void showSectionExplain(int titleRes) {
        int msg = R.string.special_im_pkg_explain;
        if (titleRes == R.string.special_cpu_ctl_title) msg = R.string.special_cpu_ctl_explain;
        if (titleRes == R.string.special_app_card_title) msg = R.string.special_app_card_explain;
        if (titleRes == R.string.special_enable_config_title) msg = R.string.special_enable_config_explain;
        if (titleRes == R.string.special_gms_title) msg = R.string.special_gms_explain;
        if (titleRes == R.string.special_ff_skip_title) msg = R.string.special_ff_skip_explain;
        if (titleRes == R.string.special_white_100_title) msg = R.string.special_white_100_explain;
        if (titleRes == R.string.special_white_010_title) msg = R.string.special_white_010_explain;
        if (titleRes == R.string.special_white_001_title) msg = R.string.special_white_001_explain;
        if (titleRes == R.string.special_black_hans_title) msg = R.string.special_black_hans_explain;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(titleRes).setMessage(msg).setPositiveButton(R.string.confirm_stage, null).show();
    }

    private void showInspectorDialog() {
        if (inspectorDialog != null && inspectorDialog.isShowing()) {
            AppLogger.e(FLOW_TAG, "Inspector dialog dismiss(before re-open)", new Exception("dismiss trace"));
            inspectorDialog.dismiss();
        }
        vm.inspectorQuery = "";
        inspectorRemovedKeys.clear();

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipToPadding(false);
        root.setPadding(24, 24, 24, 24);
        final int initialBottomPadding = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(navBars.bottom, ime.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottomPadding + bottomInset);
            return insets;
        });

        View handle = new View(requireContext());
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(96, 10);
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = 12;
        handle.setLayoutParams(handleLp);
        handle.setBackgroundColor(0xFFAAAAAA);
        root.addView(handle);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText(R.string.tombstone_inspector);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageButton close = new ImageButton(requireContext());
        close.setImageResource(R.drawable.ic_close_md);
        TypedValue closeBg = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, closeBg, true);
        close.setBackgroundResource(closeBg.resourceId);
        close.setOnClickListener(v -> {
            if (inspectorDialog != null) inspectorDialog.dismiss();
        });
        header.addView(title);
        header.addView(close);
        root.addView(header);

        TextView label = new TextView(requireContext());
        label.setText(R.string.inspector_pkg_label);
        root.addView(label);

        TextInputLayout pkgInputLayout = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        pkgInputLayout.setHintEnabled(false);
        pkgInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tvInspectorPkg = new TextInputEditText(requireContext());
        tvInspectorPkg.setHint(R.string.inspector_placeholder_pick_app);
        tvInspectorPkg.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurface, 0));
        tvInspectorPkg.setHintTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant, 0));
        tvInspectorPkg.setFocusable(false);
        tvInspectorPkg.setFocusableInTouchMode(false);
        tvInspectorPkg.setClickable(true);
        tvInspectorPkg.setLongClickable(false);
        tvInspectorPkg.setOnClickListener(v -> openPicker(REQ_INSPECTOR_PICK, new LinkedHashSet<>()));
        pkgInputLayout.addView(tvInspectorPkg, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(pkgInputLayout);

        swInspectorEditableOnly = new MaterialSwitch(requireContext());
        swInspectorEditableOnly.setText(R.string.inspector_only_editable);
        root.addView(swInspectorEditableOnly);

        rvInspectorHits = new RecyclerView(requireContext());
        rvInspectorHits.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvInspectorHits.setNestedScrollingEnabled(true);
        inspectorAdapter = new InspectorAdapter();
        rvInspectorHits.setAdapter(inspectorAdapter);
        int listHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.45f);
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight);
        rvLp.topMargin = 8;
        root.addView(rvInspectorHits, rvLp);

        MaterialButton btnTop = new MaterialButton(requireContext());
        btnTop.setText(R.string.back_to_top);
        btnTop.setOnClickListener(v -> {
            if (rvInspectorHits != null) rvInspectorHits.smoothScrollToPosition(0);
        });
        root.addView(btnTop);

        swInspectorEditableOnly.setOnCheckedChangeListener((buttonView, isChecked) -> runInspector(vm.inspectorQuery, isChecked));

        inspectorDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(root)
                .create();
        inspectorDialog.show();
        AppLogger.i(FLOW_TAG, "InspectorDialog show");
    }

    private void runInspector(String pkg, boolean onlyEditable) {
        if (pkg == null || pkg.isEmpty()) {
            if (inspectorAdapter != null) inspectorAdapter.submit(new ArrayList<>());
            return;
        }
        try {
            List<TombstoneXmlEditor.InspectorHit> hits = TombstoneXmlEditor.inspectPackage(vm.baseXmlBytes, pkg);
            if (onlyEditable) {
                List<TombstoneXmlEditor.InspectorHit> filtered = new ArrayList<>();
                for (TombstoneXmlEditor.InspectorHit hit : hits) {
                    if (hit.editableCapability != TombstoneXmlEditor.EditableCapability.NONE) filtered.add(hit);
                }
                hits = filtered;
            }
            renderInspectorHits(pkg, hits);
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderInspectorHits(String pkg, List<TombstoneXmlEditor.InspectorHit> hits) {
        if (inspectorAdapter == null) return;
        List<InspectorRow> rows = new ArrayList<>();
        addInspectorGroupRows(rows, hits, R.string.inspector_group_mainline, "enableConfig", "SysBlackApp", "sysBlack", "ffPkg.black", "whitePkg.100", "whitePkg.010", "whitePkg.001", "gms", "blackHansPkg", "proxy.gps", "proxy.sensor", "proxy.binder", "freeze_allow_list", "donot_freeze_app_list", "prevent", "allow", "android-freeze.exemptQuota");
        addInspectorGroupRows(rows, hits, R.string.inspector_group_special, "imPkg", "cpuCtlWhiteList", "appCard.ignoreApp");
        addInspectorGroupRows(rows, hits, R.string.inspector_group_feature, "pkgConfig");
        addInspectorGroupRows(rows, hits, R.string.inspector_group_binder, "cpnAsyncBinder");
        if (rows.isEmpty()) rows.add(InspectorRow.header(getString(R.string.inspector_empty_hint)));
        for (InspectorRow row : rows) row.pkg = pkg;
        inspectorAdapter.submit(rows);
    }

    private void addInspectorGroupRows(List<InspectorRow> rows, List<TombstoneXmlEditor.InspectorHit> hits, int titleRes, String... sections) {
        List<TombstoneXmlEditor.InspectorHit> group = new ArrayList<>();
        for (TombstoneXmlEditor.InspectorHit hit : hits) {
            for (String section : sections) {
                if (section.equals(hit.sectionTag)) {
                    group.add(hit);
                    break;
                }
            }
        }
        if (group.isEmpty()) return;
        rows.add(InspectorRow.header(getString(titleRes) + " (" + group.size() + ")"));
        for (TombstoneXmlEditor.InspectorHit hit : group) rows.add(InspectorRow.hit(hit));
    }

    private String inspectorTitle(TombstoneXmlEditor.InspectorHit hit) {
        if ("imPkg".equals(hit.sectionTag)) return getString(R.string.special_im_pkg_title);
        if ("cpuCtlWhiteList".equals(hit.sectionTag)) return getString(R.string.special_cpu_ctl_title);
        if ("appCard.ignoreApp".equals(hit.sectionTag)) return getString(R.string.special_app_card_title);
        if ("pkgConfig".equals(hit.sectionTag)) return getString(R.string.special_pkg_config_title) + " (pkgConfig)";
        if ("cpnAsyncBinder".equals(hit.sectionTag)) return getString(R.string.special_cpn_title) + " (cpnAsyncBinder)";
        if ("freeze_allow_list".equals(hit.sectionTag)) return "freeze_allow_list";
        if ("prevent".equals(hit.sectionTag)) return "prevent";
        if ("allow".equals(hit.sectionTag)) return "allow";
        if ("android-freeze.exemptQuota".equals(hit.sectionTag)) return "android-freeze.exemptQuota";
        if ("donot_freeze_app_list".equals(hit.sectionTag)) return "donot_freeze_app_list";
        if ("enableConfig".equals(hit.sectionTag)) return getString(R.string.special_enable_config_title);
        if ("gms".equals(hit.sectionTag)) return getString(R.string.special_gms_title);
        if ("ffPkg.black".equals(hit.sectionTag)) return getString(R.string.special_ff_skip_title);
        if ("whitePkg.100".equals(hit.sectionTag)) return getString(R.string.special_white_100_title);
        if ("whitePkg.010".equals(hit.sectionTag)) return getString(R.string.special_white_010_title);
        if ("whitePkg.001".equals(hit.sectionTag)) return getString(R.string.special_white_001_title);
        if ("blackHansPkg".equals(hit.sectionTag)) return getString(R.string.special_black_hans_title);
        if ("proxy.gps".equals(hit.sectionTag)) return getString(R.string.proxy_gps_title);
        if ("proxy.sensor".equals(hit.sectionTag)) return getString(R.string.proxy_sensor_title);
        if ("proxy.binder".equals(hit.sectionTag)) return getString(R.string.proxy_binder_title);
        return hit.sectionTag;
    }

    private String inspectorExplain(TombstoneXmlEditor.InspectorHit hit) {
        if ("imPkg".equals(hit.sectionTag)) return getString(R.string.special_im_pkg_explain);
        if ("cpuCtlWhiteList".equals(hit.sectionTag)) return getString(R.string.special_cpu_ctl_explain);
        if ("appCard.ignoreApp".equals(hit.sectionTag)) return getString(R.string.special_app_card_explain);
        if ("pkgConfig".equals(hit.sectionTag)) return getString(R.string.special_pkg_config_explain);
        if ("cpnAsyncBinder".equals(hit.sectionTag)) return getString(R.string.special_cpn_explain);
        if ("enableConfig".equals(hit.sectionTag)) return getString(R.string.special_enable_config_explain);
        if ("gms".equals(hit.sectionTag)) return getString(R.string.special_gms_explain);
        if ("ffPkg.black".equals(hit.sectionTag)) return getString(R.string.special_ff_skip_explain);
        if ("whitePkg.100".equals(hit.sectionTag)) return getString(R.string.special_white_100_explain);
        if ("whitePkg.010".equals(hit.sectionTag)) return getString(R.string.special_white_010_explain);
        if ("whitePkg.001".equals(hit.sectionTag)) return getString(R.string.special_white_001_explain);
        if ("blackHansPkg".equals(hit.sectionTag)) return getString(R.string.special_black_hans_explain);
        if ("proxy.gps".equals(hit.sectionTag)) return getString(R.string.proxy_gps_explain);
        if ("proxy.sensor".equals(hit.sectionTag)) return getString(R.string.proxy_sensor_explain);
        if ("proxy.binder".equals(hit.sectionTag)) return getString(R.string.proxy_binder_explain);
        return getString(R.string.inspector_mainline_explain);
    }

    private void removePackageFromSection(String pkg, String section) {
        if ("imPkg".equals(section)) vm.stagedImPkg.remove(pkg);
        else if ("cpuCtlWhiteList".equals(section)) vm.stagedCpuCtlWhiteList.remove(pkg);
        else if ("appCard.ignoreApp".equals(section)) vm.stagedAppCardIgnore.remove(pkg);
        else if ("freeze_allow_list".equals(section)) vm.stagedAllow.remove(pkg);
        else if ("donot_freeze_app_list".equals(section)) vm.stagedDoNot.remove(pkg);
        else if ("gms".equals(section)) vm.stagedGms.remove(pkg);
        else if ("ffPkg.black".equals(section)) vm.stagedFfSkipFastFreeze.remove(pkg);
        else if (section != null && section.startsWith("whitePkg.")) {
            String cat = section.substring("whitePkg.".length());
            Set<String> set = vm.stagedWhitePkgByCategory.get(cat);
            if (set != null) set.remove(pkg);
        }
        else if ("blackHansPkg".equals(section)) vm.stagedBlackHansTokens.remove(pkg);
        else if ("proxy.gps".equals(section)) { for (TombstoneXmlEditor.ProxyGpsItem i : vm.stagedProxyGpsConfig.items) i.pkgTokens.remove(pkg); }
        else if ("proxy.sensor".equals(section)) { for (TombstoneXmlEditor.WhiteTypePkgRule r : vm.stagedProxySensorConfig.whiteTypePkgs) r.pkgTokens.remove(pkg); }
        else if ("proxy.binder".equals(section)) { for (TombstoneXmlEditor.BinderDescRule r : vm.stagedProxyBinderConfig.descConfigs) r.pkgTokens.remove(pkg); }
        else if ("prevent".equals(section)) { vm.stagedPreventRules.removeIf(r -> pkg.equals(r.pkg)); }
        else if ("allow".equals(section)) { vm.stagedAllowRules.removeIf(r -> pkg.equals(r.pkg)); }
        else if ("android-freeze.exemptQuota".equals(section)) { vm.stagedAndroidFreezeConfig.exemptQuotas.removeIf(q -> pkg.equals(q.pkg)); }
        else if ("SysBlackApp".equals(section)) {
            for (String scene : TombstoneXmlEditor.BLACKLIST_SCENES) {
                Set<String> set = vm.stagedBlacklists.get(scene);
                if (set != null) set.remove(pkg);
            }
        }
    }

    private void restorePackageToSection(String pkg, String section) {
        if ("imPkg".equals(section)) vm.stagedImPkg.add(pkg);
        else if ("cpuCtlWhiteList".equals(section)) vm.stagedCpuCtlWhiteList.add(pkg);
        else if ("appCard.ignoreApp".equals(section)) vm.stagedAppCardIgnore.add(pkg);
        else if ("freeze_allow_list".equals(section)) vm.stagedAllow.add(pkg);
        else if ("donot_freeze_app_list".equals(section)) vm.stagedDoNot.add(pkg);
        else if ("gms".equals(section)) vm.stagedGms.add(pkg);
        else if ("ffPkg.black".equals(section)) vm.stagedFfSkipFastFreeze.add(pkg);
        else if (section != null && section.startsWith("whitePkg.")) {
            String cat = section.substring("whitePkg.".length());
            vm.stagedWhitePkgByCategory.computeIfAbsent(cat, ignored -> new LinkedHashSet<>()).add(pkg);
        }
        else if ("blackHansPkg".equals(section)) vm.stagedBlackHansTokens.add(pkg);
        else if ("proxy.gps".equals(section) && !vm.stagedProxyGpsConfig.items.isEmpty()) vm.stagedProxyGpsConfig.items.get(0).pkgTokens.add(pkg);
        else if ("proxy.sensor".equals(section) && !vm.stagedProxySensorConfig.whiteTypePkgs.isEmpty()) vm.stagedProxySensorConfig.whiteTypePkgs.get(0).pkgTokens.add(pkg);
        else if ("proxy.binder".equals(section) && !vm.stagedProxyBinderConfig.descConfigs.isEmpty()) vm.stagedProxyBinderConfig.descConfigs.get(0).pkgTokens.add(pkg);
        else if ("SysBlackApp".equals(section)) {
            Set<String> set = vm.stagedBlacklists.get("lcdoff");
            if (set != null) set.add(pkg);
        }
    }

    private void showQuickActionScopeSheet() {
        if (quickPendingPackages == null || quickPendingPackages.isEmpty()) return;
        if (quickPendingSuppress) {
            showQuickSuppressSheet();
        } else {
            showQuickKeepAliveSheet();
        }
    }

    private void showQuickSuppressSheet() {
        boolean[] scenes = new boolean[]{true, true, true};

        int horizontalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24,
                requireContext().getResources().getDisplayMetrics());
        int sectionSpacing = horizontalPadding / 3;

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, horizontalPadding / 2, 0, horizontalPadding / 2);

        NestedScrollView scroll = new NestedScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView notice = new TextView(requireContext());
        notice.setText(R.string.quick_suppress_notice_v3);
        notice.setLineSpacing(0f, 1.15f);
        notice.setPadding(horizontalPadding, horizontalPadding / 2, horizontalPadding, horizontalPadding / 2);

        LinearLayout sceneGroup = new LinearLayout(requireContext());
        sceneGroup.setOrientation(LinearLayout.VERTICAL);
        sceneGroup.setPadding(horizontalPadding, 0, horizontalPadding, sectionSpacing);

        MaterialCheckBox cbSceneLcdOn = new MaterialCheckBox(requireContext());
        cbSceneLcdOn.setText(R.string.scene_token_lcdon);
        cbSceneLcdOn.setChecked(true);
        MaterialCheckBox cbSceneLcdOff = new MaterialCheckBox(requireContext());
        cbSceneLcdOff.setText(R.string.scene_token_lcdoff);
        cbSceneLcdOff.setChecked(true);
        MaterialCheckBox cbSceneNight = new MaterialCheckBox(requireContext());
        cbSceneNight.setText(R.string.scene_token_night);
        cbSceneNight.setChecked(true);
        sceneGroup.addView(cbSceneLcdOn);
        sceneGroup.addView(cbSceneLcdOff);
        sceneGroup.addView(cbSceneNight);

        LinearLayout timingGroup = new LinearLayout(requireContext());
        timingGroup.setOrientation(LinearLayout.VERTICAL);
        timingGroup.setPadding(horizontalPadding, 0, horizontalPadding, sectionSpacing);

        MaterialCheckBox cbTimingZero = new MaterialCheckBox(requireContext());
        cbTimingZero.setText(R.string.quick_suppress_timing_enable);
        cbTimingZero.setChecked(false);
        cbTimingZero.setMaxLines(3);
        timingGroup.addView(cbTimingZero, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView timingHelp = new TextView(requireContext());
        timingHelp.setLineSpacing(0f, 1.12f);
        timingHelp.setPadding(horizontalPadding, 0, horizontalPadding, sectionSpacing);

        android.widget.RadioGroup profileGroup = new android.widget.RadioGroup(requireContext());
        profileGroup.setOrientation(android.widget.RadioGroup.VERTICAL);
        profileGroup.setPadding(horizontalPadding, 0, horizontalPadding, horizontalPadding / 2);

        android.widget.RadioButton rbL1 = new android.widget.RadioButton(requireContext());
        rbL1.setText(R.string.quick_suppress_timing_profile_l1);
        rbL1.setId(View.generateViewId());
        android.widget.RadioButton rbL2 = new android.widget.RadioButton(requireContext());
        rbL2.setText(R.string.quick_suppress_timing_profile_l2);
        rbL2.setId(View.generateViewId());
        android.widget.RadioButton rbL3 = new android.widget.RadioButton(requireContext());
        rbL3.setText(R.string.quick_suppress_timing_profile_l3);
        rbL3.setId(View.generateViewId());
        profileGroup.addView(rbL1);
        profileGroup.addView(rbL2);
        profileGroup.addView(rbL3);
        profileGroup.check(rbL1.getId());

        Runnable updateTimingHelp = () -> {
            if (!advancedEditEnabled() || !cbTimingZero.isChecked()) {
                timingHelp.setText(R.string.quick_suppress_timing_help_disabled);
                return;
            }
            int checked = profileGroup.getCheckedRadioButtonId();
            if (checked == rbL2.getId()) timingHelp.setText(R.string.quick_suppress_timing_help_l2);
            else if (checked == rbL3.getId()) timingHelp.setText(R.string.quick_suppress_timing_help_l3);
            else timingHelp.setText(R.string.quick_suppress_timing_help_l1);
        };

        int timingVisibility = advancedEditEnabled() ? View.VISIBLE : View.GONE;
        timingGroup.setVisibility(timingVisibility);
        timingHelp.setVisibility(timingVisibility);
        profileGroup.setVisibility(View.GONE);
        updateTimingHelp.run();

        cbTimingZero.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!advancedEditEnabled()) {
                profileGroup.setVisibility(View.GONE);
                updateTimingHelp.run();
                return;
            }
            profileGroup.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateTimingHelp.run();
        });
        profileGroup.setOnCheckedChangeListener((group, checkedId) -> updateTimingHelp.run());

        container.addView(notice, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(sceneGroup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(timingGroup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(timingHelp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(profileGroup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.quick_suppress)
                .setView(scroll)
                .setPositiveButton(R.string.confirm_stage, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            scenes[0] = cbSceneLcdOn.isChecked();
            scenes[1] = cbSceneLcdOff.isChecked();
            scenes[2] = cbSceneNight.isChecked();
            int timingProfile = TIMING_PROFILE_NONE;
            if (advancedEditEnabled() && timingGroup.getVisibility() == View.VISIBLE && cbTimingZero.isChecked()) {
                int checked = profileGroup.getCheckedRadioButtonId();
                if (checked == rbL2.getId()) timingProfile = TIMING_PROFILE_L2;
                else if (checked == rbL3.getId()) timingProfile = TIMING_PROFILE_L3;
                else timingProfile = TIMING_PROFILE_L1;
            }
            applyQuickSuppress(quickPendingPackages, scenes, timingProfile);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showQuickKeepAliveSheet() {
        List<String> targetNames = new ArrayList<>();
        List<java.util.function.Consumer<String>> appliers = new ArrayList<>();

        targetNames.add(getString(R.string.quick_target_donot_freeze));
        appliers.add(pkg -> vm.stagedDoNot.add(pkg));
        targetNames.add(getString(R.string.quick_target_freeze_allow));
        appliers.add(pkg -> vm.stagedAllow.add(pkg));
        targetNames.add(getString(R.string.special_im_pkg_title));
        appliers.add(pkg -> vm.stagedImPkg.add(pkg));
        targetNames.add(getString(R.string.special_app_card_title));
        appliers.add(pkg -> vm.stagedAppCardIgnore.add(pkg));
        targetNames.add(getString(R.string.special_cpu_ctl_title));
        appliers.add(pkg -> vm.stagedCpuCtlWhiteList.add(pkg));
        if (vm.hasFfPkgBlack) {
            targetNames.add(getString(R.string.quick_target_ff_skip));
            appliers.add(pkg -> vm.stagedFfSkipFastFreeze.add(pkg));
        }
        if (vm.hasWhitePkg) {
            targetNames.add(getString(R.string.quick_target_white_100));
            appliers.add(pkg -> vm.stagedWhitePkgByCategory.computeIfAbsent("100", ignored -> new LinkedHashSet<>()).add(pkg));
        }

        boolean[] targets = new boolean[targetNames.size()];
        for (int i = 0; i < targets.length; i++) targets[i] = i == 0;

        int horizontalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24,
                requireContext().getResources().getDisplayMetrics());

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);

        TextView notice = new TextView(requireContext());
        notice.setText(R.string.quick_keep_alive_notice);
        notice.setLineSpacing(0f, 1.15f);
        notice.setPadding(horizontalPadding, horizontalPadding / 2, horizontalPadding, horizontalPadding / 2);

        ListView listView = new ListView(requireContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_multiple_choice,
                targetNames));
        for (int i = 0; i < targets.length; i++) {
            listView.setItemChecked(i, targets[i]);
        }
        listView.setOnItemClickListener((parent, view, position, id) -> targets[position] = listView.isItemChecked(position));

        container.addView(notice, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.quick_keep_alive)
                .setView(container)
                .setPositiveButton(R.string.confirm_stage, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            for (int i = 0; i < targets.length; i++) {
                targets[i] = listView.isItemChecked(i);
            }
            applyQuickKeepAlive(quickPendingPackages, targets, appliers);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void applyQuickSuppress(Set<String> packages, boolean[] scenes, int timingProfile) {
        List<String> selectedScenes = new ArrayList<>();
        String[] all = new String[]{"lcdon", "lcdoff", "night"};
        for (int i = 0; i < all.length; i++) if (scenes[i]) selectedScenes.add(all[i]);
        for (String pkg : packages) {
            vm.stagedImPkg.remove(pkg);
            vm.stagedCpuCtlWhiteList.remove(pkg);
            vm.stagedAppCardIgnore.remove(pkg);
            vm.stagedAllow.remove(pkg);
            vm.stagedDoNot.remove(pkg);
            vm.stagedGms.remove(pkg);
            vm.stagedFfSkipFastFreeze.remove(pkg);
            for (Set<String> set : vm.stagedWhitePkgByCategory.values()) set.remove(pkg);
            vm.stagedBlackHansTokens.remove(pkg);
            for (String scene : selectedScenes) vm.stagedBlacklists.get(scene).add(pkg);
        }
        if (timingProfile != TIMING_PROFILE_NONE) {
            if (advancedEditEnabled()) {
                applyQuickSuppressTimingProfileZero(timingProfile);
            } else {
                Log.w(FLOW_TAG, "Quick suppress timing-profile requested without advanced edit; ignored.");
            }
        }
        renderSpecialHandling();
        renderProxySection();
        updateCounts();
        updateDirtyBadge();
        if (timingProfile == TIMING_PROFILE_NONE) {
            UiNotifier.showMessage(this, getString(R.string.quick_preview_suppress_basic));
        } else {
            String profileLabel = timingProfile == TIMING_PROFILE_L2 ? getString(R.string.quick_suppress_timing_profile_l2_short)
                    : timingProfile == TIMING_PROFILE_L3 ? getString(R.string.quick_suppress_timing_profile_l3_short)
                    : getString(R.string.quick_suppress_timing_profile_l1_short);
            UiNotifier.showMessage(this, getString(R.string.quick_preview_suppress_with_timing_profile, profileLabel));
        }
    }

    private void applyQuickSuppressTimingProfileZero(int timingProfile) {
        Set<String> attrs = new LinkedHashSet<>(java.util.Arrays.asList("guardTime", "RToM", "RToMST", "MToF", "MToFST"));
        Set<String> allowedModes = new LinkedHashSet<>();
        Set<String> allowedAppTypes = new LinkedHashSet<>();
        if (timingProfile == TIMING_PROFILE_L1) {
            allowedModes.add("highExtremeMode");
            allowedAppTypes.add("1000");
        } else if (timingProfile == TIMING_PROFILE_L2) {
            allowedModes.addAll(java.util.Arrays.asList("highExtremeMode", "extremeMode", "highPerfMode", "highLoadMode"));
            allowedAppTypes.add("1000");
        } else if (timingProfile == TIMING_PROFILE_L3) {
            allowedModes.addAll(java.util.Arrays.asList("highExtremeMode", "extremeMode", "highPerfMode", "highLoadMode"));
            allowedAppTypes.addAll(java.util.Arrays.asList("1000", "4", "7", "4#7"));
        } else {
            return;
        }

        for (String key : new ArrayList<>(vm.stagedTimingValues.keySet())) {
            if (!key.startsWith("freezeInterval{")) continue;
            int braceStart = key.indexOf('{');
            int braceEnd = key.indexOf('}');
            if (braceStart < 0 || braceEnd <= braceStart) continue;
            String scope = key.substring(braceStart + 1, braceEnd);
            String[] parts = scope.split(":", 3);
            if (parts.length < 3) continue;
            String modeTag = parts[0];
            String appType = parts[2];
            if (!allowedModes.contains(modeTag)) continue;
            if (!allowedAppTypes.contains(appType)) continue;
            int dot = key.lastIndexOf('.');
            if (dot <= 0) continue;
            String attr = key.substring(dot + 1);
            if (!attrs.contains(attr)) continue;
            vm.stagedTimingValues.put(key, "0");
        }
    }

    private void applyQuickKeepAlive(Set<String> packages, boolean[] targets, List<java.util.function.Consumer<String>> appliers) {
        for (String pkg : packages) {
            for (String scene : TombstoneXmlEditor.BLACKLIST_SCENES) {
                Set<String> set = vm.stagedBlacklists.get(scene);
                if (set != null) set.remove(pkg);
            }
            for (int i = 0; i < targets.length && i < appliers.size(); i++) {
                if (targets[i]) appliers.get(i).accept(pkg);
            }
        }
        renderSpecialHandling();
        renderProxySection();
        updateCounts();
        updateDirtyBadge();
        UiNotifier.showMessage(this, getString(R.string.quick_preview_keep_alive));
    }

    private String freezeIntervalFieldTitle(String attrName) {
        if ("guardTime".equals(attrName)) return getString(R.string.field_freeze_guard_title);
        if ("RToM".equals(attrName)) return getString(R.string.field_freeze_rtom_title);
        if ("RToMST".equals(attrName)) return getString(R.string.field_freeze_rtomst_title);
        if ("MToF".equals(attrName)) return getString(R.string.field_freeze_mtof_title);
        if ("MToFST".equals(attrName)) return getString(R.string.field_freeze_mtofst_title);
        if ("checkImportance".equals(attrName)) return getString(R.string.field_freeze_check_title);
        return attrName;
    }

    private void showFreezeTimingInfo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.freeze_timing)
                 .setMessage(R.string.freeze_timing_explain_no_unit)
                .setPositiveButton(R.string.confirm_stage, null)
                .show();
    }

    private void showWhyAffectDialog() {
        new WhyAffectBottomSheetFragment().show(getChildFragmentManager(), "why-affect");
    }

    @Override
    public void onOpenInspector() {
        AppLogger.e(FLOW_TAG, "navigate -> open inspector", new Exception("nav trace"));
        showInspectorDialog();
    }

    @Override
    public void onOpenPackageRules() {
        AppLogger.e(FLOW_TAG, "navigate -> package rules", new Exception("nav trace"));
        View target = getView() == null ? null : getView().findViewById(R.id.btnEditAllow);
        if (target != null) {
            target.requestFocus();
            target.post(() -> target.performClick());
        }
    }

    private static final class InspectorRow {
        final boolean header;
        final String headerText;
        final TombstoneXmlEditor.InspectorHit hit;
        String pkg;

        private InspectorRow(boolean header, String headerText, TombstoneXmlEditor.InspectorHit hit) {
            this.header = header;
            this.headerText = headerText;
            this.hit = hit;
        }

        static InspectorRow header(String text) { return new InspectorRow(true, text, null); }
        static InspectorRow hit(TombstoneXmlEditor.InspectorHit hit) { return new InspectorRow(false, null, hit); }
    }

    private final class InspectorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<InspectorRow> rows = new ArrayList<>();

        void submit(List<InspectorRow> next) {
            rows.clear();
            rows.addAll(next);
            notifyDataSetChanged();
        }

        @Override public int getItemViewType(int position) { return rows.get(position).header ? 0 : 1; }
        @Override public int getItemCount() { return rows.size(); }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                TextView tv = new TextView(parent.getContext());
                tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
                tv.setPadding(8, 16, 8, 8);
                return new RecyclerView.ViewHolder(tv) {};
            }
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(8, 8, 8, 12);
            return new RecyclerView.ViewHolder(row) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            InspectorRow rowData = rows.get(position);
            if (rowData.header) {
                ((TextView) holder.itemView).setText(rowData.headerText);
                return;
            }
            TombstoneXmlEditor.InspectorHit hit = rowData.hit;
            String pkg = rowData.pkg;
            LinearLayout row = (LinearLayout) holder.itemView;
            row.removeAllViews();
            String opKey = hit.sectionTag + "|" + hit.nodePath + "|" + pkg;
            boolean removed = inspectorRemovedKeys.contains(opKey);

            TextView line1 = new TextView(requireContext());
            line1.setText(inspectorTitle(hit));
            TextView line2 = new TextView(requireContext());
            line2.setText(hit.summary);
            TextView line3 = new TextView(requireContext());
            line3.setText(inspectorExplain(hit));
            if (removed) {
                line1.setPaintFlags(line1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                line2.setPaintFlags(line2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                line3.setPaintFlags(line3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            row.addView(line1);
            row.addView(line2);
            row.addView(line3);

            if (advancedEditEnabled()) {
                TextView path = new TextView(requireContext());
                path.setText("path=" + hit.nodePath);
                path.setTextColor(0xFF888888);
                path.setOnLongClickListener(v -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("path", hit.nodePath));
                        Toast.makeText(requireContext(), R.string.wallet_copy_success, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
                row.addView(path);
            }

            if (hit.editableCapability == TombstoneXmlEditor.EditableCapability.FULL_LIST) {
                MaterialButton action = new MaterialButton(requireContext());
                action.setText(removed ? R.string.restore : R.string.remove);
                action.setOnClickListener(v -> {
                    if (inspectorRemovedKeys.contains(opKey)) {
                        restorePackageToSection(pkg, hit.sectionTag);
                        inspectorRemovedKeys.remove(opKey);
                    } else {
                        removePackageFromSection(pkg, hit.sectionTag);
                        inspectorRemovedKeys.add(opKey);
                    }
                    runInspector(pkg, swInspectorEditableOnly != null && swInspectorEditableOnly.isChecked());
                    renderSpecialHandling();
                    renderProxySection();
                    updateDirtyBadge();
                });
                row.addView(action);
            } else if (hit.editableCapability == TombstoneXmlEditor.EditableCapability.PKG_TOKENS_ONLY) {
                MaterialButton edit = new MaterialButton(requireContext());
                edit.setText(R.string.edit_section_list);
                edit.setOnClickListener(v -> openSectionEditorWithRiskCheck(hit));
                row.addView(edit);
            }
        }
    }

    private void openSectionEditorWithRiskCheck(TombstoneXmlEditor.InspectorHit hit) {
        if (!advancedEditEnabled()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.risk_confirm_required)
                    .setPositiveButton(R.string.confirm_stage, null)
                    .show();
            return;
        }
        if ("pkgConfig".equals(hit.sectionTag)) {
            String prefix = "category=";
            String category = null;
            if (hit.summary != null && hit.summary.startsWith(prefix)) {
                int end = hit.summary.indexOf(',');
                category = end > prefix.length() ? hit.summary.substring(prefix.length(), end).trim() : hit.summary.substring(prefix.length()).trim();
            }
            if (category != null && !category.isEmpty()) showPkgConfigEditor(category);
        } else if ("cpnAsyncBinder".equals(hit.sectionTag)) {
            int l = hit.nodePath == null ? -1 : hit.nodePath.indexOf('[');
            int r = hit.nodePath == null ? -1 : hit.nodePath.indexOf(']');
            if (l >= 0 && r > l) {
                try {
                    int idx = Integer.parseInt(hit.nodePath.substring(l + 1, r));
                    showBinderEditor(idx);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void updateCounts() {
        updateModuleSummaries();
    }

    private void apply() {
        try {
            PolicyStore.migrateUserDirCeToDpIfNeeded(requireContext());
            if (!validateTimingValues()) return;
            Map<String, TombstoneXmlEditor.PolicyItem> policyDelta = new LinkedHashMap<>();
            for (Map.Entry<String, TombstoneXmlEditor.PolicyItem> entry : vm.policyDrafts.entrySet()) {
                TombstoneXmlEditor.PolicyItem baseline = vm.baselinePolicyDrafts.get(entry.getKey());
                if (!entry.getValue().equals(baseline)) {
                    policyDelta.put(entry.getKey(), entry.getValue());
                }
            }
            Map<String, String> timingDelta = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : vm.stagedTimingValues.entrySet()) {
                String baseline = vm.baselineTimingValues.get(entry.getKey());
                if (!entry.getValue().equals(baseline)) {
                    timingDelta.put(entry.getKey(), entry.getValue());
                }
            }

            byte[] output = TombstoneXmlEditor.applyOverlay(vm.baseXmlBytes, vm.stagedBlacklists, vm.stagedAllow, vm.stagedDoNot,
                    policyDelta, timingDelta, vm.stagedEnableConfigAttrs, vm.stagedGms, vm.stagedBlackHansEnabled, vm.stagedBlackHansTokens,
                    vm.stagedImPkg, vm.stagedCpuCtlWhiteList, vm.stagedAppCardIgnore,
                    vm.stagedFfSkipFastFreeze, vm.stagedWhitePkgByCategory,
                    vm.stagedProxyGlobalConfig, vm.stagedDefaultProxyBcActions, vm.stagedProxyBcExcludeRules,
                    vm.stagedProxyWakeLockConfig, vm.stagedProxyGpsConfig, vm.stagedProxyBtScanConfig,
                    vm.stagedProxyCpnExtensionConfig, vm.stagedProxySensorConfig, vm.stagedProxyBinderConfig,
                    vm.stagedPkgConfigTokens, vm.stagedBinderRules, buildOverlayV3C());
            PolicyStore.saveTombstoneRaw(requireContext(), output);
            reloadFromPolicy();
            updateSource();
            Toast.makeText(requireContext(), R.string.policy_applied, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("Multiple <SysBlackPolicy") || message.contains("Multiple <SysBlackRestrictMask")) {
                Toast.makeText(requireContext(), R.string.policy_group_multiple_unsupported, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.export_failed_reason, message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private TombstoneXmlEditor.OverlayV3C buildOverlayV3C() {
        TombstoneXmlEditor.OverlayV3C v = new TombstoneXmlEditor.OverlayV3C();
        v.serviceBumpReasons = new LinkedHashSet<>(vm.stagedServiceBumpReasons);
        v.preventRules = new ArrayList<>(vm.stagedPreventRules);
        v.allowRules = new ArrayList<>(vm.stagedAllowRules);
        v.cpuCtlRus.attrs.putAll(vm.stagedCpuCtlRus.attrs);
        v.highLoadingConfigs = new ArrayList<>(vm.stagedHighLoadingConfigs);
        v.strictModeSwitches.strictEnable = vm.stagedStrictModeSwitches.strictEnable;
        v.strictModeSwitches.version = vm.stagedStrictModeSwitches.version;
        v.strictModeSwitches.modes.addAll(vm.stagedStrictModeSwitches.modes);
        v.androidFreezeConfig.enable = vm.stagedAndroidFreezeConfig.enable;
        v.androidFreezeConfig.whiteEnable = vm.stagedAndroidFreezeConfig.whiteEnable;
        v.androidFreezeConfig.killWhiteEnable = vm.stagedAndroidFreezeConfig.killWhiteEnable;
        v.androidFreezeConfig.sysAppExemptEnable = vm.stagedAndroidFreezeConfig.sysAppExemptEnable;
        v.androidFreezeConfig.exemptQuotas.addAll(vm.stagedAndroidFreezeConfig.exemptQuotas);
        v.appQuotaConfig.maxDuration = vm.stagedAppQuotaConfig.maxDuration;
        v.appQuotaConfig.protectTimeAfterExceed = vm.stagedAppQuotaConfig.protectTimeAfterExceed;
        v.appQuotaConfig.parentPath = vm.stagedAppQuotaConfig.parentPath;
        return v;
    }

    private boolean validateTimingValues() {
        boolean warned = false;
        for (Map.Entry<String, String> entry : vm.stagedTimingValues.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.invalid_number_value, entry.getKey()), Toast.LENGTH_LONG).show();
                return false;
            }
            long v = parseLongSafe(value, -1L);
            if (v < 0) {
                Toast.makeText(requireContext(), getString(R.string.invalid_number_value, entry.getKey()), Toast.LENGTH_LONG).show();
                return false;
            }
            if (!warned && v > WARN_UPPER_BOUND) {
                warned = true;
                Toast.makeText(requireContext(), R.string.large_value_risk_hint, Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    private long parseLongSafe(String text, long fallback) {
        try {
            return Long.parseLong(text == null ? "" : text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private FieldMeta fieldMeta(String key, String attrName) {
        if (key.startsWith("lcdOffConfig.ffInterval")) return new FieldMeta(R.string.field_lcdOff_ffInterval, R.string.field_tip_lcdOff_ffInterval, true);
        if (key.startsWith("lcdOffConfig.interval")) return new FieldMeta(R.string.field_lcdOff_interval, R.string.field_tip_lcdOff_interval, true);
        if (key.startsWith("lcdOffConfig.ffTotal")) return new FieldMeta(R.string.field_lcdOff_ffTotal, R.string.field_tip_lcdOff_ffTotal, false);

        if (key.startsWith("lcdOnConfig.RToM")) return new FieldMeta(R.string.field_lcdOn_RToM, R.string.field_tip_lcdOn_RToM, true);
        if (key.startsWith("lcdOnConfig.MToF")) return new FieldMeta(R.string.field_lcdOn_MToF, R.string.field_tip_lcdOn_MToF, true);
        if (key.startsWith("lcdOnConfig.checkImportance")) return new FieldMeta(R.string.field_lcdOn_checkImportance, R.string.field_tip_lcdOn_checkImportance, true);

        if (key.startsWith("ffConfig.enterTimeout")) return new FieldMeta(R.string.field_ff_enterTimeout, R.string.field_tip_ff_enterTimeout, true);
        if (key.startsWith("ffConfig.interval")) return new FieldMeta(R.string.field_ff_interval, R.string.field_tip_ff_interval, true);
        if (key.startsWith("ffConfig.maxFzNum")) return new FieldMeta(R.string.field_ff_maxFzNum, R.string.field_tip_ff_maxFzNum, false);

        if (key.startsWith("powerSaveConfig.repeatTime")) return new FieldMeta(R.string.field_powerSave_repeatTime, R.string.field_tip_powerSave_repeatTime, true);

        if (key.startsWith("trafficConfig.interval")) return new FieldMeta(R.string.field_traffic_interval, R.string.field_tip_traffic_interval, true);
        if (key.startsWith("trafficConfig.trafficUnfreezeTime")) return new FieldMeta(R.string.field_traffic_unfreeze, R.string.field_tip_traffic_unfreeze, true);

        if (key.startsWith("ApiTimeoutConfig.upperTimeout")) return new FieldMeta(R.string.field_api_upper, R.string.field_tip_api_upper, true);
        if (key.startsWith("ApiTimeoutConfig.floorTimeout")) return new FieldMeta(R.string.field_api_floor, R.string.field_tip_api_floor, true);

        if (key.startsWith("queryUsageStatsConfig.queryIntervalTime")) return new FieldMeta(R.string.field_query_interval, R.string.field_tip_query_interval, true);
        if (key.startsWith("queryUsageStatsConfig.messageDelayTime")) return new FieldMeta(R.string.field_query_messageDelay, R.string.field_tip_query_messageDelay, true);
        if (key.startsWith("queryUsageStatsConfig.messageRepeateCount")) return new FieldMeta(R.string.field_query_repeatCount, R.string.field_tip_query_repeatCount, false);

        if (key.startsWith("screenOffStillForceProxyConfig.gpsInterval")) return new FieldMeta(R.string.field_forceProxy_gpsInterval, R.string.field_tip_forceProxy_gpsInterval, true);
        if (key.startsWith("screenOffStillForceProxyConfig.defaultInterval")) return new FieldMeta(R.string.field_forceProxy_defaultInterval, R.string.field_tip_forceProxy_defaultInterval, true);

        if (key.contains(".guardTime")) return new FieldMeta(R.string.field_freeze_guardTime, R.string.field_tip_freeze_guardTime, true);
        if (key.contains(".RToMST")) return new FieldMeta(R.string.field_freeze_RToMST, R.string.field_tip_freeze_RToMST, true);
        if (key.contains(".RToM")) return new FieldMeta(R.string.field_freeze_RToM, R.string.field_tip_freeze_RToM, true);
        if (key.contains(".MToFST")) return new FieldMeta(R.string.field_freeze_MToFST, R.string.field_tip_freeze_MToFST, true);
        if (key.contains(".MToF")) return new FieldMeta(R.string.field_freeze_MToF, R.string.field_tip_freeze_MToF, true);
        if (key.contains(".checkImportance")) return new FieldMeta(R.string.field_freeze_checkImportance, R.string.field_tip_freeze_checkImportance, true);

        return new FieldMeta(R.string.unit_raw, R.string.unit_unknown_hint, attrName.toLowerCase(Locale.ROOT).contains("time"));
    }



    private void openModuleDetail(Module module) {
        ModuleDetailBottomSheet.newInstance(module.name())
                .show(getChildFragmentManager(), "module_detail_" + module.name());
    }

    @Override
    public void onModuleEditAction(String actionId) {
        switch (actionId) {
            case EDIT_FF_SKIP: openPicker(REQ_FF_SKIP, vm.stagedFfSkipFastFreeze); break;
            case EDIT_DONOT: openPicker(REQ_DONOT, vm.stagedDoNot); break;
            case EDIT_WHITE_100: openPicker(REQ_WHITE_100, vm.stagedWhitePkgByCategory.get("100")); break;
            case EDIT_IMPK: openPicker(REQ_IMPK, vm.stagedImPkg); break;
            case EDIT_CPUCTL: openPicker(REQ_CPUCTL, vm.stagedCpuCtlWhiteList); break;
            case EDIT_APPCARD: openPicker(REQ_APPCARD, vm.stagedAppCardIgnore); break;
            case ACTION_KEEPALIVE: showQuickKeepAliveSheet(); break;
            case ACTION_SUPPRESS: showQuickSuppressSheet(); break;
            case EDIT_LCDON: openPicker(REQ_LCDON, vm.stagedBlacklists.get("lcdon")); break;
            case EDIT_LCDOFF: openPicker(REQ_LCDOFF, vm.stagedBlacklists.get("lcdoff")); break;
            case EDIT_NIGHT: openPicker(REQ_NIGHT, vm.stagedBlacklists.get("night")); break;
            case OPEN_POLICY_PARAMS: openAdvancedAndScrollTo(R.id.policyContainer); break;
            case OPEN_PROXY_ADVANCED: openAdvancedAndScrollTo(R.id.proxyContainer); break;
            case EDIT_SERVICE_BUMP:
                runRiskAction(() -> openEditableListSheet(RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null));
                break;
            case EDIT_PREVENT: runRiskAction(() -> showSceneRuleEditor("prevent", vm.stagedPreventRules)); break;
            case EDIT_ALLOW: runRiskAction(() -> showSceneRuleEditor("allow", vm.stagedAllowRules)); break;
            case OPEN_BUMP_ADVANCED: openAdvancedAndScrollTo(R.id.specialHighContainer); break;
            case EDIT_HIGH_LOADING: runRiskAction(this::showHighLoadingEditor); break;
            case EDIT_CPU_CTL_RUS: runRiskAction(this::showCpuCtlRusEditor); break;
            case EDIT_ANDROID_FREEZE: runRiskAction(this::showAndroidFreezeEditor); break;
            case EDIT_APP_QUOTA: runRiskAction(this::showAppQuotaEditor); break;
            case EDIT_STRICT_MODE_SWITCHES: runRiskAction(this::showStrictModeEnableEditor); break;
            case ACTION_ADVANCED_REQUIRED_TIP:
                Toast.makeText(requireContext(), R.string.advanced_editing_required, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onOpenAdvancedAnchor(int anchorViewId) {
        openAdvancedAndScrollTo(anchorViewId);
    }

    @Override
    public String getModuleSummary(String moduleId) {
        if ("FAST_FREEZE".equals(moduleId)) return tvFastFreezeSummary == null ? "" : String.valueOf(tvFastFreezeSummary.getText());
        if ("WHITELIST".equals(moduleId)) return tvWhitelistSummary == null ? "" : String.valueOf(tvWhitelistSummary.getText());
        if ("SYS_BLACK".equals(moduleId)) return tvSysBlackSummary == null ? "" : String.valueOf(tvSysBlackSummary.getText());
        if ("PROXY".equals(moduleId)) return tvProxySummary == null ? "" : String.valueOf(tvProxySummary.getText());
        if ("BUMP".equals(moduleId)) return tvBumpSummary == null ? "" : String.valueOf(tvBumpSummary.getText());
        return tvHighLoadSummary == null ? "" : String.valueOf(tvHighLoadSummary.getText());
    }

    @Override
    public boolean isAdvancedEditEnabled() {
        return advancedEditEnabled();
    }

    private void showModuleMenu(Module m) {
        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        switch (m) {
            case FAST_FREEZE:
                if (vm.hasFfPkgBlack) {
                    items.add(getString(R.string.module_menu_fast_freeze_skip));
                    actions.add(() -> openPicker(REQ_FF_SKIP, vm.stagedFfSkipFastFreeze));
                } else {
                    items.add(getString(R.string.module_not_supported_ff));
                    actions.add(() -> Toast.makeText(requireContext(), R.string.module_not_supported_ff, Toast.LENGTH_SHORT).show());
                }
                items.add(getString(R.string.module_menu_fast_freeze_explain));
                actions.add(() -> openAdvancedAndScrollTo(R.id.timeParamContainer));
                break;
            case WHITELIST:
                items.add(getString(R.string.module_menu_whitelist_donot));
                actions.add(() -> openPicker(REQ_DONOT, vm.stagedDoNot));
                if (vm.hasWhitePkg) {
                    items.add(getString(R.string.module_menu_whitelist_white100));
                    actions.add(() -> openPicker(REQ_WHITE_100, vm.stagedWhitePkgByCategory.get("100")));
                }
                items.add(getString(R.string.module_menu_whitelist_im));
                actions.add(() -> openPicker(REQ_IMPK, vm.stagedImPkg));
                items.add(getString(R.string.module_menu_whitelist_cpuctl));
                actions.add(() -> openPicker(REQ_CPUCTL, vm.stagedCpuCtlWhiteList));
                items.add(getString(R.string.module_menu_whitelist_appcard));
                actions.add(() -> openPicker(REQ_APPCARD, vm.stagedAppCardIgnore));
                items.add(getString(R.string.module_menu_whitelist_keep_alive));
                actions.add(this::showQuickKeepAliveSheet);
                break;
            case SYS_BLACK:
                items.add(getString(R.string.module_menu_sys_black_quick));
                actions.add(this::showQuickSuppressSheet);
                items.add(getString(R.string.module_menu_sys_black_lcdon));
                actions.add(() -> openPicker(REQ_LCDON, vm.stagedBlacklists.get("lcdon")));
                items.add(getString(R.string.module_menu_sys_black_lcdoff));
                actions.add(() -> openPicker(REQ_LCDOFF, vm.stagedBlacklists.get("lcdoff")));
                items.add(getString(R.string.module_menu_sys_black_night));
                actions.add(() -> openPicker(REQ_NIGHT, vm.stagedBlacklists.get("night")));
                items.add(getString(R.string.module_menu_sys_black_policy));
                actions.add(() -> openAdvancedAndScrollTo(R.id.policyContainer));
                break;
            case PROXY:
                items.add(getString(R.string.module_menu_proxy_global));
                actions.add(() -> openAdvancedAndScrollTo(R.id.proxyContainer));
                items.add(getString(R.string.module_menu_proxy_default_bc));
                actions.add(() -> runRiskAction(() -> openEditableListSheet(RK_PROXY_DEFAULT_BC, getString(R.string.proxy_default_bc_title), getString(R.string.proxy_default_bc_explain), false, null)));
                items.add(getString(R.string.module_menu_proxy_exclude));
                actions.add(() -> openAdvancedAndScrollTo(R.id.proxyContainer));
                items.add(getString(R.string.module_menu_proxy_bundle));
                actions.add(() -> openAdvancedAndScrollTo(R.id.proxyContainer));
                break;
            case BUMP:
                items.add(getString(R.string.module_menu_bump_service));
                actions.add(() -> openEditableListSheet(RK_SERVICE_BUMP, getString(R.string.v3c_service_bump_title), null, false, null));
                items.add(getString(R.string.module_menu_bump_rules));
                actions.add(() -> runRiskAction(() -> showSceneRuleEditor("prevent", vm.stagedPreventRules)));
                items.add(getString(R.string.module_menu_bump_traffic));
                actions.add(() -> openAdvancedAndScrollTo(R.id.specialHighContainer));
                break;
            case HIGH_LOAD:
                items.add(getString(R.string.module_menu_highload_loading));
                actions.add(() -> runRiskAction(this::showHighLoadingEditor));
                items.add(getString(R.string.module_menu_highload_cpuctl));
                actions.add(() -> runRiskAction(this::showCpuCtlRusEditor));
                items.add(getString(R.string.module_menu_highload_android_freeze));
                actions.add(() -> runRiskAction(this::showAndroidFreezeEditor));
                items.add(getString(R.string.module_menu_highload_quota));
                actions.add(() -> runRiskAction(this::showAppQuotaEditor));
                items.add(getString(R.string.module_menu_highload_strict));
                actions.add(() -> runRiskAction(this::showStrictModeEnableEditor));
                break;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(moduleTitleRes(m))
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which >= 0 && which < actions.size()) actions.get(which).run();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int moduleTitleRes(Module m) {
        if (m == Module.FAST_FREEZE) return R.string.module_fast_freeze_title;
        if (m == Module.WHITELIST) return R.string.module_whitelist_title;
        if (m == Module.SYS_BLACK) return R.string.module_sys_black_title;
        if (m == Module.PROXY) return R.string.module_proxy_title;
        if (m == Module.BUMP) return R.string.module_bump_title;
        return R.string.module_high_load_title;
    }

    private void runRiskAction(Runnable action) {
        if (!advancedEditEnabled()) {
            Toast.makeText(requireContext(), R.string.risk_confirm_required, Toast.LENGTH_SHORT).show();
            return;
        }
        action.run();
    }

    private static void stopScroll(ScrollView scrollView) {
        if (scrollView == null) return;
        scrollView.stopNestedScroll();
        scrollView.fling(0);
        long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        scrollView.dispatchTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private void openAdvancedAndScrollTo(int anchorId) {
        stopScroll(vm.tombstoneTabIndex == 0 ? scrollRecommended : scrollAdvanced);
        vm.tombstoneTabIndex = 1;
        TabLayout.Tab tab = tabLayoutTombstone == null ? null : tabLayoutTombstone.getTabAt(1);
        if (tab != null) tab.select();
        applyTab(1);
        if (scrollAdvanced == null) return;
        scrollAdvanced.post(() -> {
            View root = getView();
            if (root == null) return;
            View anchor = root.findViewById(anchorId);
            if (anchor != null) {
                vm.advancedScrollY = Math.max(anchor.getTop(), 0);
                scrollAdvanced.smoothScrollTo(0, anchor.getTop());
            }
        });
    }

    private void updateModuleSummaries() {
        if (tvFastFreezeSummary == null) return;
        int white100Count = vm.stagedWhitePkgByCategory.get("100") == null ? 0 : vm.stagedWhitePkgByCategory.get("100").size();
        tvFastFreezeSummary.setText(getString(R.string.module_summary_fast_freeze, vm.stagedFfSkipFastFreeze.size(), vm.hasFfPkgBlack ? getString(R.string.module_supported) : getString(R.string.module_not_supported)));
        tvWhitelistSummary.setText(getString(R.string.module_summary_whitelist, vm.stagedDoNot.size(), white100Count, vm.stagedImPkg.size(), vm.stagedCpuCtlWhiteList.size(), vm.stagedAppCardIgnore.size()));
        tvSysBlackSummary.setText(getString(R.string.module_summary_sys_black,
                sizeOf(vm.stagedBlacklists.get("lcdon")),
                sizeOf(vm.stagedBlacklists.get("lcdoff")),
                sizeOf(vm.stagedBlacklists.get("night"))));
        tvProxySummary.setText(getString(R.string.module_summary_proxy,
                vm.stagedProxyGlobalConfig.hasAny ? getString(R.string.module_parsed_editable) : getString(R.string.not_configured),
                vm.stagedDefaultProxyBcActions.size() + vm.stagedProxyBcExcludeRules.size()));
        tvBumpSummary.setText(getString(R.string.module_summary_bump, vm.stagedServiceBumpReasons.size(), vm.stagedPreventRules.size(), vm.stagedAllowRules.size()));
        boolean highLoadingEnabled = !vm.stagedHighLoadingConfigs.isEmpty() && vm.stagedHighLoadingConfigs.get(0).cpuLoadingEnable;
        tvHighLoadSummary.setText(getString(R.string.module_summary_high_load,
                highLoadingEnabled ? getString(R.string.enabled) : getString(R.string.disabled),
                vm.stagedAndroidFreezeConfig.enable ? getString(R.string.enabled) : getString(R.string.disabled),
                vm.stagedAppQuotaConfig.maxDuration));
    }

    private int sizeOf(Set<String> set) {
        return set == null ? 0 : set.size();
    }
    private int countOccurrences(String tag) {
        byte[] bytes = vm == null ? null : vm.baseXmlBytes;
        if (bytes == null || tag == null || tag.trim().isEmpty()) return 0;
        String text = new String(bytes);
        int count = 0;
        int idx = 0;
        String needle = "<" + tag;
        while (true) {
            idx = text.indexOf(needle, idx);
            if (idx < 0) break;
            count++;
            idx += needle.length();
        }
        return count;
    }


    private void showStrictModeEnableEditor() {
        List<String> lines = new ArrayList<>();
        lines.add("strictEnable=" + vm.stagedStrictModeSwitches.strictEnable);
        openEditableListSheet(RK_STRICT_SWITCH, getString(R.string.module_menu_highload_strict), null, false, null);
    }

    private void applyStrictSwitchEdit(ArrayList<String> edited) {
        boolean strictEnable = vm.stagedStrictModeSwitches.strictEnable;
        boolean hasValid = false;
        for (String line : edited) {
            if (line.startsWith("strictEnable=")) {
                strictEnable = line.endsWith("true");
                hasValid = true;
            }
        }
        if (!hasValid && !edited.isEmpty()) {
            UiNotifier.showMessage(this, getString(R.string.edit_invalid_all_ignored));
            return;
        }
        vm.stagedStrictModeSwitches.strictEnable = strictEnable;
        UiNotifier.showMessage(this, getString(R.string.staged_ok_count, edited.size()));
        View root = getView();
        if (root != null) root.post(this::renderSpecialHandling); else renderSpecialHandling();
        updateDirtyBadge();
    }
    private void applyTab(int position) {
        boolean recommended = position == 0;
        sectionRecommended.setVisibility(recommended ? View.VISIBLE : View.GONE);
        sectionAdvanced.setVisibility(recommended ? View.GONE : View.VISIBLE);
        if (scrollRecommended != null) {
            scrollRecommended.setVisibility(recommended ? View.VISIBLE : View.GONE);
        }
        if (scrollAdvanced != null) {
            scrollAdvanced.setVisibility(recommended ? View.GONE : View.VISIBLE);
        }
        if (!recommended) {
            renderProxySection();
        }
    }

    private void updateDirtyBadge() {
        tvDirtyState.setVisibility(View.GONE);
        tvDirtyState.setText("");
    }

    private void updateSource() {
        String source = PolicyStore.tombstoneSource() == PolicyStore.Source.USER ? getString(R.string.source_user) : getString(R.string.source_asset);
        tvSource.setText(getString(R.string.current_source_value, source));
        updateDirtyBadge();
    }
}
