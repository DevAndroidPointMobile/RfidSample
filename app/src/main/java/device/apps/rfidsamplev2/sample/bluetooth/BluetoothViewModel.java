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

import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * State holder for {@link BluetoothActivity}.
 *
 * <p>Wraps the Android Bluetooth APIs and the RF88 SDK so the Activity only has to observe
 * LiveData and call high-level methods. A {@link BroadcastReceiver} translates system Bluetooth
 * broadcasts into LiveData updates: discovery start/finish, found devices, ACL disconnects,
 * and adapter on/off.
 *
 * <p>{@code ACTION_ACL_DISCONNECTED} is handled here (not in {@link Rf88ConnectionManager})
 * because detaching the sled also fires the same broadcast as a side effect; treating it
 * app-scope would double-issue {@code disconnect()} on every detach burst and leave the SDK
 * stuck in {@code CONNECTING}. While this screen is open the SDK is in BT mode anyway, so
 * the ACL drop is meaningful here and only here.
 *
 * <p>SDK calls ({@code connect}, {@code disconnect}) are dispatched on a single-thread executor
 * so they never block the main thread.
 */
public class BluetoothViewModel extends ViewModel {

    /** Background dispatcher for blocking SDK calls. */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /** Singleton entry point of the RF88 SDK. */
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

    /** Receives the Bluetooth system broadcasts this screen cares about. */
    private final BroadcastReceiver receiver = new DiscoveryReceiver();

    /** System Bluetooth manager; resolved once in {@link #launch(Context)}. */
    private BluetoothManager bluetoothManager;

    /** {@code true} if the host device has Bluetooth hardware at all. */
    private boolean bluetoothSupported = false;

    /** {@code true} while a discovery scan is running; mirrors {@code ACTION_DISCOVERY_*}. */
    private final MutableLiveData<Boolean> _discoveryState = new MutableLiveData<>(false);
    public final LiveData<Boolean> discoveryState = _discoveryState;

    /** RF88 devices that have been seen during the current discovery session. */
    private final MutableLiveData<List<BluetoothDevice>> _devicesState = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<BluetoothDevice>> devicesState = _devicesState;

    /** {@code true} when the Bluetooth adapter is on; mirrors {@code ACTION_STATE_CHANGED}. */
    private final MutableLiveData<Boolean> _bluetoothEnabledState = new MutableLiveData<>(false);
    public final LiveData<Boolean> bluetoothEnabledState = _bluetoothEnabledState;

    /**
     * Look up the system {@link BluetoothManager}, register the broadcast receiver, and
     * publish the initial adapter on/off state. Must be called once before any other method.
     *
     * @param context Activity context — used to register the receiver and resolve the system service
     */
    public void launch(Context context) {
        ContextCompat.registerReceiver(context, receiver, getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        bluetoothSupported = adapter != null;
        _bluetoothEnabledState.setValue(bluetoothSupported && adapter.isEnabled());
    }

    /**
     * Whether the host device has Bluetooth hardware. Constant for the lifetime of the app.
     */
    public boolean isBluetoothSupported() {
        return bluetoothSupported;
    }

    /**
     * Unregister the broadcast receiver. Call from the Activity's {@code onDestroy}.
     *
     * @param context the same context that was passed to {@link #launch(Context)}
     */
    public void dispose(Context context) {
        context.unregisterReceiver(receiver);
    }

    /**
     * Clear the previous result list and start a fresh Bluetooth discovery scan.
     * No-op when Bluetooth is unavailable or off (caller checks).
     */
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        _devicesState.setValue(new ArrayList<>());
        bluetoothManager.getAdapter().startDiscovery();
    }

    /**
     * Cancel an in-flight discovery scan.
     */
    @SuppressLint("MissingPermission")
    public void stopDiscovery() {
        bluetoothManager.getAdapter().cancelDiscovery();
    }

    /**
     * Connect the RF88 SDK to the given Bluetooth device. The SDK call runs on a background
     * thread; the Activity learns about the result by observing
     * {@link Rf88ConnectionManager#connectState}.
     *
     * @param device device picked by the user from the discovery list
     */
    public void connect(BluetoothDevice device) {
        executorService.execute(() -> rf88Manager.connect(device.getAddress()));
    }

    /**
     * Disconnect the RF88 SDK from the currently connected device.
     */
    public void disconnect() {
        executorService.execute(rf88Manager::disconnect);
    }

    /**
     * Shut down the background executor when the ViewModel is finally cleared by the framework
     * (e.g. when the host Activity finishes for good).
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    /**
     * Handle an {@code ACTION_FOUND} broadcast. Devices that are not RF88 readers, anonymous
     * devices, or already-known devices are filtered out so the list shows only useful targets.
     *
     * @param intent the broadcast intent containing the {@link BluetoothDevice} extra
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
        // Defensive copy — observers receive an immutable snapshot, so subsequent
        // mutations to {@code devices} on later broadcasts cannot race with consumers.
        _devicesState.setValue(new ArrayList<>(devices));
    }

    /**
     * Build the {@link IntentFilter} for every Bluetooth broadcast this screen consumes.
     */
    private IntentFilter getIntentFilter() {
        final IntentFilter result = new IntentFilter();
        result.addAction(BluetoothDevice.ACTION_FOUND);
        result.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        result.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        result.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        result.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return result;
    }

    /**
     * Routes Bluetooth system broadcasts into {@link MutableLiveData} updates and SDK calls.
     */
    private class DiscoveryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null)
                return;

            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    // A nearby device responded to the discovery scan.
                    foundDevice(intent);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    // The remote dropped the link; tell the SDK so it cleans its session.
                    executorService.execute(rf88Manager::disconnect);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    _discoveryState.setValue(true);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    _discoveryState.setValue(false);
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    // The user (or another app) toggled the Bluetooth adapter.
                    final int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    _bluetoothEnabledState.setValue(newState == BluetoothAdapter.STATE_ON);
                    break;
                default:
                    break;
            }
        }
    }
}
