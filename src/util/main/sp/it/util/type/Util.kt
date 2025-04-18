package sp.it.util.type

import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Optional
import java.util.Stack
import java.util.logging.Level
import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.css.PseudoClass
import javafx.event.EventDispatcher
import javafx.geometry.Bounds
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.NodeOrientation
import javafx.geometry.Point3D
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.AccessibleRole
import javafx.scene.CacheHint
import javafx.scene.Cursor
import javafx.scene.DepthTest
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ContextMenu
import javafx.scene.control.OverrunStyle
import javafx.scene.control.Skin
import javafx.scene.control.Tooltip
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.Effect
import javafx.scene.input.InputMethodRequests
import javafx.scene.input.KeyCombination
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Background
import javafx.scene.layout.Border
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.scene.shape.PathElement
import javafx.scene.shape.Shape
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.shape.StrokeType
import javafx.scene.text.Font
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import javafx.scene.transform.Transform
import javafx.stage.Window
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.KVariance.IN
import kotlin.reflect.KVariance.INVARIANT
import kotlin.reflect.KVariance.OUT
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName
import org.jetbrains.kotlin.ir.backend.js.export.ExportedType
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.Multiline
import sp.it.util.conf.Constraint.ValueSealedSet
import sp.it.util.conf.Constraint.ValueUnsealedSet
import sp.it.util.dev.fail
import sp.it.util.functional.Option
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.net
import sp.it.util.functional.recurseBF
import sp.it.util.functional.recurseDF
import sp.it.util.functional.runTry
import sp.it.util.functional.traverse
import sp.it.util.text.decapital
import sp.it.util.type.JavafxPropertyType.JavafxDoublePropertyType
import sp.it.util.type.JavafxPropertyType.JavafxFloatPropertyType
import sp.it.util.type.JavafxPropertyType.JavafxIntegerPropertyType
import sp.it.util.type.JavafxPropertyType.JavafxLongPropertyType
import sp.it.util.type.PaneProperties.paneProperty
import sp.it.util.ui.UiDelegate

private val logger = KotlinLogging.logger { }

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
 * `Any` -> [Any::class]
 * `Any?` -> [Any::class]
 * `List<*>` -> [List::class, Any::class]
 * `List<Int?>` -> [List::class, Int::class]
 * `MutableList<out Int>` -> [List::class, Int::class]
 * `MutableList<in Int?>` -> [List::class, Int::class]
 * `MutableList<Int>` -> [List::class, Int::class]
 * `ArrayList<Int>` -> [ArrayList::class, Int::class]
 *
 * @return sequence of classes representing the specified type and its generic type arguments
 */
fun KType.toRawFlat(): Sequence<KClass<*>> = recurseDF { it.arguments.map { it.type ?: kType<Any?>() } }.map { it.raw }

/** Set specified property of this object to null. Use for disposal of read-only properties and avoiding memory leaks. */
infix fun Any.nullify(property: KProperty<*>) {
   property.javaField?.isAccessible = true
   property.javaField?.set(this, null)
}

/**
 * Returns sequence of class' all superclasses and interfaces in breadth first declaration order.
 * * For every declaration, super class precedes super interfaces, except for [Any], which (as the most generic) is always the last element.
 * * Interfaces, [Nothing], [Unit] still inherit from [Any] and will return it also
 * * May contain duplicates, if interface is inherited multiple times within the hierarchy or if the type is erased in Kotlin (such as [MutableList] erases to [List]).
 */
fun KClass<*>.superKClasses(): Sequence<KClass<*>> = superKClassesInc().drop(1)

/**
 * Returns sequence of this class, all its superclasses and interfaces in breadth first declaration order.
 * * For every declaration, super class precedes super interfaces, except for [Any], which (as the most generic) is always the last element.
 * * Interfaces, [Nothing], [Unit] still inherit from [Any] and will return it also
 * * May contain duplicates, if interface is inherited multiple times within the hierarchy or if the type is erased in Kotlin (such as [MutableList] erases to [List]).
 */
