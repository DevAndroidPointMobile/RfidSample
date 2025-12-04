package device.apps.rfidsamplev2.sample.configuration;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.databinding.ActivityConfigBinding;
import device.apps.rfidsamplev2.databinding.DialogInputBinding;
import device.apps.rfidsamplev2.databinding.DialogRadioBinding;
import device.apps.rfidsamplev2.databinding.DialogSeekbarBinding;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;
import device.apps.rfidsamplev2.sample.configuration.ui.ConfigurationAdapter;
import ex.dev.sdk.rf88.Rf88Manager;

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
     * Initialize the View mode
     */
    private void initializationViewModel() {
        _viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);
        _viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private AlertDialog progressDialog; // 필드에 추가

    private void initializationContentView() {
        final ActivityConfigBinding binding = ActivityConfigBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        setContentView(binding.getRoot());

        final ConfigurationAdapter adapter = new ConfigurationAdapter(_viewModel, this, this);
        binding.recyclerView.setAdapter(adapter);

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
     * 모달 인디케이터 다이얼로그 생성 및 표시
     */
    private void showProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // 작업 중 취소 불가(옵션)
        // ProgressBar를 포함한 간단한 바인딩-less 레이아웃 생성
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText("Processing...");
        tv.setTextSize(16);
        tv.setPadding(padding / 2, 0, 0, 0);
        layout.addView(progressBar);
        layout.addView(tv);

        builder.setView(layout);
        progressDialog = builder.create();
        progressDialog.show();
    }

    /**
     * 프로그레스 다이얼로그 숨기기
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
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