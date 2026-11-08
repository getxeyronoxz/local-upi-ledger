# LocalLedger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade, local-first Android application (`org.xeyronox.ledger`) under AGPLv3 that monitors and aggregates UPI transactions locally from BHIM and extensible banking apps via an AccessibilityService triggered by a floating PiP overlay button, storing data in an encrypted SQLCipher Room DB with password-protected AES-256 ZIP exports.

**Architecture:** A clean modular architecture separating the Accessibility & Floating Service layer from a pluggable `UpiAppParser` engine, backed by an encrypted SQLCipher Room database with Keystore key management, local WorkManager aggregation, MediaStore encrypted ZIP export, and a utilitarian Jetpack Compose UI compliant with One UI 8.5 / 9 (Android 16 / 17).

**Tech Stack:** Kotlin 2.0+, Jetpack Compose, AndroidX Room, SQLCipher (`net.zetetic:android-database-sqlcipher:4.5.5`), Zip4j (AES-256 encrypted archives), Android Keystore, WorkManager, Coroutines & Flow, Gradle Kotlin DSL, AGPLv3.

**Spec:** [docs/superpowers/specs/2026-11-08-local-ledger-design.md](file:///c:/Users/prakh/Desktop/customapp/docs/superpowers/specs/2026-11-08-local-ledger-design.md)

## Global Constraints
- Target Device & OS: Samsung Galaxy F15 5G running One UI 8.5 (Android 16) with One UI 9 (Android 17) readiness; ARM64-v8a.
- Memory & Binary Alignment: 16 KB page-size compatibility enabled for all native libraries (`useLegacyPackaging = false`).
- System UI: Edge-to-edge layout enforcement with `WindowInsets.safeDrawing` consumption.
- Zero Network: Absolutely no `android.permission.INTERNET` in `AndroidManifest.xml`.
- Security & Encryption: SQLCipher database encryption at rest; export files encrypted via password-protected AES-256 ZIP archives.
- Financial Totals: Aggregation calculations strictly enforce `WHERE status = 'SUCCESS'`.
- Package & Identity: `org.xeyronox.ledger`, digital identity `Xeyronox` (`getxeyronoxz`), AGPLv3 license.

## Review Focus
1. `FLAG_SECURE` compliance: Live extraction relies 100% on `AccessibilityNodeInfo` tree traversal; no pixel buffers or unprivileged screenshot calls.
2. Floating PiP on One UI 8.5/9: Service declared with `android:foregroundServiceType="specialUse"` and proper dismiss mechanics.
3. Sideloaded APK Setup: In-app guide for Android 13–17 Restricted Settings on Accessibility Services.
4. Duplicate Prevention: Deterministic composite hash on transactions prevents duplicate inserts across multiple taps.
5. Zero PII: Code, commit history, and test fixtures contain zero real personal names, real account numbers, or real UPI IDs.

---

### Task 1: Android Project Scaffolding & AGPL-3.0 Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `LICENSE` (GNU AGPLv3)
- Create: `.gitignore`
- Create: `gradlew`, `gradlew.bat`
- Test: Build configuration verification

**Interfaces:**
- Consumes: OpenJDK 17, Android SDK 34/35/36
- Produces: Compilable Android project baseline with zero internet permissions and 16 KB page alignment

- [ ] **Step 1: Create root build and version catalog files**
Create `settings.gradle.kts`, `build.gradle.kts`, and `gradle/libs.versions.toml` configuring plugins for Android Application, Kotlin Android, Compose, and KSP.

- [ ] **Step 2: Create `app/build.gradle.kts`**
Configure `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`, package `org.xeyronox.ledger`, Compose enabled, 16 KB page size packaging (`jniLibs.useLegacyPackaging = false`), and dependencies: Compose BOM, Room KTX, SQLCipher, Zip4j, WorkManager, Coroutines, JUnit, MockK.

- [ ] **Step 3: Create `AndroidManifest.xml` and `LICENSE`**
Declare `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`. Exclude `INTERNET`. Add GNU AGPLv3 text to `LICENSE`.

- [ ] **Step 4: Verify project build setup**
Run: `gradle tasks` or gradle dry run check.
Expected: Build scripts evaluate cleanly without errors.

- [ ] **Step 5: Commit**
```bash
git add .
git commit -m "chore: scaffold LocalLedger Android project under AGPL-3.0"
```

---

### Task 2: Core Domain Model & Pluggable Parser Contract

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/domain/model/Transaction.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/domain/model/TransactionType.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/domain/model/TransactionStatus.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/domain/parser/UpiAppParser.kt`
- Test: `app/src/test/java/org/xeyronox/ledger/domain/model/TransactionTest.kt`

**Interfaces:**
- Consumes: Java/Kotlin standard libraries
- Produces: `Transaction`, `TransactionType`, `TransactionStatus`, `UpiAppParser` interface

- [ ] **Step 1: Write unit tests for `Transaction` identity and hash computation**
Write tests in `TransactionTest.kt` verifying deterministic SHA-256 ID generation from timestamp window, amount, party name, and reference ID.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.domain.model.TransactionTest`
Expected: FAIL (unresolved reference: `Transaction`)

- [ ] **Step 3: Implement domain models and `UpiAppParser` contract**
Implement `Transaction.kt`, `TransactionType.kt` (`CREDIT`, `DEBIT`), `TransactionStatus.kt` (`SUCCESS`, `PENDING`, `FAILED`), and `UpiAppParser.kt` (`targetPackage`, `appDisplayName`, `canParse`, `parse`, `parseDetailReceipt`).

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.domain.model.TransactionTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/domain app/src/test/java/org/xeyronox/ledger/domain
git commit -m "feat(domain): implement Transaction models and UpiAppParser interface"
```

---

### Task 3: BHIM Extraction Engine (`BhimParser`)

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/parser/bhim/BhimParser.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/parser/bhim/BhimRegex.kt`
- Test: `app/src/test/java/org/xeyronox/ledger/parser/bhim/BhimParserTest.kt`

**Interfaces:**
- Consumes: `UpiAppParser`, `Transaction`, `TransactionType`, `TransactionStatus`
- Produces: `BhimParser` implementation parsing BHIM history list and detail receipt screens

- [ ] **Step 1: Write failing unit tests for BHIM text extraction and direction detection**
Write tests in `BhimParserTest.kt` asserting:
1. Credit detection for `"Received from Alice"` with amount `₹1,250.00` and date `"08 Nov 2026"` -> `Transaction(type = CREDIT, amount = 1250.0, status = SUCCESS)`.
2. Debit detection for `"Paid to Grocery Store"` with amount `₹450.50` -> `Transaction(type = DEBIT, amount = 450.5, status = SUCCESS)`.
3. Failed transaction detection for `"Declined"` / `"Failed"` -> `status = FAILED`.
4. Detail receipt parsing extracting 12-digit UTR `123456789012` and exact time.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.parser.bhim.BhimParserTest`
Expected: FAIL (unresolved reference `BhimParser`)

- [ ] **Step 3: Implement `BhimRegex` and `BhimParser`**
Implement directional keyword detection (`"Received from"`, `"Paid to"`, `↓`, `↑`), amount regex `(?:₹|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{2})?)`, status rules, and detail screen UTR extraction.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.parser.bhim.BhimParserTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/parser/bhim app/src/test/java/org/xeyronox/ledger/parser/bhim
git commit -m "feat(parser): implement BhimParser for transaction extraction"
```

