package util.parsing;

/**
 * Bidirectional String-Object converter.
 *
 * @param <T> type of object
 */
public interface StringConverter<T> extends ToStringConverter<T>, FromStringConverter<T> {}