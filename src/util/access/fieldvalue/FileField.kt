package util.access.fieldvalue

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.xmp.XmpDirectory
import util.SwitchException
import util.file.FileType
import util.file.mimetype.MimeType
import util.file.mimetype.mimeType
import util.file.nameOrRoot
import util.file.nameWithoutExtensionOrRoot
import util.localDateTimeFromMillis
import util.units.FileSize
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.util.Date
import kotlin.collections.HashSet
import kotlin.reflect.KClass

class FileField<T: Any>: ObjectFieldBase<File, T> {

    internal constructor(name: String, description: String, type: KClass<T>, extractor: (File) -> T?): super(type, extractor, name, description) {
        FIELDS_IMPL += this
    }

    override fun toS(o: T?, substitute: String) = o?.toString() ?: substitute

    companion object {
        private val FIELDS_IMPL = HashSet<FileField<*>>()
        @JvmField val FIELDS: Set<FileField<*>> = FIELDS_IMPL
        
        @JvmField val PATH = FileField("Path", "Path", String::class) { it.path }
        @JvmField val NAME = FileField("Name", "Name", String::class) { it.nameWithoutExtensionOrRoot } 
        @JvmField val NAME_FULL = FileField("Filename", "Filename", String::class) { it.nameOrRoot }
        @JvmField val EXTENSION = FileField("Extension", "Extension", String::class) { it.extension }
        @JvmField val SIZE = FileField("Size", "Size", FileSize::class) { FileSize(it) }
        @JvmField val TIME_MODIFIED = FileField("Time Modified", "Time Modified", LocalDateTime::class) { it.lastModified().localDateTimeFromMillis() }
        @JvmField val TIME_CREATED = FileField("Time Created", "Time Created", FileTime::class) {
            try {
                Files.readAttributes(it.toPath(), BasicFileAttributes::class.java).creationTime()
            } catch (e: IOException) {
                null
            }
        }
        @JvmField val TIME_CREATED_FROM_TAG = FileField("Time Created (tag)", "Time Created (metadata)", Date::class) {
            try {
                ImageMetadataReader
                        .readMetadata(it)
                        .getFirstDirectoryOfType(XmpDirectory::class.java)
                        .getDate(XmpDirectory.TAG_CREATE_DATE)
            } catch (e: IOException) {
                null
            } catch (e: ImageProcessingException) {
                null
            } catch (e: NullPointerException) {
                null
            }
        }
        @JvmField val TIME_ACCESSED = FileField("Time Accessed", "Time Accessed", FileTime::class) {
            try {
                Files.readAttributes(it.toPath(), BasicFileAttributes::class.java).lastAccessTime()
            } catch (e: IOException) {
                null
            }
        }
        @JvmField val TYPE = FileField("Type", "Type", FileType::class) { FileType.of(it) }
        @JvmField val MIME = FileField("Mime Type", "Mime Type", MimeType::class) { it.mimeType() }
        @JvmField val MIME_GROUP = FileField("Mime Group", "Mime Group", String::class) { it.mimeType().group }

        fun valueOf(s: String): FileField<*> = when(s) {
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