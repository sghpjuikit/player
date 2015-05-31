/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.Widgets.Features;

import Configuration.Config;
import Configuration.Configurable;
import java.util.Collection;

/**
 * Editor for {@link Configurable}.
 * 
 * @author Plutonium_
 */
@Feature(
  name = "Configurator",
  description = "Provides settings and configurations",
  type = ConfiguringFeature.class
)
public interface ConfiguringFeature {
    
    /** Displays configs of the specified configurable object for user to edit. */
    default void configure(Configurable c) {
        configure(c.getFields());
    };
    
    /** Displays specified configs for user to edit. */
    void configure(Collection<Config> c);
}
