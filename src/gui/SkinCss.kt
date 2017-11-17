package gui

import java.io.File

class SkinCss {
    @JvmField val name: String
    @JvmField val file: File

    constructor(cssFile: File) {
        name = cssFile.nameWithoutExtension
        file = cssFile
    }

}