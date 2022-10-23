package sp.it.util.async

import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

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

/** @return this executor limiting number of executions to at most the specified times, after which [Executor.execute] does nothing */
fun Executor.limitExecCount(max: Int): Executor {
   val exeCount = AtomicLong(0)
   return Executor {
      if (exeCount.getAndIncrement()<max)
         this@limitExecCount(it)
   }
}