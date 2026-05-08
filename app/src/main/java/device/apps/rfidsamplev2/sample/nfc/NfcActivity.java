package device.apps.rfidsamplev2.sample.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.databinding.ActivityNfcBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * "Tap to Pair" — establish an RF88 Bluetooth connection by reading a Bluetooth
 * Out-Of-Band pairing record from an NFC tag.
 *
 * <p>The screen handles three NFC-availability states:
 * <ul>
 *   <li>Hardware missing — only the hero card is shown explaining the situation.</li>
 *   <li>Hardware present but turned off — an "Open NFC Settings" button takes the
 *       user to the system NFC toggle.</li>
 *   <li>Hardware present and on — NFC foreground dispatch is enabled and the screen
 *       waits for the user to tap a tag.</li>
 * </ul>
 *
 * <p>NDEF parsing and RF88 SDK calls live in {@link NfcViewModel}; this Activity owns
 * the Android-NFC framework integration (adapter, foreground dispatch, intent
 * handling) and pushes derived strings into the data-bound layout.
 */
public class NfcActivity extends AppCompatActivity {

    private Rf88ConnectionManager connectionManager;
    private NfcViewModel viewModel;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private ActivityNfcBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        initializationNfcFunction();
        observeData();
    }

    /**
     * Re-enable foreground NFC dispatch and refresh the UI in case the user toggled
     * NFC in system settings while we were paused.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        refreshNfcUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // No onDestroy override needed — connectState.observe(this, ...) is lifecycle-bound,
    // so the observer is removed automatically when the Activity is destroyed.

    /**
     * NFC foreground dispatch routes tag reads here. We only act on Bluetooth handover
     * NDEF records; everything else is ignored silently.
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            handleNdefIntent(intent);
        }
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
     * Open the system NFC settings so the user can turn the NFC adapter on.
     * Bound from the layout when NFC is supported but currently off.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void openNfcSettings(View view) {
        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
    }

    /**
     * Resolve the application-scoped connection manager and create the screen-scoped view model.
     */
    private void initializationViewModel() {
        connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        viewModel = new ViewModelProvider(this).get(NfcViewModel.class);
    }

    /**
     * Inflate the data-bound layout and wire up navigation.
     */
    private void initializationContentView() {
        binding = ActivityNfcBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Resolve the NFC adapter and prepare the {@link PendingIntent} used by foreground
     * dispatch. When the device has no NFC hardware we leave both fields {@code null};
     * the hero card explains the situation and no further setup is required.
     */
    private void initializationNfcFunction() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) return;

        final Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Subscribe to the connection state and refresh the hero card on every change.
     */
    private void observeData() {
        connectionManager.connectState.observe(this, state -> refreshNfcUi());
    }

    /**
     * Recompute every layout variable based on (1) NFC hardware availability, (2) NFC
     * adapter on/off state and (3) the SDK connection state. Called from the connection
     * observer and from {@link #onResume} so the UI stays accurate after the user
     * toggles NFC in system settings.
     */
    private void refreshNfcUi() {
        final boolean supported = nfcAdapter != null;
        final boolean enabled = supported && nfcAdapter.isEnabled();
        final DeviceConnectionState state = connectionManager.connectState.getValue();
        final boolean connected = state == DeviceConnectionState.CONNECTED;

        binding.setIsHeroActive(supported && enabled);
        binding.setIsConnected(connected);
        binding.setShowOpenNfcSettings(supported && !enabled);
        binding.setShowActionBar((supported && !enabled) || connected);
        if (!supported) {
            binding.setStatusTitle("NFC not supported");
            binding.setStatusSubtitle("This device cannot use NFC");
        } else if (!enabled) {
            binding.setStatusTitle("NFC is off");
            binding.setStatusSubtitle("Turn on NFC to pair");
        } else {
            // NFC-level checks pass — defer the connection-state copy to the manager.
            binding.setStatusTitle(connectionManager.statusTitle.getValue());
            binding.setStatusSubtitle(connectionManager.statusSubtitle.getValue());
        }
    }

    /**
     * Forward an {@code ACTION_NDEF_DISCOVERED} intent to the view model for parsing.
     * Shows a short toast confirming the connect attempt, or letting the user know
     * the tag did not contain a Bluetooth pairing record.
     */
    private void handleNdefIntent(Intent intent) {
        final String address = viewModel.parseMacFromOobIntent(intent);
        if (address == null) {
            Toast.makeText(this, "This NFC tag does not contain a Bluetooth pairing record", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Connecting to: " + address, Toast.LENGTH_SHORT).show();
        viewModel.connect(address);
    }
}
