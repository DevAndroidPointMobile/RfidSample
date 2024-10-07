package device.apps.rfidsamplev2.data;

import java.util.ArrayList;
import java.util.List;

public class ConfigData {

    public String name;

    public String value;

    static final String TERMINATOR_NONE = "0";
    static final String TERMINATOR_SPACE = "1";
    static final String TERMINATOR_TAB = "2";
    static final String TERMINATOR_LF = "3";
    static final String TERMINATOR_TAB_LF = "4";

    public static final int VALUE_MAX_Q = 15;
    public static final int VALUE_MAX_POWER = 300;
    public static final int VALUE_MAX_SUSPEND_TIME = 30;

    public ConfigData(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static ConfigData build() {
        return new ConfigData("unknown", "0");
    }

    public static List<ConfigData> terminator() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("None", TERMINATOR_NONE));
        result.add(new ConfigData("Space", TERMINATOR_SPACE));
        result.add(new ConfigData("CR", TERMINATOR_TAB));
        result.add(new ConfigData("LF", TERMINATOR_LF));
        result.add(new ConfigData("CRLF", TERMINATOR_TAB_LF));
        return result;
    }

    public static List<ConfigData> volume() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("Disable", "0"));
        result.add(new ConfigData("Low", "1"));
        result.add(new ConfigData("High", "2"));
        return result;
    }

    public static List<ConfigData> keymap() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("[ T ] Inventory\n[ B ] Inventory", "rr"));
        result.add(new ConfigData("[ T ] Inventory\n[ B ] Scanner", "rs"));
        result.add(new ConfigData("[ T ] Scanner\n[ B ] Inventory", "sr"));
        result.add(new ConfigData("[ T ] Scanner\n[ B ] Scanner", "ss"));
        return result;
    }

    public static List<ConfigData> session() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("S0", "0"));
        result.add(new ConfigData("S1", "1"));
        result.add(new ConfigData("S2", "2"));
        result.add(new ConfigData("S3", "3"));
        return result;
    }

    public static List<ConfigData> inventoryFlag() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("AB", "0"));
        result.add(new ConfigData("A", "1"));
        result.add(new ConfigData("B", "2"));
        return result;
    }

    public static List<ConfigData> linkProfile() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("01: NA-103", "103"));
        result.add(new ConfigData("02: 11-302", "302"));
        result.add(new ConfigData("03: NA-120", "120"));
        result.add(new ConfigData("05: NA-345", "323"));
        result.add(new ConfigData("06: 15-344", "344"));
        result.add(new ConfigData("07: 12-223", "223"));
        result.add(new ConfigData("08: 03-222", "222"));
        result.add(new ConfigData("09: 05-241", "241"));
        result.add(new ConfigData("10: 07-244", "244"));
        result.add(new ConfigData("11: 13-285", "285"));
        return result;
    }

    public static List<ConfigData> target() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("S0", "0"));
        result.add(new ConfigData("S1", "1"));
        result.add(new ConfigData("S2", "2"));
        result.add(new ConfigData("S3", "3"));
        result.add(new ConfigData("SL", "4"));
        return result;
    }

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

    public static List<ConfigData> banks() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("RESERVED", "0"));
        result.add(new ConfigData("EPC", "1"));
        result.add(new ConfigData("TID", "2"));
        result.add(new ConfigData("USER", "3"));
        return result;
    }

    public static List<ConfigData> packetOptions() {
        final List<ConfigData> result = new ArrayList<>();
        result.add(new ConfigData("PC EPC", "0"));
        result.add(new ConfigData("EPC ONLY", "32"));
        result.add(new ConfigData("PC EPC CHECKSUM", "1"));
        result.add(new ConfigData("PC EPC RSSI", "2"));
        result.add(new ConfigData("PC EPC FREQUENCY", "8"));
        result.add(new ConfigData("SHOW ALL", "11"));
        return result;
    }
}
