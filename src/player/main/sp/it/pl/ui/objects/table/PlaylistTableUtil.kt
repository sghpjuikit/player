package sp.it.pl.ui.objects.table

import javafx.scene.media.MediaPlayer.Status.PAUSED as STATE_PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING as STATE_PLAYING
import javafx.scene.Node
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.PlaylistSong.Field.LENGTH
import sp.it.pl.audio.playlist.PlaylistSong.Field.NAME
import sp.it.pl.main.APP
import sp.it.pl.main.IconMA
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.vAlways
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.traverse
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.type.type
import sp.it.util.ui.tableColumn

object PLAYING: ObjectFieldBase<Song, String>(type(), { "" }, "Playing", "An UI column providing pause/resume icon for playing song row", { _, _ -> "" })

fun PlaylistTable.buildColumnFactory(): (ObjectField<PlaylistSong, Any?>) -> TableColumn<PlaylistSong,Any> = { f ->
   when (f) {
      PLAYING -> buildPlayingFieldColumn()
      else -> tableColumn(f.toString()) {
         isResizable = true
         cellValueFactory = when (f) {
            NAME -> Callback { it.value!!.nameP.asIs() }
            LENGTH -> Callback { it.value!!.timeP.asIs() }
            else -> Callback { if (it.value==null) null else PojoV(f.getOf(it.value)) }
         }
         cellFactory = Callback { f.buildFieldedCell<PlaylistSong, Any?>() }
      }
   }
}

fun PlaylistTable.buildPlayingFieldColumn(): TableColumn<PlaylistSong, Any> = tableColumn("Playing") {
   isSortable = false
   isResizable = true
   cellValueFactory = Callback { vAlways(Unit) }
   cellFactory = Callback { buildPlayingFieldCell(it) }
}

fun PlaylistTable.buildPlayingFieldCell(column: TableColumn<PlaylistSong, Any>): TableCell<PlaylistSong, Any> {
   fun computeIcon() = if (APP.audio.state.playback.status.value==STATE_PLAYING && playlist.isPlaying) IconMA.PAUSE_CIRCLE_FILLED else IconMA.PLAY_CIRCLE_FILLED
   val icon by lazy {
      val ic = properties["playing_icon"]?.asIf<Icon>() ?: Icon().apply {
         properties["playing_icon"] = this
         isFocusTraversable = false
         styleclass("playlist-table-cell-playing-icon")
         onClickDo {
            val song = this.traverse<Node> { it.parent }.find { it is TableRow<*> }?.asIf<TableRow<*>>()?.item?.asIf<PlaylistSong>()
            if (APP.audio.state.playback.status.value==STATE_PLAYING && playlist.isPlaying) APP.audio.pause()
            else if (APP.audio.state.playback.status.value==STATE_PAUSED && playlist.isPlaying) APP.audio.resume()
            else playlist.playTransformedItem(song)
         }
      }
      APP.audio.state.playback.status sync { ic.icon(computeIcon()) } on disposer
      playlist.playingSong sync { it ->
         ic.icon(computeIcon())
         val cellIndexV = column.tableView.items?.indexOf(it)
         val cellRow = cellIndexV?.let { i -> column.tableView.rows().find { it.index==i } }
         val cellIndexH = column.tableView.columns.indexOf(column).takeIf { it>=0 }
         val cell = if (cellIndexH==null || cellRow==null || cellRow.childrenUnmodifiable.size<cellIndexH) null else cellRow.childrenUnmodifiable[cellIndexH]?.asIs<TableCell<Any?,Any?>>()
         cell?.graphic = if (it==cellRow!!.item) ic else null
      } on disposer
      ic
   }
   val cell = object: TableCell<PlaylistSong, Any>() {
      override fun updateItem(item: Any?, empty: Boolean) {
         super.updateItem(item, empty)
         graphic = if (!empty && playlist.playing==tableRow?.item) icon else null
      }
   }
   return cell
}