fun KClass<*>.superKClassesInc(): Sequence<KClass<*>> = when {
   this==Any::class -> sequenceOf(Any::class)
   this==Nothing::class -> sequenceOf(Nothing::class, Any::class)
   this==Unit::class -> sequenceOf(Unit::class, Any::class)
   else -> {
      val iterated = HashSet<Any?>()
      val all = recurseBF { it.superclasses }.filter { it != Any::class } + Any::class
      sequence {
         all.forEach {
            if (it !in iterated) yield(it)
            iterated += it
         }
      }
   }
}

private val javaFxSizeConstraints = setOf(ValueUnsealedSet<Double>({ listOf(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE) }))
private data class Extractor<T>(val declaringClass: KClass<*>, val method: KFunction<T>, val type: VType<T>, val constraints: Set<Constraint<*>> = setOf())
private val extractors = setOf(
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::accessibleHelpProperty,             type<Property<String?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::accessibleRoleProperty,             type<Property<AccessibleRole>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::accessibleRoleDescriptionProperty,  type<Property<String?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::accessibleTextProperty,             type<Property<String?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::blendModeProperty,                  type<Property<BlendMode?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::boundsInLocalProperty,              type<ReadOnlyProperty<Bounds>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::boundsInParentProperty,             type<ReadOnlyProperty<Bounds>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::cacheProperty,                      type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::cacheHintProperty,                  type<Property<CacheHint>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::clipProperty,                       type<Property<Node?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::cursorProperty,                     type<Property<Cursor?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::depthTestProperty,                  type<Property<DepthTest>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::disableProperty,                    type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::disabledProperty,                   type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::effectProperty,                     type<Property<Effect?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::effectiveNodeOrientationProperty,   type<ReadOnlyProperty<NodeOrientation>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::eventDispatcherProperty,            type<Property<EventDispatcher>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::focusedProperty,                    type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::focusTraversableProperty,           type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::focusVisibleProperty,               type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::focusWithinProperty,                type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::hoverProperty,                      type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::idProperty,                         type<Property<String?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::inputMethodRequestsProperty,        type<Property<InputMethodRequests?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::layoutBoundsProperty,               type<ReadOnlyProperty<Bounds>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::layoutXProperty,                    type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::layoutYProperty,                    type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::localToParentTransformProperty,     type<ReadOnlyProperty<Transform>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::localToSceneTransformProperty,      type<ReadOnlyProperty<Transform>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::managedProperty,                    type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::mouseTransparentProperty,           type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::nodeOrientationProperty,            type<Property<NodeOrientation>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::opacityProperty,                    type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::parentProperty,                     type<ReadOnlyProperty<Node?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::pickOnBoundsProperty,               type<Property<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::pressedProperty,                    type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::getProperties,                      type<ObservableMap<Any?, Any?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::getPseudoClassStates,               type<ObservableSet<PseudoClass>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::rotateProperty,                     type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::rotationAxisProperty,               type<Property<Point3D>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::scaleXProperty,                     type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::scaleYProperty,                     type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::scaleZProperty,                     type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::sceneProperty,                      type<ReadOnlyProperty<Scene?>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::styleProperty,                      type<Property<String>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::getStyleClass,                      type<ObservableList<String>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::getTransforms,                      type<ObservableList<Transform>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::translateXProperty,                 type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::translateYProperty,                 type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::translateZProperty,                 type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::viewOrderProperty,                  type<Property<Double>>()),
      Extractor(javafx.scene.Node::class,            javafx.scene.Node::visibleProperty,                    type<Property<Boolean>>()),
      Extractor(javafx.scene.Parent::class,          javafx.scene.Parent::cacheHintProperty,                type<Property<CacheHint>>()),
      Extractor(javafx.scene.Parent::class,          javafx.scene.Parent::needsLayoutProperty,              type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.Parent::class,          javafx.scene.Parent::getStylesheets,                   type<ObservableList<String>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::backgroundProperty,        type<Property<Background?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::borderProperty,            type<Property<Border?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::blendModeProperty,         type<Property<BlendMode?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::cacheShapeProperty,        type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::centerShapeProperty,       type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::getChildrenUnmodifiable,   type<ObservableList<Node>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::insetsProperty,            type<ReadOnlyProperty<Insets?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::widthProperty,             type<ReadOnlyProperty<Double>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::heightProperty,            type<ReadOnlyProperty<Double>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::minHeightProperty,         type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::minWidthProperty,          type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::prefHeightProperty,        type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::prefWidthProperty,         type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::maxHeightProperty,         type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::maxWidthProperty,          type<Property<Double>>(), javaFxSizeConstraints),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::opaqueInsetsProperty,      type<Property<Insets?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::scaleShapeProperty,        type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::shapeProperty,             type<Property<Shape?>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::snapToPixelProperty,       type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.Region::class,   javafx.scene.layout.Region::paddingProperty,           type<Property<Insets>>()),
      Extractor(javafx.scene.layout.Pane::class,     javafx.scene.layout.Pane::backgroundProperty,          type<Property<Background?>>()),
      Extractor(javafx.scene.layout.Pane::class,     javafx.scene.layout.Pane::getChildren,                 type<ObservableList<Node>>()),
      Extractor(javafx.scene.layout.HBox::class,     javafx.scene.layout.HBox::alignmentProperty,           type<Property<Pos>>()),
      Extractor(javafx.scene.layout.HBox::class,     javafx.scene.layout.HBox::fillHeightProperty,          type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.HBox::class,     javafx.scene.layout.HBox::spacingProperty,             type<Property<Double>>()),
      Extractor(javafx.scene.layout.VBox::class,     javafx.scene.layout.VBox::alignmentProperty,           type<Property<Pos>>()),
      Extractor(javafx.scene.layout.VBox::class,     javafx.scene.layout.VBox::fillWidthProperty,           type<Property<Boolean>>()),
      Extractor(javafx.scene.layout.VBox::class,     javafx.scene.layout.VBox::spacingProperty,             type<Property<Double>>()),
      Extractor(javafx.scene.control.Control::class, javafx.scene.control.Control::contextMenuProperty,     type<Property<ContextMenu?>>()),
      Extractor(javafx.scene.control.Control::class, javafx.scene.control.Control::skinProperty,            type<Property<Skin<*>?>>()),
      Extractor(javafx.scene.control.Control::class, javafx.scene.control.Control::tooltipProperty,         type<Property<Tooltip?>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::alignmentProperty,       type<Property<Pos>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::contentDisplayProperty,  type<Property<ContentDisplay>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::ellipsisStringProperty,  type<Property<String>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::textTruncatedProperty,   type<ReadOnlyProperty<Boolean>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::fontProperty,            type<Property<Font>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::graphicProperty,         type<Property<Node?>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::graphicTextGapProperty,  type<Property<Double>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::labelPaddingProperty,    type<ReadOnlyProperty<Insets>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::lineSpacingProperty,     type<Property<Double>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::mnemonicParsingProperty, type<Property<Boolean>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::textProperty,            type<Property<String?>>(), setOf(Multiline)),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::textAlignmentProperty,   type<Property<TextAlignment>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::textFillProperty,        type<Property<Paint>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::textOverrunProperty,     type<Property<OverrunStyle>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::underlineProperty,       type<Property<Boolean>>()),
      Extractor(javafx.scene.control.Labeled::class, javafx.scene.control.Labeled::wrapTextProperty,        type<Property<Boolean>>()),
      Extractor(javafx.scene.control.Label::class,   javafx.scene.control.Label::labelForProperty,          type<Property<Node?>>()),
      Extractor(javafx.scene.control.TextArea::class,javafx.scene.control.TextArea::textProperty,           type<Property<String?>>(), setOf(Multiline)),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::fillProperty,                type<Property<Paint>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::smoothProperty,              type<Property<Boolean>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeProperty,              type<Property<Paint?>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeDashOffsetProperty,    type<Property<Double>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeLineCapProperty,       type<Property<StrokeLineCap>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeLineJoinProperty,      type<Property<StrokeLineJoin>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeMiterLimitProperty,    type<Property<Double>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeTypeProperty,          type<Property<StrokeType>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::strokeWidthProperty,         type<Property<Double>>()),
      Extractor(javafx.scene.shape.Shape::class,     javafx.scene.shape.Shape::smoothProperty,              type<Property<Boolean>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::baselineOffsetProperty,        type<ReadOnlyProperty<Double>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::boundsTypeProperty,            type<Property<TextBoundsType>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::caretBiasProperty,             type<Property<Boolean>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::caretPositionProperty,         type<Property<Int>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::caretShapeProperty,            type<ReadOnlyProperty<kotlin.Array<PathElement>>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::fontProperty,                  type<Property<Font>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::fontSmoothingTypeProperty,     type<Property<FontSmoothingType>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::lineSpacingProperty,           type<Property<Double>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::selectionEndProperty,          type<Property<Int>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::selectionFillProperty,         type<Property<Paint>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::selectionShapeProperty,        type<ReadOnlyProperty<kotlin.Array<PathElement>>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::selectionStartProperty,        type<Property<Int>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::strikethroughProperty,         type<Property<Boolean>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::tabSizeProperty,               type<Property<Int>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::textAlignmentProperty,         type<Property<TextAlignment>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::textOriginProperty,            type<Property<VPos>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::textProperty,                  type<Property<String>>(), setOf(Multiline)),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::underlineProperty,             type<Property<Boolean>>()),
      Extractor(javafx.scene.text.Text::class,       javafx.scene.text.Text::wrappingWidthProperty,         type<Property<Double>>()),
      Extractor(javafx.stage.Stage::class,           javafx.stage.Stage::fullScreenExitHintProperty,        type<Property<String?>>()),
      Extractor(javafx.stage.Stage::class,           javafx.stage.Stage::fullScreenExitKeyProperty,         type<Property<KeyCombination?>>()),
   )
   .groupBy { it.declaringClass }
   .mapValues { (_, v) -> v.associateBy { it.method.name } }

