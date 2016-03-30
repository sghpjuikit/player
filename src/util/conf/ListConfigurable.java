
package util.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static util.functional.Util.list;

/**
 * Collection implementation of {@link Configurable}. Aggregates {@link Config}
 * by index as a list retaining the order in which the configs were inserted.
 * <p/>
 * This implementation provides O(n) field access.
 * <p/>
 * Use to access configs by index.
 *
 * @author Martin Polakovic
 */
public class ListConfigurable<T> implements Configurable<T> {

    private final List<Config<T>> cs;

    public ListConfigurable(Config<T>... configs) {
        cs = new ArrayList<>();
        for(Config<T> c : configs) this.cs.add(c);
    }

    public ListConfigurable(Collection<Config<T>> configs) {
        cs = list(configs);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Implementation details:
     * <p/>
     * The configs retain their position - are in the order in which they were
     * added, thus allowing for safe casting, since we know the order.
     *
     * @return
     */
    @Override
    public List<Config<T>> getFields() {
        return cs;
    }

    /**
     * Convenience method. Returns config at specific index within the resulting
     * list of {@link #getFields() } method.
     * <p/>
     * Runs in O(1).
     * @param at
     * @return
     */
    public Config<T> getField(int at) {
        return cs.get(at);
    }

    /**
     * Adds config at specified position.
     * @param at
     * @param config
     */
    public void addField(int at, Config<T> config) {
        cs.add(at, config);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Implementation details:
     * Runs in O(n).
     */
    @Override
    public Config<T> getField(String name) {
        return cs.stream().filter(c -> name.equals(c.getName())).findAny().get();
    }
}
