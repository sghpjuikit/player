package sp.it.util.system

import com.sun.jna.platform.FileUtils
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinUser
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.DISCARD
import java.net.URI
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.util.async.IO
import sp.it.util.async.future.Fut
import sp.it.util.async.runIO
import sp.it.util.async.runNew
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.file.FileType
import sp.it.util.file.FileType.FILE
import sp.it.util.file.WindowsShortcut
import sp.it.util.file.find1stExistingParentDir
import sp.it.util.file.hasExtension
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.toFast
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.system.EnvironmentContext.defaultChooseFileDir
import sp.it.util.system.EnvironmentContext.onFileRecycled
import sp.it.util.system.EnvironmentContext.onNonExistentFileBrowse
import sp.it.util.system.EnvironmentContext.runAsProgramArgsTransformer

private val logger = KotlinLogging.logger { }

object EnvironmentContext {
   /**
    * Optional pre-processor of program arguments for [File.runAsProgram].
    * May be used, for example, for executing the command through access elevator utility.
    * Default does nothing.
    */
   var runAsProgramArgsTransformer: (List<String>) -> List<String> = { it }
   /**
    * Invoked on [File.browse] when the file or directory does not exist.
    * May be used to notify or help user handle such case, e.g. by showing a popup.
    * Default does nothing.
    */
   var onNonExistentFileBrowse: (File) -> Unit = { }
   /** Invoked on [File.recycle] if the file existed and recycling was success. Default does nothing. */
   var onFileRecycled: (File) -> Unit = {}
   /**
    * Default file chooser directory if no better default directory can be determined.
    * Default is read from `user.home` system property.
    */
   var defaultChooseFileDir: File = File(System.getProperty("user.home"))
}

/**
 * Launches this file as an executable program as a separate process on an [IO].
 * Executes an initializer block on the process right before [ProcessBuilder.start].
 * - working directory of the program will be set to the parent directory of its file
 * - [ProcessBuilder.redirectOutput] and [ProcessBuilder.redirectError] is set to [DISCARD] unless overridden
 * - the program may start as a child process if otherwise not possible
 *
 * @param arguments arguments to run the program with
 * @param then block taking the program's process as parameter executing if the program executes
 * @return success if the program is executed or error if it is not, irrespective of if and how the program finishes
 */
@ThreadSafe
@JvmOverloads
fun File.runAsProgram(vararg arguments: String, then: (ProcessBuilder) -> Unit = {}): Fut<Process> {
   return runIO {
      val f = if (Os.WINDOWS.isCurrent && hasExtension("lnk")) WindowsShortcut.targetedFile(this).orNull() ?: this
              else this
      val commandRaw = listOf(f.absolutePath, *arguments)
      val command = runAsProgramArgsTransformer(commandRaw)
      val process = ProcessBuilder(command)
         .directory(parentDirOrRoot)
         .redirectOutput(DISCARD).redirectError(DISCARD)
         .apply(then)
         .start()
      process
   }.onError(IO) {
      logger.error(it) { "Failed to launch program $absolutePath" }
   }
}

/**
 * Runs a command in new background thread as a separate process on a new thread and executes an action (on it) right
 * after it launches.
 *
 * @param command see [Runtime.exec]
 * @param then block taking the program's process as parameter executing if the program executes
 * @return success if the program is executed or error if it is not, irrespective of if and how the program finishes
 */
@JvmOverloads
fun runCommand(command: String, then: (Process) -> Unit = {}): Fut<Try<Process, Exception>> {
   return runIO {
      try {
         val process = Runtime.getRuntime().exec(command).apply(then)
         Try.ok(process)
      } catch (e: IOException) {
         logger.error(e) { "Error running command '$command'" }
         Try.error(e)
      }
   }
}

/** Equivalent to [URI.browse] for the URI denoting this file. */
fun File.browse() = toURI().browse()

/**
 * Browse this uri, in order:
 * - if it is directory/file -> browse the file in the system file browser by opening the parent directory and selecting the file.
 * If this is unsupported, [open] is called on the parent directory.
 * - if it is url -> open the url in system browser
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
 */
