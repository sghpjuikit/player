package sp.it.pl.util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.pl.audio.Player
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.layout.widget.WidgetSource.NO_LAYOUT
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.runNew
import sp.it.pl.util.async.runOn
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.isValidSkinFile
import sp.it.pl.util.file.Util.isValidWidgetFile
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.find1stExistingParentDir
import sp.it.pl.util.file.nameWithoutExtensionOrRoot
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.file.toFileOrNull
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.Try.ok
import sp.it.pl.util.graphics.ordinal
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.function.Consumer

private val logger = KotlinLogging.logger { }

/** Puts the specified string to system clipboard. Does nothing if null. */
fun copyToSysClipboard(s: String?) = copyToSysClipboard(DataFormat.PLAIN_TEXT, s)

/** Puts the specified object to system clipboard. Does nothing if null. */
fun copyToSysClipboard(df: DataFormat, o: Any?) = o?.let { Clipboard.getSystemClipboard().setContent(mapOf(df to it)) }

/**
 * Launches this file as an executable program as a separate process. Does not wait for the program or block.
 * * working directory of the program will be set to the parent directory of its file
 * * the program may start as a child process if otherwise not possible
 *
 * @param arguments arguments to run the program with
 * @return success if the program is executed or error if it is not, irrespective of if and how the program finishes
 */
fun File.runAsProgram(vararg arguments: String): Fut<Try<Void, Exception>> = runOn(Player.IO_THREAD) {
            val command = ArrayList<String>()
            if (Os.WINDOWS.isCurrent)
                command += APP.DIR_APP.childOf("elevate.exe").absolutePath   // use elevate.exe to run command

            command += absoluteFile.path
            command += arguments.asSequence().filter { it.isNotBlank() }.map { "-$it" }

            try {
                ProcessBuilder(command)
                        .directory(parentDirOrRoot)
                        .start()

                ok<Exception>()
            } catch (e: IOException) {
                logger.warn(e) { "Failed to launch program" }
                Try.error<Void, Exception>(e)
            }
        }

/**
 * Runs a command in new background thread as a process and executes an action (on it) right after it launches.
 * This allows process monitoring or waiting for it to end.
 *
 * @param command see [Runtime.exec]
 */
@JvmOverloads
fun runCommand(command: String, then: Consumer<Process>? = null) {
    runNew {
        try {
            val p = Runtime.getRuntime().exec(command)
            then?.accept(p)
        } catch (e: IOException) {
            logger.error(e) { "Error running command '$command'" }
        }
    }
}

/**
 * Browse the file in OS' file browser:
 * * if denotes a directory it will be opened
 * * if denotes a file, it's location will be opened and the file selected
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
 */
fun File.browse() = toURI().browse()

/**
 * Browse uri:
 * * if denotes an uri, opens it in its predefined internet browser
 * * if denotes a directory it will be opened
 * * if denotes a file, it's location will be opened and the file selected
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
 */
fun URI.browse() {
    logger.info { "Browsing uri=$this" }
    runNew {
        val f = toFileOrNull()
        if (f==null) {
            try {
                if (Desktop.Action.BROWSE.isSupportedOrWarn())
                    Desktop.getDesktop().browse(this)
            } catch (e: IOException) {
                logger.error(e) { "Browsing uri=$this failed" }
            }
        } else {
            if (f.isDirectory) {
                // Would be nice if this was widely supported, but it isn't
                if (Desktop.Action.BROWSE_FILE_DIR.isSupportedOrWarn())
                    Desktop.getDesktop().browseFileDirectory(f)
                else
                    f.openWindowsExplorerAndSelect()
            } else {
                // Would be nice if this did what it is supposed to, but it doesn't (tries to open the file)
                // Desktop.getDesktop().browse(this)
                if (Os.WINDOWS.isCurrent) {
                    f.openWindowsExplorerAndSelect()
                } else {
                    f.parentDirOrRoot.open()
                }
            }
        }
    }
}

/**
 * Edit the file, in order:
 * * if is a directory, [open] is called
 * * if is a file, it will be edited in default associated editor program
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
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
                    logger.error(e) { "Opening file=$this in editor failed" }
                } catch (e: IllegalArgumentException) {
                    // file does not exists, nothing to do
                }
            }
        }
    }
}

/**
 * Opens the file, in order:
 * * if is executable, it will be executed using [runAsProgram]
 * * if is application skin, it will be applied
 * * if is application component, it will be opened
 * * if is directory, it will be opened in default system's browser
 * * if it is file, it will be opened in the default associated program.
 *
 * On some platforms the operation may be unsupported. In that case this method is a no-op.
 */
