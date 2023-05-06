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
import mu.KLogging
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

   companion object: WidgetCompanion, KLogging() {
      override val name = "Playback loop mode"
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