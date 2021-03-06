package sp.it.util.type

import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Locale
import java.util.Optional
import java.util.Stack
import java.util.logging.Level
import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.MapChangeListener
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.effect.Blend
import javafx.scene.effect.Effect
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.KVariance
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.recurseBF
import sp.it.util.functional.recurseDF
import sp.it.util.functional.traverse
import sp.it.util.text.decapital
import sp.it.util.type.JavafxPropertyType.JavafxDoublePropertyType
import sp.it.util.type.JavafxPropertyType.JavafxFloatPropertyType
import sp.it.util.type.JavafxPropertyType.JavafxIntegerPropertyType
import sp.it.util.type.JavafxPropertyType.JavafxLongPropertyType
import sp.it.util.type.PaneProperties.paneProperty

private val logger = KotlinLogging.logger {}

fun Class<*>.isSuperclassOf(type: Class<*>) = isAssignableFrom(type)

fun Class<*>.isSubclassOf(type: Class<*>) = type.isSuperclassOf(this)

fun KClass<*>.isSuperclassOf(type: Class<*>) = isSuperclassOf(type.kotlin)

fun KClass<*>.isSubclassOf(type: Class<*>) = isSubclassOf(type.kotlin)

inline fun <reified T> KClass<*>.isSuperclassOf() = isSuperclassOf(T::class)

inline fun <reified T> KClass<*>.isSubclassOf() = isSubclassOf(T::class)

inline fun <reified T> Class<*>.isSuperclassOf() = isSuperclassOf(T::class.java)

inline fun <reified T> Class<*>.isSubclassOf() = isSubclassOf(T::class.java)

/** @return the most specific common supertype of the specified types */
infix fun KClass<*>.union(type: KClass<*>): KClass<*> = when {
   this==type -> this
   this==Any::class || type==Any::class -> Any::class
   this.isSuperclassOf(type) -> this
   this.isSubclassOf(type) -> type
   else -> null
      ?: java.traverse { it.superclass }.filter { it != Any::class.java }.find { it.isSuperclassOf(type.java) }?.kotlin
      ?: recurseDF { it.superclasses.asIterable() }.filter { it != Any::class }.find { it.isSuperclassOf(type) }
      ?: Any::class
}

inline fun <reified T: Annotation> Class<*>.findAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T: Annotation> Field.findAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T: Annotation> Method.findAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T: Annotation> KProperty<*>.findAnnotationAny(): T? = findAnnotation() ?: getter.findAnnotation() ?: javaField?.findAnnotation() ?: javaGetter?.findAnnotation()

