package playerControls

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.UNKNOWN
import javafx.scene.text.TextBoundsType.LOGICAL
import javafx.scene.text.TextBoundsType.VISUAL
import sp.it.pl.audio.PlayerManager.Seek
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playback.VolumeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.OFF
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.PLAYLIST
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.RANDOM
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.SONG
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.BITRATE
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.PLAYBACK_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.boundsType
import sp.it.pl.ui.objects.seeker.ChapterDisplayActivation.HOVER
import sp.it.pl.ui.objects.seeker.ChapterDisplayMode.POPUP_SHARED
import sp.it.pl.ui.objects.seeker.Seeker
import sp.it.util.access.toggle
import sp.it.util.collections.setToOne
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.functional.asIs
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.seconds
import sp.it.util.units.toHMSMs
import java.io.File

@Widget.Info(
   name = PLAYBACK_NAME,
   author = "Martin Polakovic",
   howto = "Playback actions:\n"
      + "    Control Playback\n"
      + "    Drop audio files : Adds or plays the files\n"
      + "    Left click : Seek - move playback to seek position\n"
      + "    Mouse drag : Seek (on release)\n"
      + "    Right click : Cancel seek\n"
      + "    Add button left click : Opens file chooser and plays files\n"
      + "    Add button right click: Opens directory chooser and plays files\n"
      + "    Drop audio files : Adds or plays the files\n"
      + "\nChapter actions:\n"
      + "    Right click : Create chapter\n"
      + "    Right click chapter : Open chapter\n"
      + "    Mouse hover chapter (optional) : Open chapter\n",
   description = "Playback control widget.",
   notes = "",
   version = "1.0.0",
   year = "2014",
   group = Widget.Group.PLAYBACK
)
class PlayerControls(widget: Widget): SimpleController(widget), PlaybackFeature, HorizontalDock {

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
   val f2 = IconUN(0x2aa1).icon(72.0) { PlaylistManager.playPreviousItem() /*APP.audio.seekBackwardForward(seekType.value)*/ }
   val f3 = IconUN(0x25c6).icon(128.0) { APP.audio.pauseResume() }
   val f4 = IconUN(0x2aa2).icon(72.0) { PlaylistManager.playNextItem() /*APP.audio.seekForward(seekType.value)*/ }
   val muteB = IconFA.VOLUME_UP.icon(24.0) { APP.audio.toggleMute() }
   val loopB = IconFA.RANDOM.icon(24.0) { APP.audio.toggleLoopMode() }
   val playbackButtons = listOf(f2, f3, f4, seeker)
   private val layoutSmall = LayoutSmall()
   private val layoutBig = LayoutBig()

