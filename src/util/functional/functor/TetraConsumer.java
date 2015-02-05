package util.functional.functor;

@FunctionalInterface
public interface TetraConsumer<A, B, C, D> {
    void accept(A a, B b, C c, D d);
}