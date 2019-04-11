package sp.it.pl.layout.area

import sp.it.pl.layout.widget.controller.io.Put
import sp.it.pl.main.APP
import sp.it.util.functional.asIf
import sp.it.util.type.Util.getRawType
import sp.it.util.type.isSuperclassOf
import java.lang.reflect.ParameterizedType

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