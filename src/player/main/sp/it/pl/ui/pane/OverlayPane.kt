package sp.it.pl.ui.pane

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.effect.BoxBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.Z
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color.TRANSPARENT
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.stage.WindowEvent.WINDOW_SHOWN
import kotlin.math.sign
import kotlin.math.sqrt
import kotlinx.coroutines.invoke
import sp.it.pl.core.InfoUi
import sp.it.pl.core.NameUi
import sp.it.pl.main.APP
import sp.it.pl.main.resizeIcon
import sp.it.pl.plugin.impl.WallpaperChanger
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.isOpenChild
import sp.it.util.access.focused
import sp.it.util.access.readOnly
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.access.visible
import sp.it.util.access.vn
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.mapTo01
import sp.it.util.async.coroutine.IO
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.async.runFX
import sp.it.util.collections.setTo
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.math.P
import sp.it.util.math.clip
import sp.it.util.math.dist
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach1If
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.fires
import sp.it.util.reactive.into
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventDown1
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.syncTo
import sp.it.util.reactive.syncWhile
import sp.it.util.system.getWallpaperFile
import sp.it.util.ui.Util.layStack
import sp.it.util.ui.Util.setAnchors
import sp.it.util.ui.Util.stageFMNT
import sp.it.util.ui.applyViewPort
import sp.it.util.ui.bgr
import sp.it.util.ui.containsMouse
import sp.it.util.ui.getScreen
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.hasFocus
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.imgImplLoadFX
import sp.it.util.ui.makeScreenShot
import sp.it.util.ui.pane
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.screenToLocal
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.toP
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
   /** Handlers called just after this pane starts to show. Note that this is un-intuitively not prior to but after showing is taking place.  */
   val onShowing = Handler0()
   /** Handlers called just after this pane was shown. */
   val onShowed = Handler0()
   /** Handlers called just prior to this pane starting to hide. */
   val onHiding = Handler0()
   /** Handlers called just after this pane was hidden. */
   val onHidden = Handler0()
   /** True between [onShowed] and [onHiding]. Default false. */
   private val isShowingImpl = v(false)
   /** True between [onShowed] and [onHiding]. Read-only. */
   val isShowing = isShowingImpl.readOnly()
   /** True when focused and between [onShowed] and [onHiding]. Default false. */
   private val isShowingWithFocusImpl = v(false)
   /** True when focused and between [onShowed] and [onHiding]. Read-only. */
   val isShowingWithFocus = isShowingWithFocusImpl.readOnly()
   /** True when losing focus should hide this pane. Default true. */
   val isAutohide = v(true)

   protected val stage = vn<Stage>(null)

   private val bgr = stackPane {
      onEventUp(MOUSE_CLICKED) {
         opacity = Math.random()
      }
      onEventUp(SCROLL) {
         opacity = (opacity + it.deltaX.sign*0.1).clip(0.0, 1.0)
      }
   }
   private val resizeB: Icon
   private var resizing: Subscription? = null

   /** Graphical content or null if none. Setting the same content has no effect. */
   var content: Pane? = null
      set(nv) {
         val ov = field
         if (nv===ov) return
         resizing?.unsubscribe()

         if (ov!=null) ov.styleClass -= STYLECLASS_CONTENT
         if (nv!=null) nv.styleClass += STYLECLASS_CONTENT

         if (nv==null) {
            children.clear()
         } else {
            children setTo listOf(bgr, nv, layStack(resizeB, Pos.BOTTOM_RIGHT))
            resizeB.parent.isManaged = false
            resizeB.parent.isPickOnBounds = false
            resizing = nv.paddingProperty() syncTo (resizeB.parent as StackPane).paddingProperty()
         }
         field = nv
      }

   init {
      isVisible = false
      styleClass += STYLECLASS

      // autohide
      isShowingWithFocus attachFalse {
         if (isAutohide.value)
            runFX(50.millis) {
               if (display.value.isWindowBased() && scene?.window?.net { !it.isFocused && it.isShowing && !it.isOpenChild() }==true)
                  hide()
            }
      }
      // hide
      onEventDown(MOUSE_CLICKED, SECONDARY, false) {
         if (isShown() && !APP.ui.isLayoutMode) {
            hide()
            it.consume()
         }
      }
      onEventDown(KEY_PRESSED, ESCAPE, false) {
         if (isShown() && !APP.ui.isLayoutMode) {
            hide()
            it.consume()
         }
      }
      stage.into(Stage::sceneProperty).syncWhile {
         it?.onEventDown(KEY_PRESSED, ESCAPE, false) {
            if (isShown() && !APP.ui.isLayoutMode) {
               hide()
               it.consume()
            }
         }
      }

      resizeB = resizeIcon().apply {
         cursor = Cursor.SE_RESIZE
         isVisible = false
      }
   }

   /* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

   class OpAnim(sideEffect: (Double) -> Unit) {
      var dir = true
      private val animation by lazy { anim(sideEffect).intpl { it*it } }

      fun applyAt0() = animation.applyAt(0.0)

      fun show(then: () -> Unit = {}) {
         dir = true
         animation.dur(400.millis).playOpenDo { then() }
      }

      fun hide(then: () -> Unit = {}) {
         dir = false
         animation.dur(250.millis).playCloseDo { then() }
      }
   }

   private lateinit var displayUsedForShow: ScreenGetter // prevents inconsistency in start() and stop(), see use
   private val animation = OpAnim { animDo(it) }
   private val blurMax = 15
   private val blurStep = 1
   private val blur = BoxBlur(blurMax.toDouble(), blurMax.toDouble(), 3)
   private var opacityNode: Node? = null
   private var blurNode: Node? = null

   /** Show this pane with given value. */
   abstract fun show(data: T)

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
         onHiding()
         isShowingWithFocusImpl.value = false
         isShowingImpl.value = false
         animation.hide(::animEndJustAfter)
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
      displayUsedForShow = display.value
      displayUsedForShow.animStart(this)
   }

   private fun animDo(x: Double) {
      displayUsedForShow.animDo(this, x)
   }

   private fun animEndJustAfter() {
      displayUsedForShow.animEnd(this)
   }

   interface ScreenGetter {
      fun isWindowBased(): Boolean
      fun computeScreen(): Screen
   }

   enum class Display(name: String, info: String): ScreenGetter, NameUi {
      WINDOW(
         "Active window",
         "Display overlay within the active window. If no window is available, falls back to 'Screen containing mouse'. " +
         "Not recommended as overlay content is usually designed for large area unfitting of application window."
      ),
      SCREEN_OF_WINDOW(
         "Screen of active window",
         "Display overlay on the screen containing the active window."
      ),
      SCREEN_OF_MOUSE(
         "Screen containing mouse",
         "Display overlay on the screen containing the active window. Most user friendly."
      ),
      SCREEN_PRIMARY(
         "Primary screen",
         "Display overlay on the main screen."
      );

      override val nameUi = name
               val infoUi = info
      override fun isWindowBased() = this != WINDOW
      override fun computeScreen(): Screen = when (this) {
         WINDOW -> fail()
         SCREEN_PRIMARY -> Screen.getPrimary()
         SCREEN_OF_WINDOW -> APP.windowManager.getActive()?.centre?.getScreen() ?: SCREEN_OF_MOUSE.computeScreen()
         SCREEN_OF_MOUSE -> getScreenForMouse()
      }
   }

   fun ScreenGetter.animStart(op: OverlayPane<*>) {
      if (this==Display.WINDOW) {
         APP.windowManager.getActive()
            .ifNotNull { window ->
               val root = window.root
               if (op !in root.children) {
                  root.children += op
                  setAnchors(op, 0.0)
                  op.toFront()
               }
               op.isVisible = true
               op.requestFocus()     // 'bug fix' - we need focus or key events won't work

               op.opacityNode = window.content
               op.blurNode = window.content
               op.blurNode!!.effect = op.blur

               op.sceneProperty().syncNonNullWhile { it.focusOwnerProperty() sync { op.isShowingWithFocusImpl.value = op.isShowing.value && op.hasFocus() } } on { s -> op.visible.attach1If({ !it }) { s.unsubscribe() } }
               op.animation.show {
                  isShowingImpl.value = true
                  op.onShowed()
               }
               op.onShowing()
            }
            .ifNull {
               op.displayUsedForShow = Display.SCREEN_OF_MOUSE
               Display.SCREEN_OF_MOUSE.animStart(op)
            }
      } else {
         runSuspendingFx {
            val screen = computeScreen()
            val image = op.displayBgr.value.computeImage(screen)?.let { IO { it.adjustForBlur(blurMax) } }
            val bgr = pane {
               styleClass += "bgr-image"   // replicate app window bgr for style & consistency
            }
            val contentImg = ImageView(image).apply {
               fitWidth = screen.bounds.width + op.displayBgr.value.computeImageBlurPaddingAmount() * 2 * blurMax
               fitHeight = screen.bounds.height + op.displayBgr.value.computeImageBlurPaddingAmount() * 2 * blurMax
               applyViewPort(image, OUTSIDE)
            }
            val root = stackPane(stackPane(bgr, contentImg)) {
               styleClass += "overlay-window"
               background = bgr(TRANSPARENT)
            }

            op.stage.value = stageFMNT(screen, op.displayBgr.value.stageStyle, false).apply {
               scene = Scene(root)
               scene.fill = TRANSPARENT
               initOverlayWindow(this@OverlayPane)
               scene.root.onEventDown(KEY_RELEASED, Z, consume = false) {
                  if (it.isMetaDown) {
                     op.isAutohide.toggle()
                     it.consume()
                  }
               }
            }

            if (op !in root.children) {
               root.children += op
               op.toFront()
            }
            op.isVisible = true
            op.requestFocus()     // 'bug fix' - we need focus or key events won't work

            op.opacityNode = null
            op.blurNode = contentImg
            op.blurNode!!.effect = op.blur

            op.animation.applyAt0()
            op.stage.value!!.onEventDown1(WINDOW_SHOWN) {
               op.onShowing()
               op.animation.show {
                  isShowingImpl.value = true
                  op.onShowed()
                  op.stage.value!!.focused sync { op.isShowingWithFocusImpl.value = it } on op.isShowing.fires(false)
               }
            }
            op.stage.value!!.show()
            op.stage.value!!.requestFocus()
         }
      }
   }

   fun ScreenGetter.animDo(op: OverlayPane<*>, it: Double) {
      val x = if (!op.animation.dir) it else mapTo01(it, 0.0, 0.4)
      val y = if (!op.animation.dir) it else mapTo01(it, 0.5, 1.0)
      if (this!=Display.WINDOW && (op.displayBgr.value==ScreenBgrGetter.SCREEN_BGR || op.displayBgr.value==ScreenBgrGetter.NONE)) {
         op.opacity = 1.0
         op.stage.value?.opacity = x*x
      } else {
         op.opacity = x*x
         op.stage.value?.opacity = 1.0
      }
      op.opacityNode?.opacity = 1 - x*0.5
      op.content?.opacity = y*y
      val b = if (op.displayBgr.value.needsBlur) blurMax*sqrt(it) else 0.0
      if (b==0.0 || b==blurMax.toDouble() || op.blur.height dist b > blurStep) {
         op.blur.height = b
         op.blur.width = b
      }
   }

   fun ScreenGetter.animEnd(op: OverlayPane<*>) {
      op.opacityNode?.effect = null
      op.blurNode?.effect = null
      op.opacityNode = null
      op.blurNode = null
      op.onHidden()
      if (this==Display.WINDOW) {
         op.isVisible = false
      } else {
         op.removeFromParent()
         op.stage.value?.close()
         op.stage.value?.scene?.root?.asIf<Pane>()?.children?.clear()
         op.stage.value?.scene = null
         op.stage.value = null
      }
   }

   companion object {
      private const val IS_SHOWN = "visible"
      const val STYLECLASS = "overlay-pane"
      const val STYLECLASS_BGR = "overlay-pane-bgr"
      const val STYLECLASS_CONTENT = "overlay-pane-content"
      const val PSEUDOCLASS_CONTENT_FULL_SIZE = "full-size"

      fun Window.initOverlayWindow(overlay: OverlayPane<*>): Unit = properties.put("overlayWindow", overlay).toUnit()
      fun Window.asOverlayWindow(): OverlayPane<*>? = properties["overlayWindow"].asIf()
      fun Window.isOverlayWindow(): Boolean = asOverlayWindow()!=null
   }
}

