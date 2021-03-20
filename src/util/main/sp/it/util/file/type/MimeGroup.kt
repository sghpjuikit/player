package sp.it.util.file.type

import sp.it.util.conf.UnsealedEnumerator
import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

data class MimeGroup(val name: String) {

   companion object: ConverterString<MimeGroup>, UnsealedEnumerator<MimeGroup> {
      const val audio = "audio"
      const val image = "image"
      const val video = "video"

      override fun toS(o: MimeGroup): String = o.name
      override fun ofS(s: String) = Try.ok(MimeGroup(s))
      override fun enumerateUnsealed() = MimeTypes.setOfGroups().map { MimeGroup(it) }
   }

}