package sp.it.pl.util.reactive;

import java.util.HashSet;
import org.reactfx.Subscription;
import sp.it.pl.util.functional.Functors.Ƒ;

/** Set of {@link java.lang.Runnable} taking o parameters. Use as a collection of handlers. */
public class SetƑ extends HashSet<Runnable> implements Ƒ {

	public SetƑ() {
		super(2);
	}

	@Override
	public void apply() {
		forEach(Runnable::run);
	}

	public Subscription addS(Runnable r) {
		add(r);
		return () -> remove(r);
	}

}