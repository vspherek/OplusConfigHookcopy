package com.astor.oplusconfighook;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 应用选择 Activity，承载筛选、搜索与返回结果。
 */
public class AppPickerActivity extends AppCompatActivity {
    private static final String TAG = "AppListFlow";
    private static final long CANCEL_GUARD_WINDOW_MS = 600L;

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_REQ = "req";
    public static final String EXTRA_PRESELECTED = "preselected";
    public static final String EXTRA_SELECTED = "selected";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppPickerViewModel vm;
    private AppSelectAdapter adapter;

    private RecyclerView rv;
    private EditText etSearch;
    private TextView tvFilterState;
    private CheckBox cbDescending;
    private View loadingPanel;
    private TextView tvLoadingState;
    private MaterialButton btnConfirm;
    private MaterialButton btnCancel;
    private int launchReq = -1;
    private long enteredAtElapsedRealtime;
    private boolean pendingScrollTop;
    private final AppListRepository.Listener appListListener = snapshot -> {
        if (isFinishing() || isDestroyed()) return;
        if (snapshot.state == AppListRepository.PreloadState.LOADING || snapshot.state == AppListRepository.PreloadState.IDLE) {
            vm.loadingState = AppPickerViewModel.LoadingState.LOADING;
            renderLoadingState();
            return;
        }
        if (snapshot.state == AppListRepository.PreloadState.ERROR) {
            vm.setLoadError(snapshot.error);
            renderLoadingState();
            return;
        }
        vm.setApps(snapshot.apps);
        AppLogger.i(TAG, "AppPicker bind apps count=" + vm.allApps.size());
    };

    private final Runnable searchRunnable = () -> {
        if (vm != null && etSearch != null) {
            vm.setQuery(String.valueOf(etSearch.getText()));
            AppLogger.i(TAG, "query=" + vm.query);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);
        AppLogger.i(TAG, "AppPickerActivity onCreate");

        vm = new ViewModelProvider(this).get(AppPickerViewModel.class);
        resolveLaunchExtras();
        ensureInitialSelection();
        bindViews();
        bindActions();
        bindObservers();
        restoreControlState();
        renderLoadingState();
        loadAppsIfNeeded();
    }

