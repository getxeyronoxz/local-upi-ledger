# LocalLedger (`org.xeyronox.ledger`) - System Design Specification

## 1. Intent & Purpose
LocalLedger is a local-first, zero-network, privacy-preserving Android application built to track and aggregate UPI transactions from banking applications—starting with **BHIM** (`in.org.npci.upiapp`) and architecturally extensible to **PhonePe** (`com.phonepe.app`), **FamPay** (`com.famapp`), and others.

Because financial apps enforce `FLAG_SECURE` to block screenshots and screen recordings, LocalLedger utilizes Android's `AccessibilityService` API to inspect UI node hierarchies in memory. The app runs completely offline with zero network permissions, stores all financial data in an encrypted SQLCipher database, and provides functional analytics, local alerts, and encrypted file exports to the device's Downloads directory.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
* **Reliable Extraction**: Accurately capture both incoming (credited) and outgoing (debited) UPI transactions with amounts, timestamps, counterparty identifiers, and status.
* **100% Offline & Zero Network**: Omit `android.permission.INTERNET` from `AndroidManifest.xml` entirely. No telemetry, crash reporting, or external API calls.
* **Hardware & OS Compatibility**: Fully compatible with the Samsung Galaxy F15 (One UI 6, Android 14 / API 34), accounting for foreground service restrictions and sideloaded app permission barriers.
* **Extensible Architecture**: Provide a clean pluggable parser interface so adding new banking or UPI apps requires only writing a single parser module.
* **Encrypted Storage**: Secure all persistent records at rest using SQLCipher with hardware-backed Keystore key management.
* **Encrypted Export**: Allow users to export transaction data as encrypted CSV or JSON directly into the device's `Downloads/LocalLedger/` folder.
* **Clean Open-Source Showcase**: Maintain a pristine repository under AGPLv3 for the `getxeyronoxz` GitHub portfolio without exposing personal or financial traces.

### Non-Goals
* **Play Store Distribution**: The app will not be distributed on Google Play; it is intended solely for personal and family devices.
* **Flamboyant UI**: Fancy animations, micro-interactions, and complex design systems are non-goals. The UI is strictly utilitarian, functional, and clean.
* **Root Requirements**: Must operate on stock, unrooted Android 14 devices without requiring ADB shell commands after installation.

### Anti-Goals
* **Continuous Background Scraping**: Never run invasive, battery-draining continuous screen polling. Extraction is strictly triggered on demand by the user.
* **PII Leakage**: Never hardcode, commit, or log real banking account numbers, real UPI IDs, or personal names in source code or sample data.
* **Proprietary Enclosure**: AGPLv3 license strictly forbids closed-source or commercial appropriation.

---

## 3. Constraints & Hardware Environment

* **Target Device & OS**: Samsung Galaxy F15 5G (Currently running **One UI 8.5 on Android 16**, with guaranteed upgrade to **One UI 9 on Android 17**; ARM64-v8a).
* **Android 16 / 17 (One UI 8.5 / 9) Compatibility & Forward-Hardening**:
  * **16 KB Page Size Alignment**: Native C/C++ shared libraries (SQLCipher binaries) are aligned with 16 KB page sizes required by Android 15/16/17 kernels.
  * **Edge-to-Edge Layouts**: Android 15+ enforces edge-to-edge by default; Compose views consume `WindowInsets.safeDrawing` to prevent system bar clipping on One UI 8.5 and 9.
  * **Foreground Service Safeguards**: Foreground service types (`android:foregroundServiceType="specialUse"`) declared with user notifications and dismiss actions to comply with One UI 8.5/9 background execution policies.
  * **Sideloaded App Permissions**: Setup guide included for Android 13–17 Restricted Settings on Accessibility Services.
  * **FLAG_SECURE Reality**: Semantic accessibility hierarchy inspection (`AccessibilityNodeInfo`) is utilized as the primary live extraction engine because `FLAG_SECURE` blocks pixel buffer captures across all Android versions. Optional offline ML Kit scanner is reserved for unconstrained receipt images.
* **Development Environment**: OpenJDK 17 (`JAVA_HOME`), Android CLT / SDK 34+, `adb`, Gradle Wrapper, Git, GitHub CLI (`gh`).

---

## 4. Approaches Considered

### Approach 1: Hybrid Node-First + Bundled ML Kit Fallback *(Chosen)*
* Direct `AccessibilityNodeInfo` DOM tree walking is primary (<1ms execution, 0 OCR battery drain).
* Bundled on-device `com.google.mlkit:text-recognition` is embedded in the APK (~4MB) as a secondary fallback if an app draws transaction details onto custom canvas views without accessibility text descriptions.
* **Why chosen**: Combines near-instant speed and battery conservation with bulletproof resilience across banking UI variations while remaining 100% offline.

### Approach 2: Pure Accessibility Tree Parsing (Rejected)
* Relies entirely on `AccessibilityNodeInfo`. Zero ML Kit dependencies.
* **Why rejected**: Fails completely if a banking app renders transaction receipts or lists inside a Canvas, Flutter surface, or non-standard custom view lacking accessibility nodes.

### Approach 3: Full Vision OCR Pipeline (Rejected)
* Runs OCR on every capture.
* **Why rejected**: High battery and memory overhead, latency (50-200ms), and `FLAG_SECURE` prevents capturing full-screen bitmaps directly unless traversing the accessibility window node tree.

---

## 5. Architectural Components

