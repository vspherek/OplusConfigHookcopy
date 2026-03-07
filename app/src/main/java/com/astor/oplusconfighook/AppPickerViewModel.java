package com.astor.oplusconfighook;

import android.os.Parcelable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 应用选择页面 ViewModel，负责排序、过滤与分组。
 */
public class AppPickerViewModel extends ViewModel {
    private static final String TAG = "AppPickerVM";
    public enum LoadingState { LOADING, READY, ERROR }
    public enum CheckedFilter { ALL, CHECKED_ONLY, UNCHECKED_ONLY }
    public enum AppTypeFilter { ALL, SYSTEM_ONLY, USER_ONLY }
    public enum SortMode { LABEL, PACKAGE_NAME, FIRST_INSTALL_TIME, LAST_UPDATE_TIME }

    private static final String KEY_QUERY = "query";
    private static final String KEY_DESC = "descending";
    private static final String KEY_CHECK_FILTER = "check_filter";
    private static final String KEY_APP_FILTER = "app_filter";
    private static final String KEY_SORT = "sort";
    private static final String KEY_CHECKED = "checked_pkgs";
    private static final String STATE_FILTER_BASELINE_CHECKED = "filter_baseline_checked_pkgs";

    private final SavedStateHandle state;

    public final List<AppInfo> allApps = new ArrayList<>();
    public boolean appsLoaded = false;
    public LoadingState loadingState = LoadingState.LOADING;
    public String loadingError;

    public String query;
    public boolean descending;
    public CheckedFilter checkedFilter;
    public AppTypeFilter appTypeFilter;
    public SortMode sortMode;

    public final Set<String> originalCheckedPackages = new LinkedHashSet<>();
    public final Set<String> workingCheckedPackages = new LinkedHashSet<>();
    public final Set<String> filterBaselineCheckedPackages = new LinkedHashSet<>();
    public Parcelable recyclerState;

    private boolean filterBaselineInitialized;
    private boolean selectionInitialized;
    private boolean selectionRestoredFromState;

    private final MutableLiveData<List<AppInfo>> filteredSortedApps = new MutableLiveData<>(new ArrayList<>());

    public AppPickerViewModel(SavedStateHandle state) {
        this.state = state;
        query = valueOrDefault(state.get(KEY_QUERY), "");
        descending = Boolean.TRUE.equals(state.get(KEY_DESC));
        checkedFilter = enumOrDefault(state.get(KEY_CHECK_FILTER), CheckedFilter.ALL);
        appTypeFilter = enumOrDefault(state.get(KEY_APP_FILTER), AppTypeFilter.ALL);
        sortMode = enumOrDefault(state.get(KEY_SORT), SortMode.LABEL);

        ArrayList<String> checked = state.get(KEY_CHECKED);
        if (checked != null) {
            workingCheckedPackages.addAll(checked);
            originalCheckedPackages.addAll(checked);
            selectionInitialized = true;
            selectionRestoredFromState = true;
            AppLogger.i(TAG, "[APP_PICKER_VM] restored working selection size=" + checked.size());
        } else {
            selectionInitialized = false;
            selectionRestoredFromState = false;
        }

        ArrayList<String> baselineChecked = state.get(STATE_FILTER_BASELINE_CHECKED);
        if (baselineChecked != null) {
            filterBaselineCheckedPackages.addAll(baselineChecked);
            filterBaselineInitialized = true;
        } else {
            filterBaselineInitialized = false;
        }
    }

    public LiveData<List<AppInfo>> filteredSortedApps() {
        return filteredSortedApps;
    }

    public void setApps(List<AppInfo> apps) {
        allApps.clear();
        if (apps != null) allApps.addAll(apps);
        appsLoaded = true;
        loadingState = LoadingState.READY;
        loadingError = null;
        recompute();
    }

    public void setLoadError(String error) {
        loadingState = LoadingState.ERROR;
        loadingError = error;
    }

