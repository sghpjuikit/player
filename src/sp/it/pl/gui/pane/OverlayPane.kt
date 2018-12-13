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
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import javafx.stage.Stage
import org.reactfx.Subscription
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.image.Thumbnail.FitFrom
import sp.it.pl.gui.pane.OverlayPane.Companion.globalDisplay
import sp.it.pl.gui.pane.OverlayPane.Companion.globalDisplayBgr
import sp.it.pl.main.APP
import sp.it.pl.main.resizeButton
import sp.it.pl.util.access.v
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.MultiConfigurableBase
import sp.it.pl.util.conf.cv
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.Util.createFMNTStage
import sp.it.pl.util.graphics.Util.layStack
import sp.it.pl.util.graphics.Util.screenCaptureAndDo
import sp.it.pl.util.graphics.Util.setAnchors
import sp.it.pl.util.graphics.applyViewPort
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.image.imgImplLoadFX
import sp.it.pl.util.graphics.minus
import sp.it.pl.util.graphics.screenToLocal
import sp.it.pl.util.graphics.size
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.math.P
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.Handler0
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.reactive.syncTo
import sp.it.pl.util.system.getWallpaperFile

/**
 * Pane laying 'above' standard content.
 *
 * Rather than using [StackPane.getChildren], use [content], which applies further decoration.
 * Content will align to center unless set otherwise.
 */
abstract class OverlayPane<in T>: StackPane() {

    /** Display method. */
    val display = v(Display.SCREEN_OF_MOUSE)
    /** Display bgr (for SCREEN variants only). */
    val displayBgr = v(ScreenImgGetter.SCREEN_BGR)
    /** Handlers called just after this pane was shown. */
    val onShown = Handler0()
    /** Handlers called just after this pane was hidden. */
    val onHidden = Handler0()

    private val resizeB: Icon
    private var resizing: Subscription? = null

    /** Graphical content or null if none. Setting the same content has no effect. */
    var content: Pane? = null
        set(c) {
            if (c===field) return
            resizing?.unsubscribe()

            if (c==null) {
                children.clear()
                field!!.styleClass -= CONTENT_STYLECLASS
            } else {
                children setTo listOf(c, layStack(resizeB, Pos.BOTTOM_RIGHT))
                resizeB.parent.isManaged = false
                resizeB.parent.isMouseTransparent = true
                c.styleClass += CONTENT_STYLECLASS
                c.paddingProperty() syncTo (resizeB.parent as StackPane).paddingProperty()
            }
            field = c
        }

