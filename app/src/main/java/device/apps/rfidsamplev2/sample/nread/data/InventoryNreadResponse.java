package device.apps.rfidsamplev2.sample.nread.data;

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
