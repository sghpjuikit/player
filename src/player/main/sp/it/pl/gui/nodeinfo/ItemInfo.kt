package sp.it.pl.gui.nodeinfo

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.image.Cover.CoverSource.ANY
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.util.async.runIO
import sp.it.util.identityHashCode
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.prefSize
import sp.it.util.ui.vBox
import sp.it.util.ui.x

/** Basic display for song information. */
class ItemInfo(showCover: Boolean = true): HBox(15.0), SongReader {

   private val indexL = Label()
   private val songL = Label()
   private val artistL = Label()
   private val ratingL = Label()
   private val albumL = Label()
   private var coverContainer = AnchorPane()
   private val rating = Rating()
   private val thumb: Thumbnail?
   private var dataId = null.identityHashCode()

   init {
      padding = Insets(10.0)
      lay += coverContainer
      lay += vBox(5.0, CENTER_RIGHT) {
         prefSize = -1 x -1

         lay += indexL
         lay += songL
         lay += artistL
         lay += ratingL
         lay += albumL
      }


      ratingL.graphic = rating
      rating.alignment.value = CENTER_LEFT

      if (showCover) {
         thumb = Thumbnail()
         thumb.borderVisible = true
         thumb.pane.prefSize = 200 x 200
         coverContainer.layFullArea += thumb.pane
      } else {
         thumb = null
         children -= coverContainer
      }
   }

   override fun read(songs: List<Song>) = read(songs.firstOrNull())

   override fun read(song: Song?) = setValue(song?.toMeta() ?: Metadata.EMPTY)

   /** Displays metadata information. */
   fun setValue(m: Metadata) {
      dataId = m.identityHashCode()
      thumb?.loadCoverOf(m)
      indexL.text = m.getPlaylistIndexInfo().toString()
      songL.text = m.getTitle() ?: m.getFilename()
      artistL.text = m.getArtist() ?: "<none>"
      rating.rating.value = m.getRatingPercent()
      albumL.text = m.getAlbum() ?: "<none>"
   }

   private fun Thumbnail.loadCoverOf(data: Metadata) {
      val id = data.identityHashCode()
      runIO {
         data.getCover(ANY)
      } ui {
         if (dataId==id) loadCover(it)
      }
   }

}