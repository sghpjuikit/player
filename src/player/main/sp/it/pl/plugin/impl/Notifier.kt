package sp.it.pl.plugin.impl

import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.Pane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import sp.it.pl.audio.PlayerManager.Events.PlaybackSongChanged
import sp.it.pl.audio.PlayerManager.Events.PlaybackStatusChanged
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.widget.ComponentLoader.CUSTOM
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.hasFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppErrors
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.nodeinfo.ItemInfo
import sp.it.pl.ui.objects.Text
import sp.it.pl.ui.objects.window.ShowArea
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.VarAction
import sp.it.util.action.IsAction
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverterElement
import sp.it.util.conf.valuesIn
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.type.VType
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.ui.hyperlink
import sp.it.util.ui.lay
import sp.it.util.ui.minSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.seconds

/** Provides notification functionality. */
class Notifier: PluginBase() {

   private val onStop = Disposer()
   private var n = Notification()
   private lateinit var songNotificationGui: Node
   private lateinit var songNotificationInfo: SongReader

   val notifySources by cList<NotifySource<Any>>()
      .noPersist().readOnly().uiConverterElement { it.name }
      .def(name = "Triggers", info = "Shows active event notification triggers. The handlers may have additional logic discarding some of the events.")
   val showStatusNotification by cv(false) {
         NotifySource<PlaybackStatusChanged>(type(), "On playback status change") { playbackChange(it.status) }.toSubscribed(onStop).toV(it)
      }
      .def(name = "On playback status change")
   val showSongNotification by cv(true) {
         NotifySource<PlaybackSongChanged>(type(), "On playback song change") { songChange(it.song) }.toSubscribed(onStop).toV(it)
      }
      .def(name = "On playing song change")
   val notificationAutohide by c(false)
      .def(name = "Autohide", info = "Whether notification hides on mouse click anywhere within the application", editable = EditMode.NONE)
   var notificationDuration by c(2500.millis)
      .def(name = "Autohide delay", info = "Time it takes for the notification to hide on its own")
   var notificationPos by c(Pos.BOTTOM_RIGHT)
      .def(name = "Position", info = "Position within the virtual bounding box, which is relative to screen or window")
   var notificationScr by c(ShowArea.SCREEN_ACTIVE)
      .def(name = "Position relative to", info = "Determines screen for positioning. Main screen, application window screen or all screens as one")
   val onClickL by cv("Show application") { VarAction(it) }
      .def(name = "On click left", info = "Left click action")
   val onClickR by cv("Notification hide") { VarAction(it) }
      .def(name = "On click right", info = "Right click action")
   val graphics by cv("Normal").def(
      name = "Playback change graphics"
   ).valuesIn {
      APP.widgetManager.factories.getFactories()
         .filter { it.hasFeature<SongReader>() }
         .map { it.name }
         .plus("Normal")
         .plus("Normal - no cover")
   } sync {
      when (it) {
         "Normal" -> {
            val ii = ItemInfo(true)
            songNotificationInfo = ii
            songNotificationGui = ii
            (songNotificationGui as Pane).setPrefSize(-1.0, -1.0)
         }
         "Normal - no cover" -> {
            val ii = ItemInfo(false)
            songNotificationInfo = ii
            songNotificationGui = ii
            (songNotificationGui as Pane).setPrefSize(-1.0, -1.0)
         }
         else -> APP.widgetManager.widgets.find(it, NEW(CUSTOM))
            .ifNotNull { wf ->
               songNotificationGui = wf.load()
               songNotificationInfo = wf.controller as SongReader
               (songNotificationGui as Pane).setPrefSize(900.0, 500.0)
            }
            .ifNull {
               val ii = ItemInfo(true)
               songNotificationInfo = ii
               songNotificationGui = ii
               (songNotificationGui as Pane).setPrefSize(-1.0, -1.0)
            }
      }
   }

   override fun start() {
      notifySources.onItemSyncWhile { s -> APP.actionStream.onEvent(s.type.raw) { s.block(this, it) } } on onStop
   }

   override fun stop() {
      onStop()
      n.hideImmediately()
   }

