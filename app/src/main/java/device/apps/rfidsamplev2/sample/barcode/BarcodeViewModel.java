package device.apps.rfidsamplev2.sample.barcode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import device.sdk.ScanManager;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;

/**
 * Bridges the RF88 <b>Scanner trigger key</b> to the device's built-in barcode scanner.
 *
 * <h3>About RF88 trigger keys</h3>
 * The RF88 reader has two physical trigger buttons (Top and Bottom). Each button can be
 * assigned at runtime to one of two functions through the {@code KEY_MAP} configuration
 * (encoded by {@code KeyMap}): <b>Inventory</b> (RFID tag scan) or <b>Scanner</b>
 * (barcode scan).
 *
 * <p>Importantly, the SDK splits the events by <b>function</b>, not by physical position:
 * <ul>
 *     <li>{@code onInventoryKeyPressed/Released} fires whenever the user presses
 *         <i>whichever</i> button is currently mapped to Inventory.</li>
 *     <li>{@code onScannerKeyPressed/Released} fires whenever the user presses
 *         <i>whichever</i> button is currently mapped to Scanner.</li>
 * </ul>
 * That means application code does not need to read {@code KEY_MAP} or distinguish
 * Top/Bottom — it just listens to the callback for the function it cares about. This
 * class implements the Scanner side only.
 *
 * <h3>What this class does</h3>
 * It listens for the Scanner trigger callbacks. When the user presses the Scanner
 * trigger, it tells the device's built-in scanner to start decoding; when the user
 * releases it, the scanner is told to stop. In short, this class is just a small
 * <b>bridge between two SDKs</b>.
 *
 * <h3>What this class does NOT do</h3>
 * It does <b>not</b> receive or display the decoded barcode string. The scan-result
 * pipeline (data callback, parsing, UI) belongs to the device-scanner SDK and is covered
 * by a separate sample (PointMobile / Emkit Scanner sample). This file demonstrates only
 * the "Scanner trigger → scanner start/stop" forwarding pattern.
 *
 * <h3>Why are two SDKs involved?</h3>
 * <ul>
 *     <li>{@link Rf88Manager} delivers <b>hardware-key events</b> from the RF88 reader.</li>
 *     <li>{@link ScanManager} controls the device's <b>built-in barcode scanner</b>.</li>
 * </ul>
 * When the Scanner trigger is pressed, an {@link OnHardwareKeyListener} callback fires;
 * from inside that callback we call {@link ScanManager#aDecodeSetTriggerOn(int)} to
 * toggle scanning.
 */
public class BarcodeViewModel extends ViewModel implements OnHardwareKeyListener {

    /** Argument for {@link ScanManager#aDecodeSetTriggerOn(int)} — start scanning (1). */
    private static final int SCAN_START = 1;
    /** Argument for {@link ScanManager#aDecodeSetTriggerOn(int)} — stop scanning (0). */
    private static final int SCAN_STOP = 0;

    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final ScanManager scanManager = ScanManager.getInstance();

    private final MutableLiveData<Boolean> mutableScanning = new MutableLiveData<>(false);

    /**
     * Whether the Scanner trigger key is currently being held down. The Activity observes
     * this signal to update its UI (icon highlight, status text) so the user gets visual
     * confirmation that the trigger pipeline is alive.
     */
    public final LiveData<Boolean> isScanning = mutableScanning;

    /**
     * Registers this ViewModel as the RF88 hardware-key listener.
     * <p>
     * Call this once from the Activity's {@code onCreate()}. From then on,
     * {@link #onScannerKeyPressed()} and {@link #onScannerKeyReleased()} will be invoked
     * automatically every time the user presses or releases the trigger.
     */
    public void launch() {
        rf88Manager.setOnHardwareKeyListener(this);
    }

    /** Invoked when the user presses the Scanner trigger key — start barcode decoding. */
    @Override
    public void onScannerKeyPressed() {
        mutableScanning.postValue(true);
        scanManager.aDecodeSetTriggerOn(SCAN_START);
    }

    /** Invoked when the user releases the Scanner trigger key — stop barcode decoding. */
    @Override
    public void onScannerKeyReleased() {
        mutableScanning.postValue(false);
        scanManager.aDecodeSetTriggerOn(SCAN_STOP);
    }

    /**
     * Detach this ViewModel from the RF88 hardware-key listener slot when the framework
     * finally clears the ViewModel (e.g. the host Activity finishes for good).
     *
     * <p>{@link Rf88Manager} is an app-scoped singleton, so the listener it stores would
     * otherwise outlive the ViewModel and pin it in memory.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        rf88Manager.setOnHardwareKeyListener(null);
    }
}
