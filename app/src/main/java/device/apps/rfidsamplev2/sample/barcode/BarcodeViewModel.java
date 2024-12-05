package device.apps.rfidsamplev2.sample.barcode;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import device.sdk.ScanManager;
import device.sdk.rfid.RFIDController;
import device.sdk.rfid.data.enums.value.TriggerEvent;
import device.sdk.rfid.data.listener.OnTriggerEventChangedListener;

// TODO, Guide to PointMobile scan function.
// TODO, If you want to develop scanner control, refer to tScanner folder in Emkit SDK.
public class BarcodeViewModel extends ViewModel implements OnTriggerEventChangedListener {

    private static final int OPEN = 1;
    private static final int CLOSE = 0;

    private final RFIDController _controller = RFIDController.getInstance();
    private final ScanManager _scanManager = ScanManager.getInstance();

    @Override
    public void onTriggerEventChanged(@NonNull TriggerEvent triggerEvent) {
        switch (triggerEvent) {
            case SCANNER_PRESS:
                _scanManager.aDecodeSetTriggerOn(OPEN);
                break;
            case SCANNER_RELEASE:
                _scanManager.aDecodeSetTriggerOn(CLOSE);
                break;
        }
    }

    /**
     * Set up a callback to receive the trigger event
     */
    public void launch() {
        _controller.setOnTriggerEventChangedListener(this);
    }
}
