package layout.widget.controller.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.reactfx.Subscription;

import util.type.typetoken.TypeToken;

import static util.reactive.Util.maintain;

public class Outputs {
	private final Map<String,Output<?>> m;


	public Outputs() {
		m = new HashMap<>();
	}

	public <T> Output<T> create(UUID id, String name, TypeToken<? super T> type, T val) {
		Output<T> o = create(id, name, type.getRawType(), val);
		o.typeT = type;
		return o;
	}

	public <T> Output<T> create(UUID id, String name, Class<? super T> type, T val) {
		Output<T> o = new Output<>(id,name,type);
				  o.setValue(val);
		m.put(name, o);
		return o;
	}

	public int getSize() {
		return m.size();
	}

	public Output getOutput(String name) {
		return m.get(name);
	}

	public boolean contains(String name) {
		return m.containsKey(name);
	}

	public boolean contains(Output i) {
		return m.containsValue(i);
//		return m.containsKey(i.id.name); // fast, but does not guarantee correctness
	}

	public Collection<Output<?>> getOutputs() {
		return m.values();
	}

	@SuppressWarnings("unchecked")
	public <T> Subscription monitor(String name, Consumer<T> action) {
		Output<T> o = (Output<T>) m.get(name);
		if (o==null) return Subscription.EMPTY;

		return maintain(o.val, action);
	}

}