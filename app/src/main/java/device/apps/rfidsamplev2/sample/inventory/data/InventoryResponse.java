package device.apps.rfidsamplev2.sample.inventory.data;

/**
 * One tag report emitted by the RF88 SDK during an inventory scan, plus the running
 * read count maintained by {@code InventoryViewModel} as the same EPC is re-discovered.
 *
 * <p>Field meanings (all strings since the SDK delivers them in already-formatted form):
 * <ul>
 *     <li>{@code readLine} — full hex dump line as the SDK reports it (EPC bits, used
 *         as the de-dup key and as the Class1-Gen2 Select mask for Mandatory commands).</li>
 *     <li>{@code ascii} — ASCII rendering of the EPC, when meaningful.</li>
 *     <li>{@code rssi} — signal strength of the read, in dBm, if reporting was enabled
 *         in {@code INVENTORY_RESPONSE} configuration; empty otherwise.</li>
 *     <li>{@code frequency} — channel frequency the read happened on, in MHz; same
 *         "empty when not configured" rule as RSSI.</li>
 *     <li>{@code checksum} — checksum word reported by the tag, when configured.</li>
 *     <li>{@code readCount} — how many times this EPC has been seen during the current
 *         session; bumped by {@code InventoryViewModel.inventoryProcess}.</li>
 * </ul>
 */
public class InventoryResponse {
    String readLine;
    String ascii;
    String rssi;
    String frequency;
    String checksum;
    int readCount;

    public InventoryResponse(String readLine, String ascii, String rssi, String frequency, String checksum, int readCount) {
        this.readLine = readLine;
        this.ascii = ascii;
        this.rssi = rssi;
        this.frequency = frequency;
        this.checksum = checksum;
        this.readCount = readCount;
    }

    public String getReadLine() {
        return readLine;
    }

    public void setReadLine(String readLine) {
        this.readLine = readLine;
    }

    public String getAscii() {
        return ascii;
    }

    public void setAscii(String ascii) {
        this.ascii = ascii;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }
}
