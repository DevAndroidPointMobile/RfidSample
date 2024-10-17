package device.apps.rfidsamplev2.sample.inventory.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import device.apps.rfidsamplev2.databinding.ItemInventoryBinding;
import device.sdk.rfid.model.InventoryResponse;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryResponse> inventoryList;

    public InventoryAdapter(List<InventoryResponse> inventoryList) {
        this.inventoryList = inventoryList;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryBinding binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        final InventoryResponse data = inventoryList.get(position);
        holder.bind(data);
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

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemInventoryBinding _binding;

        public InventoryViewHolder(@NonNull ItemInventoryBinding binding) {
            super(binding.getRoot());
            this._binding = binding;
        }

        public void bind(InventoryResponse data) {
            _binding.setEpc(data.getReadLine());
            _binding.setCount(String.valueOf(data.getReadCount()));
        }
    }
}

