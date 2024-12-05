package device.apps.rfidsamplev2.sample.configuration.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.databinding.ItemSwitchBinding;
import device.apps.rfidsamplev2.databinding.ItemTileBinding;
import device.apps.rfidsamplev2.sample.configuration.ConfigViewModel;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;

public class ConfigurationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TILE = 0;
    private static final int TYPE_SWITCH = 1;

    private final LifecycleOwner _lifecycleOwner;
    private final OnTileClickListener _listener;
    private final Configuration[] _configurations;
    private final ConfigViewModel _viewModel;

    public ConfigurationAdapter(ConfigViewModel viewModel, LifecycleOwner lifecycleOwner, OnTileClickListener listener) {
        this._viewModel = viewModel;
        this._listener = listener;
        this._configurations = Configuration.values();
        this._lifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getItemViewType(int position) {
        final Configuration configuration = _configurations[position];
        switch (configuration) {
            case ACCESS_PASSWORD:
            case POINTER:
            case TARGET:
            case ACTION:
            case BANK:
            case INVENTORY_RESPONSE:
            case VOLUME:
            case KEY_MAP:
            case START_Q:
            case MIN_Q:
            case MAX_Q:
            case SUSPEND_TIME:
                return TYPE_TILE;

            case INCREMENT_Q:
            case DECREMENT_Q:
            case FIXED_Q:
            case CONTINUOUS:
            case VIBRATE:
                return TYPE_SWITCH;

            default:
                return super.getItemViewType(position);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_TILE:
                final ItemTileBinding tileBinding = ItemTileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new TileViewHolder(tileBinding, _lifecycleOwner, _listener);

            case TYPE_SWITCH:
                final ItemSwitchBinding switchBinding = ItemSwitchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new SwitchViewHolder(switchBinding, _viewModel, _lifecycleOwner);

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Configuration configuration = _configurations[position];
        final LiveData<String> data = _viewModel.getConfiguration(configuration);

        if (holder instanceof TileViewHolder)
            ((TileViewHolder) holder).bind(configuration, data);

        if (holder instanceof SwitchViewHolder)
            ((SwitchViewHolder) holder).bind(configuration, data);
    }


    @Override
    public int getItemCount() {
        return _configurations.length;
    }

    public static class TileViewHolder extends RecyclerView.ViewHolder {

        private final ItemTileBinding _binding;
        private final OnTileClickListener _listener;
        private final LifecycleOwner _lifecycleOwner;

        public TileViewHolder(ItemTileBinding binding, LifecycleOwner lifecycleOwner, OnTileClickListener listener) {
            super(binding.getRoot());
            this._binding = binding;
            this._listener = listener;
            this._lifecycleOwner = lifecycleOwner;
        }

        void bind(Configuration configuration, LiveData<String> data) {
            _binding.setName(configuration.name());

            data.observe(_lifecycleOwner, value -> {
                ConfigData configData;
                switch (configuration) {
                    case SESSION:
                        configData = ConfigData.findByValue(ConfigData.session(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case SEARCH_MODE:
                        configData = ConfigData.findByValue(ConfigData.searchMode(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case LINK_PROFILE:
                        configData = ConfigData.findByValue(ConfigData.linkProfile(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case TARGET:
                        configData = ConfigData.findByValue(ConfigData.target(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case ACTION:
                        configData = ConfigData.findByValue(ConfigData.action(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case BANK:
                        configData = ConfigData.findByValue(ConfigData.banks(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case INVENTORY_RESPONSE:
                        configData = ConfigData.findByValue(ConfigData.inventoryResponse(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case VOLUME:
                        configData = ConfigData.findByValue(ConfigData.volume(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case KEY_MAP:
                        configData = ConfigData.findByValue(ConfigData.keymap(), value);
                        _binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case SUSPEND_TIME:
                        _binding.setValue(String.format("%s min", value));
                        break;
                    default:
                        _binding.setValue(value);
                        break;
                }
            });

            _binding.getRoot().setOnClickListener(v -> _listener.onClickedConfiguration(configuration));
        }
    }

    public static class SwitchViewHolder extends RecyclerView.ViewHolder {

        private final ItemSwitchBinding _binding;
        private final ConfigViewModel _viewModel;
        private final LifecycleOwner _lifecycleOwner;

        public SwitchViewHolder(ItemSwitchBinding binding, ConfigViewModel viewModel, LifecycleOwner lifecycleOwner) {
            super(binding.getRoot());
            this._binding = binding;
            this._viewModel = viewModel;
            this._lifecycleOwner = lifecycleOwner;
        }

        void bind(Configuration configuration, LiveData<String> data) {
            // dialog title.
            _binding.setName(configuration.name());

            // set Changed listener.
            _binding.valueSwitch.setOnCheckedChangeListener((view, isChecked) -> {
                        if (configuration == Configuration.CONTINUOUS)
                            _viewModel.setConfiguration(configuration, (isChecked) ? "c" : "p");
                        else
                            _viewModel.setConfiguration(configuration, (isChecked) ? "1" : "0");
                    }
            );

            // observe live data.
            data.observe(_lifecycleOwner, value -> _binding.setEnabled((configuration == Configuration.CONTINUOUS) ? value.equals("c") : value.equals("1")));
        }
    }
}
