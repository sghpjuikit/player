package sp.it.util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Schema
import com.drew.metadata.xmp.XmpDirectory
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
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
import org.jetbrains.annotations.Blocking
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.MimeType.Companion.`application∕x-krita`
import sp.it.util.file.type.mimeType
import sp.it.util.localDateTimeFromMillis
import sp.it.util.parsing.ConverterToString
import sp.it.util.parsing.Parsers
import sp.it.util.type.VType
import sp.it.util.type.type
import sp.it.util.units.FileSize

private val logger = KotlinLogging.logger { }

@Suppress("RemoveExplicitTypeArguments", "ClassName")
sealed class FileField<T>: ObjectFieldBase<File, T> {

   private constructor(name: String, description: String, type: VType<T>, extractor: (File) -> T): super(type, extractor, name, description, { o, or -> if (o==null) or else toSConverter.toS(o)})

   override fun cWidth(): Double = 160.0

   object PATH: FileField<String>("Path", "Path", type(), { it.path })
   object NAME: FileField<String>("Name", "Name", type(), { if (it.isDirectory) it.nameOrRoot else it.nameWithoutExtension })
   object NAME_FULL: FileField<String>("Filename", "Filename", type(), { it.nameOrRoot })
   object EXTENSION: FileField<String>("Extension", "Extension", type(), { if (it.isDirectory) "" else it.extension })
   object SIZE: FileField<FileSize>("Size", "Size", type(), { if (it is CachingFile) it.fileSize else it.readFileSize() })
   object TIME_ACCESSED: FileField<FileTime?>("Time Accessed", "Time Accessed", type(), { if (it is CachingFile) it.timeAccessed else it.readTimeAccessed() })
   object TIME_MODIFIED: FileField<LocalDateTime?>("Time Modified", "Time Modified", type(), { if (it is CachingFile) it.timeModified else it.readTimeModified() })
   object TIME_CREATED: FileField<FileTime?>("Time Created", "Time Created", type(), { if (it is CachingFile) it.timeCreated else it.readTimeCreated() })
   object TIME_CREATED_SMART: FileField<FileTime?>("Time Created (smart)", "Time Created (includes metadata inspection)", type(), { if (it is CachingFile) it.timeCreatedSmart else it.readTimeCreatedSmart() })
   object TYPE: FileField<FileType>("Type", "Type", type(), { FileType(it) })
   object MIME: FileField<MimeType>("Mime Type", "Mime Type", type(), { it.mimeType() })
   object MIME_GROUP: FileField<String>("Mime Group", "Mime Group", type(), { it.mimeType().group })
   object IS_HIDDEN: FileField<Boolean>("Is hidden", "Is hidden", type(), { if (it.isAbsolute && it.name.isEmpty()) false else it.isHidden }) // File::isHidden gives true for roots, hence the check

   companion object: ObjectFieldRegistry<File, FileField<*>>(File::class) {
      var toSConverter: ConverterToString<Any?> = ConverterToString<Any?> { o -> Parsers.DEFAULT.toS(o) }

      init { registerDeclared() }
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

   /** Time this file was created (reads metadata as well) or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeCreatedSmart: FileTime? by lazy { readTimeCreatedSmart() }

   /** The minimum of [timeCreatedSmart] and [timeModified] or null if unable to find out. Lazy. Blocks on first invocation. */
   val timeMinOfCreatedModified: FileTime? by lazy { readTimeMinOfCreatedAndModified() }

   /** The size of the file. Lazy. Blocks on first invocation. */
   val fileSize: FileSize by lazy { readFileSize() }
   private val isD = super.isDirectory()
   override fun isDirectory() = isD
   private val isF = if (isD) false else super.isFile()
   override fun isFile() = isF
   private val isH: Boolean by lazy { super.isHidden() }
   override fun isHidden() = isH
   private val isR: Boolean by lazy { super.canRead() }
   override fun canRead() = isR
   private val isW: Boolean by lazy { super.canWrite() }
   override fun canWrite() = isW
   private val isE: Boolean by lazy { super.canExecute() }
   override fun canExecute() = isE
}

@Blocking
private fun File.readFileSize(): FileSize = FileSize(this)

@Blocking
private fun File.readTimeAccessed(): FileTime? = readBasicFileAttributes()?.lastAccessTime()

@Blocking
private fun File.readTimeModified(): LocalDateTime? = lastModified().localDateTimeFromMillis()

@Blocking
private fun File.readTimeCreated(): FileTime? = readBasicFileAttributes()?.creationTime()

@Blocking
private fun File.readTimeCreatedSmart(): FileTime? {
   val m = this.mimeType()
   return when {
      m == `application∕x-krita` -> readKritaTimeCreated() ?: readTimeMinOfCreatedAndModified()
      m.group == MimeGroup.image -> readXmpTimeCreated() ?: readTimeMinOfCreatedAndModified()
      else -> readTimeMinOfCreatedAndModified()
   }
}

@Blocking
private fun File.readTimeMinOfCreatedAndModified(): FileTime? = readBasicFileAttributes()
   ?.run {
      minOf(creationTime(), lastModifiedTime())
   }

@Blocking
private fun File.readBasicFileAttributes(): BasicFileAttributes? =
   try {
      Files.readAttributes(toPath(), BasicFileAttributes::class.java)
   } catch (e: IOException) {
      logger.error(e) { "Unable to read basic file attributes for $this" }
      null
   } catch (e: InvalidPathException) {
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