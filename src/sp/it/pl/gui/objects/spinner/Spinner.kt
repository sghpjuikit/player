package sp.it.pl.gui.objects.spinner

import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.shape.Arc
import javafx.scene.shape.StrokeType
import javafx.util.Duration
import javafx.util.Duration.ZERO

/** Very simple custom [ProgressIndicator]. */
class Spinner: ProgressIndicator {

    @JvmOverloads constructor(progress: Double = 1.0): super(progress)

    override fun createDefaultSkin() = SpinnerSkin(this)

    class SpinnerSkin: SkinBase<Spinner> {
        private val inner: StackPane
        private val outer: StackPane
        private var rt: RotateTransition? = null
        private var playing = false

        constructor(spinner: Spinner): super(spinner) {
            val arcInner = Arc().apply {
                length = 270.0
                radiusX = 6.0
                radiusY = 6.0
                startAngle = 180.0
                strokeType = StrokeType.INSIDE
                strokeWidth = 2.5
                styleClass += "spinner"
                StackPane.setAlignment(this, Pos.BOTTOM_RIGHT)
            }
            val arcOuter = Arc().apply {
                length = 270.0
                radiusX = 9.0
                radiusY = 9.0
                strokeType = StrokeType.INSIDE
                strokeWidth = 2.5
                styleClass += "spinner"
                StackPane.setAlignment(this, Pos.TOP_LEFT)
            }
            inner = StackPane(arcInner).apply {
                setPrefSize(10.0, 10.0)
                setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)
            }
            outer = StackPane(arcOuter).apply {
                setPrefSize(15.0, 15.0)
                setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)
            }

            outer.rotateProperty().bind(Bindings.subtract(360, inner.rotateProperty()))
            children += StackPane(inner, outer)

            registerChangeListener(spinner.indeterminateProperty()) { update() }
            registerChangeListener(spinner.progressProperty()) { update() }
            registerChangeListener(spinner.visibleProperty()) { update() }
            registerChangeListener(spinner.parentProperty()) { update() }
            registerChangeListener(spinner.sceneProperty()) { update() }

            update()
        }

        override fun dispose() {
            rt?.stop()
            outer.rotateProperty().unbind()
            super.dispose()
        }

        private fun update() {
            if (skinnable.progress!=1.0 && skinnable.parent!=null && skinnable.scene!=null && skinnable.isVisible) {
                if (rt==null) {
                    rt = RotateTransition(Duration.seconds(120.0), inner).apply {
                        interpolator = Interpolator.LINEAR
                        cycleCount = Transition.INDEFINITE
                        delay = ZERO
                        byAngle = (360*100).toDouble()
                    }
                }
                if (!playing) rt!!.play()
                playing = true
            } else {
                if (playing && rt!=null) rt!!.pause()
                playing = false
            }
        }
    }
}