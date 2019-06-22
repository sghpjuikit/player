package sp.it.util.type;

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.DragEvent
import javafx.scene.layout.Pane
import sp.it.util.conf.Config.VarList
import sp.it.util.type.Util.getRawGenericPropertyType
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

class TypeUtilTest: FreeSpec({
	"Method" - {

		::getRawGenericPropertyType.name {

			val o1 = Pane()
			val o2 = object: Any() {
				fun f1(): VarList<Int>? = null
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
					rowProp<ObservableList<Int?>>(o2::f1),
					rowProp<List<*>>(o2::f2),
					rowProp<List<*>>(o2::f3),
					rowProp<List<*>>(o2::f4)
			) { property, type ->
				getRawGenericPropertyType(property) shouldBe type
			}
		}

	}
})

private inline fun <reified T: Any> rowProp(property: KFunction<Any?>) = row(property.returnType.javaType, T::class.javaObjectType)