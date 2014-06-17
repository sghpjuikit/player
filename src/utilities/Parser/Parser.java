package utilities.Parser;

import Configuration.SkinEnum;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private static final FontParser font_parser = new FontParser();
    private static final ValueOfParser valOfParser = new ValueOfParser();
    private static final FromStringParser fromStrParser = new FromStringParser();
    
    private static final List<ObjectStringParser> parsersO = new ArrayList();
    private static final List<StringParser> parsersS = new ArrayList();
    
    static {
        parsersS.add(font_parser);
        parsersO.add(prim_parser);
        parsersO.add(valOfParser);
        parsersO.add(fromStrParser);
    }
    
    public static boolean supports(Class type) {
        return  type.equals(SkinEnum.class) ||
                type.equals(File.class) ||
                prim_parser.supports(type) ||
                font_parser.supports(type) ||
                valOfParser.supports(type) ||
                fromStrParser.supports(type);
    }
    
    /**
     * Parses a string to specified type.
     * @param type
     * @param value
     * @return Object of specified type parsed from string or null if any 
     * error. Null is always an error output, never valid.
     * @throws UnsupportedOperationException if class type not supported.
     */
    public static Object fromS(Class type, String value) {
        
        if (type.equals(SkinEnum.class)) return new SkinEnum(value);
        if (type.equals(File.class))        return new FileParser().fromS(value);
        if(prim_parser.supports(type)) return prim_parser.fromS(type, value);
        if(font_parser.supports(type)) return font_parser.fromS(value);
        if(fromStrParser.supports(type)) return fromStrParser.fromS(type, value);
        if(valOfParser.supports(type)) return valOfParser.fromS(type, value);
        
        throw new UnsupportedOperationException("Class type not supported");
    }
    
    /** 
     * Converts object to String.
     * @param o Object to parse. Must not be null.
     * @throws UnsupportedOperationException if class type not supported.
     * @throws NullPointerException if parameter null
     */
    public static String toS(Object o) {
        Objects.requireNonNull(o);
        Class type = o.getClass();
        
        if (type.equals(File.class))   return new FileParser().toS((File) o);
        
        for(StringParser p: parsersS)
            if (p.supports(type))
                return p.toS(o);
        for(ObjectStringParser p: parsersO)
            if (p.supports(type))
                return p.toS(o);
        
        throw new UnsupportedOperationException("Class type not supported");
    }
    
}
