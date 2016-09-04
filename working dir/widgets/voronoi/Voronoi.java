package voronoi;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

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
import util.animation.Loop;

import static java.util.stream.Collectors.toList;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static util.Util.sqrΣ;
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
		Loop loop = new util.animation.Loop(this::draw);
		GraphicsContext gc = getGraphicsContext2D();
		List<Cell> cells;
		Cell draggedCell = null;

		public RenderNode() {
			// cell mouse dragging
			addEventHandler(MOUSE_DRAGGED, e -> {
				if (draggedCell!=null) {
					draggedCell.x = e.getX();
					draggedCell.y = e.getY();
				}
			});
			addEventHandler(MOUSE_PRESSED, e ->
				draggedCell = cells.stream().sorted(by(c -> sqrΣ(c.x-e.getX(), c.y-e.getY()))).findFirst().orElse(null)
			);
			addEventHandler(MOUSE_RELEASED, e ->
				draggedCell = null
			);
		}

		void draw() {
			int width = (int) getWidth();
			int height = (int) getHeight();
			if (width<=0 || height<=0) return;
			if (cells == null) cells = Stream.generate(() -> randomCell(width, height, .5)).limit(100).collect(toList());

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

			gc.setEffect(null);
			gc.setFill(Color.AQUA);
			gc.setStroke(Color.AQUA);
			gc.clearRect(0,0,width,height);

			PowerDiagram diagram = new PowerDiagram();

			// normal list based on an array
			OpenList sites = new OpenList();

			// create a root polygon which limits the voronoi diagram.
			//  here it is just a rectangle.
			PolygonSimple rootPolygon = new PolygonSimple();
			rootPolygon.add(0, 0);
			rootPolygon.add(width, 0);
			rootPolygon.add(width, height);
			rootPolygon.add(0, height);

			cells.forEach(c -> sites.add(new Site(c.x, c.y)));
			double r = 2;
			cells.forEach(c -> gc.fillOval(c.x-r,c.y-r,2*r,2*r));
			double rd = 4;
			if (draggedCell!=null) gc.fillOval(draggedCell.x-rd,draggedCell.y-rd,2*rd,2*rd);
			if (draggedCell!=null) gc.fillOval(draggedCell.x-rd,draggedCell.y-rd,2*rd,2*rd);

			gc.setEffect(new GaussianBlur(15));

			// set the list of points (sites), necessary for the power diagram
			diagram.setSites(sites);
			// set the clipping polygon, which limits the power voronoi diagram
			diagram.setClipPoly(rootPolygon);

			// do the computation
			diagram.computeDiagram();
			// for each site we can no get the resulting polygon of its cell.
			// note that the cell can also be empty, in this case there is no polygon for the corresponding site.
			for (int i=0;i< sites.size; i++){
				Site site = sites.array[i];
				PolygonSimple polygon=site.getPolygon();

				if (polygon!=null && polygon.getNumPoints()>1)
//					StreamEx.of(polygon.iterator()).forPairs((p1, p2) -> {
//						if (p1!=null && p2!=null)
//							drawLine(p1.x, p2.x, p1.y, p2.y);
//					});
				gc.strokePolygon(toDouble(polygon.getXpointsClosed()), toDouble(polygon.getYpointsClosed()), polygon.getNumPoints());
			}

		}

		void drawLine(double x, double y, double tox, double toy) {
//			not working? why
//			gc.moveTo(10, 10);
//			gc.lineTo(50, 50);

			// imitate using dotted line algorithm
			double dist = Math.sqrt((x-tox)*(x-tox)+(y-toy)*(y-toy));
			double cos = (x-tox)/dist;
			double sin = (y-toy)/dist;
			drawLine(x, y, dist, cos, sin);
		}

		void drawLine(double x, double y, double length, double dirCos, double dirSin) {
			for (double i=0; i<length; i+=1)
				gc.fillOval(x+i*dirCos, y+i*dirSin, 1,1);
		}

		double[] toDouble(int[] is) {
			double[] ds = new double[is.length];
			for (int x=0; x<is.length; x++) ds[x] = is[x];
			return ds;
		}

		static class Cell {
			double x, y, dx, dy;
		}

		static Cell randomCell(double width, double height, double speed) {
			Cell c = new Cell();
			c.x = rand0N(width);
			c.y = rand0N(height);
			c.dx = rand0N(speed) - speed/2;
			c.dy = rand0N(speed) - speed/2;
			return c;
		}
		static double rand0N(double n) {
			return rand.nextDouble()*n;
		}
	}
}