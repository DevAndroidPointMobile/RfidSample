package device.apps.rfidsamplev2.sample.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
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

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.sample.bluetooth.callback.OnDeviceClickListener;
import device.apps.rfidsamplev2.sample.bluetooth.ui.DevicesAdapter;
import device.apps.rfidsamplev2.databinding.ActivityBluetoothBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class BluetoothActivity extends AppCompatActivity implements OnDeviceClickListener {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;

    private DevicesAdapter adapter;
    private ActivityBluetoothBinding binding;
    private BaseViewModel baseViewModel;
    private BluetoothViewModel viewModel;
    private String[] permissions;

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
        baseViewModel.connectState.removeObservers(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (arePermissionsGranted())
            startDiscovery();
    }

    @Override
    public void onDeviceClicked(BluetoothDevice item) {
        viewModel.connect(item);
    }

    /**
     * Disconnect from the connected Bluetooth device
     *
     * @param view Button view
     */
    public void disconnect(View view) {
        viewModel.disconnect();
    }

    /**
     * Execute Bluetooth device discovery
     *
     * @param view Button view
     */
    public void onDiscovery(View view) {
        if (arePermissionsGranted())
            startDiscovery();
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        viewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        viewModel.launch(BluetoothActivity.this);
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        adapter = new DevicesAdapter(new ArrayList<>(), BluetoothActivity.this);
        binding = ActivityBluetoothBinding.inflate(getLayoutInflater());
        binding.setActivity(BluetoothActivity.this);
        binding.recyclerView.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Create the necessary permissions for Bluetooth connection and discovery
     */
    private void setBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,};
        }
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeData() {
        viewModel.devicesState.observe(this, adapter::updateItems);
        viewModel.discoveryState.observe(this, isDiscovery -> binding.setIsDiscovery(isDiscovery));
        baseViewModel.connectState.observe(this, state -> {
            binding.setState(state.toString());
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
        });
    }

    /**
     * Cancel or start discovery based on the currently running discovery state
     */
    private void startDiscovery() {
        if (Boolean.TRUE.equals(viewModel.discoveryState.getValue()))
            viewModel.stopDiscovery();
        else
            viewModel.startDiscovery();
    }

    /**
     * Request the necessary permissions from the user
     *
     * @return bluetooth permissions is granted
     */
    private boolean arePermissionsGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BluetoothActivity.this, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }
}