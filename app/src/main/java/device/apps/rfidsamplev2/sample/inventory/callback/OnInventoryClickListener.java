package device.apps.rfidsamplev2.sample.inventory.callback;


import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;

public interface OnInventoryClickListener {
    void onInventoryClicked(InventoryResponse item);
}
