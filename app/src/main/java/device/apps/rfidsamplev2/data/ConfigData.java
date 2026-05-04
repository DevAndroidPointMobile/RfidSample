package device.apps.rfidsamplev2.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A user-facing label paired with the SDK-format value it represents — used to populate
 * the radio choices in the configuration screen (e.g. {@code "S0"} → {@code "0"} for
 * Session). Each {@code public static} list method below is the option set for one
 * setting; {@link #findByValue(java.util.List, String)} reverse-maps an SDK value back
 * to its display label so the list tile can render something readable.
 *
 * <p>The {@code VALUE_MAX_*} constants are the upper bounds used by the seekbar dialog
 * for the corresponding integer settings.
 */
public class ConfigData {

    public String name;

    public String value;

    /** Maximum allowed Q (start / min / max) for the inventory algorithm. Class1-Gen2 §6.3.2.5 — Q is 4 bits, so the legal range is 0..15. */
    public static final int VALUE_MAX_Q = 15;

    /** Highest power the seekbar may set, in 0.1 dBm units (so 300 = 30.0 dBm). Covers the highest variant of the RF88-class reader currently shipped. */
    public static final int VALUE_MAX_POWER = 300;

    /** Maximum suspend timeout in minutes, after which an idle reader powers down to save battery. */
    public static final int VALUE_MAX_SUSPEND_TIME = 30;

    public ConfigData(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /** Buzzer volume options. */
    public static List<ConfigData> volume() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("Disable", "0"));
        result.add(new ConfigData("Low", "1"));
        result.add(new ConfigData("High", "2"));
        return result;
    }

    /** Class1-Gen2 inventory session (S0..S3) — controls how long a tag stays "inventoried" before it can be re-counted. */
    public static List<ConfigData> session() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("S0", "0"));
        result.add(new ConfigData("S1", "1"));
        result.add(new ConfigData("S2", "2"));
        result.add(new ConfigData("S3", "3"));
        return result;
    }

    /** Search mode — single-target (A or B) or alternating (AB). Used in conjunction with {@link #session()}. */
    public static List<ConfigData> searchMode() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("AB", "0"));
        result.add(new ConfigData("A", "1"));
        result.add(new ConfigData("B", "2"));
        return result;
    }

    /** Vendor-defined RF link profile presets — different miller / divide-ratio / Tari combinations tuned for environment trade-offs. */
    public static List<ConfigData> linkProfile() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("01: NA-103", "103"));
        result.add(new ConfigData("02: 11-302", "302"));
        result.add(new ConfigData("03: NA-120", "120"));
        // Profile 04 is intentionally omitted — not supported by the RF88 firmware.
        result.add(new ConfigData("05: NA-345", "323"));
        result.add(new ConfigData("06: 15-344", "344"));
        result.add(new ConfigData("07: 12-223", "223"));
        result.add(new ConfigData("08: 03-222", "222"));
        result.add(new ConfigData("09: 05-241", "241"));
        result.add(new ConfigData("10: 07-244", "244"));
        result.add(new ConfigData("11: 13-285", "285"));
        return result;
    }

    /** Class1-Gen2 select target (S0..S3, SL) — which tag flag the inventory algorithm should toggle. */
    public static List<ConfigData> target() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("S0", "0"));
        result.add(new ConfigData("S1", "1"));
        result.add(new ConfigData("S2", "2"));
        result.add(new ConfigData("S3", "3"));
        result.add(new ConfigData("SL", "4"));
        return result;
    }

    /** Class1-Gen2 select action (3-bit code) — encodes how the matching/non-matching tag flags should change after a Select. Labels are the binary representation. */
    public static List<ConfigData> action() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("000", "0"));
        result.add(new ConfigData("001", "1"));
        result.add(new ConfigData("010", "2"));
        result.add(new ConfigData("011", "3"));
        result.add(new ConfigData("100", "4"));
        result.add(new ConfigData("101", "5"));
        return result;
    }

    /** Class1-Gen2 memory banks (Reserved / EPC / TID / User). */
    public static List<ConfigData> banks() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("RESERVED", "0"));
        result.add(new ConfigData("EPC", "1"));
        result.add(new ConfigData("TID", "2"));
        result.add(new ConfigData("USER", "3"));
        return result;
    }

    /**
     * Inventory response packet contents. Each value is an SDK-defined code that selects
     * which fields the reader appends to every tag report — {@code "0"} is "PC + EPC",
     * {@code "32"} drops the PC for a leaner payload, and {@code "1"}/{@code "2"}/{@code "8"}
     * add checksum / RSSI / frequency respectively. {@code "11"} returns the lot.
     *
     * <p>Note: these are <b>vendor codes</b>, not bitmask additions — combinations
     * outside this list are not guaranteed to work.
     */
    public static List<ConfigData> inventoryResponse() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("PC EPC", "0"));
        result.add(new ConfigData("EPC ONLY", "32"));
        result.add(new ConfigData("PC EPC CHECKSUM", "1"));
        result.add(new ConfigData("PC EPC RSSI", "2"));
        result.add(new ConfigData("PC EPC FREQUENCY", "8"));
        result.add(new ConfigData("SHOW ALL", "11"));
        return result;
    }

    /**
     * Look up the {@link ConfigData} entry whose {@link #value} equals {@code value}.
     * Returns {@code null} when the list contains no matching entry — callers must
     * null-check before dereferencing.
     */
    @Nullable
    public static ConfigData findByValue(List<ConfigData> list, String value) {
        return list.stream()
                .filter(configData -> configData.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
