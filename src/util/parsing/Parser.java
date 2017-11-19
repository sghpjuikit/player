package util.parsing;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.itemnode.StringSplitParser;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.scene.effect.Effect;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.SwitchException;
import util.collections.map.ClassMap;
import util.conf.Configurable;
import util.functional.Functors;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.functional.Util;
import util.parsing.StringParseStrategy.From;
import util.parsing.StringParseStrategy.To;
import util.units.Bitrate;
import util.units.Dur;
import util.units.FileSize;
import util.units.NofX;
import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.joining;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.functional.Try.error;
import static util.functional.Try.errorOf;
import static util.functional.Try.ok;
import static util.functional.Try.tryF;
import static util.functional.Util.list;
import static util.functional.Util.listRO;
import static util.functional.Util.noNull;
import static util.functional.Util.split;
import static util.functional.Util.stream;
import static util.type.Util.getConstructorAnnotated;
import static util.type.Util.getMethodAnnotated;

// TODO: fix the many warnings in this class
/**
 * Multi-type bidirectional Object-String converter.
 */
public abstract class Parser {

	/**
	 * Converts string s to object o of type c.
	 *
	 * @param c specifies type of object
	 * @param s string to parse
	 * @return conversion result
	 */
	abstract public <T> Try<T,String> ofS(Class<T> c, String s);

	/**
	 * Converts object to String.
	 *
	 * @param o object to parse
	 * @return conversion result
	 */
	abstract public <T> String toS(T o);

	public <T> Predicate<String> isParsable(Class<T> c) {
		return s -> ofS(c, s).isOk();
	}

	public <T> StringConverter<T> toConverterOf(Class<T> c) {
		return new StringConverter<>() {
			@Override
			public String toS(T object) {
				return Parser.this.toS(object);
			}

			@Override
			public Try<T,String> ofS(String s) {
				return Parser.this.ofS(c, s);
			}
		};
	}

	/******************************************************************************/

	private static final String DELIMITER_CONFIG_VALUE = "-";
	private static final String DELIMITER_CONFIG_NAME = ":";
	private static final String CONSTANT_NULL = "<NULL>";

	/**
	 * Default to string parser, which calls objects toString() or returns null constant.
	 */
	public static final Function<Object,String> DEFAULT_TOS = o -> o==null ? CONSTANT_NULL : o.toString();
	/**
	 * Default from string parser. Always returns null.
	 */
	public static final Function<String,Try<Object,String>> DEFAULT_FROM = o -> error("Object has no from-text converter");
	/**
	 * Fx parser.
	 * Used when {@link StringParseStrategy.To#FX} or {link StringParseStrategy.From#FX}.
	 * <p/>
	 * The parser converts object to string that contains exact object class and name-value pairs
	 * of all its javafx property beans. The parser converts back to object by invoking the
	 * no argument constructor (which must be accessible) of the class found in the string and
	 * setting the bean values in the string to respective javafx beans of the object.
	 * <p/>
	 * The exact string format is subject to change. Dont rely on it.
	 */
	public static final Parser FX = new Parser() {

		@SuppressWarnings("unchecked")
		@Override
		public <T> Try<T,String> ofS(Class<T> type, String text) {
			// improve performance by not creating any Config/Configurable, premature optimization
			try {
				String[] vals = text.split(DELIMITER_CONFIG_VALUE);
				Class<?> objecttype = Class.forName(vals[0]);
				if (type!=null && !type.isAssignableFrom(objecttype))
					throw new Exception(); // optimization, avoids next line
				T v = (T) objecttype.getConstructor().newInstance();
				Configurable c = Configurable.configsFromFxPropertiesOf(v);
				stream(vals).skip(1)
						.forEach(str -> {
							try {
								String[] nameval = str.split(DELIMITER_CONFIG_NAME);
								if (nameval.length!=2) return; // ignore
								String name = nameval[0], val = nameval[1];
								c.setField(name, val);
							} catch (Exception e) {
								throw new RuntimeException(e.getMessage(), e);
							}
						});
				return ok(v);
			} catch (Exception e) {
				log(Parser.class).warn("Parsing failed, class={} text={}", type, text, e);
				return error(e.getMessage());
			}
		}

		@Override
		public <T> String toS(T o) {
			// improve performance by not creating any Config, premature optimization
			return o.getClass().getName() + DELIMITER_CONFIG_VALUE +
					Configurable.configsFromFxPropertiesOf(o).getFields().stream()
							.map(c -> c.getName() + DELIMITER_CONFIG_NAME + c.getValueS())
							.collect(joining(DELIMITER_CONFIG_VALUE));
		}

	};
	/**
	 * Default to/from string parser with get-go support for several classes.
	 */
	public static final DefaultParser DEFAULT = new DefaultParser();	// TODO: fix possible deadlock here

