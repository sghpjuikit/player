package sp.it.pl.plugin.notif

import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import sp.it.pl.audio.Player
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.nodeinfo.ItemInfo
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.gui.objects.popover.ScreenUse
import sp.it.pl.layout.widget.WidgetLoader.CUSTOM
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.hasFeature
import sp.it.pl.main.APP
import sp.it.pl.main.AppErrors
import sp.it.pl.plugin.PluginBase
import sp.it.util.access.VarAction
import sp.it.util.action.IsAction
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.valuesIn
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.units.millis

/** Provides notification functionality. */
class Notifier: PluginBase("Notifications", true) {
   @IsConfig(name = "On playback status change")
   var showStatusNotification by c(true)
   @IsConfig(name = "On playing song change")
   var showSongNotification by c(true)
   @IsConfig(name = "Autohide", info = "Whether notification hides on mouse click anywhere within the application", editable = EditMode.NONE)
   val notificationAutohide by c(false)
   @IsConfig(name = "Autohide delay", info = "Time it takes for the notification to hide on its own")
   var notificationDuration by c(2500.millis)
   @IsConfig(name = "Animate", info = "Use animations on the notification")
   var notificationAnimated by c(true)
   @IsConfig(name = "Animation duration")
   var notificationFadeTime = 250.millis
   @IsConfig(name = "Position", info = "Position within the virtual bounding box, which is relative to screen or window")
   var notificationPos by c(ScreenPos.SCREEN_BOTTOM_RIGHT)
   @IsConfig(name = "Position relative to", info = "Determines screen for positioning. Main screen, application window screen or all screens as one")
   var notificationScr by c(ScreenUse.APP_WINDOW)
   @IsConfig(name = "On click left", info = "Left click action")
   val onClickL by cv("Show application") { VarAction(it) }
   @IsConfig(name = "On click right", info = "Right click action")
   val onClickR by cv("Notification hide") { VarAction(it) }
   @IsConfig(name = "Playback change graphics")
   val graphics by cv("Normal").valuesIn {
      APP.widgetManager.factories.getFactories()
         .filter { it.hasFeature<SongReader>() }
         .map { it.nameGui() }
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
         else -> APP.widgetManager.widgets.find(it, NEW(CUSTOM)).ifPresentOrElse(
            { wf ->
               songNotificationGui = wf.load()
               songNotificationInfo = wf.controller as SongReader
               (songNotificationGui as Pane).setPrefSize(900.0, 500.0)
            },
            {
               val ii = ItemInfo(true)
               songNotificationInfo = ii
               songNotificationGui = ii
               (songNotificationGui as Pane).setPrefSize(-1.0, -1.0)
            }
         )
      }
   }
   private val onStop = Disposer()
   private var running = false
   private var n: Notification? = null
   private var songNotificationGui: Node? = null
   private var songNotificationInfo: SongReader? = null

   override fun start() {
      n = Notification()
      onStop += Player.playingSong.onChange { it -> songChange(it) }
      onStop += Player.state.playback.status attach {
         if (it==PAUSED || it==PLAYING || it==STOPPED)
            playbackChange(it)
      }
      running = true
   }

   override fun isRunning(): Boolean = running

   override fun stop() {
      running = false
      onStop()
      n?.hideImmediately()
      n = null
   }

   override fun isSupported(): Boolean = true

   /** Show notification for custom content. */
   fun showNotification(title: String, content: Node) {
      if (running) {
         with(n!!) {
            setContent(content, title)
            isAutoHide = notificationAutohide
            animated.value = notificationAnimated
            animationDuration.value = notificationFadeTime
            duration = notificationDuration
            focusOnShow.value = false
            screenPreference = notificationScr
            rClickAction = onClickR.valueAsAction
            lClickAction = when (title) {
               AppErrors.ERROR_NOTIFICATION_TITLE -> Runnable { AppErrors.showDetailForLastError() }
               else -> onClickL.valueAsAction
            }
            show(notificationPos)
         }
      }
   }

   /** Show notification displaying given text. */
   fun showTextNotification(title: String, contentText: String) {
      if (running) {
         val root = stackPane {
            setMinSize(150.0, 70.0)

            lay += Text(contentText).apply {
               wrappingWithNatural.value = true
            }
         }

         showNotification(title, root)
      }
   }

   /** Hide notification if showing, otherwise does nothing. */
   @IsAction(name = "Notification hide")
   fun hideNotification() {
      if (running) {
         n?.hide()
      }
   }

   @IsAction(name = "Notify now playing", desc = "Shows notification about currently playing song.", global = true, keys = "ALT + N")
   fun showNowPlayingNotification() = songChange(Player.playingSong.value)

   private fun songChange(m: Metadata) {
      if (showSongNotification && !m.isEmpty()) {
         val title = "Now playing \t${m.getPlaylistIndexInfo()}"
         songNotificationInfo!!.read(m)

         showNotification(title, songNotificationGui!!)
      }
   }

   private fun playbackChange(status: Status?) {
      if (showStatusNotification && status!=null) {
         val title = "Playback change : $status"
         val i = ItemInfo(false).apply {
            read(Player.playingSong.value)
         }

         showNotification(title, i)
      }
   }

}