---

### Task 4: Extensible Parser Registry (`ParserRegistry`) & Future App Stubs

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/parser/ParserRegistry.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/parser/phonepe/PhonePeParser.kt` (Stub for extension)
- Create: `app/src/main/java/org/xeyronox/ledger/parser/fampay/FamPayParser.kt` (Stub for extension)
- Test: `app/src/test/java/org/xeyronox/ledger/parser/ParserRegistryTest.kt`

**Interfaces:**
- Consumes: `UpiAppParser`, `BhimParser`
- Produces: `ParserRegistry.getParserForPackage(packageName: String): UpiAppParser?`

- [ ] **Step 1: Write failing unit test for `ParserRegistry`**
Test resolving `"in.org.npci.upiapp"` returns `BhimParser`, `"com.phonepe.app"` returns `PhonePeParser`, and unknown returns null.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.parser.ParserRegistryTest`
Expected: FAIL

- [ ] **Step 3: Implement `ParserRegistry`, `PhonePeParser`, and `FamPayParser`**
Implement registry with list of installed parsers, package lookup, and modular stubs for PhonePe and FamPay.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.parser.ParserRegistryTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/parser app/src/test/java/org/xeyronox/ledger/parser
git commit -m "feat(parser): implement ParserRegistry with BHIM, PhonePe, and FamPay routing"
```

---

### Task 5: Encrypted Local Storage (Room + SQLCipher)

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/data/local/entity/TransactionEntity.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/data/local/dao/TransactionDao.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/data/local/LedgerDatabase.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/data/security/KeyManager.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/data/repository/TransactionRepository.kt`
- Test: `app/src/test/java/org/xeyronox/ledger/data/repository/TransactionRepositoryTest.kt`

