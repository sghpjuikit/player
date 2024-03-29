package sp.it.pl.layout.feature

import java.io.File
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.Playlist
import sp.it.util.async.runVT
import sp.it.util.conf.Configurable
import sp.it.util.file.Util
import sp.it.util.functional.toUnit

@Feature(name = "Configurator", description = "Provides settings and configurations", type = ConfiguringFeature::class)
interface ConfiguringFeature {

   /** Display configs of the specified configurable object for user to edit. */
   fun configure(configurable: Configurable<*>?, groupToSelect: String? = null)

   fun configureAsync(groupToSelect: String? = null, configurable: () -> Configurable<*>?) {
      configure(Configurable.EMPTY)
      runVT { configurable() } ui { configure(it, groupToSelect) }
   }

}

@Feature(
   name = "File explorer",
   description = "File system viewer capable of browsing files",
   type = FileExplorerFeature::class
)
interface FileExplorerFeature {
   /** Explores file in the file system hierarchy. */
   fun exploreFile(file: File)

   /**
    * Explores the first common file in the file system hierarchy.
    *  *  if empty, does nothing
    *  *  if one file, explores the file
    *  *  if multiple files, explores their first common parent directory.
    */
   fun exploreCommonFileOf(files: Collection<File>) = Util.getCommonFile(files)?.let(::exploreFile).toUnit()
}

@Feature(name = "Horizontal dock", description = "Supports thin horizontal layout such as in window header", type = HorizontalDock::class)
interface HorizontalDock

@Feature(name = "Image display", description = "Displays image", type = ImageDisplayFeature::class)
interface ImageDisplayFeature {
   /** Displays the image. */
   fun showImage(imgFile: File?)

   /** Attempts to display the images. Depends on implementation. By default, 1st image is displayed if available. */
   fun showImages(images: Collection<File>) = showImage(images.firstOrNull())
}

@Feature(name = "Images display", description = "Displays images", type = ImagesDisplayFeature::class)
interface ImagesDisplayFeature {
   /** Displays the images. */
   fun showImages(imgFiles: Collection<File>)
}

@Feature(name = "Playback", description = "Controls song playback", type = PlaybackFeature::class)
interface PlaybackFeature

@Feature(name = "Playlist", description = "Is bound to and manages a single playlist", type = PlaylistFeature::class)
interface PlaylistFeature {
   /** Playlist this component manages */
   val playlist: Playlist
}

@Feature(name = "Detail", description = "Shows details about the object ", type = Opener::class)
interface ObjectDetail {
   /** Shows details about the object */
   fun showDetail(data: Any?)
}

@Feature(name = "Opener", description = "Capable of opening any data ", type = Opener::class)
interface Opener {
   /**
    * Opens the data. This can be any object, including an array or collection.
    * The way the data is handled is up to the implementation.
    */
   fun open(data: Any?)
}

@Feature(
   name = "Display text",
   description = "Displays the text",
   type = TextDisplayFeature::class
)
interface TextDisplayFeature {
   fun showText(text: String)
}

@Feature(name = "Song metadata reader", description = "Capable of displaying song metadata", type = SongReader::class)
interface SongReader {
   /** Passes song into this reader. */
   fun read(song: Song?) = read(listOfNotNull(song))

   /** Passes songs into this reader. Displays metadata and displays them. */
   fun read(songs: List<Song>)

   fun isRatingDisplayed(): Boolean? = null
}

@Feature(name = "Song metadata writer", description = "Capable of writing data to song tags", type = SongWriter::class)
interface SongWriter: SongReader