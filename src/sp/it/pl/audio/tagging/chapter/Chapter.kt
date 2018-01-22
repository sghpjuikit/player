package sp.it.pl.audio.tagging.chapter

import javafx.util.Duration
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.functional.Try
import sp.it.pl.util.text.Strings
import sp.it.pl.util.units.Dur
import java.util.*

/** Chapter is a text associated with specific point of time in a song. */
class Chapter: Comparable<Chapter> {

    /** Time of this chapter from the beginning of the song. Always equivalent to mathematical integer */
    var time: Dur

    /** Text of this chapter. */
    var text = ""

    @JvmOverloads constructor(time: Duration, text: String = "") {
        this.time = time.toMillis().computeTimeFromMs()
        this.text = text
    }

    /**
     * @param text string to parse, should be a result of [toString]
     * @throws RuntimeException if not parsable String
     */
    constructor(text: String) {
        val i = text.indexOf(separator)
        if (i==-1) throw IllegalArgumentException("Not parsable chapter string: $text")     // TODO: handle better
        this.time = text.substring(0, i).toDoubleOrNull()?.computeTimeFromMs() ?: Dur(0.0)
        this.text = text.substring(i+1, text.length)
    }

    /** @return true iff both chapters text and their time */
    override fun equals(other: Any?) = this===other || other is Chapter && time==other.time

    override fun hashCode() = 19*3+Objects.hashCode(this.time)

    /** @return result of comparison by time */
    override fun compareTo(other: Chapter) = time.compareTo(other.time)

    /** @return text, which can be reconstructed back to Chapter with [Chapter], e.g.: "9800-New Chapter" */
    override fun toString() = "${time.toMillis()}$separator$text"

}

class Chapters(val chapters: List<Chapter> = listOf()): Strings {

    override val strings: Sequence<String> get() = chapters.asSequence().map { it.text }

}

fun validateChapterText(text: String): Try<String> = when {
    text.contains(Metadata.SEPARATOR_CHAPTER) -> Try.error("Must not contain '${Metadata.SEPARATOR_CHAPTER}'")
    text.contains(Metadata.SEPARATOR_GROUP) -> Try.error("Must not contain '${Metadata.SEPARATOR_GROUP}'")
    text.contains(Metadata.SEPARATOR_RECORD) -> Try.error("Must not contain '${Metadata.SEPARATOR_RECORD}'")
    text.contains(Metadata.SEPARATOR_UNIT) -> Try.error("Must not contain '${Metadata.SEPARATOR_UNIT}'")
    else -> Try.ok(text)
}

private fun Double.computeTimeFromMs() = Dur(Math.rint(this)) // round to decimal number before assigning

private val separator = "-"