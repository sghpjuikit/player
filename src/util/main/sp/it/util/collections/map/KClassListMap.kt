package sp.it.util.collections.map

import java.util.function.Supplier
import kotlin.reflect.KClass

/**
 * Multiple value per key version of [KClassMap].
 */
class KClassListMap<E>(keyMapper: (E) -> KClass<*>): KCollectionMap<E, KClass<*>, MutableList<E>>(Supplier { ArrayList() }, keyMapper), MapByKClass<E>