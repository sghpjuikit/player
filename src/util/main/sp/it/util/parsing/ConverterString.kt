package sp.it.util.parsing

/** Bidirectional String-Object converter. */
interface ConverterString<T>: ConverterToString<T>, ConverterFromString<T>