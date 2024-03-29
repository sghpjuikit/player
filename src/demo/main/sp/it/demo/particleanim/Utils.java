package sp.it.demo.particleanim;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.RED;
import static javafx.scene.paint.Color.TRANSPARENT;
import static javafx.scene.paint.Color.WHITE;
import static javafx.scene.paint.Color.YELLOW;

class Utils {

	/**
	 * Clamp value between min and max
	 */
	@SuppressWarnings({"SameParameterValue", "ManualMinMaxCalculation"})
	static double clamp(double value, double min, double max) {
		if (value<min) return min;
		else if (value>max) return max;
		else return value;
	}

	/**
	 * Map value of a given range to a target range
	 */
	static double map(double value, double currentRangeStart, double currentRangeStop, double targetRangeStart, double targetRangeStop) {
		return targetRangeStart + (targetRangeStop - targetRangeStart)*((value - currentRangeStart)/(currentRangeStop - currentRangeStart));
	}

	/**
	 * Snapshot an image out of a node, consider transparency.
	 */
	static Image createImage(Node node) {

		WritableImage wi;

		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(TRANSPARENT);

		int imageWidth = (int) node.getBoundsInLocal().getWidth();
		int imageHeight = (int) node.getBoundsInLocal().getHeight();

		wi = new WritableImage(imageWidth, imageHeight);
		node.snapshot(parameters, wi);

		return wi;

	}

	/**
	 * Pre-create images with various gradient colors and sizes.
	 *
	 * @return pre-created images
	 */
	static Image[] preCreateImages() {

		int count = (int) Settings.PARTICLE_LIFE_SPAN_MAX;

		Image[] list = new Image[count];

		double radius = Settings.PARTICLE_WIDTH;

		for (int i = 0; i<count; i++) {

			double opacity = (double) i/(double) count;

			// get color depending on lifespan
			Color color;

			double threshold = 0.9;
			double threshold2 = 0.4;
			if (opacity>=threshold) {
				color = YELLOW.interpolate(WHITE, Utils.map(opacity, threshold, 1, 0, 1));
			} else if (opacity>=threshold2) {
				color = RED.interpolate(YELLOW, Utils.map(opacity, threshold2, threshold, 0, 1));
			} else {
				color = BLACK.interpolate(RED, Utils.map(opacity, 0, threshold2, 0, 1));
			}

			// create gradient image with given color
			Circle ball = new Circle(radius);

			RadialGradient gradient1 = new RadialGradient(0, 0, 0, 0, radius, false, CycleMethod.NO_CYCLE, new Stop(0, color.deriveColor(1, 1, 1, 1)), new Stop(1, color.deriveColor(1, 1, 1, 0)));

			ball.setFill(gradient1);

			// create image
			list[i] = Utils.createImage(ball);
		}

		return list;
	}
}