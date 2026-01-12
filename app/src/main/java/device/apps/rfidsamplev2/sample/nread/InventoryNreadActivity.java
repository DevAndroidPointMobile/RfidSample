package device.apps.rfidsamplev2.sample.nread;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import device.apps.rfidsamplev2.databinding.ActivityInventoryNreadBinding;
import device.apps.rfidsamplev2.sample.inventory.callback.OnInventoryClickListener;
import device.apps.rfidsamplev2.sample.nread.ui.InventoryNreadAdapter;

public class InventoryNreadActivity extends AppCompatActivity implements OnInventoryClickListener {

    private InventoryNreadAdapter adapter;
    private InventoryNreadViewModel viewModel;

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
     * Clear all the Inventory responses displayed on the screen
     *
     * @param view Button view
     */
    public void onClear(View view) {
        viewModel.readHistory.clear();
        adapter.updateData(-1);
    }
}