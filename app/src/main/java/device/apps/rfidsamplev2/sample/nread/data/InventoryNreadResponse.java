package device.apps.rfidsamplev2.sample.nread.data;

public class InventoryNreadResponse {
    String readLine;
    String ascii;
    int readCount;

    public InventoryNreadResponse(String readLine, String ascii, int readCount) {
        this.readLine = readLine;
        this.ascii = ascii;
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


    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }
}
