package sp.it.pl.core

import sp.it.pl.util.file.Util.isValidatedDirectory
import java.awt.color.ColorSpace
import java.awt.color.ICC_Profile
import java.io.File
import javax.imageio.ImageIO

class CoreImageIO(private val imageIoDir: File): Core {

    override fun init() {
        isValidatedDirectory(imageIoDir)
        ImageIO.setCacheDirectory(imageIoDir)
        ImageIO.setUseCache(true)

        // Load deferred color space profiles to avoid ConcurrentModificationException due to JDK bug
        // https://github.com/haraldk/TwelveMonkeys/issues/402
        // https://bugs.openjdk.java.net/browse/JDK-6986863
        // https://stackoverflow.com/questions/26297491/imageio-thread-safety
        ICC_Profile.getInstance(ColorSpace.CS_sRGB).data
        ICC_Profile.getInstance(ColorSpace.CS_PYCC).data
        ICC_Profile.getInstance(ColorSpace.CS_GRAY).data
        ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ).data
        ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB).data
    }

}