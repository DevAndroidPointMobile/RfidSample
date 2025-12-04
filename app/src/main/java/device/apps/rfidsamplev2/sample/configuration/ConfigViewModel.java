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

public class ConfigViewModel extends ViewModel {

    private final ExecutorService _executorService = Executors.newSingleThreadExecutor();
    private Rf88Manager controller = Rf88Manager.getInstance();

    public Map<Configuration, MutableLiveData<String>> configurations = new HashMap<>();

    // 새로 추가: 작업 중 상태를 나타내는 LiveData
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public void setBusy(boolean value) {
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
     * Fetch all the values declared in the enum class Configuration from RF88
     */
    public void load() {
        // 옵션: load 동작도 busy 표시하도록 변경 가능
        _executorService.execute(() -> {
            setBusy(true);
            for (Configuration value : Configuration.values()) {
                final String currentValue = loadConfiguration(value);
                setConfiguration(value, currentValue);
                waits(200L);
            }
            setBusy(false);
        });
    }

    /**
     * Set the values currently held by the LiveData to RF88
     */
    public void apply() {
        _executorService.execute(() -> {
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
     * Perform a factory default reset.
     */
    public void factoryDefault() {
        _executorService.execute(() -> {
            setBusy(true);
            controller.factoryDefaults();
            // 재로딩까지 하면 사용자에게 진행상태를 보여줌
            load();
            setBusy(false); // load() 내부에서도 setBusy를 조절하므로 중복 가능. 안전을 위해 남겨둠.
        });
    }

    // ... loadConfiguration, applyConfiguration, waits 메서드는 기존과 동일 (생략하지 말고 유지)
    // (기존 코드 붙여넣기)
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
                return controller.getMemoryBank();
            case POINTER:
                return controller.getPointer();
            case VOLUME:
                return controller.getBuzzerVolume();
            case SUSPEND_TIME:
                return controller.getSuspendTimeout();
            case KEY_MAP:
                return controller.getDualTriggerFunctionCode();
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
                return controller.getVibratorMode();
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

    private String applyConfiguration(Configuration configuration, String newValue) {
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
                return controller.setMemoryBank(newValue);
            case POINTER:
                return controller.setPointer(newValue);
            case ACCESS_PASSWORD:
                return controller.setAccessPassword(newValue);
            case VOLUME:
                return controller.setBuzzerVolume(newValue);
            case SUSPEND_TIME:
                return controller.setSuspendTimeout(newValue);
            case KEY_MAP:
                return controller.setDualTriggerFunctionCode(newValue);
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
                return controller.setVibratorMode(newValue);
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

    private void waits(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}