package sp.it.util.file.properties

import mu.KotlinLogging
import sp.it.util.dev.Blocks
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.file.properties.PropVal.PropValN
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8
import sp.it.util.functional.ifNotNull

private val logger = KotlinLogging.logger { }

/** Single property of a property file, characterized by key, value and a comment. */
data class Property(val key: String, val value: PropVal, val comment: String)

/** Property value. Single-value or multi-value. */
sealed class PropVal {
   /** Transform value to single-value. Multi-value returns 1st value or null if empty. */
   abstract val val1: String?
   /** Transform value to multi-value. Single-value is wrapped in a list. */
   abstract val valN: List<String>
   /** Number of values: single-value returns 1, multi-value 0-N. */
   abstract fun size(): Int

   /** Property value with a cardinality 1. */
   class PropVal1(val value: String): PropVal() {
      override val val1: String get() = value
      override val valN get() = listOf(value)
      override fun size() = 1
      override fun toString() = "${this::class.simpleName}($value)"
   }

   /** Property value with a cardinality N. */
   class PropValN(val value: List<String>): PropVal() {
      override val val1 get() = value.firstOrNull()
      override val valN get() = value
      override fun size() = value.size
      override fun toString() = "${this::class.simpleName}($value)"
   }
}

/**
 * Reads properties from this properties file.
 *
 * Supports multi-value by multiple entries with the same key.
 *
 * The text will be read in [UTF_8] and no escaping will be considered. See [writeProperties].
 *
 * Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
@Blocks
fun File.readProperties(): Try<Map<String, PropVal>, Throwable> {
   return runTry {
      useLines(UTF_8) {
         it.filterNot { it.isEmpty() || it.startsWith('#') || it.startsWith('!') || '=' !in it }
            .map { it.substringBefore("=") to it.substringAfter('=') }
            .groupBy { (key, _) -> key }
            .mapValues { (_, properties) ->
               when {
                  properties.size==1 -> when {
                     properties.first().second==">" -> PropValN(listOf())
                     else -> PropVal1(properties.first().second)
                  }
                  else -> PropValN(properties.map { it.second })
               }
            }
      }
   }.ifError {
      if (it !is FileNotFoundException)
         logger.error(it) { "Failed to read properties file= $this" }
   }
}

/**
 * Writes specified properties to this properties file, overwriting any previously existing file.
 *
 * Supports multi-value by multiple entries with the same key.
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
        |property format: {property path}{separator}{property value}
        |   - {property path}  must be lowercase, separated with '.', contain no spaces (use '-' instead), e.g.: this.is.a-path
        |   - {separator}      must be '=' or '=>' string
        |   - {property value} can be any string (even empty)
        |ignored lines:
        |   - comment lines (start with '#' or '!')
        |   - empty lines
        |multiple line values: unsupported
        |multiple values: supported
        |   - simply provide the same property multiple times
        |   - disambiguate no undefined property and multi-value property with 0 values property with =>
        |examples:
        |   ordinary.property.with.value=value      // produces 'value'
        |   ordinary.property.with.empty.value=     // produces ''
        |   ordinary.property.with.value=>value     // produces '>value'
        |   multi.property.with.no.values=>         // produces empty list
        |   multi.property.with.values=value1       // produces list containing value1 and value2
        |   multi.property.with.values=value2
        |
        |Some properties may be read-only or have value constraints. Unfit values may be ignored.
        """.trimMargin()

   fun Appendable.appendComment(comment: CharSequence) = apply {
      if (comment.isNotBlank())
         comment.split("\n").forEach { line -> append("# ").appendLine(line) }
   }

   return runTry {
      parentFile?.takeIf { !it.exists() }.ifNotNull { Files.createDirectories(it.toPath()) }

      bufferedWriter(UTF_8).use {
         it.appendComment(header)
         it.appendLine()
         properties.asSequence().sortedBy { it.key }.forEach { property ->
            it.appendComment(property.comment)
            when {
               property.value.valN.isEmpty() -> it.append(property.key).append("=>").appendLine()
               else -> property.value.valN.forEach { value -> it.append(property.key).append("=").appendLine(value) }
            }
         }
      }
   }.ifError {
      logger.error(it) { "Failed to write properties file= $this" }
   }
}