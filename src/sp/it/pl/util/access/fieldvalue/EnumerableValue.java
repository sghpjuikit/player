package sp.it.pl.util.access.fieldvalue;

import java.util.Collection;
import java.util.stream.Stream;

/** Value that is included in an enumeration that can list its possible values and which it is restricted to. */
public interface EnumerableValue<T> {

    /** @return all values in an enumeration */
    Collection<T> enumerateValues();

    default Stream<T> streamValues() {
        return enumerateValues().stream();
    }
}