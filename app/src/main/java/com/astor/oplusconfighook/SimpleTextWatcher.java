package com.astor.oplusconfighook;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

/**
 * 文本监听器简化基类，仅暴露需要覆写的回调。
 */
public class SimpleTextWatcher implements TextWatcher {
    private final Consumer<Editable> onChange;

    public SimpleTextWatcher(Consumer<Editable> onChange) {
        this.onChange = onChange;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override public void afterTextChanged(Editable s) { onChange.accept(s); }
}
