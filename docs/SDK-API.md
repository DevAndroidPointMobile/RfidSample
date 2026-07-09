# RF88 SDK API Reference

SDK: `ex.dev.sdk.rf88` — aar `ex.dev.sdk.rf88_3.1.3`.
Generated Javadoc (secondary, HTML): [../javadoc/index.html](../javadoc/index.html).

**Source of this document:** the aar itself (`javap -p` dump of every public
member of `Rf88Manager`, `RfidUtils`, and `ResultCallback` — see
`.superpowers/sdd/aar-api-dump.txt`), not the generated javadoc. The
generated javadoc under-documents the SDK: it lists 95 `Rf88Manager` methods,
while the aar exposes 115 public members (96 distinct member names — 95
methods plus the `DEBUG` field — once overloads are collapsed) — including
all 20 `*Async` variants,
`getPartNumber()`, the `String`-based overloads of `lock`/`lockAsync`,
`getScannerTriggerMode()`/`setScannerTriggerMode(String)`, `stop()`, and the
`DEBUG` field. Conversely, a handful of names the javadoc documents
(`getConfigurations`, `setConfigurations`, `getInformation`,
`getScannerOutputMode`, `setScannerOutputMode`) do **not** appear in the
3.1.3 aar at all and are omitted here as stale/renamed. Every table below is
driven off the aar dump; the javadoc is used only as a source of
descriptions where it documents a matching method. Signatures elide the
`java.lang.`/`java.util.` package qualification for readability but
otherwise match the dump exactly (types, arity, order).

## Threading (applies to every method below — replaces any per-method column)

- **Every synchronous method that talks to the device is blocking** — the
  javadoc consistently describes `read`/`write`/similar operations as
  synchronous operations that wait on the device over UART/Bluetooth. None
  of them may be called on the Android main/UI thread.
- Dispatch synchronous calls on a background `Executor`. This is exactly
  what the sample app does: every ViewModel and `Rf88ConnectionManager`
  create a `newSingleThreadExecutor` and post all SDK calls to it.
- Alternatively, use the `*Async` variant of the same operation where one
  exists (20 such variants in this SDK build — see the "async" rows in each
  table below). Async variants run the device I/O off the calling thread
  internally and deliver the outcome via
  `ex.dev.sdk.rf88.frameworks.listener.ResultCallback<T>`:
  `onSuccess(T)` on success, `onFailure(Exception)` on failure. Register
  callbacks are invoked on whatever thread the SDK's internal worker uses —
  treat them as a background thread and hop back to the main thread yourself
  before touching UI.
- There is **no per-method main-thread-safety data in any source** (dump or
  javadoc). A prior version of this document inferred one method-by-method;
  that inference has been removed. Do not add it back — apply the blanket
  rule above uniformly instead.

## `Rf88Manager` (singleton — `getInstance()`)

### Lifecycle & connection

| Method | Returns | Description |
|---|---|---|
| `getInstance()` | `Rf88Manager` (static) | Returns the singleton instance of the RF88 Manager. |
| `connect()` | `void` | Attempts to connect to the RF88 device via UART communication. Connection state changes are reported through `OnConnectionStateChangedListener`. |
| `connect(String)` | `void` | Attempts to connect to the RF88 device via Bluetooth communication (`address`). Connection state changes are reported through `OnConnectionStateChangedListener`. |
| `connectAsync(ResultCallback<Void>)` | `void` | Asynchronous variant of `connect()`; result delivered via `ResultCallback`. aar-only — not documented in the generated javadoc. |
| `connectAsync(String, ResultCallback<Void>)` | `void` | Asynchronous variant of `connect(String)` (`address`); result delivered via `ResultCallback`. aar-only — not documented in the generated javadoc. |
| `disconnect()` | `void` | Disconnects from the currently connected RF88 device and cleans up associated resources. Reported through `OnConnectionStateChangedListener`. |
| `disconnectAsync(ResultCallback<Void>)` | `void` | Asynchronous variant of `disconnect()`; result delivered via `ResultCallback`. aar-only — not documented in the generated javadoc. |
| `getConnectionType()` | `String` | Gets the current connection type between the host and the RF88 device (e.g. `"UART"`, `"BT"`). |
| `stop()` | `void` | Stops the currently running RFID operation (inventory, read, write, kill, or lock). |

### Listeners

