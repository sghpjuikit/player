package playerControlsCircle

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.CacheHint.ROTATE
import javafx.scene.Group
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode.DOWN
import javafx.scene.input.KeyCode.END
import javafx.scene.input.KeyCode.HOME
import javafx.scene.input.KeyCode.KP_DOWN
import javafx.scene.input.KeyCode.KP_LEFT
import javafx.scene.input.KeyCode.KP_RIGHT
import javafx.scene.input.KeyCode.KP_UP
import javafx.scene.input.KeyCode.LEFT
import javafx.scene.input.KeyCode.RIGHT
import javafx.scene.input.KeyCode.UP
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer.Status
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.scene.media.MediaPlayer.Status.UNKNOWN
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.text.TextBoundsType.VISUAL
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
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
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.PLAYBACK
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.layout.widget.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.boundsType
import sp.it.pl.ui.objects.seeker.bindTimeToSmooth
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.Values
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.file.div
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.math.clip
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.flatMap
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.ui.borderPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.xy
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
   val loopB = IconFA.RANDOM.icon(24.0) { APP.audio.setLoopMode(APP.audio.getLoopMode().net { v -> if (it) Values.next(v) else Values.previous(v) }) }
   val playbackButtons = listOf(f2, f3, f4)
   val seeker = SeekerCircle(333.0.emScaled)

   val ps = APP.audio.state.playback
   val seekType by cv(Seek.RELATIVE).def(name = "Seek type", info = "Forward/backward buttons seek by time (absolute) or fraction of total duration (relative).")
