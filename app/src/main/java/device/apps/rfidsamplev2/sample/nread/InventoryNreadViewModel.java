package device.apps.rfidsamplev2.sample.nread;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.constants.Bridge;
import ex.dev.sdk.rf88.domain.constants.Configuration;
import ex.dev.sdk.rf88.frameworks.listener.OnActionExecutingListener;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;

/**
 * Drives the Inventory &amp; Read sample screen. Same trigger / dedup model as
 * {@code InventoryViewModel}, but each scan cycle additionally pulls a small slice of
 * tag memory along with the EPC — useful when the application needs the TID (for a
 * unique, factory-locked identifier) or a USER bank field on every read.
 *
 * <p>The {@link #isScanning} indicator is driven exclusively by
 * {@link #onCurrentActionChanged(String)} so the UI stays in sync regardless of whether
 * the scan was started by the hardware trigger or the on-screen toggle button.
 */
public class InventoryNreadViewModel extends ViewModel implements OnHardwareKeyListener, OnInventoryResultListener, OnActionExecutingListener {

    private static final String TAG = "InventoryNreadViewModel";

    private final Rf88Manager manager = Rf88Manager.getInstance();

    /**
     * Serializes the {@code inventoryAndRead()} / {@code stop()} SDK calls off the main
     * thread. Rf88 SDK 3.1.0+ throws if the synchronous variants are invoked on the UI
     * thread and these two operations have no {@code *Async} counterpart, so we own the
     * dispatch ourselves. Single-threaded so start/stop ordering matches the user's taps.
     */
    private final ExecutorService sdkExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);
    public final LiveData<Integer> changedIndex = _changedIndex;

    private final MutableLiveData<Boolean> _isScanning = new MutableLiveData<>(false);
    public final LiveData<Boolean> isScanning = _isScanning;

    /**
     * Accumulated tag responses, deduplicated by EPC. Exposed directly so the Activity
     * can hand the same reference to the {@link device.apps.rfidsamplev2.sample.nread.ui.InventoryNreadAdapter}
     * — both observe mutations through the {@link #changedIndex} signal rather than
     * defensive copies. External code must not mutate this list; use
     * {@link #clearHistory()} to reset.
     */
    public List<InventoryNreadResponse> readHistory = new ArrayList<>();

    /**
     * Registers this ViewModel as the listener for hardware-key events, inventory
     * results, and current-operation transitions. Call once from the Activity's
     * {@code onCreate()}.
     *
     * <p>Also resets {@link #readHistory} so re-entering the screen starts from a
     * clean list, even if the ViewModel instance has survived a configuration change.
     */
    public void launch() {
        readHistory = new ArrayList<>();
        _changedIndex.setValue(-1);
        manager.setOnHardwareKeyListener(this);
        manager.setOnInventoryResultListener(this);
        manager.setOnActionExecutingListener(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sdkExecutor.shutdown();
    }

    /** Invoked when the user presses the Inventory trigger key — start the scan. */
    @Override
    public void onInventoryKeyPressed() {
        inventoryStart();
    }

    /** Invoked when the user releases the Inventory trigger key — stop the scan. */
    @Override
    public void onInventoryKeyReleased() {
        inventoryStop();
    }

    /**
     * Toggle inventory-and-read from a UI control. Mirrors the hardware trigger but
     * uses a press-to-start / press-to-stop semantic instead of press-and-hold.
     */
    public void toggleInventory() {
        if (Boolean.TRUE.equals(_isScanning.getValue())) {
            inventoryStop();
        } else {
            inventoryStart();
        }
    }

    /**
     * Stop any in-flight inventory-and-read. Safe to call when nothing is running —
     * the SDK treats it as a no-op. Intended for lifecycle-driven cleanup so the
     * reader does not keep radiating off-screen.
     */
    public void stopInventory() {
        inventoryStop();
    }

    /**
     * Drop every accumulated tag and notify the UI through the existing
     * {@link #changedIndex} observer so the adapter and unique-count badge reset
     * through a single code path.
     */
    public void clearHistory() {
        readHistory.clear();
        _changedIndex.postValue(-1);
    }

    /**
     * SDK callback fired once per tag inventoried. In addition to the EPC ({@code data}),
     * the callback delivers the bytes read from the memory bank requested in
     * {@link #inventoryStart()} as {@code readData}. Both are bundled into an
     * {@link InventoryNreadResponse} and forwarded to
     * {@link #inventoryProcess(InventoryNreadResponse)} for deduplication and counting.
     */
    @Override
    public void onInventoryAndReadDiscovered(@NonNull String data, @NonNull String ascii, @NonNull String readData) {
        Log.d(TAG, "onInventoryAndReadDiscovered: @data = " + data + "  @readData = " + readData);
        InventoryNreadResponse response = new InventoryNreadResponse(data + "\n" + readData, ascii, readData, 0);
        inventoryProcess(response);
    }

    /**
     * Begin continuous inventory-and-read. Each tag returned by the inventory cycle
     * is also read on the same air-protocol exchange, so the discovery callback
     * delivers both the EPC and the requested memory slice in one shot.
     *
     * <p>Arguments: <b>memory bank</b> ({@code "2"} = TID — see
     * {@link Configuration.MemoryBank}), <b>word offset</b> within that bank
     * ({@code "0"} = start), and <b>word length</b> to read ({@code "1"} = 1 word /
     * 16 bits / 2 bytes). Adjust these to read more bytes or a different bank.
     *
     * <p>The four-argument overload (commented sample below) additionally takes a
     * Class1-Gen2 Select mask so the read is limited to a specific tag population.
     */
    private void inventoryStart() {
        sdkExecutor.execute(() -> manager.inventoryAndRead(Configuration.MemoryBank.TID, "0", "1"));
        // e.g manager.inventoryAndRead(Configuration.MemoryBank.TID, "0", "1", "3400E280117000000210ACEAE0BE");
    }

    /** Issue the SDK command to stop the in-flight inventory-and-read scan. */
    private void inventoryStop() {
        sdkExecutor.execute(manager::stop);
    }

    /**
     * Source of truth for the scanning indicator. The SDK posts this whenever the
     * current operation transitions, so the UI stays in sync regardless of whether
     * the inventory-and-read scan was started by the hardware trigger or the
     * on-screen button.
     */
    @Override
    public void onCurrentActionChanged(@Nullable String s) {
        Log.e("ACTION", "onCurrentActionChanged: " + s);
        _isScanning.postValue(Objects.equals(s, Bridge.Actions.OPERATION_INVENTORY_AND_READ));
    }

    /**
     * Insert {@code result} into {@link #readHistory}, or — if a tag with the same EPC
     * is already there — bump its read count. Posts the changed index so the adapter
     * can animate just that row instead of redrawing the whole list.
     *
     * @param result inventory entry just received from the SDK
     */
    private void inventoryProcess(InventoryNreadResponse result) {
        boolean found = false;
        for (int i = 0; i < readHistory.size(); i++) {
            final InventoryNreadResponse existingResponse = readHistory.get(i);
            // Find the same EPC Data in the list and update it.
            if (existingResponse.getEpcData().equals(result.getEpcData())) {
                existingResponse.setReadCount(existingResponse.getReadCount() + 1);
                found = true;
                _changedIndex.postValue(i);
                break;
            }
        }

        if (!found) {
            result.setReadCount(1);
            readHistory.add(result);
            _changedIndex.postValue(readHistory.size() - 1);
        }
    }
}
