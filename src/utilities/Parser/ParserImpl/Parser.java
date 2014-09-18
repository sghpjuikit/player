package utilities.Parser.ParserImpl;

import java.util.HashMap;
import java.util.Map;
import utilities.Parser.ObjectStringParser;
import utilities.Parser.StringParser;

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
     * @param type
     * @param value
     * @return Object of specified type parsed from string
     * @throws UnsupportedOperationException if class type not supported.
     */
    public static<T> T fromS(Class<T> type, String value) {
        ObjectStringParser po = getParserO(type);
        if(po!=null) return (T) po.fromS(type, value);
        
        StringParser ps = getParserS(type);
        if(ps!=null) return (T) ps.fromS(value);
        
        throw new UnsupportedOperationException("Unsupported class for parsing");
    }
    
    /** 
     * Converts object to String.
     * 
     * @param o Object to parse. Must not be null.
     * @throws UnsupportedOperationException if class type not supported.
     * @throws NullPointerException if parameter null
     */
    public static String toS(Object o) {
        Class type = o.getClass();
        return parsers.getOrDefault(type,getParserO(type)).toS(o);
    }
    
}
