package device.apps.rfidsamplev2;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.dispatcher.ConnectionDispatcher;

public class RFIDSampleV2 extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ConnectionDispatcher.initialize(this);
    }
}
