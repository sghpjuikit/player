package sp.it.util.file.type

import java.io.File
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
      MimeTypes::class.java.getResourceAsStream("mime.types")
         .reader().buffered()
         .useLines {
            it.filter { it.isNotBlank() && !it.startsWith("#") }
               .forEach {
                  val halves = it.toLowerCase().split("\\s".toRegex(), 2).toTypedArray()
                  val mime = MimeType(halves[0], *halves[1].trim().split("\\s".toRegex()).toTypedArray())
                  register(mime)
               }
         }
   }

   fun register(mimeType: MimeType) {
      types[mimeType.name] = mimeType
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

   fun ofFile(file: File): MimeType = extensions.getOrDefault(file.extension.toLowerCase(), MimeType.UNKNOWN)

   fun ofURI(uri: URI): MimeType = extensions.getOrDefault(uri.path.substringAfterLast(".", "").toLowerCase(), MimeType.UNKNOWN)

   fun setOfGroups(): Set<String> = types.values.asSequence().map { it.group }.toSet()

   fun setOfMimeTypes(): Set<MimeType> = types.values.toSet()

   fun setOfExtensions(): Set<String> = extensions.keys.toSet()

}

fun File.mimeType(): MimeType = MimeTypes.ofFile(this)