package device.apps.rfidsamplev2;

import android.app.Application;

import ex.dev.sdk.rf88.Rf88Manager;

/**
 * Process-scope entry point for the sample app.
 *
 * <p>Two responsibilities, both run once on app launch:
 * <ol>
 *     <li>Enable the RF88 SDK's debug logging via {@link Rf88Manager#setDebugMode(boolean)}
 *         so customer-developers can trace SDK-internal events out of the box.</li>
 *     <li>Create the app-scoped {@link Rf88ConnectionRepository} and {@code start()} it.
 *         The repository owns the SDK connection-state callback and the OEM
 *         cable-attach broadcast for the entire process lifetime — this is what lets
 *         sled attach/detach work on every screen, including no screen at all.</li>
 * </ol>
 *
 * <p>Activities reach the repository via {@code ((RFIDSampleV2) getApplication()).getConnectionRepository()}.
 */
public class RFIDSampleV2 extends Application {

    private Rf88ConnectionRepository connectionRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        Rf88Manager.setDebugMode(true);
        connectionRepository = new Rf88ConnectionRepository();
        connectionRepository.start(this);
    }

    public Rf88ConnectionRepository getConnectionRepository() {
        return connectionRepository;
    }
}
