package device.apps.rfidsamplev2.sample.inventory.callback;

import device.sdk.rfid.model.InventoryResult;

public interface OnInventoryClickListener {
    void onInventoryClicked(InventoryResult item);
}