```
+-------------------------------------------------------------+
|                     Presentation Layer                      |
|  - Dashboard Screen (Totals: Credit, Debit, Net Balance)    |
|  - Transaction Ledger (Search, Filter, Date Range)          |
|  - Settings & Android 14 Permission Setup Wizard            |
|  - Export Dialog (Encrypted CSV / JSON -> Downloads/)       |
+-------------------------------------------------------------+
                              |
+-------------------------------------------------------------+
|                      Service Layer                          |
|  - FloatingOverlayService (PiP Trigger Button)              |
|  - LedgerAccessibilityService (Node Tree Inspection)        |
|  - AggregationWorker (Jetpack WorkManager offline totals)   |
|  - NotificationHelper (Local NotificationManager alerts)    |
+-------------------------------------------------------------+
                              |
+-------------------------------------------------------------+
|                   Extraction & Parser Engine                |
|  - UpiParserStrategy (Package detection & routing)          |
|    |-- BhimParser ("in.org.npci.upiapp")                    |
|    |-- PhonePeParser ("com.phonepe.app") [Stub/Extension]   |
|    |-- FamPayParser ("com.famapp") [Stub/Extension]         |
|  - BundledMlKitEngine (com.google.mlkit:text-recognition)   |
+-------------------------------------------------------------+
                              |
+-------------------------------------------------------------+
|                      Data & Security                        |
|  - RoomDatabase (Transactions, DailySummaries)              |
|  - SQLCipher SupportFactory (Encrypted at rest)             |
|  - KeystoreManager (AES-GCM DB Key Storage)                 |
|  - ExportManager (Encrypted MediaStore.Downloads writer)    |
+-------------------------------------------------------------+
```

### 5.1 Data Model
```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String, // SHA256(timestamp_window + amount + partyName + (referenceId ?: ""))
    val timestamp: Long,
    val amount: Double,
    val type: TransactionType,     // CREDIT (incoming) or DEBIT (outgoing)
    val status: TransactionStatus, // SUCCESS, PENDING, FAILED
    val partyName: String,         // Sender or Receiver label
    val upiId: String?,            // Optional VPA/UPI ID if extractable
    val referenceId: String?,      // 12-digit UTR / Transaction Ref No (enriched via detail screen)
    val appSource: String,         // "BHIM", "PhonePe", "FamPay"
    val rawTextDump: String        // Audit string for parsing verification
)

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionStatus {
    SUCCESS, PENDING, FAILED
}
```

### 5.2 Pluggable Parser Contract
```kotlin
interface UpiAppParser {
    val targetPackage: String
    val appDisplayName: String
    fun canParse(packageName: String): Boolean = packageName == targetPackage
    fun parse(rootNode: AccessibilityNodeInfo): List<TransactionEntity>
    fun parseDetailReceipt(rootNode: AccessibilityNodeInfo): TransactionEntity? = null
}
```

### 5.3 BHIM Parser Rules
* **Credit (Incoming)**:
  * Keywords: `"Received from"`, `"Money received"`, `"Credited to"`
  * Directional glyphs: `↓`, `↙`, green indicators
* **Debit (Outgoing)**:
  * Keywords: `"Paid to"`, `"Sent to"`, `"Debited from"`
  * Directional glyphs: `↑`, `↗`, red indicators
* **Amount**:
  * Regex: `(?:₹|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{2})?)`
* **Deduplication**: SHA-256 hash derived from `timestamp_window + amount + partyName + referenceId` to avoid duplicate insertions on multiple taps.

### 5.4 Offline ML Kit Bundled Engine
* Dependency: `com.google.mlkit:text-recognition:16.0.1` (bundled model).
* Operates on node visual snapshots when `node.text` is blank or obfuscated.

### 5.5 Encrypted Storage & Keystore
* Database: `androidx.room:room-ktx` + `net.zetetic:android-database-sqlcipher:4.5.4`.
* Master key generated via Android `KeyStore` using `KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT`, AES-256-GCM.
* Passphrase stored securely in `EncryptedSharedPreferences`.

### 5.6 Data Export
* **Format**: Standard password-protected **AES-256 encrypted `.zip` archive** containing `transactions.csv` and `transactions.json`.
* **Universal Decryption**: Compatible with standard desktop utilities (7-Zip, WinRAR, Python `pyzipper`) on Windows, macOS, and Linux using the user-provided passphrase.
* **Target Location**: `Environment.DIRECTORY_DOWNLOADS/LocalLedger/` via Android `MediaStore.Downloads`.
* **Security**: No unencrypted temporary files are left on external storage. Encryption streams directly in memory into the output ZIP stream.

---

## 6. Project & Git Details
* **Project Name**: LocalLedger
* **Package**: `org.xeyronox.ledger`
* **Digital Identity / Author**: Xeyronox (`getxeyronoxz`)
* **Repository**: `local-upi-ledger` on GitHub
* **License**: GNU Affero General Public License v3 (AGPL-3.0)
* **Language & Build**: Kotlin 2.0+, Jetpack Compose, Kotlin DSL Gradle Wrapper, Android SDK 34.

---

## 7. What's Left to the Builder
* Specific Gradle dependency version alignments for Kotlin 2.0 / Compose Compiler.
* Touch handling and drag-to-edge magnetic snap coordinates for the floating overlay button.
* Exact date parsing patterns used across BHIM versions (`dd MMM yyyy, hh:mm a`, etc.).
* Exact Keystore alias configuration and SQLCipher factory initialization boilerplate.
