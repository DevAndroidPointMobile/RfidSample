package device.apps.rfidsamplev2.connection;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.RFIDSampleV2;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;
import ex.dev.sdk.rf88.frameworks.listener.OnConnectionStateChangedListener;

/**
 * App-scoped owner of the RF88 connection state. Held by {@link RFIDSampleV2} and
 * started once from {@code onCreate()}, so the SDK connection-state callback lives for
 * the entire process — every screen that needs to know "am I connected?" observes the
 * same stream whether it is on top or not.
 *
 * <h3>Universal by design — no proprietary dependency</h3>
 * <p>This class depends only on the RF88 SDK (bundled in the APK) and Android APIs, so it
 * runs on any device — including stock Android such as a Galaxy phone. The cabled-sled
 * flow, which is the app's only connection-scope dependency on the proprietary
 * {@code device.sdk} framework (the GPIO probe and the {@code pm.ex.gpio.changed}
 * broadcast), lives in {@link WiredAttachDetector} and is created by {@link RFIDSampleV2}
 * only when {@link DeviceSdk#isAvailable()}. Keeping that reference out of this class is
 * what lets the app launch and run Bluetooth / NFC on non-Point-Mobile hardware.
 *
 * <h3>Single source of truth</h3>
 * <p>{@link OnConnectionStateChangedListener} fires whenever the RF88 SDK transitions
 * state; that value is published as {@link #connectState}. State updates from the SDK
 * callback are routed through the main looper using {@code setValue} (not
 * {@code postValue}) so every transition is delivered to observers in the order it
 * arrived. {@code postValue} would coalesce rapid back-to-back updates (e.g. CONNECTING
 * immediately followed by CONNECTED), causing transient states to disappear from the UI.
 *
 * <h3>Derived hero-card labels</h3>
 * <p>{@link #statusTitle} and {@link #statusSubtitle} are {@link Transformations#map}
 * derivations of {@link #connectState} that publish the canonical hero-card copy for each
 * SDK state ("Connected" / "Connecting..." / "Sleeping" / "Not Connected"). Centralising
 * the label mapping here means every screen with a hero card observes the same strings.
 * Screens with non-SDK preconditions ({@code BluetoothActivity}, {@code NfcActivity}:
 * radio off / unsupported) gate on those preconditions first and, when their gate passes,
 * map {@link #connectState}'s current value through
 * {@link #titleForState(DeviceConnectionState)} /
 * {@link #subtitleForState(DeviceConnectionState)}. They must NOT read
 * {@code statusTitle.getValue()}: it is a lazy {@link Transformations#map} that only
 * recomputes while actively observed, so a pull-based {@code getValue()} returns
 * {@code null}.
 *
 * <h3>What is NOT here</h3>
 * <p>This class is deliberately UI-free — no {@code Activity}, {@code AlertDialog}, or
 * {@link android.app.Application.ActivityLifecycleCallbacks} imports. Related pieces live
 * in their own files so each has a single responsibility:
 * <ul>
 *     <li>{@link WiredAttachDetector} — the proprietary cabled-sled attach/detach flow.</li>
 *     <li>{@link SleepBlockingDialogController} — the app-wide modal shown while the SDK
 *         reports {@link DeviceConnectionState#SLEEP}. Pure consumer of
 *         {@link #connectState}, removable in one line from {@link RFIDSampleV2}.</li>
 *     <li>{@code RFIDSampleV2.AppExitDetector} — counts live Activities so
 *         {@link #dispose()} fires once on real app exit (not on configuration change).</li>
 * </ul>
 */
public class Rf88ConnectionManager implements OnConnectionStateChangedListener {

    private final MutableLiveData<DeviceConnectionState> mutableConnectState =
            new MutableLiveData<>(DeviceConnectionState.DISCONNECTED);
    private final MutableLiveData<Boolean> mutableIsLost = new MutableLiveData<>(false);
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
     * Headline string for the hero card on every screen, derived reactively from
     * {@link #connectState} via {@link Transformations#map}. Returns "Connected" /
     * "Connecting..." / "Sleeping" / "Not Connected" matching the current SDK state.
     *
     * <p>Screens whose hero card is driven purely by SDK state observe this LiveData
     * directly. Screens with their own preconditions
     * (e.g. {@code BluetoothActivity}: Bluetooth off / unsupported,
     * {@code NfcActivity}: NFC off / unsupported) check their preconditions first and,
     * when those pass, derive the label from {@link #connectState}'s value via
     * {@link #titleForState(DeviceConnectionState)} rather than reading this lazy
     * LiveData's {@code getValue()} — the SDK state should never be reported in copy when
     * the underlying radio is unavailable, and an unobserved {@code map} yields {@code null}.
     */
    public final LiveData<String> statusTitle =
            Transformations.map(connectState, Rf88ConnectionManager::titleForState);

    /**
     * Helper text shown under {@link #statusTitle}, derived from {@link #connectState}.
     * Same per-state values across all screens; same precondition rules as
     * {@link #statusTitle}.
     */
    public final LiveData<String> statusSubtitle =
            Transformations.map(connectState, Rf88ConnectionManager::subtitleForState);

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
     * Wire up the SDK connection-state callback. Call once from
     * {@link RFIDSampleV2#onCreate()}.
     */
    public void start() {
        rf88Manager.setOnConnectionStateChangedListener(this);
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
     * Per-state headline copy — the same mapping that backs {@link #statusTitle}, exposed
     * as a pure function so screens that gate on their own preconditions can read the label
     * imperatively from {@link #connectState}'s current value instead of observing
     * {@code statusTitle}. {@code statusTitle} is a {@link Transformations#map} derivation,
     * which only recomputes while it has an active observer; reading its {@code getValue()}
     * without observing it returns {@code null}. Pull-based callers must use this function.
     */
    public static String titleForState(DeviceConnectionState state) {
        if (state == null) return "Not Connected";
        switch (state) {
            case CONNECTED:  return "Connected";
            case CONNECTING: return "Connecting...";
            case SLEEP:      return "Sleeping";
            default:         return "Not Connected";
        }
    }

    /**
     * Per-state subtitle copy — pure-function counterpart to {@link #statusSubtitle}, with
     * the same pull-based usage contract as {@link #titleForState(DeviceConnectionState)}.
     */
    public static String subtitleForState(DeviceConnectionState state) {
        if (state == null) return "Connect your RF88 reader to begin";
        switch (state) {
            case CONNECTED:  return "Your RF88 device is ready";
            case CONNECTING: return "Establishing connection";
            case SLEEP:      return "Press the trigger key to wake the reader";
            default:         return "Connect your RF88 reader to begin";
        }
    }
}
