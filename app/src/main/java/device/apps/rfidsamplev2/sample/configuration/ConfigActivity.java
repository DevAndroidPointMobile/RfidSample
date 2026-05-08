package device.apps.rfidsamplev2.sample.configuration;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.List;

import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.connection.Rf88ConnectionManager;
import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.data.KeyMap;
import device.apps.rfidsamplev2.databinding.ActivityConfigBinding;
import device.apps.rfidsamplev2.databinding.DialogInputBinding;
import device.apps.rfidsamplev2.databinding.DialogKeymapBinding;
import device.apps.rfidsamplev2.databinding.DialogRadioBinding;
import device.apps.rfidsamplev2.databinding.DialogSeekbarBinding;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;
import device.apps.rfidsamplev2.sample.configuration.ui.ConfigurationAdapter;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

/**
 * Configuration screen — lets the user inspect and change every RF88 setting the SDK
 * exposes. Backed by {@link ConfigViewModel} which loads each value at launch and writes
 * them back when the user taps Apply.
 *
 * <h3>How a single setting is edited</h3>
 * <ol>
 *     <li>The list ({@link ConfigurationAdapter}) renders one row per
 *         {@link Configuration} entry — either a tappable tile or an in-place toggle
 *         switch.</li>
 *     <li>Tapping a tile fires {@link #onClickedConfiguration(Configuration)} which
 *         dispatches to one of four dialog flavours: {@link #showSeekbarDialog} (Q /
 *         power / suspend), {@link #showRadioDialog} (most enums), {@link #showInputDialog}
 *         (free-form text — pointer / access password) or {@link #showKeymapDialog}
 *         (dual-trigger key mapping).</li>
 *     <li>Each dialog updates the corresponding {@code LiveData<String>} on the
 *         ViewModel; the row reactively redraws.</li>
 *     <li>The user taps <b>Apply</b> to flush every value to the SDK in one batch, or
 *         <b>Factory Reset</b> to revert and reload.</li>
 * </ol>
 *
 * <h3>Lifecycle</h3>
 * Closes itself if the RF88 link drops (see {@link #observeConnection()}). Long-running
 * load / apply / factory-default cycles surface a modal progress dialog driven by
 * {@link ConfigViewModel#getBusy()}.
 */
public class ConfigActivity extends AppCompatActivity implements OnTileClickListener {

