package util.parsing;

import GUI.ItemNode.StringSplitParser;
import java.io.File;
import static java.lang.Double.parseDouble;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isStatic;
import java.net.URI;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import javafx.scene.text.FontWeight;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import static util.Util.getMethodAnnotated;
import static util.dev.Util.forbidNull;
import static util.functional.Util.*;
import util.parsing.StringParseStrategy.From;
import static util.parsing.StringParseStrategy.From.*;
import util.parsing.StringParseStrategy.To;

/**
 * Utility class - parser converting Objects to String and back. It stores
 * object parsers per class in a map.
 * <p>
 * Each parser must adhere to convention:
 * <lu>
 * <li> Parser must never assume anything about the input. To string parser can
 * receive an object of particular type in any state. From string parser can 
 * receive any String as input.
 * <li> Parser does not have to handle null inpu. It will never receive it. 
 * This is because the parsers are only used
 * indirectly using {@link #toS(java.lang.Object)} and {@link #fromS(java.lang.Class, java.lang.String)}.
 * <li> Parser must never throw an exception as a result of failed parsing,
 * instead return null. Null is not a valid parsing output value. It always 
 * indicates error.
 * <li> Only one parser can be registered per single class.
 * <lu>
 * <p>
 * This class acts as a generic parser. It:
 * <lu>
 * <li> tries to build parser if no is available looking for {@link StringParseStrategy}
 * and valueOf(), fromString() methods.
 * <li> if no parser is avalable, toString() method is used for to string parsing
 * and an error parser always producing null for string to object parsing.
 * <lu>
 * <p>
 * For registering a parser, there are two options:
 * <lu>
 * <li> Use {@link StringParseStrategy} and let the parser be created and
 * registered automatically. This is done lazily (and only once so there is no
 * performance setback) and keeps the parsing code within the scope of the class
 * (code cohesion). If the method throws an exception, it is important to let
 * this know, either by using throws clause or define it in an annotation. Using
 * both is unnecessary, but recommended.
 * <p>
 * This allows only one implementation, tight to the class of an object.
 * <li> Create parser and register it manually {@link #registerConverter(java.lang.Class, util.parsing.StringConverter)}.
 * It is recommended to register both to string and
 * from string, although it is not necessary if not needed. The parser must
 * return null if any problem occurs. This can be done in two ways - return null
 * when the problem occurs (and in catch blocks, no exception must be thrown!)
 * or throw an exception and wrap the parser function into noException function
 * wrapper {@link util.functional.Util#noEx(java.util.function.Function, java.lang.Class...)}.
 * <p>
 * This allows arbitrary implementation.
 * <lu>
 * 
 * @author Plutonium_
 */
public class Parser {
    
    private static final Map<Class,Function<?,String>> parsersToS = new HashMap();
    private static final Map<Class,Function<String,?>> parsersFromS = new HashMap();
    private static final Function<String,Object> errFP = o -> null;
    private static final Function<Object,String> errTP = toString;
    
    static {
        registerConverter(Font.class,
            f -> String.format("%s, %s", f.getName(), f.getSize()), 
            noEx(Font.getDefault(), s -> {
                int i = s.indexOf(',');
                String name = s.substring(0, i);
                FontPosture style = s.toLowerCase().contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = s.toLowerCase().contains("bold") ? BOLD : NORMAL;
                double size = parseDouble(s.substring(i+2));
                return Font.font(name, weight, style, size);
            }, NumberFormatException.class, IndexOutOfBoundsException.class)
        );
        Function<Object,String> sv = String::valueOf;
        
        registerConverter(File.class,toString,File::new);
        registerConverter(Boolean.class,toString,Boolean::valueOf);
        registerConverter(boolean.class,sv,Boolean::valueOf);
        registerConverter(Integer.class,toString,noEx(Integer::valueOf,NumberFormatException.class));
        registerConverter(int.class,sv,noEx(Integer::valueOf,NumberFormatException.class));
        registerConverter(Double.class,toString,noEx(Double::valueOf,NumberFormatException.class));
        registerConverter(double.class,sv,noEx(Double::valueOf,NumberFormatException.class));
        registerConverter(Short.class,toString,noEx(Short::valueOf,NumberFormatException.class));
        registerConverter(short.class,sv,noEx(Short::valueOf,NumberFormatException.class));
        registerConverter(Long.class,toString,noEx(Long::valueOf,NumberFormatException.class));
        registerConverter(long.class,sv,noEx(Long::valueOf,NumberFormatException.class));
        registerConverter(Float.class,toString,noEx(Float::valueOf,NumberFormatException.class));
        registerConverter(float.class,sv,noEx(Float::valueOf,NumberFormatException.class));
        registerConverter(Character.class,toString,noEx(s -> s.charAt(0),IndexOutOfBoundsException.class));
        registerConverter(char.class,sv,noEx(s -> s.charAt(0),IndexOutOfBoundsException.class));
        registerConverter(Byte.class,toString,noEx(Byte::valueOf,NumberFormatException.class));
        registerConverter(byte.class,sv,noEx(Byte::valueOf,NumberFormatException.class));
        registerConverter(String.class, s->s, s->s);
        registerConverter(StringSplitParser.class,toString, noEx(StringSplitParser::new, IllegalArgumentException.class));
        registerConverter(Year.class,toString, noEx(Year::parse, DateTimeParseException.class));
        registerConverter(URI.class,toString, noEx(URI::create, IllegalArgumentException.class));
        registerConverter(Pattern.class,toString, noEx(Pattern::compile, PatternSyntaxException.class));
        registerConverter(Pattern.class,toString, noEx(Pattern::compile, PatternSyntaxException.class));
    }
    
