package util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Window
import main.App.APP
import util.file.FileType
import util.file.find1stExistingParentDir
import util.functional.Try
import util.functional.Try.ok
import util.graphics.ordinal
import java.io.File

/** Puts the specified string to system clipboard. Does nothing if null. */
fun copyToSysClipboard(s: String?) {
    copyToSysClipboard(DataFormat.PLAIN_TEXT, s)
}

/** Puts the specified object to system clipboard. Does nothing if null. */
fun copyToSysClipboard(df: DataFormat, o: Any?) {
    if (o!=null) {
        val c = ClipboardContent()
        c.put(df, o)
        Clipboard.getSystemClipboard().setContent(c)
    }
}

fun chooseFile(title: String, type: FileType, initial: File?, w: Window, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void> {
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

fun chooseFiles(title: String, initial: File?, w: Window, vararg extensions: FileChooser.ExtensionFilter): Try<List<File>, Void> {
    val c = FileChooser().apply {
        this.title = title
        this.initialDirectory = initial?.find1stExistingParentDir()?.getOr(APP.DIR_APP)
        this.extensionFilters += extensions
    }
    val fs = c.showOpenMultipleDialog(w)
    return if (fs!=null && !fs.isEmpty()) ok<List<File>, Void>(fs) else Try.error()
}

fun saveFile(title: String, initial: File?, initialName: String, w: Window, vararg extensions: FileChooser.ExtensionFilter): Try<File, Void> {
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
        if (Os.WINDOWS.isCurrent()) {
            val path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper")
            val isMultiWallpaper = path.endsWith("TranscodedWallpaper")
            if (isMultiWallpaper) {
                File(path.substringBeforeLast("Wallpaper") + "_" + (ordinal-1).toString().padStart(3, '0'))
                        .takeIf { it.exists() }
                        ?: File(path)
            } else {
                println(path)
                File(path)
            }
        } else {
                println(null)
            null
        }
