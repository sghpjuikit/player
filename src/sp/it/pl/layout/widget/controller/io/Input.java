package sp.it.pl.layout.widget.controller.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.reactfx.Subscription;
import sp.it.pl.layout.area.IOLayer;

public class Input<T> extends Put<T> {
	final String name;
	final Map<Output<? extends T>,Subscription> sources = new HashMap<>();

	public Input(String name, Class<? super T> c, Consumer<? super T> action) {
		this(name, c, null, action);
	}

	public Input(String name, Class<? super T> c, T init_val, Consumer<? super T> action) {
		super(c, init_val);
		this.name = name;
		monitorInit(action);
	}


	public String getName() {
		return name;
	}

	/**
	 * Return true if this input can receive values from given output. Equivalent to
	 * <p/>
	 * {@code getType().isAssignableFrom(output.getType())}
	 */
	public boolean canBind(Output<?> output) {
		return output.getType().isAssignableFrom(getType()) || getType().isAssignableFrom(output.getType());
	}

	/**
	 * Binds to the output.
	 * Sets its value immediately and then every time it changes.
	 * Binding multiple times has no effect.
	 */
	public Subscription bind(Output<? extends T> output) {
		// Normally we would use this, but we want to allow binding to supertype too (e.g. Object -> File) and use
		// Input.getType().isInstance(new_value) as a filter to selectively pick only the values we are interested
		// in. This has use. Say a TreeView<Object> is displaying some heterogeneous object hierarchy and we want
		// to only bind to selected values if they are of certain type and ignore the rest.
		// sources.computeIfAbsent(output, o -> o.monitor(this::setValue));
		sources.computeIfAbsent(output, o -> o.monitor(this));
		IOLayer.addConnectionE(this, output);
		return () -> unbind(output);
	}

	public void unbind(Output<? extends T> output) {
		Subscription s = sources.get(output);
		if (s!=null) s.unsubscribe();
		sources.remove(output);
		IOLayer.remConnectionE(this, output);
	}

	public void unbindAll() {
		sources.values().forEach(Subscription::unsubscribe);
		sources.clear();
	}

	public Set<Output<? extends T>> getSources() {
		return sources.keySet();
	}

	@Override
	public String toString() {
		return name + ", " + type;
	}

}