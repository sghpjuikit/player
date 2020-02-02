package sp.it.util.type;

import io.kotlintest.data.forall
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import javafx.collections.ObservableList
import javafx.collections.ObservableListBase
import javafx.collections.ObservableMap
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.TransformationList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.DragEvent
import javafx.scene.layout.Pane
import sp.it.util.collections.ObservableListRO
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.functional.asIs
import sp.it.util.type.Util.getRawGenericPropertyType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.AbstractList
import java.util.Stack
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.KVariance.IN
import kotlin.reflect.KVariance.INVARIANT
import kotlin.reflect.KVariance.OUT
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaType

class TypeUtilTest: FreeSpec({
   "Method" - {

      ::getRawGenericPropertyType.name {

         val o1 = Pane()
         val o2 = object: Any() {
            fun f2(): List<Int>? = null
            fun f3(): MutableList<in Int>? = null
            fun f4(): MutableList<out Int>? = null
         }

         forall(
            rowProp<Double>(o1::getWidth),
            rowProp<Double>(o1::widthProperty),
            rowProp<Double>(o1::getMaxWidth),
            rowProp<Double>(o1::maxWidthProperty),
            rowProp<String>(o1::getStyle),
            rowProp<String>(o1::styleProperty),
            rowProp<Node>(o1::getClip),
            rowProp<Node>(o1::clipProperty),
            rowProp<Boolean>(o1::isManaged),
            rowProp<Boolean>(o1::managedProperty),
            rowProp<Boolean>(o1::isNeedsLayout),
            rowProp<Boolean>(o1::needsLayoutProperty),
            rowProp<EventHandler<DragEvent>>(o1::getOnDragDone),
            rowProp<EventHandler<DragEvent>>(o1::onDragDoneProperty),
            rowProp<ObservableList<Node?>>(o1::getChildren),
            rowProp<ObservableList<Node?>>(o1::getChildrenUnmodifiable),
            rowProp<ObservableMap<Any?, Any?>>(o1::getProperties),
            rowProp<List<*>>(o2::f2),
            rowProp<List<*>>(o2::f3),
            rowProp<List<*>>(o2::f4)
         ) { property, type ->
            getRawGenericPropertyType(property) shouldBe type
         }
      }

      Type::toRaw.name {
         class X

         val o = object: Any() {
            val a: X = X()
            val b: Array<X> = arrayOf()
            val x: MutableList<X> = mutableListOf()
            val y: MutableList<X> = mutableListOf()
            val z: MutableList<X> = mutableListOf()
         }

         forall(
            row(o::a, X::class),
            row(o::b, Array<X>::class)
         ) { property, type ->
            property.returnType.javaType.toRaw() shouldBe type.java
         }

         forall(
            row(o::x, X::class),
            row(o::y, X::class),
            row(o::z, X::class)
         ) { property, type ->
            (property.returnType.javaType as ParameterizedType).actualTypeArguments[0].toRaw() shouldBe type.java
         }
      }

      KClass::traverseToSuper.name {
         Int::class.traverseToSuper(Int::class) shouldBe Stack(Int::class)
         Int::class.traverseToSuper(Any::class) shouldBe Stack(Int::class, Number::class, Any::class)
         Int::class.traverseToSuper(Comparable::class) shouldBe Stack(Int::class, Comparable::class)
         Int::class.traverseToSuper(Long::class) shouldBe Stack()
         FilteredList::class.traverseToSuper(List::class) shouldBe Stack(
            FilteredList::class,
            TransformationList::class,
            ObservableListBase::class,
            AbstractList::class,
            List::class
         )
      }

      ::getFirstGenericArgument.name {
         open class Covariant<out T>
         open class Invariant<T>
         open class Contravariant<in T>
         class SubCovariant: Covariant<Int>()
         class SubInvariant: Invariant<Int>()
         class SubContravariant: Contravariant<Int>()
         class SubArrayList: ArrayList<Int?>()


         List::class.printIt()
         MutableList::class.printIt()
         type<List<*>>().type.printIt()
         type<MutableList<*>>().type.printIt()
         List::class.isSubclassOf<MutableList<*>>().printIt()
         List::class.isSubclassOf(MutableList::class).printIt()
         MutableList::class.isSubclassOf<List<*>>().printIt()
         MutableList::class.isSubclassOf(List::class).printIt()

         List::class.allSupertypes.printIt()
         MutableList::class.allSupertypes.printIt()
         ArrayList::class.allSupertypes.printIt()

         println("mmm")
         ::mmm.returnType.printIt()
         ::mmm.returnType.classifier.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().supertypes.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().allSupertypes.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().superclasses.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().allSuperclasses.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().typeParameters.printIt()

         forall(
            // star projection should be retained (in Nothing == * == out Any?)
            xxx<Invariant<*>, Invariant<*>, Any?>(0, null),

            // invariant declaration + invariant site = invariant
            xxx<Invariant<Int>, Invariant<*>, Int>(0, INVARIANT),
            // invariant declaration + covariant site = covariant
            xxx<Invariant<out Int>, Invariant<*>, Int>(0, OUT),
            // invariant declaration + contravariant site = contravariant
            xxx<Invariant<in Int>, Invariant<*>, Int>(0, IN),
            // covariant declaration + (any site) = covariant
            xxx<Covariant<Int>, Covariant<*>, Int>(0, OUT),
            // contravariant declaration + (any site) = contravariant
            xxx<Contravariant<Int>, Contravariant<*>, Int>(0, IN),

            xxx<SubInvariant, Invariant<*>, Int>(0, INVARIANT),
            xxx<SubCovariant, Covariant<*>, Int>(0, IN),
            xxx<SubContravariant, Contravariant<*>, Int>(0, OUT),

            // lists are read-only => covariant
            xxx<List<Int>, List<*>, Int>(0, OUT),
            xxx<List<Int?>, List<*>, Int?>(0, OUT),
            // mutable lists are writable => invariant
            xxx<MutableList<Int>, List<*>, Int>(0, INVARIANT),
            xxx<MutableList<Int?>, List<*>, Int?>(0, INVARIANT),

            // subclasses of writable lists are writable lists
            xxx<SubArrayList, List<*>, Int?>(0, INVARIANT),
            xxx<List<Int?>, List<*>, Int?>(0, OUT),
            // MutableList is erased to List, but KType must be able to test against it
            xxx<MutableList<Int?>, MutableList<*>, Int?>(0, INVARIANT),
            // MutableList must even as a List still report as INVARIANT
            xxx<MutableList<Int?>, List<*>, Int?>(0, INVARIANT),

            // observable lists are writable => invariant
            xxx<ObservableList<Int?>, List<*>, Int?>(0, INVARIANT),
            xxx<ObservableList<Int?>, ObservableList<*>, Int?>(0, INVARIANT),
            xxx<ObservableList<Int>, ObservableList<*>, Int>(0, INVARIANT),
            xxx<FilteredList<Int?>, ObservableList<*>, Int?>(0, INVARIANT),
            xxx<FilteredList<Int>, ObservableList<*>, Int?>(0, INVARIANT),

            // read only observable lists are read only => covariant
            xxx<ObservableListRO<Int?>, List<*>, Int?>(0, OUT),
            xxx<ObservableListRO<Int>, List<*>, Int?>(0, OUT)

            // test map read/write
         ) { i, o ->
            i.first.type.argOf(i.second.jvmErasure, i.third) shouldBe (if (o.second == null) KTypeProjection.STAR else KTypeProjection(o.second, o.first.type))
         }
      }
   }
})