/** Set [java.util.logging.Logger] of specified package to specified level. Helpful for stubborn libraries. */
fun setLoggingLevelForPackage(logPackage: Package, logLevel: Level) {
   java.util.logging.Logger.getLogger(logPackage.name).apply {
      level = logLevel
      useParentHandlers = false
   }
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
 * Flattens a type to individual type fragments represented by jvm classes, removing variance (projections) and nullability.
 *
 * Examples:
 *
 * `Any` -> [Any.class]
 * `Any?` -> [Any.class]
 * `List<*>` -> [List.class, Any.class]
 * `List<Int?>` -> [List.class, Int.class]
 * `MutableList<out Int>` -> [List.class, Int.class]
 * `MutableList<in Int?>` -> [List.class, Int.class]
 * `MutableList<Int>` -> [List.class, Int.class]
 * `ArrayList<Int>` -> [ArrayList.class, Int.class]
 *
 * @return sequence of classes representing the specified type and its generic type arguments
 */
fun KType.toRawFlat(): Sequence<KClass<*>> = recurseDF { it.arguments.map { it.type ?: kType<Any?>() } }.mapNotNull { it.raw }

/** Set specified property of this object to null. Use for disposal of read-only properties and avoiding memory leaks. */
infix fun Any.nullify(property: KProperty<*>) {
   property.javaField?.isAccessible = true
   property.javaField?.set(this, null)
}

/**
 * Returns sequence of class' all superclasses and interfaces in breadth first declaration order.
 * * For every declaration, super class precedes super interfaces, except for [Any], which (as the most generic) is always the last element.
 * * Interfaces, [Nothing], [Unit] still inherit from [Any] and will return it also
 * * May contain duplicates, if interface is inherited multiple times within the hierarchy or if the type is erased in Kotlin (such as [MutableList] erases to [List].
 */
fun KClass<*>.superKClasses(): Sequence<KClass<*>> = superKClassesInc().drop(1)

/**
 * Returns sequence of this class, its all superclasses and interfaces in breadth first declaration order.
 * * For every declaration, super class precedes super interfaces, except for [Any], which (as the most generic) is always the last element.
 * * Interfaces, [Nothing], [Unit] still inherit from [Any] and will return it also
 * * May contain duplicates, if interface is inherited multiple times within the hierarchy or if the type is erased in Kotlin (such as [MutableList] erases to [List].
 */
fun KClass<*>.superKClassesInc(): Sequence<KClass<*>> = when(this) {
   Any::class -> sequenceOf(Any::class)
   Nothing::class -> sequenceOf(Nothing::class, Any::class)
   Unit::class -> sequenceOf(Unit::class, Any::class)
   else -> java.recurseBF { listOfNotNull(it.superclass) + it.interfaces }.map { it.kotlin }.filter { it != Any::class } + Any::class
   // recurse { it.superclasses }   // TODO: KClass.superclasses is bugged for anonymous Java classes
}

/**
 * Execute action for each observable value representing a javafx property of an object o.
 * Additional provided arguments are name of the property and its non-erased generic type.
 * Javafx properties are obtained from public nameProperty() methods using reflection.
 */
fun forEachJavaFXProperty(o: Any, action: (Observable, String, KType) -> Unit) {
   // Best we can do to stop platform type from spreading around... This is better then nothing.
   fun KType.resolveNullability(name: String): KType = when {
      this.isPlatformType -> when {
         o is Effect && name == "input"-> this.withNullability(true)
         o is Effect && name == "topInput"-> this.withNullability(true)
         o is Effect && name == "bottomInput"-> this.withNullability(true)
         o is Blend && name == "mode"-> this.withNullability(false)
         name == "opacity"-> this.withNullability(false)
         else -> this
      }
      else -> this
   }

   for (method in o::class.memberFunctions) {
      val methodName = method.name
      val isPublished = method.visibility==PUBLIC && !methodName.startsWith("impl")
      if (isPublished) {
         var propertyName: String? = null
         if (method.returnType.isSubtypeOf<Observable>()) {
            try {
               propertyName = methodName
               propertyName = propertyName.substringBeforeLast("Property", propertyName)
               propertyName = propertyName.substringAfter("get", propertyName)
               propertyName = propertyName.decapital()
               method.isAccessible = true
               var observable = method.call(o) as Observable?
               if (observable is Property<*> && observable.isBound) {
                  val rop = ReadOnlyObjectWrapper<Any>()
                  rop.bind(observable)
                  observable = rop.readOnlyProperty
               }
               val propertyType = method.returnType.javaFxPropertyType.resolveNullability(propertyName)
               if (observable!=null) {
                  action(observable, propertyName, propertyType)
               } else {
                  logger.warn { "Is null property='$observable' propertyName=$propertyName propertyType=$propertyType" }
               }
            } catch (e: Throwable) {
               logger.error(e) { "Could not obtain property '$propertyName' from class ${o::class} of object $o" }
            }
         }
      }
   }
   for (field in o::class.memberProperties) {
      val fieldName = field.name
      val isPublished = field.visibility==PUBLIC && !fieldName.startsWith("impl")
      if (isPublished) {
         if (field.returnType.isSubtypeOf<Observable>()) {
            try {
               field.isAccessible = true
               var observable = field.getter.call(o) as Observable?
               if (observable is Property<*> && observable.isBound) {
                  val rop = ReadOnlyObjectWrapper<Any>()
                  rop.bind(observable)
                  observable = rop.readOnlyProperty
               }
               val propertyType = field.returnType.javaFxPropertyType.resolveNullability(fieldName)
               if (observable!=null) {
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
   @Suppress("SpellCheckingInspection")
   if (o is Node) {
      when (o.parent) {
         is StackPane -> {
            action(paneProperty(o, "stackpane-alignment", StackPane::getAlignment, StackPane::setAlignment), "L: Alignment", type<Pos?>().type)
            action(paneProperty(o, "stackpane-margin", StackPane::getMargin, StackPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is AnchorPane -> {
            action(paneProperty(o, "pane-top-anchor", AnchorPane::getTopAnchor, AnchorPane::setTopAnchor), "L: Anchor (top)", type<Double?>().type)
            action(paneProperty(o, "pane-right-anchor", AnchorPane::getRightAnchor, AnchorPane::setRightAnchor), "L: Anchor (right)", type<Double?>().type)
            action(paneProperty(o, "pane-bottom-anchor", AnchorPane::getBottomAnchor, AnchorPane::setBottomAnchor), "L: Anchor (bottom)", type<Double?>().type)
            action(paneProperty(o, "pane-left-anchor", AnchorPane::getLeftAnchor, AnchorPane::setLeftAnchor), "L: Anchor (left)", type<Double?>().type)
         }
         is VBox -> {
            action(paneProperty(o, "vbox-vgrow", VBox::getVgrow, VBox::setVgrow), "L: VGrow", type<Priority?>().type)
            action(paneProperty(o, "vbox-margin", VBox::getMargin, VBox::setMargin), "L: Margin", type<Insets?>().type)
         }
         is HBox -> {
            action(paneProperty(o, "hbox-vgrow", HBox::getHgrow, HBox::setHgrow), "L: HGrow", type<Priority?>().type)
            action(paneProperty(o, "hbox-margin", HBox::getMargin, HBox::setMargin), "L: Margin", type<Insets?>().type)
         }
         is FlowPane -> {
            action(paneProperty(o, "flowpane-margin", FlowPane::getMargin, FlowPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is BorderPane -> {
            action(paneProperty(o, "borderpane-alignment", BorderPane::getAlignment, BorderPane::setAlignment), "L: Alignment", type<Pos?>().type)
            action(paneProperty(o, "borderpane-margin", BorderPane::getMargin, BorderPane::setMargin), "L: Margin", type<Insets?>().type)
         }
         is GridPane -> {
            action(paneProperty(o, "gridpane-column", GridPane::getColumnIndex, GridPane::setColumnIndex), "L: Column index", type<Int?>().type)
            action(paneProperty(o, "gridpane-column-span", GridPane::getColumnSpan, GridPane::setColumnSpan), "L: Column span", type<Int?>().type)
            action(paneProperty(o, "gridpane-row", GridPane::getRowIndex, GridPane::setRowIndex), "L: Row index", type<Int?>().type)
            action(paneProperty(o, "gridpane-row-span", GridPane::getRowSpan, GridPane::setRowSpan), "L: Row span", type<Int?>().type)
            action(paneProperty(o, "gridpane-valignment", GridPane::getValignment, GridPane::setValignment), "L: Valignment", type<VPos?>().type)
            action(paneProperty(o, "gridpane-halignment", GridPane::getHalignment, GridPane::setHalignment), "L: Halignment", type<HPos?>().type)
            action(paneProperty(o, "gridpane-vgrow", GridPane::getVgrow, GridPane::setVgrow), "L: Vgrow", type<Priority?>().type)
            action(paneProperty(o, "gridpane-hgrow", GridPane::getHgrow, GridPane::setHgrow), "L: Hgrow", type<Priority?>().type)
            action(paneProperty(o, "gridpane-margin", HBox::getMargin, HBox::setMargin), "L: Margin", type<Insets?>().type)
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


/** [KTypeProjection.type] without variance. [KTypeProjection.STAR] resolves to non null [Nothing] */
val KTypeProjection.typeResolved: KType
   get() = type ?: kTypeNothingNonNull()

/** True if this class is subclass of one of the top lvl javafx property interfaces, i.e [ObservableValue] or [WritableValue] */
val KClass<*>.isJavaFxObservableOrWritableValue
   get() = isSubclassOf<ObservableValue<*>>() || isSubclassOf<WritableValue<*>>()

/** True if this type represents platform type (denoted by !), so nullability is left unspecified */
val KType.isPlatformType: Boolean
   get() = toString().endsWith("!")

/** Type after unwrapping javafx property value types, (see [isJavaFxObservableOrWritableValue]). */
val KType.javaFxPropertyType: KType
   get() = when {
         raw.isJavaFxObservableOrWritableValue -> {
            val pt = when {
               raw.isSubclassOf<ObservableValue<*>>() -> argOf(ObservableValue::class, 0).typeResolved
               raw.isSubclassOf<WritableValue<*>>() -> argOf(WritableValue::class, 0).typeResolved
               else -> fail()
            }

            // Workaround for number properties returning Number.class, due to implementing Property<Number>.
            when {
               pt.raw.isSubclassOf<Number>() -> {
                  val typename = raw.jvmName
                  when {
                     "Integer" in typename -> type<JavafxIntegerPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeResolved
                     "Float" in typename -> type<JavafxFloatPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeResolved
                     "Long" in typename -> type<JavafxLongPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeResolved
                     "Double" in typename -> type<JavafxDoublePropertyType>().type.argOf(JavafxPropertyType::class, 0).typeResolved
                     else -> pt
                  }
               }
               else -> pt
            }
         }
         else -> this
      }


fun KType.withPlatformTypeNullability(nullable: Boolean): KType {
   return classifier
      ?.net {
         it.createType(
            arguments.map { it.copy(type = it.type?.withPlatformTypeNullability(nullable)) },
            if (isPlatformType) nullable else this.isMarkedNullable,
            annotations
         )
      }
      ?: this
}

/**
 * Because `in Nothing` and `out Any?` and `*` are equal, the result is normalized to `*`, i.e [KTypeProjection.STAR].
 */
fun KType.argOf(argType: KClass<*>, i: Int): KTypeProjection {
   return when (val c = classifier) {
      is KTypeParameter -> fail { "Type parameter not a candidate $this $i" }
      is KClass<*> -> {
         val argument = when (c) {
            argType -> arguments.getOrNull(i)
               ?.let {
                  when (it.variance) {
                     KVariance.IN, KVariance.OUT, null -> it
                     KVariance.INVARIANT -> when {
                        argType.isSubclassOf<Iterable<*>>() -> {
                           if (this.toString().startsWith("kotlin.collections.Mutable")) it
                           else it.copy(variance = KVariance.OUT)
                        }
                        argType.isSubclassOf<Map<*, *>>() && i==1 -> {
                           if (this.toString().startsWith("kotlin.collections.Mutable")) it
                           else it.copy(variance = KVariance.OUT)
                        }
                        else -> it
                     }
                  }
               }
               ?: fail { "Not found $this $i" }
            else -> {
               val st = c.allSupertypes
               //val stack = c.traverseToSuper(argType)
               st.find { it.classifier==argType }?.arguments?.getOrNull(i)
                  ?.let {
                     when (it.variance) {
                        KVariance.IN, KVariance.OUT, null -> it
                        KVariance.INVARIANT -> it //KTypeProjection(c.typeParameters.get().variance, it.type)
                     }
                  }
                  ?.let {
                     when (val at = it.type?.classifier) {
                        is KTypeParameter -> {
                           val argumentI = raw.typeParameters.indexOfFirst { it.name==at.name }
                           val argument = arguments[argumentI]
                           argument
                        }
                        else -> it
                     }
                  }
                  ?: fail { "Not found $argType $i" }
            }
         }
         argument
      }
      else -> fail { "Unknown error" }
   }.let {
      when {
         it.variance==KVariance.OUT && it.type==Any::class.createType(nullable = true) -> STAR
         it.variance==KVariance.IN && it.type==typeNothingNonNull().type -> STAR
         else -> it
      }
   }
}

/** @return stack of this class at the bottom and its superclasses in order until the specified superclass on top */
fun KClass<*>.traverseToSuper(ss: KClass<*>, stack: Stack<KClass<*>> = Stack()): Stack<KClass<*>> {
   stack.push(this)
   superclasses.forEach {
      if (stack.peek()==ss) return stack
      it.traverseToSuper(ss, stack)
   }
   if (stack.peek()!=ss) stack.pop()
   return stack
}

/** @return true iff this and the other [KTypeProjection] represent the same type */
infix fun KTypeProjection.isSame(c: KTypeProjection): Boolean = variance == c.variance && type isSame c.type

/** @return true iff this and the other [KClassifier] represent the same type */
@Suppress("SimplifyBooleanWithConstants")
infix fun KClassifier?.isSame(c: KClassifier?): Boolean = when {
   this == null && c == null -> true
   this is KClass<*> && c is KClass<*> -> this == c
   this is KTypeParameter && c is KTypeParameter -> true &&
      name == c.name &&
      variance == c.variance &&
      (upperBounds.asSequence() zip c.upperBounds.asSequence()).all { (a, b) -> a isSame b }
   else -> false
}

/** @return true iff this and the other [KType] represent the same type */
@Suppress("SimplifyBooleanWithConstants")
infix fun KType?.isSame(t: KType?): Boolean = when {
   this == null && t == null -> true
   this != null && t != null -> true &&
      isMarkedNullable == t.isMarkedNullable &&
      classifier isSame t.classifier &&
      (arguments.asSequence() zip t.arguments.asSequence()).all { (a, b) -> a isSame b }
   else -> false
}

/** @return true iff this and the other [VType] represent the same type */
infix fun VType<*>.isSame(t: VType<*>): Boolean = type isSame t.type

/**
 * Return best common super type estimate for the elements of this list for reading the list.
 *
 * Non-generic elements are estimated completely.
 * If any generic type parameter fails to be determined correctly, [KTypeProjection.STAR] will be used.
 *
 * Subsequent type estimates of the elements are resolved into best common super type.
 *
 * The returned type is guaranteed to be both run-time and compile-time compatible with every element, i.e,
 * each element can be assumed to be of the returned type as it is assignable to it.
 * However, this collection may not support elements of the returned type, as it may be narrower.
 */
// TODO: remove and merge with T.estimateRuntimeType
fun <T> Collection<T>.estimateRuntimeType(): VType<T> =
   if (isEmpty()) typeNothingNonNull()
   else asSequence().map { it.estimateRuntimeType() }.reduce { a,b ->
      when {
         a isSame typeNothingNonNull() -> b
         a isSame typeNothingNullable() -> b.nullable().asIs()
         b isSame typeNothingNonNull() -> a
         b isSame typeNothingNullable() -> a.nullable().asIs()
         a isSame b -> a
         a isSubtypeOf b -> b
         b isSubtypeOf a -> a
         // handles cases like Option<A>, Option<B> -> Option<supertype of A and B>
         // TODO: change == to isSuperClassOf
         b.jvmErasure == a.jvmErasure -> {
            VType(
               b.type.raw.createType(
                  b.type.raw.asIs<KClass<*>>().typeParameters.mapIndexed { i, _ ->
                     val ac = a.type.arguments[i].type?.raw ?: Any::class
                     val bc = b.type.arguments[i].type?.raw ?: Any::class
                     val uc = ac union bc
                     invariant(uc.createType(uc.typeParameters.map { STAR }))
                  },
                  b.isNullable || a.isNullable
               )
            )
         }
         else -> {
            a.type.raw
               .allSupertypes.asSequence()
               .flatMap {
                  sequence {
                     val c = it.raw
                     if (c != Any::class) {
                        yield(it)
                     // TODO: yield all parameter combinations by iterating each through subtypes
                        yield(c.createType(it.arguments.map { STAR }, it.isMarkedNullable, it.annotations))
                     }
                  }
               }
               .firstOrNull { VType<T>(it) isSupertypeOf b }?.net { VType(it) }
               ?: run {
                  if (a.isNullable || b.isNullable) type<Any?>().asIs()
                  else type<Any>().asIs()
               }
         }
      }
   }

/**
 * Return best type estimate for this object.
 *
 * Non-generic types are estimated completely.
 * If any generic type parameter fails to be determined correctly, [KTypeProjection.STAR] will be used.
 *
 * The returned type is guaranteed to be both run-time and compile-time compatible with this value, i.e.,
 * this value can be assumed to be of the returned type as it is assignable to it.
 * However if this value is mutable, it may not be the case after mutating.
 */
@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
fun <T> T.estimateRuntimeType(): VType<T> = when (this) {
   null -> typeNothingNullable().asIs()
   is Optional<*> -> VType(Optional::class.createType(listOf(invariant(map { it.estimateRuntimeType().type }.orElse(kTypeNothingNonNull())))))
   is Try.Ok<*> -> VType(Try.Ok::class.createType(listOf(invariant(value.estimateRuntimeType().type))))
   is Try.Error<*> -> VType(Try.Error::class.createType(listOf(invariant(value.estimateRuntimeType().type))))
   is Collection<*> -> {
      val c = this!!::class
      val cp = c.typeParameters
      when (cp.size) {
         0 -> VType(c.createType(listOf()))
         1 -> VType(c.createType(c.typeParameters.map { KTypeProjection(it.variance, this.asIs<Collection<*>>().estimateRuntimeType().type) }))
         else -> VType(c.createType(c.typeParameters.map { STAR }))
      }
   }
   else -> {
      val c = this!!::class
      VType(c.createType(c.typeParameters.map { STAR }))
   }
}