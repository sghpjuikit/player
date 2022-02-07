package sp.it.pl.access.fieldvalue

import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.type.VType
import sp.it.util.type.type

@Suppress("ClassName")
sealed class AnyField: ObjectFieldBase<Any?, String> {

   constructor(type: VType<String>, extractor: (Any?) -> String, name: String, description: String, toUi: (Any?, String) -> String): super(type, extractor, name, description, toUi)

   object STRING: AnyField(type(), { it.toS() }, "To String", "To String", { o, or -> o?.toS() ?: or })
   object STRING_UI: AnyField(type(), { it.toUi() }, "To String (ui)", "To String (ui)", { o, or -> o?.toUi() ?: or })

   companion object: ObjectFieldRegistry<Any, AnyField>(Any::class)

}