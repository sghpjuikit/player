package sp.it.util.async.future

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javafx.concurrent.Task
import org.jetbrains.annotations.Blocking
import sp.it.util.async.future.Fut.Result
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread

/**
 * Invokes [Task.run] and then [Task.get] on current thread, catching exceptions and wrapping the result in [Result].
 * Does not throw exception (thrown in the task's execution), blocks and is thread-safe.
 * Must not be called on fx thread.
 */
@ThreadSafe
@Blocking
fun <T> Task<T>.runAndGetResult(): Result<T> {

   failIfFxThread()
   run()
   return try {
      ResultOk<T>(get())
   } catch (e: InterruptedException) {
      ResultInterrupted(e)
   } catch (e: CancellationException) {
      ResultInterrupted(InterruptedException(e.message))
   } catch (e: ExecutionException) {
      ResultFail(e)
   }
}

/**
 * Invokes [Task.run] and then [Task.get] on current thread.
 * Does not throw exception (thrown in the task's execution), blocks and is thread-safe.
 * Must not be called on fx thread.
 */
@ThreadSafe
@Blocking
fun <T> Task<T>.runAndGet(): T = runAndGetResult().orThrowRaw()