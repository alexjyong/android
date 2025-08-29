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
                
                // Look for opening ||
                if (text.contains("||")) {
                    // Find the closing || by scanning ahead
                    var searchIterator = iterator
                    var foundClosing = false
                    var endPosition = iterator.index
                    
                    // Scan forward to find closing ||
                    while (searchIterator.type != null) {
                        val searchText = searchIterator.toString()
                        if (searchText.contains("||") && searchIterator.index > iterator.index) {
                            foundClosing = true
                            endPosition = searchIterator.index
                            break
                        }
                        searchIterator = searchIterator.advance()
                    }
                    
                    if (foundClosing) {
                        // Create spoiler node spanning from start to end
                        result.withNode(
                            SequentialParser.Node(
                                iterator.index..endPosition,
                                RSMElementTypes.SPOILER
                            )
                        )
                        // Skip to after the closing token
                        iterator = searchIterator.advance()
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
