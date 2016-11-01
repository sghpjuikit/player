package comet;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

import org.gamepad4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import comet.Comet.Ship.Disruptor.DisruptorField;
import comet.Comet.Ship.Shield;
import comet.Utils.*;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import gui.objects.Text;
import gui.objects.icon.Icon;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.access.V;
import util.access.VarEnum;
import util.animation.Anim;
import util.async.executor.FxTimer;
import util.collections.mapset.MapSet;
import util.conf.Config.ConfigurableVarList;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.ListConfigurable;
import util.functional.Functors.Ƒ0;
import util.functional.Functors.Ƒ1;
import util.functional.Functors.Ƒ5;
import util.reactive.SetƑ;
import util.validation.Constraint;

import static comet.Comet.Constants.*;
import static comet.Utils.AbilityKind.SHIELD;
import static comet.Utils.AbilityState.*;
import static comet.Utils.Achievement.*;
import static comet.Utils.*;
import static comet.Utils.GunControl.AUTO;
import static comet.Utils.GunControl.MANUAL;
import static comet.Utils.cos;
import static comet.Utils.sin;
import static gui.objects.window.stage.UiContext.showSettingsSimple;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.*;
import static javafx.geometry.Pos.*;
import static javafx.scene.effect.BlendMode.*;
import static javafx.scene.effect.BlurType.GAUSSIAN;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;
import static javafx.util.Duration.*;
import static util.Util.clip;
import static util.animation.Anim.mapTo01;
import static util.dev.Util.yes;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;

/**
 *
 * @author Martin Polakovic
 */
@Widget.Info(
	author = "Martin Polakovic",
	name = "Comet",
	description = "Port of the game Comet.",
//    howto = "",
//    notes = "",
	version = "0.6",
	year = "2015",
	group = Widget.Group.OTHER
)
public class Comet extends ClassController {
	private static Logger LOGGER = LoggerFactory.getLogger(Comet.class);

	final Pane playfield = new Pane();  // play field, contains scenegraph game graphics
	final Canvas canvas = new Canvas();
	final Canvas canvas_bgr = new Canvas();
	final GraphicsContext gc = canvas.getGraphicsContext2D(); // draws canvas game graphics on canvas
	final GraphicsContext gc_bgr = canvas_bgr.getGraphicsContext2D(); // draws canvas game graphics on bgr canvas
	final Text message = new Text();
	final Game game = new Game();
	final SetƑ every200ms = new SetƑ();
	final FxTimer timer200ms = new FxTimer(200,-1,every200ms);

	public Comet() {
		// message
		message.setOpacity(0);
		message.setFont(new Font(UI_FONT.getName(), 50));

		// playfield
		Rectangle playfieldMask = new Rectangle();
		playfieldMask.widthProperty().bind(playfield.widthProperty());
		playfieldMask.heightProperty().bind(playfield.heightProperty());
		playfield.setClip(playfieldMask);
		playfield.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

		// canvas
		canvas.widthProperty().bind(playfield.widthProperty());
		canvas.heightProperty().bind(playfield.heightProperty());
		canvas.setManaged(false);
		canvas_bgr.widthProperty().bind(playfield.widthProperty());
		canvas_bgr.heightProperty().bind(playfield.heightProperty());
		canvas_bgr.setManaged(false);

		maintain(playfield.widthProperty(), w -> game.field.resize(playfield.getWidth(), playfield.getHeight()));
		maintain(playfield.heightProperty(), w -> game.field.resize(playfield.getWidth(), playfield.getHeight()));

		// player stats
		double G = 10; // padding
		StackPane playerStats = layStack(
			layHorizontally(G,TOP_LEFT,     createPlayerStat(PLAYERS.list.get(0)),createPlayerStat(PLAYERS.list.get(4))),TOP_LEFT,
			layHorizontally(G,TOP_RIGHT,    createPlayerStat(PLAYERS.list.get(5)),createPlayerStat(PLAYERS.list.get(1))),TOP_RIGHT,
			layHorizontally(G,BOTTOM_LEFT,  createPlayerStat(PLAYERS.list.get(2)),createPlayerStat(PLAYERS.list.get(6))),BOTTOM_LEFT,
			layHorizontally(G,BOTTOM_RIGHT, createPlayerStat(PLAYERS.list.get(7)),createPlayerStat(PLAYERS.list.get(3))),BOTTOM_RIGHT
		);
		playerStats.setPadding(new Insets(G,0,G,G));
		playerStats.setMouseTransparent(true);
		game.players.addListener((Change<? extends Player> change) ->
			playerStats.getChildren().forEach(cc -> ((Pane)cc).getChildren().forEach(c ->
				c.setVisible(game.players.stream().anyMatch(p -> p.id.get()==(int)c.getUserData()))
			))
		);

		// layout
		setAnchor(this,
			layHorizontally(20,CENTER_LEFT,
				new Icon(MaterialDesignIcon.NUMERIC_1_BOX_OUTLINE,15,"Start 1 player game",() -> game.start(1)),
				new Icon(MaterialDesignIcon.NUMERIC_2_BOX_OUTLINE,15,"Start 2 player game",() -> game.start(2)),
				new Icon(MaterialDesignIcon.NUMERIC_3_BOX_OUTLINE,15,"Start 3 player game",() -> game.start(3)),
				new Icon(MaterialDesignIcon.NUMERIC_4_BOX_OUTLINE,15,"Start 4 player game",() -> game.start(4)),
				new Icon(MaterialDesignIcon.NUMERIC_5_BOX_OUTLINE,15,"Start 5 player game",() -> game.start(5)),
				new Icon(MaterialDesignIcon.NUMERIC_6_BOX_OUTLINE,15,"Start 6 player game",() -> game.start(6)),
				new Icon(MaterialDesignIcon.NUMERIC_7_BOX_OUTLINE,15,"Start 7 player game",() -> game.start(7)),
				new Icon(MaterialDesignIcon.NUMERIC_8_BOX_OUTLINE,15,"Start 8 player game",() -> game.start(8)),
				new Icon(null,16){{ maintain(game.paused,mapB(MaterialDesignIcon.PLAY,MaterialDesignIcon.PAUSE), this::icon); }}.onClick(() -> game.pause(!game.paused.get())),
				new Icon<>(FontAwesomeIcon.GEARS,14,"Settings").onClick(e -> showSettingsSimple(new ListConfigurable(Configurable.configsFromFieldsOf(this)),e)),
				new Icon<>(FontAwesomeIcon.INFO,14,"How to play").onClick(() -> new HowToPane().show(game))
			),
			0d,0d,null,0d,
			layStack(canvas_bgr, canvas, playfield, playerStats, message),
			20d,0d,0d,0d
		);

		// keys
		playfield.addEventFilter(KEY_PRESSED, e -> {
			KeyCode cc = e.getCode();
			boolean first_time = game.pressedKeys.add(cc);
			if (first_time) {
				game.keyPressTimes.put(cc,game.loop.now);
				game.players.stream().filter(p -> p.alive).forEach(p -> {
					if (cc==p.keyAbility.getValue()) p.rocket.ability_main.onKeyPress();
					if (cc==p.keyFire.getValue()) p.rocket.gun.fire();
				});
				// cheats
				if (cc==DIGIT1) game.runNext.add(() -> repeat(5, i -> game.mission.spawnPlanetoid()));
				if (cc==DIGIT2) game.runNext.add(() -> game.ufos.sendUfoSquadron());
				if (cc==DIGIT3) game.runNext.add(() -> repeat(5, i -> game.humans.sendSatellite()));
				if (cc==DIGIT4) game.runNext.add(() -> {
					game.oss.forEach(Asteroid.class,a -> a.dead=true);
					game.nextMission();
				});
				if (cc==DIGIT5) game.players.stream().filter(p -> p.alive).forEach(p -> game.humans.send(p.rocket, SuperShield::new));
				if (cc==DIGIT6) game.oss.get(Rocket.class).forEach(r -> randOf(game.ROCKET_ENHANCERS).enhance(r));
				if (cc==DIGIT7) game.entities.addForceField(new BlackHole(null, seconds(20),rand0N(game.field.width),rand0N(game.field.height)));
				if (cc==DIGIT8) game.start(2);
			}
		});
		playfield.addEventFilter(KEY_RELEASED, e -> {
			game.players.stream().filter(p -> p.alive).forEach(p -> {
				if (e.getCode()==p.keyAbility.getValue()) p.rocket.ability_main.onKeyRelease();
			});
			game.pressedKeys.remove(e.getCode());
		});
		playfield.setOnMouseClicked(e -> playfield.requestFocus());
		playfield.focusedProperty().addListener((o,ov,nv) -> game.pause(!nv));

		addEventHandler(Event.ANY, Event::consume);
	}

	@Override
	public void onClose() {
		game.dispose();
	}

	interface Constants {
		double FPS = 60; // frames per second (locked)
		double FPS_KEY_PRESSED = 40; // frames per second
		double FPS_KEY_PRESSED_PERIOD = 1000 / FPS_KEY_PRESSED; // ms

		Duration PLAYER_GUN_RELOAD_TIME = millis(100); // default ability
		AbilityKind PLAYER_ABILITY_INITIAL = SHIELD; // rocket fire-fire time period
		int PLAYER_LIVES_INITIAL = 5; // lives at the beginning of the game
		int PLAYER_SCORE_NEW_LIFE = 10000; // we need int since we make use of int division

		static double SCORE_ASTEROID(Asteroid a) { return 30 + 2000 / (4 * a.radius); }

		double SCORE_UFO = 250;
		double SCORE_UFO_DISC = 100;
		double BONUS_MOBILITY_MULTIPLIER = 1.25; // coefficient
		double ROTATION_SPEED = 1.3 * PI / FPS; // 540 deg/sec.
		double RESISTANCE = 0.98; // slow down factor
		int ROT_LIMIT = 100; // smooths rotation at small scale, see use
		int ROT_DEL = 10; // smooths rotation at small scale, see use

		double PLAYER_BULLET_SPEED = 420 / FPS; // bullet speed in px/s/fps
		double PLAYER_BULLET_TTL = ttl(seconds(0.7)); // bullet time of living
		double PLAYER_BULLET_RANGE = PLAYER_BULLET_SPEED * PLAYER_BULLET_TTL;
		double PLAYER_BULLET_OFFSET = 10; // px
		double PLAYER_ENERGY_INITIAL = 5000;
		double PLAYER_E_BUILDUP = 1; // energy/frame
		double PLAYER_HIT_RADIUS = 13; // energy/frame
		Duration PLAYER_RESPAWN_TIME = seconds(3); // die -> respawn time
		double ROCKET_GUN_TURRET_ANGLE_GAP = D360 / 180;

		double ROCKET_ENGINE_THRUST = 0.16; // px/s/frame
		double PULSE_ENGINE_PULSE_PERIOD_TTL = ttl(millis(20));
		double PULSE_ENGINE_PULSE_TTL = ttl(millis(400));
		double PULSE_ENGINE_PULSE_TTL1 = 1 / PULSE_ENGINE_PULSE_TTL; // saves us computation

		double KINETIC_SHIELD_INITIAL_ENERGY = 0.5; // 0-1 coefficient
		Duration KINETIC_SHIELD_RECHARGE_TIME = minutes(4);
		double ROCKET_KINETIC_SHIELD_RADIUS = 25; // px
		double ROCKET_KINETIC_SHIELD_ENERGYMAX = 5000; // energy
		double KINETIC_SHIELD_LARGE_E_RATE = 50; // 50 times
		double KINETIC_SHIELD_LARGE_RADIUS_INC = 15; // by 10 px
		double KINETIC_SHIELD_LARGE_E_MAX_INC = 1; // by 100%
		double SHUTTLE_KINETIC_SHIELD_RADIUS = 180; // px
		double SHUTTLE_KINETIC_SHIELD_ENERGYMAX = 1000000; // energy
		double SHIELD_E_ACTIVATION = 0; // energy
		double SHIELD_E_RATE = 25; // energy/frame
		Duration SHIELD_ACTIVATION_TIME = millis(0);
		Duration SHIELD_PASSIVATION_TIME = millis(0);
		double HYPERSPACE_E_ACTIVATION = 150; // energy
		double HYPERSPACE_E_RATE = 4; // energy/frame
		Duration HYPERSPACE_ACTIVATION_TIME = millis(200);
		Duration HYPERSPACE_PASSIVATION_TIME = millis(200);
		double DISRUPTOR_E_ACTIVATION = 0; // energy
		double DISRUPTOR_E_RATE = 4; // energy/frame
		Duration DISRUPTOR_ACTIVATION_TIME = millis(0);
		Duration DISRUPTOR_PASSIVATION_TIME = millis(0);

		double UFO_ENERGY_INITIAL = 3000;
		double UFO_E_BUILDUP = 1; // energy/frame
		double UFO_HIT_RADIUS = 15; // energy/frame
		double UFO_ENGINE_THRUST = 0.021 * 150 / FPS; // ufo speed in px/s/fps
		Duration UFO_GUN_RELOAD_TIME = seconds(2); // ufo fire-fire time period
		double UFO_BULLET_SPEED = 380 / FPS; // bullet speed in px/s/fps
		double UFO_BULLET_TTL = 0.8 * FPS; // bullet time to live == 1s
		double UFO_BULLET_RANGE = PLAYER_BULLET_SPEED * PLAYER_BULLET_TTL;
		double UFO_BULLET_OFFSET = 10; // pc
		double UFO_RADAR_RADIUS = 70;
		double UFO_DISC_RADIUS = 3;
		double UFO_DISC_HIT_RADIUS = 9;
		int UFO_DISC_DECISION_TIME_TTL = (int) ttl(millis(500));
		double UFO_EXPLOSION_RADIUS = 100;
		double UFO_DISC_EXPLOSION_RADIUS = 8;

		static double UFO_TTL() { return ttl(seconds(randMN(40, 100))); }

		static double UFO_SQUAD_TTL() { return ttl(seconds(randMN(300, 600))); }

		static double UFO_DISCSPAWN_TTL() { return ttl(seconds(randMN(60, 180))); }

		double SATELLITE_RADIUS = 15; // energy/frame
		double SATELLITE_SPEED = 200 / FPS; // ufo speed in px/s/fps

		static double SATELLITE_TTL() { return ttl(seconds(randMN(15, 30))); }

		Image KINETIC_SHIELD_PIECE_GRAPHICS = graphics(MaterialDesignIcon.MINUS, 13, Color.AQUA, new DropShadow(GAUSSIAN, Color.DODGERBLUE.deriveColor(1, 1, 1, 0.6), 8, 0.3, 0, 0));
		double INKOID_SIZE_FACTOR = 50;
		double ENERG_SIZE_FACTOR = 50;
		double BLACKHOLE_PARTICLES_MAX = 4000;
	}

