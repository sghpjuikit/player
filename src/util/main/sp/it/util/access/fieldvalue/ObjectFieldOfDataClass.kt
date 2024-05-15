package sp.it.util.access.fieldvalue

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible
import sp.it.util.functional.net
import sp.it.util.text.capital
import sp.it.util.type.VType

class ObjectFieldOfDataClass<T, R>(val p: KProperty1<T, R>, toUi: (R) -> String): ObjectFieldBase<T, R>(
   type = VType(p.returnType),
   extractor = { p.isAccessible=true; p.getter.call(it) },
   name = p.name.capital(),
   description = "",
   toUi = { v, or -> v?.net(toUi) ?: or }
)