    private ConfigViewModel viewModel;
    private AlertDialog progressDialog;
    private ActivityConfigBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeConnection();
    }

    /**
     * Finishes the screen when the connection is genuinely lost. Uses
     * {@link Rf88ConnectionManager#isLost} rather than observing {@code connectState}
     * directly so that the transient {@code DISCONNECTED} the SDK fires during a SLEEP
     * round-trip does not close the screen mid-sleep.
     */
    private void observeConnection() {
        final Rf88ConnectionManager connectionManager = ((RFIDSampleV2) getApplication()).getConnectionManager();
        connectionManager.isLost.observe(this, lost -> {
            if (Boolean.TRUE.equals(lost)) finish();
        });
    }

    @Override
    public void onClickedConfiguration(Configuration configuration) {
        switch (configuration) {
            case MAX_Q:
            case MIN_Q:
            case START_Q:
                showSeekbarDialog(configuration, ConfigData.VALUE_MAX_Q);
                break;
            case POWER:
                showSeekbarDialog(configuration, ConfigData.VALUE_MAX_POWER);
                break;
            case SUSPEND_TIME:
                showSeekbarDialog(configuration, ConfigData.VALUE_MAX_SUSPEND_TIME);
                break;
            case TARGET:
                showRadioDialog(configuration, ConfigData.target());
                break;
            case ACTION:
                showRadioDialog(configuration, ConfigData.action());
                break;
            case BANK:
                showRadioDialog(configuration, ConfigData.banks());
                break;
            case VOLUME:
                showRadioDialog(configuration, ConfigData.volume());
                break;
            case KEY_MAP:
                showKeymapDialog(configuration);
                break;
            case INVENTORY_RESPONSE:
                showRadioDialog(configuration, ConfigData.inventoryResponse());
                break;
            case SESSION:
                showRadioDialog(configuration, ConfigData.session());
                break;
            case SEARCH_MODE:
                showRadioDialog(configuration, ConfigData.searchMode());
                break;
            case LINK_PROFILE:
                showRadioDialog(configuration, ConfigData.linkProfile());
                break;
            case POINTER:
            case ACCESS_PASSWORD:
                showInputDialog(configuration);
                break;
            default:
                // Switch-style settings (CONTINUOUS, VIBRATE, INCREMENT_Q, DECREMENT_Q,
                // FIXED_Q) edit themselves in place via ConfigurationAdapter.SwitchViewHolder
                // and never fire this callback — see OnTileClickListener.
                break;
        }
    }

    /**
     * Reset the configuration items to their factory default state
     *
     * @param view Button view
     */
    public void onFactoryDefault(View view) {
        viewModel.factoryDefault();
    }

    /**
     * Apply the configuration settings displayed on the screen to RF88
     *
     * @param view Button view
     */
    public void onApply(View view) {
        viewModel.apply();
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        binding = ActivityConfigBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());

        final ConfigurationAdapter adapter = new ConfigurationAdapter(viewModel, this, this);
        binding.recyclerView.setAdapter(adapter);

        viewModel.getBusy().observe(this, busy -> {
            if (busy != null && busy) {
                showProgressDialog();
            } else {
                hideProgressDialog();
            }
        });
    }

    /**
     * Show the modal progress dialog while a load / apply / factory-default cycle is
     * in flight. Inflates {@code dialog_progress.xml} so the look stays consistent with
     * the other dialogs and there are no hardcoded colors to drift from the theme.
     */
    private void showProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) return;

        final View content = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(content);

        progressDialog = builder.create();
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        progressDialog.show();
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    /**
     * Create a slider dialog on the screen for setting configuration values
     *
     * @param configuration target configuration
     * @param maxValue      slider max value
     */
    private void showSeekbarDialog(Configuration configuration, int maxValue) {
        final DialogSeekbarBinding binding = DialogSeekbarBinding.inflate(getLayoutInflater());
        final LiveData<String> liveData = viewModel.getConfiguration(configuration);
        // Defensive clamp — if a firmware variant reports a value outside the UI's
        // declared range, fall back to the nearest legal slider position instead of
        // letting the Slider throw IllegalStateException.
        final int rawValue = Integer.parseInt(liveData.getValue());
        final int progress = Math.max(0, Math.min(maxValue, rawValue));

        binding.setTitle(configuration.displayName());
        binding.setMaxValue(maxValue);
        binding.setProgress(progress);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
            binding.setProgress((int) value);
        });

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int current = (int) binding.slider.getValue();
            viewModel.setConfiguration(configuration, String.valueOf(current));
        });

        dialog.show();
    }

    /**
     * Create a select dialog on the screen for setting configuration values
     *
     * @param configuration target configuration
     * @param configData    configuration values
     */
    private void showRadioDialog(Configuration configuration, List<ConfigData> configData) {
        final LiveData<String> liveData = viewModel.getConfiguration(configuration);
        final String currentValue = liveData.getValue();

        final DialogRadioBinding binding = DialogRadioBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        final RadioGroup radioGroup = binding.radioGroup;

        binding.setTitle(configuration.displayName());

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int selectedId = radioGroup.getCheckedRadioButtonId();
            final ConfigData target = configData.get(selectedId);
            viewModel.setConfiguration(configuration, target.value);
        });

        for (int i = 0; i < configData.size(); i++) {
            final ConfigData target = configData.get(i);
            final MaterialRadioButton radioButton = new MaterialRadioButton(this);
            radioButton.setText(target.name);
            radioButton.setTag(target.value);
            radioButton.setId(i);
            radioButton.setTextColor(Color.parseColor("#212121"));
            radioButton.setTextSize(16);
            radioButton.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
            radioButton.setPadding(16, 16, 16, 16);
            radioGroup.addView(radioButton);

            if (target.value.equals(currentValue))
                radioButton.setChecked(true);
        }

        dialog.show();
    }

    /**
     * Dedicated dialog for the dual-trigger key mapping setting. Each physical trigger
     * (Top / Bottom) gets its own segmented toggle so the user can independently assign
     * <b>Inventory</b> or <b>Scanner</b>; the two selections are then composed into the
     * SDK's two-character format via {@link KeyMap#compose(char, char)}.
     */
    private void showKeymapDialog(Configuration configuration) {
        final LiveData<String> liveData = viewModel.getConfiguration(configuration);
        final String currentValue = liveData.getValue();

        final DialogKeymapBinding binding = DialogKeymapBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        final boolean topIsScanner = KeyMap.top(currentValue) == KeyMap.SCANNER;
        binding.topGroup.check(topIsScanner ? binding.topScanner.getId() : binding.topInventory.getId());

        final boolean bottomIsScanner = KeyMap.bottom(currentValue) == KeyMap.SCANNER;
        binding.bottomGroup.check(bottomIsScanner ? binding.bottomScanner.getId() : binding.bottomInventory.getId());

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final char top = (binding.topGroup.getCheckedButtonId() == binding.topScanner.getId()) ? KeyMap.SCANNER : KeyMap.INVENTORY;
            final char bottom = (binding.bottomGroup.getCheckedButtonId() == binding.bottomScanner.getId()) ? KeyMap.SCANNER : KeyMap.INVENTORY;
            viewModel.setConfiguration(configuration, KeyMap.compose(top, bottom));
        });

        dialog.show();
    }

    /**
     * Create an input dialog on the screen for setting configuration values
     *
     * @param configuration target configuration
     */
    private void showInputDialog(Configuration configuration) {
        final LiveData<String> liveData = viewModel.getConfiguration(configuration);
        final String currentValue = liveData.getValue();

        final DialogInputBinding binding = DialogInputBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        binding.setTitle(configuration.displayName());
        binding.editText.setText(currentValue);
        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final String result = binding.editText.getText().toString();
            viewModel.setConfiguration(configuration, result);
        });

        dialog.show();
    }
}