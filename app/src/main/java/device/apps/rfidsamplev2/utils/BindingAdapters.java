package device.apps.rfidsamplev2.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.databinding.BindingAdapter;

import com.google.android.material.button.MaterialButton;

public class BindingAdapters {

    // Discovery 버튼 상태 변경
    @BindingAdapter("discoveryState")
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

    // 연결 상태 표시등 (activity_bluetooth에서 사용)
    @BindingAdapter("connectionState")
    public static void setConnectionState(View view, Boolean isConnected) {
        if (isConnected == null)
            isConnected = false;

        if (isConnected) {
            view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
        } else {
            view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))); // Red
        }
    }

    // 연결 버튼 상태 (MaterialButton용 - activity_wired에서 사용)
    @BindingAdapter("connectState")
    public static void setConnectState(MaterialButton view, Boolean isConnected) {
        if (isConnected == null)
            isConnected = false;

        if (isConnected) {
            view.setText("Disconnect");
            view.setBackgroundColor(Color.parseColor("#C62828")); // Red
            view.setIcon(view.getContext().getDrawable(android.R.drawable.ic_delete));
        } else {
            view.setText("Connect");
            view.setBackgroundColor(Color.parseColor("#5E35B1")); // Purple
            view.setIcon(view.getContext().getDrawable(android.R.drawable.ic_input_add));
        }
    }

    // 연결 버튼 상태 (일반 Button용 - 레거시)
    @BindingAdapter("connectState")
    public static void setConnectState(Button view, Boolean isConnected) {
        if (isConnected == null)
            isConnected = false;

        view.setEnabled(true);

        if (isConnected) {
            view.setText("DISCONNECT");
            view.setBackgroundColor(Color.parseColor("#C62828")); // Red
        } else {
            view.setText("CONNECT");
            view.setBackgroundColor(Color.parseColor("#5E35B1")); // Purple
        }
    }

    // Discovery 진행 표시
    @BindingAdapter("discoveryProgress")
    public static void setDiscoveryProgress(ProgressBar view, Boolean isDiscovery) {
        if (isDiscovery == null)
            isDiscovery = false;

        view.setVisibility(isDiscovery ? View.VISIBLE : View.GONE);
    }
}