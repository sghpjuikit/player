package util.reactive;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.stage.Screen;
import org.reactfx.Subscription;
import static org.reactfx.EventStreams.valuesOf;
import static util.dev.Util.noØ;

/**
 * Utility methods for reactive behavior.
 */
public interface Util {

	static <O> Subscription changes(ObservableValue<O> o, BiConsumer<? super O,? super O> u) {
		ChangeListener<O> l = (b, ov, nv) -> u.accept(ov, nv);
		o.addListener(l);
		return () -> o.removeListener(l);
	}

	static <O, V> Subscription maintain(ObservableValue<O> o, Function<O,V> m, Consumer<? super V> u) {
		u.accept(m.apply(o.getValue()));
		return valuesOf(o).map(m).subscribe(u);
	}

	static <O> Subscription maintain(ObservableValue<O> o, Consumer<? super O> u) {
		ChangeListener<O> l = (b, ov, nv) -> u.accept(nv);
		u.accept(o.getValue());
		o.addListener(l);
		return () -> o.removeListener(l);
	}

	static <O, V> Subscription maintain(ObservableValue<O> o, Function<? super O,? extends V> m, WritableValue<? super V> w) {
		w.setValue(m.apply(o.getValue()));
		ChangeListener<O> l = (x, ov, nv) -> w.setValue(m.apply(nv));
		o.addListener(l);
		return () -> o.removeListener(l);
	}

	static <O1, O2> Subscription maintain(ObservableValue<O1> o1, ObservableValue<O2> o2, BiConsumer<? super O1,? super O2> u) {
		ChangeListener<O1> l1 = (b, ov, nv) -> u.accept(nv, o2.getValue());
		ChangeListener<O2> l2 = (b, ov, nv) -> u.accept(o1.getValue(), nv);
		u.accept(o1.getValue(), o2.getValue());
		o1.addListener(l1);
		o2.addListener(l2);
		return () -> {
			o1.removeListener(l1);
			o2.removeListener(l2);
		};
	}

	static <O> Subscription maintain(ObservableValue<? extends O> o, WritableValue<O> w) {
		w.setValue(o.getValue());
		ChangeListener<O> l = (x, ov, nv) -> w.setValue(nv);
		o.addListener(l);
		return () -> o.removeListener(l);
	}

	static <T> Subscription sizeOf(ObservableList<T> list, Consumer<? super Integer> action) {
		ListChangeListener<T> l = change -> action.accept(list.size());
		l.onChanged(null);
		list.addListener(l);
		return () -> list.removeListener(l);
	}

	static <O> Subscription maintain(ValueStream<O> o, Consumer<? super O> u) {
		u.accept(o.getValue());
		return o.subscribe(u);
	}

	static <O> Subscription maintain(ValueStream<O> o, O initial, Consumer<? super O> u) {
		u.accept(initial);
		return o.subscribe(u);
	}

	/**
	 * Runs action (consuming the property's value) immediately if value non null or sets a one-time
	 * listener which will run the action when the value changes to non null for the 1st time and
	 * remove itself.
	 * <p/>
	 * It is guaranteed:
	 * <ul>
	 * <li> action executes at most once
	 * <li> action never consumes null
	 * <li> action executes as soon as the property value is not null - now or in the future
	 * </ul>
	 * <p/>
	 * Used to execute some kind of initialization routine, which requires nonnull value (which is
	 * not guaranteed to be the case).
	 */
	static <T> Subscription doOnceIfNonNull(ObservableValue<T> property, Consumer<T> action) {
		return doOnceIf(property, Objects::nonNull, action);
	}

	static <T> Subscription doOnceIf(ObservableValue<T> property, Predicate<? super T> condition, Consumer<T> action) {
		if (condition.test(property.getValue())) {
			action.accept(property.getValue());
			return () -> {};
		} else {
			ChangeListener<T> l = singletonListener(property, condition, action);
			property.addListener(l);
			return () -> property.removeListener(l);
		}
	}

	static <T> ChangeListener<T> singletonListener(ObservableValue<T> property, Predicate<? super T> condition, Consumer<T> action) {
		return new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends T> observable, T ov, T nv) {
				if (condition.test(nv)) {
					action.accept(nv);
					property.removeListener(this);
				}
			}
		};
	}

	static <T> Subscription installSingletonListener(ObservableValue<T> property, Predicate<? super T> condition, Consumer<T> action) {
		ChangeListener<T> l = singletonListener(property, condition, action);
		property.addListener(l);
		return () -> property.removeListener(l);
	}

	/** Creates list change listener which calls an action for every added or removed item. */
	static Subscription onScreenChange(Consumer<? super Screen> onChange) {
		ListChangeListener<Screen> l = listChangeListener(change -> {
			if (change.wasAdded()) onChange.accept(change.getAddedSubList().get(0));
		});
		Screen.getScreens().addListener(l);
		return () -> Screen.getScreens().removeListener(l);
	}

	static <T> ListChangeListener<T> listChangeListener(ListChangeListener<T> onAdded, ListChangeListener<T> onRemoved) {
		noØ(onAdded, onRemoved);
		return change -> {
			while (change.next()) {
				if (!change.wasPermutated() && !change.wasUpdated()) {
					if (change.wasAdded()) onAdded.onChanged(change);
					if (change.wasAdded()) onRemoved.onChanged(change);
				}
			}
		};
	}

	/**
	 * Creates list change listener which calls the provided listeners on every change.
	 * </p>
	 * This is a convenience method taking care of the while(change.next()) code pattern explained in
	 * {@link javafx.collections.ListChangeListener.Change}.
	 */
	static <T> ListChangeListener<T> listChangeListener(ListChangeListener<T> onChange) {
		noØ(onChange);
		return change -> {
			while (change.next()) {
				onChange.onChanged(change);
			}
		};
	}

	/** Creates list change listener which calls an action for every added or removed item. */
	static <T> ListChangeListener<T> listChangeHandlerEach(Consumer<T> addedHandler, Consumer<T> removedHandler) {
		noØ(addedHandler, removedHandler);
		return change -> {
			while (change.next()) {
				if (!change.wasPermutated() && !change.wasUpdated()) {
					if (change.wasAdded()) change.getRemoved().forEach(removedHandler);
					if (change.wasAdded()) change.getAddedSubList().forEach(addedHandler);
				}
			}
		};
	}

	/** Creates list change listener which calls an action added or removed item list. */
	static <T> ListChangeListener<T> listChangeHandler(Consumer<List<? extends T>> addedHandler, Consumer<List<? extends T>> removedHandler) {
		noØ(addedHandler, removedHandler);
		return change -> {
			while (change.next()) {
				if (!change.wasPermutated() && !change.wasUpdated()) {
					if (change.wasAdded()) removedHandler.accept(change.getRemoved());
					if (change.wasAdded()) addedHandler.accept(change.getAddedSubList());
				}
			}
		};
	}

	/**
	 * Unsubscribe the subscription. Does nothing if null. Returns (always) null.
	 * <p/>
	 * Use for concise and null safe disposal, using the following idiom:
	 * <br/><br/>
	 * {@code mySubscription = unsubscribe(mySubscription); }
	 *
	 * @param s subscription to unsubscribe or null
	 * @return null
	 */
	static Subscription unsubscribe(Subscription s) {
		if (s!=null) s.unsubscribe();
		return null;
	}
}