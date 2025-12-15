package device.apps.rfidsamplev2.main;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

import device.apps.rfidsamplev2.BaseActivity;
import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.bluetooth.BluetoothConnectActivity;
import device.apps.rfidsamplev2.sample.barcode.BarcodeActivity;
import device.apps.rfidsamplev2.sample.configuration.ConfigActivity;
import device.apps.rfidsamplev2.sample.inventory.InventoryActivity;
import device.apps.rfidsamplev2.sample.nfc.NfcActivity;
import device.apps.rfidsamplev2.sample.nread.InventoryNreadActivity;
import device.apps.rfidsamplev2.sample.wired.WiredActivity;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class MainActivity extends BaseActivity {

    private MaterialButton btnInventory;
    private MaterialButton btnInventoryNRead;
    private MaterialButton btnBarcode;
    private MaterialButton btnConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Rf88Manager.setDebugMode(true);

        setContentView(R.layout.activity_main);

        initViews();
        initClickListeners();
        updateMenuState(false);
    }

    /**
     * Initialize view references.
     */
    private void initViews() {
        btnInventory = findViewById(R.id.btnInventory);
        btnInventoryNRead = findViewById(R.id.btnInventoryNRead);
        btnBarcode = findViewById(R.id.btnBarcode);
        btnConfiguration = findViewById(R.id.btnConfiguration);
    }

    /**
     * Initialize button click listeners.
     */
    private void initClickListeners() {
        findViewById(R.id.btnBluetooth).setOnClickListener(
                v -> startActivity(new Intent(this, BluetoothConnectActivity.class)));

        findViewById(R.id.btnWired).setOnClickListener(
                v -> startActivity(new Intent(this, WiredActivity.class)));

        findViewById(R.id.btnTapToPair).setOnClickListener(
                v -> startActivity(new Intent(this, NfcActivity.class)));

        btnInventory.setOnClickListener(
                v -> startActivity(new Intent(this, InventoryActivity.class)));

        btnInventoryNRead.setOnClickListener(
                v -> startActivity(new Intent(this, InventoryNreadActivity.class)));

        btnBarcode.setOnClickListener(
                v -> startActivity(new Intent(this, BarcodeActivity.class)));

        btnConfiguration.setOnClickListener(
                v -> startActivity(new Intent(this, ConfigActivity.class)));
    }

    /**
     * Enable or disable menus that require device connection.
     */
    private void updateMenuState(boolean enabled) {
        btnInventory.setEnabled(enabled);
        btnInventoryNRead.setEnabled(enabled);
        btnBarcode.setEnabled(enabled);
        btnConfiguration.setEnabled(enabled);
    }

    /**
     * Called from BaseActivity when connection state changes.
     */
    @Override
    protected void onConnectionStateChanged(DeviceConnectionState state) {
        boolean connected = state == DeviceConnectionState.CONNECTED;
        updateMenuState(connected);
    }
}