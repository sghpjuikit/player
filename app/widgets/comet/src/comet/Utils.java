package comet;

import comet.Comet.Asteroid;
import comet.Comet.Colors;
import comet.Comet.Game;
import comet.Comet.Game.Enhancer;
import comet.Comet.Game.Mission;
import comet.Comet.MissionInfoButton;
import comet.Comet.PO;
import comet.Comet.Player;
import comet.Comet.Rocket;
import comet.Comet.Settings;
import comet.Comet.Ship;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import kotlin.reflect.KClass;
import org.gamepad4j.Controllers;
import org.gamepad4j.IController;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.ui.objects.SpitText;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.ui.pane.OverlayPane;
import sp.it.util.access.ref.R;
import sp.it.util.animation.Anim;
import sp.it.util.collections.map.KClassMap;
import sp.it.util.collections.map.Map2D;
import sp.it.util.collections.mapset.MapSet;
import sp.it.util.functional.Functors.F;
import sp.it.util.functional.Functors.F0;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.TriConsumer;
import sp.it.util.functional.Util;
import static comet.Comet.Constants.FPS;
import static comet.Utils.Achievement.achievement01;
import static comet.Utils.Achievement.achievement0N;
import static comet.Utils.Achievement.achievement1;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.ceil;
import static java.lang.Math.random;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.paint.Color.rgb;
import static javafx.scene.text.Font.font;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.minutes;
import static javafx.util.Duration.seconds;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.clip;
import static sp.it.util.Util.pyth;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.TryKt.runTry;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.array;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.forEachCartesianHalfNoSelf;
import static sp.it.util.functional.Util.forEachPair;
import static sp.it.util.functional.Util.forEachWithI;
import static sp.it.util.functional.Util.range;
import static sp.it.util.functional.Util.repeat;
import static sp.it.util.functional.Util.set;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.type.KClassExtensionsKt.getEnumValues;
import static sp.it.util.ui.Util.layHeaderTop;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.Util.layStack;
import static sp.it.util.ui.Util.layVertically;
import static sp.it.util.ui.UtilKt.setScaleXY;
import static sp.it.util.units.DurationKt.toHMSMs;

@SuppressWarnings({"FieldCanBeLocal","unused","UnnecessaryLocalVariable","SameParameterValue","UnusedReturnValue"})
interface Utils {

