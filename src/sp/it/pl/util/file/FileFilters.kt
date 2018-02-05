package sp.it.pl.util.file

import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.file.Util.getSuffix
import sp.it.pl.util.file.mimetype.MimeTypes
import sp.it.pl.util.file.mimetype.mimeType
import sp.it.pl.util.functional.Functors
import java.io.File
import java.util.function.Consumer

/**
 * Pool of file filters intended for simple enum-like file filter selection in UI.
 *
 * Because we can not yet serialize functions (see [sp.it.pl.util.functional.Functors] and
 * [sp.it.pl.util.parsing.Converter]), it is useful to define predicates not from function pool,
 * but hardcoded filters, which are enumerable and we look up by name.
 */
object FileFilters {
    val filterPrimary = Functors.PƑ0("File - all", File::class.java, Boolean::class.java) { true }
    private val filters = ArrayList<Functors.PƑ0<File, Boolean>>()

    init {
        filters += filterPrimary
        filters += Functors.PƑ0("File - is audio", File::class.java, Boolean::class.java) { AudioFileFormat.isSupported(it, AudioFileFormat.Use.PLAYBACK) }
        filters += Functors.PƑ0("File - is image", File::class.java, Boolean::class.java) { it.isImageDisplayable() }
        filters += Functors.PƑ0("File type - file", File::class.java, Boolean::class.java) { it.isFile }
        filters += Functors.PƑ0("File type - directory", File::class.java, Boolean::class.java) { it.isDirectory }
        MimeTypes.setOfGroups().forEach { group ->
            filters += Functors.PƑ0("Mime type group - is ${group.capitalize()}", File::class.java, Boolean::class.java) { group==it.mimeType().group }
        }
        MimeTypes.setOfMimeTypes().forEach { mime ->
            filters += Functors.PƑ0("Mime type - is ${mime.name}", File::class.java, Boolean::class.java) { it.mimeType()==mime }
        }
        MimeTypes.setOfExtensions().forEach { extension ->
            filters += Functors.PƑ0("Type - is $extension", File::class.java, Boolean::class.java) { getSuffix(it).equals(extension, ignoreCase = true) }
        }
    }

    /** @return filter with specified name or primary filter if no such filter */
    @JvmStatic fun getOrPrimary(name: String) = filters.find { it.name==name } ?: filterPrimary

    /** @return enumerable string value enumerating all available predicate names */
    @JvmStatic @JvmOverloads fun toEnumerableValue(applier: Consumer<in String> = Consumer {}) = FileFilterValue(filters.map { it.name }, applier)
}

class FileFilterValue(enumerated: Collection<String>, applier: Consumer<in String> = Consumer {}): VarEnum<String>("File - all", enumerated, applier) {

    private var filter = FileFilters.filterPrimary

    override fun setValue(v: String) {
        filter = FileFilters.getOrPrimary(v)
        super.setValue(v)
    }

    /** @return the filter represented by the current value, which is the name of the returned filter */
    fun getValueAsFilter() = filter
}

/** @return true iff the file can be represented as an image, e.g image, shortcut, or file with embedded image, etc. */
fun File.isImageDisplayable(): Boolean {
    val mime = mimeType()
    return when {
        mime.group == "image" -> true
        mime.name == "application/x-kra" -> true
        mime.name == "application/x-msdownload" -> true
        mime.name == "application/x-ms-shortcut" -> true
        else -> false
    }
}