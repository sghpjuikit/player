package sp.it.pl.util.parsing;

/** Bidirectional String-Object converter for concrete type. */
public interface ConverterString<T> extends ConverterToString<T>, ConverterFromString<T> {}