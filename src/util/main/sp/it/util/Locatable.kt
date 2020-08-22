package sp.it.util

import sp.it.util.file.div
import java.io.File

interface Locatable {

   val location: File

   val userLocation: File

   fun getResource(path: String): File = location/path

   fun getUserResource(path: String): File = userLocation/path

}