package device.apps.rfidsamplev2.sample.nread;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.R;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityInventoryBinding;
import device.apps.rfidsamplev2.databinding.ActivityInventoryNreadBinding;
import device.apps.rfidsamplev2.sample.inventory.InventoryActivity;
import device.apps.rfidsamplev2.sample.inventory.InventoryViewModel;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.inventory.ui.InventoryAdapter;
import device.apps.rfidsamplev2.sample.nread.data.InventoryNreadResponse;
import device.apps.rfidsamplev2.sample.nread.ui.InventoryNreadAdapter;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.frameworks.listener.OnHardwareKeyListener;
import ex.dev.sdk.rf88.frameworks.listener.OnInventoryResultListener;

public class InventoryNreadActivity extends AppCompatActivity implements OnInventoryClickListener {

    private InventoryNreadViewModel viewModel;
    private InventoryNreadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        observeLiveData();
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        final BaseViewModel baseViewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
        viewModel = new ViewModelProvider(this).get(InventoryNreadViewModel.class);
        viewModel.launch();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        adapter = new InventoryNreadAdapter(viewModel.readHistory, this);
        final ActivityInventoryNreadBinding binding = ActivityInventoryNreadBinding.inflate(getLayoutInflater());
        binding.setActivity(InventoryNreadActivity.this);
        binding.recyclerView.setAdapter(adapter);
        setContentView(binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeLiveData() {
        viewModel.changedIndex.observe(this, index -> adapter.updateData(index));
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
}