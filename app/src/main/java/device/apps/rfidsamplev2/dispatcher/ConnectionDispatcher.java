package device.apps.rfidsamplev2.dispatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;
import ex.dev.sdk.rf88.frameworks.listener.OnConnectionStateChangedListener;

public class ConnectionDispatcher extends BroadcastReceiver implements OnConnectionStateChangedListener {

    private static volatile Context CONTEXT;
    private static volatile ConnectionDispatcher INSTANCE;

    private volatile DeviceConnectionState lastKnownState = DeviceConnectionState.DISCONNECTED;

    private static final String ACTION_GPIO_CHANGED = "pm.ex.gpio.changed";
    private static final String EXTRA_ACC_DET = "acc_det";

    // Thread-safe set of registered app listeners
    private final Set<AppConnectionListener> appListeners = Collections.synchronizedSet(new HashSet<>());

    // Handler for posting to main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // SDK controller (dispatcher registers itself once)
    private final Rf88Manager mManager = Rf88Manager.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_GPIO_CHANGED.equals(intent.getAction())) {
            String accDet = intent.getStringExtra(EXTRA_ACC_DET);
            if ("0".equals(accDet)) {
                lastKnownState = DeviceConnectionState.DISCONNECTED;
                mainHandler.post(() -> {
                    AppConnectionListener[] arr;
                    synchronized (appListeners) {
                        arr = appListeners.toArray(new AppConnectionListener[0]);
                    }
                    for (AppConnectionListener l : arr) {
                        try {
                            l.onAppConnectionStateChanged(DeviceConnectionState.DISCONNECTED);

                        } catch (Throwable t) {
                            // Guard: one bad listener must not break others
                            t.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private ConnectionDispatcher() {
        mManager.setOnConnectionStateChangedListener(this);
    }

    public static void initialize(Context context) {
        if (context == null)
            return;

        // 먼저 인스턴스 생성 보장
        synchronized (ConnectionDispatcher.class) {
            if (CONTEXT == null) {
                CONTEXT = context.getApplicationContext();
                INSTANCE = new ConnectionDispatcher();
            }
        }

        // 이미 생성된 인스턴스로 작업
        try {
            IntentFilter filter = new IntentFilter(ACTION_GPIO_CHANGED);
            CONTEXT.registerReceiver(INSTANCE, filter, Context.RECEIVER_EXPORTED);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void release() {
        if (CONTEXT != null && INSTANCE != null) {
            CONTEXT.unregisterReceiver(INSTANCE);
        }
    }

    public static ConnectionDispatcher getInstance() {
        return INSTANCE;  // null일 수 있음을 호출자가 체크
    }

    /**
     * Register an application-level listener.
     * Recommended to call in Activity.onStart()
     */
    public void registerAppListener(@NonNull AppConnectionListener l) {
        appListeners.add(l);
        mainHandler.post(() -> l.onAppConnectionStateChanged(lastKnownState));
    }

    /**
     * Unregister an application-level listener.
     * Recommended to call in Activity.onStop()
     */
    public void unregisterAppListener(@NonNull AppConnectionListener l) {
        appListeners.remove(l);
    }

    /**
     * SDK callback. Convert to app-level callbacks on main thread.
     */
    @Override
    public void onConnectionStateChanged(@NonNull final DeviceConnectionState state) {
        this.lastKnownState = state; // 저장!
        mainHandler.post(() -> {
            AppConnectionListener[] arr;
            synchronized (appListeners) {
                arr = appListeners.toArray(new AppConnectionListener[0]);
            }
            for (AppConnectionListener l : arr) {
                try {
                    l.onAppConnectionStateChanged(state);
                } catch (Throwable t) {
                    // Guard: one bad listener must not break others
                    t.printStackTrace();
                }
            }
        });
    }
}