	@IsConfig
	final V<Color> ccolor = new V<>(Color.BLACK, c -> game.mission.color_canvasFade = new Color(c.getRed(), c.getGreen(), c.getBlue(), game.mission.color_canvasFade.getOpacity()));
	@IsConfig @Constraint.MinMax(min=0, max=0.1)
	final V<Double> copac = new V<>(0.05, c -> game.mission.color_canvasFade = new Color(game.mission.color_canvasFade.getRed(), game.mission.color_canvasFade.getGreen(), game.mission.color_canvasFade.getBlue(), c));
	@IsConfig
	final V<Effect> b1 = new V<>(new Glow(0.3), e -> gc_bgr.getCanvas().setEffect(e));
	@IsConfig
	final V<PlayerSpawn> spawning = new V<>(PlayerSpawn.CIRCLE);
	final ObservableList<Integer> gamepadIds = FXCollections.observableArrayList();
	@IsConfig(name = "Players")
	final ConfigurableVarList<Player> PLAYERS = new ConfigurableVarList<>(Player.class,
		new Player(1, Color.CORNFLOWERBLUE, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(2, Color.GREY, KeyCode.M, KeyCode.UP, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.N, PLAYER_ABILITY_INITIAL),
		new Player(3, Color.GREEN, KeyCode.T, KeyCode.G, KeyCode.F, KeyCode.H, KeyCode.R, PLAYER_ABILITY_INITIAL),
		new Player(4, Color.SANDYBROWN, KeyCode.I, KeyCode.K, KeyCode.J, KeyCode.L, KeyCode.U, PLAYER_ABILITY_INITIAL),
		new Player(5, Color.RED, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(6, Color.YELLOW, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(7, Color.CADETBLUE, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(8, Color.MAGENTA, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL)
	);

	private static final Color HUD_COLOR = Color.AQUA;
	private static final double HUD_OPACITY = 0.25;
	private static final double HUD_DOT_GAP = 3;
	private static final double HUD_DOT_DIAMETER = 1;
	void drawDottedLine(double x, double y, double lengthStart, double length, double cosDir, double sinDir, Color color) {
		gc.setFill(color);
		for (double i=lengthStart; i<length; i+=HUD_DOT_GAP)
			gc.fillOval(game.field.modX(x+i*cosDir), game.field.modY(y+i*sinDir), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
	}
	void drawHudLine(double x, double y, double lengthStart, double length, double cosDir, double sinDir, Color color) {
		gc.setFill(color);
		gc.setGlobalAlpha(HUD_OPACITY);

		for (double i=lengthStart; i<length; i+=HUD_DOT_GAP)
			gc.fillOval(game.field.modX(x+i*cosDir), game.field.modY(y+i*sinDir), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);

		gc.setGlobalAlpha(1);
	}
	void drawHudCircle(double x, double y, double r, double angle, double angleWidth, Color color) {
		gc.setFill(color);
		gc.setGlobalAlpha(HUD_OPACITY);

		double length = angleWidth*r;
		int pieces = (int)(length/HUD_DOT_GAP);
		double angleStart = angle-angleWidth/2;
		double angleBy = angleWidth/pieces;
		for (int p=0; p<pieces; p++) {
			double a = angleStart+p*angleBy;
			gc.fillOval(game.field.modX(x+r*cos(a)), game.field.modY(y+r*sin(a)), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
		}

		gc.setGlobalAlpha(1);
	}
	void drawHudCircle(double x, double y, double r, Color color) {
		drawHudCircle(x, y, r, 0, D360, color);
	}
	void drawPolygon(double[] xs, double[] ys, long pointCount) {
		// Degrades performance a lot (probably due to out of canvas drawing
		// gc.strokePolygon(polygon.getXPoints(), polygon.getYPoints(), polygon.getNumPoints());
		// for now use the below:

		yes(xs.length==ys.length);

		for (int j=0; j<pointCount; j++) {
			int k = j==pointCount-1 ? 0 : j+1;
			double x1 = xs[j], x2 = xs[k], y1 = ys[j], y2 = ys[k];
			if (game.field.isInside(x1,y1) || game.field.isInside(x2,y2))
				drawLine(x1,y1,x2,y2);
		}
	}
	void drawLine(double x, double y, double tox, double toy) {
		double dist = sqrt((x-tox)*(x-tox)+(y-toy)*(y-toy));
		double distX = tox-x, distY = toy-y;
		double X = x, Y = y;
		int pieces = (int)(dist/HUD_DOT_GAP);
		double dx = distX/pieces, dy = distY/pieces;
		for (double i=0; i<pieces; i++) {
			gc.fillOval(X,Y, 1,1);
			X += dx; Y += dy;
		}
	}
	void drawLine(double x, double y, double length, double dirCos, double dirSin) {
		for (double i=0; i<length; i+=10)
			gc.fillOval(x+i*dirCos, y+i*dirSin, 1,1);
	}
	Particle createRandomDisruptorParticle(double radiusMin, double radiusMax, SO ff) {
		return createRandomDisruptorParticle(radiusMin, radiusMax, ff, false);
	}
	Particle createRandomDisruptorParticle(double radiusMin, double radiusMax, SO ff, boolean noMove) {
		double angle = rand0N(D360);
		double dist = randMN(radiusMin,radiusMax);
		return new Particle(ff.x+dist*cos(angle),ff.y+dist*sin(angle), ff.dx,ff.dy, ttl(millis(350))) {
			@Override
			void draw() {
				GraphicsContext g = gc_bgr;
				g.setGlobalAlpha(ttl);
				g.setFill(game.humans.color);
				g.fillOval(x-0.5,y-0.5,1,1);
				g.setGlobalAlpha(1);
			}

			@Override
			void doLoopBegin() {
				super.doLoopBegin();
				if (noMove) {
//					dx = dy = 0;
					dx *= 0.95;
					dy *= 0.95;
				}
			}
		};
	}

	/** Encompasses entire game. */
	class Game {
		final V<Boolean> paused = new V<>(false);
		final V<Boolean> running = new V<>(false);
		private boolean isInitialized = false;

		final V<Boolean> deadly_bullets = new V<>(false);

		final ObservableSet<Player> players  = FXCollections.observableSet();
		final EntityManager entities = new EntityManager();
		final ObjectStore<PO> oss = new ObjectStore<>(o -> o.type);
		final Collection<PO> os = new ArrayDeque<>();
		final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
		final Map<KeyCode,Long> keyPressTimes = new HashMap<>();
		final GamepadDevices gamepads = new GamepadDevices() {
			private IControllerListener listener;

			@Override
			protected void onInit(IController[] gamepads) {
				listener = new IControllerListener() {
					@Override
					public void connected(IController c) {
						System.out.println("connected device:");
						System.out.println("DeviceID = " + c.getDeviceID());
						System.out.println("DeviceTypeIdentifier = " + c.getDeviceTypeIdentifier());
						System.out.println("Description = " + c.getDescription());
						System.out.println("ProductID = " + c.getProductID());
						System.out.println("VendorID = " + c.getVendorID());

						gamepadIds.add(c.getDeviceID());
						stream(players).sorted(by(p -> p.id.get()))
							.findFirst(p -> p.gamepadId.get()==null)
							.ifPresent(p -> p.gamepadId.set(c.getDeviceID()));
					}

					@Override
					public void disConnected(IController c) {
						System.out.println("disconnected device:");
						System.out.println("DeviceID = " + c.getDeviceID());
						System.out.println("DeviceTypeIdentifier = " + c.getDeviceTypeIdentifier());
						System.out.println("Description = " + c.getDescription());
						System.out.println("ProductID = " + c.getProductID());
						System.out.println("VendorID = " + c.getVendorID());

						gamepadIds.remove(c.getDeviceID());
						stream(players).filter(p -> p.gamepadId.get()!=null && p.gamepadId.get()==c.getDeviceID())
							.forEach(p -> p.gamepadId.set(null));
					}

					@Override
					public void buttonDown(IController iController, IButton iButton, ButtonID buttonID) {}

					@Override
					public void buttonUp(IController iController, IButton iButton, ButtonID buttonID) {}

					@Override
					public void moveStick(IController iController, StickID stickID) {
						System.out.println("stick moved");
					}
				};
				Controllers.instance().addListener(listener);
				gamepadIds.addAll(stream(gamepads).map(g -> g.getDeviceID()).toList());
			}

			@Override
			public void dispose() {
				if (isInitialized)
					Controllers.instance().removeListener(listener);
				super.dispose();
			}

			@Override
			protected void doLoopImpl(IController[] gamepads) {
				if (gamepads.length > 0)
					Stream.of(gamepads).forEach(g ->
						players.stream().filter(p -> p.alive).filter(p -> p.gamepadId.get()!=null && p.gamepadId.get()==g.getDeviceID()).forEach(p -> {
							IButton engine1B = g.getButton(1); // g.getButton(ButtonID.FACE_DOWN);
							IButton engine2B = g.getButton(10);
							IButton engine3B = g.getButton(11);
							IButton fireB = g.getButton(0); // g.getButton(ButtonID.FACE_LEFT);
							IButton ability1B = g.getButton(4);
							IButton ability2B = g.getButton(5);
							IButton leftB = g.getButton(6);
							IButton rightB = g.getButton(7);
							boolean isEngine = (engine1B!=null && engine1B.isPressed()) || (engine2B!=null && engine2B.isPressed()) || (engine3B!=null && engine3B.isPressed());
							boolean isAbility = (ability1B!=null && ability1B.isPressed()) || (ability2B!=null && ability2B.isPressed());
							boolean isLeft = (leftB!=null && leftB.isPressed()) || g.getDpadDirection() == DpadDirection.LEFT;
							boolean isRight = (rightB!=null && rightB.isPressed()) || g.getDpadDirection()==DpadDirection.RIGHT;
							boolean isFire = fireB!=null && fireB.isPressed();
//							boolean isFireOnce = fireB!=null && fireB.isPressedOnce(); // !support multiple players per controller
							boolean isFireOnce = isFire && !p.wasGamepadFire;
							boolean isRihtOnce = isRight && !p.wasGamepadRight;
							boolean isLeftOnce = isLeft && !p.wasGamepadLeft;
							p.wasGamepadLeft = isLeft;
							p.wasGamepadRight = isRight;
							p.wasGamepadFire = isFire;

							if (isLeftOnce) keyPressTimes.put(p.keyLeft.get(),loop.now);
							if (isRihtOnce) keyPressTimes.put(p.keyRight.get(),loop.now);

							p.isInputThrust |= isEngine;
							p.isInputLeft |= isLeft;
							p.isInputRight |= isRight;
							p.isInputFire |= isFire;
							p.isInputFireOnce |= isFireOnce;
							p.isInputAbility |= isAbility;
						})
					);
			}
		};

		final Loop loop = new Loop(this::doLoop);
		final GameSize field = new GameSize();
		final UfoFaction ufos = new UfoFaction();
		final PlayerFaction humans = new PlayerFaction();
		final TTLList runNext = new TTLList();
		final Set<PO> removables = new HashSet<>();

		Grid grid;// = new Grid(gc_bgr, 1000, 500, 50, 50);
		boolean useGrid = true;

		int mission_counter = 0;   // mission counter, starts at 1, increments by 1
		Mission mission = null; // current mission, (they repeat), starts at 1, = mission % missions +1
		boolean isMissionScheduled = false;
		MissionInfoButton mission_button;
		final StatsGame stats = new StatsGame();
		final MapSet<Integer,Mission> missions = new MapSet<>(m -> m.id,

//			new Mission(1, "Energetic fragility","10⁻¹⁵","",
//				null, Color.RED,Color.rgb(255,255,255,0.015), null,Particler::new
////				null, Color.RED,Color.rgb(0,0,0,0.08), null,Particler::new
//			).initializer(game -> game.useGrid = false, game -> game.useGrid = true),
            new Mission(1, "The strange world", "10⁻⁴m", "",
                null,Color.BLACK, Color.rgb(225,225,225, 0.2),null, PlanetoDisc::new
            ),
			new Mission(2, "Sumi-e","10⁻¹⁵","",
				null,Color.LIGHTGREEN, Color.rgb(0, 51, 51, 0.1),null, Inkoid::new
			),
			new Mission(3, "Mol's molecule","","",
				null,Color.YELLOW, Color.rgb(0, 15, 0, 0.1), null, Fermi::new
			),
			new Mission(4, "PartiCuLar elEment","10⁻¹⁵","",
				null,Color.GREEN, Color.rgb(0, 15, 0, 0.08), null, Fermi::new
			),
			new Mission(5, "Decay of the weak force","10⁻¹","",
				null,Color.GREEN, Color.rgb(0, 15, 0, 0.08), null, Fermi::new
			),
			new Mission(6, "String a string","10⁻¹⁵","",
				null,Color.YELLOW, Color.rgb(10, 11, 1, 0.2),null, Stringoid::new
			), //new Glow(0.3)
			new Mission(7, "Mother of all branes","10⁻¹⁵","",
				null,Color.DODGERBLUE, Color.rgb(0, 0, 15, 0.08), null, Genoid::new
			),
			new Mission(8, "Energetic fragility","10⁻¹⁵","",
				null,Color.DODGERBLUE, Color.rgb(10,10,25,0.08), null,Energ::new
			),
			new Mission(9, "Planc's plancton","10⁻¹⁵","",
				null,Color.DARKCYAN, new Color(0,0.08,0.08,0.09),null,Linker::new
			)//,
//			new Mission(10, "T duality of a planck boundary","10⁻¹⁵","",
//				null,Color.DARKSLATEBLUE,new Color(1,1,1,0.08),null,Energ2::new
//			),
//			new Mission(11, "Informative xperience","10⁻¹⁵","",
//				bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
//			),
//			new Mission(12, "Holographically principled","10⁻¹⁵","",
//				bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
//			)
		);

		final Set<Enhancer> ROCKET_ENHANCERS = set(

			// fire upgrades
			new Enhancer("Gun", MaterialDesignIcon.KEY_PLUS, seconds(5),
				r -> r.gun.turrets.inc(), r -> {/*r.gun.turrets.dec()*/},
				"- Mounts additional gun turret",
				"- Increases chance of hitting the target",
				"- Increases maximum possible target damage by 100%"
			),
			new Enhancer("Rapid fire", MaterialDesignIcon.BLACKBERRY, seconds(12), r -> r.rapidFire.inc(), r -> r.rapidFire.dec(),
				" - Largely increases rate of fire temporarily. Fires constant stream of bullets",
				" - Improved hit efficiency due to bullet spam",
				" - Improved mobility due to less danger of being hit",
				"Tip: Fire constantly. Be on the move. Let the decimating power of countless bullets"
			  + " be your shield. The upgrade lasts only a while - being static is a disadvantage."
			),
			new Enhancer("Long fire", MaterialDesignIcon.DOTS_HORIZONTAL, seconds(60), r -> r.powerFire.inc(), r -> r.powerFire.dec(),
				"- Increases bullet speed",
				"- Increases bullet range",
				"Tip: Aim closer to target. Faster bullet will reach target sooner."
			),
			new Enhancer("High energy fire", MaterialDesignIcon.MINUS, seconds(25), r -> r.energyFire.inc(), r -> r.energyFire.dec(),
				"- Bullets penetrate the target",
				"- Increases bullet damage, 1 hit kill",
				"- Multiple target damage",
				"Tip: Fire at bigger target or group of targets.",
				"Tip: Try lining up targets into a line."
			),
			new Enhancer("Split ammo", MaterialIcon.CALL_SPLIT, seconds(15), r -> r.splitFire.inc(), r -> r.splitFire.dec(),
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
			new Enhancer("Aim enhancer", MaterialDesignIcon.RULER, seconds(45),
				r -> {
					Ship.LaserSight ls = r.new LaserSight();
					game.runNext.add(seconds(45),ls::dispose);
				},
				"- Displays bullet path",
				"- Displays bullet range"
			),

			new Enhancer("Mobility", MaterialDesignIcon.TRANSFER, seconds(25), r -> r.engine.mobility.inc(), r -> r.engine.mobility.dec(),
				"- Increases propulsion efficiency, i.e., speed",
				"- Increases maneuverability",
				"Tip: If there is ever time to move, it is now. Don't idle around."
			),
			new Enhancer("Intel", MaterialDesignIcon.EYE, minutes(2), r ->  humans.intelOn.inc(), r -> humans.intelOn.dec(), false,
				"- Reports incoming ufo time & location",
				"- Reports incoming upgrade time & location",
				"- Reveals exact upgrade type before it is picked up",
				"- Displays bullet range",
				"- Displays player control area and marks the best area control position",
				"Tip: This upgrade is automatically shared."
			),
			new Enhancer("Share upgrades", MaterialDesignIcon.SHARE_VARIANT, minutes(2),
				r -> humans.share_enhancers=true, r -> humans.share_enhancers=false,
				"- Applies upgrades to all allies",
				"Tip: The more allies, the bigger the gain."
			),
			new Enhancer("Shuttle support", FontAwesomeIcon.SPACE_SHUTTLE, seconds(5),
				r -> humans.send(r,Shuttle::new), r -> {}, false,
				"- Calls in supply shuttle",
				"- Provides large and powerful stationary kinetic shield",
				"- Provides additional upgrades",
				"Tip: This upgrade can not be shared."
			),
			new Enhancer("Super shield", FontAwesomeIcon.CIRCLE_THIN, seconds(5),
				r -> humans.send(r,SuperShield::new), r -> {}, false,
				"- Calls in support shield",
				"- Provides large and powerful stationary kinetic shield",
				"Tip: This upgrade can not be shared."
			),
			new Enhancer("Super disruptor", MaterialIcon.BLUR_ON, seconds(5),
				r -> humans.send(r, SuperDisruptor::new), r -> {}, false,
				"- Calls in support disruptor",
				"- Provides large and powerful stationary force field that slows objects down",
				"Tip: Hide inside and use as a form of shield.",
				"Tip: Objects with active propulsion will still be able to move, albeit slowed down.",
				"Tip: This upgrade can not be shared."
			),

			// kinetic shield upgrades
			new Enhancer("Shield energizer", MaterialDesignIcon.IMAGE_FILTER_TILT_SHIFT, seconds(5),
				r -> {
					r.kinetic_shield.KSenergy_max *= 1.1;
					r.kinetic_shield.changeKSenergyToMax();
				},
				"- Sets kinetic shield energy to max",
				"- Increases maximum kinetic shield energy by 10%"
			),
			new Enhancer("Shield enhancer", FontAwesomeIcon.SUN_ALT, seconds(25), r -> r.kinetic_shield.large.inc(), r -> r.kinetic_shield.large.dec(),
				"- Increases kinetic shield range by " + KINETIC_SHIELD_LARGE_RADIUS_INC + "px",
				"- Increases maximum kinetic shield energy by " + (KINETIC_SHIELD_LARGE_E_MAX_INC*100) + "%",
				"- Increases kinetic shield energy accumulation " + (KINETIC_SHIELD_LARGE_E_RATE) +" times",
				"Tip: You are not invincible, but anyone should think twice about hitting you. Go on the offensive. Move."
			),

			// energy upgrades
			new Enhancer("Charger", MaterialDesignIcon.BATTERY_CHARGING_100, seconds(5), r -> r.energy_buildup_rate *= 1.1,
				"- Increases energy accumulation by 10%"
			),
			new Enhancer("Battery", MaterialDesignIcon.BATTERY_POSITIVE, seconds(5), r -> r.energy_max *= 1.1,
				"- Increases maximum energy by 10%"
			),
			new Enhancer("Energy (small)", MaterialDesignIcon.BATTERY_30, seconds(5),
				r -> r.energy = min(r.energy+2000,r.energy_max),
				"- Increases energy by up to 2000"
			),
			new Enhancer("Energy (medium)", MaterialDesignIcon.BATTERY_60, seconds(5),
				r -> r.energy = min(r.energy+5000,r.energy_max),
				"- Increases energy by up to 5000"
			),
			new Enhancer("Energy (large)", MaterialDesignIcon.BATTERY, seconds(5),
				r -> r.energy = min(r.energy+10000,r.energy_max),
				"- Increases energy by up to 10000"
			)
		);
		final Set<Enhancer> ROCKET_ENHANCERS_NO_SHUTTLE = stream(ROCKET_ENHANCERS).filter(re -> !"Shuttle support".equals(re.name)).toSet();
		final Set<Achievement> ACHIEVEMENTS = set(
			achievement1(
					"Dominator", MaterialDesignIcon.DUMBBELL,
					 game -> stream(game.players).max(by(p -> p.stats.controlAreaSize.getAverage())).get(),
					 "Control the largest nearby area throughout the game"
				).onlyIf(game -> game.players.size()>1),
			achievement1(
					"Control freak", MaterialDesignIcon.ARROW_EXPAND,
					game -> stream(game.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
					"Control your nearby area the most effectively"
				).onlyIf(game -> game.players.size()>1),
			achievement01(
					"Reaper's favourite", MaterialDesignIcon.HEART_BROKEN,
					game -> Optional.ofNullable(game.stats.firstDead),
					"Be the first to die"
				).onlyIf(game -> game.players.size()>1),
			achievement1(
					"Live and prosper", MaterialDesignIcon.HEART,
					game -> stream(game.players).max(by(p -> stream(p.stats.liveTimes).max(Duration::compareTo).get())).get(),
					"Live the longest"
				).onlyIf(game -> game.players.size()>1),
			achievement0N(
					"Invincible", MaterialDesignIcon.MARKER_CHECK,
					game -> stream(game.players).filter(p -> p.stats.deathCount==0),
					"Don't die"
				),
			achievement01(
					"Quickdraw", MaterialDesignIcon.CROSSHAIRS,
					game -> stream(game.players).filter(p -> p.stats.fired1stTime!=null).minBy(p -> p.stats.fired1stTime),
					"Be the first to shoot"
				).onlyIf(game -> game.players.size()>1),
			achievement01(
					"Rusher", MaterialDesignIcon.CROSSHAIRS_GPS,
					game -> stream(game.players).filter(p -> p.stats.hitEnemy1stTime!=null).minBy(p -> p.stats.hitEnemy1stTime),
					"Be the first to deal damage"
				).onlyIf(game -> game.players.size()>1),
			achievement01(
					"Mobile", MaterialDesignIcon.RUN,
					game -> stream(game.players).maxBy(p -> p.stats.distanceTravelled),
					"Travel the greatest distance"
				).onlyIf(game -> game.players.size()>1),
			achievement0N(
					"Pacifist", MaterialDesignIcon.NATURE_PEOPLE,
					game -> stream(game.players).filter(p -> p.stats.fired1stTime==null),
					"Never shoot"
				),
			// TODO: fix this for situations where killCount is the same for multiple players
			achievement01(
					"Hunter", MaterialDesignIcon.BIOHAZARD,
					game -> stream(game.players).maxBy(p -> p.stats.killUfoCount),
					"Kill most UFOs"
				).onlyIf(game -> game.players.size()>1)
		);
		Voronoi voronoi = new Voronoi2(
			(rocket,area) -> rocket.player.stats.controlAreaSize.accept(area),
			(rocket,areaCenterDistance) -> rocket.player.stats.controlAreaCenterDistance.accept(areaCenterDistance),
			(centerX,centerY) -> {
				if (humans.intelOn.is()) {
					gc_bgr.setFill(humans.color);
					gc_bgr.fillOval(centerX - 1, centerY - 1, 5, 5);
//					drawHudCircle(centerX, centerY, 10, humans.color);
				}
			},
			(edges) -> edges.forEach(edge -> {
				gc_bgr.save();
				gc_bgr.setLineWidth(1);
				gc_bgr.setStroke(HUD_COLOR);
				gc_bgr.setGlobalAlpha(0.05);
				gc_bgr.strokeLine(edge.x1, edge.y1, edge.x2, edge.y2);
				gc_bgr.restore();
			})
		);

		void pause(boolean v) {
			if (!running.get() || paused.get()==v) return;
			paused.set(v);
			if (v) loop.stop();
			else loop.start();
		}

		private void init() {
			grid = new Grid(gc, game.field.width, game.field.height, 20);
			gamepads.init();
		}

		void start(int player_count) {
			stop();

			if (!isInitialized) {
				init();
				isInitialized = true;
			}

			players.addAll(listF(player_count,PLAYERS.list::get));
			players.forEach(Player::reset);



			gamepads.getControllers()
				.filter(g -> stream(players).noneMatch(p -> p.gamepadId.get()!=null && p.gamepadId.get()==g.getDeviceID()))
				.sorted(by(g -> g.getDeviceID()))
				.forEach(g -> stream(players).sorted(by(p -> p.id.get()))
					              .findFirst(p -> p.gamepadId.get()==null)
					              .ifPresent(p -> p.gamepadId.set(g.getDeviceID()))
				);

			running.set(true);
			loop.reset();
			mission_counter = 0;
			isMissionScheduled = false;
			ufos.init();
			humans.init();

			players.forEach(Player::spawn);
			loop.start();

			timer200ms.start();
			playfield.requestFocus();
			nextMission();

			runNext.add(() -> mission_button = new MissionInfoButton());
		}

		void doLoop() {
			if (loop.isNth((long)FPS)) LOGGER.debug("particle.count= {}", oss.get(Particle.class).size());

			// collect and handle player inputs
			gamepads.doLoop();
			players.stream().filter(p -> p.alive).forEach(p -> {
				if (pressedKeys.contains(p.keyLeft.get())) p.isInputLeft |= true;
				if (pressedKeys.contains(p.keyRight.get())) p.isInputRight |= true;
				if (pressedKeys.contains(p.keyThrust.get())) p.isInputThrust |= true;
				if (pressedKeys.contains(p.keyFire.get())) p.isInputFire |= true;
				if (pressedKeys.contains(p.keyAbility.get())) p.isInputAbility |= true;
			});
			players.forEach(Player::doInputs);

			runNext.run();

			// remove inactive objects
			for (PO o : os) if (o.dead) removables.add(o);
			os.removeIf(o -> o.dead);
			for (PO o : oss.get(Particle.class)) if (o.dead) removables.add(o);
			oss.forEachSet(set -> set.removeIf(o -> o.dead));
			removables.forEach(PO::dispose);
			removables.clear();

			entities.addPending();
			entities.removePending();

//			mission_button.doLoop();

			// reset gravity potentials
			oss.forEach(o -> o.g_potential = 1);

			// apply forces
			// recalculate gravity potentials
			forEachPair(entities.forceFields, os, ForceField::apply);
			forEachPair(filter(entities.forceFields, ff -> ff instanceof DisruptorField), oss.get(Particle.class), ForceField::apply);

			// canvas clearing
			// we use optional "fade" effect. Filling canvas with transparent color repeatedly
			// will serve instead of clearing & spare drawing complex stuff & produce bgr & interesting effect
			if (mission.color_canvasFade==null) {
				gc_bgr.clearRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
			} else {
				gc_bgr.setFill(mission.color_canvasFade);
				gc_bgr.fillRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
			}
			gc.clearRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());

			if (useGrid) {
				grid.update();
				grid.draw();
			}

			// update & redraw active objects
			entities.forceFields.forEach(ForceField::doLoop);

			os.forEach(PO::doLoop);

			// guns & firing
			// todo: ship logic belongs to ship class...
			stream(oss.get(Rocket.class).stream(),oss.get(Ufo.class).stream()).forEach(ship -> {
				if (ship.gun!=null) {
					ship.gun.fireTTL--;
				}
				if (ship.gun!=null && ship.gun.control==AUTO && ship.gun.fireTTL<0) {
					ship.gun.fireTTL = ttl(ship.gun.time_reload);
					runNext.add(() -> ship.gun.ammo_type.apply(ship.gun.aimer.apply()));
				}
			});

			// collisions
			forEachPair(oss.get(Bullet.class),filter(os, e -> !(e instanceof Bullet)), Bullet::checkCollision);

			oss.forEach(Rocket.class,Rocket.class, (r1,r2) -> {
				if (!r1.isin_hyperspace && !r2.isin_hyperspace && r1.isHitDistance(r2)) {
					if (r1.ability_main instanceof Shield && r1.ability_main.isActivated()) {
						r1.dx = r1.dy = 0;
						r1.engine.off();
						r2.engine.off();
					} else {
						r1.player.die();
					}
					if (r2.ability_main instanceof Shield && r2.ability_main.isActivated()) {
						r2.dx = r2.dy = 0;
					} else {
						r2.player.die();
					}
				}
			});
			oss.forEach(Rocket.class,Satellite.class, (r,s) -> {
				if (!r.isin_hyperspace && r.isHitDistance(s)) {
					s.pickUpBy(r);
				}
			});
			oss.forEach(Rocket.class,Ufo.class, (r,u) -> {
				if (!r.isin_hyperspace && r.isHitDistance(u)) {
					if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
						r.dx = r.dy = 0;
					} else {
						r.player.die();
					}
					u.dead = true;
					ufos.onUfoDestroyed();
				}
			});
			oss.forEach(Rocket.class,UfoSwarmer.class, (r, ud) -> {
				if (!r.isin_hyperspace && r.isHitDistance(ud)) {
					if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
						r.dx = r.dy = 0;
					} else {
						r.player.die();
					}
					ud.dead = true;
				}
			});
			oss.forEach(Rocket.class,Asteroid.class, (r,a) -> {
				if (!r.isin_hyperspace && r.isHitDistance(a)) {
					if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
						((Shield)r.ability_main).onHit(a);
					} else {
						if (r.kineticEto(a)<r.kinetic_shield.KSenergy) {
							r.kinetic_shield.onShieldHit(a);

							r.dx += 0.2*a.dx;
							r.dy += 0.2*a.dy;
							r.ddirection += randOf(-1,1)*randMN(0.02,0.04);
						} else {
							r.player.die();
						}
					}
					a.onHit(r);
					onPlanetoidDestroyed();
				}
			});
			oss.forEach(Shuttle.class,Asteroid.class, (s,a) -> {
				if (s.isHitDistance(a)) {
					a.onHit(s);
					onPlanetoidDestroyed();
					s.die(a);
				}
			});
			oss.forEach(SuperShield.class,Asteroid.class, (s,a) -> {
				if (s.isHitDistance(a)) {
					if (s.kineticEto(a)<s.kinetic_shield.KSenergy) {
						s.kinetic_shield.onShieldHit(a);
					} else {
						s.die(a);
					}
					a.onHit(s);
					onPlanetoidDestroyed();
				}
			});

			oss.get(Bullet.class).forEach(Bullet::draw);

			// non-interacting stuff last
			oss.get(Particle.class).forEach(Particle::doLoop);
			stream(oss.get(Particle.class)).select(Draw2.class).forEach(Draw2::drawBack);
			stream(oss.get(Particle.class)).select(Draw2.class).forEach(Draw2::drawFront);

			voronoi.compute(oss.get(Rocket.class), game.field.width, game.field.height, this);

//	        gc.setGlobalAlpha(1);
//	        gc.setLineWidth(1);
//	        gc.setStroke(Color.GREEN);
//	        gc.beginPath();
//	        gc.moveTo(0,0);
//	        repeat(100, i -> {
//		        gc.lineTo(8*(i/2),1000);
//		        gc.lineTo(10*((i/2)+1),0);
//	        });
//	        gc.stroke();
		}

		void stop() {
			running.set(false);
			timer200ms.stop();
			loop.stop();
			players.clear();
			os.clear();
			oss.clear();
			entities.clear();
			runNext.clear();
			playfield.getChildren().clear();
		}

		/** Clears resources. No game session will occur after this. */
		void dispose() {
			stop();
			gamepads.dispose();
		}

		void nextMission() {
			if (isMissionScheduled) return;
			isMissionScheduled = true;
			if (mission!=null) mission.disposer.accept(this);
			mission_counter = mission_counter==0 ? 1 : mission_counter+1;
			int id = mission_counter%missions.size();
			int mission_id = id==0 ? missions.size() : mission_counter%missions.size(); // modulo mission count, but start at 1
			mission = missions.get(mission_id);
			mission.start();
		}
		void onPlanetoidDestroyed() {
			// it may take a cycle or two for asteroids to get disposed, hence the delay
			runNext.add(10, () -> {
				if (oss.get(Asteroid.class).isEmpty()) nextMission();
			});
		}
		void over() {
			players.forEach(p -> p.stats.accGameEnd(loop.id));
			runNext.add(seconds(5), () -> {
				Map<Player,List<Achievement>> as = stream(ACHIEVEMENTS)
							.filter(a -> a.condition==null || a.condition.test(this))
							.flatMapToEntry(a -> stream(a.evaluator.apply(this)).toMap(player -> player, player -> a))
							.grouping();
				new EndGamePane().show(as);
				stop();
			});
		}

		void message(String title) {
			message(title, "");
		}

		void message(String title, String subtitle) {
			message.setText(title+"\n"+subtitle);
			Anim a = new Anim(millis(500),x -> message.setOpacity(x*x));
			a.playOpen();
			runNext.add(seconds(2),a::playClose);
		}


		class PlayerFaction {
			final InEffect intelOn = new InEffect();
			Color color = Color.DODGERBLUE;
			Color colorTech = Color.AQUAMARINE;
			boolean share_enhancers;

			void init() {
				share_enhancers = false;
				intelOn.reset();
				runNext.addPeriodic(() -> SATELLITE_TTL()/sqrt(players.size()), humans::sendSatellite);
			}

			void send(Rocket r, Consumer<? super Rocket> action) {
				game.runNext.add(seconds(2),() -> pulseCall(r));
				game.runNext.add(seconds(4),() -> pulseCall(r));
				game.runNext.add(seconds(6),() -> action.accept(r));
			}
			void sendSatellite() {
				sendSatellite(randEnum(Side.class));
			}
			private void sendSatellite(Side side) {
				Side s = side==null ? randEnum(Side.class) : side;
				double offset = 50;
				double x = s==Side.LEFT ? offset : game.field.width-offset;
				double y = rand0N(game.field.height);
				if (humans.intelOn.is()) pulseAlert(x,y);
				runNext.add(seconds(2), () -> {
					Satellite st = new Satellite(s);
					st.x = x;
					st.y = y;
					createHyperSpaceAnimIn(game, st);
				} );
			}

			void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
			void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
			void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
			void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
			void pulseCall(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,2.5,color,false);
			}
			void pulseAlert(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,-2.5,color,false);
			}
		}
		class UfoFaction {
			int losses = 0;
			int losses_aggressive = 5;
			int losses_cannon = 20;
			Rocket ufo_enemy = null;
			boolean aggressive = false;
			boolean canSpawnDiscs = false;
			Color color = Color.rgb(114,208,74);

			void init() {
				losses = 0;
				ufo_enemy = null;
				aggressive = false;
				runNext.addPeriodic(() -> UFO_TTL()/sqrt(players.size()), ufos::sendUfo);
				runNext.addPeriodic(() -> UFO_SQUAD_TTL()/sqrt(players.size()), ufos::sendUfoSquadron);
				runNext.addPeriodic(() -> UFO_DISCSPAWN_TTL()/sqrt(players.size()), () -> canSpawnDiscs = true);
			}

			void onUfoDestroyed() {
				losses++;
				if (losses>losses_aggressive) {
					aggressive = losses%2==0;
				}
				if (losses%losses_cannon==losses_cannon-1) {
					activateSlipSpaceCannon();
				}
			}
			void activateSlipSpaceCannon() {
//				message("U.F.O. CANNON ALERT");
//				repeat(5, () -> runNext.add(seconds(rand0N(15)), this::fireSlipSpaceCannon));
			}
			void fireSlipSpaceCannon() {
				Player player = randOf(filter(players, p -> p.rocket!=null));
				runNext.add(() -> new UfoSlipSpaceBullet(player.rocket));
			}

			void sendUfo() {
				sendUfo(randEnum(Side.class));
			}
			void sendUfoSquadron() {
				ufo_enemy = players.isEmpty() ? null : randOf(players).rocket;
				Side side = randEnum(Side.class);
				int count = (int)(2+rand01()*8);
				if (randBoolean()) {
					repeat(count, () -> runNext.add(seconds(rand0N(0.5)), () -> sendUfo(side)));
				} else {
					double w = 0, h = rand0N(game.field.height);
					pulseAlert(w, h);
					int swarmId = randInt(Integer.MAX_VALUE);
					runNext.add(millis(500), () -> {
						if (randBoolean())
							forEachInLineBy(w, h, -15, -15, 8 * count, (x, y) -> new UfoSwarmer(x, game.field.modY(y), D360)).forEach(u -> {
									u.isActive = false;
									u.isInitialOutOfField = true;
									u.swarmId = swarmId;
								});
						else
							forEachOnCircleBy(w, h, 15, 8 * count, (x, y, a) -> new UfoSwarmer(x, game.field.modY(y), D360)).forEach(u -> {
									u.isActive = false;
									u.isInitialOutOfField = true;
									u.swarmId = swarmId;
								});
					});
				}
			}
			private void sendUfo(Side side) {
				Side s = side==null ? randEnum(Side.class) : side;
				double offset = 50;
				double x = s==Side.LEFT ? offset : game.field.width-offset;
				double y = rand0N(game.field.height);
				if (humans.intelOn.is()) pulseAlert(x,y);
				runNext.add(seconds(2), () -> {
					Ufo u = new Ufo(s,aggressive);
					u.x = x;
					u.y = y;
					createHyperSpaceAnimIn(game, u);
				} );
			}

			void pulseCall(PO o) {
				pulseCall(o.x,o.y,o.dx,o.dy);
				new RadioWavePulse(o,-2.5,color,true);
			}
			void pulseCall(double x, double y) {
				pulseCall(x,y,0,0);
			}
			void pulseAlert(double x, double y) {
				pulseAlert(x,y,0,0);
			}
			void pulseAlert(PO o) {
				pulseAlert(o.x,o.y,o.dx,o.dy);
			}
			void pulseCall(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,-2.5,color,true);
			}
			void pulseAlert(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,-2.5,color,true);
			}
		}
		class Mission {
			final int id;
			final String name, scale, details;
			final Background bgr;
			final Ƒ5<Double,Double,Double,Double,Double,Asteroid> planetoidConstructor;
			final Color color;
			Color color_canvasFade; // normally null, canvas fade effect
			final Effect toplayereffect;
			Consumer<Game> initializer = game -> {};
			Consumer<Game> disposer = game -> {};

			public Mission(int ID, String NAME, String SCALE, String DETAILS, Background BGR, Color COLOR, Color CANVAS_REDRAW,
					Effect effect, Ƒ5<Double,Double,Double,Double,Double,Asteroid> planetoidFactory) {
				id = ID;
				name = NAME; scale = SCALE; details = DETAILS;
				bgr = BGR;
				color = COLOR;
				planetoidConstructor = planetoidFactory;
				color_canvasFade = CANVAS_REDRAW;
				toplayereffect = effect;
			}

			Mission initializer(Consumer<Game> INITIALIZER, Consumer<Game> DISPOSER) {
				initializer = INITIALIZER;
				disposer = DISPOSER;
				return this;
			}

			void start() {
				((Pane)playfield.getParent()).setBackground(bgr);
				playfield.setEffect(toplayereffect);
				grid.color = color;

				double size = sqrt(game.field.height)/1000;
				int planetoids = 3 + (int)(2*(size-1)) + (mission_counter-1) + players.size()/2;
				double delay = ttl(seconds(mission_counter==1 ? 2 : 5));
				runNext.add(delay/2, () -> message("Level " + mission_counter, name));
				runNext.add(delay, () -> repeat(planetoids, i -> spawnPlanetoid()));
				runNext.add(delay, () -> isMissionScheduled = false);
				initializer.accept(Game.this);


				game.humans.color=color;
				game.humans.colorTech=color;
				game.ufos.color = color;
			}

			void spawnPlanetoid() {
				boolean vertical = randBoolean();
				planetoidConstructor.apply(
					vertical ? 0 : rand01()*game.field.width,
					vertical ? rand01()*game.field.height : 0,
					randMN(-1,1)*0.7,rand0N(D360), 1d
				);
			}
		}
		class EntityManager {
			final Set<ForceField> forceFields = new HashSet<>();
			final Set<ForceField> forceFieldstoAdd = new HashSet<>();  // stores new entities that will be added on next loop start

			void addPending() {
				forceFields.addAll(forceFieldstoAdd);
				forceFieldstoAdd.clear();
			}

			void removePending() {
				forceFields.removeIf(ff -> ff.dead);
			}

			void addForceField(ForceField ff) {
				forceFieldstoAdd.add(ff);
				ff.dead = false;
			}

			void removeForceField(ForceField ff) {
				ff.dead = true;
			}

			void clear() {
				forceFields.clear();
				forceFieldstoAdd.clear();
			}
		}
	}

