package util.file.mimetype

import util.dev.log
import util.file.Util.getSuffix
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility registry of mime types, with lookups by mime type and by file
 * extensions.
 *
 * @author https://github.com/amr/mimetypes
 */
object MimeTypes {

    private val types = ConcurrentHashMap<String, MimeType>()
    private val extensions = ConcurrentHashMap<String, MimeType>()

    init {
        try {
            MimeTypes::class.java.getResourceAsStream("mime.types").use { file ->
                InputStreamReader(file, "UTF-8").use { ir ->
                    BufferedReader(ir).lineSequence()
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .forEach {
                                val halves = it.toLowerCase().split("\\s".toRegex(), 2).toTypedArray()
                                val mime = MimeType(halves[0], *halves[1].trim().split("\\s".toRegex()).toTypedArray())
                                register(mime)
                            }
                }
            }
        } catch (e: Exception) {
            log().error("Failed to load default mime types", e)
        }
    }

    fun register(mimeType: MimeType) {
        types.put(mimeType.name, mimeType)
        mimeType.extensions.forEach { extensions[it] = mimeType }
    }

    /**
     * Get a @{link MimeType} instance for the given mime type identifier from
     * the loaded mime type definitions.
     *
     * @param type lower-case mime type identifier string
     * @return Instance of MimeType for the given mime type identifier or null if none was found
     */
    fun ofType(type: String): MimeType = types.getOrDefault(type, MimeType.UNKNOWN)

    /**
     * Get a @{link MimeType} instance for the given extension from the loaded
     * mime type definitions.
     *
     * @param extension lower-case extension
     * @return Instance of MimeType for the given ext or null if none was found
     */
    fun ofExtension(extension: String): MimeType = extensions.getOrDefault(extension.toLowerCase(), MimeType.UNKNOWN)

    fun ofFile(file: File): MimeType = extensions.getOrDefault(getSuffix(file).toLowerCase(), MimeType.UNKNOWN)

    fun ofURI(url: URI): MimeType = extensions.getOrDefault(getSuffix(url).toLowerCase(), MimeType.UNKNOWN)

    fun setOfGroups(): Set<String> = types.values.asSequence().map { it.group }.toSet()

    fun setOfMimeTypes(): Set<MimeType> = types.values.toSet()

    fun setOfExtensions(): Set<String> = extensions.keys.toSet()

}

fun File.mimeType(): MimeType = MimeTypes.ofFile(this)