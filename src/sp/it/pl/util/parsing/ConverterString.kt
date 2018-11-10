package sp.it.pl.util.parsing

/** Bidirectional String-Object converter. */
interface ConverterString<T>: ConverterToString<T>, ConverterFromString<T>