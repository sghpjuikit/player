package sp.it.pl.layout.widget.controller.io;

import java.util.Objects;
import java.util.UUID;
import sp.it.pl.util.reactive.Subscription;
import static sp.it.pl.util.functional.UtilKt.consumer;

public class Output<T> extends Put<T> {
	public final Id id;

	public Output(UUID id, String name, Class<T> c) {
		super(c, name, null);
		this.id = new Id(id, name);
	}

	Subscription monitor(Input<T> input) {
		if (!input.canBind(this)) throw new IllegalArgumentException("Input<" + input.type + "> can not bind to put<" + type + ">");
		return sync(consumer(v -> {
			if (v!=null && input.type.isInstance(v))
				input.setValue(v);
		}));
	}

	/** Calls {@link sp.it.pl.layout.widget.controller.io.Input#bind(Output)} on specified input with this output. */
	public Subscription bind(Input<? super T> input) {
		return input.bind(this);
	}

	/** Calls {@link sp.it.pl.layout.widget.controller.io.Input#unbind(Output)} on specified input with this output. */
	public void unbind(Input<? super T> input) {
		input.unbind(this);
	}

	@Override
	public boolean equals(Object o) {
		return this==o || o instanceof Output && id.equals(((Output) o).id);
	}

	@Override
	public int hashCode() {
		return 5 * 89 + Objects.hashCode(this.id);
	}

	public static class Id {
		public final UUID carrier_id;
		public final String name;

		public Id(UUID carrier_id, String name) {
			this.carrier_id = carrier_id;
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			return this==o || (o instanceof Id && ((Id)o).name.equals(name) && ((Id)o).carrier_id.equals(carrier_id));
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 79 * hash + Objects.hashCode(this.carrier_id);
			hash = 79 * hash + Objects.hashCode(this.name);
			return hash;
		}

		@Override
		public String toString() {
			return name + "," + carrier_id.toString();
		}

		public static Id fromString(String s) {
			int i = s.indexOf(",");
			String n = s.substring(0,i);
			UUID u = UUID.fromString(s.substring(i+1, s.length()));
			return new Id(u,n);
		}

	}

}