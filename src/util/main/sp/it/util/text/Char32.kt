package sp.it.util.text

/** 32-bit Unicode character, [Int]. See [Character] and [Char16]. Has full Unicode code-unit range support. See e.g. [String.codePointAt]. */
data class Char32(val value: Int) {
   fun toInt() = value
   override fun toString() = String(IntArray(1) { value }, 0, 1)

   companion object {
      val MIN = Char32(0)
      val MAX = Char32(0x10FFFF)
   }
}

fun Int.toChar32(): Char32 = Char32(this)