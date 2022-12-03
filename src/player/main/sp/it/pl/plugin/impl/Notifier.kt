package sp.it.pl.plugin.impl

import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import javafx.util.Duration
import javafx.util.Duration.INDEFINITE
import sp.it.pl.audio.PlayerManager.Events.PlaybackSongChanged
import sp.it.pl.audio.PlayerManager.Events.PlaybackStatusChanged
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.conf.Command
import sp.it.pl.conf.Command.DoAction
import sp.it.pl.layout.ComponentLoader.CUSTOM
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.hasFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.emScaled
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.nodeinfo.SongInfo
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.rating.Rating
import sp.it.pl.ui.objects.window.ShowArea
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.action.IsAction
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.materialize
import sp.it.util.conf.EditMode
import sp.it.util.conf.butElement
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.conf.readOnly
import sp.it.util.conf.uiConverter
import sp.it.util.conf.valuesIn
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.type.VType
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.ui.containsMouse
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
   private val ns = mutableSetOf<Notification>()
   private lateinit var songNotificationUi: Node
   private lateinit var songNotificationInfo: SongReader
   private var songNotificationSubscription: Subscription? = null
   private var songPlaybackNotificationUi: StackPane? = null
   private val songRateNotificationUi by lazy { Rating() }

   val notifySources by cList<NotifySource<Any>>()
      .noPersist().readOnly().butElement { uiConverter { it.name } }
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
      .def(name = "Autohide delay", info = "Time it takes for the notification to hide on its own. Ignored if notification is permanent")
   var notificationPos by c(Pos.BOTTOM_RIGHT)
      .def(name = "Position", info = "Position within the virtual bounding box, which is relative to screen or window")
   var notificationScr by c(ShowArea.SCREEN_ACTIVE)
      .def(name = "Position relative to", info = "Determines screen for positioning. Main screen, application window screen or all screens as one")
   val onClickL by cv<Command>(DoAction("Show application"))
      .def(name = "On click left", info = "Left click action")
   val onClickR by cv<Command>(DoAction("Notification hide"))
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
            val ii = SongInfo(true)
            songNotificationInfo = ii
            songNotificationUi = ii
            (songNotificationUi as Pane).setPrefSize(-1.0, -1.0)
         }
         "Normal - no cover" -> {
            val ii = SongInfo(false)
            songNotificationInfo = ii
            songNotificationUi = ii
            (songNotificationUi as Pane).setPrefSize(-1.0, -1.0)
         }
         else -> APP.widgetManager.widgets.find(it, NEW(CUSTOM))
            .ifNotNull { wf ->
               songNotificationUi = wf.load()
               songNotificationInfo = wf.controller as SongReader
               (songNotificationUi as Pane).setPrefSize(600.emScaled, 250.emScaled)
            }
            .ifNull {
               val ii = SongInfo(true)
               songNotificationInfo = ii
               songNotificationUi = ii
               (songNotificationUi as Pane).setPrefSize(-1.0, -1.0)
            }
      }
   }

   override fun start() {
      notifySources.onItemSyncWhile { s -> APP.actionStream.onEvent(s.type.raw) { s.block(this, it) } } on onStop
   }

   override fun stop() {
      onStop()
      ns.materialize().forEach(Notification::hideImmediately)
   }

   /** Show notification for custom content. */
   fun showNotification(title: String, content: Node, isPermanent: Boolean = false): Notification {
      val n = ns.find { it.content.value === content } ?: Notification()
      val isReused = n in ns
      val nss = ns - n

      n.setContent(content, title)
      n.isAutohide.value = notificationAutohide
      n.duration = if (isPermanent) INDEFINITE else notificationDuration
      n.rClickAction = onClickR.value
      n.lClickAction = onClickL.value
      n.onShown attach1 { ns += n }
      n.onHidden attach1 { ns -= n }
      n.show(
         notificationScr(notificationPos).map {
            if (nss.isEmpty()) it
            else if (isReused) it.x x y
            else when(notificationPos.vpos!!) {
               VPos.BOTTOM, VPos.CENTER -> it.x x ((nss.minOfOrNull { it.root.localToScreen(0.0, 0.0).y } ?: 0.0) - n.root.height)
               VPos.BASELINE, VPos.TOP -> it.x x (nss.maxOfOrNull { it.root.localToScreen(0.0, it.root.height).y } ?: notificationScr.bounds().second.maxY)
            }
         }
      )

      return n
   }

   /** Show notification displaying given text. */
   fun showTextNotification(error: AppError, isPermanent: Boolean = false): Notification {
      val root = vBox(10.0, CENTER_LEFT) {
         lay += SpitText(error.textShort).apply {
            wrappingWithNatural.subscribe()
         }
         lay += hyperlink("Show full details") {
            onEventDown(MOUSE_CLICKED, PRIMARY) { AppEventLog.showDetailForLastError() }
         }
         lay += supplyIf(error.action!=null) {
            hyperlink(error.action!!.name) {
               onEventDown(MOUSE_CLICKED, PRIMARY) { error.action.action() }
            }
         }
      }

      return showNotification("Error", root, isPermanent)
   }

   /** Show notification displaying given text. */
   fun showTextNotification(title: String, contentText: String, isPermanent: Boolean = false): Notification {
      val root = stackPane {
         lay += SpitText(contentText).apply {
            wrappingWithNatural.subscribe()
         }
      }

      return showNotification(title, root, isPermanent)
   }

   /** Hides hovered notification or last shown notification or does nothing if no showing. */
   @IsAction(name = "Notification hide", info = "Hides hovered notification or last shown notification or does nothing if no showing.")
   fun hideNotification() {
      val n = ns.find { it.root.containsMouse() } ?: ns.lastOrNull()
      n?.hide()
   }

   @IsAction(name = "Notify now playing", info = "Shows notification about currently playing song.", global = true, keys = "ALT + N")
   fun showNowPlayingNotification() = songChange(APP.audio.playingSong.value)

   fun showSongRatingChangedNotification(rating: Double?) {
      showNotification("Song rating changed", songRateNotificationUi)
      songRateNotificationUi.rating.value = rating
   }

   private fun songChange(song: Metadata) {
      if (!song.isEmpty()) {
         fun Metadata.computeSongChangeTitle() = "Now playing \t${getPlaylistIndexInfo()}"

         // keep updating song
         songNotificationSubscription?.unsubscribe()
         songNotificationSubscription = APP.audio.playingSong.updated sync { songNotificationInfo.read(it) }

         val n = showNotification(song.computeSongChangeTitle(), songNotificationUi)

         // keep updating title
         val s = APP.audio.playingSong.updated attach { n.title.value = "Now playing \t${it.computeSongChangeTitle()}" }
         n.onHidden += { s.unsubscribe() }
         n.onHidden += { songNotificationSubscription?.unsubscribe() }
      }
   }

   private fun playbackChange(status: Status) {
      if (status==PAUSED || status==PLAYING || status==STOPPED) {
         val title = "Playback change : $status"
         val disposer = Disposer()
         val i = (songPlaybackNotificationUi ?: StackPane()).apply {
            lay += SongInfo(false).apply {
               APP.audio.playingSong.updated sync { read(it) } on disposer
            }
         }
         songPlaybackNotificationUi = i

         showNotification(title, i).apply {
            onHidden += { songPlaybackNotificationUi = null }
         }
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
      onContentShown += { if (!root.isHover) closer.start() }
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