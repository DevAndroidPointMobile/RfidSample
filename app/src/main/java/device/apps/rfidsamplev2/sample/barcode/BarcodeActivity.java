package device.apps.rfidsamplev2.sample.barcode;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.databinding.ActivityBarcodeBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Sample screen showing how to forward the RF88 <b>Scanner trigger key</b> to the device's
 * built-in barcode scanner.
 *
 * <p>The RF88 reader has two physical trigger keys (Inventory and Scanner); this screen
 * deals with the Scanner one only — see {@link BarcodeViewModel} for the bridging logic
 * and the broader explanation of the two-key model.
 *
 * <p>What happens when the screen opens:
 * <ol>
 *     <li>{@link #initializationViewModel()} — creates {@link BarcodeViewModel} and
 *         registers it as the RF88 hardware-key listener so Scanner-trigger events are
 *         routed to the scanner.</li>
 *     <li>{@link #initializationContentView()} — inflates the layout, applies the idle
 *         hero colors, and wires the toolbar's back arrow.</li>
 *     <li>{@link #observeConnection()} — closes the screen automatically if the RF88
 *         device is not (or is no longer) connected. Without an RF88 link the trigger
 *         flow has nothing to do, so staying here would be meaningless.</li>
 *     <li>{@link #observeScanState()} — animates the hero card into a "scanning"
 *         appearance while the user is holding the Scanner trigger, so they get visual
 *         confirmation that the SDK plumbing is alive.</li>
 * </ol>
 *
 * <p>Receiving and displaying the actual decoded barcode is intentionally out of scope —
 * that lives in the dedicated scanner-SDK sample. This screen only demonstrates the
 * trigger-forwarding pattern.
 */
public class BarcodeActivity extends AppCompatActivity {

    /** Total duration of the trigger-feedback animation (color + text crossfade). */
    private static final long ANIM_DURATION_MS = 220L;

    private BarcodeViewModel viewModel;
    private ActivityBarcodeBinding binding;

    /** Tracks the last delivered scanning value so we don't re-animate on duplicate emits. */
    private boolean lastScanningState = false;
    /** Currently running animator, if any — cancelled when a new state arrives. */
    private AnimatorSet currentAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeConnection();
        observeScanState();
    }

    /**
     * Creates the {@link BarcodeViewModel} via {@link ViewModelProvider} and lets it
     * register the RF88 hardware-key listener through {@link BarcodeViewModel#launch()}.
     * <p>
     * Using {@link ViewModelProvider} (rather than {@code new BarcodeViewModel()}) means
     * the ViewModel instance survives configuration changes such as screen rotation —
     * the framework keeps it alive in the Activity's {@code ViewModelStore}.
     */
    private void initializationViewModel() {
        viewModel = new ViewModelProvider(this).get(BarcodeViewModel.class);
        viewModel.launch();
    }

    /**
     * Inflates the layout, applies the idle hero colors directly (without animation), and
     * wires the toolbar's back-arrow.
     */
    private void initializationContentView() {
        binding = ActivityBarcodeBinding.inflate(getLayoutInflater());
        binding.setIsScanning(false);
        binding.heroCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.brand_surface));
        ImageViewCompat.setImageTintList(binding.heroIcon,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Finishes the screen when the connection is genuinely lost. Uses
     * {@link Rf88ConnectionManager#isLost} rather than observing {@code connectState}
     * directly so that the transient {@code DISCONNECTED} the SDK fires during a SLEEP
     * round-trip does not close the screen mid-sleep.
     *
     * <p>{@link Rf88ConnectionManager} is an app-scoped object held by
     * {@link RFIDSampleV2}; any Activity can retrieve it via
     * {@code getConnectionManager()}. The {@code observe(this, ...)} call is bound to
     * this Activity's lifecycle, so the observer is removed automatically when the
     * Activity is destroyed — no manual cleanup is required.
     */
    private void observeConnection() {
        final Rf88ConnectionManager connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        connectionManager.isLost.observe(this, lost -> {
            if (Boolean.TRUE.equals(lost)) finish();
        });
    }

    /**
     * Watches {@link BarcodeViewModel#isScanning} and, on a real state change, runs the
     * trigger-feedback animation. The first observation matches the initial idle state so
     * it is intentionally a no-op.
     */
    private void observeScanState() {
        viewModel.isScanning.observe(this, scanning -> {
            final boolean active = Boolean.TRUE.equals(scanning);
            if (active == lastScanningState) return;
            lastScanningState = active;
            animateScanState(active);
        });
    }

    /**
     * Animates the hero card into (or out of) its "scanning" appearance.
     * <ul>
     *     <li>Card background and icon tint cross-fade between the brand surface/primary
     *         palette using {@link ArgbEvaluator}.</li>
     *     <li>Title and subtitle alpha-fade out, swap their text via the DataBinding
     *         variable, then fade back in — a quick crossfade that masks the abrupt
     *         text change.</li>
     * </ul>
     * Any in-flight animation is cancelled first so rapid trigger toggles always reach
     * the latest target state.
     */
    private void animateScanState(boolean active) {
        if (currentAnimator != null) currentAnimator.cancel();

        final int cardFrom = binding.heroCard.getCardBackgroundColor().getDefaultColor();
        final int cardTo = ContextCompat.getColor(this,
                active ? R.color.brand_primary : R.color.brand_surface);
        final ColorStateList currentTint = ImageViewCompat.getImageTintList(binding.heroIcon);
        final int iconFrom = currentTint != null
                ? currentTint.getDefaultColor()
                : ContextCompat.getColor(this, R.color.brand_primary);
        final int iconTo = ContextCompat.getColor(this,
                active ? R.color.white : R.color.brand_primary);

        final ValueAnimator cardAnim = ValueAnimator.ofObject(new ArgbEvaluator(), cardFrom, cardTo);
        cardAnim.addUpdateListener(a ->
                binding.heroCard.setCardBackgroundColor((int) a.getAnimatedValue()));

        final ValueAnimator iconAnim = ValueAnimator.ofObject(new ArgbEvaluator(), iconFrom, iconTo);
        iconAnim.addUpdateListener(a -> ImageViewCompat.setImageTintList(
                binding.heroIcon, ColorStateList.valueOf((int) a.getAnimatedValue())));

        final AnimatorSet set = new AnimatorSet();
        set.playTogether(cardAnim, iconAnim);
        set.setDuration(ANIM_DURATION_MS);
        set.setInterpolator(new FastOutSlowInInterpolator());
        currentAnimator = set;
        set.start();

        // Text crossfade: out → swap → in, lined up with the color animation length.
        final long halfDuration = ANIM_DURATION_MS / 2;
        binding.heroTitle.animate()
                .alpha(0f)
                .setDuration(halfDuration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> {
                    binding.setIsScanning(active);
                    binding.heroTitle.animate().alpha(1f).setDuration(halfDuration).start();
                    binding.heroSubtitle.animate().alpha(1f).setDuration(halfDuration).start();
                })
                .start();
        binding.heroSubtitle.animate()
                .alpha(0f)
                .setDuration(halfDuration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }
}