fun URI.browse() {
   logger.info { "Browsing uri=$this" }
   runIO {
      toFileOrNull()
         .ifNotNull {
            if (it.exists()) {
               if (Os.WINDOWS.isCurrent) {
                  it.openWindowsExplorerAndSelect()
               } else {
                  // Would be nice if this was widely supported, but it isn't
                  if (Desktop.Action.BROWSE_FILE_DIR.isSupportedOrWarn()) {
                     Desktop.getDesktop().browseFileDirectory(it)
                  } else {
                     it.parentDirOrRoot.open()
                  }
               }
            } else {
               onNonExistentFileBrowse(it)
            }
         }
         .ifNull {
            try {
               if (Desktop.Action.BROWSE.isSupportedOrWarn())
                  Desktop.getDesktop().browse(this)
            } catch (e: IOException) {
               logger.error(e) { "Browsing uri=$this failed" }
            }
         }
   }
}

/**
 * Open this URI, in order:
 * - if it is a file -> [File.open]
 * - else -> [URI.browse]
 */
fun URI.open() {
   logger.info { "Browsing uri=$this" }
   runIO {
      toFileOrNull()
         .ifNotNull {
            if (it.exists()) it.open()
            else onNonExistentFileBrowse(it)
         }
         .ifNull { browse() }
   }
}

/**
 * Edit this file, in order:
 * - if it is directory -> [open] is called
 * - if it is file -> open in system associated editor if any is available or [open] is called instead
 * - if it does not exist -> no-op
 *
 * On some platforms the operation may be unsupported. In that case this [open] is called.
 */
fun File.edit() {
   logger.info { "Editing file=$this" }
   runIO {
      if (isDirectory) {
         open()
      } else {
         if (Desktop.Action.EDIT.isSupportedOrWarn()) {
            try {
               Desktop.getDesktop().edit(this)
            } catch (e: IOException) {
               val noApp1 = "No application is associated with the specified file for this operation" in e.message.orEmpty()
               val noApp2 = "Application not found" in e.message.orEmpty()
               if (noApp1 || noApp2) logger.info(e) { "Couldn't find an editor association for file=$this" }
               else logger.error(e) { "Opening file=$this in system editor failed" }

               if (noApp1 || noApp2) open()
            } catch (e: IllegalArgumentException) {
               // file does not exist, nothing to do
            }
         } else {
            open()
         }
      }
   }
}

/**
 * Open this file, in order:
 * - if it is executable, it will be executed using [runAsProgram]
 * - if it is application skin, it will be applied
 * - if it is application component, it will be opened
 * - if it is directory, it will be opened in default system's browser
 * - if it is file, it will be opened in the default associated program.
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
 */
fun File.open() {
   logger.info { "Opening file=$this" }
   runNew<Unit> {
      when {
         // If the file is executable, Desktop#open() will execute it, however the spawned process' working directory
         // will be set to the working directory of this application, which is not illegal, but definitely dangerous
         // Hence, we execute files on our own
         isExecutable() -> runAsProgram()
         else -> {
            if (Desktop.Action.OPEN.isSupportedOrWarn()) {
               try {
                  Desktop.getDesktop().open(this)
               } catch (e: IOException) {
                  val noApp1 = "No application is associated with the specified file for this operation" in e.message.orEmpty()
                  val noApp2 = "Application not found" in e.message.orEmpty()
                  if (noApp1 || noApp2) logger.warn(e) { "Couldn't find an application association for file=$this" }
                  else logger.error(e) { "Opening file=$this in native app failed" }
               } catch (e: IllegalArgumentException) {
                  // file does not exist, nothing to do
               }
            }
         }
      }
   }
}

/**
 * Deletes this file by moving it to the recycle bin of the underlying OS.
 *
 * @return success if file was deleted or did not exist or error if error occurs during deletion
 */
fun File.recycle(): Try<Nothing?, String> {
   logger.info { "Recycling file=$this" }

   return when (Os.current) {
      Os.WINDOWS, Os.OSX ->
         runTry {
            FileUtils.getInstance().moveToTrash(this)
            onFileRecycled(this)
         }.map { null }.mapError {
            it.message ?: "Unknown reason"
         }
      else ->
         try {
            val r = Desktop.getDesktop().moveToTrash(this)
            if (r) {
               onFileRecycled(this)
               Try.ok()
            } else {
               Try.error("Failed")
            }
         } catch (e: SecurityException) {
            Try.error("Access denied")
         } catch (e: UnsupportedOperationException) {
            Try.error("Unsupported")
         } catch (e: IllegalArgumentException) {
            // file does not exist, nothing to do
            Try.ok()
         }
   }
}

