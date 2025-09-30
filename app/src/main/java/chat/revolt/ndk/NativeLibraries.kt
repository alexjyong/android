package chat.revolt.ndk

import chat.revolt.BuildConfig

annotation class NativeLibrary(val name: String) {
    companion object {
        const val LIB_NAME_NATIVE_MARKDOWN = "stendal"
        const val LIB_NAME_NATIVE_MARKDOWN_V2 = "finalmarkdown"
    }
}

object NativeLibraries {
    fun init() {
        System.loadLibrary(NativeLibrary.LIB_NAME_NATIVE_MARKDOWN)
        System.loadLibrary(NativeLibrary.LIB_NAME_NATIVE_MARKDOWN_V2)
        Stendal.init()
        FinalMarkdown.init(BuildConfig.DEBUG)
    }
}
