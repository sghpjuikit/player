package sp.it.pl.util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.xmp.XmpDirectory
import mu.KotlinLogging
import sp.it.pl.util.SwitchException
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.mimetype.MimeType
import sp.it.pl.util.file.mimetype.mimeType
import sp.it.pl.util.file.nameOrRoot
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.localDateTimeFromMillis
import sp.it.pl.util.math.toDate
import sp.it.pl.util.units.FileSize
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.zip.ZipFile
import kotlin.reflect.KClass

private typealias F = JvmField

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
        @F val TIME_CREATED = FileField("Time Created", "Time Created", FileTime::class) { if (it is CachingFile) it.timeMinOfCreatedModified else it.readTimeMinOfCreatedAndModified() }
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
            else -> throw SwitchException(s)
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
    val timeCreated: Date? by lazy { readTimeCreated() }
    /** The minimum of [timeCreated] and [timeModified] or null if unable to find out. */
    val timeMinOfCreatedModified: FileTime? by lazy { readTimeMinOfCreatedAndModified() }
}

private fun File.readTimeAccessed(): FileTime? = readBasicFileAttributes()?.lastAccessTime()

private fun File.readTimeModified(): LocalDateTime? = lastModified().localDateTimeFromMillis()

private fun File.readTimeCreated(): Date? =
        when (this.mimeType().name) {
            "application/x-kra" -> {
                try {
                    ZipFile(this)
                            .let { it.getInputStream(it.getEntry("documentinfo.xml")) }
                            .reader(Charsets.UTF_8).useLines {
                                it.find { it.contains("creation-date") }
                                        ?.substringAfter(">")?.substringBefore("</")
                                        ?.let { Date.from(LocalDateTime.parse(it).toInstant(ZoneOffset.UTC)) }
                            }
                            ?: readTimeMinOfCreatedAndModified()?.toDate()
                } catch (e: IOException) {
                    readTimeMinOfCreatedAndModified()?.toDate()
                }
            }
            else -> {
                try {
                    ImageMetadataReader
                            .readMetadata(this)
                            .getFirstDirectoryOfType(XmpDirectory::class.java)
                            ?.getDate(XmpDirectory.TAG_CREATE_DATE)
                } catch (e: IOException) {
                    readTimeMinOfCreatedAndModified()?.toDate()
                } catch (e: ImageProcessingException) {
                    readTimeMinOfCreatedAndModified()?.toDate()
                }
            }
        }

private fun File.readTimeMinOfCreatedAndModified(): FileTime? =
        readBasicFileAttributes()?.run {
            val createdAt = creationTime()
            val modifiedAt = lastModified()
            if (createdAt.toMillis()<modifiedAt) createdAt else FileTime.fromMillis(modifiedAt)
        }

private fun File.readBasicFileAttributes(): BasicFileAttributes? {
    return try {
        Files.readAttributes(toPath(), BasicFileAttributes::class.java)
    } catch (e: IOException) {
        logger.error(e) { "Unable to read basic file attributes for $this" }
        null
    }
}