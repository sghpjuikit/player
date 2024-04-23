package sp.it.util.text

import sp.it.util.dev.failIf

/** 32-bit Unicode character, [Int]. See [Character] and [Char16]. Has full Unicode code-unit range support. See e.g. [String.char32At]. */
data class Char32(val value: Int) {

   init {
      failIf(value !in 0..0x10FFFF) { "Invalid Char32 value: $value" }
   }

   /** @return [Int] codepoint value */
   fun toInt() = value

   /** @return [toChar16] */
   fun toChar(): Char? = toChar()

   /** @return [Char16] if this codepoint is in [Char16] range or null */
   fun toChar16(): Char16? =
      if (value in Char.MIN_VALUE.code..Char.MAX_VALUE.code) value.toChar()
      else null

   override fun toString() = String(IntArray(1) { value }, 0, 1)

   companion object {
      val MIN = Char32(0)
      val MAX = Char32(0x10FFFF)
   }
}

fun Char16.toChar32(): Char32 = code.toChar32()

fun Int.toChar32(): Char32 = Char32(this)

fun Char32.toPrintableNonWhitespace() = when {
   Character.isSpaceChar(value) -> '·'.toChar32()
   // U+0000—U+001F (C0)
   value in 0..32 -> Char32(2*16*16 + 4*16 + value) // \u2400..\u241F
   // U+0008 (BACKSPACE)
   value == 8 -> Char32(8592) // Backspace Unicode icon
   // U+007F (DELETE)
   value==127 -> Char32(2421)
   // U+0080—U+009F (C1 controls).
   value in 128..159 -> "·".char32At(0)
   // whitespace
   Character.isWhitespace(value) -> "·".char32At(0)
   // printable
   else -> value.toChar32() // TODO: handle unicode non-printable characters like \u0FEFF
}