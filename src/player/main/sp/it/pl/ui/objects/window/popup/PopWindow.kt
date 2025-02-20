package sp.it.pl.ui.objects.window.popup

import javafx.stage.Window as WindowFx
import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Popup
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle.TRANSPARENT
import javafx.stage.WindowEvent
import javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import javafx.stage.WindowEvent.WINDOW_HIDING
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.stage.WindowEvent.WINDOW_SHOWN
import sp.it.pl.main.APP
import sp.it.pl.main.resizeIcon
import sp.it.pl.main.windowPinIcon
import sp.it.pl.ui.objects.window.Shower
import sp.it.pl.ui.objects.window.WindowRoot
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.installWindowInteraction
import sp.it.pl.ui.objects.window.stage.popWindowOwner
import sp.it.pl.ui.pane.OverlayPane.Companion.isOverlayWindow
import sp.it.util.access.OrV
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.v
import sp.it.util.access.vAlways
import sp.it.util.access.vn
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.runFX
import sp.it.util.async.runLater
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.dev.printStacktrace
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.recurse
import sp.it.util.functional.traverse
import sp.it.util.math.P
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventDown1
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onEventUp1
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncWhile
import sp.it.util.system.hasFileChooserOpen
import sp.it.util.ui.areaBy
import sp.it.util.ui.borderPane
import sp.it.util.ui.hBox
import sp.it.util.ui.initClip
import sp.it.util.ui.initMouseDrag
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.max
import sp.it.util.ui.maxSize
import sp.it.util.ui.min
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.prefSize
import sp.it.util.ui.scene
import sp.it.util.ui.screenXy
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.size
import sp.it.util.ui.uiDelegate
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.ui.xy
import sp.it.util.units.millis

open class PopWindow {

   private var window: WindowFx? = null
   private var stageOwner: WindowFx? = null
   private val stage by lazy {
      val ownerNoShow = stageOwner==null
      val owner = stageOwner ?: APP.windowManager.createStageOwnerNoShow().apply { show() }
      object: Stage() {
         override fun hide() {
            //  Due to JavaFX bugs it is impossible to hide the owner after this is hidden, so we override hide()
            //  so the owner is hidden first. It automatically trues to hide this, after which we call super.hide()
            if (ownerNoShow && owner.isShowing) {
               owner.hide()
            } else {
               super.hide()
            }
         }
      }.apply {
         initStyle(TRANSPARENT)
         minWidth = 10.0
         minHeight = 10.0
         size = 10 x 10

         uiDelegate = this@PopWindow
         initOwner(owner)
         titleProperty() syncFrom this@PopWindow.title
         resizableProperty() syncFrom userResizable

         onEventUp(WINDOW_SHOWING) { onShowing() }
         onEventUp(WINDOW_SHOWN) { onShown() }
         onEventUp(WINDOW_HIDING) { onHiding() }
         onEventUp(WINDOW_HIDDEN) { onHidden() }

         scene = scene {
            fill = Color.TRANSPARENT
            installWindowInteraction()
         }
      }
   }
   private val popup by lazy {
      Popup().apply {
         uiDelegate = this@PopWindow
         isAutoFix = true
         autoHideProperty() syncFrom isAutohide
         hideOnEscapeProperty() syncFrom isEscapeHide
         scene.installWindowInteraction()
         size = 10 x 10

         onEventUp(WINDOW_SHOWING) { onShowing() }
         onEventUp(WINDOW_SHOWN) { onShown() }
         onEventUp(WINDOW_HIDING) { onHiding() }
         onEventUp(WINDOW_HIDDEN) { onHidden() }
      }
   }
   val root = WindowRoot()
   private val contentP: BorderPane
   private val titleL: Label
   private val header: BorderPane
   private val tillHidden = Disposer()
   private val tillHiding = Disposer()