**Interfaces:**
- Consumes: `Transaction`, SQLCipher SupportFactory, Android Keystore
- Produces: `TransactionRepository` with reactive queries (`getAll()`, `getCreditSum()`, `getDebitSum()`, `getNetBalance()`, `insertOrUpdate()`)

- [ ] **Step 1: Write repository unit tests**
Test repository operations: inserting credit and debit transactions, calculating credit/debit totals, verifying that `getNetBalance()` excludes `status == FAILED`.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.data.repository.TransactionRepositoryTest`
Expected: FAIL

- [ ] **Step 3: Implement Room Entity, DAO, SQLCipher Database, and Repository**
1. Implement `TransactionEntity.kt` and `TransactionDao.kt` with `@Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'CREDIT' AND status = 'SUCCESS'")`.
2. Implement `KeyManager.kt` using Android Keystore to generate and retrieve AES-256 database passphrase.
3. Implement `LedgerDatabase.kt` with `SupportFactory(passphrase)` and `TransactionRepository.kt`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.data.repository.TransactionRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/data app/src/test/java/org/xeyronox/ledger/data
git commit -m "feat(data): implement SQLCipher encrypted Room database and repository"
```

---

### Task 6: Password-Protected AES-256 ZIP Export Engine

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/export/ExportManager.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/export/ZipCryptoHelper.kt`
- Test: `app/src/test/java/org/xeyronox/ledger/export/ExportManagerTest.kt`

**Interfaces:**
- Consumes: `List<Transaction>`, Zip4j (`net.lingala.zip4j.ZipFile`)
- Produces: `ExportManager.exportToEncryptedZip(transactions, password, outputStream)`

- [ ] **Step 1: Write export unit test**
Write test in `ExportManagerTest.kt` generating CSV and JSON from transactions, compressing them into an AES-256 password-protected ZIP archive, and asserting valid decryption with the correct password.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.export.ExportManagerTest`
Expected: FAIL

- [ ] **Step 3: Implement `ZipCryptoHelper` and `ExportManager`**
Stream CSV and JSON formatted transaction lists into an AES-256 encrypted ZIP file using Zip4j with `ZipParameters.setEncryptionMethod(EncryptionMethod.AES)`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.export.ExportManagerTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/export app/src/test/java/org/xeyronox/ledger/export
git commit -m "feat(export): implement password-protected AES-256 ZIP export engine"
```

---

### Task 7: Floating PiP Overlay & Accessibility Service

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/service/FloatingOverlayService.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/service/LedgerAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/res/layout/floating_overlay_bubble.xml`
- Test: `app/src/test/java/org/xeyronox/ledger/service/ServiceConfigurationTest.kt`

**Interfaces:**
- Consumes: `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, `ParserRegistry`, `TransactionRepository`
- Produces: User-triggered floating bubble overlay and single-pass Accessibility extraction pipeline

