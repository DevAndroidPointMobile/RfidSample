package device.apps.rfidsamplev2.connection;

/**
 * One-shot runtime probe for Point Mobile's proprietary {@code device.sdk} framework.
 *
 * <p>{@code device.sdk} is a {@code compileOnly} dependency (see the module build script):
 * it is supplied by the platform framework on Point Mobile hardware and is <b>absent at
 * runtime on any other Android device</b> (e.g. a Galaxy phone). Referencing a
 * {@code device.sdk} class where it is missing throws {@link NoClassDefFoundError}, so
 * every proprietary feature (Wired cable auto-detect, Barcode scanner delegation) must
 * gate on {@link #isAvailable()} before touching one — otherwise a single eager reference
 * during app startup takes down the whole process, including the universal Bluetooth / NFC
 * features that would run fine on stock Android.
 *
 * <p>The probe loads the class without initializing it, so it never triggers SDK side
 * effects, and caches the result for the process lifetime because framework availability
 * cannot change while the app is running.
 */
public final class DeviceSdk {

    /** Canonical class we treat as the presence marker for the whole {@code device.sdk} library. */
    private static final String MARKER_CLASS = "device.sdk.Control";

    private static final boolean AVAILABLE = probe();

    private DeviceSdk() {
    }

    /**
     * @return {@code true} when the proprietary {@code device.sdk} framework is present at
     *         runtime (Point Mobile hardware); {@code false} on stock Android.
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static boolean probe() {
        try {
            Class.forName(MARKER_CLASS, false, DeviceSdk.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            // ClassNotFoundException on non-PM hardware, or any linkage error — either way
            // the proprietary SDK is unusable, so callers must fall back gracefully.
            return false;
        }
    }
}
