package device.apps.rfidsamplev2.data;

/**
 * Encoding helpers for the RF88 dual-trigger key mapping setting.
 *
 * <p>The SDK represents the key mapping as a two-character string where the first char is
 * the function assigned to the <b>Top</b> physical trigger and the second to the
 * <b>Bottom</b> trigger. Each char is either {@link #INVENTORY} ({@code 'r'}, RFID
 * inventory) or {@link #SCANNER} ({@code 's'}, barcode scanner). Hence the four legal
 * values are {@code "rr"}, {@code "rs"}, {@code "sr"}, {@code "ss"}.
 *
 * <p>This class exists so the encoding stays in one place — the configuration UI splits
 * the value into two independent toggle groups (Top / Bottom), and the list tile renders
 * a human-readable summary via {@link #describe(String)}.
 */
public final class KeyMap {

    /** Function code for the RFID inventory trigger. */
    public static final char INVENTORY = 'r';
    /** Function code for the barcode scanner trigger. */
    public static final char SCANNER = 's';

    private KeyMap() {
    }

    /** Function assigned to the Top physical trigger, or {@link #INVENTORY} if {@code value} is malformed. */
    public static char top(String value) {
        return (value != null && !value.isEmpty()) ? value.charAt(0) : INVENTORY;
    }

    /** Function assigned to the Bottom physical trigger, or {@link #INVENTORY} if {@code value} is malformed. */
    public static char bottom(String value) {
        return (value != null && value.length() >= 2) ? value.charAt(1) : INVENTORY;
    }

    /** Compose the SDK-format value from the two trigger selections. */
    public static String compose(char top, char bottom) {
        return "" + top + bottom;
    }

    /** Human-readable name for a single function code. */
    public static String label(char function) {
        return function == SCANNER ? "Scanner" : "Inventory";
    }

    /** One-line summary suitable for a list tile, e.g. {@code "Top: Inventory · Bottom: Scanner"}. */
    public static String describe(String value) {
        return "Top: " + label(top(value)) + " · Bottom: " + label(bottom(value));
    }
}
