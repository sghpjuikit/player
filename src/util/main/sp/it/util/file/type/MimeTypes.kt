package sp.it.util.file.type

import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import sp.it.util.dev.failIf

/**
 * A utility registry of mime types, with look-ups by mime type and by file extensions.
 * Inspired by: https://github.com/amr/mimetypes
 */
object MimeTypes {

   private val types = ConcurrentHashMap<String, MimeType>()
   private val extensions = ConcurrentHashMap<String, MimeType>()

   fun register(mimeType: MimeType) {
      failIf(types[mimeType.name]!=null) { "Mime $mimeType already registered" }
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
   fun ofType(type: String): MimeType = types.getOrDefault(type, MimeType.unknown)

   /**
    * Get a @{link MimeType} instance for the given extension from the loaded
    * mime type definitions.
    *
    * @param extension lower-case extension
    * @return Instance of MimeType for the given ext or null if none was found
    */
   fun ofExtension(extension: String): MimeType = extensions.getOrDefault(extension.toLowerCase(), MimeType.unknown)

   fun ofFile(file: File): MimeType = extensions.getOrDefault(file.extension.toLowerCase(), MimeType.unknown)

   fun ofURI(uri: URI): MimeType = extensions.getOrDefault(uri.path.substringAfterLast(".", "").toLowerCase(), MimeType.unknown)

   fun setOfGroups(): Set<String> = types.values.asSequence().map { it.group }.toSet()

   fun setOfMimeTypes(): Set<MimeType> = types.values.toSet()

   fun setOfExtensions(): Set<String> = extensions.keys.toSet()

}

/** Equivalent to [MimeTypes.ofFile] */
fun File.mimeType(): MimeType = MimeTypes.ofFile(this)