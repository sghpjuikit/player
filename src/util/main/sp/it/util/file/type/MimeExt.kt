package sp.it.util.file.type

import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterString

data class MimeExt(val name: String) {

   companion object: ConverterString<MimeExt> {
      const val exe = "exe"
      const val lnk = "lnk"

      override fun toS(o: MimeExt): String = o.name
      override fun ofS(s: String) = Try.ok(MimeExt(s))
   }

}