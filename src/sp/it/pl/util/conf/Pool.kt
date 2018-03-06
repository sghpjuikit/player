package sp.it.pl.util.conf

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap


fun initStaticConfigs(type: Class<*>) {
    ConfigurableTypesPool.types += type
}

object ConfigurableTypesPool {
    @JvmField val types = Collections.newSetFromMap<Class<*>>(ConcurrentHashMap<Class<*>, Boolean>())
}