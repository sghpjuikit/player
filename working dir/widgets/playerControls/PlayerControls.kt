package playerControls

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.media.MediaPlayer.Status
import javafx.util.Duration
import sp.it.pl.audio.Player
import sp.it.pl.audio.Player.Seek
import sp.it.pl.audio.playback.BalanceProperty
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playback.VolumeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.BITRATE
import sp.it.pl.gui.Gui
import sp.it.pl.gui.objects.balancer.Balancer
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.seeker.Seeker
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.initClose
import sp.it.pl.util.Util.formatDuration
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.drag.DragUtil.getAudioItems
import sp.it.pl.util.graphics.drag.DragUtil.hasAudio
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.setAnchors
import sp.it.pl.util.reactive.maintain
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncBi
import sp.it.pl.util.reactive.syncTo
import java.io.File

@Widget.Info(
        name = "Playback",
        author = "Martin Polakovic",
        howto = "Playback actions:\n"
                +"    Control Playback\n"
                +"    Drop audio files : Adds or plays the files\n"
                +"    Left click : Seek - move playback to seeked position\n"
                +"    Mouse drag : Seek (on release)\n"
                +"    Right click : Cancel seek\n"
                +"    Add button left click : Opens file chooser and plays files\n"
                +"    Add button right click: Opens directory chooser and plays files\n"
                +"    Drop audio files : Adds or plays the files\n"
                +"\nChapter actions:\n"
                +"    Right click : Create chapter\n"
                +"    Right click chapter : Open chapter\n"
                +"    Mouse hover chapter (optional) : Open chapter\n",
        description = "Playback control widget.",
        notes = "",
        version = "0.8",
        year = "2014",
        group = Widget.Group.PLAYBACK
)
class PlayerControls: FXMLController(), PlaybackFeature {

    @FXML lateinit var entireArea: AnchorPane
    @FXML lateinit var controlPanel: BorderPane
    @FXML lateinit var soundPane: HBox
    @FXML lateinit var volume: Slider
    @FXML lateinit var currTime: Label
    @FXML lateinit var totTime: Label
    @FXML lateinit var realTime: Label
    @FXML lateinit var titleL: Label
    @FXML lateinit var artistL: Label
    @FXML lateinit var bitrateL: Label
    @FXML lateinit var sampleRateL: Label
    @FXML lateinit var channelsL: Label
    @FXML lateinit var playButtons: HBox

    lateinit var balance: Balancer
    val seeker = Seeker()
    val f1 = Icon(FontAwesomeIcon.ANGLE_DOUBLE_LEFT, 15.0).onClickDo { Player.seekBackward(seekType.get()) }
    val f2 = Icon(FontAwesomeIcon.FAST_BACKWARD, 15.0).onClickDo { PlaylistManager.playPreviousItem() }
    val f3 = Icon(FontAwesomeIcon.PLAY, 30.0).onClickDo { Player.pause_resume() }
    val f4 = Icon(FontAwesomeIcon.FAST_FORWARD, 15.0).onClickDo { PlaylistManager.playNextItem() }
    val f5 = Icon(FontAwesomeIcon.ANGLE_DOUBLE_RIGHT, 15.0).onClickDo { Player.seekForward(seekType.get()) }
    val muteB = Icon(FontAwesomeIcon.VOLUME_UP, 15.0).onClickDo { Player.toggleMute() }
    val loopB = Icon(FontAwesomeIcon.RANDOM, 15.0).onClickDo { Player.toggleLoopMode(it) }
    var lastUpdateTime = Double.MIN_VALUE // reduces time update events

    @IsConfig(name = "Seek type", info = "Seek by time (absolute) or fraction of total duration (relative).")
    val seekType = v(Seek.RELATIVE)
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

        balance = Balancer(ps.balance)
        (soundPane.parent as Pane).children.add(0, balance)
        balance.step.set(BalanceProperty.STEP)
        balance.prefHeight = 100.0
        initClose { balance.balance syncBi ps.balance }

        volume.min = ps.volume.min
        volume.max = ps.volume.max
        volume.blockIncrement = VolumeProperty.STEP
        volume.value = ps.volume.get()
        initClose { volume.valueProperty() syncBi ps.volume }

        initClose { seeker.bindTime(ps.duration, ps.currentTime) }
        entireArea.children += seeker
        seeker.prefHeight = 30.0
        seeker.setAnchors(null, 0.0, 0.0, 0.0)
        initClose { Gui.snapDistance syncTo seeker.chapterSnapDistance }

        playButtons.children.setAll(f1, f2, f3, f4, f5, loopB)
        soundPane.children.add(0, muteB)

        initClose { ps.duration sync { totTime.text = it.print() } }
        initClose { ps.currentTime sync { timeChanged(ps) } }
        initClose { ps.status sync { statusChanged(it) } }
        initClose { ps.loopMode sync { loopModeChanged(it) } }
        initClose { ps.mute sync { muteChanged(ps) } }
        initClose { ps.volume sync { muteChanged(ps) } }
        initClose { Player.onSeekDone.addS { lastUpdateTime = Double.MIN_VALUE } }
        initClose { Player.playingItem.onUpdateAndNow { playingItemChanged(it) } }

        currTime.setOnMouseClicked { cycleElapsed() }
        installDrag(
                entireArea,
                MaterialDesignIcon.PLAYLIST_PLUS,
                "Add to active playlist",
                { e -> hasAudio(e) },
                { e ->
                    val items = getAudioItems(e)
                    PlaylistManager.use { if (playDropped) it.setNplay(items) else it.addItems(items) }
                }
        )
    }

    override fun refresh() {}

    private fun playFile(file: File) {
        PlaylistManager.use {
            it.addUri(file.toURI())
            it.playLastItem()
        }
    }

    private fun cycleElapsed() {
        elapsedTime = !elapsedTime
        timeChanged(Player.state.playback)
    }

    private fun playingItemChanged(nv: Metadata) {
        lastUpdateTime = Double.MIN_VALUE

        titleL.text = nv.getTitle()
        artistL.text = nv.getArtist()
        bitrateL.text = nv.getFieldS(BITRATE, "")
        sampleRateL.text = nv.getSampleRate()
        channelsL.text = nv.getChannels()
        seeker.reloadChapters(nv)
    }

    private fun statusChanged(newStatus: Status?) {
        lastUpdateTime = Double.MIN_VALUE
        if (newStatus==null || newStatus==Status.UNKNOWN) {
            controlPanel.isDisable = true
            seeker.isDisable = true
            f3.icon(FontAwesomeIcon.PLAY)
        } else {
            controlPanel.isDisable = false
            seeker.isDisable = false
            f3.icon(when (newStatus) {
                Status.PLAYING -> FontAwesomeIcon.PAUSE
                else -> FontAwesomeIcon.PLAY
            })
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
            realTime.text = Player.player.realTime.get().print()
            currTime.text = if (elapsedTime) pb.currentTime.value.print() else "- "+pb.remainingTime.print()
        }
    }

    private fun Duration.print() = formatDuration(this)

}