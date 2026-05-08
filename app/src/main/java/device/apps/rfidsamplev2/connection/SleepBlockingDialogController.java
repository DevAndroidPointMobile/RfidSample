package device.apps.rfidsamplev2.connection;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;

import device.apps.rfidsamplev2.RFIDSampleV2;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * App-wide UI policy: shows a non-dismissable modal dialog whenever the RF88 reader
 * reports {@link DeviceConnectionState#SLEEP}, on whichever Activity is in the
 * foreground. The dialog blocks touch and back input on every screen and disappears
 * only when the SDK reports a non-SLEEP state.
 *
 * <h3>Why a separate class (not folded into {@link Rf88ConnectionManager})</h3>
 * <p>This controller is a pure <em>consumer</em> of
 * {@link Rf88ConnectionManager#connectState} — it observes one specific value (SLEEP)
 * and reacts with UI. Keeping it isolated:
 * <ul>
 *     <li>leaves {@link Rf88ConnectionManager} free of UI/Activity imports
 *         ({@link Application.ActivityLifecycleCallbacks}, {@link AlertDialog}) — the
 *         manager stays focused on producing connection state;</li>
 *     <li>makes the controller easy to remove or restyle without touching the
 *         connection-state machinery — customers who want a toast, a snackbar, or no
 *         UI at all can drop the {@link #attach(Application)} line in
 *         {@link RFIDSampleV2#onCreate()} and delete this file;</li>
 *     <li>illustrates the standard "consume LiveData app-wide" pattern as a small
 *         self-contained example for customer-developers reading the sample.</li>
 * </ul>
 *
 * <h3>Activity rebinding</h3>
 * <p>The dialog is bound to whichever Activity is currently in the foreground. On
 * Activity transitions (including configuration changes) the dialog is dismissed in
 * {@link #onActivityPaused} and re-attached to the next Activity in
 * {@link #onActivityResumed}, which keeps it from leaking a window from a destroyed
 * Activity.
 *
 * <h3>Wiring</h3>
 * <p>Created and attached once from {@link RFIDSampleV2#onCreate()}:
 * <pre>{@code
 * new SleepBlockingDialogController(connectionManager.connectState).attach(this);
 * }</pre>
 * Process-scoped, so {@link LiveData#observeForever} is the right pairing — the
 * controller has no LifecycleOwner and lives for the entire app process.
 */
public class SleepBlockingDialogController implements Application.ActivityLifecycleCallbacks {

    private final LiveData<DeviceConnectionState> connectState;

    private Activity foregroundActivity;
    private AlertDialog dialog;
    private boolean sleeping;

    public SleepBlockingDialogController(LiveData<DeviceConnectionState> connectState) {
        this.connectState = connectState;
    }

    /**
     * Wire up to the {@link Application}'s Activity lifecycle and the connection-state
     * stream. Call once from {@link RFIDSampleV2#onCreate()}; the controller lives for
     * the entire process so {@code observeForever} is the right pairing.
     */
    public void attach(Application app) {
        app.registerActivityLifecycleCallbacks(this);
        connectState.observeForever(this::onStateChanged);
    }

    private void onStateChanged(DeviceConnectionState state) {
        final boolean nowSleeping = state == DeviceConnectionState.SLEEP;
        if (nowSleeping == sleeping) return;
        sleeping = nowSleeping;
        if (sleeping) showDialog();
        else dismissDialog();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        foregroundActivity = activity;
        if (sleeping) showDialog();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Drop the dialog before the Activity's window goes away; the next Activity's
        // onResume will rebuild it if SLEEP is still in effect.
        if (activity == foregroundActivity) {
            dismissDialog();
            foregroundActivity = null;
        }
    }

    private void showDialog() {
        if (foregroundActivity == null) return;
        if (dialog != null && dialog.isShowing()) return;
        dialog = new AlertDialog.Builder(foregroundActivity)
                .setTitle("Reader is sleeping")
                .setMessage("The RF88 reader has entered sleep mode. Press the trigger key on the device to wake it up.")
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog == null) return;
        if (dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException ignored) {
                // Window was already detached (e.g. host Activity finished); safe to ignore.
            }
        }
        dialog = null;
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}
