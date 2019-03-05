package playerControls

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.media.MediaPlayer.Status
import sp.it.pl.audio.Player
import sp.it.pl.audio.Player.Seek
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playback.VolumeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.BITRATE
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.seeker.ChapterDisplayActivation.HOVER
import sp.it.pl.gui.objects.seeker.ChapterDisplayMode.POPUP_SHARED
import sp.it.pl.gui.objects.seeker.Seeker
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.scaleEM
import sp.it.pl.util.access.toggle
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.EM
import sp.it.pl.util.graphics.anchorPane
import sp.it.pl.util.graphics.drag.DragUtil.getAudioItems
import sp.it.pl.util.graphics.drag.DragUtil.hasAudio
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.prefSize
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.graphics.x
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncBi
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.units.toHMSMs
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

    val volume = Slider()
    val currTime = Label("00:00")
    val totalTime = Label("00:00")
    val realTime = Label("00:00")
    val bitrateL = Label()
    val sampleRateL = Label()
    val channelsL = Label()
    val titleL = Label()
    val artistL = Label()
    val seeker = Seeker()
    val f1 = IconFA.ANGLE_DOUBLE_LEFT.icon(24.0) { Player.seekBackward(seekType.value) }
    val f2 = IconFA.FAST_BACKWARD.icon(24.0) { PlaylistManager.playPreviousItem() }
    val f3 = IconFA.PLAY.icon(48.0, { gap(36.0) }) { Player.pause_resume() }
    val f4 = IconFA.FAST_FORWARD.icon(24.0) { PlaylistManager.playNextItem() }
    val f5 = IconFA.ANGLE_DOUBLE_RIGHT.icon(24.0) { Player.seekForward(seekType.value) }
    val muteB = IconFA.VOLUME_UP.icon(12.0) { Player.toggleMute() }
    val loopB = IconFA.RANDOM.icon(24.0) { Player.toggleLoopMode(it) }
    val playbackButtons = listOf(f1, f2, f3, f4, f5, seeker)
    private var lastCurrentTimeS: Double? = null
    private var lastRemainingTimeS: Double? = null
    private var lastRealTimeS: Double? = null

    @IsConfig(name = "Seek type", info = "Forward/backward buttons seek by time (absolute) or fraction of total duration (relative).")
    val seekType by cv(Seek.RELATIVE)
    @IsConfig(name = "Chapters show", info = "Display chapter marks on seeker.")
    val showChapters by cv(POPUP_SHARED) { dv -> seeker.chapterDisplayMode.apply { value = dv } }
    @IsConfig(name = "Chapter open on", info = "Opens chapter also when mouse hovers over them.")
    val showChapOnHover by cv(HOVER) { dv -> seeker.chapterDisplayActivation.apply { value = dv } }
    @IsConfig(name = "Snap seeker to chapters", info = "Enable snapping to chapters during dragging.")
    val snapToChap by cv(false) { dv -> seeker.chapterSnap.apply { value = dv } }
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    val elapsedTime by cv(true)

    init {
        root.prefSize = 850.scaleEM() x 200.scaleEM()

        val ps = Player.state.playback

        seeker.bindTime(ps.duration, ps.currentTime) on onClose
        seeker.chapterSnapDistance syncFrom APP.ui.snapDistance on onClose
        seeker.prefHeight = 30.0

        volume.styleClass += "volume"
        volume.prefWidth = 100.0
        volume.min = ps.volume.min
        volume.max = ps.volume.max
        volume.blockIncrement = VolumeProperty.STEP
        volume.value = ps.volume.get()
        volume.valueProperty() syncBi ps.volume on onClose

        root.lay += anchorPane {
            lay(0.0, 0.0, 60.0, 0.0) += hBox(40.0) {
                lay += hBox(5.0, CENTER) {
                    padding = Insets(0.0, 0.0, 0.0, 20.0)

                    lay += listOf(f1, f2, f3, f4, f5, loopB)
                }
                lay += vBox(0.0, CENTER) {
                    lay += titleL
                    lay += artistL
                }
            }
            lay(null, 0.0, 30.0, 0.0) += hBox(20.0, CENTER_RIGHT) {
                prefHeight = 30.0

                lay += hBox(5, CENTER) {
                    lay += bitrateL
                    lay += sampleRateL
                    lay += channelsL
                }
                lay += hBox(5, CENTER) {
                    lay += currTime
                    lay += Label("/")
                    lay += totalTime
                    lay += Label("/")
                    lay += realTime
                }
                lay += hBox(5, CENTER) {
                    lay += muteB
                    lay += volume
                }
            }
            lay(null, 0.0, 0.0, 0.0) += seeker
        }

        ps.duration sync { totalTime.text = it.toHMSMs() } on onClose
        ps.currentTime sync { timeChanged(ps) } on onClose
        ps.status sync { statusChanged(it) } on onClose
        ps.loopMode sync { loopModeChanged(it) } on onClose
        ps.mute sync { muteChanged(ps) } on onClose
        ps.volume sync { muteChanged(ps) } on onClose
        Player.playingSong.onUpdateAndNow { playingItemChanged(it) } on onClose
        elapsedTime sync { timeChanged(ps, true) } on onClose

        currTime.onEventDown(MOUSE_CLICKED) { elapsedTime.toggle() }
        installDrag(
                root,
                IconMD.PLAYLIST_PLUS,
                "Add to active playlist",
                { e -> hasAudio(e) },
                { e ->
                    val items = getAudioItems(e)
                    PlaylistManager.use { it.addItems(items) }
                }
        )
    }

    private fun playFile(file: File) {
        PlaylistManager.use {
            it.addUri(file.toURI())
            it.playLastItem()
        }
    }

    private fun playingItemChanged(song: Metadata) {
        titleL.text = song.getTitle()
        artistL.text = song.getArtist()
        bitrateL.text = song.getFieldS(BITRATE, "")
        sampleRateL.text = song.getSampleRate()
        channelsL.text = song.getChannels()
        seeker.reloadChapters(song)
    }

    private fun statusChanged(newStatus: Status?) {
        when (newStatus) {
            null, Status.UNKNOWN -> {
                playbackButtons.forEach { it.isDisable = true }
                f3.icon(IconFA.PLAY)
            }
            else -> {
                playbackButtons.forEach { it.isDisable = false }
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

    private fun muteChanged(playback: PlaybackState) {
        muteB.icon(when {
            playback.mute.value -> IconFA.VOLUME_OFF
            playback.volume.value>0.5 -> IconFA.VOLUME_UP
            else -> IconFA.VOLUME_DOWN
        })
    }

    private fun timeChanged(playback: PlaybackState, forceUpdate: Boolean = false) {
        if (forceUpdate) {
            lastCurrentTimeS = null
            lastRemainingTimeS = null
        }

        if (elapsedTime.value) {
            val currentTimeS = playback.currentTime.value.toSeconds()
            if (currentTimeS!=lastCurrentTimeS)
                currTime.text = playback.currentTime.value.toHMSMs()
            lastCurrentTimeS = currentTimeS
        } else {
            val remainingTimeS = playback.remainingTime.toSeconds()
            if (remainingTimeS!=lastRemainingTimeS)
                currTime.text =  "- "+playback.remainingTime.toHMSMs()
            lastRemainingTimeS = remainingTimeS
        }

        val realTimeS = Player.player.realTime.get().toSeconds()
        if (realTimeS!=lastRealTimeS)
            realTime.text = Player.player.realTime.get().toHMSMs()
        lastRealTimeS = realTimeS
    }

    companion object {
        fun GlyphIcons.icon(size: Double, init: Icon.() -> Unit = {}, block: (MouseEvent) -> Unit) = Icon(this, size).onClickDo(block).apply(init)!!
    }

}