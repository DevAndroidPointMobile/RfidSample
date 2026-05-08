package device.apps.rfidsamplev2.sample.wired;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.databinding.ActivityWiredBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Connect the RF88 over the cabled (USB) accessory port.
 *
 * <p>The screen pairs a hero status card with a short two-step guide and a single
 * sticky action button. The OEM cable-detect broadcast receiver, the GPIO probe, and
 * the {@code connectState} LiveData all live in the app-scoped
 * {@link Rf88ConnectionManager} so the cable can drive connect/disconnect from any
 * screen — including no screen at all. This Activity adds the screen-scoped pieces:
 * <ul>
 *     <li>the manual Connect / Disconnect buttons (delegated to {@link WireViewModel});</li>
 *     <li>a one-shot auto-connect on entry when the sled is already attached, kicked
 *         off from {@code onCreate} via {@link WireViewModel#autoConnectIfAttached()};</li>
 *     <li>the {@code wireScreenActive} flag so ATTACH broadcasts only auto-connect while
 *         this screen is in the foreground.</li>
 * </ul>
 *
 * <p>A small note in the guide is shown on devices without GPIO auto-detect (e.g. PM90)
 * so the user knows automatic connection is unavailable and they must tap Connect.
 */
public class WiredActivity extends AppCompatActivity {

    private Rf88ConnectionManager connectionManager;
    private WireViewModel viewModel;
    private ActivityWiredBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeData();
        viewModel.autoConnectIfAttached();
    }

    /**
     * Open the {@code wireScreenActive} gate so ATTACH broadcasts are honoured as
     * auto-connect triggers while this screen is in the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        connectionManager.setWireScreenActive(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionManager.setWireScreenActive(false);
    }

    // No onDestroy override needed — connectState.observe(this, ...) is lifecycle-bound,
    // so the observer is removed automatically when the Activity is destroyed.

    /**
     * Initiate a cable connection. Bound from the layout's primary action button.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void connect(View view) {
        viewModel.connect();
    }

    /**
     * Disconnect from the cabled device. Bound from the layout's secondary action button.
     *
     * @param view Button view passed by the data-binding click handler
     */
    public void disconnect(View view) {
        viewModel.disconnect();
    }

    /**
     * Resolve the application-scoped connection manager and create the screen-scoped view model.
     * The cable-attach broadcast and the GPIO probe live in {@link Rf88ConnectionManager}
     * (started from {@link RFIDSampleV2}), so this screen only needs the ViewModel for the
     * manual button taps.
     */
    private void initializationViewModel() {
        connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        viewModel = new ViewModelProvider(this).get(WireViewModel.class);
    }

    /**
     * Inflate the data-bound layout and wire up navigation.
     */
    private void initializationContentView() {
        binding = ActivityWiredBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Subscribe to the connection state and feed the data-bound layout.
     *
     * <p>{@code isAutoDetectSupported} is a static value so it is pushed once. Title and
     * subtitle are recomputed on every connection-state change so the hero card always
     * reflects the latest SDK state.
     */
    private void observeData() {
        binding.setIsAutoDetectSupported(connectionManager.isAutoDetectSupported());
        connectionManager.connectState.observe(this, state ->
                binding.setIsConnected(state == DeviceConnectionState.CONNECTED));
        connectionManager.statusTitle.observe(this, binding::setStatusTitle);
        connectionManager.statusSubtitle.observe(this, binding::setStatusSubtitle);
    }
}
