package com.astor.oplusconfighook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.astor.oplusconfighook.editor.EditorRegistry;
import com.astor.oplusconfighook.editor.EditorSpec;
import com.astor.oplusconfighook.editor.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class EditableListEditorBottomSheet extends BottomSheetDialogFragment {
    public static final String MODE_PLAIN_LIST = "PLAIN_LIST";
    public static final String MODE_KEY_VALUE = "KEY_VALUE";
    public static final String MODE_CSV_RULE = "CSV_RULE";
    public static final String MODE_TOKEN_LIST = "TOKEN_LIST";
    public static final String MODE_ANDROID_FREEZE = "ANDROID_FREEZE";

    public interface Host {
        void onListEditorConfirmed(String requestKey, ArrayList<String> edited);
        void onListEditorCancelled(String requestKey);

        default void onListEditorPickAppsRequested(String requestKey, ArrayList<String> current) {
        }
    }

    private static final String ARG_REQUEST_KEY = "requestKey";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_INITIAL_ITEMS = "initialItems";
    private static final String ARG_ENABLE_PICK_APPS = "enablePickApps";
    private static final String ARG_PICK_APPS_LABEL = "pickAppsLabel";
    private static final String ARG_MODE = "mode";
    private static final String STATE_ITEMS = "state.items";
    private static final String STATE_INPUT = "state.input";
    private static final String STATE_RV = "state.rv";

    private final ArrayList<String> items = new ArrayList<>();
    private TextInputEditText etInput;
    private TextView tvErrors;
    private ItemAdapter adapter;
    private RecyclerView rvItems;
    private Parcelable rvState;

    public static EditableListEditorBottomSheet newInstance(
            String requestKey,
            String title,
            @Nullable String description,
            ArrayList<String> initialItems,
            String mode,
            boolean enablePickApps,
            @Nullable String pickAppsLabel
    ) {
        EditableListEditorBottomSheet sheet = new EditableListEditorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putStringArrayList(ARG_INITIAL_ITEMS, initialItems == null ? new ArrayList<>() : initialItems);
        args.putBoolean(ARG_ENABLE_PICK_APPS, enablePickApps);
        args.putString(ARG_PICK_APPS_LABEL, pickAppsLabel);
        args.putString(ARG_MODE, mode);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_editable_list_editor, container, false);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        int initialSize = args == null || args.getStringArrayList(ARG_INITIAL_ITEMS) == null
                ? 0 : args.getStringArrayList(ARG_INITIAL_ITEMS).size();
        AppLogger.i("ListEditor", "create key=" + requestKey() + " initialSize=" + initialSize + " saved=" + (savedInstanceState != null));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                ViewGroup.LayoutParams lp = sheet.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                sheet.setLayoutParams(lp);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                sheet.requestLayout();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        AppLogger.i("ListEditor", "viewCreated key=" + requestKey() + " saved=" + (savedInstanceState != null) + " items=" + items.size());
        String title = args.getString(ARG_TITLE, "");
        String description = args.getString(ARG_DESCRIPTION, "");
        boolean enablePickApps = args.getBoolean(ARG_ENABLE_PICK_APPS, false);
        String pickAppsLabel = args.getString(ARG_PICK_APPS_LABEL);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDescription = view.findViewById(R.id.tvDescription);
        tvErrors = view.findViewById(R.id.tvErrors);
        etInput = view.findViewById(R.id.etInput);
        rvItems = view.findViewById(R.id.rvItems);
        View sheetRoot = view.findViewById(R.id.sheetRoot);
        if (tvTitle == null || tvDescription == null || etInput == null || rvItems == null || sheetRoot == null) {
            AppLogger.e("ListEditor", "render failed key=" + requestKey() + " titleNull=" + (tvTitle == null) + " inputNull=" + (etInput == null) + " rvNull=" + (rvItems == null), null);
            Toast.makeText(requireContext(), R.string.editor_render_failed_reopen, Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
            return;
        }
        tvTitle.setText(title);
        if (!TextUtils.isEmpty(description)) {
            tvDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(description);
        }

        etInput.setSaveEnabled(false);

        if (savedInstanceState != null) {
            items.clear();
            ArrayList<String> restored = savedInstanceState.getStringArrayList(STATE_ITEMS);
            items.addAll(restored == null ? new ArrayList<>() : restored);
            etInput.setText(savedInstanceState.getString(STATE_INPUT, ""));
            rvState = savedInstanceState.getParcelable(STATE_RV);
        } else {
            items.clear();
            items.addAll(args.getStringArrayList(ARG_INITIAL_ITEMS) == null ? new ArrayList<>() : args.getStringArrayList(ARG_INITIAL_ITEMS));
        }
        dedupeInPlace(items);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        rvItems.setLayoutManager(lm);
        adapter = new ItemAdapter(items, this::showEditDialog, this::removeAt);
        rvItems.setAdapter(adapter);
        if (rvState != null) {
            lm.onRestoreInstanceState(rvState);
        }

        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        MaterialButton btnImport = view.findViewById(R.id.btnImport);
        MaterialButton btnSort = view.findViewById(R.id.btnSort);
        MaterialButton btnClear = view.findViewById(R.id.btnClear);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnPickApps = view.findViewById(R.id.btnPickApps);

        btnAdd.setOnClickListener(v -> addCurrentInput());
        btnImport.setOnClickListener(v -> importFromClipboard());
        btnSort.setOnClickListener(v -> {
            Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
            clearValidationErrors();
            adapter.notifyDataSetChanged();
        });
        btnClear.setOnClickListener(v -> {
            items.clear();
            clearValidationErrors();
            adapter.notifyDataSetChanged();
        });
        btnCancel.setOnClickListener(v -> {
            Host host = findHost();
            if (host != null) host.onListEditorCancelled(args.getString(ARG_REQUEST_KEY, ""));
            dismiss();
        });
        btnConfirm.setOnClickListener(v -> {
            String requestKey = args.getString(ARG_REQUEST_KEY, "");
            EditorSpec spec = EditorRegistry.get(requestKey);
            ValidationResult validation = spec.validate(items, requestKey);
            if (!validation.ok) {
                showValidationErrors(validation);
                return;
            }
            Host host = findHost();
            if (host != null) {
                clearValidationErrors();
                host.onListEditorConfirmed(args.getString(ARG_REQUEST_KEY, ""), dedupedCopy(items));
                dismiss();
                return;
            }
            AppLogger.e("ListEditor", "confirm failed: host not found key=" + requestKey() + " parent=" + (getParentFragment() == null ? "null" : getParentFragment().getClass().getSimpleName()) + " fm=" + getParentFragmentManager().getClass().getSimpleName(), null);
            Toast.makeText(requireContext(), R.string.editor_submit_host_not_found, Toast.LENGTH_SHORT).show();
        });

        if (enablePickApps) {
            btnPickApps.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(pickAppsLabel)) btnPickApps.setText(pickAppsLabel);
            btnPickApps.setOnClickListener(v -> {
                Host host = findHost();
                if (host != null) {
                    host.onListEditorPickAppsRequested(args.getString(ARG_REQUEST_KEY, ""), new ArrayList<>(items));
                    dismiss();
                    return;
                }
                AppLogger.e("ListEditor", "pick apps failed: host not found key=" + requestKey(), null);
                Toast.makeText(requireContext(), R.string.editor_submit_host_not_found, Toast.LENGTH_SHORT).show();
            });
        }

        final int initialBottomPadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(sheetRoot, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(navBars.bottom, ime.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottomPadding + bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(sheetRoot);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_ITEMS, new ArrayList<>(items));
        outState.putString(STATE_INPUT, etInput != null && etInput.getText() != null ? etInput.getText().toString() : "");
        if (rvItems != null && rvItems.getLayoutManager() != null) {
            outState.putParcelable(STATE_RV, rvItems.getLayoutManager().onSaveInstanceState());
        }
    }

    private void addCurrentInput() {
        if (etInput == null || etInput.getText() == null) return;
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        items.add(text);
        dedupeInPlace(items);
        clearValidationErrors();
        etInput.setText("");
        adapter.notifyDataSetChanged();
    }

    private void importFromClipboard() {
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) {
            Toast.makeText(requireContext(), R.string.list_editor_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() <= 0) {
            Toast.makeText(requireContext(), R.string.list_editor_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence cs = clip.getItemAt(0).coerceToText(requireContext());
        if (cs == null) {
            Toast.makeText(requireContext(), R.string.list_editor_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        int before = items.size();
        String[] lines = cs.toString().split("\\r?\\n");
        for (String line : lines) {
            String cleaned = line == null ? "" : line.trim();
            if (!cleaned.isEmpty()) items.add(cleaned);
        }
        dedupeInPlace(items);
        clearValidationErrors();
        adapter.notifyDataSetChanged();
        int added = Math.max(0, items.size() - before);
        Toast.makeText(requireContext(), getString(R.string.list_editor_import_result, added), Toast.LENGTH_SHORT).show();
    }

    private void showEditDialog(int index) {
        if (index < 0 || index >= items.size()) return;
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setText(items.get(index));
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setSaveEnabled(false);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.list_editor_edit_item)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.list_editor_save, (d, w) -> {
                    String text = input.getText() == null ? "" : input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    items.set(index, text);
                    dedupeInPlace(items);
                    clearValidationErrors();
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private void removeAt(int index) {
        if (index < 0 || index >= items.size()) return;
        items.remove(index);
        clearValidationErrors();
        adapter.notifyItemRemoved(index);
    }

    private Host findHost() {
        Fragment parent = getParentFragment();
        if (parent instanceof Host) return (Host) parent;

        FragmentActivity activity = getActivity();
        if (activity != null) {
            for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
                if (fragment instanceof Host) return (Host) fragment;
                for (Fragment child : fragment.getChildFragmentManager().getFragments()) {
                    if (child instanceof Host) return (Host) child;
                }
            }
        }

        if (activity instanceof Host) return (Host) activity;
        return null;
    }

    private String requestKey() {
        Bundle args = getArguments();
        return args == null ? "" : args.getString(ARG_REQUEST_KEY, "");
    }

    private static void dedupeInPlace(List<String> source) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : source) {
            String cleaned = item == null ? "" : item.trim();
            if (!cleaned.isEmpty()) set.add(cleaned);
        }
        source.clear();
        source.addAll(set);
    }

    private static ArrayList<String> dedupedCopy(List<String> source) {
        ArrayList<String> out = new ArrayList<>(source == null ? new ArrayList<>() : source);
        dedupeInPlace(out);
        return out;
    }


    private void clearValidationErrors() {
        if (tvErrors != null) {
            tvErrors.setText("");
            tvErrors.setVisibility(View.GONE);
        }
    }

    private void showValidationErrors(ValidationResult result) {
        if (tvErrors != null) {
            tvErrors.setVisibility(View.VISIBLE);
            tvErrors.setText(TextUtils.join("\n", result.errors));
        }
        Toast.makeText(requireContext(), getString(R.string.list_editor_invalid_lines_count, result.errors.size()), Toast.LENGTH_SHORT).show();
    }

    private static final class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
        interface EditAction { void edit(int index); }
        interface DeleteAction { void delete(int index); }

        private final List<String> values;
        private final EditAction editAction;
        private final DeleteAction deleteAction;

        ItemAdapter(List<String> values, EditAction editAction, DeleteAction deleteAction) {
            this.values = values;
            this.editAction = editAction;
            this.deleteAction = deleteAction;
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editable_list_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(values.get(position), position, editAction, deleteAction);
        }

        @Override
        public int getItemCount() { return values.size(); }

        @Override
        public long getItemId(int position) {
            return values.get(position).toLowerCase(Locale.ROOT).hashCode();
        }

        static final class VH extends RecyclerView.ViewHolder {
            private final TextView tv;
            private final ImageButton edit;
            private final ImageButton delete;

            VH(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tvItemText);
                edit = itemView.findViewById(R.id.btnEdit);
                delete = itemView.findViewById(R.id.btnDelete);
            }

            void bind(String value, int position, EditAction editAction, DeleteAction deleteAction) {
                tv.setText(value);
                itemView.setOnClickListener(v -> editAction.edit(position));
                edit.setOnClickListener(v -> editAction.edit(position));
                delete.setOnClickListener(v -> deleteAction.delete(position));
            }
        }
    }
}