    public void setInitialSelection(Set<String> selected) {
        if (selectionInitialized) {
            AppLogger.i(TAG, "[APP_PICKER_VM] setInitialSelection skip reason="
                    + (selectionRestoredFromState ? "restored_from_state" : "already_initialized"));
            if (!filterBaselineInitialized) {
                filterBaselineCheckedPackages.clear();
                if (selected != null) {
                    filterBaselineCheckedPackages.addAll(selected);
                }
                state.set(STATE_FILTER_BASELINE_CHECKED, new ArrayList<>(filterBaselineCheckedPackages));
                filterBaselineInitialized = true;
            }
            return;
        }

        originalCheckedPackages.clear();
        workingCheckedPackages.clear();
        if (selected != null) {
            originalCheckedPackages.addAll(selected);
            workingCheckedPackages.addAll(selected);
        }
        selectionInitialized = true;
        AppLogger.i(TAG, "[APP_PICKER_VM] setInitialSelection apply reason=first_init size=" + workingCheckedPackages.size());
        if (!filterBaselineInitialized) {
            filterBaselineCheckedPackages.clear();
            if (selected != null) {
                filterBaselineCheckedPackages.addAll(selected);
            }
            state.set(STATE_FILTER_BASELINE_CHECKED, new ArrayList<>(filterBaselineCheckedPackages));
            filterBaselineInitialized = true;
        }
        persistCheckedSet();
    }

    public void setQuery(String q) {
        query = q == null ? "" : q;
        state.set(KEY_QUERY, query);
        recompute();
    }

    public void setDescending(boolean desc) {
        descending = desc;
        state.set(KEY_DESC, descending);
        recompute();
    }

    public void setCheckedFilter(CheckedFilter mode) {
        checkedFilter = mode == null ? CheckedFilter.ALL : mode;
        state.set(KEY_CHECK_FILTER, checkedFilter.name());
        recompute();
    }

    public void setAppTypeFilter(AppTypeFilter mode) {
        appTypeFilter = mode == null ? AppTypeFilter.ALL : mode;
        state.set(KEY_APP_FILTER, appTypeFilter.name());
        recompute();
    }

    public void setSortMode(SortMode mode) {
        sortMode = mode == null ? SortMode.LABEL : mode;
        state.set(KEY_SORT, sortMode.name());
        recompute();
    }

    public void updateWorkingSelection(Set<String> selected) {
        workingCheckedPackages.clear();
        if (selected != null) workingCheckedPackages.addAll(selected);
        persistCheckedSet();
        recompute();
    }

    private void recompute() {
        List<AppInfo> filtered = new ArrayList<>();
        String queryLower = query.trim().toLowerCase(Locale.ROOT);

        for (AppInfo app : allApps) {
            if (appTypeFilter == AppTypeFilter.SYSTEM_ONLY && !app.systemApp) continue;
            if (appTypeFilter == AppTypeFilter.USER_ONLY && app.systemApp) continue;

            boolean baselineChecked = filterBaselineCheckedPackages.contains(app.packageName);
            if (checkedFilter == CheckedFilter.CHECKED_ONLY && !baselineChecked) continue;
            if (checkedFilter == CheckedFilter.UNCHECKED_ONLY && baselineChecked) continue;

            if (!queryLower.isEmpty()) {
                String l = app.label == null ? "" : app.label.toLowerCase(Locale.ROOT);
                String p = app.packageName == null ? "" : app.packageName.toLowerCase(Locale.ROOT);
                if (!l.contains(queryLower) && !p.contains(queryLower)) continue;
            }
            filtered.add(app);
        }

        filtered.sort(buildComparator());
        filteredSortedApps.setValue(filtered);
    }

    private Comparator<AppInfo> buildComparator() {
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);

