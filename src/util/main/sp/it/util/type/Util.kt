package sp.it.util.type

import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.MapChangeListener
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import mu.KotlinLogging
import sp.it.util.dev.fail
import sp.it.util.functional.asIs
import sp.it.util.functional.recurse
import sp.it.util.type.PaneProperties.paneProperty
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

fun Class<*>.isSuperclassOf(type: Class<*>) = isAssignableFrom(type)

fun Class<*>.isSubclassOf(type: Class<*>) = type.isSuperclassOf(this)

fun KClass<*>.isSuperclassOf(type: Class<*>) = isSuperclassOf(type.kotlin)

fun KClass<*>.isSubclassOf(type: Class<*>) = isSubclassOf(type.kotlin)

inline fun <reified T> KClass<*>.isSuperclassOf() = isSuperclassOf(T::class)

inline fun <reified T> KClass<*>.isSubclassOf() = isSubclassOf(T::class)

inline fun <reified T> Class<*>.isSuperclassOf() = isSuperclassOf(T::class.java)

inline fun <reified T> Class<*>.isSubclassOf() = isSubclassOf(T::class.java)

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> KType.isSupertypeOf() = isSupertypeOf(typeOf<T>())

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> KType.isSubtypeOf(): Boolean = isSubtypeOf(typeOf<T>())

/** @return the most specific common supertype of the specified types */
infix fun KClass<*>.union(type: KClass<*>): KClass<*> = when {
   this==type -> this
   this==Any::class || type==Any::class -> Any::class
   this.isSuperclassOf(type) -> this
   type.isSubclassOf(type) -> type
   else -> Any::class
}

inline fun <reified T: Annotation> Class<*>.findAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T: Annotation> Field.findAnnotation(): T? = getAnnotation(T::class.java)

/** Set [java.util.logging.Logger] of specified package to specified level. Helpful for stubborn libraries. */
fun setLoggingLevelForPackage(logPackage: Package, logLevel: Level) {
   java.util.logging.Logger.getLogger(logPackage.name).apply {
      level = logLevel
      useParentHandlers = false
   }
}

/** @return thread-safe [ReadWriteProperty] backed by [AtomicReference] */
fun <T> atomic(initialValue: T) = object: ReadWriteProperty<Any?, T> {

   private val ref = AtomicReference<T>(initialValue)

   override fun getValue(thisRef: Any?, property: KProperty<*>) = ref.get()

   override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = ref.set(value)

}

/** @return class representing this type, i.e., type stripped of its generic type parameters */
fun Type.toRaw(): Class<*> = let { type ->
   when (type) {
      is Class<*> -> type
      is ParameterizedType -> {
         val rawType = type.rawType
         if (rawType is Class<*>) rawType else fail { "Unable to determine raw type of parameterized type=$type, it's rawType=$rawType is not instance of ${Class::class.java}" }
      }
      is GenericArrayType -> {
         val componentType = type.genericComponentType
         Array.newInstance(componentType.toRaw(), 0).javaClass
      }
      is WildcardType -> {
         if (type.lowerBounds.isEmpty()) type.upperBounds[0].toRaw()
         else type.lowerBounds[0].toRaw()
      }
      is TypeVariable<*> -> type.bounds[0].toRaw()
      else -> fail { "Unable to determine raw type of type=$type, unsupported kind" }
   }
}

/**
 * Flattens a type to individual type fragments represented by jvm classes, removing variance (wildcards) and nullability.
 *
 * Examples:
 *
 * `Any` -> [Object.class]
 * `Any?` -> [Object.class]
 * `List<*>` -> [List.class, Object.class]
 * `List<Int?>` -> [List.class, Integer.class]
 * `List<out Int>` -> [List.class, Integer.class]
 * `List<in Int?>` -> [List.class, Integer.class]
 * `MutableList<Int>` -> [List.class, Integer.class]
 * `ArrayList<Int>` -> [ArrayList.class, Integer.class]
 *
 * @return sequence of classes representing the specified type and its generic type arguments
 */
fun Type.flattenToRawTypes(): Sequence<Class<*>> = when (this) {
   is WildcardType -> (if (lowerBounds.isNullOrEmpty()) upperBounds else lowerBounds).asSequence().flatMap { it.flattenToRawTypes() }
   is ParameterizedType -> sequenceOf(toRaw()) + actualTypeArguments.asSequence().flatMap { it.flattenToRawTypes() }
   is Class<*> -> sequenceOf(this)
   else -> throw Exception(toString())
}

