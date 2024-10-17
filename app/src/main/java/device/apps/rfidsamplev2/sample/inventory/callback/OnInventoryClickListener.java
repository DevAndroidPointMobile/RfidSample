package device.apps.rfidsamplev2.sample.inventory.callback;

import device.sdk.rfid.model.InventoryResponse;

public interface OnInventoryClickListener {
    void onInventoryClicked(InventoryResponse item);
}
