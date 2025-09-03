package chat.revolt.markdown.jbm

import chat.revolt.markdown.jbm.sequentialparsers.SpoilerParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager

/**
 * Enhanced RSM Flavour Descriptor that inherits from the original working RSMFlavourDescriptor.
 * This enhanced version adds support for spoilers by carefully adding SpoilerParser 
 * at the end of the parsing sequence to avoid conflicts with existing parsers.
 */
class RSMEnhancedFlavourDescriptor : RSMFlavourDescriptor() {
    override val sequentialParserManager = object : SequentialParserManager() {
        override fun getParserSequence(): List<SequentialParser> {
            val originalParsers = super@RSMEnhancedFlavourDescriptor.sequentialParserManager.getParserSequence()
            
            return originalParsers + SpoilerParser()
        }
    }
}