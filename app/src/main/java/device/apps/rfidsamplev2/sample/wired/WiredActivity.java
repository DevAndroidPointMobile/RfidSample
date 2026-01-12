package device.apps.rfidsamplev2.sample.wired;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityWiredBinding;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class WiredActivity extends AppCompatActivity {

    private BaseViewModel baseViewModel;
    private WireViewModel viewModel;
    private ActivityWiredBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.dispose(WiredActivity.this);
        baseViewModel.connectState.removeObservers(this);
    }

    /**
     * Allow the user to directly attempt to connect to or disconnect from the connected device
     *
     * @param view Button view
     */
    public void onConnection(View view) {
        if (baseViewModel.connectState.getValue() != DeviceConnectionState.CONNECTED) {
            viewModel.connect();
        } else {
            viewModel.disconnect();
        }
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        viewModel = new ViewModelProvider(this).get(WireViewModel.class);
        viewModel.launch(WiredActivity.this, baseViewModel.connectState.getValue() == DeviceConnectionState.CONNECTED);
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        binding = ActivityWiredBinding.inflate(getLayoutInflater());
        binding.setActivity(WiredActivity.this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeData() {
        baseViewModel.connectState.observe(this, state -> {
            Log.d("TAG", "onCreate: " + state.name());
            binding.setState(state.toString());
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
        });
    }
}