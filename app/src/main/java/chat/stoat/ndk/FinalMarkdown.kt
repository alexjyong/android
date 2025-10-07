package chat.stoat.ndk

import kotlinx.serialization.Serializable

@Serializable
data class FinalMarkdownNodeTest(
    val test: Int,
)

@Suppress("KotlinJniMissingFunction")
object FinalMarkdown {
    external fun init(debug: Boolean)
    external fun process(input: String): FinalMarkdownNodeTest
}