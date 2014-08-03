/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Non abstract implementation of {@link Configurable}. Aggregates {@link Config}
 * and virtualizes them as semantically connected Configs of some
 * object that exists only virtually.
 * <p>
 * Normally a Configurable exports object's properties and attributes by introspection
 * and reflection. Because Configurable provides standard to accessing and configuring
 * itself (more precisely the implementing object which gains the behavior from
 * interface) from outside transparently, sometimes it is desirable to wrap an object
 * into a Config and construct a Configurable around it. This Configurable could
 * aggregate multiple unrelated values and virtually pass them off as a configurable
 * portion of some object which does not exist.
 * <p>
 * Notice that this class puts no restriction to implementation of the Config it
 * can aggregate. Any Config can be aggregated as they are all equal because they
 * take care of the setting and applying the values themselves.
 * However the most frequent use of this class probably involves {@link ValueConfig}.
 * <p>
 * An expected example for the mentioned use case is to pass values into a method
 * or object that takes Configurable as a parameter. This object usually operates
 * on the configs in order to change state of the configured object (represented
 * by the totality of the configs). The modification could be done by the user through GUI.
 * <p>
 * Basically, this class can be used to pass arbitrary objects or any subset of
 * other Configurable somewhere to modify it and then provide the result.
 * 
 * @author Plutonium_
 */
public class ValueConfigurable implements Configurable {
    
    List<Config> configs;
    
    public ValueConfigurable(Config... configs) {
        this.configs = new ArrayList();
        for(Config c : configs) this.configs.add(c);
    }
    
    public ValueConfigurable(List<Config> configs) {
        this.configs = new ArrayList();
        for(Config c : configs) this.configs.add(c);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Use to get Configs and access their values. The configs retain their
     * position - are in the order in which they were added. Because there is
     * no knowing of the type of the Config and its generic type - the type
     * of object it stores, casting is necessary.
     * <p>
     * There are two possible ways:<br>
     * <t>Casting to Config with the correct generic parameter and then calling
     * the getValue() like this:
     * <p>
     * String val = ((Config<String>) c.getFields().get(0)).getValue();
     * <p>
     *     Or obtaining the value and then casting it to the correct type. This 
     * should be the preferred way of doing this. Like this:
     * <p>
     * String val = (String) c.getFields().get(0).getValue();
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
     * @param at
     * @return 
     */
    public Config getField(int at) {
        return configs.get(at);
    }
    
    
    
}
