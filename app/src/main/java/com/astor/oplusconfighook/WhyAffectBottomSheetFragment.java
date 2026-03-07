package com.astor.oplusconfighook;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 影响原因说明底部弹窗，解释规则命中原因。
 */
public class WhyAffectBottomSheetFragment extends BottomSheetDialogFragment {

    public interface Actions {
        void onOpenInspector();
        void onOpenPackageRules();
    }

    private static final String TAG = "AppListFlow";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_why_affect_bottom_sheet, null, false);
        final int initialBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(navBars.bottom, ime.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottomPadding + bottomInset);
            return insets;
        });
        dialog.setContentView(content);

        View itemInspector = content.findViewById(R.id.itemOpenInspector);
        View itemRules = content.findViewById(R.id.itemOpenRules);
        View btnConfirm = content.findViewById(R.id.btnWhyConfirm);

        itemInspector.setOnClickListener(v -> {
            if (getParentFragment() instanceof Actions) {
                ((Actions) getParentFragment()).onOpenInspector();
            }
            dismissAllowingStateLoss();
        });
        itemRules.setOnClickListener(v -> {
            if (getParentFragment() instanceof Actions) {
                ((Actions) getParentFragment()).onOpenPackageRules();
            }
            dismissAllowingStateLoss();
        });
        btnConfirm.setOnClickListener(v -> dismissAllowingStateLoss());
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        AppLogger.i(TAG, "WhyAffectBottomSheet onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        AppLogger.i(TAG, "WhyAffectBottomSheet onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        AppLogger.i(TAG, "WhyAffectBottomSheet onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        AppLogger.i(TAG, "WhyAffectBottomSheet onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLogger.i(TAG, "WhyAffectBottomSheet onDestroy");
    }
}
