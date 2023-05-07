package sp.it.pl.core

import java.io.File
import javax.imageio.ImageIO
import sp.it.util.file.Util.isValidatedDirectory

class CoreImageIO(private val imageIoDir: File): Core {

   override fun init() {
      isValidatedDirectory(imageIoDir)
      ImageIO.setCacheDirectory(imageIoDir)
      ImageIO.setUseCache(false)

      // disable logging from net.sf.javavp8decoder
      java.util.logging.Logger.getLogger("net.sf.javavp8decoder.vp8Decoder").apply {
         handlers.toList().forEach(::removeHandler)
         level = java.util.logging.Level.OFF
         useParentHandlers = false
      }
   }

}