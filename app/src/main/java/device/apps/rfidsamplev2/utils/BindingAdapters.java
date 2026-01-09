package device.apps.rfidsamplev2.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.databinding.BindingAdapter;

import com.google.android.material.button.MaterialButton;

public class BindingAdapters {

    @BindingAdapter("app:discoveryState")
    public static void setDiscoveryState(MaterialButton view, Boolean isDiscovery) {
        if (isDiscovery == null)
            isDiscovery = false;

        if (isDiscovery) {
            view.setText("Stop Discovery");
            view.setBackgroundColor(Color.parseColor("#C62828")); // Red
        } else {
            view.setText("Start Discovery");
            view.setBackgroundColor(Color.parseColor("#5E35B1")); // Purple
        }
    }

    @BindingAdapter("app:connectionState")
    public static void setConnectionState(View view, Boolean isConnected) {
        if (isConnected == null)
            isConnected = false;

        if (isConnected) {
            view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
        } else {
            view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Red
        }
    }

    @BindingAdapter("app:discoveryProgress")
    public static void setDiscoveryProgress(ProgressBar view, Boolean isDiscovery) {
        if (isDiscovery == null)
            isDiscovery = false;

        view.setVisibility(isDiscovery ? View.VISIBLE : View.GONE);
    }
}