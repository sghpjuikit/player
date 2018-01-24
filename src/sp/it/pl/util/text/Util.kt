package sp.it.pl.util.text

/** @return plural of this word if count is more than 1 or this word otherwise */
fun String.plural(count: Int = 2) = org.atteo.evo.inflector.English.plural(this, count)!!

/** @return true iff this string is nonempty palindrome */
fun String.isPalindrome(): Boolean = !isEmpty() && isPalindromeOrEmpty()

/** @return true iff this string is palindrome or empty string */
fun String.isPalindromeOrEmpty(): Boolean {
    val l = length
    return (0 until l/2).none { this[it]!=this[l-it-1] }
}