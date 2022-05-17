package sp.it.util.type

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import java.util.function.Consumer
import java.util.function.Function
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
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaType

@Suppress("RemoveRedundantQualifierName", "RemoveExplicitTypeArguments")
class TypeUtilTest: FreeSpec({

   "Method" - {
      "Assumptions" {
         // Mutable collections classes erase to collections
         Collection::class shouldBe MutableCollection::class
         List::class shouldBe MutableList::class

         // Mutable collections types !erase to collections
         type<Collection<Int>>() shouldNotBe type<MutableCollection<Int>>()
         type<List<Int>>() shouldNotBe type<MutableList<Int>>()

         // Nothing erases to Void
         Nothing::class shouldBe Void::class
         Nothing::class.toString() shouldBe "class java.lang.Void"

         // Unit is ordinary
         Unit::class.toString() shouldBe "class kotlin.Unit"

         // Unit represents Unit
         List<*>::forEach.returnType shouldBe kType<Unit>()
         List<*>::forEach.returnType.classifier shouldBe Unit::class

         // Void represents Void
         class VoidField(val void: Void)
         VoidField::void.returnType shouldBe kType<Void>()
         VoidField::void.returnType.classifier shouldBe Void::class

         // Unit represents Void as no return
         Consumer<*>::accept.returnType shouldBe kType<Unit>()
         Consumer<*>::accept.returnType.classifier shouldBe Unit::class
      }

      KType::isSubtypeOf.name {
         open class NonGeneric
         open class Covariant<out T>
         open class Invariant<T>
         open class Contravariant<in T>

         class SubCovariant<T>: Covariant<T>()
         class SubInvariant<T>: Invariant<T>()
         class SubContravariant<T>: Contravariant<T>()

         // Any/Nothing
         type<Any>() isSubtypeOf type<Any>() shouldBe true
         type<Any?>() isSubtypeOf type<Any?>() shouldBe true
         typeNothingNonNull() isSubtypeOf typeNothingNonNull() shouldBe true
         typeNothingNonNull() isSubtypeOf typeNothingNullable() shouldBe true
         typeNothingNullable() isSubtypeOf typeNothingNonNull() shouldBe false
         typeNothingNullable() isSubtypeOf typeNothingNullable() shouldBe true
         typeNothingNonNull() isSubtypeOf type<Any>() shouldBe true
         typeNothingNonNull() isSubtypeOf type<Any?>() shouldBe true
         typeNothingNullable() isSubtypeOf type<Any>() shouldBe false
         typeNothingNullable() isSubtypeOf type<Any?>() shouldBe true

         // simple type
         type<NonGeneric>() isSubtypeOf type<NonGeneric>() shouldBe true
         type<NonGeneric>() isSubtypeOf type<Any>() shouldBe true
         type<NonGeneric>() isSubtypeOf type<Any?>() shouldBe true
         type<NonGeneric?>() isSubtypeOf type<Any>() shouldBe false
         type<NonGeneric?>() isSubtypeOf type<Any?>() shouldBe true
         typeNothingNonNull() isSubtypeOf type<NonGeneric>() shouldBe true
         typeNothingNonNull() isSubtypeOf type<NonGeneric?>() shouldBe true
         typeNothingNullable() isSubtypeOf type<NonGeneric>() shouldBe false
         typeNothingNullable() isSubtypeOf type<NonGeneric?>() shouldBe true

         // covariance
         type<Covariant<Int>>() isSubtypeOf type<Covariant<Int>>() shouldBe true
         type<Covariant<Int>>() isSubtypeOf type<Covariant<Number>>() shouldBe true
         type<Covariant<Number>>() isSubtypeOf type<Covariant<Int>>() shouldBe false
         type<Covariant<Int>>() isSubtypeOf type<Covariant<Int?>>() shouldBe true
         type<Covariant<Int?>>() isSubtypeOf type<Covariant<Int>>() shouldBe false

         // invariance
         type<Invariant<Int>>() isSubtypeOf type<Invariant<Int>>() shouldBe true
         type<Invariant<Int>>() isSubtypeOf type<Invariant<Number>>() shouldBe false
         type<Invariant<Number>>() isSubtypeOf type<Invariant<Int>>() shouldBe false
         type<Invariant<Int>>() isSubtypeOf type<Invariant<Int?>>() shouldBe false
         type<Invariant<Int?>>() isSubtypeOf type<Invariant<Int>>() shouldBe false

         // contravariance
         type<Contravariant<Int>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true
         type<Contravariant<Int>>() isSubtypeOf type<Contravariant<Number>>() shouldBe false
         type<Contravariant<Number>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true
         type<Contravariant<Int>>() isSubtypeOf type<Contravariant<Int?>>() shouldBe false
         type<Contravariant<Int?>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true

         // sub-classed covariance
         type<SubCovariant<Int>>() isSubtypeOf type<Covariant<Int>>() shouldBe true
         type<SubCovariant<Int>>() isSubtypeOf type<Covariant<Number>>() shouldBe true
         type<SubCovariant<Number>>() isSubtypeOf type<Covariant<Int>>() shouldBe false
         type<SubCovariant<Int>>() isSubtypeOf type<Covariant<Int?>>() shouldBe true
         type<SubCovariant<Int?>>() isSubtypeOf type<Covariant<Int>>() shouldBe false

         // sub-classed invariance
         type<SubInvariant<Int>>() isSubtypeOf type<Invariant<Int>>() shouldBe true
         type<SubInvariant<Int>>() isSubtypeOf type<Invariant<Number>>() shouldBe false
         type<SubInvariant<Number>>() isSubtypeOf type<Invariant<Int>>() shouldBe false
         type<SubInvariant<Int>>() isSubtypeOf type<Invariant<Int?>>() shouldBe false
         type<SubInvariant<Int?>>() isSubtypeOf type<Invariant<Int>>() shouldBe false

         // sub-classed contravariance
         type<SubContravariant<Int>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true
         type<SubContravariant<Int>>() isSubtypeOf type<Contravariant<Number>>() shouldBe false
         type<SubContravariant<Number>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true
         type<SubContravariant<Int>>() isSubtypeOf type<Contravariant<Int?>>() shouldBe false
         type<SubContravariant<Int?>>() isSubtypeOf type<Contravariant<Int>>() shouldBe true

         // super-classed covariance
         type<Covariant<Int>>() isSubtypeOf type<SubCovariant<Int>>() shouldBe false
         type<Covariant<Int>>() isSubtypeOf type<SubCovariant<Number>>() shouldBe false
         type<Covariant<Number>>() isSubtypeOf type<SubCovariant<Int>>() shouldBe false
         type<Covariant<Int>>() isSubtypeOf type<SubCovariant<Int?>>() shouldBe false
         type<Covariant<Int?>>() isSubtypeOf type<SubCovariant<Int>>() shouldBe false

         // super-classed invariance
         type<Invariant<Int>>() isSubtypeOf type<SubInvariant<Int>>() shouldBe false
         type<Invariant<Int>>() isSubtypeOf type<SubInvariant<Number>>() shouldBe false
         type<Invariant<Number>>() isSubtypeOf type<SubInvariant<Int>>() shouldBe false
         type<Invariant<Int>>() isSubtypeOf type<SubInvariant<Int?>>() shouldBe false
         type<Invariant<Int?>>() isSubtypeOf type<SubInvariant<Int>>() shouldBe false

         // super-classed contravariance
         type<Contravariant<Int>>() isSubtypeOf type<SubContravariant<Int>>() shouldBe false
         type<Contravariant<Int>>() isSubtypeOf type<SubContravariant<Number>>() shouldBe false
         type<Contravariant<Number>>() isSubtypeOf type<SubContravariant<Int>>() shouldBe false
         type<Contravariant<Int>>() isSubtypeOf type<SubContravariant<Int?>>() shouldBe false
         type<Contravariant<Int?>>() isSubtypeOf type<SubContravariant<Int>>() shouldBe false

         // list
         type<List<Int>>() isSubtypeOf type<List<Number>>() shouldBe true // List is covariant
         type<MutableList<Int>>() isSubtypeOf type<MutableList<Number>>() shouldBe false // MutableList is invariant
         type<MutableList<in Number>>() isSubtypeOf type<MutableList<in Int>>() shouldBe true // MutableList is invariant
         type<MutableList<out Int>>() isSubtypeOf type<MutableList<out Number>>() shouldBe true // MutableList is invariant
         type<MutableList<Int>>() isSubtypeOf type<List<Number>>() shouldBe true
         type<MutableList<out Int>>() isSubtypeOf type<List<Number>>() shouldBe true

         // star (declaration variance)
         type<Covariant<Int>>() isSubtypeOf type<Covariant<*>>() shouldBe true
         type<Contravariant<Int>>() isSubtypeOf type<Contravariant<*>>() shouldBe true

         // star (on-site variance)
         type<Invariant<out Int>>() isSubtypeOf type<Invariant<*>>() shouldBe true
         type<Invariant<in Int>>() isSubtypeOf type<Invariant<*>>() shouldBe true
      }

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

      KType::isPlatformType.name {
         Node::getScene.returnType.toString().endsWith("!") shouldBe true
         Node::getScene.returnType.isPlatformType shouldBe true
         String::length.returnType.isPlatformType shouldBe false
      }

      @Suppress("REDUNDANT_PROJECTION")
      KType::javaFxPropertyType.name {
         val o1 = Pane()
         val o2 = object: Any() {
            fun f2(): List<Int> = arrayListOf()
            fun f3(): MutableList<in Int> = arrayListOf()
            fun f4(): MutableList<out Int> = arrayListOf()
            fun fn2(): List<Int?>? = null
            fun fn3(): MutableList<in Int?>? = null
            fun fn4(): MutableList<out Int?>? = null
         }

         o2::fn4.returnType shouldBe kType<MutableList<out Int?>?>()
         o2::fn4.returnType.javaFxPropertyType shouldBe kType<MutableList<out Int?>?>()
         o2::fn4.returnType.javaFxPropertyType.classifier shouldBe List::class
         o2::fn4.returnType.javaFxPropertyType.withPlatformTypeNullability(false) shouldBe kType<List<out Int?>?>()
         o2::fn4.returnType.javaFxPropertyType.withPlatformTypeNullability(true) shouldBe kType<List<out Int?>?>()

         forAll(
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
            rowProp<EventHandler<in DragEvent>>(o1::getOnDragDone),
            rowProp<EventHandler<in DragEvent>>(o1::onDragDoneProperty),
            rowProp<ObservableList<Node>>(o1::getChildren),
            rowProp<ObservableList<Node>>(o1::getChildrenUnmodifiable),
            rowProp<ObservableMap<Any, Any>>(o1::getProperties),
            rowProp<List<Int>>(o2::f2),
            rowProp<MutableList<in Int>>(o2::f3),
            rowProp<MutableList<out Int>>(o2::f4),
            rowProp<List<Int?>?>(o2::fn2),
            rowProp<MutableList<in Int?>?>(o2::fn3),
            rowProp<MutableList<out Int?>?>(o2::fn4)
         ) { property, type ->
            property.javaFxPropertyType.withPlatformTypeNullability(false) shouldBe type.type.withPlatformTypeNullability(false)
         }
      }

      Type::toRaw.name {
         class X

         val o = object: Any() {
            val x: X = X()
            val arrayOfX: Array<X> = arrayOf()
            val arrayOfDouble: Array<Double> = arrayOf()
            val doubleArray: DoubleArray = DoubleArray(0)
            val listI: MutableList<X> = mutableListOf()
            val listIn: MutableList<in X> = mutableListOf()
            val listOut: MutableList<out X> = mutableListOf()
         }

         o::x.returnType.javaType.toRaw() shouldBe X::class.java
         o::arrayOfX.returnType.javaType.toRaw() shouldBe Array<X>::class.java
         o::arrayOfX.returnType.javaType.toRaw() shouldNotBe Array::class.java
         o::arrayOfDouble.returnType.javaType.toRaw() shouldNotBe DoubleArray::class.java
         o::arrayOfDouble.returnType.javaType.toRaw() shouldBe Array<Double>::class.java
         o::arrayOfDouble.returnType.javaType.toRaw() shouldNotBe Array::class.java
         o::doubleArray.returnType.javaType.toRaw() shouldBe DoubleArray::class.java
         o::doubleArray.returnType.javaType.toRaw() shouldNotBe Array<Double>::class.java
         o::doubleArray.returnType.javaType.toRaw() shouldNotBe Array::class.java
         o::listI.returnType.javaType.toRaw() shouldBe MutableList::class.java

         forAll(
            row(o::listI, X::class),
            row(o::listIn, X::class),
            row(o::listOut, X::class)
         ) { property, type ->
            (property.returnType.javaType as ParameterizedType).actualTypeArguments[0].toRaw() shouldBe type.java
         }
      }

      KType::toRawFlat.name {
         // simple type
         kType<Any>().toRawFlat().toList() shouldBe listOf(Any::class)
         // nullability stripped
         kType<Any?>().toRawFlat().toList() shouldBe listOf(Any::class)
         // variance stripped
         kType<List<Int?>>().toRawFlat().toList() shouldBe listOf(List::class, Integer::class)
         kType<MutableList<out Int>>().toRawFlat().toList() shouldBe listOf(List::class, Integer::class)
         kType<MutableList<in Int?>>().toRawFlat().toList() shouldBe listOf(List::class, Integer::class)
         kType<MutableList<Int>>().toRawFlat().toList() shouldBe listOf(List::class, Integer::class)
         kType<ArrayList<Int>>().toRawFlat().toList() shouldBe listOf(ArrayList::class, Integer::class)
         // star resolved as covariant Any
         kType<List<*>>().toRawFlat().toList() shouldBe listOf(List::class, Any::class)
         kType<Function<*,*>>().toRawFlat().toList() shouldBe listOf(Function::class, Any::class, Any::class)
         // arbitrary depth
         kType<List<List<*>>>().toRawFlat().toList() shouldBe listOf(List::class, List::class, Any::class)
         // special types
         kType<Unit>().toRawFlat().toList() shouldBe listOf(Unit::class)
         kType<Unit?>().toRawFlat().toList() shouldBe listOf(Unit::class)
         kTypeNothingNonNull().toRawFlat().toList() shouldBe listOf(Nothing::class)
         kTypeNothingNullable().toRawFlat().toList() shouldBe listOf(Nothing::class)
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

      KClass<*>::union.name {
         // self
         Int::class union Int::class shouldBe Int::class

         // simple hierarchy
         Int::class union Long::class shouldBe Number::class
         Long::class union Int::class shouldBe Number::class

         // union with top of the hierarchy
         Any::class union Int::class shouldBe Any::class
         Any::class union Any::class shouldBe Any::class
         Int::class union Any::class shouldBe Any::class

         // union with bottom of the hierarchy
         Int::class union Nothing::class shouldBe Any::class
         Nothing::class union Int::class shouldBe Any::class

         // complex hierarchy
         listOf<Int>()::class union setOf<Int>()::class shouldBe Collection::class
         listOf<Int>()::class union listOf<Int>()::class shouldBe listOf<Int>()::class
         listOf<Int>(1)::class union listOf<Int>(1)::class shouldBe listOf<Int>(1)::class
         listOf<Int>(1, 2)::class union listOf<Int>(1, 2)::class shouldBe listOf<Int>(1, 2)::class
         listOf<Int>(1, 2, 3)::class union listOf<Int>(1, 2, 3)::class shouldBe listOf<Int>(1, 2, 3)::class

         // interface
         Int::class union Comparable::class shouldBe Comparable::class
         listOf<Int>()::class union List::class shouldBe List::class
      }

      KType::argOf.name + " preconditions" {
         List::class.toString() shouldBe "class kotlin.collections.List"
         MutableList::class.toString() shouldBe "class kotlin.collections.List"
         type<List<*>>().type.toString() shouldBe "kotlin.collections.List<*>"
         type<MutableList<*>>().type.toString() shouldBe "kotlin.collections.MutableList<*>"

         List::class.isSubclassOf<MutableList<*>>() shouldBe true
         List::class.isSubclassOf(MutableList::class) shouldBe true
         MutableList::class.isSubclassOf<List<*>>() shouldBe true
         MutableList::class.isSubclassOf(List::class) shouldBe true

         List::class.allSupertypes.toString() shouldBe "[kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, kotlin.Any]"
         MutableList::class.allSupertypes.toString() shouldBe "[kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, kotlin.Any]"
         ArrayList::class.allSupertypes.toString() shouldBe "[java.util.AbstractList<E!>, java.util.AbstractCollection<E!>, kotlin.collections.MutableCollection<E!>, kotlin.collections.Iterable<E>, kotlin.Any, kotlin.collections.MutableList<E!>, kotlin.collections.Collection<E>, kotlin.collections.Iterable<E>, java.util.RandomAccess, kotlin.Cloneable, java.io.Serializable, kotlin.collections.MutableList<E>]"
      }

      "!${KType::argOf.name}" {
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

         forAll(
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
            // xxx<Invariant<in Nothing?>, Invariant<*>, Nothing?>(0, IN), // TODO: enable (does not compile in 1.3.70)
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

            val a1 = i.first.type.argOf(i.second.raw, i.third)
            val a2 = if (o.second==null) STAR else KTypeProjection(o.second, o.first.type)
            a1.toSimpleString() shouldBe a2.toSimpleString()
         }
      }
   }
})

interface Ia
interface Ib

private inline fun <reified T: Any?> rowProp(property: KFunction<Any?>) = row(property.returnType, type<T>())
private inline fun <reified TYPE, reified ARG, reified SHOULD> xxx(i: Int, variance: KVariance?) = row(Triple(type<TYPE>(), type<ARG>(), i), type<SHOULD>() to variance)

fun mmm(): MutableList<Int>? = null