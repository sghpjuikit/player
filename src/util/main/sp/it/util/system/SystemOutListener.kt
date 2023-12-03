package sp.it.util.system

import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.CopyOnWriteArrayList
import sp.it.util.async.runFX
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.addRem

/**
 * Stream that self-inserts as [System.out], but instead of redirecting it, it continues
 * to provide to it, functioning effectively as a listener. Designed as a distributor to
 * end-listeners that actually process the data. These can be added or removed anytime and in
 * any count easily and without interfering with each other or the original stream. In addition,
 * execute on fx thread.
 *
 * It is not recommended creating multiple instances, instead observe this stream with multiple listeners.
 */
class SystemOutListener private constructor(private val stream: SystemOutDuplicateStream): PrintStream(stream) {
   constructor(): this(SystemOutDuplicateStream()) {
      System.setOut(this)
   }

   /** Add listener that will receive the stream data (always on fx thread). */
   fun addListener(listener: (String) -> Unit): Subscription =
      stream.listeners addRem listener

   fun removeListener(listener: (String) -> Unit) {
      stream.listeners -= listener
   }

   /** Helper class for [SystemOutListener]. */
   private class SystemOutDuplicateStream: OutputStream() {
      val sout = System.out!!
      val listeners = CopyOnWriteArrayList<(String) -> Unit>()

      override fun write(b: Int) {
         // Less efficient
         // sout.write(b);
         // if (!listeners.isEmpty())
         //     runFX(() -> listeners.forEach(l -> l.accept(b)));
         throw AssertionError("Operation not allowed for performance reasons.")
      }

      override fun write(b: ByteArray, off: Int, len: Int) {
         // copied from super.write(...) implementation
         if (off<0 || off>b.size || len<0 || off + len>b.size || off + len<0) {
            throw IndexOutOfBoundsException()
         } else if (len==0) {
            return
         }

         // for (int i=0 ; i<len ; i++) write(b[off + i]);
         sout.write(b, off, len)
         if (listeners.isNotEmpty()) {
            val s = String(b, off, len, UTF_8)
            runFX { listeners.forEach { it(s) } }
         }
      }
   }
}