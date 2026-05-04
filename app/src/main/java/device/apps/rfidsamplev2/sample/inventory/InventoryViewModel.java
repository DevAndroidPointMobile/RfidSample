package device.apps.rfidsamplev2.sample.inventory;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.constants.Bridge;
import ex.dev.sdk.rf88.domain.constants.Configuration;
import ex.dev.sdk.rf88.domain.constants.Constants;
import ex.dev.sdk.rf88.domain.enums.LockActionMask;
import ex.dev.sdk.rf88.domain.enums.LockMemoryMask;
import ex.dev.sdk.rf88.frameworks.listener.OnActionExecutingListener;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;
import ex.dev.sdk.rf88.utils.RfidUtils;

/**
 * Drives the Inventory sample screen. Handles the RF88 Inventory trigger key, receives
 * each tag discovered during a scan, deduplicates by EPC while bumping a per-tag read
 * count, and executes the four Class1-Gen2 "mandatory" commands (read / write / lock /
 * kill) the user launches from the long-press dialog.
 *
 * <p>The {@link #isScanning} indicator is driven exclusively by
 * {@link #onCurrentActionChanged(String)} so the UI stays in sync regardless of whether
 * inventory was started by the hardware trigger or by the on-screen toggle button.
 */
public class InventoryViewModel extends ViewModel implements OnHardwareKeyListener, OnInventoryResultListener, OnActionExecutingListener {

    private static final String TAG = "InventoryViewModel";