	/** Game player. Survives game sessions. */
	public class Player implements Configurable {
		@IsConfig(editable = false) public final V<Integer> id = new V<>(null);
		@IsConfig public final V<String> name = new V<>("");
		@IsConfig public final V<Color> color = new V<>(Color.WHITE);
		@IsConfig public final V<KeyCode> keyFire = new V<>(KeyCode.W);
		@IsConfig public final V<KeyCode> keyThrust = new V<>(KeyCode.S);
		@IsConfig public final V<KeyCode> keyLeft = new V<>(KeyCode.A);
		@IsConfig public final V<KeyCode> keyRight = new V<>(KeyCode.D);
		@IsConfig public final V<KeyCode> keyAbility = new V<>(KeyCode.Q);
		@IsConfig public final V<AbilityKind> ability_type = new V<>(AbilityKind.SHIELD);

		@IsConfig final VarEnum<Integer> gamepadId = new VarEnum<Integer>(null, gamepadIds);
//		@IsConfig(editable = false) final V<Integer> gamepadId = new V<>(null);
		boolean isInputLeft = false, isInputRight = false, isInputFire = false, isInputFireOnce = false, isInputThrust = false, isInputAbility = false;
		boolean wasGamepadLeft = false, wasGamepadRight = false, wasGamepadFire = false;
		public boolean alive = false;
		public final V<Integer> lives = new V<>(PLAYER_LIVES_INITIAL);
		public final V<Integer> score = new V<>(0);
		public final V<Double> energy = new V<>(0d);
		public Rocket rocket;
		public final StatsPlayer stats = new StatsPlayer();

		public Player(int ID, Color COLOR, KeyCode kfire, KeyCode kthrust, KeyCode kleft, KeyCode kright, KeyCode kability, AbilityKind ABILITY) {
			id.set(ID);
			name.set("Player " + ID);
			color.set(COLOR);
			ability_type.set(ABILITY);
			keyFire.set(kfire);
			keyThrust.set(kthrust);
			keyLeft.set(kleft);
			keyRight.set(kright);
			keyAbility.set(kability);
			ability_type.onChange(v -> {
				if (rocket!=null) rocket.changeAbility(v);
			});
			score.onChange((os,ns) -> {
				if (os/PLAYER_SCORE_NEW_LIFE<ns/PLAYER_SCORE_NEW_LIFE) lives.setValueOf(l -> l+1);
			});
			every200ms.add(() -> { if (rocket!=null) energy.set(rocket.energy); });
		}

		void die() {
			if (!alive) return;
			game.stats.playerDied(this);
			alive = false;
			rocket.dead = true;
			if (lives.getValue()>0) {
				stats.accDeath(game.loop.id);
				game.grid.applyExplosiveForce(100, new Vec(rocket.x,rocket.y), 50);
				spawnMidGame();
			} else {
				if (game.players.stream().noneMatch(p -> p.alive))
					game.over();
			}
		}

		void spawn() {
			if (alive) return;
			stats.accSpawn(game.loop.id);
			alive = true;
			lives.setValueOf(lives -> lives-1);
			rocket = new Rocket(this);
			rocket.dead = false;
			rocket.x = spawning.get().computeStartingX(game.field.width,game.field.height,game.players.size(),id.get());
			rocket.y = spawning.get().computeStartingY(game.field.width,game.field.height,game.players.size(),id.get());
			rocket.dx = 0;
			rocket.dy = 0;
			rocket.direction = spawning.get().computeStartingAngle(game.players.size(),id.get());
			rocket.energy = PLAYER_ENERGY_INITIAL;
			rocket.engine.enabled = false; // cant use engine.off() as it could produce unwanted behavior
			new Enhancer("Super shield", FontAwesomeIcon.SUN_ALT, seconds(5), r -> r.kinetic_shield.large.inc().inc(), r -> r.kinetic_shield.large.dec().dec(), "").enhance(rocket);
			createHyperSpaceAnimIn(game, rocket);
		}

		void spawnMidGame() {
			game.runNext.add(PLAYER_RESPAWN_TIME.divide(2), () -> new SuperDisruptor(
				spawning.get().computeStartingX(game.field.width,game.field.height,game.players.size(),id.get()),
				spawning.get().computeStartingY(game.field.width,game.field.height,game.players.size(),id.get())
			));
			game.runNext.add(PLAYER_RESPAWN_TIME, this::spawn);
		}

		void reset() {
			alive = false;
			score.setValue(0);
			lives.setValue(PLAYER_LIVES_INITIAL);
			if (rocket!=null) rocket.dead = true; // just in case
			rocket = null;
		}

		void doInputs() {
			if (alive) {
				long now = game.loop.now;
				if (isInputLeft) rocket.direction -= computeRotSpeed(now-game.keyPressTimes.getOrDefault(keyLeft.get(), 0L));
				if (isInputRight) rocket.direction += computeRotSpeed(now-game.keyPressTimes.getOrDefault(keyRight.get(), 0L));
				if (isInputThrust) rocket.engine.on(); else rocket.engine.off();
				if (isInputLeft && isInputRight) rocket.ddirection = 0;
				if (isInputFireOnce || (game.loop.isNth(3) && rocket.rapidFire.is() && isInputFire)) rocket.gun.fire();
				if (isInputAbility) rocket.ability_main.activate(); else rocket.ability_main.passivate();
			}
			isInputLeft = isInputRight = isInputFire = isInputFireOnce = isInputThrust = isInputAbility = false;
		}

		double computeRotSpeed(long pressedMsAgo) {
			// Shooting at long distance becomes hard due to 'smallest rotation angle' being too big so
			// we slow down rotation within the first ROT_LIMIT ms after key press and reduce rotation
			// limit temporarily without decreasing maneuverability.
			// The rotation decrease is nonlinear and continuous
//			double r = pressedMsAgo<ROT_LIMIT ? ROTATION_SPEED/((ROT_LIMIT/ROT_DEL+1)-pressedMsAgo/ROT_DEL) : ROTATION_SPEED;
			double r = pressedMsAgo<ROT_LIMIT ? ROTATION_SPEED*clip(0.1,pow(pressedMsAgo/ROT_LIMIT,2),1) : ROTATION_SPEED;
			return rocket.engine.mobility.value()*r;
		}

	}

	/** Loop object - object with per loop behavior. Executes once per loop. */
	private interface LO {
		void doLoop();
		default void dispose() {}
	}
	private abstract class SO implements LO {
		double x = 0;
		double y = 0;
		double dx = 0;
		double dy = 0;
		boolean isin_hyperspace = false; // other dimension, implies near-zero interactivity
		boolean dead = false; // flags signaling disposal on next cycle
		double g_potential = 1; // 1-gravitational potential, 0-1, 1 == no gravity, 0 == event horizon
		double radius = 0;

