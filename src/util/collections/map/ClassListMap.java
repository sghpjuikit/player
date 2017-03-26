package util.collections.map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import util.collections.map.abstr.MapByClass;

/**
 * Multi value per key version of {@link ClassMap}.
 *
 * @author Martin Polakovic
 */
public class ClassListMap<E> extends CollectionMap<E,Class<?>,List<E>> implements MapByClass<E> {

	public ClassListMap(Function<E,Class<?>> keyMapper) {
		super(ArrayList::new, keyMapper);
	}

}