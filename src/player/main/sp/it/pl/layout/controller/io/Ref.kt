package sp.it.pl.layout.controller.io


data class InputRef(val name: String, val input: Input<*>)

data class OutputRef(val name: String, val output: Output<*>)

fun Input<*>.toRef() = InputRef(if (this in IOLayer.allInputsApp) "App.$name" else name, this)

fun Output<*>.toRef() = OutputRef(if (this in IOLayer.allOutputsApp) "App.$name" else name, this)

fun InOutput<*>.toInputRef() = InputRef(if (this in IOLayer.allInoutputsApp) "App.${i.name}" else i.name, i)

fun InOutput<*>.toOutputRef() = OutputRef(if (this in IOLayer.allInoutputsApp) "App.${o.name}" else o.name, o)