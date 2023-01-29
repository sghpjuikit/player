package sp.it.pl.ui.objects.textfield

import sp.it.pl.main.APP
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.util.parsing.ConverterString
import sp.it.util.type.VType

abstract class ValueTextFieldBi<T>(
   initialValue: T? = null,
   type: VType<T>,
   val valueConverter: ConverterString<T?> = APP.converter.general.toConverterOf(type).nullable(textNoVal)
): ValueTextField<T>(
   initialValue, valueConverter::toS
)