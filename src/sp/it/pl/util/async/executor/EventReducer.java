package sp.it.pl.util.async.executor;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.util.functional.Functors.Ƒ2;
import static javafx.util.Duration.millis;
import static sp.it.pl.util.async.executor.FxTimer.fxTimer;
import static sp.it.pl.util.functional.UtilKt.runnable;

/**
 * Event frequency reducer. Consumes events and reduces close temporal successions into (exactly)
 * single event.
 * <p/>
 * The reducing can work in two ways:
 * <ul>
 * <li>Firing the first event will
 * be instantaneous (as soon as it arrives) and will in effect ignore all future events of the
 * succession.
 * <p/>
 * <li>Firing the final event will cause all previous events to be accumulated into one event using
 * a reduction function (by default it simply ignores the events until the last one). It is then
 * fired, when the succession ends. Note the delay between last consumed event of the succession and
 * the succession ending. It only ends when the timer runs out and future events will start a new
 * succession. Even if the succession has only 1 event, there will still be delay between consuming
 * it and firing it as a reduced event.
 */
public abstract class EventReducer<E> {
	protected Consumer<E> action;
	protected double inter_period;
	protected final Ƒ2<E,E,E> r;
	protected E e;

	private EventReducer(double inter_period, Ƒ2<E,E,E> reduction, Consumer<E> handler) {
		this.inter_period = inter_period;
		action = handler;
		r = reduction;
	}

	public void push(E event) {
		e = r==null || e==null ? event : r.apply(e, event);
		handle();
	}

	protected abstract void handle();

	@NotNull public static <E> EventReducer<E> toFirst(double inter_period, Consumer<E> handler) {
		return new HandlerFirst<>(inter_period, handler);
	}

	@NotNull public static <E> EventReducer<E> toFirst(double inter_period, Runnable handler) {
		return new HandlerFirst<>(inter_period, e -> handler.run());
	}

	@NotNull public static <E> EventReducer<E> toFirstDelayed(double inter_period, Consumer<E> handler) {
		return new HandlerFirstDelayed<>(inter_period, handler);
	}

	@NotNull public static <E> EventReducer<E> toFirstDelayed(double inter_period, Runnable handler) {
		return new HandlerFirstDelayed<>(inter_period, e -> handler.run());
	}

	@NotNull public static <E> HandlerLast<E> toLast(double inter_period, Consumer<E> handler) {
		return new HandlerLast<>(inter_period, null, handler);
	}

	@NotNull public static <E> HandlerLast<E> toLast(double inter_period, Runnable handler) {
		return new HandlerLast<>(inter_period, null, e -> handler.run());
	}

	@NotNull public static <E> HandlerLast<E> toLast(double inter_period, Ƒ2<E,E,E> reduction, Consumer<E> handler) {
		return new HandlerLast<>(inter_period, reduction, handler);
	}

	@NotNull public static <E> HandlerLast<E> toLast(double inter_period, Ƒ2<E,E,E> reduction, Runnable handler) {
		return new HandlerLast<>(inter_period, reduction, e -> handler.run());
	}

	@NotNull public static <E> EventReducer<E> toEvery(double inter_period, Consumer<E> handler) {
		return new HandlerEvery<>(inter_period, (a, b) -> b, handler);
	}

	@NotNull public static <E> EventReducer<E> toEvery(double inter_period, Runnable handler) {
		return new HandlerEvery<>(inter_period, (a, b) -> b, e -> handler.run());
	}

	@NotNull public static <E> EventReducer<E> toEvery(double inter_period, Ƒ2<E,E,E> reduction, Consumer<E> handler) {
		return new HandlerEvery<>(inter_period, reduction, handler);
	}

	@NotNull public static <E> EventReducer<E> toEvery(double inter_period, Ƒ2<E,E,E> reduction, Runnable handler) {
		return new HandlerEvery<>(inter_period, reduction, e -> handler.run());
	}

	@NotNull public static <E> EventReducer<E> toFirstOfAtLeast(double inter_period, double atLeast, Consumer<E> handler) {
		return new HandlerFirstOfAtLeast<>(inter_period, atLeast, handler);
	}

	@NotNull public static <E> EventReducer<E> toFirstOfAtLeast(double inter_period, double atLeast, Runnable handler) {
		return new HandlerFirstOfAtLeast<>(inter_period, atLeast, e -> handler.run());
	}

	public static class HandlerLast<E> extends EventReducer<E> {
		private final FxTimer t;

		public HandlerLast(double inter_period, Ƒ2<E,E,E> reduction, Consumer<E> handler) {
			super(inter_period, reduction, handler);
			t = fxTimer(millis(inter_period), 1, runnable(() -> action.accept(e)));
		}

		@Override
		public void handle() {
			t.start(millis(inter_period));
		}

		public boolean hasEventsQueued() {
			return t.isRunning();
		}

	}

	private static class HandlerEvery<E> extends EventReducer<E> {
		private FxTimer t;
		private long last = 0;
		boolean fired = false;

		public HandlerEvery(double inter_period, Ƒ2<E,E,E> reduction, Consumer<E> handler) {
			super(inter_period, reduction, handler);
			t = fxTimer(millis(inter_period), 1, runnable(() -> {
				action.accept(e);
				if (fired) t.start();
				fired = false;
			}));
		}

		@Override
		public void handle() {
			long now = System.currentTimeMillis();
			long diff = now - last;
			last = now;

			if (diff>inter_period) {
				action.accept(e);
				fired = false;
				if (!t.isRunning()) t.start();
			} else {
				fired = true;
			}
		}

	}

	private static class HandlerFirst<E> extends EventReducer<E> {
		private long last = 0;

		public HandlerFirst(double inter_period, Consumer<E> handler) {
			super(inter_period, null, handler);
		}

		@Override
		public void handle() {
			long now = System.currentTimeMillis();
			long diff = now - last;
			last = now;
			if (diff>inter_period) action.accept(e);
		}

	}

	private static class HandlerFirstDelayed<E> extends EventReducer<E> {

		private long last = 0;
		private final FxTimer t;

		public HandlerFirstDelayed(double inter_period, Consumer<E> handler) {
			super(inter_period, null, handler);
			t = fxTimer(millis(inter_period), 1, runnable(() -> action.accept(e)));
		}

		@Override
		public void handle() {
			long now = System.currentTimeMillis();
			long diff = now - last;
			boolean isFirst = diff>=inter_period;
			if (isFirst && !t.isRunning()) t.start();

			last = now;
		}

	}

	private static class HandlerFirstOfAtLeast<E> extends EventReducer<E> {
		private long first = 0;
		private long last = 0;
		private final double atLeast;
		private boolean ran = false;

		public HandlerFirstOfAtLeast(double inter_period, double atLeast, Consumer<E> handler) {
			super(inter_period, null, handler);
			this.atLeast = atLeast;
		}

		@Override
		public void handle() {
			long now = System.currentTimeMillis();
			long diff = now - last;
			boolean isFirst = diff>=inter_period;

			if (isFirst) {
				first = now;
				ran = false;
			}

			boolean isLongEnough = now - first>=atLeast;
			if (isLongEnough && !ran) {
				action.accept(e);
				ran = true;
			}

			last = now;
		}

	}
}