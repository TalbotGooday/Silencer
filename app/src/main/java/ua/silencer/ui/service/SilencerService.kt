package ua.silencer.ui.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import okhttp3.*
import ua.silencer.MainActivity
import ua.silencer.R
import ua.silencer.ui.status.StatusActivity
import ua.silencer.utils.ServerException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class SilencerService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 9083110
        private const val NUMBER_OF_CORES = 5

        private const val STOP_ACTION = "stop_silencer"

        const val SILENCER_EVENT = "SILENCER_EVENT"

        const val EVENT_STOP = "stop"
        const val EVENT_MESSAGE = "message"
    }

    private val addresses = mutableListOf<String>()
    private val addressesDown = mutableSetOf<String>()

    private val client = createClient()

    private var executor: ThreadPoolExecutor? = null

    private val silenceRunnable = Runnable {
        try {
            val addressesCopy = ArrayList(addresses)

            while (isRunning) {
                addressesCopy.forEach { address ->
                    if (isRunning) {
                        // TODO ADD THIS TO THE SETTINGS
                        if (addressesDown.contains(address).not()) {
                            post(address)
                        }
                    }
                }

                // TODO ADD THIS TO THE SETTINGS
                // isRunning = addressesDown.size != addressesCopy.size
            }
        } catch (e: InterruptedException) {
            // Restore interrupt status.
            Thread.currentThread().interrupt()
        }
    }

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(200L, TimeUnit.MILLISECONDS)
            .build()
    }

    private var isRunning = true

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val elements = intent?.getStringArrayExtra("addressees") ?: emptyArray()

        if (intent?.action == STOP_ACTION || elements.isEmpty()) {
            isRunning = false
            executor?.shutdown()
            executor = null

            stopSelf()
            sendStopEvent()

            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.starting)))

        addresses.clear()
        addresses.addAll(elements)

        Log.d("Silencer", addresses.joinToString("\n"))

        executor = ThreadPoolExecutor(
            NUMBER_OF_CORES,
            NUMBER_OF_CORES,
            1L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue()
        ).also { executor ->
            repeat(NUMBER_OF_CORES) {
                executor.execute(silenceRunnable)
            }
        }

        isRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        executor?.shutdown()
        executor = null
    }

    private fun post(url: String) {
        val fixedUrl = url.let {
            if (url.startsWith("http").not()) {
                "http://$url"
            } else url
        }

        updateNotification(fixedUrl)

        val request = Request.Builder()
            .url(fixedUrl)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            .addHeader("Accept-Language", " en-US,en;q=0.5")
            .addHeader("Upgrade-Insecure-Requests", " 1")
            .addHeader("Sec-Fetch-Dest", " document")
            .addHeader("Sec-Fetch-Mode", " navigate")
            .addHeader("Sec-Fetch-Site", " none")
            .addHeader("Sec-Fetch-User", " ?1")
            .addHeader("Pragma", " no-cache")
            .addHeader("Cache-Control", " no-cache")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code in 500..599) {
                        throw ServerException()
                    } else {
                        sendStatus(fixedUrl, true)
                    }
                }

                sendStatus(fixedUrl, true)
            }
        } catch (e: Exception) {
            if (e is InterruptedIOException || e is ConnectException || e is ServerException) {
                addressesDown.add(url)
                sendStatus(fixedUrl, false)
            }
        }
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(message: String): Notification {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val stopIntent = Intent(this, SilencerService::class.java)
            .apply {
                action = STOP_ACTION
            }.let {
                PendingIntent.getService(
                    this,
                    NOTIFICATION_ID,
                    it,
                    flag
                )
            }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.app_name))
            .bigText(message)

        val builder = NotificationCompat.Builder(this, channelId)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setOngoing(true)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.stop), stopIntent)

        val intent = Intent(this, StatusActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("addresses", addresses.toTypedArray())

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "Channel"
        val channelName = "Floating Background Service"
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun sendStatus(url: String, isAlive: Boolean) {
        Log.d("Silencer", "----> $url, isAlive: $isAlive")
        val intent = Intent(SILENCER_EVENT).apply {
            putExtra("address", url)
            putExtra("isAlive", isAlive)
            putExtra("event", EVENT_MESSAGE)
        }

        sendBroadcast(intent)
    }

    private fun sendStopEvent() {
        val intent = Intent(SILENCER_EVENT).apply {
            putExtra("event", EVENT_STOP)
        }

        sendBroadcast(intent)
    }
}