package device.apps.rfidsamplev2.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.Rf88ConnectionRepository;
import device.apps.rfidsamplev2.databinding.ActivityMainBinding;
import device.apps.rfidsamplev2.sample.barcode.BarcodeActivity;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothActivity;
import device.apps.rfidsamplev2.sample.configuration.ConfigActivity;
import device.apps.rfidsamplev2.sample.inventory.InventoryActivity;
import device.apps.rfidsamplev2.sample.nfc.NfcActivity;
import device.apps.rfidsamplev2.sample.nread.InventoryNreadActivity;
import device.apps.rfidsamplev2.sample.wired.WiredActivity;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * The sample app's launcher screen and feature hub.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *     <li><b>Surface the global RF88 connection state</b> — observes
 *         {@link Rf88ConnectionRepository#connectState} and feeds the result into the
 *         header status card and the disabled state of the Features buttons.</li>
 *     <li><b>List every sample screen</b> — the layout exposes one button per sample
 *         under the <i>Connection</i> and <i>Features</i> sections. Each button is wired
 *         via DataBinding ({@code android:onClick="@{activity::routeXxx}"}) to a
 *         corresponding {@code routeXxx} method below.</li>
 *     <li><b>Gate feature routes by connection</b> — the Inventory / Inventory N Read /
 *         Barcode / Configuration tiles bind {@code android:clickable},
 *         {@code android:focusable}, and {@code android:alpha} to {@code isConnected}
 *         so the user cannot enter them without a device, and the disabled tiles look
 *         visibly dimmed.</li>
 * </ol>
 *
 * <h3>Why connection state lives outside this Activity</h3>
 * The RF88 connection itself is owned by the application-scoped
 * {@link Rf88ConnectionRepository} (held by {@link RFIDSampleV2}), not by this Activity.
 * That means navigating away to a feature screen and returning here does <b>not</b>
 * drop the device — the same connection persists for the lifetime of the application
 * process.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /**
     * Inflates the layout, plugs this Activity into DataBinding so the layout's
     * {@code android:onClick="@{activity::routeXxx}"} references can resolve, and
     * starts observing the global connection state — which drives both the header
     * status card and the disabled state of the Features buttons.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Rf88ConnectionRepository connectionRepository =
                ((RFIDSampleV2) getApplication()).getConnectionRepository();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.setActivity(MainActivity.this);

        setContentView(binding.getRoot());
        connectionRepository.connectState.observe(this, state -> {
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
            binding.setStatusTitle(getStatusTitle(state));
            binding.setStatusSubtitle(getStatusSubtitle(state));
        });
    }

    /**
     * Map the SDK connection state to the headline shown on the hero card. Surfacing
     * {@code CONNECTING} explicitly is the whole reason for this method — collapsing
     * everything except {@code CONNECTED} into "Not Connected" hides the brief connect
     * transition (especially on auto-connect via the sled) from the user.
     */
    private String getStatusTitle(DeviceConnectionState state) {
        if (state == null) return "Not Connected";
        switch (state) {
            case CONNECTED:
                return "Connected";
            case CONNECTING:
                return "Connecting...";
            default:
                return "Not Connected";
        }
    }

    /** Map the SDK connection state to the helper text shown under the headline. */
    private String getStatusSubtitle(DeviceConnectionState state) {
        if (state == null) return "Choose a connection method below";
        switch (state) {
            case CONNECTED:
                return "Your RF88 device is ready to use";
            case CONNECTING:
                return "Establishing connection";
            default:
                return "Choose a connection method below";
        }
    }

    /*
     * Navigation handlers below.
     *
     * Each method is referenced from activity_main.xml via DataBinding click bindings
     * (e.g. android:onClick="@{activity::routeBluetoothConnection}"), which is why they
     * must be public and accept a View parameter — even though the View parameter is
     * unused here. The names are the contract; do not rename without updating the
     * matching binding expression in the layout.
     */

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
