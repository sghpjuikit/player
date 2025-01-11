package sp.it.pl.ui.objects.table

import javafx.scene.media.MediaPlayer.Status.PAUSED as STATE_PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING as STATE_PLAYING
import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos.CENTER
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.PlaylistSong.Field.LENGTH
import sp.it.pl.audio.playlist.PlaylistSong.Field.NAME
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.layout.ComponentLoader
import sp.it.pl.layout.ComponentLoader.POPUP
import sp.it.pl.layout.ComponentLoader.POPUP_CUSTOM
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.main.APP
import sp.it.pl.main.IconMA
import sp.it.pl.main.isPlaying
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.stage.Window
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
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.tableColumn
import sp.it.util.ui.traverseParents

object PLAYING: ObjectFieldBase<Any?, String>(type(), { "" }, "Playing", "An UI column providing pause/resume icon for playing song row", { _, _ -> "" }), MetaField

fun PlaylistTable.buildColumn(f: ObjectField<PlaylistSong, Any?>): TableColumn<PlaylistSong,Any> = when (f) {
   PLAYING -> buildPlayingFieldColumn().asIs()
   else -> tableColumn {
      text = f.cName()
      isResizable = true
      styleClass += if (f.type.isSubclassOf<String>()) "column-header-align-left" else "column-header-align-right"
      cellValueFactory = when (f) {
         NAME -> Callback { it.value!!.nameP.asIs() }
         LENGTH -> Callback { it.value!!.timeP.asIs() }
         else -> Callback { if (it.value==null) null else vAlways(f.getOf(it.value)) }
      }
      cellFactory = Callback { f.buildFieldedCell<PlaylistSong, Any?>() }
   }
}

fun <T> TableView<T>.buildPlayingFieldColumn(): TableColumn<T, Any> = tableColumn {
   isSortable = false
   isResizable = true
   cellValueFactory = Callback { vAlways(Unit) }
   cellFactory = Callback { buildPlayingFieldCell(it) }
}

fun <T> TableView<T>.buildPlayingFieldCell(column: TableColumn<*, Any>): TableCell<T, Any> {
   val table = this
   val tableDisposer = if (table is PlaylistTable) table.disposer else table.onNodeDispose

   fun TableCell<*,*>.cellRowIsPlaying(): Boolean {
      return if (table is PlaylistTable) {
         table.playlist.playing==tableRow?.item
      } else {
         val rowItem = tableRow?.item
         when(rowItem) {
            is Song -> rowItem.isPlaying()
            is MetadataGroup -> rowItem.isPlaying()
            else -> false
         }
      }
   }
   fun cellComputeIcon(): GlyphIcons {
      return if (table is PlaylistTable) {
         if (APP.audio.state.playback.status.value==STATE_PLAYING && table.playlist.isPlaying) IconMA.PAUSE_CIRCLE_FILLED else IconMA.PLAY_CIRCLE_FILLED
      } else {
         if (APP.audio.state.playback.status.value==STATE_PLAYING) IconMA.PAUSE_CIRCLE_FILLED else IconMA.PLAY_CIRCLE_FILLED
      }
   }

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
               if (table is PlaylistTable) {
                  val song = traverseParents().filterIsInstance<TableRow<*>>().firstOrNull()?.item?.asIf<PlaylistSong>()
                  if (APP.audio.state.playback.status.value==STATE_PLAYING && table.playlist.isPlaying) APP.audio.pause()
                  else if (APP.audio.state.playback.status.value==STATE_PAUSED && table.playlist.isPlaying) APP.audio.resume()
                  else table.playlist.playTransformedItem(song)
               } else {
                  if (APP.audio.state.playback.status.value==STATE_PLAYING) APP.audio.pause()
                  else if (APP.audio.state.playback.status.value==STATE_PAUSED) APP.audio.resume()
               }
            }
            if (e.button==SECONDARY) {
               // open playback controls on RMB
               val widgetKey = "playlist-table-playback-control-widget"
               fun customizePopup(it: PopWindow) {
                  it.headerVisible.value = false
                  it.userResizable.value = false
                  it.root.style = "-fx-background-radius: 1000; -fx-border-radius: 1000;"
                  it.effect.override.value = true
                  it.effect.value = Window.BgrEffect.OFF
               }
               fun obtainWidget() = APP.widgetManager.widgets.findAll(OPEN).find { widgetKey in it.properties }?.apply { focusWithWindow() }
               fun buildWidget() = APP.widgetManager.widgets.find("Playback seeker", NEW(POPUP.customize(::customizePopup)(this)))?.apply { properties[widgetKey] = widgetKey }
               obtainWidget() ?: buildWidget()
            }
         }
      }

      fun cellUninstallIcon() {
         ic.traverseParents().filterIsInstance<TableCell<*,*>>().firstOrNull()?.graphic = null
      }
      fun cellInstallIcon(played: Any?) {
         val cellIndexV = column.tableView.items?.indexOfFirst(if (table is PlaylistTable) {{ it===played }} else {{ when (it) { is Song -> it.isPlaying(); is MetadataGroup -> it.isPlaying(); else -> false }}})
         val cellRow = cellIndexV?.let { i -> column.tableView.rows().find { it.index==i } }
         val cellIndexH = column.tableView.columns.indexOf(column).takeIf { it>=0 }
         val cell = if (cellIndexH==null || cellRow==null) null else cellRow.childrenUnmodifiable.find { it.asIf<TableCell<Any?,Any?>>()?.tableColumn==column }?.asIs<TableCell<Any?,Any?>>()
         cell?.graphic = if (cellIndexV==cellRow!!.index) ic else null
      }

      // keep changing icon glyph
      APP.audio.state.playback.status sync { ic.icon(cellComputeIcon()) } on tableDisposer
      // keep changing icon row
      (if (table is PlaylistTable) table.playlist.playingSong else APP.audio.playingSong.changed) sync {
         cellUninstallIcon()
         ic.icon(cellComputeIcon())
         cellInstallIcon(it)
      } on tableDisposer

      ic
   }
   val cell = object: TableCell<T, Any>() {
      override fun updateItem(item: Any?, empty: Boolean) {
         super.updateItem(item, empty)
         alignment = CENTER
         contentDisplay = GRAPHIC_ONLY
         graphic = if (!empty && cellRowIsPlaying()) icon else null
      }
   }
   return cell
}