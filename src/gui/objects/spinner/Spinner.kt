package gui.objects.spinner

import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.beans.binding.Bindings
import javafx.fxml.FXML
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Skin
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.shape.Arc
import javafx.util.Duration
import javafx.util.Duration.ZERO
import org.reactfx.Subscription
import util.graphics.fxml.ConventionFxmlLoader
import util.reactive.maintain
import util.reactive.unsubscribe

/**
 * Very simple custom [ProgressIndicator].
 */
@Suppress("unused")
class Spinner: ProgressIndicator {

    private var hidingOnIdle: Subscription? = null

    @JvmOverloads
    constructor(progress: Double = 1.0): super() {
        skin = createDefaultSkin()
        setProgress(progress)
    }

    fun hidingOnIdle(v: Boolean): Spinner {
        if (v) {
            if (hidingOnIdle == null)
                hidingOnIdle = progressProperty().maintain({ it.toDouble() != 1.0 && it.toDouble() != 0.0 }, visibleProperty())
        } else {
            hidingOnIdle = unsubscribe(hidingOnIdle)
        }
        return this
    }

    fun isHidingOnIdle() = hidingOnIdle != null

    override fun createDefaultSkin(): Skin<*> {
        return SpinnerSkin(this)
    }

    private class SpinnerSkin(spinner: Spinner) : SkinBase<Spinner>(spinner) {

        private var root = StackPane()
        @FXML private lateinit var inner: StackPane
        @FXML private lateinit var outer: StackPane
        @FXML private lateinit var inner_arc: Arc
        @FXML private lateinit var outer_arc: Arc
        private var rt: RotateTransition? = null
        private var playing = false

        init {
            // load fxml part
            ConventionFxmlLoader(Spinner::class.java, root, this).loadNoEx<Any>()

            // register listeners
            registerChangeListener(spinner.indeterminateProperty()) { update() }
            registerChangeListener(spinner.progressProperty()) { update() }
            registerChangeListener(spinner.visibleProperty()) { update() }
            registerChangeListener(spinner.parentProperty()) { update() }
            registerChangeListener(spinner.sceneProperty()) { update() }

            outer.rotateProperty().bind(Bindings.subtract(360, inner.rotateProperty()))
            children += root
        }

        override fun dispose() {
            rt?.stop()
            outer.rotateProperty().unbind()
        }

        private fun update() {
            if (skinnable.progress != 1.0 && skinnable.parent != null && skinnable.scene != null && skinnable.isVisible) {
                if (rt == null) {
                    rt = RotateTransition(Duration.seconds(120.0), inner).apply {
                        interpolator = Interpolator.LINEAR
                        cycleCount = Transition.INDEFINITE
                        delay = ZERO
                        byAngle = (360 * 100).toDouble()
                    }
                }
                if (!playing) rt!!.play()
                playing = true
            } else {
                if (playing && rt != null) rt!!.pause()
                playing = false
            }
        }
    }
}