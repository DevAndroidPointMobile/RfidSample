package device.apps.rfidsamplev2.sample.inventory;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityInventoryBinding;
import device.apps.rfidsamplev2.databinding.DialogMandatoryBinding;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.inventory.ui.InventoryAdapter;
import device.sdk.rfid.model.InventoryResponse;

public class InventoryActivity extends AppCompatActivity implements OnInventoryClickListener {

    private InventoryViewModel _viewModel;
    private InventoryAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeLiveData();
    }

    @Override
    public void onInventoryClicked(InventoryResponse item) {
        showMandatoryDialog(item);
    }

    /**
     * Clear all the Inventory responses displayed on the screen
     *
     * @param view Button view
     */
    public void onClear(View view) {
        _viewModel.readHistory.clear();
        _adapter.updateData(-1);
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        final BaseViewModel baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        _viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        _viewModel.launch();
    }


    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        _adapter = new InventoryAdapter(_viewModel.readHistory);
        final ActivityInventoryBinding binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        binding.setActivity(InventoryActivity.this);
        binding.recyclerView.setAdapter(_adapter);
        setContentView(binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeLiveData() {
        _viewModel.changedIndex.observe(this, index -> _adapter.updateData(index));
    }

    /**
     * Create a dialog on the screen to execute the mandatory command.
     *
     * @param response target inventory resposne
     */
    private void showMandatoryDialog(InventoryResponse response) {
        final DialogMandatoryBinding binding = DialogMandatoryBinding.inflate(getLayoutInflater());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());
        final AlertDialog dialog = builder.create();
        binding.cancel.setOnClickListener(view -> dialog.dismiss());
        binding.read.setOnClickListener(view -> _viewModel.read(response));
        binding.write.setOnClickListener(view -> _viewModel.write(response));
        binding.lock.setOnClickListener(view -> _viewModel.lock(response));
        binding.kill.setOnClickListener(view -> _viewModel.kill(response));
        dialog.show();
    }
}