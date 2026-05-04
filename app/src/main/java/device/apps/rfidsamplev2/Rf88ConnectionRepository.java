package device.apps.rfidsamplev2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.sdk.Control;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;
import ex.dev.sdk.rf88.frameworks.listener.OnConnectionStateChangedListener;

/**
 * App-scoped owner of every connection-lifecycle source the app cares about. Held by
 * {@link RFIDSampleV2} and started once from {@code onCreate()}, so the receivers live
 * for the entire process — attaching/detaching the sled or losing the Bluetooth link
 * works whether the user is on the Wired/Bluetooth screen, on a different sample, or
 * on no screen at all.
 *
 * <h3>Two sources, one state</h3>
 * <ul>
 *   <li><b>SDK</b> — {@link OnConnectionStateChangedListener} fires whenever the RF88
 *       SDK transitions state. This is the single source of truth published as
 *       {@link #connectState}; every screen that needs to know "am I connected?"
 *       observes this LiveData.</li>
 *   <li><b>Wire</b> — the OEM {@code pm.ex.gpio.changed} broadcast tells us when the
 *       reader is attached to or detached from the sled. Detach always disconnects so
 *       the SDK matches physical reality. Attach updates {@link #sledAttached} but only
 *       triggers a connect when the Wired screen is in the foreground (see
 *       {@link #setWireScreenActive(boolean)}) — the project rule is "connection happens
 *       inside the connection screen", so attach on Main / Inventory / etc. does
 *       <b>not</b> auto-connect.</li>
 * </ul>
 *
 * <p>Bluetooth-link-loss ({@code ACTION_ACL_DISCONNECTED}) is intentionally NOT handled
 * here: detaching the sled also fires that broadcast as a side effect, and reacting to
 * it in addition to the GPIO broadcast double-issues {@code disconnect()} in the same
 * detach burst, which leaves the SDK stuck in {@code CONNECTING}. The Bluetooth screen
 * handles its own ACL drops while it is open.</p>
 *
 * <p>State updates from the SDK callback are routed through the main looper using
 * {@code setValue} (not {@code postValue}) so every transition is delivered to observers
 * in the order it arrived. {@code postValue} would coalesce rapid back-to-back updates
 * (e.g. CONNECTING immediately followed by CONNECTED), causing transient states to
 * disappear from the UI.
 */
public class Rf88ConnectionRepository implements OnConnectionStateChangedListener {

    // ── Wire constants ──────────────────────────────────────────────────────────────
    /** Value of {@code acc_det} when the cable is currently attached. */
    private static final String ATTACH = "1";
    /** Value of {@code acc_det} when the cable is currently detached. */
    private static final String DETACH = "0";
    /** OEM broadcast emitted by the platform whenever the expansion accessory state changes. */
    private static final String ACTION_DEVICE_CHANGED = "pm.ex.gpio.changed";
    /** String extra on the broadcast — "1" for attached, "0" for detached. */
    private static final String EXTRA_CONNECT_STATE = "acc_det";

    private final MutableLiveData<DeviceConnectionState> mutableConnectState =
            new MutableLiveData<>(DeviceConnectionState.DISCONNECTED);
    private final MutableLiveData<Boolean> mutableSledAttached = new MutableLiveData<>();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final Control control = Control.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver receiver = new ConnectionReceiver();

    /** Single source of truth for "is RF88 connected?". Observed by every screen. */
    public final LiveData<DeviceConnectionState> connectState = mutableConnectState;

    /**
     * Whether the reader is currently mounted on the sled. {@code null} means "unknown" —
     * either the GPIO probe has not completed yet, or the host hardware does not expose
     * the auto-detect line (see {@link #isAutoDetectSupported()}). Screens that gate UI
     * on sled state should treat {@code null} as "do not gate" so PM90 users are not
     * locked out of any connection method.
     */
    public final LiveData<Boolean> sledAttached = mutableSledAttached;

    /**
     * When {@code true}, an ATTACH broadcast triggers an automatic wire connect. Only
     * the Wired screen flips this on (in {@code onResume}) so connection always happens
     * "inside the connection screen" — sled attach on Main, Inventory, etc. simply
     * updates {@link #sledAttached} without forcing a connect. Detach still disconnects
     * unconditionally so the SDK stays consistent with physical reality.
     */
    private volatile boolean wireScreenActive = false;

    /**
     * Whether the host device exposes the GPIO line we use to auto-detect the sled cable.
     * Constant for the lifetime of the app.
     */
    public boolean isAutoDetectSupported() {
        return !Build.MODEL.contains("PM90");
    }

    /**
     * Wire up every connection source. Call once from {@link RFIDSampleV2#onCreate()}.
     *
     * @param context the {@link android.app.Application} context — used to register the
     *                broadcast receiver with app-wide lifetime
     */
    public void start(Context context) {
        rf88Manager.setOnConnectionStateChangedListener(this);
        ContextCompat.registerReceiver(context, receiver, getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        probeCableOnLaunch();
    }

    /**
     * SDK callback — every connection state transition lands here. Hopped to the main
     * looper and published via {@code setValue} (not {@code postValue}) so observers
     * see every transient state in arrival order; see the class header for why
     * coalescing would be wrong.
     */
    @Override
    public void onConnectionStateChanged(@NonNull DeviceConnectionState state) {
        mainHandler.post(() -> mutableConnectState.setValue(state));
    }

    /**
     * Toggle the "auto-connect on sled attach" gate. Call from
     * {@code WiredActivity#onResume} with {@code true} and {@code onPause} with {@code false}.
     */
    public void setWireScreenActive(boolean active) {
        wireScreenActive = active;
    }

    /**
     * Probe the GPIO line once at app startup so {@link #sledAttached} starts with the
     * real value instead of {@code null}. Does NOT trigger an auto-connect — connection
     * is intentionally left to the Wired screen ({@code WiredActivity}). Skipped on
     * hardware without the GPIO line — see {@link #isAutoDetectSupported()}.
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
     * Translates the sled attach/detach broadcast into RF88 SDK calls.
     *
     * <p>Broadcast receivers run on the main thread, so SDK calls are dispatched on the
     * background executor — a slow {@code connect}/{@code disconnect} would otherwise
     * block the main thread and trigger an ANR.
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