- [ ] **Step 1: Write unit test verifying service configuration and intent wiring**
Verify `accessibility_service_config.xml` has `canRetrieveWindowContent="true"` and package filter matching UPI apps.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.service.ServiceConfigurationTest`
Expected: FAIL

- [ ] **Step 3: Implement `FloatingOverlayService` and `LedgerAccessibilityService`**
1. Implement `FloatingOverlayService` as a `specialUse` foreground service displaying a floating, draggable bubble.
2. When bubble is tapped, notify `LedgerAccessibilityService` to inspect active window root node, route to `ParserRegistry`, parse transactions, and insert into `TransactionRepository`. Show brief on-screen toast with count of extracted transactions.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.service.ServiceConfigurationTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/service app/src/main/res
git commit -m "feat(service): implement FloatingOverlayService and LedgerAccessibilityService"
```

---

### Task 8: Utilitarian Presentation UI (Jetpack Compose)

**Files:**
- Create: `app/src/main/java/org/xeyronox/ledger/ui/MainActivity.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/DashboardViewModel.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/DashboardScreen.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/TransactionListScreen.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/PermissionsGuideDialog.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/ExportDialog.kt`
- Create: `app/src/main/java/org/xeyronox/ledger/ui/theme/Theme.kt`
- Test: `app/src/test/java/org/xeyronox/ledger/ui/DashboardViewModelTest.kt`

**Interfaces:**
- Consumes: `TransactionRepository`, `ExportManager`
- Produces: Jetpack Compose dashboard UI with `safeDrawing` insets, status cards (Credit/Debit/Net), transaction feed, overlay toggle button, export prompt, and Android 16/17 permission setup wizard.

- [ ] **Step 1: Write failing unit test for `DashboardViewModel`**
Test `DashboardViewModel` state flow: emits credit total, debit total, net balance, and transaction list accurately.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.ui.DashboardViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement `DashboardViewModel` and Compose UI components**
1. Implement `DashboardViewModel.kt` managing UI state flows.
2. Implement `DashboardScreen.kt` with credit/debit summary cards, net balance, and floating service toggle.
3. Implement `TransactionListScreen.kt` with transaction rows showing credit (+) / debit (-), amounts, counterparty, and status badges.
4. Implement `PermissionsGuideDialog.kt` guiding users through One UI 8.5/9 Restricted Settings.
5. Implement `ExportDialog.kt` prompting for password and initiating encrypted ZIP export to `Downloads/LocalLedger/`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests org.xeyronox.ledger.ui.DashboardViewModelTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/org/xeyronox/ledger/ui app/src/test/java/org/xeyronox/ledger/ui
git commit -m "feat(ui): implement Jetpack Compose utilitarian dashboard and permission wizard"
```

---

### Task 9: Full Build Verification & GitHub Repository Initialization

**Files:**
- Create: `README.md`
- Create: `.github/workflows/android-ci.yml`
- Test: Full debug build and test suite execution

**Interfaces:**
- Consumes: All completed modules
- Produces: Ready-to-push git repository for `getxeyronoxz/local-upi-ledger` under AGPLv3

- [ ] **Step 1: Create `README.md` and GitHub Actions CI workflow**
Document architecture, privacy model (0 internet permissions), security, Samsung One UI 8.5/9 setup steps, build instructions, and AGPLv3 copy.

- [ ] **Step 2: Run complete clean test suite and build**
Run: `./gradlew test assembleDebug`
Expected: BUILD SUCCESSFUL, all unit tests passing, debug APK generated.

- [ ] **Step 3: Initialize Git repo and check for zero PII**
Verify `git log` and diffs contain zero personal names, zero account numbers, and zero real credentials.

- [ ] **Step 4: Create GitHub repository via `gh` CLI and push**
Run: `gh repo create getxeyronoxz/local-upi-ledger --public --source=. --push` (or private if preferred).
Expected: Repository created and code pushed successfully.

- [ ] **Step 5: Final verification and summary**
Confirm repository URL, build status, and APK artifact path.
