package sp.it.pl.util.type;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.conf.Config.VarList;
import sp.it.pl.util.functional.Functors;
import sp.it.pl.util.functional.TriConsumer;
import static kotlin.text.StringsKt.substringBeforeLast;
import static sp.it.pl.util.dev.DebugKt.logger;
import static sp.it.pl.util.functional.Util.list;

/**
 * Reflection utility methods.
 */
@SuppressWarnings("unused")
public interface Util {

	/**
	 * Execute action for each observable value representing a javafx property of an object o.
	 * Additional provided arguments are name of the property and its non-erased generic type.
	 * Javafx properties are obtained from public nameProperty() methods using reflection.
	 */
	@SuppressWarnings({"UnnecessaryLocalVariable", "unchecked", "ConstantConditions"})
	static void forEachJavaFXProperty(Object o, TriConsumer<Observable,String,Class> action) {
		// Standard JavaFX Properties
		for (Method method : o.getClass().getMethods()) {
			String methodName = method.getName();
			boolean isPublished = !Modifier.isStatic(method.getModifiers()) && !methodName.startsWith("impl");
			if (isPublished) {
				String propertyName = null;
				if (Observable.class.isAssignableFrom(method.getReturnType())) {
					try {
						propertyName = methodName;
						propertyName = substringBeforeLast(propertyName, "Property", propertyName);
						propertyName = kotlin.text.StringsKt.substringAfter(propertyName, "get", propertyName);
						propertyName = kotlin.text.StringsKt.decapitalize(propertyName);
						method.setAccessible(true);
						Observable observable = (Observable) method.invoke(o);

						if (observable instanceof Property && ((Property) observable).isBound()) {
							ReadOnlyObjectWrapper<Object> rop = new ReadOnlyObjectWrapper<>();
							rop.bind((Property) observable);
							observable = rop.getReadOnlyProperty();
						}

						Class<?> propertyType = getGenericPropertyType(method.getGenericReturnType());
						if (observable!=null && propertyName!=null && propertyType!=null) {
							action.accept(observable, propertyName, propertyType);
						} else {
							logger(Util.class).warn("Is null property='{}' propertyName={} propertyType={}", observable, propertyName, propertyName);
						}

					} catch (IllegalAccessException|InvocationTargetException e) {
						logger(Util.class).error("Could not obtain property '{}' from object", propertyName);
					}
				}
			}
		}
		// Extended JavaFX Properties as exposed fields
		for (Field field : o.getClass().getFields()) {
			String fieldName = field.getName();
			boolean isPublished = !Modifier.isStatic(field.getModifiers()) && !fieldName.startsWith("impl");
			if (isPublished) {
				String propertyName = fieldName;
				if (Observable.class.isAssignableFrom(field.getType())) {
					try {
						field.setAccessible(true);
						Observable observable = (Observable) field.get(o);

						if (observable instanceof Property && ((Property) observable).isBound()) {
							ReadOnlyObjectWrapper<Object> rop = new ReadOnlyObjectWrapper<>();
							rop.bind((Property) observable);
							observable = rop.getReadOnlyProperty();
						}

						Class<?> propertyType = getGenericPropertyType(field.getGenericType());
						if (observable!=null && propertyName!=null && propertyType!=null) {
							action.accept(observable, propertyName, propertyType);
						} else {
							logger(Util.class).warn("Is null property='{}' propertyName={} propertyType={}", observable, propertyName, propertyName);
						}

					} catch (IllegalAccessException e) {
						logger(Util.class).error("Could not obtain property '{}' from object", propertyName);
					}
				}
			}
		}

		// add synthetic javafx layout properties for nodes in scene graph
		var child = o instanceof Node ? (Node) o : null;
		var parent = o instanceof Node ? ((Node) o).getParent() : null;
		if (parent instanceof StackPane) {
			action.accept(paneProperty(parent, child, Pos.class, "stackpane-alignment", StackPane::getAlignment, StackPane::setAlignment), "L: Alignment", Pos.class);
			action.accept(paneProperty(parent, child, Insets.class, "stackpane-alignment", StackPane::getMargin, StackPane::setMargin), "L: Margin", Insets.class);
		}
		if (parent instanceof AnchorPane) {
			action.accept(paneProperty(parent, child, Double.class, "pane-top-anchor", AnchorPane::getTopAnchor, AnchorPane::setTopAnchor), "L: Anchor (top)", Double.class);
			action.accept(paneProperty(parent, child, Double.class, "pane-right-anchor", AnchorPane::getRightAnchor, AnchorPane::setRightAnchor), "L: Anchor (right)", Double.class);
			action.accept(paneProperty(parent, child, Double.class, "pane-bottom-anchor", AnchorPane::getBottomAnchor, AnchorPane::setBottomAnchor), "L: Anchor (bottom)", Double.class);
			action.accept(paneProperty(parent, child, Double.class, "pane-left-anchor", AnchorPane::getLeftAnchor, AnchorPane::setLeftAnchor), "L: Anchor (left)", Double.class);
		}
		if (parent instanceof VBox) {
			action.accept(paneProperty(parent, child, Priority.class, "vbox-vgrow", VBox::getVgrow, VBox::setVgrow), "L: VGrow", Priority.class);
			action.accept(paneProperty(parent, child, Insets.class, "vbox-margin", VBox::getMargin, VBox::setMargin), "L: Margin", Insets.class);
		}
		if (parent instanceof HBox) {
			action.accept(paneProperty(parent, child, Priority.class, "hbox-vgrow", HBox::getHgrow, HBox::setHgrow), "L: HGrow", Priority.class);
			action.accept(paneProperty(parent, child, Insets.class, "hbox-margin", HBox::getMargin, HBox::setMargin), "L: Margin", Insets.class);
		}
		if (parent instanceof BorderPane) {
			action.accept(paneProperty(parent, child, Pos.class, "borderpane-alignment", BorderPane::getAlignment, BorderPane::setAlignment), "L: Alignment", Pos.class);
			action.accept(paneProperty(parent, child, Insets.class, "borderpane-margin", BorderPane::getMargin, BorderPane::setMargin), "L: Margin", Insets.class);
		}
		if (parent instanceof GridPane) {
			action.accept(paneProperty(parent, child, Integer.class, "gridpane-column", GridPane::getColumnIndex, GridPane::setColumnIndex), "L: Column index", Integer.class);
			action.accept(paneProperty(parent, child, Integer.class, "gridpane-column-span", GridPane::getColumnSpan, GridPane::setColumnSpan), "L: Column span", Integer.class);
			action.accept(paneProperty(parent, child, Integer.class, "gridpane-row", GridPane::getRowIndex, GridPane::setRowIndex), "L: Row index", Integer.class);
			action.accept(paneProperty(parent, child, Integer.class, "gridpane-row-span", GridPane::getRowSpan, GridPane::setRowSpan), "L: Row span", Integer.class);
			action.accept(paneProperty(parent, child, VPos.class, "gridpane-valignment", GridPane::getValignment, GridPane::setValignment), "L: Valignment", VPos.class);
			action.accept(paneProperty(parent, child, HPos.class, "gridpane-halignment", GridPane::getHalignment, GridPane::setHalignment), "L: Halignment", HPos.class);
			action.accept(paneProperty(parent, child, Priority.class, "gridpane-vgrow", GridPane::getVgrow, GridPane::setVgrow), "L: Vgrow", Priority.class);
			action.accept(paneProperty(parent, child, Priority.class, "gridpane-hgrow", GridPane::getHgrow, GridPane::setHgrow), "L: Hgrow", Priority.class);
			action.accept(paneProperty(parent, child, Insets.class, "gridpane-margin", HBox::getMargin, HBox::setMargin), "L: Margin", Insets.class);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Observable paneProperty(Parent parent, Node child, Class<T> type, String key, Functors.Ƒ1<Node,T> getter, BiConsumer<Node, T> setter) {
		return new SimpleObjectProperty<>(getter.apply(child)) {
			{
				child.getProperties().addListener((MapChangeListener.Change<?,?> v) -> {
					if (v.getKey().equals(key)) {
						super.setValue((T) v.getValueAdded());
					}
				});
			}

			@Override
			public void setValue(T v) {
				super.setValue(v);
				setter.accept(child, v);
			}
		};
	}

/* ---------- REFLECTION - INSTANTIATION ---------------------------------------------------------------------------- */

	static <T> T instantiateOrThrow(Class<T> type) {
		boolean isSingleton = isSingleton(type);
		return isSingleton ? instantiateAsSingleton(type) : instantiateWithDefConstructor(type);
	}

	static boolean isSingleton(Class<?> type) {
		String fieldName = "INSTANCE";
		try {
			Field f = type.getDeclaredField(fieldName);
			return Modifier.isStatic(f.getModifiers());
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	static <T> T instantiateAsSingleton(Class<T> type) {
		String fieldName = "INSTANCE";
		try {
			Field f = type.getDeclaredField(fieldName);
			Class<?> fType = f.getType();

			if (!Modifier.isStatic(f.getModifiers())) throw new NoSuchFieldException(fieldName + " field must be static=" + fType);
			if (fType!=type) throw new NoSuchFieldException(fieldName + " field has wrong type=" + fType);

			try {
				return (T) f.get(null);
			} catch (IllegalAccessException e) {
				throw new NoSuchFieldException("Field " + f + " is not accessible");
			}

		} catch(NoSuchFieldException e) {
			throw new RuntimeException("Could not instantiate class=" + type + " as singleton.", e);
		}
	}

	static <T> T instantiateWithDefConstructor(Class<T> type) {
		try {
			return type.getConstructor().newInstance();
		} catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException("Could not instantiate class=" + type + " using default constructor. Undeclared or inaccessible.", e);
		}
	}

/* ---------- REFLECTION - FIELD ------------------------------------------------------------------------------------ */

	@SuppressWarnings("unchecked")
	static <T> T getValueFromFieldMethodHandle(MethodHandle mh, Object instance) {
		try {
			if (instance==null) return (T) mh.invoke();
			else return (T) mh.invokeWithArguments(instance);
		} catch (Throwable e) {
			throw new RuntimeException("Error during getting value from a config field. ", e);
		}
	}

	/**
	 * Returns all declared fields of the class including private, static and inherited ones.
	 * Equivalent to union of declared fields of the class and all its superclasses.
	 */
	@SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
	static List<Field> getAllFields(Class clazz) {
		List<Field> fields = new ArrayList<>();

		// get all fields of the class (but not inherited fields)
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

		// get super class' fields recursively
		Class superClazz = clazz.getSuperclass();
		if (superClazz!=null) fields.addAll(getAllFields(superClazz));

		// get interface' fields recursively
		for (Class<?> i: clazz.getInterfaces())
			fields.addAll(getAllFields(i));

		return fields;
	}

	/**
	 * Returns all declared methods of the class including private, static and inherited ones.
	 * Equivalent to union of declared methods of the class and all its superclasses.
	 */
	static List<Method> getAllMethods(Class clazz) {
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
		Class superClazz = clazz.getSuperclass();
		if (superClazz!=null) methods.addAll(getAllMethods(superClazz));

		MapSet<String,Method> ms = new MapSet<>(m -> m.getName());
		ms.addAll(methods);

		return list(ms);
	}

/* ---------- REFLECTION - ANNOTATION ------------------------------------------------------------------------------- */

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
	 * Returns all superclasses and interfaces.
	 *
	 * @return list containing all superclasses
	 * @see #getSuperClassesInc(Class)
	 */
	static List<Class<?>> getSuperClasses(Class<?> c) {
		return getSuperClasses(c, list());
	}

	/**
	 * Returns all superclasses and interfaces and the class.
	 *
	 * @return list containing the class and all its superclasses
	 * @see #getSuperClasses(Class)
	 */
	static List<Class<?>> getSuperClassesInc(Class<?> c) {
		return getSuperClasses(c, list(c));
	}

	private static List<Class<?>> getSuperClasses(Class<?> c, List<Class<?>> cs) {
		Class<?> sc = c.getSuperclass();
		if (sc!=null) {
			cs.add(sc);
			getSuperClasses(sc, cs);
		}
		Class<?>[] is = c.getInterfaces();
		for (Class<?> i : is) {
			cs.add(i);
			getSuperClasses(i, cs);
		}
		return cs;
	}

	/**
	 * Converts class representing primitive type or void to its respective wrapper class.<br/>
     * This applies to the following 9 types:
     * <ul>
     *     <li> {@literal void.class}
     *     <li> {@literal byte.class}
     *     <li> {@literal boolean.class}
     *     <li> {@literal short.class}
     *     <li> {@literal integer.class}
     *     <li> {@literal long.class}
     *     <li> {@literal float.class}
     *     <li> {@literal double.class}
     *     <li> {@literal character.class}
     * </ul>
	 *
	 * @param c any class
	 * @return respective primitive wrapper class of given class or the class itself if it is not primitive.
	 */
	@SuppressWarnings("unchecked")
	static <T> Class<T> unPrimitivize(Class<T> c) {
		if (c.isPrimitive()) {
			if (c.equals(boolean.class)) return (Class) Boolean.class;
			if (c.equals(int.class)) return (Class) Integer.class;
			if (c.equals(float.class)) return (Class) Float.class;
			if (c.equals(double.class)) return (Class) Double.class;
			if (c.equals(long.class)) return (Class) Long.class;
			if (c.equals(byte.class)) return (Class) Byte.class;
			if (c.equals(short.class)) return (Class) Short.class;
			if (c.equals(char.class)) return (Class) Character.class;
			if (c.equals(void.class)) return (Class) Void.class;
		}
		return c;
	}

	/**
	 * Returns i-th generic parameter of the field starting from 0.
	 * For example {@code Integer for List<Integer>}
	 *
	 * @param field field to get generic parameter of
	 * @return i-th generic parameter of the field starting from 0.
	 */
	static Class getGenericType(Field field, int i) {
		ParameterizedType pType = (ParameterizedType) field.getGenericType();
		return (Class<?>) pType.getActualTypeArguments()[i];
	}

	/**
	 * Returns i-th generic parameter of the class starting from 0.
	 * For example Integer for class {@code IntegerList extends List<Integer>}
	 * <p/>
	 * Will NOT work on variables, using getClass() method on them.
	 *
	 * @param type class to get generic parameter of
	 * @param i index of the parameter
	 * @return i-th generic parameter of the class starting from 0.
	 */
	static Class getGenericClass(Class type, int i) {
		return (Class) ((ParameterizedType) type.getGenericSuperclass()).getActualTypeArguments()[i];
	}

	/**
	 * Intended use case: discovering the generic type of a javafx property in the runtime
	 * using reflection on parent object's {@link java.lang.reflect.Field} or {@link java.lang.reflect.Method} return
	 * type.
	 * <p/>
	 * This works around java's type erasure and makes it possible to determine exact property type
	 * even when property value is null or when the value is subtype of the property's generic type.
	 * <p/>
	 * Returns generic type of a {@link javafx.beans.property.Property} - usually the 1st generic
	 * parameter type of the first generic superclass or interface the provided type inherits from or
	 * implements.
	 * <p/>
	 * If type is:
	 * <ul>
	 *     <li/> primitive class, its boxed type is returned
	 *     <li/> class that does not inherit {@link javafx.beans.property.Property}, the class is returned as is
	 *     <li/> otherwise type of the property is returned
	 * </ul>
	 * In other words: this method makes the property wrapper transparent.
	 *
	 * @return exact generic type of {@link javafx.beans.property.Property} represented by the specified type
	 */
	static Class getGenericPropertyType(Type t) {
		if (t instanceof Class<?>) {
			boolean isProperty = ObservableValue.class.isAssignableFrom((Class<?>) t);
			if (!isProperty) return unPrimitivize((Class<?>) t);
		}

		if (t instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) t).getRawType();
			boolean isProperty = rawType instanceof Class<?> && ObservableValue.class.isAssignableFrom((Class<?>) rawType);
			if (!isProperty) return getRawType(t);
		}

		if (t instanceof WildcardType) {
			Type rawType = ((WildcardType) t).getUpperBounds()[0];
			boolean isProperty = rawType instanceof Class<?> && ObservableValue.class.isAssignableFrom((Class<?>) rawType);
			if (!isProperty) return getRawType(t);
		}

		Class<?> gpt = getGenericPropertyTypeImpl(t);

		// Workaround for number properties returning Number.class, due to implementing Property<Number>.
		if (gpt==Number.class) {
			String typename = t.getTypeName();
			if (typename.contains("Double")) return Double.class;
			if (typename.contains("Integer")) return Integer.class;
			if (typename.contains("Float")) return Float.class;
			if (typename.contains("Long")) return Double.class;
		}

		return gpt;
	}

	private static Class<?> getGenericPropertyTypeImpl(Type t) {

		// TODO: handle types defining generic property type indirectly, like: X<T> extends Property<Generic<T>>
		String typename = t.getTypeName();
		if (typename.contains(VarList.class.getSimpleName())) return ObservableList.class;

		if (t instanceof ParameterizedType) {
			Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
			return genericTypes.length==0 ? null : getRawType(genericTypes[0]);
		}

		if (t instanceof Class) {
			// recursively traverse class hierarchy until we find ParameterizedType and return result if not null.
			Type supertype = ((Class) t).getGenericSuperclass();
			Class output = null;
			if (supertype!=null && supertype!=Object.class)
				output = getGenericPropertyTypeImpl(supertype);
			if (output!=null) return output;

			// else try interfaces
			Type[] superinterfaces = ((Class) t).getGenericInterfaces();
			for (Type superinterface : superinterfaces) {
				if (superinterface instanceof ParameterizedType) {
					output = getGenericPropertyTypeImpl(superinterface);
					if (output!=null) return output;
				}
			}
		}

		return null;
	}

	private static Class<?> getRawType(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();
			return rawType instanceof Class<?> ? (Class<?>) rawType : null;
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType)type).getGenericComponentType();
			return Array.newInstance(getRawType(componentType), 0).getClass();
		} else if (type instanceof WildcardType) {
			return getRawType(((WildcardType) type).getUpperBounds()[0]);
		} else {
			return null;
		}
	}

	/**
	 * Returns i-th generic parameter of the class starting from 0.
	 * Same as {@link #getGenericClass(Class, int)} but for interfaces.
	 *
	 * @param type class to get generic parameter of
	 * @param i index of the interface
	 * @param p index of the parameter
	 * @return i-th generic parameter of the class starting from 0.
	 */
	static Class getGenericInterface(Class type, int i, int p) {
		return (Class) ((ParameterizedType) type.getGenericInterfaces()[i]).getActualTypeArguments()[p];
	}

	/**
	 * Returns field named n in class c.
	 *
	 * @implSpec the field can be declared in the class or any of its superclasses as opposed to standard reflection
	 * behavior which checks only the specified class
	 */
	static Field getField(Class c, String n) throws NoSuchFieldException {
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
	static void setField(Class c, Object o, String name, Object v) {
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

	/**
	 * Returns whether class is an enum.
	 * Works even for enums with class method bodies, where {@link Class#isEnum()} does not work.
	 *
	 * @return true if class is enum or false otherwise
	 * @see #getEnumConstants(Class)
	 */
	static boolean isEnum(Class<?> c) {
		return c.isEnum() || (c.getEnclosingClass()!=null && c.getEnclosingClass().isEnum());
	}

	/**
	 * Returns enum constants of an enum class in declared order.
	 * Works even for enums with class method bodies, where {@link Class#getEnumConstants()} does not work.
	 *
	 * @param type type of enum
	 * @return non null array of enum constants
	 * @throws java.lang.RuntimeException if class not an enum
	 */
	@SuppressWarnings({"unchecked", "deprecation"})
	static <T> T[] getEnumConstants(Class<?> type) {
		// handle enums
		if (type.isEnum()) return (T[]) type.getEnumConstants();

			// handle enum with class method bodies (they are not recognized as enums)
		else {
			Class<?> c = type.getEnclosingClass();
			if (c!=null && c.isEnum()) return (T[]) c.getEnumConstants();
			else throw new IllegalArgumentException("Class=" + type + " is not an Enum.");
		}
	}
}