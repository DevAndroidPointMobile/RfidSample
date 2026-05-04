package device.apps.rfidsamplev2.sample.nread.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import device.apps.rfidsamplev2.databinding.ItemInventoryNreadBinding;
import device.apps.rfidsamplev2.sample.nread.callback.OnInventoryNreadClickListener;
import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;

/**
 * RecyclerView adapter for the discovered RF88 tag list on the Inventory &amp; Read
 * sample screen. Each row is an {@code item_inventory_nread.xml} card showing the EPC,
 * the running read count, and the bytes read from the tag's memory bank in the same
 * air-protocol exchange.
 *
 * <p>The adapter holds a direct reference to the ViewModel's {@code readHistory} list
 * — mutations there are visible here without any data copy. The Activity drives
 * incremental updates through {@link #updateData(int)} based on the ViewModel's
 * {@code changedIndex} signal.
 */
public class InventoryNreadAdapter extends RecyclerView.Adapter<InventoryNreadAdapter.InventoryNreadViewHolder> {

    private final List<InventoryNreadResponse> inventoryList;
    private final OnInventoryNreadClickListener clickListener;

    public InventoryNreadAdapter(List<InventoryNreadResponse> inventoryList, OnInventoryNreadClickListener clickListener) {
        this.inventoryList = inventoryList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public InventoryNreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryNreadBinding binding = ItemInventoryNreadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

    /**
     * Re-bind the row at {@code changedIndex}, or refresh the entire list when
     * {@code changedIndex} is {@code -1} (used by {@code clearHistory()} and full
     * resets). The {@code -1} sentinel matches the initial value of the ViewModel's
     * {@code changedIndex} LiveData so the very first observe-time emit is harmless.
     *
     * @param changedIndex index of the row to refresh, or {@code -1} for a full refresh
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(int changedIndex) {
        if (changedIndex == -1)
            notifyDataSetChanged();
        else
            notifyItemChanged(changedIndex);
    }

    public static class InventoryNreadViewHolder extends RecyclerView.ViewHolder {

        private final ItemInventoryNreadBinding binding;

        public InventoryNreadViewHolder(@NonNull ItemInventoryNreadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Push the row's EPC / count / read-value strings into the data-bound layout
         * and wire the tile's click to {@code clickListener}. A null
         * {@code clickListener} makes the row inert.
         */
        public void bind(InventoryNreadResponse data, OnInventoryNreadClickListener clickListener) {
            binding.setEpc(data.getEpcData());
            binding.setCount(String.valueOf(data.getReadCount()));
            binding.setReadValue(data.getReadValue());
            binding.inventoryTile.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInventoryNreadClicked(data);
                }
            });
        }
    }
}


