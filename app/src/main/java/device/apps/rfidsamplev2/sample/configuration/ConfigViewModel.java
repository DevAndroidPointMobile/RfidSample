package device.apps.rfidsamplev2.sample.configuration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.data.Configuration;
import device.sdk.rfid.RFIDController;

public class ConfigViewModel extends ViewModel {

    private final ExecutorService _executorService = Executors.newSingleThreadExecutor();

    private RFIDController controller = RFIDController.getInstance();

    public Map<Configuration, MutableLiveData<String>> configurations = new HashMap<>();

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
     * Fetch all the values declared in the enum class Configuration from RF88
     */
    public void load() {
        _executorService.execute(() -> {
            for (Configuration value : Configuration.values()) {
                final String currentValue = loadConfiguration(value);
                setConfiguration(value, currentValue);
                waits(200L);
            }
        });
    }

    /**
     * Set the values currently held by the LiveData to RF88
     */
    public void apply() {
        _executorService.execute(() -> {
            for (Configuration value : Configuration.values()) {
                final LiveData<String> target = getConfiguration(value);
                applyConfiguration(value, target.getValue());
                waits(200L);
            }
        });
    }

    /**
     * Perform a factory default reset.
     */
    public void factoryDefault() {
        _executorService.execute(() -> {
            controller.defaultAll();
            load();
        });
    }

    /**
     * Call the API corresponding to the provided Configuration argument and return the result
     *
     * @param configuration target configuration
     * @return current value
     */
    private String loadConfiguration(Configuration configuration) {
        switch (configuration) {
            case START_Q:
                return controller.getStartQ();
            case MIN_Q:
                return controller.getMinQ();
            case MAX_Q:
                return controller.getMaxQ();
            case TARGET:
                return controller.getTarget();
            case ACTION:
                return controller.getAction();
            case ACCESS_PASSWORD:
                return controller.getAccessPassword();
            case BANK:
                return controller.getBank();
            case POINTER:
                return controller.getPointer();
            case VOLUME:
                return controller.getBuzzerVolume();
            case SUSPEND_TIME:
                return controller.getIntoSleepModeTime();
            case KEY_MAP:
                return controller.getTriggerKeymap();
            case INVENTORY_RESPONSE:
                return controller.getPacketOption();
            case INCREMENT_Q:
                return controller.getIncrementQ();
            case DECREMENT_Q:
                return controller.getDecrementQ();
            case FIXED_Q:
                return controller.getFixedQ();
            case CONTINUOUS:
                return controller.getContinuousMode();
            case VIBRATE:
                return controller.getVibrate();
            case LINK_PROFILE:
                return controller.getLinkProfile();
            case POWER:
                return controller.getPower();
            case SESSION:
                return controller.getSession();
            case SEARCH_MODE:
                return controller.getSearchMode();
            default:
                throw new IllegalArgumentException("Unexpected value: " + configuration);
        }
    }

    /**
     * Call the API corresponding to the provided Configuration argument and set the second argument, `newValue`, in that API
     *
     * @param configuration target configuration
     * @param newValue      new value for RF88 configuration
     * @return result code
     */
    private int applyConfiguration(Configuration configuration, String newValue) {
        switch (configuration) {
            case START_Q:
                return controller.setStartQ(newValue);
            case MIN_Q:
                return controller.setMinQ(newValue);
            case MAX_Q:
                return controller.setMaxQ(newValue);
            case TARGET:
                return controller.setTarget(newValue);
            case ACTION:
                return controller.setAction(newValue);
            case BANK:
                return controller.setBank(newValue);
            case POINTER:
                return controller.setPointer(newValue);
            case ACCESS_PASSWORD:
                return controller.setAccessPassword(newValue);
            case VOLUME:
                return controller.setBuzzerVolume(newValue);
            case SUSPEND_TIME:
                return controller.setIntoSleepModeTime(newValue);
            case KEY_MAP:
                return controller.setTriggerKeymap(newValue);
            case INVENTORY_RESPONSE:
                return controller.setPacketOption(newValue);
            case INCREMENT_Q:
                return controller.setIncrementQ(newValue);
            case DECREMENT_Q:
                return controller.setDecrementQ(newValue);
            case FIXED_Q:
                return controller.setFixedQ(newValue);
            case CONTINUOUS:
                return controller.setContinuousMode(newValue);
            case VIBRATE:
                return controller.setVibrate(newValue);
            case LINK_PROFILE:
                return controller.setLinkProfile(newValue);
            case POWER:
                return controller.setPower(newValue);
            case SESSION:
                return controller.setSession(newValue);
            case SEARCH_MODE:
                return controller.setSearchMode(newValue);
            default:
                throw new IllegalArgumentException("Unexpected value: " + configuration);
        }
    }

    /**
     * Put the thread into a sleep state for the duration specified by the passed value
     *
     * @param millis wait millis
     */
    private void waits(long millis) {
        try {
            Thread.sleep(millis);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
