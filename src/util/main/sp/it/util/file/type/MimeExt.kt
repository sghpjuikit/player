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
      const val url = "url"

      private val enumerateUnsealed by lazy { enumerateUnsealed { true }.toList() }
      override fun toS(o: MimeExt): String = o.name
      override fun ofS(s: String) = Try.ok(MimeExt(s))
      override fun enumerateUnsealed(): Collection<MimeExt> = enumerateUnsealed
               fun enumerateUnsealed(predicate: (MimeType) -> Boolean): Sequence<MimeExt> =
                  MimeType.enumerateUnsealed().asSequence()
                     .filter(predicate)
                     .flatMap { it.extensions.asSequence() }
                     .distinct()
                     .map { MimeExt(it) }
   }

}