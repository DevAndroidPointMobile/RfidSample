package device.apps.rfidsamplev2.sample.nread.data;

/**
 * One tag report emitted by the RF88 SDK during an inventory-and-read scan, plus the
 * running read count maintained by {@code InventoryNreadViewModel} as the same EPC
 * is re-discovered.
 *
 * <p>Field meanings:
 * <ul>
 *     <li>{@code epcData} — the row label shown on the tile. Stored as the
 *         "{@code data}\n{@code readData}" composite that
 *         {@code InventoryNreadViewModel.onInventoryAndReadDiscovered} produces, so
 *         the tile can render EPC + read-bytes on two lines without a separate field.
 *         Also serves as the de-dup key.</li>
 *     <li>{@code ascii} — ASCII rendering of the EPC, when meaningful.</li>
 *     <li>{@code readValue} — the bytes read from the memory bank requested in
 *         {@code InventoryNreadViewModel.inventoryStart()} (TID by default).</li>
 *     <li>{@code readCount} — how many times this EPC has been seen during the current
 *         session; bumped by {@code InventoryNreadViewModel.inventoryProcess}.</li>
 * </ul>
 */
public class InventoryNreadResponse {
    String epcData;
    String ascii;
    String readValue;
    int readCount;

    public InventoryNreadResponse(String epcData, String ascii, String readValue, int readCount) {
        this.epcData = epcData;
        this.ascii = ascii;
        this.readValue = readValue;
        this.readCount = readCount;
    }

    public String getEpcData() {
        return epcData;
    }

    public void setEpcData(String epcData) {
        this.epcData = epcData;
    }

    public String getAscii() {
        return ascii;
    }

    public void setAscii(String ascii) {
        this.ascii = ascii;
    }

    public String getReadValue() {
        return readValue;
    }

    public void setReadValue(String readValue) {
        this.readValue = readValue;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    @Override
    public String toString() {
        return "InventoryNreadResponse{" +
                "epcData='" + epcData + '\'' +
                ", ascii='" + ascii + '\'' +
                ", readValue='" + readValue + '\'' +
                ", readCount=" + readCount +
                '}';
    }
}
