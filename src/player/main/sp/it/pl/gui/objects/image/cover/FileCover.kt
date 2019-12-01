package sp.it.pl.gui.objects.image.cover

import sp.it.pl.image.ImageStandardLoader
import sp.it.util.ui.image.ImageSize
import java.io.File

/** Denotes Cover represented by a [java.io.File]. */
data class FileCover(private val file: File?, private val description: String): Cover {

   override fun getImage() = ImageStandardLoader(file)

   override fun getImage(width: Double, height: Double) = if (file==null) null else ImageStandardLoader(file, ImageSize(width, height))

   override fun getFile() = file

   override fun isEmpty() = file==null

   override fun getDescription() = description

}