   /** Handlers called just after this popup was shown. Called even when it was already showing. */
   val onContentShown = Handler0()
   /** Handlers called just before this popup was shown. */
   val onShowing = Handler0()
   /** Handlers called just after this popup was shown. */
   val onShown = Handler0()
   /** Handlers called just before this popup was hidden. */
   val onHiding = Handler0()
   /** Handlers called just after this popup was hidden. */
   val onHidden = Handler0()
   /** Root's [Node.styleClass]. */
   val styleClass get() = root.styleClass!!
   /** Root's [Parent.stylesheets]. */
   val stylesheets get() = root.stylesheets!!
   /** Root's [Node.properties]. */
   val properties get() = root.properties!!
   /** Whether show/hide is animated. */
   val animated = v(true)
   /** Show/hide animation duration. */
   val animationDuration = v(200.millis)

   /** Title text. Default empty. */
   val title = v("")
   /** Content node. Default null. */
   val content = vn<Node>(null)
   /** Whether header is visible. Header contains title and icons. Default true. */
   val headerVisible = v(true)
   /** Whether header icons are visible. Default true. */
   val headerIconsVisible = v(true)
   /** Whether this popup can be resized by dragging the resize icon. Default true. */
   val userResizable = v(true)
   /** Whether this popup can be moved by dragging the popup. Default true. */
   val userMovable = v(true)
   /** Whether this popup hides when it loses focus. Default false. */
   val isAutohide = v(false)
   /** Whether this popup hides when unconsumed [ESCAPE] is pressed. Default true. */
   val isEscapeHide = v(true)
   /** Whether this popup hides when unconsumed [javafx.scene.input.MouseButton.PRIMARY] is pressed. Default false. */
   val isClickHide = v(false)
   /** Whether showing this popup steals focus from currently focused system window. Default true. */
   val focusOnShow = v(true)
   /** Additional icons in the header. Order is respected (from left to right). Default empty. */
   val headerIcons = observableArrayList<Node>()!!

   /** Whether window content has transparent decoration. Default off. */
   val transparency = OrV(APP.windowManager.windowTransparency, Inherit());
   /** Whether window content has transparent decoration. Default off. */
   val effect = OrV(APP.windowManager.windowEffect, Inherit());

   /** Whether this popup is currently showing. */
   val isShowing get() = window?.isShowing ?: false

   /** Determines whether global window stage style setting is overridden for this window */
   var ignoreAsOwner: Boolean = false

   private val animation = lazy {
      anim {
         window?.opacity = it*it
         root.setScaleXYByTo(it, -15.0, 0.0)
      }.apply {
         onHiding += ::stop
      }
   }

   init {
      titleL = label {
         styleClass += "pop-window-title"

         textProperty() syncFrom title
      }

      val headerControls = hBox {
         styleClass += "header-buttons"
         styleClass += "pop-window-header-icons"
         alignment = CENTER_RIGHT

         val pinB = windowPinIcon(isAutohide).apply {
            styleclass("pop-window-pin-button")
            isFocusTraversable = false
         }

         headerIcons.onChangeAndNow {
            children setTo (headerIcons + pinB)
            children.forEach { it.isFocusTraversable = false }
         }
      }

      header = borderPane {
         styleClass += "pop-window-header"

         headerIconsVisible sync {
            children.clear()
            if (it) {
               left = titleL
               right = headerControls
               BorderPane.setAlignment(titleL, CENTER_LEFT)
               BorderPane.setAlignment(headerControls, CENTER_RIGHT)
            } else {
               right = titleL
               BorderPane.setAlignment(titleL, CENTER_RIGHT)
            }
         }
      }

      contentP = borderPane {
         styleClass += "pop-window-content"
         minPrefMaxWidth = Pane.USE_COMPUTED_SIZE
         minPrefMaxHeight = Pane.USE_COMPUTED_SIZE

         initClip()
         centerProperty() syncFrom content
         topProperty() syncFrom headerVisible.map { if (it) header else null }
      }

      val resizeB = resizeIcon().apply {
         padding = Insets(15.0)
         visibleProperty() syncFrom userResizable
         initMouseDrag(
            P(),
            { drag -> drag.data = window!!.size },
            { drag ->
               var newSize = drag.data + drag.diff
                   newSize.modify(root.minWidth(root.height) x root.minHeight(root.width), Double::coerceAtLeast)
               if (userResizable.value) window!!.size = newSize
               if (userResizable.value) root.prefSize = newSize
            }
         )
      }

      root.apply {
         isPickOnBounds = false
         styleClass += "pop-window"
         minPrefMaxWidth = Pane.USE_COMPUTED_SIZE
         minPrefMaxHeight = Pane.USE_COMPUTED_SIZE

         onEventDown(KEY_PRESSED, ESCAPE, consume = false) {
            if (isEscapeHide.value) {
               hide()
               it.consume()
            }
         }
         onEventDown(MOUSE_CLICKED, PRIMARY, consume = false) {
            if (isClickHide.value && it.isStillSincePress) {
               hide()
               it.consume()
            }
         }

         var dragStartWLocation = P()
         var dragStartLocation = P()
         var isBeingMoved = false
         onEventDown(MOUSE_PRESSED) {
            if (it.isPrimaryButtonDown) {
               dragStartWLocation = window!!.xy
               dragStartLocation = it.screenXy
               isBeingMoved = true
            }
         }
         onEventDown(MOUSE_DRAGGED) {
            if (userMovable.value && isBeingMoved)
               window!!.xy = dragStartWLocation + it.screenXy - dragStartLocation
         }
         onEventUp(MOUSE_RELEASED) {
            isBeingMoved = false
         }

         lay += contentP
         userResizable attach {
            if (it) lay(BOTTOM_RIGHT) += resizeB
            else lay -= resizeB
         }
      }
   }

