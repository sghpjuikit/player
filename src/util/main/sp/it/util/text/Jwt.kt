package sp.it.util.text

import java.util.Locale
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsonAst
import sp.it.util.file.json.toCompactS
import sp.it.util.file.json.toPrettyS
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString
import sp.it.util.parsing.ConverterToUiString

data class Jwt(val header: JsObject, val payload: JsObject, val signature: String) {
   companion object: ConverterString<Jwt>, ConverterToUiString<Jwt> {

      override fun toUiS(o: Jwt, locale: Locale): String = o.header.toPrettyS() + "\n.\n" + o.payload.toPrettyS() + "\n.\n" + o.signature

      override fun toS(o: Jwt): String = o.header.toCompactS().encodeBase64() + "." + o.payload.toCompactS().encodeBase64() + "." + o.signature

      override fun ofS(s: String): Try<Jwt, String> {
         return runTry {
            val json = JsonAst()
            val (header, payload, signature) = s.split3(".")
            Jwt(json.ast(header.decodeBase64().orThrow).orThrow.asIs(), json.ast(payload.decodeBase64().orThrow).orThrow.asIs(), signature)
         }.mapError { it.message ?: "Unknown error" }
      }

   }
}