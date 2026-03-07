package com.astor.oplusconfighook;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用选择列表适配器，负责条目绑定与选择状态维护。
 */
public class AppSelectAdapter extends ListAdapter<AppInfo, AppSelectAdapter.VH> {
    public interface SelectionListener {
        void onSelectionChanged(Set<String> selected);
    }

    private final PackageManager packageManager;
    private final LruCache<String, Drawable> iconCache = new LruCache<>(256);
    private final Set<String> iconLoading = new LinkedHashSet<>();
    private final ExecutorService iconExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SelectionListener selectionListener;
    private final Set<String> selected = new LinkedHashSet<>();

    public AppSelectAdapter(PackageManager packageManager,
                            Set<String> selectedPackages,
                            SelectionListener selectionListener) {
        super(DIFF);
        this.packageManager = packageManager;
        this.selectionListener = selectionListener;
        if (selectedPackages != null) selected.addAll(selectedPackages);
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<AppInfo> DIFF = new DiffUtil.ItemCallback<AppInfo>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.packageName.equals(newItem.packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.packageName.equals(newItem.packageName)
                    && oldItem.label.equals(newItem.label)
                    && oldItem.systemApp == newItem.systemApp
                    && oldItem.firstInstallTime == newItem.firstInstallTime
                    && oldItem.lastUpdateTime == newItem.lastUpdateTime;
        }
    };

    @Override
    public long getItemId(int position) {
        AppInfo info = getItem(position);
        return info.packageName.hashCode();
    }

    public void submitApps(List<AppInfo> apps, Runnable committed) {
        submitList(apps == null ? new ArrayList<>() : new ArrayList<>(apps), committed);
    }

    public void setSelectedPackages(Set<String> packages) {
        selected.clear();
        if (packages != null) selected.addAll(packages);
        notifyDataSetChanged();
    }

    public Set<String> selectedPackages() {
        return new LinkedHashSet<>(selected);
    }

    public void shutdown() {
        iconExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_select, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppInfo info = getItem(position);
        h.boundPackageName = info.packageName;

        String label = info.label == null || info.label.trim().isEmpty() ? info.packageName : info.label;
        String pkg = info.packageName == null ? "" : info.packageName;
        h.label.setText(label);
        h.pkg.setText(pkg);
        h.systemBadge.setVisibility(info.systemApp ? View.VISIBLE : View.GONE);

        Drawable icon = iconCache.get(pkg);
        h.icon.setImageDrawable(null);
        if (icon != null) h.icon.setImageDrawable(icon);
        else loadIconAsync(h, pkg);

        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(selected.contains(pkg));
        h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> toggle(pkg, isChecked));
        h.itemView.setOnClickListener(v -> h.cb.performClick());
        h.itemView.setAlpha(1f);
        h.itemView.setTranslationX(0f);
        h.itemView.setTranslationY(0f);
    }

    private void toggle(String packageName, boolean checked) {
        if (checked) selected.add(packageName); else selected.remove(packageName);
        if (selectionListener != null) selectionListener.onSelectionChanged(selectedPackages());
    }

    private void loadIconAsync(VH holder, String packageName) {
        if (iconLoading.contains(packageName)) return;
        iconLoading.add(packageName);
        iconExecutor.execute(() -> {
            Drawable loaded = null;
            try {
                loaded = packageManager.getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            Drawable finalLoaded = loaded;
            mainHandler.post(() -> {
                iconLoading.remove(packageName);
                if (finalLoaded == null) return;
                iconCache.put(packageName, finalLoaded);
                if (!packageName.equals(holder.boundPackageName)) return;
                holder.icon.setImageDrawable(finalLoaded);
            });
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;
        TextView pkg;
        TextView systemBadge;
        CheckBox cb;
        String boundPackageName;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivIcon);
            label = itemView.findViewById(R.id.tvLabel);
            pkg = itemView.findViewById(R.id.tvPackage);
            systemBadge = itemView.findViewById(R.id.tvSystemBadge);
            cb = itemView.findViewById(R.id.cbSelected);
        }
    }
}
