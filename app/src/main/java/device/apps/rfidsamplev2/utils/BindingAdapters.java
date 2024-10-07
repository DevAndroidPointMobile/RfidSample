package device.apps.rfidsamplev2.utils;

import android.content.Context;
import android.graphics.Color;
import android.widget.Button;

import androidx.databinding.BindingAdapter;

public class BindingAdapters {

    @BindingAdapter("app:discoveryState")
    public static void setDiscoveryState(Button view, Boolean isDiscovery) {
        final Context context = view.getContext();
        if (isDiscovery == null)
            isDiscovery = false;

        if (isDiscovery) {
            view.setText("STOP");
            view.setBackgroundColor(Color.parseColor("#000000"));

        } else {
            view.setText("START");
            view.setBackgroundColor(Color.parseColor("#000000"));
        }
    }

    @BindingAdapter("app:connectState")
    public static void setConnectionState(Button view, Boolean isConnected) {
        final Context context = view.getContext();
        if (isConnected == null)
            isConnected = false;

        if (isConnected) {
            view.setText("DISCONNECT");
            view.setBackgroundColor(Color.parseColor("#000000"));

        } else {
            view.setText("CONNECT");
            view.setBackgroundColor(Color.parseColor("#000000"));
        }
    }
}