		void move() {}
		void draw() {}
		public void doLoop() {
			move();
			x += dx;
			y += dy;
			doLoopOutOfField();
			draw();
		}
		void doLoopOutOfField() {
			x = game.field.modX(x);
			y = game.field.modY(y);
		}
		double distance(PO o) {
			return game.field.dist(this.x,this.y,o.x,o.y);
		}
		boolean isDistanceLess(SO o, double dist) {
			return game.field.isDistLess(this.x,this.y,o.x,o.y, dist);
		}
		boolean isHitDistance(SO o) {
			return isDistanceLess(o,radius+o.radius);
		}
		/** direction angle between objects. In radians. */
		double dir(SO to) {
			return dir(to.x, to.y);
		}
		double dir(double tox, double toy) {
			double tx = game.field.distXSigned(x,tox);
			double ty = game.field.distYSigned(y,toy);
			return (tx<0 ? 0 : PI) + atan(ty/tx);
			// ???
//			double a = atan2(tx,-ty);
//			return a<0 ? a+D360 : a;
		}
		double speed() {
			return sqrt(dx*dx+dy*dy);
		}
		double speedTo(PO o) {
			double sx = dx-o.dx;
			double sy = dy-o.dy;
			return sqrt(sx*sx+sy*sy);
		}

		void die() {
			die(null);
		}

		void die(Object cause) {
			dead = true;
		}
	}
	/** Object with physical properties. */
	abstract class PO extends SO {
		double mass = 0;
		Engine engine = null;
		Class type;
		Image graphics;
		double graphicsDir = 0;
		double graphicsScale = 1;
		Set<LO> children = null;
		double direction = -D90;
		double ddirection = 0;

		PO(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Image GRAPHICS) {
			type = TYPE;
			x = X; y = Y; dx = DX; dy = DY;
			radius = HIT_RADIUS;
			mass = 2*HIT_RADIUS*HIT_RADIUS; // 4/3d*PI*HIT_RADIUS*HIT_RADIUS*HIT_RADIUS;
			graphics = GRAPHICS;
			init();
		}

		@Override public void doLoop(){
			doLoopBegin();
			super.doLoop();
			doLoopEnd();
			if (children!=null) children.forEach(LO::doLoop);
		}

		void doLoopBegin(){}

		void doLoopEnd(){}

		@Override void move() {
			dx *= RESISTANCE;
			dy *= RESISTANCE;
			ddirection *= RESISTANCE;
			direction += ddirection;
		}

		@Override void draw() {
			if (graphics!=null) {
				double scale = graphicsScale*(clip(0.7,20*g_potential,1));
				drawImageRotatedScaled(gc, graphics, deg(graphicsDir), x, y, scale);
			}
		}

		void init() {
			game.os.add(this);
			game.oss.add(this);
		}

		@Override public void dispose() {
			if (children!=null) list(children).forEach(LO::dispose);
		}

		double kineticE() {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
			return mass;// * (dx*dy);
		}

		@SuppressWarnings("unused")
		double kineticEto(SO o) {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
			return mass;// * (dx*dy);
		}


		class Engine {
			boolean enabled = false;
			final InEffectValue<Double> mobility = new InEffectValue<>(times -> pow(BONUS_MOBILITY_MULTIPLIER,times));

			final void on() {
				if (!enabled) {
					enabled = true;
					onOn();
				}
			}

			final void off() {
				if (enabled) {
					enabled = false;
					onOff();
				}
			}
			void onOn(){}

			void onOff(){}

			final void doLoop(){
				if (enabled) {
					onDoLoop();
				}
			}

			void onDoLoop(){}
		}
	}
	/** Object with engine, gun and other space ship characteristics. */
	abstract class Ship extends PO {
		double energy = 0;
		double energy_buildup_rate;
		double energy_max = 10000;
		Gun gun = null;
		Ability ability_main;
		KineticShield kinetic_shield = null;

		// to avoid multiple calculations/loop
		double cache_speed; // sqrt(dx^2 + dy^2)
		double cosdir = 0; // cos(dir)
		double sindir = 0; // sin(dir)
		double dx_old = 0; // allows calculating ddx (2nd derivation - acceleration)
		double dy_old = 0;

		public Ship(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Image GRAPHICS, double E, double dE) {
			super(TYPE, X, Y, DX, DY, HIT_RADIUS, GRAPHICS);
			energy = E;
			energy_buildup_rate = dE;
			children = new HashSet<>(10);
		}

		@Override void doLoopBegin() {
			cache_speed = speed();
			cosdir = cos(direction);
			sindir = sin(direction);
			dx_old = dx;
			dy_old = dy;

			// generate energy
			// moving (engine on) increases energy accumulation
			double dE = engine!=null && engine.enabled ? 1.25*energy_buildup_rate : energy_buildup_rate;
			energy = min(energy+dE,energy_max);
			if (engine!=null) engine.doLoop();
		}

		class Gun {
			final GunControl control;
			final Ƒ0<Double> aimer; // determines bullet direction
			final Ƒ1<Double,Bullet> ammo_type; // bullet factory
			final InEffectValue<Double[]> turrets = new InEffectValue<>(1, Utils::calculateGunTurretAngles);
			final Duration time_reload;
			final InEffect blackhole = new InEffect();
			double fireTTL; // frames till next fire

			public Gun(GunControl CONTROL, Duration TIME_RELOAD, Ƒ0<Double> AIMER, Ƒ1<Double,Bullet> AMMO_TYPE) {
				control = CONTROL;
				time_reload = TIME_RELOAD;
				aimer = AIMER;
				ammo_type = AMMO_TYPE;
				fireTTL = ttl(time_reload);
			}

			void fire() {
				if (!isin_hyperspace) {
					if (Ship.this instanceof Rocket)
						((Rocket) Ship.this).player.stats.accFiredBullet(game.loop.id);

					// for each turret, fire
					game.runNext.add(() -> {
						if (blackhole.is()) {
							blackhole.dec();
							Bullet b = ammo_type.apply(aimer.apply());
								   b.isBlackHole = true;
						} else {
							for (Double fire_angle : turrets.value()) {
								Bullet b = ammo_type.apply(aimer.apply()+fire_angle);
									   b.isHighEnergy = Ship.this instanceof Rocket && ((Rocket)Ship.this).energyFire.is();
							}
						}
					});
				}
			}
		}

		class RocketEngine extends Engine {
			double ttl = 0;
			double thrust = ROCKET_ENGINE_THRUST;
			final double particle_speed = 1/1/FPS;

			@Override
			void onDoLoop() {
				dx += cos(direction)*mobility.value()*thrust;
				dy += sin(direction)*mobility.value()*thrust;

				if (!isin_hyperspace) {
					ttl--;
					gc_bgr.setFill(game.humans.color);

					// style 1
					drawOval(gc_bgr,x,y,3);

					if (ttl<0) {
						ttl = ttl(millis(90));

						// style 2
//						ROCKET_ENGINE_DEBRIS_EMITTER.emit(x,y,direction+PI, mobility.value());

						// style3
						double s = speed();
						new Particle(x+25*cos(direction+PI),y+25*sin(direction+PI),cos(dx-dx/s),sin(dy-dy/s),ttl(millis(250))) {
							@Override
							void draw() {
								gc.save();
								gc.setStroke(game.humans.color);
								gc.setLineWidth(1+3*ttl);
								gc.setGlobalAlpha(0.1+0.5*ttl);
								double w = 10+30*(1-ttl), h=10+20*(1-ttl);
								Affine a = Utils.rotate(gc, deg(Ship.this.direction+PI/2),x,y);
								gc.strokeOval(x-w/2,y-h/2,w,h);
								gc.setTransform(a);
								gc.restore();
							}
						};
					}
				}
			}
		}
		class PulseEngine extends Engine {
			private double pulseTTL = 0;
			private double shipDistance = 9;

			@Override
			void onOn() { pulseTTL = 0; }
			@Override
			void onOff() {}
			@Override
			void onDoLoop() {
				pulseTTL--;
				if (pulseTTL<0) {
					pulseTTL = PULSE_ENGINE_PULSE_PERIOD_TTL;
					pulse();
				}
			}
			void pulse() {
				game.entities.addForceField(new PulseEngineForceField());
			}

			class PulseEngineForceField extends ForceField {
				final double mobility_multiplier_effect = mobility.value();
				private double ttl = 1;
				private boolean debris_done = false; // prevents spawning debris multiple times

				PulseEngineForceField() {
					double d = Ship.this.direction+PI;
					x = Ship.this.x + shipDistance*cos(d);
					y = Ship.this.y + shipDistance*sin(d);
					isin_hyperspace = Ship.this.isin_hyperspace;
				}

				public void doLoop() {
					ttl -= PULSE_ENGINE_PULSE_TTL1;
					if (ttl<0) dead = true;

					if (!debris_done && ttl<0.7) {
						double m = mobility.value();
						debris_done = true;
						if (!isin_hyperspace) {
							game.runNext.add(() -> {
								new PulseEngineDebris(x-2,y+2,-.5d, .5d,m);
								new PulseEngineDebris(x+2,y+2, .5d, .5d,m);
								new PulseEngineDebris(x+2,y-2, .5d,-.5d,m);
								new PulseEngineDebris(x-2,y-2,-.5d,-.5d,m);
							});
						}
					}
				}

				void apply(PO o) {
					if (!(o instanceof Rocket)) return;  // too much performance costs for no benefits
					if (isin_hyperspace!=o.isin_hyperspace) return;

					double distX = game.field.distXSigned(x,o.x);
					double distY = game.field.distYSigned(y,o.y);
					double dist = game.field.dist(distX,distY)+1; // +1 avoids /0 " + dist);
					double f = force(o.mass,dist);

					// apply force
					o.dx += distX*f/dist;
					o.dy += distY*f/dist;
				}

				public double force(double mass, double dist) {
					return dist==0 ? 0 : -255*mobility_multiplier_effect*ttl*ttl/(dist*dist*dist);
				}
			}
		}

		class Ability implements LO {
			AbilityState state = OFF;
			double activation = 0; // 0==passive, 1 == active, 0-1 == transition state
			final double timeActivation; // seconds it takes to activate, 0 = instantaneous
			final double timePassivation; // seconds it takes to passivate, 0 = instantaneous// seconds it takes to passivate, 0 = instantaneous
			final double e_act; // e needed to activate ability
			final double e_rate; // e/frame consumption when activated
			final boolean onHold; // true = press-release; false = press-press

			public Ability(boolean ONHOLD, Duration timeToAct, Duration timeToPass, double E_ACT, double E_RATE) {
				timeActivation = timeToAct.toSeconds();
				timePassivation = timeToPass.toSeconds();
				e_act = E_ACT;
				e_rate = E_RATE;
				onHold = ONHOLD;
				init();
			}

			void init(){}

			public void dispose(){
				passivate(); // forcefully deactivate
				onPassivateStart(); // forcefully deactivate
				onPassivateEnd(); // forcefully deactivate
				if (ability_main==this) ability_main = null;
				children.remove(this);
			}

			void onKeyPress(){
				if (onHold) {
					activate();
				} else {
					if (state==ACTIVATING || activation==1) passivate(); else activate();
				}
			}
			void onKeyRelease(){
				if (onHold) {
					passivate();
				}
			}
			void activate() {
				if (state==OFF) {
					double min_energy_required = e_act+5*e_rate;
					if (energy >= min_energy_required) {
						energy -= min(energy,e_act);
						state=ACTIVATING;
						onActivateStart();
					}
				}
			}
			void passivate() {
				if (state==ON) {
					state=PASSSIVATING;
					onPassivateStart();
				}
			}
			void onActivateStart(){}

			void onActivateEnd(){}

			void onActive(){}

			void onPassivateStart(){}

			void onPassivateEnd(){}

			void onPassive(){}

			void onActiveChanged(double activation){}

			boolean isActivated() { return activation==1; }
			public void doLoop() {
				if (state==ACTIVATING) {
					activation = timeActivation==0 ? 1 : min(1,activation+1/(timeActivation*FPS));
					if (activation==1) {
						state = ON;
						onActivateEnd();
					}
					onActiveChanged(activation);
				} else
				if (state==PASSSIVATING) {
					activation = timePassivation==0 ? 0 : max(0,activation-1/(timePassivation*FPS));
					if (activation==0) {
						state = OFF;
						onPassivateEnd();
					}
					onActiveChanged(activation);
				} else if (activation==0) onPassive();
				else if (activation==1) onActive();

				if (activation==1) energy -= min(energy,e_rate);
				if (energy==0) passivate();

			}
		}
		class AbilityWithSceneGraphics extends Ability {
			final Image graphicsA;
			double graphicsADir = 0;
			double graphicsAScale = 1;

			public AbilityWithSceneGraphics(boolean ONHOLD, Duration timeToAct, Duration timeToPass, double E_ACT, double E_RATE, Image GRAPHICS) {
				super(ONHOLD, timeToAct, timeToPass, E_ACT, E_RATE);
				graphicsA = GRAPHICS;
			}
			public void doLoop() {
				super.doLoop();

				if (graphicsA!=null) {
					drawImageRotatedScaled(gc, graphicsA, deg(graphicsADir), x, y, graphicsAScale);
				}
			}
		}
		class Disruptor extends Ability {
			final ForceField field = new DisruptorField();

			Disruptor() {
				super(true, DISRUPTOR_ACTIVATION_TIME,DISRUPTOR_PASSIVATION_TIME, DISRUPTOR_E_ACTIVATION,DISRUPTOR_E_RATE );
			}

			void onActivateStart() {
				game.entities.addForceField(field);
			}
			void onPassivateEnd() {
				game.entities.removeForceField(field);
			}

			class DisruptorField extends ForceField {
				public double force(double mass, double dist) {
					double d = max(dist,30);
					return dist==0 ? 0 : -260/d/d;
				}

				@Override
				void apply(PO o) {
					if (o==Ship.this) return; // must not affect itself
					if (isin_hyperspace || o.isin_hyperspace) return;

					double distX = game.field.distXSigned(x,o.x);
					double distY = game.field.distYSigned(y,o.y);
					double dist = game.field.dist(distX,distY)+1; // +1 avoids /0 " + dist);
					double f = force(o.mass,dist);
					boolean noeffect = false;

					// disrupt ufo bullets
					if (o instanceof Particle) {
					} else
					if (o instanceof Bullet && ((Bullet)o).owner instanceof Ufo) {
						double strength = dist>500 ? 0 : 1-dist/500;
						double deceleration = (1-0.05*strength); // strength==1 ? 0.95 : 1
						o.dx *= deceleration;
						o.dy *= deceleration;
					} else
					// disrupt ufos
					if (o instanceof Ufo) {
						double strength = dist>400 ? 0 : 1-dist/400;
						double deceleration = (1-0.1*strength); // strength==1 ? 0.9 : 1
						o.dx *= deceleration;
						o.dy *= deceleration;
					} else
					// shield pulls disruptor
					// Makes disruptor vs shield battles more interesting
					if (o instanceof Rocket && ((Rocket)o).ability_main instanceof Shield && ((Rocket)o).ability_main.isActivated()) {
						f *= -1;
					} else
					if (o instanceof Shuttle || o instanceof SuperShield || o instanceof SuperDisruptor) {
						noeffect = true;
					}

					// apply force
					if (noeffect) return;
					o.dx += distX*f/dist;
					o.dy += distY*f/dist;
				}

				@Override
				public void doLoop() {
					this.x = Ship.this.x;
					this.y = Ship.this.y;
					this.isin_hyperspace = Ship.this.isin_hyperspace; // must always match

					if (activation==1 && !isin_hyperspace) {
						// radiate particles
						createRandomDisruptorParticle(30,60,this);
						// warp grid
						double strength = 17 - 2*cache_speed;
						game.grid.applyExplosiveForce(10*strength, new Vec(x,y), 60);
					}
				}
			}
		}
		class Hyperspace extends AbilityWithSceneGraphics {
			Hyperspace() {
				super(
					false, HYPERSPACE_ACTIVATION_TIME,HYPERSPACE_PASSIVATION_TIME,HYPERSPACE_E_ACTIVATION,HYPERSPACE_E_RATE,
					graphics(MaterialDesignIcon.PLUS,30,game.humans.color,null)
				);
				graphicsAScale = 0;
			}

			void onActivateStart() {
				isin_hyperspace = true;
				game.grid.applyExplosiveForce(5*90f, new Vec(x,y), 70);
			}
			void onPassivateStart() {
				game.grid.applyExplosiveForce(5*90f, new Vec(x,y), 70);
			}
			void onPassivateEnd() {
				isin_hyperspace = false;
			}
			void onActiveChanged(double activation) {
				graphicsScale = 1-activation;
				graphicsAScale = activation;
			}
			public void doLoop() {
				super.doLoop();

				if (isActivated()) {
					double strength = 6 + cache_speed/4;
					game.grid.applyImplosiveForce(strength, new Vec(x,y), 30);
				}
			}
		}
		class Shield extends Ability {

			Shield() {
				super(true, SHIELD_ACTIVATION_TIME,SHIELD_PASSIVATION_TIME,SHIELD_E_ACTIVATION,SHIELD_E_RATE);
			}

			void onActivateStart() {
				kinetic_shield.large.inc();
			}
			void onPassivateStart() {
				kinetic_shield.large.dec();
			}
			public void doLoop() {
				super.doLoop();
			}
			@SuppressWarnings("unused")
			void onHit(Asteroid a) {
				// makes shield hardly usable, instead we drain energy constantly while active
				// energy -= min(energy,kineticEto(a));
			}

		}
		class KineticShield extends Ability {
			double KSenergy_maxInit;
			double KSenergy_max;
			double KSenergy;
			double KSenergy_rateInit;
			double KSenergy_rate;
			double KSradiusInit;
			double KSradius;
			int pieces; // needs update when KSradius changes
			double piece_angle; // same
			final Runnable activationRun = this::showActivation;
			final LO ksemitter = null; // = new ShieldPulseEmitter(); // emits radial waves
			final boolean draw_ring;

			// large KS ability
			final InEffect large = new InEffect(times -> {
				KSenergy_rate = (times>0 ? KINETIC_SHIELD_LARGE_E_RATE : 1)*KSenergy_rateInit;
				KSradius = KSradiusInit + KINETIC_SHIELD_LARGE_RADIUS_INC*times;
				KSenergy_max = KSenergy_maxInit*(1 + KINETIC_SHIELD_LARGE_E_MAX_INC*times);
				postRadiusChange();
			});
			double largeTTL = 1;
			double largeTTLd;
			double largeLastPiece = 0;

			// syncs = KS graphics, the bars forming a circle,  we can draw a shape/pattern into it
			int syncs_len = 1000;
			double[] syncs = new double[syncs_len];
			int sync_index = 0;

			public KineticShield(double RADIUS, double ENERGY_MAX) {
				super(true, Duration.ZERO,Duration.ZERO,0,0);
				KSenergy_maxInit = ENERGY_MAX;
				KSenergy_max = KSenergy_maxInit;
				KSenergy_rateInit = KSenergy_max/ttl(KINETIC_SHIELD_RECHARGE_TIME);
				KSenergy_rate = KSenergy_rateInit;
				KSenergy = KINETIC_SHIELD_INITIAL_ENERGY*KSenergy_max;
				KSradiusInit = RADIUS;
				KSradius = KSradiusInit;
				draw_ring = Ship.this instanceof Shuttle;
				postRadiusChange();
				children.add(this);
				scheduleActivation();

				double sync_reps = Ship.this instanceof Rocket ? ((Rocket)Ship.this).player.id.get() :
								   Ship.this instanceof Shuttle ? ((Shuttle)Ship.this).owner.player.id.get() :
								   1+randInt(5);
				double syncs_range = sync_reps*D360;
				double syncs_range_d = syncs_range/syncs_len;
				for (int i=0; i<syncs_len; i++)
					syncs[i] = (1+sin(i*syncs_range_d))/2;
			}

