package device.apps.rfidsamplev2.sample.nfc;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ex.dev.sdk.rf88.Rf88Manager;

/**
 * State holder for {@link NfcActivity}.
 *
 * <p>Owns the RF88 SDK calls and the NDEF parsing logic so the Activity is left with
 * just the Android-NFC framework integration (foreground dispatch, intent handling).
 *
 * <p>SDK calls run on a single-thread executor; the executor is shut down when the
 * ViewModel is finally cleared.
 */
public class NfcViewModel extends ViewModel {

    /** MIME type used by the Bluetooth Out-Of-Band pairing handover NDEF record. */
    private static final String OOB_MIME_TYPE = "application/vnd.bluetooth.ep.oob";

    /** Minimum OOB payload length we can extract a MAC from: 2-byte length header + 6-byte MAC. */
    private static final int MIN_OOB_PAYLOAD_LENGTH = 8;

    /** Background dispatcher for blocking SDK calls. */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /** Singleton entry point of the RF88 SDK. */
    private final Rf88Manager rf88Manager = Rf88Manager.getInstance();

    /**
     * Extract the Bluetooth MAC address from an {@code ACTION_NDEF_DISCOVERED} intent if
     * the tag carries a Bluetooth Out-Of-Band pairing handover record.
     *
     * @param intent the intent delivered to {@code onNewIntent}
     * @return the MAC address as a colon-separated upper-case string, or {@code null} when
     *         the intent does not contain a usable Bluetooth handover record
     */
    @Nullable
    public String parseMacFromOobIntent(Intent intent) {
        final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null || rawMessages.length == 0) return null;
        if (!(rawMessages[0] instanceof NdefMessage)) return null;

        final NdefRecord[] records = ((NdefMessage) rawMessages[0]).getRecords();
        if (records.length == 0) return null;

        final NdefRecord first = records[0];
        final String mimeType = first.toMimeType();
        if (mimeType == null || !mimeType.contains(OOB_MIME_TYPE)) return null;

        final byte[] payload = first.getPayload();
        // Defensive — a malformed tag with the OOB MIME type but a truncated payload would
        // otherwise crash inside the parser. Treat it like a non-OOB tag.
        if (payload == null || payload.length < MIN_OOB_PAYLOAD_LENGTH) return null;

        return parseMacAddressFromOobPayload(payload);
    }

    /**
     * Connect the RF88 SDK to the Bluetooth device at the given address. The SDK call
     * runs on the background executor; the Activity learns about the result by
     * observing {@code Rf88ConnectionRepository#connectState}.
     *
     * @param address Bluetooth MAC address parsed from the NFC tag
     */
    public void connect(String address) {
        executorService.execute(() -> rf88Manager.connect(address));
    }

    /**
     * Disconnect the RF88 SDK from the currently connected device.
     */
    public void disconnect() {
        executorService.execute(rf88Manager::disconnect);
    }

    /**
     * Shut down the background executor when the ViewModel is finally cleared by the
     * framework (e.g. when the host Activity finishes for good).
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    /**
     * Decode a Bluetooth OOB handover payload into a colon-separated MAC address.
     *
     * <p>Layout of the payload:
     * <pre>
     *   bytes [0..1]  total length (little-endian, unused here)
     *   bytes [2..7]  MAC address bytes (little-endian — must be reversed)
     *   bytes [8..]   optional EIR fields (ignored)
     * </pre>
     */
    private String parseMacAddressFromOobPayload(byte[] payload) {
        final byte[] macBytes = Arrays.copyOfRange(payload, 2, 8);
        final byte[] reversed = reverse(macBytes);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < reversed.length; i++) {
            if (i > 0) builder.append(":");
            builder.append(String.format("%02X", reversed[i]));
        }
        return builder.toString();
    }

    /**
     * Return a new array with the bytes in reverse order. Used to flip the
     * little-endian MAC address bytes coming from the OOB payload.
     */
    private byte[] reverse(byte[] array) {
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[array.length - 1 - i];
        }
        return result;
    }
}
