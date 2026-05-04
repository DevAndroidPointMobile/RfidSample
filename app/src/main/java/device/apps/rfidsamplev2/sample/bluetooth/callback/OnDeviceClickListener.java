package device.apps.rfidsamplev2.sample.bluetooth.callback;

import android.bluetooth.BluetoothDevice;

/**
 * Click listener for rows in the discovered-devices list.
 *
 * <p>{@code BluetoothActivity} implements this interface and the row layout invokes it from
 * the data-bound {@code android:onClick} expression. Keeping the contract one method wide makes
 * the path from "user taps a row" to "Activity initiates a connect" trivial to follow.
 */
public interface OnDeviceClickListener {

    /**
     * Called when the user taps a discovered device row.
     *
     * @param item the {@link BluetoothDevice} represented by the tapped row
     */
    void onDeviceClicked(BluetoothDevice item);

}
