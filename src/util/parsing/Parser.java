package util.parsing;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
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

import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import Configuration.Configurable;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.itemnode.StringSplitParser;
import util.SwitchException;
import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;
import util.parsing.StringParseStrategy.From;
import util.parsing.StringParseStrategy.To;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.joining;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import static util.Util.getConstructorAnnotated;
import static util.Util.getMethodAnnotated;
import static util.dev.Util.noØ;
import static util.functional.Util.*;

/**
 * Multi type bidirectional object-string converter.
 *
 * @author Plutonium_
 */
public abstract class Parser {

    /**
     * Converts string s to object o of type c.
     * Null input not allowed.
     * Null output if and only if error occurs.
     *
     * @param c specifies type of object
     * @param s string to parse
     * @return parsing result or null if not parsable
     * @throws NullPointerException if any parameter null
     */
    abstract public <T> T fromS(Class<T> c, String s);

    /**
     * Converts object to String.
     * Null input not allowed.
     * Null output if and only if error occurs.
     *
     * @param o object to parse
     * @return parsed string or null if not parsable
     * @throws NullPointerException if parameter null
     */
    abstract public <T> String toS(T o);

    public <T> Predicate<String> isParsable(Class<T> c) {
        return s -> fromS(c, s)==null;
    }

    public <T> StringConverter<T> toConverter(Class<T> c) {
        return new StringConverter<T>() {
            @Override
            public String toS(T object) {
                return Parser.this.toS(object);
            }

            @Override
            public T fromS(String source) {
                return Parser.this.fromS(c, source);
            }
        };
    }

/******************************************************************************/

    private static final String DELIMITER_CONFIG_VALUE = "-";
    private static final String DELIMITER_CONFIG_NAME = ":";
    private static final String CONSTANT_NULL = "<NULL>";