    public static<T> void registerConverter(Class<T> c, StringConverter<T> parser) {
        registerConverterToS(c, parser::toS);
        registerConverterFromS(c, parser::fromS);
    }
    
    public static<T> void registerConverter(Class<T> c, Function<? super T,String> to, Function<String,? super T> from) {
        registerConverterToS(c, to);
        registerConverterFromS(c, from);
    }
    
    public static<T> void registerConverterToS(Class<T> c, Function<? super T,String> parser) {
        parsersToS.put(c, parser);
    }
    
    public static<T> void registerConverterFromS(Class<T> c, Function<String,? super T> parser) {
        parsersFromS.put(c, parser);
    }
    
    
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
    public static <T> T fromS(Class<T> c, String s) {
        forbidNull(c,"Parsing type must be specified!");
        forbidNull(s,"Parsing null not allowed!");
        return getParserFromS(c).apply(s);
    }
    
    /** 
     * Converts object to String.
     * Null input not allowed. 
     * Null output if and only if error occurs. 
     * 
     * @param o object to parse
     * @return parsed string or null if not parsable
     * @throws NullPointerException if parameter null
     */
    public static <T> String toS(T o) {
        forbidNull(o,"Parsing null not allowed!");
        return getParserToS((Class<T>)o.getClass()).apply(o);
    }
    
    public static <T> Predicate<String> isParsable(Class<T> c) {
        return s -> fromS(c, s)==null;
    }
    
    public static <T> StringConverter<T> toConverter(Class<T> c) {
        return new StringConverter<T>() {
            @Override public String toS(T object) {
                return Parser.toS(object);
            }
            @Override public T fromS(String source) {
                return Parser.fromS(c, source);
            }
        };
    }
    
/******************************************************************************/
    
    /** @return parser or null if none available */
    private static <T> Function<T,String> getParserToS(Class<T> c) {
        if(!parsersToS.containsKey(c))
            registerConverterToS(c, noNull(buildTosParser(c), errTP));
        return (Function) parsersToS.get(c);
    }
    
    /** @return parser or null if none available */
    private static <T> Function<String,T> getParserFromS(Class<T> c) {
        if(!parsersFromS.containsKey(c))
            registerConverterFromS(c, noNull(buildFromsParser(c), errFP));
        return (Function) parsersFromS.get(c);
    }
    
/******************************************************************************/
    
