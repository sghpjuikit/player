package sp.it.pl.ui.objects.complexfield

import sp.it.pl.main.toUi
import sp.it.util.functional.Try
import sp.it.util.parsing.ConverterFromString
import sp.it.util.parsing.ConverterToString

/** [TagTextField] for [String] with reasonable defaults for converters. */
open class StringTagTextField(
   converter: ConverterFromString<String> = ConverterFromString { if (it.isBlank()) Try.error("Must not be blank") else Try.ok(it) },
   converterToUi: ConverterToString<String> = ConverterToString { it.toUi() }
): TagTextField<String>(
   converter,
   converterToUi,
)