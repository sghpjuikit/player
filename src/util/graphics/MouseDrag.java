package util.graphics;

import java.util.function.Consumer;
import javafx.scene.Node;
import static javafx.scene.input.MouseEvent.*;
import static util.dev.Util.noØ;

/**
 * Helper for mouse dragging behavior, such as moving or resizing graphics.
 *
 * @param <T> type of user data
 */
public class MouseDrag<T> {
	public final T data;
	public final P start = new P();
	public final P diff = new P();
	public boolean isDragging = false;

	/**
	 * Creates a mouse drag behavior.
	 *
	 * @param node nonnull graphics to install behavior on
	 * @param data nullable user data
	 * @param onStart nonnull onDragStart action
	 * @param onDrag nonnull onDragMove action
	 */
	public MouseDrag(Node node, T data, Consumer<? super MouseDrag<T>> onStart, Consumer<? super MouseDrag<T>> onDrag) {
		noØ(node, onStart, onDrag);
		this.data = data;
		node.addEventFilter(MOUSE_PRESSED, e -> {
			isDragging = true;
			start.setX(e.getX());
			start.setY(e.getY());
			onStart.accept(this);
			e.consume();
		});
		node.addEventFilter(MOUSE_DRAGGED, e -> {
			if (isDragging) {
				diff.setX(e.getX() - start.getX());
				diff.setY(e.getY() - start.getY());
				onDrag.accept(this);
			}
			e.consume();
		});
		node.addEventFilter(MOUSE_RELEASED, e -> {
			isDragging = false;
		});
	}
}