//   val showChapters by cv(POPUP_SHARED) { dv -> seeker.chapterDisplayMode.apply { value = dv } }.def(name = "Chapters show", info = "Display chapter marks on seeker.")
//   val showChapOnHover by cv(HOVER) { dv -> seeker.chapterDisplayActivation.apply { value = dv } }.def(name = "Chapter open on", info = "Opens chapter also when mouse hovers over them.")
//   val snapToChap by cv(false) { dv -> seeker.chapterSnap.apply { value = dv } }.def(name = "Snap seeker to chapters", info = "Enable snapping to chapters during dragging.")
   val elapsedTime by cv(true).def(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")

   init {
      root.prefSize = 850.emScaled x 200.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      ps.status sync { statusChanged(it) } on onClose
      ps.loopMode sync { loopModeChanged(it) } on onClose
      ps.mute sync { muteChanged(ps) } on onClose
      ps.volume sync { muteChanged(ps) } on onClose
      APP.audio.playingSong.onUpdateAndNow { playingItemChanged(it) } on onClose

      root.onEventDown(MOUSE_CLICKED, BACK) { PlaylistManager.playPreviousItem() }
      root.onEventDown(MOUSE_CLICKED, FORWARD) { PlaylistManager.playNextItem() }
      root.installDrag(
         IconMD.PLAYLIST_PLUS,
         "Add to active playlist",
         { e -> e.dragboard.hasAudio() },
         { e -> PlaylistManager.use { it.addItems(e.dragboard.getAudio()) } }
      )

      root.lay += hBox {
         padding = Insets(10.emScaled)

         lay += stackPane {
            lay += seeker.apply {
               valueSymmetrical.value = true

               blockIncrement syncFrom seekType.flatMap {
                  when (it) {
                     Seek.RELATIVE -> APP.audio.seekUnitP
                     Seek.ABSOLUTE -> ps.duration zip APP.audio.seekUnitT map { (td, sd) -> sd.divMillis(td) }
                  }
               } on onClose

               val valueSuppressor = Suppressor()
               bindTimeToSmooth(ps) { valueSuppressor.suppressing { value.value = it } } on onClose
               valueSoft attach { valueSuppressor.suppressed { if (!isValueChanging.value) APP.audio.seek(it) } } on onClose
            }
            lay += vBox {
               alignment = Pos.CENTER
               isPickOnBounds = false
               lay += label {
                  isMouseTransparent = true
                  isPickOnBounds = false
                  styleClass += "seeker-label"
               }
               lay += hBox {
                  alignment = Pos.CENTER
                  isPickOnBounds = false
                  isFillHeight = false
                  lay += f2.size(36).scale(2.0)
                  lay += f3.size(72).scale(2.0)
                  lay += f4.size(36).scale(2.0)
               }
               lay += currTime.apply {
                  isPickOnBounds = false
                  styleClass += "seeker-label"
                  onEventDown(MOUSE_CLICKED, PRIMARY) { elapsedTime.toggle() }

                  seeker.ticks += listOf(0.0, 0.25, 0.5, 0.75)
                  seeker.isValueChanging flatMap {
                     if (it) seeker.valueShown zip ps.duration map { (at, total) -> total * at }
                     else ps.currentTime map { it.toSeconds().toLong() } map { ps.currentTime.value }
                  } zip elapsedTime sync { (current, e) ->
                     text = if (e) current.toHMSMs() else "- " + (ps.duration.value - current).toHMSMs()
                  } on onClose
               }
            }
         }
         lay += borderPane {
            top = stackPane {
               lay += SeekerCircle(100.emScaled).apply {
                  editable.value = false
                  ps.loopMode sync {
                     value.value = when (it) {
                        OFF -> 0.0
                        PLAYLIST -> 1.0
                        SONG -> 0.1
                        RANDOM -> 0.5
                     }
                  }
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += loopB
               }
            }
            bottom = stackPane {
               lay += SeekerCircle(100.emScaled).apply {
                  blockIncrement.value = VolumeProperty.STEP
                  value attachTo ps.volume
                  ps.volume sync { value.value = it.toDouble() }
               }
               lay += stackPane {
                  isPickOnBounds = false
                  lay += muteB
               }
            }
         }
      }
   }

   private fun playFile(file: File) {
      PlaylistManager.use {
         it.addUri(file.toURI())
         it.playLastItem()
      }
   }

   private fun playingItemChanged(song: Metadata) {
//      seeker.reloadChapters(song)
      seeker.ticks setTo song.getChapters().chapters.map { it.time.divMillis(song.getLength()) }
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

   /** Circular [Slider] with value always normalized to 0..1. */
   class SeekerCircle(val W: Double): StackPane() {
      /** Value, 0..1. Default 0. */
      val value = v(0.0)
      /** Represents [value] but changes only when [isValueChanging] is false. Default 0. */
      val valueSoft = Handler1<Double>()
      /** Value shown to user, 0..1. Mey differ from [value] due to [isAnimated] and [isValueChanging]. Default 0. */
      val valueShown = v(value.value)
      /** Whether user can change [value] through ui. Only if true. Default true. */
      val editable = v(true)
      /** Whether animation between value changes is enabled. Default true. */
      val isAnimated = v(true)
      /** Whether user is changing the value. */
      val isValueChanging = v(false)
      /** Angle in degrees where the value starts. Default 0.0. */
      val valueStartAngle = v(0.0)
      /** Whether the value grows in both directions, ranging from 0-180 degrees. Default true. */
      val valueSymmetrical = v(false)
      /** Same as [Slider.blockIncrement] */
      val blockIncrement = v(0.1)
      /** Similar to as [Slider.showTickMarks] and [Slider.snapToTicks]. Default contains 0.0, 0.5 and 1.0. */
      val snaps = observableList(0.0, 0.5, 1.0)
      val ticks = observableList<Double>()

      private val anim = anim(200.millis) { valueShown.value = valueAnimFrom + it*(valueAnimTo-valueAnimFrom) }.intpl { sin(PI/2*it) }
      private val animSuppressor = Suppressor()
      private var valueAnimFrom = value.value
      private var valueAnimTo = value.value

      init {
         styleClass += "seeker-circle"
         editable sync { pseudoClassToggle("readonly", !editable.value) }
         prefSize = W/2.0 x W/2.0
         minSize = W/2.0 x W/2.0
         maxSize = W/2.0 x W/2.0
         isFocusTraversable = true
         onEventDown(KEY_PRESSED, DOWN) { decrement() }
         onEventDown(KEY_PRESSED, KP_DOWN) { decrement() }
         onEventDown(KEY_PRESSED, LEFT) { decrement() }
         onEventDown(KEY_PRESSED, KP_LEFT) { decrement() }
         onEventDown(KEY_PRESSED, RIGHT) { increment() }
         onEventDown(KEY_PRESSED, KP_RIGHT) { increment() }
         onEventDown(KEY_PRESSED, UP) { increment() }
         onEventDown(KEY_PRESSED, KP_UP) { increment() }
         onEventDown(KEY_PRESSED, HOME) { decrementToMin() }
         onEventDown(KEY_PRESSED, END) { incrementToMax() }

         value attach { if (!isValueChanging.value) valueSoft(it) }
         value attachChanges { _, n ->
            if (animSuppressor.isSuppressed) anim.stop()
            animSuppressor.suppressed {
               if (!isValueChanging.value) {
                  valueAnimFrom = valueShown.value
                  valueAnimTo = n
                  if (isAnimated.value) anim.playFromStart() else valueShown.value = valueAnimTo
               }
            }
         }

         lay += Group().apply {
            children += Rectangle().apply {
               fill = Color.TRANSPARENT
               width = W
               height = W
               isCache = true
               isCacheShape = true
               cacheHint = ROTATE
            }
            children += Circle(W/4.0).apply {
               styleClass += "seeker-circle-bgr"
               centerX = W/2.0
               centerY = W/2.0
               isCache = true
               isCacheShape = true
               cacheHint = ROTATE
               valueShown zip valueSymmetrical sync { (v, vs) -> rotate = if (vs) 180.0*v else 0.0 }
            }
            children += Circle(W/4.0).apply {
               styleClass += "seeker-circle-frg"
               centerX = W/2.0
               centerY = W/2.0
               clip = Group().apply {
                  children += Rectangle().apply {
                     fill = Color.TRANSPARENT
                     width = W
                     height = W
                     isCache = true
                     isCacheShape = true
                     cacheHint = ROTATE
                  }
                  children += Arc().apply {
                     this.type = ArcType.ROUND
                     this.centerX = W/2.0
                     this.centerY = W/2.0
                     this.radiusX = W/4.0
                     this.radiusY = W/4.0


                     isCache = true
                     isCacheShape = true
                     cacheHint = ROTATE

                     val updater = { _: Any? ->
                        length = 360.0*valueShown.value
                        startAngle = valueStartAngle.value - if (valueSymmetrical.value) 180.0*valueShown.value else 0.0
                     }
                     valueSymmetrical attach updater
                     valueStartAngle attach updater
                     valueShown attach updater
                     updater(Unit)
                  }
               }

               fun updateFromMouse(e: MouseEvent, onlyInside: Boolean, anim: Boolean) {
                  val polarPos = (e.xy - (centerX x centerY))
                  val angleRad = atan2(polarPos.x, polarPos.y) + PI + valueStartAngle.value*PI/180.0
                  val centerDist = polarPos distance (0 x 0)
                  val vNorm = (angleRad/2.0/PI + 0.25).rem(1.0)
                  val vRaw = when {
                     valueSymmetrical.value -> if (vNorm<=0.5) vNorm*2.0 else (1.0-vNorm)*2.0
                     else -> vNorm
                  }
                  val v = vRaw.clip().snap(e)
                  if (editable.value && (!onlyInside || centerDist<=radius)) {
                     if (isValueChanging.value && !anim) valueToAnimFalse(v)
                     else valueToAnimTrue(v)
                  }
               }
               onEventDown(SCROLL) { e ->
                  val polarPos = (e.xy - (centerX x centerY))
                  val centerDist = polarPos distance (0 x 0)
                  if (centerDist<=radius && e.deltaY.sign<0) increment() else decrement()
                  e.consume()
               }
               onEventDown(MOUSE_PRESSED, PRIMARY) {
                  this@SeekerCircle.requestFocus()
                  if (editable.value) {
                     updateFromMouse(it, true, true)
                     isValueChanging.value = true
                  }
               }
               onEventDown(MOUSE_RELEASED, PRIMARY) {
                  if (isValueChanging.value) {
                     isValueChanging.value = false
                     updateFromMouse(it, false, true)
                  }
               }
               onEventDown(MOUSE_DRAGGED, PRIMARY) {
                  if (isValueChanging.value)
                     updateFromMouse(it, false, false)
               }
               onEventDown(MOUSE_RELEASED, SECONDARY) {
                  if (isValueChanging.value) {
                     isValueChanging.value = false
                     valueShownToActualAnimTrue()
                  }
               }
               onEventDown(MOUSE_CLICKED, SECONDARY, false) {
                  if (it.isPrimaryButtonDown)
                     it.consume()
               }
               onEventDown(MOUSE_PRESSED, SECONDARY) {
                  if (isValueChanging.value) {
                     isValueChanging.value = false
                     valueShownToActualAnimTrue()
                  }
               }
            }
         }

         lay += object: StackPane() {
            val radius = W/4.0 + 5

            init {
               isPickOnBounds = false
               valueSymmetrical attach { requestLayout() }
               ticks.onChangeAndNow {
                  children setTo ticks.map { tick ->
                     Circle(5.0, Color.RED).apply {
                        styleClass += "seeker-circle-chapter"
                     }
                  }
               }
            }

            override fun layoutChildren() {
               (children zip ticks).forEach { (c, t) ->
                  val aRaw = 2.0*PI*t.toDouble() + valueStartAngle.value*PI/180.0
                  val a = if (valueSymmetrical.value) aRaw/2.0 else aRaw
                  c.asIs<Circle>().centerX = layoutBounds.centerX + radius*cos(a)
                  c.asIs<Circle>().centerY = layoutBounds.centerY + radius*sin(a)
               }
            }
         }
      }

      fun decrementToMin() { if (editable.value) value.setValueOf { 0.0 } }

      fun increment() { if (editable.value) value.setValueOf { (it - blockIncrement.value).clip() } }

      fun decrement() { if (editable.value) value.setValueOf { (it + blockIncrement.value).clip() } }

      fun incrementToMax() { if (editable.value) value.setValueOf { 1.0 } }

      private fun valueShownToActualAnimTrue() {
         valueAnimFrom = valueShown.value
         valueAnimTo = value.value
         if (isAnimated.value) anim.playFromStart() else valueShown.value = valueAnimTo
      }

      private fun valueShownToActualAnimFalse() {
         valueShown.value = value.value
      }

      private fun valueToAnimTrue(v: Double) {
         value.value = v
         if (!isValueChanging.value && value.value == v) valueSoft(v)
      }

      private fun valueToAnimFalse(v: Double) {
         animSuppressor.suppressing {
            valueShown.value = v
            value.value = v
            if (!isValueChanging.value && value.value == v) valueSoft(v)
         }
      }

      private fun Double.clip(): Double = clip(0.0, 1.0)

      private fun Double.snap(e: MouseEvent? = null): Double {
         val snap = e!=null && !e.isShiftDown && !e.isShortcutDown
         val snapBy = APP.ui.snapDistance.value / (2*PI*W/4.0)
         val snaps = if (snap) (snaps + ticks).toSet() else setOf()
         return snaps.minByOrNull { (it-this).absoluteValue }?.takeIf { (it-this).absoluteValue<=snapBy } ?: this
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Playback knobs"
      override val description = "Audio playback knob controls"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
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
//         Entry("Seeker > Chapter", "Add chapter", "seeker RMB"),
//         Entry("Seeker > Chapter mark", "Open chapter", "Hover"),
//         Entry("Seeker > Chapter popup", "Hide chapter", "LMB"),
//         Entry("Seeker > Chapter popup", "Hide chapter", "Escape"),
//         Entry("Seeker > Chapter popup", "Play from chapter", "2xLMB"),
//         Entry("Seeker > Chapter popup > Edit", "Start edit", "2xRMB"),
//         Entry("Seeker > Chapter popup > Edit", "Apply edit", "Enter"),
//         Entry("Seeker > Chapter popup > Edit", "Append new line", "Shift + Enter"),
//         Entry("Seeker > Chapter popup > Edit", "Cancel edit", "Escape"),
      )
      override val group = PLAYBACK

      fun GlyphIcons.icon(size: Double, block: (Boolean) -> Unit) = Icon(this, size).apply {
         boundsType = VISUAL
         onEventDown(MOUSE_CLICKED, PRIMARY) { block(true) }
         onEventDown(MOUSE_CLICKED, SECONDARY) { block(false) }
      }
   }

}