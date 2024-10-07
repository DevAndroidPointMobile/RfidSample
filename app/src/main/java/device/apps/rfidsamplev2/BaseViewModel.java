package device.apps.rfidsamplev2;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.sdk.rfid.RFIDController;
import device.sdk.rfid.data.enums.state.ConnectState;
import device.sdk.rfid.data.listener.OnStateChangedListener;

public class BaseViewModel extends ViewModel implements OnStateChangedListener {

    private final MutableLiveData<ConnectState> _connectState = new MutableLiveData<>(ConnectState.DISCONNECTED);
    private final RFIDController _controller = RFIDController.getInstance();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<ConnectState> connectState = _connectState;

    @Override
    public void onConnectState(@NonNull ConnectState connectState) {
        executorService.execute(() -> _connectState.postValue(connectState));
    }

    /**
     * Set up a listener to receive connection status with RF88 through a callback
     */
    public void launch() {
        _controller.setOnStateChangedListener(this);
    }
}
