package device.apps.rfidsamplev2.sample.nread.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import device.apps.rfidsamplev2.databinding.ItemInventoryBinding;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;

public class InventoryNreadAdapter extends RecyclerView.Adapter<InventoryNreadAdapter.InventoryNreadViewHolder> {

    private final List<InventoryNreadResponse> inventoryList;
    private final OnInventoryClickListener clickListener;

    public InventoryNreadAdapter(List<InventoryNreadResponse> inventoryList, OnInventoryClickListener clickListener) {
        this.inventoryList = inventoryList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public InventoryNreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryBinding binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new InventoryNreadViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryNreadViewHolder holder, int position) {
        final InventoryNreadResponse data = inventoryList.get(position);
        holder.bind(data, clickListener);
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(int changedIndex) {
        if (changedIndex == -1)
            notifyDataSetChanged();
        else
            notifyItemChanged(changedIndex);
    }

    public static class InventoryNreadViewHolder extends RecyclerView.ViewHolder {

        private final ItemInventoryBinding _binding;

        public InventoryNreadViewHolder(@NonNull ItemInventoryBinding binding) {
            super(binding.getRoot());
            this._binding = binding;
        }

        public void bind(InventoryNreadResponse data, OnInventoryClickListener clickListener) {
            _binding.setEpc(data.getEpcData());
            _binding.setCount(String.valueOf(data.getReadCount()));
            _binding.setReadValue(data.getReadValue());
            _binding.inventoryTile.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInventoryClicked(data);
                }
            });
        }
    }
}


