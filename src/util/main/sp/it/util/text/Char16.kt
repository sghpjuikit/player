package sp.it.util.text

/** 16-bit Unicode character, [Char]. See [Character] and [Char32]. See e.g. [String.get]. */
typealias Char16 = Char

/** Equivalent to [Int.toChar] */
fun Int.toChar16(): Char16 = toChar()