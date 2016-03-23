/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import util.collections.map.ClassMap;

/**
 *
 * @author Plutonium_
 */
public class ServiceManager {

    private final ClassMap<Service> services = new ClassMap<>();

    public void addService(Service s) {
        Class<?> c = s.getClass();
        if(services.containsKey(c)) throw new IllegalStateException("There already is a service of this type");
        services.put(c, s);
    }

    public<S extends Service> Optional<S> getService(Class<S> type) {
        return Optional.ofNullable((S) services.get(type));
    }

    public Stream<Service> getAllServices() {
        return services.values().stream();
    }

    public void forEach(Consumer<Service> action) {
        services.values().forEach(action);
    }
}
