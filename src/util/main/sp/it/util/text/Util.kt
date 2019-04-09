package sp.it.util.text

import javafx.scene.input.KeyCode
import sp.it.util.action.Action

/** @return plural of this word if count is more than 1 or this word otherwise */
fun String.plural(count: Int = 2) = org.atteo.evo.inflector.English.plural(this, count)!!

/** @return text in format 'x units', where x is the specified count amd units [String.plural] of this string */
fun String.pluralUnit(count: Int = 2) = "$count " + plural(count)

/** @return true iff this string is nonempty palindrome */
fun String.isPalindrome(): Boolean = !isEmpty() && isPalindromeOrEmpty()

/** @return true iff this string is palindrome or empty string */
fun String.isPalindromeOrEmpty(): Boolean {
    val l = length
    return (0 until l/2).none { this[it]!=this[l-it-1] }
}

/** @return pretty text representing the keys, intended for UI */
fun keys(keys: String) = keys.splitToSequence("+").map(::key).joinToString(" + ")

/** @return pretty text representing the key, intended for UI */
fun KeyCode.getNamePretty() = key(getName())

/** @return pretty text representing the keys, intended for UI */
fun Action.getKeysPretty() = keys.let { if (it.isBlank()) it else keys(keys) }!!

private fun key(key: String) = prettyKeys.getOrDefault(key.trim().toUpperCase(), key)

private val prettyKeys = mapOf(
        "ESCAPE" to "\u238B",
        "TAB" to "\u21E5",
        "CAPS LOCK" to "\u21EA",
        "SHIFT" to "\u21E7",
        "CONTROL" to "\u2303",
        "CTRL" to "\u2303",
        "OPTION" to "\u2325",
        "ALT" to "\u2325",
        "ALT_GRAPH" to "\u2325",
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