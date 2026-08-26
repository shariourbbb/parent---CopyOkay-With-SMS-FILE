# ⚡ Quick Start Guide

## ১. Server চালু করুন

```bash
cd server
npm install
npm start
```

Server: `http://localhost:3000`

## ২. ngrok চালু করুন (নতুন terminal)

```bash
ngrok http 3000
```

HTTPS URL copy করুন (যেমন: `https://abc123.ngrok-free.app`)

## ৩. Android App এ URL আপডেট করুন

**File:** `android-app/app/src/main/java/com/parentaltracker/MonitoringService.kt`

**Line 38:** SERVER_URL পরিবর্তন করুন
```kotlin
private val SERVER_URL = "https://abc123.ngrok-free.app"
```

## ৪. App Build এবং Install করুন

1. Android Studio → Build → Make Project
2. Device/Emulator এ install করুন
3. Permissions allow করুন
4. Dashboard এ device দেখুন!

---

**Full Guide:** `SETUP_GUIDE.md` file দেখুন