private inline fun <reified T: Any> rowProp(property: KFunction<Any?>) = row(property.returnType.javaType, T::class.javaObjectType)
private inline fun <reified TYPE, reified ARG, reified SHOULD> xxx(i: Int, variance: KVariance?) = row(Triple(type<TYPE>(), type<ARG>(), i), type<SHOULD>() to variance)

fun mmm(): MutableList<Int>? = null
fun mmms(): MutableList<*>? = null
fun fgat(t: KType): KType? = when (val c = t.classifier) {
   is KTypeParameter -> c.upperBounds.firstOrNull()
   is KClass<*> -> null
      ?: t.arguments.firstOrNull()?.let {
         when (it.variance) {
            IN, OUT, null -> it
            INVARIANT -> KTypeProjection(c.typeParameters.first().variance, it.type)
         }
      }?.type
      ?: run {
         Util.getGenericPropertyTypeImpl(t.javaType)?.toRaw()?.kotlin?.createType(nullable = true)
      }
   else -> null
}

fun fgav(t: KType): Any? = when (val c = t.classifier) {
   is KTypeParameter -> c.upperBounds.firstOrNull()
   is KClass<*> -> null
      ?: t.arguments.firstOrNull()?.let {
         when (it.variance) {
            IN, OUT, INVARIANT, null -> it
         }
      }?.variance
      ?: Util.getGenericPropertyTypeImpl(t.javaType)?.toRaw()?.kotlin?.createType(nullable = true)
   else -> null
}

inline fun <reified T> KType.argOf(i: Int): KTypeProjection = argOf(type<T>().jvmErasure, i)

fun KType.argOf(argType: KClass<*>, i: Int): KTypeProjection = when (val c = classifier) {
   is KTypeParameter -> fail { "Type parameter not a candidate $this $i" }
   is KClass<*> -> {
      val argument = when (c) {
         argType -> arguments.getOrNull(i)
            ?.let {
               when (it.variance) {
                  IN, OUT, null -> it
                  INVARIANT -> when {
                     argType.isSubclassOf<Iterable<*>>() -> {
                        if (this.toString().startsWith("kotlin.collections.Mutable")) it
                        else it.copy(variance = OUT)
                     }
                     argType.isSubclassOf<Map<*,*>>() && i==1 -> {
                        if (this.toString().startsWith("kotlin.collections.Mutable")) it
                        else it.copy(variance = OUT)
                     }
                     else -> it
                  }
               }
            }
            ?: fail { "Not found $this $i" }
         else -> {
            val st = c.allSupertypes
            val stack = c.traverseToSuper(argType)
            st.find { it.classifier==argType }?.arguments?.getOrNull(i)
               ?.let {
                  when (it.variance) {
                     IN, OUT, null -> it
                     INVARIANT -> it //KTypeProjection(c.typeParameters.get().variance, it.type)
                  }
               }
               ?: fail { "Not found $argType $i" }
         }
      }
      argument
   }
   else -> fail { "Unknown error" }
}

fun KClass<*>.traverseToSuper(s: KClass<*>, stack: Stack<KClass<*>> = Stack()): Stack<KClass<*>> {
   stack.push(this)
   superclasses.forEach {
      if (stack.peek()==s) return stack
      it.traverseToSuper(s, stack)
   }
   if (stack.peek()!=s) stack.pop()
   return stack
}