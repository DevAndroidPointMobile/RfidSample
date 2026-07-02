package device.apps.rfidsamplev2.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.RemoteException;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.sdk.Control;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * Point-Mobile-only owner of the cabled-sled attach / detach flow.
 *
 * <h3>Why this is a separate class</h3>
 * <p>The cable flow is the app's only dependency on the proprietary {@code device.sdk}
 * framework at connection scope — the {@link Control} GPIO probe and the
 * {@code pm.ex.gpio.changed} broadcast. That framework does not exist on stock Android
 * (e.g. a Galaxy phone), so touching {@link Control} there throws
 * {@link NoClassDefFoundError}. Keeping every {@code device.sdk} reference in this one
 * class lets {@link RFIDSampleV2} create it <b>only</b> when
 * {@link DeviceSdk#isAvailable()} — on non-PM hardware it is never instantiated, so
 * {@link Rf88ConnectionManager} and the universal Bluetooth / NFC features stay free of
 * any proprietary reference and the app launches normally.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li>Detach ({@code acc_det == "0"}) always disconnects so the SDK matches physical
 *       reality regardless of which screen is on top.</li>
 *   <li>Attach ({@code acc_det == "1"}) updates {@link #sledAttached} but only triggers a
 *       connect when the Wired screen is in the foreground (see
 *       {@link #setWireScreenActive(boolean)}) — the project rule is "connection happens
 *       inside the connection screen".</li>
 * </ul>
 *
 * <p>Broadcast receivers and this class's public methods run on the main thread, so the
 * blocking SDK / Binder calls are dispatched on a background executor to avoid an ANR.
 */
public final class WiredAttachDetector {

    /** Value of {@code acc_det} when the cable is currently attached. */
    private static final String ATTACH = "1";
    /** Value of {@code acc_det} when the cable is currently detached. */
    private static final String DETACH = "0";
    /** OEM broadcast emitted by the platform whenever the expansion accessory state changes. */
    private static final String ACTION_DEVICE_CHANGED = "pm.ex.gpio.changed";
    /** String extra on the broadcast — "1" for attached, "0" for detached. */
    private static final String EXTRA_CONNECT_STATE = "acc_det";

    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final Control control = Control.getInstance();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> mutableSledAttached = new MutableLiveData<>();
    private final BroadcastReceiver receiver = new ConnectionReceiver();

    /**
     * When {@code true}, an ATTACH broadcast triggers an automatic wire connect. Only the
     * Wired screen flips this on (in {@code onResume}) so connection always happens
     * "inside the connection screen"; detach still disconnects unconditionally.
     */
    private volatile boolean wireScreenActive = false;

    /**
     * Whether the reader is currently mounted on the sled. {@code null} means "unknown" —
     * either the GPIO probe has not completed yet, or the host hardware does not expose
     * the auto-detect line (see {@link #isAutoDetectSupported()}).
     */
    public final LiveData<Boolean> sledAttached = mutableSledAttached;

    /**
     * Register the OEM cable-attach broadcast receiver with app-wide lifetime and take an
     * initial GPIO reading. Call once from {@link RFIDSampleV2#onCreate()}.
     *
     * @param context the {@link android.app.Application} context
     */
    public void start(Context context) {
        ContextCompat.registerReceiver(context, receiver, getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        probeCableOnLaunch();
    }

    /**
     * Whether the host device exposes the GPIO line we use to auto-detect the sled cable.
     * Constant for the lifetime of the app. Devices without it (e.g. PM90) rely on the
     * manual Connect button.
     */
    public boolean isAutoDetectSupported() {
        return !Build.MODEL.contains("PM90");
    }

    /**
     * Toggle the "auto-connect on sled attach" gate. Call from
     * {@code WiredActivity#onResume} with {@code true} and {@code onPause} with {@code false}.
     */
    public void setWireScreenActive(boolean active) {
        wireScreenActive = active;
    }

    /**
     * Auto-connect once on Wired-screen entry when the sled is already attached. The GPIO
     * probe ({@link Control#getExpansionAccDetGpio()}) is a Binder call and the SDK
     * {@code connect} is synchronous — both run off the main thread. A RemoteException from
     * the probe (e.g. on hardware without the auto-detect GPIO line such as PM90) is
     * swallowed: in that case the user just taps Connect manually.
     */
    public void autoConnectIfAttached() {
        if (!isAutoDetectSupported())
            return;

        executorService.execute(() -> {
            try {
                if (ATTACH.equals(control.getExpansionAccDetGpio()))
                    rf88Manager.connect();

            } catch (RemoteException ignored) {
                // GPIO probe unavailable — auto-connect skipped, manual Connect still works.
            }
        });
    }

    /**
     * Probe the GPIO line once at app startup so {@link #sledAttached} starts with the real
     * value instead of {@code null}. Does NOT trigger an auto-connect — connection is
     * intentionally left to the Wired screen. Skipped on hardware without the GPIO line.
     */
    private void probeCableOnLaunch() {
        if (!isAutoDetectSupported())
            return;

        executorService.execute(() -> {
            try {
                final String detected = control.getExpansionAccDetGpio();
                mutableSledAttached.postValue(Objects.equals(detected, ATTACH));
            } catch (RemoteException exception) {
                // Probe failed — subsequent broadcasts can still drive the flow.
            }
        });
    }

    private IntentFilter getIntentFilter() {
        final IntentFilter result = new IntentFilter();
        result.addAction(ACTION_DEVICE_CHANGED);
        return result;
    }

    /**
     * Translates the sled attach/detach broadcast into RF88 SDK calls. Runs on the main
     * thread, so SDK calls are dispatched on the background executor to avoid an ANR.
     */
    private class ConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_DEVICE_CHANGED.equals(intent.getAction()))
                return;

            final String detected = intent.getStringExtra(EXTRA_CONNECT_STATE);
            if (Objects.equals(detected, ATTACH)) {
                mutableSledAttached.setValue(true);
                if (wireScreenActive)
                    executorService.execute(rf88Manager::connect);

            } else if (Objects.equals(detected, DETACH)) {
                mutableSledAttached.setValue(false);
                executorService.execute(rf88Manager::disconnect);
            }
        }
    }
}
