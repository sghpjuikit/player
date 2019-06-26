package sp.it.pl.gui.pane

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.effect.BoxBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import javafx.stage.Stage
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.resizeButton
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.collections.setTo
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.orNull
import sp.it.util.math.P
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.syncTo
import sp.it.util.system.getWallpaperFile
import sp.it.util.ui.Util.createFMNTStage
import sp.it.util.ui.Util.layStack
import sp.it.util.ui.Util.setAnchors
import sp.it.util.ui.applyViewPort
import sp.it.util.ui.containsMouse
import sp.it.util.ui.getScreen
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.imgImplLoadFX
import sp.it.util.ui.makeScreenShot
import sp.it.util.ui.minus
import sp.it.util.ui.pane
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.screenToLocal
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.units.millis

/**
 * Pane laying 'above' standard content.
 *
 * Rather than using [StackPane.getChildren], use [content], which applies further decoration.
 * Content will align to center unless set otherwise.
 */
abstract class OverlayPane<in T>: StackPane() {

   /** Display method. */
   val display = v<ScreenGetter>(Display.SCREEN_OF_MOUSE)
   /** Display bgr (for SCREEN variants only). */
   val displayBgr = v(ScreenBgrGetter.SCREEN_BGR)
   /** Handlers called just after this pane was shown. */
   val onShown = Handler0()
   /** Handlers called just after this pane was hidden. */
   val onHidden = Handler0()

   private val resizeB: Icon
   private var resizing: Subscription? = null

   /** Graphical content or null if none. Setting the same content has no effect. */
   var content: Pane? = null
      set(nv) {
         val ov = field
         if (nv===ov) return
         resizing?.unsubscribe()

         if (ov!=null) ov.styleClass -= CONTENT_STYLECLASS
         if (nv!=null) nv.styleClass += CONTENT_STYLECLASS

         if (nv==null) {
            children.clear()
         } else {
            children setTo listOf(nv, layStack(resizeB, Pos.BOTTOM_RIGHT))
            resizeB.parent.isManaged = false
            resizeB.parent.isPickOnBounds = false
            resizing = nv.paddingProperty() syncTo (resizeB.parent as StackPane).paddingProperty()
         }
         field = nv
      }

   init {
      isVisible = false
      styleClass += ROOT_STYLECLASS

      onEventDown(MOUSE_CLICKED, SECONDARY, false) {
         if (isShown()) {
            hide()
            it.consume()
         }
      }
      onEventDown(KEY_PRESSED, ESCAPE, false) {
         if (isShown()) {
            hide()
            it.consume()
         }
      }
      onEventDown(KeyEvent.ANY) { it.consume() }  // user should not be able to interact with UI below

      resizeB = resizeButton().apply {
         cursor = Cursor.SE_RESIZE
         isVisible = false
      }
   }

   /* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

   private lateinit var displayUsedForShow: ScreenGetter // prevents inconsistency in start() and stop(), see use
   private val animation by lazy { anim(APP.animationFps) { animDo(it) }.dur(200.millis).intpl { it*it } } // lowering fps can help on hd screens & low-end hardware
   private val blurBack = BoxBlur(0.0, 0.0, 3)  // we need best possible quality
   private val blurFront = BoxBlur(0.0, 0.0, 1) // we do not need quality, hence iterations==1
   private var opacityNode: Node? = null
   private var blurFrontNode: Node? = null
   private var blurBackNode: Node? = null
   protected var stage: Stage? = null


   /** Show this pane with given value. */
   abstract fun show(data: T?)

   /** Show this pane. The content should be set before calling this method. */
   protected open fun show() {
      if (!isShown()) {
         properties[IS_SHOWN] = IS_SHOWN
         animStart()
      }
   }

   /** Hide this pane. */
   open fun hide() {
      if (isShown()) {
         properties -= IS_SHOWN
         animation.playCloseDo { animEnd() }
      }
   }

   /** @return true iff [.show] has been called and [.hide] not yet  */
   fun isShown(): Boolean = properties.containsKey(IS_SHOWN)

