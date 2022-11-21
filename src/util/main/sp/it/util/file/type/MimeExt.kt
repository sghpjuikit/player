package sp.it.util.file.type

import sp.it.util.conf.UnsealedEnumerator
import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

data class MimeExt(val name: String) {

   companion object: ConverterString<MimeExt>, UnsealedEnumerator<MimeExt> {
      const val exe = "exe"
      const val lnk = "lnk"
      const val md = "md"
      const val txt = "txt"

      private val enumerateUnsealed by lazy { MimeType.enumerateUnsealed().flatMap { it.extensions.asSequence().distinct().map { MimeExt(it) } } }
      override fun toS(o: MimeExt): String = o.name
      override fun ofS(s: String) = Try.ok(MimeExt(s))
      override fun enumerateUnsealed(): Collection<MimeExt> = enumerateUnsealed
   }

}