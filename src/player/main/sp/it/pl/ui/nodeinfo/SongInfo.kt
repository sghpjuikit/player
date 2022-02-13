package sp.it.pl.ui.nodeinfo

import java.util.concurrent.atomic.AtomicLong
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.image.Cover.CoverSource.ANY
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.rating.Rating
import sp.it.util.access.toWritable
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attachNonNullWhile
import sp.it.util.reactive.on
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.vBox
import sp.it.util.ui.x

/** Basic display for song information. */
class SongInfo(showCover: Boolean = true): HBox(15.0), SongReader {

   private val rating = Rating().apply { alignment.value = CENTER_LEFT }
   private val indexL = label()
   private val songL = label()
   private val artistL = label()
   private val ratingL = label { graphic = rating }
   private val albumL = label()
   private var coverContainer = AnchorPane()
   private val thumb = if (!showCover) null else Thumbnail().apply {
      borderVisible = true
      pane.prefSize = 200 x 200
   }
   private var dataId = AtomicLong(1L)
   private var songImpl: Metadata? = null
   private val dongImplInvListeners = mutableListOf<InvalidationListener>()
   private val dongImplChaListeners = mutableListOf<ChangeListener<in Metadata?>>()
   /** Displayed song. */
   val song = object: ObservableValue<Metadata?> {
      override fun getValue() = songImpl
      override fun addListener(listener: InvalidationListener) = dongImplInvListeners.add(listener).toUnit()
      override fun addListener(listener: ChangeListener<in Metadata?>) = dongImplChaListeners.add(listener).toUnit()
      override fun removeListener(listener: InvalidationListener) = dongImplInvListeners.remove(listener).toUnit()
      override fun removeListener(listener: ChangeListener<in Metadata?>) = dongImplChaListeners.remove(listener).toUnit()
   }.let { o ->
      o.toWritable { nv ->
         val ov = songImpl
         if (ov!==nv) {
            val m = nv ?: Metadata.EMPTY
            songImpl = m
            thumb?.loadCoverOf(m)
            indexL.text = m.getPlaylistIndexInfo().toString()
            songL.text = m.getTitle() ?: m.getFilename()
            artistL.text = m.getArtist() ?: "<none>"
            rating.rating.value = m.getRatingPercent()
            albumL.text = m.getAlbum() ?: "<none>"
            dongImplInvListeners.forEach { it.invalidated(o) }
            dongImplChaListeners.forEach { it.changed(o, ov, m) }
         }
      }
   }

   init {
      padding = Insets(10.0)
      lay += coverContainer
      lay += vBox(5.0, CENTER_RIGHT) {
         prefSize = -1 x -1
         lay += listOf(indexL, songL, artistL, ratingL, albumL)
      }

      if (thumb!=null) coverContainer.layFullArea += thumb.pane
      else children -= coverContainer

      // keep updated content
      sceneProperty().attachNonNullWhile { APP.audio.onSongRefresh(::songImpl, ::read) } on onNodeDispose
   }

   override fun read(songs: List<Song>) = read(songs.firstOrNull())

   override fun read(song: Song?) = apply { this.song.value = song?.toMeta() }.toUnit()

   private fun Thumbnail.loadCoverOf(data: Metadata) {
      val id = dataId.incrementAndGet()
      runIO {
         if (dataId.get()==id) {
            val cover = data.getCover(ANY)
            runFX {
               if (dataId.get()==id)
                  loadCover(cover)
            }
         }
      }
   }

}