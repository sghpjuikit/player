package sp.it.pl.util.collections;

import java.util.Objects;

/**
 * Mutable tuple.
 */
public final class TupleM4<A, B, C, D> {

	public A a;
	public B b;
	public C c;
	public D d;

	public TupleM4(A a, B b, C c, D d) {
		set(a, b, c, d);
	}

	public void set(A a, B b, C c, D d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TupleM4) {
			TupleM4 that = (TupleM4) o;
			return Objects.equals(this.a, that.a) &&
					Objects.equals(this.b, that.b) &&
					Objects.equals(this.c, that.c) &&
					Objects.equals(this.d, that.d);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, b, c, d);
	}

	@Override
	public String toString() {
		return "(" + Objects.toString(a) + ", "
				+ Objects.toString(b) + ", "
				+ Objects.toString(c) + ", "
				+ Objects.toString(d)
				+ ")";
	}
}