package services.database

import main.App.APP
import java.util.*
import javax.persistence.Entity

/** Map of string collections. Singleton. */
@Entity(name = "StringStore")
class StringStore {
    private val pool = HashMap<String, HashSet<String>>()

    fun getStrings(name: String): MutableSet<String> {
        val n = name.toLowerCase()
        return pool.computeIfAbsent(n, { HashSet() })
    }

    fun addString(name: String, s: String) {
        val wasChanged = getStrings(name.toLowerCase()).add(s)
        if (wasChanged) {
            APP.db.em.transaction.begin()
            APP.db.em.merge(APP.db.stringPool)
            APP.db.em.transaction.commit()
        }
    }

    fun addStrings(name: String, s: List<String>) {
        val wasChanged = getStrings(name.toLowerCase()).addAll(s)
        if (wasChanged) {
            APP.db.em.transaction.begin()
            APP.db.em.merge(APP.db.stringPool)
            APP.db.em.transaction.commit()
        }
    }

}