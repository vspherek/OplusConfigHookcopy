package com.astor.oplusconfighook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 自启动配置页面，展示并编辑白名单相关策略。
 */
public class AutostartFragment extends Fragment {
    private Set<String> staged = new LinkedHashSet<>();
    private TextView tvSource;

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                ArrayList<String> selected = result.getData().getStringArrayListExtra(AppPickerActivity.EXTRA_SELECTED);
                staged = new LinkedHashSet<>(selected == null ? new ArrayList<>() : selected);
                if (!shouldSuppressUnappliedToast()) {
                    Toast.makeText(requireContext(), R.string.staged_not_applied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    byte[] data = in == null ? null : in.readAllBytes();
                    if (data == null || PolicyStore.parseAutostartPackages(data).isEmpty()) throw new IllegalArgumentException(getString(R.string.autostart_invalid));
                    PolicyStore.saveAutostartRaw(requireContext(), data);
                    PolicyStore.refresh(requireContext());
                    staged = new LinkedHashSet<>(PolicyStore.parseAutostartPackages(PolicyStore.autostartBytesFast()));
                    updateSource();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), getString(R.string.import_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                if (uri == null) return;
                try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    PolicyStore.refresh(requireContext());
                    byte[] bytes = PolicyStore.autostartBytesFast();
                    if (bytes != null) out.write(bytes);
                    Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), getString(R.string.export_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_autostart, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvSource = view.findViewById(R.id.tvAutostartSource);
        view.<MaterialButton>findViewById(R.id.btnEditAutostart).setOnClickListener(v -> openPicker());
        view.<MaterialButton>findViewById(R.id.btnApplyAutostart).setOnClickListener(v -> applyStaged());
        view.<MaterialButton>findViewById(R.id.btnRestoreAutostart).setOnClickListener(v -> {
            PolicyStore.deleteAutostartUser(requireContext());
            PolicyStore.refresh(requireContext());
            reloadStagedFromEffective();
            updateSource();
        });
        view.<MaterialButton>findViewById(R.id.btnImportAutostart).setOnClickListener(v -> importLauncher.launch(new String[]{"text/plain"}));
        view.<MaterialButton>findViewById(R.id.btnExportAutostart).setOnClickListener(v -> exportLauncher.launch("autostart_white_list.txt"));

        reloadStagedFromEffective();
        updateSource();
    }


    private boolean shouldSuppressUnappliedToast() {
        Activity activity = getActivity();
        return activity == null
                || !isAdded()
                || getView() == null
                || activity.isChangingConfigurations()
                || activity.isFinishing()
                || isRemoving();
    }
    private void openPicker() {
        Intent i = new Intent(requireContext(), AppPickerActivity.class);
        i.putExtra(AppPickerActivity.EXTRA_MODE, "AUTOSTART");
        i.putStringArrayListExtra(AppPickerActivity.EXTRA_PRESELECTED, new ArrayList<>(staged));
        pickerLauncher.launch(i);
    }

    private void applyStaged() {
        try {
            PolicyStore.migrateUserDirCeToDpIfNeeded(requireContext());
            PolicyStore.saveAutostartPackages(requireContext(), staged);
            updateSource();
            Toast.makeText(requireContext(), R.string.policy_applied, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.export_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void reloadStagedFromEffective() {
        PolicyStore.refresh(requireContext());
        staged = new LinkedHashSet<>(PolicyStore.parseAutostartPackages(PolicyStore.autostartBytesFast()));
    }

    private void updateSource() {
        String source = PolicyStore.autostartSource() == PolicyStore.Source.USER ? getString(R.string.source_user) : getString(R.string.source_asset);
        tvSource.setText(getString(R.string.current_source_value, source));
    }
}
