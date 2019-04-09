package sp.it.util.access;

import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

public class Vo<T> implements ObservableValue<T>, WritableValue<T> {
	public final BooleanProperty override = new SimpleBooleanProperty(true);
	public final ObjectProperty<T> real = new SimpleObjectProperty<>();
	public final Property<T> parent;
	public final ObjectProperty<T> current;

	public Vo(Property<T> parent) {
		this(parent.getValue(), false, parent);
	}

	public Vo(boolean override, Property<T> parent) {
		this(parent.getValue(), override, parent);
	}

	public Vo(T val, boolean override, Property<T> parent) {
		this.override.set(override);
		this.real.set(val);
		this.parent = parent;
		current = new SimpleObjectProperty<>(override ? val : parent.getValue());

		ChangeListener<Object> l = (o, ov, nv) -> change();
		this.override.addListener(l);
		this.real.addListener(l);
		this.parent.addListener(l);
	}

	@Override
	public void addListener(ChangeListener<? super T> listener) {
		current.addListener(listener);
	}

	@Override
	public void removeListener(ChangeListener<? super T> listener) {
		current.removeListener(listener);
	}

	@Override
	public T getValue() {
		return current.getValue();
	}

	@Override
	public void setValue(T val) {
		real.setValue(val);
	}

	@Override
	public void addListener(InvalidationListener listener) {
		current.addListener(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		current.removeListener(listener);
	}

	private void change() {
		T t = override.get() ? real.getValue() : parent.getValue();
		if (!Objects.equals(t, current.get()))
			current.setValue(t);
	}

}