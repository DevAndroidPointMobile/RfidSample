package device.apps.rfidsamplev2;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import device.apps.rfidsamplev2.dispatcher.AppConnectionListener;
import device.apps.rfidsamplev2.dispatcher.ConnectionDispatcher;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public abstract class BaseActivity extends AppCompatActivity {

    private AlertDialog progressDialog;
    private AlertDialog alertDialog;

    private final AppConnectionListener connectionListener = state -> {
        onConnectionStateChanged(state);

        switch (state) {
            case CONNECTING:
                showProgressDialog("Connecting to Bluetooth device...");
                break;

            case CONNECTED:
            case DISCONNECTED:
                dismissAllDialogs();
                break;

            case DISCONNECTING:
                showProgressDialog("Disconnecting Bluetooth device...");
                break;

            case SLEEP:
                showAlertDialog(
                        "Sleep Mode",
                        "The device is in sleep mode.\nOperation is not available."
                );
                break;

            case FAILURE:
                showAlertDialog(
                        "Connection Failed",
                        "Failed to connect to Bluetooth device.\nPlease try again."
                );
                break;
        }
    };

    protected void onConnectionStateChanged(DeviceConnectionState state) {
        // Optional override
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectionDispatcher dispatcher = ConnectionDispatcher.getInstance();
        if (dispatcher != null) {
            dispatcher.registerAppListener(connectionListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ConnectionDispatcher dispatcher = ConnectionDispatcher.getInstance();
        if (dispatcher != null) {
            dispatcher.unregisterAppListener(connectionListener);
        }
        dismissAllDialogs();
    }

    private void dismissAllDialogs() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    /**
     * Shows a custom Material progress dialog.
     */
    private void showProgressDialog(String message) {
        dismissAllDialogs();

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_progress, null);

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        tvMessage.setText(message);

        progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }

    /**
     * Shows a custom Material alert dialog.
     */
    private void showAlertDialog(String title, String message) {
        dismissAllDialogs();

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_alert, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        View btnOk = view.findViewById(R.id.btnOk);

        tvTitle.setText(title);
        tvMessage.setText(message);

        alertDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        btnOk.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }
}