package sp.it.util.text

typealias Char16 = Char

/** 32-bit, [Int] based [Char] with full Unicode code-unit range support. See e.g. [String.codePointAt] */
data class Char32(val value: Int) {
   fun toInt() = value
   override fun toString() = String(IntArray(1) { value }, 0, 1)

   companion object {
      val MIN = Char32(0)
      val MAX = Char32(1114111)
   }
}

fun Int.toChar32(): Char32 = Char32(this)