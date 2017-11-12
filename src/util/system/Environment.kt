package util.system

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.io.File

/** @return file representing the currently used wallpaper or null if file not accessible, error or not supported */
fun getWallpaperFile(): File? =
    if (Os.WINDOWS.isCurrent()) {
        val path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop", "Wallpaper");
        val file = File(path)
        file.takeIf { it.exists() && it.canRead() }
    } else {
        null
    }
