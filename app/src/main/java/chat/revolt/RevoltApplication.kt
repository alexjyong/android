package chat.revolt

import android.app.Application
import android.os.StrictMode
import chat.revolt.internals.EmojiRepository
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import logcat.AndroidLogcatLogger
import logcat.LogPriority

@HiltAndroidApp
class RevoltApplication : Application() {
    companion object {
        lateinit var instance: RevoltApplication
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
        
        EmojiRepository.initialize(applicationScope)
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
