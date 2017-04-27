package util.async.executor;

import java.util.concurrent.Executor;

/**
 * Executor with an execution count limit.
 * <p/>
 * Guarantees the number of executions (irrelevant of the Runnable), as
 * one may wish for this executor to execute at most most n times.
 */
public class ExecuteN implements Executor {

	private long executed = 0;
	private final long max;

	/**
	 * @param limit maximum number of times this executor can execute any runnable
	 */
	public ExecuteN(long limit) {
		max = limit;
	}

	@Override
	public void execute(Runnable r) {
		if (executed<max) r.run();
		executed++;
	}

}