			@Override void init() {}    // no init
			@Override public void dispose() {} // no dispose
			@Override public void doLoop() {
				KSenergy = min(KSenergy_max,KSenergy+KSenergy_rate);
				if (ksemitter!=null) ksemitter.doLoop();

				if (large.is()) {
					largeTTL -= 0.3;
					if (largeTTL<0) {
						largeTTL = 1;
						largeLastPiece = largeLastPiece%pieces;
						game.runNext.add(() -> new KineticShieldPiece(direction+largeLastPiece*piece_angle).max_opacity = 0.4);
						largeLastPiece++;
					}
				}

				if (KSenergy!=KSenergy_max) sync_index++; // rotate only when loading
				double sync_gap = 1;
				int syncs_amount = (int) (2*KSradius/sync_gap);
				double syncs_angle = D360/syncs_amount;
				double r = KSradius;
				int sync_index_real = sync_index * 50/((int)r-20); // adjusts speed (slow down)
				double energy_fullness = KSenergy/KSenergy_max;
				// 1% margin enables 'always full' shields by using high energy
				// accummulation rate. The short lived shield damagee will thus be transparent.
				boolean energy_full = energy_fullness>0.99;
				gc.setGlobalAlpha(energy_fullness);
				gc.setStroke(COLOR_DB);
				gc.setLineWidth(2);
				for (int i=0; i<syncs_amount; i++) {
					double angle = direction+i*syncs_angle;
					double acos = cos(angle);
					double asin = sin(angle);
					double alen = 0.3*r*syncs[Math.floorMod(i*syncs_len/syncs_amount+sync_index_real,syncs_len)];
					gc.strokeLine(x+r*acos,y+r*asin,x+(r-alen)*acos,y+(r-alen)*asin);
					// draw a 'ring' to signal full shield to the player
					// note drawing opacity will always be approximately 1 here
					if (energy_full) gc.strokeLine(x+r*acos,y+r*asin,x+(r-1)*acos,y+(r-1)*asin);
				}
				gc.setGlobalAlpha(1);

			}
			void onShieldHit(PO o) {
				KSenergy = max(0,KSenergy-kineticEto(o));
				scheduleActivation();

				double d = dir(o);
				new KineticShieldPiece(d+piece_angle);
				new KineticShieldPiece(d);
				new KineticShieldPiece(d-piece_angle);
			}
			void showActivation() {
				repeat(pieces, i -> new KineticShieldPiece(true,i*piece_angle));
				repeat(pieces, i -> new KineticShieldPiece(true,i*piece_angle));
			}
			protected void scheduleActivation() {
				if (KSenergy<KSenergy_max) {
					game.runNext.remove(activationRun);
					game.runNext.add((KSenergy_max-KSenergy)/KSenergy_rate, activationRun);
				}
			}
			void changeKSenergyBy(double e){
				KSenergy = clip(0,KSenergy+e,KSenergy_max);
			}
			void changeKSenergyToMax(){
				if (KSenergy<KSenergy_max) {
					KSenergy = KSenergy_max;
					showActivation();
				}
			}
			private void postRadiusChange() {
				pieces = ((int)(D360*KSradius))/11;
				piece_angle = D360/pieces;
				largeTTLd = 1/FPS/seconds(1/60/pieces).toSeconds();
			}

			/** Not an ability, simply a graphics for an ability. Extends Ability to avoid code duplicity. */
			class KineticShieldPiece extends Ability {
				final double dirOffset;
				double delay_ttl;
				double ttl = 1;
				double ttld = 1/ ttl(seconds(1));
				double max_opacity = 1;

				KineticShieldPiece(double DIR) {
					this(false,DIR);
				}
				KineticShieldPiece(boolean delayed, double DIR) {
					super(true, Duration.ZERO,Duration.ZERO,0,0);
					delay_ttl = ttl(seconds(delayed ? 0.2 : 1));
					dirOffset = DIR-direction;
					children.add(this);
				}

				public void doLoop() {
					super.doLoop();
					double KSPdir = direction+dirOffset;
					double KSPx = cos(KSPdir)*KSradius;
					double KSPy = sin(KSPdir)*KSradius;


					gc.setGlobalAlpha(max_opacity*ttl*ttl);
					// gc.setGlobalBlendMode(ADD);
					drawImageRotated(gc, KINETIC_SHIELD_PIECE_GRAPHICS, deg(D90+KSPdir), KSPx+x, KSPy+y);
					gc.setGlobalAlpha(1);
//                    gc.setGlobalBlendMode(SRC_OVER);

					delay_ttl--;
					if (delay_ttl<0) {
						ttl -= ttld;
						if (ttl<0) game.runNext.add(this::dispose);
					}
				}
				public void dispose() {
					passivate(); // forcefully deactivate
					onPassivateStart(); // forcefully deactivate
					onPassivateEnd(); // forcefully deactivate
					if (ability_main==this) ability_main = null;
					children.remove(this);
				}
			}
			/** Emits shield pulses. */
			class ShieldPulseEmitter implements LO {
				double ttl = 0;

				public void doLoop() {
					ttl--;
					if (ttl<0) {
						ttl = ttl(seconds(1+kinetic_shield.KSradius/100*0.7));
						ShieldPulse p = new ShieldPulse(Ship.this,x,y);
									p.dxy = 0.4;
									p.ttld = 1/(1.3*KSradius/0.4);
					}
				}
			}
		}
		class LaserSight extends Ability {

			LaserSight() {
				super(false,seconds(0),seconds(0),0,0);
				children.add(this);
			}

			@Override
			public void doLoop() {
				Rocket r = (Rocket)Ship.this;
				drawHudLine(x,y, 40, r.bulletRange, cosdir, sindir, HUD_COLOR);
				// drawHudCircle(x,y,r.bulletRange, HUD_COLOR); // nah drawing ranges is more cool
				drawHudCircle(x,y,r.bulletRange,r.direction,D30, HUD_COLOR);
				drawHudCircle(x,y,r.bulletRange,r.direction+D360/3,PI/8, HUD_COLOR);
				drawHudCircle(x,y,r.bulletRange,r.direction-D360/3,PI/8, HUD_COLOR);
			}
		}
		class Range extends Ability {

			Range() {
				super(false,seconds(0),seconds(0),0,0);
				children.add(this);
			}

			public void doLoop() {
				gc.setFill(Color.AQUA);
				gc.setGlobalAlpha(0.4);
				for (int i=20; i<500; i+=5)
					gc.fillOval(game.field.modX(x+i*cosdir), game.field.modY(y+i*sindir), 2,2);
				gc.setGlobalAlpha(1);
			}
		}
	}

	/** Default player ship. */
	class Rocket extends Ship {

		final Player player;
		final InEffect rapidFire = new InEffect();
		final InEffectValue<Double> powerFire = new InEffectValue<>(0, times -> 1+0.5*times, times -> computeBulletRange());
		final InEffect energyFire = new InEffect();
		final InEffect splitFire = new InEffect();
		double bulletRange = computeBulletRange();
		final double cacheRandomVoronoiTranslation = randOf(-1,1)*randMN(0.01,0.012);

		Rocket(Player PLAYER) {
			super(
				Rocket.class,
				game.field.width/2,game.field.height/2,0,0,PLAYER_HIT_RADIUS,
				graphics(MaterialDesignIcon.ROCKET,34,PLAYER.color.get(),null),
				// graphics(FontAwesomeIcon.ROCKET,40,PLAYER.color.get(),null), alternative
				PLAYER_ENERGY_INITIAL,PLAYER_E_BUILDUP
			);
			player = PLAYER;
			kinetic_shield = new KineticShield(ROCKET_KINETIC_SHIELD_RADIUS,ROCKET_KINETIC_SHIELD_ENERGYMAX);
			changeAbility(player.ability_type.get());
			engine = rand01()<0.5 ? new RocketEngine() : new PulseEngine();

			gun = new Gun(
				MANUAL,
				PLAYER_GUN_RELOAD_TIME,
				() -> direction,
				dir -> splitFire.is()
					? new SplitBullet(
							this,
							x + PLAYER_BULLET_OFFSET*cos(dir),
							y + PLAYER_BULLET_OFFSET*sin(dir),
							dx + powerFire.value()*cos(dir)*PLAYER_BULLET_SPEED,
							dy + powerFire.value()*sin(dir)*PLAYER_BULLET_SPEED,
							0,
							PLAYER_BULLET_TTL
						)
					: new Bullet(
							this,
							x + PLAYER_BULLET_OFFSET*cos(dir),
							y + PLAYER_BULLET_OFFSET*sin(dir),
							dx + powerFire.value()*cos(dir)*PLAYER_BULLET_SPEED,
							dy + powerFire.value()*sin(dir)*PLAYER_BULLET_SPEED,
							0,
							PLAYER_BULLET_TTL
						)
			);
		}

		@Override
		void doLoopBegin() {
			super.doLoopBegin();
			player.stats.accTravel(cache_speed);
		}

		@Override
		void draw() {
			graphicsDir = D45 + direction; // add 45 degrees due to the graphics TODO: fix this

			// Draw custom graphics
//			super.draw();
			double scale = graphicsScale*(clip(0.7,20*g_potential,1));
//				gc.setFill(game.humans.color);
			gc.setFill(player.color.get());
			drawTriangle(gc, x,y,scale*15, direction, 3*PI/4);
			drawOval(gc_bgr,x-5*cos(direction),y-5*sin(direction),8);

			if (game.humans.intelOn.is() && bulletRange<game.field.diagonal) {
				drawHudCircle(x,y,bulletRange,HUD_COLOR);
			}

			if (gun.blackhole.is()) {
				gc.setFill(Color.BLACK);
				drawHudCircle(game.field.modX(x+bulletRange*cos(direction)),game.field.modY(y+bulletRange*sin(direction)), 50, HUD_COLOR);
			}

			// rocket-rocket 'quark entanglement' formation force
			// Repells at short distance, pulls at long distance.
			//
			// Nice idea, but the force requires some tuning. It needs to be fairly strong to shape
			// the formation properly and player movement can be disrupted or itself disruptive.
//             double mid = 150;
//             game.oss.get(Rocket.class).forEach(r -> {
//                 if (this==r) return;
//                 double d = dir(r);
//                 double cd = cos(d);
//                 double sd = sin(d);
//                 double dist = distance(r);
//                 double f = dist<mid ? -8*pow((1-dist/mid),2) : 6*pow((dist-mid)/2000,2);
//	                    f *= 0.01;
//                 dx += f*cd;
//                 dy += f*sd;
//                 r.dx -= f*cd;
//                 r.dy -= f*sd;
//                 drawHudLine(x,y,20,dist-2*20,cd,sd,COLOR_DB);
//             });

			// todo: force from centre
//	        Set<Rocket> rs = game.oss.get(Rocket.class);
//	        if (rs.isEmpty()) return;
//	        Vec centre = new Vec(
//		        rs.stream().mapToDouble(r -> r.x).average().getAsDouble(),
//		        rs.stream().mapToDouble(r -> r.x).average().getAsDouble()
//	        );
		}
		void changeAbility(AbilityKind type) {
			if (ability_main!=null) ability_main.dispose();
			children.remove(ability_main);
			ability_main = type.create(this);
			children.add(ability_main);
		}

		@Override
		boolean isHitDistance(SO o) {
			if (o instanceof Bullet)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			if (o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			return isDistanceLess(o,radius+o.radius);
		}

		double computeBulletRange() {
			return bulletRange = powerFire.value()*PLAYER_BULLET_RANGE;
		}

		@Override
		void die(Object cause) {
			super.die(cause);
			game.grid.applyExplosiveForce(9000f, new Vec(x,y), 2000);
		}
	}
	/** Default enemy ship. */
	class Ufo extends Ship {
		boolean aggressive = false;
		private final Runnable radio = () -> game.ufos.pulseCall(this);
		private final Runnable tryDiscs = () -> {
			if (game.ufos.canSpawnDiscs) {
				game.ufos.canSpawnDiscs = false;
				double spawnX = x, spawnY = y;
				radio.run();
				game.runNext.add(millis(500), () -> repeat(5, i -> new UfoSwarmer(spawnX, spawnY, i*D360/5)));
			}
		};

		double discpos = 0; // 0-1, 0=close, 1=far
		double discdspeed = 0;
		double disc_forceJump(double pos) { return pos>=1 ? -2*discdspeed : 0.01; } // jump force
		double disc_forceBio(double pos) { return pos<0.5 ? 0.01 : -0.01; } // standard force
		double interUfoForce(Ufo u){
			double d = distance(u);
			double f = d<80 ? -pow(1-d/80,1) : d<160 ? 0.1 : 0;
			return 1*f;
		}

		Ufo(Side side, boolean AGGRESSIVE) {
			super(
				Ufo.class,
				(side==Side.RIGHT ? 1 : 0) * game.field.width,
				rand01()*game.field.height,0,0,UFO_HIT_RADIUS,
				graphics(MaterialDesignIcon.BIOHAZARD,40,game.ufos.color,null),
				UFO_ENERGY_INITIAL,UFO_E_BUILDUP
			);
			direction = x<game.field.width/2 ? 0 : PI; // left->right || left<-right
			aggressive = AGGRESSIVE;
			engine = new Engine() {
				double engineDirChangeTTL = 1;
				double engineDirChangeTTLd = 1/((2+rand01()*2)*FPS);

				@Override void onDoLoop() {
					engineDirChangeTTL -= engineDirChangeTTLd;
					if (engineDirChangeTTL<0) {
						engineDirChangeTTL = 1;
						// generate new direction
						double r = rand01();
						if (direction==0)            direction = r<0.5 ? D45 : -D45;
						else if (direction==D45)     direction = r<0.5 ? 0 : -D45;
						else if (direction==-D45)    direction = r<0.5 ? 0 : D45;
						else if (direction== PI)     direction = r<0.5 ? 3*D45 : 3*D45;
						else if (direction== 3*D45)  direction = r<0.5 ? PI : -3*D45;
						else if (direction==-3*D45)  direction = r<0.5 ? PI : 3*D45;
						// preserve speed
						// this causes movements changes to be abrupt (game is more dificult)
						double s = speed();
						dx = s*cos(direction);
						dy = s*sin(direction);
					}
					dx += cos(direction)*UFO_ENGINE_THRUST;
					dy += sin(direction)*UFO_ENGINE_THRUST;


					// shape formation using ufo-ufo force
//                    for (Ufo u : game.oss.get(Ufo.class)) {
//                        if (u==Ufo.this) continue;
//                        double f = interUfoForce(u);
//                        boolean toright = x<u.x;
//                        boolean tobottom = y<u.y;
//                        dx += (toright ? 1 : -1) * f;
//                        dy += (toright ? 1 : -1) * f;
//        //                u2.dx += (tobottom ? -1 : 1) * f;
//        //                u2.dy += (tobottom ? -1 : 1) * f;
//                    }
				}
			};
			engine.enabled = true;
			gun = new Gun(
				AUTO,
				UFO_GUN_RELOAD_TIME,
				() -> {
					if (!aggressive || game.ufos.ufo_enemy==null) return rand0N(D360);
					Rocket enemy = isDistanceLess(game.ufos.ufo_enemy, UFO_BULLET_RANGE)
						? game.ufos.ufo_enemy
						: findClosestRocketTo(this);
					return enemy==null ? rand0N(D360) : dir(enemy) + randMN(-D30,D30);
				},
				dir -> new UfoBullet(
					this,
					x + UFO_BULLET_OFFSET*cos(dir),
					y + UFO_BULLET_OFFSET*sin(dir),
					dx + cos(dir)*UFO_BULLET_SPEED,
					dy + sin(dir)*UFO_BULLET_SPEED
				)
			);
			game.runNext.addPeriodic(() -> ttl(seconds(5)), tryDiscs);
			tryDiscs.run();
		}

		@Override void doLoopOutOfField() {
			y = game.field.modY(y);
			if (game.field.isOutsideX(x)) dead = true;
		}

		@Override void draw() {
			super.draw();

			drawUfoRadar(x,y);

			// Use jump force to make the disc bounce smoothly only on inner side and bounce
			// instantly on the outer side. Standard force is more natural and 'biological', while
			// jump force looks more mechanical and alien.
			discdspeed += disc_forceJump(discpos);
			discpos += discdspeed;
			double dist = 40+discpos*20;
			double dir1 = -3*D30, dir2 = -7*D30, dir3 = -11*D30;
			drawUfoDisc(x+dist*cos(dir1),y+dist*sin(dir1), dir1, 1);
			drawUfoDisc(x+dist*cos(dir2),y+dist*sin(dir2), dir2, 1);
			drawUfoDisc(x+dist*cos(dir3),y+dist*sin(dir3), dir3, 1);

			if (game.humans.intelOn.is())
				drawHudCircle(x,y,UFO_BULLET_RANGE,game.ufos.color);
		}

		@Override public void dispose() {
			super.dispose();
			game.runNext.remove(radio);
			game.runNext.remove(tryDiscs);
		}
	}
	/** Ufo heavy projectiles. Autonomous rocket-seekers. */
	class UfoSwarmer extends Ship {
		Rocket enemy = null;
		/** True if actively looks for target to pursuit. */
		boolean isActive = true;
		/** Set to true if spawns outside of the field to prevent instant death. */
		boolean isInitialOutOfField = false;
		int swarmId = -1;
		double dRotationMax = ttlVal(D360,seconds(3));

		public UfoSwarmer(double X, double Y, double DIR) {
			super(UfoSwarmer.class, X, Y,0,0, UFO_DISC_HIT_RADIUS, null, UFO_ENERGY_INITIAL,UFO_E_BUILDUP);
			mass = 4;
			direction = DIR;
			engine = new Engine() {
				double acceleration = 0.13;
				{
					enabled = true;
				}
				@Override
				void onDoLoop() {
					dx += acceleration*cos(direction);
					dy += acceleration*sin(direction);
				}
			};
			createHyperSpaceAnimIn(game,this);
		}

		@Override void move() {
			// prevents overlap using repulsion
			for (UfoSwarmer u : game.oss.get(UfoSwarmer.class)) {
				boolean isInFormation = !u.isActive && swarmId==u.swarmId;
				if (u == this || isInFormation) continue;
				double f = interUfoDiscForce(u);
				boolean toRight = x < u.x;
				boolean toBottom = y < u.y;
				dx += (toRight ? 1 : -1) * f;
				dy += (toBottom ? 1 : -1) * f;
			}
			seek();
			dx *= 0.96;
			dy *= 0.96;
		}

		@Override void doLoopOutOfField() {
			if (isInitialOutOfField) {
				if (game.field.isInside(x, y))
					isInitialOutOfField = false;
			} else {
				if (!isActive) {
					if (game.field.isOutsideX(x))
						dead = true;
				} else
					super.doLoopOutOfField();
			}
		}

		private void seek() {
			// recompute target if actively seeing one
			// Note: Avoid unnecessary computation cheaply using n-th game loop strategy
			if (isActive) {
				if (game.loop.isNth(UFO_DISC_DECISION_TIME_TTL))
					enemy = findClosestRocketTo(this);
			}

			if (isActive) {
				boolean isPursuit = !(enemy==null || enemy.player.rocket != enemy || enemy.isin_hyperspace);
				if (isPursuit) {
					engine.on();

					// Seeking algorithm
					// 1) go directly towards enemy - works quite well
					//    Behavior: can change direction instantaneously
					//direction = dir(enemy);

					// 2) turn to enemy with max rotation speed - looks very natural
					//    Behavior: simulated turning with
					double dirTarget = dir(enemy);
					double dirDiff = dirDiff(direction,dirTarget);
					double dRotationAbs = min(dRotationMax,abs(dirDiff));
					direction += sign(dirDiff)*dRotationAbs;
				} else {
					// Behavior
					// 1) Do nothing - not bad, works pretty well
					// engine.off();

					// 2) Keep going straight - more natural than 1)
					engine.on();
					ddirection = dRotationMax;
					direction += 2*ddirection;
				}
			} else {
				engine.on();
			}
		}

		@Override void draw() {
			double makeBigger = 2;
			drawUfoDisc(x,y,direction, graphicsScale*makeBigger);
		}

		double interUfoDiscForce(UfoSwarmer ud) {
			double distMax = 16;
			double dist = distance(ud);
			return dist>distMax ? 0 : -0.5*pow((1-dist/distMax),2);
		}

