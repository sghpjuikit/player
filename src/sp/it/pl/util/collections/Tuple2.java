package sp.it.pl.util.collections;

import java.util.Objects;
import static sp.it.pl.util.collections.Tuples.tuple;

public class Tuple2<A, B> {
	public final A _1;
	public final B _2;

	Tuple2(A a, B b) {
		_1 = a;
		_2 = b;
	}

	public Tuple2<A,B> update1(A a) {
		return tuple(a, _2);
	}

	public Tuple2<A,B> update2(B b) {
		return tuple(_1, b);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Tuple2) {
			Tuple2<?,?> that = (Tuple2<?,?>) other;
			return Objects.equals(this._1, that._1)
					&& Objects.equals(this._2, that._2);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(_1, _2);
	}

	@Override
	public String toString() {
		return "("
				+ Objects.toString(_1) + ", "
				+ Objects.toString(_2)
				+ ")";
	}
}