/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.collections;

import java.util.ArrayList;

/**
 * Marker interface for List implementations extending concrete generic type of
 * ArrayList - this allows reflection to get a hold of the generic type in runtime.
 * <p>
 * @author Plutonium_
 */
public class PrimitiveList<T> extends ArrayList<T> {
    private static final long serialVersionUID = 31L;
    
}
