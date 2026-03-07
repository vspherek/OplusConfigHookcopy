package com.astor.oplusconfighook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 设置页面，提供开关项与基础行为控制。
 */
public class SettingsFragment extends Fragment {
    private boolean bindingUi = false;
    private final ActivityResultLauncher<String> exportLogsLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                if (uri == null) return;
                try {
                    LogExporter.exportLogsZip(requireContext(), uri);
                    Toast.makeText(requireContext(), R.string.export_logs_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    AppLogger.e("SettingsFragment", "export logs failed", e);
                    Toast.makeText(requireContext(), getString(R.string.export_failed_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            });

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Prefs.ensureDefaults(requireContext(), "SettingsFragment.onViewCreated");
        MaterialCheckBox cbMaster = view.findViewById(R.id.cbMaster);
        MaterialCheckBox cbAutostart = view.findViewById(R.id.cbAutostart);
        MaterialCheckBox cbTombstone = view.findViewById(R.id.cbTombstone);
        MaterialCheckBox cbLogOnly = view.findViewById(R.id.cbLogOnly);
        MaterialCheckBox cbLogStack = view.findViewById(R.id.cbLogStack);
        MaterialCheckBox cbAllowAndroidFsHook = view.findViewById(R.id.cbAllowAndroidFsHook);
        MaterialCheckBox cbDebugForceMaster = view.findViewById(R.id.cbDebugForceMaster);
        MaterialButton btnCleanupSystemState = view.findViewById(R.id.btnCleanupSystemState);
        MaterialCheckBox cbAdvancedPolicy = view.findViewById(R.id.cbAdvancedPolicyEdit);
        MaterialCheckBox cbPhonemanagerReadconfigObserve = view.findViewById(R.id.cbPhonemanagerReadconfigObserve);
        MaterialCheckBox cbPhonemanagerReadconfigModify = view.findViewById(R.id.cbPhonemanagerReadconfigModify);
        MaterialCheckBox cbPhonemanagerFsHook = view.findViewById(R.id.cbPhonemanagerFsHook);
        MaterialCheckBox cbSafecenterReadconfigObserve = view.findViewById(R.id.cbSafecenterReadconfigObserve);
        MaterialCheckBox cbSafecenterReadconfigModify = view.findViewById(R.id.cbSafecenterReadconfigModify);
        MaterialCheckBox cbSafecenterFsHook = view.findViewById(R.id.cbSafecenterFsHook);
        TextView tvHookConfigSource = view.findViewById(R.id.tvHookConfigSource);
        TextView tvHookMasterResolved = view.findViewById(R.id.tvHookMasterResolved);
        TextView tvHookAutostartResolved = view.findViewById(R.id.tvHookAutostartResolved);
        TextView tvHookTombstoneResolved = view.findViewById(R.id.tvHookTombstoneResolved);
        TextView tvHookUserFact = view.findViewById(R.id.tvHookUserFact);
        TextView tvAboutInfo = view.findViewById(R.id.tvAboutInfo);

        bindingUi = true;
        cbMaster.setChecked(Prefs.enabledMaster(requireContext()));
        cbAutostart.setChecked(Prefs.enabledAutostart(requireContext()));
        cbTombstone.setChecked(Prefs.enabledTombstone(requireContext()));
        cbLogOnly.setChecked(Prefs.logOnlyMatches(requireContext()));
        cbLogStack.setChecked(Prefs.logStack(requireContext()));
        cbAllowAndroidFsHook.setChecked(Prefs.allowAndroidFsHook(requireContext()));
        cbDebugForceMaster.setChecked(Prefs.debugForceMaster(requireContext()));
        cbAdvancedPolicy.setChecked(Prefs.tombstoneAdvancedEdit(requireContext()));
        cbPhonemanagerReadconfigObserve.setChecked(Prefs.allowPhonemanagerReadConfigObserve(requireContext()));
        cbPhonemanagerReadconfigModify.setChecked(Prefs.allowPhonemanagerReadConfigModify(requireContext()));
        cbPhonemanagerFsHook.setChecked(Prefs.allowPhonemanagerFsHook(requireContext()));
        cbSafecenterReadconfigObserve.setChecked(Prefs.allowSafecenterReadConfigObserve(requireContext()));
        cbSafecenterReadconfigModify.setChecked(Prefs.allowSafecenterReadConfigModify(requireContext()));
        cbSafecenterFsHook.setChecked(Prefs.allowSafecenterFsHook(requireContext()));
        bindingUi = false;
        refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        tvAboutInfo.setText(getString(R.string.about_version_label) + resolveVersionName() + "\n"
                + getString(R.string.about_author_label) + getString(R.string.author_name));
        Prefs.logMasterReadUi(requireContext(), "SettingsFragment.onViewCreated");
        Prefs.logMasterDiag(requireContext(), "SettingsFragment.onViewCreated");
        Prefs.logUiBoolDiag(requireContext(), "SettingsFragment.onViewCreated", Prefs.KEY_LOG_STACKTRACE, false);
        Prefs.logUiBoolDiag(requireContext(), "SettingsFragment.onViewCreated", Prefs.KEY_TOMBSTONE_ADVANCED_EDIT, false);

        updateChildEnabled(cbMaster.isChecked(), cbAutostart, cbTombstone, cbAllowAndroidFsHook);

        cbMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ENABLED_MASTER, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
            updateChildEnabled(isChecked, cbAutostart, cbTombstone, cbAllowAndroidFsHook);
        });
        cbAutostart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ENABLED_AUTOSTART, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbTombstone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ENABLED_TOMBSTONE, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
            cbAllowAndroidFsHook.setEnabled(cbMaster.isChecked() && isChecked);
        });
        cbAllowAndroidFsHook.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_ANDROID_FS_HOOK, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbDebugForceMaster.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_DEBUG_FORCE_MASTER, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        btnCleanupSystemState.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.cleanup_system_state_title)
                    .setMessage(R.string.cleanup_system_state_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.cleanup_system_state_continue, (d, w) -> {
                        btnCleanupSystemState.setEnabled(false);
                        btnCleanupSystemState.setText(R.string.cleanup_system_state_running);
                        new Thread(() -> {
                            Prefs.CleanupResult result = Prefs.cleanSystemPersistentState(requireContext().getApplicationContext());
                            requireActivity().runOnUiThread(() -> {
                                btnCleanupSystemState.setEnabled(true);
                                btnCleanupSystemState.setText(R.string.cleanup_system_state_button);
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
                            });
                        }).start();
                    })
                    .show();
        });
        cbLogOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_LOG_ONLY_MATCHED, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbLogStack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_LOG_STACKTRACE, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbAdvancedPolicy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            if (!isChecked) {
                Prefs.putBoolean(requireContext(), Prefs.KEY_TOMBSTONE_ADVANCED_EDIT, false);
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.advanced_editing)
                    .setMessage(R.string.advanced_editing_confirm)
                    .setNegativeButton(R.string.cancel, (d, w) -> cbAdvancedPolicy.setChecked(false))
                    .setPositiveButton(R.string.confirm_stage, (d, w) ->
                            Prefs.putBoolean(requireContext(), Prefs.KEY_TOMBSTONE_ADVANCED_EDIT, true))
                    .show();
        });
        cbPhonemanagerReadconfigObserve.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_OBSERVE, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbPhonemanagerReadconfigModify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_PHONEMANAGER_READCONFIG_MODIFY, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbPhonemanagerFsHook.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_PHONEMANAGER_FS_HOOK, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbSafecenterReadconfigObserve.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_OBSERVE, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbSafecenterReadconfigModify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_SAFECENTER_READCONFIG_MODIFY, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });
        cbSafecenterFsHook.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            Prefs.putBoolean(requireContext(), Prefs.KEY_ALLOW_SAFECENTER_FS_HOOK, isChecked);
            refreshHookDiagnostics(tvHookConfigSource, tvHookMasterResolved, tvHookAutostartResolved, tvHookTombstoneResolved, tvHookUserFact);
        });

        view.<MaterialButton>findViewById(R.id.btnDonation).setOnClickListener(v -> startActivity(new Intent(requireContext(), DonationActivity.class)));
        view.<MaterialButton>findViewById(R.id.btnOpenProject).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.project_homepage_url)));
            startActivity(i);
        });

        view.<MaterialButton>findViewById(R.id.btnExportLogs).setOnClickListener(v -> exportLogsLauncher.launch("oplusconfighook-logs.zip"));
        view.<MaterialButton>findViewById(R.id.btnCopyCrash).setOnClickListener(v -> {
            String crash = LogExporter.readLastCrashText();
            if (crash.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_crash_log, Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("crash-log", crash));
            Toast.makeText(requireContext(), R.string.copy_crash_success, Toast.LENGTH_SHORT).show();
        });
    }


    private void refreshHookDiagnostics(TextView tvHookConfigSource,
                                        TextView tvHookMasterResolved,
                                        TextView tvHookAutostartResolved,
                                        TextView tvHookTombstoneResolved,
                                        TextView tvHookUserFact) {
        Context c = requireContext();
        tvHookConfigSource.setText(getString(R.string.hook_config_source_value, Prefs.hookConfigSourceSummary(c)));
        tvHookMasterResolved.setText(getString(R.string.hook_master_resolved_value,
                String.valueOf(Prefs.enabledMaster(c)), Prefs.enabledMasterSource(c)));
        tvHookAutostartResolved.setText(getString(R.string.hook_autostart_resolved_value,
                String.valueOf(Prefs.enabledAutostart(c)), Prefs.enabledAutostartSource(c)));
        tvHookTombstoneResolved.setText(getString(R.string.hook_tombstone_resolved_value,
                String.valueOf(Prefs.enabledTombstone(c)), Prefs.enabledTombstoneSource(c)));
        tvHookUserFact.setText(getString(R.string.hook_user_fact_value,
                String.valueOf(Prefs.hasUserConfigurationFact(c)), Prefs.userConfigurationFactSummary(c)));
    }

    private void updateChildEnabled(boolean enabled, MaterialCheckBox cbAutostart, MaterialCheckBox cbTombstone, MaterialCheckBox cbAllowAndroidFsHook) {
        cbAutostart.setEnabled(enabled);
        cbTombstone.setEnabled(enabled);
        cbAllowAndroidFsHook.setEnabled(enabled && cbTombstone.isChecked());
    }

    private String resolveVersionName() {
        try {
            Context context = requireContext();
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "-";
        }
    }
}
