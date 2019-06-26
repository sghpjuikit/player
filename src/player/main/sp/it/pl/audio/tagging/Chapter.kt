package sp.it.pl.audio.tagging

import javafx.util.Duration
import sp.it.util.functional.Try
import sp.it.util.text.Strings
import java.util.Objects

/** Chapter is a text associated with specific point of time in a song. */
class Chapter: Comparable<Chapter> {

    /** Time of this chapter from the beginning of the song. Always equivalent to mathematical integer */
    var time: Duration
    /** Text of this chapter. */
    var text = ""

    constructor(time: Duration, text: String = "") {
        this.time = time.toMillis().computeTimeFromMs()
        this.text = text
    }

    /** @return true iff both chapters text and their time */
    override fun equals(other: Any?) = this===other || other is Chapter && time==other.time

    override fun hashCode() = 19*3 + Objects.hashCode(time)

    /** @return result of comparison by time */
    override fun compareTo(other: Chapter) = time.compareTo(other.time)

    /** @return text, which can be reconstructed back to Chapter with [Chapter], e.g.: "9800-New Chapter" */
    override fun toString() = "${time.toMillis()}$separator$text"

    companion object {
        private const val separator = "-"

        /**
         * @param text string to parse, should be a result of [Chapter.toString]
         * @return chapter from the specified string or error if conversion not possible
         */
        fun chapter(text: String): Try<Chapter, String> {
            val i = text.indexOf(separator)
            return if (i==-1) {
                Try.error("Not parsable chapter string: '$text' - must contain $separator")
            } else {
                val cTime = text.substring(0, i).toDoubleOrNull()?.computeTimeFromMs() ?: Duration(0.0)
                val cText = text.substring(i + 1, text.length)
                Try.ok(Chapter(cTime, cText))
            }
        }

        private fun Double.computeTimeFromMs() = Duration(Math.rint(this)) // round to decimal number before assigning

        @JvmStatic fun validateChapterText(text: String): Try<String, String> = when {
            text.contains(Metadata.SEPARATOR_CHAPTER) -> Try.error("Must not contain '${Metadata.SEPARATOR_CHAPTER}'")
            text.contains(Metadata.SEPARATOR_GROUP) -> Try.error("Must not contain '${Metadata.SEPARATOR_GROUP}'")
            text.contains(Metadata.SEPARATOR_RECORD) -> Try.error("Must not contain '${Metadata.SEPARATOR_RECORD}'")
            text.contains(Metadata.SEPARATOR_UNIT) -> Try.error("Must not contain '${Metadata.SEPARATOR_UNIT}'")
            else -> Try.ok(text)
        }

    }
}

class Chapters(val chapters: List<Chapter> = listOf()): Strings {

    override val strings: Sequence<String> get() = chapters.asSequence().map { it.text }

}