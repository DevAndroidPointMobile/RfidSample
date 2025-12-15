package device.apps.rfidsamplev2.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.BaseActivity;
import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.bluetooth.ui.BluetoothDeviceAdapter;
import device.apps.rfidsamplev2.utils.BluetoothPermissionHelper;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class BluetoothConnectActivity extends BaseActivity {

    private static final String TAG = "BtConnectActivity";
    private static final String TARGET_DEVICE_NAME = "RF88";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, BluetoothDevice> deviceMap = new HashMap<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDeviceAdapter adapter;

    private final Rf88Manager rfManager = Rf88Manager.getInstance();

    /**
     * BroadcastReceiver that listens for Bluetooth discovery results.
     */
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                handleDeviceFound(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);
        initBluetoothAdapter();
        initRecyclerView();
        initButtons();
        registerDiscoveryReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDiscovery();
        unregisterReceiver(discoveryReceiver);
        ioExecutor.shutdownNow();
    }

    @Override
    protected void onConnectionStateChanged(DeviceConnectionState state) {
        runOnUiThread(() -> updateConnectionStatus(state));
    }

    /**
     * Initializes the system BluetoothAdapter.
     */
    private void initBluetoothAdapter() {
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = manager.getAdapter();
    }

    /**
     * Initializes RecyclerView and its adapter.
     */
    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BluetoothDeviceAdapter(this::connectToDevice);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Initializes start/stop discovery buttons.
     */
    private void initButtons() {
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> startDiscovery());
        btnStop.setOnClickListener(v -> stopDiscovery());
    }

    /**
     * Registers BroadcastReceiver for Bluetooth discovery events.
     */
    private void registerDiscoveryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);
    }

    /**
     * Starts Bluetooth device discovery after permission and state checks.
     */
    private void startDiscovery() {
        if (!BluetoothPermissionHelper.hasPermission(this)) {
            BluetoothPermissionHelper.request(this);
            return;
        }

        if (!isBluetoothEnabled()) {
            return;
        }

        resetDiscoveryState();
        bluetoothAdapter.startDiscovery();
    }

    /**
     * Stops ongoing Bluetooth discovery if running.
     */
    private void stopDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Clears previous discovery results and stops any running discovery.
     */
    private void resetDiscoveryState() {
        stopDiscovery();
        deviceMap.clear();
        adapter.setItems(new ArrayList<>());
    }

    /**
     * Handles a discovered Bluetooth device from ACTION_FOUND intent.
     *
     * @param intent Broadcast intent containing BluetoothDevice information
     */
    private void handleDeviceFound(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return;

        String address = device.getAddress();
        if (address == null || deviceMap.containsKey(address)) {
            return;
        }

        String name = device.getName();
        Log.d(TAG, "Device found: " + name);

        if (!isTargetDevice(name)) {
            return;
        }

        deviceMap.put(address, device);
        adapter.setItems(new ArrayList<>(deviceMap.values()));
    }

    /**
     * Checks whether Bluetooth is enabled on the device.
     *
     * @return true if Bluetooth is enabled
     */
    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Checks whether the discovered device matches the target criteria.
     *
     * @param name Bluetooth device name
     * @return true if the device name matches target rules
     */
    private boolean isTargetDevice(String name) {
        return name != null && name.contains(TARGET_DEVICE_NAME);
    }

    /**
     * Connects to the selected Bluetooth device on a background thread.
     *
     * @param device Target BluetoothDevice
     */
    private void connectToDevice(BluetoothDevice device) {
        ioExecutor.execute(() -> rfManager.connect(device.getAddress()));
    }

    private void updateConnectionStatus(DeviceConnectionState state) {
        switch (state) {
            case CONNECTING:
                setStatus("Connecting...", R.color.status_orange);
                break;
            case CONNECTED:
                setStatus("Connected", R.color.status_green);
                break;
            case DISCONNECTED:
                setStatus("Disconnected", R.color.status_gray);
                break;
            case DISCONNECTING:
                setStatus("Disconnecting...", R.color.status_orange);
                break;
            case FAILURE:
                setStatus("Connection Failed", R.color.status_red);
                break;
            case SLEEP:
                setStatus("Sleep Mode", R.color.status_red);
                break;
        }
    }

    private void setStatus(String text, int colorRes) {
        TextView statusText = findViewById(R.id.statusText);
        View statusIndicator = findViewById(R.id.statusIndicator);
        statusText.setText(text);
        statusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
    }
}