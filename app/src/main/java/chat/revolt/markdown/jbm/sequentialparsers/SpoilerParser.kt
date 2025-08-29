package chat.revolt.markdown.jbm.sequentialparsers

import chat.revolt.markdown.jbm.RSMElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class SpoilerParser : SequentialParser {
    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.TEXT) {
                val text = iterator.toString()
                val startIndex = text.indexOf("||")
                if (startIndex != -1) {
                    val endIndex = text.indexOf("||", startIndex + 2)
                    if (endIndex != -1) {
                        // Found complete spoiler within single text token
                        result.withNode(
                            SequentialParser.Node(
                                iterator.index..iterator.index + 1,
                                RSMElementTypes.SPOILER
                            )
                        )
                        iterator = iterator.advance()
                        continue
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}
