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
        android.util.Log.d("SpoilerParser", "SpoilerParser.parse() called with ${rangesToGlue.size} ranges")
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            android.util.Log.d("SpoilerParser", "Processing token: ${iterator.type}, text: '${iterator.toString()}'")
            
            if (iterator.type == MarkdownTokenTypes.TEXT) {
                val text = iterator.toString()
                
                if (text == "||" || text.startsWith("||") || text.contains("||")) {
                    android.util.Log.d("SpoilerParser", "Found potential spoiler start: '$text'")
                    
                    val start = iterator.index
                    var searchIterator = iterator.advance()
                    var foundContent = false
                    var foundEnd = false
                    
                    while (searchIterator.type != null) {
                        val searchText = searchIterator.toString()
                        android.util.Log.d("SpoilerParser", "Scanning: '${searchText}' (${searchIterator.type})")
                        
                        if (searchText == "||" || searchText.endsWith("||") || searchText.contains("||")) {
                            android.util.Log.d("SpoilerParser", "Found potential spoiler end: '$searchText'")
                            foundEnd = true
                            
                            result.withNode(
                                SequentialParser.Node(
                                    start..searchIterator.index,
                                    RSMElementTypes.SPOILER
                                )
                            )
                            android.util.Log.d("SpoilerParser", "Created SPOILER node from $start to ${searchIterator.index}")
                            iterator = searchIterator.advance()
                            break
                        }
                        
                        if (searchIterator.type == MarkdownTokenTypes.TEXT || searchIterator.type.toString() == "WHITE_SPACE") {
                            foundContent = true
                        }
                        
                        searchIterator = searchIterator.advance()
                    }
                    
                    if (foundEnd) {
                        continue
                    } else {
                        android.util.Log.d("SpoilerParser", "No closing || found")
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}
