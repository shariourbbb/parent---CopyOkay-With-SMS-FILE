# Connection Problem Fix

## Problem
Service crash হচ্ছে কারণ `FOREGROUND_SERVICE_DATA_SYNC` permission missing ছিল।

## Solution Applied
✅ Added `FOREGROUND_SERVICE_DATA_SYNC` permission to AndroidManifest.xml

## Next Steps

1. **Rebuild and Reinstall the app:**
   ```bash
   # In Android Studio:
   Build → Clean Project
   Build → Rebuild Project
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

2. **Uninstall old version completely:**
   - Settings → Apps → Device Tracker → Uninstall
   - Or: `adb uninstall com.parentaltracker`

3. **Install new APK:**
   - Install the newly built APK

4. **Check Server URL:**
   - Make sure `MonitoringService.kt` line 38 has correct server URL:
   ```kotlin
   private val SERVER_URL = "https://YOUR_NGROK_URL"
   // Or local network:
   // private val SERVER_URL = "http://192.168.1.100:3000"
   ```

5. **Verify Server is Running:**
   ```bash
   cd server
   npm start
   ```

6. **Check ngrok (if using):**
   ```bash
   ngrok http 3000
   ```

7. **Check Logcat:**
   - Look for "MonitoringService onCreate" 
   - Look for socket connection messages
   - Should NOT see SecurityException anymore

## Expected Behavior

After fix:
- ✅ Service starts without SecurityException
- ✅ Notification appears
- ✅ Device connects to server
- ✅ Device appears in Dashboard
- ✅ Real-time updates work

## If Still Not Connecting

1. **Check SERVER_URL** in MonitoringService.kt
2. **Check server logs** for connection attempts
3. **Check device internet** connection
4. **Check firewall** blocking ngrok or server
5. **Check logcat** for socket errors
