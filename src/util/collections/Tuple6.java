package util.collections;

import java.util.Objects;
import static util.collections.Tuples.tuple;

public class Tuple6<A, B, C, D, E, F> {
	public final A _1;
	public final B _2;
	public final C _3;
	public final D _4;
	public final E _5;
	public final F _6;

	Tuple6(A a, B b, C c, D d, E e, F f) {
		_1 = a;
		_2 = b;
		_3 = c;
		_4 = d;
		_5 = e;
		_6 = f;
	}

	public Tuple6<A,B,C,D,E,F> update1(A a) {
		return tuple(a, _2, _3, _4, _5, _6);
	}

	public Tuple6<A,B,C,D,E,F> update2(B b) {
		return tuple(_1, b, _3, _4, _5, _6);
	}

	public Tuple6<A,B,C,D,E,F> update3(C c) {
		return tuple(_1, _2, c, _4, _5, _6);
	}

	public Tuple6<A,B,C,D,E,F> update4(D d) {
		return tuple(_1, _2, _3, d, _5, _6);
	}

	public Tuple6<A,B,C,D,E,F> update5(E e) {
		return tuple(_1, _2, _3, _4, e, _6);
	}

	public Tuple6<A,B,C,D,E,F> update6(F f) {
		return tuple(_1, _2, _3, _4, _5, f);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Tuple6) {
			Tuple6<?,?,?,?,?,?> that = (Tuple6<?,?,?,?,?,?>) other;
			return Objects.equals(this._1, that._1)
					&& Objects.equals(this._2, that._2)
					&& Objects.equals(this._3, that._3)
					&& Objects.equals(this._4, that._4)
					&& Objects.equals(this._5, that._5)
					&& Objects.equals(this._6, that._6);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(_1, _2, _3, _4, _5, _6);
	}

	@Override
	public String toString() {
		return "("
				+ Objects.toString(_1) + ", "
				+ Objects.toString(_2) + ", "
				+ Objects.toString(_3) + ", "
				+ Objects.toString(_4) + ", "
				+ Objects.toString(_5) + ", "
				+ Objects.toString(_6)
				+ ")";
	}
}