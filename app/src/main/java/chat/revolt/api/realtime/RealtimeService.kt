package chat.revolt.api.realtime

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder


class RealtimeService : Service() {
    class LocalBinder : Binder() {
        fun getService(): RealtimeService {
            return RealtimeService()
        }
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {

    }
}