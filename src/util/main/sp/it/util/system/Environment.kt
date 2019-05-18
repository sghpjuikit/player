package sp.it.util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.util.async.future.Fut
import sp.it.util.async.runNew
import sp.it.util.file.FileType
import sp.it.util.file.find1stExistingParentDir
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.system.EnvironmentContext.defaultChooseFileDir
import sp.it.util.system.EnvironmentContext.runAsProgramArgsTransformer
import sp.it.util.ui.ordinal
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI

private val logger = KotlinLogging.logger { }

object EnvironmentContext {
    var runAsProgramArgsTransformer: (List<String>) -> List<String> = { it }
    var defaultChooseFileDir: File = File(System.getProperty("user.home"))
}

/** Puts the specified string to system clipboard. Does nothing if null. */
fun copyToSysClipboard(s: String?) = copyToSysClipboard(DataFormat.PLAIN_TEXT, s)

/** Puts the specified object to system clipboard. Does nothing if null. */
fun copyToSysClipboard(df: DataFormat, o: Any?) = o?.let { Clipboard.getSystemClipboard().setContent(mapOf(df to it)) }

/**
 * Launches this file as an executable program as a separate process on a new thread and executes an action (on it)
 * right after it launches.
 * - working directory of the program will be set to the parent directory of its file
 * - the program may start as a child process if otherwise not possible
 *
 * @param arguments arguments to run the program with
 * @param then block taking the program's process as parameter executing if the program executes
 * @return success if the program is executed or error if it is not, irrespective of if and how the program finishes
 */
@JvmOverloads
fun File.runAsProgram(vararg arguments: String, then: (Process) -> Unit = {}): Fut<Try<Process, Exception>> {
    return runNew {
        val commandRaw = listOf(absoluteFile.path, *arguments)
        val command = runAsProgramArgsTransformer(commandRaw)
        try {
            val process = ProcessBuilder(command)
                    .directory(parentDirOrRoot)
                    .start()
                    .apply(then)

            Try.ok(process)
        } catch (e: IOException) {
            logger.error(e) { "Failed to launch program" }
            Try.error(e)
        }
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
    return runNew {
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
    runNew {
        toFileOrNull()
                .ifNotNull {
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
 * Edit this file, in order:
 * - if it is directory -> [open] is called
 * - if it is file -> open in system associated editor if any is available or [open] is called instead
 * - if it does not exist -> no-op
 *
 * On some platforms the operation may be unsupported. In that case this [open] is called.
 */
fun File.edit() {
    logger.info { "Editing file=$this" }
    runNew {
        if (isDirectory) {
            open()
        } else {
            if (Desktop.Action.EDIT.isSupportedOrWarn()) {
                try {
                    Desktop.getDesktop().edit(this)
                } catch (e: IOException) {
                    val noApp = "No application is associated with the specified file for this operation" in e.message.orEmpty()
                    if (noApp) logger.info(e) { "Couldn't find an editor association for file=$this" }
                    else logger.error(e) { "Opening file=$this in system editor failed" }

                    if (noApp) open()
                } catch (e: IllegalArgumentException) {
                    // file does not exists, nothing to do
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
 * - if it it is file, it will be opened in the default associated program.
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
            else ->  {
                if (Desktop.Action.OPEN.isSupportedOrWarn()) {
                    try {
                        Desktop.getDesktop().open(this)
                    } catch (e: IOException) {
                        val noApp = "No application is associated with the specified file for this operation" in e.message.orEmpty()
                        if (noApp) logger.warn(e) { "Couldn't find an application association for file=$this" }
                        else logger.error(e) { "Opening file=$this in native app failed" }
                    } catch (e: IllegalArgumentException) {
                        // file does not exists, nothing to do
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
fun File.recycle(): Try<Nothing?, Nothing?> {
    logger.info { "Recycling file=$this" }
    return if (Desktop.Action.MOVE_TO_TRASH.isSupportedOrWarn()) {
        try {
            if (Desktop.getDesktop().moveToTrash(this)) Try.ok() else Try.error()
        } catch (e: IllegalArgumentException) {
            Try.ok()
        }
    } else {
        Try.error()
    }
}

fun chooseFile(title: String, type: FileType, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void?> {
    when (type) {
        FileType.DIRECTORY -> {
            val c = DirectoryChooser().apply {
                this.title = title
                this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(defaultChooseFileDir)
            }
            val f = c.showDialog(w)
            return if (f!=null) Try.ok(f) else Try.error()
        }
        FileType.FILE -> {
            val c = FileChooser().apply {
                this.title = title
                this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(defaultChooseFileDir)
                this.extensionFilters += extensions
            }
            val f = c.showOpenDialog(w)
            return if (f!=null) Try.ok(f) else Try.error()
        }
    }
}

fun chooseFiles(title: String, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<List<File>, Void?> {
    val c = FileChooser().apply {
        this.title = title
        this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(defaultChooseFileDir)
        this.extensionFilters += extensions
    }
    val fs = c.showOpenMultipleDialog(w)
    return if (fs!=null && fs.isNotEmpty()) Try.ok(fs) else Try.error()
}

fun saveFile(title: String, initial: File? = null, initialName: String, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void?> {
    val c = FileChooser().apply {
        this.title = title
        this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(defaultChooseFileDir)
        this.initialFileName = initialName
        this.extensionFilters += extensions
    }
    val f = c.showSaveDialog(w)
    return if (f!=null) Try.ok(f) else Try.error()
}

/** @return file representing the current desktop wallpaper or null if not supported */
fun Screen.getWallpaperFile(): File? =
        if (Os.WINDOWS.isCurrent) {
            val path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper")
            val isMultiWallpaper = path.endsWith("TranscodedWallpaper")
            if (isMultiWallpaper) {
                val pathPrefix = path.substringBeforeLast("Wallpaper")
                val index = (ordinal-1).toString().padStart(3, '0')
                File("${pathPrefix}_$index")
                        .takeIf { it.exists() }
                        ?: File(path)
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
            Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", "\"$path\""))
            true
        } catch (e: IOException) {
            logger.error(e) { "Failed to open explorer.exe and select file=$this" }
            false
        }

/** @return true if the file is an executable file */
private fun File.isExecutable(): Boolean =
        when (Os.current) {
            Os.WINDOWS -> path.endsWith(".exe", true) || path.endsWith(".bat", true)
            else -> path.endsWith(".sh")
        }