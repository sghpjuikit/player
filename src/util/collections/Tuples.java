package util.collections;

public class Tuples {

    public static <A, B> Tuple2<A, B> tuple(A a, B b) {
        return new Tuple2<>(a, b);
    }

    public static <A, B, C> Tuple3<A, B, C> tuple(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }

    /** Crates immutable tetra-tuple. */
    public static <A, B, C, D> Tuple4<A, B, C, D> tuple(A a, B b, C c, D d) {
        return new Tuple4<>(a, b, c, d);
    }
    /** Crates mutable tetra-tuple. */
    public static <A, B, C, D> TupleM4 tupleM(A a, B b, C c, D d) {
        return new TupleM4<>(a, b, c, d);
    }

    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> tuple(A a, B b, C c, D d, E e) {
        return new Tuple5<>(a, b, c, d, e);
    }

    public static <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F> tuple(A a, B b, C c, D d, E e, F f) {
        return new Tuple6<>(a, b, c, d, e, f);
    }
}