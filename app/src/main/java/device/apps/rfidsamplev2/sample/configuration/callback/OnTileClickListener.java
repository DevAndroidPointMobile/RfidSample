package device.apps.rfidsamplev2.sample.configuration.callback;

import device.apps.rfidsamplev2.data.Configuration;

/**
 * Notifies the host Activity that the user tapped a tile-style configuration row, so the
 * Activity can open the appropriate editor dialog (radio / seekbar / input / keymap).
 *
 * <p>Switch-style rows in the same list edit themselves in place and never fire this
 * callback.
 */
public interface OnTileClickListener {

    /**
     * @param configuration the setting whose tile was tapped
     */
    void onClickedConfiguration(Configuration configuration);

}