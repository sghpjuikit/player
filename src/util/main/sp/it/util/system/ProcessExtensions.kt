package sp.it.util.system

import java.io.InputStream
import java.util.concurrent.TimeUnit
import sp.it.util.async.runIO
import sp.it.util.dev.Blocks

/** [Process.waitFor] which consumes STDIN and STDOUT on [runIO] into [String] results. Blocks. */
@Blocks
fun Process.waitForResult(timeout: Long, unit: TimeUnit, result: (InputStream) -> String): Pair<String, String> {
   fun String?.wrap() = if (isNullOrBlank()) "" else "\n$this"
   var textStdout = ""
   var textStdErr = ""
   val stdoutListener = runIO { textStdout = result(inputStream).wrap() }
   val stderrListener = runIO { textStdErr = result(errorStream).wrap() }
   waitFor(timeout, unit)
   stdoutListener.block()
   stderrListener.block()
   return textStdout to textStdErr
}