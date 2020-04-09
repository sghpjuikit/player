package sp.it.util.text

typealias Char16 = Char

/** 32-bit, [Int] based [Char] with full Unicode code-unit range support. See e.g. [String.codePointAt] */
class Char32(val value: Int) {
   fun toInt() = value
}

fun Int.toChar32(): Char32 = Char32(this)