package ossim.util;

import java.util.ArrayList;
import java.util.List;

/** Input parsing with descriptive validation messages. */
public final class Parse {
    private Parse() { }

    public static int integer(String raw, String label, int min, int max) {
        if (raw == null || raw.isBlank()) {
            throw new InputException(label + " is blank.");
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InputException(label + ": '" + raw.trim() + "' is not a valid whole number.");
        }
        if (value < min || value > max) {
            String upper = max == Integer.MAX_VALUE ? "no upper bound" : String.valueOf(max);
            throw new InputException(label + ": " + value + " is out of range (minimum " + min + ", maximum " + upper + ").");
        }
        return value;
    }

    public static int integer(String raw, String label, int min) {
        return integer(raw, label, min, Integer.MAX_VALUE);
    }

    /** Parses numbers separated by commas, semicolons or whitespace. */
    public static List<Integer> integerList(String text, String label, int min, int max) {
        if (text == null || text.isBlank()) {
            throw new InputException(label + " is blank.");
        }
        String[] tokens = text.trim().split("[\\s,;]+");
        List<Integer> values = new ArrayList<>(tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            values.add(integer(tokens[i], label + " (item " + (i + 1) + ")", min, max));
        }
        return values;
    }
}
