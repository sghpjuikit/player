package sp.it.pl.util.parsing;

import sp.it.pl.util.functional.Try;

/** String to Object converter. */
public interface ConverterFromString<T> {

    /** @return object converted from the text or error */
    Try<T,String> ofS(String s);

    /** @return whether string can be parsed into the object of specified type successfully */
    default boolean isValid(String s) {
        return ofS(s).isOk();
    }

}