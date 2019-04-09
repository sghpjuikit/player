package sp.it.util.demo.particleanim;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

/**
 * Allow dragging of attractors and repellers
 */
class MouseGestures {

	final DragContext dragContext = new DragContext();

	private final EventHandler<MouseEvent> onMousePressedEventHandler = event -> {
		dragContext.x = event.getSceneX();
		dragContext.y = event.getSceneY();
	};

	private final EventHandler<MouseEvent> onMouseDraggedEventHandler = event -> {

		Sprite sprite = (Sprite) event.getSource();

		double offsetX = event.getSceneX() - dragContext.x;
		double offsetY = event.getSceneY() - dragContext.y;

		sprite.setLocationOffset(offsetX, offsetY);

		dragContext.x = event.getSceneX();
		dragContext.y = event.getSceneY();

	};

	private final EventHandler<MouseEvent> onMouseReleasedEventHandler = event -> {};

	public void makeDraggable(final Sprite sprite) {
		sprite.setOnMousePressed(onMousePressedEventHandler);
		sprite.setOnMouseDragged(onMouseDraggedEventHandler);
		sprite.setOnMouseReleased(onMouseReleasedEventHandler);
	}

	static class DragContext {
		double x;
		double y;
	}

}