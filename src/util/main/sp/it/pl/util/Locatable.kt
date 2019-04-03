package sp.it.pl.util

import sp.it.pl.util.file.div
import java.io.File

interface Locatable {

    val location: File

    val userLocation: File

    @JvmDefault
    fun getResource(path: String): File = location/path

    @JvmDefault
    fun getUserResource(path: String): File = userLocation/path

}