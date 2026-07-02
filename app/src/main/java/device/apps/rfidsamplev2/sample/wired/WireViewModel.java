package device.apps.rfidsamplev2.sample.wired;

import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.connection.WiredAttachDetector;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * State holder for {@link WiredActivity} — exposes the manual Connect / Disconnect
 * actions for the buttons on the screen.
 *
 * <p>Depends only on the RF88 SDK, so it carries no proprietary {@code device.sdk}
 * reference. The cable-attach broadcast, the GPIO probe, and the one-shot
 * auto-connect-on-entry all live in the app-scoped {@link WiredAttachDetector} (which is
 * the sole owner of {@code device.sdk} at connection scope). This ViewModel only covers
 * the manual-tap path, which matters chiefly on hardware without GPIO auto-detect
 * (e.g. PM90). Screens learn the result by observing
 * {@link Rf88ConnectionManager#connectState}.
 */
public class WireViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

    /**
     * Connect the RF88 SDK over the cable. The SDK call runs on a background thread; the
     * Activity learns about the result by observing
     * {@link Rf88ConnectionManager#connectState}.
     */
    public void connect() {
        executorService.execute(rf88Manager::connect);
    }

    /**
     * Disconnect the RF88 SDK from the cabled device.
     */
    public void disconnect() {
        executorService.execute(rf88Manager::disconnect);
    }

    /**
     * Shut down the background executor when the ViewModel is finally cleared by the framework
     * (e.g. when the host Activity finishes for good).
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
