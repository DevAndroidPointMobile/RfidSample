package device.apps.rfidsamplev2.sample.inventory.callback;

import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;

/**
 * Click listener for rows in the Inventory tag list. Implemented by
 * {@code InventoryActivity} and invoked from the row layout's data-bound
 * {@code android:onClick} expression — see {@code InventoryAdapter}.
 */
public interface OnInventoryClickListener {

    /**
     * Called when the user taps a tag row — typically opens the Mandatory dialog
     * (Read / Write / Lock / Kill) for the tapped tag.
     *
     * @param item the {@link InventoryResponse} represented by the tapped row
     */
    void onInventoryClicked(InventoryResponse item);
}
