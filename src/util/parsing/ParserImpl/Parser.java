package util.parsing.ParserImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import static util.Util.getMethod;
import util.parsing.*;
import util.parsing.StringParseStrategy.From;
import static util.parsing.StringParseStrategy.From.*;
import util.parsing.StringParseStrategy.To;

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
    
    private static final PrimitiveParser prim_parser = new PrimitiveParser();
    private static final ValueOfParser valOfParser = new ValueOfParser();
    private static final FromStringParser fromStrParser = new FromStringParser();
    
    private static final Map<Class,StringParser> parsers = new HashMap();
    private static final Map<Class,ObjectStringParser> parsersO = new HashMap();
    
    static {        
        registerConverter(new FontParser());
        registerConverter(new FileParser());
        registerConverter(new ColorParser());
        registerConverter(new PrimitiveParser());
        registerConverter(new StringStringParser());
    }
    
    public static<T> void registerConverter(StringParser<T> parser) {
        parser.getSupportedClasses().forEach(c -> parsers.put(c, parser));
    }
    
    public static<T> void registerConverter(ObjectStringParser parser) {
        parser.getSupportedClasses().forEach(c -> parsersO.put(c, parser));
    }
    
    public static boolean supports(Class type) {
        return parsers.get(type)==null || 
                    type.isEnum() ||
                        valOfParser.supports(type) || 
                            fromStrParser.supports(type);
    }
    
    public static StringParser getParserS(Class type) {
        return parsers.get(type);
    }
    
    public static ObjectStringParser getParserO(Class type) {
        if(parsersO.containsKey(type)) return parsersO.get(type);
        if(type.isEnum()) return prim_parser;
        if(valOfParser.supports(type)) return valOfParser;
        if(fromStrParser.supports(type)) return fromStrParser;
        return null;
    }
    
    /**
     * Parses a string to specified type.
     * 
     * @param c
     * @param o
     * @return Object of specified type parsed from string
     * @throws UnsupportedOperationException if class type not supported.
     */
    public static<T> T fromS(Class<T> c, String o) {
        
        
        StringParseStrategy a = c.getAnnotation(StringParseStrategy.class);
        if(a!=null) {
            From strategy = a.from();
            if(strategy==VALUE_OF_METHOD) {
                return (T) valOfParser.fromS(c, o);
            } else if (strategy==FROM_STRING_METHOD) {
                return (T) fromStrParser.fromS(c, o);
            } else if (strategy==From.NONE) {
                throw new IllegalArgumentException("Object not parsable from string: " + o);
            } else if (strategy==From.ANNOTATED_METHOD) {
                Method m = getMethod(c,ParsesFromString.class);
                if(m==null || m.getParameterCount()!=1 || m.getParameters()[0].getType().equals(String.class))
                    throw new IllegalArgumentException("Object not parsable from string, because responsible method was not found: " + o);
                else {
                    try {
                        return (T) m.invoke(null,o);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new RuntimeException("Object parsing failed to invoke method: " + o, ex);
                    }
                }
            } else if (strategy==CONSTRUCTOR) {
                try {
                    Constructor<T> cn = c.getConstructor();
                    return cn.newInstance();
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new RuntimeException("Object parsing failed to invoke constructor: " + o, ex);
                }
            } else if (strategy==CONSTRUCTOR_STR) {
                try {
                    Constructor<T> cn = c.getConstructor(String.class);
                    return cn.newInstance(o);
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new RuntimeException("Object parsing failed to invoke constructor: " + o, ex);
                }
            }
        }
        
        
        
        ObjectStringParser po = getParserO(c);
        if(po!=null) return (T) po.fromS(c, o);
        
        StringParser ps = getParserS(c);
        if(ps!=null) return (T) ps.fromS(o);
        
        throw new UnsupportedOperationException("Unsupported class for parsing");
    }
    
    /** 
     * Converts object to String.
     * 
     * @param o Object to parse. Must not be null.
     * @throws UnsupportedOperationException if class type not supported.
     * @throws NullPointerException if parameter null
     */
    public static <T> String toS(T o) {
        Class<?> c = o.getClass();
        
        StringParseStrategy a = c.getAnnotation(StringParseStrategy.class);
        if(a!=null) {
            To strategy = a.to();
            if(strategy==To.CONSTANT) {
                return a.constant();
            } else if (strategy==To.NONE) {
                throw new IllegalArgumentException("Object not parsable to string: " + o);
            } else if (strategy==To.ANNOTATED_METHOD) {
                Method m = getMethod(c,ParsesToString.class);
                if(m==null || m.getParameterCount()!=0)
                    throw new IllegalArgumentException("Object not parsable to string, because responsible method was not found: " + o);
                else {
                    try {
                        return (String) m.invoke(o);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new RuntimeException("Object parsing failed to invoke method: " + o, ex);
                    }
                }
            } else if (strategy==To.TO_STRING_METHOD) {
                o.toString();
            }
        }

        return parsers.getOrDefault(c,getParserO(c)).toS(o);
    }
    
}
