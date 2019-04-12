package sp.it.demo;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Demo for JavaFX Path Transition and bezier curves.
 *
 * @author James_D (github)
 * @see <a href="https://stackoverflow.com/questions/32359955/updating-an-animation-in-javafx-path-transition-over-an-adjustable-curve</a>
 */
public class BezierDemo extends Application {

	public static void main(String[] args) throws Exception { launch(args); }

	@Override
	public void start(final Stage stage) throws Exception {

		//Create a curve
		CubicCurve curve = new CubicCurve();
		curve.setStartX(100);
		curve.setStartY(100);
		curve.setControlX1(150);
		curve.setControlY1(50);
		curve.setControlX2(250);
		curve.setControlY2(150);
		curve.setEndX(300);
		curve.setEndY(100);
		curve.setStroke(Color.FORESTGREEN);
		curve.setStrokeWidth(4);
		curve.setFill(Color.CORNSILK.deriveColor(0, 1.2, 1, 0.6));

		//Create anchor points at each end of the curve
		Anchor start    = new Anchor(Color.PALEGREEN, curve.startXProperty(),    curve.startYProperty());
		Anchor end      = new Anchor(Color.TOMATO,    curve.endXProperty(),      curve.endYProperty());



		//Create object that follows the curve
		Rectangle rectPath = new Rectangle (0, 0, 40, 40);
		rectPath.setArcHeight(25);
		rectPath.setArcWidth(25);
		rectPath.setFill(Color.ORANGE);


		Transition transition = new Transition() {

			{
				setCycleDuration(Duration.millis(2000));
			}

			@Override
			protected void interpolate(double frac) {
				Point2D start = new Point2D(curve.getStartX(), curve.getStartY());
				Point2D control1 = new Point2D(curve.getControlX1(), curve.getControlY1());
				Point2D control2 = new Point2D(curve.getControlX2(), curve.getControlY2());
				Point2D end = new Point2D(curve.getEndX(), curve.getEndY());

				Point2D center = bezier(frac, start, control1, control2, end);

				double width = rectPath.getBoundsInLocal().getWidth() ;
				double height = rectPath.getBoundsInLocal().getHeight() ;

				rectPath.setTranslateX(center.getX() - width /2);
				rectPath.setTranslateY(center.getY() - height / 2);

				Point2D tangent = bezierDeriv(frac, start, control1, control2, end);
				double angle = Math.toDegrees(Math.atan2(tangent.getY(), tangent.getX()));
				rectPath.setRotate(angle);
			}

		};

		transition.setCycleCount(Animation.INDEFINITE);
		transition.setAutoReverse(true);
		transition.play();


		Group root = new Group();
		root.getChildren().addAll(curve, start, end, rectPath);

		stage.setScene(new Scene( root, 400, 400, Color.ALICEBLUE));
		stage.show();
	}

	private Point2D bezier(double t, Point2D... points) {
		if (points.length == 2) {
			return points[0].multiply(1-t).add(points[1].multiply(t));
		}
		Point2D[] leftArray = new Point2D[points.length - 1];
		System.arraycopy(points, 0, leftArray, 0, points.length - 1);
		Point2D[] rightArray = new Point2D[points.length - 1];
		System.arraycopy(points, 1, rightArray, 0, points.length - 1);
		return bezier(t, leftArray).multiply(1-t).add(bezier(t, rightArray).multiply(t));
	}

	private Point2D bezierDeriv(double t, Point2D... points) {
		if (points.length == 2) {
			return points[1].subtract(points[0]);
		}
		Point2D[] leftArray = new Point2D[points.length - 1];
		System.arraycopy(points, 0, leftArray, 0, points.length - 1);
		Point2D[] rightArray = new Point2D[points.length - 1];
		System.arraycopy(points, 1, rightArray, 0, points.length - 1);
		return bezier(t, leftArray).multiply(-1).add(bezierDeriv(t, leftArray).multiply(1-t))
			.add(bezier(t, rightArray)).add(bezierDeriv(t, rightArray).multiply(t));
	}



	/**
	 * Create draggable anchor points
	 */
	class Anchor extends Circle {
		Anchor(Color color, DoubleProperty x, DoubleProperty y) {
			super(x.get(), y.get(), 10);
			setFill(color.deriveColor(1, 1, 1, 0.5));
			setStroke(color);
			setStrokeWidth(2);
			setStrokeType(StrokeType.OUTSIDE);

			x.bind(centerXProperty());
			y.bind(centerYProperty());
			enableDrag();
		}

		// make a node movable by dragging it around with the mouse.
		private void enableDrag() {
			final Delta dragDelta = new Delta();

			setOnMousePressed(mouseEvent -> {
				// record a delta distance for the drag and drop operation.
				dragDelta.x = getCenterX() - mouseEvent.getX();
				dragDelta.y = getCenterY() - mouseEvent.getY();
				getScene().setCursor(Cursor.MOVE);
			});

			setOnMouseReleased(mouseEvent -> getScene().setCursor(Cursor.HAND));

			setOnMouseDragged(mouseEvent -> {
				double newX = mouseEvent.getX() + dragDelta.x;
				if (newX > 0 && newX < getScene().getWidth()) {
					setCenterX(newX);
				}
				double newY = mouseEvent.getY() + dragDelta.y;
				if (newY > 0 && newY < getScene().getHeight()) {
					setCenterY(newY);
				}
			});

			setOnMouseEntered(mouseEvent -> {
				if (!mouseEvent.isPrimaryButtonDown()) {
					getScene().setCursor(Cursor.HAND);
				}
			});

			setOnMouseExited(mouseEvent -> {
				if (!mouseEvent.isPrimaryButtonDown()) {
					getScene().setCursor(Cursor.DEFAULT);
				}
			});
		}

		// records relative x and y co-ordinates.
		private class Delta { double x, y; }
	}
}