    private static <T> Function<String,T> buildFromsParser(Class<T> c) {
        Function<String,?> fromS = null;
        StringParseStrategy a = c.getAnnotation(StringParseStrategy.class); 
        
        if(fromS==null && a!=null) {
            From strategy = a.from();
            if(strategy==VALUE_OF_METHOD) {
                Method m = getValueOfStatic(c);
                if(m==null) throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: "+c+".valueOf(String s)");
                fromS = parserOfM(m, String.class, c, a);
            } else if (strategy==FROM_STRING_METHOD) {
                Method m = getMethodStatic("fromString", c);
                if(m==null) throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: "+c+".fromString(String s)");
                fromS = parserOfM(m, String.class, c, a);
            } else if (strategy==From.NONE) {
                throw new IllegalArgumentException("Failed to create from string converter. Class '"+ c +"'s parsing strategy forbids parsing from string.");
            } else if (strategy==From.ANNOTATED_METHOD) {
                Method m = getMethodAnnotated(c,ParsesFromString.class);
                if(m==null || !isStatic(m.getModifiers()) || m.getParameterCount()!=1 || m.getParameters()[0].getType().equals(String.class))
                    throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: " + m);
                fromS = parserOfM(m, String.class, c, a);
            } else if (strategy==CONSTRUCTOR) {
                fromS = parserOfC(a, c);
            } else if (strategy==CONSTRUCTOR_STR) {
                fromS = parserOfC(a, c, String.class);
            }
        }
        
        // try to fall back to valueOf or fromString parsers
        if(fromS==null) {
            Method m = getValueOfStatic(c);
            if(m!=null) fromS = noEx(parserOfM(m, String.class, c, null), Exception.class);
        }
        if(fromS==null) {
            Method m = getMethodStatic("fromString",c);
            if(m!=null) fromS = noEx(parserOfM(m, String.class, c, null), Exception.class);
        }
        
        return (Function)fromS;
    }
    private static <T> Function<T,String> buildTosParser(Class<T> c) {
        Function<?,String> toS = null;
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
                if(m==null || m.getParameterCount()!=0 || !m.getReturnType().equals(String.class))
                    throw new IllegalArgumentException("Failed to create to string converter. Class not parsable to string, because responsible method was not found: " + m);
                Function<?,String> f = in -> {
                    try {
                        return (String) m.invoke(in);
                    } catch( IllegalAccessException | InvocationTargetException e ) {
                        throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
                    }
                };
                toS = noExWrap(m, a, f);

            } else if (strategy==To.TO_STRING_METHOD) {
                toS = toString;
            }
        }
        
        // always fall back to toStirng()
        if(toS==null) toS = toString;
        
        return (Function)toS;
    }
    
    
    private static Method getValueOfStatic(Class type) {
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
    
    private static Method getMethodStatic(String name, Class type) {
        try {
            Method m = type.getDeclaredMethod(name, String.class);
            if (!m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return m;
        } catch ( NoSuchMethodException ex) {
            return null;
        }
    }
    
    private static <I,O> Function<I,O> parserOfM(Method m, Class<I> i, Class<O> o, StringParseStrategy a) {       
        Set<Class<?>> ecs = new HashSet();
        if(a!=null) ecs.addAll(list(a.ex())); else ecs.add(Exception.class);
        if(m!=null) ecs.addAll(list(m.getExceptionTypes()));
        Function<I,O> f = in -> {
            try {
                return (O) m.invoke(null, in);
            } catch( IllegalAccessException | InvocationTargetException e ) {
                for(Class<?> ec : ecs) {
                    if(ec.isInstance(e.getCause().getCause())) return null;
                    if(ec.isInstance(e.getCause())) return null;
                    if(ec.isInstance(e)) return null;
                }
                throw new RuntimeException("Parser cant invoke the method: " + m, e.getCause());
            }
        };
        return noExWrap(m, a, f);
    }
    
    private static <O> Function<String,O> parserOfC(StringParseStrategy a, Class<O> type, Class... params) {        
        try {
            Constructor<O> cn = type.getConstructor(params);
            boolean passinput = params.length==1;
            Set<Class<?>> ecs = new HashSet();
            if(a!=null) ecs.addAll(list(a.ex()));
            if(cn!=null) ecs.addAll(list(cn.getExceptionTypes()));
              Function<String,O> f = in -> {
                    try {
                        Object[] p = passinput ? new Object[]{in} : new Object[]{};
                        return cn.newInstance(p);
                    } catch (ExceptionInInitializerError | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        for(Class<?> ec : ecs) {
                            if(ec.isInstance(e.getCause().getCause())) return null;
                            if(ec.isInstance(e.getCause())) return null;
                            if(ec.isInstance(e)) return null;
                        }
                        throw new RuntimeException("String '"+in+"' parsing failed to invoke constructor in class " + cn.getDeclaringClass(), e);
                    }
                };
              return noExWrap(cn, a, f);
          } catch (NoSuchMethodException | SecurityException e) {
              throw new RuntimeException("Parser cant find no param constructor in " + type, e);
          }
    }
    
    private static <I,O> Function<I,O> noExWrap(Executable m, StringParseStrategy a, Function<I,O> f) {
        Set<Class<?>> ecs = new HashSet();
        if(a!=null) ecs.addAll(list(a.ex()));
        if(m!=null) ecs.addAll(list(m.getExceptionTypes()));
        return noEx(f, ecs);
    }
}