	private static Font loadUiFont() {
		try {
			return Font.loadFont(new FileInputStream(new File(APP.getLocation().getWidgets(), "Comet/rsc/Tele-Marines.TTF")), 14.0);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

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
	double[] degSinMemo = IntStream.rangeClosed(-UNITS,UNITS).mapToDouble(i -> i/(double)precision).map(angle -> Math.sin(rad(angle))).toArray();
	double[] degCosMemo = IntStream.rangeClosed(-UNITS,UNITS).mapToDouble(i -> i/(double)precision).map(angle -> Math.cos(rad(angle))).toArray();
	Random RAND = new Random();
	Font FONT_UI = loadUiFont();
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
	static double atan(double a) {
		return Math.atan(a);
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
	static double abs(double a) {
		return Math.abs(a);
	}
	static double max(double a, double b) {
		return Math.max(a, b);
	}
	static double min(double a, double b) {
		return Math.min(a, b);
	}
	static double pow(double number, double exponent) {
		return Math.pow(number, exponent);
	}
	static int floorMod(int number, int mod) {
		return Math.floorMod(number, mod);
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
		return (i%2==1
				? range(-i/2d,i/2d)  // ... -3 -2 -1 0 +1 +2 +3 ...
				: stream(range(0.5-i/2.0,-0.5),range(0.5,i/2.0-0.5))   // ... -1.5 -0.5 +0.5 +1.5 ...
			)
			.map(x -> gap*x)
			.toArray(Double[]::new);
	}

	/** Relocates node such the center of the node is at the coordinates. */
	static void relocateCenter(Node n, double x, double y) {
		n.relocate(x-n.getLayoutBounds().getWidth()/2,y-n.getLayoutBounds().getHeight()/2);
	}

	static Node createPlayerStat(Game game, Player p) {
		Label nameL = new Label();
		nameL.setFont(font(FONT_UI.getFamily(), FONT_UI.getSize()*1.5));
		syncC(p.name,nameL::setText);

		Label scoreLabel = new Label();
		scoreLabel.setFont(FONT_UI);
		p.score.syncC(s -> scoreLabel.setText("Score: " + s));

		var livesIcon = createPlayerLiveIcon();
		var livesLabel = new Label(p.lives.get().toString() + "x");
		livesLabel.setFont(FONT_UI);
		livesLabel.setPadding(new Insets(0.0, 5.0, 0.0, -5.0));
		p.lives.syncC(nl -> livesLabel.setText(nl + " x"));

		var energyIcon = createPlayerEnergyIcon();
		Label energyLabel = new Label();
		energyLabel.setFont(FONT_UI);
		energyLabel.setPadding(new Insets(0.0, 0.0, 0.0, -5.0));
		p.energy.syncC(e -> energyLabel.setText(p.energy.getValue().intValue() + ""));
		p.energy.syncC(e -> updatePlayerLiveIcon(energyIcon, p.energy.getValue()/p.energyMax.getValue()));
		p.energyMax.syncC(e -> updatePlayerLiveIcon(energyIcon, p.energy.getValue()/p.energyMax.getValue()));

		VBox node = layVertically(5, CENTER_LEFT, nameL,scoreLabel,layHorizontally(0.0, CENTER_LEFT, livesIcon, livesLabel, energyIcon, energyLabel));
		node.setMaxHeight(VBox.USE_PREF_SIZE); // fixes alignment in parent by not expanding this box
		node.setPrefWidth(140*game.scaleUi); // fixes position changing on children resize
		node.setUserData(p.id.get()); // to recognize which belongs to which
		return node;
	}

	static void installFont(Labeled l, Font f) {
		Font ft = f==null ? Font.getDefault() : f;
		l.setFont(ft);
		l.setStyle("{ -fx-font: \"" + ft.getFamily() + "\"; -fx-font-size }"); // bug fix, suddenly !work without this...
	}

	static void updatePlayerLiveIcon(Icon icon, double energy01) {
		if (energy01>=1.0) icon.icon(MaterialDesignIcon.BATTERY_CHARGING_100);
		else if (energy01>=0.8) icon.icon(MaterialDesignIcon.BATTERY_CHARGING_80);
		else if (energy01>=0.6) icon.icon(MaterialDesignIcon.BATTERY_CHARGING_60);
		else if (energy01>=0.4) icon.icon(MaterialDesignIcon.BATTERY_CHARGING_40);
		else if (energy01>=0.2) icon.icon(MaterialDesignIcon.BATTERY_CHARGING_20);
		else if (energy01>=0.0) icon.icon(MaterialDesignIcon.BATTERY_OUTLINE);
	}
	static Icon createPlayerLiveIcon() {
		return new Icon(MaterialDesignIcon.ROCKET,15);
	}
	static Icon createPlayerEnergyIcon() {
		return new Icon(MaterialDesignIcon.BATTERY,15);
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
	static Image createImage(Icon n, double radius) {
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
		i.scale(2);
		i.setEffect(effect);
		return createImage(i, radius);
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
	 * </br>
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
	static void strokeOval(GraphicsContext g, double x, double y, double r) {
		double d = 2*r;
		g.strokeOval(x-r,y-r,d,d);
	}
	static void drawTriangle(GraphicsContext gc, double x, double y, double r, double dir, double angleOffset) {
		double d1 = dir, d2 = dir+angleOffset, d3 = dir-angleOffset;
		gc.beginPath();
		gc.moveTo(
			x+r*cos(d1),
			y+r*sin(d1)
		);
		gc.lineTo(
			x+r*cos(d2),
			y+r*sin(d2)
		);
		gc.lineTo(
			x+r*cos(d3),
			y+r*sin(d3)
		);
		gc.closePath();
		gc.stroke();
	}
	static void fillTriangle(GraphicsContext gc, double x, double y, double r, double dir, double angleOffset) {
		double d1 = dir, d2 = dir+angleOffset, d3 = dir-angleOffset;
		gc.beginPath();
		gc.moveTo(
			x+r*cos(d1),
			y+r*sin(d1)
		);
		gc.lineTo(
			x+r*cos(d2),
			y+r*sin(d2)
		);
		gc.lineTo(
			x+r*cos(d3),
			y+r*sin(d3)
		);
		gc.closePath();
		gc.fill();
	}
	static void strokeLine(GraphicsContext g, double x, double y, double length, double angleRad) {
		g.strokeLine(x,y,x+length*cos(angleRad),y+length*sin(angleRad));
	}
	static void strokePolygon(GraphicsContext gc, Geometry polygon) {
		// Performs very badly
		// gc.strokePolygon(xs, ys, cs.length);

		// Performs about the same as above
		// Coordinate[] cs = polygon.getCoordinates();
		// gc.beginPath();
		// gc.moveTo(cs[0].x, cs[0].y);
		// for (int j=1; j<cs.length; j++)
		// 	gc.lineTo(cs[j].x, cs[j].y);
		// gc.closePath();
		// gc.stroke();

		// Performs so much better than the above, not even funny.
		Coordinate[] cs = polygon.getCoordinates();
		for (int j=0; j<cs.length-1; j++)
			gc.strokeLine(cs[j].x, cs[j].y, cs[j+1].x, cs[j+1].y);
		gc.strokeLine(cs[0].x, cs[0].y, cs[cs.length-1].x, cs[cs.length-1].y);
	}
	static void strokePolygon(GraphicsContext gc, double[] xs, double[] ys) {
		failIf(xs.length!=ys.length, () -> "Number of x and y coordinates do not match");

		for (int j=0; j<xs.length-1; j++)
			gc.strokeLine(xs[j], ys[j], xs[j+1], ys[j+1]);
		gc.strokeLine(xs[0], ys[0], xs[xs.length-1], ys[ys.length-1]);
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
		failIf(xs.length!=ys.length, () -> "Number of x and y coordinates do not match");

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
	static <E extends Enum<E>> E randEnum(Class<E> enumType) {
		return randOf(getEnumValues(enumType));
	}
	static <T> T randOf(T a, T b) {
		return randBoolean() ? a : b;
	}
	static int randOf(int a, int b) {
		return randBoolean() ? a : b;
	}
	static double randOf(double a, double b) {
		return randBoolean() ? a : b;
	}
	@SafeVarargs
	static <T> T randOf(T... c) {
		failIf(c.length==0);
		return c[randInt(c.length)];
	}
	static <T> T randOf(Collection<T> c) {
		failIf(c.isEmpty());
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
			return switch (this) {
				case NONE -> s.new Ability(true, Duration.ZERO, Duration.ZERO, 0, 0);
				case DISRUPTOR -> s.new Disruptor();
				case HYPERSPACE -> s.new Hyperspace();
				case SHIELD -> s.new Shield();
			};
		}

	}
	enum PlayerSpawn {
		CIRCLE,
		LINE,
		RECTANGLE;

		double computeStartingAngle(int ps, int p) {
			return switch (this) {
				case CIRCLE -> ps==0 ? 0 : p*D360/ps;
				case LINE, RECTANGLE -> -D90;
			};
		}

		@SuppressWarnings("unused")
		double computeStartingX(double w, double h, int ps, int p) {
			return switch(this) {
				case CIRCLE -> w/2 + 50*cos(computeStartingAngle(ps, p));
				case LINE -> w/(ps+1)*p;
				case RECTANGLE -> {
					var a = sqrt(ps);
					yield w/(a+1)*(1+(p-1)/a);
				}
			};
		}

		@SuppressWarnings("unused")
		double computeStartingY(double w, double h, int ps, int p) {
			return switch(this) {
				case CIRCLE -> h/2 + 50*sin(computeStartingAngle(ps, p));
				case LINE -> h/2;
				case RECTANGLE -> {
					int a = ((int)ceil(sqrt(ps)));
					yield h/(a+1)*(1+(p-1)%a);
				}
			};
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
		final F1<Game,Set<Player>> evaluator;
		Predicate<? super Game> condition;

		private Achievement(String NAME, GlyphIcons ICON, F1<Game,Set<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			super(NAME, ICON, DESCRIPTION);
			evaluator = EVALUATOR;
		}

		static Achievement achievement1(String NAME, GlyphIcons ICON, F1<? super Game,? extends Player> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> singleton(EVALUATOR.apply(game)), DESCRIPTION);
		}

		static Achievement achievement01(String NAME, GlyphIcons ICON, F1<? super Game,? extends Optional<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> EVALUATOR.apply(game).stream().collect(toSet()), DESCRIPTION);
		}

		static Achievement achievement0N(String NAME, GlyphIcons ICON, F1<Game,Stream<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> EVALUATOR.apply(game).collect(toSet()), DESCRIPTION);
		}

		public Achievement onlyIf(Predicate<? super Game> CONDITION) {
			this.condition = CONDITION;
			return this;
		}
	}

	class Draw {
		Image graphics;

		public Draw(Image graphics) {
			this.graphics = graphics;
		}

		public void draw(GraphicsContext gc, double x, double y) {
			gc.setGlobalAlpha(1);
			gc.drawImage(graphics, x-graphics.getWidth()/2, y-graphics.getHeight()/2);
		}

		public void draw(GraphicsContext gc, double x, double y, double scale, double angle) {
			gc.setGlobalAlpha(1);
			drawImageRotatedScaled(gc, graphics, deg(angle), x, y, scale);
		}
	}

	/** How to play help pane. */
	class HowToPane extends OverlayPane<Game> {
		private final GridPane g = new GridPane();

		public HowToPane() {
			getDisplay().set(Display.WINDOW);

			ScrollPane sp = new ScrollPane();
					   sp.setOnScroll(Event::consume);
					   sp.setContent(layStack(g, CENTER));
					   sp.setFitToWidth(true);
					   sp.setFitToHeight(false);
					   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
					   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			var l = layStack(sp, CENTER);
			    l.setMaxWidth(800);
			    l.maxHeightProperty().bind(heightProperty().subtract(100));
			setContent(l);
		}

		@Override
		public void show(Game game) {
			super.show();

			buildEnhancers(game.mode.enhancers().stream());
		}

		private void buildEnhancers(Stream<Game.Enhancer> enhancers) {
			g.getChildren().clear();
			g.getRowConstraints().clear();
			g.getColumnConstraints().clear();

			g.getColumnConstraints().add(new ColumnConstraints(100,100,100, NEVER, HPos.RIGHT, false));
			g.getColumnConstraints().add(new ColumnConstraints(20));
			g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

			R<Integer> i = new R<>(-1); // row index
			enhancers
				.sorted(by(enhancer -> enhancer.name))
				.forEach(enhancer -> {
					i.setOf(v -> v+1);

					Icon icon = new Icon(enhancer.icon, 20);
					Label nameL = new Label(enhancer.name);
					SpitText descL = new SpitText(enhancer.description);
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
		private final SpitText text = new SpitText(null);
		private final Icon helpI = infoIcon("Mission details");

		public MissionPane() {
			getDisplay().set(Display.WINDOW);

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
	class EndGamePane extends OverlayPane<Game> {
		private final Label gameModeName = new Label();
		private final StackPane gameResultPane = new StackPane();
		private final GridPane achievementPane = new GridPane();

		public EndGamePane() {
			getDisplay().set(Display.WINDOW);
			gameModeName.setFont(font(FONT_UI.getFamily(), 30));

			ScrollPane sp = new ScrollPane();
			sp.setOnScroll(Event::consume);
			sp.setContent(
				layVertically(50, Pos.CENTER,
					gameModeName,
					gameResultPane,
					layStack(achievementPane, CENTER)
				)
			);
			sp.setFitToWidth(true);
			sp.setFitToHeight(false);
			sp.setHbarPolicy(ScrollBarPolicy.NEVER);
			sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			var l = layStack(sp, CENTER);
			    l.setMaxWidth(800);
			    l.maxHeightProperty().bind(heightProperty().subtract(100));
			setContent(l);
		}

		@Override
		public void show(Game game) {
			super.show();

			gameModeName.setText(game.mode.name);
			gameResultPane.getChildren().setAll(game.mode.buildResultGraphics());

			// clear content
			achievementPane.getChildren().clear();
			achievementPane.getRowConstraints().clear();
			achievementPane.getColumnConstraints().clear();

			// build columns
			achievementPane.getColumnConstraints().add(new ColumnConstraints(100,100,100, NEVER, HPos.RIGHT, false));
			achievementPane.getColumnConstraints().add(new ColumnConstraints(20));
			achievementPane.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

			// build rows
			R<Integer> i = new R<>(-1); // row index
			game.mode.achievements().stream()
				.filter(a -> a.condition==null || a.condition.test(game))
				.flatMap(a -> stream(a.evaluator.apply(game)).collect(toMap(player -> player, player -> a)).entrySet().stream())
				.collect(groupingBy(e -> e.getKey()))
				.entrySet().stream()
				.sorted(by(e -> e.getKey().name.get()))
				.forEach(e -> {
					i.setOf(v -> v+1);
					achievementPane.add(new Label(e.getKey().name.get()), 0,i.get());
					i.setOf(v -> v+1);
					achievementPane.add(new Label(), 2,i.get()); // empty row

					e.getValue().stream()
						.map(Map.Entry::getValue)
						.sorted(by(achievement -> achievement.name))
						.forEach(enhancer -> {
							i.setOf(v -> v+1);

							Icon icon = new Icon(enhancer.icon, 20);
							Label nameL = new Label(enhancer.name);
							SpitText descL = new SpitText(enhancer.description);
							descL.setWrappingWidth(400);
							achievementPane.add(icon, 0,i.get());
							achievementPane.add(nameL, 2,i.get());
							i.setOf(v -> v+1);
							achievementPane.add(descL, 2,i.get());
							i.setOf(v -> v+1);
							achievementPane.add(new Label(), 2,i.get()); // empty row
						});

					achievementPane.add(new Label(), 2,i.get()); // empty row
					achievementPane.add(new Label(), 2,i.get()); // empty row
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
		private int times;
		private T value;
		private final F1<Integer,T> valueCalc;
		private final Consumer<Integer> changeApplier;

		InEffectValue(int times_init, F1<Integer,T> valueCalculator, Consumer<Integer> onChange) {
			times = times_init;
			valueCalc = valueCalculator;
			changeApplier = onChange;
			value = valueCalc.apply(times);
		}
		InEffectValue(int times_init, F1<Integer,T> valueCalculator) {
			this(times_init, valueCalculator, null);
		}
		InEffectValue(F1<Integer,T> valueCalculator) {
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

		InEffectValue<T> inc() {
			times++;
			value = valueCalc.apply(times);
			if (changeApplier!=null) changeApplier.accept(times);
			return this;
		}

		InEffectValue<T> dec() {
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
	class TimeDouble {
		public double value;
		protected double from, by, to;
		public long cycle = 0;

		public TimeDouble(double from) {
			this(from, 0, Double.MAX_VALUE);
		}

		public TimeDouble(double from, double by) {
			this(from, by, Double.MAX_VALUE);
		}

		public TimeDouble(double from, double by, double to) {
			failIf((from<to && by<0) || (from>to && by>0));
			this.value = from;
			this.from = from;
			this.by = by;
			this.to = to;
		}

		public TimeDouble(double from, double to, Duration per) {
			this(from, ttlVal(to-from, per), to);
		}

		public boolean run() {
			value += by;
			return isDone();
		}

		public boolean isDone() {
			return by>0
					? value >= to
					: value <= to;
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

		public TimeDouble setToFrom() {
			value = from;
			return this;
		}

		public TimeDouble setToTo() {
			value = to;
			return this;
		}

		public TimeDouble setTo(double v) {
			failIf((by>0 && v<from && v>to) || (by<0 && v>from && v<to));
			value = v;
			return this;
		}

		public TimeDouble periodic() {
			return new TimeDouble(this.from, this.by, this.to) {
				@Override
				public boolean run() {
					boolean is = super.run();
					if (is) {
						cycle++;
						this.value = this.from;
					}
					return is;
				}
			};
		}

		public TimeDouble oscillating() {
			return new TimeDouble(this.from, this.by, this.to) {
				@Override
				public boolean run() {
					boolean is = super.run();
					if (is) {
						cycle++;
						double tmp = from;
						this.from = to;
						this.by = -by;
						this.to = tmp;
					}
					return is;
				}
			};
		}
	}

	class ObjectStore<O> {
		private final Map<Class<?>,Set<O>> m = new HashMap<>();
		private final F1<O,Class<?>> mapper;

		ObjectStore(F1<O,Class<?>> classMapper) {
			mapper = classMapper;
		}

		void add(O o) {
			m.computeIfAbsent(mapper.apply(o), c -> new HashSet<>()).add(o);
		}

		void remove(O o) {
			Set<O> l = m.get(mapper.apply(o));
			if (l!=null) l.remove(o);
		}

		@SuppressWarnings("unchecked")
		<T extends O> Set<T> get(Class<T> c) {
			return (Set<T>) m.getOrDefault(c, Set.of());
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

		@SuppressWarnings({"unchecked", "rawtypes"})
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
		private final F0<P> fac;

		 Pool(int max_size, F0<P> factory) {
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
		private final KClassMap<Pool<P>> pools = new KClassMap<>();
		private final KClassMap<F1<KClass<?>,Pool<P>>> factories = new KClassMap<>();

		PoolMap() {}

		void registerPool(Class<?> type, F0<Pool<P>> poolFactory) {
			var kType = getKotlinClass(type);
			factories.put(kType, c -> poolFactory.apply());
		}
		void add(Class<?> type, P p) {
			var kType = getKotlinClass(type);
			pools.computeIfAbsent(kType, factories.get(kType)).add(p);
		}
		P get(Class<?> type) {
			var kType = getKotlinClass(type);
			return pools.computeIfAbsent(kType, factories.get(kType)).get();
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

		@SuppressWarnings({"unchecked", "rawtypes"})
		public <A extends PO, B extends PO> void add(Class<A> type1, Class <B> type2, BiConsumer<? super A,? super B> handler) {
			hs.put(type1, type2, (BiConsumer)handler);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		public <A extends PO, B extends PO> void forEach(TriConsumer<Class<A>,Class<B>,BiConsumer<? super A,? super B>> action) {
			hs.forEach((TriConsumer)action);
		}
	}

	class TTLList implements Runnable {
		final List<Ttl> lt = new ArrayList<>();
		final List<TtlC> ltc = new ArrayList<>();
		final List<PTtl> lpt = new ArrayList<>();
		final Set<Runnable> lr = new HashSet<>();
		final Set<Runnable> temp = new HashSet<>();

		/** Adds runnable that will run next time this runs. */
		void add(Runnable r) {
			lr.add(F.f(r));
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
		void addPeriodic(F0<Double> ttl, Runnable r) {
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

			lr.forEach(Runnable::run);
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
		final F0<Double> ttlPeriod;

		PTtl(F0<Double> TTL, Runnable R) {
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

	class Loop extends sp.it.util.animation.Loop {
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
			return min(x, width);
		}

		double clipInsideY(double y) {
			if (y<0) return 0;
			return min(y, height);
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
		public final String name;

		public GameMode(Game game, String name) {
			this.game = game;
			this.name = name;
		}

		public Set<Achievement> achievements() {
			return set();
		}

		public Set<Game.Enhancer> enhancers() {
			return set();
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public final void start(int player_count) {
			game.settings = new Settings();
			startDo(player_count);
		}

		abstract protected void startDo(int playerCount);

		abstract public Node buildResultGraphics();
	}
	class ClassicMode extends GameMode {
		final MapSet<Integer,Mission> missions;
		int mission_counter = 0;   // mission counter, starts at 1, increments by 1
		int firstMissionId = 1;
		boolean isMissionScheduled = false;
		boolean isMissionStartPlanetoidSplitting = false;
		final Set<Achievement> achievements;
		final Set<Enhancer> enhancers;

		public ClassicMode(Game game) {
			this(game, "Classic");
		}

		public ClassicMode(Game game, String name) {
			super(game, name);
			missions = new MapSet<>(m -> m.id,
//				new Mission(
//					1, "Energetic fragility","10⁻¹⁵","",
//					null, Color.RED,Color.rgb(255,255,255,0.015), null,(a,b,c,d,e) -> game.owner.new Particler(a,b,c,d,e)
////					Color.RED,Color.rgb(0,0,0,0.08), null,(a,b,c,d,e) -> game.owner.new Particler(a,b,c,d,e)
//				).initializer(game -> game.useGrid = false, game -> game.useGrid = true),
//				game.new Mission(
//					1, "The strange world", "10⁻⁴m", "",
////					null,Color.BLACK, Color.rgb(225,225,225, 0.2), (a,b,c,d,e) -> game.owner.new PlanetoDisc(a,b,c,d,e)
//					Color.LIGHTGREEN, rgb(0,51,51, 0.1), (a,b,c,d,e) -> game.owner.new PlanetoDisc(a,b,c,d,e)
//				),
				game.new Mission(
					1, "Sumi-e","10⁻¹⁵","",
					Color.LIGHTGREEN, rgb(0, 51, 51, 0.1), (a,b,c,d,e) -> game.owner.new Inkoid(a,b,c,d,e)
				),
				game.new Mission(
					2, "Strunctur-E-lement","10⁻¹⁵","",
					Color.LIGHTGREEN, rgb(0, 15, 0, 0.1), (a,b,c,d,e) -> game.owner.new PGon(a,b,c,d,e)
				),
				game.new Mission(
					3, "Mol's molecule","","",
					Color.GREEN, rgb(0, 15, 0, 0.08), (a,b,c,d,e) -> game.owner.new Fermi(a,b,c,d,e)
				),
				game.new Mission(
					4, "PartiCuLar elEment","10⁻¹⁵","",
					Color.YELLOW, rgb(0, 15, 0, 0.1), (a,b,c,d,e) -> game.owner.new Chargion(a,b,c,d,e)
				),
				game.new Mission(
					5, "Decay of the weak force","10⁻¹","",
					Color.GREEN, rgb(0, 15, 0, 0.08), (a,b,c,d,e) -> game.owner.new Fermi(a,b,c,d,e)
				),
				game.new Mission(
					6, "String a string","10⁻¹⁵","",
					Color.YELLOW, rgb(10, 11, 1, 0.2), (a,b,c,d,e) -> game.owner.new Stringoid(a,b,c,d,e)
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
					Color.DARKCYAN, new Color(0,0.08,0.08,0.09), (a,b,c,d,e) -> game.owner.new Linker(a,b,c,d,e)
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
					g -> stream(g.players).max(by(p -> p.stats.controlAreaSize.getAverage())).orElseThrow(),
					"Control the largest nearby area throughout the game"
				).onlyIf(g -> g.players.size()>1),
				achievement1(
					"Control freak", MaterialDesignIcon.ARROW_EXPAND,
					g -> stream(g.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).orElseThrow(),
					"Control your nearby area the most effectively"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Reaper's favourite", MaterialDesignIcon.HEART_BROKEN,
					g -> Optional.ofNullable(g.stats.firstDead),
					"Be the first to die"
				).onlyIf(g -> g.players.size()>1),
				achievement1(
					"Live and prosper", MaterialDesignIcon.HEART,
					g -> stream(g.players).max(by(p -> stream(p.stats.liveTimes).max(Duration::compareTo).orElseThrow())).orElseThrow(),
					"Live the longest"
				).onlyIf(g -> g.players.size()>1),
				achievement0N(
					"Invincible", MaterialDesignIcon.MARKER_CHECK,
					g -> stream(g.players).filter(p -> p.stats.deathCount==0),
					"Don't die"
				),
				achievement01(
					"Quickdraw", MaterialDesignIcon.CROSSHAIRS,
					g -> stream(g.players).min(by(p -> p.stats.fired1stTime, Comparator::nullsLast)),
					"Be the first to shoot"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Rusher", MaterialDesignIcon.CROSSHAIRS_GPS,
					g -> stream(g.players).min(by(p -> p.stats.hitEnemy1stTime, Comparator::nullsLast)),
					"Be the first to deal damage"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Mobile", MaterialDesignIcon.RUN,
					g -> g.players.stream().collect(Util.<Player,Double>maxBy(p -> p.stats.distanceTravelled)),
					"Travel the greatest distance"
				).onlyIf(g -> g.players.size()>1),
				achievement01(
					"Crusher", FontAwesomeIcon.TRUCK,
					g -> g.players.stream().collect(Util.<Player,Long>maxBy(p -> p.stats.asteroidRamCount)),
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
					g -> g.players.stream().collect(Util.<Player,Long>maxBy(p -> p.stats.killUfoCount)),
					"Kill most UFOs"
				).onlyIf(g -> g.players.size()>1)
			);
			enhancers = set(
				game.new Enhancer("Gun", MaterialDesignIcon.KEY_PLUS, seconds(5),
					r -> { r.gun.turrets.inc(); r.engine.mobility.dec(); }, (Rocket r) -> {},
					"- Mounts additional gun turret",
					"- Increases chance of hitting the target",
					"- Increases maximum possible target damage by 100%",
					"- Decreases acceleration, speed, maneuverability"
				),
				game.new Enhancer("Rapid fire", MaterialDesignIcon.BLACKBERRY, seconds(12), r -> r.rapidFire.inc(), r -> r.rapidFire.dec(),
					" - Largely increases rate of fire temporarily. Fires constant stream of bullets",
					" - Improved hit efficiency due to bullet spam",
					" - Improved mobility due to less danger of being hit",
					"Tip: Fire constantly. Be on the move. Let the decimating power of countless bullets"
						+ " be your shield. The upgrade lasts only a while - being static is a disadvantage."
				),
				game.new Enhancer("Long fire", MaterialDesignIcon.DOTS_HORIZONTAL, seconds(60), r -> r.powerFire.inc(), r -> r.powerFire.dec(),
					"- Increases bullet speed",
					"- Increases bullet range",
					"Tip: Aim closer to target. Faster bullet will reach target sooner."
				),
				game.new Enhancer("High energy fire", MaterialDesignIcon.MINUS, seconds(25), r -> r.energyFire.inc(), r -> r.energyFire.dec(),
					"- Bullets penetrate the target",
					"- Increases bullet damage, 1 hit kill",
					"- Multiple target damage",
					"Tip: Fire at bigger target or group of targets.",
					"Tip: Try lining up targets into a line."
				),
				game.new Enhancer("Split ammo", MaterialIcon.CALL_SPLIT, seconds(15), r -> r.splitFire.inc(), r -> r.splitFire.dec(),
					"- Bullets split into 2 bullets on hit",
					"- Multiple target damage",
					"Tip: Strategic weapon. The damage potential raises exponentially"
						+ " with the number of targets. Annihilate the most dense enemy area with ease."
				),
				// TODO: make useful
				//			new Enhancer("Black hole cannon", MaterialDesignIcon.CAMERA_IRIS, seconds(5), r -> r.gun.blackhole.inc(),
				//				"- Fires a bullet generating a black hole",
				//				"- Lethal to everything, including players",
				//				"- Player receives partial score for all damage caused by the black hole",
				//				"Tip: Strategic weapon. Do not endanger yourself or your allies."
				//			),
				game.new Enhancer("Aim enhancer", MaterialDesignIcon.RULER, seconds(45),
					r -> {
						Ship.LaserSight ls = r.new LaserSight();
						game.runNext.add(seconds(45),ls::dispose);
					},
					"- Displays bullet path",
					"- Displays bullet range"
				),
				game.new Enhancer("Engine thrust", MaterialDesignIcon.TRANSFER, seconds(25), r -> r.engine.mobility.inc(), r -> r.engine.mobility.dec(),
					"- Increases propulsion efficiency, i.e., speed",
					"- Increases acceleration, speed, maneuverability",
					"Tip: If there is ever time to move, it is now. Don't idle around."
				),
				game.new Enhancer("Engine", MaterialDesignIcon.ENGINE, seconds(25), r -> r.engine.mobility.inc(), r -> {},
					"- Adds permanent engine",
					"- Increases acceleration, speed, maneuverability"
				),
				game.new Enhancer("Intel", MaterialDesignIcon.EYE, minutes(2), r ->  game.humans.intelOn.inc(), r -> game.humans.intelOn.dec(), false,
					"- Reports incoming ufo time & location",
					"- Reports incoming upgrade time & location",
					"- Reveals exact upgrade type before it is picked up",
					"- Displays bullet range",
					"- Displays player control area and marks the best area control position",
					"Tip: This upgrade is automatically shared."
				),
				game.new Enhancer("Share upgrades", MaterialDesignIcon.SHARE_VARIANT, minutes(2),
					r -> game.humans.share_enhancers=true, r -> game.humans.share_enhancers=false,
					"- Applies upgrades to all allies",
					"Tip: The more allies, the bigger the gain."
				),
				game.new Enhancer("Shuttle support", FontAwesomeIcon.SPACE_SHUTTLE, seconds(5),
					r -> game.humans.send(r, rc -> game.owner.new Shuttle(rc)), r -> {}, false,
					"- Calls in supply shuttle",
					"- Provides large and powerful stationary kinetic shield",
					"- Provides additional upgrades",
					"Tip: This upgrade can not be shared."
				),
				game.new Enhancer("Super shield", FontAwesomeIcon.CIRCLE_THIN, seconds(5),
					r -> game.humans.send(r, rc -> game.owner.new SuperShield(rc)), r -> {}, false,
					"- Calls in support shield",
					"- Provides large and powerful stationary kinetic shield",
					"Tip: This upgrade can not be shared."
				),
				game.new Enhancer("Super disruptor", MaterialIcon.BLUR_ON, seconds(5),
					r -> game.humans.send(r, rc -> game.owner.new SuperDisruptor(rc)), r -> {}, false,
					"- Calls in support disruptor",
					"- Provides large and powerful stationary force field that slows objects down",
					"Tip: Hide inside and use as a form of shield.",
					"Tip: Objects with active propulsion will still be able to move, albeit slowed down.",
					"Tip: This upgrade can not be shared."
				),
				game.new Enhancer("Shield energizer", MaterialDesignIcon.IMAGE_FILTER_TILT_SHIFT, seconds(5),
					r -> {
						r.kinetic_shield.KSenergy_max *= 1.1;
						r.kinetic_shield.changeKSenergyToMax();
					},
					"- Sets kinetic shield energy to max",
					"- Increases maximum kinetic shield energy by 10%"
				),
				game.new Enhancer("Shield enhancer", FontAwesomeIcon.SUN_ALT, seconds(25), r -> r.kinetic_shield.large.inc(), r -> r.kinetic_shield.large.dec(),
					"- Increases kinetic shield range by " + game.settings.KINETIC_SHIELD_LARGE_RADIUS_INC + "px",
					"- Increases maximum kinetic shield energy by " + (game.settings.KINETIC_SHIELD_LARGE_E_MAX_INC*100) + "%",
					"- Increases kinetic shield energy accumulation " + (game.settings.KINETIC_SHIELD_LARGE_E_RATE) +" times",
					"Tip: You are not invincible, but anyone should think twice about hitting you. Go on the offensive. Move."
				),
				game.new Enhancer("Battery", MaterialDesignIcon.BATTERY_PLUS, seconds(5),
					r -> {
						r.energy_max *= 1.1;
						r.energy_buildup_rate *= 1.1;
					},
					"- Increases maximum energy by 10%",
					"- Increases energy accumulation by 10%"
				),
				game.new Enhancer("Energy (S)", MaterialDesignIcon.BATTERY_30, seconds(5),
					r -> r.energy = min(r.energy+2000,r.energy_max),
					"- Increases energy by up to 2000"
				),
				game.new Enhancer("Energy (M)", MaterialDesignIcon.BATTERY_60, seconds(5),
					r -> r.energy = min(r.energy+5000,r.energy_max),
					"- Increases energy by up to 5000"
				),
				game.new Enhancer("Energy (L)", MaterialDesignIcon.BATTERY, seconds(5),
					r -> r.energy = min(r.energy+10000,r.energy_max),
					"- Increases energy by up to 10000"
				)
			);
		}

		@Override
		public void init() {}

		@Override
		public void startDo(int player_count) {
			mission_counter = 0;
			isMissionScheduled = false;
			nextMission();

			game.runNext.addPeriodic(() -> game.settings.SATELLITE_TTL()/sqrt(game.players.size()), game.humans::sendSatellite);
			game.runNext.addPeriodic(() -> game.settings.UFO_TTL()/sqrt(game.players.size()), game.ufos::sendUfo);
			game.runNext.addPeriodic(() -> game.settings.UFO_SWARM_TTL()/sqrt(game.players.size()), game.ufos::sendUfoSwarm);
			game.runNext.addPeriodic(() -> game.settings.UFO_DISC_SPAWN_TTL()/sqrt(game.players.size()), () -> game.ufos.canSpawnDiscs = true);
//			game.runNext.add(() -> game.mission_button = game.owner.new MissionInfoButton());
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

		@Override
		public Set<Enhancer> enhancers() {
			return enhancers;
		}

		@Override
		public Node buildResultGraphics() {
			return new Pane();
		}

		protected void nextMission() {
			// schedule
			if (isMissionScheduled) return;
			isMissionScheduled = true;

			// get mission
			if (game.mission!=null) game.mission.disposer.accept(game);
			mission_counter = mission_counter==0 ? firstMissionId : mission_counter+1;
			int id = mission_counter%missions.size();
			int mission_id = id==0 ? missions.size() : mission_counter%missions.size(); // modulo mission count, but start at 1
			Mission mNew = missions.get(mission_id);
			Colors cOld = game.mission==null ? game.colors : game.mission.colors;

			// start mission
			game.mission = mNew;
			double delay = ttl(seconds(mission_counter==1 ? 2 : 5));
			if (game.settings.spawnAsteroids) {
				boolean isEasy = mission_counter < 4;
				double size = sqrt(game.field.height) / 1000;
				int planetoidCount = 3 + (int) (2 * (size - 1)) + (mission_counter - 1) + game.players.size() / 2;
				int planetoids = isEasy ? planetoidCount / 2 : planetoidCount;
				game.runNext.add(delay / 2, () -> game.message("Level " + mission_counter, mNew.name));
				game.runNext.add(delay, () -> repeat(planetoids, i -> mNew.spawnPlanetoid()));
				if (isEasy) {
					isMissionStartPlanetoidSplitting = true;
					game.runNext.add(delay, () -> {
						game.oss.get(Asteroid.class).forEach(a -> a.split(null));
						isMissionStartPlanetoidSplitting = false;
					});
				}
			}
			game.runNext.add(delay, () -> isMissionScheduled = false);
			game.mission.initializer.accept(game);

			// transition color scheme
			game.runNext.add(delay/2, () ->
				game.runNext.addAnim01(millis(300), p -> game.colors.interpolate(cOld, mNew.colors, p))
			);
		}
	}
	class TimeTrial extends ClassicMode {
		private final TimeDouble missionTimer = new TimeDouble(0, 1, ttl(minutes(1)));
		private final TimeDouble remainingTimeMs = new TimeDouble(0, 1, ttl(seconds(30)));

		public TimeTrial(Game game) {
			super(game, "Time Trial");

			enhancers.clear();
			enhancers.add(
				game.new Enhancer(
					"+5 seconds", FontAwesomeIcon.CLOCK_ALT, Duration.ZERO,
					r -> {
						remainingTimeMs.value -= ttl(seconds(5));
						game.humans.sendSmallStationarySatellite();
					},
					"Increase remaining time by 5 seconds"
				)
			);
		}

		@Override
		public void init() {
			super.init();
		}

		@Override
		public void startDo(int player_count) {
			game.settings.useGrid = false;
			game.settings.playerGunDisabled = true;
			game.settings.playerNoKineticShield = true;
			game.settings.UFO_BULLET_TTL *= 2;
			game.settings.UFO_BULLET_SPEED /= 3;
			game.settings.player_ability_auto_on = true;
			game.settings.DISRUPTOR_E_RATE = 0;
			game.settings.DISRUPTOR_E_ACTIVATION = 0;
			game.settings.UFO_GUN_RELOAD_TIME = millis(20);
			game.settings.spawnSwarms = false;
			game.settings.spawnAsteroids = false;

			game.players.forEach(p -> p.ability_type.set(AbilityKind.DISRUPTOR));
			game.humans.intelOn.inc();
			remainingTimeMs.setToFrom();
			missionTimer.setToFrom();
			mission_counter = 0;
			isMissionScheduled = false;
			nextMission();

			game.runNext.add(seconds(5), game.humans::sendSmallStationarySatellite);
			game.runNext.addPeriodic(seconds(4), game.ufos::sendUfo);
		}

		@Override
		public void doLoop() {
			super.doLoop();

			missionTimer.run();
			if (missionTimer.isDone()) {
				nextMission();
				missionTimer.value = 0;
			}

			remainingTimeMs.run();
			if (remainingTimeMs.isDone()) {
				game.over();
			}
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

		@Override
		public Node buildResultGraphics() {
			Label l = new Label(toHMSMs(time(game.loop.id)));
			l.setFont(font(FONT_UI.getFamily(), 20));
			return l;
		}
	}
	class BounceHellMode extends ClassicMode {

		public BounceHellMode(Game game) {
			super(game, "Bounce");
		}

		@Override
		public void init() {
			super.init();
		}

		@Override
		public void startDo(int player_count) {
			game.settings.playerGunDisabled = true;
			game.settings.player_ability_auto_on = true;
			game.settings.SHIELD_E_RATE = 0;
			game.settings.SHIELD_E_ACTIVATION = 0;
			game.settings.spawnSwarms = false;

			super.startDo(player_count);

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
	class AreaMode extends GameMode {
		private final Duration gameLength = minutes(2);
		private final TimeDouble remainingTimeMs = new TimeDouble(gameLength.toMillis(), -1000/FPS, 0);
		private MissionInfoButton timeDisplay;

		public AreaMode(Game game) {
			super(game, "Area");
		}

		@Override
		public void init() {}

		@Override
		public void doLoop() {
			if (remainingTimeMs.isDone()) {
				game.over();
			} else {
				remainingTimeMs.run();
			}

			var victors = stream(game.players).sorted(by((Player p) -> p.stats.controlAreaSize.getAverage()).reversed()).toList();

			// Highlight player ranking
			forEachWithI(victors, (i,p) -> {
				if (p.alive)
					game.fillText("" + (i+1), p.rocket.x, p.rocket.y);
			});

			// Highlight player with biggest area
			game.players.stream()
				.filter(p -> p.alive && p.rocket.voronoiArea!=null)
				.collect(Util.maxBy(p -> p.rocket.voronoiArea))
				.ifPresent(p -> drawHudCircle(game.owner.gc, game.field, p.rocket.x, p.rocket.y, 50, game.colors.hud));

			timeDisplay.doLoop();
			timeDisplay.draw();
		}

		@Override
		public void handleEvent(Object event) {}

		@Override
		public void stop() {}

		@Override
		public void pause(boolean v) {}

		@Override
		protected void startDo(int playerCount) {
			game.settings.useGrid = false;
			game.settings.playerGunDisabled = true;
			game.settings.player_ability_auto_on = true;
			game.settings.playerNoKineticShield = true;
			game.settings.voronoiDraw = true;

			remainingTimeMs.setToFrom();
			game.players.forEach(p -> p.ability_type.set(AbilityKind.NONE));
			timeDisplay = game.owner.new MissionInfoButton() {
				private final TimeDouble pulse = new TimeDouble(1,1.5, millis(500)).oscillating();

				@Override
				public void doLoop() {
					super.doLoop();
					if (remainingTimeMs.get() <= 10000) pulse.run();
				}

				@Override
				void draw() {
					game.fillText(toHMSMs(millis(remainingTimeMs.get())), x,y, pulse.get());
				}
			};
		}

		@Override
		public Node buildResultGraphics() {
			var text = "Average\n\n%s\n\nMax\n\n%s\n\nMin\n\n%s".formatted(
				stream(game.players)
					  .sorted(Util.<Player,Double>by(p -> p.stats.controlAreaSize.getAverage()).reversed())
					  .map(p -> p.name.get() + ": %.2d".formatted(p.stats.controlAreaSize.getAverage()))
					  .collect(joining("\n")),
				stream(game.players)
					  .sorted(Util.<Player,Double>by(p -> p.stats.controlAreaSize.getMax()).reversed())
					  .map(p -> p.name.get() + ": %.2d".formatted(p.stats.controlAreaSize.getMax()))
					  .collect(joining("\n")),
				stream(game.players)
					  .sorted(Util.<Player,Double>by(p -> p.stats.controlAreaSize.getMin()).reversed())
					  .map(p -> p.name.get() + ": %.2d".formatted(p.stats.controlAreaSize.getMin()))
					  .collect(joining("\n"))
			);

			Label l = new Label(text);
			l.setFont(font(FONT_UI.getFamily(), 15));
			return new StackPane(l);
		}

	}
	class VoronoiMode extends GameMode {
		private List<Cell> cells = null;

		public VoronoiMode(Game game) {
			super(game, "Voronoi");
		}

		@Override
		public void init() {
			// for development
			game.owner.sanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> cells.add(new Cell(e.getX(), e.getY())));

			// generate cells
			int W = (int) game.field.width;
			int H = (int) game.field.height;
			int cellCount = 40;
			if (cells == null) {
				// circle
				double wh = min(W,H);
				cells = new ArrayList<>();
				cells.addAll(
					DoubleStream.iterate(0, a-> a+2*PI/11).limit(11)
							.mapToObj(a -> new Cell(0,0) {
								double angle = a;
								{
									moving = (w,h) -> {
										angle += 0.001;
										x = wh/2+wh/20*cos(angle);
										y = wh/2+wh/20*sin(angle);
										x += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
										y += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
									};
								}
							})
							.map(c -> (Cell)c)
							.toList()
				);
				cells.addAll(
					DoubleStream.iterate(0, a-> a+2*PI/3).limit(3)
								.mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh/2+wh/10*cos(angle);
											 y = wh/2+wh/10*sin(angle);
											 x += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
											 y += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								.map(c -> (Cell)c)
								.toList()
				);
				cells.addAll(
					DoubleStream.iterate(0, a-> a+2*PI/cellCount).limit(cellCount)
								.mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh-wh/6+wh/8*cos(angle);
											 y = wh/6+wh/8*sin(angle);
											 x += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
											 y += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								.map(c -> (Cell)c)
								.toList()
				);
				cells.addAll(
					DoubleStream.iterate(0, a-> a+2*PI/cellCount).limit(cellCount)
								.mapToObj(a -> new Cell(0,0) {
									 double angle = a;
									 {
										 moving = (w,h) -> {
											 angle -= 0.002;
											 x = wh/2+wh/4*cos(angle);
											 y = wh/2+wh/4*sin(angle);
											 x += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
											 y += Utils.randOf(-1,1)*randMN(0.0005,0.00051);
										 };
									 }
								 })
								.map(c -> (Cell)c)
								.toList()
				);
				// horizontal sequence
//				cells = IntStream.range(0,cellCount)
//								.mapToObj(a -> new Cell(W*0.1+W*0.8/cellCount*a, H/2))
//								.toList();
				// random
//				cells = Stream.generate(() -> Cell.random(W, H, .5)).limit(cellCount).toList();

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
		}

		@Override
		public void doLoop() {
//			computeVoronoi(
//				stream(Asteroid.class, Rocket.class)
//					.flatMap(c -> game.oss.get(c).stream())
//					.toList(),
//				game
//			);

			// make rockets generate voronoi path trail
			if (game.loop.isNth(7))
				stream(game.players)
//					.filter(p -> p.alive && p.rocket.speed()>ttlVal(100, seconds(1)))
					.filter(p -> p.alive && p.rocket.ability.isActivated())
					.forEach(p -> cells.add(new Cell(p.rocket.x + randMN(0.01,0.012), p.rocket.y + randMN(0.01,0.012), p)));

			// move cells
			cells.stream().filter(cell -> cell.moving!=null)
				.forEach(cell -> cell.moving.accept(game.field.width, game.field.height));

			GraphicsContext gc = game.owner.gc;
			gc.setFill(game.colors.hud);
			gc.setStroke(game.colors.hud);

			// draw cells
			gc.save();
			Set<Cell> selectedCells = stream(game.players)
				.filter(p -> p.alive)
				.map(p -> cells.stream().collect(Util.minBy(c -> c.distance(p.rocket.x, p.rocket.y))).orElse(null))
				.filter(ISNT0)
				.collect(toSet());
			Map<Coordinate,Cell> inputOutputMap = cells.stream().collect(toMap(o -> new Coordinate(o.x, o.y), o -> o));
			Collection<Coordinate> cords = inputOutputMap.keySet();
			VoronoiDiagramBuilder diagram = new VoronoiDiagramBuilder();
			diagram.setClipEnvelope(new Envelope(0, game.field.width, 0, game.field.height));
			diagram.setSites(cords);
			runTry(() -> diagram.getDiagram(new GeometryFactory()))
				.ifErrorUse(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOkUse(g ->
						IntStream.range(0, g.getNumGeometries())
							.mapToObj(g::getGeometryN)
							.peek(polygon -> polygon.setUserData(inputOutputMap.get(polygon.getUserData())))
							.forEach(polygon -> {
								var cell = (Cell) polygon.getUserData();
								var player = (Player) cell.userData;
								var isSelected = selectedCells.contains(cell);
//								Point centroid = polygon.getCentroid();

								strokePolygon(gc, polygon);

								var cs = polygon.getCoordinates();
								var xs = new double[cs.length];
								var ys = new double[cs.length];
								for (int j = 0; j < cs.length; j++) {
									xs[j] = cs[j].x;
									ys[j] = cs[j].y;
								}

								if (isSelected || player != null) {
									gc.setFill(player == null ? game.colors.hud : player.color.getValue());
									gc.setGlobalAlpha(isSelected && player != null ? 0.3 : 0.2);
									gc.fillPolygon(xs, ys, polygon.getNumPoints());
									gc.setFill(game.colors.hud);
									gc.setGlobalAlpha(1);
								}
							})
				);
			gc.restore();

			// draw cell seeds
			gc.save();
			double r = 2;
			cells.forEach(c -> gc.fillOval(c.x-r,c.y-r,2*r,2*r));
			gc.restore();
		}

		@Override
		public void handleEvent(Object event) {}

		@Override
		public void stop() {}

		@Override
		public void pause(boolean v) {}

		@Override
		protected void startDo(int playerCount) {
			game.settings.useGrid = false;
			game.settings.playerGunDisabled = true;
			game.settings.playerNoKineticShield = true;

			game.players.forEach(p -> p.ability_type.set(AbilityKind.NONE));
		}

		@Override
		public Node buildResultGraphics() {
			Label l = new Label(toHMSMs(time(game.loop.id)));
			l.setFont(font(FONT_UI.getFamily(), 15));
			return new StackPane(l);
		}

		static class P {
			double x,y;

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
		static class Cell extends P {
			public double dx=0, dy=0;
			public BiConsumer<Double,Double> moving = null;
			public Object userData;

			public Cell(double x, double y) {
				super(x, y);
			}
			public Cell(double x, double y, Object userData) {
				this(x, y);
				this.userData = userData;
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
			if (!(o instanceof Vec v)) return false;

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
			if (!(o instanceof Lin l)) return false;

			return (
				(Double.compare(l.x1, x1)==0 && Double.compare(l.x2, x2)==0 && Double.compare(l.y1, y1)==0 && Double.compare(l.y2, y2)==0) ||
				(Double.compare(l.x1, x2)==0 && Double.compare(l.x2, x1)==0 && Double.compare(l.y1, y2)==0 && Double.compare(l.y2, y1)==0)
			);
		}

		@SuppressWarnings("IfStatementWithIdenticalBranches")
		@Override
		public int hashCode() {
			int result;
			long temp;
			if (x1 < x2) {
				temp = Double.doubleToLongBits(x1);
				result = (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(y1);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(x2);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(y2);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
			} else if (x1 == x2) {
				if (y1 < y2) {
					temp = Double.doubleToLongBits(y1);
					result = (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
				} else {
					temp = Double.doubleToLongBits(y2);
					result = (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x2);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(y1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
					temp = Double.doubleToLongBits(x1);
					result = 31 * result + (int) (temp ^ (temp >>> 32));
				}
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
		private final Map<Coordinate,RocketB> inputOutputMap = new HashMap<>(8*9); // maps input (rockets) to polygons

		static class RocketB {
			final Rocket rocket;
			final boolean isMain;

			RocketB(Rocket rocket, boolean flag) {
				this.rocket = rocket;
				this.isMain = flag;
			}
		}

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
				Rocket r = rockets.stream().findFirst().orElseThrow();
				areaAction.accept(r, W*H);
				distAction.accept(r, 0d);
				centerAction.accept(r.x, r.y);
				edgesAction.accept(stream());
			// or we fall back to algorithm
			} else {
				doCompute(rockets, W, H, game);
			}
		}

		protected void doCompute(Set<Rocket> rockets, double W, double H, Game game) {
			inputOutputMap.clear();


			List<Coordinate> cells = stream(rockets)
				.flatMap(rocket -> {
					Vec r = new Vec(rocket.x+rocket.cacheRandomVoronoiTranslation, rocket.y+rocket.cacheRandomVoronoiTranslation);
					Coordinate cMain = new Coordinate(r.x, r.y);
					inputOutputMap.put(cMain, new RocketB(rocket,true));
					return stream(
						stream(cMain),
						stream(
							new Coordinate(r.x + W, r.y), new Coordinate(r.x, r.y + H), new Coordinate(r.x - W, r.y), new Coordinate(r.x, r.y - H),
							new Coordinate(r.x + W, r.y + H), new Coordinate(r.x + W, r.y - H), new Coordinate(r.x - W, r.y + H), new Coordinate(r.x - W, r.y - H)
						)
						.peek(c -> inputOutputMap.put(c, new RocketB(rocket,false)))
					);
				})
				.toList();

			VoronoiDiagramBuilder voronoi = new VoronoiDiagramBuilder();
			voronoi.setClipEnvelope(new Envelope(0, W, 0, H));
			voronoi.setSites(cells);
			runTry(() -> voronoi.getDiagram(new GeometryFactory()))
				.ifErrorUse(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOkUse(g ->
					edgesAction.accept(IntStream.range(0, g.getNumGeometries())
						.mapToObj(g::getGeometryN)
						.peek(polygon -> polygon.setUserData(inputOutputMap.get(polygon.getUserData())))
						.collect(groupingBy(polygon -> ((RocketB) polygon.getUserData()).rocket))
						.values().stream()
						.flatMap(ss -> {
							List<Lin> lines = stream(ss)
								.peek(polygon -> {
									RocketB data = (RocketB) polygon.getUserData();
									if (data.isMain) {
										Point c = polygon.getCentroid();
										centerAction.accept(c.getX(), c.getY());
										areaAction.accept(data.rocket, polygon.getArea());
										distAction.accept(data.rocket, game.field.dist(c.getX(), c.getY(), data.rocket.x, data.rocket.y));
									}
								})
								.filter(polygon -> game.settings.voronoiDraw|| game.humans.intelOn.is())
								// optimization: return edges -> draw edges instead of polygons, we can improve performance
								.flatMap(polygon -> {
									Coordinate[] cs = polygon.getCoordinates();
									double[] xs = new double[cs.length];
									double[] ys = new double[cs.length];
									for (int j = 0; j < cs.length; j++) {
										xs[j] = cs[j].x;
										ys[j] = cs[j].y;
									}

//									game.drawPolygon(xs,ys,cs.length);
									Stream.Builder<Lin> s = Stream.builder();
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
							Set<Lin> linesDuplicate = stream(lines).filter(n -> !linesUnique.add(n)).collect(toSet());
							linesUnique.removeAll(linesDuplicate);
							return linesUnique.stream();
						})
						// optimization: draw each edge only once by removing duplicates with Set and proper hashCode()
						.distinct()
					)
				);
		}
	}

	static void computeVoronoi(Collection<? extends PO> os, Game game) {
		Map<Coordinate,PO> inputOutputMap = stream(os).collect(toMap(o -> new Coordinate(o.x, o.y), o -> o));
		Set<Coordinate> cells = inputOutputMap.keySet();

		VoronoiDiagramBuilder voronoi = new VoronoiDiagramBuilder();
		voronoi.setClipEnvelope(new Envelope(0, game.field.width, 0, game.field.height));
		voronoi.setSites(cells);
		runTry(() -> voronoi.getDiagram(new GeometryFactory()))
			.ifErrorUse(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
			.ifOkUse(g ->
					IntStream.range(0, g.getNumGeometries())
						.mapToObj(g::getGeometryN)
						.peek(polygon -> polygon.setUserData(inputOutputMap.get(polygon.getUserData())))
						.forEach(polygon -> {
//						.groupingBy(polygon -> ((PO) polygon.getUserData()).type)
//						.entrySet()
//						.forEach(e -> {
//							Class<?> type = e.getKey();
//							List<Geometry> polygons = e.getValue();

							game.owner.gc.setStroke(game.colors.hud);
							game.owner.gc.setLineWidth(1);
//							game.owner.gc.setFill(color(game.colors.hud, 0.1));
							strokePolygon(game.owner.gc, polygon);
						})
		);
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

			springs = springList.toArray(new Spring[0]);
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

		public void start() {}

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

		public Stream<IController> getControllers() {
			return stream(Controllers.getControllers()).filter(o -> o!=null);
		}

		public void dispose() {
			if (!isInitialized) return;
			Controllers.shutdown();
		}
	}
}