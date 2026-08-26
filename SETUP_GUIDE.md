# 📚 Step-by-Step Setup Guide

## ধাপ ১: Node.js Server Setup

### 1.1 Server Folder এ যান
```bash
cd server
```

### 1.2 Dependencies Install করুন
```bash
npm install
```

### 1.3 Server চালু করুন
```bash
npm start
```

আপনি দেখবেন:
```
Server running on http://localhost:3000
Dashboard available at http://localhost:3000
```

## ধাপ ২: ngrok Setup (Public URL)

### 2.1 ngrok ডাউনলোড করুন
- https://ngrok.com/download থেকে ডাউনলোড করুন
- Windows হলে `ngrok.exe` extract করুন

### 2.2 ngrok Account তৈরি করুন
1. https://ngrok.com/signup এ signup করুন
2. Dashboard থেকে `authtoken` copy করুন

### 2.3 ngrok Authenticate করুন
```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

### 2.4 ngrok Tunnel চালু করুন
নতুন terminal/command prompt খুলুন:
```bash
ngrok http 3000
```

আপনি দেখবেন:
```
Forwarding  https://abc123.ngrok-free.app -> http://localhost:3000
```

**এই HTTPS URL টি কপি করুন** (যেমন: `https://abc123.ngrok-free.app`)

## ধাপ ৩: Android App Setup

### 3.1 Android Studio এ Project Open করুন
1. Android Studio খুলুন
2. File → Open → `android-app` folder select করুন
3. Gradle Sync করুন (automatic হতে পারে)

### 3.2 Server URL আপডেট করুন

`MonitoringService.kt` file খুলুন:
```
android-app/app/src/main/java/com/parentaltracker/MonitoringService.kt
```

Line 38 এ SERVER_URL পরিবর্তন করুন:
```kotlin
// Before:
private val SERVER_URL = "http://YOUR_SERVER_URL:3000"

// After (ngrok URL ব্যবহার করে):
private val SERVER_URL = "https://abc123.ngrok-free.app"

// অথবা local network এর জন্য (device এবং computer same WiFi তে থাকলে):
private val SERVER_URL = "http://192.168.1.100:3000"
```

### 3.3 Local Network IP জানতে

**Windows:**
```bash
ipconfig
```
"IPv4 Address" দেখুন (যেমন: 192.168.1.100)

**Mac/Linux:**
```bash
ifconfig
```
বা
```bash
ip addr show
```

### 3.4 App Build করুন
1. Build → Make Project (Ctrl+F9 / Cmd+F9)
2. Build → Build Bundle(s) / APK(s) → Build APK(s)

### 3.5 Device এ Install করুন
1. APK file টি device এ transfer করুন
2. Install করুন (Unknown Sources allow করতে হতে পারে)
3. App open করুন

## ধাপ ৪: Permissions Allow করুন

App open করার পর:
1. সব permissions allow করুন (INTERNET, SMS, Call Log, Storage)
2. Welcome screen দেখাবে
3. App automatically background এ চলে যাবে
4. Notification bar এ "Device Tracker" notification দেখাবে

## ধাপ ৫: Dashboard Check করুন

1. Browser এ `http://localhost:3000` open করুন
2. আপনার device টেবিলে দেখা যাবে:
   - Device Name
   - Model
   - Battery %
   - Status (Active/Inactive)
   - Actions buttons

## ধাপ ৬: Testing

### SMS Test:
1. Device এ test SMS send করুন
2. Dashboard এ "View SMS" button click করুন
3. SMS list দেখুন (real-time update হবে)

### Call Test:
1. Device এ test call করুন
2. Dashboard এ "View Calls" button click করুন
3. Call log দেখুন

### File Manager Test:
1. Dashboard এ "View Files" button click করুন
2. Device এর files/folders browse করুন

## 🔧 Troubleshooting

### Problem: App connect করতে পারছে না

**Solution:**
1. SERVER_URL সঠিক আছে কিনা check করুন
2. Server running আছে কিনা check করুন (`npm start`)
3. ngrok running আছে কিনা check করুন
4. Device এর internet connection check করুন
5. Firewall/Antivirus ngrok block করছে কিনা check করুন

### Problem: Permissions denied

**Solution:**
1. Settings → Apps → Device Tracker → Permissions
2. Manually সব permissions enable করুন
3. App restart করুন

### Problem: Service background এ kill হচ্ছে

**Solution:**
1. Settings → Apps → Device Tracker → Battery → Unrestricted
2. Settings → Apps → Device Tracker → App Launch → Allow all
3. Brand-specific auto-start settings enable করুন

### Problem: Dashboard এ device দেখাচ্ছে না

**Solution:**
1. App notification আছে কিনা check করুন
2. Server console এ connection message আছে কিনা check করুন
3. Device internet connection check করুন
4. App reinstall করুন

## 📝 Important Notes

1. **ngrok Free Version:** 
   - Session expire হতে পারে (8 hours)
   - URL change হতে পারে
   - Restart হলে নতুন URL generate হবে

2. **Production Use:**
   - ngrok paid plan ব্যবহার করুন
   - অথবা VPS/Cloud server setup করুন
   - Domain name add করুন

3. **Security:**
   - Admin dashboard এ authentication add করুন
   - HTTPS ব্যবহার করুন
   - Sensitive data encrypt করুন

## 🎉 Success!

আপনার Parental Device Tracker এখন fully functional! 

Dashboard এ device monitor করতে পারবেন এবং real-time updates পাবেন।

---

**Need Help?** README.md file এ আরো details আছে।
