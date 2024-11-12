package sp.it.pl.ui.objects.image

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntFunction
import javafx.animation.Animation
import javafx.animation.Animation.INDEFINITE
import javafx.animation.Animation.Status.STOPPED
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.image.Image
import javafx.util.Duration
import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import sp.it.util.JavaLegacy
import sp.it.util.async.VT
import sp.it.util.async.coroutine.launch
import sp.it.util.async.future.Fut
import sp.it.util.async.future.orNull
import sp.it.util.async.runVT
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.file.type.mimeType
import sp.it.util.functional.consumer
import sp.it.util.functional.ifNull
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.type.Util
import sp.it.util.ui.image.ImageFrame
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.Params
import sp.it.util.ui.image.imgImplLoadFX
import sp.it.util.ui.image.isImageAnimated
import sp.it.util.ui.image.loadImageFrames
import sp.it.util.ui.size
import sp.it.util.units.millis

/** [Thumbnail] animation helper */
class ThumbnailAnim(val thumbnail: Thumbnail) {

   private var animInitialized = false
   private var animCycleCount = INDEFINITE
   private var animation: Timeline? = null
   private var animImages: Fut<List<ImageFrame>?>? = null
   private var animCommand = {}

   /** Clears state to be usable for next animation/image. Call just before loading image. */
   fun animDispose() {
      animInitialized = false
      animCycleCount = INDEFINITE
      animCommand = {}
      animation?.stop()
      animation = null
      animImages?.ui { it?.forEach { JavaLegacy.destroyImage(it.img) } }
      animImages = null
      thumbnail.image.value.net { if (thumbnail.imageView.image!==it) thumbnail.setImgFrame(it) }
   }

   private fun animInitialize(lazy: Boolean) {
      if (!lazy) animInitializeImpl()
      if (animImages==null) return
      if (animImages!!.isDone()) animCommand()
      else animImages = animImages!!.ui { animCommand(); it }
   }

   private fun animInitializeImpl() {
      if (animInitialized) return
      animInitialized = true

      val i = thumbnail.image.value
      var f = thumbnail.file
      var fMime = f?.mimeType()
      if (i==null || fMime==null) {
         return
      } else if (fMime.name=="image/gif") {
         try {
            var isAnim = isImageAnimated(f, fMime)
            if (isAnim) {
               var p = Params(f, ImageSize(i.size), thumbnail.fitFrom.value, fMime, false)
               animImages = runVT { imgImplLoadFX(p.file, p.size, p.scaleExact) }.then {
                  listOf(ImageFrame(0, 0, it))
               }.ui { it: List<ImageFrame>? ->
                  if (it!=null) {
                     var img = it.firstOrNull()?.img
                     thumbnail.setImgFrame(img)
                     animation = if (img==null) null else Util.getFieldValue(Util.getFieldValue(img, "animation"), "timeline")
                     animation?.stop()
                     animation?.cycleCount = animCycleCount
                  }
                  it
               }
            }
         } catch (t: Throwable) {
            logger.error(t) { "Failed to load image=$f animation" }
         }
      } else if (fMime.name=="image/webp") {
         try {
            var isAnim = isImageAnimated(f, fMime)
            if (isAnim) {
               var p = Params(f, ImageSize(i.size), thumbnail.fitFrom.value, fMime, false)
               animImages = runVT { loadImageFrames(p) }.ui { frames ->
                  animation = Timeline(
                     *frames!!.map { f -> KeyFrame(f.delayMs.millis, EventHandler { _ -> thumbnail.setImgFrame(f.img) }) }.toTypedArray()
                  )
                  animation?.cycleCount = animCycleCount
                  frames
               }
            }
         } catch (t: Throwable) {
            logger.error(t) { "Failed to load image=$f animation" }
         }
      }
   }

   /** @return whether the current image has animation (animation must be initialized before this call) */
   fun isAnimated(): Fut<Boolean> =
      animImages?.then { animation!=null } ?: Fut.fut(false) // same impl as Image.isAnimation(), which is not public

   /** @return whether the current image has animation and it is playing */
   fun isAnimating(): Boolean =
      animation?.net { it.currentRate!=0.0 } ?: false

   /** Plays or pauses currently loaded image animation  */
   fun animationPlayPause(play: Boolean): Unit =
      if (play) animationPlay()
      else animationPause()

   /** Plays currently loaded image animation  */
   fun animationPlay() {
      animCommand = { animation?.play() }
      animInitialize(false)
   }

   /** Pauses currently loaded image animation  */
   fun animationPause() {
      animCommand = { animation?.pause() }
      animInitialize(true)
   }

   /** Sets block to be executed if the current image animation finishes. Animatin is set to 1 loop. Call before animation starts.  */
   fun animationPlayOnceAndWait(): Fut<Unit> {
      // initialize
      if (animInitialized) fail { "Must be called before image animation is initialized" }
      animCycleCount = 1 // must not be indefinite or else future never ends
      animationPlay()
      // build future that waits for animation end
      val a = animImages
      return if (a==null)
          Fut.fut(Unit)
      else
         a.then {
            animation
         }.then(VT) {
            if (it==null) Unit else while (it.status!=STOPPED) Thread.sleep(1)
         }.thenRecover {
            Unit
         }
   }

   companion object {
      private val logger = KotlinLogging.logger { }
   }

}