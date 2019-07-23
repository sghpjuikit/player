package sp.it.util.access;

public interface TypedValue<C> {

	/** @return class type of the value (not class of this object) */
	Class<C> getType();

}