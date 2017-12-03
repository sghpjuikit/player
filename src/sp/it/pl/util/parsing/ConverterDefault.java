package sp.it.pl.util.parsing;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.collections.map.ClassMap;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.parsing.Parsers.Invokable;
import sp.it.pl.util.parsing.Parsers.ParseDir;
import sp.it.pl.util.parsing.StringParseStrategy.From;
import sp.it.pl.util.parsing.StringParseStrategy.To;
import static sp.it.pl.util.functional.Try.error;
import static sp.it.pl.util.functional.Try.ok;
import static sp.it.pl.util.functional.Util.noNull;
import static sp.it.pl.util.parsing.Parsers.getMethodStatic;
import static sp.it.pl.util.parsing.Parsers.getValueOfStatic;
import static sp.it.pl.util.parsing.Parsers.noExWrap;
import static sp.it.pl.util.parsing.Parsers.parserOfI;
import static sp.it.pl.util.parsing.Parsers.parserOfM;
import static sp.it.pl.util.type.Util.getConstructorAnnotated;
import static sp.it.pl.util.type.Util.getMethodAnnotated;

/**
 * Converter implementation storing individual type converters in a map. This means:
 * <ul>
 * <li> O(1) converter lookup.
 * <li> Single initialization. No converter is ever initialized more than once.
 * <li> Lazy initialization. No converter is initialized until needed.
 * </ul>
 * <p/>
 * Only one converter can be registered per single class.
 * <p/>
 * Converter lookup is polymorphic. This means that if there is no converter for given type, converter
 * for any supertype or interface the type extends will be looked up as well. Hence adding
 * converter for given type effectively adds it for the entire subclass hierarchy, unless overridden.
 * <p/>
 * Each converter must adhere to convention:
 * <ul>
 * <li> Converter must never assume anything about the input. To string converter can
 * receive an object of particular type in any state and any subtype. From string converter can
 * receive any String as input.
 * <li> Converter never receives null input. Null is handled by this converter itself, thus
 * consistently across all converters - always.
 * Parsing object from null string throws exception, because even null must be represented by
 * string.
 * Parsing null object to string produces null string constant (which when parsed produces null again)
 * <li> Converter must never throw an exception as a result of failed parsing, instead returns null.
 * </ul>
 * <p/>
 * This class acts as a generic converter. It:
 * <ul>
 * <li> tries to build converter if none is available looking for {@link StringParseStrategy}
 * and valueOf(), fromString() methods.
 * <li> if no converter is available, toString() method is used for to string parsing
 * and an error converter always producing null for string to object parsing.
 * </ul>
 * <p/>
 * For registering a converter, there are two options:
 * <ul>
 * <li> Use {@link StringParseStrategy} and let the converter be created and
 * registered automatically (lazily and only once) and keeps the parsing code within the scope
 * of the parsed class. If the method throws an exception, it is important to let
 * this know, either by using throws clause or define it in an annotation. Using
 * both is unnecessary, but recommended.
 * <p/>
 * This allows only one implementation, tightly coupled to the parsed object's class.
 * <li> Create converter and register it manually {@link #addParser(Class, ConverterString)}.
 * It is recommended (although not necessary) to register both to string and from string.
 * The converter must return null if any problem occurs.
 * </ul>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConverterDefault extends Converter {
	/** Null placeholder */
	public final String stringNull = "<NULL>";
	/** Default to string parser, which calls objects toString() or returns null constant. */
	public final Function<Object,String> defaultTos = o -> o==null ? stringNull : o.toString();
	/** Default from string parser. Always returns null. */
	public final Function<String,Try<Object,String>> defaultFromS = o -> error("Object has no from-text converter");

    private final ClassMap<Function<? super Object,Try<String,String>>> parsersToS = new ClassMap<>();
    private final ClassMap<Function<? super String,Try<Object,String>>> parsersFromS = new ClassMap<>();

    public <T> void addParser(Class<T> c, ConverterString<T> parser) {
        addParser(c, parser::toS, parser::ofS);
    }

    public <T> void addParser(Class<T> c, Function<? super T,String> to, Function<String,Try<T,String>> of) {
        addParserToS(c, to);
        addParserOfS(c, of);
    }

    public <T> void addParserAsF(Class<T> c, Function<? super T,String> to, Function<String,? extends T> of) {
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
    @NotNull
    @Override
    public <T> Try<T,String> ofS(@NotNull Class<T> c, @NotNull String s) {
        if (stringNull.equals(s)) return ok(null);
        return getParserOfS(c).apply(s);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public <T> String toS(T o) {
        if (o==null) return stringNull;
        String s = ((Function<T,Try<String,String>>) getParserToS(o.getClass())).apply(o).getOr(null);
        return noNull(s, stringNull);
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
            () -> defaultFromS
        );
    }

    @SuppressWarnings("unchecked")
    private <T> Function<? super T,Try<String,String>> findToSparser(Class<T> c) {
        return (Function) noNull(
            () -> parsersToS.getElementOfSuper(c),
            () -> buildToSParser(c),
            () -> defaultTos.andThen(Try::ok)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private <T> Function<String,Try<T,String>> buildOfSParser(Class<T> type) {
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
                ofS = text -> Parsers.FX.ofS(type, text);
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

    @SuppressWarnings("unchecked")
    private <T> Function<T,Try<String,String>> buildToSParser(Class<T> c) {
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
                                throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
                            }
                        }
                    : in -> {
                            try {
                                return (String) m.invoke(in);
                            } catch (IllegalAccessException|InvocationTargetException e) {
                                throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
                            }
                        };
                return noExWrap(m, a, ParseDir.TOS, f);
            } else if (strategy==To.TO_STRING_METHOD) {
                return (Function) defaultTos.andThen(Try::ok);
            } else if (strategy==To.FX) {
                return in -> ok(Parsers.FX.toS(in));
            } else {
                throw new SwitchException(strategy);
            }
        }
        return null;
    }

}