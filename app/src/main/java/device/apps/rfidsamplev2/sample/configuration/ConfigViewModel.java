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

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

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
        executorService.execute(() -> {
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
     * Perform a factory default reset.
     */
    public void factoryDefault() {
        executorService.execute(() -> {
            setBusy(true);
            rf88Manager.factoryDefaults();
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