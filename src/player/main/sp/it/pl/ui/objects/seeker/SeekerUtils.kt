package sp.it.pl.ui.objects.seeker

import sp.it.pl.audio.tagging.Metadata as SongMetadata
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.Event
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import javafx.scene.media.MediaPlayer.Status.PLAYING
import javafx.util.Duration
import javafx.util.Duration.ONE
import javafx.util.Duration.ZERO
import sp.it.pl.audio.playback.PlaybackState
import sp.it.pl.audio.tagging.Chapter
import sp.it.pl.audio.tagging.Chapter.Companion.validateChapterText
import sp.it.pl.audio.tagging.write
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.appTooltip
import sp.it.pl.main.emScaled
import sp.it.pl.ui.item_node.STYLECLASS_CONFIG_EDITOR_WARN_BUTTON
import sp.it.pl.ui.objects.SpitText.Companion.computeNaturalWrappingWidth
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.Shower
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.v
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Loop
import sp.it.util.collections.setTo
import sp.it.util.functional.getAny
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.Util.layHeaderRight
import sp.it.util.ui.lay
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.textArea
import sp.it.util.ui.typeText
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.divMillis
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs

/** Observes [PlaybackState] durations to update playback position in 0-1 value range. */
fun bindTimeTo(playback: PlaybackState, smooth: Boolean, block: (Double) -> Unit): Subscription =
   if (smooth) bindTimeToSmooth(playback, block)
   else bindTimeToDiscrete(playback, block)

/** Observes [PlaybackState] durations to update playback position in 0-1 value range in regular macro intervals. */
fun bindTimeToDiscrete(playback: PlaybackState, block: (Double) -> Unit): Subscription =
   playback.currentTime zip playback.duration sync { (c, t) -> block(if (t==null) 0.0 else c.divMillis(t)) }

/** Observes [PlaybackState] durations to update playback position in 0-1 value range on each JavaFX UI pulse. */
fun bindTimeToSmooth(playback: PlaybackState, block: (Double) -> Unit): Subscription {
   var timeTot: ObjectProperty<Duration> = playback.duration
   var timeCur: ObjectProperty<Duration> = playback.currentTime
   var posLast = 0.0
   var posLastFrame: Long = 0
   var posUpdateInterval = 20.0
   var posLastUpdate: Long = 0

   fun timeUpdate() {
      if (timeTot.value==null) return  // bug fix
      posLast = timeCur.value.divMillis(timeTot.value)
      posLastFrame = 0
      posUpdateInterval = 0.0
      block(posLast)
   }

   fun timeUpdateDo(frame: Long) {
      if (APP.audio.state.playback.status.value==PLAYING) {
         val dt = if (posLastFrame==0L) 0 else (frame - posLastFrame)/1000000
         val dp = dt/timeTot.get().toMillis()
         posLast += dp
         val now = System.currentTimeMillis()
         if (now - posLastUpdate>posUpdateInterval) {
            posLastUpdate = now
            block(posLast)
         }
      }
      posLastFrame = frame
   }

   val timeUpdater = ChangeListener<Duration> { _: ObservableValue<*>, _: Any, _: Any -> timeUpdate() }
   val timeLoop = Loop { frame: Long -> timeUpdateDo(frame) }

   timeTot.removeListener(timeUpdater)
   timeCur.removeListener(timeUpdater)
   timeTot.addListener(timeUpdater)
   timeCur.addListener(timeUpdater)
   timeUpdater.changed(null, ZERO, ZERO)
   timeLoop.start()
   return Subscription {
      timeLoop.stop()
      timeTot.unbind()
      timeCur.unbind()
      timeTot.removeListener(timeUpdater)
      timeCur.removeListener(timeUpdater)
      timeTot = SimpleObjectProperty(ONE)
      timeCur = SimpleObjectProperty(ONE)
      timeUpdater.changed(null, null, null)
   }

}

class SongChapterEdit(song: SongMetadata, chapter: Chapter, pos01: Double, i: Int, isNew: Boolean) {
   @JvmField val position01 = pos01
   @JvmField val song = song
   @JvmField val chapter = chapter
   @JvmField val i = i

   /** Whether this chapter is being created */
   @JvmField val isNew = isNew

   /** Whether editing is currently active */
   @JvmField val isEdited = v(false)

   @JvmField val onHidden = Handler0()

   private var messageAnimation: Anim? = null
   private val message = textArea()
   private val content = stackPane(message)
   private lateinit var p: PopWindow
   private lateinit var ta: TextArea
   private lateinit var prevB: Icon
   private lateinit var nextB: Icon
   private lateinit var editB: Icon
   private lateinit var commitB: Icon
   private lateinit var delB: Icon
   private lateinit var cancelB: Icon

   constructor(song: SongMetadata, posDuration: Duration, pos01: Double): this(song, Chapter(posDuration, ""), pos01, INDEX_NEW, true)
   constructor(song: SongMetadata, i: Int): this(song, song.getChapters().chapters[i], song.getChapters().chapters[i].time divMillis song.getLength(), i, false)

