package device.apps.rfidsamplev2.sample.wired;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.Rf88ConnectionRepository;
import device.apps.rfidsamplev2.databinding.ActivityWiredBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Connect the RF88 over the cabled (USB) accessory port.
 *
 * <p>The screen pairs a hero status card with a short two-step guide and a single
 * sticky action button. The OEM cable-detect broadcast receiver, the GPIO probe, and
 * the {@code connectState} LiveData all live in the app-scoped
 * {@link Rf88ConnectionRepository} so the cable can drive connect/disconnect from any
 * screen — including no screen at all. This Activity adds the screen-scoped pieces:
 * <ul>
 *     <li>the manual Connect / Disconnect buttons (delegated to {@link WireViewModel});</li>
 *     <li>a one-shot auto-connect on first entry when the sled is already attached
 *         (see {@link #tryAutoConnect()});</li>
 *     <li>the {@code wireScreenActive} flag so ATTACH broadcasts only auto-connect while
 *         this screen is in the foreground.</li>
 * </ul>
 *
 * <p>A small note in the guide is shown on devices without GPIO auto-detect (e.g. PM90)
 * so the user knows automatic connection is unavailable and they must tap Connect.
 */
public class WiredActivity extends AppCompatActivity {

    private Rf88ConnectionRepository connectionRepository;
    private WireViewModel viewModel;
    private ActivityWiredBinding binding;

    /**
     * Tracks whether the per-instance auto-connect attempt has already been made, so a
     * task-switch round-trip does not silently reconnect after the user has manually
     * tapped Disconnect.
     */
    private boolean autoConnectAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeData();
    }

    /**
     * Tell the repository to honour ATTACH broadcasts as auto-connect triggers while
     * this screen is in the foreground, and — on the first resume only — kick off a
     * connect when the sled is already attached so the user does not have to tap
     * Connect manually after entering with the cable already plugged in.
     */
    @Override
    protected void onResume() {
        super.onResume();
        connectionRepository.setWireScreenActive(true);
        if (!autoConnectAttempted) {
            autoConnectAttempted = true;
            tryAutoConnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionRepository.setWireScreenActive(false);
    }

    // No onDestroy override needed — connectState.observe(this, ...) is lifecycle-bound,
    // so the observer is removed automatically when the Activity is destroyed.

    /**
     * One-shot connect for the "entered Wired screen with sled already attached" case.
     * Skipped if no sled is attached or the SDK is already connected/connecting.
     */
    private void tryAutoConnect() {
        if (!Boolean.TRUE.equals(connectionRepository.sledAttached.getValue()))
            return;
        final DeviceConnectionState state = connectionRepository.connectState.getValue();
        if (state == DeviceConnectionState.CONNECTED || state == DeviceConnectionState.CONNECTING)
            return;
        viewModel.connect();
    }

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
     * Resolve the application-scoped connection repository and create the screen-scoped view model.
     * The cable-attach broadcast and the GPIO probe live in {@link Rf88ConnectionRepository}
     * (started from {@link RFIDSampleV2}), so this screen only needs the ViewModel for the
     * manual button taps.
     */
    private void initializationViewModel() {
        connectionRepository = ((RFIDSampleV2) getApplication()).getConnectionRepository();
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
        binding.setIsAutoDetectSupported(connectionRepository.isAutoDetectSupported());
        connectionRepository.connectState.observe(this, state -> {
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
            binding.setStatusTitle(getStatusTitle(state));
            binding.setStatusSubtitle(getStatusSubtitle(state));
        });
    }

    /**
     * Map the SDK connection state to the headline shown on the hero card.
     */
    private String getStatusTitle(DeviceConnectionState state) {
        if (state == null) return "Disconnected";
        switch (state) {
            case CONNECTED:
                return "Connected";
            case CONNECTING:
                return "Connecting...";
            default:
                return "Disconnected";
        }
    }

    /**
     * Map the SDK connection state to the helper text shown under the headline.
     */
    private String getStatusSubtitle(DeviceConnectionState state) {
        if (state == null) return "Connect via the cable below";
        switch (state) {
            case CONNECTED:
                return "Your RF88 device is ready";
            case CONNECTING:
                return "Establishing connection";
            default:
                return "Connect via the cable below";
        }
    }
}
