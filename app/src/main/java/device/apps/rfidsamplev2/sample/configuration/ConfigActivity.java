package device.apps.rfidsamplev2.sample.configuration;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.slider.Slider;

import java.util.List;

import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.databinding.ActivityConfigBinding;
import device.apps.rfidsamplev2.databinding.DialogInputBinding;
import device.apps.rfidsamplev2.databinding.DialogRadioBinding;
import device.apps.rfidsamplev2.databinding.DialogSeekbarBinding;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;
import device.apps.rfidsamplev2.sample.configuration.ui.ConfigurationAdapter;

public class ConfigActivity extends AppCompatActivity implements OnTileClickListener {

    private ConfigViewModel _viewModel;
    private AlertDialog _progressDialog;
    private ActivityConfigBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        setupToolbar();
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
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        _binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        _viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        _viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        _binding = ActivityConfigBinding.inflate(getLayoutInflater());
        _binding.setActivity(this);
        setContentView(_binding.getRoot());

        final ConfigurationAdapter adapter = new ConfigurationAdapter(_viewModel, this, this);
        _binding.recyclerView.setAdapter(adapter);

        // ViewModel의 busy 상태 관찰
        _viewModel.getBusy().observe(this, busy -> {
            if (busy != null && busy) {
                showProgressDialog();
            } else {
                hideProgressDialog();
            }
        });
    }

    /**
     * Show modal progress dialog
     */
    private void showProgressDialog() {
        if (_progressDialog != null && _progressDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setGravity(android.view.Gravity.CENTER);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.parseColor("#5E35B1")));

        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText("Processing...");
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#212121"));
        tv.setPadding(padding, 0, 0, 0);

        layout.addView(progressBar);
        layout.addView(tv);

        builder.setView(layout);
        _progressDialog = builder.create();
        _progressDialog.show();
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (_progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
            _progressDialog = null;
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
        final LiveData<String> liveData = _viewModel.getConfiguration(configuration);
        final int progress = Integer.parseInt(liveData.getValue());

        binding.setTitle(configuration.name());
        binding.setMaxValue(maxValue);
        binding.setProgress(progress);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Slider 이벤트 리스너
        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
            binding.setProgress((int) value);
        });

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int current = (int) binding.slider.getValue();
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
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        final RadioGroup radioGroup = binding.radioGroup;

        binding.setTitle(configuration.name());

        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.apply.setOnClickListener(view -> {
            dialog.dismiss();
            final int selectedId = radioGroup.getCheckedRadioButtonId();
            final ConfigData target = configData.get(selectedId);
            _viewModel.setConfiguration(configuration, target.value);
        });

        // MaterialRadioButton 사용
        for (int i = 0; i < configData.size(); i++) {
            final ConfigData target = configData.get(i);
            final MaterialRadioButton radioButton = new MaterialRadioButton(this);
            radioButton.setText(target.name);
            radioButton.setTag(target.value);
            radioButton.setId(i);
            radioButton.setTextColor(Color.parseColor("#212121"));
            radioButton.setTextSize(16);
            radioButton.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#5E35B1")));
            radioButton.setPadding(16, 16, 16, 16);
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
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

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