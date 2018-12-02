package sp.it.pl.layout.widget.feature

import sp.it.pl.util.file.Util.getCommonFile
import java.io.File

@Feature(
        name = "File explorer",
        description = "File system viewer capable of browsing files",
        type = FileExplorerFeature::class
)
interface FileExplorerFeature {

    /** Explores file in the file system hierarchy.  */
    fun exploreFile(f: File)

    /**
     * Explores first common file in the file system hierarchy.
     *  *  if empty, does nothing
     *  *  if has one file, explores the file
     *  *  if has multiple files, explores their first common parent directory.
     */
    fun exploreCommonFileOf(files: Collection<File>) {
        val f = getCommonFile(files)
        if (f!=null) exploreFile(f)
    }

}