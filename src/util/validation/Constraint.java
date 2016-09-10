package util.validation;

import java.io.File;
import java.lang.annotation.*;
import java.util.function.Predicate;

import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static util.dev.Util.noØ;

/**
 * Created by Plutonium_ on 9/10/2016.
 */
public interface Constraint<T> {

	boolean validate(T t);
	String message();

/* ---------- ANNOTATION -> IMPLEMENTATION MAPPING ------------------------------------------------------------------ */

	ClassMap<Ƒ1<? extends Annotation,? extends Constraint>> MAPPER = new ClassMap<>() {{
		put(FileType.class, (FileType constraint) -> constraint.value());
		put(MinMax.class, (MinMax constraint) -> new NumberMinMax(constraint.min(), constraint.max()));
	}};

	@SuppressWarnings("unchecked")
	static <A extends Annotation,X> Constraint toConstraint(A a) {
		return ((Ƒ1<A,Constraint<X>>)noØ(MAPPER.get(a.annotationType()))).apply(a);
	}

/* ---------- IMPLEMENTATIONS --------------------------------------------------------------------------------------- */

//	abstract class C_Base<T> implements Constraint<T> {
//		private
//	}

	/**
	 * Denotes type of [java.io.File] an actor can produce/consume.
	 * For example to decide between file and directory chooser.
	 */
	enum FileActor implements Constraint<File>{
		FILE(File::isFile, "File must not be directory"),
		DIRECTORY(File::isDirectory, "File must be directory"),
		ANY(f -> true, "");

		private final Predicate<File> condition;
		private final String message;

		FileActor(Predicate<File> condition, String message) {
			this.condition = condition;
			this.message = message;
		}


		@Override
		public boolean validate(File file) {
			return condition.test(file);
		}

		@Override
		public String message() {
			return message;
		}
	}

	class NumberMinMax implements Constraint<Number> {
		public final double min, max;

		public NumberMinMax(double min, double max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public boolean validate(Number d) {
			double n = d.doubleValue();
			return min<=n && n<=max;
		}

		@Override
		public String message() {
			return "Number must be in range " + min + " - " + max;
		}
	}

/* ---------- ANNOTATIONS ------------------------------------------------------------------------------------------- */

	@Documented
	@Target(ANNOTATION_TYPE)
	@Retention(RUNTIME)
	@interface IsConstraint {
		Class<?>value();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(File.class)
	@interface FileType {
		FileActor value() default FileActor.ANY;
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(Number.class)
	@interface Min {
		double value();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(Number.class)
	@interface Max {
		double value();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(Number.class)
	@interface MinMax {
		double min();
		double max();
	}
}