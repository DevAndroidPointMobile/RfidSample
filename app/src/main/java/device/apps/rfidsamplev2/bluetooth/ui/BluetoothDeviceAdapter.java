package device.apps.rfidsamplev2.bluetooth.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import device.apps.rfidsamplev2.R;

public class BluetoothDeviceAdapter
        extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public BluetoothDeviceAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BluetoothDevice> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        BluetoothDevice device = items.get(pos);
        h.name.setText(device.getName() != null ? device.getName() : "Unknown Device");
        h.address.setText(device.getAddress());
        h.itemView.setOnClickListener(v -> listener.onClick(device));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            address = v.findViewById(R.id.tvAddress);
        }
    }
}