    /** Default to string parser, which calls objects toString() or returns null constant. */
    public static final Function<Object,String> DEFAULT_TOS = o -> o==null ? CONSTANT_NULL : o.toString();
    /** Default from string parser. Always returns null. */
    public static final Function<String,Object> DEFAULT_FROM = o -> null;
    /**
     * Fx parser.
     * Used when {@link StringParseStrategy.To#FX} or {link StringParseStrategy.From#FX}.
     * <p>
     * The parser converts object to string that contains exact object class and name-value pairs
     * of all its javafx property beans. The parser converts back to object by invoking the
     * no argument constructor (which must be accessible) of the class found in the string and
     * setting the bean values in the string to respective javafx beans of the object.
     * <p>
     * The exact string format is subject to change. Dont rely on it.
     */
    public static final Parser FX = new Parser() {

        @Override
        public <T> T fromS(Class<T> type, String text) {
            // improve performance by not creating any Config/Configurable, premature optimization
            try {
                String[] vals = text.split(DELIMITER_CONFIG_VALUE);
                Class<?> objecttype = Class.forName(vals[0]);
                if(type!=null && !type.isAssignableFrom(objecttype)) throw new Exception(); // optimization, avoids next line
                T v = (T) objecttype.newInstance();
                Configurable c = Configurable.configsFromFxPropertiesOf(v);
                stream(vals).skip(1)
                            .forEach(str -> {
                                try {
                                    String[] nameval = str.split(DELIMITER_CONFIG_NAME);
                                    if(nameval.length!=2) return; // ignore
                                    String name = nameval[0], val = nameval[1];
                                    c.setField(name, val);
                                } catch(Exception e){
                                    // If for whatever reason setting value fails, we move onto another
                                    // instead crashing whole object parsing and returning null. If we
                                    // cant parse everything properly, at least we return some instance.
                                }
                            });
                return v;
            } catch(Exception e) {
                // e.printStackTrace(); // debug, in production we ignore unparsable values
                return null;
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
    /** Default to/from string parser with get-go support for several classes. */
    public static final DefaultParser DEFAULT = new DefaultParser();

    static {
        Class<? extends Throwable> nfe = NumberFormatException.class;
        Class<? extends Throwable> iae = IllegalArgumentException.class;
        Class<? extends Throwable> obe = IndexOutOfBoundsException.class;

        DEFAULT.addParser(Boolean.class,DEFAULT_TOS,Boolean::valueOf);
        DEFAULT.addParser(boolean.class,String::valueOf,Boolean::valueOf);
        DEFAULT.addParser(Integer.class,DEFAULT_TOS,noEx(Integer::valueOf,nfe));
        DEFAULT.addParser(int.class,String::valueOf,noEx(Integer::valueOf,nfe));
        DEFAULT.addParser(Double.class,DEFAULT_TOS,noEx(Double::valueOf,nfe));
        DEFAULT.addParser(double.class,String::valueOf,noEx(Double::valueOf,nfe));
        DEFAULT.addParser(Short.class,DEFAULT_TOS,noEx(Short::valueOf,nfe));
        DEFAULT.addParser(short.class,String::valueOf,noEx(Short::valueOf,nfe));
        DEFAULT.addParser(Long.class,DEFAULT_TOS,noEx(Long::valueOf,nfe));
        DEFAULT.addParser(long.class,String::valueOf,noEx(Long::valueOf,nfe));
        DEFAULT.addParser(Float.class,DEFAULT_TOS,noEx(Float::valueOf,nfe));
        DEFAULT.addParser(float.class,String::valueOf,noEx(Float::valueOf,nfe));
        DEFAULT.addParser(Character.class,DEFAULT_TOS,noEx(s -> s.charAt(0),obe));
        DEFAULT.addParser(char.class,String::valueOf,noEx(s -> s.charAt(0),obe));
        DEFAULT.addParser(Byte.class,DEFAULT_TOS,noEx(Byte::valueOf,nfe));
        DEFAULT.addParser(byte.class,String::valueOf,noEx(Byte::valueOf,nfe));
        DEFAULT.addParser(String.class, s -> s, s -> s);
        DEFAULT.addParser(StringSplitParser.class,DEFAULT_TOS, noEx(StringSplitParser::new, iae));
        DEFAULT.addParser(Year.class,DEFAULT_TOS, noEx(Year::parse, DateTimeParseException.class));
        DEFAULT.addParser(File.class,DEFAULT_TOS,File::new);
        DEFAULT.addParser(URI.class,DEFAULT_TOS, noEx(URI::create, iae));
        DEFAULT.addParser(Pattern.class,DEFAULT_TOS, noEx(Pattern::compile, PatternSyntaxException.class));
        DEFAULT.addParser(Font.class,
            f -> String.format("%s, %s", f.getName(),f.getSize()),
            noEx(Font.getDefault(), s -> {
                int i = s.indexOf(',');
                String name = s.substring(0, i);
                FontPosture style = s.toLowerCase().contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = s.toLowerCase().contains("bold") ? BOLD : NORMAL;
                double size = parseDouble(s.substring(i+2));
                return Font.font(name, weight, style, size);
            }, nfe,obe)
        );
        DEFAULT.addParser(LocalDateTime.class,DEFAULT_TOS,noEx(LocalDateTime::parse, DateTimeException.class));
        DEFAULT.addParserFromS(Duration.class, noEx(s -> Duration.valueOf(s.replaceAll(" ", "")), iae)); // fixes java's inconsistency
        DEFAULT.addParserToS(FontAwesomeIcon.class,FontAwesomeIcon::name);
        DEFAULT.addParser(Effect.class, FX::toS, text -> FX.fromS(Effect.class, text));
    }

/******************************************************************************/


    /**
     * Default parser implementation storing individual type parsers in a map. This means:
     * <ul>
     * <li> O{1) parser lookup.
     * <li> Single initialization. No parser is ever initialized more than once.
     * <li> Lazy initialization. Individual parsers are initialized only on parsing request. Until
     * then no instance is created.
     * </ul>
     * <p>
     * Only one parser can be registered per single class. But the same parser instance can be
     * reused across subclasses.
     * <p>
     * Parser lookup is polymorphic. This means that if there is no parser for given type, parser
     * for any supertype or interface that type extends will be looked up as well. Hence adding
     * parser for given type adds it for the entire subclass hierarchy.
     * <p>
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
     * <p>
     * This class acts as a generic parser. It:
     * <ul>
     * <li> tries to build parser if none is available looking for {@link StringParseStrategy}
     * and valueOf(), fromString() methods.
     * <li> if no parser is available, toString() method is used for to string parsing
     * and an error parser always producing null for string to object parsing.
     * </ul>
     * <p>
     * For registering a parser, there are two options:
     * <ul>
     * <li> Use {@link StringParseStrategy} and let the parser be created and
     * registered automatically (lazily and only once) and keeps the parsing code within the scope
     * of the parsed class. If the method throws an exception, it is important to let
     * this know, either by using throws clause or define it in an annotation. Using
     * both is unnecessary, but recommended.
     * <p>
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

        private final ClassMap<Function<?,String>> parsersToS = new ClassMap<>();
        private final ClassMap<Function<String,?>> parsersFromS = new ClassMap<>();
        private String error = "";

        public <T> void addParser(Class<T> c, StringConverter<T> parser) {
            addParser(c, parser::toS, parser::fromS);
        }

        public <T> void addParser(Class<T> c, Function<? super T,String> to, Function<String,? super T> from) {
            addParserToS(c, to);
            addParserFromS(c, from);
        }

        public <T> void addParserToS(Class<T> c, Function<? super T,String> parser) {
            parsersToS.put(c, parser);
        }

        public <T> void addParserFromS(Class<T> c, Function<String,? super T> parser) {
            parsersFromS.put(c, parser);
        }

        @Override
        public <T> T fromS(Class<T> c, String s) {
            noØ(c,"Parsing type must be specified!");
            noØ(s,"Parsing null not allowed!");
            if(CONSTANT_NULL.equals(s)) return null;
            return getParserFromS(c).apply(s);
        }

        @Override
        public <T> String toS(T o) {
            if(o==null) return CONSTANT_NULL;
            String s = getParserToS((Class<T>)o.getClass()).apply(o);
            return noNull(s,CONSTANT_NULL);
        }

        public String getError() {
            return noNull(error,"");
        }

        private <T> Function<String,T> getParserFromS(Class<T> c) {
            return parsersFromS.computeIfAbsent(c, this::findFromSparser);
        }

        private <T> Function<T,String> getParserToS(Class<T> c) {
            return parsersToS.computeIfAbsent(c, this::findToSparser);
        }

        private <T> Function<String,? super T> findFromSparser(Class<T> c) {
            return (Function) noNull(
                () -> parsersFromS.getElementOfSuper(c),
                () -> buildFromsParser(c),
                () -> DEFAULT_FROM
            );
        }

        private <T> Function<? super T,String> findToSparser(Class<T> c) {
            return (Function) noNull(
                () -> parsersToS.getElementOfSuper(c),
                () -> buildTosParser(c),
                () -> DEFAULT_TOS
            );
        }
    }

/******************************************************************************/

    private static <T> Function<String,T> buildFromsParser(Class<T> t) {
        Function<String,T> fromS = null;
        StringParseStrategy a = t.getAnnotation(StringParseStrategy.class);

        if(fromS==null && a!=null) {
            From strategy = a.from();
            if (strategy==From.NONE) {
                throw new IllegalArgumentException("Failed to create from string converter. Class '"+ t +"'s parsing strategy forbids parsing from string.");
            } else if (strategy==From.ANNOTATED_METHOD) {
                Invokable<T,Object> invokableAny = null;  // in class T returns ?

                if(invokableAny==null) {
                    Constructor<T> c = getConstructorAnnotated(t, ParsesFromString.class);
                    if(c!=null) invokableAny = (Invokable) TypeToken.of(t).constructor(c);
                }

                if(invokableAny==null) {
                    Method m = getMethodAnnotated(t, ParsesFromString.class);
                    if(m!=null) invokableAny = TypeToken.of(t).method(m);
                    if(!invokableAny.isStatic()) invokableAny = null;
                }

                if(invokableAny==null)
                    throw new IllegalArgumentException("Failed to create from string converter. Responsible method was not found");
                if(!invokableAny.getReturnType().isSubtypeOf(t))
                    throw new IllegalArgumentException("Failed to create from string converter. Responsible method returns bad type");

                Invokable<T,T> invokable = invokableAny.returning(t); // in class T returns T
                fromS = parserOfI(invokable, String.class, t, a, ParseDir.FROMS);
            } else if (strategy==From.FX) {
                fromS = text -> FX.fromS(t, text);
            } else {
                throw new SwitchException(strategy);
            }
        }

        // try to fall back to valueOf or fromString parsers
        if(fromS==null) {
            Method m = getValueOfStatic(t);
            if(m!=null) fromS = noEx(parserOfM(m, String.class, t, null, null), Exception.class);
        }
        if(fromS==null) {
            Method m = getMethodStatic("fromString",t);
            if(m!=null) fromS = noEx(parserOfM(m, String.class, t, null, null), Exception.class);
        }

        return fromS;
    }

    private static <T> Function<T,String> buildTosParser(Class<T> c) {
        Function<T,String> toS = null;
        StringParseStrategy a = c.getAnnotation(StringParseStrategy.class);

        if(toS==null && a!=null) {
            To strategy = a.to();
            if(strategy==To.CONSTANT) {
                String constant = a.constant();
                toS = in -> constant;
            } else if (strategy==To.NONE) {
                throw new IllegalArgumentException("Failed to create to string converter. Class '"+ c +"'s parsing strategy forbids parsing to string.");
            } else if (strategy==To.ANNOTATED_METHOD) {
                Method m = getMethodAnnotated(c,ParsesToString.class);
                if(m==null || m.getReturnType()!=String.class || m.getParameterCount()>1 || (m.getParameterCount()==1 && !m.getParameterTypes()[0].isAssignableFrom(c)))
                    throw new IllegalArgumentException("Failed to create to string converter. Class not parsable to string, because suitable method was not found: " + m);
                boolean pass_params = m.getParameterCount()==1;
                Function<T,String> f = pass_params
                    ? in -> {
                        try {
                            return (String) m.invoke(in,in);
                        } catch( IllegalAccessException | InvocationTargetException e ) {
                            throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                        }
                    }
                    : in -> {
                        try {
                            return (String) m.invoke(in);
                        } catch( IllegalAccessException | InvocationTargetException e ) {
                            throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                        }
                    };
                toS = noExWrap(m, a, ParseDir.TOS, f);
            } else if (strategy==To.TO_STRING_METHOD) {
                toS = (Function)DEFAULT_TOS;
            } else if (strategy==To.FX) {
                toS = FX::toS;
            } else {
                throw new SwitchException(strategy);
            }
        }

        return toS;
    }

    private static Method getValueOfStatic(Class type) {

        // DAMN, is this really needed? If no - discard. If it is, make sure its handled in other
        // methods as well...
        // hadle enum with class bodies that dont identify as enums
        // simply fool the parser by changing the class to the enum
        // note: getDeclaringClass() does not seem to work here though
        if(type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = type.getEnclosingClass();

        try {
            return type.getDeclaredMethod("valueOf", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
        } catch ( NoSuchMethodException ex) {
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

    private static <I,O> Function<I,O> parserOfI(Invokable<?,O> m, Class<I> itype, Class<O> otype, StringParseStrategy a, ParseDir dir) {
        Collection<Parameter> params = m.getParameters();
        if(params.size()>1)
            throw new IllegalArgumentException("Parser method/constructor must take 0 or 1 parameter");

        // exceptions (we will make union of those annotated and those known to be thrown
        Set<Class<?>> ecs = new HashSet<>();
        if(a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom())); else ecs.add(Exception.class);
        if(m!=null) ecs.addAll(map(m.getExceptionTypes(),tt -> tt.getRawType()));

        boolean no_input = params.isEmpty();
        Function<I,O> f = no_input
            ?   in -> {
                    try {
                        return m.invoke(null);
                    } catch(IllegalAccessException | InvocationTargetException e ) {
                        for(Class<?> ec : ecs) {
                            if(e.getCause()!=null && ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                    }
                }
            :   in -> {
                    try {
                        return m.invoke(null, in);
                    } catch(IllegalAccessException | InvocationTargetException e ) {
                        for(Class<?> ec : ecs) {
                            if(e.getCause()!=null && ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                    }
                };

        return noEx(f, ecs);
    }

    private static <I,O> Function<I,O> parserOfM(Method m, Class<I> i, Class<O> o, StringParseStrategy a, ParseDir dir) {
        Set<Class<?>> ecs = new HashSet<>();
        if(a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom())); else ecs.add(Exception.class);
        if(m!=null) ecs.addAll(list(m.getExceptionTypes()));
        boolean isSupplier = i==Void.class || i==void.class || i==null;
        boolean isStatic = Modifier.isStatic(m.getModifiers());
        Function<I,O> f = isSupplier
            ?   in -> {
                    try {
                        return (O) m.invoke(null);
                    } catch(IllegalAccessException | InvocationTargetException e ) {
                        for(Class<?> ec : ecs) {
                            if(e.getCause()!=null && ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                    }
                }
            :   in -> {
                    try {
                        return (O) m.invoke(null, in);
                    } catch(IllegalAccessException | InvocationTargetException e ) {
                        for(Class<?> ec : ecs) {
                            if(e.getCause()!=null && ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                    }
                };
        return noExWrap(m, a, dir, f);
    }

    private static <O> Function<String,O> parserOfC(StringParseStrategy a, ParseDir dir, Class<O> type, Class<?>... params) {
        try {
            Constructor<O> cn = type.getConstructor(params);
            boolean passinput = params.length==1;
            Set<Class<?>> ecs = new HashSet<>();
            if(a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
            if(cn!=null) ecs.addAll(list(cn.getExceptionTypes()));
                Function<String,O> f = in -> {
                    try {
                        Object[] p = passinput ? new Object[]{in} : new Object[]{};
                        return cn.newInstance(p);
                    } catch (ExceptionInInitializerError | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        for(Class<?> ec : ecs) {
                            if(e.getCause()!=null && ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("String '"+in+"' parsing failed to invoke constructor in class " + cn.getDeclaringClass(), e);
                    }
                };
              return noExWrap(cn, a, dir, f);
          } catch (NoSuchMethodException | SecurityException e) {
              throw new RuntimeException("Parser cant find constructor suitable for parsing " + type, e);
          }
    }

    private static <I,O> Function<I,O> noExWrap(Executable m, StringParseStrategy a, ParseDir dir, Function<I,O> f) {
        Set<Class<?>> ecs = new HashSet<>();
        if(a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
        if(m!=null) ecs.addAll(list(m.getExceptionTypes()));
        return noEx(f, ecs);
    }

    // these noEx methods must absolutely remain private, since it uses DEFAULT parser instance
    // this isnt quite well figured out with the error...

    private static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Collection<Class<?>> ecs) {
        return i -> {
            try {
                return f.apply(i);
            } catch(Exception e) {
                for(Class<?> ec : ecs)
                    if(ec.isAssignableFrom(e.getClass())) {
                        DEFAULT.error = e.getMessage();
                        return or;
                    }
                throw e;
            }
        };
    }

    private static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Class<?>... ecs) {
        return noEx(null, f, list(ecs));
    }

    private static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Collection<Class<?>> ecs) {
        return noEx(null, f, ecs);
    }

    private static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Class<?>... ecs) {
        return noEx(or, f, list(ecs));
    }

    private static enum ParseDir {
        TOS, FROMS
    }
}