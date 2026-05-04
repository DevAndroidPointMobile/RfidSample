package device.apps.rfidsamplev2.sample.configuration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.data.Configuration;
import ex.dev.sdk.rf88.Rf88Manager;

/**
 * State holder for {@link ConfigActivity}.
 *
 * <p>Owns one {@link MutableLiveData} per {@link Configuration} entry plus a single
 * {@link #busy} flag for the progress dialog. All SDK calls go through a single-thread
 * executor so the main thread is never blocked and the order of getters / setters stays
 * deterministic across {@link #load()}, {@link #apply()}, and {@link #factoryDefault()}.
 *
 * <p>The two {@code switch} dispatchers ({@link #loadConfiguration} and
 * {@link #applyConfiguration}) are intentionally explicit — a reader can see exactly
 * which SDK call backs each setting on the screen. Wiring this through the enum would
 * be shorter but would force readers to chase method references to learn the same fact.
 */
public class ConfigViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

    /** Per-setting LiveData map; consumers go through {@link #getConfiguration(Configuration)} only. */
    private final Map<Configuration, MutableLiveData<String>> configurations = new HashMap<>();

    /** True while a load / apply / factory-default cycle is in flight; drives the modal progress dialog. */
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    private void setBusy(boolean value) {
        busy.postValue(value);
    }

    /**
     * Initialize the configuration LiveData and fetch the configuration values from RF88
     */
    public void launch() {
        for (Configuration value : Configuration.values())
            configurations.put(value, new MutableLiveData<>("0"));

        load();
    }

    /**
     * Return the LiveData for the configuration passed as an argument
     *
     * @param configuration target configuration
     * @return live data
     */
    public LiveData<String> getConfiguration(Configuration configuration) {
        return configurations.get(configuration);
    }

    /**
     * Set the second argument, `newValue`, to the LiveData of the `configuration` passed as an argument
     *
     * @param configuration target configuration
     * @param newValue      new value for target configuration
     */
    public void setConfiguration(Configuration configuration, String newValue) {
        final MutableLiveData<String> target = configurations.get(configuration);
        target.postValue(newValue);
    }

    /**
     * Fetch every value declared in {@link Configuration} from the RF88 reader and
     * push it into the corresponding LiveData. Runs on the background executor and
     * holds {@link #busy} for the whole pass so the UI shows a single progress
     * dialog from start to finish.
     */
    public void load() {
        executorService.execute(() -> {
            setBusy(true);
            loadAll();
            setBusy(false);
        });
    }

    /**
     * Push every value currently held by the LiveData map back to the RF88 reader.
     */
    public void apply() {
        executorService.execute(() -> {
            setBusy(true);
            for (Configuration value : Configuration.values()) {
                final LiveData<String> target = getConfiguration(value);
                applyConfiguration(value, target.getValue());
                waits(200L);
            }
            setBusy(false);
        });
    }

    /**
     * Reset the reader to factory defaults and immediately reload every value so the
     * UI reflects the new state. Both steps run in a single background task — calling
     * {@link #load()} from here would queue a separate task and toggle {@link #busy}
     * twice, causing the progress dialog to flicker.
     */
    public void factoryDefault() {
        executorService.execute(() -> {
            setBusy(true);
            rf88Manager.factoryDefaults();
            loadAll();
            setBusy(false);
        });
    }

    /**
     * Allow in-flight SET / GET calls to drain but reject any new submissions. Called
     * from {@link #onCleared()} so an Activity that finishes mid-apply doesn't leave
     * the reader in a partially-applied state, while preventing later coroutines from
     * piling up against a dead ViewModel.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    /** Sequentially read every {@link Configuration} from the reader into its LiveData. */
    private void loadAll() {
        for (Configuration value : Configuration.values()) {
            final String currentValue = loadConfiguration(value);
            setConfiguration(value, currentValue);
            waits(200L);
        }
    }

    /**
     * Dispatch table — given a {@link Configuration} key, call the matching SDK getter
     * and return its raw string value. Kept as an explicit switch rather than wired
     * through the enum so a reader can see, in one place, which SDK call backs each
     * setting on the screen.
     */
    private String loadConfiguration(Configuration configuration) {
        switch (configuration) {
            case START_Q:
                return rf88Manager.getStartQ();
            case MIN_Q:
                return rf88Manager.getMinQ();
            case MAX_Q:
                return rf88Manager.getMaxQ();
            case TARGET:
                return rf88Manager.getTarget();
            case ACTION:
                return rf88Manager.getAction();
            case ACCESS_PASSWORD:
                return rf88Manager.getAccessPassword();
            case BANK:
                return rf88Manager.getMemoryBank();
            case POINTER:
                return rf88Manager.getPointer();
            case VOLUME:
                return rf88Manager.getBuzzerVolume();
            case SUSPEND_TIME:
                return rf88Manager.getSuspendTimeout();
            case KEY_MAP:
                return rf88Manager.getDualTriggerFunctionCode();
            case INVENTORY_RESPONSE:
                return rf88Manager.getPacketOption();
            case INCREMENT_Q:
                return rf88Manager.getIncrementQ();
            case DECREMENT_Q:
                return rf88Manager.getDecrementQ();
            case FIXED_Q:
                return rf88Manager.getFixedQ();
            case CONTINUOUS:
                return rf88Manager.getContinuousMode();
            case VIBRATE:
                return rf88Manager.getVibratorMode();
            case LINK_PROFILE:
                return rf88Manager.getLinkProfile();
            case POWER:
                return rf88Manager.getPower();
            case SESSION:
                return rf88Manager.getSession();
            case SEARCH_MODE:
                return rf88Manager.getSearchMode();
            default:
                throw new IllegalArgumentException("Unexpected value: " + configuration);
        }
    }

    /**
     * Counterpart to {@link #loadConfiguration(Configuration)} — given a key and a new
     * string value, call the matching SDK setter. Same explicit-switch rationale.
     */
    private String applyConfiguration(Configuration configuration, String newValue) {
        switch (configuration) {
            case START_Q:
                return rf88Manager.setStartQ(newValue);
            case MIN_Q:
                return rf88Manager.setMinQ(newValue);
            case MAX_Q:
                return rf88Manager.setMaxQ(newValue);
            case TARGET:
                return rf88Manager.setTarget(newValue);
            case ACTION:
                return rf88Manager.setAction(newValue);
            case BANK:
                return rf88Manager.setMemoryBank(newValue);
            case POINTER:
                return rf88Manager.setPointer(newValue);
            case ACCESS_PASSWORD:
                return rf88Manager.setAccessPassword(newValue);
            case VOLUME:
                return rf88Manager.setBuzzerVolume(newValue);
            case SUSPEND_TIME:
                return rf88Manager.setSuspendTimeout(newValue);
            case KEY_MAP:
                return rf88Manager.setDualTriggerFunctionCode(newValue);
            case INVENTORY_RESPONSE:
                return rf88Manager.setPacketOption(newValue);
            case INCREMENT_Q:
                return rf88Manager.setIncrementQ(newValue);
            case DECREMENT_Q:
                return rf88Manager.setDecrementQ(newValue);
            case FIXED_Q:
                return rf88Manager.setFixedQ(newValue);
            case CONTINUOUS:
                return rf88Manager.setContinuousMode(newValue);
            case VIBRATE:
                return rf88Manager.setVibratorMode(newValue);
            case LINK_PROFILE:
                return rf88Manager.setLinkProfile(newValue);
            case POWER:
                return rf88Manager.setPower(newValue);
            case SESSION:
                return rf88Manager.setSession(newValue);
            case SEARCH_MODE:
                return rf88Manager.setSearchMode(newValue);
            default:
                throw new IllegalArgumentException("Unexpected value: " + configuration);
        }
    }

    private void waits(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
