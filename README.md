# PayNey Capture

**The Android companion app for [PayNey](https://payney.vercel.app) — your expense tracker.**

PayNey Capture turns your phone into an automatic transaction sensor. It reads
your bank/UPI **SMS alerts** and **payment-app notifications** the moment they
arrive, and forwards them to your PayNey account so every expense lands in your
tracker without a single manual entry. Snap a photo of a paper receipt and it
extracts the amount, merchant, and date for you too.

Think of it as the "eyes and ears" of PayNey on your device — the web app is
where you review, categorize, and analyze; Capture is what feeds it in real time.

---

## Why it exists

Manually logging every coffee, cab, and grocery run is the reason most expense
trackers get abandoned. In India, though, almost every spend already generates a
signal: a bank SMS ("Rs. 450 debited...") or a UPI app notification (GPay,
PhonePe, Paytm). PayNey Capture listens for exactly those signals, filters out
everything that isn't a real transaction, and streams the rest to your PayNey
backend — so your ledger stays complete with zero effort.

---

## Features

- **Automatic SMS capture** — Listens for incoming bank/UPI SMS and forwards
  matching messages to PayNey in the background.
- **Notification capture** — Reads notifications from allowlisted payment apps
  (Google Pay, PhonePe, Paytm, CRED) and forwards transaction alerts.
- **Scan a receipt** — Take a photo of a paper receipt; the backend extracts
  amount, merchant, date, and a suggested category for you to review, edit, and
  confirm into your PayNey review queue.
- **Privacy-first allowlisting** — Only messages from a known list of bank
  sender IDs and payment-app packages are ever sent off-device. Arbitrary
  personal SMS and notifications are silently ignored.
- **Durable offline outbox** — Captures made while offline (or during a server
  outage) are queued locally and retried automatically when connectivity
  returns, so a transaction is never lost.
- **Secure device pairing** — Link the app to your PayNey account with a
  one-time pairing code from the web app; the resulting device token is stored
  in encrypted storage.
- **Live status dashboard** — See permission state, capture on/off, and the
  time of your last successful sync at a glance.

---

## How it works

```
 ┌─────────────────────┐        ┌──────────────────────┐        ┌───────────────────┐
 │  Bank / UPI SMS     │──────▶ │                      │        │                   │
 ├─────────────────────┤        │   PayNey Capture     │──────▶ │   PayNey Backend  │
 │  Payment-app        │──────▶ │   (allowlist +       │  HTTPS │   /api/...        │
 │  notifications      │        │    offline outbox)   │        │                   │
 ├─────────────────────┤        │                      │        │   Web expense     │
 │  Scanned receipt    │──────▶ │                      │        │   tracker         │
 └─────────────────────┘        └──────────────────────┘        └───────────────────┘
```

1. **Pair** the device once using a code from the PayNey web app
   (`POST /api/device/pair` → device token).
2. `SmsReceiver` and `NotificationCaptureService` observe incoming events and
   check each one against `AllowList`.
3. Matches are handed to a single `CaptureRepository`, which posts them to
   `POST /api/transactions/ingest` — or queues them in the local outbox and
   retries later if the network is down.
4. Receipts are uploaded to `POST /api/transactions/receipt` for field
   extraction, then confirmed via `POST /api/transactions/receipt/confirm`.

### Backend API

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/device/pair` | POST | Exchange a pairing code for a device token |
| `/api/transactions/ingest` | POST | Ingest a captured SMS / notification |
| `/api/transactions/receipt` | POST (multipart) | Extract fields from a receipt image |
| `/api/transactions/receipt/confirm` | POST | Persist a reviewed receipt to the review queue |

---

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM (ViewModel + Compose state), Navigation Compose
- **Networking:** Retrofit 2 + OkHttp, `kotlinx.serialization` JSON
- **Concurrency:** Kotlin Coroutines
- **Security:** AndroidX Security Crypto (encrypted token storage)
- **Capture:** `BroadcastReceiver` (SMS) + `NotificationListenerService`
- **Min SDK:** 26 (Android 8.0) &nbsp;•&nbsp; **Target/Compile SDK:** 36

---

## Getting started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- An Android device or emulator running API 26+
- A running PayNey backend (local or deployed)

### Setup

1. **Clone the repo**

   ```bash
   git clone https://github.com/othzer/payney-capture.git
   cd payney-capture
   ```

2. **Point the app at your backend.** Copy the example and edit as needed:

   ```bash
   cp local.properties.example local.properties
   ```

   - **Emulator (default):** no changes needed — it uses `http://10.0.2.2:3000`.
   - **Physical device:** run `adb reverse tcp:3000 tcp:3000` and set
     `PAYNEY_API_BASE_URL=http://127.0.0.1:3000`, or point it at your deployed
     backend URL.

3. **Build & run** from Android Studio, or from the command line:

   ```bash
   ./gradlew installDebug
   ```

> Release builds ignore the local debug URL and always target the deployed
> backend (`PAYNEY_RELEASE_API_BASE_URL`, default `https://payney.vercel.app`),
> so a distributed APK can never accidentally ship pointing at localhost.

### Usage

1. Open **PayNey Capture** and enter the **pairing code** shown in the PayNey web app.
2. Grant **SMS** permission and enable **Notification access**
   (Settings → Notification access) from the in-app Status screen.
3. Leave capture on — transactions now flow to PayNey automatically.
4. Tap **Scan a receipt** to photograph a bill and confirm the extracted details.

---

## Permissions & privacy

PayNey Capture is deliberately conservative about what leaves your device:

| Permission | Why it's needed |
| --- | --- |
| `RECEIVE_SMS` / `READ_SMS` | Detect incoming bank/UPI transaction SMS |
| Notification access | Read notifications from allowlisted payment apps |
| `CAMERA` | Photograph receipts for scanning |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Send captures and retry the offline queue |

Only messages matching the bank sender IDs and payment-app packages defined in
`AllowList` are ever transmitted. Everything else — personal texts, unrelated
notifications — is discarded on-device and never sent anywhere. The device token
is kept in encrypted storage, and `allowBackup` is disabled.

---

## Project structure

```
app/src/main/java/com/otzrlabs/payney/capture/
├── capture/                    # SMS receiver, notification listener, allowlist
│   ├── AllowList.kt
│   ├── NotificationCaptureService.kt
│   └── SmsReceiver.kt
├── data/                       # Repository, outbox, prefs, token store
│   ├── CaptureRepository.kt
│   ├── CaptureOutbox.kt
│   ├── CapturePrefs.kt
│   ├── TokenStore.kt
│   └── network/                # Retrofit service + models
├── ui/                         # Compose screens (pair, status, scan receipt)
│   ├── pair/  status/  receipt/
│   ├── brand/  nav/  theme/
└── util/                       # Camera capture helper
```

---

## License

Proprietary — © OtzrLabs. All rights reserved.
