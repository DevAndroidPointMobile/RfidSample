package device.apps.rfidsamplev2.sample.barcode;

import androidx.lifecycle.ViewModel;

import device.sdk.ScanManager;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;

// TODO, Guide to PointMobile scan function.
// TODO, If you want to develop scanner control, refer to tScanner folder in Emkit SDK.
public class BarcodeViewModel extends ViewModel implements OnHardwareKeyListener {

    private Rf88Manager manager = Rf88Manager.getInstance();


    private static final int OPEN = 1;
    private static final int CLOSE = 0;

    @Override
    public void onScannerKeyPressed() {
        _scanManager.aDecodeSetTriggerOn(OPEN);
    }

    @Override
    public void onScannerKeyReleased() {
        _scanManager.aDecodeSetTriggerOn(CLOSE);
    }

    private final ScanManager _scanManager = ScanManager.getInstance();

    /**
     * Set up a callback to receive the trigger event
     */
    public void launch() {
        manager.setOnHardwareKeyListener(this);
    }
}