		void explode() {
			dead = true;
			drawUfoDiscExplosion(x,y);
			for (UfoSwarmer ud : game.oss.get(UfoSwarmer.class)) {
				if (distance(ud)<=UFO_DISC_EXPLOSION_RADIUS)
					game.runNext.add(millis(100),ud::explode);
			}
		}
	}
	/** Non-interactive mission info button. */
	class MissionInfoButton extends PO {
		MissionInfoButton() {
			super(MissionInfoButton.class, 0, 0, 0, 0, 0, graphics(FontAwesomeIcon.INFO,15,game.humans.color,null));
			x = rand0N(game.field.width);
			y = rand0N(game.field.height);
			// graphics.setOnMouseClicked(e -> new MissionPane().show(game.mission));
			playfield.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if ((x-e.getX())*(x-e.getX())+(y-e.getY())*(y-e.getY())<=10*10) {
					new MissionPane().show(game.mission);
					e.consume();
				}
			});
		}
		@Override void init() {}
		@Override public void dispose() {}
		@Override public void doLoop() {
			super.doLoop();
			applyPlayerRepulseForce(this,400);
		}
	}

	private void drawUfoDisc(double x, double y, double dir, double scale) {
		gc.setFill(game.ufos.color);
		drawTriangle(gc, x,y,scale*UFO_DISC_RADIUS, dir, 3*PI/4);
	}
	private void drawUfoRadar(double x, double y) {
		gc.setGlobalAlpha(0.3);
		gc.setFill(new RadialGradient(0,0,0.5,0.5,1,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(1,Color.rgb(114,208,74))));
		gc.fillOval(x-UFO_RADAR_RADIUS,y-UFO_RADAR_RADIUS,2*UFO_RADAR_RADIUS,2*UFO_RADAR_RADIUS);
		gc.setGlobalAlpha(1);
	}
	private void drawUfoExplosion(double x, double y) {
		new FermiGraphics(x, y, UFO_EXPLOSION_RADIUS).color = game.ufos.color;
	}
	private void drawUfoDiscExplosion(double x, double y) {
		new FermiGraphics(x, y, UFO_DISC_EXPLOSION_RADIUS).color = game.ufos.color;
	}

	class Shuttle extends Ship {
		double ttl = ttl(seconds(50));
		final double graphicsDirBy = randOf(-1,1)*D360/ ttl(seconds(40));
		final Rocket owner;

		public Shuttle(Rocket r) {
			super(
				Shuttle.class, r.x+50,r.y-50,0,0,PLAYER_HIT_RADIUS,
				graphics(FontAwesomeIcon.SPACE_SHUTTLE,40,game.humans.colorTech,null), 0,0
			);
			graphicsDir = randOf(-1,1)*deg(D360/ ttl(seconds(20)));
			graphicsScale = 0;
			owner = r;
			createHyperSpaceAnimIn(game, this);
			game.runNext.add(3*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(4*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(5*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(6*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(7*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(8*ttl/10, () -> { if (!dead) new Satellite(this,rand0N(D360)); });
			game.runNext.add(ttl, () -> { if (!dead) createHyperSpaceAnimOut(game, this); });
			game.runNext.add(ttl + ttl(millis(200)), () -> dead=true);
		}
		@Override void draw() {
			graphicsDir += graphicsDirBy;
			super.draw();
		}
		@Override void die(Object cause) {
			super.die(cause);
			new FermiGraphics(x, y, UFO_EXPLOSION_RADIUS).color = game.humans.color;
		}
	}
	class SuperShield extends Ship {
		double ttl = ttl(seconds(50));
		final double graphicsDirBy = randOf(-1,1)*D360/ ttl(seconds(40));
		final Rocket owner;

		public SuperShield(Rocket r) {
			super(
				SuperShield.class, r.x+50,r.y-50,0,0,10,
				graphics(MaterialIcon.DETAILS,20,game.humans.colorTech,null), 0,0
			);
			graphicsDir = randOf(-1,1)*deg(D360/ ttl(seconds(20)));
			graphicsScale = 0;
			owner = r;
			kinetic_shield = new KineticShield(SHUTTLE_KINETIC_SHIELD_RADIUS,SHUTTLE_KINETIC_SHIELD_ENERGYMAX) {
				// disables effect
				@Override protected void scheduleActivation() {}
			};
			kinetic_shield.KSenergy = kinetic_shield.KSenergy_max;
			createHyperSpaceAnimIn(game, this);
			game.runNext.add(ttl, () -> { if (!dead) createHyperSpaceAnimOut(game, this); });
			game.runNext.add(ttl + ttl(millis(200)), () -> dead=true);
		}
		@Override boolean isHitDistance(SO o) {
			if (o instanceof Bullet)
				return isDistanceLess(o,kinetic_shield.KSradius+PLAYER_BULLET_SPEED/2+o.radius);
			if (o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			return isDistanceLess(o,radius+o.radius);
		}
		@Override void draw() {
//			direction += graphicsDir;   // produces interesting effect
			graphicsDir += graphicsDirBy;
			super.draw();
		}
		@Override void die(Object cause) {
			super.die(cause);
			new FermiGraphics(x, y, UFO_EXPLOSION_RADIUS).color = game.humans.color;
		}
	}
	class SuperDisruptor extends Ship {
		final double ttl;
		final double graphicsDirBy = randOf(-1,1)*D360/ ttl(seconds(40));
		final SO owner;
		final ForceField forceField;

		public SuperDisruptor(double x, double y) {
			this(x, y, null, PLAYER_RESPAWN_TIME.divide(2), 0,null);
		}
		public SuperDisruptor(SO OWNER) {
			this(OWNER.x + 50, OWNER.y - 50, OWNER, seconds(50), 10, graphics(MaterialIcon.DISC_FULL, 20, game.humans.colorTech, null));
		}
		private SuperDisruptor(double x, double y, SO OWNER, Duration TTL, double HIT_RADIUS, Image GRAPHICS) {
			super(SuperDisruptor.class, x, y, 0, 0, HIT_RADIUS, GRAPHICS, 0, 0);
			ttl = ttl(TTL);
			graphicsDir = randOf(-1,1)*deg(D360/ ttl(seconds(20)));
			graphicsScale = 0;
			owner = OWNER;
			forceField = new ForceField() {
				final double radius = SHUTTLE_KINETIC_SHIELD_RADIUS;
				@Override void apply(PO o) {
					if (isDistanceLess(o, radius)) {
						o.dx *= 0.96;
						o.dy *= 0.96;
					}
				}
				@Override double force(double mass, double dist) {
					return 0;
				}

				@Override
				public void doLoop() {
					super.doLoop();
					x = SuperDisruptor.this.x;
					y = SuperDisruptor.this.y;
					this.isin_hyperspace = SuperDisruptor.this.isin_hyperspace; // must always match
					// radiate particles
					createRandomDisruptorParticle(30,radius,this); // radiation effect
					// warp grid
					double strength = 17 - 2*cache_speed;
					game.grid.applyExplosiveForce(10*strength, new Vec(x,y), 60);
				}
			};
			createHyperSpaceAnimIn(game, this);
			game.entities.addForceField(forceField);
			game.runNext.add(ttl, () -> { if (!dead) createHyperSpaceAnimOut(game, this); });
			game.runNext.add(ttl + ttl(millis(200)), () -> {
				game.entities.removeForceField(forceField);
				dead=true;
			});
		}
		@Override boolean isHitDistance(SO o) {
			return isDistanceLess(o,radius+o.radius);
		}
		@Override void draw() {
			graphicsDir += graphicsDirBy;
			super.draw();
		}
		@Override void die(Object cause) {
			super.die(cause);
			game.entities.removeForceField(forceField);
			new FermiGraphics(x, y, UFO_EXPLOSION_RADIUS).color = game.humans.color;
		}
	}
	/** Represents defunct ship. Gives upgrades. */
	class Satellite extends PO {
		final Enhancer e;
		final boolean isLarge;

		/** Creates small satellite out of Shuttle or large Satellite. */
		public Satellite(PO s, double DIR) {
			super(Satellite.class,
				s.x,s.y,
				s instanceof Shuttle ? 0.2*cos(DIR) : s.dx,
				s instanceof Shuttle ? 0.2*sin(DIR) : s.dy,
				SATELLITE_RADIUS/2, null
			);
			e = s instanceof Shuttle ? randOf(game.ROCKET_ENHANCERS_NO_SHUTTLE) : ((Satellite)s).e;
			children = new HashSet<>(2);
			graphics = graphics(game.humans.intelOn.is() ? e.icon : MaterialDesignIcon.SATELLITE_VARIANT,40,game.humans.colorTech,null);
			isLarge = false;
			graphicsScale = 0.5;
		}
		/** Creates large Satellite. */
		public Satellite() {
			this(randEnum(Side.class));
		}
		/** Creates large Satellite. */
		public Satellite(Side dir) {
			super(Satellite.class,
				(dir==Side.LEFT ? 0 : 1)*game.field.width, rand01()*game.field.height,
				(dir==Side.LEFT ? 1 : -1)*SATELLITE_SPEED, 0,
				SATELLITE_RADIUS, graphics(MaterialDesignIcon.SATELLITE_VARIANT,40,game.humans.colorTech,null)
			);
			e = randOf(game.ROCKET_ENHANCERS);
			children = new HashSet<>(2);
			if (game.humans.intelOn.is()) new EIndicator(this,e);
			isLarge = true;
			graphicsScale = 1;
		}

		void move() {}
		void doLoopOutOfField() {
			y = game.field.modY(y);
			if (isLarge)
				if (game.field.isOutsideX(x)) dead = true;
			else
				x = game.field.modX(x);
		}
		void pickUpBy(Rocket r) {
			e.enhance(r);
			dead = true;
		}
		void explode() {
			if (isLarge) {
				dead = true;
				game.runNext.add(() -> new Satellite(this, -1));
			}
		}
	}

	/** Gun projectile. */
	class Bullet extends PO {
		final Ship owner; // can be null
		Color color;
		double ttl;
		double ttl_d;
		boolean isBlackHole = false;
		boolean isHighEnergy = false;
		private double tempX, tempY;    // cache for collision checking
		private short bounced = 0;    // prevents multiple bouncing off shield per loop

		Bullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL) {
			super(Bullet.class,x,y,dx,dy,hit_radius,null);
			owner = ship;
			color = game.mission.color;
			ttl = 1;
			ttl_d = 1/TTL;
		}

		@Override
		public void doLoop() {
			if (bounced==1 || bounced==2) bounced++;
			x += dx;
			y += dy;
			doLoopOutOfField();

			// draw(); must be called after collision checking

			ttl -= g_potential*ttl_d;
			if (ttl<0) {
				dead = true;
				onExpire(null);
			}

			if (isBlackHole) {
				double strength = 10; //speed();
				game.grid.applyImplosiveForce((float)strength, new Vec(x,y), 40);
			}
		}

		@Override
		void draw() {
			// the classic point bullet
//			double r = isHighEnergy ?  4 : 2;
//			gc_bgr.setFill(color);
//			gc_bgr.fillOval(x-1,y-1,r,r);

			// line bullets
			GraphicsContext g = gc_bgr;
			g.setGlobalAlpha(0.4);
			g.setStroke(color);
			g.setLineWidth(isHighEnergy ?  5 : 3);
			g.strokeLine(x,y,x+dx*0.7,y+dy*0.7);
			g.setStroke(null);
			g.setGlobalAlpha(1);
		}

		// cause == null => natural expiration, else hit object
		void onExpire(PO cause) {
			if (isBlackHole && !isin_hyperspace) {
				Player own = owner instanceof Rocket ?((Rocket)owner).player : null;
				game.entities.addForceField(new BlackHole(own, seconds(20),x,y));
			}
		}

		void checkCollision(PO e) {
			if (owner==e) return; // avoid self-hits (bug fix)
			if (isin_hyperspace!=e.isin_hyperspace) return;   // forbid space-hyperspace interaction

			// Fast bullets need interpolating (we check inter-frame collisions)
			// Im still not sure this is the best implementation as far as performance goes and
			// it may require some tuning, but otherwise helps a lot.
			double speedSqr = dx*dx+dy*dy;
			if (speedSqr>25) {// if speed > 5px/frame
				int iterations = (int) speedSqr/25;
				for (int i=-(iterations-1); i<=0; i++) {
					boolean isHit = checkWithXY(e, x+dx*i/iterations, y+dy*i/iterations);
					if (isHit) break;
				}
			} else {
				checkWithXY(e,x,y);
			}
		}

		private boolean checkWithXY(PO e, double X, double Y) {
			if (dead || e.dead) return true;  // dead objects must not participate

			// hack, we temporarily set old co--ordinates, we must restore this before method returns
			tempX = x; tempY = y;
			x = X; y = Y;

			if (e.isHitDistance(this)) {
				dead = true; // bullet always dies
				if (e instanceof Rocket) {
					Rocket r = (Rocket)e;
					if (!game.deadly_bullets.get() && owner instanceof Rocket) {
						r.kinetic_shield.new KineticShieldPiece(r.dir(this));
						bounceOffShieldOf(r);

						if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
							double speed = speed();
							r.dx += 0.5 + 0.1 * dx / speed;
							r.dy += 0.5 + 0.1 * dy / speed;
							r.ddirection += randOf(-1, 1) * rand0N(0.01);
						}
					}
					if (game.deadly_bullets.get() || !(owner instanceof Rocket)) {
						if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
							r.kinetic_shield.new KineticShieldPiece(r.dir(this));
							bounceOffShieldOf(r);
							r.dx = r.dy = 0;
							r.engine.off();
						} else {
							r.player.die();
						}
					}
				} else
				if (e instanceof Asteroid) {
					if (owner instanceof Rocket)
						((Rocket) owner).player.stats.accHitEnemy(game.loop.id);

					Asteroid a = (Asteroid)e;
					a.onHit(this);
					a.explosion();
					if (owner instanceof Rocket)
						((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_ASTEROID(a));
				} else
				if (e instanceof Ufo) {
					if (owner instanceof Rocket) {
						((Rocket) owner).player.score.setValueOf(s -> s + (int) SCORE_UFO);
						((Rocket) owner).player.stats.accHitEnemy(game.loop.id);
						((Rocket) owner).player.stats.accKillUfo();
					}

					Ufo u = (Ufo)e;
					if (!(owner instanceof Ufo)) {
						u.dead = true;
						game.ufos.onUfoDestroyed();
						drawUfoExplosion(u.x,u.y);
					}
				} else
				if (e instanceof UfoSwarmer) {
					if (owner instanceof Rocket)
						((Rocket) owner).player.stats.accHitEnemy(game.loop.id);

					UfoSwarmer ud = (UfoSwarmer)e;
					if (owner instanceof Rocket) {
						game.oss.get(UfoSwarmer.class).stream()
								.filter(u -> ud.swarmId==u.swarmId && !u.isActive)
								.forEach(u -> {
									u.enemy = (Rocket)owner;
									u.isActive = true;
								});
						ud.explode();
						((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_UFO_DISC);
					}
				} else
				if (e instanceof Shuttle) {
					dead = false;   // shoot-through when allies
				} else
				if (e instanceof SuperShield) {
					SuperShield ss = (SuperShield) e;
					// we are assuming its kinetic shield is always active (by game design)
					// ignore bullets when allies | shooting from inside the shield
					if (owner instanceof Rocket && owner.distance(ss)<ss.kinetic_shield.KSradius) {
						dead = false;
					} else {
						ss.kinetic_shield.new KineticShieldPiece(e.dir(this));
						bounceOffShieldOf(ss);
					}
				} else
				if (e instanceof Satellite) {
					Satellite s = (Satellite)e;
					if (s.isLarge) s.explode();
					else dead = false; // small satellites are shoot-through
				}

				boolean wasHit = dead;
				if (isHighEnergy) dead &= false; // bullet lives on
				if (dead) onExpire(e);

				x = tempX; y = tempY;
				return wasHit;
			}

			x = tempX; y = tempY;
			return false;
		}

		private void bounceOffShieldOf(Ship s) {
			dead = false;
			if (bounced==1 || bounced==2) return;
			double d = s.dir(this);
			s.kinetic_shield.new KineticShieldPiece(d);

			// bounce bullet
			double dirBulletIn = s.dir(s.x-dx, s.y-dy); // opposite direction of incoming bullet from rocket perspective
			double dirNormal = d; // direction of hit point from rocket perspective = axis of bullet bounce
			double dirBulletOut = dirBulletIn+2*(dirNormal-dirBulletIn); // direction of bounced bullet from rocket perspective
			double speed = speed();
			dx = speed*cos(dirBulletOut);
			dy = speed*sin(dirBulletOut);
			x = tempX = s.x+s.kinetic_shield.KSradius*cos(dirNormal);
			y = tempY = s.y+s.kinetic_shield.KSradius*sin(dirNormal);
			ttl = 1;    // increase bullet range after shield bounce (cool & useful game mechanics)
			bounced = 1;

			// debug
//			gc_bgr.setLineWidth(1);
//			gc_bgr.setStroke(Color.RED);
//			gc_bgr.strokeLine(x-20,y,x+20,y);
//			gc_bgr.strokeLine(x,y-20,x,y+20);
//			gc_bgr.setStroke(Color.YELLOW);
//			gc_bgr.strokeLine(s.x,s.y,s.x+200*cos(dirBulletIn),s.y+200*sin(dirBulletIn));
//			gc_bgr.strokeLine(s.x,s.y,s.x+300*cos(dirNormal),s.y+300*sin(dirNormal));
//			gc_bgr.strokeLine(s.x,s.y,s.x+200*cos(dirBulletOut),s.y+200*sin(dirBulletOut));
		}
	}
	class SplitBullet extends Bullet {
		private int splits = 6; // results in 2^splits bullets
		public SplitBullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL) {
			super(ship, x, y, dx, dy, hit_radius, TTL);
		}

		@Override
		void onExpire(PO cause) {
			super.onExpire(cause);
//            if (splits==0) return;
			if (isBlackHole || isin_hyperspace) return;
			if (cause!=null && !isBlackHole && !isin_hyperspace && !(cause instanceof Rocket)) {
				double life_degradation = 0.9;
				double s = speed();
				double d = cause==null ? dirOf(dx,dy,s) : dir(cause);
				double d1 = d + D30;
				double d2 = d - D30;
				game.runNext.add(millis(150),() -> {
					new SplitBullet(owner, x, y, s*cos(d1), s*sin(d1), radius, life_degradation*1/ttl_d).splits = splits-1;
					new SplitBullet(owner, x, y, s*cos(d2), s*sin(d2), radius, life_degradation*1/ttl_d).splits = splits-1;
				});
			}
		}
	}
	class UfoBullet extends Bullet {
		double scale_factor;

		public UfoBullet(Ship ship, double x, double y, double dx, double dy) {
			super(ship, x,y, dx,dy, 0, UFO_BULLET_TTL);
			scale_factor = 20;
			color = game.ufos.color;
		}

		@Override void draw() {
			gc_bgr.setGlobalAlpha(0.8*(1-ttl));
			gc_bgr.setStroke(color);
			// gc_bgr.setFill(color);   //effect 2
			gc_bgr.setLineWidth(2);
			double r = scale_factor*(ttl*ttl);
			double d = 2*r;
			gc_bgr.strokeOval(x-r,y-r,d,d);
			// gc_bgr.fillOval(x-r,y-r,d,d); // effect 2
			gc_bgr.setStroke(null);
			gc_bgr.setGlobalAlpha(1);
		}

		@Override public void doLoop() {
			super.doLoop();

			double strength = 10;
			game.grid.applyExplosiveForce((float)strength, new Vec(x,y), 55);
		}
	}
	class UfoSlipSpaceBullet extends UfoBullet {
		PO target;

		public UfoSlipSpaceBullet(PO TARGET) {
			super(null, TARGET.x,TARGET.y, 0,0);
			double last_longer_times = 2;
			target = TARGET;
			ttl_d /= last_longer_times;
			scale_factor = 220;
			radius = 25; // has actual damage range
		}

		@Override public void doLoop() {
			if (target.dead) dead=true;
			else {
				// move 1px per frame, each dimension separately
				dx += 0.05*sign(target.x-x);
				dy += 0.05*sign(target.y-y);
//                x += 1*sign(target.x-x);
//                y += 1*sign(target.y-y);
			}

			super.doLoop();
		}

		@Override void draw() {
			super.draw();
			gc.setStroke(game.ufos.color);
			double r = 100 - (100-radius)*(1-ttl);
			gc.strokeRect(x-r, y-r, 2*r, 2*r);
		}

		@Override void checkCollision(PO e) {
			// We want the bullet to only hit once so we perform the check only once - when
			// bullet expires
			if (dead && ttl<=0)  {
				dead = false; // needs to be set first
				isin_hyperspace = target!=null && target.isin_hyperspace; // hyperspace targets too
				super.checkCollision(e);
				dead = true; // absolutely preserve the value
			}
		}

		@Override void onExpire(PO cause) {}
	}

	class Particle extends PO {
		double ttld;
		double ttl;
		boolean ignore_blackholes = false;

		Particle(double x, double y, double dx, double dy, double TTL) {
			super(Particle.class, x,y,dx,dy,0,null);
			ttl = 1;
			ttld = 1/TTL;
			mass = 1;
		}

		@Override void init() {
			game.oss.add(this);
		}
		@Override void move(){}
		@Override public void doLoop() {
			move();
			x += dx;
			y += dy;
			doLoopOutOfField();
			draw();

			ttl -= g_potential*ttld;
			if (ttl<0) {
				dead = true;
				onExpire();
			}
		}
		@Override void draw() {}
		void onExpire() {}
	}
	interface ParticleEmitter {
		void emit(double x, double y, double dir, double param1);
	}

	class RocketEngineDebris extends Particle {
		RocketEngineDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
			super(x,y,dx,dy,ttlmultiplier*ttl(millis(150)));
		}

		@Override void draw() {
			gc.setGlobalAlpha(ttl);
			gc.setFill(game.humans.color);
			gc.fillOval(x-1,y-1,1,1);
			gc.setGlobalAlpha(1);
		}
	}
	class BlackHoleDebris extends Particle {
		BlackHoleDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
			super(x,y,dx,dy,ttlmultiplier*ttl(millis(150)));
		}

		@Override void draw() {
			gc_bgr.setGlobalAlpha(ttl);
//			gc_bgr.setFill(ttl<0.5 ? game.mission.color : game.humans.color);
			gc_bgr.setFill(game.mission.color.interpolate(game.humans.color,ttl));
			gc_bgr.fillOval(x-1,y-1,1,1);
			gc_bgr.setGlobalAlpha(1);
		}
	}
	private final ParticleEmitter ROCKET_ENGINE_DEBRIS_EMITTER = (x,y,dir,strength) -> {
		double dispersion_angle = D45;
		double d1 = dir + (rand01())*dispersion_angle;
		double d4 = dir + .5*(rand01())*dispersion_angle;
		double d2 = dir - (rand01())*dispersion_angle;
		double d3 = dir - .5*(rand01())*dispersion_angle;
		game.runNext.add(() -> {
			new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d1),1*sin(d1),strength);
			new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d2),1*sin(d2),strength);
			new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d3),1*sin(d3),strength);
			new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d4),1*sin(d4),strength);
		});
	};
	class PulseEngineDebris extends Particle {
		PulseEngineDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
			super(x,y,dx,dy,ttlmultiplier* ttl(millis(250)));
		}

		@Override void draw() {
			gc.setGlobalAlpha(ttl);
			gc.setFill(game.humans.color);
			gc.fillOval(x-1,y-1,2,2);
			gc.setGlobalAlpha(1);
		}
	}
	/** Omnidirectional expanding wave. Represents active communication of the ship. */
	class RadioWavePulse extends Particle {
		final double dxy;
		final boolean rect;
		double radius = 0;
		final Color color;
		final boolean inverse;
		SO owner = null;

		RadioWavePulse(SO OWNER, double EXPANSION_RATE, Color COLOR, boolean RECTANGULAR) {
			this(OWNER.x,OWNER.y,0,0,EXPANSION_RATE, COLOR, RECTANGULAR);
			owner = OWNER;
		}
		RadioWavePulse(double x, double y, double dx, double dy, double EXPANSION_RATE, Color COLOR, boolean RECTANGULAR) {
			super(x,y,dx,dy, ttl(seconds(1.5)));
			dxy = EXPANSION_RATE;
			inverse = dxy<0;
			color = COLOR;
			rect = RECTANGULAR;
			radius = inverse ? -dxy*ttl/ttld : 0;
		}

		@Override void move() {
			if (owner!=null) {
				x = owner.x;
				y = owner.y;
			} else {
				super.move();
			}
		}

		@Override void draw() {
			radius += dxy;
			gc_bgr.setGlobalAlpha(0.5*(inverse ? 1-ttl : ttl));
			gc_bgr.setStroke(color);
			gc_bgr.setLineWidth(2);
			if (rect) {
				Affine a = rotate(gc_bgr, 360*ttl/3, x,y);
				gc_bgr.strokeRect(x-radius,y-radius, 2*radius,2*radius);
				gc_bgr.setTransform(a);
			} else {
				gc_bgr.strokeOval(x-radius,y-radius, 2*radius,2*radius);
			}
			gc_bgr.setGlobalAlpha(1);
		}
	}

	static Color COLOR_DB = new Color(0.11764706f,0.5647059f,1.0f,0.2);
	class ShieldPulse extends Particle {
		double dxy;
		Color color;
		final Ship s;

		ShieldPulse(Ship ship, double x, double y) {
			super(x,y,0,0,0);
			color = Color.DODGERBLUE;
			radius = 0;
			s = ship;
		}

		@Override public void doLoop() {
			radius += dxy;
			super.doLoop();
		}
		@Override void move() {
			dx = 0.95*s.dx;
			dy = 0.95*s.dy;
		}

		@Override void draw() {
			if (s.dead) return;
//            gc.setGlobalBlendMode(OVERLAY);
			gc.setGlobalAlpha((s.kinetic_shield.KSenergy/s.kinetic_shield.KSenergy_max)*sqrt(ttl));
			gc.setStroke(null);
			gc.setFill(new RadialGradient(0,0,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(0.85,COLOR_DB),new Stop(1,Color.TRANSPARENT)));
			drawOval(gc,x,y,radius);
			gc.setGlobalAlpha(1);
//            gc.setGlobalBlendMode(SRC_OVER);
		}
	}

	/** Default floating space object. */
	abstract class Asteroid<M extends Mover> extends PO {
		double dir;
		double speed;
		double size;    // normalized to <0;1> range
		double size_hitdecr; // radius decrease coeficient when hit
		double size_child = 0.5;    // child.size = size_child*size
		double size_min = 0.3; // if smaller -> split !occur
		int splits = 2; // number of children when split occurs
		M propulsion = null;
		int hits = 0;
		int hits_max = 0;

		public Asteroid(double X, double Y, double SPEED, double DIR, double RADIUS) {
			super(Asteroid.class, X, Y, SPEED*cos(DIR), SPEED*sin(DIR), RADIUS, null);
			size = radius;
			dir = DIR;
			speed = SPEED;
			splits = size>size_min ? 2 : 0;
			mass = size*100;
		}

		@Override void move() {}
		@Override void draw() {
			throw new UnsupportedOperationException();
		}
		void onHit(SO o) {
			hits++;
			if (!(o instanceof Bullet) || hits>hits_max) split(o);
			else size *= size_hitdecr;
			onHitParticles(o);
		}
		void split(SO o) {
			boolean spontaneous = o instanceof BlackHole;
			dead = true;
			game.onPlanetoidDestroyed();
			game.runNext.add(() ->
				repeat(splits, i -> {
					double h = rand01();
					double v = rand01();
					double dxnew = spontaneous ? dx : dx+randMN(-1,1.1);
					double dynew = spontaneous ? dy : dy+randMN(-1,1.1);
					double speednew = sqrt(dxnew*dxnew+dynew*dynew);
					double dirnew = dirOf(dxnew,dynew,speednew);
					game.mission.planetoidConstructor.apply(x+h*0.2*size,y+v*0.2*size,speednew,dirnew, size_child*size);
				})
			);
		}
		void explosion() {
			new FermiGraphics(x,y,radius*2.5);

//			gc_bgr.setGlobalAlpha(0.2);
//			gc_bgr.setFill(game.mission.color);
//			drawOval(gc_bgr,x,y,100);
//			gc_bgr.setGlobalAlpha(1);
		}
		abstract void onHitParticles(SO o);
	}

	private interface Mover {
		void calcSpeed(Asteroid o);
	}
	private interface Draw2 {
		void drawBack();
		void drawFront();
	}

	private static class OrganelleMover implements Mover {
			double dirchange = rand0N(D360)/5/FPS;
			double ttldirchange = ttl(seconds(rand0N(12)));
			double ttldirchanging = ttl(seconds(rand0N(3)));

			public void calcSpeed(Asteroid o){
				// rotate at random time for random duration by random angle
				ttldirchange--;
				if (ttldirchange<0) {
					o.dir += dirchange;
					ttldirchanging--;
					if (ttldirchanging<0) {
						ttldirchange = ttl(seconds(rand0N(10)));
						ttldirchanging = ttl(seconds(rand0N(3)));
					}
				}
				o.dx = o.speed*cos(o.dir);
				o.dy = o.speed*sin(o.dir);
			}
		}
	private class Energ extends Asteroid<OrganelleMover> {
		Color colordead = Color.BLACK;
		Color coloralive = Color.DODGERBLUE;
		double heartbeat = 0;
		double heartbeat_speed = 0.5*D360/ ttl(seconds(1+rand0N(size/30))); // times/sec

		public Energ(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 0.98;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		@Override public void doLoop() {
			heartbeat += heartbeat_speed;
			super.doLoop();
		}

		@Override void draw() {
			gc_bgr.setStroke(null);
			// alternative looks (eye in the center), looks more organic and undoubtedly more creepy
//            gc_bgr.setFill(new RadialGradient(0,0,0.5,0.5,0.5,true,NO_CYCLE,
//                    new Stop(0,Color.BLACK),
//                    new Stop(0.35 + 0.25 + 0.25 * sin(heartbeat),ffff),
//                    new Stop(0.8 + 0.2*sin(heartbeat),Color.TRANSPARENT))
//            );

			gc_bgr.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,
					 new Stop(0+abs(0.3*sin(heartbeat)),colordead),
					 new Stop(0.5,coloralive),
					 new Stop(1,Color.TRANSPARENT)));
			drawOval(gc_bgr,x,y,radius);

			// trying to emulate bloom effect with overlay blending here, so far marginally successful
//		    gc.setFill(ccccc);
//		    gc.setGlobalBlendMode(BlendMode.OVERLAY);
//		    drawOval(gc,x,y,radius);
//		    gc.setGlobalBlendMode(BlendMode.SRC_OVER);
		}

		@Override void onHitParticles(SO o) {
			double hitdir = dir(o);
			int particles = (int)radius/2;
			repeat(15*particles, i -> new EnergParticle(hitdir));
		}

		class EnergParticle extends Particle {
			final double r = randMN(0.5,2.5+Energ.this.size);

			public EnergParticle(double hitdir) {
				super(
					Energ.this.x+Energ.this.radius*cos(hitdir),
					Energ.this.y+Energ.this.radius*sin(hitdir),
					Energ.this.dx + randMN(-1,1) + 1.5*rand01()*cos(hitdir),
					Energ.this.dy + randMN(-1,1) + 1.5*rand01()*sin(hitdir),
					ttl(seconds(0.5+rand0N(1)+rand0N(size)))
				);
			}

			@Override void draw() {
				gc_bgr.setFill(ttl<0.5 ? colordead : coloralive); // crisp
//                 gc_bgr.setFill(colordead.interpolate(coloralive, sqrt(ttl))); // smooth
				gc_bgr.fillOval(x,y,r,r);
			}
		}
	}
	private static final Color ccccc = Color.color(0.7,0.8,1,0.4);
	private static final Effect eeeee = new Bloom(0);
	private class Energ2 extends Energ {
		public Energ2(double X, double Y, double SPEED, double DIR, double RADIUS) {
			super(X, Y, SPEED, DIR, RADIUS);
			coloralive = Color.rgb(244,48,48);
			hits_max = splits;
		}

		@Override void draw() {
			gc.setGlobalBlendMode(DARKEN);
			gc.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0,coloralive),new Stop(0.2,coloralive),new Stop(0.5,Color.BLACK),new Stop(1,Color.TRANSPARENT)));
			drawOval(gc,x,y,radius);
			gc.setGlobalBlendMode(SRC_OVER);
		}
	}
	private class Particler extends Asteroid<OrganelleMover> {
		Color colordead = Color.BLACK;
		Color coloralive = Color.RED;
		double heartbeat = 0;
		double heartbeat_speed = 0.5*D360/ ttl(seconds(1+rand0N(size/30))); // times/sec

		public Particler(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 0.98;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		@Override public void doLoop() {
			heartbeat += heartbeat_speed;
			super.doLoop();

			int particleCount = (int)radius/5;
			repeat(particleCount, i -> new ParticlerParticle(randAngleRad()));

//			gc.setStroke(colordead);
//			repeat(particleCount, i -> {
//				double d = randAngleRad();
//				double r = randMN(0.5,1)*radius;
//				gc.strokeLine(x,y, x+r*cos(d),y+r*sin(d));
//			});
//			gc.setStroke(coloralive);
//			repeat(particleCount, i -> {
//				double d = randAngleRad();
//				double r = randMN(0,0.5)*radius;
//				gc.strokeLine(x,y, x+r*cos(d),y+r*sin(d));
//			});
		}

		@Override void draw() {}

		@Override void onHitParticles(SO o) {
			double hitdir = dir(o);
			int particleCount = (int)radius/2;
			repeat(15*particleCount, i -> new ParticlerDeadParticle(hitdir));
		}

		class ParticlerParticle extends Particle {
			final double r = randMN(0.8,2.5+2*Particler.this.size);
//			double dbyx = 0, dbyy = 0;
//			double byx = 0, byy = 0;

			public ParticlerParticle(double hitdir) {
				super(
					Particler.this.x,
					Particler.this.y,
					Particler.this.dx + (rand0N(0.2)+0.3)*cos(hitdir),
					Particler.this.dy + (rand0N(0.2)+0.3)*sin(hitdir),
					ttl(seconds(0.15+rand0N(size)))
				);
//				dbyx = (rand0N(0.2)+0.3)*cos(hitdir);
//				dbyy = (rand0N(0.2)+0.3)*sin(hitdir);
			}

			@Override
			public void doLoop() {
				super.doLoop();
//				byx+=dbyx; byy+=dbyy;
			}

			@Override void draw() {
				gc.setFill(ttl<0.5 ? colordead : coloralive); // crisp
//				gc.setStroke(ttl<0.5 ? colordead : coloralive); // crisp
//				gc.setFill(colordead.interpolate(coloralive, sqrt(ttl))); // smooth
				gc.fillOval(x,y,r+ttl*ttl,r+ttl*ttl);


//				gc.strokeLine(Particler.this.x,Particler.this.y,Particler.this.x+byx,Particler.this.y+byy);
			}
		}
		class ParticlerDeadParticle extends Particle {
			final double r = randMN(0.5,2+Particler.this.size);

			public ParticlerDeadParticle(double hitdir) {
				super(
					Particler.this.x,
					Particler.this.y,
					Particler.this.dx + randMN(-1,1) + 1*rand01()*cos(hitdir),
					Particler.this.dy + randMN(-1,1) + 1*rand01()*sin(hitdir),
					ttl(seconds(0.2+rand0N(size)))
				);
			}

			@Override void draw() {
//				gc_bgr.setFill(colordead); // crisp
				gc_bgr.setFill(ttl<0.5 ? colordead : coloralive); // crisp
//				gc_bgr.setFill(colordead.interpolate(coloralive, sqrt(ttl))); // smooth
				gc_bgr.fillOval(x,y,r,r);
			}
		}
	}
	private class PlanetoDisc extends Asteroid<OrganelleMover> {
		public PlanetoDisc(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		@Override public void doLoop() {
			super.doLoop();
			applyPlayerRepulseForce(this,300);
		}
		@Override void draw() {
			double d = radius*2;
			gc_bgr.setFill(game.mission.color);
			gc_bgr.fillOval(x-radius,y-radius,d,d);
		}
		@Override void onHitParticles(SO o) {}
	}
	private class Stringoid extends Asteroid<OrganelleMover> {
		public Stringoid(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
			createHyperSpaceAnimOut(game,this);
		}

		@Override void draw() {
			double d = radius*2;
			gc_bgr.setStroke(game.mission.color);
			gc_bgr.setLineWidth(3);
			gc_bgr.strokeOval(x-radius,y-radius,d,d);
		}
		@Override void onHitParticles(SO o) {
			repeat((int)(size*4), i -> 
				game.runNext.add(millis(randMN(100,300)), () -> {
					double r = 50+radius*2;
					double d = randAngleRad();
					new FermiGraphics(x+r*cos(d),y+r*sin(d),2).color = game.mission.color;
				})
			);
		}
		@Override void explosion() {
			new FermiGraphics(x,y,4+radius*1.3);
		}
	}
	private class Linker extends Asteroid<OrganelleMover> {
		double graphicsRadius;
		double ineptTtl = ttl(seconds(1));
		public Linker(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			size = LIFE;
			radius = 10;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (2-3) * 0.5 -> 2 * 0.25 -> 4 * 0.125
			splits = size>0.5 ? randOf(3,2) : size>0.125 ? 4 : 0;
			graphicsRadius = size>0.5 ? 8 : size>0.125 ? 4 : 2;
			hits_max = 0;
			createHyperSpaceAnimOut(game,this);
		}

		@Override
		public void doLoop() {
			super.doLoop();
			ineptTtl--;
		}

		@Override void draw() {
			double r = graphicsRadius;
			gc.setFill(game.mission.color);
			drawOval(gc, x,y,r);

			double connectionDistMin = 20, connectionDistMax = 100;
			double forceDistMin = 20, forceDistMax = 80, forceDistMid = (forceDistMax-forceDistMin)*2/3;
			stream(game.oss.get(Asteroid.class)).select(Linker.class).forEach(l -> {
				double dist = distance(l);
				double dir = dir(l);
				// link
				if (dist>connectionDistMin && dist<connectionDistMax) {
					gc.setGlobalAlpha(1-dist/connectionDistMax);
					drawDottedLine(x,y,0,dist,cos(dir),sin(dir), game.mission.color);
					gc.setGlobalAlpha(1);
				}
				// glue
				if (dist<forceDistMax && ineptTtl<=0 && l.ineptTtl<=0) {

					// Algorithm (1): adjusts relative speed of the nodes
					// This causes nodes to form and move as part of net groups, as their speeds cancel each other out
					// The unfortunate effect here is that eventually all groups stand still
//					dx += (l.dx-dx)/1000;
//					dy += (l.dy-dy)/1000;

					// Algorithm (2): modification of (1), adjusts relative speed of the slower node
					// This does not cause the net groups to slow down so much. Because now only one node is adjusted,
					// we must make the effect twice as strong
					// The unfortunate effect here is that eventually all groups move the same speed & direction
//					if (speed() < l.speed()) {
//						dx += (l.dx - dx)/500;
//						dy += (l.dy - dy)/500;
//					}

					// Algorithm (3): uses repel-near-attract-distant forces at close distance
					// This causes dynamic net groups with nodes often orbiting around the centre
					// The downside is somewhat chaotic behavior as opposed to 1) & 2).
					double force = dist<forceDistMid
										? -mapTo01(dist, forceDistMin, forceDistMid)
										:  mapTo01(dist, forceDistMid, forceDistMax);
						   force /= 200;
					dx += force*cos(dir);
					dy += force*sin(dir);
				}
			});
		}
		@Override void onHitParticles(SO o) {
			repeat((int)(size*4), i -> game.runNext.add(millis(randMN(100,300)), () -> {
				  double r = 50+radius*2*Utils.rand01();
				  double d = randAngleRad();
				  new FermiGraphics(x+r*cos(d),y+r*sin(d),2).color = game.mission.color;
			}));
		}
		@Override void explosion() {
			new FermiGraphics(x,y,4+radius*1.3);
		}
	}
	private class Inkoid extends Asteroid<OrganelleMover> {
		double trail_ttl = ttl(seconds(0.5+rand0N(2)));

		public Inkoid(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		public void doLoop() {
			super.doLoop();
			trail_ttl--;
			if (trail_ttl<0) {
				trail_ttl = ttl(seconds(0.5+rand0N(2)));
				new InkoidDebris(x,y,0,0,5,seconds(2));
			}
		}

		@Override void onHit(SO o) {
			super.onHit(o);
			propulsion.dirchange *= 2; // speed rotation up
			propulsion.ttldirchange = -1; // change direction now
		}
		@Override void draw() {
			new InkoidGraphics(x,y,radius);
		}
		@Override void onHitParticles(SO o) {
//            double hitdir = dir(o);
			int particles = (int)randMN(1,3);
			repeat(particles, i ->
				new InkoidDebris(
					x,y,
					randMN(-2,2),randMN(-2,2),
					2 + rand01()*size_child*radius/4,
					seconds(0.5+rand0N(1)+rand0N(size))
				)
			);
		}

		private class InkoidGraphics extends Particle implements Draw2 {
			private final Color COLOR = new Color(0,.1,.1, 1);
			double r;

			public InkoidGraphics(double x, double y, double RADIUS) {
				this(x,y,0,0,RADIUS,seconds(0.4));
			}
			public InkoidGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy, ttl(time));
				radius = RADIUS;
			}

			@Override void draw() {
				r = radius*ttl;
			}
			@Override public void drawBack() {
				double rr = 2+r;
				double d = rr*2;
				gc_bgr.setFill(game.mission.color);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			@Override public void drawFront() {
				double rr = max(0,r - (1-ttl)*2);
				double d = rr*2;
	//            gc_bgr.setFill(Color.BLACK);
				gc_bgr.setFill(COLOR);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}
		private class InkoidDebris extends InkoidGraphics {
			private final Color COLOR = new Color(0,.1,.1, 1);
			public InkoidDebris(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy,RADIUS,time);
			}
			@Override public void drawBack() {
	//            double r = radius*(1-ttl)/3+7+(radius-5)*ttl;
				double rr = 2+r;
				double d = rr*2;
				gc_bgr.setFill(game.mission.color);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			@Override public void drawFront() {
	//            double r = radius*(1-ttl)/3+5+(radius-5)*ttl;
				double rr = max(2,r - (1-ttl)*2);
				double d = rr*2;
	//            gc_bgr.setFill(Color.BLACK);
				gc_bgr.setFill(COLOR);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}
	}
	private class Genoid extends Asteroid<OrganelleMover> {
		double circling = 0;
		double circling_speed = 0.5*D360/ ttl(seconds(0.5)); // times/sec
		double circling_mag = 0;

		final PTtl trail = new PTtl(() -> ttl(seconds(0.5+rand0N(2))),() -> {
			if (0.9>size && size >0.4) {
				new GenoidDebris(x+1.5*radius*cos(circling),y+1.5*radius*sin(circling),0,0,2,seconds(1.6));
				new GenoidDebris(x+1.5*radius*cos(circling),y+1.5*radius*sin(circling),0,0,2,seconds(1.6));
			} else {
				new GenoidDebris(x+circling_mag*1.5*radius*cos(dir+D90),y+circling_mag*1.5*radius*sin(dir+D90),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
				new GenoidDebris(x+circling_mag*1.5*radius*cos(dir-D90),y+circling_mag*1.5*radius*sin(dir-D90),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
			}
		});

		public Genoid(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		@Override public void doLoop() {
			super.doLoop();
			circling += circling_speed;
			circling_mag = sin(circling);
			trail.run();
		}

		@Override void doLoopEnd() {
			super.doLoopEnd();
		}

		@Override void onHit(SO o) {
			super.onHit(o);
			propulsion.dirchange *= 2; // speed rotation up
			propulsion.ttldirchange = -1; // change direction now
		}

		@Override void draw() {
			new GenoidGraphics(x,y,radius);
		}

		@Override void onHitParticles(SO o) {
			// double hitDir = dir(o);
			int particles = (int)randMN(1,3);
			repeat(particles, i ->
				new GenoidDebris(
					x,y,
					randMN(-2,2),randMN(-2,2),
					2 + rand01()*size_child*radius/4,
					seconds(0.5+rand0N(1)+rand0N(size))
				)
			);
		}

		private class GenoidGraphics extends Particle implements Draw2 {
			double r;

			public GenoidGraphics(double x, double y, double RADIUS) {
				this(x,y,0,0,RADIUS,seconds(0.4));
			}
			public GenoidGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy, ttl(time));
				radius = RADIUS;
			}

			@Override void draw() {
				r = radius*ttl;
			}
			@Override public void drawBack() {
				double rr = ttl*(2+r);
				double d = rr*2;

				// we are only interested in the border & strokeOval performs > fillOval
				// gc_bgr.setFill(game.mission.color);
				// gc_bgr.fillOval(x-rr,y-rr,d,d);
				gc_bgr.setStroke(game.mission.color);
				gc_bgr.setLineWidth(3);
				gc_bgr.strokeOval(x-rr,y-rr,d,d);
			}
			@Override public void drawFront() {
				double rr = max(0,r - (1-ttl)*2);
				rr *= ttl;  // no visual effect, but produces nice radial pattern effect when inkoid dies
				double d = rr*2;
				gc_bgr.setFill(Color.BLACK);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}
		private class GenoidDebris extends GenoidGraphics {
			public GenoidDebris(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy,RADIUS,time);
			}
			@Override public void drawBack() {
	//            double r = radius*(1-ttl)/3+7+(radius-5)*ttl;
				double rr = 2+r;
				double d = rr*2;
				gc_bgr.setFill(game.mission.color);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			@Override public void drawFront() {
	//            double r = radius*(1-ttl)/3+5+(radius-5)*ttl;
				double rr = max(2,r - (1-ttl)*2);
				double d = rr*2;
				gc_bgr.setFill(Color.BLACK);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}
	}
	private class Fermi extends Asteroid<OrganelleMover> {
		final PTtl trail = new PTtl(() -> ttl(seconds(0.5+rand0N(2))), () -> new FermiDebris(x,y,0,0,5,seconds(0.6)));
		double ttlocillation = 0;
		FermiMove pseudomovement;

		public Fermi(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;

			if (size<=0.5 && randBoolean()) {
				Ƒ1<Fermi,FermiMove> m = randOf(FERMI_MOVES);
				pseudomovement = m.apply(this);
			}
		}

		public void doLoop() {
			if (pseudomovement!=null) ttlocillation += pseudomovement.oscillationIncrement();
			double dxtemp = dx;
			double dytemp = dy;

			if (pseudomovement!=null) pseudomovement.modifyMovement();
			super.doLoop();
			trail.run();

			dx = dxtemp;
			dy = dytemp;
		}

		@Override
		void onHit(SO o) {
			super.onHit(o);
			propulsion.dirchange *= 2; // speed rotation up
			propulsion.ttldirchange = -1; // change direction now
		}
		void draw() {
			new FermiGraphics(x,y,radius);
		}
		void onHitParticles(SO o) {
			// double hitDir = dir(o);
			int particles = (int)randMN(1,3);
			repeat(particles, i ->
				new FermiDebris(
					x,y,
					randMN(-2,2),randMN(-2,2),
					2 + rand01()*size_child*radius/4,
					seconds(0.5+rand0N(1)+rand0N(size))
				)
			);

			gc_bgr.setLineWidth(2);
			int lightnings = (int)(size*5);
			repeat(lightnings, i -> strokeLine(gc_bgr, x,y,rand0N(size*300),randAngleRad()));
		}

		abstract class FermiMove {
			abstract double oscillationIncrement();
			abstract void modifyMovement();
		}
		class StraightMove extends FermiMove {
			double oscillationIncrement() {
				return 0;
			}
			public void modifyMovement() {}
		}
		class WaveMove extends FermiMove {
			double oscillationIncrement() {
				return 0.5/(10*size);
			}
			public void modifyMovement() {
				double s = sin(ttlocillation);
				dx += 2*cos(dir+s*D90) ;
				dy += 2*sin(dir+s*D90);
			}
		}
		class ZigZagMove extends FermiMove {
			double oscillationIncrement() {
				return 1;
			}
			public void modifyMovement() {
				if (ttlocillation%20<10) {
					dx += cos(dir-D90) ;
					dy += sin(dir-D90);
				} else {
					dx += cos(dir+D90) ;
					dy += sin(dir+D90);
				}
			}
		}
		class FlowerMove extends FermiMove {
			double oscillationIncrement() {
				return 0.5/(10*size);
			}
			public void modifyMovement() {
				double s = sin(ttlocillation);
				dx += 2*cos(dir+s*PI) ;
				dy += 2*sin(dir+s*PI);
			}
		}
		class KnobMove extends FermiMove {
			double oscillationIncrement() {
				return 0.5/(10*size);
			}
			public void modifyMovement() {
				double s = sin(ttlocillation);
				dx += 2*cos(dir+s*D360) ;
				dy += 2*sin(dir+s*D360);
			}
		}
		class SpiralMove extends FermiMove {
			double oscillationIncrement() {
				return .18;
			}
			public void modifyMovement() {
				dx += 2*cos(dir+ttlocillation) ;
				dy += 2*sin(dir+ttlocillation);
			}
		}
		class SidespiralMove extends FermiMove {
			double ttl = 1;
			double ttld = 1/ ttl(seconds(randMN(5,10)));
			double dir;
			double dird = 0.2 * randOf(-1,1) * D360/ ttl(seconds(3));

			double oscillationIncrement() {
				return .18;
			}
			public void modifyMovement() {
				if (ttl==1) dir = dirOf(dx,dy,1); // init direction
				ttl -= ttld;
				if (ttl<=0) pseudomovement = null; // remove itself after timeout

				dir += dird;
				dird *= 1.005; // increase rotation speed with time

				// we apply speed to coordinates directly, fixes some problems with force fields
				// not working correctly when this is in effect
				x += cos(dir);
				y += sin(dir);
			}
		}

		private class FermiDebris extends FermiGraphics {
			public FermiDebris(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy,RADIUS,time);
			}
			public void drawBack() {
	//            double r = radius*(1-ttl)/3+7+(radius-5)*ttl;
				double rr = 2+r;
				double d = rr*2;
				gc_bgr.setFill(game.mission.color);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			public void drawFront() {
	//            double r = radius*(1-ttl)/3+5+(radius-5)*ttl;
				double rr = max(2,r - (1-ttl)*2);
				double d = rr*2;
				gc_bgr.setFill(Color.BLACK);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}
	}
	private static final Set<Ƒ1<Fermi,Fermi.FermiMove>> FERMI_MOVES = set(f -> f.new StraightMove(),f -> f.new WaveMove(),f -> f.new FlowerMove(),f -> f.new ZigZagMove(),f -> f.new KnobMove(),f -> f.new SpiralMove(),f -> f.new SidespiralMove());
	private class FermiGraphics extends Particle implements Draw2 {
			double r;
			Color color = game.mission.color;

			public FermiGraphics(double x, double y, double RADIUS) {
				this(x,y,0,0,RADIUS,seconds(0.3));
			}
			public FermiGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy, ttl(time));
				radius = RADIUS;
			}

			void draw() {
				r = radius*ttl;
			}

			public void drawBack() {
				double rr = 2+r;
					   rr *= ttl;
				// this has no effect on graphics, but produces nice radial pattern effect when inkoid dies
	//            rr *= ttl;
				double d = rr*2;
				gc_bgr.setFill(color);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			public void drawFront() {
				double rr = max(0,r - (1-ttl)*2);
				// this has no effect on graphics, but produces nice radial pattern effect when inkoid dies
				rr *= ttl;
				double d = rr*2;
				gc_bgr.setFill(Color.BLACK);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
		}

	/** Generates force field affecting objects. */
	abstract class ForceField extends SO {
		abstract void apply(PO o);
		abstract double force(double mass, double dist);
	}
	class BlackHole extends ForceField {
		final Player owner;
		double life;
		double ttl = 1;
		double ttld = 0;
		double radius_even_horizon_max = 20;
		double radius_even_horizon;
		double radius_ergosphere = 512;
		double radius_gravity = 1000;
		double dir = 0;
		double mass = 0;
		double massCritical = 3000;
		double magnetic_force_dir = rand0N(D360);
		int anti = 1; // attract-repulse

		public BlackHole(Player OWNER, Duration TTL, double X, double Y) {
			x=X; y=Y; ttl=1; ttld=1/ ttl(TTL); owner = OWNER;
		}

		@Override public void die() {
			dead = true;
			game.grid.applyExplosiveForce(200, new Vec(x,y), 100);
		}

		@Override void apply(PO o) {
			// Gravity affects all dimensions. Hyperspace is irrelevant.
			// if (ff.isin_hyperspace!=o.isin_hyperspace) return;

			double distX = game.field.distXSigned(x,o.x);
			double distY = game.field.distYSigned(y,o.y);
			double dist = game.field.dist(distX,distY)+1; // +1 avoids /0 or /very_small_number);
			double f = force(o.mass,dist);

			double gravity_potential_inv = computeForceInversePotential(dist,radius_gravity);
			// double gravity_potential = 1-gravity_potential_inv;

			// Used for gravity based effects. E.g. time/space dilatation.
			o.g_potential *= gravity_potential_inv;

			// Bullets are affected more & stop aging
			if (o instanceof Bullet && dist<220)
				f = f*9; // add more force

			boolean isShip = o instanceof Ship;
			boolean isRocket = isShip && o instanceof Rocket;

			// Ergosphere. Rockets rotate towards force origin - black hole.
			// This actually simplifies near-BH control (it allows orbiting BH using just 1 key)
			// and its a nice effect.
			if (isShip && dist<radius_ergosphere) {
				double ergo_potential_inv = computeForceInversePotential(dist,radius_ergosphere);
				double ergo_potential = 1-ergo_potential_inv;
				double dir = o.dir(this);
				Ship s = (Ship)o;
				double angle = s.direction-dir;
//                double angled = 0.1 * dist01*dist01 * sin(angle);
				double angled = 0.06 * ergo_potential * sin(angle);
				s.direction -= anti*angled; // no idea why - and not +
			}

			// Space resistance. Ether exists!
			// Rises chance (otherwise very low) of things falling into BH.
			// Do note this affects rockets too (makes BH escape more difficult)
			if (dist<200) {
				o.dx *= 0.994;
				o.dy *= 0.994;
			}

			// Roche limit.
			// Asteroids disintegrate due to BH tidal forces.
			// It would be ugly for asteroids not do this...
			if (o instanceof Asteroid) {
				Asteroid a = ((Asteroid)o);
				if (dist/220<a.size) {
					a.onHit(this);
				}
			}

			// Consumed debris.
			// Instead of counting all particles (debris), we approximate to 1 particle
			// per near-BH object per loop, using 0.1 mass per particle.
			// Normally debris would take time to get consumed (if at all), this way it
			// happens instantly.
			if (dist<220) {
				mass += 0.1;
			}

			// Consuming objects
			if (dist<radius_even_horizon_max) {
				if (isRocket) ((Rocket)o).player.die();
				o.dead = true;
				if (owner!=null) owner.score.setValueOf(score -> score + 20);
				mass += o.mass;
				System.out.println("consumed " + this.hashCode() + " " + mass);
			}

//            double dir = o.dir(this)+D90;
//            o.x += (1-dist/2000)*1*cos(dir);
//            o.y += (1-dist/2000)*1*sin(dir);

//            if (dist<220) {
//                double dir = o.dir(ff)+D90;
//                o.dx += (1-dist/220)*0.05*cos(dir);
//                o.dy += (1-dist/220)*0.05*sin(dir);
//            }

			// apply force
			o.dx += anti*distX*f/dist;
			o.dy += anti*distY*f/dist;
		}

		@Override public double force(double mass, double dist) {
//            if (dist>220) return 0.03*(1-dist/1000);
//            else return 0.02+0.18*(1-dist/220);

			return 0.3*pow(1-dist/radius_gravity,6);

//            if (dist>220) return 0.02*(1-dist/1000);
//            else return 0.02+0.18*(1-pow(dist/220,2));
		}

		@Override public void doLoop() {
			// lifecycle
			ttl -= ttld;
			// life = ttl;              // time limit death
			life = (massCritical-mass)/massCritical;
			if (life <= 0) {
				die();
				return;
			}

			dir += D360/FPS/2;
			radius_even_horizon = (1-life)*radius_even_horizon_max;

			// space-time warping
			// High strength produces 'quantum space-time' effect. We use that when near death.
			double grid_warp_strength = 10+70*(1-life);
			game.grid.applyImplosiveForce(grid_warp_strength, new Vec(x,y), 25);

			// gravitational waves
			double gwave_strength = 20*(1-life)*(1-life);
			double gwave_originDist = 70;
			game.grid.applyExplosiveForce(gwave_strength, new Vec(x+gwave_originDist*cos(dir),y+gwave_originDist*sin(dir)), 40);
			game.grid.applyExplosiveForce(gwave_strength, new Vec(x+gwave_originDist*cos(dir+PI),y+gwave_originDist*sin(dir+PI)), 40);

			// hawking radiation
			double d = randAngleRad();
//			double s = randMN(2,6);
//			new BlackHoleDebris(x+50*cos(d),y+50*sin(d), s*cos(d+0.1),s*sin(d+0.1), ttl(millis(500)));
//			new BlackHoleDebris(x+100*cos(d),y+100*sin(d), 3*cos(d-D90+0.1),3*sin(d-D90+0.1), ttl(millis(500)));
			createRandomDisruptorParticle(30,radius_ergosphere/2,this, true); // radiation effect

			// forms acretion disc
			if (randBoolean())
				new RocketEngineDebris(x+50*cos(d),y+50*sin(d), 4*cos(d-D90+0.1),4*sin(d-D90+0.1), ttl(millis(500)));
			else
				new BlackHoleDebris(x+50*cos(d),y+50*sin(d), 4*cos(d-D90+0.1),4*sin(d-D90+0.1), ttl(millis(500)));

			gc.setFill(Color.BLACK);
			drawOval(gc, x,y,radius_even_horizon);

			if (game.mission.id==4) {
				gc_bgr.setGlobalBlendMode(OVERLAY);
				gc_bgr.setGlobalAlpha(1-life);
//				gc_bgr.setGlobalAlpha(0.1*(1-ttl));
				gc_bgr.setEffect(new BoxBlur(100,100,1));
				gc_bgr.setFill(Color.AQUA);
				drawOval(gc_bgr, x,y,20+(1-life)*40);
				gc_bgr.setGlobalBlendMode(SRC_OVER);
				gc_bgr.setGlobalAlpha(1);
				gc_bgr.setEffect(null);
			}

			boolean isCrowded = game.oss.get(Particle.class).size() > BLACKHOLE_PARTICLES_MAX;
			for (Particle p : game.oss.get(Particle.class)) {
				if (p.ignore_blackholes) continue;

				double distX = game.field.distXSigned(x,p.x);
				double distY = game.field.distYSigned(y,p.y);
				double dist = game.field.dist(distX,distY)+1; // +1 avoids /0
				double f = force(p.mass,dist);

				p.dx *= 0.99;
				p.dy *= 0.99;
				p.dx += distX*f/dist;
				p.dy += distY*f/dist;

				// Overload effect
				// Too many particles cause BH to erupt some.
				// Two reasons:
				//    1) BHs stop particle aging and cause particle count explosion (intended) and
				//       if got out of hand, performance suffers significantly. This solves the
				//       problem rather elegantly
				//          a) system agnostic - no tuning is necessary except for PARTICLE LIMIT
				//          b) no particle lifetime management overhead
				//    2) Cool effect.
				if (isCrowded && dist<2*radius_even_horizon_max) {
					p.ignore_blackholes = true;
					p.ttl = 1;
				}

				// Time dilatation
				// Particles live longer in strong gravity fields
				if (dist<220) {
					double gravity_potential_inv = computeForceInversePotential(dist,220);
					double gravity_potential = 1-gravity_potential_inv;

					// don't age near black hole
					p.g_potential *= gravity_potential_inv;

					// ergosphere rotation effect
					double dir = p.dir(this)+D90;
					p.dx += gravity_potential*0.05*cos(dir);
					p.dy += gravity_potential*0.05*sin(dir);

				// alternative implementation
//                    p.x += (1-gravity_potential)*5*cos(dir);
//                    p.y += (1-gravity_potential)*5*sin(dir);
				}
			}
		}
	}

	/**
	 * A rocket ability. Enhances player's rocket in some way.
	 */
	class Enhancer extends Displayable {
		final Duration duration;
		final Consumer<Rocket> starter;
		final Consumer<Rocket> stopper;
		final boolean isShareable;

		Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, Consumer<Rocket> STOPPER, boolean ISSHAREABLE, CharSequence... DESCRIPTION) {
			super(NAME, ICON, DESCRIPTION);
			duration = DURATION;
			starter = STARTER;
			stopper = STOPPER;
			isShareable = ISSHAREABLE;
		}

		Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, Consumer<Rocket> STOPPER, CharSequence... DESCRIPTION) {
			this(NAME, ICON, DURATION, STARTER, STOPPER, true, DESCRIPTION);
		}

		Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, CharSequence... DESCRIPTION) {
			this(NAME, ICON, DURATION, STARTER, r -> {}, DESCRIPTION);
		}

		void enhance(Rocket r) {
			start(r);
			game.runNext.add(duration,() -> stop(r));
		}

		void start(Rocket r) {
			if (isShareable && game.humans.share_enhancers) {
				game.oss.get(Rocket.class).forEach(starter);
				game.oss.get(Rocket.class).forEach(rk -> new EIndicator(rk,this));
			} else {
				starter.accept(r);
				new EIndicator(r,this);
			}
		}

		void stop(Rocket r) {
			if (isShareable && game.humans.share_enhancers) game.oss.get(Rocket.class).forEach(stopper);
			else stopper.accept(r);
		}
	}
	/**
	 * Rocket enhancer indicator icon moving with player rocket to indicate active enhancers.
	 */
	class EIndicator implements LO {
		double ttl;
		final PO owner;
		final Node graphics;
		final int index;

		public EIndicator(PO OWNER, Enhancer enhancer) {
			owner = OWNER;
			ttl = ttl(owner instanceof Satellite ? minutes(10) : enhancer.duration);
			index = findFirstInt(0, i -> stream(owner.children).select(EIndicator.class).noneMatch(o -> o.index==i));
			owner.children.add(this);
			graphics = new Icon(enhancer.icon,15);
			playfield.getChildren().add(graphics);
		}

		public void doLoop() {
			ttl--;
			if (ttl<0) game.runNext.add(this::dispose);
			relocateCenter(graphics, owner.x+30+20*index, owner.y-30); // javafx inverts y, hence minus
		}

		public void dispose() {
			owner.children.remove(this);
			playfield.getChildren().remove(graphics);
		}

	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Finds closest non-hyperspacing rocket to the obejct. */
	Rocket findClosestRocketTo(SO to) {
		return stream(game.oss.get(Rocket.class)).filter(r -> !r.isin_hyperspace)
			.minBy(to::distance).orElse(null);
	}

	/** Applies repulsive force from every player. */
	void applyPlayerRepulseForce(PO o, double maxDist) {
		double fx = 0;
		double fy = 0;
		for (Player p : game.players) {
			if (p.rocket!=null) {
				double distX = game.field.distXSigned(o.x,p.rocket.x);
				double distY = game.field.distYSigned(o.y,p.rocket.y);
				double dist = game.field.dist(distX,distY)+1;
				double f = 1 - min(1,dist/maxDist);
				fx += distX*f*f*f/dist;
				fy += distY*f*f*f/dist;
			}
		}
		o.dx += fx;
		o.dy += fy;
		o.dx *= 0.9;
		o.dy *= 0.9;
	}
}