/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services;

import java.util.HashMap;
import java.util.Map;

import Configuration.CachedConfigurable;
import Configuration.Config;
import Configuration.Configurable;
import Configuration.IsConfig;
import util.access.Ѵ;

/**
 *
 * @author Plutonium_
 */
public interface Service extends Configurable {
    void start();
    boolean isRunning();
    void stop();
    default boolean isSupported() { return true; };
    default boolean isDependency() { return false; };
    
    public static abstract class ServiceBase implements Service, CachedConfigurable {
        
        private final HashMap<String,Config<Object>> configs = new HashMap<>();
        
        @IsConfig(name = "Enabled", info = "Starts or stops the service")
        private final Ѵ<Boolean> enabled;
        
        public ServiceBase(boolean isEnabled) {
             enabled = new Ѵ<>(isEnabled, this::enable);
        }
        
        private void enable(boolean isToBeRunning) {
            boolean isRunning = isRunning();
            if(isRunning==isToBeRunning) return;
            
            if(isToBeRunning && !isDependency() && isSupported()) start();
            if(!isToBeRunning) stop();
        }
        
        @Override
        public Map<String, Config<Object>> getFieldsMap() {
            return configs;
        }
    }
    
}
