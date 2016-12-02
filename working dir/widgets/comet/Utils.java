package comet;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import org.gamepad4j.Controllers;
import org.gamepad4j.IController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import comet.Comet.*;
import comet.Comet.Game.Mission;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.objects.Text;
import gui.objects.icon.Icon;
import gui.pane.OverlayPane;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import unused.TriConsumer;
import util.R;
import util.SwitchException;
import util.animation.Anim;
import util.collections.Tuple2;
import util.collections.map.ClassMap;
import util.collections.map.Map2D;
import util.collections.mapset.MapSet;
import util.functional.Functors.Ƒ0;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.reactive.SetƑ;

import static comet.Comet.Constants.FPS;
import static comet.Utils.Achievement.*;
import static gui.objects.icon.Icon.createInfoIcon;
import static java.lang.Double.max;
import static java.lang.Math.*;
import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Pos.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.paint.Color.rgb;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static util.Util.clip;
import static util.Util.pyth;
import static util.collections.Tuples.tuple;
import static util.dev.Util.throwIf;
import static util.dev.Util.throwIfNot;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;

/**
 * @author Martin Polakovic
 */
interface Utils {

	// superscript 	⁰ 	¹ 	²	³	⁴ 	⁵ 	⁶ 	⁷ 	⁸ 	⁹ 	⁺ 	⁻ 	⁼ 	⁽ 	⁾ 	ⁿ
	// subscript 	₀ 	₁ 	₂ 	₃ 	₄ 	₅ 	₆ 	₇ 	₈ 	₉ 	₊ 	₋ 	₌ 	₍ 	₎

	Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	double PI = Math.PI;
	double D0 = 0;
	double D30 = PI/6;
	double D45 = PI/4;
	double D60 = PI/3;
	double D90 = PI/2;
	double D120 = D90+D30;
	double D180 = PI;
	double D360 = 2*PI;
	double SIN45 = Math.sin(PI/4);
	int precision = 100;
	int UNITS = 360*precision;
	double[] degSinMemo = IntStreamEx.rangeClosed(-UNITS,UNITS).mapToDouble(i -> i/(double)precision).map(angle -> Math.sin(rad(angle))).toArray();
	double[] degCosMemo = IntStreamEx.rangeClosed(-UNITS,UNITS).mapToDouble(i -> i/(double)precision).map(angle -> Math.cos(rad(angle))).toArray();
	Random RAND = new Random();
	// Tele-Marines is packed with windows 8.1, but to be sure it works on any version and
	// platform it is packed with the widget.
	Font UI_FONT = Font.loadFont(Utils.class.getResourceAsStream("Tele-Marines.TTF"), 12.0);
	double HUD_DOT_GAP = 3;
	double HUD_DOT_DIAMETER = 1;

	/** Converts radians to degrees, mathematically 360*rad/(2*PI). */
	static double deg(double rad) {
		return Math.toDegrees(rad);
	}
	/** Converts degrees to radians. */
	static double rad(double deg) {
		return Math.toRadians(deg);
	}
	static double sinD(double deg) {
		return degSinMemo[((int)Math.round(deg*precision))%UNITS+UNITS];
	}
	static double cosD(double deg) {
		return degCosMemo[((int)Math.round(deg*precision))%UNITS+UNITS];
	}
	static double sin(double rad) {
		return degSinMemo[((int)Math.round(deg(rad)*precision))%UNITS+UNITS];
	}
	static double cos(double rad) {
		return degCosMemo[((int)Math.round(deg(rad)*precision))%UNITS+UNITS];
	}
	static double sign(double number) {
		return Math.signum(number);
	}
	static double sqr(double d) {
		return d*d;
	}
	static double sqrt(double d) {
		return Math.sqrt(d);
	}
	/** Returns angle in rad for given sin and cos. */
	static double dirOf(double x, double y, double dist) {
		double c = x/dist;
		double s = y/dist;
		if (c>0) return asin(s);
		return (s<0) ? acos(c) : acos(c)+PI/2;
	}
	static double dirDiff(double dirFrom, double dirTo) {
		double diff = dirTo%D360-dirFrom%D360;
		if (diff>+PI) return diff-D360;
		if (diff<-PI) return diff+D360;
		return diff;
	}

	static double computeForceInversePotential(double distance, double maxDistance) {
		return distance >= maxDistance ? 1 : distance/maxDistance;
	}

	/**
	 * Creates array of fire angles for specified number of turrets. Angles are a symmetrical
	 * sequence with 0 in the middle and consistent angle gap in between each.
	 */
	static Double[] calculateGunTurretAngles(int i, double gap) {
		if (i<=1) return array(0d);
		return ( i%2==1
			? range(-i/2d,i/2d)  // ... -3 -2 -1 0 +1 +2 +3 ...
			: stream(range(0.5-i/2,-0.5),range(0.5,i/2-0.5))   // ... -1.5 -0.5 +0.5 +1.5 ...
		).map(x -> gap*x)
		 .toArray(Double[]::new);
	}

	/** Relocates node such the center of the node is at the coordinates. */
	static void relocateCenter(Node n, double x, double y) {
		n.relocate(x-n.getLayoutBounds().getWidth()/2,y-n.getLayoutBounds().getHeight()/2);
	}

	static Node createPlayerStat(Player p) {
		Label score = new Label();
		installFont(score, UI_FONT);
		p.score.maintain(s -> score.setText("Score: " + s));

		Label nameL = new Label();
		installFont(nameL, UI_FONT);
		maintain(p.name,nameL::setText);

		HBox lives = layHorizontally(5, CENTER_LEFT);
		repeat(p.lives.get(), () -> lives.getChildren().add(createPlayerLiveIcon()));
		p.lives.onChange((ol,nl) -> {
			repeat(ol-nl, i -> {
				Icon ic = (Icon)lives.getChildren().get(lives.getChildren().size()-1-i);
				createHyperSpaceAnim(ic).playOpenDo(() -> lives.getChildren().remove(ic));
			});
			repeat(nl-ol, () -> {
				Icon ic = createPlayerLiveIcon();
				lives.getChildren().add(ic);
				createHyperSpaceAnim(ic).intpl(x -> 1-x).playOpen();
			});
		});

		Label energy = new Label();
		installFont(energy, UI_FONT);
		p.energy.maintain(e -> energy.setText("Energy: " + e.intValue()));

		VBox node = layVertically(5, CENTER_LEFT, nameL,score,lives,energy);
		node.setMaxHeight(VBox.USE_PREF_SIZE); // fixes alignment in parent by not expanding this box
		node.setPrefWidth(140); // fixes position changing on children resize
		node.setUserData(p.id.get()); // to recognize which belongs to which
		return node;
	}

	static void installFont(Labeled l, Font f) {
		Font ft = f==null ? Font.getDefault() : f;
		l.setFont(ft);
		l.setStyle("{ -fx-font: \"" + ft.getFamily() + "\"; }"); // bug fix, suddenly !work without this...
	}

	static Icon createPlayerLiveIcon() {
		return new Icon(MaterialDesignIcon.ROCKET,15);
	}

	static Anim createHyperSpaceAnim(Node n) {
		return new Anim(millis(300), x -> setScaleXY(n,1-x*x));
	}

	static void createHyperSpaceAnimIn(Game game, PO o) {
		o.graphicsScale = 0;
		game.runNext.addAnim01(millis(300), x -> o.graphicsScale = x*x);
	}

	static void createHyperSpaceAnimOut(Game game, PO o) {
		o.graphicsScale = 1;
		game.runNext.addAnim01(millis(300), x -> o.graphicsScale = 1-x*x);
	}

	/** Snapshot an image out of a node, consider transparency. */
	static Image createImage(Node n) {
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);

		int imageWidth = (int) n.getBoundsInLocal().getWidth();
		int imageHeight = (int) n.getBoundsInLocal().getHeight();

		WritableImage wi = new WritableImage(imageWidth, imageHeight);
		n.snapshot(parameters, wi);

