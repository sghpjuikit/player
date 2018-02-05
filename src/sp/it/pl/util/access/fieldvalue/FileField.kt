package sp.it.pl.util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.xmp.XmpDirectory
import mu.KLogging
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

class FileField<T: Any>: ObjectFieldBase<File, T> {

    internal constructor(name: String, description: String, type: KClass<T>, extractor: (File) -> T?): super(type, extractor, name, description) {
        FIELDS_IMPL += this
    }

    override fun toS(o: T?, substitute: String) = o?.toString() ?: substitute

    companion object: KLogging() {
        private val FIELDS_IMPL = HashSet<FileField<*>>()
        @JvmField val FIELDS: Set<FileField<*>> = FIELDS_IMPL

        @JvmField val PATH = FileField("Path", "Path", String::class) { it.path }
        @JvmField val NAME = FileField("Name", "Name", String::class) { it.nameWithoutExtensionOrRoot }
        @JvmField val NAME_FULL = FileField("Filename", "Filename", String::class) { it.nameOrRoot }
        @JvmField val EXTENSION = FileField("Extension", "Extension", String::class) { it.extension }
        @JvmField val SIZE = FileField("Size", "Size", FileSize::class) { FileSize(it) }
        @JvmField val TIME_MODIFIED = FileField("Time Modified", "Time Modified", LocalDateTime::class) { it.lastModified().localDateTimeFromMillis() }
        @JvmField val TIME_CREATED = FileField("Time Created", "Time Created", FileTime::class) {
            it.readBasicFileAttributes()?.run {
                val createdAt = creationTime()
                val modifiedAt = it.lastModified()
                if (createdAt.toMillis()<modifiedAt) createdAt else FileTime.fromMillis(modifiedAt)
            }
        }
        @JvmField val TIME_CREATED_FROM_TAG = FileField("Time Created (tag)", "Time Created (metadata)", Date::class) {
            when (it.mimeType().name) {
                "application/x-kra" -> {
                    try {
                        ZipFile(it)
                                .let { it.getInputStream(it.getEntry("documentinfo.xml")) }
                                .reader(Charsets.UTF_8).useLines {
                                    it.find { it.contains("creation-date") }
                                            ?.substringAfter(">")?.substringBefore("</")
                                            ?.let { Date.from(LocalDateTime.parse(it).toInstant(ZoneOffset.UTC)) }
                                }
                                ?: TIME_CREATED.getOf(it)?.toDate()
                    } catch (e: IOException) {
                        TIME_CREATED.getOf(it)?.toDate()
                    }
                }
                else -> {
                    try {
                        ImageMetadataReader
                                .readMetadata(it)
                                .getFirstDirectoryOfType(XmpDirectory::class.java)
                                ?.getDate(XmpDirectory.TAG_CREATE_DATE)
                    } catch (e: IOException) {
                        TIME_CREATED.getOf(it)?.toDate()
                    } catch (e: ImageProcessingException) {
                        TIME_CREATED.getOf(it)?.toDate()
                    }
                }
            }
        }
        @JvmField val TIME_ACCESSED = FileField("Time Accessed", "Time Accessed", FileTime::class) { it.readBasicFileAttributes()?.lastAccessTime() }
        @JvmField val TYPE = FileField("Type", "Type", FileType::class) { FileType.of(it) }
        @JvmField val MIME = FileField("Mime Type", "Mime Type", MimeType::class) { it.mimeType() }
        @JvmField val MIME_GROUP = FileField("Mime Group", "Mime Group", String::class) { it.mimeType().group }

        fun valueOf(s: String): FileField<*> = when (s) {
            PATH.name() -> PATH
            NAME.name() -> NAME
            NAME_FULL.name() -> NAME_FULL
            EXTENSION.name() -> EXTENSION
            SIZE.name() -> SIZE
            TIME_MODIFIED.name() -> TIME_MODIFIED
            TIME_CREATED.name() -> TIME_CREATED
            TIME_CREATED_FROM_TAG.name() -> TIME_CREATED_FROM_TAG
            TIME_ACCESSED.name() -> TIME_ACCESSED
            TYPE.name() -> TYPE
            MIME.name() -> MIME
            MIME_GROUP.name() -> MIME_GROUP
            else -> throw SwitchException(s)
        }
    }

}

private fun File.readBasicFileAttributes(): BasicFileAttributes? {
    return try {
        Files.readAttributes(toPath(), BasicFileAttributes::class.java)
    } catch (e: IOException) {
        FileField.logger.error(e) { "Unable to read basic file attributes for $this" }
        null
    }
}