package sp.it.pl.util.async.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.concurrent.Task;
import sp.it.pl.util.dev.Util;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.throwIf;

/**
 * Task that is a function, both for the consumer and developer.
 * <p/>
 * Consumer can use the task as a simple function, supplying an input and receiving output. The difference from a normal
 * function is conceptual, this task/function is blocking and is intended to be used asynchronously, using futures,
 * such as {@link sp.it.pl.util.async.future.Fut}.
 * <p/>
 * Developer leverages simpler implementation, writing the task as function, instead of awkwardly closing over an
 * input as with standard {@link javafx.concurrent.Task}.
 *
 * @param <I> type of input
 * @param <O> type of output
 */
public abstract class FTask<I, O> extends Task<O> implements Ƒ1<I,O> {
	private I input;
	private boolean isInputAssigned = false;
	private final AtomicBoolean isInputAssignmentDoable = new AtomicBoolean(true);

	/**
	 * Sets the input for the computation. This can be only be done before computation begins, and must be done at least
	 * once. Only the last invocation will have any effect.
	 *
	 * @param input immutable or effectively final object
	 */
	public void setInput(I input) {
		throwIf(!isInputAssignmentDoable.get());
		isInputAssigned = true;
		this.input = input;
	}

	/**
	 * Equivalent to calling:
	 * <ul>
	 * <li/> {@link #setInput(Object)}, which sets the input for the computation
	 * <li/> {@link #run()}, which computes the result and blocks
	 * <li/> {@link #get()}, which returns the result or throws... (see documentation)
	 * </ul>
	 * in that order.
	 * <p/>
	 * This is a blocking operation, waiting for the computation to finish.
	 *
	 * @param input immutable or effectively final object
	 * @return the output or null if interrupted
	 * @throws java.util.concurrent.CancellationException if cancelled
	 */
	@Override
	public O apply(I input) {
		setInput(input);
		run();
		try {
			return get();
		} catch (InterruptedException|ExecutionException e) {
			Util.logger(FTask.class).error("Task execution failed", e);
			return null;    // TODO hint throwing runtime exception better ?
		}
	}

	/**
	 * Equivalent to calling:
	 * <ul>
	 * <li/> {@link #setInput(Object)}, which sets the input for the computation
	 * <li/> {@link #run()}, which computes the result and blocks
	 * </ul>
	 * in that order.
	 * <p/>
	 * This is a blocking operation, waiting for the computation to finish.
	 *
	 * @param input immutable or effectively final object
	 * @return this
	 */
	public FTask<I,O> consume(I input) {
		setInput(input);
		run();
		return this;
	}

	@Override
	protected O call() throws Exception {
		throwIf(!isInputAssigned);
		isInputAssignmentDoable.set(false);
		return compute(input);
	}

	/**
	 * @param input of this action
	 * @return result of the computation
	 */
	abstract protected O compute(I input);
}