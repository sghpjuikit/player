package playerVolume

import de.jensd.fx.glyphs.GlyphIcons
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.paint.Color.TRANSPARENT
import javafx.scene.shape.Circle
import javafx.scene.text.TextBoundsType.VISUAL
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.playback.VolumeProperty
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.HorizontalDock
import sp.it.pl.layout.feature.PlaybackFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
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
import sp.it.util.file.div
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class PlayerVolume(widget: Widget): SimpleController(widget), PlaybackFeature, HorizontalDock {
   val muteB = IconFA.VOLUME_UP.icon(24.0) { APP.audio.toggleMute() }
   val ps = APP.audio.state.playback
   val size = 100.emScaled

   init {
      root.prefSize = size x size
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      ps.mute sync { muteChanged(ps) } on onClose
      ps.volume sync { muteChanged(ps) } on onClose

      root.onEventDown(MOUSE_CLICKED, BACK) { PlaylistManager.playPreviousItem() }
      root.onEventDown(MOUSE_CLICKED, FORWARD) { PlaylistManager.playNextItem() }
      root.installDrag(
         IconMD.PLAYLIST_PLUS,
         "Add to active playlist",
         { e -> e.dragboard.hasAudio() },
         { e -> PlaylistManager.use { it.addItems(e.dragboard.getAudio()) } }
      )

      root.lay += SliderCircular(size).apply {
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

   companion object: WidgetCompanion {
      override val name = "Playback volume"
      override val description = "Playback volume circular control"
      override val descriptionLong = "$description."
      override val icon = IconMD.MUSIC_CIRCLE
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2023)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO)
      override val summaryActions = listOf(
         Entry("Controls", "Change volume", "Scroll"),
         Entry("Controls", "Mute", "Mute icon LMB | RMB"),
      )

      val logger = KotlinLogging.logger { }

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