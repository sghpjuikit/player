package sp.it.pl.ui.objects.table

import javafx.scene.media.MediaPlayer.Status.PAUSED as STATE_PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING as STATE_PLAYING
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.PlaylistSong.Field.LENGTH
import sp.it.pl.audio.playlist.PlaylistSong.Field.NAME
import sp.it.pl.layout.ComponentLoader.POPUP
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.main.APP
import sp.it.pl.main.IconMA
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.util.access.fieldvalue.MetaField
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.vAlways
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.type.isSubclassOf
import sp.it.util.type.type
import sp.it.util.ui.tableColumn
import sp.it.util.ui.traverseParents

object PLAYING: ObjectFieldBase<Song, String>(type(), { "" }, "Playing", "An UI column providing pause/resume icon for playing song row", { _, _ -> "" }), MetaField

fun PlaylistTable.buildColumn(f: ObjectField<PlaylistSong, Any?>): TableColumn<PlaylistSong,Any> = when (f) {
   PLAYING -> buildPlayingFieldColumn()
   else -> tableColumn {
      text = f.cName()
      isResizable = true
      styleClass += if (f.type.isSubclassOf<String>()) "column-header-align-left" else "column-header-align-right"
      cellValueFactory = when (f) {
         NAME -> Callback { it.value!!.nameP.asIs() }
         LENGTH -> Callback { it.value!!.timeP.asIs() }
         else -> Callback { if (it.value==null) null else PojoV(f.getOf(it.value)) }
      }
      cellFactory = Callback { f.buildFieldedCell<PlaylistSong, Any?>() }
   }
}

fun PlaylistTable.buildPlayingFieldColumn(): TableColumn<PlaylistSong, Any> = tableColumn {
   isSortable = false
   isResizable = true
   cellValueFactory = Callback { vAlways(Unit) }
   cellFactory = Callback { buildPlayingFieldCell(it) }
}

fun PlaylistTable.buildPlayingFieldCell(column: TableColumn<PlaylistSong, Any>): TableCell<PlaylistSong, Any> {
   fun computeIcon() = if (APP.audio.state.playback.status.value==STATE_PLAYING && playlist.isPlaying) IconMA.PAUSE_CIRCLE_FILLED else IconMA.PLAY_CIRCLE_FILLED
   val icon by lazy {
      val pt = this
      val ic = pt.properties["playing_icon"]?.asIf<Icon>() ?: Icon().apply {
               pt.properties["playing_icon"] = this

         isFocusTraversable = false
         styleclass("playlist-table-cell-playing-icon")
         selectHard(true)
         onClickDo(null, 1) { _, e ->
            if (e==null) return@onClickDo
            // play/pause on LMB
            if (e.button==PRIMARY) {
               val song = traverseParents().filterIsInstance<TableRow<*>>().firstOrNull()?.item?.asIf<PlaylistSong>()
               if (APP.audio.state.playback.status.value==STATE_PLAYING && playlist.isPlaying) APP.audio.pause()
               else if (APP.audio.state.playback.status.value==STATE_PAUSED && playlist.isPlaying) APP.audio.resume()
               else playlist.playTransformedItem(song)
            }
            if (e.button==SECONDARY) {
               // open playback controls on RMB
               val widgetKey = "playlist-table-playback-control-widget}"
               fun obtainWidget() = APP.widgetManager.widgets.findAll(OPEN).find { widgetKey in it.properties }?.apply { focusWithWindow() }
               fun buildWidget() = APP.widgetManager.widgets.find("Playback knobs", NEW(POPUP))?.apply { properties[widgetKey] = widgetKey }
               obtainWidget() ?: buildWidget()
            }
         }
      }
      APP.audio.state.playback.status sync { ic.icon(computeIcon()) } on disposer
      playlist.playingSong sync { it ->
         ic.icon(computeIcon())
         val cellIndexV = column.tableView.items?.indexOf(it)
         val cellRow = cellIndexV?.let { i -> column.tableView.rows().find { it.index==i } }
         val cellIndexH = column.tableView.columns.indexOf(column).takeIf { it>=0 }
         val cell = if (cellIndexH==null || cellRow==null) null else cellRow.childrenUnmodifiable.find { it.asIf<TableCell<Any?,Any?>>()?.tableColumn==column }?.asIs<TableCell<Any?,Any?>>()
         cell?.graphic = if (it==cellRow!!.item) ic else null
      } on disposer
      ic
   }
   val cell = object: TableCell<PlaylistSong, Any>() {
      override fun updateItem(item: Any?, empty: Boolean) {
         super.updateItem(item, empty)
         alignment = CENTER
         contentDisplay = GRAPHIC_ONLY
         graphic = if (!empty && playlist.playing==tableRow?.item) icon else null
      }
   }
   return cell
}