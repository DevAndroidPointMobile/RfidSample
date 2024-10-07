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

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

    private List<BluetoothDevice> _items;
    private final OnDeviceClickListener _listener;

    public DevicesAdapter(List<BluetoothDevice> _items, OnDeviceClickListener listener) {
        this._items = _items;
        this._listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final ItemDeviceBinding binding = ItemDeviceBinding.inflate(inflater, parent, false);
        return new DeviceViewHolder(binding, _listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        final BluetoothDevice item = _items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return _items.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateItems(List<BluetoothDevice> items) {
        _items = items;
        notifyDataSetChanged();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private final ItemDeviceBinding _binding;
        private final OnDeviceClickListener _listener;

        public DeviceViewHolder(@NonNull ItemDeviceBinding itemView, OnDeviceClickListener listener) {
            super(itemView.getRoot());
            this._binding = itemView;
            this._listener = listener;
        }

        @SuppressLint("MissingPermission")
        public void bind(BluetoothDevice device) {
            _binding.setDevice(device);
            _binding.setListener(_listener);
            _binding.executePendingBindings();
        }
    }
}
