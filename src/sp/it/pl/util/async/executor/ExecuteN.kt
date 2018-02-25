package sp.it.pl.util.async.executor

import java.util.concurrent.Executor

/**
 * Executor with an execution count limit.
 *
 * Guarantees the number of executions (irrelevant of the Runnable), as
 * one may wish for this executor to execute at most n times.
 */
class ExecuteN
/** @param limit maximum number of times this executors will [execute] */
(private val max: Long) : Executor {

    private var executed: Long = 0

    override fun execute(r: Runnable) {
        if (executed < max)
            r.run()
        executed++
    }

}