fun chooseFile(title: String, type: FileType, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Nothing?> {
   when (type) {
      FileType.DIRECTORY -> {
         val c = DirectoryChooser().apply {
            this.title = title
            this.initialDirectory = initial?.find1stExistingParentDir()?.orNull() ?: defaultChooseFileDir
         }

         w?.hasFileChooserOpen = true
         val f = c.showDialog(w)?.toFast(type)
         w?.hasFileChooserOpen = false
         return if (f!=null) Try.ok(f) else Try.error()
      }
      FILE -> {
         val c = FileChooser().apply {
            this.title = title
            this.initialDirectory = initial?.find1stExistingParentDir()?.orNull() ?: defaultChooseFileDir
            this.extensionFilters += extensions
         }
         w?.hasFileChooserOpen = true
         val f = c.showOpenDialog(w)?.toFast(type)
         w?.hasFileChooserOpen = false
         return if (f!=null) Try.ok(f) else Try.error()
      }
   }
}

fun chooseFiles(title: String, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<List<File>, Nothing?> {
   val c = FileChooser().apply {
      this.title = title
      this.initialDirectory = initial?.find1stExistingParentDir()?.orNull() ?: defaultChooseFileDir
      this.extensionFilters += extensions
   }
   w?.hasFileChooserOpen = true
   val fs = c.showOpenMultipleDialog(w)?.map { it.toFast(FILE) }
   w?.hasFileChooserOpen = false
   return if (!fs.isNullOrEmpty()) Try.ok(fs) else Try.error()
}

fun saveFile(title: String, initial: File? = null, initialName: String, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Nothing?> {
   val c = FileChooser().apply {
      this.title = title
      this.initialDirectory = initial?.find1stExistingParentDir()?.orNull() ?: defaultChooseFileDir
      this.initialFileName = initialName
      this.extensionFilters += extensions
   }
   w?.hasFileChooserOpen = true
   val f = c.showSaveDialog(w)?.toFast(FILE)
   w?.hasFileChooserOpen = false
   return if (f!=null) Try.ok(f) else Try.error()
}

/** @return file representing the current desktop wallpaper or null if not supported */
fun Screen.getWallpaperFile(): File? =
   if (Os.WINDOWS.isCurrent) {
      val path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper")
      val isMultiWallpaper = path.endsWith("TranscodedWallpaper")
      if (isMultiWallpaper) {
         val pathPrefix = path.substringBeforeLast("Wallpaper")

         // TODO: implement properly
         // https://github.com/cbucher/console/issues/187
         // https://stackoverflow.com/questions/31092395/monitors-position-on-windows-wallpaper
         // https://github.com/java-native-access/jna/blob/master/contrib/monitordemo/src/com/sun/jna/contrib/demo/MonitorInfoDemo.java
         /** @return index of the screen as reported by the underlying os */
         fun Screen.ordinal(): Int {
            var ord = 0

            User32.INSTANCE.EnumDisplayMonitors(null, null, { hMonitor, _, _, _ ->
               val info = WinUser.MONITORINFOEX()
               User32.INSTANCE.GetMonitorInfo(hMonitor, info)

               if (info.rcWork.left==bounds.minX.toInt() && info.rcWork.right==bounds.maxX.toInt() && info.rcWork.top==bounds.minY.toInt() && info.rcWork.bottom==bounds.maxY.toInt()) {
                  ord = String(info.szDevice).substringAfter("device")[0].digitToInt()
               }

               1
            }, WinDef.LPARAM(0))

            return ord
         }

         val index = (ordinal() - 1).toString().padStart(3, '0')
         File("${pathPrefix}_$index").takeIf { it.exists() } ?: File(path)
      } else {
         File(path)
      }
   } else {
      null
   }

private fun Desktop.Action.isSupportedOrWarn() =
   if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(this)) {
      true
   } else {
      logger.warn { "Unsupported operation=$this" }
      false
   }

private fun File.openWindowsExplorerAndSelect() =
   try {
      Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", "\"$absolutePath\""))
      true
   } catch (e: IOException) {
      logger.error(e) { "Failed to open explorer.exe and select file=$this" }
      false
   }

/**
 * This is best estimate only.
 *
 * @return true if the file is an executable file
 */
@Blocks(false)
fun File.isExecutable(): Boolean = when (Os.current) {
   Os.WINDOWS -> hasExtension("exe", "bat") || (this.hasExtension("lnk") && WindowsShortcut.targetedFile(this).orNull()?.isExecutable()==true)
   else -> hasExtension("sh")
}

var Window.hasFileChooserOpen: Boolean
   set(value) {
      if (value) {
         properties["no-auto-hide"] = Any()
      } else {
         properties -= "no-auto-hide"
         requestFocus()
      }
   }
   get() = "no-auto-hide" in properties
