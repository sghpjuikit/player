package util.type;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.value.ObservableValue;

import unused.TriConsumer;

import static util.dev.Util.log;
import static util.functional.Util.list;

/**
 * Reflection utility methods.
 *
 * @author Martin Polakovic
 */
public interface Util {

    static <T> T build(T t, Consumer<? super T> postAction) {
        postAction.accept(t);
        return t;
    }

    static <T> T build(Supplier<? extends T> constructor, Consumer<? super T> postAction) {
        T t  = constructor.get();
        postAction.accept(t);
        return t;
    }

    /**
     * Execute action for each observable value representing a javafx property of an object o.
     * Additional provided arguments are name of the property and its non-erased generic type.
     * Javafx properties are obtained from public nameProperty() methods using reflection.
     */
    static void forEachJavaFXProperty(Object o, TriConsumer<ObservableValue,String,Class> action) {
        for (Method method : getAllMethods(o.getClass())) {
            String methodName = method.getName();
            // We are looking for javafx property bean methods
            // We must filter out nonpublic and impl (can be public) ones. Why? Real life example:
            // Serialization serializes all javafx property bean values of an graphical object -
            // effect. Upon deserialization a 'private' flag indicating redrawing is restored
            // which prevents the effect from updating values upon change. Such flags are usually
            // the implNameProperty methods and can be public due to reasons...
            //
            // In other words anything non-public is not safe.
            if (methodName.endsWith("Property") && Modifier.isPublic(method.getModifiers()) && !methodName.startsWith("impl")) {
                try {
                    Class<?> returnType = method.getReturnType();
                    if (ObservableValue.class.isAssignableFrom(returnType)) {
                        String propertyName = methodName.substring(0, methodName.lastIndexOf("Property"));
                        method.setAccessible(true);
                        ObservableValue<?> property = (ObservableValue) method.invoke(o);
                        Class<?> propertyType = getGenericPropertyType(method.getGenericReturnType());
                        if(util.functional.Util.isNoneØ(property, propertyName, propertyType))
                            action.accept(property, propertyName, propertyType);
                    }
                } catch(IllegalAccessException | InvocationTargetException e) {
                    log(Util.class).error("Could not obtain property from object",e);
                }
            }
        }
    }

/* ---------- REFLECTION - FIELD ------------------------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    static <T> T getValueFromFieldMethodHandle(MethodHandle mh, Object instance) {
        try {
            if(instance==null) return (T) mh.invoke();
            else return (T) mh.invokeWithArguments(instance);
        } catch (Throwable e) {
            throw new RuntimeException("Error during getting value from a config field. ", e);
        }
    }

    /**
     * Returns all declared fields of the class including inherited ones.
     * Equivalent to union of declared fields of the class and all its
     * superclasses.
     */
    static List<Field> getAllFields(Class clazz) {
        List<Field> fields = new ArrayList<>();
        // get all fields of the class (but not inherited fields)
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        Class superClazz = clazz.getSuperclass();
        // get super class' fields recursively
        if(superClazz != null) fields.addAll(getAllFields(superClazz));

        return fields;
    }

    /**
     * Returns all declared methods of the class including inherited ones.
     * Equivalent to union of declared fields of the class and all its
     * superclasses.
     */
    static List<Method> getAllMethods(Class clazz) {
        List<Method> methods = new ArrayList<>();
        // get all fields of the class (but not inherited fields)
        methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));

        Class superClazz = clazz.getSuperclass();
        // get super class' fields recursively
        if(superClazz != null) methods.addAll(getAllMethods(superClazz));

        return methods;
    }

