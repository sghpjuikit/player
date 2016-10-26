package util.functional;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Provides functional methods for object.
 * 
 * @param <O> Object - self type: {@code X extends Operable<X> }
 */
@SuppressWarnings("unchecked")
public interface Operable<O> {

	default O apply(UnaryOperator<O> op) {
		return op.apply((O)this);
	}

	default <R> R apply(Function<O, R> op) {
		return op.apply((O)this);
	}

	default O apply(O e, BinaryOperator<O> op) {
		return op.apply((O)this, e);
	}

	default void use(Consumer<O> op) {
		op.accept((O)this);
	}

	default O useAnd(Consumer<O> op) {
		op.accept((O)this);
		return (O) this;
	}

	default boolean isIn(O os) {
		return Stream.of(os).anyMatch(o -> o==this);
	}

}