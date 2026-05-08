package device.apps.rfidsamplev2.sample.inventory;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.databinding.ActivityInventoryBinding;
import device.apps.rfidsamplev2.databinding.DialogMandatoryBinding;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import device.apps.rfidsamplev2.sample.inventory.ui.InventoryAdapter;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Sample screen for RF88 RFID inventory: hold the Inventory trigger key (or press the
 * on-screen Start/Stop button) to scan, see discovered tags accumulate in the list with
 * per-tag read counts, and tap any tag to open the Mandatory dialog (Read / Write / Lock /
 * Kill).
 *
 * <p>The blinking dot in the list header is purely visual feedback that scanning is
 * active; the underlying state comes from {@link InventoryViewModel#isScanning}, which is
 * itself driven by the SDK's current-action callback so the trigger and on-screen button
 * stay in sync.
 *
 * <p>Closes itself on RF88 disconnect — see {@link #observeConnection()}.
 */
public class InventoryActivity extends AppCompatActivity implements OnInventoryClickListener {

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private ActivityInventoryBinding binding;
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
     * Finishes the screen when the connection is genuinely lost. Uses
     * {@link Rf88ConnectionManager#isLost} rather than observing {@code connectState}
     * directly so that the transient {@code DISCONNECTED} the SDK fires during a SLEEP
     * round-trip does not close the screen mid-sleep.
     */
    private void observeConnection() {
        final Rf88ConnectionManager connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        connectionManager.isLost.observe(this, lost -> {
            if (Boolean.TRUE.equals(lost)) finish();
        });
    }

    /**
     * Stop any in-flight inventory the moment the screen leaves the foreground. This
     * covers every exit path (back press, home, navigation to another Activity) so the
     * RF88 reader never keeps radiating after the user has moved on. The Mandatory
     * dialog is an in-Activity {@link AlertDialog} and does not trigger {@code onPause}.
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

    /** Tap on a tag row in the list — opens the Mandatory command dialog for that tag. */
    @Override
    public void onInventoryClicked(InventoryResponse item) {
        showMandatoryDialog(item);
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
     * Toggle inventory from the on-screen button. Acts as a software equivalent of
     * the hardware trigger so users can scan without holding a physical key.
     *
     * @param view Button view
     */
    public void onToggleInventory(View view) {
        viewModel.toggleInventory();
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        adapter = new InventoryAdapter(viewModel.readHistory, this);
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.setUniqueCount(0);
        binding.setIsScanning(false);
        binding.recyclerView.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
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
     * indefinitely with REVERSE so the dot fades in and out smoothly while the user
     * holds the Inventory trigger key.
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
     * partially transparent if the trigger is released mid-frame.
     */
    private void stopScanBlink() {
        if (scanBlinkAnimator == null) return;
        scanBlinkAnimator.cancel();
        scanBlinkAnimator = null;
        binding.scanDot.setAlpha(1f);
    }

    /**
     * Build and show the Mandatory command dialog (Read / Write / Lock / Kill) bound
     * to {@code response}. Each card dispatches to the matching ViewModel call and
     * dismisses the dialog.
     *
     * @param response inventory entry the dialog will operate on
     */
    private void showMandatoryDialog(InventoryResponse response) {
        final DialogMandatoryBinding dialogBinding = DialogMandatoryBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogBinding.getRoot());
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogBinding.cancel.setOnClickListener(view -> dialog.dismiss());
        dialogBinding.read.setOnClickListener(view -> {
            viewModel.read(response);
            dialog.dismiss();
        });
        dialogBinding.write.setOnClickListener(view -> {
            viewModel.write(response);
            dialog.dismiss();
        });
        dialogBinding.lock.setOnClickListener(view -> {
            viewModel.lock(response);
            dialog.dismiss();
        });
        dialogBinding.kill.setOnClickListener(view -> {
            viewModel.kill(response);
            dialog.dismiss();
        });
        dialog.show();
    }
}
