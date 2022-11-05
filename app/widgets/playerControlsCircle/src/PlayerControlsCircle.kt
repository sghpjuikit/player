package playerControlsCircle

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control.Label
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.UNKNOWN
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.text.TextBoundsType.VISUAL
import mu.KLogging
import sp.it.pl.audio.PlayerManager.Seek
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playback.VolumeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.OFF
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.PLAYLIST
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.RANDOM
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.SONG
import sp.it.pl.audio.tagging.Chapter
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.HorizontalDock
import sp.it.pl.layout.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.SliderCircular
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.boundsType
import sp.it.pl.ui.objects.icon.onClickDelegateKeyTo
import sp.it.pl.ui.objects.icon.onClickDelegateMouseTo
import sp.it.pl.ui.objects.seeker.SongChapterEdit
import sp.it.pl.ui.objects.seeker.bindTimeToSmooth
import sp.it.pl.ui.objects.window.NodeShow.DOWN_RIGHT
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggle
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.file.div
import sp.it.util.functional.traverse
import sp.it.util.math.dist
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.ui.borderPane
import sp.it.util.ui.center
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.lookupChildAt
import sp.it.util.ui.prefSize
import sp.it.util.ui.radius
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.divMillis
import sp.it.util.units.millis
import sp.it.util.units.minus
import sp.it.util.units.times
import sp.it.util.units.toHMSMs
import sp.it.util.units.version
import sp.it.util.units.year

