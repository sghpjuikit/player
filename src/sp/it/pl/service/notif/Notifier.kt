package sp.it.pl.service.notif

import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PAUSED
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.STOPPED
import sp.it.pl.audio.Player
import sp.it.pl.audio.playback.PLAYBACK
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.infonode.ItemInfo
import sp.it.pl.gui.objects.Text
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.gui.objects.popover.ScreenPos.SCREEN_BOTTOM_RIGHT
import sp.it.pl.gui.objects.popover.ScreenUse
import sp.it.pl.gui.objects.popover.ScreenUse.APP_WINDOW
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.NEW
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.service.ServiceBase
import sp.it.pl.util.access.VarAction
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.action.IsActionable
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfig.EditMode
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.attach
import kotlin.streams.asSequence

/** Provides notification functionality. */
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
            { when (it) {
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
                    else -> APP.widgetManager.find(it, NEW, true).ifPresentOrElse(
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
    )
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
        if (showSongNotification && !m.isEmpty()) {
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