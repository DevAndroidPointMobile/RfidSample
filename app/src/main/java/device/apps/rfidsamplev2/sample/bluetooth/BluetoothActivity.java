package device.apps.rfidsamplev2.sample.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.sample.bluetooth.callback.OnDeviceClickListener;
import device.apps.rfidsamplev2.sample.bluetooth.ui.DevicesAdapter;
import device.apps.rfidsamplev2.databinding.ActivityBluetoothBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Discover nearby RF88 devices over Bluetooth and let the user pick one to connect.
 *
 * <p>The screen handles three Bluetooth-availability states:
 * <ul>
 *   <li>Hardware missing — the action bar is hidden and the hero card explains the situation.</li>
 *   <li>Hardware present but turned off — a "Turn on Bluetooth" button asks the system to enable it.</li>
 *   <li>Hardware present and on — discovery, connect, and disconnect are all available.</li>
 * </ul>
 *
 * <p>UI state flows in one direction: the {@link BluetoothViewModel} owns the discovery LiveData
 * and the {@link Rf88ConnectionManager} owns the connection LiveData; this Activity simply
 * observes both and pushes derived values into the data-bound layout.
 */
public class BluetoothActivity extends AppCompatActivity implements OnDeviceClickListener {

    /** Request code used when prompting the user to grant the Bluetooth permissions. */
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;

    private DevicesAdapter adapter;
    private ActivityBluetoothBinding binding;
    private Rf88ConnectionManager connectionManager;
    private BluetoothViewModel viewModel;

    /** Permissions required by this screen, populated based on the OS version. */
    private String[] requiredPermissions;

    /**
     * The action the user originally requested when permissions had to be prompted.
     * Replayed once permissions are granted so the user does not need to tap twice.
     */
    private Runnable pendingAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBluetoothPermissions();
        initializationViewModel();
        initializationContentView();
        observeData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.dispose(this);
        connectionManager.connectState.removeObservers(this);
    }

    /**
     * Replay the pending action when the user grants the Bluetooth permissions, so a single
     * tap completes the request that triggered the prompt.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != BLUETOOTH_PERMISSION_REQUEST_CODE) return;
        final Runnable action = pendingAction;
        pendingAction = null;
        if (action != null && hasPermissions()) action.run();
    }

    /**
     * Forward a tap on a device row to the view model, which initiates the SDK connect call.
     */
    @Override
    public void onDeviceClicked(BluetoothDevice item) {
        viewModel.connect(item);
    }

    /**
     * Disconnect from the currently connected RF88 device. Bound from the layout.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void disconnect(View view) {
        viewModel.disconnect();
    }

    /**
     * Toggle Bluetooth discovery on or off. Bound from the layout for both the
     * "Start Discovery" and "Stop Discovery" buttons.
     *
     * <p>If the Bluetooth permissions have not been granted yet, the request is replayed
     * automatically once the user accepts the system prompt.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void onDiscovery(View view) {
        if (!hasPermissions()) {
            pendingAction = this::toggleDiscovery;
            requestRequiredPermissions();
            return;
        }
        toggleDiscovery();
    }

    /**
     * Ask the system to turn Bluetooth on. The result arrives via the
     * {@code BluetoothAdapter.ACTION_STATE_CHANGED} broadcast handled by the view model,
     * which then updates {@code bluetoothEnabledState} and the visible UI.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void enableBluetooth(View view) {
        if (!hasPermissions()) {
            pendingAction = this::launchEnableBluetoothIntent;
            requestRequiredPermissions();
            return;
        }
        launchEnableBluetoothIntent();
    }

    /**
     * Resolve the application-scoped connection manager and create the screen-scoped view model.
     * Called once during {@link #onCreate}.
     */
    private void initializationViewModel() {
        connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        viewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        viewModel.launch(this);
    }

    /**
     * Inflate the data-bound layout, attach the device list adapter, and wire up navigation.
     * Called once during {@link #onCreate}.
     */
    private void initializationContentView() {
        adapter = new DevicesAdapter(new ArrayList<>(), this);
        binding = ActivityBluetoothBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.recyclerView.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Build the list of runtime permissions required for the current OS version.
     *
     * <p>Android 12 (API 31) introduced dedicated Bluetooth permissions; older versions
     * piggy-back on the location permission, since pre-Android-12 device discovery can be
     * used to infer the device's location.
     */
    private void setBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };
        } else {
            requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }
    }

    /**
     * Subscribe to all UI state sources and feed them into the data-binding layout.
     *
     * <p>{@link #refreshStatusLabels()} is called from both the Bluetooth-enabled observer and
     * the connection-state observer because the hero card text depends on both signals.
     */
    private void observeData() {
        binding.setIsBluetoothSupported(viewModel.isBluetoothSupported());
        viewModel.bluetoothEnabledState.observe(this, enabled -> {
            binding.setIsBluetoothEnabled(enabled);
            refreshStatusLabels();
        });
        viewModel.devicesState.observe(this, devices -> {
            adapter.updateItems(devices);
            binding.setDeviceCount(devices.size());
        });
        viewModel.discoveryState.observe(this, isDiscovery -> binding.setIsDiscovery(isDiscovery));
        connectionManager.connectState.observe(this, state -> {
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
            refreshStatusLabels();
        });
    }

    /**
     * Update the hero card title/subtitle. Bluetooth-level problems (unsupported, off) take
     * priority over the SDK connection state because there is no point reporting "Disconnected"
     * when the radio is not even on. When Bluetooth is healthy, fall back to the centralised
     * labels exposed by {@link Rf88ConnectionManager}.
     */
    private void refreshStatusLabels() {
        if (!viewModel.isBluetoothSupported()) {
            binding.setStatusTitle("Bluetooth not supported");
            binding.setStatusSubtitle("This device cannot use Bluetooth");
            return;
        }
        if (!Boolean.TRUE.equals(viewModel.bluetoothEnabledState.getValue())) {
            binding.setStatusTitle("Bluetooth is off");
            binding.setStatusSubtitle("Turn on Bluetooth to find devices");
            return;
        }
        binding.setStatusTitle(connectionManager.statusTitle.getValue());
        binding.setStatusSubtitle(connectionManager.statusSubtitle.getValue());
    }

    /**
     * Start a fresh discovery, or stop the running one. The discovery toggle button binds to
     * {@link #onDiscovery(View)}, which calls this method after the permission check passes.
     */
    private void toggleDiscovery() {
        if (Boolean.TRUE.equals(viewModel.discoveryState.getValue())) {
            viewModel.stopDiscovery();
        } else {
            viewModel.startDiscovery();
        }
    }

    /**
     * Send the system intent that prompts the user to enable Bluetooth. The result is observed
     * indirectly through the {@code BluetoothAdapter.ACTION_STATE_CHANGED} broadcast.
     */
    private void launchEnableBluetoothIntent() {
        startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    /**
     * Pure check — returns whether every required permission has already been granted.
     * Has no side effects; pair with {@link #requestRequiredPermissions()} when missing.
     */
    private boolean hasPermissions() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Show the system permission prompt for everything in {@link #requiredPermissions}.
     * The grant result lands in {@link #onRequestPermissionsResult}.
     */
    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
    }
}
