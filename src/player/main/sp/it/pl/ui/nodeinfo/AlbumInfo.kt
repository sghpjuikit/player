package sp.it.pl.ui.nodeinfo

import java.util.concurrent.atomic.AtomicLong
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Cover.CoverSource.ANY
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.access.toWritable
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attachNonNullWhile
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.text.pluralUnit
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.prefSize
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs

/** Basic display for song album information. */
class AlbumInfo(showCover: Boolean = true): HBox(15.0), SongReader {

   private val nameL = label { styleClass += listOf("h4", "h4p-up") }
   private val artistL = label { styleClass += listOf("h4p-bottom") }
   private val yearL = label()
   private val tracksL = label()
   private val discsL = label()
   private val lengthL = label()
   private var coverContainer = AnchorPane()
   private val thumb = if (!showCover) null else Thumbnail().apply {
      borderVisible = true
      pane.prefSize = 200 x 200
   }
   private var dataId = AtomicLong(1L)
   private var songImpl: Metadata? = null
   private val songImplInvListeners = mutableListOf<InvalidationListener>()
   private val songImplChaListeners = mutableListOf<ChangeListener<in Metadata?>>()
   /** Displayed song. */
   val song = object: ObservableValue<Metadata?> {
      override fun getValue() = songImpl
      override fun addListener(listener: InvalidationListener) = songImplInvListeners.add(listener).toUnit()
      override fun addListener(listener: ChangeListener<in Metadata?>) = songImplChaListeners.add(listener).toUnit()
      override fun removeListener(listener: InvalidationListener) = songImplInvListeners.remove(listener).toUnit()
      override fun removeListener(listener: ChangeListener<in Metadata?>) = songImplChaListeners.remove(listener).toUnit()
   }.let { o ->
      o.toWritable { nv ->
         val ov = songImpl
         if (ov!==nv) {
            val m = nv ?: Metadata.EMPTY
            songImpl = m
            val album = m.getAlbum()
            val artist = m.getAlbumArtist()
            val albumSongs = if (m.isEmpty()) listOf() else APP.db.songsById.filter { it.getAlbum()!=null && album==it.getAlbum() && artist==it.getAlbumArtist() }

            thumb?.loadCoverOf(m)
            nameL.text = m.getAlbum() ?: "n/a"
            artistL.text = m.getAlbumArtist() ?: "n/a"
            yearL.text = m.getYear()?.toUi() ?: "n/a"
            tracksL.text = "song".pluralUnit(m.getTracksTotal() ?: albumSongs.size)
            discsL.text = m.getDiscsTotal()?.net("disc"::pluralUnit) ?: "n/a"
            lengthL.text = albumSongs.sumOf { it.getLengthInMs() }.millis.toHMSMs()
            songImplInvListeners.forEach { it.invalidated(o) }
            songImplChaListeners.forEach { it.changed(o, ov, m) }
         }
      }
   }

   init {
      padding = Insets(10.0)
      lay += coverContainer
      lay += vBox {
         alignmentProperty() syncFrom this@AlbumInfo.alignmentProperty()
         prefSize = -1 x -1
         lay += nameL
         lay += artistL
         lay += hBox {
            alignmentProperty() syncFrom this@AlbumInfo.alignmentProperty().map { when (it.hpos!!) { HPos.LEFT -> CENTER_LEFT; HPos.CENTER -> CENTER; HPos.RIGHT -> CENTER_RIGHT } }
            lay += Icon(IconFA.CALENDAR).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += yearL
            lay += Icon(IconFA.CLOCK_ALT).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += lengthL
         }
         lay += hBox {
            alignmentProperty() syncFrom this@AlbumInfo.alignmentProperty().map { when (it.hpos!!) { HPos.LEFT -> CENTER_LEFT; HPos.CENTER -> CENTER; HPos.RIGHT -> CENTER_RIGHT } }
            lay += Icon(IconFA.MUSIC).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += tracksL
            lay += Icon(IconMA.ALBUM).apply { isMouseTransparent = true; isFocusTraversable = false }
            lay += discsL
         }
      }

      if (thumb!=null) coverContainer.layFullArea += thumb.pane
      else children -= coverContainer

      // keep updated content
      sceneProperty().attachNonNullWhile { APP.audio.onSongRefresh(::songImpl, ::read) } on onNodeDispose

      // initialize
      song.value = null
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