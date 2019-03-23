package sp.it.pl.service

import sp.it.pl.main.APP
import sp.it.pl.util.collections.map.ClassMap
import sp.it.pl.util.reactive.Subscription
import java.lang.reflect.InvocationTargetException
import java.util.HashSet
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.reflect.KClass

class ServiceManager {

    private val services = ClassMap<Service>()
    private val subscribers = ConcurrentHashMap<Service, MutableSet<Subscription>>()

    fun addService(s: Service) {
        val type = s.javaClass
        if (services.containsKey(type)) throw IllegalStateException("There already is a service of this type")
        services[type] = s
        subscribers.computeIfAbsent(s) { HashSet() }
    }

    operator fun plusAssign(s: Service) = addService(s)

    @Suppress("UNCHECKED_CAST")
    fun <S: Service> getService(type: Class<S>): Optional<S> = Optional.ofNullable(services[type] as S)

    fun getAllServices(): Sequence<Service> = services.values.asSequence()

    inline fun <reified S: Service> use(noinline action: (S) -> Unit) = use(S::class.java, Consumer(action))

    fun <S: Service> use(type: KClass<S>, action: (S) -> Unit) = use(type.java, Consumer(action))

    fun <S: Service> use(type: Class<S>, action: Consumer<in S>) =
            getService(type).filter { it.isRunning() }.ifPresent(action)

    @Suppress("UNCHECKED_CAST", "unused")
    fun <S: Service> acquire(type: Class<S>): Subscription {
        val service = services.computeIfAbsent(type) { instantiate(it as Class<S>) } as S
        if (!service.isRunning()) service.start()
        val s = object: Subscription {
            override fun unsubscribe() {
                release(type, this)
            }
        }
        subscribers.computeIfAbsent(service) { HashSet() } += (s)
        return s
    }

    // Normally we would allow subscriber to be Object (why restrict if not necessary) and make
    // this public API, but that would lead to memory leaks due to holding onto object's reference.
    // Making subscriber a Subscription (new object with no reference of the original subscriber)
    // makes sure the subscriber can be garbage collected anytime (without releasing the service).
    private fun <S: Service> release(type: Class<S>, subscriber: Subscription) {
        services[type]?.let { s ->
            subscribers[s]?.let { subscribers ->
                subscribers -= subscriber
                if (subscribers.isEmpty() && s.isRunning())
                    s.stop()
            }
        }
    }

    private fun <S> instantiate(type: Class<S>): S {
        try {
            return type.getConstructor().newInstance()
        } catch (e: InstantiationException) {
            throw RuntimeException("Could not instantiate service $type", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Could not instantiate service $type", e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Could not instantiate service $type", e)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Could not instantiate service $type", e)
        }
    }
}


fun Service.runWhenReady(block: () -> Unit) {
    if (APP.isInitialized.isOk) {
        block()
    } else {
        APP.onStarted += { runWhenReady(block) }
    }
}