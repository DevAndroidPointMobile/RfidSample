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

    private BaseViewModel _baseViewModel;
    private WireViewModel _viewModel;
    private ActivityWiredBinding _binding;

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
        _viewModel.dispose(WiredActivity.this);
        _baseViewModel.connectState.removeObservers(this);
    }

    /**
     * Allow the user to directly attempt to connect to or disconnect from the connected device
     *
     * @param view Button view
     */
    public void onConnection(View view) {
        if (_baseViewModel.connectState.getValue() != DeviceConnectionState.CONNECTED) {
            // TODO, Manual connection action for PM90.
             _viewModel.connect();
        } else {
            _viewModel.disconnect();
        }
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        _baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        _viewModel = new ViewModelProvider(this).get(WireViewModel.class);
        _viewModel.launch(WiredActivity.this, _baseViewModel.connectState.getValue() == DeviceConnectionState.CONNECTED);
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        _binding = ActivityWiredBinding.inflate(getLayoutInflater());
        _binding.setActivity(WiredActivity.this);
        setContentView(_binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeData() {
        _baseViewModel.connectState.observe(this, state -> {
            Log.d("TAG", "onCreate: " + state.name());
            _binding.setState(state.toString());
            _binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
        });
    }
}