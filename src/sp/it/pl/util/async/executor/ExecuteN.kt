package sp.it.pl.util.async.executor

import java.util.concurrent.Executor

/**
 * Executor with an execution count limit.
 *
 * Guarantees the number of executions (irrelevant of the [Runnable]), as
 * one may wish for this executor to execute at most n times.
 */
class ExecuteN : Executor {

    /** @param max maximum number of times this executor will [execute] */
    constructor(max: Long) { this.max = max }

    private val max: Long

    private var executed: Long = 0

    override fun execute(r: Runnable) {
        if (executed >= max)
            return
        r.run()
        executed++
    }

}