| Method | Returns | Description |
|---|---|---|
| `setOnConnectionStateChangedListener(OnConnectionStateChangedListener)` | `void` | Sets a listener to receive connection state change events (connected, disconnected, etc.), or `null` to remove. |
| `setOnHardwareKeyListener(OnHardwareKeyListener)` | `void` | Sets a listener to receive hardware key (trigger) events from the RF88 device, or `null` to remove. Key mapping is configured via `setDualTriggerFunctionCode(String)`. |
| `setOnInventoryResultListener(OnInventoryResultListener)` | `void` | Sets a listener to receive RFID inventory operation results (EPC, RSSI, and other configured packet options), or `null` to remove. |
| `setOnActionExecutingListener(OnActionExecutingListener)` | `void` | Sets a listener to receive notifications when actions are being executed, useful for progress/status indicators, or `null` to remove. |

### Tag operations — sync

| Method | Returns | Description |
|---|---|---|
| `inventory()` | `void` | Executes a continuous RFID inventory operation to discover all visible tags; results delivered via `OnInventoryResultListener`. Continues until `stop()` is called. |
| `inventory(String)` | `void` | Executes an RFID inventory operation with tag filtering (`mask`), using the current memory-bank/pointer configuration. |
| `inventoryAndRead(String, String, String)` | `void` | Executes a continuous inventory operation and, for each discovered tag, immediately reads the memory area (`memoryBank, pointer, length`). Continues until `stop()` is called. |
| `inventoryAndRead(String, String, String, String)` | `void` | Same as the 3-arg overload with tag filtering added (`memoryBank, pointer, length, mask`). |
| `read(String, String, String)` | `String` | Synchronously reads data from an RFID tag's memory bank (`memoryBank, pointer, length`); if multiple tags are present, reads the first responding tag. Returns the hex string read, or empty string on failure. |
| `read(String, String, String, String)` | `String` | Synchronously reads data from a specific tag's memory bank using tag filtering (`memoryBank, pointer, length, mask`). |
| `write(String, String, String)` | `String` | Synchronously writes data to an RFID tag's memory bank (`memoryBank, pointer, newValue`); if multiple tags are present, writes to the first responding tag. Returns success/fail status. |
| `write(String, String, String, String)` | `String` | Synchronously writes data to a specific tag's memory bank using tag filtering (`memoryBank, pointer, newValue, mask`). |
| `writeAccessPassword(String)` | `String` | Writes the access password (`newValue`, up to 8 hex chars / 32 bits) to word offset 2 of the reserved memory bank; required for future read/write of a locked tag. |
| `writeAccessPassword(String, String)` | `String` | Same as the 1-arg overload with tag filtering added (`newValue, mask`). |
| `writeKillPassword(String)` | `String` | Writes the kill password (`newValue`, up to 8 hex chars / 32 bits) to word offset 0 of the reserved memory bank; required by `kill(String)`. |
| `writeKillPassword(String, String)` | `String` | Same as the 1-arg overload with tag filtering added (`newValue, mask`). |
| `kill(String)` | `String` | Permanently disables an RFID tag using its kill password (`password`). Irreversible — makes the tag unresponsive to all future RFID operations. |
| `kill(String, String)` | `String` | Same as the 1-arg overload with tag filtering added (`password, mask`). Irreversible. |
| `lock(LockMemoryMask, LockActionMask)` | `String` | Locks specific memory regions of an RFID tag (`memoryMask, actionMask`) per EPC Gen2 Table 6.50 ("Lock Action-field functionality"). |
| `lock(LockMemoryMask, LockActionMask, String)` | `String` | Same as the 2-arg overload with tag filtering added (`memoryMask, actionMask, mask`). |
| `lock(String, String)` | `String` | Raw-`String` overload of `lock(LockMemoryMask, LockActionMask)` — `memoryMask`/`actionMask` passed as hex strings instead of the enum types. aar-only — not documented in the generated javadoc. |
| `lock(String, String, String)` | `String` | Raw-`String` overload of `lock(LockMemoryMask, LockActionMask, String)` — `memoryMask`/`actionMask` as hex strings, plus tag filtering (`mask`). aar-only — not documented in the generated javadoc. |
| `getAction()` | `String` | Gets the current action value applied to the RFID module ("0"–"5"); see EPC Gen2 Table 6.29 ("Select"). |
| `setAction(String)` | `String` | Sets the action value for the RFID module (`newValue`, "0"–"5"). |

### Tag operations — async

All rows deliver the outcome via `ex.dev.sdk.rf88.frameworks.listener.ResultCallback<T>` (`onSuccess(T)` / `onFailure(Exception)`) instead of blocking the caller. None are documented in the generated javadoc (it predates/omits every `*Async` method); descriptions here are terse and factual, inferred from the sync counterpart's documented behavior.