		return wi;
	}
	/** Creates image from icon. */
	static Image graphics(GlyphIcons icon, double radius, Color c, Effect effect) {
		Icon i = new Icon(icon,radius);
		i.setFill(c);
		i.setEffect(effect);
		return createImage(i);
	}
	/**
	 * Sets the transform for the GraphicsContext to rotate around a pivot point.
	 *
	 * @param gc the graphics context the transform to applied to.
	 * @param angle the angle of rotation in degrees.
	 * @param px the x pivot co-ordinate for the rotation (in canvas co-ordinates).
	 * @param py the y pivot co-ordinate for the rotation (in canvas co-ordinates).
	 */
	static Affine rotate(GraphicsContext gc, double angle, double px, double py) {
		Affine a = gc.getTransform();
		Rotate r = new Rotate(angle, px, py);
		gc.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
		return a;
	}
	/** Adjusts color opacity. */
	static Color color(Color c, double opacity) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity);
	}
	/**
	 * Draws an image on a graphics context.
	 *
	 * The image is drawn at (tlpx, tlpy) rotated by angle pivoted around the point:
	 *   (tlpx + image.getWidth() / 2, tlpy + image.getHeight() / 2)
	 *
	 * @param gc the graphics context the image is to be drawn on
	 * @param angle the angle of rotation in degrees
	 * @param x the x co-ordinate of the centre of the image
	 * @param y the y co-ordinate of the centre of the image
	 */
	static void drawImageRotated(GraphicsContext gc, Image i, double angle, double x, double y) {
		Affine a = rotate(gc, angle, x, y);
		gc.drawImage(i, x - i.getWidth()/2, y - i.getHeight()/2);
		gc.setTransform(a);
	}
	static void drawImageRotatedScaled(GraphicsContext gc, Image i, double angle, double x, double y, double scale) {
		if (scale==0) return;   // optimization
		double w = i.getWidth()*scale, h = i.getHeight()*scale;
		Affine a = rotate(gc, angle, x, y);
		gc.drawImage(i, x-w/2, y-h/2, w, h);
		gc.setTransform(a);
	}
	static void drawImage(GraphicsContext gc, Image i, double x, double y) {
		gc.drawImage(i, x+i.getWidth()/2, y+i.getHeight()/2, i.getWidth(), i.getHeight());
	}
	static void drawScaledImage(GraphicsContext gc, Image i, double x, double y, double scale) {
		gc.drawImage(i, x-scale*(i.getWidth()/2), y-scale*(i.getHeight()/2), scale*i.getWidth(), scale*i.getHeight());
	}
	static void drawOval(GraphicsContext g, double x, double y, double r) {
		double d = 2*r;
		g.fillOval(x-r,y-r,d,d);
	}
	static void drawTriangle(GraphicsContext gc, double x, double y, double r, double dir, double angleOffset) {
		gc.beginPath();
		gc.moveTo(
			x+r*cos(dir),
			y+r*sin(dir)
		);
		gc.lineTo(
			x+r*cos(dir+angleOffset),
			y+r*sin(dir+angleOffset)
		);
		gc.lineTo(
			x+r*cos(dir-angleOffset),
			y+r*sin(dir-angleOffset)
		);
		gc.closePath();
		gc.fill();
	}
	static void strokeLine(GraphicsContext g, double x, double y, double length, double angleRad) {
		g.strokeLine(x,y,x+length*cos(angleRad),y+length*sin(angleRad));
	}
	static void drawRect(GraphicsContext g, double x, double y, double r) {
		double d = 2*r;
		g.fillRect(x-r,y-r,d,d);
	}
	static void drawFading(Game g, DoubleConsumer drawCommand) {
		g.runNext.addAnim01(millis(200), p -> drawCommand.accept(1-p));
	}
	static void drawFading(Game g, Duration ttl, DoubleConsumer drawCommand) {
		g.runNext.addAnim01(ttl, p -> drawCommand.accept(1-p));
	}
	static void drawHudLine(GraphicsContext gc, GameSize field, double x, double y, double length, double dirCos, double dirSin) {
		for (double i=0; i<length; i+=HUD_DOT_GAP)
			gc.fillOval(field.modX(x+i*dirCos), field.modY(y+i*dirSin), 1,1);
	}
	static void drawHudLine(GraphicsContext gc, GameSize field, double x, double y, double lengthStart, double length, double cosDir, double sinDir, Color color) {
		gc.setFill(color);
		for (double i=lengthStart; i<length; i+=HUD_DOT_GAP)
			gc.fillOval(field.modX(x+i*cosDir), field.modY(y+i*sinDir), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
	}
	static void drawHudLine(GraphicsContext gc, GameSize field, double x, double y, double tox, double toy) {
		double dist = pyth(x-tox, y-toy);
		double distX = tox-x, distY = toy-y;
		double X = x, Y = y;
		int pieces = (int)(dist/HUD_DOT_GAP);
		double dx = distX/pieces, dy = distY/pieces;
		for (double i=0; i<pieces; i++) {
			gc.fillOval(field.modX(X),field.modY(Y), 1,1);
			X += dx; Y += dy;
		}
	}
	static void drawHudCircle(GraphicsContext gc, GameSize field, double x, double y, double r, double angle, double angleWidth, Color color) {
		gc.setFill(color);
		double length = angleWidth*r;
		int pieces = (int)(length/HUD_DOT_GAP);
		double angleStart = angle-angleWidth/2;
		double angleBy = angleWidth/pieces;
		for (int p=0; p<pieces; p++) {
			double a = angleStart+p*angleBy;
			gc.fillOval(field.modX(x+r*cos(a)), field.modY(y+r*sin(a)), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
		}
	}
	static void drawHudCircle(GraphicsContext gc, GameSize field, double x, double y, double r, Color color) {
		drawHudCircle(gc, field, x, y, r, 0, D360, color);
	}
	static void drawHudPolygon(GraphicsContext gc, GameSize field, double[] xs, double[] ys, long pointCount) {
		throwIfNot(xs.length==ys.length);

		for (int j=0; j<pointCount; j++) {
			int k = j==pointCount-1 ? 0 : j+1;
			double x1 = xs[j], x2 = xs[k], y1 = ys[j], y2 = ys[k];
			if (field.isInside(x1,y1) || field.isInside(x2,y2))
				drawHudLine(gc, field, x1,y1,x2,y2);
		}
	}

	static double ttl(Duration d) {
		return d.toSeconds()*FPS;
	}
	static double ttlVal(double val, Duration d) {
		return val/ttl(d);
	}
	static Duration time(long frames) {
		return seconds(frames/FPS);
	}
	static Duration timeOfLoop(long loopIndex) {
		return millis(loopIndex*FPS);
	}

	static double randMN(double m, double n) {
		return m+random()*(n-m);
	}
	static double rand0N(double n) {
		return RAND.nextDouble()*n;
	}
	static double rand01() {
		return RAND.nextDouble();
	}
	static int rand0or1() {
		return randBoolean() ? 0 : 1;
	}
	static int randInt(int n) {
		return RAND.nextInt(n);
	}
	static double randAngleRad() {
		return rand0N(2*PI);
	}
	static double randAngleDeg() {
		return rand0N(360);
	}
	static boolean randBoolean() {
		return RAND.nextBoolean();
	}
	static <E extends Enum> E randEnum(Class<E> enumType) {
		return randOf(enumType.getEnumConstants());
	}
	static <T> T randOf(T a, T b) {
		return randBoolean() ? a : b;
	}
	@SafeVarargs
	static <T> T randOf(T... c) {
		throwIf(c.length==0);
		return c[randInt(c.length)];
	}
	static <T> T randOf(Collection<T> c) {
		throwIf(c.isEmpty());
		int size = c.size();
		return c.stream().skip((long)(random()*(max(0,size)))).findAny().orElse(null);
	}

	enum Side {
		LEFT,RIGHT
	}
	enum GunControl {
		AUTO,MANUAL
	}
	enum AbilityState {
		ACTIVATING, PASSSIVATING, OFF, ON
	}
	enum AbilityKind {
		NONE,
		HYPERSPACE,
		DISRUPTOR,
		SHIELD;

		Ship.Ability create(Ship s) {
			switch(this) {
				case NONE : return null;
				case DISRUPTOR : return s.new Disruptor();
				case HYPERSPACE : return s.new Hyperspace();
				case SHIELD : return s.new Shield();
				default: throw new SwitchException(this);
			}
		}

	}
	enum Relations {
		ALLY, NEUTRAL, ENEMY
	}
	enum PlayerSpawn {
		CIRCLE,
		LINE,
		RECTANGLE;

		double computeStartingAngle(int ps, int p) {
			switch(this) {
				case CIRCLE : return ps==0 ? 0 : p*D360/ps;
				case LINE :
				case RECTANGLE : return -D90;
			}
			throw new SwitchException(this);
		}

		@SuppressWarnings("unused")
		double computeStartingX(double w, double h, int ps, int p) {
			switch(this) {
				case CIRCLE : return w/2 + 50*cos(computeStartingAngle(ps, p));
				case LINE : return w/(ps+1)*p;
				case RECTANGLE : {
					int a = ((int)ceil(sqrt(ps)));
					return w/(a+1)*(1+(p-1)/a);
				}
			}
			throw new SwitchException(this);
		}

		@SuppressWarnings("unused")
		double computeStartingY(double w, double h, int ps, int p) {
			switch(this) {
				case CIRCLE : return h/2 + 50*sin(computeStartingAngle(ps, p));
				case LINE : return h/2;
				case RECTANGLE : {
					int a = ((int)ceil(sqrt(ps)));
					return h/(a+1)*(1+(p-1)%a);
				}
			}
			throw new SwitchException(this);
		}
	}

	class Displayable {
		final String name;
		final String description;
		final GlyphIcons icon;

		public Displayable(String name, GlyphIcons icon, CharSequence... description) {
			this.name = name;
			this.description = String.join("\n", description);
			this.icon = icon;
		}
	}
	class Achievement extends Displayable {
		final Ƒ1<Game,Set<Player>> evaluator;
		Predicate<? super Game> condition;

		private Achievement(String NAME, GlyphIcons ICON, Ƒ1<Game,Set<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			super(NAME, ICON, DESCRIPTION);
			evaluator = EVALUATOR;
		}

		static Achievement achievement1(String NAME, GlyphIcons ICON, Ƒ1<? super Game,? extends Player> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> singleton(EVALUATOR.apply(game)), DESCRIPTION);
		}

		static Achievement achievement01(String NAME, GlyphIcons ICON, Ƒ1<? super Game,? extends Optional<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> EVALUATOR.apply(game).stream().collect(toSet()), DESCRIPTION);
		}

		static Achievement achievement0N(String NAME, GlyphIcons ICON, Ƒ1<Game,Stream<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> EVALUATOR.apply(game).collect(toSet()), DESCRIPTION);
		}

		public Achievement onlyIf(Predicate<? super Game> CONDITION) {
			this.condition = CONDITION;
			return this;
		}
	}

	/** How to play help pane. */
	class HowToPane extends OverlayPane<Game> {
		private final GridPane g = new GridPane();
		private final Icon helpI = createInfoIcon("How to play");

		public HowToPane() {
			display.set(Display.WINDOW);

			ScrollPane sp = new ScrollPane();
					   sp.setOnScroll(Event::consume);
					   sp.setContent(layStack(g, CENTER));
					   sp.setFitToWidth(true);
					   sp.setFitToHeight(false);
					   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
					   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			VBox l = layHeaderTop(5, CENTER,
				layHorizontally(5,CENTER_RIGHT, helpI),
				layStack(sp, CENTER)
			);
			l.setMaxWidth(800);
			l.maxHeightProperty().bind(heightProperty().subtract(100));
			setContent(l);
		}

		@Override
		public void show(Game game) {
			super.show();

			// clear content
			g.getChildren().clear();
			g.getRowConstraints().clear();
			g.getColumnConstraints().clear();

			// build columns
			g.getColumnConstraints().add(new ColumnConstraints(100,100,100, NEVER, HPos.RIGHT, false));
			g.getColumnConstraints().add(new ColumnConstraints(20));
			g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

			// build rows
			R<Integer> i = new R<>(-1); // row index
			game.ROCKET_ENHANCERS.stream()
				.sorted(by(enhancer -> enhancer.name))
				.forEach(enhancer -> {
					i.setOf(v -> v+1);

					Icon icon = new Icon(enhancer.icon, 20);
					Label nameL = new Label(enhancer.name);
					Text descL = new Text(enhancer.description);
						 descL.setWrappingWidth(400);
					g.add(icon, 0,i.get());
					g.add(nameL, 2,i.get());
					i.setOf(v -> v+1);
					g.add(descL, 2,i.get());
					i.setOf(v -> v+1);
					g.add(new Label(), 2,i.get()); // empty row
				});
		}
	}
	/** Mission details help pane. */
	class MissionPane extends OverlayPane<Mission> {
		private final Text text = new Text();
		private final Icon helpI = createInfoIcon("Mission details");

		public MissionPane() {
			display.set(Display.WINDOW);

			ScrollPane sp = new ScrollPane();
			sp.setOnScroll(Event::consume);
			sp.setContent(layStack(text, CENTER));
			sp.setFitToWidth(true);
			sp.setFitToHeight(false);
			sp.setHbarPolicy(ScrollBarPolicy.NEVER);
			sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			VBox l = layHeaderTop(5, CENTER,
				layHorizontally(5,CENTER_RIGHT, helpI),
				layStack(sp, CENTER)
			);
			l.setMaxWidth(800);
			l.maxHeightProperty().bind(heightProperty().subtract(100));
			setContent(l);
		}

		@Override
		public void show(Mission mission) {
			super.show();

			text.setText(
				"Name: " + mission.name + "\n" +
					"Scale: " + mission.scale + "\n\n" +
					mission.details
			);
		}
	}
	/** How to play help pane. */
	class EndGamePane extends OverlayPane<Map<Player,List<Achievement>>> {
		private final GridPane g = new GridPane();
		private final Icon helpI = createInfoIcon("How to play");

		public EndGamePane() {
			display.set(Display.WINDOW);

			ScrollPane sp = new ScrollPane();
			sp.setOnScroll(Event::consume);
			sp.setContent(layStack(g, CENTER));
			sp.setFitToWidth(true);
			sp.setFitToHeight(false);
			sp.setHbarPolicy(ScrollBarPolicy.NEVER);
			sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			VBox l = layHeaderTop(5, CENTER,
				layHorizontally(5,CENTER_RIGHT, helpI),
				layStack(sp, CENTER)
			);
			l.setMaxWidth(800);
			l.maxHeightProperty().bind(heightProperty().subtract(100));
			setContent(l);
		}

		@Override
		public void show(Map<Player,List<Achievement>> game) {
			super.show();

			// clear content
			g.getChildren().clear();
			g.getRowConstraints().clear();
			g.getColumnConstraints().clear();

			// build columns
			g.getColumnConstraints().add(new ColumnConstraints(100,100,100, NEVER, HPos.RIGHT, false));
			g.getColumnConstraints().add(new ColumnConstraints(20));
			g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

			// build rows
			R<Integer> i = new R<>(-1); // row index
			game.keySet().stream()
				.sorted(by(player -> player.name.get()))
				.forEach(player -> {
					i.setOf(v -> v+1);
					g.add(new Label(player.name.get()), 0,i.get());
					i.setOf(v -> v+1);
					g.add(new Label(), 2,i.get()); // empty row

					game.get(player).stream()
						.sorted(by(achievement -> achievement.name))
						.forEach(enhancer -> {
							i.setOf(v -> v+1);

							Icon icon = new Icon(enhancer.icon, 20);
							Label nameL = new Label(enhancer.name);
							Text descL = new Text(enhancer.description);
							descL.setWrappingWidth(400);
							g.add(icon, 0,i.get());
							g.add(nameL, 2,i.get());
							i.setOf(v -> v+1);
							g.add(descL, 2,i.get());
							i.setOf(v -> v+1);
							g.add(new Label(), 2,i.get()); // empty row
						});

					g.add(new Label(), 2,i.get()); // empty row
					g.add(new Label(), 2,i.get()); // empty row
				});
		}
	}

	/** Weighted boolean - stores how many times it is. False if not once. True if at least once. */
	class InEffect extends InEffectValue<Void> {
		InEffect() {
			super(t -> null);
		}
		InEffect(Consumer<Integer> onChange) {
			super(0, t -> null, onChange);
		}
	}
	class InEffectValue<T> {
		private int times = 0;
		private T value;
		private final Ƒ1<Integer,T> valueCalc;
		private final Consumer<Integer> changeApplier;

		InEffectValue(int times_init, Ƒ1<Integer,T> valueCalculator, Consumer<Integer> onChange) {
			times = times_init;
			valueCalc = valueCalculator;
			changeApplier = onChange;
			value = valueCalc.apply(times);
		}
		InEffectValue(int times_init, Ƒ1<Integer,T> valueCalculator) {
			this(times_init, valueCalculator, null);
		}
		InEffectValue(Ƒ1<Integer,T> valueCalculator) {
			this(0, valueCalculator);
		}

		boolean is() {
			return times>0;
		}

		int isTimes() {
			return times;
		}

		T value() {
			return value;
		}

		InEffectValue inc() {
			times++;
			value = valueCalc.apply(times);
			if (changeApplier!=null) changeApplier.accept(times);
			return this;
		}

		InEffectValue dec() {
			if (times<=0) return this;
			times--;
			value = valueCalc.apply(times);
			if (changeApplier!=null) changeApplier.accept(times);
			return this;
		}

		void reset() {
			if (times!=0) {
				times = 0;
				value = valueCalc.apply(times);
				if (changeApplier!=null) changeApplier.accept(times);
			}
		}
	}
	class TimeDouble implements Runnable {
		public double value;
		public final double by;

		public TimeDouble(double start, double by) {
			this.value = start;
			this.by = by;
		}

		@Override
		public void run() {
			value += by;
		}

		public double get() {
			return value;
		}

		public double getAndRun() {
			double d = value;
			run();
			return d;
		}

		public double runAndGet() {
			run();
			return value;
		}
	}
	class ObjectStore<O> {
		private final Map<Class,Set<O>> m = new HashMap<>();
		private final Ƒ1<O,Class> mapper;

		ObjectStore(Ƒ1<O,Class> classMapper) {
			mapper = classMapper;
		}

		void add(O o) {
			m.computeIfAbsent(mapper.apply(o), c -> new HashSet<>()).add(o);
		}

		void remove(O o) {
			Set l = m.get(mapper.apply(o));
			if (l!=null) l.remove(o);
		}

		@SuppressWarnings("unchecked")
		<T extends O> Set<T> get(Class<T> c) {
			return (Set<T>) m.getOrDefault(c,setRO());
		}

		void clear() {
			m.values().forEach(Set::clear); m.clear();
		}

		void forEach(Consumer<? super O> action) {
			m.forEach((k,set) -> set.forEach(action));
		}

		@SuppressWarnings("unchecked")
		<T extends O> void forEachT(Class<T> c, Consumer<? super T> action) {
			Set<T> l = (Set<T>) m.get(c);
			if (l!=null) l.forEach(action);
		}

		@SuppressWarnings("unchecked")
		<T extends O,E extends O> void forEachTE(Class<T> t, Class<E> e, BiConsumer<? super T,? super E> action) {
			if (t==e) forEachCartesianHalfNoSelf(get(t), (BiConsumer)action);
			else forEachPair(get(t),get(e), action);
		}

		void forEachSet(Consumer<? super Set<O>> action) {
			m.values().forEach(action);
		}
	}
	class Pool<P> {
		private final List<P> pool;
		 final int max;
		private final Ƒ0<P> fac;

		 Pool(int max_size, Ƒ0<P> factory) {
			max = max_size;
			fac = factory;
			pool = new ArrayList<>(max_size);
		}

		void add(P p) {
			if (pool.size()<max) pool.add(p);
		}
		P get() {
			P n = pool.isEmpty() ? null : pool.get(0);
			if (n!=null) pool.remove(0);
			return n==null ? fac.apply() : n;
		}

		int sizeNow(){ return pool.size(); }
		int sizeMax(){ return max; }
		void clear() { pool.clear(); }
	}
	class PoolMap<P> {
		private final ClassMap<Pool<P>> pools = new ClassMap<>();
		private final ClassMap<Ƒ1<Class,Pool<P>>> factories = new ClassMap<>();

		 PoolMap() {
		}

		void registerPool(Class<?> type, Ƒ0<Pool<P>> poolFactory) {
			factories.put(type, c -> poolFactory.apply());
		}
		void add(Class<?> type, P p) {
			pools.computeIfAbsent(type,factories.get(type)).add(p);
		}
		P get(Class<?> type) {
			return pools.computeIfAbsent(type,factories.get(type)).get();
		}
		void clear() {
			pools.values().forEach(Pool::clear);
		}
	}
	class SpatialHash {
		private final int xMax;
		private final int yMax;
		private final int bucketSpan;
		private final Set<PO>[][] a;

		@SuppressWarnings("unchecked")
		SpatialHash(int width, int height, int bucket_SPAN) {
			xMax = width;
			yMax = height;
			bucketSpan = bucket_SPAN;
			a = new Set[xMax][yMax];
			for (int i = 0; i< xMax; i++)
				for (int j = 0; j< yMax; j++)
					a[i][j] = new HashSet<>();
		}

		void add(Collection<PO> os) {
			for (PO o : os) {
				int i = (int) o.x/bucketSpan;
				int j = (int) o.y/bucketSpan;
				a[i][j].add(o);
			}
//            for (PO o : os) {
//                int minI = (int) (o.x-o.hit_radius)/bucketSpan;
//                int minJ = (int) (o.y-o.hit_radius)/bucketSpan;
//                int maxI = (int) (o.x+o.hit_radius)/bucketSpan;
//                int maxJ = (int) (o.y+o.hit_radius)/bucketSpan;
//                for (int i=minI; i<=maxI; i++)
//                    for (int j=minJ; j<maxJ; j++)
//                        a[i][j].add(o);
//            }
		}
		void forEachClose(PO o, BiConsumer<? super PO, ? super PO> action) {
			int minI = (int) (o.x-o.radius)/bucketSpan;
			int minJ = (int) (o.y-o.radius)/bucketSpan;
			int maxI = (int) (o.x+o.radius)/bucketSpan;
			int maxJ = (int) (o.y+o.radius)/bucketSpan;
			for (int i=minI; i<=maxI; i++)
				for (int j=minJ; j<maxJ; j++)
					for (PO e : a[i][j])
						action.accept(o,e);
		}
	}
	class CollisionHandlers {
		private final Map2D<Class<? extends PO>,Class<? extends PO>,BiConsumer<? super PO,? super PO>> hs = new Map2D<>();

		@SuppressWarnings("unchecked")
		public <A extends PO, B extends PO> void add(Class<A> type1, Class <B> type2, BiConsumer<? super A,? super B> handler) {
			hs.put(type1, type2, (BiConsumer)handler);
		}

		@SuppressWarnings("unchecked")
		public <A extends PO, B extends PO> void forEach(TriConsumer<Class<A>,Class<B>,BiConsumer<? super A,? super B>> action) {
			hs.forEach((TriConsumer)action);
		}
	}

	class TTLList implements Runnable {
		final List<Ttl> lt = new ArrayList<>();
		final List<TtlC> ltc = new ArrayList<>();
		final List<PTtl> lpt = new ArrayList<>();
		final SetƑ lr = new SetƑ();
		final Set<Runnable> temp = new HashSet<>();

		/** Adds runnable that will run next time this runs. */
		void add(Runnable r) {
			lr.add(r);
		}

		/** Adds runnable that will run after this runs specified number of times. */
		void add(double times, Runnable r) {
			lt.add(new Ttl(times, r));
		}

		/** Adds runnable that will run after specified time. */
		void add(Duration delay, Runnable r) {
			lt.add(new Ttl(ttl(delay), r));
		}

		/**
		 * Adds an animation.
		 * <p/>
		 * Adds runnable that will consume double every time this runs during the specified time from now on.
		 * The double is interpolated from 0 to 1 by pre-calculated step from duration.
		 */
		void addAnim01(Duration dur, DoubleConsumer r) {
			ltc.add(new TtlC(0,1,1/ ttl(dur), r));
		}

		/**
		 * Adds an animation.
		 * <p/>
		 * Adds runnable that will consume double every time this runs during the specified time from now on.
		 * The double is interpolated between specified values by pre-calculated step from duration.
		 */
		void addAnim(double from, double to, Duration dur, DoubleConsumer r) {
			ltc.add(new TtlC(from,to,(to-from)/ ttl(dur), r));
		}

		void addPeriodic(Duration delay, Runnable r) {
			lpt.add(new PTtl(() -> ttl(delay), r));
		}
		void addPeriodic(Ƒ0<Double> ttl, Runnable r) {
			lpt.add(new PTtl(ttl, r));
		}

		void remove(Runnable r) {
			lt.removeIf(t -> t.r==r);
			lpt.removeIf(t -> t.r==r);
			lr.remove(r);
		}

		public void run() {
			ltc.forEach(Runnable::run);
			ltc.removeIf(TtlC::isDone);

			for (int i=lt.size()-1; i>=0; i--) {
				Ttl t = lt.get(i);
				if (t.ttl>1) t.ttl--;
				else {
					lt.remove(i);
					temp.add(t);
				}
			}
			temp.forEach(Runnable::run);
			temp.clear();

			for (int i=lpt.size()-1; i>=0; i--) {
				PTtl t = lpt.get(i);
				if (t.ttl>1) t.ttl--;
				else t.run();
			}

			lr.apply();
			lr.clear();
		}

		void clear() {
			lt.clear();
			lr.clear();
			lpt.clear();
		}
	}
	class Ttl implements Runnable {
		double ttl;
		final Runnable r;

		Ttl(double TTL, Runnable R) {
			ttl = TTL;
			r = R;
		}

		public void run() {
			r.run();
		}
	}
	class TtlC implements Runnable {
		double ttl;
		double ttlTo;
		double ttlBy;
		boolean isIncrement;
		final DoubleConsumer r;

		TtlC(double TtlFrom, double TtlTo, double TtlBy, DoubleConsumer R) {
			ttl = TtlFrom;
			ttlTo = TtlTo;
			ttlBy = TtlBy;
			isIncrement = ttlBy>0;
			if (ttlBy ==0) throw new IllegalArgumentException("Infinite runnable");
			r = R;
		}

		public void run() {
			ttl += ttlBy;
			r.accept(ttl);
		}

		 boolean isDone() {
			return (isIncrement && ttl>= ttlTo) || (!isIncrement && ttl<= ttlTo);
		}
	}
	class PTtl extends Ttl {
		final Ƒ0<Double> ttlPeriod;

		PTtl(Ƒ0<Double> TTL, Runnable R) {
			super(0, R);
			ttlPeriod = TTL;
			ttl = TTL.apply();
		}

		public void run() {
			r.run();
			ttl = ttlPeriod.apply();
		}

	}

	interface Stats<T> {
		void clear();
	}
	class StatsGame implements Stats<Game> {
		Player firstDead = null;

		public void playerDied(Player p) {
			if (firstDead!=null)
				firstDead = p;
		}

		@Override
		public void clear() {

		}
	}
	class StatsPlayer implements Stats<Player> {
		public long bulletFiredCount;
		public long spawnCount;
		public long deathCount;
		public Duration fired1stTime;
		public Duration hitEnemy1stTime;
		public long killUfoCount;
		public List<Duration> liveTimes;
		private Duration lastSpawn;
		private Duration longestAlive;
		public DoubleSummaryStatistics controlAreaSize;
		public DoubleSummaryStatistics controlAreaCenterDistance;
		public double distanceTravelled;
		public long asteroidRamCount;

		public StatsPlayer() {
			clear();
		}

		public void accSpawn(long loopId) {
			spawnCount++;
			lastSpawn = timeOfLoop(loopId);
			liveTimes.add(lastSpawn);
			longestAlive = stream(liveTimes).max(Duration::compareTo).orElseThrow(AssertionError::new);
		}

		public void accDeath(long loopId) {
			deathCount++;
			Duration death = timeOfLoop(loopId);
			liveTimes.add(death.subtract(lastSpawn));
		}

		public void accGameEnd(long loopId) {
			Duration end = timeOfLoop(loopId);
			liveTimes.add(lastSpawn==null ? end : end.subtract(lastSpawn));
		}

		public void accFiredBullet(long loopId) {
			bulletFiredCount++;
			if (fired1stTime==null) fired1stTime = timeOfLoop(loopId);
		}

		public void accHitEnemy(long loopId) {
			if (hitEnemy1stTime==null) hitEnemy1stTime = timeOfLoop(loopId);
		}

		public void accKillUfo() {
			killUfoCount++;
		}

		public void accTravel(double distance) {
			distanceTravelled += distance;
		}

		public void accRamAsteroid() {
			asteroidRamCount++;
		}

		@Override
		public void clear() {
			controlAreaSize = new DoubleSummaryStatistics();
			controlAreaCenterDistance = new DoubleSummaryStatistics();
			bulletFiredCount = 0;
			fired1stTime = null;
			hitEnemy1stTime = null;
			killUfoCount = 0;
			liveTimes = new ArrayList<>();
			lastSpawn = null;
			longestAlive = null;
			spawnCount = 0;
			deathCount = 0;
			distanceTravelled = 0;
			asteroidRamCount = 0;
		}
	}

	class Loop extends util.animation.Loop {
		/** Loop id, starts at 0, incremented by 1 every loop. */
		public long id = 0;
		/** The timestamp of the current loop given in milliseconds. */
		public long now = 0;

		public Loop(Runnable action) {
			super(action);
			reset();
		}

		public final void reset() {
			id = 0;
			now = 0;
		}

		@Override
		protected void doLoop(long now) {
			this.id++;
			this.now = now/1000000;
			super.doLoop(now);
		}

		boolean isNth(long n) {
			return id % n == 0;
		}

		boolean isNth(Duration d) {
			return isNth((long) ttl(d));
		}
	}
	class GameSize {
		double width=0, height=0, diagonal=0, area=0, length=0;

		GameSize resize(double width, double height) {
			this.width = width;
			this.height = height;
			diagonal = sqrt(width*width+height*height);
			length = 2*(width+height);
			area = width*height;
			return this;
		}

		boolean isInside(double x, double y) {
			return x>=0 && y>=0 && x<=width && y<=height;
		}

		boolean isInsideX(double x) {
			return x>=0 && x<=width;
		}

		boolean isInsideY(double y) {
			return y>=0 && y<=height;
		}

		boolean isOutside(double x, double y) {
			return !isInside(x, y);
		}

		boolean isOutsideX(double x) {
			return !isInsideX(x);
		}

		boolean isOutsideY(double y) {
			return !isInsideY(y);
		}

		double clipInsideX(double x) {
			if (x<0) return 0;
			if (x>width) return width;
			return x;
		}

		double clipInsideY(double y) {
			if (y<0) return 0;
			if (y>height) return height;
			return y;
		}

		/** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.width)*/
		public double modX(double x) {
			if (x<0) return modX(width+x);
			else if (x>width) return modX(x-width);
			else return x;
		}

		/** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.height)*/
		public double modY(double y) {
			if (y<0) return modY(height+y);
			else if (y>height) return modY(y-height);
			else return y;
		}

		public double distX(double x1, double x2) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return abs(x1-x2);

			if (x1<x2) return min(x2-x1, x1+width-x2);
			else return min(x1-x2, x2+width-x1);
		}

		public double distY(double y1, double y2) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return abs(y1-y2);

			if (y1<y2) return min(y2-y1, y1+height-y2);
			else return min(y1-y2, y2+height-y1);
		}

		public double distXSigned(double x1, double x2) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return x1-x2;

			if (x1<x2) {
				double d1 = x2-x1;
				double d2 = x1+width-x2;
				return d1<d2 ? -d1 : d2;
			} else {
				double d1 = x1-x2;
				double d2 = x2+width-x1;
				return d1<d2 ? d1 : -d2;
			}
		}

		public double distYSigned(double y1, double y2) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return y1-y2;

			if (y1<y2) {
				double d1 = y2-y1;
				double d2 = y1+height-y2;
				return d1<d2 ? -d1 : d2;
			} else {
				double d1 = y1-y2;
				double d2 = y2+height-y1;
				return d1<d2 ? d1 : -d2;
			}
		}

		public double dist(double x1, double y1, double x2, double y2) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));

			double dx = distX(x1, x2);
			double dy = distY(y1, y2);
			return sqrt(dx*dx+dy*dy);
		}

		public double dist(double distX, double distY) {
			return sqrt(distX*distX+distY*distY);
		}

		public boolean isDistLess(double x1, double y1, double x2, double y2, double as) {
			// because we use modular coordinates (infinite field connected by borders), distance
			// calculation needs a tweak
			// return (x1-x2)*(x1-x2)+(y1-y2)*(y1-y2) < as*as;

			double dx = distX(x1, x2);
			double dy = distY(y1, y2);
			return dx*dx+dy*dy < as*as;
		}
	}
	interface Events {
		Object PLANETOID_DESTROYED = new Object();
		Object PLAYER_NO_LIVES_LEFT = new Object();
		Object COMMAND_NEXT_MISSION = new Object();
	}
	/** Loop object - object with per loop behavior. Executes once per loop. */
	interface LO {
		void doLoop();
		default void dispose() {}
	}
	interface Play extends LO {
		void init();
		void start(int player_count);
		void doLoop();
		void handleEvent(Object event);
		void stop();
		void pause(boolean v);
	}
	abstract class GameMode implements Play {
		protected Game game;

		public GameMode(Game game) {
			this.game = game;
		}

		public Set<Achievement> achievements() {
			return set();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}
	class ClassicMode extends GameMode {
		final MapSet<Integer,Mission> missions;
		int mission_counter = 0;   // mission counter, starts at 1, increments by 1
		boolean isMissionScheduled = false;
		boolean isMissionStartPlanetoidSplitting = false;
		final Set<Achievement> achievements;

		public ClassicMode(Game game) {
			super(game);
			missions = new MapSet<>(m -> m.id,
//				new Mission(
//					1, "Energetic fragility","10⁻¹⁵","",
//					null, Color.RED,Color.rgb(255,255,255,0.015), null,(a,b,c,d,e) -> game.owner.new Particler(a,b,c,d,e)
////					Color.RED,Color.rgb(0,0,0,0.08), null,(a,b,c,d,e) -> game.owner.new Particler(a,b,c,d,e)
//				).initializer(game -> game.useGrid = false, game -> game.useGrid = true),
				game.new Mission(
					1, "The strange world", "10⁻⁴m", "",
//					null,Color.BLACK, Color.rgb(225,225,225, 0.2), (a,b,c,d,e) -> game.owner.new PlanetoDisc(a,b,c,d,e)
					Color.LIGHTGREEN, rgb(0,51,51, 0.1), (a,b,c,d,e) -> game.owner.new PlanetoDisc(a,b,c,d,e)
				),
				game.new Mission(
					2, "Sumi-e","10⁻¹⁵","",
					Color.LIGHTGREEN, rgb(0, 51, 51, 0.1), (a,b,c,d,e) -> game.owner.new Inkoid(a,b,c,d,e)
				),
				game.new Mission(
					3, "Mol's molecule","","",
					Color.YELLOW, rgb(0, 15, 0, 0.1), (a,b,c,d,e) -> game.owner.new Fermi(a,b,c,d,e)
				),
				game.new Mission(
					4, "PartiCuLar elEment","10⁻¹⁵","",
					Color.GREEN, rgb(0, 15, 0, 0.08), (a,b,c,d,e) -> game.owner.new Fermi(a,b,c,d,e)
				),
				game.new Mission(
					5, "Decay of the weak force","10⁻¹","",
					Color.GREEN, rgb(0, 15, 0, 0.08), (a,b,c,d,e) -> game.owner.new Fermi(a,b,c,d,e)
				),
				game.new Mission(
					6, "String a string","10⁻¹⁵","",
					Color.YELLOW, rgb(10, 11, 1, 0.2),(a,b,c,d,e) -> game.owner.new Stringoid(a,b,c,d,e)
				), //new Glow(0.3)
				game.new Mission(
					7, "Mother of all branes","10⁻¹⁵","",
					Color.DODGERBLUE, rgb(0, 0, 15, 0.08), (a,b,c,d,e) -> game.owner.new Genoid(a,b,c,d,e)
				),
				game.new Mission(
					8, "Energetic fragility","10⁻¹⁵","",
					Color.DODGERBLUE, rgb(10,10,25,0.08), (a,b,c,d,e) -> game.owner.new Energ(a,b,c,d,e)
				),
				game.new Mission(
					9, "Planc's plancton","10⁻¹⁵","",
					Color.DARKCYAN, new Color(0,0.08,0.08,0.09),(a,b,c,d,e) -> game.owner.new Linker(a,b,c,d,e)
				)//,
//				new Mission(10, "T duality of a planck boundary","10⁻¹⁵","",
//					Color.DARKSLATEBLUE,new Color(1,1,1,0.08),null,Energ2::new
//				),
//				new Mission(11, "Informative xperience","10⁻¹⁵","",
//					Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
//				),
//				new Mission(12, "Holographically principled","10⁻¹⁵","",
//					Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
//				)
			);
			achievements = set(
				achievement1(
					"Dominator", MaterialDesignIcon.DUMBBELL,
					g -> stream(g.players).max(by(p -> p.stats.controlAreaSize.getAverage())).get(),
					"Control the largest nearby area throughout the game"
				).onlyIf(g -> g.players.size()>1),
				achievement1(
					"Control freak", MaterialDesignIcon.ARROW_EXPAND,
					g -> stream(g.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
					"Control your nearby area the most effectively"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Reaper's favourite", MaterialDesignIcon.HEART_BROKEN,
					g -> Optional.ofNullable(g.stats.firstDead),
					"Be the first to die"
				).onlyIf(g -> g.players.size()>1),
				achievement1(
					"Live and prosper", MaterialDesignIcon.HEART,
					g -> stream(g.players).max(by(p -> stream(p.stats.liveTimes).max(Duration::compareTo).get())).get(),
					"Live the longest"
				).onlyIf(g -> g.players.size()>1),
				achievement0N(
					"Invincible", MaterialDesignIcon.MARKER_CHECK,
					g -> stream(g.players).filter(p -> p.stats.deathCount==0),
					"Don't die"
				),
				achievement01(
					"Quickdraw", MaterialDesignIcon.CROSSHAIRS,
					g -> stream(g.players).min(by(p -> p.stats.fired1stTime, nullsLast(Comparable::compareTo))),
					"Be the first to shoot"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Rusher", MaterialDesignIcon.CROSSHAIRS_GPS,
					g -> stream(g.players).min(by(p -> p.stats.hitEnemy1stTime, nullsLast(Comparable::compareTo))),
					"Be the first to deal damage"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Mobile", MaterialDesignIcon.RUN,
					g -> stream(g.players).maxBy(p -> p.stats.distanceTravelled),
					"Travel the greatest distance"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Crusher", FontAwesomeIcon.TRUCK,
					g -> stream(g.players).maxBy(p -> p.stats.asteroidRamCount),
					"Destroy the most asteroids with your kinetic shield"
				).onlyIf(g -> g.players.size()>1),
				achievement0N(
					"Pacifist", MaterialDesignIcon.NATURE_PEOPLE,
					g -> stream(g.players).filter(p -> p.stats.fired1stTime==null),
					"Never shoot"
				),
				// TODO: fix this for situations where killCount is the same for multiple players
				achievement01(
					"Hunter", MaterialDesignIcon.BIOHAZARD,
					g -> stream(g.players).maxBy(p -> p.stats.killUfoCount),
					"Kill most UFOs"
				).onlyIf(g -> g.players.size()>1)
			);
		}

		@Override
		public void init() {}

		@Override
		public void start(int player_count) {
			mission_counter = 0;
			isMissionScheduled = false;
			nextMission();
		}

		@Override
		public void doLoop() {}

		@Override
		public void handleEvent(Object event) {
			if (event==Events.PLANETOID_DESTROYED) {
				// it may take a cycle or two for asteroids to get disposed, hence the delay
				game.runNext.add(10, () -> {
					if (game.oss.get(Asteroid.class).isEmpty())
						nextMission();
				});
			}
			if (event==Events.PLAYER_NO_LIVES_LEFT) {
				if (game.players.stream().allMatch(p -> !p.alive && p.lives.get()<=0))
					game.over();
			}
			if (event==Events.COMMAND_NEXT_MISSION) {
				nextMission();
			}
		}

		@Override
		public void stop() {}

		@Override
		public void pause(boolean v) {}

		@Override
		public Set<Achievement> achievements() {
			return achievements;
		}

		protected void nextMission() {
			// schedule
			if (isMissionScheduled) return;
			isMissionScheduled = true;

			// get mission
			int firstMissionId = 2;
			if (game.mission!=null) game.mission.disposer.accept(game);
			mission_counter = mission_counter==0 ? firstMissionId : mission_counter+1;
			int id = mission_counter%missions.size();
			int mission_id = id==0 ? missions.size() : mission_counter%missions.size(); // modulo mission count, but start at 1
			Mission mNew = missions.get(mission_id);
			Mission mOld = game.mission==null ? mNew : game.mission;

			// start mission
			game.mission = mNew;
			boolean isEasy = mission_counter<4;
			double size = sqrt(game.field.height)/1000;
			int planetoidCount = 3 + (int)(2*(size-1)) + (mission_counter-1) + game.players.size()/2;
			int planetoids = isEasy ? planetoidCount/2 : planetoidCount;
			double delay = ttl(seconds(mission_counter==1 ? 2 : 5));
			game.runNext.add(delay/2, () -> game.message("Level " + mission_counter, mNew.name));
			game.runNext.add(delay, () -> repeat(planetoids, i -> mNew.spawnPlanetoid()));
			if (isEasy) {
				isMissionStartPlanetoidSplitting = true;
				game.runNext.add(delay, () -> {
					game.oss.get(Asteroid.class).forEach(a -> a.split(null));
					isMissionStartPlanetoidSplitting = false;
				});
			}
			game.runNext.add(delay, () -> isMissionScheduled = false);
			game.mission.initializer.accept(game);

			// transition color scheme
			game.runNext.add(delay/2, () ->
				game.runNext.addAnim01(millis(300), p -> {
					game.color = mOld.color.interpolate(mNew.color, p);
					game.colorCanvasFade = mOld.colorCanvasFade.interpolate(mNew.colorCanvasFade, p);
					game.humans.color = mOld.color.interpolate(mNew.color, p);
					game.humans.colorTech = mOld.color.interpolate(mNew.color, p);
					game.ufos.color = mOld.color.interpolate(mNew.color, p);
					game.grid.color = mOld.color.interpolate(mNew.color, p);
				})
			);
		}
	}
	class UfoHellMode extends ClassicMode {
		private final Game game;

		public UfoHellMode(Game game) {
			super(game);
			this.game = game;
		}

		@Override
		public void init() {
			super.init();
		}

		@Override
		public void start(int player_count) {
			super.start(player_count);

			game.settings = new Settings();
			game.settings.useGrid = false;
			game.settings.playerGunDisabled = true;
			game.settings.UFO_BULLET_TTL *= 2;
			game.settings.UFO_BULLET_SPEED /= 3;
			game.settings.player_ability_auto_on = true;
			game.settings.DISRUPTOR_E_RATE = 0;
			game.settings.DISRUPTOR_E_ACTIVATION = 0;
			game.settings.UFO_GUN_RELOAD_TIME = millis(20);

			game.runNext.addPeriodic(seconds(4), game.ufos::sendUfo);
			game.players.forEach(p -> p.ability_type.set(AbilityKind.DISRUPTOR));
		}

		@Override
		public void doLoop() {
			super.doLoop();
		}

		@Override
		public void handleEvent(Object event) {
			super.handleEvent(event);
		}

		@Override
		public void stop() {
			super.stop();
		}

		@Override
		public void pause(boolean v) {
			super.pause(v);
		}
	}
	class BounceHellMode extends ClassicMode {
		private final Game game;

		public BounceHellMode(Game game) {
			super(game);
			this.game = game;
		}

		@Override
		public void init() {
			super.init();
		}

		@Override
		public void start(int player_count) {
			super.start(player_count);

			game.settings = new Settings();
			game.settings.playerGunDisabled = true;
			game.settings.player_ability_auto_on = true;
			game.settings.SHIELD_E_RATE = 0;
			game.settings.SHIELD_E_ACTIVATION = 0;

			game.players.forEach(p -> p.ability_type.set(AbilityKind.SHIELD));
		}

		@Override
		public void doLoop() {
			super.doLoop();
		}

		@Override
		public void handleEvent(Object event) {
			super.handleEvent(event);

			if (event==Events.PLANETOID_DESTROYED && !isMissionStartPlanetoidSplitting) {
				// game.mission.spawnPlanetoid();
				// tun next cycle to avoid possible concurrent modification error
				game.runNext.add(game.mission::spawnPlanetoid);
			}
		}

		@Override
		public void stop() {
			super.stop();
		}

		@Override
		public void pause(boolean v) {
			super.pause(v);
		}
	}

	/**
	 * 2d vector. Mutable.
	 */
	class Vec {
		double x;
		double y;

		Vec(double x, double y) {
			this.x = x;
			this.y = y;
		}

		Vec(Vec v) {
			set(v);
		}

		void set(Vec v) {
			this.x = v.x;
			this.y = v.y;
		}

		void set(double x, double y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * Multiplies this vector by the specified scalar value.
		 * @param scale the scalar value
		 */
		void mul(double scale) {
			x *= scale;
			y *= scale;
		}

		/**
		 * Sets the value of this vector to the difference
		 * of vectors t1 and t2 (this = t1 - t2).
		 * @param t1 the first vector
		 * @param t2 the second vector
		 */
		void setSub(Vec t1, Vec t2) {
			this.x = t1.x - t2.x;
			this.y = t1.y - t2.y;
		}

		/**
		 * Sets the value of this vector to the difference of
		 * itself and vector t1 (this = this - t1) .
		 * @param t1 the other vector
		 */
		void sub(Vec t1) {
			this.x -= t1.x;
			this.y -= t1.y;
		}

		/**
		 * Sets the value of this vector to the sum
		 * of vectors t1 and t2 (this = t1 + t2).
		 * @param t1 the first vector
		 * @param t2 the second vector
		 */
		void setAdd(Vec t1, Vec t2) {
			this.x = t1.x + t2.x;
			this.y = t1.y + t2.y;
		}

		void setResult(DoubleUnaryOperator f) {
			this.x = f.applyAsDouble(x);
			this.y = f.applyAsDouble(y);
		}

		/**
		 * Sets the value of this vector to the sum of
		 * itself and vector t1 (this = this + t1) .
		 * @param t1 the other vector
		 */
		void add(Vec t1) {
			this.x += t1.x;
			this.y += t1.y;
		}
		void addMul(double s, Vec t1) {
			this.x += s*t1.x;
			this.y += s*t1.y;
		}

		/**
		 * Sets the value of this vector to the mul
		 * of vectors t1 and t2 (this = t1 * t2).
		 * @param t1 the first vector
		 * @param t2 the second vector
		 */
		void setMul(Vec t1, Vec t2) {
			this.x = t1.x * t2.x;
			this.y = t1.y * t2.y;
		}

		/**
		 * Sets the value of this vector to the mul
		 * of vectors t1 and scalar s (this = s * t1).
		 * @param v vector
		 * @param s scalar
		 */
		void setMul(double s, Vec v) {
			this.x = s * v.x;
			this.y = s * v.y;
		}

		/**
		 * Returns the length of this vector.
		 * @return the length of this vector
		 */
		double length() {
			return Math.sqrt(x*x + y*y);
		}

		/**
		 * Returns the length of this vector squared.
		 * @return the length of this vector squared
		 */
		double lengthSqr() {
			return x*x + y*y;
		}

		double distX(Vec to) {
			return x>to.x ? x-to.x : to.x-x;
		}

		double distY(Vec to) {
			return y>to.y ? y-to.y : to.y-y;
		}

		double dist(Vec to) {
			return sqrt((x-to.x)*(x-to.x) + (y-to.y)*(y-to.y));
		}

		double distSqr(Vec to) {
			return (x-to.x)*(x-to.x) + (y-to.y)*(y-to.y);
		}

		Vec diff(Vec to) {
			return new Vec(x-to.x, y-to.y);
		}

		void normalize() {
			double norm = 1.0 / length();
			this.x = x * norm;
			this.y = y * norm;
		}

		double dot(Vec v1) {
			return this.x * v1.x + this.y * v1.y;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Vec)) return false;

			Vec v = (Vec) o;
			return Double.compare(v.x, x)==0 && Double.compare(v.y, y)==0;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits(x);
			result = (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(y);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "Vec[" + x + ", " + y + "]";
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
	/**
	 * Voronoi diagram computation for player rockets.
	 * <p/>
	 * @implNote  Class is abstract to abstracts away from the algorithm, which seems to not be implemented robustly
	 * in libraries, so I wrap it in a class, which also provides optimization for number of points 0 and 1.
	 */
	class Voronoi {
		private final BiConsumer<Rocket,Double> areaAction;
		private final BiConsumer<Rocket,Double> distAction;
		private final BiConsumer<Double,Double> centerAction;
		private final Consumer<Stream<Lin>> edgesAction;
		private final Map<Coordinate,Tuple2<Rocket,Boolean>> inputOutputMap = new HashMap<>(8*9); // maps input (rockets) to polygons

		public Voronoi(BiConsumer<Rocket, Double> areaAction, BiConsumer<Rocket, Double> distAction, BiConsumer<Double,Double> centerAction, Consumer<Stream<Lin>> edgesAction) {
			this.areaAction = areaAction;
			this.distAction = distAction;
			this.edgesAction = edgesAction;
			this.centerAction = centerAction;
		}

		public void compute(Set<Rocket> rockets, double W, double H, Game game) {
			int size = rockets.size();
			// has no solution for 0 points
			if (size==0) return;
			// optimization for known solution for 1 points
			if (size==1) {
				Rocket r = rockets.stream().findFirst().get();
				areaAction.accept(r, W*H);
				distAction.accept(r, 0d);
				centerAction.accept(r.x, r.y);
				edgesAction.accept(stream());
			// or we fall back to algorithm
			} else {
				doCompute(rockets, W, H, game);
			}
		}

		@SuppressWarnings("unchecked")
		protected void doCompute(Set<Rocket> rockets, double W, double H, Game game) {
			inputOutputMap.clear();
			List<Coordinate> cells = stream(rockets)
				.flatMap(rocket -> {
					Vec r = new Vec(rocket.x+rocket.cacheRandomVoronoiTranslation, rocket.y+rocket.cacheRandomVoronoiTranslation);
					Coordinate cMain = new Coordinate(r.x, r.y);
					inputOutputMap.put(cMain,tuple(rocket,true));
					return stream(
							new Coordinate(r.x + W, r.y), new Coordinate(r.x, r.y + H), new Coordinate(r.x - W, r.y), new Coordinate(r.x, r.y - H),
							new Coordinate(r.x + W, r.y + H), new Coordinate(r.x + W, r.y - H), new Coordinate(r.x - W, r.y + H), new Coordinate(r.x - W, r.y - H)
						)
						.peek(c -> inputOutputMap.put(c,tuple(rocket,false)))
						.append(cMain);
					}
				)
				.toList();

			VoronoiDiagramBuilder voronoi = new VoronoiDiagramBuilder();
			voronoi.setClipEnvelope(new Envelope(0, W, 0, H));
			voronoi.setSites(cells);
			Try.tryS(() -> voronoi.getDiagram(new GeometryFactory()), Exception.class)
				.ifError(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOk(g ->
					edgesAction.accept(IntStreamEx.range(0, g.getNumGeometries())
						.mapToObj(g::getGeometryN)
						.peek(polygon -> polygon.setUserData(inputOutputMap.get((Coordinate)polygon.getUserData())))
						.groupingBy(polygon -> ((Tuple2<Rocket, Boolean>) polygon.getUserData())._1)
						.values().stream()
						.flatMap(ss -> {
							List<Lin> lines = stream(ss)
								.peek(polygon -> {
									Tuple2<Rocket,Boolean> data = (Tuple2<Rocket,Boolean>) polygon.getUserData();
									Rocket rocket = data._1;
									Boolean isMain = data._2;
									if (isMain) {
										Point c = polygon.getCentroid();
										centerAction.accept(c.getX(), c.getY());
										areaAction.accept(rocket, polygon.getArea());
										distAction.accept(rocket, game.field.dist(c.getX(), c.getY(), rocket.x, rocket.y));
									}
								})
								.filter(polygon -> game.humans.intelOn.is())
								// optimization: return edges -> draw edges instead of polygons, we can improve performance
								 .flatMap(polygon -> {
									 Coordinate[] cs = polygon.getCoordinates();
									 double[] xs = new double[cs.length];
									 double[] ys = new double[cs.length];
									 for (int j = 0; j < cs.length; j++) {
										 xs[j] = cs[j].x;
										 ys[j] = cs[j].y;
									 }

//									 game.drawPolygon(xs,ys,cs.length);
									 Stream.Builder s = Stream.builder();
									 for (int j=0; j<cs.length; j++) {
										int k = j==cs.length-1 ? 0 : j+1;
										double x1 = xs[j], x2 = xs[k], y1 = ys[j], y2 = ys[k];
										if ((x1>=0 && y1>=0 && x1<=W && y1<=H) || (x2>=0 && y2>=0 && x2<=W && y2<=H))
											s.add(new Lin(x1,y1,x2,y2));
									 }
									 return s.build();
								 }).toList();
//						        .groupingBy(x -> x, counting())
//						        .entrySet().stream()
//						        .peek(e -> System.out.println(e.getValue()))
//						        .filter(e -> e.getValue()==1)
//						        .map(Entry::getKey)
							Set<Lin> linesUnique = new HashSet<>();
							Set<Lin> linesDuplicate = stream(lines).filter(n -> !linesUnique.add(n)).toSet();
							linesUnique.removeAll(linesDuplicate);
							return linesUnique.stream();
						})
						// optimization: draw each edge only once by removing duplicates with Set and proper hashCode()
						.distinct()
					)
				);
		}
	}

	/**
	 * Graphical 2D grid with warp effects.
	 * Based on:
	 * <a href="http://gamedevelopment.tutsplus.com/tutorials/make-a-neon-vector-shooter-in-xna-the-warping-grid--gamedev-9904">neon-vector-shooter-in-xna</a>
	 *
	 */
	class Grid {
		boolean enabled = true;
		Spring[] springs;
		PointMass[][] points;
		GraphicsContext gc;
		Color color = rgb(30, 30, 139, 0.85);
		double WIDTH;
		double HEIGHT;
		int thick_frequency = 5;

		 Grid(GraphicsContext gc, double width, double height, double gap) {
			this.gc = gc;
			this.WIDTH = width;
			this.HEIGHT = height;
			List<Spring> springList = new ArrayList<>();

			int numColumns = (int)(width/gap) + 1;
			int numRows = (int)(height/gap) + 1;
			points = new PointMass[numColumns][numRows];

			// these fixed points will be used to anchor the grid to fixed positions on the screen
			PointMass[][] fixedPoints = new PointMass[numColumns][numRows];

			// create the point masses
			int column = 0, row = 0;
			for (float y = 0; y <= height; y += gap) {
				for (float x = 0; x <= width; x += gap) {
					points[column][row] = new PointMass(new Vec(x, y), 1);
					fixedPoints[column][row] = new PointMass(new Vec(x, y), 0);
					column++;
				}
				row++;
				column = 0;
			}

			// link the point masses with springs
			for (int y = 0; y < numRows; y++) {
				for (int x = 0; x < numColumns; x++) {
					if (x == 0 || y == 0 || x == numColumns - 1 || y == numRows - 1)    // anchor the border of the grid
						springList.add(new Spring(fixedPoints[x][y], points[x][y], 0.1f, 0.1f));
					else if (x % thick_frequency == 0 && y % thick_frequency == 0)      // loosely anchor 1/9th of the point masses
						springList.add(new Spring(fixedPoints[x][y], points[x][y], 0.002f, 0.02f));

					double stiffness = 0.28f;
					double damping = 0.05f;
					if (x > 0)
						springList.add(new Spring(points[x - 1][y], points[x][y], stiffness, damping));
					if (y > 0)
						springList.add(new Spring(points[x][y - 1], points[x][y], stiffness, damping));
				}
			}

			springs = springList.toArray(new Spring[springList.size()]);
		}

		void update() {
			if (!enabled) return;

			for (Spring s : springs)
				s.update();

			for (PointMass[] ps : points)
				for (PointMass p : ps)
					p.update();
		}

		 void applyDirectedForce(Vec force, Vec position, double radius) {
			if (!enabled) return;

			for (PointMass[] ps : points)
				for (PointMass p : ps) {
					double distSqr = position.distSqr(p.position);
					double dist = sqrt(distSqr);
					if (distSqr < radius*radius) {
						Vec f = new Vec(force);
							 f.setResult(v -> 10*v / (10+dist));
						p.applyForce(f);
					}
				}
		}

		 void applyImplosiveForce(double force, Vec position, double radius) {
			if (!enabled) return;

			for (PointMass[] ps : points)
				for (PointMass p : ps) {
				double dist2 = position.distSqr(p.position);
				if (dist2 < radius*radius) {
					Vec f = new Vec(position);
						 f.sub(p.position);
						 f.setResult(v -> 10*force*v / (100+dist2));
					p.applyForce(f);
					p.incDamping(0.6f);
				}
			}
		}

		 void applyExplosiveForce(double force, Vec position, double radius) {
			if (!enabled) return;

			for (PointMass[] ps : points)
				for (PointMass p : ps) {
				double dist2 = position.distSqr(p.position);
				if (dist2 < radius*radius) {
					Vec f = new Vec(p.position);
						 f.sub(position);
						 f.setResult(v -> 100*force*v / (10000+dist2));
					p.applyForce(f);
					p.incDamping(0.6f);
				}
			}
		}

		 void draw() {
			if (!enabled) return;

			gc.save();
			gc.setStroke(color);
			double opacityMin = 0.02, opacityMax = 0.5;

			int width = points.length;
			int height = points[0].length;

			for (int y = 1; y < height; y++) {
				for (int x = 1; x < width; x++) {
					double px = points[x][y].position.x;
					double py = points[x][y].position.y;

					Vec p = points[x][y].position;
					Vec piInit = points[x][y].positionInitial;
					double warpDist = 0.1*p.distX(piInit)*p.distY(piInit);
						// this method mitigates warpDist strength for diagonals for some reason,
						// multiples of 90deg (PI/2) have less opacity than in diagonal directions.
						// warpDist = piInit.distSqr(p);
					double warp_factor = min(warpDist/1600,1);
					double opacity = warp_factor*warp_factor;
					gc.setGlobalAlpha(clip(opacityMin,opacity,opacityMax));

					if (x > 1) {
						Vec left = points[x - 1][y].position;
						float thickness = y % thick_frequency == 1 ? 1f : 0.5f;
						drawLine(left.x,left.y,px,py, thickness);
					}
					if (y > 1) {
						Vec up =  points[x][y - 1].position;
						float thickness = x % thick_frequency == 1 ? 1f : 0.5f;
						drawLine(up.x,up.y,px,py, thickness);
					}

//                    if (x > 1 && y > 1) {
//                        Vec left = points[x - 1][y].position;
//                        Vec up =  points[x][y - 1].position;
//                        Vec upLeft = points[x - 1][y - 1].position;
//                        drawLine(0.5*(upLeft.x + up.x),0.5*(upLeft.y + up.y), 0.5*(left.x + px),0.5*(left.y + py), 0.5f);   // vertical line
//                        drawLine(0.5*(upLeft.x + left.x),0.5*(upLeft.y + left.y), 0.5*(up.x + px),0.5*(up.y + py), 0.5f);   // horizontal line
//                    }
				}
			}
			gc.restore();
		}

		private void drawLine(double x1, double y1, double x2, double y2, double thickness) {
			gc.setLineWidth(thickness);
			gc.strokeLine(x1, y1, x2, y2);
		}

		static class PointMass {

			private static final double DAMPING_INIT = 0.97;

			Vec position;
			Vec positionInitial;
			Vec velocity;
			double massI; // inverse mass == 1/mass
			Vec acceleration;
			double damping = DAMPING_INIT;

			 PointMass(Vec position, double inverse_mass) {
				this.position = position;
				this.positionInitial = new Vec(position);
				this.massI = inverse_mass;
				this.velocity = new Vec(0,0);
				this.acceleration = new Vec(0,0);
			}

			 void applyForce(Vec force) {
			   acceleration.addMul(massI, force);
			}

			 void incDamping(double factor) {
			   damping *= factor;
			}

			 void update() {
				velocity.add(acceleration);
				position.add(velocity);
				acceleration = new Vec(0,0);
				if (velocity.lengthSqr() < 0.000001) // forbids small values, performance optimization
					velocity = new Vec(0,0);

				velocity.mul(damping);
				damping = DAMPING_INIT;
			}
		}
		static class Spring {
			PointMass end1;
			PointMass end2;
			double length;
			double stiffness;
			double damping;

			 Spring(PointMass end1, PointMass end2, double stiffness, double damping) {
				this.end1 = end1;
				this.end2 = end2;
				this.length = end1.position.dist(end2.position)*0.95;
				this.stiffness = stiffness;
				this.damping = damping;
			}

			void update() {
				Vec force = end1.position.diff(end2.position);
				double posDiffLen = force.length();
				if (posDiffLen <= length) // we will only pull, not push
					return;

				force.setResult(v -> (v/posDiffLen) * (posDiffLen-length));
				Vec velDiff = end2.velocity.diff(end1.velocity);
					 velDiff.mul(damping);
				force.mul(stiffness);
				force.setSub(force,velDiff);
				// force.mul(-1);
				end2.applyForce(force);
				force.mul(-1);
				end1.applyForce(force);
			}
		}
	}

	abstract class GamepadDevices {
		protected boolean isInitialized = false;

		public final void init() {
			try {
				Controllers.initialize();
				isInitialized = true;
				Controllers.checkControllers();
				onInit(Controllers.getControllers());
			} catch (Throwable e) {
				isInitialized = false;
				LOGGER.error("Failed to initialize gamepad controllers", e);
			}
		}

		public final void doLoop() {
			if (!isInitialized) return;

			try {
				Controllers.checkControllers();
				IController[] gamepads = Controllers.getControllers();

				//			Button discovery tool
				//			System.out.println("buttons=" + gamepads[0].getButtons().length);
				//			System.out.println("sticks=" + gamepads[0].getSticks().length);
				//			System.out.println("axes=" + gamepads[0].getAxes().length);
				//			System.out.println("triggers=" + gamepads[0].getTriggers().length);
				//			stream(gamepads[0].getButtons()).filter(ISNTØ).filter(b -> b.isPressed()).forEach(b -> {
				//				System.out.println("button.getID() = " + b.getID());
				//				System.out.println("button.getCode() = " + b.getCode());
				//				System.out.println("button.getLabelKey() = " + b.getLabelKey());
				//				System.out.println("button.getDefaultLabel() = " + b.getDefaultLabel());
				//			});
				//			stream(gamepads[0].getTriggers()).filter(ISNTØ).forEach(t -> {
				//				System.out.println("trigger.getID() = " + t.getID());
				//				System.out.println("trigger.getCode() = " + t.getCode());
				//				System.out.println("trigger.getLabelKey() = " + t.getLabelKey());
				//				System.out.println("trigger.getDefaultLabel() = " + t.getDefaultLabel());
				//				System.out.println("trigger.analogValue() = " + t.analogValue());
				//				System.out.println("trigger.getPercentage() = " + t.getPercentage());
				//			});
				//			stream(gamepads[0].getAxes()).filter(ISNTØ).forEach(a -> {
				//				System.out.println("trigger.getID() = " + a.getID());
				//				System.out.println("trigger.getNumber() = " + a.getNumber());
				//				System.out.println("trigger.getValue() = " + a.getValue());
				//			});
				//			stream(gamepads[0].getSticks()).filter(ISNTØ).forEach(a -> {
				//				System.out.println("trigger.getID() = " + a.getID());
				//				System.out.println("trigger.getNumber() = " + a.getPosition());
				//			});
				//			System.out.println();

				doLoopImpl(gamepads);
			} catch (ArrayIndexOutOfBoundsException e) {
				LOGGER.info("Library bug encountered. Caught as exception & moving on.", e);
			}
		}

		abstract protected void onInit(IController[] gamepads);
		abstract protected void doLoopImpl(IController[] gamepads);

		public StreamEx<IController> getControllers() {
			return stream(Controllers.getControllers()).nonNull();
		}

		public void dispose() {
			if (!isInitialized) return;
			Controllers.shutdown();
		}
	}
}