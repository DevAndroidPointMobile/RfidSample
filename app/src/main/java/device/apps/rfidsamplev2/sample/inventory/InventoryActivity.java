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
import device.apps.rfidsamplev2.sample.inventory.data.InventoryResponse;
import device.apps.rfidsamplev2.sample.inventory.ui.InventoryAdapter;

public class InventoryActivity extends AppCompatActivity implements OnInventoryClickListener {

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;

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
        viewModel.readHistory.clear();
        adapter.updateData(-1);
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        final BaseViewModel baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        viewModel.launch();
    }


    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        adapter = new InventoryAdapter(viewModel.readHistory, this);
        final ActivityInventoryBinding binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        binding.setActivity(InventoryActivity.this);
        binding.recyclerView.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setContentView(binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeLiveData() {
        viewModel.changedIndex.observe(this, index -> adapter.updateData(index));
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
        binding.read.setOnClickListener(view -> viewModel.read(response));
        binding.write.setOnClickListener(view -> viewModel.write(response));
        binding.lock.setOnClickListener(view -> viewModel.lock(response));
        binding.kill.setOnClickListener(view -> viewModel.kill(response));
        dialog.show();
    }
}