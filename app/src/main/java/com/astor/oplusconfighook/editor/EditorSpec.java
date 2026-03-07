package com.astor.oplusconfighook.editor;

import com.astor.oplusconfighook.TombstoneViewModel;

import java.util.ArrayList;
import java.util.List;

public interface EditorSpec {
    String mode();

    String description(TombstoneViewModel vm, String requestKey);

    List<String> serialize(TombstoneViewModel vm, String requestKey);

    ValidationResult validate(List<String> lines, String requestKey);

    ApplyResult apply(TombstoneViewModel vm, List<String> lines, String requestKey);

    final class ApplyResult {
        public final boolean ok;
        public final String message;

        private ApplyResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static ApplyResult ok() {
            return new ApplyResult(true, null);
        }

        public static ApplyResult fail(String message) {
            return new ApplyResult(false, message);
        }
    }

    abstract class Base implements EditorSpec {
        protected static List<String> clean(List<String> lines) {
            List<String> out = new ArrayList<>();
            if (lines == null) return out;
            for (String line : lines) {
                String cleaned = line == null ? "" : line.trim();
                if (!cleaned.isEmpty()) out.add(cleaned);
            }
            return out;
        }
    }
}