   fun show(shower: Shower) = show(shower.owner, shower.show)

   fun show(windowOwner: WindowFx? = null, shower: (WindowFx) -> P) {
      tillHiding()
      tillHidden()
      runFX(100.millis) {
         if (focusOnShow.value) {
            stageOwner = windowOwner
            stage.apply {
               window = this
               if (!isShowing) window?.opacity = 0.0
               window?.popWindowOwner = windowOwner

               fun initHideWithOwner() {
                  if (windowOwner!=null && windowOwner!==UNFOCUSED_OWNER.orNull()) {
                     windowOwner.onEventUp(WINDOW_HIDING) { if (isShowing) hideImmediately() } on tillHidden
                     windowOwner.onEventUp(WINDOW_CLOSE_REQUEST) { if (isShowing) hideImmediately() } on tillHidden
                  }
               }

               fun initZOrder() {
                  if (windowOwner==null || windowOwner===UNFOCUSED_OWNER.orNull()) {
                     isAlwaysOnTop = true
                  } else {
                     val a1 = windowOwner.asIf<Stage>()?.alwaysOnTopProperty() ?: vAlways(false)
                     val a2 = windowOwner.focusedProperty()
                     val a3 = focusedProperty()
                     a1.syncWhile {
                        if (it) {
                           stage.isAlwaysOnTop = it
                           Subscription()
                        } else {
                           stage.isAlwaysOnTop = false
                           Subscription(
                              a2 attachTrue { stage.isAlwaysOnTop = true; runFX(50.millis) { stage.isAlwaysOnTop = false } },
                              a3 attachTrue { stage.isAlwaysOnTop = true; stage.isAlwaysOnTop = false },
                           )
                        }
                     } on tillHidden
                  }
               }

               fun initAutohide() {
                  focusedProperty() attach {
                     if (!it && isAutohide.value) {
                        runLater {
                           if (!isFocusedChild() && owner?.isFocused!=true && isShowing && owner.isShowing)
                              this@PopWindow.hide()
                        }
                     }
                  } on tillHidden
               }

               initHideWithOwner()
               initZOrder()
               initHideOnEscapeWhenNoFocus()
               onEventUp1(WINDOW_SHOWN) { onContentShown() }

               val wasShowing = isShowing
               if (!isShowing && animated.value) fadeIn()
               if (!isShowing) show()

               scene.root = root
               runFX((if (wasShowing) 0 else 200).millis) {
                  sizeToScene()
                  focus()
                  xy = shower(stage).net {
                     val s = Screen.getScreensForRectangle(it.areaBy(1 x 1)).firstOrNull()
                     if (s!=null) stage.maxSize = s.bounds.size
                     if (s==null) it else it max s.bounds.min min (s.bounds.max - size)
                  }
                  onIsShowing1 { initAutohide() } on tillHidden
               }
            }
         } else {
            popup.apply {
               window = this
               if (!isShowing) window?.opacity = 0.0
               window?.popWindowOwner = windowOwner
               initHideOnEscapeWhenNoFocus()

               content setToOne root

               if (!isShowing && animated.value) fadeIn()
               show(windowOwner ?: UNFOCUSED_OWNER.value)
               sizeToScene()
               xy = shower(this).net {
                  val s = Screen.getScreensForRectangle(it.areaBy(1 x 1)).firstOrNull()
                  if (s==null) it else it max s.bounds.min min (s.bounds.max - size)
               }
               onContentShown()
            }
         }
      }
   }

