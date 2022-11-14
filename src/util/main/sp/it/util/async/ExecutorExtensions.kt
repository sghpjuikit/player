package sp.it.util.async

import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong
import javafx.util.Duration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import sp.it.util.async.coroutine.runSuspending
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe

/** @return this executor limiting number of parallel executions to at most the specified times, after which new executions wait for [Semaphore.acquire] */
fun Executor.limitParallelism(permits: Int): Executor {
   val s = Semaphore(permits)
   return Executor {
      this@limitParallelism {
         s.acquire()
         it.run()
         s.release()
      }
   }
}

/** @return this executor limiting executions such when the specified lock is active, new executions will wait for [SemaphoreLock.acquireAndRelease] */
fun Executor.limitAccess(lock: SemaphoreLock): Executor {
   return Executor {
      this@limitAccess {
         lock.acquireAndRelease()
         it.run()
      }
   }
}
/** @return this executor limiting number of executions to at most the specified times, after which [Executor.execute] does nothing */
fun Executor.limitExecCount(max: Int): Executor {
   val exeCount = AtomicLong(0)
   return Executor {
      if (exeCount.getAndIncrement()<max)
         this@limitExecCount(it)
   }
}

/** Lock that can be locked by multiple concurrent sources for certain durations Thread-safe. */
class SemaphoreLock {
   private val semaphore: Semaphore = Semaphore(1)
   private val parallelismBlocked = AtomicLong(0)

   @ThreadSafe
   @Blocks
   fun acquireAndRelease() {
      semaphore.acquire()
      semaphore.release()
   }

   @ThreadSafe
   fun lockWhile(block: () -> Unit) {
      parallelismSlow()
      try {
         block()
      } finally {
         parallelismFast()
      }
   }

   @ThreadSafe
   fun lockFor(duration: Duration) {
      runSuspending(Dispatchers.IO, CoroutineStart.UNDISPATCHED) {
         parallelismSlow()
         delay(duration.toMillis().toLong())
         parallelismFast()
      }
   }

   @ThreadSafe
   private fun parallelismSlow() {
      if (parallelismBlocked.getAndIncrement()==0L) semaphore.acquire()
   }

   @ThreadSafe
   private fun parallelismFast() {
      if (parallelismBlocked.decrementAndGet()==0L) semaphore.release()
   }
}