# RfidSample — Agent Guide

RfidSample is a customer-facing sample app demonstrating RF88 UHF RFID
reader integration on Android (Java, View/DataBinding). It targets the
`ex.dev.sdk.rf88` SDK (aar `ex.dev.sdk.rf88_3.1.4`).

- Full SDK API reference: [SDK-API.md](SDK-API.md)
- Build & environment details: [../README.md](../README.md)
- Generated Javadoc (HTML): [../javadoc/index.html](../javadoc/index.html)

## Architecture map

Application package: `device.apps.rfidsamplev2`
(under `app/src/main/java/device/apps/rfidsamplev2/`).

- `RFIDSampleV2.java` — Application class. Owns the single
  `ActivityLifecycleCallbacks` that calls SDK `disconnect()` once, at
  app-process exit.
- `main/MainActivity.java` — entry screen; hero card showing connection
  status; launches each sample.
- `connection/`
  - `Rf88ConnectionManager.java` — UI-free connection + state holder;
    centrally exposes status labels as derived LiveData.
  - `DeviceSdk.java` — runtime gate for Point Mobile `device.sdk`
    (`compileOnly`); guards Wired/Barcode features.
  - `SleepBlockingDialogController.java` — UI policy consumer of the
    manager (SLEEP dialog).
  - `WiredAttachDetector.java` — detects wired-reader attach/detach.
- `data/` — `ConfigData`, `Configuration`, `KeyMap` (config models +
  hardware-key mapping).
- `util/WindowInsetsUtil.java` — edge-to-edge inset application.
- `sample/<feature>/` — one screen per RF88 capability. Most follow a
  common structure: `<Feature>Activity` + `<Feature>ViewModel`, with
  optional `callback/` (click listeners) and `ui/` (RecyclerView
  adapters) and `data/` (response models):
  - `bluetooth/` — pair/connect over Bluetooth (Activity, ViewModel,
    `callback/OnDeviceClickListener`, `ui/DevicesAdapter`).
  - `wired/` — wired (USB/serial) reader (Activity, ViewModel).
  - `nfc/` — NFC tag read (Activity, ViewModel).
  - `configuration/` — RFID settings editor (Activity, ViewModel,
    `callback/OnTileClickListener`, `ui/ConfigurationAdapter`).
  - `inventory/` — tag inventory (Activity, ViewModel,
    `callback/OnInventoryClickListener`, `ui/InventoryAdapter`,
    `data/InventoryResponse`).
  - `nread/` — inventory-and-read (Activity, ViewModel,
    `callback/OnInventoryNreadClickListener`, `ui/InventoryNreadAdapter`,
    `data/InventoryNreadResponse`).
  - `barcode/` — RF88 trigger delegated to the device's own scanner
    (Activity, ViewModel). Result display is intentionally out of scope.

### Where do I change X?

| I want to change… | Look at |
|---|---|
| Connection/pairing behavior | `sample/bluetooth/`, `connection/Rf88ConnectionManager.java` |
| Connection status label / hero card | `connection/Rf88ConnectionManager.java`, `main/MainActivity.java` |
| Tag inventory list/UI | `sample/inventory/` |
| Inventory + read (nread) | `sample/nread/` |
| RFID settings (power/Q/session/region…) | `sample/configuration/`, `data/Configuration.java` |
| Hardware trigger key mapping | `data/KeyMap.java` |
| Wired reader detection | `connection/WiredAttachDetector.java`, `sample/wired/` |
| NFC | `sample/nfc/` |
| Barcode trigger | `sample/barcode/` |
| Edge-to-edge insets on a screen | `util/WindowInsetsUtil.java` |
| App-exit disconnect | `RFIDSampleV2.java` |

## SDK integration flow

Typical call sequence (signatures: see [SDK-API.md](SDK-API.md)):

1. `Rf88Manager.getInstance()` — obtain the singleton.
2. Ensure the `BLUETOOTH_SCAN` runtime permission is granted.
3. `connect(...)` — connect to the reader.
4. Register listeners:
   `setOnConnectionStateChangedListener`,
   `setOnInventoryResultListener`,
   `setOnHardwareKeyListener`,
   `setOnActionExecutingListener`.
5. Call an operation: `inventory()`, `inventoryAndRead()`, `read()`.
6. Receive results asynchronously via the registered listeners.
7. `disconnect()` — only at app-process exit (see conventions).

## Conventions & pitfalls

Rules an agent MUST follow when modifying this app:

- **No sync SDK calls on the main thread.** RF88 SDK 3.1.0+ crashes if a
  synchronous method runs on the UI thread. Use the `*Async` variants or
  dispatch on an `Executor`.
- **Disconnect → finish.** When a feature screen receives `DISCONNECTED`,
  it calls `finish()` rather than rendering a disconnected state.
- **Single disconnect at app exit.** `disconnect()` is called only from
  `RFIDSampleV2`'s `ActivityLifecycleCallbacks`, never from an individual
  Activity's `onDestroy()`.
- **Guard every `connect()` with `BLUETOOTH_SCAN`.** The SDK's connect
  calls `cancelDiscovery` internally; Android 12+ requires the runtime
  permission at every connect entry point.
- **Runtime-gate `device.sdk`.** It is `compileOnly` and absent on
  non-Point-Mobile devices. Gate Wired/Barcode behind `DeviceSdk`;
  Bluetooth/NFC work everywhere.
- **Apply edge-to-edge insets.** targetSdk forces edge-to-edge; new
  screens apply insets via `WindowInsetsUtil` and set light status/nav
  bar flags.
- **Status labels come from the Manager.** The hero card
  title/subtitle are derived LiveData exposed centrally by
  `Rf88ConnectionManager`; do not compute them per-screen.
- **Comments/Javadoc in English.**

## Build & environment (summary)

Android Studio Koala 2024.1.1+, JDK 17, Java with View/DataBinding.
The exact `compileSdk` / `targetSdk` / `minSdk` levels are defined in
[../app/build.gradle.kts](../app/build.gradle.kts) — read them there
rather than relying on a copy here. Full setup and troubleshooting:
[../README.md](../README.md).
