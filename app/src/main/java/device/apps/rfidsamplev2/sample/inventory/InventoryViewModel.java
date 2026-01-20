package device.apps.rfidsamplev2.sample.inventory;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.constants.Configuration;
import ex.dev.sdk.rf88.domain.constants.Constants;
import ex.dev.sdk.rf88.domain.enums.LockActionMask;
import ex.dev.sdk.rf88.domain.enums.LockMemoryMask;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;
import ex.dev.sdk.rf88.utils.RfidUtils;

public class InventoryViewModel extends ViewModel implements OnHardwareKeyListener, OnInventoryResultListener {

    private static final String TAG = "InventoryViewModel";

    private final Rf88Manager controller = Rf88Manager.getInstance();

    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);
    public LiveData<Integer> changedIndex = _changedIndex;

    public List<InventoryResponse> readHistory = new ArrayList<>();

    public void launch() {
        readHistory = new ArrayList<>();
        _changedIndex.setValue(-1);
        controller.setOnHardwareKeyListener(this);
        controller.setOnInventoryResultListener(this);
    }

    @Override
    public void onInventoryKeyPressed() {
        inventoryStart();
    }

    @Override
    public void onInventoryKeyReleased() {
        inventoryStop();
    }

    @Override
    public void onInventoryDiscovered(@NonNull String data, @NonNull String ascii, @NonNull String rssi, @NonNull String frequency, @NonNull String checksum) {
        InventoryResponse response = new InventoryResponse(data, ascii, rssi, frequency, checksum, 0);
        inventoryProcess(response);
    }

    private void inventoryStart() {
        controller.inventory();
    }

    private void inventoryStop() {
        controller.stop();
    }

    /**
     * Execute the mandatory read command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
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
     * Execute the mandatory write command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
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
     * Execute the mandatory lock command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
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
     * Execute the mandatory kill command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
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
     * Check the provided InventoryResult class and update the count of times it has been read
     *
     * @param result receive model class from SDK
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
}
