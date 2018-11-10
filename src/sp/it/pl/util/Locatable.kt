package sp.it.pl.util

import sp.it.pl.util.file.childOf
import java.io.File

interface Locatable {

    val location: File

    val userLocation: File

    @JvmDefault
    fun getResource(path: String): File {
        return location.childOf(path)
    }

    @JvmDefault
    fun getUserResource(path: String): File {
        return userLocation.childOf(path)
    }

}