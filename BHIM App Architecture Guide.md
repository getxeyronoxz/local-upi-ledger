# Local-First BHIM UPI Tracker - Architecture & Implementation Guide

## 1. Core Concept & Security Strategy

The objective is to build a local-first, privacy-preserving Android application designed to monitor and aggregate BHIM UPI transaction records without transmitting data to any external server. Because banking applications implement `FLAG_SECURE` to prevent standard screen recording or overlay captures, the system relies on Android's native `AccessibilityService` API to read UI nodes directly from the screen hierarchy.

## 2. Data Ingestion & Capture Mechanism

### AccessibilityService Integration
Instead of continuously running, the service is designed to be triggered manually. When the user navigates to the BHIM transaction history page, they activate a lightweight, floating picture-in-picture (PiP) overlay button. Tapping this button instructs the `AccessibilityService` to execute a single-pass extraction of the visible screen nodes.

### Floating PiP Overlay Setup
* **System Alert Window Permission:** The overlay is implemented as a floating service using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
* **User Control:** The PiP icon stays minimized on the edge of the screen. When tapped, it triggers the accessibility extraction routine without blocking your view of the BHIM app.

### Bypassing Security Restrictions
By pulling `AccessibilityNodeInfo` instances, the app reads text descriptions of elements without triggering standard screenshot blocks (`FLAG_SECURE`). This allows the extraction of amounts, dates, and directional indicators directly from the node tree.

## 3. Lightweight Vision & Text Parsing Model

### On-Device Model Selection
* **Primary Choice:** **Google ML Kit Text Recognition API** (On-Device). 
* **Why:** It operates entirely offline, has a footprint under 10MB, executes in under 50ms, and consumes negligible system resources compared to heavy LLMs or server-based vision models.

### Parsing Logic & Directional Rules
The extraction engine parses the node text and layout cues tailored for BHIM UPI transaction histories:
* **Incoming Payments:** Identified by keywords such as `"Received from"` or directional indicators like down/left arrows (`↓`, `↙`).
* **Outgoing Payments:** Identified by keywords like `"Paid to"` or `"Sent to"` and directional indicators like up/right arrows (`↑`, `↗`).
* **Amount Extraction:** Parsed using regular expressions isolating currency values following symbols like `₹` or `Rs.`.

## 4. Local Persistence & Privacy Architecture

### Encrypted Local Storage
* **Framework:** Android Jetpack Room Database over SQLite.
* **Encryption:** Integrated with **SQLCipher** and Android Keystore to encrypt the database file on internal storage.
* **Data Security:** No network permissions (`android.permission.INTERNET`) are included in the `AndroidManifest.xml`, ensuring complete isolation.

## 5. Background Aggregation & Reporting Logic

### Automated Summaries
* **Job Scheduler:** Jetpack `WorkManager` triggers periodic daily, weekly, and monthly calculations in the background.
* **Calculations:** Summarizes total credited amount (Incoming), total debited amount (Outgoing), and net balance for specific date ranges (daily, weekly, monthly, yearly).

### Local Notifications
* **Notification System:** `NotificationManager` schedules local system notifications to alert you about weekly summary reports without relying on push notification servers.

## 6. Technical Summary Matrix

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| **Capture Engine** | `AccessibilityService` | Bypasses `FLAG_SECURE` to read screen UI nodes |
| **User Interface** | `WindowManager` Floating Overlay | Manual single-tap trigger mechanism |
| **OCR / Parsing Engine** | ML Kit Text Recognition (On-Device) | Lightweight offline text extraction |
| **Database** | Room + SQLCipher | Encrypted offline storage for transaction records |
| **Background Processing** | Jetpack WorkManager | Computes daily, weekly, and monthly totals |
| **Alerts** | Android NotificationManager | Delivers periodic offline updates |