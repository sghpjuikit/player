package sp.it.util.text

import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import sp.it.util.Util.StringDirection
import sp.it.util.Util.StringDirection.FROM_END
import sp.it.util.Util.StringDirection.FROM_START
import sp.it.util.action.Action
import sp.it.util.dev.failIf
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import java.text.BreakIterator
import java.util.Locale
import kotlin.streams.asSequence
import sp.it.util.functional.net

/** @return true iff this string is equal to the specified string, ignoring the case */
infix fun String.equalsNc(other: String) = this.equals(other, ignoreCase = true)

/** Length of this string in characters */
val String.lengthInChars: Int get() = length

/** Length of this string in code points */
val String.lengthInCodePoints: Int get() = codePointCount(0, length)

/** Length of this string in graphemes */
val String.lengthInGraphemes: Int get() {
   var graphemeCount = 0
   val graphemeCounter = BreakIterator.getCharacterInstance(Locale.ROOT)
   graphemeCounter.setText(this)
   while (graphemeCounter.next()!=BreakIterator.DONE) graphemeCount++
   return graphemeCount
}

/** @return [Char16] at the specified index or throws [IndexOutOfBoundsException]. */
fun String.char16At(at: Int): Char16 = get(at)

/** @return [Char32] at the specified index or throws [IndexOutOfBoundsException]. */
fun String.char32At(at: Int): Char32 = codePointAt(at).toChar32()

/** @return [Char32] at the specified index from the specified side or throws [IndexOutOfBoundsException]. */
fun String.char32At(at: Int, dir: StringDirection): Char32 = when (dir) {
   FROM_START -> codePointAt(at).toChar32()
   FROM_END -> codePointAt(lengthInCodePoints - at).toChar32()
}

/** @return [Char32] at the specified index or throws [IndexOutOfBoundsException]. */
fun String.graphemeAt(at: Int): Char32 = codePointAt(at).toChar32()  // TODO: verify this is ok

/** @return this string or null if it is null or [String.isEmpty] */
fun String?.nullIfEmpty() = this?.takeUnless { it.isEmpty() }

/** @return this string or null if it is null or [String.isBlank] */
fun String?.nullIfBlank() = this?.takeUnless { it.isBlank() }

/** @return plural of this word if count is more than 1 or this word otherwise */
fun String.plural(count: Int = 2): String = org.atteo.evo.inflector.English.plural(this, if (count==0) 2 else count)

/** @return text in format 'x units', where x is the specified count and units is [String.plural] of this string */
fun String.pluralUnit(count: Int = 2): String = "$count " + plural(count)

/** @return true iff this string is nonempty palindrome */
fun String.isPalindrome(): Boolean = !isEmpty() && isPalindromeOrEmpty()

/** @return true iff this string is palindrome or empty string */
fun String.isPalindromeOrEmpty(): Boolean {
   val l = length
   return (0 until l/2).none { this[it]!=this[l - it - 1] }
}

/** @return camel case string converted to lower dash case */
@Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
fun String.camelToDashCase() = codePoints().asSequence()
   .fold(ArrayList<Int>(length)) { str, char ->
      if (str.isEmpty()) {
         str += Character.toLowerCase(char)
      } else {
         if (Character.isUpperCase(char)) {
            str += '-'.code
            str += Character.toLowerCase(char)
         } else {
            str += char
         }
      }
      str
   }
   .joinToString("", "", "") { Character.toString(it) }

/** @return camel case string converted to lower dot case */
@Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
fun String.camelToDotCase() = codePoints().asSequence()
   .fold(ArrayList<Int>(length)) { str, char ->
      if (str.isEmpty()) {
         str += Character.toLowerCase(char)
      } else {
         if (Character.isUpperCase(char)) {
            str += '.'.code
            str += Character.toLowerCase(char)
         } else {
            str += char
         }
      }
      str
   }
   .joinToString("", "", "") { Character.toString(it) }

/** @return pretty text representing the keys, intended for UI */
fun keys(keys: String): String = keys.splitToSequence("+").map(::key).joinToString(" + ")

/** @return pretty text representing the keys, intended for UI */
fun keys(vararg keys: KeyCode): String = keys.mapNotNull { it.resolved?.net { key(it.name) } }.joinToString(" + ")

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
   ?: prettyKeys[key.trim().uppercase()]
   ?: runTry { KeyCode.valueOf(key.trim().uppercase()).resolved }.map { prettyKeys[it?.getName()?.uppercase() ?: ""] }.orNull()
   ?: runTry { KeyCode.valueOf(key.trim().uppercase()).resolved }.map { it?.char ?: "<none>" }.orNull()
   ?: key

private val prettyKeys = mapOf(
   "F1" to "F1",
   "F2" to "F2",
   "F3" to "F3",
   "F4" to "F4",
   "F5" to "F5",
   "F6" to "F6",
   "F7" to "F7",
   "F8" to "F8",
   "F9" to "F9",
   "F10" to "F10",
   "F11" to "F11",
   "F12" to "F12",
   "ESC" to "\u238B",
   "ESCAPE" to "\u238B",
   "TAB" to "\u21b9",
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
   "ENTER" to (if (Os.OSX.isCurrent) "\u21a9" else "\u21b5"),
   "EJECT" to "\u23CF",
   "POWER" to "\u233D",
   "WIN" to "\u2756",
   "WINDOWS" to "\u2756",
   "INSERT" to "\u2380",
   "PRINTSCREEN" to "\u2399",
   "SCROLL_LOCK" to "\u21f3",
   "NUM_LOCK" to "\u21ed"
)

/** @return tuple of elements split by the specified delimiter from this string or exception if less than 2 results */
fun String.split2Partial(delimiter: String, ignoreCase: Boolean = false): Pair<String, String> {
   val i = indexOf(delimiter, 0, ignoreCase)
   failIf(i == -1) { "Text must contain '$delimiter'" }
   return substring(0, i) to substring(i + delimiter.length)
}

/** @return tuple of elements split by the specified delimiter from this string or exception if not 2 results */
fun String.split2(delimiter: String, ignoreCase: Boolean = false): Pair<String, String> =
   split(delimiter, ignoreCase = ignoreCase).let {
      failIf(it.size!=2) { "Array by '$delimiter' must have 2 elements, but is $it" }
      it[0] to it[1]
   }

/** @return triple of elements split by the specified delimiter from this string or exception if not 3 results */
fun String.split3(delimiter: String, ignoreCase: Boolean = false): Triple<String, String, String> =
   split(delimiter, ignoreCase = ignoreCase).let {
      failIf(it.size!=3) { "Array by '$delimiter' must have 3 elements, but is $it" }
      Triple(it[0], it[1], it[2])
   }

/** Same as Java's [String.split] with limit -1 (which is unsupported in Kotlin), i.e., trims empty elements */
fun String.splitTrimmed(delimiter: String, ignoreCase: Boolean = false): List<String> =
   split(delimiter, ignoreCase = ignoreCase).dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }