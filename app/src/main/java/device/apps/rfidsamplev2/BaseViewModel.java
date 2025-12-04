package device.apps.rfidsamplev2;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;
import ex.dev.sdk.rf88.frameworks.listener.OnConnectionStateChangedListener;

public class BaseViewModel extends ViewModel implements OnConnectionStateChangedListener {

    private final MutableLiveData<DeviceConnectionState> _connectState = new MutableLiveData<>(DeviceConnectionState.DISCONNECTED);
    private final Rf88Manager _controller = Rf88Manager.getInstance();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<DeviceConnectionState> connectState = _connectState;

    /**
     * Set up a listener to receive connection status with RF88 through a callback
     */
    public void launch() {
        _controller.setOnConnectionStateChangedListener(this);
    }

    @Override
    public void onConnectionStateChanged(@NonNull DeviceConnectionState deviceConnectionState) {
        executorService.execute(() -> _connectState.postValue(deviceConnectionState));
    }
}
