package sp.it.pl.gui

import java.io.File

data class SkinCss(val name: String, val file: File) {

    constructor(cssFile: File): this(cssFile.nameWithoutExtension, cssFile)

}