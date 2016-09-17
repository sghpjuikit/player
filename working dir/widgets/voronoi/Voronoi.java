package voronoi;

import java.util.List;
import java.util.Random;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;

import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import one.util.streamex.StreamEx;
import util.animation.Loop;

import static java.lang.Math.sqrt;
import static javafx.scene.input.MouseEvent.*;
import static util.Util.clip;
import static util.Util.pyth;
import static util.functional.Util.by;

/**
 * Displays animated Voronoi diagram.
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	author = "Martin Polakovic",
	name = "Voronoi",
	description = "Displays real time visualisation of voronoi diagram of moving points",
	howto = "",
	notes = "",
	version = "1",
	year = "2016",
	group = Widget.Group.VISUALISATION
)
public class Voronoi extends ClassController  {
	private final RenderNode canvas = new RenderNode();

	public Voronoi() {
		getChildren().add(canvas);
		canvas.heightProperty().bind(heightProperty());
		canvas.widthProperty().bind(widthProperty());
		canvas.loop.start();
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
			int width = (int) getWidth();
			int height = (int) getHeight();
			if (width<=0 || height<=0) return;

			loopId++;
			//			if (loopId>1) return;

			// generate cells (runs only once, we need to do it here to make sure width & height is initialized)
			int cellCount = 40;
			if (cells == null) {
				// random
				cells = StreamEx.generate(() -> Cell.random(width, height, .5)).limit(cellCount).toList();
				// circle
				//				double wh = min(width,height);
				//				cells = DoubleStreamEx.iterate(0, a-> a+2*PI/cellCount).limit(cellCount)
				//			                          .mapToObj(a -> new Cell(wh/2+wh/10*cos(a), wh/2+wh/10*sin(a)))
				//			                          .toList();
				//				cells.add(new Cell(wh/2+10, wh/2+10));
				//				cells.add(new Cell(wh/2+10, wh/2-10));
				//				cells.add(new Cell(wh/2-10, wh/2-10));
				//				cells.add(new Cell(wh/2-10, wh/2+10));
				// horizontal sequence
				//				cells = IntStream.range(0,cellCount)
				//						         .mapToObj(a -> new Cell(width*0.1+width*0.8/cellCount*a, height/2))
				//						         .toList();
			}


			// move cells
			cells.forEach(c -> {
				double x = c.x+c.dx;
				double y = c.y+c.dy;
				if (x<0) {
					c.dx = -c.dx;
					c.x = -x;
				} else if (x>width) {
					c.dx = -c.dx;
					c.x = 2*width-x;
				} else
					c.x = x;

				if (y<0) {
					c.dy = -c.dy;
					c.y = -y;
				} else if (y>height) {
					c.dy = -c.dy;
					c.y = 2*height-y;
				} else
					c.y = y;
			});

			draw();
		}

		void draw() {
			int width = (int) getWidth();
			int height = (int) getHeight();

			gc.setEffect(null);
			gc.setFill(Color.AQUA);
			gc.setStroke(Color.AQUA);
			gc.clearRect(0,0,width,height);

			OpenList sites = new OpenList();
			cells.forEach(c -> sites.add(new Site(c.x, c.y)));

			// create a root polygon which limits the voronoi diagram. Here it is just a rectangle.
			PolygonSimple rootPolygon = new PolygonSimple();
			rootPolygon.add(0, 0);
			rootPolygon.add(width, 0);
			rootPolygon.add(width, height);
			rootPolygon.add(0, height);

			PowerDiagram diagram = new PowerDiagram();
			diagram.setSites(sites);
			diagram.setClipPoly(rootPolygon);

			// do the computation
			// unfortunately it can fail under some conditions, so we recover -> cancel this & all fture loops
			// we could just stop this loop and continue with the next one, but once the computation fails, it
			// will most probably fail again, at which point it can quickly crash the entire VM.
			try {
				diagram.computeDiagram();
			} catch (Exception e) {
				e.printStackTrace();
				loop.stop();
				return;
			}

			// draw cells
			gc.save();
			gc.setEffect(new GaussianBlur(15));
			for (Site site : sites) {
				// for each site we can now get the resulting polygon of its cell.
				// note that the cell can also be empty, in which case there is no polygon for the corresponding site.
				PolygonSimple polygon = site.getPolygon();
				if (polygon!=null && polygon.getNumPoints()>1) {
					boolean isSelected = selectedCell != null && site.x == selectedCell.x && site.y == selectedCell.y;
					boolean isDragged = draggedCell==null;
					if (isSelected) {
						gc.save();
						gc.setGlobalAlpha(isDragged ? 0.1 : 0.2);
						gc.fillPolygon(toDouble(polygon.getXpointsClosed()), toDouble(polygon.getYpointsClosed()), polygon.getNumPoints());
						gc.restore();
					}
					gc.strokePolygon(toDouble(polygon.getXpointsClosed()), toDouble(polygon.getYpointsClosed()), polygon.getNumPoints());
				}
			}
			gc.restore();

			// draw lines
			if (mousePos!=null) {
				gc.save();
				double distMin = 0;
				double distMax = 0.2*pyth(width, height);
				double distDiff = distMax-distMin;
				for (Cell c : cells) {
					double dist = mousePos.distance(c.x, c.y);
					double distNormalized = 1-(clip(distMin, dist, distMax) - distMin)/distDiff;
					double opacity = 0.8*sqrt(distNormalized);
					gc.setGlobalAlpha(opacity);
					drawLine(c.x,c.y,mousePos.x,mousePos.y);
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

		void drawLine(double x, double y, double tox, double toy) {
//			not working? why
//			gc.moveTo(x, y);
//			gc.lineTo(tox, toy);
//			gc.stroke();

			// imitate using dotted line algorithm
			double dist = sqrt((x-tox)*(x-tox)+(y-toy)*(y-toy));
			double cos = (tox-x)/dist;
			double sin = (toy-y)/dist;
			drawLine(x, y, dist, cos, sin);
		}

		void drawLine(double x, double y, double length, double dirCos, double dirSin) {
			for (double i=0; i<length; i+=2)
				gc.fillOval(x+i*dirCos, y+i*dirSin, 1,1);
		}

		double[] toDouble(int[] is) {
			double[] ds = new double[is.length];
			for (int x=0; x<is.length; x++) ds[x] = is[x];
			return ds;
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
		static class Cell extends P{
			double dx=0, dy=0;

			public Cell(double x, double y) {
				super(x, y);
			}

			static Cell random(double width, double height, double speed) {
				Cell c = new Cell(rand0N(width),rand0N(height));
				c.dx = rand0N(speed) - speed/2;
				c.dy = rand0N(speed) - speed/2;
				return c;
			}
		}
		static double rand0N(double n) {
			return rand.nextDouble()*n;
		}
	}
}