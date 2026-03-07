package com.astor.oplusconfighook;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

final class UiNotifier {
    private UiNotifier() {
    }

    static void showMessage(@NonNull Fragment fragment, @NonNull CharSequence message) {
        View root = fragment.getView();
        if (root == null) {
            return;
        }

        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        View anchor = fragment.requireActivity().findViewById(R.id.bottomNav);
        if (anchor != null) {
            snackbar.setAnchorView(anchor);
        }

        TextView textView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(6);
        }

        snackbar.show();
    }
}
