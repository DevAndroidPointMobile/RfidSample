package device.apps.rfidsamplev2.sample.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ex.dev.sdk.rf88.Rf88Manager;


public class BluetoothViewModel extends ViewModel {

    private final ExecutorService _executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager _controller = Rf88Manager.getInstance();
    private final MutableLiveData<Boolean> _discoveryState = new MutableLiveData<>(false);
    private final MutableLiveData<List<BluetoothDevice>> _devicesState = new MutableLiveData<>(new ArrayList<>());
    private final BroadcastReceiver _receiver = new DiscoveryReceiver();

    private BluetoothManager _manager;

    public LiveData<Boolean> discoveryState = _discoveryState;
    public LiveData<List<BluetoothDevice>> devicesState = _devicesState;

    /**
     * Create a BluetoothManager object for Bluetooth-related operations and register a BroadcastReceiver to receive related actions
     *
     * @param context Application context
     */
    public void launch(Context context) {
        ContextCompat.registerReceiver(context, _receiver, getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        _manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    /**
     * Unregister the registered BroadcastReceiver
     *
     * @param context : application context
     */
    public void dispose(Context context) {
        context.unregisterReceiver(_receiver);
    }

    /**
     * Start the Bluetooth discovery process.
     */
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        _devicesState.setValue(new ArrayList<>());
        _manager.getAdapter().startDiscovery();
    }

    /**
     * Stop the ongoing Bluetooth discovery process.
     */
    @SuppressLint("MissingPermission")
    public void stopDiscovery() {
        _manager.getAdapter().cancelDiscovery();
    }

    /**
     * Attempt to connect to the Bluetooth device passed as an argument.
     *
     * @param device Target blueooth device
     */
    public void connect(BluetoothDevice device) {
        _executorService.execute(() -> _controller.connect(device.getAddress()));
    }

    /**
     * Disconnect from the Bluetooth device
     */
    public void disconnect() {
        _executorService.execute(_controller::disconnect);
    }

    /**
     * Find the Bluetooth device that matches the conditions from the Intent received through ACTION_FOUND and update the LiveData
     *
     * @param intent Android intent
     */
    @SuppressLint("MissingPermission")
    private void foundDevice(Intent intent) {
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        final String name = device.getName();
        final List<BluetoothDevice> devices = _devicesState.getValue();

        if (name == null || name.isEmpty())
            return;

        if (!name.contains("RF88"))
            return;

        if (devices.contains(device))
            return;

        devices.add(device);
        _devicesState.setValue(devices);
    }

    /**
     * Return the Intent filter to be received by the receiver
     *
     * @return Intent filter
     */
    private IntentFilter getIntentFilter() {
        final IntentFilter result = new IntentFilter();
        result.addAction(BluetoothDevice.ACTION_FOUND);
        result.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        result.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        result.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return result;
    }

    private class DiscoveryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null)
                return;

            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    foundDevice(intent);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    _controller.disconnect();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    _discoveryState.setValue(true);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    _discoveryState.setValue(false);
                    break;
                default:
                    break;

            }
        }
    }
}
