package device.apps.rfidsamplev2.sample.nread.callback;

import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;

/**
 * Click listener for rows in the Inventory &amp; Read tag list. Implemented by
 * {@code InventoryNreadActivity} and invoked from the row layout's data-bound
 * {@code android:onClick} expression — see {@code InventoryNreadAdapter}.
 *
 * <p>Defined in the nread package (rather than reused across both inventory samples)
 * so each sample owns its own callback and there is no cross-package coupling.
 */
public interface OnInventoryNreadClickListener {

    /**
     * Called when the user taps a tag row.
     *
     * @param item the {@link InventoryNreadResponse} represented by the tapped row
     */
    void onInventoryNreadClicked(InventoryNreadResponse item);
}
