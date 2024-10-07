package device.apps.rfidsamplev2.sample.wired;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.RemoteException;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.sdk.Control;
import device.sdk.rfid.RFIDController;
import device.sdk.rfid.consts.DeviceConst;

public class WireViewModel extends ViewModel {

    private static final String ATTACH = "1";
    private static final String DETACH = "0";

    private static final String ACTION_DEVICE_CHANGED = "pm.ex.gpio.changed";
    private static final String EXTRA_CONNECT_STATE = "scc_det";

    private final ExecutorService _executorService = Executors.newSingleThreadExecutor();
    private final DetectReceiver _receiver = new DetectReceiver();
    private final Control _control = Control.getInstance();
    private final RFIDController _controllre = RFIDController.getInstance();

    /**
     * Detect the connection between RF88 and the device, and if connected, attempt to connect.
     * Note that our PM90 model does not support this feature
     *
     * @param context Application context
     */
    public void launch(Context context) {
        ContextCompat.registerReceiver(context, _receiver, getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        try {
            if (Build.MODEL.contains("PM90"))
                return;

            final String detected = _control.getExpansionAccDetGpio();
            if (detected == null)
                return;

            if (detected.equals(DeviceConst.ENABLE))
                _controllre.connect();

        } catch (RemoteException exception) {
            //
        }
    }

    /**
     * Unregister the registered BroadcastReceiver
     *
     * @param context Application context
     */
    public void dispose(Context context) {
        context.unregisterReceiver(_receiver);
    }

    /**
     * Attempt to connect to the currently attached device
     */
    public void connect() {
        _executorService.execute(_controllre::connect);
    }

    /**
     * Disconnect from the currently attached device
     */
    public void disconnect() {
        _executorService.execute(_controllre::disconnect);
    }

    /**
     * Return the Intent filter to be received by the receiver
     *
     * @return Intent filter
     */
    private IntentFilter getIntentFilter() {
        final IntentFilter result = new IntentFilter();
        result.addAction(ACTION_DEVICE_CHANGED);
        return result;
    }

    class DetectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null || intent.getAction().equals(ACTION_DEVICE_CHANGED)) {
                final String detected = intent.getStringExtra(EXTRA_CONNECT_STATE);
                if (Objects.equals(detected, DETACH))
                    _controllre.disconnect();

                if (Objects.equals(detected, ATTACH))
                    _controllre.connect();
            }
        }
    }
}
