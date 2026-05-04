package device.apps.rfidsamplev2.data;

/**
 * Identifier for every RF88 setting the {@code ConfigActivity} can read or write. Each
 * constant carries its own human-readable label via {@link #displayName()} so the UI
 * never has to expose the raw enum identifier.
 *
 * <p>Declaration order is significant — it doubles as the display order on the
 * configuration list. Entries are loosely grouped by topic for readability:
 * <ul>
 *   <li><b>Q algorithm</b> — {@link #START_Q}, {@link #MIN_Q}, {@link #MAX_Q}, plus
 *       the toggles {@link #INCREMENT_Q}, {@link #DECREMENT_Q}, {@link #FIXED_Q}.</li>
 *   <li><b>Class1-Gen2 protocol</b> — {@link #TARGET}, {@link #ACTION}, {@link #BANK},
 *       {@link #POINTER}, {@link #ACCESS_PASSWORD}, {@link #SESSION},
 *       {@link #SEARCH_MODE}.</li>
 *   <li><b>Device behaviour</b> — {@link #VOLUME}, {@link #SUSPEND_TIME},
 *       {@link #KEY_MAP}, {@link #INVENTORY_RESPONSE}, {@link #CONTINUOUS},
 *       {@link #VIBRATE}.</li>
 *   <li><b>Radio</b> — {@link #POWER}, {@link #LINK_PROFILE}.</li>
 * </ul>
 */
public enum Configuration {
    START_Q("Start Q"),
    MIN_Q("Min Q"),
    MAX_Q("Max Q"),
    TARGET("Target"),
    ACTION("Action"),
    BANK("Memory Bank"),
    POINTER("Pointer"),
    ACCESS_PASSWORD("Access Password"),
    VOLUME("Buzzer Volume"),
    SUSPEND_TIME("Suspend Time"),
    KEY_MAP("Key Mapping"),
    INVENTORY_RESPONSE("Inventory Response"),
    INCREMENT_Q("Increment Q"),
    DECREMENT_Q("Decrement Q"),
    FIXED_Q("Fixed Q"),
    CONTINUOUS("Continuous Mode"),
    VIBRATE("Vibrator"),
    POWER("Power"),
    SESSION("Session"),
    SEARCH_MODE("Search Mode"),
    LINK_PROFILE("Link Profile");

    private final String displayName;

    Configuration(String displayName) {
        this.displayName = displayName;
    }

    /** Label shown to the user — never expose {@link #name()} in the UI. */
    public String displayName() {
        return displayName;
    }
}
