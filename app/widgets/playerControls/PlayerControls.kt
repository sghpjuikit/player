package playerControls

import de.jensd.fx.glyphs.GlyphIcons
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
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
import sp.it.pl.gui.objects.balancer.Balancer
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.seeker.Seeker
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.util.Util.formatDuration
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.EM
import sp.it.pl.util.graphics.anchorPane
import sp.it.pl.util.graphics.drag.DragUtil.getAudioItems
import sp.it.pl.util.graphics.drag.DragUtil.hasAudio
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncBi
import sp.it.pl.util.reactive.syncFrom
import java.io.File

@Widget.Info(
        name = PLAYBACK,
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
class PlayerControls(widget: Widget): SimpleController(widget), PlaybackFeature {

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

    var balance: Balancer
    val seeker = Seeker()
    val f1 = IconFA.ANGLE_DOUBLE_LEFT.icon(24.0) { Player.seekBackward(seekType.get()) }
    val f2 = IconFA.FAST_BACKWARD.icon(24.0) { PlaylistManager.playPreviousItem() }
    val f3 = IconFA.PLAY.icon(48.0, { gap(36.0) }) { Player.pause_resume() }
    val f4 = IconFA.FAST_FORWARD.icon(24.0) { PlaylistManager.playNextItem() }
    val f5 = IconFA.ANGLE_DOUBLE_RIGHT.icon(24.0) { Player.seekForward(seekType.get()) }
    val muteB = IconFA.VOLUME_UP.icon(12.0) { Player.toggleMute() }
    val loopB = IconFA.RANDOM.icon(24.0){ Player.toggleLoopMode(it) }
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

    init {
        ConventionFxmlLoader(root, this).loadNoEx<Any>()

        val ps = Player.state.playback

        seeker.bindTime(ps.duration, ps.currentTime) on onClose
        seeker.chapterSnapDistance syncFrom APP.ui.snapDistance on onClose
        seeker.prefHeight = 30.0
        root.lay += anchorPane {
            lay(null, 0.0, 0.0, 0.0) += seeker
        }

        balance = Balancer(ps.balance)
        (soundPane.parent as Pane).children.add(0, balance)
        balance.step.set(BalanceProperty.STEP)
        balance.prefHeight = 100.0
        balance.balance syncBi ps.balance on onClose

        volume.min = ps.volume.min
        volume.max = ps.volume.max
        volume.blockIncrement = VolumeProperty.STEP
        volume.value = ps.volume.get()
        volume.valueProperty() syncBi ps.volume on onClose

        playButtons.children setTo listOf(f1, f2, f3, f4, f5, loopB)
        soundPane.children.add(0, muteB)

        ps.duration sync { totTime.text = it.print() } on onClose
        ps.currentTime sync { timeChanged(ps) } on onClose
        ps.status sync { statusChanged(it) } on onClose
        ps.loopMode sync { loopModeChanged(it) } on onClose
        ps.mute sync { muteChanged(ps) } on onClose
        ps.volume sync { muteChanged(ps) } on onClose
        Player.onSeekDone.addS { lastUpdateTime = Double.MIN_VALUE } on onClose
        Player.playingItem.onUpdateAndNow { playingItemChanged(it) } on onClose

        currTime.onEventDown(MOUSE_CLICKED) { cycleElapsed() }
        installDrag(
                root,
                IconMD.PLAYLIST_PLUS,
                "Add to active playlist",
                { e -> hasAudio(e) },
                { e ->
                    val items = getAudioItems(e)
                    PlaylistManager.use { if (playDropped) it.setNplay(items) else it.addItems(items) }
                }
        )
    }

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
            f3.icon(IconFA.PLAY)
        } else {
            controlPanel.isDisable = false
            seeker.isDisable = false
            f3.icon(when (newStatus) {
                Status.PLAYING -> IconFA.PAUSE
                else -> IconFA.PLAY
            })
            f3.glyphOffsetX.value = when (newStatus) {
                Status.PLAYING -> -APP.ui.font.value.size.EM*0.2
                else -> APP.ui.font.value.size.EM*3.0
            }
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
            LoopMode.OFF -> IconMD.REPEAT_OFF
            LoopMode.PLAYLIST -> IconMD.REPEAT
            LoopMode.SONG -> IconMD.REPEAT_ONCE
            LoopMode.RANDOM -> IconFA.RANDOM
        })
    }

    private fun muteChanged(pb: PlaybackState) {
        muteB.icon(when {
            pb.mute.value -> IconFA.VOLUME_OFF
            pb.volume.value>0.5 -> IconFA.VOLUME_UP
            else -> IconFA.VOLUME_DOWN
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

    companion object {

        fun Duration.print() = formatDuration(this)!!

        fun GlyphIcons.icon(size: Double, init: Icon.() -> Unit = {}, block: (MouseEvent) -> Unit) = Icon(this, size).onClickDo(block).apply(init)!!

    }
}