package sp.it.util.parsing;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.functional.Try;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.TryKt.runTry;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.Util.stream;

@SuppressWarnings({"unused", "ConstantConditions", "SameParameterValue"})
public interface Parsers {

    ConverterDefault DEFAULT = new ConverterDefault();
    Converter FX = new ConverterFX();

    static Method getValueOfStatic(Class<?> type) {
        if (type.getEnclosingClass()!=null && type.getEnclosingClass().isEnum())
            type = type.getEnclosingClass();

        try {
            return type.getDeclaredMethod("valueOf", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    static Method getMethodStatic(String name, Class<?> type) {
        try {
            Method m = type.getDeclaredMethod(name, String.class);
            if (!m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return m;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    static <I, O> Function<I,Try<O,String>> parserOfI(Invokable<O> m, Class<I> typeIn, Class<O> typeOut, StringParseStrategy a, ParseDir dir) {
        Collection<Parameter> params = m.getParameters();
        if (params.size()>1)
            throw new IllegalArgumentException("Converter method/constructor must take 0 or 1 parameter");

        // exceptions (we will make union of those annotated and those known to be thrown
        Set<Class<?>> ecs = new HashSet<>();
        if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
        else ecs.add(Exception.class);
        if (m!=null) ecs.addAll(m.getExceptionTypes());

        boolean no_input = params.isEmpty();
        return no_input
            ? in -> {
                try {
                    return ok(m.invoke((Object[]) null));
                } catch (IllegalAccessException|InvocationTargetException e) {
                    for (Class<?> ec : ecs) {
                        if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
                            return errorOf(e.getCause().getCause());
                        if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
                        if (ec.isInstance(e)) return errorOf(e);
                    }
                    throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
                }
            }
            : in -> {
                try {
                    return ok(m.invoke(null, in));
                } catch (IllegalAccessException|InvocationTargetException e) {
                    for (Class<?> ec : ecs) {
                        if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
                            return errorOf(e.getCause().getCause());
                        if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
                        if (ec.isInstance(e)) return errorOf(e);
                    }
                    throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
                }
            };
    }

    @SuppressWarnings("unchecked")
    static <I, O> Function<I,Try<O,String>> parserOfM(Method m, Class<I> i, Class<O> o, StringParseStrategy a, ParseDir dir) {
        Set<Class<?>> ecs = new HashSet<>();
        if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
        else ecs.add(Exception.class);
        if (m!=null) ecs.addAll(list(m.getExceptionTypes()));
        boolean isSupplier = i==Void.class || i==void.class || i==null;
        boolean isStatic = Modifier.isStatic(m.getModifiers());
        return isSupplier
            ? in -> {
            try {
                return ok((O) m.invoke(null));
            } catch (IllegalAccessException|InvocationTargetException e) {
                for (Class<?> ec : ecs) {
                    if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
                        return errorOf(e.getCause().getCause());
                    if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
                    if (ec.isInstance(e)) return errorOf(e);
                }
                throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
            }
        }
            : in -> {
            try {
                return ok((O) m.invoke(null, in));
            } catch (IllegalAccessException|InvocationTargetException e) {
                for (Class<?> ec : ecs) {
                    if (e.getCause()!=null && ec.isInstance(e.getCause().getCause()))
                        return errorOf(e.getCause().getCause());
                    if (ec.isInstance(e.getCause())) return errorOf(e.getCause());
                    if (ec.isInstance(e)) return errorOf(e);
                }
                throw new RuntimeException("Converter cant invoke the method: " + m, e.getCause());
            }
        };
    }

    static <O> Function<String,Try<O,String>> parserOfC(StringParseStrategy a, ParseDir dir, Class<O> type, Class<?>... params) {
        try {
            Constructor<O> c = type.getConstructor(params);
            if (c==null) throw new NoSuchMethodException();
            boolean isInputParam = params.length==1;
            Set<Class<?>> ecs = new HashSet<>();
            if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
            if (c!=null) ecs.addAll(list(c.getExceptionTypes()));
            return in -> {
                try {
                    Object[] p = isInputParam ? new Object[]{in} : new Object[]{};
                    return ok(c.newInstance(p));
                } catch (ExceptionInInitializerError|InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
                    for (Class<?> ec : ecs) {
                        if (e.getCause()!=null && ec.isInstance(e.getCause().getCause())) {
                            return errorOf(e.getCause().getCause());
                        }
                        if (ec.isInstance(e.getCause())) {
                            return errorOf(e.getCause());
                        }
                        if (ec.isInstance(e)) {
                            return errorOf(e);
                        }
                    }
                    throw new RuntimeException("String '" + in + "' parsing failed to invoke constructor in class " + c.getDeclaringClass(), e);
                }
            };
        } catch (NoSuchMethodException|SecurityException e) {
            throw new RuntimeException("Converter cant find constructor suitable for parsing " + type + " with parameters" + sp.it.util.functional.Util.toS(", ", params), e);
        }
    }

    static <I, O> Ƒ1<I,Try<O,String>> noExWrap(Executable m, StringParseStrategy a, ParseDir dir, Function<I,O> f) {
        Set<Class<?>> ecs = new HashSet<>();
        if (a!=null) ecs.addAll(list(dir==ParseDir.TOS ? a.exTo() : a.exFrom()));
        if (m!=null) ecs.addAll(list(m.getExceptionTypes()));
        return i -> runTry(() -> f.apply(i))
            .ifErrorUse(e -> {
                if (ecs.stream().noneMatch(ec -> ec.isInstance(e)))
                    throw new RuntimeException("Unhandled exception thrown in Try operation", e);
            })
            .mapError(e -> e.getMessage()!=null ? e.getMessage() : "Unknown error");
    }

    private static <R> Try<R,String> errorOf(Throwable e) {
        return Try.Java.error(e.getMessage()!=null ? e.getMessage() : "Unknown error");
    }

    interface Invokable<I> {
        I invoke(Object... params) throws IllegalAccessException, InvocationTargetException;

        Collection<Parameter> getParameters();

        Collection<Class<? extends Throwable>> getExceptionTypes();

        static <T> Invokable<T> of(Constructor<T> c) {
            return new Invokable<>() {
                @Override
                public final T invoke(Object... params) throws IllegalAccessException, InvocationTargetException {
                    try {
                        // We must skip the null object receiver - 1st parameter
                        return c.newInstance(stream(params).skip(1).toArray());
                    } catch (IllegalArgumentException|InstantiationException e) {
                        throw new InvocationTargetException(e);
                    }
                }

                @Override
                public Collection<Parameter> getParameters() {
                    return listRO(c.getParameters());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Collection<Class<? extends Throwable>> getExceptionTypes() {
                    return (List) listRO(c.getExceptionTypes());
                }
            };
        }

        static <T> Invokable<T> ofStaticMethod(Method m) {
            return new Invokable<>() {
                @SuppressWarnings("unchecked")
                @Override
                public T invoke(Object... params) throws IllegalAccessException, InvocationTargetException {
                    return (T) m.invoke(null, params);
                }

                @Override
                public Collection<Parameter> getParameters() {
                    return listRO(m.getParameters());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Collection<Class<? extends Throwable>> getExceptionTypes() {
                    return (List) listRO(m.getExceptionTypes());
                }
            };
        }
    }

    enum ParseDir {
        TOS, OFS
    }
}