    private void bindViews() {
        rv = findViewById(R.id.rvApps);
        etSearch = findViewById(R.id.etSearch);
        tvFilterState = findViewById(R.id.tvFilterState);
        cbDescending = findViewById(R.id.cbDescending);
        loadingPanel = findViewById(R.id.loadingPanel);
        tvLoadingState = findViewById(R.id.tvLoadingState);
        loadingPanel.setOnClickListener(v -> {
            if (vm.loadingState == AppPickerViewModel.LoadingState.ERROR && !vm.appsLoaded) {
                vm.loadingState = AppPickerViewModel.LoadingState.LOADING;
                renderLoadingState();
                AppListRepository.ensurePreloaded(getApplicationContext());
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setItemAnimator(null);
        rv.setHasFixedSize(true);

        adapter = new AppSelectAdapter(getPackageManager(), vm.workingCheckedPackages, selected -> vm.updateWorkingSelection(selected));
        rv.setAdapter(adapter);
    }

    private void bindActions() {
        MaterialButton btnFilter = findViewById(R.id.btnFilter);
        MaterialButton btnSort = findViewById(R.id.btnSort);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setFocusable(false);
        btnCancel.setFocusableInTouchMode(false);
        btnConfirm.setFocusable(false);
        btnConfirm.setFocusableInTouchMode(false);
        btnCancel.setAccessibilityTraversalAfter(R.id.etSearch);
        btnConfirm.setAccessibilityTraversalAfter(R.id.btnCancel);

        etSearch.addTextChangedListener(new SimpleTextWatcher(s -> {
            mainHandler.removeCallbacks(searchRunnable);
            mainHandler.postDelayed(searchRunnable, 180L);
        }));

        btnFilter.setOnClickListener(v -> showFilterMenu());
        btnSort.setOnClickListener(v -> showSortMenu());

        cbDescending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vm.setDescending(isChecked);
            AppLogger.i(TAG, "descending=" + isChecked);
        });

        btnConfirm.setOnClickListener(v -> finishWithSelection());
        btnCancel.setOnClickListener(v -> {
            if (!btnCancel.isEnabled()) return;
            Exception clickTrace = new Exception("cancel click");
            boolean byAccessibility = isAccessibilityClick(clickTrace);
            long elapsed = SystemClock.elapsedRealtime() - enteredAtElapsedRealtime;
            if (elapsed < CANCEL_GUARD_WINDOW_MS) {
                AppLogger.e(TAG,
                        "cancel(click) ignored by debounce elapsed=" + elapsed + "ms "
                                + (byAccessibility ? "CANCEL_BY_ACCESSIBILITY" : "CANCEL_BY_USER_OR_UNKNOWN"),
                        clickTrace);
                return;
            }
            vm.updateWorkingSelection(vm.originalCheckedPackages);
            AppLogger.i(TAG, "cancel(click) " + (byAccessibility ? "CANCEL_BY_ACCESSIBILITY" : "CANCEL_BY_USER_OR_UNKNOWN"));
            AppLogger.e(TAG, "AppPicker finish(cancel click)", clickTrace);
            AppLogger.e(TAG, "AppPicker finish(cancel click) finish trace", new Exception("finish trace"));
            finishWithReason("cancel_click");
        });
    }

    private void bindObservers() {
        vm.filteredSortedApps().observe(this, apps -> {
            if (isFinishing() || isDestroyed()) return;
            adapter.setSelectedPackages(vm.workingCheckedPackages);
            adapter.submitApps(apps, () -> {
                if (pendingScrollTop && rv != null) {
                    rv.scrollToPosition(0);
                    pendingScrollTop = false;
                }
            });
            updateFilterStateLabel();
            renderLoadingState();
        });
    }

    private void resolveLaunchExtras() {
        Intent intent = getIntent();
        if (intent == null) {
            AppLogger.i(TAG, "AppPicker invalid intent: null");
            toastInvalidLaunch();
            launchReq = -1;
            return;
        }
        String mode = intent.getStringExtra(EXTRA_MODE);
        if (!"AUTOSTART".equals(mode) && !"TOMBSTONE".equals(mode)) {
            AppLogger.i(TAG, "AppPicker invalid EXTRA_MODE=" + mode);
            toastInvalidLaunch();
        }
        launchReq = intent.getIntExtra(EXTRA_REQ, -1);
    }

    private void ensureInitialSelection() {
        if (vm.appsLoaded) return;
        Set<String> selected = new LinkedHashSet<>(readPreselectedPackages());
        vm.setInitialSelection(selected);
        vm.loadingState = AppPickerViewModel.LoadingState.LOADING;
        AppLogger.i(TAG, "initial selection count=" + selected.size());
    }

    private ArrayList<String> readPreselectedPackages() {
        try {
            Intent intent = getIntent();
            ArrayList<String> extras = intent == null ? null : intent.getStringArrayListExtra(EXTRA_PRESELECTED);
            return extras == null ? new ArrayList<>() : extras;
        } catch (RuntimeException e) {
            AppLogger.e(TAG, "parse preselected failed", e);
            toastInvalidLaunch();
            return new ArrayList<>();
        }
    }

    private void loadAppsIfNeeded() {
        AppListRepository.registerListener(appListListener);
        AppListRepository.ensurePreloaded(getApplicationContext());
    }

    private void finishWithSelection() {
        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_SELECTED, new ArrayList<>(vm.workingCheckedPackages));
        data.putStringArrayListExtra("selected", new ArrayList<>(vm.workingCheckedPackages));
        data.putExtra(EXTRA_REQ, launchReq);
        AppLogger.i(TAG, "confirm selection count=" + vm.workingCheckedPackages.size());
        AppLogger.e(TAG, "AppPicker finish(confirm)", new Exception("finish trace"));
        setResult(RESULT_OK, data);
        finishWithReason("confirm_click");
    }


