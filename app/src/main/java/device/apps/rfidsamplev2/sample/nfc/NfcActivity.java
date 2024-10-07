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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import device.apps.rfidsamplev2.BaseViewModel;
import device.apps.rfidsamplev2.RFIDSampleV2;
import device.apps.rfidsamplev2.databinding.ActivityBluetoothBinding;
import device.apps.rfidsamplev2.databinding.ActivityNfcBinding;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothActivity;
import device.apps.rfidsamplev2.sample.bluetooth.BluetoothViewModel;
import device.apps.rfidsamplev2.sample.bluetooth.ui.DevicesAdapter;
import device.sdk.rfid.RFIDController;
import device.sdk.rfid.data.enums.state.ConnectState;

public class NfcActivity extends AppCompatActivity {

    private final RFIDController _controller = RFIDController.getInstance();

    private BaseViewModel _viewModel;
    private BluetoothManager _manager;
    private NfcAdapter _adapter;
    private PendingIntent _pendingIntent;
    private ActivityNfcBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializationViewModel();
        initializationContentView();
        initializationNfcFunction();
        observeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _adapter.enableForegroundDispatch(this, _pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        _adapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() == null)
            return;

        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED))
            analyzerNdefMessage(intent);
    }

    public void disconnect(View view) {
        _controller.disconnect();
    }

    /**
     * Initialize the View model
     */
    private void initializationViewModel() {
        _viewModel = ((RFIDSampleV2) getApplication()).getBaseViewModel();
    }

    /**
     * Initialize the views used on the activity
     */
    private void initializationContentView() {
        _binding = ActivityNfcBinding.inflate(getLayoutInflater());
        _binding.setActivity(this);
        setContentView(_binding.getRoot());
    }

    /**
     * Obseve the data used on the screen and provide it to the view using data binding
     */
    private void observeData() {
        _viewModel.connectState.observe(this, state -> {
            _binding.setState(state.name());
            _binding.setIsConnected(state == ConnectState.CONNECTED);
        });
    }

    /**
     *
     */
    private void initializationNfcFunction() {
        _manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        _adapter = NfcAdapter.getDefaultAdapter(this);

        final Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        _pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
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
                        final BluetoothDevice device = _manager.getAdapter().getRemoteDevice(address);
                        _controller.connect(device);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Convert the byte array passed as an argument to a usable hexadecimal format and return it
     *
     * @param data mac adress byte array
     * @return Strings mac adress
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
     * @return revesed byte array
     */
    private byte[] reverseArray(byte[] array) {
        byte[] reversedArray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversedArray[i] = array[array.length - 1 - i];
        }
        return reversedArray;
    }
}