fun File.open() {
    logger.info { "Opening file=$this" }
    runNew<Unit> {
        when {
            // If the file is executable, Desktop#open() will execute it, however the spawned process' working directory
            // will be set to the working directory of this application, which is not illegal, but definitely dangerous
            // Hence, we executable files on our own
            isExecutable() -> runAsProgram()
            else -> when {
                isDirectory && APP.DIR_SKINS==parentDir || isValidSkinFile(this) -> APP.ui.setSkin(this)
                isDirectory && APP.DIR_WIDGETS==parentDir || isValidWidgetFile(this) -> APP.widgetManager.widgets.find(nameWithoutExtensionOrRoot, NO_LAYOUT, false)
                else -> {
                    if (Desktop.Action.OPEN.isSupportedOrWarn()) {
                        try {
                            Desktop.getDesktop().open(this)
                        } catch (e: IOException) {
                            val noApp = "No application is associated with the specified file for this operation" in e.message.orEmpty()
                            if (noApp) logger.warn(e) { "Opening file=$this in native app failed" }
                            else logger.error(e) { "Opening file=$this in native app failed" }
                        } catch (e: IllegalArgumentException) {
                            // file does not exists, nothing to do
                        }
                    }
                }
            }
        }
    }
}

/**
 * Deletes the file by moving it to the recycle bin of the underlying OS.
 * * if denotes a directory, it will be deleted including its content
 * * file will not be deleted permanently, only recycled
 *
 *  @return success if file was deleted or did not exist or error if error occurs during deletion
 */
fun File.recycle(): Try<Void, Void> {
    logger.info { "Recycling file=$this" }
    return if (Desktop.Action.MOVE_TO_TRASH.isSupportedOrWarn()) {
        try {
            if (Desktop.getDesktop().moveToTrash(this)) Try.ok<Void>() else Try.error()
        } catch (e: IllegalArgumentException) {
            Try.ok<Void>()
        }
    } else {
        Try.error()
    }
}

fun File.openIn() {
    when {
        AudioFileFormat.isSupported(this, AudioFileFormat.Use.PLAYBACK) -> PlaylistManager.use { it.addUri(toURI()) }
        ImageFileFormat.isSupported(this) -> APP.widgetManager.widgets.use<ImageDisplayFeature>(NO_LAYOUT) { it.showImage(this) }
        else -> open()
    }
}

fun openIn(files: List<File>) {
    if (files.isEmpty()) {
        return
    } else if (files.size==1) {
        files[0].openIn()
    } else {
        val audio = files.filter { AudioFileFormat.isSupported(it, AudioFileFormat.Use.PLAYBACK) }
        val images = files.filter { ImageFileFormat.isSupported(it) }

        if (!audio.isEmpty())
            PlaylistManager.use { it.addUris(audio.map { it.toURI() }) }

        if (images.size==1) {
            APP.widgetManager.widgets.use<ImageDisplayFeature>(NO_LAYOUT) { it.showImage(images[0]) }
        } else if (images.size>1) {
            APP.widgetManager.widgets.use<ImagesDisplayFeature>(NO_LAYOUT) { it.showImages(images) }
        }
    }
}

fun chooseFile(title: String, type: FileType, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void> {
    when (type) {
        FileType.DIRECTORY -> {
            val c = DirectoryChooser().apply {
                this.title = title
                this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(APP.DIR_APP)
            }
            val f = c.showDialog(w)
            return if (f!=null) ok<File, Void>(f) else Try.error()
        }
        FileType.FILE -> {
            val c = FileChooser().apply {
                this.title = title
                this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(APP.DIR_APP)
                this.extensionFilters += extensions
            }
            val f = c.showOpenDialog(w)
            return if (f!=null) ok<File, Void>(f) else Try.error()
        }
    }
}

fun chooseFiles(title: String, initial: File? = null, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<List<File>, Void> {
    val c = FileChooser().apply {
        this.title = title
        this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(APP.DIR_APP)
        this.extensionFilters += extensions
    }
    val fs = c.showOpenMultipleDialog(w)
    return if (fs!=null && !fs.isEmpty()) ok<List<File>, Void>(fs) else Try.error()
}

fun saveFile(title: String, initial: File? = null, initialName: String, w: Window? = null, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void> {
    val c = FileChooser().apply {
        this.title = title
        this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(APP.DIR_APP)
        this.initialFileName = initialName
        this.extensionFilters += extensions
    }
    val f = c.showSaveDialog(w)
    return if (f!=null) Try.ok(f) else Try.error()
}

/** @return file representing the currently used wallpaper or null if error not supported */
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
            null    // TODO: implement
        }

private fun Desktop.Action.isSupportedOrWarn() =
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(this)) {
            true
        } else {
            logger.warn { "Unsupported operation=$this" }
            false
        }

private fun File.openWindowsExplorerAndSelect() {
    try {
        Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", "\"$path\""))
    } catch (e: IOException) {
        logger.error(e) { "Failed to open explorer.exe and select file=$this" }
    }
}

// TODO: implement
/** @return true if the file is an executable file */
private fun File.isExecutable(): Boolean =
        when (Os.current) {
            Os.WINDOWS -> path.endsWith(".exe", true)
            else -> false
        }