| Method | Returns | Description |
|---|---|---|
| `readAsync(String, String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `read(String, String, String)`. |
| `readAsync(String, String, String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `read(String, String, String, String)`. |
| `writeAsync(String, String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `write(String, String, String)`. |
| `writeAsync(String, String, String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `write(String, String, String, String)`. |
| `writeAccessPasswordAsync(String, ResultCallback<String>)` | `void` | Asynchronous variant of `writeAccessPassword(String)`. |
| `writeAccessPasswordAsync(String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `writeAccessPassword(String, String)`. |
| `writeKillPasswordAsync(String, ResultCallback<String>)` | `void` | Asynchronous variant of `writeKillPassword(String)`. |
| `writeKillPasswordAsync(String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `writeKillPassword(String, String)`. |
| `killAsync(String, ResultCallback<String>)` | `void` | Asynchronous variant of `kill(String)`. Irreversible. |
| `killAsync(String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `kill(String, String)`. Irreversible. |
| `lockAsync(LockMemoryMask, LockActionMask, ResultCallback<String>)` | `void` | Asynchronous variant of `lock(LockMemoryMask, LockActionMask)`. |
| `lockAsync(LockMemoryMask, LockActionMask, String, ResultCallback<String>)` | `void` | Asynchronous variant of `lock(LockMemoryMask, LockActionMask, String)`. |
| `lockAsync(String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `lock(String, String)`. aar-only — not documented in the generated javadoc. |
| `lockAsync(String, String, String, ResultCallback<String>)` | `void` | Asynchronous variant of `lock(String, String, String)`. aar-only — not documented in the generated javadoc. |

### RFID configuration — getters & setters

| Method | Returns | Description |
|---|---|---|
| `getPower()` | `String` | Gets the current transmit power value applied to the RFID module (0–300). |
| `setPower(String)` | `String` | Sets the transmit power value for the RFID module (`newValue`, 0–300). Higher values increase read range but also power consumption. |
| `getPowerAsync(ResultCallback<String>)` | `void` | Asynchronous variant of `getPower()`. aar-only — not documented in the generated javadoc. |
| `setPowerAsync(String, ResultCallback<String>)` | `void` | Asynchronous variant of `setPower(String)`. aar-only — not documented in the generated javadoc. |
| `getFixedQ()` | `String` | Gets the activation status of the Fixed Q algorithm (Q value held constant during inventory rather than dynamically adjusted). |
| `setFixedQ(String)` | `String` | Sets whether the RFID module will activate the Fixed Q algorithm (`newValue`, "1"/"0"). |
| `getMinQ()` | `String` | Gets the current Min Q value (lower bound for the dynamic Q algorithm in EPC Gen2 singulation), 0–15. |
| `setMinQ(String)` | `String` | Sets the Min Q value for the RFID module (`newValue`, 0–15). |
| `getMaxQ()` | `String` | Gets the current Max Q value (upper bound for the dynamic Q algorithm), 0–15. |
| `setMaxQ(String)` | `String` | Sets the Max Q value for the RFID module (`newValue`, 0–15). |
| `getStartQ()` | `String` | Gets the current Start Q value (initial Q for the EPC Gen2 singulation algorithm), 0–15. |
| `setStartQ(String)` | `String` | Sets the Start Q value for the RFID module (`newValue`, 0–15). |
| `getIncrementQ()` | `String` | Gets the activation status of the Increment Q algorithm (auto-increment Q on collision during inventory). |
| `setIncrementQ(String)` | `String` | Sets whether the RFID module will activate the Increment Q algorithm (`newValue`, "1"/"0"). |
| `getDecrementQ()` | `String` | Gets the activation status of the Decrement Q algorithm (auto-decrement Q when no tag responses are detected). |
| `setDecrementQ(String)` | `String` | Sets whether the RFID module will activate the Decrement Q algorithm (`newValue`, "1"/"0"). |
| `getSession()` | `String` | Gets the current session value ("0"–"3") applied to the RFID module; determines tag persistence/inventory behavior per EPC Gen2. |
| `setSession(String)` | `String` | Sets the session value for the RFID module (`newValue`, "0"–"3"). |
| `getTarget()` | `String` | Gets the current target value ("0"–"4": S0/S1/S2/S3 inventoried, or SL) per EPC Gen2 Table 6.29 ("Select"). |
| `setTarget(String)` | `String` | Sets the target value for the RFID module (`newValue`, "0"–"4"). |
| `getRegion()` | `String` | Gets the current regulatory region code of the RFID module. |
| `setRegion(String)` | `String` | Sets the regulatory region for the RFID module (`newValue`); determines frequency band and power limits per local regulation. See `getSupportedRegions()`. |
| `getSupportedRegions()` | `Map<String, String>` | Gets a mapping of supported regulatory region names to region codes for this device (default US band regions if unavailable). |
| `getMemoryBank()` | `String` | Gets the current memory bank setting ("0" Reserved, "1" EPC, "2" TID, "3" User) used for select operations. |
| `setMemoryBank(String)` | `String` | Sets the memory bank value for the RFID module (`newValue`, "0"–"3"). |
| `getLinkProfile()` | `String` | Gets the current link profile value (communication parameters: data rate, modulation, encoding) applied to the RFID module. |
| `setLinkProfile(String)` | `String` | Sets the link profile value for the RFID module (`newValue`). See `getLinkProfile()`. |
| `getContinuousMode()` | `String` | Gets the activation status of continuous mode: `"c"` (double-tap starts continuous inventory) or `"p"` (hold-trigger mode, default). |
| `setContinuousMode(String)` | `String` | Sets whether the RF88 will activate continuous mode (`newValue`, `"c"`/`"p"`). |
| `getSearchMode()` | `String` | Gets the current search mode ("0" Dual Target, "1" Single Target A, "2" Single Target B) per EPC Gen2. |
| `setSearchMode(String)` | `String` | Sets the search mode for the RFID module (`newValue`, "0"–"2"). |
| `getPointer()` | `String` | Gets the current pointer value (starting word offset within the memory bank for select operations) per EPC Gen2 Table 6.29. |
| `setPointer(String)` | `String` | Sets the pointer value for the RFID module (`newValue`, word offset). |
| `getPacketOption()` | `String` | Gets the current packet option value (additive bit flags controlling extra data attached to inventory results: "1" checksum, "2" RSSI, "8" frequency, "32" EPC-only). |
| `setPacketOption(String)` | `String` | Sets the packet option for the RFID module (`newValue`); values combine additively except "32", which cannot be mixed with others. |
| `getAccessPassword()` | `String` | Gets the current access password applied to the RFID module (hex string). |
| `setAccessPassword(String)` | `String` | Sets the access password for the RFID module (`newValue`, hex string up to 8 chars). |
| `getScannerTriggerMode()` | `String` | Gets the current scanner/barcode trigger mode setting of the RFID module. aar-only naming — the generated javadoc documents a differently-named, non-matching pair (`getScannerOutputMode`/`setScannerOutputMode`) that is absent from this aar version; treat as undocumented — see javadoc. |
| `setScannerTriggerMode(String)` | `String` | Sets the scanner/barcode trigger mode for the RFID module (`newValue`). aar-only naming — see note on `getScannerTriggerMode()`. |
| `getSuspendTimeout()` | `String` | Gets the current suspend timeout setting (minutes before the RFID device automatically enters sleep mode). |
| `setSuspendTimeout(String)` | `String` | Sets the suspend timeout for the RF88 (`newValue`, minutes), to conserve battery power. |
| `getTriggerEventMode()` | `String` | Gets the activation status of trigger event reporting (whether trigger press/release events are reported via `OnHardwareKeyListener`). |
| `setTriggerEventMode(String)` | `String` | Sets whether the RF88 will report trigger events (`newValue`). |
| `getDualTriggerFunctionCode()` | `String` | Gets the current trigger key mapping configuration (which operation each of the top/bottom trigger keys invokes). |
| `setDualTriggerFunctionCode(String)` | `String` | Sets the trigger key mapping for the RF88 (`newValue`). |
| `defaultRfidSettings()` | `String` | Resets only the RFID module configurations to factory default values, preserving system-level settings (device name, buzzer volume, etc.). |
| `factoryDefaults()` | `String` | Resets **all** device configurations (RFID and system) to factory default values. Irreversible — erases all custom settings. |

### Device & battery info

| Method | Returns | Description |
|---|---|---|
| `getRfidVersion()` | `String` | Gets the firmware version of the RFID module. |
| `getRfidVersionAsync(ResultCallback<String>)` | `void` | Asynchronous variant of `getRfidVersion()`. aar-only — not documented in the generated javadoc. |
| `getPartNumber()` | `String` | Gets the part number of the RF88 device. aar-only — not documented in the generated javadoc; semantics inferred from naming only — see javadoc. |
| `getDeviceName()` | `String` | Gets the device name embedded in the Bluetooth module of the RF88. |
| `getMacAddress()` | `String` | Gets the MAC address of the Bluetooth module in the RF88 device. |
| `getPowerClass()` | `String` | Gets the power class of the Bluetooth module in the RF88 device. |
| `getBatteryLevel()` | `String` | Gets the current battery level of the RF88 device, as a percentage string. |
| `getBatteryHealth()` | `String` | Gets the battery health status of the RF88 device. |
| `getBatteryTemperature()` | `String` | Gets the current battery temperature of the RF88 device. |
| `getBatteryVoltage()` | `String` | Gets the current battery voltage of the RF88 device. |
| `getChargingStatus()` | `String` | Gets the charging status of the RF88 device (e.g. `"1:CHARGING"`, `"2:DISCHARGING"`, `"3:FULL"`). |
| `getFirmwareVersion()` | `String` | Gets the current (system) firmware version of the RF88 device. |
| `getBluetoothVersion()` | `String` | Gets the firmware version of the Bluetooth module in the RF88 device. |
| `getHardwareRevision()` | `String` | Gets the hardware revision of the RF88 device. |

### Feedback & misc

| Method | Returns | Description |
|---|---|---|
| `getBuzzerVolume()` | `String` | Gets the current buzzer volume setting of the RF88. |
| `setBuzzerVolume(String)` | `String` | Sets the buzzer volume for the RF88 (`newValue`); the buzzer provides audio feedback for operations/events. |
| `getVibratorMode()` | `String` | Gets the activation status of the vibrator (haptic feedback) for inventory operations. |
| `setVibratorMode(String)` | `String` | Sets whether the RF88 will activate vibrator feedback for inventory operations (`newValue`). |
| `executeBeep()` | `String` | Triggers an immediate beep on the RF88 device using the current buzzer volume setting. |
| `setDebugMode(boolean)` | `void` (static) | Enables or disables SDK debug mode (`isEnabled`); when enabled, detailed logging is emitted for troubleshooting. |
| `DEBUG` | `boolean` (static volatile field) | Current SDK debug-mode flag, toggled by `setDebugMode(boolean)`. aar-only field — no javadoc field-level documentation exists for it (only the private `sInstance` field is documented) — see javadoc. |

Coverage note: every one of the 115 public `Rf88Manager` members listed in
`aar-api-dump.txt` is placed in exactly one of the seven tables above
(9 lifecycle + 4 listeners + 20 sync tag ops + 14 async tag ops + 47 RFID
configuration + 14 device/battery + 7 feedback/misc = 115). None are
dropped into a catch-all "Other" bucket.

## `ex.dev.sdk.rf88.utils.RfidUtils`

All methods are `static`. The dump also lists a public no-arg constructor,
`RfidUtils()`, included below for completeness even though the class only
exposes static utility methods.

| Method | Returns | Description |
|---|---|---|
| `RfidUtils()` | (constructor) | Public no-arg constructor. No description in the generated javadoc — see javadoc. |
| `bytesToHex(byte[])` | `String` | Converts a byte array to its hexadecimal string representation (`bytes`). No description in the generated javadoc — see javadoc. |
| `bytesToHex(List<Byte>)` | `String` | Converts a `List<Byte>` to its hexadecimal string representation (`bytes`). No description in the generated javadoc — see javadoc. |
| `parseResultCode(String)` | `String` | Parses the result code returned by an executed API call into a human-readable message (`resultCode`). Returns the parsed message. |
| `checkPCLength(String, String)` | `boolean` | Checks whether the calculated PC value (`pc`, from `calculatePC`) matches the length of `writeData`. Returns `true` if the length matches, otherwise `false`. |
| `calculatePC(String, boolean, boolean, boolean, String)` | `String` | Calculates the Protocol Control (PC) value (`writeData`; `umi` — user-memory presence flag; `xi` — XPC_W1 protocol support flag; `toggle` — selects attribute vs. AFI; `attributeOrAfi` — attribute or AFI value). Returns the calculated PC value. |
| `REGEX_HEX` | `String` (static final field) | Regex constant used internally for hex-string validation. No description in the generated javadoc (Constant Field Values only) — see javadoc. |

## `ex.dev.sdk.rf88.frameworks.listener.ResultCallback<T>`

Async callback interface used by every `*Async` method above.

| Method | Returns | Description |
|---|---|---|
| `onSuccess(T)` | `void` (abstract) | Invoked when the async operation completes successfully, with the same result value the synchronous counterpart would have returned. |
| `onFailure(Exception)` | `void` (abstract) | Invoked when the async operation fails, with the causing `Exception`. |
