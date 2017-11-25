package sp.it.pl.service.database

import sp.it.pl.core.CoreSerializer
import java.io.Serializable
import java.util.*

/** Map of string collections. Singleton. */
class StringStore: Serializable {
    private val pool = HashMap<String, HashSet<String>>()

    fun getStrings(name: String) = pool.computeIfAbsent(name.toLowerCase(), { HashSet() })

    fun modify(modifier: (StringStore) -> Unit) {
        modifier(this)
        CoreSerializer.writeSingleStorage(this)
    }

    fun addString(name: String, s: String) {
        val wasChanged = getStrings(name.toLowerCase()).add(s)
        if (wasChanged) {
            CoreSerializer.writeSingleStorage(this)
        }
    }

    fun addStrings(name: String, s: List<String>) {
        val wasChanged = getStrings(name.toLowerCase()).addAll(s)
        if (wasChanged) {
            CoreSerializer.writeSingleStorage(this)
        }
    }

}