/**
 * @return all observable javafx properties of the specified object in no particular order
 * Includes name of the property and its non-erased generic type as well as properties of object's [UiDelegate]
 */
fun forEachJavaFXProperty(o: Any): Sequence<InspectedFxProperty> =
   forEachJavaFXPropertyImpl(o) + UiDelegate.of(o)?.net(::forEachJavaFXPropertyImpl).orEmpty()

private fun forEachJavaFXPropertyImpl(o: Any): Sequence<InspectedFxProperty> = sequence {

   fun isBlacklisted(instanceClass: KClass<*>, methodName: String) = Pane::class.isSuperclassOf(instanceClass) && methodName=="getChildrenUnmodifiable"

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

   val instanceClass = o::class
      // TODO: workaround for https://youtrack.jetbrains.com/issue/KT-41373 && https://youtrack.jetbrains.com/issue/KT-22792
      .net { c -> runTry { c.objectInstance }.map { c }.getOrSupply { c.resolveAnonymous() } }
   instanceClass.superKClassesInc().filter { !it.java.isInterface }.forEach { declaringClass ->
      declaringClass.declaredMemberFunctions.forEach { method ->
         val methodName = method.name
         val isPublished = method.visibility==PUBLIC && !methodName.startsWith("impl")
         if (isPublished && !isBlacklisted(instanceClass, methodName)) {
            var propertyName: String? = null
            val returnType = method.returnType
            if (returnType.isSubtypeOf<Observable>()) {
               try {
                  propertyName = methodName
                  propertyName = propertyName.substringBeforeLast("Property", propertyName)
                  propertyName = propertyName.substringAfter("get", propertyName)
                  propertyName = propertyName.decapital()
                  method.isAccessible = true
                  val extractor = extractors[declaringClass].orEmpty()[methodName]
                  val propertyType = extractor?.type?.type?.javaFxPropertyType ?: returnType.javaFxPropertyType.resolveNullability(propertyName)
                  val observableRaw = method.call(o) as Observable?
                  if (observableRaw!=null) {
                     val observable = {
                        if (observableRaw is Property<*> && observableRaw.isBound) {
                           val rop = ReadOnlyObjectWrapper<Any>()
                           rop.bind(observableRaw)
                           rop.readOnlyProperty
                        } else {
                           observableRaw
                        }
                     }
                     val isReadOnly = when {
                        observableRaw is ObservableList<*> || observableRaw is ObservableSet<*> || observableRaw is ObservableMap<*,*> -> observableRaw::class.jvmName.contains("unmodifiable", true)
                        else -> !returnType.raw.isSubclassOf(WritableValue::class) || (observableRaw is Property<*> && observableRaw.isBound)
                     }
                     yield(InspectedFxProperty(observable, propertyName, isReadOnly, declaringClass, propertyType, extractor?.constraints.orEmpty()))
                  }
                  else logger.warn { "Is null declaringClass='$declaringClass' propertyName=$propertyName propertyType=$propertyType" }
               } catch (e: Throwable) {
                  logger.error(e) { "Could not obtain property '$propertyName' from class ${o::class} of object $o" }
               }
            }
         }
      }

      declaringClass.declaredMemberProperties.forEach { field ->
         val fieldName = field.name
         val isPublished = field.visibility==PUBLIC && !fieldName.startsWith("impl")
         if (isPublished) {
            val returnType = field.returnType
            if (returnType.isSubtypeOf<Observable>()) {
               try {
                  field.isAccessible = true
                  val propertyType = returnType.javaFxPropertyType.resolveNullability(fieldName)
                  val observableRaw = field.getter.call(o) as Observable?
                  if (observableRaw!=null) {
                     val observable = {
                        if (observableRaw is Property<*> && observableRaw.isBound) {
                           val rop = ReadOnlyObjectWrapper<Any>()
                           rop.bind(observableRaw)
                           rop.readOnlyProperty
                        } else {
                           observableRaw
                        }
                     }
                     val isReadOnly = when {
                        observableRaw is ObservableList<*> || observableRaw is ObservableSet<*> || observableRaw is ObservableMap<*,*> -> observableRaw::class.jvmName.contains("unmodifiable", true)
                        else -> !returnType.raw.isSubclassOf(WritableValue::class) || (observableRaw is Property<*> && observableRaw.isBound)
                     }
                     yield(InspectedFxProperty(observable, fieldName, isReadOnly, declaringClass, propertyType))
                  }
                  else logger.warn { "Is null declaringClass='$declaringClass' propertyName=$fieldName propertyType=$propertyType" }
               } catch (e: IllegalAccessException) {
                  logger.error(e) { "Could not obtain property '$fieldName' from object" }
               }
            }
         }
      }
   }


   // add synthetic javafx layout properties for nodes in scene graph
   @Suppress("SpellCheckingInspection")
   if (o is Node) {
      when (o.parent) {
         is StackPane -> {
            yield(InspectedFxProperty(paneProperty(o, "stackpane-alignment", StackPane::getAlignment, StackPane::setAlignment), "L: Alignment", false, StackPane::class, type<Pos?>().type))
            yield(InspectedFxProperty(paneProperty(o, "stackpane-margin", StackPane::getMargin, StackPane::setMargin), "L: Margin", false, StackPane::class, type<Insets?>().type))
         }
         is AnchorPane -> {
            yield(InspectedFxProperty(paneProperty(o, "pane-top-anchor", AnchorPane::getTopAnchor, AnchorPane::setTopAnchor), "L: Anchor (top)", false, AnchorPane::class, type<Double?>().type))
            yield(InspectedFxProperty(paneProperty(o, "pane-right-anchor", AnchorPane::getRightAnchor, AnchorPane::setRightAnchor), "L: Anchor (right)", false, AnchorPane::class, type<Double?>().type))
            yield(InspectedFxProperty(paneProperty(o, "pane-bottom-anchor", AnchorPane::getBottomAnchor, AnchorPane::setBottomAnchor), "L: Anchor (bottom)", false, AnchorPane::class, type<Double?>().type))
            yield(InspectedFxProperty(paneProperty(o, "pane-left-anchor", AnchorPane::getLeftAnchor, AnchorPane::setLeftAnchor), "L: Anchor (left)", false, AnchorPane::class, type<Double?>().type))
         }
         is VBox -> {
            yield(InspectedFxProperty(paneProperty(o, "vbox-vgrow", VBox::getVgrow, VBox::setVgrow), "L: VGrow", false, VBox::class, type<Priority?>().type))
            yield(InspectedFxProperty(paneProperty(o, "vbox-margin", VBox::getMargin, VBox::setMargin), "L: Margin", false, VBox::class, type<Insets?>().type))
         }
         is HBox -> {
            yield(InspectedFxProperty(paneProperty(o, "hbox-vgrow", HBox::getHgrow, HBox::setHgrow), "L: HGrow", false, HBox::class, type<Priority?>().type))
            yield(InspectedFxProperty(paneProperty(o, "hbox-margin", HBox::getMargin, HBox::setMargin), "L: Margin", false, HBox::class, type<Insets?>().type))
         }
         is FlowPane -> {
            yield(InspectedFxProperty(paneProperty(o, "flowpane-margin", FlowPane::getMargin, FlowPane::setMargin), "L: Margin", false, FlowPane::class, type<Insets?>().type))
         }
         is BorderPane -> {
            yield(InspectedFxProperty(paneProperty(o, "borderpane-alignment", BorderPane::getAlignment, BorderPane::setAlignment), "L: Alignment", false, BorderPane::class, type<Pos?>().type))
            yield(InspectedFxProperty(paneProperty(o, "borderpane-margin", BorderPane::getMargin, BorderPane::setMargin), "L: Margin", false, BorderPane::class, type<Insets?>().type))
         }
         is GridPane -> {
            yield(InspectedFxProperty(paneProperty(o, "gridpane-column", GridPane::getColumnIndex, GridPane::setColumnIndex), "L: Column index", false, GridPane::class, type<Int?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-column-span", GridPane::getColumnSpan, GridPane::setColumnSpan), "L: Column span", false, GridPane::class, type<Int?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-row", GridPane::getRowIndex, GridPane::setRowIndex), "L: Row index", false, GridPane::class, type<Int?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-row-span", GridPane::getRowSpan, GridPane::setRowSpan), "L: Row span", false, GridPane::class, type<Int?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-valignment", GridPane::getValignment, GridPane::setValignment), "L: Valignment", false, GridPane::class, type<VPos?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-halignment", GridPane::getHalignment, GridPane::setHalignment), "L: Halignment", false, GridPane::class, type<HPos?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-vgrow", GridPane::getVgrow, GridPane::setVgrow), "L: Vgrow", false, GridPane::class, type<Priority?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-hgrow", GridPane::getHgrow, GridPane::setHgrow), "L: Hgrow", false, GridPane::class, type<Priority?>().type))
            yield(InspectedFxProperty(paneProperty(o, "gridpane-margin", HBox::getMargin, HBox::setMargin), "L: Margin", false, GridPane::class, type<Insets?>().type))
         }
      }
   }
}

