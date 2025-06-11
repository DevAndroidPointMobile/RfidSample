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

1. Open the project in **Android Studio 2024.1.1 Koala or higher**
2. Wait for Gradle sync to complete  
   > If the sync fails, verify your JDK version is set to **17**
3. Clean and rebuild the project:
  - Build > Clean Project
  - Build > Rebuild Project
---

## 🧭 Project Overview

- Written entirely in Java
- Uses ViewBinding to connect UI and logic
- DataBinding used for layout and interaction simplicity
- Designed for Android devices with RFID capability

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
