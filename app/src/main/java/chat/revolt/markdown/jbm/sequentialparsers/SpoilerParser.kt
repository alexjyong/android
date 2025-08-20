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
            if (iterator.type == MarkdownTokenTypes.TEXT && iterator.charLookup(0) == '|') {
                val nextIterator = iterator.advance()
                if (nextIterator.type == MarkdownTokenTypes.TEXT && nextIterator.charLookup(0) == '|') {
                    val endIterator = findClosingSpoiler(nextIterator.advance())

                    if (endIterator != null) {
                        result.withNode(
                            SequentialParser.Node(
                                iterator.index..endIterator.index + 1,
                                RSMElementTypes.SPOILER
                            )
                        )
                        iterator = endIterator.advance()
                        continue
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }

    private fun findClosingSpoiler(it: TokensCache.Iterator): TokensCache.Iterator? {
        var iterator = it
        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.TEXT && iterator.charLookup(0) == '|') {
                val nextIterator = iterator.advance()
                if (nextIterator.type == MarkdownTokenTypes.TEXT && nextIterator.charLookup(0) == '|') {
                    return nextIterator
                }
            }
            iterator = iterator.advance()
        }
        return null
    }
}