enum class ScreenBgrGetter(val stageStyle: StageStyle, val needsBlur: Boolean, override val nameUi: String, override val infoUi: String): NameUi, InfoUi {
   NONE(
      StageStyle.TRANSPARENT, false,
      "Content behind",
      "Use user preferred application window effect (transparency, opacity, blur). " +
      "Can display dynamic content behind window (such as video). " +
      "Requires transparent window which reduces performance, significantly so if blue is blur is also enabled."
   ),
   SCREEN_SHOT(
      StageStyle.UNDECORATED, true,
      "Content behind (static)",
      "Displays static background of the screen content at the time the overlay was shown. " +
      "Essentially `Content behind` without support of dynamic background content, but much better performance"
   ),
   SCREEN_BGR(
      StageStyle.UNDECORATED, true,
      "Wallpaper",
      "Displays desktop wallpaper as background. Performs the best and arguably the least distracting."
   );

   suspend fun computeImage(screen: Screen): Image? =
      when (this) {
         NONE -> null
         SCREEN_SHOT -> screen.makeScreenShot()
         SCREEN_BGR -> null
               ?: APP.plugins.get<WallpaperChanger>()?.wallpaperImage?.value
               ?: IO { screen.getWallpaperFile()?.let { imgImplLoadFX(it, ImageSize(-1.0, -1.0), true) } }
      }

   fun computeImageBlurPaddingAmount(): Double =
      when (this) {
         NONE -> 0.0
         SCREEN_SHOT -> 1.0
         SCREEN_BGR -> 2.0
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
                     offset = resizable.size - resizable.screenToLocal(it).toP()
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
                     offset = resizable.size - resizable.screenToLocal(it).toP()
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