   private fun WindowFx.initHideOnEscapeWhenNoFocus() {
      scene.onEventDown(KEY_PRESSED) {
         if (it.code==ESCAPE && isEscapeHide.value) {
            hide()
            it.consume()
         }
      } on tillHidden
   }

   /** Focuses this popup if it supports focus. */
   fun focus() {
//      stageOwner?.requestFocus()
//      window?.requestFocus()
//      window?.ifNotNull {
//         if (!it.isFocused) {
//            it.asIf<Stage>()?.toFront()
//            it.requestFocus()
//         }
//      }

      fun tryFocus() {
         if (window?.isFocused==false) {
            stageOwner?.requestFocus()
            window?.requestFocus()
         }
         root.asIs<Node>().recurse { if (it is Parent) it.childrenUnmodifiable else listOf() }
            .firstOrNull { it.isFocusTraversable }?.requestFocus()
      }
      tryFocus()
      runLater { tryFocus() }
   }

   /** Hides this popup. If is [animated], the hiding animation is started, otherwise happens immediately. */
   fun hide() {
      if (!isShowing) return
      if (animated.value) fadeOut()
      else hideImmediately()
   }

   /** Hides this popup immediately. Use when the hiding animation is not desired (regardless the [animated] value). */
   fun hideImmediately() {
      if (!isShowing) return
      tillHiding()
      window?.hide()
      window = null
      tillHidden()
   }

   private fun fadeIn() = animation.value.apply {
      applyNow()
      dur(animationDuration.value)
      delay(if (position.value==0.0) 200.millis else 0.0.millis) // give ui time to initialize to decreases animation stutter
      playOpenDo { }
   }

   private fun fadeOut() = animation.value.apply {
      applyNow()
      dur(animationDuration.value)
      delay(0.millis)
      playCloseDo { hideImmediately() }
      hideChildPopWindows()
   }

   private fun hideChildPopWindows() {
      WindowFx.getWindows().mapNotNull { it.asPopWindow()?.takeIf { it.window?.popWindowOwner==window } }.forEach { it.hide() }
   }

   companion object {

      fun popWindow(block: PopWindow.() -> Unit): PopWindow = PopWindow().apply(block)

      fun WindowFx.asPopWindow(): PopWindow? = uiDelegate.asIf()

      fun WindowFx.isPopWindow(): Boolean = asPopWindow()!=null

      fun Window.asPopWindow(): PopWindow? = stage.asPopWindow()

      fun Window.isPopWindow(): Boolean = stage.isPopWindow()

      fun WindowFx.isOpenChild(): Boolean = hasFileChooserOpen ||  Stage.getWindows().any { it!=this && it.isOverlayWindow() } || Stage.getWindows().any { this isParent it }

      fun WindowFx.isFocusedChild(): Boolean = hasFileChooserOpen || Stage.getWindows().find { it.isFocused }?.net { this isParent it }==true

      fun WindowFx.traverseOwners() = traverse { it.asPopWindow()?.window?.popWindowOwner ?: it.asIf<Stage>()?.owner }.drop(1)

      infix fun WindowFx.isChild(w: WindowFx) = traverseOwners().any { it===w }

      infix fun WindowFx.isParent(w: WindowFx) = w isChild this

      fun WindowFx.onIsShowing1(block: () -> Unit): Subscription = if (isShowing) { block(); Subscription() } else onEventDown1(WINDOW_SHOWN) { block() }

      private val UNFOCUSED_OWNER = lazy { APP.windowManager.createStageOwnerNoShow().apply { show() } }

   }

}