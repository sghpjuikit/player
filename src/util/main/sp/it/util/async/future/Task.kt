package sp.it.util.async.future

import javafx.concurrent.Task
import mu.KotlinLogging
import sp.it.util.async.future.Fut.Result
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread
import java.util.concurrent.ExecutionException

private val logger = KotlinLogging.logger {}

/**
 * Thread-safe and type-safe convenience method to execute this task on current thread.
 *
 * Invokes [Task.run] and then [Task.get] on current thread, catching exceptions and wrapping the result in [Result].
 * Does not throw exception (thrown in the task's execution), blocks and is thread-safe.
 * Must not be called on fx thread.
 */
@ThreadSafe
@Blocks
fun <T> Task<T>.runGet(): Result<T> {
   failIfFxThread()

   run()
   return try {
      ResultOk<T>(get())
   } catch (e: InterruptedException) {
      logger.warn(e) { "Task execution failed" }
      ResultInterrupted(e)
   } catch (e: ExecutionException) {
      logger.warn(e) { "Task execution failed" }
      ResultFail(e)
   }
}