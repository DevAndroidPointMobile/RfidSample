package device.apps.rfidsamplev2.sample.wired;

import android.os.RemoteException;

import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.sdk.Control;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * State holder for {@link WiredActivity} — exposes the manual Connect / Disconnect
 * actions for the buttons on the screen.
 *
 * <p>The cable-attach broadcast and the GPIO probe live in {@link Rf88ConnectionManager}
 * (app-scoped) so they continue to drive {@code connect}/{@code disconnect} no matter
 * which screen is on top — including no screen at all. This ViewModel only needs to
 * cover the manual-tap path, which matters chiefly on hardware without GPIO auto-detect
 * (e.g. PM90).
 */
public class WireViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();
    private final Control control = Control.getInstance();

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
     * Auto-connect once on screen entry when the sled is already attached. The GPIO
     * probe ({@link Control#getExpansionAccDetGpio()}) is a Binder call and the SDK
     * {@code connect} is a synchronous call — both must run off the main thread, so the
     * whole sequence is dispatched on the background executor. A RemoteException from
     * the probe (e.g. on hardware without the auto-detect GPIO line such as PM90) is
     * swallowed: in that case the user just taps Connect manually.
     */
    public void autoConnectIfAttached() {
        executorService.execute(() -> {
            try {
                if ("1".equals(control.getExpansionAccDetGpio()))
                    rf88Manager.connect();

            } catch (RemoteException ignored) {
                // GPIO probe unavailable — auto-connect skipped, manual Connect still works.
            }
        });
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
