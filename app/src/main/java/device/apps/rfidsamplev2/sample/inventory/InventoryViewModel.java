package device.apps.rfidsamplev2.sample.inventory;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.contract.Configuration;
import ex.dev.sdk.rf88.domain.contract.Contract;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;
import ex.dev.sdk.rf88.utils.RfidUtils;

public class InventoryViewModel extends ViewModel implements OnHardwareKeyListener, OnInventoryResultListener {

    private final Rf88Manager _controller = Rf88Manager.getInstance();
    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);

    public LiveData<Integer> changedIndex = _changedIndex;
    public List<InventoryResponse> readHistory = new ArrayList<>();

    // launch android beep
    // private ToneGenerator _toneGenerator;

    public void launch() {
        readHistory = new ArrayList<>();
        _changedIndex.setValue(-1);
        _controller.setOnHardwareKeyListener(this);
        _controller.setOnInventoryResultListener(this);
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
        // todo, launch android beep
        // _toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        _controller.inventory();
    }

    private void inventoryStop() {
        _controller.stop();
        // todo, launch android beep
//        if (_toneGenerator != null)
//            _toneGenerator.release();
    }

    /**
     * Execute the mandatory read command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
     */
    public void read(InventoryResponse response) {
        final String selectMask = response.getReadLine();   // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String result = _controller.read(Configuration.MemoryBank.RESERVED, "0", "2", selectMask);
        if (result.equals(Contract.ResultCodes.OTHER_ERROR)) {
            // TODO, fail
        } else {
            // TODO, success read packet
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
        final String result = _controller.write(Configuration.MemoryBank.EPC, "1", pc + writeData, selectMask);
        if (result.equals(Contract.ResultCodes.SUCCESS)) {
            // TODO, success
        } else {
            // TODO, fail
        }
    }

    /**
     * Execute the mandatory lock command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
     */
    public void lock(InventoryResponse response) {
        final String selectMask = response.getReadLine();    // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String result = _controller.lock("00300", "00200", selectMask);
        if (result.equals(Contract.ResultCodes.SUCCESS)) {
            // TODO, success
        } else {
            // TODO, fail
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
        final String result = _controller.kill(killPassword, selectMask);
        if (result.equals(Contract.ResultCodes.SUCCESS)) {
            // TODO, success
        } else {
            // TODO, fail
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
