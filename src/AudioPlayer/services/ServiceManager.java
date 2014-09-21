/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Plutonium_
 */
public class ServiceManager {
    
    private final Map<Class<? extends Service>,Service> services = new HashMap();
    
    public void addService(Service s) {
        services.put(s.getClass(), s);
    }
    
    public Optional<Service> getService(Class<? extends Service> type) {
        return Optional.ofNullable(services.get(type));
    }
    
    public Stream<Service> getAllServices() {
        return services.values().stream();
    }
    
    public void forEach(Consumer<Service> action) {
        services.values().forEach(action);
    }
}
