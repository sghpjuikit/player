/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains set of utility methods for development. For example leading to
 * easier bug discovery or stronger runtime checking.
 *
 * @author Martin Polakovic
 */
public interface Util {

    static void no(boolean v) {
        if (v) throw new IllegalStateException("Requirement condition not met");
    }

    static void no(boolean v, String s) {
        if (v) throw new IllegalStateException("Requirement condition not met: " + s);
    }

    @SafeVarargs
	static <T> void no(Predicate<? super T> condition,  T... os) {
		for (T o : os) if (condition.test(o)) throw new IllegalStateException("Forbidden constraint");
	}

    static void yes(boolean v) {
        if (!v) throw new IllegalStateException("Requirement condition not met");
    }

    static void yes(boolean v, String s) {
        if (!v) throw new IllegalStateException("Requirement condition not met: " + s);
    }

    @SafeVarargs
	static <T> void yes(Predicate<? super T> condition,  T... os) {
		for (T o : os) if (!condition.test(o)) throw new IllegalStateException("Forbidden constraint");
	}

    static <T> T noØ(T o) {
        if (o==null) throw new IllegalStateException("Null forbidden");
	    return o;
    }

    static <T> T noØ(T o, String message) {
        if (o==null) throw new IllegalStateException("Null forbidden: " + message);
	    return o;
    }

    static void noØ(Object o1, Object o2) {
        if (o1==null || o2==null) throw new IllegalStateException("Null forbidden");
    }

    static void noØ(Object o1, Object o2, Object o3) {
        if (o1==null || o2==null || o3==null) throw new IllegalStateException("Null forbidden");
    }

    static void noØ(Object o1, Object o2, Object o3, Object o4) {
        if (o1==null || o2==null || o3==null || o4==null) throw new IllegalStateException("Null forbidden");
    }

    static void noØ(Object... os) {
        for (Object o : os) if (o==null) throw new IllegalStateException("Null forbidden");
    }

    static void noFinal(Field f) {
        if (Modifier.isFinal(f.getModifiers()))
    }

    static void yesFinal(Field f) {
        if (!Modifier.isFinal(f.getModifiers()))
    }


    static void measureTime(Runnable r) {
        long t = System.currentTimeMillis();
        r.run();
        System.out.println((System.currentTimeMillis()-t));
    }

    static <T> T measureTime(Supplier<T> r) {
        long t = System.currentTimeMillis();
        T o = r.get();
        System.out.println((System.currentTimeMillis()-t));
        return o;
    }

    static <T> void measureTime(T val, Consumer<T> r) {
        long t = System.currentTimeMillis();
        r.accept(val);
        System.out.println((System.currentTimeMillis()-t));
    }

    static <I,O> O measureTime(I in, Function<I,O> r) {
        long t = System.currentTimeMillis();
        O o = r.apply(in);
        System.out.println((System.currentTimeMillis()-t));
        return o;
    }

    /**
     * Returns {@link org.slf4j.Logger} for the specified class.
     * Equivalent to:
     * <pre>{@code LoggerFactory.getLogger(c);}</pre>
     */
    static Logger log(Class<?> c) {
        return LoggerFactory.getLogger(c);
    }

    /**
     * Returns all running threads. Incurs performance penalty, do not use
     * besides debugging purposes.
     */
    static Stream<Thread> activeThreads() {
        return Thread.getAllStackTraces().keySet().stream();
    }

    /** Prints names of all currently running non daemon threads. */
    static void printNonDaemonThreads() {
         activeThreads().filter(t->!t.isDaemon()).forEach(t -> System.out.println(t.getName()));
    }
}