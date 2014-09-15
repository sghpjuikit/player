/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.filtering;

import java.util.function.BiPredicate;

/**
 *
 * @author Plutonium_
 */
public enum StringPredicates implements Predicates<String> {
    IS,
    CONTAINS,
    ENDS_WITH,
    STARTS_WITH,
    IS_NOCASE,
    CONTAINS_NOCASE,
    ENDS_WITH_NOCASE,
    STARTS_WITH_NOCASE,
    REGEX;

    
    public BiPredicate<String, String> predicate() {
        return predicate(String.class);
    }
    
    @Override
    public BiPredicate<String, String> predicate(Class<String> type) {
        switch(this) {
            case IS: return (text,b) -> text.equals(b);
            case CONTAINS: return (text,b) -> text.contains(b);
            case ENDS_WITH: return (text,b) -> text.endsWith(b);
            case STARTS_WITH: return (text,b) -> text.startsWith(b);
            case IS_NOCASE: return (text,b) -> text.equalsIgnoreCase(b);
            case CONTAINS_NOCASE: return (text,b) -> text.toLowerCase().contains(b.toLowerCase());
            case ENDS_WITH_NOCASE: return (text,b) -> text.toLowerCase().endsWith(b.toLowerCase());
            case STARTS_WITH_NOCASE: return (text,b) -> text.toLowerCase().startsWith(b.toLowerCase());
            case REGEX: return (text,b) -> text.matches(b);
            default: throw new AssertionError();
        }
    }
    
    
}
