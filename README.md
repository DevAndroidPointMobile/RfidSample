# RfidSample

This is a sample application demonstrating basic RFID feature integration on Android.  
Please make sure your development environment meets the following specifications to ensure successful build and execution.

---

## 📦 Environment Requirements

| Item | Value |
|------|-------|
| **Android Studio Version** | **2024.1.1 Koala or higher** (*Required*) |
| Gradle Plugin Version | `8.5.0` |
| JDK Version | `17` |
| Compile SDK | `34` |
| Target SDK | `34` |
| Min SDK | `26` |
| Language | Java |
| Architecture | View Binding, Data Binding |

> ⚠️ **Do not use older versions of Android Studio.**  
> The project relies on Gradle 8.5.0 and JDK 17 features, which are supported only in **Android Studio 2024.1.1 Koala or later**.

---

## ✅ How to Build

1. **Launch Android Studio 2024.1.1 Koala or higher**
2. Open the project directory (`RfidSample`)
3. Wait for Gradle sync to complete
4. If build fails, check the following settings:

---

### ⚙️ Set JDK Location

1. Go to **`File > Project Structure (Ctrl+Alt+Shift+S)`**
2. In the **SDK Location** tab:
   - Set **JDK Location** to a valid JDK 17 path  
     Example:  
     ```
     C:\Program Files\Android\JDK\jdk-17
     ```
   - Or, use the embedded JDK if it supports Java 17
---

### ⚙️ Set Android SDK Location

1. Still in **`Project Structure > SDK Location`**:
   - Make sure **Android SDK Location** is set properly  
     Example:
     ```
     C:\Users\YourName\AppData\Local\Android\Sdk
     ```

2. If SDK path is missing or broken:
   - Go to **`File > Settings > Appearance & Behavior > System Settings > Android SDK`**
   - Download and install **SDK Platform 34**
   - Apply and sync again

---

## 🧭 Project Overview

- Written entirely in Java
- Uses ViewBinding to connect UI and logic
- Uses **ViewBinding** for UI control
- **DataBinding** is used for simplified interaction between layout and code

---

## ⚠️ Common Issues & Fixes

| Issue | Solution |
|-------|----------|
| `Unsupported Gradle or JDK version` | Upgrade to Android Studio 2024.1.1 or higher and ensure JDK 17 is selected |
| `Cannot resolve symbol 'binding'` | Make sure ViewBinding is enabled in `build.gradle` and synced |
| Build fails with obscure errors | Try: `File > Invalidate Caches / Restart` and rebuild |

---

## 💬 Support

Before reporting a build issue, please verify:

- Android Studio version is **Koala (2024.1.1)** or newer  
- JDK version is set to **17**
- Minimum SDK is supported on your device or emulator

If you need further assistance, please contact your integration support representative with:

- Your Android Studio version
- JDK version
- Full error logs (if available)
