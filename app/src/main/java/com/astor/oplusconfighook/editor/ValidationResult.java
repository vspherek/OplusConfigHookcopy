package com.astor.oplusconfighook.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {
    public final boolean ok;
    public final List<String> errors;

    private ValidationResult(boolean ok, List<String> errors) {
        this.ok = ok;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult error(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
