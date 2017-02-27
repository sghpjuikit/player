package util.collections;

import java.util.Objects;

/**
 * Mutable tuple.
 */
public final class TupleM6<A, B, C, D, E, F> {

	public A a;
	public B b;
	public C c;
	public D d;
	public E e;
	public F f;

	public TupleM6(A a, B b, C c, D d, E e, F f) {
		set(a, b, c, d, e, f);
	}

	public void set(A a, B b, C c, D d, E e, F f) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TupleM6) {
			TupleM6 that = (TupleM6) o;
			return Objects.equals(this.a, that.a) &&
					Objects.equals(this.b, that.b) &&
					Objects.equals(this.c, that.c) &&
					Objects.equals(this.d, that.d) &&
					Objects.equals(this.e, that.e) &&
					Objects.equals(this.f, that.f);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(a, b, c, d, e, f);
	}

	@Override
	public String toString() {
		return "(" + Objects.toString(a) + ", "
				+ Objects.toString(b) + ", "
				+ Objects.toString(c) + ", "
				+ Objects.toString(d) + ", "
				+ Objects.toString(e) + ", "
				+ Objects.toString(f)
				+ ")";
	}
}