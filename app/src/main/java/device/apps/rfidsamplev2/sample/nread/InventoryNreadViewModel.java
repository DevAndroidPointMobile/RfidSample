package device.apps.rfidsamplev2.sample.nread;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;

public class InventoryNreadViewModel extends ViewModel implements OnHardwareKeyListener, OnInventoryResultListener {

    private final Rf88Manager manager = Rf88Manager.getInstance();
    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);
    public LiveData<Integer> changedIndex = _changedIndex;

    public List<InventoryNreadResponse> readHistory = new ArrayList<>();

    public void launch() {
        readHistory = new ArrayList<>();
        _changedIndex.setValue(-1);
        manager.setOnHardwareKeyListener(this);
        manager.setOnInventoryResultListener(this);
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
    public void onInventoryAndReadDiscovered(@NonNull String data, @NonNull String ascii, @NonNull String readData) {
        Log.d("TAG", "onInventoryAndReadDiscovered: @data = " + data + "  @readData = " + readData);
        InventoryNreadResponse response = new InventoryNreadResponse(data + "\n" + readData, ascii, readData, 0);
        inventoryProcess(response);
    }

    private void inventoryStart() {
        manager.inventoryAndRead("2", "0", "1");
        // e.g manager.inventoryAndRead("2", "0", "1", "3400E280117000000210ACEAE0BE");
    }

    private void inventoryStop() {
        manager.stop();
    }

    /**
     * Check the provided InventoryResult class and update the count of times it has been read
     *
     * @param result receive model class from SDK
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
