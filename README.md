# LocalLedger (`org.xeyronox.ledger`)

> **Local-First, Privacy-Respecting UPI Transaction Aggregator for Android 16 & 17**  
> Engineered by **Xeyronox** ([@getxeyronoxz](https://github.com/getxeyronoxz)) under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.

---

## 🔒 Privacy & Threat Model

LocalLedger is built from first principles for personal and family use on **Samsung Galaxy F15 5G** devices (tested on **One UI 8.5 / Android 16** with forward-compatibility for **One UI 9 / Android 17**).

* **Zero Internet Permissions (`0 INTERNET`)**: The `android.permission.INTERNET` manifest flag is completely omitted from the codebase. It is physically impossible for the app or any bundled library to make outgoing network calls or exfiltrate data.
* **Encrypted Storage (SQLCipher + Android Keystore)**: Local persistence is backed by SQLCipher 4.x over Room. Passphrases are generated inside hardware-backed Android Keystore using AES-256 GCM and never stored in plaintext.
* **Bypassing `FLAG_SECURE` in RAM**: Banking apps (BHIM, PhonePe, FamPay) enable Android's `FLAG_SECURE` window flag, which blackouts standard screenshots and screen recordings. LocalLedger's accessibility engine inspects the active window's `AccessibilityNodeInfo` hierarchy directly in memory, bypassing window capture restrictions without root or modified OS kernels.
* **Encrypted Exports**: Database dumps are exported into password-protected **AES-256 encrypted ZIP archives** saved to `Downloads/LocalLedger/` containing `transactions.csv` and `transactions.json`.

---

## 🚀 Key Features

* **Pluggable Parser Architecture**:
  * **BHIM UPI** (`in.org.npci.upiapp`): Directional regex scanning (Credit vs. Debit), UTR extraction, status validation (`SUCCESS`, `PENDING`, `FAILED`).
  * **PhonePe** (`com.phonepe.app`): Extensible transaction feed parser.
  * **FamPay** (`com.famapp`): Extensible peer & card payment parser.
* **Floating PiP Scan Bubble**:
  * Draggable overlay bubble (`SYSTEM_ALERT_WINDOW`) running under an Android 16/17 `specialUse` foreground service.
  * Single-tap instant memory scan when viewing transaction histories or payment receipts.
* **Utilitarian Jetpack Compose Dashboard**:
  * Real-time calculation of Total Received (Credits), Total Spent (Debits), and Verified Net Balance (`WHERE status = 'SUCCESS'`).
  * Instant search, filtering, and transaction inspection.
  * One UI 8.5/9 edge-to-edge support with `WindowInsets.safeDrawing`.

---

## 📱 Samsung One UI 8.5 / 9 Setup (Restricted Settings)

Android 13+ (and strictly enforced on Android 16 & 17) blocks accessibility services for apps installed outside official app stores by displaying a **"Restricted setting"** popup.

To activate LocalLedger on Samsung Galaxy devices:

1. **Allow Restricted Settings**:
   * Open device **Settings** -> **Apps** -> **LocalLedger**.
   * Tap the **⋮ (Three Dots)** in the top right corner.
   * Tap **Allow restricted settings**.
   * Confirm with your device PIN or Fingerprint.
2. **Enable Accessibility Service**:
   * Open device **Settings** -> **Accessibility** -> **Installed apps**.
   * Tap **LocalLedger** and toggle **ON**.
3. **Allow Appear on Top**:
   * Go to **Settings** -> **Apps** -> **Special app access** -> **Appear on top**.
   * Toggle **LocalLedger** ON to enable the floating scan bubble.

---

## 🛠️ Build & Verification

### Prerequisites
* JDK 17 (OpenJDK 17 recommended)
* Android SDK 35 (Command-line Tools & Build Tools 35.0.0+)
* Gradle 8.11.1 (included via `./gradlew`)

### Running Tests
Execute the full test suite (domain models, regex engines, parser registry, SQLCipher repositories, export encryption, and viewmodels):
```bash
./gradlew testDebugUnitTest
```

### Compiling Debug APK
Compile the 16 KB page-aligned debug APK:
```bash
./gradlew assembleDebug
```
The output artifact is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This software is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.  
See the [LICENSE](LICENSE) file for the full license text.
