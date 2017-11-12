package services.notif

import audio.Player
import audio.playback.PLAYBACK
import audio.tagging.Metadata
import gui.infonode.ItemInfo
import gui.objects.Text
import gui.objects.popover.ScreenPos
import gui.objects.popover.ScreenPos.SCREEN_BOTTOM_RIGHT
import gui.objects.popover.ScreenUse
import gui.objects.popover.ScreenUse.APP_WINDOW
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import layout.widget.WidgetManager.WidgetSource.NEW
import layout.widget.feature.SongReader
import main.App.APP
import services.ServiceBase
import util.access.VarAction
import util.access.VarEnum
import util.action.Action
import util.action.IsAction
import util.action.IsActionable
import util.conf.IsConfig
import util.conf.IsConfig.EditMode
import util.conf.IsConfigurable
import util.math.millis
import util.reactive.Disposer
import util.reactive.attach
import java.util.function.Consumer
import kotlin.streams.asSequence

/** Provides notification functionality.  */
@Suppress("unused")
@IsActionable
@IsConfigurable("Notifications")
class Notifier: ServiceBase(true) {
    @IsConfig(name = "On playback status change")
    var showStatusNotification = true
    @IsConfig(name = "On playing song change")
    var showSongNotification = true
    @IsConfig(name = "Autohide", info = "Whether notification hides on mouse click anywhere within the application", editable = EditMode.NONE)
    val notificationAutohide = false
    @IsConfig(name = "Autohide delay", info = "Time it takes for the notification to hide on its own")
    var notificationDuration = millis(2500)
    @IsConfig(name = "Animate", info = "Use animations on the notification")
    var notificationAnimated = true
    @IsConfig(name = "Animation duration")
    var notificationFadeTime = millis(250)
    @IsConfig(name = "Position", info = "Position within the virtual bounding box, which is relative to screen or window")
    var notificationPos: ScreenPos = SCREEN_BOTTOM_RIGHT
    @IsConfig(name = "Position relative to", info = "Determines screen for positioning. Main screen, application window screen or all screens as one")
    var notificationScr: ScreenUse = APP_WINDOW
    @IsConfig(name = "On click left", info = "Left click action")
    val onClickL = VarAction("Show application", Action.EMPTY)
    @IsConfig(name = "On click right", info = "Right click action")
    val onClickR = VarAction("Notification hide", Action.EMPTY)
    @IsConfig(name = "Playback change graphics")
    val graphics = VarEnum.ofSequence("Normal",
            {
                APP.widgetManager.getFactories().asSequence()
                        .filter { it.hasFeature(SongReader::class.java) }
                        .map { it.nameGui() }
                        .plus("Normal")
                        .plus("Normal - no cover")
            },
            Consumer { v ->
                when (v) {
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
                    else -> APP.widgetManager.find(v, NEW, true).ifPresent { wf ->
                        songNotificationGui = wf.load()
                        songNotificationInfo = wf.controller as SongReader
                        (songNotificationGui as Pane).setPrefSize(900.0, 500.0)
                    }
                // TODO: fix possible null ?
                }
            })
    private val onStop = Disposer()
    private var running = false
    private var n: Notification? = null
    private var songNotificationGui: Node? = null
    private var songNotificationInfo: SongReader? = null

    override fun start() {
        n = Notification()
        onStop += Player.playingItem.onChange { it -> songChange(it) }
        onStop += PLAYBACK.statusProperty() attach {
            if (it==PAUSED || it==PLAYING || it==STOPPED)
                playbackChange(it)
        }
        running = true
    }

    override fun isRunning(): Boolean = running

    override fun stop() {
        running = false
        onStop()
        n?.hideImmediatelly()
        n = null
    }

    override fun isSupported(): Boolean = true

    /** Show notification for custom content.  */
    fun showNotification(content: Node, title: String) {
        if (running) {
            n!!.run {
                setContent(content, title)
                isAutoHide = notificationAutohide
                animated.value = notificationAnimated
                animationDuration.value = notificationFadeTime
                duration = notificationDuration
                focusOnShow.value = false
                lClickAction = onClickL.getValueAction()
                rClickAction = onClickR.getValueAction()
                screenPreference = notificationScr
                show(notificationPos)
            }
        }
    }

    /** Show notification displaying given text.  */
    fun showTextNotification(text: String, title: String) {
        if (running) {
            val message = Text(text).apply {
                wrappingWithNatural.value = true
            }
            val root = StackPane(message).apply {
                setMinSize(150.0, 70.0)
            }

            showNotification(root, title)
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
    fun showNowPlayingNotification() = songChange(Player.playingItem.get())

    private fun songChange(m: Metadata) {
        if (showSongNotification) {
            val title = "Now playing \t${m.getPlaylistIndexInfo()}"
            songNotificationInfo!!.read(m)

            showNotification(songNotificationGui!!, title)
        }
    }

    private fun playbackChange(s: Status?) {
        if (showStatusNotification && s!=null) {
            val title = "Playback change : $s"
            val i = ItemInfo(false).apply {
                read(Player.playingItem.get())
            }

            showNotification(i, title)
        }
    }

}