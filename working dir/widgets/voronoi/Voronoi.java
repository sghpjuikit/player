package voronoi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

import javafx.event.Event;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import util.access.V;
import util.animation.Loop;
import util.functional.Try;

import static java.lang.Math.*;
import static javafx.scene.input.MouseEvent.*;
import static util.Util.clip;
import static util.Util.pyth;
import static util.functional.Util.by;
import static util.functional.Util.stream;

/**
 * Displays animated Voronoi diagram.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	author = "Martin Polakovic",
	name = "Voronoi",
	description = "Playground to experiment and visualize voronoi diagrams of moving points",
	howto = "To configure the visualization edit the source code.",
//	notes = "",
	version = "1",
	year = "2016",
	group = Widget.Group.VISUALISATION
)
public class Voronoi extends ClassController  {
	private static final Logger LOGGER = LoggerFactory.getLogger(Voronoi.class);
	private final RenderNode canvas = new RenderNode();

	public Voronoi() {
		getChildren().add(canvas);
		canvas.heightProperty().bind(heightProperty());
		canvas.widthProperty().bind(widthProperty());
		canvas.loop.start();
		setOnMouseClicked(e -> canvas.pause(false));
		focusedProperty().addListener((o,ov,nv) -> canvas.pause(!nv));
		addEventHandler(Event.ANY, Event::consume);
	}

	@Override
	public void onClose() {
		canvas.loop.stop();
	}

	private static class RenderNode extends Canvas {
		static Random rand = new Random();
		final Loop loop = new util.animation.Loop(this::loop);
		final GraphicsContext gc = getGraphicsContext2D();
		List<Cell> cells;       // delayed initialization
		P draggedCell = null;   // null if none
		P selectedCell = null;  // null if none
		P mousePos = null;      // null if outside
		long loopId = 0;        // loop id, autoincrement, used for simple loop control
		private final Map<Coordinate,Cell> inputOutputMap = new HashMap<>(); // maps inputs to polygons
		final V<Boolean> running = new V<>(true);

		public RenderNode() {
			// cell mouse dragging
			addEventHandler(MOUSE_DRAGGED, e -> {
				if (draggedCell!=null) {
					draggedCell.x = e.getX();
					draggedCell.y = e.getY();
				}
				mousePos = new P(e.getX(), e.getY());
			});
			addEventHandler(MOUSE_PRESSED, e -> draggedCell = selectedCell );
			addEventHandler(MOUSE_RELEASED, e -> draggedCell = null );
			// mouse position
			addEventHandler(MOUSE_MOVED, e -> {
				mousePos = new P(e.getX(), e.getY());
				selectedCell = cells.stream().sorted(by(c -> c.distance(mousePos))).findFirst().orElse(null);
			});
			addEventHandler(MOUSE_EXITED, e -> {
				mousePos = null;
				selectedCell = null;
			});
		}

		void loop() {
			double W = getWidth();
			double H = getHeight();
			if (W<=0 || H<=0) return;

			loopId++;
			//			if (loopId>1) return;

			// generate cells (runs only once, we need to do it here to make sure width & height is initialized)
			int cellCount = 40;
			if (cells == null) {
				// random
				cells = StreamEx.generate(() -> Cell.random(W, H, .5)).limit(cellCount).toList();
				// circle
				double wh = min(W,H);
				cells = DoubleStreamEx.iterate(0, a-> a+2*PI/11).limit(11)
						  .mapToObj(a -> new Cell(0,0) {
								double angle = a;
								{
									moving = (w,h) -> {
										angle += 0.001;
										x = wh/2+wh/20*cos(angle);
										y = wh/2+wh/20*sin(angle);
										x += randOf(-1,1)*randMN(0.0005,0.00051);
										y += randOf(-1,1)*randMN(0.0005,0.00051);
									};
								}
							})
							.map(c -> (Cell)c)
							.toList();
				cells.addAll(DoubleStreamEx.iterate(0, a-> a+2*PI/3).limit(3)
								 .mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh/2+wh/10*cos(angle);
											 y = wh/2+wh/10*sin(angle);
											 x += randOf(-1,1)*randMN(0.0005,0.00051);
											 y += randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								 .map(c -> (Cell)c)
								 .toList());
				cells.addAll(DoubleStreamEx.iterate(0, a-> a+2*PI/cellCount).limit(cellCount)
								 .mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh-wh/6+wh/8*cos(angle);
											 y = wh/6+wh/8*sin(angle);
											 x += randOf(-1,1)*randMN(0.0005,0.00051);
											 y += randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								 .map(c -> (Cell)c)
								 .toList());
				cells.addAll(DoubleStreamEx.iterate(0, a-> a+2*PI/cellCount).limit(cellCount)
								 .mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh/2+wh/4*cos(angle);
											 y = wh/2+wh/4*sin(angle);
											 x += randOf(-1,1)*randMN(0.0005,0.00051);
											 y += randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								 .map(c -> (Cell)c)
								 .toList());
				// horizontal sequence
//				cells = IntStreamEx.range(0,cellCount)
//			         .mapToObj(a -> new Cell(W*0.1+W*0.8/cellCount*a, H/2))
//			         .toList();

				// add noise to avoid arithmetic problem
				cells.forEach(cell -> {
					cell.x += randOf(-1,1)*randMN(0.01,0.012);
					cell.y += randOf(-1,1)*randMN(0.01,0.012);
				});

				cells.stream().filter(cell -> cell.moving==null)
					.forEach(c -> c.moving = (w,h) -> {
						double x = c.x+c.dx;
						double y = c.y+c.dy;
						if (x<0) {
							c.dx = -c.dx;
							c.x = -x;
						} else if (x>w) {
							c.dx = -c.dx;
							c.x = 2*w-x;
						} else
							c.x = x;

						if (y<0) {
							c.dy = -c.dy;
							c.y = -y;
						} else if (y>h) {
							c.dy = -c.dy;
							c.y = 2*h-y;
						} else
							c.y = y;
				});
			}

			// move cells
			cells.stream().filter(cell -> cell.moving!=null).forEach(cell -> cell.moving.accept(W,H));

			draw();
		}

		void pause(boolean v) {
			if (running.get()==v) return;
			if (v) loop.stop();
			else loop.start();
		}

		void draw() {
			inputOutputMap.clear();
			int W = (int) getWidth();
			int H = (int) getHeight();

			gc.setEffect(null);
			gc.setFill(Color.AQUA);
			gc.setStroke(Color.AQUA);
			gc.clearRect(0,0,W,H);
			gc.setGlobalAlpha(0.7);

			List<Coordinate> cords = stream(cells)
				 .map(cell -> {
					 Coordinate c = new Coordinate(cell.x, cell.y);
					 inputOutputMap.put(c,cell);
					 return c;
				 })
				 .toList();
			VoronoiDiagramBuilder diagram = new VoronoiDiagramBuilder();
			diagram.setClipEnvelope(new Envelope(0, W, 0, H));
			diagram.setSites(cords);

			// draw cells
			gc.save();

			// Unfortunately the computation can fail under some circumstances, so lets defend against it with Try
			Try.tryS(() -> diagram.getDiagram(new GeometryFactory()), Exception.class)
				.ifError(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOk(g ->
					IntStreamEx.range(0, g.getNumGeometries())
						.mapToObj(g::getGeometryN)
						.peek(polygon -> polygon.setUserData(inputOutputMap.get((Coordinate)polygon.getUserData())))
						.forEach(polygon -> {
							Cell cell = (Cell) polygon.getUserData();
							Coordinate[] cs = polygon.getCoordinates();
							double[] xs = new double[cs.length];
							double[] ys = new double[cs.length];
							for (int j = 0; j < cs.length; j++) {
								xs[j] = cs[j].x;
								ys[j] = cs[j].y;
							}

							boolean isSelected = selectedCell != null && cell.x == selectedCell.x && cell.y == selectedCell.y;
							boolean isDragged = draggedCell==null;
							if (isSelected) {
								gc.setGlobalAlpha(isDragged ? 0.1 : 0.2);
								gc.fillPolygon(xs, ys, polygon.getNumPoints());
								gc.setGlobalAlpha(1);
							}

							strokePolygon(gc, polygon);
						})
				);
			gc.applyEffect(new GaussianBlur(10));
			gc.restore();

			// draw lines
			if (mousePos!=null) {
				gc.save();
				double distMin = 0;
				double distMax = 0.2*pyth(W, H);
				double distDiff = distMax-distMin;
				for (Cell c : cells) {
					double dist = mousePos.distance(c.x, c.y);
					double distNormalized = 1-(clip(distMin, dist, distMax) - distMin)/distDiff;
					double opacity = 0.8*sqrt(distNormalized);
					gc.setGlobalAlpha(opacity);
					gc.strokeLine(c.x,c.y,mousePos.x,mousePos.y);
				}
				gc.restore();
			}

			// draw cell seeds
			gc.save();
			double r = 2;
			cells.forEach(c -> gc.fillOval(c.x-r,c.y-r,2*r,2*r));
			double rd = 4;
			if (selectedCell!=null) gc.fillOval(selectedCell.x-rd,selectedCell.y-rd,2*rd,2*rd);
			gc.restore();

		}

		static class P {
			double x=0,y=0;

			P(double x, double y) {
				this.x = x;
				this.y = y;
			}

			double distance(P p) {
				return distance(p.x,p.y);
			}

			double distance(double x, double y) {
				return sqrt((x-this.x)*(x-this.x)+(y-this.y)*(y-this.y));
			}
		}
		/**
		 * Line. 2 point connection. Mutable.
		 */
		class Lin {
			double x1, y1, x2, y2;

			public Lin(double x1, double y1, double x2, double y2) {
				this.x1 = x1;
				this.y1 = y1;
				this.x2 = x2;
				this.y2 = y2;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (!(o instanceof Lin)) return false;

				Lin l = (Lin) o;
				return (
						   (Double.compare(l.x1, x1)==0 && Double.compare(l.x2, x2)==0 &&
								Double.compare(l.y1, y1)==0 && Double.compare(l.y2, y2)==0) ||
							   (Double.compare(l.x1, x2)==0 && Double.compare(l.x2, x1)==0 &&
									Double.compare(l.y1, y2)==0 && Double.compare(l.y2, y1)==0)
				);
			}

			@Override
			public int hashCode() {
				int result;
				long temp;
				if (x1<x2) {
					temp = Double.doubleToLongBits(x1);
					result = (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
				} else {
					temp = Double.doubleToLongBits(x2);
					result = (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
				}
				return result;
			}
		}
		static class Cell extends P{
			public double dx=0, dy=0;
			public BiConsumer<Double,Double> moving = null;

			public Cell(double x, double y) {
				super(x, y);
			}

			public Cell moving(BiConsumer<Double,Double> moving) {
				this.moving = moving;
				return this;
			}

			static Cell random(double width, double height, double speed) {
				Cell c = new Cell(rand0N(width),rand0N(height));
				c.dx = rand0N(speed) - speed/2;
				c.dy = rand0N(speed) - speed/2;
				return c;
			}
		}
		static boolean randBoolean() {
			return rand.nextBoolean();
		}
		static double rand0N(double n) {
			return rand.nextDouble()*n;
		}
		static <T> T randOf(T a, T b) {
			return randBoolean() ? a : b;
		}
		static double randMN(double m, double n) {
			return m+Math.random()*(n-m);
		}
		static void strokePolygon(GraphicsContext gc, Geometry polygon) {
			Coordinate[] cs = polygon.getCoordinates();
			for (int j=0; j<cs.length-1; j++)
				gc.strokeLine(cs[j].x, cs[j].y, cs[j+1].x, cs[j+1].y);
			gc.strokeLine(cs[0].x, cs[0].y, cs[cs.length-1].x, cs[cs.length-1].y);
		}
	}
}