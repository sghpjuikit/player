package util.access;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import org.reactfx.Subscription;

/**
 * Var/variable - simple object wrapper similar to {@link javafx.beans.property.Property}, but
 * simpler (no binding) and with the ability to apply value change.
 * <p/>
 * Does not permit null values.
 *
 * @author Martin Polakovic
 */
public class V<T> extends SimpleObjectProperty<T> implements ApplicableValue<T> {

	private Consumer<T> applier;

	public V(T val) {
		setValue(val);
	}

	public V(T val, Consumer<T> applier) {
		this(val);
		setApplier(applier);
	}

	public V(T val, Runnable applier) {
		this(val, v -> applier.run());
	}

	/** {@inheritDoc} */
	@Override
	public void applyValue(T val) {
		if (applier!=null) applier.accept(val);
	}

	/**
	 * Sets applier. Applier is a code that applies the value in any way.
	 *
	 * @param applier or null to disable applying
	 */
	public final void setApplier(Consumer<T> applier) {
		this.applier = applier;
	}

	/**
	 * Gets applier. Applier is a code that applies the value. It can do anything.
	 * Default null.
	 *
	 * @return applier or null if none.
	 */
	public Consumer<T> getApplier() {
		return applier;
	}

	public Subscription onChange(Consumer<? super T> action) {
		ChangeListener<T> l = (o, ov, nv) -> action.accept(nv);
		addListener(l);
		return () -> removeListener(l);
	}

	public Subscription onChange(BiConsumer<? super T,? super T> action) {
		ChangeListener<T> l = (o, ov, nv) -> action.accept(ov, nv);
		addListener(l);
		return () -> removeListener(l);
	}

	public Subscription maintain(Consumer<? super T> action) {
		ChangeListener<T> l = (o, ov, nv) -> action.accept(nv);
		addListener(l);
		action.accept(getValue());
		return () -> removeListener(l);
	}

	public Subscription onInvalid(Runnable action) {
		InvalidationListener l = o -> action.run();
		addListener(l);
		return () -> removeListener(l);
	}

}