data class InspectedFxProperty(val observable: () -> Observable, val name: String, val isReadOnly: Boolean, val declaringClass: KClass<*>, val type: KType, val constraints: Set<Constraint<*>> = setOf())

private object PaneProperties {
   inline fun <reified T: Any> paneProperty(o: Node, key: String, crossinline getter: (Node) -> T?, crossinline setter: (Node, T?) -> Unit): () -> Observable = {
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
}


/** [KTypeProjection.type] without variance. [KTypeProjection.STAR] resolves to non-null [Nothing] */
val KTypeProjection.typeOrNothing: KType
   get() = type ?: kTypeNothingNonNull()

/** [KTypeProjection.type] without variance. [KTypeProjection.STAR] resolves to non-null [Any]? */
val KTypeProjection.typeOrAny: KType
   get() = type ?: kType<Any?>()

/** True if this class is subclass of one of the top lvl javafx property interfaces, i.e. [ObservableValue] or [WritableValue] */
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
               raw.isSubclassOf<ObservableValue<*>>() -> argOf(ObservableValue::class, 0).typeOrNothing
               raw.isSubclassOf<WritableValue<*>>() -> argOf(WritableValue::class, 0).typeOrNothing
               else -> fail()
            }

            // Workaround for number properties returning Number.class, due to implementing Property<Number>.
            when {
               pt.raw.isSubclassOf<Number>() -> {
                  val typename = raw.jvmName
                  when {
                     "Integer" in typename -> type<JavafxIntegerPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeOrNothing
                     "Float" in typename -> type<JavafxFloatPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeOrNothing
                     "Long" in typename -> type<JavafxLongPropertyType>().type.argOf(JavafxPropertyType::class, 0).typeOrNothing
                     "Double" in typename -> type<JavafxDoublePropertyType>().type.argOf(JavafxPropertyType::class, 0).typeOrNothing
                     else -> pt
                  }
               }
               else -> pt
            }
         }
         else -> this
      }


