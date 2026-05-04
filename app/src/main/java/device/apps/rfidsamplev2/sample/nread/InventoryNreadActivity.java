package device.apps.rfidsamplev2.sample.nread;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.Rf88ConnectionRepository;
import device.apps.rfidsamplev2.databinding.ActivityInventoryNreadBinding;
import device.apps.rfidsamplev2.sample.nread.callback.OnInventoryNreadClickListener;
import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;
import device.apps.rfidsamplev2.sample.nread.ui.InventoryNreadAdapter;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Sample screen for RF88 <b>Inventory &amp; Read</b>: hold the Inventory trigger key (or
 * press the on-screen Start/Stop button) to scan, see discovered tags accumulate in the
 * list along with the bytes read from each tag's memory bank in the same air-protocol
 * exchange.
 *
 * <p>The blinking dot in the list header is purely visual feedback that scanning is
 * active; the underlying state comes from {@link InventoryNreadViewModel#isScanning},
 * which is itself driven by the SDK's current-action callback so the trigger and
 * on-screen button stay in sync.
 *
 * <p>Closes itself on RF88 disconnect — see {@link #observeConnection()}.
 */
public class InventoryNreadActivity extends AppCompatActivity implements OnInventoryNreadClickListener {

    private InventoryNreadAdapter adapter;
    private InventoryNreadViewModel viewModel;
    private ActivityInventoryNreadBinding binding;
    private ObjectAnimator scanBlinkAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeConnection();
        observeLiveData();
    }

    /**
     * Stop any in-flight inventory the moment the screen leaves the foreground so the
     * RF88 reader never keeps radiating after the user has moved on.
     */
    @Override
    protected void onPause() {
        super.onPause();
        viewModel.stopInventory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScanBlink();
    }

    /**
     * Tap on a tag row in the list — placeholder. The Inventory &amp; Read sample does
     * not currently surface a follow-up action (unlike the Inventory sample's Mandatory
     * dialog), so taps are intentionally a no-op. Implementations that want to act on
     * the row (open detail, copy EPC, etc.) start here.
     */
    @Override
    public void onInventoryNreadClicked(InventoryNreadResponse item) {
        // No-op for now — the screen displays read results and does not act on row taps.
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        viewModel = new ViewModelProvider(this).get(InventoryNreadViewModel.class);
        viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        adapter = new InventoryNreadAdapter(viewModel.readHistory, this);
        binding = ActivityInventoryNreadBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.setUniqueCount(0);
        binding.setIsScanning(false);
        binding.recyclerView.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Watches the global RF88 connection state and finishes the screen when it becomes
     * {@link DeviceConnectionState#DISCONNECTED}. Other non-connected states ({@code SLEEP},
     * {@code DISCONNECTING}, {@code CONNECTING}, {@code FAILURE}) keep the screen open —
     * notably {@code SLEEP}, which is a temporary low-power pause that the device
     * recovers from on its own, so finishing here would lose the user's context.
     */
    private void observeConnection() {
        final Rf88ConnectionRepository connectionRepository = ((RFIDSampleV2) getApplication()).getConnectionRepository();
        connectionRepository.connectState.observe(this, state -> {
            if (state == DeviceConnectionState.DISCONNECTED)
                finish();
        });
    }

    /**
     * Observe the ViewModel signals that drive the UI: the changed-row index for the
     * adapter (and the unique-count badge), and the scanning state for the dot
     * blink animation.
     */
    private void observeLiveData() {
        viewModel.changedIndex.observe(this, index -> {
            adapter.updateData(index);
            binding.setUniqueCount(viewModel.readHistory.size());
        });
        viewModel.isScanning.observe(this, scanning -> {
            final boolean active = Boolean.TRUE.equals(scanning);
            binding.setIsScanning(active);
            if (active) startScanBlink();
            else stopScanBlink();
        });
    }

    /**
     * Start the alpha-pulse animation on the scanning dot. The animator repeats
     * indefinitely with REVERSE so the dot fades in and out smoothly while the
     * inventory-and-read scan is running.
     */
    private void startScanBlink() {
        if (scanBlinkAnimator != null) return;
        scanBlinkAnimator = ObjectAnimator.ofFloat(binding.scanDot, View.ALPHA, 1f, 0.2f);
        scanBlinkAnimator.setDuration(450L);
        scanBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanBlinkAnimator.start();
    }

    /**
     * Cancel the blink animation and reset the dot's alpha so it does not stay
     * partially transparent if the scan ends mid-frame.
     */
    private void stopScanBlink() {
        if (scanBlinkAnimator == null) return;
        scanBlinkAnimator.cancel();
        scanBlinkAnimator = null;
        binding.scanDot.setAlpha(1f);
    }

    /**
     * Clear all the Inventory responses displayed on the screen
     *
     * @param view Button view
     */
    public void onClear(View view) {
        viewModel.clearHistory();
    }

    /**
     * Toggle inventory-and-read from the on-screen button. Acts as a software equivalent
     * of the hardware trigger so users can scan without holding a physical key.
     *
     * @param view Button view
     */
    public void onToggleInventory(View view) {
        viewModel.toggleInventory();
    }
}