   /** Show notification for custom content. */
   fun showNotification(title: String, content: Node) {
      n.setContent(content, title)
      n.isAutohide.value = notificationAutohide
      n.duration = notificationDuration
      n.rClickAction = onClickR.valueAsAction
      n.lClickAction = onClickL.valueAsAction
      n.show(notificationScr(notificationPos))
   }

   /** Show notification displaying given text. */
   fun showTextNotification(error: AppError) {
      val root = vBox(10.0, CENTER_LEFT) {
         lay += Text(error.textShort).apply {
            wrappingWithNatural.subscribe()
         }
         lay += hyperlink("Click to show full details") {
            onEventDown(MOUSE_CLICKED, PRIMARY) { AppErrors.showDetailForLastError() }
         }
         lay += supplyIf(error.action!=null) {
            hyperlink(error.action!!.name) {
               onEventDown(MOUSE_CLICKED, PRIMARY) { error.action.action() }
            }
         }
      }

      showNotification("Error", root)
   }

   /** Show notification displaying given text. */
   fun showTextNotification(title: String, contentText: String) {
      val root = stackPane {
         lay += Text(contentText).apply {
            wrappingWithNatural.subscribe()
         }
      }

      showNotification(title, root)
   }

   /** Hide notification if showing, otherwise does nothing. */
   @IsAction(name = "Notification hide", info = "Hide notification if it is showing")
   fun hideNotification() {
      n.hide()
   }

   @IsAction(name = "Notify now playing", info = "Shows notification about currently playing song.", global = true, keys = "ALT + N")
   fun showNowPlayingNotification() = songChange(APP.audio.playingSong.value)

   private fun songChange(song: Metadata) {
      if (!song.isEmpty()) {
         val title = "Now playing \t${song.getPlaylistIndexInfo()}"
         songNotificationInfo.read(song)

         showNotification(title, songNotificationGui)
      }
   }

   private fun playbackChange(status: Status) {
      if (status==PAUSED || status==PLAYING || status==STOPPED) {
         val title = "Playback change : $status"
         val i = ItemInfo(false).apply {
            read(APP.audio.playingSong.value)
         }

         showNotification(title, i)
      }
   }

   companion object: PluginInfo {
      override val name = "Notifications"
      override val description = "Provides a general purpose corner notification"
      override val isSupported = true
      override val isSingleton = false
      override val isEnabledByDefault = true
   }
}

/** Application event handler for events of the specified type to show notification using the specified block. */
data class NotifySource<out T: Any>(val type: VType<T>, val name: String, val block: Notifier.(@UnsafeVariance T) -> Unit) {

   /**
    * Returns subscribed that enables/disables notifications from this source through [Notifier.notifySources]
    * for [Notifier] plugin using [sp.it.pl.plugin.PluginManager.syncWhile].
    */
   fun toSubscribed(disposer: Disposer? = null): Subscribed = Subscribed {
      APP.plugins.syncWhile<Notifier> {
         if (this !in it.notifySources) it.notifySources += this
         Subscription { it.notifySources -= this }
      }.apply {
         disposer?.plusAssign { unsubscribe() }
      }
   }
}

/** Notification popup. */
class Notification: PopWindow() {
   /** Invokes [hide] with delay of inactivity. */
   private val closer = fxTimer(5.seconds, 1, ::hide)
   /** Executes on left mouse click. Default does nothing. */
   var lClickAction = {}
   /** Executes on right mouse click. Default does nothing. */
   var rClickAction = {}
   /** Time this notification will remain visible. Default 5 seconds. */
   var duration: Duration
      get() = closer.period
      set(duration) {
         closer.period = duration
      }

   init {
      userResizable.value = false
      userMovable.value = false
      isEscapeHide.value = false
      isAutohide.value = false
      headerIconsVisible.value = false
      focusOnShow.value = false
      styleClass += "notification"
      onContentShown += { closer.start() }
      root.apply {
         minSize = 150 x 70
         onEventDown(MOUSE_CLICKED, PRIMARY) { lClickAction() }
         onEventDown(MOUSE_CLICKED, SECONDARY) { rClickAction() }
         onEventUp(MOUSE_ENTERED) { closer.pause() }
         onEventUp(MOUSE_EXITED) { closer.unpause() }
      }
   }

   fun setContent(content: Node, titleText: String) {
      headerVisible.value = titleText.isNotEmpty()
      title.value = titleText
      this.content.value = content
   }

}