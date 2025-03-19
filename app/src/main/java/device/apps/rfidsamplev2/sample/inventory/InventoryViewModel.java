package device.apps.rfidsamplev2.sample.inventory;

import android.media.AudioManager;
import android.media.ToneGenerator;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import device.sdk.rfid.RFIDController;
import device.sdk.rfid.RFIDUtils;
import device.sdk.rfid.consts.RFIDConst;
import device.sdk.rfid.data.enums.value.TriggerEvent;
import device.sdk.rfid.data.listener.OnInventoryResultChangedListener;
import device.sdk.rfid.data.listener.OnTriggerEventChangedListener;
import device.sdk.rfid.model.InventoryResponse;
import device.sdk.rfid.model.OperationData;

public class InventoryViewModel extends ViewModel implements OnTriggerEventChangedListener, OnInventoryResultChangedListener {

    private final RFIDController _controller = RFIDController.getInstance();
    private final MutableLiveData<Integer> _changedIndex = new MutableLiveData<>(-1);

    public LiveData<Integer> changedIndex = _changedIndex;
    public List<InventoryResponse> readHistory = new ArrayList<>();

    // launch android beep
    // private ToneGenerator _toneGenerator;

    public void launch() {
        readHistory = new ArrayList<>();
        _changedIndex.setValue(-1);
        _controller.setOnInventoryResultChangedListener(this);
        _controller.setOnTriggerEventChangedListener(this);
    }

    @Override
    public void onTriggerEventChanged(@NonNull TriggerEvent triggerEvent) {
        switch (triggerEvent) {
            case RFID_PRESS:
                inventoryStart();
                break;
            case RFID_RELEASE:
                inventoryStop();
                break;
            default:
                break;
        }
    }

    @Override
    public void onInventoryResultChanged(@NonNull InventoryResponse inventoryResponse) {
        // launch android beep.
//        if (_toneGenerator != null)
//            _toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        inventoryProcess(inventoryResponse);
    }

    private void inventoryStart() {
        // launch android beep
        // _toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        _controller.inventory();
    }

    private void inventoryStop() {
        _controller.stop();
        // launch android beep
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
        final OperationData result = _controller.readTag(RFIDConst.RESERVED, "0", "2", selectMask);
    }

    /**
     * Execute the mandatory write command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
     */
    public void write(InventoryResponse response) {
        final String selectMask = response.getReadLine();   // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String writeData = "11112222333344445555";
        final String pc = RFIDUtils.calculatePC(writeData, true, false, false, "00");
        final OperationData result = _controller.writeTag(RFIDConst.EPC, "1", pc + writeData, selectMask);
    }

    /**
     * Execute the mandatory lock command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
     */
    public void lock(InventoryResponse response) {
        final String selectMask = response.getReadLine();    // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final OperationData result = _controller.lockTag("00300", "00200", selectMask);
    }

    /**
     * Execute the mandatory kill command. Refer to the accompanying Java-doc for detailed usage instructions
     *
     * @param response target inventory response
     */
    public void kill(InventoryResponse response) {
        final String selectMask = response.getReadLine();    // Class1-gen2, 6.3.2.7 Selecting Tag populations.
        final String killPassword = "11110000";              // 2 word, 00h ~ 1Fh
        final OperationData result = _controller.killTag(killPassword, selectMask);
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
