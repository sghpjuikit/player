package sp.it.util

import java.io.File
import sp.it.util.file.div

interface Locatable {

   val location: File

   val userLocation: File

   fun getResource(path: String): File = location/path

   fun getUserResource(path: String): File = userLocation/path

}