package sp.it.util.collections.map

import sp.it.util.collections.map.abstr.MapByKClass
import java.util.ArrayList
import java.util.function.Supplier
import kotlin.reflect.KClass

/**
 * Multiple value per key version of [KClassMap].
 */
class KClassListMap<E>(keyMapper: (E) -> KClass<*>): KCollectionMap<E, KClass<*>, MutableList<E>>(Supplier { ArrayList<E>() }, keyMapper), MapByKClass<E>