	static {
		Class<? extends Throwable> nfe = NumberFormatException.class;
		Class<? extends Throwable> iae = IllegalArgumentException.class;
		Class<? extends Throwable> obe = IndexOutOfBoundsException.class;

		DEFAULT.addParsef(Boolean.class, DEFAULT_TOS, Boolean::valueOf);
		DEFAULT.addParsef(boolean.class, String::valueOf, Boolean::valueOf);
		DEFAULT.addParser(Integer.class, DEFAULT_TOS, tryF(Integer::valueOf, nfe));
		DEFAULT.addParser(int.class, String::valueOf, tryF(Integer::valueOf, nfe));
		DEFAULT.addParser(Double.class, DEFAULT_TOS, tryF(Double::valueOf, nfe));
		DEFAULT.addParser(double.class, String::valueOf, tryF(Double::valueOf, nfe));
		DEFAULT.addParser(Short.class, DEFAULT_TOS, tryF(Short::valueOf, nfe));
		DEFAULT.addParser(short.class, String::valueOf, tryF(Short::valueOf, nfe));
		DEFAULT.addParser(Long.class, DEFAULT_TOS, tryF(Long::valueOf, nfe));
		DEFAULT.addParser(long.class, String::valueOf, tryF(Long::valueOf, nfe));
		DEFAULT.addParser(Float.class, DEFAULT_TOS, tryF(Float::valueOf, nfe));
		DEFAULT.addParser(float.class, String::valueOf, tryF(Float::valueOf, nfe));
		DEFAULT.addParser(Character.class, DEFAULT_TOS, tryF(s -> s.charAt(0), obe));
		DEFAULT.addParser(char.class, String::valueOf, tryF(s -> s.charAt(0), obe));
		DEFAULT.addParser(Byte.class, DEFAULT_TOS, tryF(Byte::valueOf, nfe));
		DEFAULT.addParser(byte.class, String::valueOf, tryF(Byte::valueOf, nfe));
		DEFAULT.addParsef(String.class, s -> s, s -> s);
		DEFAULT.addParser(StringSplitParser.class, DEFAULT_TOS, tryF(StringSplitParser::new, iae));
		DEFAULT.addParser(Year.class, DEFAULT_TOS, tryF(Year::parse, DateTimeParseException.class));
		DEFAULT.addParsef(File.class, DEFAULT_TOS, File::new);
		DEFAULT.addParser(URI.class, DEFAULT_TOS, tryF(URI::create, iae));
		DEFAULT.addParser(Pattern.class, DEFAULT_TOS, tryF(Pattern::compile, PatternSyntaxException.class));
		DEFAULT.addParser(Bitrate.class, DEFAULT_TOS, tryF(Bitrate::fromString, iae, obe, nfe));
		DEFAULT.addParser(Dur.class, DEFAULT_TOS, tryF(Dur::fromString, iae));
		DEFAULT.addParserOfS(Duration.class, tryF(s -> Duration.valueOf(s.replaceAll(" ", "")), iae)); // fixes java's inconsistency
		DEFAULT.addParser(FileSize.class, DEFAULT_TOS, tryF(FileSize::fromString, nfe));
		DEFAULT.addParser(NofX.class, DEFAULT_TOS, tryF(NofX::fromString, PatternSyntaxException.class, nfe, obe));
		DEFAULT.addParser(Font.class,
				f -> String.format("%s, %s", f.getName(), f.getSize()),
				tryF(s -> {
					int i = s.indexOf(',');
					String name = s.substring(0, i);
					FontPosture style = s.toLowerCase().contains("italic") ? ITALIC : REGULAR;
					FontWeight weight = s.toLowerCase().contains("bold") ? BOLD : NORMAL;
					double size = parseDouble(s.substring(i + 2));
					return Font.font(name, weight, style, size);
				}, nfe, obe)
		);
		DEFAULT.addParser(LocalDateTime.class, DEFAULT_TOS, tryF(LocalDateTime::parse, DateTimeException.class));
		DEFAULT.addParserToS(FontAwesomeIcon.class, FontAwesomeIcon::name);
		DEFAULT.addParser(Effect.class, FX::toS, text -> FX.ofS(Effect.class, text));
//        DEFAULT.addParser(Class.class, Class::getName, noCEx(Class::forName, ClassNotFoundException.class,LinkageError.class));  // TODO: ??
		DEFAULT.addParser(Class.class, Class::getName, tryF(text -> {
			try {
				return Class.forName(text);
			} catch (ClassNotFoundException|LinkageError ex) {
				return null;
			}
		}, Exception.class));
		DEFAULT.addParsef(Functors.PƑ.class, f -> f.name + "," + f.in + "," + f.out, text -> {
			List<String> data = split(text, ",");
			if (data.size()!=3) return null;
			String name = data.get(0);
			Class<?> in = DEFAULT.ofS(Class.class, data.get(1)).getOr(null);
			Class<?> out = DEFAULT.ofS(Class.class, data.get(2)).getOr(null);
			if (name==null || in==null || out==null) return null;
			return Functors.pool.getPF(name, in, out);
		});
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Default parser implementation storing individual type parsers in a map. This means:
	 * <ul>
	 * <li> O{1) parser lookup.
	 * <li> Single initialization. No parser is ever initialized more than once.
	 * <li> Lazy initialization. Individual parsers are initialized only on parsing request. Until
	 * then no instance is created.
	 * </ul>
	 * <p/>
	 * Only one parser can be registered per single class. But the same parser instance can be
	 * reused across subclasses.
	 * <p/>
	 * Parser lookup is polymorphic. This means that if there is no parser for given type, parser
	 * for any supertype or interface that type extends will be looked up as well. Hence adding
	 * parser for given type adds it for the entire subclass hierarchy.
	 * <p/>
	 * Each parser must adhere to convention:
	 * <ul>
	 * <li> Parser must never assume anything about the input. To string parser can
	 * receive an object of particular type in any state and any subtype. From string parser can
	 * receive any String as input.
	 * <li> Parser never receives null input. Null is handled by this parser itself, thus
	 * consistently across all parsers - always.
	 * Parsing object from null string throws exception, because even null must be represented by
	 * string.
	 * Parsing null object to string produces null string constant (which will be parsed into null
	 * in opposite direction)
	 * <li> Parser must never throw an exception as a result of failed parsing, instead returns null.
	 * From string parser returning null always indicates error. To string parser returning null
	 * will produce null string constant.
	 * </ul>
	 * <p/>
	 * This class acts as a generic parser. It:
	 * <ul>
	 * <li> tries to build parser if none is available looking for {@link StringParseStrategy}
	 * and valueOf(), fromString() methods.
	 * <li> if no parser is available, toString() method is used for to string parsing
	 * and an error parser always producing null for string to object parsing.
	 * </ul>
	 * <p/>
	 * For registering a parser, there are two options:
	 * <ul>
	 * <li> Use {@link StringParseStrategy} and let the parser be created and
	 * registered automatically (lazily and only once) and keeps the parsing code within the scope
	 * of the parsed class. If the method throws an exception, it is important to let
	 * this know, either by using throws clause or define it in an annotation. Using
	 * both is unnecessary, but recommended.
	 * <p/>
	 * This allows only one implementation, tightly coupled to the parsed object's class.
	 * <li> Create parser and register it manually {@link #addParser(java.lang.Class, util.parsing.StringConverter)}.
	 * It is recommended (although not necessary) to register both to string and from string.
	 * The parser must return null if any problem occurs. This can be done in two ways - return null
	 * when the problem occurs (and in catch blocks, no exception must be thrown!)
	 * or throw an exception and wrap the parser function into noException function
	 * wrapper {@link util.functional.Util#noEx(java.util.function.Function, java.lang.Class...)}.
	 * </ul>
	 */
	public static class DefaultParser extends Parser {

		private final ClassMap<Function<? super Object,Try<String,String>>> parsersToS = new ClassMap<>();
		private final ClassMap<Function<? super String,Try<Object,String>>> parsersFromS = new ClassMap<>();

		public <T> void addParser(Class<T> c, StringConverter<T> parser) {
			addParser(c, parser::toS, parser::ofS);
		}

		public <T> void addParser(Class<T> c, Function<? super T,String> to, Function<String,Try<T,String>> of) {
			addParserToS(c, to);
			addParserOfS(c, of);
		}

		public <T> void addParsef(Class<T> c, Function<? super T,String> to, Function<String,? extends T> of) {
			addParserToS(c, to);
			addParserOfS(c, of.andThen(Try::ok));
		}

		@SuppressWarnings("unchecked")
		public <T> void addParserToS(Class<T> c, Function<? super T,String> parser) {
			parsersToS.put(c, v -> ok(parser.apply((T) v)));
		}

		@SuppressWarnings("unchecked")
		public <T> void addParserOfS(Class<T> c, Function<String,Try<T,String>> parser) {
			parsersFromS.put(c, (Function) parser);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Try<T,String> ofS(Class<T> c, String s) {
			noØ(c, "Parsing type must be specified!");
			noØ(s, "Parsing null not allowed!");
			if (CONSTANT_NULL.equals(s)) return ok(null);
			return getParserOfS(c).apply(s);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> String toS(T o) {
			if (o==null) return CONSTANT_NULL;
			String s = ((Function<T,Try<String,String>>) getParserToS(o.getClass())).apply(o).getOr(null);
			return noNull(s, CONSTANT_NULL);
		}

		@SuppressWarnings("unchecked")
		private <T> Function<? super String,Try<T,String>> getParserOfS(Class<T> c) {
			return (Function) parsersFromS.computeIfAbsent(c, key -> (Function) findOfSparser(key));
		}

		@SuppressWarnings("unchecked")
		private <T> Function<? super T,Try<String,String>> getParserToS(Class<T> c) {
			return parsersToS.computeIfAbsent(c, key -> (Function) findToSparser(key));
		}

		@SuppressWarnings("unchecked")
		private <T> Function<? super String,Try<T,String>> findOfSparser(Class<T> c) {
			return (Function) noNull(
					() -> parsersFromS.getElementOfSuper(c),
					() -> buildOfSParser(c),
					() -> DEFAULT_FROM
			);
		}

		@SuppressWarnings("unchecked")
		private <T> Function<? super T,Try<String,String>> findToSparser(Class<T> c) {
			return (Function) noNull(
					() -> parsersToS.getElementOfSuper(c),
					() -> buildToSParser(c),
					() -> DEFAULT_TOS.andThen(Try::ok)
			);
		}
	}

	/******************************************************************************/

	@SuppressWarnings("ConstantConditions")
	private static <T> Function<String,Try<T,String>> buildOfSParser(Class<T> type) {
		Function<String,Try<T,String>> ofS = null;
		StringParseStrategy a = type.getAnnotation(StringParseStrategy.class);

		if (ofS==null && a!=null) {
			From strategy = a.from();
			if (strategy==From.NONE) {
				throw new IllegalArgumentException("Failed to create from string converter. Class '" + type + "'s parsing strategy forbids parsing from string.");
			} else if (strategy==From.SINGLETON) {
				String fieldName = "INSTANCE";
				try {
					Field f = type.getDeclaredField(fieldName);
					Class<?> fType = f.getType();
					if (!Modifier.isStatic(f.getModifiers())) throw new NoSuchFieldException(fieldName + " field must be static=" + fType);
					if (fType!=type) throw new NoSuchFieldException(fieldName + " field has wrong type=" + fType);

					ofS = text -> {
						try {
							//noinspection unchecked
							return Try.ok((T) f.get(null));
						} catch (IllegalAccessException e) {
							throw new IllegalStateException("Field " + f + " is not accessible");
						}
					};

				} catch(NoSuchFieldException e) {
					throw new IllegalArgumentException("Failed to create from string converter. Singleton class " + type + " has no public static " + fieldName + " field");
				}
			} else if (strategy==From.ANNOTATED_METHOD) {
				Invokable<T> invokable = null;  // in class T returns ?

				if (invokable==null) {
					Constructor<T> c = getConstructorAnnotated(type, ParsesFromString.class);
					if (c!=null) invokable = Invokable.of(c);
				}

				if (invokable==null) {
					Method m = getMethodAnnotated(type, ParsesFromString.class);
					if (m!=null && Modifier.isStatic(m.getModifiers()))
						invokable = Invokable.ofStaticMethod(m);
				}

				if (invokable==null)
					throw new IllegalArgumentException("Failed to create from string converter. Responsible method was not found");

				ofS = parserOfI(invokable, String.class, type, a, ParseDir.OFS);
			} else if (strategy==From.FX) {
				ofS = text -> FX.ofS(type, text);
			} else {
				throw new SwitchException(strategy);
			}
		}

		// try to fall back to valueOf method
		if (ofS==null) {
			Method m = getValueOfStatic(type);
			if (m!=null) ofS = parserOfM(m, String.class, type, null, null);
		}
		// try to fall back to fromString method
		if (ofS==null) {
			Method m = getMethodStatic("fromString", type);
			if (m!=null) ofS = parserOfM(m, String.class, type, null, null);
		}

		return ofS;
	}

	private static <T> Function<T,Try<String,String>> buildToSParser(Class<T> c) {
		StringParseStrategy a = c.getAnnotation(StringParseStrategy.class);
		if (a!=null) {
			To strategy = a.to();
			if (strategy==To.CONSTANT) {
				String constant = a.constant();
				return in -> ok(constant);
			} else if (strategy==To.NONE) {
				throw new IllegalArgumentException("Failed to create to string converter. Class '" + c + "'s parsing strategy forbids parsing to string.");
			} else if (strategy==To.SINGLETON) {
				return in -> ok(in.getClass().getName());
			} else if (strategy==To.ANNOTATED_METHOD) {
				Method m = getMethodAnnotated(c, ParsesToString.class);
				if (m==null || m.getReturnType()!=String.class || (!m.isVarArgs() && m.getParameterCount()>1) || (m.getParameterCount()==1 && !m.getParameterTypes()[0].isAssignableFrom(c)))
					throw new IllegalArgumentException("Failed to create to string converter. Class not parsable to string, because suitable method was not found: " + m);
				boolean pass_params = m.getParameterCount()==1;
				Function<T,String> f = pass_params
						? in -> {
							try {
								return (String) m.invoke(in, in);
							} catch (IllegalAccessException|InvocationTargetException e) {
								throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
							}
						}
						: in -> {
							try {
								return (String) m.invoke(in);
							} catch (IllegalAccessException|InvocationTargetException e) {
								throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
							}
						};
				return noExWrap(m, a, ParseDir.TOS, f);
			} else if (strategy==To.TO_STRING_METHOD) {
				return (Function) DEFAULT_TOS.andThen(Try::ok);
			} else if (strategy==To.FX) {
				return in -> ok(FX.toS(in));
			} else {
				throw new SwitchException(strategy);
			}
		}
		return null;
	}

	private static Method getValueOfStatic(Class type) {

		// DAMN, is this really needed? If no - discard. If it is, make sure its handled in other
		// methods as well...
		// hadle enum with class bodies that dont identify as enums
		// simply fool the parser by changing the class to the enum
		// note: getDeclaringClass() does not seem to work here though
		if (type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
			type = type.getEnclosingClass();

		try {
			return type.getDeclaredMethod("valueOf", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Method getMethodStatic(String name, Class<?> type) {
		try {
			Method m = type.getDeclaredMethod(name, String.class);
			if (!m.getReturnType().equals(type)) throw new NoSuchMethodException();
			return m;
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static <I, O> Function<I,Try<O,String>> parserOfI(Invokable<O> m, Class<I> itype, Class<O> otype, StringParseStrategy a, ParseDir dir) {
		Collection<Parameter> params = m.getParameters();
		if (params.size()>1)
			throw new IllegalArgumentException("Parser method/constructor must take 0 or 1 parameter");

		// exceptions (we will make union of those annotated and those known to be thrown
		Set<Class<?>> ecs = new HashSet<>();
		if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
		else ecs.add(Exception.class);
		if (m!=null) ecs.addAll(m.getExceptionTypes());

		boolean no_input = params.isEmpty();
		return no_input
				? in -> {
					try {
						return ok(m.invoke((Object[]) null));
					} catch (IllegalAccessException|InvocationTargetException e) {
						for (Class<?> ec : ecs) {
							if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
								return errorOf(e.getCause().getCause());
							if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
							if (ec.isInstance(e)) return errorOf(e);
						}
						throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
					}
				}
				: in -> {
					try {
						return ok(m.invoke(null, in));
					} catch (IllegalAccessException|InvocationTargetException e) {
						for (Class<?> ec : ecs) {
							if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
								return errorOf(e.getCause().getCause());
							if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
							if (ec.isInstance(e)) return errorOf(e);
						}
						throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
					}
				};
	}

	private static <I, O> Function<I,Try<O,String>> parserOfM(Method m, Class<I> i, Class<O> o, StringParseStrategy a, ParseDir dir) {
		Set<Class<?>> ecs = new HashSet<>();
		if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
		else ecs.add(Exception.class);
		if (m!=null) ecs.addAll(list(m.getExceptionTypes()));
		boolean isSupplier = i==Void.class || i==void.class || i==null;
		boolean isStatic = Modifier.isStatic(m.getModifiers());
		return isSupplier
				? in -> {
					try {
						return ok((O) m.invoke(null));
					} catch (IllegalAccessException|InvocationTargetException e) {
						for (Class<?> ec : ecs) {
							if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
								return errorOf(e.getCause().getCause());
							if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
							if (ec.isInstance(e)) return errorOf(e);
						}
						throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
					}
				}
				: in -> {
					try {
						return ok((O) m.invoke(null, in));
					} catch (IllegalAccessException|InvocationTargetException e) {
						for (Class<?> ec : ecs) {
							if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
								return errorOf(e.getCause().getCause());
							if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
							if (ec.isInstance(e)) return errorOf(e);
						}
						throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
					}
				};
	}

	private static <O> Function<String,Try<O,String>> parserOfC(StringParseStrategy a, ParseDir dir, Class<O> type, Class<?>... params) {
		try {
			Constructor<O> c = type.getConstructor(params);
			if (c==null) throw new NoSuchMethodException();
			boolean isInputParam = params.length==1;
			Set<Class<?>> ecs = new HashSet<>();
			if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
			if (c!=null) ecs.addAll(list(c.getExceptionTypes()));
			return in -> {
				try {
					Object[] p = isInputParam ? new Object[]{in} : new Object[]{};
					return ok(c.newInstance(p));
				} catch (ExceptionInInitializerError|InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
					for (Class<?> ec : ecs) {
						if (e.getCause()!=null && ec.isInstance(e.getCause().getCause())) {
							return errorOf(e.getCause().getCause());
						}
						if (ec.isInstance(e.getCause())) {
							return errorOf(e.getCause());
						}
						if (ec.isInstance(e)) {
							return errorOf(e);
						}
					}
					throw new RuntimeException("String '" + in + "' parsing failed to invoke constructor in class " + c.getDeclaringClass(), e);
				}
			};
		} catch (NoSuchMethodException|SecurityException e) {
			throw new RuntimeException("Parser cant find constructor suitable for parsing " + type + " with parameters" + Util.toS(", ", params), e);
		}
	}

	private static <I, O> Ƒ1<I,Try<O,String>> noExWrap(Executable m, StringParseStrategy a, ParseDir dir, Function<I,O> f) {
		Set<Class<?>> ecs = new HashSet<>();
		if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
		if (m!=null) ecs.addAll(list(m.getExceptionTypes()));
		return tryF(f, ecs);
	}

	private interface Invokable<I> {
		I invoke(Object... params) throws IllegalAccessException, InvocationTargetException;

		Collection<Parameter> getParameters();

		Collection<Class<? extends Throwable>> getExceptionTypes();

		static <T> Invokable<T> of(Constructor<T> c) {
			return new Invokable<>() {
				@Override
				public final T invoke(Object... params) throws IllegalAccessException, InvocationTargetException {
					try {
						// We must skip the null object receiver - 1st parameter
						return c.newInstance(stream(params).skip(1).toArray());
					} catch (IllegalArgumentException|InstantiationException e) {
						throw new InvocationTargetException(e);
					}
				}

				@Override
				public Collection<Parameter> getParameters() {
					return listRO(c.getParameters());
				}

				@Override
				public Collection<Class<? extends Throwable>> getExceptionTypes() {
					return (List) listRO(c.getExceptionTypes());
				}
			};
		}

		static <T> Invokable<T> ofStaticMethod(Method m) {
			return new Invokable<>() {
				@Override
				public T invoke(Object... params) throws IllegalAccessException, InvocationTargetException {
					return (T) m.invoke(null, params);
				}

				@Override
				public Collection<Parameter> getParameters() {
					return listRO(m.getParameters());
				}

				@Override
				public Collection<Class<? extends Throwable>> getExceptionTypes() {
					return (List) listRO(m.getExceptionTypes());
				}
			};
		}
	}

	private enum ParseDir {
		TOS, OFS
	}
}