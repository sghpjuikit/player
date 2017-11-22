package sp.it.pl.util.demo.particleanim;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * A simple node which serves as indicator for the wind direction
 */
public class Attractor extends Sprite {

	public Attractor(Vector2D location, Vector2D velocity, Vector2D acceleration, double width, double height) {
		super(location, velocity, acceleration, width, height);
	}

	/**
	 * Circle with a label
	 */
	@Override
	public Node createView() {

		Group group = new Group();

		double radius = width/2;

		Circle circle = new Circle(radius);

		circle.setCenterX(radius);
		circle.setCenterY(radius);

		circle.setStroke(Color.RED);
		circle.setFill(Color.RED.deriveColor(1, 1, 1, 0.3));

		group.getChildren().add(circle);

		Text text = new Text("Attractor\n(Direction)");
		text.setStroke(Color.RED);
		text.setFill(Color.RED);
		text.setBoundsType(TextBoundsType.VISUAL);

		text.relocate(radius - text.getLayoutBounds().getWidth()/2, radius - text.getLayoutBounds().getHeight()/2);

		group.getChildren().add(text);

		return group;
	}

}