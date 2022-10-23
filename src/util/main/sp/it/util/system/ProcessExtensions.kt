package sp.it.util.system

import java.io.InputStream
import java.util.concurrent.TimeUnit
import sp.it.util.async.runVT
import sp.it.util.dev.Blocks

/** [Process.waitFor] which consumes STDIN and STDOUT on [runVT] into [String] results. Blocks. */
@Blocks
fun Process.waitForResult(timeout: Long, unit: TimeUnit, result: (InputStream) -> String): Pair<String, String> {
   val stdout = runVT { inputStream?.let { result(it.buffered()) }.orEmpty() }
   val stderr = runVT { errorStream?.let { result(it.buffered()) }.orEmpty() }
   waitFor(timeout, unit)
   return stdout.blockAndGetOrThrow() to stderr.blockAndGetOrThrow()
}