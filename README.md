# Simple Parental Device Tracker

একটি সহজ এবং কার্যকর Parental Control প্রজেক্ট যা Android ডিভাইস মনিটরিং এবং Admin Dashboard প্রদান করে।

## 📋 প্রজেক্ট স্ট্রাকচার

```
parent/
├── android-app/          # Android Kotlin Application
│   └── app/
│       └── src/main/
│           ├── java/com/parentaltracker/
│           │   ├── MainActivity.kt
│           │   └── MonitoringService.kt
│           ├── res/layout/
│           │   └── activity_main.xml
│           └── AndroidManifest.xml
└── server/               # Node.js Server
    ├── server.js
    ├── package.json
    └── public/
        └── index.html
```

## 🚀 ইনস্টলেশন এবং সেটআপ

### Android App সেটআপ

1. **Android Studio খুলুন** এবং `android-app` ফোল্ডার open করুন
2. **Gradle Sync** করুন (File → Sync Project with Gradle Files)
3. **SERVER_URL আপডেট করুন:**
   - `MonitoringService.kt` ফাইলে `SERVER_URL` পরিবর্তন করুন
   - Example: `private val SERVER_URL = "http://192.168.1.100:3000"`
   - অথবা ngrok URL ব্যবহার করুন: `private val SERVER_URL = "https://abc123.ngrok.io"`

4. **Build এবং Run করুন** আপনার Android device বা emulator এ

### Node.js Server সেটআপ

1. **Terminal/Command Prompt খুলুন** এবং `server` ফোল্ডারে যান:
```bash
cd server
```

2. **Dependencies ইনস্টল করুন:**
```bash
npm install
```

3. **Server চালু করুন:**
```bash
npm start
```

Server `http://localhost:3000` এ চালু হবে।

### Public URL তৈরি (ngrok ব্যবহার করে)

1. **ngrok ডাউনলোড করুন:** https://ngrok.com/download

2. **ngrok ইন্সটল করুন** এবং account তৈরি করুন

3. **ngrok চালু করুন** (নতুন terminal/command prompt এ):
```bash
ngrok http 3000
```

4. **ngrok URL কপি করুন** (যেমন: `https://abc123.ngrok.io`)

5. **Android App এ URL আপডেট করুন:**
   - `MonitoringService.kt` ফাইলে:
   ```kotlin
   private val SERVER_URL = "https://abc123.ngrok.io"
   ```

6. **App recompile করুন** এবং device এ install করুন

## 📱 Android App Features

- ✅ Runtime Permissions (SMS, Call Log, Storage, Phone State)
- ✅ Simple Welcome Screen
- ✅ Background Monitoring Service (Foreground Service)
- ✅ Real-time Socket.IO Connection
- ✅ Every 5 seconds: Device Info, Battery, Charging Status
- ✅ SMS Detection (ContentObserver)
- ✅ Call Log Detection (ContentObserver)
- ✅ File Manager Access (Folder/File Listing)

## 🌐 Admin Dashboard Features

- ✅ Modern, Responsive UI
- ✅ Real-time Device List Table
- ✅ Battery Status with Charging Indicator
- ✅ Active/Inactive Status Indicator
- ✅ SMS History Modal
- ✅ Call History Modal
- ✅ File Manager Modal (Browse Folders)

## 🔧 Configuration

### Android App

**Minimum SDK:** 21 (Android 5.0 Lollipop)
**Target SDK:** 34 (Android 14)
**Socket.IO Client:** 2.1.0

### Server

**Port:** 3000 (default)
**Storage:** In-memory (no database)

## 📝 Permissions Required

Android App নিম্নলিখিত permissions ব্যবহার করে:
- `INTERNET` - Server connection
- `READ_SMS` - SMS monitoring
- `SEND_SMS` - SMS access
- `READ_CALL_LOG` - Call log monitoring
- `READ_PHONE_STATE` - Device info
- `READ_EXTERNAL_STORAGE` - File access (Android < 13)
- `WRITE_EXTERNAL_STORAGE` - File access (Android < 13)
- `READ_MEDIA_IMAGES` - Media access (Android 13+)
- `READ_MEDIA_VIDEO` - Media access (Android 13+)
- `READ_MEDIA_AUDIO` - Media access (Android 13+)

## 🛠️ Troubleshooting

### App server এ connect করতে পারছে না

1. **SERVER_URL সঠিক কিনা** check করুন
2. **Firewall/Antivirus** ngrok বা server port block করছে কিনা check করুন
3. **Device এবং Computer same network** এ আছে কিনা check করুন (ngrok ব্যবহার করলে এটা দরকার নেই)

### Permissions denied

1. **Settings → Apps → Device Tracker → Permissions** থেকে manually permissions enable করুন
2. App reinstall করুন

### Service background এ kill হচ্ছে

1. **Battery Optimization** disable করুন:
   - Settings → Apps → Device Tracker → Battery → Unrestricted
2. **Auto-start** enable করুন (brand-specific settings)

## 📌 Important Notes

- এটি একটি **parental safety** প্রজেক্ট এবং ethical use এর জন্য
- App **foreground service** ব্যবহার করে যাতে background এ kill না হয়
- সব data **in-memory** store হয় - server restart করলে data clear হবে
- Production এর জন্য database (MongoDB/PostgreSQL) add করুন

## 🔒 Security

- Production environment এ **HTTPS** ব্যবহার করুন
- **Authentication** add করুন admin dashboard এর জন্য
- **Encryption** add করুন sensitive data এর জন্য

## 📞 Support

কোনো সমস্যা হলে issue report করুন বা code review করুন।

---

**Made with ❤️ for Parental Safety**
