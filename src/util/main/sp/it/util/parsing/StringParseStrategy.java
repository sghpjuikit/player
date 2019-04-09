package sp.it.util.parsing;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static sp.it.util.parsing.StringParseStrategy.From.NONE;
import static sp.it.util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/**
 * Indicates that the class has defined strategy for parsing to and from String.
 * This gives hint to parser as to how the parsing should be done.
 * <p/>
 * The strategy for parsing from and to String must be consistent, i.e. parsing
 * from string the object was parsed into must not fail and be able to return
 * non null value.
 *
 * @see #from() for more details
 * @see #to() for more details
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface StringParseStrategy {

	/** Defines strategy for parsing object from String. */
	From from() default NONE;

	/** Defines strategy for parsing object to String. */
	To to() default TO_STRING_METHOD;

	/** Use along with {@link To#CONSTANT}. */
	String constant() default "";

	/**
	 * Use when to string strategy relies on a method that can throw an
	 * exception. Here it is possible to specify all exceptions regarding the
	 * conversion process. Converter can then catch them and provide a unification
	 * mechanism for handling erroneous input and failed conversions.
	 * <p/>
	 * This shifts the implementation in fail cases from the object to the
	 * parsing framework.
	 * <p/>
	 * Enumerate all exceptions (runtime or checked) that can be thrown as a
	 * result of to string conversion failure. Exceptions due to programming
	 * error or other causes should not be listed).
	 * <p/>
	 * Default value is empty array.
	 */
	Class<? extends Exception>[] exTo() default {};

	/** Same as {@link #exTo() } but for parsing from string instead. */
	Class<? extends Exception>[] exFrom() default {};

	/** Defines strategy for parsing object from String. */
	enum From {
		/** {@link ParsesFromString} annotation decides responsible method. */
		ANNOTATED_METHOD,
		/**
		 * Will use fx parser. The string format of the fx parser is subject to change and should
		 * not be relied on and will not be disclosed here.
		 * <p/>
		 * Should be used in combination with {#link StringParseStrategy.To#FX }.
		 * <p/>
		 * Instance will be created by invoking the public no-arg constructor (fails if not
		 * accessible) of the exact class, name of which is in the string.
		 * Converter then uses available name-value pairs and sets the values to appropriate
		 * javafx property beans of the instance.
		 */
		FX,
		/** Parses consistently into the same instance defined as public static final INSTANCE field of the class. */
		SINGLETON,
		/** Parsing strategy undefined. */
		NONE
	}

	/** Defines strategy for parsing object to String. */
	enum To {
		/** toString() method is responsible. */
		TO_STRING_METHOD,
		/** {@link ParsesToString} annotation decides responsible method. */
		ANNOTATED_METHOD,
		/** Parses consistently into the same string value defined in. {@link StringParseStrategy#constant() } */
		CONSTANT,
		/**
		 * Will use fx parser. The string format of the fx parser is subject to change and should
		 * not be relied on and will not be disclosed here.
		 * <p/>
		 * Should be used in combination with {#link StringParseStrategy.From#FX }.
		 * <p/>
		 * The parsed string contains exact class and name-value pairs of all
		 * javafx property beans.
		 */
		FX,
		/** Parses consistently into a constant identifying the object class. */
		SINGLETON,
		/** Parsing strategy undefined. */
		NONE
	}
}