   fun hidePopup(): Unit =
      if (this::p.isInitialized)
         p.hide() else Unit

   fun showPopup(shower: Shower) {
      if (!this::p.isInitialized) {

         // text content
         message.isWrapText = true
         message.isEditable = false
         message.prefSize = USE_COMPUTED_SIZE.x2
         val messageInterpolator: Function1<Double, String> = typeText(chapter.text, '\u2007')
         messageAnimation = anim((10*chapter.text.length).millis) { message.text = messageInterpolator(it) }.delay(200.millis)
         content.minSize = 300.emScaled x 200.emScaled
         content.prefSize = 300.emScaled x 200.emScaled
         content.padding = Insets(10.0)
         content.onEventDown(Event.ANY) { if (isEdited.value) it.consume() }

         // buttons
         editB = Icon(IconFA.EDIT, 11.0, "Edit chapter") { editStart() }
         commitB = Icon(IconFA.CHECK, 11.0, "Confirm changes") { editCommit() }
         delB = Icon(IconFA.TRASH_ALT, 11.0, "Remove chapter") { song.write { it.removeChapter(chapter, song) } }
         cancelB = Icon(IconFA.REPLY, 11.0, "Cancel edit") { editCancel() }
         prevB = Icon(IconFA.CHEVRON_LEFT, 11.0, "Previous chapter") {
            hidePopup()
            p.onHidden.attach1 { SongChapterEdit(song, i-1).showPopup(shower) }
         }
         nextB = Icon(IconFA.CHEVRON_RIGHT, 11.0, "Next chapter") {
            p.onHidden.attach1 { SongChapterEdit(song, i+1).showPopup(shower) }
            hidePopup()
         }
         kotlin.run {
            val chapters = song.getChapters().chapters
            if (chapters.size - 1==i) nextB.isDisable = true
            if (0==i) prevB.isDisable = true
         }
         // popup
         p = PopWindow().also { p ->
            p.content.value = content
            isEdited sync { p.isAutohide.value = !it } // breaks editing >> p.setAutoHide(true);
            p.isEscapeHide.value = true
            p.onHidden += {
               if (isEdited.value) editCancel()
               onHidden()
            }
            p.title.value = chapter.time.toHMSMs()
            p.headerIcons setTo listOf(prevB, nextB, editB, delB)
            content.onEventUp(MOUSE_CLICKED) {
               if (!isEdited.value) {
                  if (it.clickCount==1 && it.button==PRIMARY && it.isStillSincePress && p.isAutohide.value) {
                     hidePopup()
                     it.consume()
                  }
                  if (it.clickCount==2) {
                     if (it.button==SECONDARY) editStart()
                     if (it.button==PRIMARY) seekTo()
                     it.consume()
                  }
               }
            }
         }
      }
      if (!p.isShowing) {
         p.show(shower)
         messageAnimation?.play()
      }
      if (isNew) editStart()
   }

   /** Starts editable mode. */
   fun editStart() {
      if (isEdited.value) return

      isEdited.value = true
      ta = textArea {
         // resize on text change
         textProperty() sync {
            val w = computeNaturalWrappingWidth(it, font)
            prefWidth = w
            prefHeight = 0.8*w
         }
         isWrapText = true
         text = message.text
         onEventDown(KEY_PRESSED, ENTER, false) {
            if (isEdited.value) {
               if (it.isShiftDown) insertText(caretPosition, "\n")
               else editCommit()
               it.consume()
            }
         }
         onEventDown(KEY_PRESSED, ESCAPE, false) {
            if (isEdited.value) {
               editCancel()
               it.consume()
            }
         }

      }

      // validation
      val warnTooltip = appTooltip()
      val warnB = Icon().apply {
         size(11)
         styleclass(STYLECLASS_CONFIG_EDITOR_WARN_BUTTON)
         tooltip(warnTooltip)
      }
      ta.textProperty() sync {
         val result = validateChapterText(it)
         warnB.isVisible = result.isError
         commitB.isDisable = result.isError
         warnTooltip.text = result.map { "" }.getAny()
      }

      // maintain content
      content.lay -= message
      content.lay += layHeaderRight(5.0, CENTER, ta, warnB)
      p.headerIcons setTo listOf(commitB, cancelB)
   }

   /** Ends editable mode and applies changes. */
   fun editCommit() {
      if (chapter.text!=ta.text) {
         message.text = ta.text
         chapter.text = ta.text
         song.write { it.addChapter(chapter, song) }
      }

      // maintain content
      content.lay -= ta.parent
      content.lay += message
      p.headerIcons setTo listOf(prevB, nextB, editB, delB)

      isEdited.value = false
   }

   /** Ends editable mode and discards all changes. */
   fun editCancel() {
      if (isNew) {
         hidePopup()
      } else {
         // maintain content
         content.lay -= ta.parent
         content.lay += message
         p.headerIcons setTo listOf(prevB, nextB, editB, delB)
      }
      // stop edit
      isEdited.value = false
   }

   fun seekTo() = APP.audio.seek(chapter.time)

   companion object {
      const val INDEX_NEW = -1
   }
}