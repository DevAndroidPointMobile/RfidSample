package device.apps.rfidsamplev2;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;

public class RFIDSampleV2 extends Application {

    private BaseViewModel _viewModel;

    @Override
    public void onCreate() {
        super.onCreate();
        _viewModel = new ViewModelProvider.AndroidViewModelFactory(this).create(BaseViewModel.class);
    }

    /**
     * Return the base view model
     *
     * @return base view model
     */
    public BaseViewModel getBaseViewModel() {
        return _viewModel;
    }
}
