package chat.revolt

import android.app.Application
import android.os.StrictMode
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

        if (BuildConfig.DEBUG) {
            // Enable strict mode primarily to catch non-API usage, although we detect all
            // violations for our reference.
            // https://developer.android.com/reference/android/os/StrictMode
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy
                    .Builder()
                    .apply {
                        detectAll()
                        penaltyLog()
                    }
                    .build()
            )
        }
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
