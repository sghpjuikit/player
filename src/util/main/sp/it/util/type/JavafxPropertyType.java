package sp.it.util.type;

public interface JavafxPropertyType<T> {
	interface JavafxIntegerPropertyType extends JavafxPropertyType<Integer> {}
	interface JavafxLongPropertyType extends JavafxPropertyType<Long> {}
	interface JavafxFloatPropertyType extends JavafxPropertyType<Float> {}
	interface JavafxDoublePropertyType extends JavafxPropertyType<Double> {}
}
