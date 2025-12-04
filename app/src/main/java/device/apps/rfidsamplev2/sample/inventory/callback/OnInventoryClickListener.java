package device.apps.rfidsamplev2.sample.inventory.callback;


import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;

public interface OnInventoryClickListener {
    default void onInventoryClicked(InventoryResponse item) {
    }

    default void onInventoryClicked(InventoryNreadResponse item) {
    }
}
