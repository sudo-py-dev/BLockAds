<h1 align="center">
  <img src="logo.svg" width="100" alt="AdsBlock Logo" />
  <br/>
  AdsBlock VPN
</h1>

<p align="center">
  <strong>A modern, lightweight, and robust DNS-based Ad-Blocker for Android.</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#building">Building</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## 🛡️ Overview

AdsBlock is an on-device VPN application designed to intercept and filter DNS requests in real-time, effectively blocking ads, trackers, and malicious domains before they even load. Built entirely with **Jetpack Compose** and modern Kotlin coroutines, it provides a slick, localized, and ultra-fast user experience with deep transparency into exactly what is happening under the hood.

Developed by **ed apps**.

## ✨ Features

- **Real-Time DNS Tunneling**: Parses Raw IPv4 and UDP packets to inspect DNS queries on-the-fly using `dnsjava`.
- **Zero-Logging VPN**: All filtering happens locally on your device. No traffic is sent to remote servers for inspection.
- **Query Logs & Transparency**: View exactly which domains are being requested and blocked in real-time via the built-in Logs screen.
- **Dynamic Pause & Resume**: Need to bypass a strict firewall? Pause the blocker for 15, 30, or 60 minutes with background notification controls.
- **Full Localization Support**: Completely localized UI, system strings, and RTL (Right-to-Left) layout support.
- **Modern Jetpack Compose UI**: Built entirely using Material 3 dynamic theming, smooth animations, and dark/light modes.
- **Optimized & Lightweight**: Heavily minified with aggressive R8 proguard rules and resource shrinking for minimal battery and storage footprint.

## 🏗️ Architecture

AdsBlock leverages modern Android architecture patterns to ensure strict safety, low overhead, and scalability:

- **UI Layer**: Jetpack Compose (`Material 3`, `NavHost`)
- **State Management**: `StateFlow` and `DataStore Preferences`
- **Networking Core**: `VpnService` with raw socket packet parsing via custom `DatagramSocket` tunneling.
- **DNS Parsing**: Integrated `dnsjava` for robust packet decoding and manipulation.
- **Asynchrony**: `Kotlin Coroutines` (Dispatchers.IO) for high-throughput packet processing.

## 🚀 Building from Source

To build AdsBlock locally, you need [Android Studio](https://developer.android.com/studio) installed.

1. **Clone the repository:**
   ```bash
   git clone https://github.com/sudo-py-dev/adsblock.git
   cd AdsBlock
   ```

2. **Open in Android Studio:**
   Open the project directory in Android Studio. Gradle will automatically sync the dependencies.

3. **Build the APK:**
   To build a heavily optimized release APK, ensure you have your keystore properties set up in `local.properties`, or simply build the debug variant:
   ```bash
   ./gradlew assembleDebug
   ```

## 🌍 Localization

AdsBlock natively supports multiple languages. The app strictly enforces layout directions (LTR/RTL) across Jetpack Compose automatically based on active locales. If you wish to contribute a translation, please submit a PR modifying the `strings.xml` resources!

## 📜 License

Copyright © ed apps. All rights reserved.
