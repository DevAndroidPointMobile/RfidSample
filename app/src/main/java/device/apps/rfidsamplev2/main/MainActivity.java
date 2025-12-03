package device.apps.rfidsamplev2.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.sample.barcode.BarcodeActivity;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothActivity;
import device.apps.rfidsamplev2.databinding.ActivityMainBinding;
import device.apps.rfidsamplev2.sample.configuration.ConfigActivity;
import device.apps.rfidsamplev2.sample.inventory.InventoryActivity;
import device.apps.rfidsamplev2.sample.nfc.NfcActivity;
import device.apps.rfidsamplev2.sample.wired.WiredActivity;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding _binding;
    private BaseViewModel _viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _viewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        _viewModel.launch();

        _binding = ActivityMainBinding.inflate(getLayoutInflater());
        _binding.setActivity(MainActivity.this);

        setContentView(_binding.getRoot());
        _viewModel.connectState.observe(this, state -> {
            _binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
        });
    }

    public void routeBluetoothConnection(View view) {
        final Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
        startActivity(intent);
    }

    public void routeWireConnection(View view) {
        final Intent intent = new Intent(MainActivity.this, WiredActivity.class);
        startActivity(intent);
    }

    public void routeConfiguration(View view) {
        final Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
        startActivity(intent);
    }

    public void routeTapToPair(View view) {
        final Intent intent = new Intent(MainActivity.this, NfcActivity.class);
        startActivity(intent);
    }

    public void routeInventory(View view) {
        final Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
        startActivity(intent);
    }

    public void routeBarcodeScan(View view) {
        final Intent intent = new Intent(MainActivity.this, BarcodeActivity.class);
        startActivity(intent);
    }
}