package util.parsing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import static util.Util.getMethod;
import util.parsing.StringParseStrategy.From;
import static util.parsing.StringParseStrategy.From.*;
import util.parsing.StringParseStrategy.To;
import util.parsing.impl.ColorParser;
import util.parsing.impl.FileParser;
import util.parsing.impl.FontParser;
import util.parsing.impl.StringStringParser;

/**
 * 
 * Utility class - parser converting Objects to String and back.
 * 
 * There is no complete list of supported types as it depends on what types
 * particular used parsers can handle, but method is provided to check
 * whether the type is supported.
 *
 * @author Plutonium_
 */
public class Parser {
    
    private static final Function<?,String> OtoS = Object::toString;
    private static final Function<?,String> SvOf = String::valueOf;
    
    private static final Map<Class,StringParser> parsers = new HashMap();
    
    static {        
        registerConverter(new FontParser());
        registerConverter(new FileParser());
        registerConverter(new ColorParser());
        registerConverter(new StringStringParser());
    }
    
    public static<T> void registerConverter(StringParser<T> parser) {
        parser.getSupportedClasses().forEach(c -> parsers.put(c, parser));
    }
    
    /**
     * Parses a string to specified type.
     * 
     * @param c
     * @param o
     * @return Object of specified type parsed from string
     * @throws UnsupportedOperationException if class type not supported.
     */
    public static <T> T fromS(Class<T> c, String o) {
        return (T) getParser(c).fromS(o);
    }
    
    /** 
     * Converts object to String.
     * 
     * @param o Object to parse. Must not be null.
     * @throws UnsupportedOperationException if class type not supported.
     * @throws NullPointerException if parameter null
     */
    public static <T> String toS(T o) {
        return getParser(o.getClass()).toS(o);
    }
    
    
    public static <T> StringParser getParser(Class<T> c) {
        StringParser<T> p = parsers.get(c);
        if(p==null) {
            p = getOrBuildParser(c);
            parsers.put(c, p);
        }
        return p;
    }

