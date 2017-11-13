package util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import javafx.stage.Screen
import util.graphics.ordinal
import java.io.File

/** @return file representing the currently used wallpaper or null if error not supported */
fun Screen.getWallpaperFile(): File? =
        if (Os.WINDOWS.isCurrent()) {
            val path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper")
            val isMultiWallpaper = path.endsWith("TranscodedWallpaper")
            if (isMultiWallpaper) {
                File(path.substringBeforeLast("Wallpaper") + "_" + ordinal.toString().padStart(3, '0'))
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
