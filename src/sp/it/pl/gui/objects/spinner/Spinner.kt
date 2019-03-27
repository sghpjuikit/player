package sp.it.pl.gui.objects.spinner

import javafx.animation.Interpolator
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.geometry.Pos
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.shape.Arc
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.ui.lay
import sp.it.pl.util.ui.maxSize
import sp.it.pl.util.ui.stackPane
import sp.it.pl.util.ui.x
import sp.it.pl.util.units.millis
import sp.it.pl.util.units.seconds

/** Very simple custom [ProgressIndicator]. */
class Spinner: ProgressIndicator {

    constructor(progress: Double = 1.0): super(progress)

    override fun createDefaultSkin() = SpinnerSkin(this)

    class SpinnerSkin(spinner: Spinner): SkinBase<Spinner>(spinner) {
        private val inner: StackPane
        private val outer: StackPane
        private var rt: RotateTransition? = null
        private var playing = false
        private val onDispose = Disposer()

        init {
            inner = stackPane {
                lay(BOTTOM_RIGHT) += Arc().apply {
                    length = 270.0
                    startAngle = 180.0
                    this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble() } on onDispose
                    this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble() } on onDispose
                    styleClass += "spinner"
                    styleClass += "spinner-in"
                }
                styleClass += "spinner-in"
                maxSize = USE_PREF_SIZE x USE_PREF_SIZE
            }
            outer = stackPane {
                lay(Pos.TOP_LEFT) += Arc().apply {
                    length = 270.0
                    this@stackPane.prefWidthProperty() sync { radiusX = it.toDouble() } on onDispose
                    this@stackPane.prefWidthProperty() sync { radiusY = it.toDouble() } on onDispose
                    styleClass += "spinner"
                    styleClass += "spinner-out"
                }
                styleClass += "spinner-out"
                maxSize = USE_PREF_SIZE x USE_PREF_SIZE
            }

            inner.rotateProperty() sync { outer.rotate = 360.0-it.toDouble() } on onDispose
            children += stackPane(inner, outer)

            spinner.indeterminateProperty() attach { update() } on onDispose
            spinner.progressProperty() attach { update() } on onDispose
            spinner.visibleProperty() attach { update() } on onDispose
            spinner.parentProperty() attach { update() } on onDispose
            spinner.sceneProperty() attach { update() } on onDispose

            update()
        }

        override fun dispose() {
            rt?.stop()
            onDispose()
            super.dispose()
        }

        private fun update() {
            if (skinnable.progress!=1.0 && skinnable.parent!=null && skinnable.scene!=null && skinnable.isVisible) {
                rt = rt ?: RotateTransition(120.seconds, inner).apply {
                    interpolator = Interpolator.LINEAR
                    cycleCount = Transition.INDEFINITE
                    delay = 0.millis
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