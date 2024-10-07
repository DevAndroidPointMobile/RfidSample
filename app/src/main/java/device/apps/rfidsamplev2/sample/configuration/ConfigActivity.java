package device.apps.rfidsamplev2.sample.configuration;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.databinding.ActivityBluetoothBinding;
import device.apps.rfidsamplev2.databinding.ActivityConfigBinding;
import device.apps.rfidsamplev2.databinding.DialogInputBinding;
import device.apps.rfidsamplev2.databinding.DialogRadioBinding;
import device.apps.rfidsamplev2.databinding.DialogSeekbarBinding;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothActivity;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothViewModel;
import device.apps.rfidsamplev2.sample.bluetooth.ui.DevicesAdapter;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;
import device.apps.rfidsamplev2.sample.configuration.ui.ConfigurationAdapter;

public class ConfigActivity extends AppCompatActivity implements OnTileClickListener {

    private ConfigViewModel _viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
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
                showRadioDialog(configuration, ConfigData.keymap());
                break;
            case PACKET:
                showRadioDialog(configuration, ConfigData.packetOptions());
                break;
            case SESSION:
                showRadioDialog(configuration, ConfigData.session());
                break;
            case SEARCH_MODE:
                showRadioDialog(configuration, ConfigData.inventoryFlag());
                break;
            case LINK_PROFILE:
                showRadioDialog(configuration, ConfigData.linkProfile());
                break;
            case POINTER:
            case ACCESS_PASSWORD:
                showInputDialog(configuration);
                break;
            default:
                break;
        }
    }

    /**
     * Reset the configuration items to their factory default state
     *
     * @param view Button view
     */
    public void onFactoryDefault(View view) {
        _viewModel.factoryDefault();
    }

    /**
     * Apply the configuration settings displayed on the screen to RF88
     *
     * @param view Button view
     */
    public void onApply(View view) {
        _viewModel.apply();
    }


    /**
     * Initialize the View mode
     */
    private void initializationViewModel() {
        _viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        _viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        final ActivityConfigBinding binding = ActivityConfigBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        setContentView(binding.getRoot());

        final ConfigurationAdapter adapter = new ConfigurationAdapter(_viewModel, this, this);
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Create a seek bar dialog on the screen for setting configuration values
     *
     * @param configuration target configration
     * @param maxValue      seek bar max value
     */
    private void showSeekbarDialog(Configuration configuration, int maxValue) {
        final DialogSeekbarBinding binding = DialogSeekbarBinding.inflate(getLayoutInflater());
        final LiveData<String> liveData = _viewModel.getConfiguration(configuration);
        final int progress = Integer.parseInt(liveData.getValue());

        binding.setTitle(configuration.name());
        binding.setMaxValue(maxValue);
        binding.setProgress(progress);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();

        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.setProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not used
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not used
            }
        });

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int current = binding.seekbar.getProgress();
            _viewModel.setConfiguration(configuration, String.valueOf(current));
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
        final LiveData<String> liveData = _viewModel.getConfiguration(configuration);
        final String currentValue = liveData.getValue();

        final DialogRadioBinding binding = DialogRadioBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        final RadioGroup radioGroup = binding.radioGroup;

        binding.setTitle(configuration.name());

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int selectedId = radioGroup.getCheckedRadioButtonId();
            final ConfigData target = configData.get(selectedId);
            _viewModel.setConfiguration(configuration, target.value);
        });

        for (int i = 0; i < configData.size(); i++) {
            final ConfigData target = configData.get(i);
            final RadioButton radioButton = new RadioButton(this);
            radioButton.setText(target.name);
            radioButton.setTag(target.value);
            radioButton.setId(i);
            radioGroup.addView(radioButton);

            if (target.value.equals(currentValue))
                radioButton.setChecked(true);
        }

        dialog.show();
    }

    /**
     * Create an input dialog on the screen for setting configuration values
     *
     * @param configuration target configuration
     */
    private void showInputDialog(Configuration configuration) {
        final LiveData<String> liveData = _viewModel.getConfiguration(configuration);
        final String currentValue = liveData.getValue();

        final DialogInputBinding binding = DialogInputBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        binding.setTitle(configuration.name());
        binding.editText.setText(currentValue);
        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final String result = binding.editText.getText().toString();
            _viewModel.setConfiguration(configuration, result);
        });

        dialog.show();
    }
}