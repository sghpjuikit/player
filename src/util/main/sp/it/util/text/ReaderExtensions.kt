package sp.it.util.text

import java.io.IOException
import java.io.InputStream
import java.io.Reader
import sp.it.util.functional.net

/** @return one codepoint read from this reader (consuming its bytes) as [Int] or throw [IOException] */
@Throws(IOException::class)
fun InputStream.readCodepoint(): Int? {
   val first = read()
   if (first == -1) return null
   if (!Character.isHighSurrogate(first.toChar())) return first

   val second = read()
   if (second == -1) throw IOException("Low surrogate expected after $first")
   if (!Character.isLowSurrogate(second.toChar())) throw IOException("Invalid surrogate pair ($first, $second)")

   return Character.toCodePoint(first.toChar(), second.toChar())
}

/** @return one codepoint read from this reader (consuming its bytes) as [Char32] or throw [IOException] */
@Throws(IOException::class)
fun InputStream.readChar32(): Char32? =
   readCodepoint()?.let(::Char32)

/** @return (at most) specified number of codepoints read from this reader (consuming their bytes) as [String] or throw [IOException] */
@Throws(IOException::class)
fun InputStream.readCodePoints(n: Int): String =
   buildString {
      repeat(n) {
         val codePoint = readCodepoint()
         if (codePoint==null) return@repeat // End of stream
         appendCodePoint(codePoint)
      }
   }