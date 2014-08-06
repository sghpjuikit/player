
package Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection implementation of {@link Configurable}. Aggregates {@link Config}
 * by index as a list retaining the order in which the configs were inserted.
 * <p>
 * This implementation provides O(n) field access.
 * <p>
 * Use to access configs by index.
 * 
 * @author Plutonium_
 */
public class ListConfigurable implements Configurable {
    
    List<Config> configs;
    
    public ListConfigurable(Config... configs) {
        this.configs = new ArrayList();
        for(Config c : configs) this.configs.add(c);
    }
    
    public ListConfigurable(List<Config> configs) {
        this.configs = new ArrayList();
        for(Config c : configs) this.configs.add(c);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementation details:
     * <p>
     * The configs retain their position - are in the order in which they were 
     * added, thus allowing for safe casting, since we know the order.
     * 
     * @return 
     */
    @Override
    public List<Config> getFields() {
        return configs;
    }
    
    /**
     * Convenience method. Returns config at specific index within the resulting
     * list of {@link #getFields() } method.
     * <p>
     * Runs in O(1).
     * @param at
     * @return 
     */
    public Config getField(int at) {
        return configs.get(at);
    }
    
    /**
     * Adds config at specified position.
     * @param at
     * @param config 
     */
    public void addField(int at, Config config) {
        configs.add(at, config);
    }

    /** 
     * {@inheritDoc} 
     * <p>
     * Implementation details:
     * Runs in O(n).
     */
    @Override
    public Config getField(String name) {
        return configs.stream().filter(c -> name.equals(c.getName())).findAny().get();
    }
}
