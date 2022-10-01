package sp.it.util.action

import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCombination.NO_MATCH
import kotlin.String
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.getOr
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString

data class ActionDb(val isGlobal: Boolean, val keys: String) {

   val keysAsKeyCombination: KeyCombination
      get() = if (keys.isEmpty()) NO_MATCH else runTry { KeyCodeCombination.valueOf(keys) }.getOr(NO_MATCH)

   override fun toString() = "$isGlobal,$keys"

   companion object: ConverterString<ActionDb> {
      override fun toS(o: ActionDb) = o.toString()
      override fun ofS(s: String): Try<ActionDb, String> {
         val i = s.indexOf(",")
         if (i==-1) return Error("Must contain ','")
         val isGlobal = s.substring(0, i).toBooleanStrict()
         val keys = s.substring(i + 1)
         return Ok(ActionDb(isGlobal, keys))
      }
   }
}