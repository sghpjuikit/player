package sp.it.util.file

import mu.KotlinLogging
import sp.it.util.dev.Blocks
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import java.io.File
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8

private val logger = KotlinLogging.logger { }

/**
 * Single property of a property file.
 *
 * Multi-line values or comments must use `'\n'` separator.
 */
data class Property(val key: String, val value: String, val comment: String)

/**
 * Reads properties from this properties file.
 *
 * The text will be read in [UTF_8] and no escaping will considered. See [writeProperties].
 *
 * Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
@Blocks
fun File.readProperties(): Try<Map<String, String>, Throwable> {
   return runTry {
      useLines(UTF_8) {
         it.filterNot { it.isEmpty() || it.startsWith('#') || it.startsWith('!') || '=' !in it }
            .associate { it.substringBefore("=") to it.substringAfter('=') }
      }
   }.ifError {
      logger.error(it) { "Failed to read properties file= $this" }
   }
}

/**
 * Writes specified properties to this properties file, overwriting any previously existing file.
 *
 * The text will be written in [UTF_8] and no escaping will be applied. See [readProperties].
 *
 * Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
@Blocks
fun File.writeProperties(title: String, properties: Collection<Property>): Try<Unit, Throwable> {
   val header = """
        |name: $title
        |type: property file
        |last auto-modified: ${LocalDateTime.now()}
        |encoding: UTF-8
        |line separator: '\n', '\r' or "\r\n"
        |escaping: UTF-8
        |property format: {property path}{separator}{property value}
        |   - {property path}  must be lowercase, separated with '.', contain no spaces (use '-' instead), e.g.: this.is.a-path
        |   - {separator}      must be '=' string
        |   - {property value} can be any string (even empty)
        |ignored lines:
        |   - comment lines (start with '#' or '!')
        |   - empty lines
        |
        |Some properties may be read-only or have value constraints. Unfit values may be ignored.
        """.trimMargin()

   fun Appendable.appendComment(comment: CharSequence) = apply {
      if (comment.isNotBlank())
         comment.split("\n").forEach { line -> append("# ").appendln(line) }
   }

   return runTry {
      bufferedWriter(UTF_8).use {
         it.appendComment(header)
         it.appendln()
         it.appendln()
         properties.asSequence().sortedBy { it.key }.forEach { (name, value, comment) ->
            it.appendComment(comment)
            it.append(name)
            it.append("=")
            it.appendln(value)
         }
      }
   }.ifError {
      logger.error(it) { "Failed to write properties file= $this" }
   }
}