    private static <T> StringParser getOrBuildParser(Class<T> c) {
        
        Function<String,?> fromS = null;
        Function<?,String> toS = null;

        StringParseStrategy a = c.getAnnotation(StringParseStrategy.class);
        
        // handle primitives
        if (c.equals(boolean.class))      fromS = Boolean::valueOf;
        else if (c.equals(Boolean.class)) fromS = Boolean::valueOf;
        else if (c.equals(int.class))     fromS = Integer::valueOf;
        else if (c.equals(Integer.class)) fromS = Integer::valueOf;
        else if (c.equals(double.class))  fromS = Double::valueOf;
        else if (c.equals(Double.class))  fromS = Double::valueOf;
        else if (c.equals(long.class))    fromS = Long::valueOf;
        else if (c.equals(Long.class))    fromS = Long::valueOf;
        else if (c.isEnum())              fromS = s -> Enum.valueOf((Class<Enum>)c, s);
        else if (c.equals(byte.class))    fromS = Byte::valueOf;
        else if (c.equals(Byte.class))    fromS = Byte::valueOf;
        else if (c.equals(short.class))   fromS = Short::valueOf;
        else if (c.equals(Short.class))   fromS = Short::valueOf;
        else if (c.equals(float.class))   fromS = Float::valueOf;
        else if (c.equals(Float.class))   fromS = Float::valueOf;
        else if (c.equals(String.class))  fromS = s -> s;
        
        if(fromS==null && a!=null) {
            From strategy = a.from();
            if(strategy==VALUE_OF_METHOD) {
                if(!hasValueOfMethod(c))
                    throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: "+c+".valueOf(String s)");
                fromS = s -> invokeValueOf(c, s);
            } else if (strategy==FROM_STRING_METHOD) {
                if(!hasMethod("fromString",c))
                    throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: "+c+".fromString(String s)");
                fromS = s -> invokeMethod("fromString",c,s);
            } else if (strategy==From.NONE) {
                throw new IllegalArgumentException("Failed to create from string converter. Class '"+ c +"'  has forbids parsing from string.");
            } else if (strategy==From.ANNOTATED_METHOD) {
                Method m = getMethod(c,ParsesFromString.class);
                if(m==null || m.getParameterCount()!=1 || m.getParameters()[0].getType().equals(String.class))
                    throw new IllegalArgumentException("Failed to create from string converter. Class not parsable from string, because responsible method was not found: " + m);
                else {
                    fromS = s -> {
                        try {
                            return (T) m.invoke(null,s);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            throw new RuntimeException("String '"+s+"' parsing failed to invoke static method: " + m, ex);
                        }
                    };
                }
            } else if (strategy==CONSTRUCTOR) {
                fromS = s -> {
                    try {
                        Constructor<T> cn = c.getConstructor();
                        return cn.newInstance();
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new RuntimeException("String '"+s+"' parsing failed to invoke no param constructor", ex);
                    }
                };
            } else if (strategy==CONSTRUCTOR_STR) {
                fromS = s -> {
                    try {
                        Constructor<T> cn = c.getConstructor(String.class);
                        return cn.newInstance(s);
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new RuntimeException("String '"+s+"' parsing failed to invoke String param constructor", ex);
                    }
                };
            } else {
            }
        }
        
        // fallback to valueOf or fromString parsers
        if(fromS==null && hasValueOfMethod(c)) fromS = s -> invokeValueOf(c,s);
        if(fromS==null && hasMethod("fromString",c)) fromS = s -> invokeMethod("fromString",c,s);
        
        
        // handle primitives
        if (c.equals(boolean.class))      toS = SvOf;
        else if (c.equals(Boolean.class)) toS = OtoS;
        else if (c.equals(int.class))     toS = SvOf;
        else if (c.equals(Integer.class)) toS = OtoS;
        else if (c.equals(double.class))  toS = SvOf;
        else if (c.equals(Double.class))  toS = OtoS;
        else if (c.equals(long.class))    toS = SvOf;
        else if (c.equals(Long.class))    toS = OtoS;
        else if (c.isEnum())              toS = OtoS;
        else if (c.equals(byte.class))    toS = SvOf;
        else if (c.equals(Byte.class))    toS = OtoS;
        else if (c.equals(short.class))   toS = SvOf;
        else if (c.equals(Short.class))   toS = OtoS;
        else if (c.equals(float.class))   toS = SvOf;
        else if (c.equals(Float.class))   toS = OtoS;
        else if (c.equals(String.class))  toS = s->(String)s;
        
        if(toS==null && a!=null) {
            To strategy = a.to();
            if(strategy==To.CONSTANT) {
                String constant = a.constant();
                toS = o -> constant;
            } else if (strategy==To.NONE) {
                throw new IllegalArgumentException("Failed to create to string converter. Class '"+ c +"' forbids parsing to string.");
            } else if (strategy==To.ANNOTATED_METHOD) {
                Method m = getMethod(c,ParsesToString.class);
                if(m==null || m.getParameterCount()!=0)
                    throw new IllegalArgumentException("Failed to create to string converter. Class not parsable to string, because responsible method was not found: " + m);
                else {
                    toS = o -> {
                        try {
                            return (String) m.invoke(o);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            throw new RuntimeException("Object parsing failed to invoke instance method: " + m + " on object: " + o, ex);
                        }
                    };
                }
            } else if (strategy==To.TO_STRING_METHOD) {
                toS = OtoS;
            }
        }
        // fallback to toString()
        if(toS==null) toS = OtoS;
        
        
        // throw if cant build parser
        if(toS==null) throw new IllegalStateException("Failed to create to string converter for class '"+c+"'");
        if(fromS==null) throw new IllegalStateException("Failed to create from string converter for class '"+c+"'");
        
        // build parser
        Function<String,T> fromSF = (Function)fromS;
        Function<T,String> toSF = (Function)toS;
        return new StringParser<T>() {
            public String toS(T o) {
                return toSF.apply(o);
            }
            public T fromS(String s) {
                return fromSF.apply(s);
            }
        };
    }
    
    
    private static boolean hasValueOfMethod(Class type) {
        // hadle enum with class bodies that dont identify as enums
        // simply fool the parser by changing the class to the enum
        // note: getDeclaringClass() does not seem to work here though
        if(type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = type.getEnclosingClass();
        
        try {
            Method m = type.getDeclaredMethod("valueOf", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return true;
        } catch ( NoSuchMethodException ex) {
            return false;
        }
    }

    private static <T> T invokeValueOf(Class<T> type, String source) {
        // hadle enum with class bodies that dont identify as enums
        // simply fool the parser by changing the class to the enum
        // note: getDeclaringClass() does not seem to work here though
        if(type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = (Class<T>) type.getEnclosingClass();
        
        //try parsing unknown types with valueOf(String) method if available
        try {
            Method m = type.getDeclaredMethod("valueOf", String.class);
            return (T) m.invoke(null, source);
        } catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
    
    private static boolean hasMethod(String name, Class type) {
        try {
            Method m = type.getDeclaredMethod(name, String.class);
            if (!m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return true;
        } catch ( NoSuchMethodException ex) {
            return false;
        }
    }

    private static <T> T invokeMethod(String name, Class<T> type, String source) {        
        try {
            Method m = type.getDeclaredMethod(name, String.class);
            return (T) m.invoke(null, source);
        } catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
