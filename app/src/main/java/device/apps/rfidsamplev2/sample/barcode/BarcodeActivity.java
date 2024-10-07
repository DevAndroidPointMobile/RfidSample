package device.apps.rfidsamplev2.sample.barcode;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityBarcodeBinding;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothViewModel;

// TODO, Guide to PointMobile scan function.
// TODO, If you want to develop scanner control, refer to tScanner folder in Emkit SDK.

public class BarcodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
    }

    /**
     * initialization view model
     */
    private void initializationViewModel() {
        final BaseViewModel baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        final BarcodeViewModel viewModel = new ViewModelProvider(this).get(BarcodeViewModel.class);
        viewModel.launch();
    }

    /**
     * Initialize the View binding
     */
    private void initializationContentView() {
        final ActivityBarcodeBinding binding = ActivityBarcodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}