   override fun layoutChildren() {
      super.layoutChildren()

      if (resizeB.parent in children)
         resizeB.parent.resizeRelocate(content!!.layoutX, content!!.layoutY, content!!.width, content!!.height)
   }

   fun makeResizableByUser() {
      if (resizeB.isVisible) return
      resizeB.isVisible = true
      PolarResize().install(resizeB, this, content)
   }

   private fun animStart() {
      displayUsedForShow = display.get()
      displayUsedForShow.animStart(this)
   }

   private fun animDo(x: Double) {
      displayUsedForShow.animDo(this, x)
   }

   private fun animEnd() {
      displayUsedForShow.animEnd(this)
   }

   interface ScreenGetter {
      fun computeScreen(): Screen
   }

   enum class Display: ScreenGetter {
      WINDOW, SCREEN_OF_WINDOW, SCREEN_OF_MOUSE, SCREEN_PRIMARY;

      override fun computeScreen(): Screen = when (this) {
         WINDOW -> fail()
         SCREEN_PRIMARY -> Screen.getPrimary()
         SCREEN_OF_WINDOW -> APP.windowManager.getActive().orNull()?.centre?.getScreen()
            ?: SCREEN_OF_MOUSE.computeScreen()
         SCREEN_OF_MOUSE -> getScreenForMouse()
      }
   }

   fun ScreenGetter.animStart(op: OverlayPane<*>) {
      if (this==Display.WINDOW) {
         APP.windowManager.getActive().ifPresentOrElse(
            { window ->
               // display overlay pane
               val root = window.root
               if (op !in root.children) {
                  root.children += op
                  setAnchors(op, 0.0)
                  op.toFront()
               }
               op.isVisible = true
               op.requestFocus()     // 'bug fix' - we need focus or key events wont work

               // blur can reduce animation performance
               // - apply blur only on the content, it is generally smaller than this entire pane
               // - decrease blur iteration count (we don not need super blur quality here)
               // - decrease blur amount

               op.opacityNode = window.content
               op.blurBackNode = window.root
               if (!op.children.isEmpty()) op.blurFrontNode = op.children[0]
               op.blurBackNode!!.effect = op.blurBack
               op.blurFrontNode!!.effect = op.blurFront

               // start showing
               op.animation.playOpenDo(null)
               op.onShown()
            },
            {
               op.displayUsedForShow = Display.SCREEN_OF_MOUSE
               Display.SCREEN_OF_MOUSE.animStart(op)
            }
         )
      } else {
         val screen = computeScreen()
         op.displayBgr.get().getImgAndDo(screen) { image ->
            val bgr = pane {
               styleClass += "bgr-image"   // replicate app window bgr for style & consistency
            }
            val contentImg = ImageView(image).apply {
               fitWidth = screen.bounds.width
               fitHeight = screen.bounds.height
               applyViewPort(image, FitFrom.OUTSIDE)
            }
            val root = stackPane(stackPane(bgr, contentImg))

            op.stage = createFMNTStage(screen, false).apply {
               scene = Scene(root)
            }

            // display overlay pane
            if (op !in root.children) {
               root.children += op
               op.toFront()
            }
            op.isVisible = true
            op.requestFocus()     // 'bug fix' - we need focus or key events wont work

            // apply effects (will be updated in animation)
            op.opacityNode = contentImg
            op.blurBackNode = contentImg
            if (!op.children.isEmpty()) op.blurFrontNode = op.children[0]
            op.blurBackNode!!.effect = op.blurBack
            op.blurFrontNode!!.effect = op.blurFront

            op.animation.applyAt(0.0)
            op.stage!!.show()
            op.stage!!.requestFocus()

            // start showing
            // the preparation may cause an animation lag, hence delay a bit
            runFX(30.millis) {
               op.animation.playOpenDo(null)
               op.onShown()
            }
         }
      }
   }

