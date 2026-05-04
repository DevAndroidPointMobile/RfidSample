package device.apps.rfidsamplev2.sample.configuration.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import device.apps.rfidsamplev2.data.ConfigData;
import device.apps.rfidsamplev2.data.Configuration;
import device.apps.rfidsamplev2.data.KeyMap;
import device.apps.rfidsamplev2.databinding.ItemSwitchBinding;
import device.apps.rfidsamplev2.databinding.ItemTileBinding;
import device.apps.rfidsamplev2.sample.configuration.ConfigViewModel;
import device.apps.rfidsamplev2.sample.configuration.callback.OnTileClickListener;

/**
 * RecyclerView adapter for the configuration screen. Renders each {@link Configuration}
 * as one of two row types depending on what kind of value it holds:
 *
 * <ul>
 *     <li>{@link #TYPE_TILE} — settings whose value the user picks via a follow-up
 *         dialog (radio / seekbar / input / keymap). Tapping the tile fires
 *         {@link OnTileClickListener} so the host Activity can present that dialog.</li>
 *     <li>{@link #TYPE_SWITCH} — boolean toggles that flip in place; no dialog.</li>
 * </ul>
 *
 * <p>Routing happens in {@link #getItemViewType(int)}. Each row binds directly to the
 * matching {@code LiveData<String>} from the ViewModel so external value changes
 * (initial load, factory reset) propagate without touching the adapter.
 */
public class ConfigurationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TILE = 0;
    private static final int TYPE_SWITCH = 1;

    private final LifecycleOwner lifecycleOwner;
    private final OnTileClickListener listener;
    private final Configuration[] configurations;
    private final ConfigViewModel viewModel;

    public ConfigurationAdapter(ConfigViewModel viewModel, LifecycleOwner lifecycleOwner, OnTileClickListener listener) {
        this.viewModel = viewModel;
        this.listener = listener;
        this.configurations = Configuration.values();
        this.lifecycleOwner = lifecycleOwner;
    }

    @Override
    public int getItemViewType(int position) {
        final Configuration configuration = configurations[position];
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
                return new TileViewHolder(tileBinding, lifecycleOwner, listener);

            case TYPE_SWITCH:
                final ItemSwitchBinding switchBinding = ItemSwitchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new SwitchViewHolder(switchBinding, viewModel, lifecycleOwner);

            default:
                // getItemViewType only ever returns TYPE_TILE or TYPE_SWITCH for known
                // Configuration entries. An unknown viewType means the enum gained a
                // value but getItemViewType wasn't updated — fail loudly instead of
                // silently returning null and crashing later inside RecyclerView.
                throw new IllegalStateException("Unexpected viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Configuration configuration = configurations[position];
        final LiveData<String> data = viewModel.getConfiguration(configuration);

        if (holder instanceof TileViewHolder)
            ((TileViewHolder) holder).bind(configuration, data);

        if (holder instanceof SwitchViewHolder)
            ((SwitchViewHolder) holder).bind(configuration, data);
    }


    @Override
    public int getItemCount() {
        return configurations.length;
    }

    public static class TileViewHolder extends RecyclerView.ViewHolder {

        private final ItemTileBinding binding;
        private final OnTileClickListener listener;
        private final LifecycleOwner lifecycleOwner;

        public TileViewHolder(ItemTileBinding binding, LifecycleOwner lifecycleOwner, OnTileClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.lifecycleOwner = lifecycleOwner;
        }

        /**
         * Render the row's title and keep its value text in sync with {@code data}. The
         * switch translates the SDK-format string into a friendly label by looking it up
         * in the matching {@link ConfigData} option list (e.g. {@code "0"} → {@code "S0"}
         * for Session). Settings without an option list fall through to the default arm
         * and display the raw value.
         */
        void bind(Configuration configuration, LiveData<String> data) {
            binding.setName(configuration.displayName());

            data.observe(lifecycleOwner, value -> {
                ConfigData configData;
                switch (configuration) {
                    case SESSION:
                        configData = ConfigData.findByValue(ConfigData.session(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case SEARCH_MODE:
                        configData = ConfigData.findByValue(ConfigData.searchMode(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case LINK_PROFILE:
                        configData = ConfigData.findByValue(ConfigData.linkProfile(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case TARGET:
                        configData = ConfigData.findByValue(ConfigData.target(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case ACTION:
                        configData = ConfigData.findByValue(ConfigData.action(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case BANK:
                        configData = ConfigData.findByValue(ConfigData.banks(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case INVENTORY_RESPONSE:
                        configData = ConfigData.findByValue(ConfigData.inventoryResponse(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case VOLUME:
                        configData = ConfigData.findByValue(ConfigData.volume(), value);
                        binding.setValue(configData != null ? configData.name : "unknown");
                        break;
                    case KEY_MAP:
                        binding.setValue(KeyMap.describe(value));
                        break;
                    case SUSPEND_TIME:
                        binding.setValue(String.format("%s min", value));
                        break;
                    default:
                        binding.setValue(value);
                        break;
                }
            });

            binding.getRoot().setOnClickListener(v -> listener.onClickedConfiguration(configuration));
        }
    }

    public static class SwitchViewHolder extends RecyclerView.ViewHolder {

        private final ItemSwitchBinding binding;
        private final ConfigViewModel viewModel;
        private final LifecycleOwner lifecycleOwner;

        public SwitchViewHolder(ItemSwitchBinding binding, ConfigViewModel viewModel, LifecycleOwner lifecycleOwner) {
            super(binding.getRoot());
            this.binding = binding;
            this.viewModel = viewModel;
            this.lifecycleOwner = lifecycleOwner;
        }

        /**
         * Render the toggle's label and wire it to {@code data}. {@link Configuration#CONTINUOUS}
         * is the odd one out — the SDK encodes its on/off as {@code "c"}/{@code "p"} (continuous /
         * pulse) instead of the {@code "1"}/{@code "0"} every other boolean setting uses, so both
         * the write listener and the read observer special-case it.
         */
        void bind(Configuration configuration, LiveData<String> data) {
            binding.setName(configuration.displayName());

            binding.valueSwitch.setOnCheckedChangeListener((view, isChecked) -> {
                        if (configuration == Configuration.CONTINUOUS)
                            viewModel.setConfiguration(configuration, (isChecked) ? "c" : "p");
                        else
                            viewModel.setConfiguration(configuration, (isChecked) ? "1" : "0");
                    }
            );

            data.observe(lifecycleOwner, value -> binding.setEnabled((configuration == Configuration.CONTINUOUS) ? value.equals("c") : value.equals("1")));
        }
    }
}
