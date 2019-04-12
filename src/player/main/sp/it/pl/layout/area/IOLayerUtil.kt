package sp.it.pl.layout.area

import javafx.scene.shape.Circle
import javafx.scene.shape.Path
import sp.it.pl.layout.widget.controller.io.Put
import sp.it.pl.main.APP
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.functional.asIf
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.type.Util.getRawType
import sp.it.util.type.isSuperclassOf
import sp.it.util.ui.setScaleXY
import sp.it.util.units.millis
import java.lang.reflect.ParameterizedType
import kotlin.math.sqrt

fun <T> Put<T>.xPutToStr(): String = "${typeAsStr()} : $name\n${APP.instanceName[value]}"

private fun <T> Put<T>.typeAsStr(): String {
    val t = typeRaw
    return if (t!=null) {
        val c = getRawType(t)
        when {
            Collection::class.isSuperclassOf(c) -> "List of " + APP.className[getRawType(t.asIf<ParameterizedType>()!!.actualTypeArguments[0])]
            else -> APP.className[c]
        }
    } else {
        APP.className[type]
    }
}

fun Path.duplicateTo(path: Path) {
    elements.onItemAdded { path.elements += it }
    elements.onItemRemoved { path.elements.clear() }
}

fun IOLayer.dataArrived(x: Double, y: Double) {
    children += Circle(5.0).apply {
        styleClass += "ioline-effect-receive"
        isManaged = false
        relocate(x-radius, y-radius)

        anim(300.millis) {
            setScaleXY(4*sqrt(it))
            opacity = 1-it*it
        }.then {
            children -= this
        }.play()
    }
}