class PlayerControlsCircle(widget: Widget): SimpleController(widget), PlaybackFeature, HorizontalDock {
   val currTime = Label("00:00")
   val f2 = IconUN(0x2aa1).icon(72.0) { if (it) PlaylistManager.playPreviousItem() else APP.audio.seekBackward(seekType.value) }
   val f3 = IconUN(0x25c6).icon(128.0) { APP.audio.pauseResume() }
   val f4 = IconUN(0x2aa2).icon(72.0) { if (it) PlaylistManager.playNextItem() else APP.audio.seekForward(seekType.value) }
   val muteB = IconFA.VOLUME_UP.icon(24.0) { APP.audio.toggleMute() }
   val loopB = IconFA.RANDOM.icon(24.0) { APP.audio.state.playback.loopMode.toggle(it) }
   val playbackButtons = listOf(f2, f3, f4)
   val seeker = SliderCircular(333.0.emScaled)
   val seekerChapters = observableList<SeekerChapter>()
   val ps = APP.audio.state.playback
   val seekType by cv(Seek.RELATIVE).def(name = "Seek type", info = "Forward/backward buttons seek by time (absolute) or fraction of total duration (relative).")
   val elapsedTime by cv(true).def(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")

   init {
      root.prefSize = 850.emScaled x 200.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      ps.status sync { statusChanged(it) } on onClose
      ps.loopMode sync { loopModeChanged(it) } on onClose
      ps.mute sync { muteChanged(ps) } on onClose
      ps.volume sync { muteChanged(ps) } on onClose
      APP.audio.playingSong.updated sync { seekerChapters setTo it.getChapters().chapters.mapIndexed { i, c -> SeekerChapter(it, c, i) } } on onClose

      root.onEventDown(MOUSE_CLICKED, BACK) { PlaylistManager.playPreviousItem() }
      root.onEventDown(MOUSE_CLICKED, FORWARD) { PlaylistManager.playNextItem() }
      root.installDrag(
         IconMD.PLAYLIST_PLUS,
         "Add to active playlist",
         { e -> e.dragboard.hasAudio() },
         { e -> PlaylistManager.use { it.addItems(e.dragboard.getAudio()) } }
      )

      root.lay += hBox {
         lay += stackPane {
            padding = Insets(5.0)
            lay += stackPane {

               seekerChapters.onChangeAndNow {
                  val cSpan01 = 0.2
                  val cSpanDeg = 0.8*cSpan01*180.0
                  val csByLvl = HashMap<Int, ArrayList<Double>>()

                  seeker.lookupChildAt<Group>(0).children.removeIf { "slider-circular-mark" in it.styleClass }
                  seekerChapters.forEach { c ->
                     val cLvl = 1.traverse { it + 1 }.first { csByLvl[it].orEmpty().none { it dist c.position01 <= cSpan01 } }
                     csByLvl.getOrPut(cLvl, ::ArrayList) += c.position01

                     seeker.lookupChildAt<Group>(0).children += Arc().apply {
                        center = (seeker.W/2.0).x2
                        radius = (seeker.W/4.0 - cLvl*8.emScaled).x2
                        type = ArcType.OPEN
                        styleClass += "slider-circular-mark"
                        length = cSpanDeg
                        startAngle = 360-c.position01*180 -cSpanDeg/2.0

                        onEventDown(MOUSE_CLICKED, PRIMARY) {
                           if (!c.isNew)
                              SongChapterEdit(c.song, c.i).showPopup(DOWN_RIGHT(seeker))
                        }
                     }
                  }
               }
            }
            lay += seeker.apply {
               valueSymmetrical.value = true
               seekerChapters.onChange { snaps setTo seekerChapters.map { it.position01 } }
               blockIncrement syncFrom seekType.flatMap {
                  when (it) {
                     Seek.RELATIVE -> APP.audio.seekUnitP
                     Seek.ABSOLUTE -> ps.duration zip APP.audio.seekUnitT map { (td, sd) -> sd.divMillis(td) }
                  }
               } on onClose

               val valueSuppressor = Suppressor()
               bindTimeToSmooth(ps) { valueSuppressor.suppressing { value.value = it } } on onClose
               valueSoft attach { valueSuppressor.suppressed { if (!isValueChanging.value) APP.audio.seek(it) } } on onClose

               onEventDown(MOUSE_CLICKED, SECONDARY) {
                  val time01 = seeker.computeValueNull(it)
                  if (time01!=null) {
                     val timeMs = time01*APP.audio.playingSong.value.getLengthInMs()
                     val ch = Chapter(timeMs.millis, "")
                     val sc = SeekerChapter(APP.audio.playingSong.value, ch, -1)
                     seekerChapters += sc
                     SongChapterEdit(APP.audio.playingSong.value, timeMs.millis, time01).apply {
                        onHidden += { seekerChapters -= sc }
                        showPopup(DOWN_RIGHT(seeker))
                     }
                  }
               }

               lay += vBox {
                  alignment = Pos.CENTER
                  isPickOnBounds = false
                  isFillWidth = false

                  lay += label {
                     isMouseTransparent = true
                     styleClass += "seeker-label"
                  }
                  lay += hBox {
                     alignment = Pos.CENTER
                     isPickOnBounds = false
                     isFillHeight = false
                     lay += f2.size(36).scale(2.0)
                     lay += f3.size(72).scale(2.0).apply {
                        isFocusTraversable = false
                        focusOwner.value = seeker
                        onClickDelegateKeyTo(seeker)
                     }
                     lay += f4.size(36).scale(2.0)
                  }
                  lay += currTime.apply {
                     isPickOnBounds = false
                     styleClass += "seeker-label"
                     onEventDown(MOUSE_CLICKED, PRIMARY) { elapsedTime.toggle() }

                     seeker.isValueChanging flatMap {
                        if (it) seeker.valueShown zip ps.duration map { (at, total) -> total*at }
                        else ps.currentTime map { it.toSeconds().toLong() } map { ps.currentTime.value }
                     } zip elapsedTime sync { (current, e) ->
                        text = if (e) current.toHMSMs() else "- " + (ps.duration.value - current).toHMSMs()
                     } on onClose
                  }
               }
            }
         }
         lay += borderPane {
            top = SliderCircular(100.emScaled).apply {
               val valueCount = LoopMode.values().size.toDouble() - 1.0
               val mapping = LoopMode.values().associateWith {
                  when (it) {
                     OFF -> 0.0
                     SONG -> 1/valueCount
                     RANDOM -> 2/valueCount
                     PLAYLIST -> 1.0
                  }
               }
               val mappingInv = { it: Double -> mapping.entries.minByOrNull { (_, v) -> it dist v }!!.key }

               snaps setTo mapping.values
               blockIncrement.value = 1.0/valueCount
               value attach { ps.loopMode.value = mappingInv(it) }
               ps.loopMode sync { value.value = mapping[it]!! }

               lay += loopB

               // knob delegates focus & some events to icon
               loopB.isFocusTraversable = false
               loopB.isMouseTransparent = true
               loopB.focusOwner.value = this
               onEventUp(MouseEvent.ANY) { if (it.eventType in setOf(MOUSE_PRESSED, MOUSE_RELEASED, MOUSE_DRAGGED, MOUSE_CLICKED)) it.consume() }
               loopB.onClickDelegateKeyTo(this)
               loopB.onClickDelegateMouseTo(this)
            }
            bottom =  SliderCircular(100.emScaled).apply {
               blockIncrement.value = VolumeProperty.STEP
               value attachTo ps.volume
               ps.volume sync { value.value = it.toDouble() }

               // knob delegates focus & some events to icon
               muteB.isFocusTraversable = false
               muteB.isMouseTransparent = true
               muteB.focusOwner.value = this
               muteB.onClickDelegateKeyTo(this)
               onEventUp(KEY_RELEASED, muteB.onKeyReleased!!::handle)

               lay += muteB
               lay += Circle(20.0, TRANSPARENT).apply {
                  muteB.onClickDelegateMouseTo(this)
               }
            }
         }
      }
   }

   private fun playFile(file: File) {
      PlaylistManager.use {
         it.addUri(file.toURI())
         it.playTransformedLast()
      }
   }

   private fun statusChanged(newStatus: Status?) {
      when (newStatus) {
         null, UNKNOWN -> {
            seeker.editable.value = false
            playbackButtons.forEach { it.isDisable = true }
            f3.icon(IconUN(0x25c6))
         }
         else -> {
            seeker.editable.value = true
            playbackButtons.forEach { it.isDisable = false }
            f3.icon(
               when (newStatus) {
                  PLAYING -> IconUN(0x25ca)
                  else -> IconUN(0x27e0)
               }
            )
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
      loopB.icon(
         when (looping) {
            OFF -> IconMD.REPEAT_OFF
            PLAYLIST -> IconMD.REPEAT
            SONG -> IconMD.REPEAT_ONCE
            RANDOM -> IconFA.RANDOM
         }
      )
   }

   private fun muteChanged(playback: PlaybackState) {
      muteB.icon(
         when {
            playback.mute.value -> IconUN(0x1f507)
            playback.volume.value==0.0 -> IconUN(0x1f508)
            playback.volume.value>0.5 -> IconUN(0x1f50a)
            else -> IconUN(0x1f509)
         }
      )
   }

   data class SeekerChapter(val song: Metadata, val chapter: Chapter, val i: Int) {
      val position01 = chapter.time divMillis song.getLength()
      val isNew: Boolean get() = i==SongChapterEdit.INDEX_NEW
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Playback knobs"
      override val description = "Audio playback knob controls"
      override val descriptionLong = "$description."
      override val icon = IconMD.MUSIC_CIRCLE
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO)
      override val summaryActions = listOf(
         Entry("Controls", "Play previous song", "BMB"),
         Entry("Controls", "Play previous song", "back icon LMB"),
         Entry("Controls", "Seek backward", "back icon RMB"),
         Entry("Controls", "Play/pause", "play/pause icon LMB or RMB"),
         Entry("Controls", "Seek forward", "forward icon RMB"),
         Entry("Controls", "Play next song", "forward icon LMB"),
         Entry("Controls", "Play next song", "FMB"),
         Entry("Controls", "Toggle next song mode", "loop icon LMB or RMB"),
         Entry("Controls", "Change volume", "Scroll"),
         Entry("Controls", "Mute", "Mute icon LMB or RMB"),
         Entry("View", "Toggle elapsed/remaining time", "current time label LBM"),
         Entry("Playlist", "Add songs to active playlist", "Drag & drop songs"),
         Entry("Seeker", "Seek playback", "LMB"),
         Entry("Seeker", "Seek playback", "drag & release LMB"),
         Entry("Seeker", "Cancel seeking playback", "drag LMB + RMB"),
         Entry("Seeker > Chapter", "Add chapter", "seeker RMB"),
         Entry("Seeker > Chapter mark", "Open chapter", "LMB"),
         Entry("Seeker > Chapter popup", "Hide chapter", "LMB"),
         Entry("Seeker > Chapter popup", "Hide chapter", "Escape"),
         Entry("Seeker > Chapter popup", "Play from chapter", "2xLMB"),
         Entry("Seeker > Chapter popup > Edit", "Start edit", "2xRMB"),
         Entry("Seeker > Chapter popup > Edit", "Apply edit", "Enter"),
         Entry("Seeker > Chapter popup > Edit", "Append new line", "Shift + Enter"),
         Entry("Seeker > Chapter popup > Edit", "Cancel edit", "Escape"),
      )

      fun GlyphIcons.icon(size: Double, block: (Boolean) -> Unit) = Icon(this, size).apply {
         boundsType = VISUAL
         onClickDo(null, null) { _, e ->
            when (e?.button) {
               null -> block(true)
               PRIMARY -> block(true)
               SECONDARY -> block(false)
               else -> Unit
            }
         }
      }
   }

}