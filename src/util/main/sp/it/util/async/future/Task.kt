package sp.it.util.async.future

import java.util.concurrent.ExecutionException
import javafx.concurrent.Task
import mu.KotlinLogging
import sp.it.util.async.future.Fut.Result
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import org.jetbrains.annotations.Blocking
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread

private val logger = KotlinLogging.logger {}

/**
 * Thread-safe and type-safe convenience method to execute this task on current thread.
 *
 * Invokes [Task.run] and then [Task.get] on current thread, catching exceptions and wrapping the result in [Result].
 * Does not throw exception (thrown in the task's execution), blocks and is thread-safe.
 * Must not be called on fx thread.
 */
@ThreadSafe
@Blocking
fun <T> Task<T>.runGet(): Result<T> {
   failIfFxThread()

   run()
   return try {
      ResultOk<T>(get())
   } catch (e: InterruptedException) {
      logger.trace(e) { "Task execution interrupted" }
      ResultInterrupted(e)
   } catch (e: ExecutionException) {
      logger.warn(e) { "Task execution failed" }
      ResultFail(e)
   }
}