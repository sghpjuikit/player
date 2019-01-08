package sp.it.pl.gui.objects.spinner

import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.shape.Arc
import javafx.util.Duration
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.math.seconds
import sp.it.pl.util.reactive.sync

/** Very simple custom [ProgressIndicator]. */
class Spinner: ProgressIndicator {

    constructor(progress: Double = 1.0): super(progress)

    override fun createDefaultSkin() = SpinnerSkin(this)

    class SpinnerSkin(spinner: Spinner): SkinBase<Spinner>(spinner) {
        private val inner: StackPane
        private val outer: StackPane
        private var rt: RotateTransition? = null
        private var playing = false

        init {
            inner = stackPane {
                lay(Pos.BOTTOM_RIGHT) += Arc().apply {
                    length = 270.0
                    startAngle = 180.0
                    this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble() }
                    this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble() }
                    styleClass += "spinner"
                    styleClass += "spinner-in"
                }
                styleClass += "spinner-in"
                setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)
            }
            outer = stackPane {
                lay(Pos.TOP_LEFT) += Arc().apply {
                    length = 270.0
                    this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble() }
                    this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble() }
                    styleClass += "spinner"
                    styleClass += "spinner-out"
                }
                styleClass += "spinner-out"
                setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)
            }

            inner.rotateProperty() sync { outer.rotate = 360.0-it.toDouble() }
            children += stackPane(inner, outer)

            registerChangeListener(spinner.indeterminateProperty()) { update() }
            registerChangeListener(spinner.progressProperty()) { update() }
            registerChangeListener(spinner.visibleProperty()) { update() }
            registerChangeListener(spinner.parentProperty()) { update() }
            registerChangeListener(spinner.sceneProperty()) { update() }

            update()
        }

        override fun dispose() {
            rt?.stop()
            super.dispose()
        }

        private fun update() {
            if (skinnable.progress!=1.0 && skinnable.parent!=null && skinnable.scene!=null && skinnable.isVisible) {
                rt = rt ?: RotateTransition(120.seconds, inner).apply {
                    interpolator = Interpolator.LINEAR
                    cycleCount = Transition.INDEFINITE
                    delay = Duration.ZERO
                    byAngle = 360*100.0
                }
                if (!playing) rt?.play()
                playing = true
            } else {
                if (playing) rt?.pause()
                playing = false
            }
        }
    }

}