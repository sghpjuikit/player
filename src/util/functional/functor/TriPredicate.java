package util.functional.functor;

@FunctionalInterface
public interface TriPredicate<A, B, C> {
    boolean test(A a, B b, C c);
}