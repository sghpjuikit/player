package sp.it.util.ui

import javafx.scene.image.Image
import sp.it.util.file.Util
import sp.it.util.file.WindowsShortcut
import sp.it.util.file.div
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.file.type.MimeExt.Companion.exe
import sp.it.util.file.type.MimeExt.Companion.lnk
import sp.it.util.file.writeTextTry
import sp.it.util.functional.orNull
import sp.it.util.functional.runIf
import sp.it.util.system.Os
import sp.it.util.ui.image.toFX
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

/**
 * Extracts an icon for a file type of specific file.
 *
 * http://stackoverflow.com/questions/15629069/extract-application-icons-from-desktop-folder-to-application
 * http://stackoverflow.com/questions/15149565/how-does-jtree-display-file-name/15150756#15150756
 * http://stackoverflow.com/questions/28034432/javafx-file-listview-with-icon-and-file-name
 * http://stackoverflow.com/questions/26192832/java-javafx-set-swing-icon-for-javafx-label
 */
object IconExtractor {
   private val dirTmp = File(System.getProperty("java.io.tmpdir"))
   private val helperFileSystemView by lazy { FileSystemView.getFileSystemView() }
   private val mapOfFileExtToSmallIcon = ConcurrentHashMap<String, Image?>()

   @JvmStatic fun getFileIcon(file: File): Image? {

      val ext = Util.getSuffix(file.path).toLowerCase()

      // shortcuts have icons of files they refer to
      if (lnk==ext)
         return WindowsShortcut.targetedFile(file).map(::getFileIcon).orNull()

      // Handle windows executable files (we need to handle each individually)
      val isExe = exe==ext
      val key = if (isExe) file.nameWithoutExtensionOrRoot else ext

      return mapOfFileExtToSmallIcon.computeIfAbsent(key) {
         val iconFile = file.takeIf { it.exists() } ?: runIf(!isExe) {
            val f = dirTmp/"file_type_icons.$it"
            f.takeIf { it.exists() || it.writeTextTry("").isOk }
         }
         iconFile?.getSwingIconFromFileSystem()?.toImage()
      }
   }

   private fun File.getSwingIconFromFileSystem(): Icon? = when (Os.current) {
      Os.WINDOWS -> helperFileSystemView.getSystemIcon(this)
      // TODO: implement
      // Os.OSX -> {
      //     final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
      //     return icon = fc.getUI().getFileView(fc).getIcon(file);
      // }
      else -> null
   }

   private fun Icon.toImage(): Image? {
      val image = BufferedImage(this.iconWidth, this.iconHeight, TYPE_INT_ARGB)
      paintIcon(null, image.graphics, 0, 0)
      return image.toFX(null)
   }

}