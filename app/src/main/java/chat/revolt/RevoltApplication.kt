package chat.revolt

import android.app.Application
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
        
        EmojiRepository.initialize(applicationScope)
    }

    init {
        instance = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
