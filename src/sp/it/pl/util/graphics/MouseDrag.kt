package sp.it.pl.util.graphics

import javafx.scene.Node
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.math.P
import java.util.function.Consumer

/**
 * Helper for mouse dragging behavior, such as moving or resizing graphics.
 *
 * @param <T> type of user data
 */
class MouseDrag<T> {
    @JvmField val data: T
    @JvmField val start: P
    @JvmField val diff: P
    @JvmField var isDragging = false

    /**
     * Creates a mouse drag behavior.
     *
     * @param node graphics to install behavior on
     * @param data user data
     * @param onStart onDragStart action
     * @param onDrag onDragMove action
     */
    constructor(node: Node, data: T, onStart: Consumer<in MouseDrag<T>>, onDrag: Consumer<in MouseDrag<T>>) {
        this.data = data
        this.start = P()
        this.diff = P()
        node.addEventFilter(MOUSE_PRESSED) { e ->
            isDragging = true
            start.x = e.screenX
            start.y = e.screenY
            onStart(this)
            e.consume()
        }
        node.addEventFilter(MOUSE_DRAGGED) { e ->
            if (isDragging) {
                diff.x = e.screenX-start.x
                diff.y = e.screenY-start.y
                onDrag(this)
            }
            e.consume()
        }
        node.addEventFilter(MOUSE_RELEASED) { isDragging = false }
    }

}

fun <T> Node.initMouseDrag(data: T, onStart: Consumer<in MouseDrag<T>>, onDrag: Consumer<in MouseDrag<T>>) =
        MouseDrag(this, data, onStart, onDrag)