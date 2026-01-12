package device.apps.rfidsamplev2.sample.inventory.data;

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
