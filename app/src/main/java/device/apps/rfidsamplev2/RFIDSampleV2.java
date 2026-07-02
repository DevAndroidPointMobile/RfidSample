package device.apps.rfidsamplev2;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import device.apps.rfidsamplev2.connection.DeviceSdk;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.connection.SleepBlockingDialogController;
import device.apps.rfidsamplev2.connection.WiredAttachDetector;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * Process-scope entry point for the sample app.
 *
 * <p>Responsibilities, all run once on app launch:
 * <ol>
 *     <li>Enable the RF88 SDK's debug logging via {@link Rf88Manager#setDebugMode(boolean)}
 *         so customer-developers can trace SDK-internal events out of the box.</li>
 *     <li>Create the app-scoped {@link Rf88ConnectionManager} and {@code start()} it.
 *         The manager owns the SDK connection-state callback for the entire process
 *         lifetime — this is what lets connection state be observed on every screen,
 *         including no screen at all.</li>
 *     <li>Create the {@link WiredAttachDetector} — but only when {@link DeviceSdk#isAvailable()},
 *         because it depends on the proprietary {@code device.sdk} framework that is absent
 *         on stock Android (a Galaxy phone). On such hardware the detector stays {@code null}
 *         and the app runs its universal Bluetooth / NFC features normally instead of
 *         crashing at launch. When present it owns the OEM cable-attach broadcast for the
 *         whole process, so sled attach / detach works on every screen.</li>
 *     <li>Attach the {@link SleepBlockingDialogController} to the manager's
 *         connection-state stream. The controller is a separate file by design — it
 *         is a pure UI consumer of {@link Rf88ConnectionManager#connectState}, so
 *         removing or restyling the SLEEP modal is a one-line change here that does
 *         not touch the connection-state machinery. Comment out the
 *         {@code attach(this)} call to disable the modal entirely.</li>
 * </ol>
 *
 * <p>An {@link android.app.Application.ActivityLifecycleCallbacks} also tracks the live
 * Activity count so {@link Rf88ConnectionManager#dispose()} fires once when the
 * user fully exits the app — leaving the reader disconnected on process teardown
 * regardless of which sample screen they were on.
 *
 * <p>Activities reach the manager via {@code ((RFIDSampleV2) getApplication()).getConnectionManager()}.
 */
public class RFIDSampleV2 extends Application {

    private Rf88ConnectionManager connectionManager;

    /**
     * Owner of the proprietary cabled-sled flow. {@code null} on non-Point-Mobile hardware
     * (e.g. a Galaxy phone) where the backing {@code device.sdk} framework is absent — see
     * {@link DeviceSdk}. Consumers (the Wired screen) must null-check before use.
     */
    @Nullable
    private WiredAttachDetector wiredAttachDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        Rf88Manager.setDebugMode(true);
        connectionManager = new Rf88ConnectionManager();
        connectionManager.start();
        new SleepBlockingDialogController(connectionManager.connectState).attach(this);

        // The cabled-sled auto-detect flow depends on the proprietary device.sdk framework,
        // which does not exist on stock Android. Create it only where that framework is
        // present so the app still launches — and Bluetooth / NFC still work — on a Galaxy.
        if (DeviceSdk.isAvailable()) {
            wiredAttachDetector = new WiredAttachDetector();
            wiredAttachDetector.start(this);
        }

        registerActivityLifecycleCallbacks(new AppExitDetector());
    }

    public Rf88ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * @return the cabled-sled detector, or {@code null} on hardware without the proprietary
     *         {@code device.sdk} framework. Callers must null-check.
     */
    @Nullable
    public WiredAttachDetector getWiredAttachDetector() {
        return wiredAttachDetector;
    }

    /**
     * Counts live Activities so the app can detect when the user has fully exited
     * (last Activity destroyed without a configuration change) and disconnect the
     * reader before the process is torn down. Sample-to-sample navigation does not
     * trip this — only an actual exit does.
     */
    private final class AppExitDetector implements ActivityLifecycleCallbacks {

        private int liveActivities = 0;

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            liveActivities++;
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            // A configuration change destroys and recreates Activities; that is not an exit.
            if (activity.isChangingConfigurations()) return;
            if (--liveActivities == 0) connectionManager.dispose();
        }

        @Override public void onActivityStarted(@NonNull Activity activity) {}
        @Override public void onActivityResumed(@NonNull Activity activity) {}
        @Override public void onActivityPaused(@NonNull Activity activity) {}
        @Override public void onActivityStopped(@NonNull Activity activity) {}
        @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    }
}
