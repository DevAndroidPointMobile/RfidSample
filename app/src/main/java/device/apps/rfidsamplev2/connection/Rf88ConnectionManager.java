package device.apps.rfidsamplev2.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.sdk.Control;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;
import ex.dev.sdk.rf88.frameworks.listener.OnConnectionStateChangedListener;

/**
 * App-scoped owner of the RF88 connection state. Held by {@link RFIDSampleV2} and
 * started once from {@code onCreate()}, so the receivers live for the entire process —
 * sled attach/detach work whether the user is on the Wired/Bluetooth screen, on a
 * different sample, or on no screen at all.
 *
 * <h3>Two state sources, one stream</h3>
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
 *
 * <h3>Derived hero-card labels</h3>
 * <p>{@link #statusTitle} and {@link #statusSubtitle} are
 * {@link Transformations#map} derivations of {@link #connectState} that publish the
 * canonical hero-card copy for each SDK state ("Connected" / "Connecting..." /
 * "Sleeping" / "Not Connected"). Centralising the label mapping here means every screen
 * with a hero card observes the same strings, so the same SDK state never reads as
 * different copy across the app. Screens with non-SDK preconditions
 * ({@code BluetoothActivity}, {@code NfcActivity}: radio off / unsupported) gate on
 * those preconditions first and only fall through to {@code statusTitle.getValue()}
 * when their gate passes.
 *
 * <h3>What is NOT here</h3>
 * <p>This class is deliberately UI-free — no {@code Activity}, {@code AlertDialog},
 * or {@link android.app.Application.ActivityLifecycleCallbacks} imports. Two related
 * pieces live in their own files so each has a single responsibility:
 * <ul>
 *     <li>{@link SleepBlockingDialogController} — the app-wide modal shown while the
 *         SDK reports {@link DeviceConnectionState#SLEEP}. Pure consumer of
 *         {@link #connectState}, removable in one line from {@link RFIDSampleV2}.</li>
 *     <li>{@code RFIDSampleV2.AppExitDetector} — counts live Activities so
 *         {@link #dispose()} fires once on real app exit (not on configuration change).</li>
 * </ul>
 */
public class Rf88ConnectionManager implements OnConnectionStateChangedListener {

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
    private final MutableLiveData<Boolean> mutableIsLost = new MutableLiveData<>(false);
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final Control control = Control.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver receiver = new ConnectionReceiver();

    /**
     * Tracks whether the reader entered SLEEP without an intervening CONNECTED. The
     * SDK fires a transient {@link DeviceConnectionState#DISCONNECTED} during the wake
     * transition (SLEEP → DISCONNECTED → CONNECTED); this flag lets {@link #isLost}
     * suppress that transient so feature screens do not {@code finish()} on a sleep
     * recovery they are about to keep using.
     */
    private boolean wasSleeping = false;

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
     * Headline string for the hero card on every screen, derived reactively from
     * {@link #connectState} via {@link Transformations#map}. Returns "Connected" /
     * "Connecting..." / "Sleeping" / "Not Connected" matching the current SDK state.
     *
     * <p>Screens whose hero card is driven purely by SDK state observe this LiveData
     * directly. Screens with their own preconditions
     * (e.g. {@code BluetoothActivity}: Bluetooth off / unsupported,
     * {@code NfcActivity}: NFC off / unsupported) check their preconditions first and
     * only read {@code statusTitle.getValue()} when those preconditions pass — the SDK
     * state should never be reported in copy when the underlying radio is unavailable.
     */
    public final LiveData<String> statusTitle =
            Transformations.map(connectState, Rf88ConnectionManager::titleFor);

    /**
     * Helper text shown under {@link #statusTitle}, derived from {@link #connectState}.
     * Same per-state values across all screens; same precondition rules as
     * {@link #statusTitle}.
     */
    public final LiveData<String> statusSubtitle =
            Transformations.map(connectState, Rf88ConnectionManager::subtitleFor);

    /**
     * "The reader connection is genuinely lost" — the right signal for feature screens
     * (Inventory, InventoryNread, Configuration, Barcode) that should {@code finish()}
     * when the device drops, rather than observing
     * {@code connectState == DISCONNECTED} directly.
     *
     * <p>Why a separate signal: the RF88 SDK fires a transient {@code DISCONNECTED}
     * around SLEEP transitions (typically SLEEP → DISCONNECTED → CONNECTED on wake).
     * An Activity that finishes on the raw {@code DISCONNECTED} would close itself in
     * the middle of a sleep recovery the user expects to come back from. This LiveData
     * uses {@link #wasSleeping} to swallow the transient {@code DISCONNECTED} during a
     * sleep round-trip, emitting {@code true} only when the disconnect is real.
     *
     * <p>Screens that just want to react to connection state for UI (e.g. Main's hero
     * card) should keep observing {@link #connectState} directly — {@code isLost} is
     * specifically the "should I leave this screen?" signal.
     */
    public final LiveData<Boolean> isLost = mutableIsLost;

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
     * Tear down the connection on the way out of the process. Routed through
     * {@link #executorService} because Rf88 SDK 3.1.0+ rejects synchronous calls on
     * the main thread. Intended to be called from {@link RFIDSampleV2}'s app-wide
     * lifecycle hook when the last Activity is destroyed and the process is going
     * away — leaves the reader cleanly disconnected so the next launch starts fresh.
     */
    public void dispose() {
        executorService.execute(rf88Manager::disconnect);
    }

    /**
     * SDK callback — every connection state transition lands here. Hopped to the main
     * looper and published via {@code setValue} (not {@code postValue}) so observers
     * see every transient state in arrival order; see the class header for why
     * coalescing would be wrong.
     */
    @Override
    public void onConnectionStateChanged(@Nullable DeviceConnectionState state) {
        // The SDK can deliver null on its callback contract; treat it as a no-op so we
        // don't push spurious nulls through connectState or perturb the wasSleeping /
        // isLost state machines.
        if (state == null) return;
        mainHandler.post(() -> {
            mutableConnectState.setValue(state);
            if (state == DeviceConnectionState.SLEEP) wasSleeping = true;
            else if (state == DeviceConnectionState.CONNECTED) wasSleeping = false;
            // Suppress the transient DISCONNECTED that the SDK emits during a sleep
            // round-trip; only treat real disconnects as "lost".
            final boolean lost = state == DeviceConnectionState.DISCONNECTED && !wasSleeping;
            if (!Objects.equals(lost, mutableIsLost.getValue())) mutableIsLost.setValue(lost);
        });
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

    /** Per-state headline copy used by {@link #statusTitle}. */
    private static String titleFor(DeviceConnectionState state) {
        if (state == null) return "Not Connected";
        switch (state) {
            case CONNECTED:  return "Connected";
            case CONNECTING: return "Connecting...";
            case SLEEP:      return "Sleeping";
            default:         return "Not Connected";
        }
    }

    /** Per-state subtitle copy used by {@link #statusSubtitle}. */
    private static String subtitleFor(DeviceConnectionState state) {
        if (state == null) return "Connect your RF88 reader to begin";
        switch (state) {
            case CONNECTED:  return "Your RF88 device is ready";
            case CONNECTING: return "Establishing connection";
            case SLEEP:      return "Press the trigger key to wake the reader";
            default:         return "Connect your RF88 reader to begin";
        }
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
