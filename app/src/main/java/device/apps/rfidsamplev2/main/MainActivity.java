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
import device.apps.rfidsamplev2.sample.nread.InventoryNreadActivity;
import device.apps.rfidsamplev2.sample.wired.WiredActivity;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private BaseViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Rf88Manager.setDebugMode(true);

        viewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        viewModel.launch();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.setActivity(MainActivity.this);

        setContentView(binding.getRoot());
        viewModel.connectState.observe(this, state -> {
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
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

    public void routeInventoryNread(View view) {
        final Intent intent = new Intent(MainActivity.this, InventoryNreadActivity.class);
        startActivity(intent);
    }

    public void routeBarcodeScan(View view) {
        final Intent intent = new Intent(MainActivity.this, BarcodeActivity.class);
        startActivity(intent);
    }
}