   val seekType by cv(Seek.RELATIVE).def(name = "Seek type", info = "Forward/backward buttons seek by time (absolute) or fraction of total duration (relative).")
   val showChapters by cv(POPUP_SHARED) { dv -> seeker.chapterDisplayMode.apply { value = dv } }.def(name = "Chapters show", info = "Display chapter marks on seeker.")
   val showChapOnHover by cv(HOVER) { dv -> seeker.chapterDisplayActivation.apply { value = dv } }.def(name = "Chapter open on", info = "Opens chapter also when mouse hovers over them.")
   val snapToChap by cv(false) { dv -> seeker.chapterSnap.apply { value = dv } }.def(name = "Snap seeker to chapters", info = "Enable snapping to chapters during dragging.")
   val elapsedTime by cv(true).def(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")

   init {
      root.prefSize = 850.emScaled x 200.emScaled

      val ps = APP.audio.state.playback

      seeker.bindTime(ps.duration, ps.currentTime) on onClose
      seeker.chapterSnapDistance syncFrom APP.ui.snapDistance on onClose
      seeker.prefHeight = 30.0

      volume.styleClass += "volume"
      volume.prefWidth = 100.0
      volume.min = ps.volume.min
      volume.max = ps.volume.max
      volume.blockIncrement = VolumeProperty.STEP
      volume.value = ps.volume.get()
      volume.valueProperty() syncBiFrom ps.volume on onClose

      muteB.boundsType = LOGICAL

      ps.duration sync { totalTime.text = it.toHMSMs() } on onClose
      ps.currentTime.map(onClose) { it.toSeconds().toLong() } sync { timeChanged(ps) }
      ps.realTimeImpl.map(onClose) { it.toSeconds().toLong() } sync { realTime.text = it.seconds.toHMSMs() }
      ps.status sync { statusChanged(it) } on onClose
      ps.loopMode sync { loopModeChanged(it) } on onClose
      ps.mute sync { muteChanged(ps) } on onClose
      ps.volume sync { muteChanged(ps) } on onClose
      APP.audio.playingSong.onUpdateAndNow { playingItemChanged(it) } on onClose
      elapsedTime sync { timeChanged(ps) } on onClose

      currTime.onEventDown(MOUSE_CLICKED, PRIMARY) { elapsedTime.toggle() }

      installDrag(
         root,
         IconMD.PLAYLIST_PLUS,
         "Add to active playlist",
         { e -> e.dragboard.hasAudio() },
         { e -> PlaylistManager.use { it.addItems(e.dragboard.getAudio()) } }
      )

      root.heightProperty().map(onClose) { it.toDouble()<100.0.emScaled } sync {
         val layout: Layout = if (it) layoutSmall else layoutBig
         root.children setToOne layout.with(f2, f3, f4, muteB, seeker)
         f2.size(if (it) 24 else 72)
         f3.size(if (it) 40 else 144)
         f4.size(if (it) 24 else 72)
      }
   }

   private fun playFile(file: File) {
      PlaylistManager.use {
         it.addUri(file.toURI())
         it.playLastItem()
      }
   }

   private fun playingItemChanged(song: Metadata) {
      titleL.text = song.getTitle() ?: "<no title>"
      artistL.text = song.getArtist() ?: "<no artist>"
      bitrateL.text = song.getFieldS(BITRATE, "<no bitrate>")
      sampleRateL.text = song.getSampleRate() ?: "<no sample rate>"
      channelsL.text = song.getChannels() ?: "<no channels>"
      seeker.reloadChapters(song)
   }

   private fun statusChanged(newStatus: Status?) {
      when (newStatus) {
         null, UNKNOWN -> {
            playbackButtons.forEach { it.isDisable = true }
            f3.icon(IconUN(0x25c6))
         }
         else -> {
            playbackButtons.forEach { it.isDisable = false }
            f3.icon(when (newStatus) {
               PLAYING -> IconUN(0x25ca)
               else -> IconUN(0x27e0)
            })
         }
      }
   }

   private fun loopModeChanged(looping: LoopMode) {
      if (loopB.tooltip==null) loopB.tooltip("ignoredText")
      loopB.tooltip.text = when (looping) {
         OFF -> "Loop mode: off"
         PLAYLIST -> "Loop mode: playlist"
         SONG -> "Loop mode: song"
         RANDOM -> "Loop mode: random"
      }
      loopB.icon(when (looping) {
         OFF -> IconMD.REPEAT_OFF
         PLAYLIST -> IconMD.REPEAT
         SONG -> IconMD.REPEAT_ONCE
         RANDOM -> IconFA.RANDOM
      })
   }

   private fun muteChanged(playback: PlaybackState) {
      muteB.icon(when {
         playback.mute.value -> IconUN(0x1f507)
         playback.volume.value==0.0 -> IconUN(0x1f508)
         playback.volume.value>0.5 -> IconUN(0x1f50a)
         else -> IconUN(0x1f509)
      })
   }

   private fun timeChanged(playback: PlaybackState) {
      currTime.text = when {
         elapsedTime.value -> playback.currentTime.value.toHMSMs()
         else -> "- " + playback.remainingTime.toHMSMs()
      }
   }

   companion object {
      fun GlyphIcons.icon(size: Double, block: (Icon) -> Unit) = Icon(this, size).onClickDo(block).apply { boundsType = VISUAL }
   }

   interface Layout {
      fun add(_f2: Node, _f3: Node, _f4: Node, _muteB: Node, _seeker: Node)
      fun with(_f2: Node, _f3: Node, _f4: Node, _muteB: Node, _seeker: Node) = let {
         listOf(_f2, _f3, _f4, _muteB, _seeker).forEach { it.removeFromParent() }
         add(_f2, _f3, _f4, _muteB, _seeker)
         this as Node
      }
   }

   inner class LayoutBig: AnchorPane(), Layout {
      lateinit var iconParent: Pane

      init {
         lay(0.0, 0.0, 15.0, 0.0) += hBox(40.0) {
            lay += hBox(0, CENTER) {
               padding = Insets(0.0, 0.0, 0.0, 20.0)
               iconParent = this

               lay += listOf(loopB)
            }
            lay += vBox(0.0, CENTER) {
               lay += titleL
               lay += artistL
            }
         }
         lay(null, 0.0, 30.0, 0.0) += hBox(20.0, CENTER_RIGHT) {
            prefHeight = 30.0

            lay += hBox(5, CENTER) {
               lay += currTime
               lay += Label("/")
               lay += totalTime
               lay += Label("/")
               lay += realTime
            }
            lay += hBox(5, CENTER) {
               lay += bitrateL
               lay += sampleRateL
               lay += channelsL
            }
            lay += hBox(5, CENTER) {
               lay += volume
            }
         }
      }

      override fun add(_f2: Node, _f3: Node, _f4: Node, _muteB: Node, _seeker: Node) {
         iconParent.children.add(0, _f2)
         iconParent.children.add(1, _f3)
         iconParent.children.add(2, _f4)
         volume.parent.asIs<Pane>().children.add(0, muteB)
         lay(null, 0.0, 0.0, 0.0) += seeker
      }

   }

   inner class LayoutSmall: Pane(), Layout {

      override fun add(_f2: Node, _f3: Node, _f4: Node, _muteB: Node, _seeker: Node) {
         children.setAll(_f2, _f3, _f4, _muteB, _seeker)
      }

      override fun layoutChildren() {
         val gap = 5.0
         var left = snappedLeftInset()
         sequenceOf(f2, f3, f4).forEach {
            layoutInArea(it, left, 0.0, it.layoutBounds.width, height, 0.0, HPos.CENTER, VPos.CENTER)
            left += it.layoutBounds.width + gap
         }

         var right = snappedRightInset()
         layoutInArea(muteB, width - right - muteB.width, 0.0, muteB.layoutBounds.width, height, 0.0, HPos.CENTER, VPos.CENTER)
         right += muteB.layoutBounds.width + gap

         layoutInArea(seeker, left, 0.0, width - left - right, height, 0.0, HPos.CENTER, VPos.CENTER)
      }

   }
}