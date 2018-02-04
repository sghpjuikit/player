package sp.it.pl.gui

import javafx.scene.image.Image
import sp.it.pl.main.AppUtil.APP
import java.io.File

data class SkinCss(@JvmField val name: String, @JvmField val file: File) {

    constructor(cssFile: File): this(cssFile.nameWithoutExtension, cssFile)

}

// TODO: implement cursor
// val image = Image(File(APP.DIR_RESOURCES, "icons/cursor.png").getAbsoluteFile().toURI().toString());
// val cursor = new ImageCursor(image,3,3);
// stage.getScene().setCursor(cursor);