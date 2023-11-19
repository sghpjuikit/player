package sp.it.util.text

import java.io.InputStream
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8
import sp.it.util.dev.Experimental

interface StringSeq {
   val seq: Sequence<String>

   fun anyContains(text: String, ignoreCase: Boolean = false): Boolean =
      seq.any { it.contains(text, ignoreCase) }

   fun isEmpty() = size()==0

   fun size() = seq.count()

}

fun Sequence<String>.toStringSeq() = object: StringSeq {
   override val seq get() = this@toStringSeq
}

/** See [InputStream.strings] */
fun <T> InputStream.useStrings(bufferSize: Int, charset: Charset = UTF_8, block: (Sequence<String>) -> T): T =
   reader(charset).use {
      block(
         sequence {
            val buffer = CharArray(bufferSize)
            var csRead: Int
            while (it.read(buffer).also { csRead = it }!=-1)
               yield(String(buffer, 0, csRead))
         }
      )
   }

/**
 * Similar to [java.io.Reader.useLines], but instead of lines the elements as written.
 *
 * @param bufferSize maximum size of element (longer element will be split)
 *        Using big value (like Int.MAX will cause memory issues.
 *        Using small value (like 1) may cause codepoints not fit in and corrupt the output.
 *        It is intended for this value to be fairly big (in size of char length of expected maximum element)
 */
@Experimental("Although tested, the actual behavior depends on the underlying streams used")
fun InputStream.strings(bufferSize: Int, charset: Charset = UTF_8): List<String> =
   useStrings(bufferSize, charset) { it.toList() }

/** Maps this sequence to seuence of lines, splitting or concatenating subsequent elements as needed. */
fun Sequence<String>.lines() =
   sequence {
      var l = ""
      for (ss in this@lines) {
         var s = ss
         while ('\n' in s) {
            l += s.substringBefore('\n')
            yield(l)
            l = ""
            s = s.substringAfter('\n')
         }
         l += s
      }
      yield(l)
   }

