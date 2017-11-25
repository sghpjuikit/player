package sp.it.pl.gui

import java.io.File

data class SkinCss(@JvmField val name: String, @JvmField val file: File) {

    constructor(cssFile: File): this(cssFile.nameWithoutExtension, cssFile)

}

// TODO: implement cursor
// Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());
// ImageCursor c = new ImageCursor(image,3,3);
// stage.getScene().setCursor(c);