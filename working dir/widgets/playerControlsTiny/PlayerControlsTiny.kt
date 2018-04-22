package playerControlsTiny

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import javafx.animation.Animation.INDEFINITE
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.UNKNOWN
import javafx.util.Duration
import javafx.util.Duration.seconds
import sp.it.pl.audio.Player
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.seeker.Seeker
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.initClose
import sp.it.pl.main.onDispose
import sp.it.pl.util.Util.clip
import sp.it.pl.util.Util.formatDuration
import sp.it.pl.util.animation.Anim
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.Util.layStack
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.getAudioItems
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo
import java.lang.Double.max

@Widget.Info(
        name = "Playback Mini",
        author = "Martin Polakovic",
        howto = "Playback actions:\n"
                +"    Control Playback\n"
                +"    Drop audio files : Adds or plays the files\n"
                +"    Left click : Seek - move playback to seeked position\n"
                +"    Mouse drag : Seek (on release)\n"
                +"    Right click : Cancel seek\n"
                +"    Drop audio files : Adds or plays the files\n"
                +"\nChapter actions:\n"
                +"    Right click : Create chapter\n"
                +"    Right click chapter : Open chapter\n"
                +"    Mouse hover chapter (optional) : Open chapter\n",
        description = "Minimalistic playback control widget.",
        notes = "", version = "1",
        year = "2015",
        group = Widget.Group.PLAYBACK
)
class PlayerControlsTiny: FXMLController(), PlaybackFeature, HorizontalDock {

    @FXML lateinit var root: AnchorPane
    @FXML lateinit var layout: HBox
    @FXML lateinit var controlBox: HBox
    @FXML lateinit var currTime: Label
    var scrollLabel = Label("")
    var seeker = Seeker()
    var prevB = Icon(FontAwesomeIcon.FAST_BACKWARD, 14.0).onClickDo { PlaylistManager.playPreviousItem() }
    var playB = Icon(null, 14.0+3).onClickDo { Player.pause_resume() }
    var nextB = Icon(FontAwesomeIcon.FAST_FORWARD, 14.0).onClickDo { PlaylistManager.playNextItem() }
    var loopB = Icon(null, 14.0).onClickDo { Player.toggleLoopMode(it) }
    var muteB = Icon(null, 14.0).onClickDo { Player.toggleMute() }
    var lastUpdateTime = Double.MIN_VALUE // reduces time update events
    lateinit var scroller: Anim

    @IsConfig(name = "Chapters show", info = "Display chapter marks on seeker.")
    val showChapters = seeker.chapterDisplayMode
    @IsConfig(name = "Chapter open on", info = "Opens chapter also when mouse hovers over them.")
    val showChapOnHover = seeker.chapterDisplayActivation
    @IsConfig(name = "Snap seeker to chapters", info = "Enable snapping to chapters during dragging.")
    val snapToChap = seeker.chapterSnap
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    var elapsedTime = true
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    var playDropped = false

    override fun init() {
        val ps = Player.state.playback

        initClose { seeker.bindTime(ps.duration, ps.currentTime) }
        initClose { APP.ui.snapDistance syncTo seeker.chapterSnapDistance }
        layout.children.add(2, seeker)
        HBox.setHgrow(seeker, ALWAYS)

        controlBox.children += listOf(prevB, playB, nextB, Label(), loopB)
        layout.children += muteB

        val scrollWidth = 200.0
        val scrollerPane = ScrollPane(layStack(scrollLabel, Pos.CENTER)).apply {
            prefWidth = scrollWidth
            isPannable = false
            isFitToHeight = true
            vbarPolicy = NEVER
            hbarPolicy = NEVER
        }
        (currTime.parent as Pane).children.add((currTime.parent as Pane).children.indexOf(currTime)+1, scrollerPane)
        scroller = anim(seconds(5.0), { scrollerPane.hvalue = it }).apply {
            intpl { x -> clip(0.0, x*1.5-0.25, 1.0) } // linear, but waits a bit around 0 and 1
            isAutoReverse = true
            cycleCount = INDEFINITE
            play()
        }
        initClose { scrollLabel.widthProperty() sync { scroller.rate = 50/max(50.0, it.toDouble()-scrollWidth) } }  // maintain constant speed
        onDispose { scroller.stop() }

        initClose { ps.volume sync { muteChanged(ps) } }
        initClose { ps.mute sync { muteChanged(ps) } }
        initClose { ps.status sync { statusChanged(it) } }
        initClose { ps.currentTime sync { timeChanged(ps) } }
        initClose { ps.loopMode sync { loopModeChanged(it) } }
        initClose { Player.onSeekDone.addS { lastUpdateTime = Double.MIN_VALUE } }
        initClose { Player.playingItem.onUpdateAndNow { playingItemChanged(it) } }

        currTime.setOnMouseClicked { cycleElapsed() }
        installDrag(
                root,
                MaterialDesignIcon.PLAYLIST_PLUS,
                "Add to active playlist",
                { e -> DragUtil.hasAudio(e) },
                { e ->
                    val items = getAudioItems(e)
                    PlaylistManager.use { p -> if (playDropped) p.setNplay(items) else p.addItems(items) }
                }
        )
    }

    override fun refresh() {}

    private fun cycleElapsed() {
        elapsedTime = !elapsedTime
        timeChanged(Player.state.playback)
    }

    private fun playingItemChanged(m: Metadata) {
        lastUpdateTime = Double.MIN_VALUE
        seeker.reloadChapters(m)
        scrollLabel.text = "${m.getArtistOrEmpty()} - ${m.getTitleOrEmpty()}"
    }

    private fun statusChanged(status: Status?) {
        if (status==null || status==UNKNOWN) {
            seeker.isDisable = true
            playB.icon(FontAwesomeIcon.PLAY)
        } else if (status==PLAYING) {
            seeker.isDisable = false
            playB.icon(FontAwesomeIcon.PAUSE)
        } else {
            seeker.isDisable = false
            playB.icon(FontAwesomeIcon.PLAY)
        }
    }

    private fun loopModeChanged(looping: LoopMode) {
        if (loopB.tooltip==null) loopB.tooltip("ignoredText") // lazy init
        loopB.tooltip.text = when (looping) {
            LoopMode.OFF -> "Loop mode: off"
            LoopMode.PLAYLIST -> "Loop mode: playlist"
            LoopMode.SONG -> "Loop mode: song"
            LoopMode.RANDOM -> "Loop mode: random"
        }
        loopB.icon(when (looping) {
            LoopMode.OFF -> MaterialDesignIcon.REPEAT_OFF
            LoopMode.PLAYLIST -> MaterialDesignIcon.REPEAT
            LoopMode.SONG -> MaterialDesignIcon.REPEAT_ONCE
            LoopMode.RANDOM -> FontAwesomeIcon.RANDOM
        })
    }

    private fun muteChanged(pb: PlaybackState) {
        muteB.icon(when {
            pb.mute.value -> FontAwesomeIcon.VOLUME_OFF
            pb.volume.value>0.5 -> FontAwesomeIcon.VOLUME_UP
            else -> FontAwesomeIcon.VOLUME_DOWN
        })
    }

    private fun timeChanged(pb: PlaybackState) {
        val millis = pb.currentTime.get().toMillis()
        if (lastUpdateTime+1000<=millis) {
            lastUpdateTime = millis
            currTime.text = if (elapsedTime) pb.currentTime.value.print() else "- "+pb.remainingTime.print()
        }
    }

    private fun Duration.print() = formatDuration(this)
}