    private void finishWithReason(String reason) {
        String queryText = vm == null ? "" : vm.query;
        AppPickerViewModel.CheckedFilter checked = vm == null ? AppPickerViewModel.CheckedFilter.ALL : vm.checkedFilter;
        boolean dirty = vm != null && !vm.workingCheckedPackages.equals(vm.originalCheckedPackages);
        AppLogger.i(TAG, "[APP_PICKER_FINISH] reason=" + reason
                + " checkedFilter=" + checked
                + " query=" + queryText
                + " dirty=" + dirty
                + " thread=" + Thread.currentThread().getName());
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            vm.updateWorkingSelection(vm.originalCheckedPackages);
            finishWithReason("toolbar_home");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderLoadingState() {
        boolean loading = vm.loadingState == AppPickerViewModel.LoadingState.LOADING && !vm.appsLoaded;
        boolean enableActions = !loading;
        btnCancel.setEnabled(enableActions);
        btnConfirm.setEnabled(enableActions);
        btnCancel.setClickable(enableActions);
        btnConfirm.setClickable(enableActions);
        int important = enableActions ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO : View.IMPORTANT_FOR_ACCESSIBILITY_NO;
        btnCancel.setImportantForAccessibility(important);
        btnConfirm.setImportantForAccessibility(important);
        if (vm.loadingState == AppPickerViewModel.LoadingState.LOADING && !vm.appsLoaded) {
            loadingPanel.setVisibility(View.VISIBLE);
            tvLoadingState.setText(R.string.app_picker_loading_apps);
            rv.setVisibility(View.INVISIBLE);
            return;
        }
        if (vm.loadingState == AppPickerViewModel.LoadingState.ERROR && !vm.appsLoaded) {
            loadingPanel.setVisibility(View.VISIBLE);
            String reason = vm.loadingError == null ? "" : ("\n" + vm.loadingError);
            tvLoadingState.setText(getString(R.string.app_picker_load_failed) + reason);
            rv.setVisibility(View.INVISIBLE);
            return;
        }
        loadingPanel.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        etSearch.requestFocus();
    }

    private void restoreControlState() {
        etSearch.setText(vm.query);
        etSearch.setSelection(etSearch.getText() == null ? 0 : etSearch.getText().length());
        etSearch.requestFocus();
        cbDescending.setChecked(vm.descending);
        updateFilterStateLabel();
    }

    @Override
    public void onBackPressed() {
        vm.updateWorkingSelection(vm.originalCheckedPackages);
        AppLogger.i(TAG, "cancel(back)");
        AppLogger.e(TAG, "AppPicker finish(cancel back)", new Exception("finish trace"));
        finishWithReason("back_pressed");
    }

    private void showFilterMenu() {
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.btnFilter));
        Menu m = menu.getMenu();
        MenuItem checked = m.add(0, 1, 0, R.string.filter_checked_only).setCheckable(true);
        MenuItem unchecked = m.add(0, 2, 1, R.string.filter_unchecked_only).setCheckable(true);
        MenuItem system = m.add(0, 3, 2, R.string.filter_system_apps).setCheckable(true);
        MenuItem user = m.add(0, 4, 3, R.string.filter_user_apps).setCheckable(true);

        checked.setChecked(vm.checkedFilter == AppPickerViewModel.CheckedFilter.CHECKED_ONLY);
        unchecked.setChecked(vm.checkedFilter == AppPickerViewModel.CheckedFilter.UNCHECKED_ONLY);
        system.setChecked(vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.SYSTEM_ONLY);
        user.setChecked(vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.USER_ONLY);

