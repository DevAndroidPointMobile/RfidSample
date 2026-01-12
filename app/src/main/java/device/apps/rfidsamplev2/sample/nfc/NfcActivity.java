package device.apps.rfidsamplev2.sample.nfc;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityNfcBinding;
import ex.dev.sdk.rf88.Rf88Manager;
import ex.dev.sdk.rf88.domain.enums.DeviceConnectionState;

public class NfcActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

    private BaseViewModel viewModel;
    private BluetoothManager manager;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private ActivityNfcBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        initializationNfcFunction();
        observeData();
        setupToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // NFC 어댑터 null 체크
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // NFC 어댑터 null 체크
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() == null)
            return;

        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED))
            analyzerNdefMessage(intent);
    }

    /**
     * Disconnect from the Bluetooth device
     *
     * @param view Button view
     */
    public void disconnect(View view) {
        rf88Manager.disconnect();
    }

    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        viewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        binding = ActivityNfcBinding.inflate(getLayoutInflater());
        binding.setActivity(this);
        setContentView(binding.getRoot());
    }

    /**
     * Observe the data used on the screen and provide it to the view using data binding
     */
    private void observeData() {
        viewModel.connectState.observe(this, state -> {
            binding.setState(state.name());
            binding.setIsConnected(state == DeviceConnectionState.CONNECTED);
        });
    }

    /**
     * Initialize NFC functionality and check NFC support
     */
    private void initializationNfcFunction() {
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // NFC 지원 여부 확인
        if (nfcAdapter == null) {
            showNfcNotSupportedDialog();
            return;
        }

        // NFC 활성화 여부 확인
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC in your device settings", Toast.LENGTH_LONG).show();
        }

        // PendingIntent 설정
        final Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Show dialog when NFC is not supported
     */
    private void showNfcNotSupportedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NFC Not Supported")
                .setMessage("This device does not support NFC.\n\nPlease use Bluetooth or Wire connection instead.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Extract the NdefMessage object from the Intent received via ACTION_NDEF_DISCOVERED and
     * attempt to connect to the Bluetooth device based on the information in that object
     *
     * @param intent Android intent
     */
    private void analyzerNdefMessage(Intent intent) {
        final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (rawMessages != null) {
            final List<NdefMessage> messages = new ArrayList<>();
            for (Parcelable rawMessage : rawMessages)
                if (rawMessage instanceof NdefMessage)
                    messages.add((NdefMessage) rawMessage);

            if (!messages.isEmpty()) {
                final NdefRecord record = messages.get(0).getRecords()[0];
                final String mimeType = record.toMimeType();
                if (mimeType != null && mimeType.contains("application/vnd.bluetooth.ep.oob")) {
                    try {
                        final String address = parseMacAddressToNFC(record.getPayload());
                        final BluetoothDevice device = manager.getAdapter().getRemoteDevice(address);
                        executorService.execute(() -> rf88Manager.connect(device.getAddress()));

                        Toast.makeText(this, "Connecting to: " + address, Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    /**
     * Convert the byte array passed as an argument to a usable hexadecimal format and return it
     *
     * @param data mac address byte array
     * @return Strings mac address
     */
    private String parseMacAddressToNFC(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        short oobLength = buffer.getShort();
        byte[] macAddressBytes = Arrays.copyOfRange(data, 2, 8);
        byte[] reversedMacAddressBytes = reverseArray(macAddressBytes);
        StringBuilder macAddress = new StringBuilder();
        for (int i = 0; i < reversedMacAddressBytes.length; i++) {
            if (i > 0)
                macAddress.append(":");

            macAddress.append(String.format("%02X", reversedMacAddressBytes[i]));
        }

        return macAddress.toString();
    }

    /**
     * Reverse the byte array passed as an argument
     *
     * @param array target byte array
     * @return reversed byte array
     */
    private byte[] reverseArray(byte[] array) {
        byte[] reversedArray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversedArray[i] = array[array.length - 1 - i];
        }
        return reversedArray;
    }
}