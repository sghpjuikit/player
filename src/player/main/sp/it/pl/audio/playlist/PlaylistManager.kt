package sp.it.pl.audio.playlist

import sp.it.pl.audio.playlist.sequence.PlayingSequence
import sp.it.pl.main.APP
import sp.it.pl.main.audioExtensionFilter
import sp.it.pl.main.configure
import sp.it.pl.main.isAudio
import sp.it.util.access.V
import sp.it.util.action.IsAction
import sp.it.util.collections.mapset.MapSet
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.ValueConfig
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.Util.getFilesR
import sp.it.util.functional.Util
import sp.it.util.functional.ifNotNull
import sp.it.util.system.chooseFile
import sp.it.util.system.chooseFiles
import sp.it.util.type.type
import sp.it.util.units.uri
import java.io.File
import java.net.URI
import java.util.UUID
import kotlin.streams.toList

/** Manages playlists. */
object PlaylistManager: GlobalSubConfigDelegator("Playback") {
   /** All playlists in the application. */
   @JvmField val playlists = MapSet<UUID, Playlist> { it.id }
   /** [Playlist.id] of currently active playlist or null if none active. */
   @JvmField var active: UUID? = null
   /** Selects next playing item. */
   @JvmField val playingItemSelector = PlayingSequence()
   /** Last selected item on playlist or null if none. */
   @JvmField val selectedItemES = V<PlaylistSong?>(null)
   /** Selected items on playlist or empty list if none. */
   @JvmField val selectedItemsES = V<List<PlaylistSong>>(Util.listRO())

   fun use(action: (Playlist) -> Unit) {
      val p: Playlist? = null
         ?: active?.let { playlists[it] }
         ?: playlists.firstOrNull()
      p.ifNotNull(action)
   }

   fun <T> use(action: (Playlist) -> T, or: T): T {
      val p: Playlist? = null
         ?: active?.let { playlists[it] }
         ?: playlists.firstOrNull()
      return if (p==null) or else action(p)
   }

   /** Plays first item on playlist. */
   @IsAction(name = "Play first", info = "Plays first item on playlist.", keys = "ALT+W", global = true)
   fun playFirstItem() = use { it.playFirstItem() }

   /** Plays last item on playlist. */
   @IsAction(name = "Play last", info = "Plays last item on playlist.", global = true)
   fun playLastItem() = use { it.playLastItem() }

   /** Plays next item on playlist according to its selector logic. */
   @IsAction(name = "Play next", info = "Plays next item on playlist.", keys = "ALT+Z", global = true)
   fun playNextItem() = use { it.playNextItem() }

   /** Plays previous item on playlist according to its selector logic. */
   @IsAction(name = "Play previous", info = "Plays previous item on playlist.", keys = "ALT+BACK_SLASH", global = true)
   fun playPreviousItem() = use { it.playPreviousItem() }

   /** Open chooser and add new to end of playlist. */
   @IsAction(name = "Enqueue files", info = "Open file chooser to add files to playlist.")
   fun chooseFilesToAdd() = use { it.addOrEnqueueFiles(true) }

   /** Open chooser and add new to end of playlist. */
   @IsAction(name = "Enqueue directory", info = "Open file chooser to add files from directory to playlist.")
   fun chooseFolderToAdd() = use { it.addOrEnqueueFolder(true) }

   /** Open chooser and add new to end of playlist. */
   @IsAction(name = "Enqueue url", info = "Open file chooser to add url to playlist.")
   fun chooseUrlToAdd() = use { it.addOrEnqueueUrl(true) }

   /** Open chooser and play new items. Clears previous playlist */
   @IsAction(name = "Play files", info = "Open file chooser to play files to playlist.")
   fun chooseFilesToPlay() = use { it.addOrEnqueueFiles(false) }

   /** Open chooser and play new items. Clears previous playlist */
   @IsAction(name = "Play directory", info = "Open file chooser to play files from directory to playlist.")
   fun chooseFolderToPlay() = use { it.addOrEnqueueFolder(false) }

   /** Open chooser and play new items. Clears previous playlist */
   @IsAction(name = "Play url", info = "Open file chooser to add url play playlist.")
   fun chooseUrlToPlay() = use { it.addOrEnqueueUrl(false) }
}

/**
 * Open chooser and add or play new songs.
 *
 * @param add true to add songs, false to clear playlist and play songs
 */
fun Playlist.addOrEnqueueFiles(add: Boolean) {
   chooseFiles(
      "Choose Audio Files",
      APP.audio.browse,
      APP.windowManager.getFocused()?.stage,
      audioExtensionFilter()
   ).ifOk { files ->
      files.firstOrNull()?.parentFile.ifNotNull { APP.audio.browse = it }
      if (add) {
         addFiles(files)
      } else {
         APP.audio.stop()
         clear()
         addFiles(files)
         playFirstItem()
      }
   }
}

/**
 * Open chooser and add or play new songs.
 *
 * @param add true to add songs, false to clear playlist and play songs
 */
fun Playlist.addOrEnqueueFolder(add: Boolean) {
   chooseFile(
      "Choose Audio Files From Directory Tree", DIRECTORY,
      APP.audio.browse,
      APP.windowManager.getFocused()?.stage
   ).ifOk { dir: File ->
      APP.audio.browse = dir
      val files = getFilesR(dir, Int.MAX_VALUE) { it.isAudio() }.toList()
      if (add) {
         addFiles(files)
      } else {
         APP.audio.stop()
         clear()
         addFiles(files)
         playFirstItem()
      }
   }
}

/**
 * Open chooser and add or play new songs.
 *
 * @param add true to add songs, false to clear playlist and play songs
 */
fun Playlist.addOrEnqueueUrl(add: Boolean) {
   ValueConfig(
      type<URI>(),
      "Url",
      uri("https://www.example.com"),
      "Direct uri a file, e.g., a file on the web. The url should end with file audio file suffix like."
   ).configure(if (add) "Add url song." else "Play url song.") {
      if (add) {
         addUri(it.value)
      } else {
         APP.audio.stop()
         clear()
         addUri(it.value)
         playFirstItem()
      }
   }
}