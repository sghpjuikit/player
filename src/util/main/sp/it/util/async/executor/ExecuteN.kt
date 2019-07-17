package sp.it.util.async.executor

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

/**
 * Executor with an execution count limit. Executes on current thread.
 *
 * Guarantees maximum number of executions to be at most the specified times, after which [execute] does nothing.
 */
class ExecuteN(max: Long): Executor {
   private val max = max
   private var executed = AtomicLong(0)

   override fun execute(r: Runnable) {
      executed.incrementAndGet()
      if (executed.get()<=max) r.run()
   }
}