package sp.it.pl.util.reactive;

import java.util.HashSet;
import java.util.function.Consumer;
import org.reactfx.Subscription;
import sp.it.pl.util.functional.Functors.Ƒ1;

/** Set of consumers/functions taking 1 parameter. For use as a collection of handlers/listeners. */
public class SetƑ1<I> extends HashSet<Consumer<I>> implements Ƒ1<I,Void> {

	public SetƑ1() {
		super(2);
	}

	@Override
	public Void apply(I input) {
		forEach(c -> c.accept(input));
		return null;
	}

	public Subscription addS(Consumer<I> r) {
		add(r);
		return () -> remove(r);
	}

}