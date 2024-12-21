package chat.revolt

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority

@HiltAndroidApp
class RevoltApplication : Application() {
    companion object {
        lateinit var instance: RevoltApplication
    }

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
