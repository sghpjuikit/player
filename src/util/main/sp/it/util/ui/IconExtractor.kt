package sp.it.util.ui

import java.awt.image.BufferedImage as ImageBf
import javafx.scene.image.Image as ImageFx
import javax.swing.Icon as ImageSw
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.WinDef
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import mu.KLogging
import sp.it.util.file.WindowsShortcut
import sp.it.util.file.div
import sp.it.util.file.type.MimeExt.Companion.exe
import sp.it.util.file.type.MimeExt.Companion.lnk
import sp.it.util.file.writeTextTry
import sp.it.util.functional.asIs
import sp.it.util.functional.orNull
import sp.it.util.functional.runIf
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.system.Os.OSX
import sp.it.util.system.Os.WINDOWS
import sp.it.util.ui.image.toFxAndFlush

/**
 * Extracts an icon for a file type of specific file.
 *
 * http://stackoverflow.com/questions/15629069/extract-application-icons-from-desktop-folder-to-application
 * http://stackoverflow.com/questions/15149565/how-does-jtree-display-file-name/15150756#15150756
 * http://stackoverflow.com/questions/28034432/javafx-file-listview-with-icon-and-file-name
 * http://stackoverflow.com/questions/26192832/java-javafx-set-swing-icon-for-javafx-label
 */
object IconExtractorK: KLogging() {

   private val dirTmp = File(System.getProperty("java.io.tmpdir"))
   private val helperFileSystemView by lazy { FileSystemView.getFileSystemView() }
   private val icons = ConcurrentHashMap<String, ImageFx?>()

   fun getFileIcon(file: File): ImageFx? {

      val ext = file.extension.lowercase()

      // resolve links
      if (lnk==ext)
         return WindowsShortcut.targetedFile(file).map(::getFileIcon).orNull()

      val hasUniqueIcon = ext==exe
      val key = if (hasUniqueIcon) file.absolutePath else ext

      return icons.computeIfAbsent(key) {
         runTry {
            null
               ?: run {
                  if (file.extension in setOf("dll", "ico", "exe"))
                     file.iconOfExecutable()?.toFxAndFlush()
                  else
                     null
               }
               ?: run {
                  val iconFile = null
                     ?: file.takeIf { it.exists() }
                     ?: runIf(!hasUniqueIcon) {
                        val f = dirTmp/"iconCache"/"iconForExtension.$ext"
                        f.takeIf { it.exists() || it.writeTextTry("").isOk }
                     }
                  iconFile?.getSwingIconFromFileSystem()?.toImage()
               }
         }.orNull {
            // https://bugs.openjdk.org/browse/JDK-8293862
            logger.error { "Error obtaining icon for file=${file}" }
         }
      }
   }

   private fun File.getSwingIconFromFileSystem(): ImageSw? = when (Os.current) {
      WINDOWS -> helperFileSystemView.getSystemIcon(this)
      OSX -> {
         // final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
         // fc.getUI().getFileView(fc).getIcon(file);
         null
      }
      else -> null
   }

   private fun File.iconOfExecutable(): ImageBf? = when (Os.current) {
      WINDOWS -> {
         val iconCount = Shell32.INSTANCE.ExtractIconEx(absolutePath, -1, null, null, 0)
         if (iconCount>0) {
            val iconHandles = arrayOfNulls<WinDef.HICON?>(iconCount).apply {
               (0..<iconCount).map { Shell32.INSTANCE.ExtractIconEx(absolutePath, it, this, null, 1) }
            }

            iconHandles.asSequence()
               .filterNotNull()
               .maxByOrNull { IconExtractorJna.getIconSize(it).width }
               ?.let(IconExtractorJna::getWindowIcon)
         } else {
            null
         }
      }
      else -> null
   }

   private fun ImageSw.toImage(): ImageFx {
      return when {
         this is ImageIcon && image is ImageBf -> image.asIs<ImageBf>().toFxAndFlush()
         else -> {
            val image = ImageBf(this.iconWidth, this.iconHeight, TYPE_INT_ARGB)
            paintIcon(null, image.graphics, 0, 0)
            return image.toFxAndFlush()
         }
      }
   }

}