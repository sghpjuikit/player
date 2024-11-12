package playerLoopMode

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.text.TextBoundsType.VISUAL
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.OFF
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.PLAYLIST
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.RANDOM
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode.SONG
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.HorizontalDock
import sp.it.pl.layout.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.SliderCircular
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.icon.boundsType
import sp.it.pl.ui.objects.icon.onClickDelegateKeyTo
import sp.it.pl.ui.objects.icon.onClickDelegateMouseTo
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggle
import sp.it.util.collections.setTo
import sp.it.util.file.div
import sp.it.util.math.dist
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class PlayerLoopMode(widget: Widget): SimpleController(widget), PlaybackFeature, HorizontalDock {
   val loopB = IconFA.RANDOM.icon(24.0) { APP.audio.state.playback.loopMode.toggle(it) }
   val ps = APP.audio.state.playback
   val size = 100.emScaled

   init {
      root.prefSize = size x size
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      ps.loopMode sync { loopModeChanged(it) } on onClose

      root.onEventDown(MOUSE_CLICKED, BACK) { PlaylistManager.playPreviousItem() }
      root.onEventDown(MOUSE_CLICKED, FORWARD) { PlaylistManager.playNextItem() }
      root.installDrag(
         IconMD.PLAYLIST_PLUS,
         "Add to active playlist",
         { e -> e.dragboard.hasAudio() },
         { e -> PlaylistManager.use { it.addItems(e.dragboard.getAudio()) } }
      )

      root.lay += SliderCircular(size).apply {
         val valueCount = LoopMode.entries.size.toDouble() - 1.0
         val mapping = LoopMode.entries.associateWith {
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

   companion object: WidgetCompanion {
      override val name = "Playback loop mode"
      override val description = "Playback loop mode circular control"
      override val descriptionLong = "$description."
      override val icon = IconMD.MUSIC_CIRCLE
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2023)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO)
      override val summaryActions = listOf(
         Entry("Controls", "Toggle next song mode", "Scroll"),
         Entry("Controls", "Toggle next song mode", "Loop icon LMB | RMB"),
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