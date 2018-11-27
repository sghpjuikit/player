package sp.it.pl.service.click

import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.animation.ScaleTransition
import javafx.application.Platform.runLater
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.scene.CacheHint
import javafx.scene.effect.BlendMode
import javafx.scene.effect.GaussianBlur
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import sp.it.pl.main.APP
import sp.it.pl.service.ServiceBase
import sp.it.pl.util.access.V
import sp.it.pl.util.access.initAttach
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.setAnchors
import sp.it.pl.util.math.millis
import java.util.ArrayList

class ClickEffect: ServiceBase("ClickEffect", false) {

    private val clickHandler = EventHandler<MouseEvent> { run(it.sceneX, it.sceneY) }
    private var screen: AnchorPane? = null  // TODO: improve by supporting multiple windows
    private var isRunning = false
    private val pool = ArrayList<Effect>()

    @IsConfig(name = "Show click effect", info = "Show effect on click.")
    val showClickEffect by cv(true) { V(it).initAttach { applyC() } }
    @IsConfig(name = "Click effect duration", info = "Duration of the click effect.")
    val duration by cv(350.millis) { V(it).initAttach { apply() } }
    @IsConfig(name = "Click effect min", info = "Starting scale value of cursor click effect animation.")
    val minScale by cv(0.2) { V(it).initAttach { apply() } }
    @IsConfig(name = "Click effect max", info = "Ending scale value of cursor click effect animation.")
    val maxScale by cv(0.7) { V(it).initAttach { apply() } }
    @IsConfig(name = "Click effect delay", info = "Delay of the click effect.")
    val delay by cv(0.millis) { V(it).initAttach { apply() } }
    @IsConfig(name = "Blend Mode", info = "Blending mode for the effect.")
    val blendMode by cv(BlendMode.SRC_OVER) { V(it).initAttach { apply() } }

    private fun applyC() {
        // TODO: improve by monitoring windows dynamically
        val ws = APP.windowManager.windows
        ws.forEach { it.stage.scene.root.removeEventFilter(MOUSE_PRESSED, clickHandler) }
        if (showClickEffect.get())
            ws.forEach { it.stage.scene.root.addEventFilter(MOUSE_PRESSED, clickHandler) }
    }

    private fun apply() = pool.forEach { it.apply() }

    fun create(): Effect {
        return if (pool.isEmpty())
            Effect()
        else {
            val c = pool[0]
            pool.removeAt(0)
            c
        }
    }

    /** Run at specific coordinates. The graphics of the effect is centered - [0,0] is at its center system. */
    fun run(x: Double, y: Double) {
        if (!isRunning) return  // create() must not execute when not running since screen==null
        create().play(x, y)
    }

    fun run(xy: Point2D) = run(xy.x, xy.y)

    override fun start() {
        isRunning = true

        val s = AnchorPane().apply {
            isMouseTransparent = true
            style = "-fx-background-color: null;"
            isPickOnBounds = false
        }
        screen = s
        runLater {
            val p = APP.windowManager.activeOrNew.stage.scene?.root as AnchorPane?
            if (p!=null) {
                p.children += s
                s.setAnchors(0.0)
            }
        }
    }

    override fun isRunning() = isRunning

    override fun stop() {
        isRunning = false

        runLater {
            val p = APP.windowManager.activeOrNew.stage.scene?.root as AnchorPane?
            if (p!=null) {
                p.children.remove(screen)
                screen = null
            }
        }
    }

    inner class Effect {
        private val root = Circle()
        private val fade = FadeTransition()
        private val scale = ScaleTransition()
        private val anim = ParallelTransition(root, fade, scale)
        private var scaleB = 1.0

        init {
            root.radius = 15.0
            root.fill = null
            root.effect = GaussianBlur(5.5)
            root.stroke = Color.AQUA
            root.strokeWidth = 4.5
            root.isVisible = false
            root.isCache = true
            root.cacheHint = CacheHint.SPEED
            root.isMouseTransparent = true
            anim.setOnFinished { pool += this }

            screen!!.children += root

            apply()
        }

        fun setScale(s: Double): Effect {
            scaleB = s
            return this
        }

        fun play(X: Double, Y: Double) {
            // center position on run
            root.layoutX = X
            root.layoutY = Y
            // run effect
            root.isVisible = true
            anim.play()
        }

        fun apply() {
            root.blendMode = blendMode.value
            anim.delay = delay.value

            fade.duration = duration.value
            fade.fromValue = 0.6
            fade.toValue = 0.0

            scale.duration = duration.value
            scale.fromX = scaleB*minScale.value
            scale.fromY = scaleB*minScale.value
            scale.toX = scaleB*maxScale.value
            scale.toY = scaleB*maxScale.value
        }
    }
}