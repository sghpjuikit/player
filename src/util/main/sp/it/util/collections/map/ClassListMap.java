package sp.it.util.collections.map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import sp.it.util.collections.map.abstr.MapByClass;

/**
 * Multiple value per key version of {@link sp.it.util.collections.map.ClassMap}.
 */
public class ClassListMap<E> extends CollectionMap<E,Class<?>,List<E>> implements MapByClass<E> {

	public ClassListMap(Function<E,Class<?>> keyMapper) {
		super(ArrayList::new, keyMapper);
	}

}