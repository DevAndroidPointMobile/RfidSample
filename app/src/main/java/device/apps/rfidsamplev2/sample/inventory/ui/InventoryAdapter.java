package device.apps.rfidsamplev2.sample.inventory.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import device.apps.rfidsamplev2.databinding.ItemInventoryBinding;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;

/**
 * RecyclerView adapter for the discovered RF88 tag list. Each row is an
 * {@code item_inventory.xml} card showing the EPC, the running read count, and an
 * optional detail line (RSSI / frequency).
 *
 * <p>The adapter holds a direct reference to the ViewModel's {@code readHistory} list
 * — mutations there are visible here without any data copy. The Activity drives
 * incremental updates through {@link #updateData(int)} based on the ViewModel's
 * {@code changedIndex} signal.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryResponse> inventoryList;
    private final OnInventoryClickListener clickListener;

    public InventoryAdapter(List<InventoryResponse> inventoryList, OnInventoryClickListener clickListener) {
        this.inventoryList = inventoryList;
        this.clickListener = clickListener;
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

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemInventoryBinding binding;

        public InventoryViewHolder(@NonNull ItemInventoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Push the row's EPC / count / detail strings into the data-bound layout and
         * wire the tile's click to {@code clickListener}. A null {@code clickListener}
         * makes the row inert.
         */
        public void bind(InventoryResponse data, OnInventoryClickListener clickListener) {
            binding.setEpc(data.getReadLine());
            binding.setCount(String.valueOf(data.getReadCount()));
            binding.setDetail(formatDetail(data));
            binding.inventoryTile.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInventoryClicked(data);
                }
            });
        }

        /**
         * Compose the optional detail line shown under the EPC. When the SDK has not
         * been configured to report RSSI / frequency yet (and therefore returns empty
         * strings), this method returns an empty string and the line is hidden by
         * the layout's visibility binding.
         */
        private String formatDetail(InventoryResponse data) {
            final String rssi = data.getRssi();
            final String frequency = data.getFrequency();
            final boolean hasRssi = rssi != null && !rssi.isEmpty();
            final boolean hasFrequency = frequency != null && !frequency.isEmpty();
            if (hasRssi && hasFrequency) return "RSSI " + rssi + " · " + frequency + " MHz";
            if (hasRssi) return "RSSI " + rssi;
            if (hasFrequency) return frequency + " MHz";
            return "";
        }
    }
}