        Comparator<AppInfo> comparator;
        if (sortMode == SortMode.FIRST_INSTALL_TIME) {
            comparator = (a, b) -> {
                int c = Long.compare(b.firstInstallTime, a.firstInstallTime);
                if (c != 0) return c;
                c = collator.compare(safe(a.label, a.packageName), safe(b.label, b.packageName));
                if (c != 0) return c;
                return safe(a.packageName, "").compareToIgnoreCase(safe(b.packageName, ""));
            };
        } else if (sortMode == SortMode.LAST_UPDATE_TIME) {
            comparator = (a, b) -> {
                int c = Long.compare(b.lastUpdateTime, a.lastUpdateTime);
                if (c != 0) return c;
                c = collator.compare(safe(a.label, a.packageName), safe(b.label, b.packageName));
                if (c != 0) return c;
                return safe(a.packageName, "").compareToIgnoreCase(safe(b.packageName, ""));
            };
        } else {
            comparator = (a, b) -> compareWithBucket(a, b, collator, sortMode == SortMode.PACKAGE_NAME);
        }

        if (descending) {
            return (a, b) -> -comparator.compare(a, b);
        }
        return comparator;
    }

    private int compareWithBucket(AppInfo a, AppInfo b, Collator collator, boolean packageSort) {
        String aKey = packageSort ? safe(a.packageName, "") : safe(a.label, a.packageName);
        String bKey = packageSort ? safe(b.packageName, "") : safe(b.label, b.packageName);

        BucketInfo ba = bucketInfo(aKey);
        BucketInfo bb = bucketInfo(bKey);

        int c = Integer.compare(ba.rank, bb.rank);
        if (c != 0) return c;

        if (ba.rank == 0) {
            c = Character.compare(ba.bucket, bb.bucket);
            if (c != 0) return c;
            c = ba.alphaKey.compareToIgnoreCase(bb.alphaKey);
            if (c != 0) return c;
            c = aKey.compareToIgnoreCase(bKey);
            if (c != 0) return c;
            return safe(a.packageName, "").compareToIgnoreCase(safe(b.packageName, ""));
        }

        c = collator.compare(aKey, bKey);
        if (c != 0) return c;
        return safe(a.packageName, "").compareToIgnoreCase(safe(b.packageName, ""));
    }

    private static final class BucketInfo {
        final int rank;
        final char bucket;
        final String alphaKey;

        BucketInfo(int rank, char bucket, String alphaKey) {
            this.rank = rank;
            this.bucket = bucket;
            this.alphaKey = alphaKey;
        }
    }

    private BucketInfo bucketInfo(String value) {
        if (value == null) return new BucketInfo(1, '#', "");
        int i = 0;
        while (i < value.length()) {
            int cp = value.codePointAt(i);
            if (Character.isWhitespace(cp) || Character.getType(cp) == Character.OTHER_PUNCTUATION
                    || Character.getType(cp) == Character.DASH_PUNCTUATION
                    || Character.getType(cp) == Character.MATH_SYMBOL
                    || Character.getType(cp) == Character.MODIFIER_SYMBOL
                    || Character.getType(cp) == Character.OTHER_SYMBOL) {
                i += Character.charCount(cp);
                continue;
            }
            char c = Character.toUpperCase((char) cp);
            if (c >= 'A' && c <= 'Z') {
                return new BucketInfo(0, c, extractAsciiAlpha(value));
            }
            return new BucketInfo(1, '#', "");
        }
        return new BucketInfo(1, '#', "");
    }

    private String extractAsciiAlpha(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toUpperCase(value.charAt(i));
            if (c >= 'A' && c <= 'Z') sb.append(c);
        }
        return sb.toString();
    }

    private void persistCheckedSet() {
        state.set(KEY_CHECKED, new ArrayList<>(workingCheckedPackages));
    }

    private static String valueOrDefault(String v, String def) {
        return v == null ? def : v;
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback == null ? "" : fallback;
        return value;
    }

    private static <E extends Enum<E>> E enumOrDefault(String raw, E fallback) {
        if (raw == null) return fallback;
        try {
            @SuppressWarnings("unchecked")
            Class<E> enumType = (Class<E>) fallback.getDeclaringClass();
            return Enum.valueOf(enumType, raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