/* ---------- REFLECTION - ANNOTATION ------------------------------------------------------------------------------- */

    /** Finds all declared methods in the class that are annotated by annotation of specified type. */
    static <A extends Annotation> Method getMethodAnnotated(Class<?> type, Class<A> ca) {
        for(Method m: type.getDeclaredMethods()) {
            A a = m.getAnnotation(ca);
            if(a!=null) return m;
        }
        return null;
    }

    /** Finds all declared constructors in the class that are annotated by annotation of specified type. */
    @SuppressWarnings("unchecked")
    static <A extends Annotation, T> Constructor<T> getConstructorAnnotated(Class<T> type, Class<A> ca) {
        for(Constructor<?> m: type.getDeclaredConstructors()) {
            A a = m.getAnnotation(ca);
            if(a!=null) return (Constructor<T>) m; // safe right? what else can the constructor return than T ?
        }
        return null;
    }

    /**
     * Returns all superclasses and interfaces.
     * @return list containing all superclasses
     * @see #getSuperClassesInc(Class)
     */
    static List<Class> getSuperClasses(Class<?> c) {
        return getSuperClasses(c, list());
    }

    /**
     * Returns all superclasses and interfaces and the class.
     * @return list containing the class and all its superclasses
     * @see #getSuperClasses(Class)
     */
    static List<Class> getSuperClassesInc(Class<?> c) {
        return getSuperClasses(c, list(c));
    }

    private static List<Class> getSuperClasses(Class<?> c, List<Class> cs) {
        Class<?> sc = c.getSuperclass();
        if(sc!=null) {
            cs.add(sc);
            getSuperClasses(sc, cs);
        }
        Class<?>[] is = c.getInterfaces();
        for(Class<?> i : is) {
            cs.add(i);
            getSuperClasses(i, cs);
        }
        return cs;
    }

    /**
     * Converts primitive class to wrapper class.
     *
     * @param c any class
     * @return respective primitive wrapper class of given class or the class itself if it is not primitive.
     */
    @SuppressWarnings("unchecked")
    static <T> Class<T> unPrimitivize(Class<T> c) {
        if(c.isPrimitive()) {
            if(c.equals(boolean.class)) return (Class) Boolean.class;
            if(c.equals(int.class)) return (Class) Integer.class;
            if(c.equals(float.class)) return (Class) Float.class;
            if(c.equals(double.class)) return (Class) Double.class;
            if(c.equals(long.class)) return (Class) Long.class;
            if(c.equals(byte.class)) return (Class) Byte.class;
            if(c.equals(short.class)) return (Class) Short.class;
            if(c.equals(char.class)) return (Class) Character.class;
        }
        return c;
    }

    /**
     * Returns i-th generic parameter of the field starting from 0.
     * For example {@code Integer for List<Integer>}
     *
     * @param f
     * @return
     */
    static Class getGenericType(Field f, int i) {
        ParameterizedType pType = (ParameterizedType) f.getGenericType();
        Class<?> genericType = (Class<?>) pType.getActualTypeArguments()[i];
        return genericType;
    }

    /**
     * Returns i-th generic parameter of the class starting from 0.
     * For example Integer for class {@code IntegerList extends List<Integer>}
     * <p/>
     * Will NOT work on variables, using getClass() method on them.
     *
     * @param c
     * @param i
     * @return
     */
    static Class getGenericClass(Class c, int i) {
        return (Class) ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments()[i];
    }

    /**
     * Intended use case: discovering the generic type of a javafx property in the runtime
     * using reflection on parent object's {@link java.lang.reflect.Field} or {@link java.lang.reflect.Method} return
     * type (javafx property specification).
     * <p/>
     * This works around java's type erasure and makes it possible to determine exact property type
     * even when property value is null or when the value is subtype of the property's generic type.
     * <p/>
     * Returns generic type of a {@link javafx.beans.property.Property} or formally the 1st generic
     * parameter type of the first generic superclass or interface the provided type inherits from or
     * implements.
     * <p/>
     * The method inspects the class hierarchy and interfaces (if previous yields no result) and
     * looks for generic types. If any class or interface found is generic and its 1st generic
     * parameter type is available it is returned. Otherwise the inspection continues. In case of no
     * success, null is returned
     *
     * @return class of the 1st generic parameter of the specified type or of some of its supertype or
     * null if none found.
     */
    static Class getGenericPropertyType(Type t) {
        // TODO: fix this returning null for EventHandlerProperty

        // debug, reveals the class inspection order
        // System.out.println("inspecting " + t.getTypeName());

        // Workaround for all number properties returning Number.class instead of their respective
        // class, due to all implementing something along the lines Property<Number>. As per
        // javadoc review, the affected are the four classes : Double, Float, Long, Integer.
        String typename = t.getTypeName(); // classname
        if(typename.contains("Double")) return Double.class;
        if(typename.contains("Integer")) return Integer.class;
        if(typename.contains("Float")) return Float.class;
        if(typename.contains("Long")) return Double.class;

        // This method is called recursively, but if ParameterizedType is passed in, we are halfway
        // there. We just return generic type if it is available. If not we return null and the
        // iteration will continue on upper level.
        if(t instanceof ParameterizedType) {
            Type[] generictypes = ((ParameterizedType)t).getActualTypeArguments();
            if(generictypes.length>0 && generictypes[0] instanceof Class)
                return (Class)generictypes[0];
            else return null;
        }

        if(t instanceof Class) {
            // recursively traverse class hierarchy until we find ParameterizedType
            // and return result if not null.
            Type supertype = ((Class)t).getGenericSuperclass();
            Class output = null;
            if(supertype!=null && supertype!=Object.class)
                output = getGenericPropertyType(supertype);
            if(output!=null) return output;

            // else try interfaces
            Type[] superinterfaces = ((Class)t).getGenericInterfaces();
            for(Type superinterface : superinterfaces) {
                if(superinterface instanceof ParameterizedType) {
                    output = getGenericPropertyType(superinterface);
                    if(output!=null) return output;
                }
            }
        }

        return null;
    }

    /**
     * Same as {@link #getGenericClass(Class, int)} but for interfaces.
     * Returns p-th generic parameter of the i-th interface of c class starting from 0.
     *
     * @param type
     * @param i
     * @param p
     * @return
     */
    static Class getGenericInterface(Class type, int i, int p) {
        return (Class) ((ParameterizedType) type.getGenericInterfaces()[i]).getActualTypeArguments()[p];
    }

    /**
     * Returns field named n declared in class c.
     *
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class
     */
    static Field getField(Class c, String n) throws NoSuchFieldException {
        // get all fields of the class (but not inherited fields)
        Field f = null;
        try {
            f = c.getDeclaredField(n);
        } catch (NoSuchFieldException | SecurityException ex) {
            // ignore
        }

        if (f!=null) return f;

        // get super class' fields recursively
        Class<?> superClazz = c.getSuperclass();
        if (superClazz != null) return getField(superClazz, n);
        else throw new NoSuchFieldException();
    }

    /**
     * Gets value of a field of an object using reflection or null on error. Consumes all
     * exceptions.
     * @return value of a field of given object or null if value null or not possible
     */
    static <T> T getFieldValue(Object o, Class<T> type, String fieldname) {
        try {
            Field f = getField(o.getClass(), fieldname);
            f.setAccessible(true);
            T t = (T) f.get(o);
            f.setAccessible(false);
            return t;
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Set field named f of the object o to value v.
     *
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class
     * @throws RuntimeException if reflection error occurs
     */
    static void setField(Object o, String f, Object v) {
        setField(o.getClass(), o, f, v);
    }

    /**
     * Set field named f of the object o declared in class c to value v.

     *
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class
     * @throws RuntimeException if reflection error occurs
     */
    static void setField(Class c, Object o, String f, Object v) {
        try {
            Field fl = getField(c,f);
            fl.setAccessible(true);
            fl.set(o,v);
            fl.setAccessible(false);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    /** Invokes method with no parameters on given object and returns the result. */
    static <T> Object invokeMethodP0(Class<T> exactclass, T o, String name) {
        try {
            Method m = exactclass.getDeclaredMethod(name);
            m.setAccessible(true);
            Object r = m.invoke(o);
            m.setAccessible(false);
            return r;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke method: " + name, e);
        }
    }

    /** Invokes method with no parameters on given object and returns the result. */
    static <T,P> Object invokeMethodP1(Class<T>  exactclass, T o, String name, Class<P> paramtype, P param) {
        try {
            Method m = exactclass.getDeclaredMethod(name,paramtype);
            m.setAccessible(true);
            Object r = m.invoke(o,param);
            m.setAccessible(false);
            return r;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Failed to invoke method: " + name, e);
        }
    }

    /**
     * Renames declared enum sonstant using the mapper function on the enum
     * constant string.
     * <p/>
     * This method effectively overrides both enum's toString() and valueOf()
     * methods. It allows using arbitrary string values for enum constants,
     * but in toString/valueOf cpliant way.
     * <p/>
     * Use in enum constructor. For example:
     * <br/>
     * <pre>
     * {@code
     *  class MyEnum {
     *      A,
     *      B;
     *
     *      public MuEnum() {
     *          mapEnumConstantName(MyEnum.class, this, String::toLowerCase);
     *      }
     *  }
     * }
     * </pre>
     *
     * @param <E>
     * @param constant enum constant
     * @param mapper function to apply on the constant
     * @throws RuntimeException if reflection error occurs
     */
    static <E extends Enum<E>> void mapEnumConstantName(E constant, Function<E, String> mapper) {
        setField(constant.getClass().getSuperclass(), constant, "name", mapper.apply(constant));
    }

    /**
     * Returns whether class is an enum. Works for
     * enums with class method bodies (where Class.isEnum) does not work.
     *
     * @return true if class is enum or false otherwise
     * @see #getEnumConstants(Class)
     */
    static boolean isEnum(Class<?> c) {
        return c.isEnum() || (c.getEnclosingClass()!=null && c.getEnclosingClass().isEnum());
    }

    /**
     * Returns enum constants of an enum class in declared order. Works for
     * enums with class method bodies (where Enum.getEnumConstants) does not work.
     * <p/>
     * Always use {@link #isEnum(Class)} before this method.
     *
     * @param c
     * @return array of enum constants, never null
     * @throws IllegalArgumentException if class not an enum
     */
    static <T> T[] getEnumConstants(Class c) {
        // handle enums
        if(c.isEnum()) return (T[]) c.getEnumConstants();

            // handle enum with class method bodies (they are not recognized as enums)
        else {
            Class ec = c.getEnclosingClass();
            if(ec!=null && ec.isEnum())
                return (T[]) ec.getEnumConstants();
            else
                throw new IllegalArgumentException("Class " + c + " is not an Enum.");
        }
    }
}