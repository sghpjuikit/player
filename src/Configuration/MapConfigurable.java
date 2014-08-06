
package Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection implementation of {@link Configurable}. Aggregates {@link Config}
 * by name as a map with config name as a key.
 * <p>
 * This implementation provides O(1) field access.
 * <p>
 * Use to access configs by name.
 * 
 * @author Plutonium_
 */
public class MapConfigurable implements Configurable {
    
    Map<String,Config> configs;
    
    public MapConfigurable(Config... configs) {
        this.configs = new HashMap();
        for(Config c : configs) this.configs.put(c.getName(),c);
    }
    
    public MapConfigurable(List<Config> configs) {
        this.configs = new HashMap();
        for(Config c : configs) this.configs.put(c.getName(),c);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Implementation details:
     * <p>
     * The configs do not retain their position.
     * 
     * @return 
     */
    @Override
    public List<Config> getFields() {
        return new ArrayList(configs.values());
    }

    /** 
     * {@inheritDoc} 
     * <p>
     * Implementation details:
     * Runs in O(1).
     */
    @Override
    public Config getField(String name) {
        return configs.get(name);
    }
}