/** Set specified property of this object to null. Use for disposal of read-only properties and avoiding memory leaks. */
infix fun Any.nullify(property: KProperty<*>) {
   property.javaField?.isAccessible = true
   property.javaField?.set(this, null)
}

/** Returns sequence of class' all superclasses and interfaces in depth first declaration order. */
fun KClass<*>.superKClasses(): Sequence<KClass<*>> = superKClassesInc().drop(1)

/** Returns sequence of this class, its all superclasses and interfaces in depth first declaration order. */
fun KClass<*>.superKClassesInc(): Sequence<KClass<*>> = when {
   this==Nothing::class -> sequenceOf(Nothing::class)
   this==Unit::class -> sequenceOf(Unit::class)
   else -> java.recurse { listOfNotNull(it.superclass) + it.interfaces }.map { it.kotlin }
   // recurse { it.superclasses }   // TODO: KClass.superclasses is bugged for anonymous Java classes
}


/**
 * Execute action for each observable value representing a javafx property of an object o.
 * Additional provided arguments are name of the property and its non-erased generic type.
 * Javafx properties are obtained from public nameProperty() methods using reflection.
 */
fun forEachJavaFXProperty(o: Any, action: (Observable, String, KType) -> Unit) {
   for (method in o::class.memberFunctions) {
      val methodName = method.name
      val isPublished = method.visibility==PUBLIC && !methodName.startsWith("impl")
      if (isPublished) {
         var propertyName: String? = null
         if (method.returnType.raw.isSubclassOf<Observable>()) {
            try {
               propertyName = methodName
               propertyName = propertyName.substringBeforeLast("Property", propertyName)
               propertyName = propertyName.substringAfter("get", propertyName)
               propertyName = propertyName.decapitalize()
               method.isAccessible = true
               var observable = method.call(o) as Observable?
               if (observable is Property<*> && observable.isBound) {
                  val rop = ReadOnlyObjectWrapper<Any>()
                  rop.bind(observable)
                  observable = rop.readOnlyProperty
               }
               val propertyType = javaFxPropertyType(method.returnType)
               if (observable!=null && propertyType!=null) {
                  action(observable, propertyName, propertyType)
               } else {
                  logger.warn { "Is null property='$observable' propertyName=$propertyName propertyType=$propertyType" }
               }
            } catch (e: IllegalAccessException) {
               logger.error(e) { "Could not obtain property '$propertyName' from object" }
            } catch (e: InvocationTargetException) {
               logger.error(e) { "Could not obtain property '$propertyName' from object" }
            }
         }
      }
   }
   for (field in o::class.memberProperties) {
      val fieldName = field.name
      val isPublished = field.visibility==PUBLIC && !fieldName.startsWith("impl")
      if (isPublished) {
         if (field.returnType.raw.isSubclassOf<Observable>()) {
            try {
               field.isAccessible = true
               var observable = field.getter.call(o) as Observable?
               if (observable is Property<*> && observable.isBound) {
                  val rop = ReadOnlyObjectWrapper<Any>()
                  rop.bind(observable)
                  observable = rop.readOnlyProperty
               }
               val propertyType = javaFxPropertyType(field.returnType)
               if (observable!=null && propertyType!=null) {
                  action(observable, fieldName, propertyType)
               } else {
                  logger.warn { "Is null property='$observable' propertyName=$fieldName propertyType=$propertyType" }
               }
            } catch (e: IllegalAccessException) {
               logger.error(e) { "Could not obtain property '$fieldName' from object" }
            }
         }
      }
   }

   // add synthetic javafx layout properties for nodes in scene graph
   if (o is Node) {
      when (o.parent) {
         is StackPane -> {
            action(paneProperty(o,"stackpane-alignment", StackPane::getAlignment, StackPane::setAlignment), "L: Alignment", type<Pos?>().type)
            action(paneProperty(o,"stackpane-margin", StackPane::getMargin, StackPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is AnchorPane -> {
            action(paneProperty(o,"pane-top-anchor", AnchorPane::getTopAnchor, AnchorPane::setTopAnchor), "L: Anchor (top)", type<Double?>().type)
            action(paneProperty(o,"pane-right-anchor", AnchorPane::getRightAnchor, AnchorPane::setRightAnchor), "L: Anchor (right)", type<Double?>().type)
            action(paneProperty(o,"pane-bottom-anchor", AnchorPane::getBottomAnchor, AnchorPane::setBottomAnchor), "L: Anchor (bottom)", type<Double?>().type)
            action(paneProperty(o,"pane-left-anchor", AnchorPane::getLeftAnchor, AnchorPane::setLeftAnchor), "L: Anchor (left)", type<Double?>().type)
         }
         is VBox -> {
            action(paneProperty(o,"vbox-vgrow", VBox::getVgrow, VBox::setVgrow), "L: VGrow", type<Priority?>().type)
            action(paneProperty(o,"vbox-margin", VBox::getMargin, VBox::setMargin), "L: Margin", type<Insets?>().type)
         }
         is HBox -> {
            action(paneProperty(o,"hbox-vgrow", HBox::getHgrow, HBox::setHgrow), "L: HGrow", type<Priority?>().type)
            action(paneProperty(o,"hbox-margin", HBox::getMargin, HBox::setMargin), "L: Margin", type<Insets?>().type)
         }
         is FlowPane -> {
            action(paneProperty(o,"flowpane-margin", FlowPane::getMargin, FlowPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is BorderPane -> {
            action(paneProperty(o,"borderpane-alignment", BorderPane::getAlignment, BorderPane::setAlignment), "L: Alignment", type<Pos?>().type)
            action(paneProperty(o,"borderpane-margin", BorderPane::getMargin, BorderPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is GridPane -> {
            action(paneProperty(o,"gridpane-column", GridPane::getColumnIndex, GridPane::setColumnIndex), "L: Column index", type<Int?>().type)
            action(paneProperty(o,"gridpane-column-span", GridPane::getColumnSpan, GridPane::setColumnSpan), "L: Column span", type<Int?>().type)
            action(paneProperty(o,"gridpane-row", GridPane::getRowIndex, GridPane::setRowIndex), "L: Row index", type<Int?>().type)
            action(paneProperty(o,"gridpane-row-span", GridPane::getRowSpan, GridPane::setRowSpan), "L: Row span", type<Int?>().type)
            action(paneProperty(o,"gridpane-valignment", GridPane::getValignment, GridPane::setValignment), "L: Valignment", type<VPos?>().type)
            action(paneProperty(o,"gridpane-halignment", GridPane::getHalignment, GridPane::setHalignment), "L: Halignment", type<HPos?>().type)
            action(paneProperty(o,"gridpane-vgrow", GridPane::getVgrow, GridPane::setVgrow), "L: Vgrow", type<Priority?>().type)
            action(paneProperty(o,"gridpane-hgrow", GridPane::getHgrow, GridPane::setHgrow), "L: Hgrow", type<Priority?>().type)
            action(paneProperty(o,"gridpane-margin", HBox::getMargin, HBox::setMargin), "L: Margin", type<Insets?>().type)
         }
      }
   }
}

private object PaneProperties {
   inline fun <reified T: Any> paneProperty(o: Node, key: String, crossinline getter: (Node) -> T?, crossinline setter: (Node, T?) -> Unit): Observable =
      object: SimpleObjectProperty<T?>(getter(o)) {
         init {
            o.properties.addListener(MapChangeListener {
               if (it.key==key)
                  super.setValue(it.valueAdded.asIs())
            })
         }
         override fun setValue(v: T?) {
            super.setValue(v)
            setter(o, v)
         }
      }
}

// TODO: switch to implementation from TypeUtilTest
fun javaFxPropertyType(t: KType): KType? {
   val c = t.classifier
   if (c is KClass<*>) {
      val isProperty = c.isSubclassOf<ObservableValue<*>>()
      if (!isProperty) return t
   }
   if (c is KTypeParameter) {
      val isProperty = c is KClass<*> && c.isSubclassOf<ObservableValue<*>>()
      if (!isProperty) return t
   }
   val gpt = getFirstGenericArgument(t)

   // Workaround for number properties returning Number.class, due to implementing Property<Number>.
   if (gpt?.raw?.isSubclassOf<Number>()==true) {
      val typename = t.raw.jvmName
      if ("Double" in typename) return type<Double>().type.withNullability(gpt.isMarkedNullable)
      if ("Integer" in typename) return type<Int>().type.withNullability(gpt.isMarkedNullable)
      if ("Float" in typename) return type<Float>().type.withNullability(gpt.isMarkedNullable)
      if ("Long" in typename) return type<Double>().type.withNullability(gpt.isMarkedNullable)
   }
   return gpt
}

fun getFirstGenericArgument(t: KType): KType? = when (val c = t.classifier) {
   is KTypeParameter -> c.upperBounds.firstOrNull()
   is KClass<*> -> t.arguments.firstOrNull()?.type ?: Util.getGenericPropertyTypeImpl(t.javaType)?.toRaw()?.kotlin?.createType(nullable = true)
   else -> null
}