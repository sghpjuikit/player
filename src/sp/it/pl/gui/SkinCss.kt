package sp.it.pl.gui

import java.io.File

data class SkinCss(@JvmField val name: String, @JvmField val file: File) {

    constructor(cssFile: File): this(cssFile.nameWithoutExtension, cssFile)

}