    init {
        isVisible = false
        styleClass += ROOT_STYLECLASS

        setOnMouseClicked {
            if (it.button==SECONDARY && isShown()) {
                hide()
                it.consume()
            }
        }
        addEventHandler(KeyEvent.ANY) {
            // close on ESC press
            if (it.eventType==KeyEvent.KEY_PRESSED && it.code==ESCAPE && isShown()) hide()

            // prevent events from propagating, user should not be able to interact with UI below
            it.consume()
        }

        resizeB = resizeButton().apply {
            cursor = Cursor.SE_RESIZE
            isVisible = false
        }
    }

/* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

    private lateinit var displayUsedForShow: Display // prevents inconsistency in start() and stop(), see use
    private val animation by lazy { anim(APP.animationFps) { animDo(it) }.dur(200.millis).intpl { it*it } } // lowering fps can help on hd screens & low-end hardware
    private var stg: Stage? = null
    private val blurBack = BoxBlur(0.0, 0.0, 3)  // we need best possible quality
    private val blurFront = BoxBlur(0.0, 0.0, 1) // we do not need quality, hence iterations==1
    private var opacityNode: Node? = null
    private var blurFrontNode: Node? = null
    private var blurBackNode: Node? = null


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

    enum class Display {
        WINDOW, SCREEN_OF_WINDOW, SCREEN_OF_MOUSE, SCREEN_PRIMARY;

        private fun computeScreen(): Screen = when (this) {
            WINDOW -> fail()
            SCREEN_PRIMARY -> Screen.getPrimary()
            SCREEN_OF_WINDOW -> APP.windowManager.active.orNull()?.screen ?: SCREEN_OF_MOUSE.computeScreen()
            SCREEN_OF_MOUSE -> getScreenForMouse()
        }

        internal fun animStart(op: OverlayPane<*>) {
            if (this==WINDOW) {
                APP.windowManager.active.ifPresentOrElse(
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
                            op.blurBackNode = window.subroot
                            if (!op.children.isEmpty()) op.blurFrontNode = op.children[0]
                            op.blurBackNode!!.effect = op.blurBack
                            op.blurFrontNode!!.effect = op.blurFront

                            // start showing
                            op.animation.playOpenDo(null)
                            op.onShown()
                        },
                        {
                            op.displayUsedForShow = SCREEN_OF_MOUSE
                            SCREEN_OF_MOUSE.animStart(op)
                        }
                )
            } else {
                val screen = computeScreen()
                op.displayBgr.get().getImgAndDo(screen) { image ->
                    val bgr = Pane().apply {
                        styleClass += "bgr-image"   // replicate app window bgr for style & consistency
                    }
                    val contentImg = ImageView(image).apply {
                        fitWidth = screen.bounds.width
                        fitHeight = screen.bounds.height
                        applyViewPort(image, FitFrom.OUTSIDE)
                    }
                    val root = stackPane(stackPane(bgr, contentImg))

                    op.stg = createFMNTStage(screen, false).apply {
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
                    op.stg!!.show()
                    op.stg!!.requestFocus()

                    // start showing
                    // the preparation may cause an animation lag, hence delay a bit
                    runFX(30.millis) {
                        op.animation.playOpenDo(null)
                        op.onShown()
                    }
                }
            }
        }

        internal fun animDo(op: OverlayPane<*>, x: Double) = op.apply {
            if (opacityNode!=null) { // bug fix, not 100% sure why it is necessary
                if (this@Display!=WINDOW && (op.displayBgr.get()==ScreenImgGetter.SCREEN_BGR || op.displayBgr.get()==ScreenImgGetter.NONE)) {
                    stg!!.opacity = x
                    opacityNode!!.opacity = 1-x*0.5
                    opacity = 1.0
                    blurBack.height = 15.0*x*x
                    blurBack.width = 15.0*x*x
                    blurFront.height = 20*(1-x*x)
                    blurFront.width = 20*(1-x*x)
                    scaleX = 1+0.2*(1-x)
                    scaleY = 1+0.2*(1-x)
                } else {
                    opacityNode!!.opacity = 1-x*0.5
                    opacity = x
                    blurBack.height = 15.0*x*x
                    blurBack.width = 15.0*x*x
                    blurFront.height = 20*(1-x*x)
                    blurFront.width = 20*(1-x*x)
                    scaleX = 1+2*(1-x)
                    scaleY = 1+2*(1-x)
                }
            }
        }

        internal fun animEnd(op: OverlayPane<*>) = op.apply {
            opacityNode!!.effect = null
            blurFrontNode!!.effect = null
            blurBackNode!!.effect = null
            opacityNode = null
            blurFrontNode = null
            blurBackNode = null
            onHidden()
            if (this@Display==WINDOW) {
                setVisible(false)
            } else {
                stg!!.close()
            }
        }
    }

    companion object: MultiConfigurableBase("View") {
        private const val IS_SHOWN = "visible"
        private const val ROOT_STYLECLASS = "overlay-pane"
        private const val CONTENT_STYLECLASS = "overlay-pane-content"

        @IsConfig(name = "Display method", group = "View", info = "Area of content. Screen provides more space than window, but can get in the way of other apps.")
        val globalDisplay by cv(Display.SCREEN_OF_MOUSE)
        @IsConfig(name = "Display background", group = "View", info = "Content background")
        val globalDisplayBgr by cv(ScreenImgGetter.SCREEN_BGR)
    }
}

fun <T, P: OverlayPane<T>> P.initApp() = apply {
    display syncFrom globalDisplay
    displayBgr syncFrom globalDisplayBgr
}

enum class ScreenImgGetter {
    NONE, SCREEN_SHOT, SCREEN_BGR;

    fun getImgAndDo(screen: Screen, action: (Image?) -> Unit) {
        when (this) {
            NONE -> action(null)
            SCREEN_SHOT -> screenCaptureAndDo(screen) { action(it) }
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
        if (resizable==null) return Subscription.EMPTY

        resizable.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE)

        return Subscription.multi(
                resizable.onEventDown(MOUSE_PRESSED) {
                    if (dragActivator!=null) {
                        // drag by a resizable Node
                        if (dragActivator.contains(dragActivator.screenToLocal(it))) {
                            isActive = true
                            offset = resizable.size-resizable.screenToLocal(it)
                        }
                    } else {
                        // drag by corner
                        val cornerSize = 30.0
                        val n = it.source as Pane
                        if (it.x>=n.width-cornerSize && it.y>=n.height-cornerSize) {
                            isActive = true
                            offset = resizable.size-resizable.screenToLocal(it)
                        }
                    }
                },
                eventEmitter.onEventDown(MOUSE_RELEASED) { isActive = false },
                eventEmitter.onEventDown(MOUSE_DRAGGED) {
                    if (isActive) {
                        val n = it.source as Pane
                        resizable.setPrefSize(
                                2*(it.x+offset.x-n.layoutBounds.width/2),
                                2*(it.y+offset.y-n.layoutBounds.height/2)
                        )
                        it.consume()
                    }
                }
        )
    }
}