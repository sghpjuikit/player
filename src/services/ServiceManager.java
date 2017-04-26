package services;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.reactfx.Subscription;
import util.collections.map.ClassMap;

public class ServiceManager {

	private final ClassMap<Service> services = new ClassMap<>();
	private final Map<Service,Set<Subscription>> subscribers = new ConcurrentHashMap<>();

	public void addService(Service s) {
		Class<? extends Service> type = s.getClass();
		if (services.containsKey(type)) throw new IllegalStateException("There already is a service of this type");
		services.put(type, s);
		subscribers.computeIfAbsent(s, service -> new HashSet<>());
	}

	@SuppressWarnings("unchecked")
	public <S extends Service> Optional<S> getService(Class<S> type) {
		return Optional.ofNullable((S) services.get(type));
	}

	public Stream<Service> getAllServices() {
		return services.values().stream();
	}

	public void forEach(Consumer<Service> action) {
		services.values().forEach(action);
	}

	@SuppressWarnings("unchecked")
	public <S extends Service> Subscription acquire(Class<S> type) {
		S service = (S) services.computeIfAbsent(type, s -> (Service) instantiate(s));
		if (!service.isRunning()) service.start();
		Set<Subscription> ss = subscribers.computeIfAbsent(service, s -> new HashSet<>());
		Subscription s = new Subscription() {
			@Override
			public void unsubscribe() {
				release(type, this);
			}
		};
		ss.add(s);
		return s;
	}

	// Normally we would allow subscriber to be Object (why restrict if not necessary) and make
	// this public API, but that would lead to memory leaks due to holding onto object's reference.
	// Making subscriber a Subscription (new object with no reference of the original subscriber)
	// makes sure the subscriber can be garbage collected anytime (without releasing the service).
	@SuppressWarnings("unchecked")
	private <S extends Service> void release(Class<S> type, Subscription subscriber) {
		S service = (S) services.get(type);                     // get service single instance
		Optional.ofNullable(service)                            // or return if none
				.map(subscribers::get)                          // get all subscribers
				.ifPresent(ss -> {
					ss.remove(subscriber);                      // remove subscriber
					if (ss.isEmpty() && service.isRunning())
						service.stop();
				});
	}

	private <S> S instantiate(Class<S> type) {
		try {
			return type.getConstructor().newInstance();
		} catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
			throw new RuntimeException("Could not instantiate service " + type, e);
		}
	}
}