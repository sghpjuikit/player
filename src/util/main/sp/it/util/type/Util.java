package sp.it.util.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import sp.it.util.collections.mapset.MapSet;
import static sp.it.util.functional.Util.list;

public interface Util {

	/**
	 * Returns all declared fields of the class including private, static and inherited ones.
	 * Equivalent to union of declared fields of the class and all its superclasses.
	 */
	@SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
	static List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();

		// get all fields of the class (but not inherited fields)
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

		// get super class' fields recursively
		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz!=null) fields.addAll(getAllFields(superClazz));

		// get interface fields recursively
		for (Class<?> i: clazz.getInterfaces())
			fields.addAll(getAllFields(i));

		return fields;
	}

	/**
	 * Returns all declared methods of the class including private, static and inherited ones.
	 * Equivalent to union of declared methods of the class and all its superclasses.
	 */
	static List<Method> getAllMethods(Class<?> clazz) {
		List<Method> methods = new ArrayList<>();

		if (clazz.isInterface()) {
			for (Class<?> i: clazz.getInterfaces()) {
				methods.addAll(getAllMethods(i));
			}
			for (Method m: clazz.getDeclaredMethods()) {
				if (m.isDefault())
					methods.add(m);
			}
			return methods;
		}

		// get all methods of the class (but not inherited methods)
		Collections.addAll(methods, clazz.getDeclaredMethods());

		// get default methods
		for (Class<?> i: clazz.getInterfaces()) {
			methods.addAll(getAllMethods(i));
		}

		// get super class' methods
		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz!=null) methods.addAll(getAllMethods(superClazz));

		MapSet<String,Method> ms = new MapSet<>(m -> m.getName());
		ms.addAll(methods);

		return list(ms);
	}

	/** Finds all declared methods in the class that are annotated by annotation of specified type. */
	static <A extends Annotation> Method getMethodAnnotated(Class<?> type, Class<A> ca) {
		for (Method m : type.getDeclaredMethods()) {
			A a = m.getAnnotation(ca);
			if (a!=null) return m;
		}
		return null;
	}

	/** Finds all declared constructors in the class that are annotated by annotation of specified type. */
	@SuppressWarnings("unchecked")
	static <A extends Annotation, T> Constructor<T> getConstructorAnnotated(Class<T> type, Class<A> ca) {
		for (Constructor<?> m : type.getDeclaredConstructors()) {
			A a = m.getAnnotation(ca);
			if (a!=null) return (Constructor<T>) m; // safe right? what else can the constructor return than T ?
		}
		return null;
	}

	/**
	 * Returns field named n in class c.
	 *
	 * @implSpec the field can be declared in the class or any of its superclasses as opposed to standard reflection
	 * behavior which checks only the specified class
	 */
	static Field getField(Class<?> c, String n) throws NoSuchFieldException {
		// get all fields of the class (but not inherited fields)
		Field f = null;
		try {
			f = c.getDeclaredField(n);
		} catch (NoSuchFieldException|SecurityException ex) {
			// ignore
		}

		if (f!=null) return f;

		// get super class' fields recursively
		Class<?> superClazz = c.getSuperclass();
		if (superClazz!=null) return getField(superClazz, n);
		else throw new NoSuchFieldException();
	}

	/**
	 * Gets value of a field of an object using reflection or null on error. Consumes all
	 * exceptions.
	 *
	 * @return value of a field of given object or null if value null or not possible
	 */
	@SuppressWarnings("unchecked")
	static <T> T getFieldValue(Object o, String name) {
		try {
			Field f = getField(o.getClass(), name);
			f.setAccessible(true);
			return (T) f.get(o);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Set field named f of the object o to value v.
	 *
	 * @throws RuntimeException if reflection error occurs
	 * @implSpec the field can be declared in the class or any of its super classes as opposed to standard reflection
	 * behavior which checks only the specified class
	 */
	static void setField(Object o, String f, Object v) {
		setField(o.getClass(), o, f, v);
	}

	/**
	 * Set field named f of the object o declared in class c to value v.
	 *
	 * @throws RuntimeException if reflection error occurs
	 * @implSpec the field can be declared in the class or any of its super classes as opposed to standard reflection
	 * behavior which checks only the specified class
	 */
	static void setField(Class<?> c, Object o, String name, Object v) {
		try {
			Field f = getField(c, name);
			f.setAccessible(true);
			f.set(o, v);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns method named m in class c.
	 *
	 * @implSpec the method can be declared in the class or any of its superclasses as opposed to standard reflection
	 * behavior which checks only the specified class
	 */
	static Method getMethod(Class<?> c, String n, Class<?>... params) throws NoSuchMethodException {
		// get all methods of the class (but not inherited methods)
		Method m = null;
		try {
			m = c.getDeclaredMethod(n, params);
		} catch (NoSuchMethodException|SecurityException ex) {
			// ignore
		}

		if (m!=null) return m;

		// get super class' methods recursively
		Class<?> superClazz = c.getSuperclass();
		if (superClazz!=null) return getMethod(superClazz, n, params);
		else throw new NoSuchMethodException();
	}

	/** Invokes method with no parameters on given object and returns the result. */
	static <T> Object invokeMethodP0(T o, String name) {
		try {
			Method m = getMethod(o.getClass(), name);
			m.setAccessible(true);
			return m.invoke(o);
		} catch (NoSuchMethodException|SecurityException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke method: " + name, e);
		}
	}

	/** Invokes method with no parameters on given object and returns the result. */
	static <T, P> Object invokeMethodP1(T o, String name, Class<P> paramType, P param) {
		try {
			Method m = getMethod(o.getClass(), name, paramType);
			m.setAccessible(true);
			return m.invoke(o, param);
		} catch (NoSuchMethodException|SecurityException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
			throw new RuntimeException("Failed to invoke method '" + name + "' for " + param.getClass(), e);
		}
	}

}