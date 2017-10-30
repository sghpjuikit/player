package util.validation;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.util.Duration;
import org.atteo.classindex.ClassIndex;
import org.atteo.classindex.IndexAnnotated;
import util.Password;
import util.collections.map.ClassListMap;
import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.type.Util;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static util.dev.Util.noØ;
import static util.dev.Util.throwIfNot;
import static util.functional.Try.error;
import static util.functional.Try.ok;
import static util.functional.Util.stream;
import static util.type.Util.getGenericInterface;
import static util.type.Util.instantiateOrThrow;
import static util.validation.Constraint.DeclarationType.Declaration.IMPLICIT;

@SuppressWarnings({"unchecked", "unused"})
public interface Constraint<T> {

	boolean isValid(T t);

	String message();

	default Try<Void,String> validate(T t) {
		return isValid(t) ? ok(null) : error(message());
	}

/* ---------- ANNOTATION -> IMPLEMENTATION MAPPING ------------------------------------------------------------------ */

	@SuppressWarnings("Convert2MethodRef")
	ClassMap<Ƒ1<? extends Annotation,? extends Constraint>> MAPPER = new ClassMap<>() {{
		put(FileType.class, (FileType constraint) -> constraint.value());
		put(MinMax.class, (MinMax constraint) -> new NumberMinMax(constraint.min(), constraint.max()));
		put(NonEmpty.class, (NonEmpty constraint) -> new StringNonEmpty());
		put(Length.class, (Length constraint) -> new StringLength(constraint.min(), constraint.max()));
		put(ConstrainsBy.class, (ConstrainsBy constraint) -> instantiateOrThrow(constraint.value()));
		put(NonNullElements.class, (NonNullElements constraint) -> new HasNonNullElements());
	}};
	ClassListMap<Constraint> IMPLICIT_CONSTRAINTS = new ClassListMap<>(o -> getGenericInterface(o.getClass(), 0, 0)) {{
		stream(ClassIndex.getAnnotated(DeclarationType.class).iterator())
					// report programming errors
					.map(c -> {
						if (!Constraint.class.isAssignableFrom(c))
							throw new RuntimeException("Only subclasses of " + Constraint.class + " can be annotated by " + DeclarationType.class);
						return (Class<Constraint>) c;
					})
					// only implicit
					.filter(c -> c.getAnnotation(DeclarationType.class).value()==IMPLICIT)
					// instantiate constraints
					.map(Util::instantiateOrThrow)
					.forEach(this::accumulate);
	}};

	@SuppressWarnings("unchecked")
	static <A extends Annotation, X> Constraint toConstraint(A a) {
		return ((Ƒ1<A,Constraint<X>>) noØ(MAPPER.get(a.annotationType()))).apply(a);
	}

	@Documented
	@IndexAnnotated
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface DeclarationType {
		Declaration value() default Declaration.EXPLICIT;

		enum Declaration {IMPLICIT, EXPLICIT}
	}

/* ---------- IMPLEMENTATIONS --------------------------------------------------------------------------------------- */

	/**
	 * Denotes type of [java.io.File] an actor can produce/consume.
	 * For example to decide between file and directory chooser.
	 */
	enum FileActor implements Constraint<File> {
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
		public boolean isValid(File file) {
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
			throwIfNot(max>min, "Max value must be greater than min value");
		}

		@Override
		public boolean isValid(Number d) {
			double n = d.doubleValue();
			return min<=n && n<=max;
		}

		@Override
		public String message() {
			return "Number must be in range " + min + " - " + max;
		}
	}

	class StringNonEmpty implements Constraint<String> {
		@Override
		public boolean isValid(String s) {
			return s!=null && !s.isEmpty();
		}

		@Override
		public String message() {
			return "String must not be empty";
		}
	}

	class PasswordNonEmpty implements Constraint<Password> {
		@Override
		public boolean isValid(Password s) {
			return s!=null && s.get()!=null && !s.get().isEmpty();
		}

		@Override
		public String message() {
			return "Password must not be empty";
		}
	}

	class StringLength implements Constraint<String> {
		public final int min, max;

		public StringLength(int min, int max) {
			this.min = min;
			this.max = max;
			throwIfNot(max>min, "Max value must be greater than min value");
		}

		@Override
		public boolean isValid(String str) {
			double n = str.length();
			return min<=n && n<=max;
		}

		@Override
		public String message() {
			return "Text must be at least " + min + " and at most" + max + " characters long";
		}
	}

	@DeclarationType(IMPLICIT)
	class DurationNonNegative implements Constraint<Duration> {
		@Override
		public boolean isValid(Duration duration) {
			return duration==null || duration.greaterThanOrEqualTo(Duration.ZERO);
		}

		@Override
		public String message() {
			return "Duration can not be negative";
		}
	}

	class HasNonNullElements implements Constraint<Collection> {
		@Override
		public boolean isValid(Collection collection) {
			return collection.stream().allMatch(Objects::nonNull);
		}

		@Override
		public String message() {
			return "All items of the list must be non null";
		}
	}

/* ---------- ANNOTATIONS ------------------------------------------------------------------------------------------- */

	@Documented
	@Target(ANNOTATION_TYPE)
	@Retention(RUNTIME)
	@interface IsConstraint {
		Class<?> value();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(Object.class)
	@interface ConstrainsBy {
		Class<? extends Constraint> value();
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

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(String.class)
	@interface NonEmpty {}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(String.class)
	@interface Length {
		int min();

		int max();
	}

	@Documented
	@Target(FIELD)
	@Retention(RUNTIME)
	@IsConstraint(Collection.class)
	@interface NonNullElements {}
}