// TODO: since Kotlin 1.6.30, MutableCollection retains type, but classifier here loses it, fix for all Mutable types
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
 * Return [KTypeProjection] of the [i]th argument of [argType] superclass of this type.
 * I.e., `type<ArrayList<Int>>().argOf(List.class, 0) == KTypeProjection(OUT, type<Int>)`
 * Because `in Nothing` and `out Any?` and `*` are equal, the result is normalized to `*`, i.e [KTypeProjection.STAR].
 */
fun KType.argOf(argType: KClass<*>, i: Int): KTypeProjection {
   fun KType.isMutableCollection() = 
      (listOf(this) + jvmErasure.allSupertypes).any { it.toString().startsWith("kotlin.collections.Mutable") }
   val c = classifier
   return when {
      argType.java.isArray -> {
         if (raw.java.isArray) arguments.getOrNull(i) ?: fail { "Not found $this $i" }
         else fail { "Not found $this $i" }
      }
      c is KTypeParameter -> fail { "Type parameter not a candidate $this $i" }
      c is KClass<*> -> {
         val argument = when (c) {
            argType -> arguments.getOrNull(i)
               ?.let {
                  when (it.variance) {
                     IN, OUT, null -> it
                     INVARIANT -> when {
                        argType.isSubclassOf<Iterable<*>>() -> {
                           if (this.isMutableCollection()) it
                           else it.copy(variance = OUT)
                        }
                        argType.isSubclassOf<Map<*, *>>() && i==1 -> {
                           if (this.isMutableCollection()) it
                           else it.copy(variance = OUT)
                        }
                        else -> KTypeProjection(argType.typeParameters[i].variance, it.type)
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
                        IN, OUT, null -> it
                        INVARIANT -> when {
                           argType.isSubclassOf<Iterable<*>>() -> {
                              if (this.isMutableCollection()) it
                              else it.copy(variance = OUT)
                           }
                           argType.isSubclassOf<Map<*, *>>() && i==1 -> {
                              if (this.isMutableCollection()) it
                              else it.copy(variance = OUT)
                           }
                           else -> KTypeProjection(argType.typeParameters[i].variance, it.type)
                        }
                     }
                  }
                  ?.let {
                     // resolve declaration site variance from supertype
                     // resolve star projection to upper bounds
                     when (val at = it.type?.classifier) {
                        is KTypeParameter -> {
                           val argumentI = raw.typeParameters.indexOfFirst { it.name==at.name }
                           val argument = arguments[argumentI]
                           KTypeProjection(it.variance, argument.type ?: at.upperBounds[0])
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
         it.variance==OUT && it.type==Any::class.createType(nullable = true) -> STAR
         it.variance==IN && it.type==typeNothingNonNull().type -> STAR
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
 * Subsequent type estimates of the elements are resolved into the best common super type.
 *
 * The returned type is guaranteed to be both run-time and compile-time compatible with every element, i.e,
 * each element can be assumed to be of the returned type as it is assignable to it.
 * However, this collection may not support elements of the returned type, as it may be narrower.
 */
// TODO: remove and merge with T.estimateRuntimeType
fun <T> Collection<T>.estimateRuntimeType(): VType<T> =
   if (isEmpty()) typeNothingNonNull()
   else asSequence().map { it.estimateRuntimeType() }.toList().reduce { a,b ->
      when {
         a isSame typeNothingNonNull() -> b
         a isSame typeNothingNullable() -> b.nullable().asIs()
         b isSame typeNothingNonNull() -> a
         b isSame typeNothingNullable() -> a.nullable().asIs()
         a isSame b && a.raw.visibility==PUBLIC -> a
         a isSubtypeOf b && b.raw.visibility==PUBLIC -> b
         b isSubtypeOf a && a.raw.visibility==PUBLIC -> a
         // handles cases like Option<A>, Option<B> -> Option<supertype of A and B>
         // TODO: change == to isSuperClassOf
         b.raw == a.raw && a.raw.visibility==PUBLIC -> {
            VType(
               b.type.raw.createType(
                  b.type.raw.asIs<KClass<*>>().typeParameters.mapIndexed { i, _ ->
                     val ac = a.type.arguments[i].type?.raw ?: Any::class
                     val bc = b.type.arguments[i].type?.raw ?: Any::class
                     val uc = ac union bc
                     invariant(uc.createTypeStar())
                  },
                  b.isNullable || a.isNullable
               )
            )
         }
         else -> {
            a.type.raw
               .allSupertypes.asSequence()
               .firstOrNull { it.raw != Any::class && it.raw.visibility == PUBLIC && it.raw.isSuperclassOf(b.raw) }
               ?.let {
                  val c = it.raw
                  val params: List<KTypeProjection?> = c.typeParameters.mapIndexed { i, _ ->
                     val ap = a.type.argOf(c, i)
                     val bp = b.type.argOf(c, i)
                     when (ap.variance) {
                        null -> KTypeProjection.STAR
                        INVARIANT -> if (bp==ap) ap else null
                        OUT ->
                           if (ap.type==kTypeNothingNullable() || ap.type==kTypeNothingNonNull())
                              (sequenceOf(bp.type!!) + bp.type!!.raw.allSupertypes.asSequence()).flatMap { if (it.isMarkedNullable) sequenceOf(it) else sequenceOf(it, it.withNullability(true)) }.firstOrNull { it.isSupertypeOf(ap.type!!) }?.let { KTypeProjection(INVARIANT, it) }
                           else
                              (sequenceOf(ap.type!!) + ap.type!!.raw.allSupertypes.asSequence()).flatMap { if (it.isMarkedNullable) sequenceOf(it) else sequenceOf(it, it.withNullability(true)) }.firstOrNull { it.isSupertypeOf(bp.type!!) }?.let { KTypeProjection(INVARIANT, it) }
                        IN -> (sequenceOf(bp.type!!) + bp.type!!.raw.allSupertypes.asSequence()).flatMap { if (it.isMarkedNullable) sequenceOf(it) else sequenceOf(it, it.withNullability(true)) }.firstOrNull { it.isSupertypeOf(ap.type!!) }?.let { KTypeProjection(INVARIANT, it) }
                     }
                  }
                  if (null in params) null else VType<T>(c.createType(params.asIs(), it.isMarkedNullable, it.annotations))
               }
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
 * However, if this value is mutable, it may not be the case after mutating.
 */
@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
fun <T> T.estimateRuntimeType(): VType<T> = when (this) {
   null -> typeNothingNullable().asIs()
   is Optional<*> -> VType(Optional::class.createType(listOf(invariant(map { it.estimateRuntimeType().type }.orElse(kTypeNothingNonNull())))))
   is Option.None -> VType(Option.None::class.createType())
   is Option.Some<*> -> VType(Option.Some::class.createType(listOf(invariant(value.estimateRuntimeType().type))))
   is Try.Ok<*> -> VType(Try.Ok::class.createType(listOf(invariant(value.estimateRuntimeType().type))))
   is Try.Error<*> -> VType(Try.Error::class.createType(listOf(invariant(value.estimateRuntimeType().type))))
   is Collection<*> -> {
      val c = this!!::class
      val cp = c.typeParameters
      when (cp.size) {
         0 -> VType(c.createType(listOf()))
         1 -> VType(c.createType(c.typeParameters.map { KTypeProjection(it.variance, this.asIs<Collection<*>>().estimateRuntimeType().type) }))
         else -> VType(c.createTypeStar())
      }
   }
   else -> {
      val c = this!!::class
      VType(c.createTypeStar())
   }
}

fun KClass<*>.createTypeStar(nullable: Boolean = false, annotations: List<Annotation> = emptyList()): KType = createType(typeParameters.map { STAR }, nullable, annotations)