   fun ScreenGetter.animDo(op: OverlayPane<*>, x: Double) {
      if (opacityNode!=null) { // bug fix, not 100% sure why it is necessary
         if (this!=Display.WINDOW && (op.displayBgr.get()==ScreenBgrGetter.SCREEN_BGR || op.displayBgr.get()==ScreenBgrGetter.NONE)) {
            op.stage!!.opacity = x
            op.opacityNode!!.opacity = 1 - x*0.5
            op.opacity = 1.0
            op.blurBack.height = 15.0*x*x
            op.blurBack.width = 15.0*x*x
            // improves performance a lot with little demerit
            // op.blurFront.height = 20*(1-x*x)
            // op.blurFront.width = 20*(1-x*x)
            op.scaleX = 1 + 0.2*(1 - x)
            op.scaleY = 1 + 0.2*(1 - x)
         } else {
            op.opacityNode!!.opacity = 1 - x*0.5
            op.opacity = x
            op.blurBack.height = 15.0*x*x
            op.blurBack.width = 15.0*x*x
            // improves performance a lot with little demerit
            // op.blurFront.height = 20*(1-x*x)
            // op.blurFront.width = 20*(1-x*x)
            op.scaleX = 1 + 2*(1 - x)
            op.scaleY = 1 + 2*(1 - x)
         }
      }
   }

   fun ScreenGetter.animEnd(op: OverlayPane<*>) {
      op.opacityNode!!.effect = null
      op.blurFrontNode!!.effect = null
      op.blurBackNode!!.effect = null
      op.opacityNode = null
      op.blurFrontNode = null
      op.blurBackNode = null
      op.onHidden()
      if (this==Display.WINDOW) {
         op.setVisible(false)
      } else {
         op.stage!!.close()
         op.stage?.scene?.root?.asIf<Pane>()?.children?.clear()
         op.stage?.scene = null
         op.stage = null
         op.removeFromParent()
      }
   }

   companion object {
      private const val IS_SHOWN = "visible"
      private const val ROOT_STYLECLASS = "overlay-pane"
      private const val CONTENT_STYLECLASS = "overlay-pane-content"
   }
}

enum class ScreenBgrGetter {
   NONE, SCREEN_SHOT, SCREEN_BGR;

   fun getImgAndDo(screen: Screen, action: (Image?) -> Unit) {
      when (this) {
         NONE -> action(null)
         SCREEN_SHOT -> action(screen.makeScreenShot())
         SCREEN_BGR -> {
            runNew {
               val img = screen.getWallpaperFile()?.let { imgImplLoadFX(it, -1, -1, true) }
               runFX { action(img) }
            }
         }
      }
   }
}

private class PolarResize {
   private var isActive = false
   private lateinit var offset: P

   /**
    * @param dragActivator some child node or null to use corner of the resizable
    * @param eventEmitter the node that has mouse event handlers installed
    * @param resizable the node that will resize
    */
   fun install(dragActivator: Node?, eventEmitter: Node, resizable: Pane?): Subscription {
      if (resizable==null) return Subscription()

      return Subscription(
         when {
            dragActivator!=null -> {
               dragActivator.onEventUp(MOUSE_PRESSED) {
                  // drag by a resizable Node
                  if (dragActivator.containsMouse(it)) {
                     isActive = true
                     offset = resizable.size - resizable.screenToLocal(it)
                  }
               }
            }
            else -> {
               resizable.onEventUp(MOUSE_PRESSED) {
                  // drag by corner
                  val cornerSize = 30.0
                  val n = it.source as Pane
                  if (it.x>=n.width - cornerSize && it.y>=n.height - cornerSize) {
                     isActive = true
                     offset = resizable.size - resizable.screenToLocal(it)
                  }
               }
            }
         },
         eventEmitter.onEventDown(MOUSE_RELEASED) { isActive = false },
         eventEmitter.onEventDown(MOUSE_DRAGGED) {
            if (isActive) {
               val n = it.source as Pane
               resizable.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)
               resizable.setPrefSize(
                  2*(it.x + offset.x - n.layoutBounds.width/2),
                  2*(it.y + offset.y - n.layoutBounds.height/2)
               )
               it.consume()
            }
         }
      )
   }

}