        menu.setOnMenuItemClickListener(item -> {
            pendingScrollTop = true;
            if (item.getItemId() == 1) {
                vm.setCheckedFilter(vm.checkedFilter == AppPickerViewModel.CheckedFilter.CHECKED_ONLY
                        ? AppPickerViewModel.CheckedFilter.ALL
                        : AppPickerViewModel.CheckedFilter.CHECKED_ONLY);
            } else if (item.getItemId() == 2) {
                vm.setCheckedFilter(vm.checkedFilter == AppPickerViewModel.CheckedFilter.UNCHECKED_ONLY
                        ? AppPickerViewModel.CheckedFilter.ALL
                        : AppPickerViewModel.CheckedFilter.UNCHECKED_ONLY);
            } else if (item.getItemId() == 3) {
                vm.setAppTypeFilter(vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.SYSTEM_ONLY
                        ? AppPickerViewModel.AppTypeFilter.ALL
                        : AppPickerViewModel.AppTypeFilter.SYSTEM_ONLY);
            } else if (item.getItemId() == 4) {
                vm.setAppTypeFilter(vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.USER_ONLY
                        ? AppPickerViewModel.AppTypeFilter.ALL
                        : AppPickerViewModel.AppTypeFilter.USER_ONLY);
            }
            AppLogger.i(TAG, "filter changed checked=" + vm.checkedFilter + " type=" + vm.appTypeFilter);
            updateFilterStateLabel();
            return true;
        });
        menu.show();
    }

    private void showSortMenu() {
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.btnSort));
        menu.getMenu().add(0, 1, 0, R.string.sort_by_label);
        menu.getMenu().add(0, 2, 1, R.string.sort_by_package);
        menu.getMenu().add(0, 3, 2, R.string.sort_by_install_time);
        menu.getMenu().add(0, 4, 3, R.string.sort_by_update_time);
        menu.setOnMenuItemClickListener(item -> {
            pendingScrollTop = true;
            if (item.getItemId() == 2) vm.setSortMode(AppPickerViewModel.SortMode.PACKAGE_NAME);
            else if (item.getItemId() == 3) vm.setSortMode(AppPickerViewModel.SortMode.FIRST_INSTALL_TIME);
            else if (item.getItemId() == 4) vm.setSortMode(AppPickerViewModel.SortMode.LAST_UPDATE_TIME);
            else vm.setSortMode(AppPickerViewModel.SortMode.LABEL);
            AppLogger.i(TAG, "sort=" + vm.sortMode);
            return true;
        });
        menu.show();
    }

    private void updateFilterStateLabel() {
        int appTypeTextRes = R.string.filter_all_apps;
        if (vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.SYSTEM_ONLY) appTypeTextRes = R.string.filter_system_apps;
        if (vm.appTypeFilter == AppPickerViewModel.AppTypeFilter.USER_ONLY) appTypeTextRes = R.string.filter_user_apps;

        int checkedRes = R.string.filter_checked_all;
        if (vm.checkedFilter == AppPickerViewModel.CheckedFilter.CHECKED_ONLY) checkedRes = R.string.filter_checked_only;
        if (vm.checkedFilter == AppPickerViewModel.CheckedFilter.UNCHECKED_ONLY) checkedRes = R.string.filter_unchecked_only;

        String appTypeText = appTypeTextRes == R.string.filter_all_apps ? "" : getString(appTypeTextRes);
        String checkedText = checkedRes == R.string.filter_checked_all ? "" : getString(checkedRes);
        String summary;
        if (appTypeText.isEmpty() && checkedText.isEmpty()) summary = getString(R.string.filter_all_apps);
        else if (appTypeText.isEmpty()) summary = checkedText;
        else if (checkedText.isEmpty()) summary = appTypeText;
        else summary = checkedText + " · " + appTypeText;
        tvFilterState.setText(getString(R.string.filter_state_value, summary));
    }

    private void toastInvalidLaunch() {
        mainHandler.post(() -> android.widget.Toast.makeText(this,
                R.string.app_picker_invalid_launch_fallback,
                android.widget.Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppLogger.i(TAG, "AppPickerActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        enteredAtElapsedRealtime = SystemClock.elapsedRealtime();
        if (etSearch != null) etSearch.requestFocus();
        AppLogger.i(TAG, "AppPickerActivity onResume");
    }

    private boolean isAccessibilityClick(Throwable trace) {
        for (StackTraceElement element : trace.getStackTrace()) {
            String className = element.getClassName();
            if (className != null && className.contains("Accessibility")) return true;
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLogger.i(TAG, "AppPickerActivity onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vm == null) return;
        if (rv != null && rv.getLayoutManager() != null) {
            vm.recyclerState = rv.getLayoutManager().onSaveInstanceState();
        }
        if (adapter != null) vm.updateWorkingSelection(adapter.selectedPackages());
        vm.setQuery(etSearch != null && etSearch.getText() != null ? etSearch.getText().toString() : "");
        AppLogger.i(TAG, "AppPickerActivity onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppListRepository.unregisterListener(appListListener);
        if (adapter != null) adapter.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
        AppLogger.i(TAG, "[APP_PICKER] onDestroy finishing=" + isFinishing() + " destroyed=" + isDestroyed());
        AppLogger.i(TAG, "AppPickerActivity onDestroy");
    }
}
