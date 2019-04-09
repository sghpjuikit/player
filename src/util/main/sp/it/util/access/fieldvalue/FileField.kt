package sp.it.util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Schema
import com.drew.metadata.xmp.XmpDirectory
import mu.KotlinLogging
import sp.it.util.dev.failCase
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.localDateTimeFromMillis
import sp.it.util.units.FileSize
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.zip.ZipFile
import kotlin.reflect.KClass
import kotlin.jvm.JvmField as F

private val logger = KotlinLogging.logger { }

class FileField<T: Any>: ObjectFieldBase<File, T> {

    private constructor(name: String, description: String, type: KClass<T>, extractor: (File) -> T?): super(type, extractor, name, description) {
        FIELDS_IMPL += this
    }

    override fun toS(o: T?, substitute: String) = o?.toString() ?: substitute

    companion object {
        private val FIELDS_IMPL = HashSet<FileField<*>>()
        @F val FIELDS: Set<FileField<*>> = FIELDS_IMPL

        @F val PATH = FileField("Path", "Path", String::class) { it.path }
        @F val NAME = FileField("Name", "Name", String::class) { it.nameWithoutExtensionOrRoot }
        @F val NAME_FULL = FileField("Filename", "Filename", String::class) { it.nameOrRoot }
        @F val EXTENSION = FileField("Extension", "Extension", String::class) { it.extension }
        @F val SIZE = FileField("Size", "Size", FileSize::class) { FileSize(it) }
        @F val TIME_ACCESSED = FileField("Time Accessed", "Time Accessed", FileTime::class) { if (it is CachingFile) it.timeAccessed else it.readTimeAccessed() }
        @F val TIME_MODIFIED = FileField("Time Modified", "Time Modified", LocalDateTime::class) { if (it is CachingFile) it.timeModified else it.readTimeModified() }
        @F val TIME_CREATED = FileField("Time Created", "Time Created", FileTime::class) { if (it is CachingFile) it.timeCreated else it.readTimeCreated() }
        @F val TYPE = FileField("Type", "Type", FileType::class) { FileType.of(it) }
        @F val MIME = FileField("Mime Type", "Mime Type", MimeType::class) { it.mimeType() }
        @F val MIME_GROUP = FileField("Mime Group", "Mime Group", String::class) { it.mimeType().group }

        fun valueOf(s: String): FileField<*> = when (s) {
            PATH.name() -> PATH
            NAME.name() -> NAME
            NAME_FULL.name() -> NAME_FULL
            EXTENSION.name() -> EXTENSION
            SIZE.name() -> SIZE
            TIME_ACCESSED.name() -> TIME_ACCESSED
            TIME_MODIFIED.name() -> TIME_MODIFIED
            TIME_CREATED.name() -> TIME_CREATED
            TYPE.name() -> TYPE
            MIME.name() -> MIME
            MIME_GROUP.name() -> MIME_GROUP
            else -> failCase(s)
        }
    }

}

/** File implementation that provides additional lazy properties */
class CachingFile(f: File): File(f.path) {
    /** Time this file was last accessed or null if unable to find out */
    val timeAccessed: FileTime? by lazy { readTimeAccessed() }
    /** Time this file was last modified or null if unable to find out */
    val timeModified: LocalDateTime? by lazy { readTimeModified() }
    /** Time this file was created or null if unable to find out. */
    val timeCreated: FileTime? by lazy { readTimeCreated() }
    /** The minimum of [timeCreated] and [timeModified] or null if unable to find out. */
    val timeMinOfCreatedModified: FileTime? by lazy { readTimeMinOfCreatedAndModified() }
}

private fun File.readTimeAccessed(): FileTime? = readBasicFileAttributes()?.lastAccessTime()

private fun File.readTimeModified(): LocalDateTime? = lastModified().localDateTimeFromMillis()

private fun File.readTimeCreated(): FileTime? =
        when (this.mimeType().name) {
            "application/x-kra" -> readKritaTimeCreated() ?: readTimeMinOfCreatedAndModified()
            else -> when {
                // poor but fast way of avoiding xmp reading for directories
                name.contains(".") -> readXmpTimeCreated() ?: readTimeMinOfCreatedAndModified()
                else -> readTimeMinOfCreatedAndModified()
            }
        }

private fun File.readTimeMinOfCreatedAndModified(): FileTime? = readBasicFileAttributes()
        ?.run {
            minOf(creationTime(), lastModifiedTime())
        }

private fun File.readBasicFileAttributes(): BasicFileAttributes? =
        try {
            Files.readAttributes(toPath(), BasicFileAttributes::class.java)
        } catch (e: IOException) {
            logger.error(e) { "Unable to read basic file attributes for $this" }
            null
        }

private fun Long.toFileTime() = FileTime.fromMillis(this)

private fun LocalDateTime.toFileTime() = toInstant(ZoneOffset.UTC).toEpochMilli().toFileTime()

private fun File.readXmpTimeCreated(): FileTime? =
        try {
            ImageMetadataReader.readMetadata(this).getFirstDirectoryOfType(XmpDirectory::class.java)
                    ?.xmpMeta?.getPropertyDate(Schema.XMP_PROPERTIES, "CreateDate")
                    ?.calendar?.timeInMillis?.toFileTime()
        } catch (e: IOException) {
            logger.error(e) { "Unable to read file creation date from XMP tag for $this" }
            null
        } catch (e: ImageProcessingException) {
            // 12Monkey bug: exception type !tell us when we are dealing with an error and when an unsupported file type
            when {
                e.message=="File format is not supported" -> {}
                e.message=="File format could not be determined" -> {}
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
            ZipFile(this)
                    .let { it.getInputStream(it.getEntry("documentinfo.xml")) }
                    .reader(Charsets.UTF_8).useLines {
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