    private final Rf88Manager controller = Rf88Manager.getInstance();

    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);
    public final LiveData<Integer> changedIndex = _changedIndex;

    private final MutableLiveData<Boolean> _isScanning = new MutableLiveData<>(false);
    public final LiveData<Boolean> isScanning = _isScanning;

    /**
     * Accumulated tag responses, deduplicated by EPC. Exposed directly so the Activity
     * can hand the same reference to the {@link device.apps.rfidsamplev2.sample.inventory.ui.InventoryAdapter}
     * — both observe mutations through the {@link #changedIndex} signal rather than
     * defensive copies. External code must not mutate this list; use
     * {@link #clearHistory()} to reset.
     */
    public List<InventoryResponse> readHistory = new ArrayList<>();

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
        controller.setOnHardwareKeyListener(this);
        controller.setOnInventoryResultListener(this);
        controller.setOnActionExecutingListener(this);
    }

    /** Invoked when the user presses the Inventory trigger key — start the tag scan. */
    @Override
    public void onInventoryKeyPressed() {
        inventoryStart();
    }

    /** Invoked when the user releases the Inventory trigger key — stop the tag scan. */
    @Override
    public void onInventoryKeyReleased() {
        inventoryStop();
    }

    /**
     * Stop any in-flight inventory. Safe to call when nothing is running — the SDK
     * treats it as a no-op. Intended for lifecycle-driven cleanup (e.g. the Activity
     * leaving the foreground) so the reader does not keep radiating off-screen.
     */
    public void stopInventory() {
        inventoryStop();
    }

    /**
     * Drop every accumulated tag and notify the UI. Posting {@code -1} on
     * {@link #changedIndex} reuses the same observer that handles per-tag updates,
     * so the adapter performs a full refresh and the unique-count badge resets to 0
     * through a single code path.
     */
    public void clearHistory() {
        readHistory.clear();
        _changedIndex.postValue(-1);
    }

    /**
     * Toggle inventory from a UI control. Mirrors the hardware trigger but uses a
     * press-to-start / press-to-stop semantic instead of press-and-hold.
     */
    public void toggleInventory() {
        if (Boolean.TRUE.equals(_isScanning.getValue())) {
            inventoryStop();
        } else {
            inventoryStart();
        }
    }

    /**
     * SDK callback fired once per tag read. Wraps the raw fields in an
     * {@link InventoryResponse} and forwards it to
     * {@link #inventoryProcess(InventoryResponse)} for deduplication and counting.
     */
    @Override
    public void onInventoryDiscovered(@NonNull String data, @NonNull String ascii, @NonNull String rssi, @NonNull String frequency, @NonNull String checksum) {
        InventoryResponse response = new InventoryResponse(data, ascii, rssi, frequency, checksum, 0);
        inventoryProcess(response);
    }

    /** Issue the SDK command to begin continuous inventory. */
    private void inventoryStart() {
        controller.inventory();
    }

    /** Issue the SDK command to stop the in-flight inventory (or any other operation). */
    private void inventoryStop() {
        controller.stop();
    }

    /**
     * Reads 2 words (4 bytes) from offset 0 of the <b>Reserved</b> memory bank — i.e.
     * the kill password — of the single tag matching {@code response}'s EPC.
     *
     * <p>The tag is targeted via a Class1-Gen2 Select command built from the EPC bits
     * (see Class1-Gen2 §6.3.2.7). The result is logged; this sample does not surface
     * the read bytes back to the UI.
     *
     * @param response the inventory entry whose EPC identifies the target tag
     */
    public void read(InventoryResponse response) {
        final String selectMask = response.getReadLine();   // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String result = controller.read(Configuration.MemoryBank.RESERVED, "0", "2", selectMask);
        if (result.contains(Constants.ResultCodes.SUCCESS)) {
            Log.i(TAG, "read: success, @result = " + result);
        } else {
            Log.e(TAG, "read: fail, @result = " + result);
        }
    }

    /**
     * Overwrites the targeted tag's <b>EPC</b> bank with a fixed sample payload. The
     * PC (Protocol Control) word is recomputed for the new payload via
     * {@link RfidUtils#calculatePC} and prepended so the tag advertises the correct
     * EPC length on its next inventory.
     *
     * <p>The payload here is hardcoded for demo purposes — replace with user-supplied
     * data in real applications.
     *
     * @param response the inventory entry whose EPC identifies the target tag
     */
    public void write(InventoryResponse response) {
        final String selectMask = response.getReadLine();   // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String writeData = "11112222333344445555";
        final String pc = RfidUtils.calculatePC(writeData, true, false, false, "00");
        final String result = controller.write(Configuration.MemoryBank.EPC, "1", pc + writeData, selectMask);
        if (result.contains(Constants.ResultCodes.SUCCESS)) {
            Log.i(TAG, "write: success, @result = " + result);
        } else {
            Log.e(TAG, "write: fail, @result = " + result);
        }
    }

    /**
     * Permanently write-locks the targeted tag's <b>EPC</b> memory bank
     * ({@link LockActionMask#LOCK}). After this call the EPC bank can no longer be
     * modified without the access password — and is unrecoverable if the tag has none.
     *
     * @param response the inventory entry whose EPC identifies the target tag
     */
    public void lock(InventoryResponse response) {
        final String selectMask = response.getReadLine();    // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String result = controller.lock(LockMemoryMask.EPC, LockActionMask.LOCK, selectMask);
        if (result.contains(Constants.ResultCodes.SUCCESS)) {
            Log.i(TAG, "lock: success, @result = " + result);
        } else {
            Log.e(TAG, "lock: fail, @result = " + result);
        }
    }

    /**
     * Permanently disables the targeted tag using the Class1-Gen2 Kill command. The
     * tag must have a non-zero kill password programmed; this sample uses the demo
     * password {@code "11110000"}, so it will only succeed against tags written with
     * the same value.
     *
     * <p><b>Destructive and irreversible</b> — a killed tag never responds again.
     *
     * @param response the inventory entry whose EPC identifies the target tag
     */
    public void kill(InventoryResponse response) {
        final String selectMask = response.getReadLine();    // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String killPassword = "11110000";              // 2 word, 00h ~ 1Fh
        final String result = controller.kill(killPassword, selectMask);
        if (result.contains(Constants.ResultCodes.SUCCESS)) {
            Log.i(TAG, "kill: success, @result = " + result);
        } else {
            Log.e(TAG, "kill: fail, @result = " + result);
        }
    }

    /**
     * Insert {@code result} into {@link #readHistory}, or — if a tag with the same EPC
     * is already there — bump its read count. Posts the changed index so the adapter
     * can animate just that row instead of redrawing the whole list.
     *
     * @param result inventory entry just received from the SDK
     */
    private void inventoryProcess(InventoryResponse result) {
        boolean found = false;
        for (int i = 0; i < readHistory.size(); i++) {
            final InventoryResponse existingResponse = readHistory.get(i);
            // Find the same EPC Data in the list and update it.
            if (existingResponse.getReadLine().equals(result.getReadLine())) {
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

    /**
     * Source of truth for the scanning indicator. The SDK posts this whenever the
     * current operation transitions, so the UI stays in sync regardless of whether
     * inventory was started by the hardware trigger or by the on-screen button.
     */
    @Override
    public void onCurrentActionChanged(@Nullable String s) {
        _isScanning.postValue(Objects.equals(s, Bridge.Actions.OPERATION_INVENTORY));
    }
}
