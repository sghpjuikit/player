package sp.it.util.text

import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import sp.it.util.action.Action
import sp.it.util.dev.failIf
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.system.Os

/** @return this string or null if it is null or [String.isEmpty] */
fun String?.nullIfEmpty() = this?.takeUnless { it.isEmpty() }

/** @return this string or null if it is null or [String.isBlank] */
fun String?.nullIfBlank() = this?.takeUnless { it.isBlank() }

/** @return plural of this word if count is more than 1 or this word otherwise */
fun String.plural(count: Int = 2) = org.atteo.evo.inflector.English.plural(this, count)

/** @return text in format 'x units', where x is the specified count amd units [String.plural] of this string */
fun String.pluralUnit(count: Int = 2) = "$count " + plural(count)

/** @return true iff this string is nonempty palindrome */
fun String.isPalindrome(): Boolean = !isEmpty() && isPalindromeOrEmpty()

/** @return true iff this string is palindrome or empty string */
fun String.isPalindromeOrEmpty(): Boolean {
   val l = length
   return (0 until l/2).none { this[it]!=this[l - it - 1] }
}

/** @return pretty text representing the keys, intended for UI */
fun keys(keys: String): String = keys.splitToSequence("+").map(::key).joinToString(" + ")

/** @return pretty text representing the key, intended for UI */
val KeyCode.nameUi: String
   get() = key(resolved?.getName() ?: "<none>")

/** @return key representing this key on the current OS */
val KeyCode.resolved: KeyCode?
   get() = when(this) {
      KeyCode.META -> when (Os.current) {
         Os.OSX -> KeyCode.ALT
         else -> KeyCode.WINDOWS
      }
      KeyCode.SHORTCUT -> when (Os.current) {
         Os.OSX -> KeyCode.COMMAND
         else -> KeyCode.WINDOWS
      }
      KeyCode.UNDEFINED -> null
      else -> this
   }

/** @return pretty text representing the button, intended for UI */
val MouseButton.nameUi: String
   get() = when(this) {
      MouseButton.PRIMARY -> "LMB"
      MouseButton.MIDDLE -> "MMB"
      MouseButton.SECONDARY -> "RMB"
      MouseButton.FORWARD -> "FMB"
      MouseButton.BACK -> "BMB"
      MouseButton.NONE -> "NoMB"
   }

/** @return pretty text representing the keys, intended for UI */
fun Action.keysUi(): String = keys.let { if (it.isBlank()) it else keys(keys) }

private fun key(key: String): String = null
   ?: prettyKeys[key.trim().toUpperCase()]
   ?: runTry { KeyCode.valueOf(key.trim().toUpperCase()).char }.orNull()
   ?: key

private val prettyKeys = mapOf(
   "ESCAPE" to "\u238B",
   "TAB" to "\u21E5",
   "CAPS LOCK" to "\u21EA",
   "SHIFT" to "\u21E7",
   "CONTROL" to "\u2303",
   "CTRL" to "\u2303",
   "OPTION" to "\u2325",
   "ALT" to "\u2325",
   "ALT GRAPH" to "\u2325",
   "APPLE" to "\uF8FF ",
   "COMMAND" to "\u2318",
   "SPACE" to "\u2423",
   "RETURN" to "\u23CE",
   "BACKSPACE" to "\u232B",
   "DELETE" to "\u2326",
   "HOME" to "\u21F1",
   "END" to "\u21F2",
   "PAGE UP" to "\u21DE",
   "PAGE DOWN" to "\u21DF",
   "UP" to "\u2191",
   "UP ARROW" to "\u2191",
   "DOWN" to "\u2193",
   "DOWN ARROW" to "\u2193",
   "LEFT" to "\u2190",
   "LEFT ARROW" to "\u2190",
   "RIGHT" to "\u2192",
   "RIGHT ARROW" to "\u2192",
   "CLEAR" to "\u2327",
   "NUM LOCK" to "\u21ED",
   "ENTER" to "\u2324",
   "EJECT" to "\u23CF",
   "POWER" to "\u233D"
)

/** @return tuple of elements split by the specified delimiter from this string or exception if not 2 results */
fun String.split2(delimiter: String, ignoreCase: Boolean = false): Pair<String, String> =
   split(delimiter, ignoreCase = ignoreCase).let {
      failIf(it.size!=2) { "Array by $delimiter must have 2 elements, but is $it" }
      it[0] to it[1]
   }

/** @return triple of elements split by the specified delimiter from this string or exception if not 3 results */
fun String.split3(delimiter: String, ignoreCase: Boolean = false): Triple<String, String, String> =
   split(delimiter, ignoreCase = ignoreCase).let {
      failIf(it.size!=3) { "Array by $delimiter must have 3 elements, but is $it" }
      Triple(it[0], it[1], it[2])
   }

/** Same as Java's [String.split] with limit -1 (which is unsupported in Kotlin), i.e., trims empty elements */
fun String.splitTrimmed(delimiter: String, ignoreCase: Boolean = false): List<String> =
   split(delimiter, ignoreCase = ignoreCase).dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }