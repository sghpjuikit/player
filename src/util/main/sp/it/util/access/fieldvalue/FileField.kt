package sp.it.util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Schema
import com.drew.metadata.xmp.XmpDirectory
import mu.KotlinLogging
import sp.it.util.dev.Blocks
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.localDateTimeFromMillis
import sp.it.util.parsing.ConverterToString
import sp.it.util.parsing.Parsers
import sp.it.util.type.VType
import sp.it.util.type.type
import sp.it.util.units.FileSize
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.zip.ZipFile
import kotlin.text.Charsets.UTF_8

private val logger = KotlinLogging.logger { }

class FileField<T>: ObjectFieldBase<File, T> {

   private constructor(name: String, description: String, type: VType<T>, extractor: (File) -> T): super(type, extractor, name, description)

   override fun toS(o: T?, substitute: String) = if (o==null) substitute else toSConverter.toS(o)

   override fun cWidth(): Double = 160.0

   @Suppress("RemoveExplicitTypeArguments")
   companion object: ObjectFieldRegistry<File, FileField<*>>(File::class) {
      var toSConverter: ConverterToString<Any?> = object: ConverterToString<Any?> {
         override fun toS(o: Any?) = Parsers.DEFAULT.toS(o)
      }

      val PATH = this + FileField("Path", "Path", type<String>()) { it.path }
      val NAME = this + FileField("Name", "Name", type<String>()) { it.nameWithoutExtensionOrRoot }
      val NAME_FULL = this + FileField("Filename", "Filename", type<String>()) { it.nameOrRoot }
      val EXTENSION = this + FileField("Extension", "Extension", type<String>()) { it.extension }
      val SIZE = this + FileField("Size", "Size", type<FileSize>()) { if (it is CachingFile) it.fileSize else it.readFileSize() }
      val TIME_ACCESSED = this + FileField("Time Accessed", "Time Accessed", type<FileTime?>()) { if (it is CachingFile) it.timeAccessed else it.readTimeAccessed() }
      val TIME_MODIFIED = this + FileField("Time Modified", "Time Modified", type<LocalDateTime?>()) { if (it is CachingFile) it.timeModified else it.readTimeModified() }
      val TIME_CREATED = this + FileField("Time Created", "Time Created", type<FileTime?>()) { if (it is CachingFile) it.timeCreated else it.readTimeCreated() }
      val TYPE = this + FileField("Type", "Type", type<FileType>()) { FileType(it) }
      val MIME = this + FileField("Mime Type", "Mime Type", type<MimeType>()) { it.mimeType() }
      val MIME_GROUP = this + FileField("Mime Group", "Mime Group", type<String>()) { it.mimeType().group }
      val IS_HIDDEN = this + FileField("Is hidden", "Is hidden", type<Boolean>()) { if (it.isAbsolute && it.name.isEmpty()) false else it.isHidden } // File::isHidden gives true for roots, hence the check
   }

}

/** File implementation that provides additional lazy properties */
class CachingFile(f: File): File(f.path) {
   /** Time this file was last accessed or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeAccessed: FileTime? by lazy { readTimeAccessed() }

   /** Time this file was last modified or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeModified: LocalDateTime? by lazy { readTimeModified() }

   /** Time this file was created or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeCreated: FileTime? by lazy { readTimeCreated() }

   /** The minimum of [timeCreated] and [timeModified] or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeMinOfCreatedModified: FileTime? by lazy { readTimeMinOfCreatedAndModified() }

   /** The size of the file. Lazy. Blocks on first invocation. */
   val fileSize: FileSize by lazy { readFileSize() }
   private val isDirectory = super.isDirectory()
   override fun isDirectory() = isDirectory
   private val isFile = if (isDirectory) false else super.isFile()
   override fun isFile() = isFile
   private val isHidden = super.isHidden()
   override fun isHidden() = isHidden
}

@Blocks
private fun File.readFileSize(): FileSize = FileSize(this)

@Blocks
private fun File.readTimeAccessed(): FileTime? = readBasicFileAttributes()?.lastAccessTime()

@Blocks
private fun File.readTimeModified(): LocalDateTime? = lastModified().localDateTimeFromMillis()

@Blocks
private fun File.readTimeCreated(): FileTime? =
   when (this.mimeType().name) {
      "application/x-kra" -> readKritaTimeCreated() ?: readTimeMinOfCreatedAndModified()
      else -> when {
         // poor but fast way of avoiding xmp reading for directories
         name.contains(".") -> readXmpTimeCreated() ?: readTimeMinOfCreatedAndModified()
         else -> readTimeMinOfCreatedAndModified()
      }
   }

@Blocks
private fun File.readTimeMinOfCreatedAndModified(): FileTime? = readBasicFileAttributes()
   ?.run {
      minOf(creationTime(), lastModifiedTime())
   }

@Blocks
private fun File.readBasicFileAttributes(): BasicFileAttributes? =
   try {
      Files.readAttributes(toPath(), BasicFileAttributes::class.java)
   } catch (e: IOException) {
      logger.error(e) { "Unable to read basic file attributes for $this" }
      null
   }

private fun Long.toFileTime() = FileTime.fromMillis(this)

private fun LocalDateTime.toFileTime() = toInstant(UTC).toEpochMilli().toFileTime()

private fun File.readXmpTimeCreated(): FileTime? =
   try {
      ImageMetadataReader.readMetadata(this).getFirstDirectoryOfType(XmpDirectory::class.java)
         ?.xmpMeta?.getPropertyDate(Schema.XMP_PROPERTIES, "CreateDate")
         ?.calendar?.timeInMillis?.toFileTime()
   } catch (e: FileNotFoundException) {
      null
   } catch (e: IOException) {
      logger.error(e) { "Unable to read file creation date from XMP tag for $this" }
      null
   } catch (e: ImageProcessingException) {
      // 12Monkey bug: exception type !tell us when we are dealing with an error and when an unsupported file type
      when (e.message) {
         "File format is not supported" -> Unit
         "File format could not be determined" -> Unit
         else -> logger.error(e) { "Unable to read file creation date from XMP tag for $this" }
      }
      null
   } catch (e: Exception) {
      // 12Monkey bug: java.lang.NegativeArraySizeException
      logger.error(e) { "Unable to read file creation date from XMP tag for $this" }
      null
   }

private fun File.readKritaTimeCreated(): FileTime? =
   try {
      ZipFile(this).let { it.getInputStream(it.getEntry("documentinfo.xml")) }.reader(UTF_8).useLines {
         it.find { it.contains("creation-date") }
            ?.substringAfter(">")?.substringBefore("</")
            ?.takeUnless { it.isBlank() }
            ?.parseKritaDateCreated()?.toFileTime()
      }
   } catch (e: IOException) {
      logger.error(e) { "Unable to read Krita file creation date for $this" }
      null
   }

private fun String.parseKritaDateCreated(): LocalDateTime? =
   try {
      LocalDateTime.parse(this, KRITA_CREATION_DATE_PARSER)
   } catch (e: DateTimeParseException) {
      logger.error(e) { "Unable to read Krita file creation date for $this" }
      null
   }

private val KRITA_CREATION_DATE_PARSER = DateTimeFormatterBuilder()
   .parseCaseInsensitive()
   .append(ISO_LOCAL_DATE)
   .appendLiteral('T')
   .append(
      DateTimeFormatterBuilder()
         .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NORMAL)
         .appendLiteral(':')
         .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NORMAL)
         .optionalStart()
         .appendLiteral(':')
         .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NORMAL)
         .optionalStart()
         .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
         .toFormatter()
   )
   .toFormatter()