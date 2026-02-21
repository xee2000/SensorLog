package com.sensorlog.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.sensorlog.api.RetrofitClient
import com.sensorlog.ui.main.MainActivity
import com.sensorlog.util.ThresholdPreferences
import kotlinx.coroutines.*

/**
 * 포그라운드 서비스 — 60초마다 센서 최신값을 조회하여
 * 임계값 초과 시 알림 + 반복 진동
 *
 * ※ BuildConfig import 없음 — 폴링 주기는 companion object 상수로 관리
 */
class SensorMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.sensorlog.ACTION_START"
        const val ACTION_STOP  = "com.sensorlog.ACTION_STOP"

        /** 폴링 주기 (밀리초) */
        private const val INTERVAL_MS = 60_000L

        private const val CHANNEL_MONITORING  = "ch_monitoring"
        private const val CHANNEL_ALERT       = "ch_alert"
        private const val NOTIF_ID_MONITORING = 2001
        private const val NOTIF_ID_ALERT      = 2002

        fun start(context: Context) {
            val intent = Intent(context, SensorMonitorService::class.java)
                .apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SensorMonitorService::class.java)
                .apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private lateinit var vibrator: Vibrator
    private lateinit var prefs: ThresholdPreferences
    private var isVibrating = false

    // ─────────────────────────────────────────────
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = ThresholdPreferences(this)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(
                    NOTIF_ID_MONITORING,
                    buildMonitoringNotification("센서 모니터링 중...")
                )
                startMonitoring()
            }
            ACTION_STOP -> {
                stopVibration()
                cancelAlertNotification()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        stopVibration()
        cancelAlertNotification()
        prefs.setMonitoringEnabled(false)
        super.onDestroy()
    }

    // ─────────────────────────────────────────────
    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                checkSensorValues()
                delay(INTERVAL_MS)
            }
        }
    }

    private suspend fun checkSensorValues() {
        val sensorId = prefs.getSensorId()
        if (sensorId.isBlank()) return

        try {
            val response = RetrofitClient.sensorApiService.getLatestSensorData(sensorId)
            if (!response.isSuccessful) return
            val data = response.body() ?: return

            val temp     = data.temperature
            val humidity = data.humidity

            val tempMin = prefs.getTempMin().toDouble()
            val tempMax = prefs.getTempMax().toDouble()
            val humMin  = prefs.getHumidityMin().toDouble()
            val humMax  = prefs.getHumidityMax().toDouble()

            val alerts = mutableListOf<String>()
            if (temp < tempMin || temp > tempMax) {
                alerts.add("온도 %.1f°C (허용: %.1f ~ %.1f°C)".format(temp, tempMin, tempMax))
            }
            if (humidity < humMin || humidity > humMax) {
                alerts.add("습도 %.1f%% (허용: %.1f ~ %.1f%%)".format(humidity, humMin, humMax))
            }

            updateMonitoringNotification("온도: %.1f°C | 습도: %.1f%%".format(temp, humidity))

            if (alerts.isNotEmpty()) {
                showAlertNotification(alerts.joinToString("\n"))
                startVibration()
            } else {
                cancelAlertNotification()
                stopVibration()
            }
        } catch (_: Exception) {
            // 네트워크 오류 시 조용히 무시
        }
    }

    // ─────────────────────────────────────────────
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                "센서 모니터링",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "백그라운드 모니터링 상태 표시" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                "센서 임계값 경보",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "임계값 초과 경보"
                enableVibration(false)
            }
        )
    }

    private fun mainPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildMonitoringNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("SensorLog 모니터링 중")
            .setContentText(text)
            .setContentIntent(mainPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun updateMonitoringNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_MONITORING, buildMonitoringNotification(text))
    }

    private fun showAlertNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ 센서 임계값 초과!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(mainPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_ALERT, notification)
    }

    private fun cancelAlertNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIF_ID_ALERT)
    }

    // ─────────────────────────────────────────────
    /** 임계값 초과 시 반복 진동 (0ms 대기 → 600ms 진동 → 400ms 정지 → 반복) */
    private fun startVibration() {
        if (isVibrating) return
        isVibrating = true
        val pattern = longArrayOf(0, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, intArrayOf(0, 255, 0), 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    /** 조건 해제 시 진동 중지 */
    private fun stopVibration() {
        if (!isVibrating) return
        isVibrating = false
        vibrator.cancel()
    }
}
