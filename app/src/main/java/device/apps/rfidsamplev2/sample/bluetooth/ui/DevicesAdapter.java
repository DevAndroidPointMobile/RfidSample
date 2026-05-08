package device.apps.rfidsamplev2.sample.bluetooth.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import device.apps.rfidsamplev2.sample.bluetooth.callback.OnDeviceClickListener;
import device.apps.rfidsamplev2.databinding.ItemDeviceBinding;

/**
 * RecyclerView adapter for the list of discovered RF88 Bluetooth devices.
 *
 * <p>Each row is a {@code item_device.xml} card with the device name, MAC address, and an
 * arrow icon. Row binding is delegated to data binding so the layout file is the single source
 * of truth for the row's appearance.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

    private List<BluetoothDevice> items;
    private final OnDeviceClickListener listener;

    /**
     * @param items    the initial list of devices (may be empty)
     * @param listener row click listener — usually the host Activity
     */
    public DevicesAdapter(List<BluetoothDevice> items, OnDeviceClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final ItemDeviceBinding binding = ItemDeviceBinding.inflate(inflater, parent, false);
        return new DeviceViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        final BluetoothDevice item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Replace the displayed list. Called whenever the view model emits a new
     * {@code devicesState} value.
     *
     * <p>A full {@code notifyDataSetChanged()} is used for simplicity; the discovery list is
     * short (a handful of nearby RF88 devices) so DiffUtil would be overkill here.
     *
     * @param items new list of devices to display
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateItems(List<BluetoothDevice> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    /**
     * View holder that owns the data-binding instance for a single device row.
     */
    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private final ItemDeviceBinding binding;
        private final OnDeviceClickListener listener;

        public DeviceViewHolder(@NonNull ItemDeviceBinding itemView, OnDeviceClickListener listener) {
            super(itemView.getRoot());
            this.binding = itemView;
            this.listener = listener;
        }

        /**
         * Push the device and the click listener into the data-bound layout.
         */
        @SuppressLint("MissingPermission")
        public void bind(BluetoothDevice device) {
            binding.setDevice(device);
            binding.setListener(listener);
            binding.executePendingBindings();
        }
    }
}
