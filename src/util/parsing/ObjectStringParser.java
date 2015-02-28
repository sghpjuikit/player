/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing;

import java.util.Collection;

/**
 *
 * @author Plutonium_
 */
public interface ObjectStringParser extends StringParser<Object> {
    
    
    /** {@inheritDoc} */
    @Override
    default Collection<Class> getSupportedClasses() {
        throw new UnsupportedOperationException("Class must be specified before parsing");
    }    

    /**
     * 
     * @param type Must be primitive type or wrapper of primitive type or enum
     * or String.
     * @param source
     * @throws UnsupportedOperationException if class type not supported.
     */
    public Object fromS(Class type, String source);
    
    /** {@inheritDoc} */
    @Override
    default String toS(Object object) {
        throw new UnsupportedOperationException("Class must be specified before parsing");
    }
    
    /** {@inheritDoc} */
    @Override
    default String fromS(String source) {
        throw new UnsupportedOperationException("Class must be specified before parsing");
    }
    
}
