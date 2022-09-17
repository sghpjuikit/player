package sp.it.util.action

import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyCombination.NO_MATCH
import kotlin.String
import sp.it.util.functional.getOr
import sp.it.util.functional.runTry

data class ActionDb(val isGlobal: Boolean, val keys: String) {

   val keysAsKeyCombination: KeyCombination
      get() = if (keys.isEmpty()) NO_MATCH else runTry { KeyCodeCombination.valueOf(keys) }.getOr(NO_MATCH)

   override fun toString() = "$isGlobal,$keys"

   companion object {
      fun fromString(str: String): ActionDb? {
         val i = str.indexOf(",")
         if (i==-1) return null
         val isGlobal = str.substring(0, i).toBooleanStrict()
         val keys = str.substring(i + 1)
         return ActionDb(isGlobal, keys)
      }
   }
}