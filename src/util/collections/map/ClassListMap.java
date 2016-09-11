package util.collections.map;

import java.util.function.Function;

import util.collections.map.abstr.MapByClass;

/**
 * Multi value per key version of {@link ClassMap}
 * 
 * @author Martin Polakovic
 */
public class ClassListMap<E> extends ListMap<E,Class> implements MapByClass<E> {

    public ClassListMap(Function<E, Class> keyMapper) {
        super(keyMapper);
    }
    
}