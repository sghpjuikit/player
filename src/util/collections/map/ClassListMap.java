/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections.map;

import util.collections.map.abstr.MapByClass;
import java.util.function.Function;

/**
 * Multi value per key version of {@link ClassMap}
 * 
 * @author Plutonium_
 */
public class ClassListMap<E> extends ListMap<E,Class> implements MapByClass<E> {

    public ClassListMap(Function<E, Class> keyMapper) {
        super(keyMapper);
    }
    
}
