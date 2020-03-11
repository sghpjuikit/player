package sp.it.util.type

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import javafx.collections.ListChangeListener
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
import sp.it.util.collections.stackOf
import sp.it.util.dev.fail
import sp.it.util.dev.printIt
import sp.it.util.functional.asIs
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.AbstractList
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KVariance
import kotlin.reflect.KVariance.IN
import kotlin.reflect.KVariance.INVARIANT
import kotlin.reflect.KVariance.OUT
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaType

@Suppress("RemoveRedundantQualifierName")
class TypeUtilTest: FreeSpec({

   "Method" - {
      KClass<*>::superKClassesInc.name {
         open class A: Ia
         open class B: A(), Ib

         Any::class.superclasses shouldBe listOf()
         Ia::class.superclasses shouldBe listOf(Any::class)
         Unit::class.superclasses shouldBe listOf(Any::class)
         Nothing::class.superclasses shouldBe listOf(Any::class)

         // special cases
         Any::class.superKClassesInc().toList() shouldBe listOf(Any::class)
         Nothing::class.superKClassesInc().toList() shouldBe listOf(Nothing::class, Any::class)
         Unit::class.superKClassesInc().toList() shouldBe listOf(Unit::class, Any::class)
         // simple cases
         Ia::class.superKClassesInc().toList() shouldBe listOf(Ia::class, Any::class)
         A::class.superKClassesInc().toList() shouldBe listOf(A::class, Ia::class, Any::class)
         B::class.superKClassesInc().toList() shouldBe listOf(B::class, A::class, Ib::class, Ia::class, Any::class)
         // complex cases
         ArrayList::class.superKClassesInc().toList() shouldBe listOf(
            java.util.ArrayList::class,
            java.util.AbstractList::class,
            kotlin.collections.List::class,
            java.util.RandomAccess::class,
            kotlin.Cloneable::class,
            java.io.Serializable::class,
            java.util.AbstractCollection::class,
            kotlin.collections.List::class,
            kotlin.collections.Collection::class,
            kotlin.collections.Collection::class,
            kotlin.collections.Collection::class,
            kotlin.collections.Iterable::class,
            kotlin.collections.Iterable::class,
            kotlin.collections.Iterable::class,
            kotlin.Any::class
         )
      }

      KType::javaFxPropertyType.name {

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
            property.javaFxPropertyType shouldBe type.type
         }
      }

      KType::isPlatformType.name {
         Node::getScene.returnType.toString().endsWith("!") shouldBe true
         Node::getScene.returnType.isPlatformType shouldBe true
         String::length.returnType.isPlatformType shouldBe false
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

      KClass<*>::traverseToSuper.name {
         Int::class.traverseToSuper(Int::class) shouldBe stackOf(Int::class)
         Int::class.traverseToSuper(Any::class) shouldBe stackOf(Int::class, Number::class, Any::class)
         Int::class.traverseToSuper(Comparable::class) shouldBe stackOf(Int::class, Comparable::class)
         Int::class.traverseToSuper(Long::class) shouldBe stackOf()
         FilteredList::class.traverseToSuper(List::class) shouldBe stackOf(
            FilteredList::class,
            TransformationList::class,
            ObservableListBase::class,
            AbstractList::class,
            List::class
         )
      }

      "${KType::argOf.name} preconditions" {
         List::class.toString() shouldBe "class kotlin.collections.List"
         MutableList::class.toString() shouldBe "class kotlin.collections.List"
         type<List<*>>().type.toString() shouldBe "kotlin.collections.List<kotlin.Any?>"
         type<MutableList<*>>().type.toString() shouldBe "kotlin.collections.List<out kotlin.Any?>"

         List::class.isSubclassOf<MutableList<*>>() shouldBe true
         List::class.isSubclassOf(MutableList::class) shouldBe true
         MutableList::class.isSubclassOf<List<*>>() shouldBe true
         MutableList::class.isSubclassOf(List::class) shouldBe true

         List::class.allSupertypes.toString() shouldBe "[kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, kotlin.Any]"
         MutableList::class.allSupertypes.toString() shouldBe "[kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, kotlin.Any]"
         ArrayList::class.allSupertypes.toString() shouldBe "[java.util.AbstractList<E!>, java.util.AbstractCollection<E!>, kotlin.collections.MutableCollection<E!>, kotlin.collections.Iterable<E>, kotlin.Any, kotlin.collections.MutableList<E!>, kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, java.util.RandomAccess, kotlin.Cloneable, java.io.Serializable, kotlin.collections.MutableList<E>]"
      }

      KType::argOf.name {
         open class Covariant<out T>
         open class Invariant<T>
         open class Contravariant<in T>

         class SubCovariant: Covariant<Int>()
         class SubInvariant: Invariant<Int>()
         class SubContravariant: Contravariant<Int>()
         class SubArrayList: ArrayList<Int?>()

         open class X1<T>
         open class X2<R, E>: X1<E>()
         open class X3<W>: X2<Int, W>()

         open class Y1<Z, T>
         open class Y2<R, T>: Y1<Double, R>()
         open class Y31<T>: Y2<Int, T>()
         open class Y32<T>: Y2<Int?, T>()

         @Suppress("UNREACHABLE_CODE")
         class MyList<W>: TransformationList<W, W>(null) {
            override val size = fail()
            override fun get(index: Int) = fail()
            override fun getSourceIndex(index: Int) = fail()
            override fun getViewIndex(index: Int) = fail()
            override fun sourceChanged(c: ListChangeListener.Change<out W>?) = fail()
         }
         type<MyList<Int>>().type.raw.traverseToSuper(List::class).printIt()
         type<MyList<Int>>().type.let { listOf(it) + it.raw.allSupertypes }.printIt()
         type<MyList<Int>>().type.argOf(List::class, 0).printIt()
         println("--")

         @Suppress("UNREACHABLE_CODE")
         class MyListInt: TransformationList<Int, Int>(null) {
            override val size = fail()
            override fun get(index: Int) = fail()
            override fun getSourceIndex(index: Int) = fail()
            override fun getViewIndex(index: Int) = fail()
            override fun sourceChanged(c: ListChangeListener.Change<out Int>?) = fail()
         }
         type<MyListInt>().type.raw.traverseToSuper(List::class).printIt()
         type<MyListInt>().type.let { listOf(it) + it.raw.allSupertypes }.printIt()
         type<MyListInt>().type.argOf(List::class, 0).printIt()

         println("--")
         ::mmm.returnType.printIt()
         ::mmm.returnType.classifier.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().supertypes.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().allSupertypes.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().superclasses.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().allSuperclasses.printIt()
         ::mmm.returnType.classifier.asIs<KClass<*>>().typeParameters.printIt()

         forall(
            // supertypes argument lookup
            xxx<Y31<Long>, Y1<*, *>, Int>(1, INVARIANT),
            xxx<Y32<Long>, Y1<*, *>, Int?>(1, INVARIANT),

            // supertypes parameter resolution
            xxx<X3<Int>, X1<*>, Int>(0, INVARIANT),
            xxx<X3<Int?>, X1<*>, Int?>(0, INVARIANT),

            // star projection should be retained (in Nothing == * == out Any?)
            xxx<Invariant<*>, Invariant<*>, Any?>(0, null),

            // star projection should be normalized
            xxx<Invariant<in Nothing>, Invariant<*>, Any?>(0, null),
            xxx<Invariant<out Any?>, Invariant<*>, Any?>(0, null),

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

            // borderline star projection should not be star projection
            // xxx<Invariant<in Nothing?>, Invariant<*>, Nothing?>(0, IN), // TODO: enable (does not compile in 1.3.60)
            xxx<Invariant<out Any>, Invariant<*>, Any>(0, OUT),

            // variance is preserved in simple subclassing
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
            fun KTypeProjection.toSimpleString(): String = when (this) {
               STAR -> "*"
               else -> variance!!.name + " " + this.type
            }

            val a1 = i.first.type.argOf(i.second.jvmErasure, i.third)
            val a2 = if (o.second==null) STAR else KTypeProjection(o.second, o.first.type)
            a1.toSimpleString() shouldBe a2.toSimpleString()
         }
      }
   }
})

interface Ia
interface Ib
private inline fun <reified T: Any> rowProp(property: KFunction<Any?>) = row(property.returnType, type<T>())
private inline fun <reified TYPE, reified ARG, reified SHOULD> xxx(i: Int, variance: KVariance?) = row(Triple(type<TYPE>(), type<ARG>(), i), type<SHOULD>() to variance)

fun mmm(): MutableList<Int>? = null