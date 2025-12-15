package device.apps.rfidsamplev2.dispatcher;

import androidx.annotation.NonNull;

import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public interface AppConnectionListener {
    void onAppConnectionStateChanged(@NonNull DeviceConnectionState state);
}
