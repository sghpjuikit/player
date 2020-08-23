package comet;

import comet.Comet.Fermi.FermiMove;
import comet.Comet.Game.Enhancer;
import comet.Comet.Ship.Disruptor.DisruptorField;
import comet.Comet.Ship.Hyperspace;
import comet.Comet.Ship.Shield;
import comet.Utils.AbilityKind;
import comet.Utils.AbilityState;
import comet.Utils.AreaMode;
import comet.Utils.BounceHellMode;
import comet.Utils.ClassicMode;
import comet.Utils.CollisionHandlers;
import comet.Utils.Displayable;
import comet.Utils.Draw;
import comet.Utils.EndGamePane;
import comet.Utils.Events;
import comet.Utils.GameMode;
import comet.Utils.GameSize;
import comet.Utils.GamepadDevices;
import comet.Utils.Grid;
import comet.Utils.GunControl;
import comet.Utils.HowToPane;
import comet.Utils.InEffect;
import comet.Utils.InEffectValue;
import comet.Utils.LO;
import comet.Utils.Loop;
import comet.Utils.ObjectStore;
import comet.Utils.PTtl;
import comet.Utils.Play;
import comet.Utils.PlayerSpawn;
import comet.Utils.Side;
import comet.Utils.StatsGame;
import comet.Utils.StatsPlayer;
import comet.Utils.TTLList;
import comet.Utils.TimeDouble;
import comet.Utils.TimeTrial;
import comet.Utils.Vec;
import comet.Utils.Voronoi;
import comet.Utils.VoronoiMode;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.effect.MotionBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.gamepad4j.ButtonID;
import org.gamepad4j.Controllers;
import org.gamepad4j.DpadDirection;
import org.gamepad4j.IButton;
import org.gamepad4j.IController;
import org.gamepad4j.IControllerListener;
import org.gamepad4j.StickID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.ui.itemnode.ConfigEditor;
import sp.it.pl.ui.objects.Text;
import sp.it.pl.ui.objects.icon.Icon;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.util.access.V;
import sp.it.util.access.VarEnum;
import sp.it.util.animation.Anim;
import sp.it.util.conf.Config;
import sp.it.util.conf.ConfigurableByReflect;
import sp.it.util.conf.EditMode;
import sp.it.util.conf.FixedConfList;
import sp.it.util.conf.IsConfig;
import sp.it.util.functional.Functors.F0;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Functors.F5;
import sp.it.util.functional.Util;
import static comet.Comet.Constants.FPS;
import static comet.Comet.Constants.PLAYER_ABILITY_INITIAL;
import static comet.Utils.AbilityKind.SHIELD;
import static comet.Utils.AbilityState.ACTIVATING;
import static comet.Utils.AbilityState.OFF;
import static comet.Utils.AbilityState.ON;
import static comet.Utils.AbilityState.PASSSIVATING;
import static comet.Utils.D0;
import static comet.Utils.D120;
import static comet.Utils.D180;
import static comet.Utils.D30;
import static comet.Utils.D360;
import static comet.Utils.D45;
import static comet.Utils.D90;
import static comet.Utils.FONT_PLACEHOLDER;
import static comet.Utils.FONT_UI;
import static comet.Utils.GunControl.AUTO;
import static comet.Utils.GunControl.MANUAL;
import static comet.Utils.PI;
import static comet.Utils.abs;
import static comet.Utils.atan;
import static comet.Utils.calculateGunTurretAngles;
import static comet.Utils.color;
import static comet.Utils.computeForceInversePotential;
import static comet.Utils.cos;
import static comet.Utils.createHyperSpaceAnimIn;
import static comet.Utils.createHyperSpaceAnimOut;
import static comet.Utils.createPlayerStat;
import static comet.Utils.deg;
import static comet.Utils.dirDiff;
import static comet.Utils.dirOf;
import static comet.Utils.drawFading;
import static comet.Utils.drawHudCircle;
import static comet.Utils.drawHudLine;
import static comet.Utils.drawImageRotated;
import static comet.Utils.drawImageRotatedScaled;
import static comet.Utils.drawOval;
import static comet.Utils.drawTriangle;
import static comet.Utils.floorMod;
import static comet.Utils.graphics;
import static comet.Utils.max;
import static comet.Utils.min;
import static comet.Utils.pow;
import static comet.Utils.rand01;
import static comet.Utils.rand0N;
import static comet.Utils.randAngleRad;
import static comet.Utils.randBoolean;
import static comet.Utils.randEnum;
import static comet.Utils.randInt;
import static comet.Utils.randMN;
import static comet.Utils.randOf;
import static comet.Utils.rotate;
import static comet.Utils.sign;
import static comet.Utils.sin;
import static comet.Utils.sqr;
import static comet.Utils.sqrt;
import static comet.Utils.strokeLine;
import static comet.Utils.strokeOval;
import static comet.Utils.strokePolygon;
import static comet.Utils.ttl;
import static comet.Utils.ttlVal;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.geometry.Pos.BOTTOM_RIGHT;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.geometry.Pos.TOP_RIGHT;
import static javafx.scene.effect.BlendMode.DARKEN;
import static javafx.scene.effect.BlendMode.OVERLAY;
import static javafx.scene.effect.BlendMode.SRC_OVER;
import static javafx.scene.effect.BlurType.GAUSSIAN;
import static javafx.scene.input.KeyCode.DIGIT1;
import static javafx.scene.input.KeyCode.DIGIT2;
import static javafx.scene.input.KeyCode.DIGIT3;
import static javafx.scene.input.KeyCode.DIGIT4;
import static javafx.scene.input.KeyCode.DIGIT5;
import static javafx.scene.input.KeyCode.DIGIT6;
import static javafx.scene.input.KeyCode.DIGIT7;
import static javafx.scene.input.KeyCode.DIGIT8;
import static javafx.scene.input.KeyCode.DIGIT9;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.paint.Color.rgb;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.minutes;
import static javafx.util.Duration.seconds;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.clip;
import static sp.it.util.Util.pyth;
import static sp.it.util.animation.Anim.map01To010;
import static sp.it.util.animation.Anim.mapTo01;
import static sp.it.util.conf.ConfigurableKt.toConfigurableByReflect;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.findFirstInt;
import static sp.it.util.functional.Util.forEachInLineBy;
import static sp.it.util.functional.Util.forEachOnCircleBy;
import static sp.it.util.functional.Util.forEachPair;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listF;
import static sp.it.util.functional.Util.minBy;
import static sp.it.util.functional.Util.repeat;
import static sp.it.util.functional.Util.set;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.Util.computeFontHeight;
import static sp.it.util.ui.Util.computeFontWidth;
import static sp.it.util.ui.Util.layAnchor;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.Util.layStack;

@SuppressWarnings({"unused","UnnecessaryLocalVariable"})
@Widget.Info(
	author = "Martin Polakovic",
	name = "Comet",
	description = "Port of the game Comet.",
	version = "0.6.0",
	year = "2015",
	group = Widget.Group.OTHER
)
@LegacyController
public class Comet extends SimpleController {
	private static Logger LOGGER = LoggerFactory.getLogger(Comet.class);

	final Pane playfield = new Pane();  // play field, contains scenegraph game graphics
	final Canvas canvas = new Canvas();
	final Canvas canvas_bgr = new Canvas();
	final GraphicsContext gc = canvas.getGraphicsContext2D(); // draws canvas game graphics on canvas
	final GraphicsContext gc_bgr = canvas_bgr.getGraphicsContext2D(); // draws canvas game graphics on bgr canvas
	final Text message = new Text(null);
	final Game game = new Game();

	public Comet(Widget widget) {
		super(widget);

		// message
		message.setOpacity(0);
		message.setFont(new Font(FONT_UI.getName(), 50));

		// canvas
		canvas.widthProperty().bind(playfield.widthProperty());
		canvas.heightProperty().bind(playfield.heightProperty());
		canvas.setManaged(false);
		canvas_bgr.widthProperty().bind(playfield.widthProperty());
		canvas_bgr.heightProperty().bind(playfield.heightProperty());
		canvas_bgr.setManaged(false);

		syncC(playfield.widthProperty(), w -> game.field.resize(playfield.getWidth(), playfield.getHeight()));
		syncC(playfield.heightProperty(), w -> game.field.resize(playfield.getWidth(), playfield.getHeight()));

		// player stats
		double G = 10; // padding
		StackPane playerStats = layStack(
			layHorizontally(G,TOP_LEFT,     createPlayerStat(PLAYERS.getList().get(0)),createPlayerStat(PLAYERS.getList().get(4))),TOP_LEFT,
			layHorizontally(G,TOP_RIGHT,    createPlayerStat(PLAYERS.getList().get(5)),createPlayerStat(PLAYERS.getList().get(1))),TOP_RIGHT,
			layHorizontally(G,BOTTOM_LEFT,  createPlayerStat(PLAYERS.getList().get(2)),createPlayerStat(PLAYERS.getList().get(6))),BOTTOM_LEFT,
			layHorizontally(G,BOTTOM_RIGHT, createPlayerStat(PLAYERS.getList().get(7)),createPlayerStat(PLAYERS.getList().get(3))),BOTTOM_RIGHT
		);
		playerStats.setPadding(new Insets(G,0,G,G));
		playerStats.setMouseTransparent(true);
		game.players.addListener((Change<? extends Player> change) ->
			playerStats.getChildren().forEach(cc -> ((Pane)cc).getChildren().forEach(c ->
				c.setVisible(game.players.stream().anyMatch(p -> p.id.get()==(int)c.getUserData()))
			))
		);

		// layout
		root.getChildren().add(layAnchor(
			layHorizontally(20,CENTER_LEFT,
				ConfigEditor.create(Config.forProperty(GameMode.class, "Mode", mode)).buildNode().getChildren().get(0),
				new Icon(MaterialDesignIcon.NUMERIC_1_BOX_OUTLINE,15,"Start 1 player game",() -> game.start(1)),
				new Icon(MaterialDesignIcon.NUMERIC_2_BOX_OUTLINE,15,"Start 2 player game",() -> game.start(2)),
				new Icon(MaterialDesignIcon.NUMERIC_3_BOX_OUTLINE,15,"Start 3 player game",() -> game.start(3)),
				new Icon(MaterialDesignIcon.NUMERIC_4_BOX_OUTLINE,15,"Start 4 player game",() -> game.start(4)),
				new Icon(MaterialDesignIcon.NUMERIC_5_BOX_OUTLINE,15,"Start 5 player game",() -> game.start(5)),
				new Icon(MaterialDesignIcon.NUMERIC_6_BOX_OUTLINE,15,"Start 6 player game",() -> game.start(6)),
				new Icon(MaterialDesignIcon.NUMERIC_7_BOX_OUTLINE,15,"Start 7 player game",() -> game.start(7)),
				new Icon(MaterialDesignIcon.NUMERIC_8_BOX_OUTLINE,15,"Start 8 player game",() -> game.start(8)),
				new Icon(null,16){{ syncC(game.paused, it -> icon(it ? MaterialDesignIcon.PLAY : MaterialDesignIcon.PAUSE)); }}.action(() -> game.pause(!game.paused.get())),
				new Icon(FontAwesomeIcon.GEARS,14,"Settings").action(e -> APP.windowManager.showSettings(toConfigurableByReflect(this),e)),
				new Icon(FontAwesomeIcon.INFO,14,"How to play").action(() -> new HowToPane().show(game))
			),
			0d,0d,null,0d,
			layStack(canvas_bgr, canvas, playfield, playerStats, message),
			20d,0d,0d,0d
		));

		// keys
		playfield.addEventFilter(KEY_PRESSED, e -> {
			KeyCode cc = e.getCode();
			boolean first_time = game.pressedKeys.add(cc);
			if (first_time) {
				game.keyPressTimes.put(cc,game.loop.now);
				game.players.stream().filter(p -> p.alive).forEach(p -> {
					if (cc==p.keyAbility.getValue()) p.rocket.ability.onKeyPress();
					if (cc==p.keyFire.getValue()) p.rocket.gun.fire();
				});
				// cheats
				if (cc==DIGIT1) game.runNext.add(() -> repeat(5, i -> game.mission.spawnPlanetoid()));
				if (cc==DIGIT2) game.runNext.add(game.ufos::sendUfoSwarm);
				if (cc==DIGIT3) game.runNext.add(() -> repeat(5, i -> game.humans.sendSatellite()));
				if (cc==DIGIT4) game.runNext.add(() -> {
					game.oss.forEachT(Asteroid.class, a -> a.dead=true);
					game.handleEvent(Events.COMMAND_NEXT_MISSION);
				});
				if (cc==DIGIT5) game.players.stream().filter(p -> p.alive).forEach(p -> game.humans.send(p.rocket, SuperShield::new));
				if (cc==DIGIT6) game.oss.get(Rocket.class).forEach(r -> randOf(game.mode.enhancers()).enhance(r));
				if (cc==DIGIT7) game.entities.addForceField(new BlackHole(null, seconds(20),rand0N(game.field.width),rand0N(game.field.height)));
				if (cc==DIGIT8) game.start(2);
				if (cc==DIGIT9) game.runNext.add(game.ufos::sendUfoSquadron);
			}
		});
		playfield.addEventFilter(KEY_RELEASED, e -> {
			game.players.stream().filter(p -> p.alive).forEach(p -> {
				if (e.getCode()==p.keyAbility.getValue()) p.rocket.ability.onKeyRelease();
			});
			game.pressedKeys.remove(e.getCode());
		});
		playfield.setOnMouseClicked(e -> playfield.requestFocus());
		playfield.focusedProperty().addListener((o,ov,nv) -> game.pause(!nv));

		root.addEventHandler(Event.ANY, Event::consume);

		onClose.plusAssign(game::dispose);
	}

	interface Constants {
		double FPS = 60; // frames per second (locked)
		double FPS_KEY_PRESSED = 40; // frames per second
		double FPS_KEY_PRESSED_PERIOD = 1000 / FPS_KEY_PRESSED; // ms
		AbilityKind PLAYER_ABILITY_INITIAL = SHIELD; // rocket fire-fire time period
	}
	static class Settings {
		Duration PLAYER_GUN_RELOAD_TIME = millis(100); // default ability
		int PLAYER_LIVES_INITIAL = 5; // lives at the beginning of the game
		int PLAYER_SCORE_NEW_LIFE = 10000; // we need int since we make use of int division

		double SCORE_ASTEROID(Asteroid<?> a) { return 30 + 2000 / (4 * a.radius); }

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
		boolean playerGunDisabled = false;
		boolean deadly_bullets = false;
		boolean player_ability_auto_on = false;
		boolean playerNoKineticShield = false;

		double ROCKET_ENGINE_THRUST = 0.16; // px/s/frame
		double PULSE_ENGINE_PULSE_PERIOD_TTL = ttl(millis(20));
		double PULSE_ENGINE_PULSE_TTL = ttl(millis(400));
		double PULSE_ENGINE_PULSE_TTL1 = 1 / PULSE_ENGINE_PULSE_TTL; // saves us computation

		double KINETIC_SHIELD_INITIAL_ENERGY = 0.5; // 0-1 coefficient
		Duration KINETIC_SHIELD_RECHARGE_TIME = minutes(4);
		double ROCKET_KINETIC_SHIELD_RADIUS = 25; // px
		double ROCKET_KINETIC_SHIELD_ENERGY_MAX = 4000; // energy
		double KINETIC_SHIELD_LARGE_E_RATE = 50; // 50 times
		double KINETIC_SHIELD_LARGE_RADIUS_INC = 5; // by 5 px
		double KINETIC_SHIELD_LARGE_E_MAX_INC = 1; // by 100%
		double SHUTTLE_KINETIC_SHIELD_RADIUS = 180; // px
		double SHUTTLE_KINETIC_SHIELD_ENERGY_MAX = 1000000; // energy
		double SHIELD_E_ACTIVATION = 0; // energy
		double SHIELD_E_RATE = 20; // energy/frame
		Duration SHIELD_ACTIVATION_TIME = millis(0);
		Duration SHIELD_PASSIVATION_TIME = millis(0);
		double HYPERSPACE_E_ACTIVATION = 200; // energy
		double HYPERSPACE_E_RATE = 5; // energy/frame
		Duration HYPERSPACE_ACTIVATION_TIME = millis(200);
		Duration HYPERSPACE_PASSIVATION_TIME = millis(200);
		double DISRUPTOR_E_ACTIVATION = 0; // energy
		double DISRUPTOR_E_RATE = 6; // energy/frame
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
		double UFO_DISC_RADIUS = 4;
		double UFO_DISC_HIT_RADIUS = 10;
		int UFO_DISC_DECISION_TIME_TTL = (int) ttl(millis(500));
		double UFO_EXPLOSION_RADIUS = 100;
		double UFO_DISC_EXPLOSION_RADIUS = 8;

		double UFO_TTL() { return ttl(seconds(randMN(30, 60))); }

		double UFO_SWARM_TTL() { return ttl(seconds(randMN(60, 120))); }

		double UFO_DISC_SPAWN_TTL() { return ttl(seconds(randMN(80, 200))); }

		double SATELLITE_RADIUS = 15; // energy/frame
		double SATELLITE_SPEED = 200 / FPS; // ufo speed in px/s/fps

		double SATELLITE_TTL() { return ttl(seconds(randMN(25, 40))); }

		Image KINETIC_SHIELD_PIECE_GRAPHICS = graphics(MaterialDesignIcon.MINUS, 13, Color.AQUA, new DropShadow(GAUSSIAN, Color.DODGERBLUE.deriveColor(1, 1, 1, 0.6), 8, 0.3, 0, 0));
		double INKOID_SIZE_FACTOR = 50;
		double ENERG_SIZE_FACTOR = 50;
		double BLACK_HOLE_PARTICLES_MAX = 4000;

		boolean spawnAsteroids = true;
		boolean spawnSwarms = true;
		boolean useGrid = true;
		boolean voronoiDraw = false;
	}
	static class Colors {
		public Color main = Color.LIGHTGREEN;
		public Color canvasFade = rgb(0,31,41, 0.1);
		public Color humans = Color.DODGERBLUE;
		public Color humansTech = Color.AQUAMARINE;
		public Color ufos = rgb(114,208,74);
		public Color grid = Color.LIGHTGREEN;
		public Color hud = color(Color.AQUA, 0.25);

		public Colors interpolate(Colors from, Colors to, double v) {
			main = from.main.interpolate(to.main , v);
			canvasFade = from.canvasFade.interpolate(to.canvasFade, v);
			humans = from.humans.interpolate(to.humans, v);
			humansTech = from.humansTech.interpolate(to.humansTech, v);
			ufos = from.ufos.interpolate(to.ufos, v);
			grid = from.grid.interpolate(to.grid, v);
			hud = from.hud.interpolate(to.hud, v);
			return this;
		}
	}

	@IsConfig
	final V<Color> devCanvasFadeColor = new V<>(Color.BLACK).initAttachC(c -> game.colors.canvasFade = color(c, game.colors.canvasFade.getOpacity()));
	@IsConfig(info = "Clipped to min=0, max=0.1")
	final V<Double> devCanvasFadeOpacity = new V<>(0.05).initAttachC(c -> game.colors.canvasFade = color(game.colors.canvasFade, clip(0, c, 0.1)));
	@IsConfig(nullable = true)
	final V<Effect> devCanvasBgrEffect = new V<Effect>(new Glow(0.3)).initAttachC(e -> gc_bgr.getCanvas().setEffect(e));
	@IsConfig
	final V<PlayerSpawn> spawning = new V<>(PlayerSpawn.CIRCLE);
	final ObservableList<Integer> gamepadIds = FXCollections.observableArrayList();
	@IsConfig(name = "Players")
	final FixedConfList<Player> PLAYERS = new FixedConfList<>(Player.class, false,
		new Player(1, Color.CORNFLOWERBLUE, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(2, Color.GREY, KeyCode.M, KeyCode.UP, KeyCode.LEFT, KeyCode.RIGHT, KeyCode.N, PLAYER_ABILITY_INITIAL),
		new Player(3, Color.GREEN, KeyCode.T, KeyCode.G, KeyCode.F, KeyCode.H, KeyCode.R, PLAYER_ABILITY_INITIAL),
		new Player(4, Color.SANDYBROWN, KeyCode.I, KeyCode.K, KeyCode.J, KeyCode.L, KeyCode.U, PLAYER_ABILITY_INITIAL),
		new Player(5, Color.RED, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(6, Color.YELLOW, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(7, Color.CADETBLUE, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL),
		new Player(8, Color.MAGENTA, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D, KeyCode.Q, PLAYER_ABILITY_INITIAL)
	);
	@IsConfig
	final VarEnum<GameMode> mode = new VarEnum<>(
		new ClassicMode(game),
		new TimeTrial(game),
		new BounceHellMode(game),
		new AreaMode(game),
		new VoronoiMode(game)
	);

	Particle createRandomDisruptorParticle(double radiusMin, double radiusMax, SO ff) {
		return createRandomDisruptorParticle(radiusMin, radiusMax, ff, false);
	}
	Particle createRandomDisruptorParticle(double radiusMin, double radiusMax, SO ff, boolean noMove) {
		double angle = rand0N(D360);
		double dist = randMN(radiusMin,radiusMax);
		return new Particle(ff.x+dist*cos(angle),ff.y+dist*sin(angle), ff.dx,ff.dy, ttl(millis(350))) {
			@Override
			void draw() {
//				GraphicsContext g = gc_bgr;
//				g.setGlobalAlpha(ttl);
//				g.setFill(game.humans.color);
//				g.fillOval(x-0.5,y-0.5,1,1);
//				g.setGlobalAlpha(1);

				double opacity = ttl, x = this.x, y = this.y;
				drawFading(game, millis(200), ttl -> {
					gc.setGlobalAlpha(ttl*ttl*opacity);
					gc.setFill(game.colors.humans);
					gc.fillOval(x-0.5,y-0.5,1,1);
					gc.setGlobalAlpha(1);
				});
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
	/** Finds closest non-hyperspace rocket to the object. */
	Rocket findClosestRocketTo(SO to) {
		return game.oss.get(Rocket.class).stream()
			.filter(r -> !r.isHyperspace)
			.collect(minBy(to::distance))
			.orElse(null);
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

	/** Encompasses entire game. */
	class Game implements Play {
		final Comet owner = Comet.this;
		final V<Boolean> paused = new V<>(false);
		final V<Boolean> running = new V<>(false);
		private boolean isInitialized = false;

		Settings settings = new Settings();
		final Colors colors = new Colors();
		final ObservableSet<Player> players  = FXCollections.observableSet();
		final EntityManager entities = new EntityManager();
		final ObjectStore<PO> oss = new ObjectStore<>(o -> o.type);
		final Collection<PO> os = new ArrayDeque<>();
		final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
		final Map<KeyCode,Long> keyPressTimes = new HashMap<>();
		final GamepadDevices gamepads = new GamepadDevices() {
			private IControllerListener listener;

			@Override
			public void start() {
				gamepads.getControllers()
					.filter(g -> stream(players).noneMatch(p -> p.gamepadId.get()!=null && p.gamepadId.get()==g.getDeviceID()))
					.sorted(by(IController::getDeviceID))
					.forEach(g -> stream(players).sorted(by(p -> p.id.get()))
						              .filter(p -> p.gamepadId.get()==null).findFirst()
						              .ifPresent(p -> p.gamepadId.set(g.getDeviceID()))
					);
			}

			@Override
			protected void onInit(IController[] gamepads) {
				listener = new IControllerListener() {
					@Override
					public void connected(IController c) {
//						System.out.println("connected device:");
//						System.out.println("DeviceID = " + c.getDeviceID());
//						System.out.println("DeviceTypeIdentifier = " + c.getDeviceTypeIdentifier());
//						System.out.println("Description = " + c.getDescription());
//						System.out.println("ProductID = " + c.getProductID());
//						System.out.println("VendorID = " + c.getVendorID());

						gamepadIds.add(c.getDeviceID());
						stream(players).sorted(by(p -> p.id.get()))
							.filter(p -> p.gamepadId.get()==null).findFirst()
							.ifPresent(p -> p.gamepadId.set(c.getDeviceID()));
					}

					@Override
					public void disConnected(IController c) {
//						System.out.println("disconnected device:");
//						System.out.println("DeviceID = " + c.getDeviceID());
//						System.out.println("DeviceTypeIdentifier = " + c.getDeviceTypeIdentifier());
//						System.out.println("Description = " + c.getDescription());
//						System.out.println("ProductID = " + c.getProductID());
//						System.out.println("VendorID = " + c.getVendorID());

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
//						System.out.println("stick moved");
					}
				};
				Controllers.instance().addListener(listener);
				gamepadIds.addAll(stream(gamepads).map(IController::getDeviceID).collect(toList()));
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
							IButton engine3B = g.getButton(5);
							IButton fireB = g.getButton(0); // g.getButton(ButtonID.FACE_LEFT);
							IButton ability1B = g.getButton(4);
							IButton ability2B = g.getButton(11);
							IButton leftB = g.getButton(6);
							IButton rightB = g.getButton(7);
							boolean isEngine = (engine1B!=null && engine1B.isPressed()) || (engine2B!=null && engine2B.isPressed()) || (engine3B!=null && engine3B.isPressed());
							boolean isAbility = (ability1B!=null && ability1B.isPressed()) || (ability2B!=null && ability2B.isPressed());
							boolean isLeft = (leftB!=null && leftB.isPressed()) || g.getDpadDirection() == DpadDirection.LEFT;
							boolean isRight = (rightB!=null && rightB.isPressed()) || g.getDpadDirection()==DpadDirection.RIGHT;
							boolean isFire = fireB!=null && fireB.isPressed();
//							boolean isFireOnce = fireB!=null && fireB.isPressedOnce(); // !support multiple players per controller
							boolean isFireOnce = isFire && !p.wasGamepadFire;
							boolean isRightOnce = isRight && !p.wasGamepadRight;
							boolean isLeftOnce = isLeft && !p.wasGamepadLeft;
							p.wasGamepadLeft = isLeft;
							p.wasGamepadRight = isRight;
							p.wasGamepadFire = isFire;

							if (isLeftOnce) keyPressTimes.put(p.keyLeft.get(),loop.now);
							if (isRightOnce) keyPressTimes.put(p.keyRight.get(),loop.now);

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
		final CollisionHandlers collisionStrategies = new CollisionHandlers();

		GameMode mode;
		Grid grid;// = new Grid(gc_bgr, 1000, 500, 50, 50);
		Mission mission = null; // current mission, (they repeat), starts at 1, = mission % missions +1
		final StatsGame stats = new StatsGame();

		private final MotionBlur cee = new MotionBlur(0, 200);
		//		private final BoxBlur cee = new BoxBlur(100,2,2);
		private final Effect cee2 = new Bloom(0.3);
		private final TimeDouble cceStrengthW = new TimeDouble(0,0.01);
		private final TimeDouble cceStrengthH = new TimeDouble(0.5,0.01);

		final Voronoi voronoi = new Voronoi(
			(rocket,area) -> {
				rocket.voronoiArea = area;
				rocket.player.stats.controlAreaSize.accept(area);
			},
			(rocket,areaCenterDistance) -> {
				rocket.voronoiAreaCenterDistance = areaCenterDistance;
				rocket.player.stats.controlAreaCenterDistance.accept(areaCenterDistance);
			},
			(centerX,centerY) -> {
//				if (humans.intelOn.is()) {
//					// Nice, but ends up being distracting and poorly communicated to players, who then wonder wth this is
//					// gc_bgr.setFill(humans.color);
//					// gc_bgr.fillOval(centerX - 1, centerY - 1, 5, 5);
//				}
			},
			(edges) -> edges.forEach(edge -> {
				gc_bgr.save();
				gc_bgr.setLineWidth(1);
				gc_bgr.setStroke(game.colors.hud);
				gc_bgr.setGlobalAlpha(0.05);
				gc_bgr.strokeLine(edge.x1, edge.y1, edge.x2, edge.y2);
				gc_bgr.restore();

				double opacity = 0.2;
				drawFading(this, millis(200), ttl -> {
					gc.setLineWidth(1);
					gc.setStroke(game.colors.hud);
					gc.setGlobalAlpha(ttl*opacity);
					gc.strokeLine(edge.x1, edge.y1, edge.x2, edge.y2);
				});
			})
		);

		@Override
		public void init() {
			grid = new Grid(gc, game.field.width, game.field.height, 20);
			gamepads.init();
			Comet.this.mode.enumerateValues().forEach(Play::init);

			collisionStrategies.add(Rocket.class,Rocket.class, (r1,r2) -> {
				if (!r1.isHyperspace && !r2.isHyperspace && r1.isHitDistance(r2)) {
					if (r1.ability.isActiveOfType(Shield.class)) {
						((Shield) r1.ability).onHit(r2);
					} else {
						r1.player.die();
					}
					if (r2.ability.isActiveOfType(Shield.class)) {
						((Shield) r2.ability).onHit(r1);
					} else {
						r2.player.die();
					}
				}
			});
			collisionStrategies.add(Rocket.class,Satellite.class, (r, s) -> {
				 if ((!r.isHyperspace || r.ability.isActiveOfType(Hyperspace.class)) && r.isHitDistance(s)) {
					s.pickUpBy(r);
				}
			});
			collisionStrategies.add(Rocket.class,Ufo.class, (r, u) -> {
				if (!r.isHyperspace && r.isHitDistance(u)) {
					if (r.ability.isActiveOfType(Shield.class)) {
						((Shield)r.ability).onHit(u);
					} else {
						r.player.die();
					}
					u.die(r);
				}
			});
			collisionStrategies.add(Rocket.class,UfoSwarmer.class, (r, ud) -> {
				if (!r.isHyperspace && !ud.isInitialOutOfField && r.isHitDistance(ud)) {
					if (r.ability.isActiveOfType(Shield.class)) {
						((Shield)r.ability).onHit(ud);
					} else {
						r.player.die();
					}
					ud.explode();
				}
			});
			collisionStrategies.add(Rocket.class,Asteroid.class, (r, a) -> {
				if (!r.isHyperspace && r.isHitDistance(a)) {
					if (r.ability.isActiveOfType(Shield.class)) {
						((Shield)r.ability).onHit(a);
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
					game.handleEvent(Events.PLANETOID_DESTROYED);
					r.player.stats.accRamAsteroid();
				}
			});
			collisionStrategies.add(Shuttle.class,Asteroid.class, (s, a) -> {
				if (s.isHitDistance(a)) {
					a.onHit(s);
					game.handleEvent(Events.PLANETOID_DESTROYED);
					s.die(a);
				}
			});
			collisionStrategies.add(SuperShield.class,Asteroid.class, (s, a) -> {
				if (s.isHitDistance(a)) {
					if (s.kineticEto(a)<s.kinetic_shield.KSenergy) {
						s.kinetic_shield.onShieldHit(a);
					} else {
						s.die(a);
					}
					a.onHit(s);
					game.handleEvent(Events.PLANETOID_DESTROYED);
				}
			});
		}

		@Override
		public void start(int player_count) {
			stop();

			if (!isInitialized) {
				init();
				isInitialized = true;
			}

			players.addAll(listF(player_count,PLAYERS.getList()::get));
			players.forEach(Player::reset);
			gamepads.start();

			running.set(true);
			loop.reset();
			ufos.init();
			humans.init();

			mode = Comet.this.mode.get();
			mode.start(player_count);
			players.forEach(Player::spawn);
			loop.start();
			playfield.requestFocus();

			grid.enabled = settings.useGrid;
		}

		@Override
		public void pause(boolean v) {
			if (!running.get() || paused.get()==v) return;
			paused.set(v);
			if (v) loop.stop(); else loop.start();
			mode.pause(v);
		}

		@Override
		public void doLoop() {
			if (loop.isNth((long)FPS)) LOGGER.debug("particle.count= {}", oss.get(Particle.class).size());

			gamepads.doLoop();
			players.forEach(Player::doLoop);

			// remove inactive objects
			for (PO o : os) if (o.dead) removables.add(o);
			os.removeIf(o -> o.dead);
			for (PO o : oss.get(Particle.class)) if (o.dead) removables.add(o);
			oss.forEachSet(set -> set.removeIf(o -> o.dead));
			removables.forEach(PO::dispose);
			removables.clear();

			entities.addPending();
			entities.removePending();

			// reset gravity potentials
			oss.forEach(o -> o.g_potential = 1);

			// apply forces
			// recalculate gravity potentials
			forEachPair(entities.forceFields, os, ForceField::apply);
			forEachPair(filter(entities.forceFields, ff -> ff instanceof DisruptorField), oss.get(Particle.class), ForceField::apply);

			// canvas clearing
			gc_bgr.setFill(colors.canvasFade);
//			gc_bgr.setFill(color(rgb(0,0,0), 0.1));
			gc_bgr.fillRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
			gc.clearRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());

			grid.update();
			grid.draw();

			// update objects
			runNext.run();
			entities.forceFields.forEach(ForceField::doLoop);
			os.forEach(PO::doLoop);
			oss.get(Particle.class).forEach(Particle::doLoop);

			// collisions
			forEachPair(oss.get(Bullet.class),filter(os, e -> !(e instanceof Bullet)), Bullet::checkCollision);
			collisionStrategies.forEach(oss::forEachTE);

			entities.forceFields.forEach(ForceField::draw);
			os.forEach(PO::draw);
			oss.get(Bullet.class).forEach(Bullet::draw);

			boolean isHighDetails = true;
			if (isHighDetails) {
				if (cceStrengthW.value>1) cceStrengthW.value = 0;
				if (cceStrengthH.value>1) cceStrengthH.value = 0;
				cee.setAngle(cceStrengthH.getAndRun()*360);
				gc_bgr.applyEffect(cee);
				gc.applyEffect(cee2);

				oss.get(Bullet.class).forEach(Bullet::draw);
				os.forEach(PO::draw);
			}

			// non-interacting stuff last
			oss.get(Particle.class).forEach(Particle::draw);
			stream(oss.get(Particle.class)).filter(Draw2.class::isInstance).map(Draw2.class::cast).forEach(Draw2::drawBack);
			stream(oss.get(Particle.class)).filter(Draw2.class::isInstance).map(Draw2.class::cast).forEach(Draw2::drawFront);

			voronoi.compute(oss.get(Rocket.class), game.field.width, game.field.height, this);
			mode.doLoop();
		}

		@Override
		public void stop() {
			running.set(false);
			loop.stop();
			players.clear();
			os.clear();
			oss.clear();
			entities.clear();
			runNext.clear();
			playfield.getChildren().clear();
			if (mode!=null) mode.stop();
		}

		/** Clears resources. No game session will occur after this. */
		@Override
		public void dispose() {
			stop();
			gamepads.dispose();
			if (mode!=null) mode.dispose();
		}

		@Override
		public void handleEvent(Object event) {
			mode.handleEvent(event);
		}

		void over() {
			players.forEach(p -> p.stats.accGameEnd(loop.id));
			runNext.add(seconds(5), () -> {
				new EndGamePane().show(this);
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

		void placeholder(String text, PO o) {
			placeholder(text, o, 0, 0);
		}
		void placeholder(String text, double x, double y) {
			placeholder(text, null, x, y);
		}
		void placeholder(String text, PO o, double x, double y) {
			boolean isFollow = o!=null;
			double fW = computeFontWidth(FONT_PLACEHOLDER, text);
			double fH = computeFontHeight(FONT_PLACEHOLDER, text);
			game.runNext.addAnim01(seconds(2), p -> {
				double s = sqrt(map01To010(p, 0.9));
				double tx = game.field.modX(isFollow ? o.x-15 : x);
				double ty = game.field.modY(isFollow ? o.y-15 : y);

				Affine sa = new Affine();
				sa.append(new Scale(s,s, tx + fW/2,ty - fH/2));

				Affine a1 = gc.getTransform();
				Affine a2 = gc_bgr.getTransform();
				gc.setTransform(sa);
				gc.setFont(FONT_PLACEHOLDER);
				gc.setFill(game.colors.main);
				gc.setGlobalAlpha(1);
				gc.fillText(text, tx, ty);
				gc.setTransform(a1);
				gc_bgr.setTransform(sa);
				gc_bgr.setFont(FONT_PLACEHOLDER);
				gc_bgr.setFill(game.colors.main);
				gc_bgr.setGlobalAlpha(1);
				gc_bgr.fillText(text, tx, ty);
				gc_bgr.setTransform(a2);
			});
		}

		public void fillText(String text, double x, double y) {
			fillText(text, x, y, 1);
		}

		public void fillText(String text, double x, double y, double scale) {
			double fW = computeFontWidth(FONT_PLACEHOLDER, text);
			double fH = computeFontHeight(FONT_PLACEHOLDER, text);
			double tx = game.field.modX(x+15 - fW/2);
			double ty = game.field.modY(y-15 - fH/2);
			Affine sa = new Affine();
			sa.append(new Scale(scale,scale, tx + fW/2,ty - fH/2));

			Affine a1 = gc.getTransform();
			Affine a2 = gc_bgr.getTransform();
			gc.setTransform(sa);
			gc.setFont(FONT_PLACEHOLDER);
			gc.setFill(game.colors.main);
			gc.setGlobalAlpha(1);
			gc.fillText(text, tx, ty);
			gc.setTransform(a1);
			gc_bgr.setTransform(sa);
			gc_bgr.setFont(FONT_PLACEHOLDER);
			gc_bgr.setFill(game.colors.main);
			gc_bgr.setGlobalAlpha(1);
			gc_bgr.fillText(text, tx, ty);
			gc_bgr.setTransform(a2);
		}


		class PlayerFaction {
			final InEffect intelOn = new InEffect();
			boolean share_enhancers;

			void init() {
				share_enhancers = false;
				intelOn.reset();
			}

			void send(Rocket r, Consumer<? super Rocket> action) {
				game.runNext.add(seconds(2.0),() -> pulseCall(r));
				game.runNext.add(seconds(2.2),() -> pulseCall(r));
				game.runNext.add(seconds(2.4),() -> pulseCall(r));
				game.runNext.add(seconds(4.0),() -> pulseCall(r));
				game.runNext.add(seconds(4.2),() -> pulseCall(r));
				game.runNext.add(seconds(4.4),() -> pulseCall(r));
				game.runNext.add(seconds(6.0),() -> action.accept(r));
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
				if (humans.intelOn.is()) placeholder("Support", x,y);
				runNext.add(seconds(2), () -> {
					Satellite st = new Satellite(s);
					st.x = x;
					st.y = y;
					createHyperSpaceAnimIn(game, st);
				});
			}
			void sendSmallStationarySatellite() {
				double x = rand0N(game.field.width);
				double y = rand0N(game.field.height);
				if (humans.intelOn.is()) pulseAlert(x,y);
				if (humans.intelOn.is()) placeholder("Support", x,y);
				runNext.add(seconds(2), () -> {
					Satellite s = new Satellite(Side.LEFT).small();
					s.x = x;
					s.y = y;
					s.dx = s.dy = 0;
					createHyperSpaceAnimIn(game, s);
				});
			}

			void pulseCall(PO o) {
				new RadioWavePulse(o,2.5,colors.humans,false);
			}
			void pulseCall(double x, double y) {
				pulseCall(x,y,0,0);
			}
			void pulseAlert(PO o) {
				new RadioWavePulse(o, 2.5,colors.humans,false);
			}
			void pulseAlert(double x, double y) {
				pulseAlert(x,y,0,0);
			}
			void pulseCall(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,2.5,colors.humans,false);
			}
			void pulseAlert(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,-2.5,colors.humans,false);
			}
		}
		class UfoFaction {
			int losses = 0;
			int losses_aggressive = 5;
			int losses_cannon = 20;
			Rocket ufo_enemy = null;
			boolean aggressive = false;
			boolean canSpawnDiscs = false;
			private final double spawnEdgeOffsetX = 50;

			void init() {
				losses = 0;
				ufo_enemy = null;
				aggressive = false;
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
				repeat(count, () -> runNext.add(seconds(rand0N(0.5)), () -> sendUfo(side)));
			}
			void sendUfoSwarm() {
				ufo_enemy = players.isEmpty() ? null : randOf(players).rocket;
				Side side = randEnum(Side.class);
				int count = (int)(2+rand01()*8);
				double w = side==Side.LEFT ? spawnEdgeOffsetX : game.field.width-spawnEdgeOffsetX;
				double h = rand0N(game.field.height);
				double d = side==Side.LEFT ? D0 : D180;
				if (humans.intelOn.is()) pulseAlert(w, h);
				if (humans.intelOn.is()) placeholder("Ufo", w,h);
				int swarmId = randInt(Integer.MAX_VALUE);
				runNext.add(seconds(2), () -> {
					if (randBoolean()) {
						double by = side == Side.LEFT ? -15 : 15;
						forEachInLineBy(w, h, by, by, 8 * count, (x, y) -> new UfoSwarmer(x, game.field.modY(y), d)).forEach(u -> {
							u.isActive = false;
							u.isInitialOutOfField = true;
							u.swarmId = swarmId;
						});
					} else
						forEachOnCircleBy(w, h, 15, 8 * count, (x, y, a) -> new UfoSwarmer(x, game.field.modY(y), d)).forEach(u -> {
								u.isActive = false;
								u.isInitialOutOfField = true;
								u.swarmId = swarmId;
							});
				});
			}
			private void sendUfo(Side side) {
				Side s = side==null ? randEnum(Side.class) : side;
				double x = s==Side.LEFT ? spawnEdgeOffsetX : game.field.width-spawnEdgeOffsetX;
				double y = rand0N(game.field.height);
				if (humans.intelOn.is()) pulseAlert(x,y);
				if (humans.intelOn.is()) placeholder("Ufo", x,y);
				runNext.add(seconds(2), () -> {
					Ufo u = new Ufo(s,aggressive);
					u.x = x;
					u.y = y;
					createHyperSpaceAnimIn(game, u);
				} );
			}

			void pulseCall(PO o) {
				pulseCall(o.x,o.y,o.dx,o.dy);
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
				new RadioWavePulse(x,y,dx,dy,-2.5,colors.ufos,true);
			}
			void pulseAlert(double x, double y, double dx, double dy) {
				new RadioWavePulse(x,y,dx,dy,-2.5,colors.ufos,true);
			}
		}
		class Mission {
			final int id;
			final String name, scale, details;
			final Colors colors = new Colors();
			final F5<Double,Double,Double,Double,Double,Asteroid<?>> planetoidConstructor;
			Consumer<Game> initializer = game -> {};
			Consumer<Game> disposer = game -> {};

			public Mission(int ID, String NAME, String SCALE, String DETAILS, Color COLOR, Color CANVAS_REDRAW,
					F5<Double,Double,Double,Double,Double,Asteroid<?>> planetoidFactory) {
				id = ID;
				name = NAME; scale = SCALE; details = DETAILS;
				planetoidConstructor = planetoidFactory;
				colors.main = COLOR==null ? Color.TRANSPARENT : COLOR;
				colors.canvasFade = CANVAS_REDRAW==null ? Color.TRANSPARENT : CANVAS_REDRAW;
				colors.humans = colors.main;
				colors.humansTech = colors.main;
				colors.ufos = colors.main;
				colors.grid = colors.main;
			}

			Mission initializer(Consumer<Game> INITIALIZER, Consumer<Game> DISPOSER) {
				initializer = INITIALIZER;
				disposer = DISPOSER;
				return this;
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
			final Set<ForceField> forceFieldsToAdd = new HashSet<>();  // stores new entities that will be added on next loop start

			void addPending() {
				forceFields.addAll(forceFieldsToAdd);
				forceFieldsToAdd.clear();
			}

			void removePending() {
				forceFields.removeIf(ff -> ff.dead);
			}

			void addForceField(ForceField ff) {
				forceFieldsToAdd.add(ff);
				ff.dead = false;
			}

			void removeForceField(ForceField ff) {
				ff.dead = true;
			}

			void clear() {
				forceFields.clear();
				forceFieldsToAdd.clear();
			}
		}
		/**
		 * A rocket ability. Enhances player's rocket in some way.
		 */
		public class Enhancer extends Displayable {
			final Duration duration;
			final Consumer<Rocket> starter;
			final Consumer<Rocket> stopper;
			final boolean isShareable;

			public Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, Consumer<Rocket> STOPPER, boolean IS_SHAREABLE, CharSequence... DESCRIPTION) {
				super(NAME, ICON, DESCRIPTION);
				duration = DURATION;
				starter = STARTER;
				stopper = STOPPER;
				isShareable = IS_SHAREABLE;
			}

			public Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, Consumer<Rocket> STOPPER, CharSequence... DESCRIPTION) {
				this(NAME, ICON, DURATION, STARTER, STOPPER, true, DESCRIPTION);
			}

			public Enhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, CharSequence... DESCRIPTION) {
				this(NAME, ICON, DURATION, STARTER, r -> {}, DESCRIPTION);
			}

			void enhance(Rocket r) {
				start(r);
				Game.this.runNext.add(duration,() -> stop(r));
			}

			void start(Rocket r) {
				if (isShareable && Game.this.humans.share_enhancers) {
					Game.this.oss.get(Rocket.class).forEach(starter);
					Game.this.oss.get(Rocket.class).forEach(rk -> new EIndicator(rk, this));
				} else {
					starter.accept(r);
					new EIndicator(r,this);
				}
			}

			void stop(Rocket r) {
				if (isShareable && Game.this.humans.share_enhancers) Game.this.oss.get(Rocket.class).forEach(stopper);
				else stopper.accept(r);
			}
		}
	}

	/** Game player. Survives game sessions. */
	public class Player implements LO, ConfigurableByReflect {
		@IsConfig(editable = EditMode.APP, nullable = true) public final V<Integer> id = new V<>(null);
		@IsConfig public final V<String> name = new V<>("");
		@IsConfig public final V<Color> color = new V<>(Color.WHITE);
		@IsConfig public final V<KeyCode> keyFire = new V<>(KeyCode.W);
		@IsConfig public final V<KeyCode> keyThrust = new V<>(KeyCode.S);
		@IsConfig public final V<KeyCode> keyLeft = new V<>(KeyCode.A);
		@IsConfig public final V<KeyCode> keyRight = new V<>(KeyCode.D);
		@IsConfig public final V<KeyCode> keyAbility = new V<>(KeyCode.Q);
		@IsConfig public final V<AbilityKind> ability_type = new V<>(AbilityKind.SHIELD);

		@IsConfig(nullable = true) final VarEnum<Integer> gamepadId = new VarEnum<Integer>(null, gamepadIds);
		boolean isInputLeft, isInputRight, isInputFire, isInputFireOnce, isInputThrust, isInputAbility;
		boolean wasInputLeft, wasInputRight, wasInputFire, wasInputFireOnce, wasInputThrust, wasInputAbility;
		boolean wasGamepadLeft, wasGamepadRight, wasGamepadFire;
		public boolean alive = false;
		public final V<Integer> lives = new V<>(game.settings.PLAYER_LIVES_INITIAL);
		public final V<Integer> score = new V<>(0);
		public final V<Double> energy = new V<>(0d);
		public Rocket rocket;
		public final StatsPlayer stats = new StatsPlayer();
		private final long hudUpdateFrequency = (long) ttl(millis(200));

		public Player(int ID, Color COLOR, KeyCode kFire, KeyCode kThrust, KeyCode kLeft, KeyCode kRight, KeyCode kAbility, AbilityKind ABILITY) {
			id.set(ID);
			name.set("Player " + ID);
			color.set(COLOR);
			ability_type.set(ABILITY);
			keyFire.set(kFire);
			keyThrust.set(kThrust);
			keyLeft.set(kLeft);
			keyRight.set(kRight);
			keyAbility.set(kAbility);
			ability_type.attachC(v -> {
				if (rocket!=null) rocket.changeAbility(v);
			});
			score.attachChangesC((os, ns) -> {
				if (os/game.settings.PLAYER_SCORE_NEW_LIFE<ns/game.settings.PLAYER_SCORE_NEW_LIFE)
					lives.setValueOf(l -> l+1);
			});
		}

		@Override
		public void doLoop() {
			if (rocket!=null && game.loop.isNth(hudUpdateFrequency))
				energy.set(rocket.energy);

			if (alive) {
				isInputLeft |= game.pressedKeys.contains(keyLeft.get());
				isInputRight |= game.pressedKeys.contains(keyRight.get());
				isInputThrust |= game.pressedKeys.contains(keyThrust.get());
				isInputFire |= game.pressedKeys.contains(keyFire.get());
				isInputAbility |= game.pressedKeys.contains(keyAbility.get());
			}
			doInputs();
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
				game.handleEvent(Events.PLAYER_NO_LIVES_LEFT);
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
			rocket.energy = game.settings.PLAYER_ENERGY_INITIAL;
			rocket.engine.enabled = false; // cant use engine.off() as it could produce unwanted behavior
			rocket.voronoiArea = null;
			rocket.voronoiAreaCenterDistance = null;
			rocket.changeAbility(ability_type.get());
			if (!game.settings.playerNoKineticShield) game.new Enhancer("Super shield", FontAwesomeIcon.SUN_ALT, seconds(5), r -> r.kinetic_shield.large.inc().inc(), r -> r.kinetic_shield.large.dec().dec(), "").enhance(rocket);
			createHyperSpaceAnimIn(game, rocket);
		}

		void spawnMidGame() {
			game.runNext.add(game.settings.PLAYER_RESPAWN_TIME.divide(2), () -> new SuperDisruptor(
				spawning.get().computeStartingX(game.field.width,game.field.height,game.players.size(),id.get()),
				spawning.get().computeStartingY(game.field.width,game.field.height,game.players.size(),id.get())
			));
			game.runNext.add(game.settings.PLAYER_RESPAWN_TIME, this::spawn);
			// Avoid instant-death leading to possible instant-game-over
			game.runNext.add(game.settings.PLAYER_RESPAWN_TIME, () -> game.oss.forEach(o -> {
				if (!(o instanceof Particle) && !(o instanceof Rocket) && rocket.isHitDistance(o))
					o.dead = true;
			}));
		}

		void reset() {
			alive = false;
			score.setValue(0);
			lives.setValue(game.settings.PLAYER_LIVES_INITIAL);
			if (rocket!=null) rocket.dead = true; // just in case
			rocket = null;
			isInputLeft = isInputRight = isInputFire = isInputFireOnce = isInputThrust = isInputAbility =
			wasInputLeft = wasInputRight = wasInputFire = wasInputFireOnce = wasInputThrust = wasInputAbility =
			wasGamepadLeft = wasGamepadRight = wasGamepadFire = false;
		}

		void doInputs() {
			if (alive) {
				long now = game.loop.now;
				if (isInputLeft) rocket.direction -= computeRotSpeed(now-game.keyPressTimes.getOrDefault(keyLeft.get(), 0L));
				if (isInputRight) rocket.direction += computeRotSpeed(now-game.keyPressTimes.getOrDefault(keyRight.get(), 0L));
				if (isInputThrust) rocket.engine.on(); else rocket.engine.off();
				if (isInputLeft && isInputRight) rocket.ddirection = 0;
				if (isInputFireOnce || (game.loop.isNth(3) && rocket.rapidFire.is() && isInputFire)) rocket.gun.fire();
				if (isInputAbility || game.settings.player_ability_auto_on) rocket.ability.activate(); else rocket.ability.passivate();
			}
			wasInputLeft = isInputLeft;
			wasInputRight = isInputRight;
			wasInputThrust = isInputThrust;
			wasInputAbility = isInputAbility;
			wasInputFire = isInputFire;
			wasInputFireOnce = isInputFireOnce;
			isInputLeft = isInputRight = isInputFire = isInputFireOnce = isInputThrust = isInputAbility = false;
		}

		double computeRotSpeed(long pressedMsAgo) {
			// Shooting at long distance becomes hard due to 'smallest rotation angle' being too big so
			// we slow down rotation within the first ROT_LIMIT ms after key press and reduce rotation
			// limit temporarily without decreasing maneuverability.
			double r = pressedMsAgo<game.settings.ROT_LIMIT
				? game.settings.ROTATION_SPEED*clip(0.1,sqr(pressedMsAgo/game.settings.ROT_LIMIT),1)
				: game.settings.ROTATION_SPEED;
			return rocket.engine.mobility.value()*r;
		}

	}

	private abstract class SO implements LO {
		double x = 0;
		double y = 0;
		double dx = 0;
		double dy = 0;
		boolean isHyperspace = false; // other dimension, implies near-zero interactivity
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
		Class<?> type;
		Draw graphics;
		double graphicsDir = 0;
		double graphicsScale = 1;
		Set<LO> children = null;
		double direction = -D90;
		double ddirection = 0;

		PO(Class<?> TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Image GRAPHICS) {
			type = TYPE;
			x = X; y = Y; dx = DX; dy = DY;
			radius = HIT_RADIUS;
			mass = 2*HIT_RADIUS*HIT_RADIUS; // 4/3d*PI*HIT_RADIUS*HIT_RADIUS*HIT_RADIUS;
			graphics = GRAPHICS==null ? null : new Draw(GRAPHICS);
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
			dx *= game.settings.RESISTANCE;
			dy *= game.settings.RESISTANCE;
			ddirection *= game.settings.RESISTANCE;
			direction += ddirection;
		}

		@Override void draw() {
			if (graphics!=null) {
				double scale = graphicsScale*(clip(0.7,20*g_potential,1));
				graphics.draw(gc, x, y, scale, graphicsDir);
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
			boolean forceOff = false;
			final InEffectValue<Double> mobility = new InEffectValue<>(times -> pow(game.settings.BONUS_MOBILITY_MULTIPLIER,times));

			final void on() {
				if (!enabled && !forceOff) {
					enabled = true;
					onOn();
				}
			}

			final void off() {
				if (enabled || forceOff) {
					enabled = false;
					onOff();
				}
			}
			void onOn(){}

			void onOff(){}

			void doLoop(){
				if (enabled && !forceOff) {
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
		Ability ability;
		KineticShield kinetic_shield = null;

		// to avoid multiple calculations/loop
		double cache_speed; // sqrt(dx^2 + dy^2)
		double cosdir = 0; // cos(dir)
		double sindir = 0; // sin(dir)
		double dx_old = 0; // allows calculating ddx (2nd derivation - acceleration)
		double dy_old = 0;

		public Ship(Class<?> TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Image GRAPHICS, double E, double dE) {
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
			if (gun!=null) gun.doLoop();
		}

		class Gun {
			final GunControl control;
			final F0<Double> aimer; // determines bullet direction
			final F1<Double,Bullet> ammo_type; // bullet factory
			final InEffectValue<Double[]> turrets = new InEffectValue<>(1,
                       count -> calculateGunTurretAngles(count, game.settings.ROCKET_GUN_TURRET_ANGLE_GAP));
			final InEffect blackhole = new InEffect();
			final TimeDouble reload;

			public Gun(GunControl CONTROL, Duration reloadTime, F0<Double> AIMER, F1<Double,Bullet> AMMO_TYPE) {
				control = CONTROL;
				reload = new TimeDouble(0, 1, reloadTime).periodic();
				aimer = AIMER;
				ammo_type = AMMO_TYPE;
			}

			public void doLoop() {
				boolean loaded = reload.run();
				if (loaded && gun.control == AUTO)
					game.runNext.add(() -> ammo_type.apply(aimer.apply()));
			}

			void fire() {
				if (!isHyperspace) {
					if (Ship.this instanceof Rocket) {
						((Rocket) Ship.this).player.stats.accFiredBullet(game.loop.id);
						if (game.settings.playerGunDisabled) return;
					}

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
			double thrust = game.settings.ROCKET_ENGINE_THRUST;

			double stage2ThrustMultiplier = 1.5;
			double stage2TimeMin = ttl(seconds(5));
			double stage2TimeCurrent = 0;
			double stage2TimeBy = 1;

			@Override
			void doLoop() {
				super.doLoop();
				if (enabled) stage2TimeCurrent += stage2TimeBy;
				else stage2TimeCurrent = 0;
			}

			@Override
			void onDoLoop() {
				boolean isStage2 = stage2TimeCurrent>stage2TimeMin;
				boolean wasStage2 = stage2TimeCurrent-stage2TimeBy>stage2TimeMin;
				boolean wasStage2Activated = isStage2 && !wasStage2;
				if (wasStage2Activated) {
					dx += 200/FPS*cos(direction);
					dy += 200/FPS*sin(direction);
					ddirection /= 2;    // TODO: decrease max rotation speed instead
					repeat(20, () -> new Particle(x,y,cos(randAngleRad()),sin(randAngleRad()),ttl(millis(400))));

					double d = direction+PI;
					double dr = deg(Ship.this.direction+PI/2);
					new Particle(x+25*cos(d),y+25*sin(d),1*cos(d),1*sin(d),ttl(millis(400))) {
						@Override
						void draw() {
							gc.save();
							gc.setStroke(game.colors.humans);
							gc.setLineWidth(1+15*ttl);
							gc.setGlobalAlpha(0.1+0.5*ttl);
							double w = 30+180*(1-ttl), h=10+20*(1-ttl);
							Affine a = comet.Utils.rotate(gc, dr,x,y);
							gc.strokeOval(x-w/2,y-h/2,w,h);
							gc.setTransform(a);
							gc.restore();
						}
					};
				}

				boolean isRocket = Ship.this instanceof Rocket;
				boolean isLeft = isRocket && ((Rocket)Ship.this).player.wasInputLeft;
				boolean isRight = isRocket && ((Rocket)Ship.this).player.wasInputRight;
				if (!(isLeft && isRight) || isStage2) {
					double acc = mobility.value() * thrust;
					if (isStage2) acc *= stage2ThrustMultiplier;
					dx += acc * cos(direction);
					dy += acc * sin(direction);
				}

				if (!isHyperspace) {
					ttl--;

					// style 1
					// gc_bgr.setFill(game.humans.color);
					// drawOval(gc_bgr,x,y,3);

					if (ttl<0) {
						ttl = ttl(millis(90));

						// style 2
//						ROCKET_ENGINE_DEBRIS_EMITTER.emit(x,y,direction+PI, mobility.value());

						// style 3
						double d = direction+PI;
						double dr = deg(Ship.this.direction+PI/2);
						new Particle(x+25*cos(d),y+25*sin(d),1*cos(d),1*sin(d),ttl(millis(250))) {
							@Override
							void draw() {
								gc.save();
								gc.setStroke(game.colors.humans);
								gc.setLineWidth(1+3*ttl);
								gc.setGlobalAlpha(0.1+0.5*ttl);
								double w = 10+30*(1-ttl), h=5+10*(1-ttl);
								Affine a = comet.Utils.rotate(gc, dr,this.x,this.y);
								gc.strokeOval(x-w/2,y-h/2,w,h);
								gc.setTransform(a);
								gc.restore();
							}
						};
					}
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
				if (ability ==this) ability = null;
				children.remove(this);
			}

			<T extends Ability> boolean isActiveOfType(Class<T> type) {
				return type.isInstance(this) && isActivated();
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
				} else if (state==PASSSIVATING) {
					activation = timePassivation==0 ? 0 : max(0,activation-1/(timePassivation*FPS));
					if (activation==0) {
						state = OFF;
						onPassivateEnd();
					}
					onActiveChanged(activation);
				} else if (activation==0) {
					onPassive();
				} else if (activation==1) {
					onActive();
				}

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
			final double outboundForceCoefficient = 0.4;

			Disruptor() {
				super(
					true,
					game.settings.DISRUPTOR_ACTIVATION_TIME,
					game.settings.DISRUPTOR_PASSIVATION_TIME,
					game.settings.DISRUPTOR_E_ACTIVATION,
					game.settings.DISRUPTOR_E_RATE
				);
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
					if (isHyperspace || o.isHyperspace) return;

					double distX = game.field.distXSigned(x,o.x);
					double distY = game.field.distYSigned(y,o.y);
					double dist = game.field.dist(distX,distY)+1; // +1 avoids /0 " + dist);
					double f = force(o.mass,dist);
					boolean hasNoEffect = false;

					// disrupt ufo bullets
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
					if (o instanceof Rocket && ((Rocket)o).ability.isActiveOfType(Shield.class)) {
						f *= -3;
					} else
					if (o instanceof Satellite || o instanceof Shuttle || o instanceof SuperShield || o instanceof SuperDisruptor) {
						hasNoEffect = true;
					}

					// apply force
					if (hasNoEffect) return;
					boolean isOutboundX = sign(dx-o.dx)==sign(distX);
					double outboundSofteningX = isOutboundX ? outboundForceCoefficient : 1;
					boolean isOutboundY = sign(dy-o.dy)==sign(distY);
					double outboundSofteningY = isOutboundY ? outboundForceCoefficient : 1;
					o.dx += distX*f/dist*outboundSofteningX;
					o.dy += distY*f/dist*outboundSofteningY;
				}

				@Override
				public void doLoop() {
					this.x = Ship.this.x;
					this.y = Ship.this.y;
					this.isHyperspace = Ship.this.isHyperspace; // must always match

					if (activation==1 && !isHyperspace) {
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
					false,
					game.settings.HYPERSPACE_ACTIVATION_TIME,
					game.settings.HYPERSPACE_PASSIVATION_TIME,
					game.settings.HYPERSPACE_E_ACTIVATION,
					game.settings.HYPERSPACE_E_RATE,
					graphics(MaterialDesignIcon.PLUS, 30, game.colors.humans, null)
				);
				graphicsAScale = 0;
			}

			void onActivateStart() {
				isHyperspace = true;
				game.grid.applyExplosiveForce(5*90f, new Vec(x,y), 70);
			}
			void onPassivateStart() {
				game.grid.applyExplosiveForce(5*90f, new Vec(x,y), 70);
			}
			void onPassivateEnd() {
				isHyperspace = false;
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
				super(
					true,
					game.settings.SHIELD_ACTIVATION_TIME,
					game.settings.SHIELD_PASSIVATION_TIME,
					game.settings.SHIELD_E_ACTIVATION,
					game.settings.SHIELD_E_RATE
				);
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
			void onHit(PO a) {
				// Makes for unbalanced game mechanics, because it is too predictable.
				// Instead drain energy constantly while active. This makes for killer suspense when player holds
				// shield active for a prolonged amount of time due to fear of death only to drain its power in vain.
				// energy -= min(energy,kineticEto(a));

//				double dir = dir(a);
//				double dxΔ = dx-a.dx;
//				double dyΔ = dy-a.dy;
//				double speed = a.speed();
//				dx = 5*cos(dir-PI);
//				dy = 5*sin(dir-PI);
//				ddirection += randOf(-1, 1) * randMN(0.03,0.05);
//				engine.off();
//				engine.forceOff = true;
//				game.runNext.add(millis(300), () -> engine.forceOff = false);


				double dir = direction+PI;
				dx = 5*cos(dir);
				dy = 5*sin(dir);
				ddirection += randOf(-1, 1) * randMN(0.03,0.05);
				engine.off();
				engine.forceOff = true;
				game.runNext.add(millis(300), () -> engine.forceOff = false);
				gc_bgr.setFill(color(game.colors.humans, 0.8));
				drawOval(gc_bgr, x,y,kinetic_shield.KSradius);
			}

		}
		class KineticShield extends Ability {
			double KSenergy_maxInit;
			double KSenergy_min;
			double KSenergy_max;
			double KSenergy;
			double KSenergy_rateInit;
			double KSenergy_rate;
			double KSradiusInit;
			double KSradius;
			int pieces; // needs update when KSradius changes
			double piece_angle; // same
			final Runnable activationRun = this::showActivation;
			final LO KSemitter = new ShieldPulseEmitter(); // emits radial waves
			final boolean draw_ring;

			// large KS ability
			final InEffect large = new InEffect(times -> {
				KSenergy_rate = (times>0 ? game.settings.KINETIC_SHIELD_LARGE_E_RATE : 1)*KSenergy_rateInit;
				KSradius = KSradiusInit + game.settings.KINETIC_SHIELD_LARGE_RADIUS_INC*times;
				KSenergy_max = KSenergy_maxInit*(1 + game.settings.KINETIC_SHIELD_LARGE_E_MAX_INC*times);
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
				KSenergy_rateInit = KSenergy_max/ttl(game.settings.KINETIC_SHIELD_RECHARGE_TIME);
				KSenergy_rate = KSenergy_rateInit;
				KSenergy = game.settings.KINETIC_SHIELD_INITIAL_ENERGY*KSenergy_max;
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
				if (KSemitter !=null) KSemitter.doLoop();

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

				draw();
				double tmpX = x, tmpY = y;
				x=tmpX+game.field.width; y=tmpY+0;
				draw();
				x=tmpX+game.field.width; y=tmpY+game.field.height;
				draw();
				x=tmpX; y=tmpY+game.field.height;
				draw();
				x=tmpX-game.field.width; y=tmpY+game.field.height;
				draw();
				x=tmpX-game.field.width; y=tmpY;
				draw();
				x=tmpX-game.field.width; y=tmpY-game.field.height;
				draw();
				x=tmpX; y=tmpY-game.field.height;
				draw();
				x=tmpX+game.field.width; y=tmpY-game.field.height;
				draw();
				x = tmpX; y = tmpY;
			}
			void draw() {
				double r = graphicsScale*KSradius;
				double sync_gap = 1;
				int syncs_amount = (int) (2*r/sync_gap);
				double syncs_angle = D360/syncs_amount;
				int sync_index_real = sync_index * 50/((int)KSradius-20); // adjusts speed (slow down)
				double energy_fullness = KSenergy/KSenergy_max;
				// 1% margin enables 'always full' shields by using high energy
				// accumulation rate. The short lived shield damage will thus be transparent.
				boolean energy_full = energy_fullness>0.99;
				gc.setGlobalAlpha(energy_fullness);
				gc.setStroke(COLOR_DB);
				gc.setLineWidth(2);
				for (int i=0; i<syncs_amount; i++) {
					double angle = direction+i*syncs_angle;
					double acos = cos(angle);
					double asin = sin(angle);
					double alen = 0.3*r*syncs[floorMod(i*syncs_len/syncs_amount+sync_index_real,syncs_len)];
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
				scheduleActivation();
			}
			void changeKSenergyToMax(){
				if (KSenergy<KSenergy_max) {
					KSenergy = KSenergy_max;
					showActivation();
				}
			}
			void changeKSenergyToMin(){
				if (KSenergy>KSenergy_min) {
					KSenergy = KSenergy_min;
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
					drawImageRotated(gc, game.settings.KINETIC_SHIELD_PIECE_GRAPHICS, deg(D90+KSPdir), game.field.modX(KSPx+x), game.field.modY(KSPy+y));
					gc.setGlobalAlpha(1);

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
					if (ability ==this) ability = null;
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
				drawHudLine(gc, game.field, x,y, 40, r.bulletRange, cosdir, sindir, game.colors.hud);
				// drawHudCircle(gc, game.field, x,y,r.bulletRange, HUD_COLOR); // nah drawing ranges is more cool
				drawHudCircle(gc, game.field, x,y,r.bulletRange,r.direction,D30, game.colors.hud);
				drawHudCircle(gc, game.field, x,y,r.bulletRange,r.direction+D360/3,PI/8, game.colors.hud);
				drawHudCircle(gc, game.field, x,y,r.bulletRange,r.direction-D360/3,PI/8, game.colors.hud);
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
		Double voronoiArea = null;
		Double voronoiAreaCenterDistance = null;

		Rocket(Player PLAYER) {
			super(
				Rocket.class,
				game.field.width/2,game.field.height/2,0,0,game.settings.PLAYER_HIT_RADIUS,
				graphics(MaterialDesignIcon.ROCKET,34,PLAYER.color.get(),null),
				game.settings.PLAYER_ENERGY_INITIAL,game.settings.PLAYER_E_BUILDUP
			);
			player = PLAYER;
			kinetic_shield = game.settings.playerNoKineticShield
				? null
				: new KineticShield(game.settings.ROCKET_KINETIC_SHIELD_RADIUS,game.settings.ROCKET_KINETIC_SHIELD_ENERGY_MAX);
			engine = new RocketEngine();
			gun = new Gun(
				MANUAL,
				game.settings.PLAYER_GUN_RELOAD_TIME,
				() -> direction,
				dir -> splitFire.is()
					? new SplitBullet(
							this,
							x + game.settings.PLAYER_BULLET_OFFSET*cos(dir),
							y + game.settings.PLAYER_BULLET_OFFSET*sin(dir),
							dx + powerFire.value()*cos(dir)*game.settings.PLAYER_BULLET_SPEED,
							dy + powerFire.value()*sin(dir)*game.settings.PLAYER_BULLET_SPEED,
							0,
							game.settings.PLAYER_BULLET_TTL
						)
					: new Bullet(
							this,
							x + game.settings.PLAYER_BULLET_OFFSET*cos(dir),
							y + game.settings.PLAYER_BULLET_OFFSET*sin(dir),
							dx + powerFire.value()*cos(dir)*game.settings.PLAYER_BULLET_SPEED,
							dy + powerFire.value()*sin(dir)*game.settings.PLAYER_BULLET_SPEED,
							0,
							game.settings.PLAYER_BULLET_TTL
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
			graphicsDir = D45 + direction; // add 45 degrees due to the graphics

			// Draw custom graphics
			// super.draw();

			Color c = player.color.get(); // game.humans.color
			double scale = graphicsScale*(clip(0.7,20*g_potential,1));
			gc.setFill(c);
			gc.setStroke(c);
			gc.setGlobalAlpha(1);
			drawTriangle(gc, x,y,scale*15, direction, 3*PI/4);

			// draw speed effect
			if (!isHyperspace) {
				double opacityMax = mapTo01(cache_speed, 1, 10);
				double x = this.x, y = this.y;
				drawFading(game, ttl -> {
					gc.setFill(c);
					gc.setGlobalAlpha(opacityMax*ttl*ttl);
					drawOval(gc, x, y, graphicsScale*8);
					gc.setGlobalAlpha(1);
				});
			}

			if (game.humans.intelOn.is() && bulletRange<game.field.diagonal) {
				// drawHudCircle(x,y,bulletRange,game.colorHud);
				drawHudCircle(gc, game.field, x,y,bulletRange,direction,D30, game.colors.hud);
				drawHudCircle(gc, game.field, x,y,bulletRange,direction+D360/3,PI/8, game.colors.hud);
				drawHudCircle(gc, game.field, x,y,bulletRange,direction-D360/3,PI/8, game.colors.hud);
			}

			if (gun.blackhole.is()) {
				gc.setFill(Color.BLACK);
				drawHudCircle(gc, game.field, x+bulletRange*cos(direction),y+bulletRange*sin(direction), 50, game.colors.hud);
			}

			// rocket-rocket 'quark entanglement' formation force
			// Repels at short distance, pulls at long distance.
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
			if (ability !=null) ability.dispose();
			children.remove(ability);
			ability = type.create(this);
			children.add(ability);
		}

		@Override
		boolean isHitDistance(SO o) {
			if (o instanceof Bullet && kinetic_shield!=null)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			if (o instanceof Asteroid && kinetic_shield!=null && kineticEto(o)<kinetic_shield.KSenergy)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			return isDistanceLess(o,radius+o.radius);
		}

		double computeBulletRange() {
			return bulletRange = powerFire.value()*game.settings.PLAYER_BULLET_RANGE;
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
			if (game.settings.spawnSwarms && game.ufos.canSpawnDiscs) {
				game.ufos.canSpawnDiscs = false;
				double spawnX = x, spawnY = y;
				radio.run();
				game.runNext.add(millis(500), () -> repeat(5, i -> new UfoSwarmer(x, y, i*D360/5)));
			}
		};
		public final TimeDouble gunAngleBase = new TimeDouble(0, D120);
		public final TimeDouble gunAngle = new TimeDouble(0, comet.Utils.ttlVal(D360, seconds(2)));


//		boolean hasSwarmers = randBoolean();
		boolean hasSwarmers = true;
		boolean isSwarmActive = randBoolean();
		double swarmA = 0, dswarmA = D360/1/FPS;

		double discpos = 0; // 0-1, 0=close, 1=far
		double discdspeed = 0;
		double disc_forceJump(double pos) { return pos>=1 ? -2*discdspeed : 0.01; } // jump force
		double disc_forceBio(double pos) { return pos<0.5 ? 0.01 : -0.01; } // standard force
		double interUfoForce(Ufo u){
			double d = distance(u);
			double f = d<80 ? -(1-d/80) : d<160 ? 0.1 : 0;
			return 1*f;
		}

		Ufo(Side side, boolean AGGRESSIVE) {
			super(
				Ufo.class,
				(side==Side.RIGHT ? 1 : 0) * game.field.width,
				rand01()*game.field.height,0,0, game.settings.UFO_HIT_RADIUS,
				graphics(MaterialDesignIcon.BIOHAZARD,40,game.colors.ufos,null),
				game.settings.UFO_ENERGY_INITIAL,game.settings.UFO_E_BUILDUP
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
					dx += cos(direction)*game.settings.UFO_ENGINE_THRUST;
					dy += sin(direction)*game.settings.UFO_ENGINE_THRUST;


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
				game.settings.UFO_GUN_RELOAD_TIME,
				() -> {
					return gunAngleBase.getAndRun()+gunAngle.getAndRun();
//					if (!aggressive || game.ufos.ufo_enemy==null) return rand0N(D360);
//					Rocket enemy = isDistanceLess(game.ufos.ufo_enemy, UFO_BULLET_RANGE)
//						? game.ufos.ufo_enemy
//						: findClosestRocketTo(this);
//					return enemy==null ? rand0N(D360) : dir(enemy) + randMN(-D30,D30);
				},
				dir -> new UfoBullet(
					this,
					x + game.settings.UFO_BULLET_OFFSET*cos(dir),
					y + game.settings.UFO_BULLET_OFFSET*sin(dir),
					dx + cos(dir)*game.settings.UFO_BULLET_SPEED,
					dy + sin(dir)*game.settings.UFO_BULLET_SPEED
				)
			);
			game.runNext.addPeriodic(() -> ttl(seconds(5)), tryDiscs);
			tryDiscs.run();
		}

		@Override void doLoopOutOfField() {
			y = game.field.modY(y);
			if (game.field.isOutsideX(x)) dead = true;
		}


//		List<TimeDouble> rotations = DoubleStreamEx.of(1,-2,3,-4).mapToObj(v -> new TimeDouble(0, sign(v)*D360, millis(3600+2*PI*20*v)).periodic()).toList();
		@Override void draw() {
//			super.draw();
//			drawUfoRadar(x,y);

			drawUfoBullet(x, y, 1-gun.reload.get());

			if (hasSwarmers) {
				// Orbit mini swarmers
				// Use jump force to make the disc bounce smoothly only on inner side and bounce
				// instantly on the outer side. Standard force is more natural and 'biological', while
				// jump force looks more mechanical and alien.
				discdspeed += disc_forceJump(discpos);
				discpos += discdspeed;
				double dist = 40 + discpos * 20;
				DoubleStream.of(-3*D30, -7*D30, -11*D30)
					.forEach(dir -> drawUfoDisc(x + dist*cos(dir), y + dist*sin(dir), dir, 1));
			}

			if (hasSwarmers) {
				// Orbit swarmers
				swarmA += dswarmA/3;
				double dist = 30;
				double angleOffset = isSwarmActive ? PI/2 : PI;
				DoubleStream.of(swarmA-D120, swarmA, swarmA+D120)
					.forEach(dir -> drawUfoDisc(x + dist*cos(dir), y + dist*sin(dir), dir+angleOffset, 2));

//				DoubleStream.of(swarmA, swarmA+D30, swarmA-D30)
//					.flatMap(a -> DoubleStream.of(a-D120, a, a+D120))
//					.forEach(dir -> drawUfoDisc(x + dist*cos(dir), y + dist*sin(dir), dir+angleOffset, 2));
//				double dist1 = dist+30;
//				DoubleStream.of(swarmA, swarmA+D30, swarmA-D30)
//					.flatMap(a -> DoubleStream.of(a-D120, a, a+D120))
//					.forEach(dir -> drawUfoDisc(x + dist1*cos(dir), y + dist1*sin(dir), dir+angleOffset, 2));
//
//				double dist2 = dist1+30;
//				DoubleStream.of(swarmA, swarmA+D30, swarmA-D30)
//					.flatMap(a -> DoubleStream.of(a-D120, a, a+D120))
//					.forEach(dir -> drawUfoDisc(x + dist2*cos(dir), y + dist2*sin(dir), dir+angleOffset, 2));

//				IntStream.of(1,2,3,4)
				//					.forEach(i -> {
				//						double distance = 30*i;
				//						long count = 3*i;
				//						double angleBy = D360/count;
				//						DoubleStream.iterate(0+swarmA, x -> x+angleBy)
				//							.limit(count)
				//							.forEach(dir -> drawUfoDisc(x + distance*cos(dir), y + distance*sin(dir), dir+angleOffset, 2));
				//					});

//				rotations.forEach(TimeDouble::run);
//				IntStream.of(1,2,3,4)
//					.forEach(i -> {
//						double distance = 30*i;
//						long count = 3*i;
//						double angleBy = D360/count/3;
//						DoubleStream.iterate(0+rotations.get(i-1).get(), x -> x+angleBy)
//							.limit(count)
//							.forEach(dir -> drawUfoDisc(x + distance*cos(dir), y + distance*sin(dir), dir+angleOffset, 2));
//					});
			}

		}

		@Override
		void die(Object cause) {
			super.die(cause);
			game.ufos.onUfoDestroyed();
			drawUfoExplosion(x,y);

			if (hasSwarmers) {
				// release swarmers
				double dist = 30;
				double angleOffset = isSwarmActive ? PI/2 : PI;
				DoubleStream.of(swarmA-D120, swarmA, swarmA+D120)
					.mapToObj(dir -> new UfoSwarmer(x + dist*cos(dir), y + dist*sin(dir), dir+angleOffset))
					.forEach(u -> {
						u.isActive = true;
						u.isAggressive = isSwarmActive;
					});
			}
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
		/** True if capable of pursuit. */
		boolean isActive = true;
		/** True if actively looks for target to pursuit if not in pursuit already. */
		boolean isAggressive = true;
		/** Set to true if spawns outside of the field to prevent instant death. */
		boolean isInitialOutOfField = false;
		/** Id of the formation swarm or -1 if standalone. */
		int swarmId = -1;
		double ddirectionMax = ttlVal(D360,seconds(3));

		public UfoSwarmer(double X, double Y, double DIR) {
			super(UfoSwarmer.class, X, Y,0,0, game.settings.UFO_DISC_HIT_RADIUS, null,
				game.settings.UFO_ENERGY_INITIAL,game.settings.UFO_E_BUILDUP);
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
			// Find pursuit target
			// Note: Avoid unnecessary computation cheaply using n-th game loop strategy
			if (isActive && isAggressive) {
				if (game.loop.isNth(game.settings.UFO_DISC_DECISION_TIME_TTL))
					enemy = findClosestRocketTo(this);
			}

			if (isActive) {
				boolean isPursuit = !(enemy==null || enemy.player.rocket != enemy || enemy.isHyperspace);
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
					double dRotationAbs = min(ddirectionMax,abs(dirDiff));
					direction += sign(dirDiff)*dRotationAbs;
				} else {
					// Behavior
					// 1) Do nothing - good for "inactive" swarmers
					// engine.off();

					// 2) Keep going straight - good for "active dumb" swarmers
					// engine.on();

					// 3) Keep going straight - good for "active smart" swarmers
					engine.on();
					ddirection = ddirectionMax;
					double circleRadiusReducer = 2;
					direction += circleRadiusReducer*ddirection;
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
			return dist>distMax ? 0 : -0.5*sqr(1-dist/distMax);
		}

		void explode() {
			dead = true;
			drawUfoDiscExplosion(x,y);
			for (UfoSwarmer ud : game.oss.get(UfoSwarmer.class)) {
				if (distance(ud)<=game.settings.UFO_DISC_EXPLOSION_RADIUS)
					game.runNext.add(millis(100),ud::explode);
			}
		}
	}

	/** Non-interactive mission info button. */
	class MissionInfoButton extends PO {
		MissionInfoButton() {
			super(MissionInfoButton.class, 0, 0, 0, 0, 0, null);
//			super(MissionInfoButton.class, 0, 0, 0, 0, 0, graphics(FontAwesomeIcon.INFO,15,game.colors.humans,null));graphics=null;
			x = rand0N(game.field.width);
			y = rand0N(game.field.height);
			// graphics.setOnMouseClicked(e -> new MissionPane().show(game.mission));
//			playfield.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
//				if ((x-e.getX())*(x-e.getX())+(y-e.getY())*(y-e.getY())<=10*10) {
//					new MissionPane().show(game.mission);
//					e.consume();
//				}
//			});
		}
		@Override void init() {}
		@Override public void dispose() {}
		@Override public void doLoop() {
			super.doLoop();
			applyPlayerRepulseForce(this,400);
		}
	}

	private void drawUfoDisc(double x, double y, double dir, double scale) {
		gc.setFill(game.colors.ufos);
		gc.setStroke(game.colors.ufos);
		drawTriangle(gc, x,y,scale*game.settings.UFO_DISC_RADIUS, dir, 3*PI/4);
	}
	private void drawUfoRadar(double x, double y) {
		double r = game.settings.UFO_RADAR_RADIUS;
		gc.setFill(new RadialGradient(0,0,0.5,0.5,1,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(1, rgb(114,208,74, 0.3))));
		gc.fillOval(x-r,y-r,2*r,2*r);
	}
	private void drawUfoExplosion(double x, double y) {
		new FermiGraphics(x, y, game.settings.UFO_EXPLOSION_RADIUS).color = game.colors.ufos;
	}
	private void drawUfoDiscExplosion(double x, double y) {
		new FermiGraphics(x, y, game.settings.UFO_DISC_EXPLOSION_RADIUS).color = game.colors.ufos;
	}

	class Shuttle extends Ship {
		double ttl = ttl(seconds(50));
		final TimeDouble rotation = new TimeDouble(0, randOf(-1,1)*D360, seconds(40)).periodic();
		final Rocket owner;

		public Shuttle(Rocket r) {
			super(
				Shuttle.class, r.x+50,r.y-50,0,0,game.settings.PLAYER_HIT_RADIUS,
				graphics(FontAwesomeIcon.SPACE_SHUTTLE,40,game.colors.humansTech,null), 0,0
			);
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

		@Override
		public void doLoop() {
			super.doLoop();
			rotation.run();
			graphicsDir = rotation.get();
			// graphicsDir += rotation.get();   // produces interesting effect
		}

		@Override void die(Object cause) {
			super.die(cause);
			new FermiGraphics(x, y, game.settings.UFO_EXPLOSION_RADIUS).color = game.colors.humans;
		}
	}
	class SuperShield extends Ship {
		double ttl = ttl(seconds(50));
		final TimeDouble rotation = new TimeDouble(0, randOf(-1,1)*D360, seconds(40)).periodic();
		final Rocket owner;

		public SuperShield(Rocket r) {
			super(
				SuperShield.class, r.x+50,r.y-50,0,0,10,
				graphics(MaterialIcon.DETAILS, 20, game.colors.humansTech, null), 0,0
			);
			graphicsScale = 0;
			owner = r;
			kinetic_shield = new KineticShield(game.settings.SHUTTLE_KINETIC_SHIELD_RADIUS,game.settings.SHUTTLE_KINETIC_SHIELD_ENERGY_MAX) {
				// disables effect
				@Override protected void scheduleActivation() {}
			};
			kinetic_shield.KSenergy = kinetic_shield.KSenergy_max;
			createHyperSpaceAnimIn(game, this);
			game.runNext.add(ttl, () -> { if (!dead) createHyperSpaceAnimOut(game, this); });
			game.runNext.add(ttl + ttl(millis(200)), () -> dead=true);
		}

		@Override
		boolean isHitDistance(SO o) {
			if (o instanceof Bullet)
				return isDistanceLess(o,kinetic_shield.KSradius+game.settings.PLAYER_BULLET_SPEED/2+o.radius);
			if (o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
				return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
			return isDistanceLess(o,radius+o.radius);
		}

		@Override
		public void doLoop() {
			super.doLoop();
			rotation.run();
			graphicsDir = rotation.get();
			// graphicsDir += rotation.get();   // produces interesting effect
		}

		@Override
		void die(Object cause) {
			super.die(cause);
			new FermiGraphics(x, y, game.settings.UFO_EXPLOSION_RADIUS).color = game.colors.humans;
		}
	}
	class SuperDisruptor extends Ship {
		final double ttl;
		final TimeDouble rotation = new TimeDouble(0, randOf(-1,1)*D360, seconds(40)).periodic();
		final SO owner;
		final ForceField forceField;

		public SuperDisruptor(double x, double y) {
			this(x, y, null, game.settings.PLAYER_RESPAWN_TIME.divide(2), 0,null);
		}
		public SuperDisruptor(SO OWNER) {
			this(OWNER.x + 50, OWNER.y - 50, OWNER, seconds(50), 10, graphics(MaterialIcon.DISC_FULL, 20, game.colors.humansTech, null));
		}
		private SuperDisruptor(double x, double y, SO OWNER, Duration TTL, double HIT_RADIUS, Image GRAPHICS) {
			super(SuperDisruptor.class, x, y, 0, 0, HIT_RADIUS, GRAPHICS, 0, 0);
			ttl = ttl(TTL);
			graphicsScale = 0;
			owner = OWNER;
			forceField = new ForceField() {
				final double radius = game.settings.SHUTTLE_KINETIC_SHIELD_RADIUS;
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
					this.isHyperspace = SuperDisruptor.this.isHyperspace; // must always match
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

		@Override
		boolean isHitDistance(SO o) {
			return isDistanceLess(o,radius+o.radius);
		}

		@Override
		public void doLoop() {
			super.doLoop();
			rotation.run();
			graphicsDir = rotation.get();
			// graphicsDir += rotation.get();   // produces interesting effect
		}

		@Override
		void die(Object cause) {
			super.die(cause);
			game.entities.removeForceField(forceField);
			new FermiGraphics(x, y, game.settings.UFO_EXPLOSION_RADIUS).color = game.colors.humans;
		}
	}
	/** Represents defunct ship. Gives upgrades. */
	class Satellite extends PO {
		final Enhancer e;
		private boolean isLarge = true;

		/** Creates small satellite out of Shuttle or large Satellite. */
		public Satellite(PO s, double DIR) {
			super(Satellite.class,
				s.x,s.y,
				s instanceof Shuttle ? 0.2*cos(DIR) : s.dx,
				s instanceof Shuttle ? 0.2*sin(DIR) : s.dy,
				game.settings.SATELLITE_RADIUS/2, null
			);
			e = s instanceof Shuttle
				? randOf(stream(game.mode.enhancers()).filter(en -> !"Shuttle support".equals(en.name)).collect(toList()))
				: ((Satellite)s).e;
			children = new HashSet<>(2);
			graphics = new Draw(graphics(game.humans.intelOn.is() ? e.icon : MaterialDesignIcon.SATELLITE_VARIANT, 40, game.colors.humansTech, null));
			small();
		}

		/** Creates large Satellite. */
		public Satellite() {
			this(randEnum(Side.class));
		}

		/** Creates large Satellite. */
		public Satellite(Side dir) {
			super(Satellite.class,
				(dir==Side.LEFT ? 0 : 1)*game.field.width, rand01()*game.field.height,
				(dir==Side.LEFT ? 1 : -1)*game.settings.SATELLITE_SPEED, 0,
				game.settings.SATELLITE_RADIUS, graphics(MaterialDesignIcon.SATELLITE_VARIANT, 40, game.colors.humansTech, null)
			);
			e = randOf(game.mode.enhancers());
			children = new HashSet<>(2);
			if (game.humans.intelOn.is()) new EIndicator(this,e);
		}

		Satellite small() {
			isLarge = false;
			graphicsScale = 0.5;
			return this;
		}

		void move() {}
		void doLoopOutOfField() {
			y = game.field.modY(y);
			if (isLarge) {
				if (game.field.isOutsideX(x)) dead = true;
			} else {
				x = game.field.modX(x);
			}
		}
		void pickUpBy(Rocket r) {
			e.enhance(r);
			dead = true;
			game.placeholder(e.name, x+15, this.y-15);
//			game.placeholder(e.name, r);
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
		double ttl;
		double ttl_d;
		boolean isBlackHole = false;
		boolean isHighEnergy = false;
		private double tempX, tempY;    // cache for collision checking
		private short bounced = 0;    // prevents multiple bouncing off shield per loop

		Bullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL) {
			super(Bullet.class,x,y,dx,dy,hit_radius,null);
			owner = ship;
			ttl = 1;
			ttl_d = 1/TTL;
		}

		@Override
		public void doLoop() {
			if (bounced==1 || bounced==2) bounced++;
			x += dx;
			y += dy;
			doLoopOutOfField();

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
			// Style 1) - classic point bullet
//			double r = isHighEnergy ?  4 : 2;
//			gc_bgr.setFill(color);
//			gc_bgr.fillOval(x-1,y-1,r,r);

			// Style 2) - line bullets
			GraphicsContext g = gc_bgr;
//			g.setGlobalAlpha(0.4);
			g.setStroke(color(game.colors.main, 0.4));
			g.setLineWidth(isHighEnergy ?  5 : 3);
			g.strokeLine(x,y,x+dx*0.7,y+dy*0.7);
//			g.setGlobalAlpha(1);

			double xFrom = this.x, yFrom = this.y, xTo = x+dx*0.7, yTo = y+dy*0.7, opacity = 0.4, w = isHighEnergy ?  5 : 3;
			drawFading(game, ttl -> {
				gc.setGlobalAlpha(ttl*opacity);
				gc.setLineWidth(w);
				gc.setStroke(game.colors.hud);
				gc.strokeLine(xFrom,yFrom,xTo,yTo);
				gc.setGlobalAlpha(1);
			});
		}

		// cause == null => natural expiration, else hit object
		void onExpire(PO cause) {
			if (isBlackHole && !isHyperspace) {
				Player own = owner instanceof Rocket ?((Rocket)owner).player : null;
				game.entities.addForceField(new BlackHole(own, seconds(20),x,y));
			}
		}

		void checkCollision(PO e) {
			if (owner==e) return; // avoid self-hits (bug fix)
			if (isHyperspace !=e.isHyperspace) return;   // forbid space-hyperspace interaction

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
					if (!game.settings.deadly_bullets && owner instanceof Rocket) {
						r.kinetic_shield.new KineticShieldPiece(r.dir(this));
						bounceOffShieldOf(r);

						if (r.ability.isActiveOfType(Shield.class)) {
							((Shield)r.ability).onHit(this);
						}
					}
					if (game.settings.deadly_bullets || !(owner instanceof Rocket)) {
						if (r.ability.isActiveOfType(Shield.class)) {
							r.kinetic_shield.new KineticShieldPiece(r.dir(this));
							bounceOffShieldOf(r);
							((Shield)r.ability).onHit(this);
						} else if (r.kinetic_shield.KSenergy>=r.kinetic_shield.KSenergy_max) {
							r.kinetic_shield.new KineticShieldPiece(r.dir(this));
							bounceOffShieldOf(r);
							r.kinetic_shield.onShieldHit(this);
							r.kinetic_shield.changeKSenergyToMin();
						} else {
							r.player.die();
						}
					}
				} else
				if (e instanceof Asteroid) {
					if (owner instanceof Rocket)
						((Rocket) owner).player.stats.accHitEnemy(game.loop.id);

					Asteroid<?> a = (Asteroid)e;
					a.onHit(this);
					a.explosion();
					if (owner instanceof Rocket)
						((Rocket)owner).player.score.setValueOf(s -> s + (int) game.settings.SCORE_ASTEROID(a));
				} else
				if (e instanceof Ufo) {
					if (owner instanceof Rocket) {
						((Rocket) owner).player.score.setValueOf(s -> s + (int) game.settings.SCORE_UFO);
						((Rocket) owner).player.stats.accHitEnemy(game.loop.id);
						((Rocket) owner).player.stats.accKillUfo();
					}

					Ufo u = (Ufo)e;
					if (!(owner instanceof Ufo)) {
						u.die(this);
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
						((Rocket)owner).player.score.setValueOf(s -> s + (int) game.settings.SCORE_UFO_DISC);
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
			if (cause!=null && !isBlackHole && !isHyperspace && !(cause instanceof Rocket)) {
				double life_degradation = 0.9;
				double s = speed();
				double d = dir(cause);
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

		public UfoBullet(Ship ship, double x, double y, double dx, double dy) {
			super(ship, x,y, dx,dy, 0, game.settings.UFO_BULLET_TTL);
			scale_factor = 20;
		}

		@Override void draw() {
			drawUfoBullet(x, y, ttl);
		}

		@Override public void doLoop() {
			super.doLoop();

			double strength = 10;
			game.grid.applyExplosiveForce((float)strength, new Vec(x,y), 55);
		}
	}
	double scale_factor = 20;
	void drawUfoBullet(double x, double y, double ttl) {
		//			gc_bgr.setGlobalAlpha(0.8*(1-ttl));
		//			gc_bgr.setStroke(color);
		//			// gc_bgr.setFill(color);   //effect 2
		//			gc_bgr.setLineWidth(2);
		//			double r = scale_factor*(ttl*ttl);
		//			double d = 2*r;
		//			gc_bgr.strokeOval(x-r,y-r,d,d);
		//			// gc_bgr.fillOval(x-r,y-r,d,d); // effect 2
		//			gc_bgr.setStroke(null);
		//			gc_bgr.setGlobalAlpha(1);


		//			Color c = game.colors.ufos;
		Color c = color(game.colors.ufos, 0.8*(1-ttl));
		//			gc_bgr.setGlobalAlpha(0.8*(1-ttl));
		gc_bgr.setStroke(c);
		gc_bgr.setFill(c);   //effect 2
		gc_bgr.setLineWidth(2);
		double r = scale_factor*(ttl*ttl);
		double d = 2*r;
		gc_bgr.strokeOval(x-r,y-r,d,d);
		gc_bgr.fillOval(x-r,y-r,d,d); // effect 2
		gc_bgr.setStroke(null);
		//			gc_bgr.setGlobalAlpha(1);

		//			double x = this.x, y = this.y, radius = 3*scale_factor*(ttl*ttl), opacity = 0.7*(1-ttl);
		//			drawFading(game, seconds(0.5), ttl -> {
		//				double r = (1-ttl)*radius, d = 2*r;
		//				gc.setGlobalAlpha(ttl*ttl*opacity);
		//				gc.setStroke(game.colors.ufos);
		//				gc.setLineWidth(2);
		//				gc.strokeOval(x-(1-ttl)*r,y-(1-ttl)*r,(1-ttl)*d,(1-ttl)*d);
		//				gc.setStroke(null);
		//				gc.setGlobalAlpha(1);
		//			});
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
			gc.setStroke(game.colors.ufos);
			double r = 100 - (100-radius)*(1-ttl);
			gc.strokeRect(x-r, y-r, 2*r, 2*r);
		}

		@Override void checkCollision(PO e) {
			// We want the bullet to only hit once so we perform the check only once - when
			// bullet expires
			if (dead && ttl<=0)  {
				dead = false; // needs to be set first
				isHyperspace = target!=null && target.isHyperspace; // hyperspace targets too
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
		double cacheTtldOld = 0;

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

			cacheTtldOld = g_potential*ttld;
			ttl -= cacheTtldOld;
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
			gc.setFill(game.colors.humans);
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
//			gc_bgr.setFill(ttl<0.5 ? game.color : game.humans.color);
			gc_bgr.setFill(game.colors.humans.interpolate(game.colors.humans,ttl)); // TODO: possible performance killer
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
	/** Omnidirectional expanding wave. Represents active communication of the ship. */
	class RadioWavePulse extends Particle {
		double dxy;
		boolean rect;
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
			dxy = inverse ? dxy : dxy*0.985; // makes radius = f(ttl)
			radius += dxy;
			gc_bgr.setGlobalAlpha(0.5*(inverse ? 1-ttl : ttl));
			gc_bgr.setStroke(color);
			gc_bgr.setLineWidth(2*ttl+2); // width = f(ttl) is pretty
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
			gc.setGlobalAlpha((s.kinetic_shield.KSenergy/s.kinetic_shield.KSenergy_max)*sqrt(ttl));
			gc.setStroke(null);
			gc.setFill(new RadialGradient(0,0,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(0.85,COLOR_DB),new Stop(1,Color.TRANSPARENT)));
			drawOval(gc,x,y,radius);
			gc.setGlobalAlpha(1);
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
			boolean spontaneous = o==null || o instanceof BlackHole;
			dead = true;
			game.handleEvent(Events.PLANETOID_DESTROYED);
			game.runNext.add(() ->
				repeat(splits, i -> {
					double h = rand01();
					double v = rand01();
					double dxnew = spontaneous ? dx : dx+randMN(-1,1.1);
					double dynew = spontaneous ? dy : dy+randMN(-1,1.1);
					double speednew = pyth(dxnew,dynew);
					double dirnew = dirOf(dxnew,dynew,speednew);
					game.mission.planetoidConstructor.apply(x+h*0.2*size,y+v*0.2*size,speednew,dirnew, size_child*size);
				})
			);
		}
		void explosion() {
			new FermiGraphics(x,y,radius*2.5);

//			gc_bgr.setGlobalAlpha(0.2);
//			gc_bgr.setFill(game.color);
//			drawOval(gc_bgr,x,y,100);
//			gc_bgr.setGlobalAlpha(1);
		}
		abstract void onHitParticles(SO o);
	}

	private interface Mover {
		void calcSpeed(Asteroid<?> o);
	}
	private interface Draw2 {
		void drawBack();
		void drawFront();
	}

	private static class OrganelleMover implements Mover {
			double dirchange = rand0N(D360)/5/FPS;
			double ttldirchange = ttl(seconds(rand0N(12)));
			double ttldirchanging = ttl(seconds(rand0N(3)));

			@Override public void calcSpeed(Asteroid<?> o){
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
	class Energ extends Asteroid<OrganelleMover> {
		Color colordead = Color.BLACK;
		Color coloralive = Color.DODGERBLUE;
		double heartbeat = 0;
		double heartbeat_speed = 0.5*D360/ ttl(seconds(1+rand0N(size/30))); // times/sec

		public Energ(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
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
//				gc_bgr.setFill(ttl<0.5 ? colordead : coloralive); // crisp
////                 gc_bgr.setFill(colordead.interpolate(coloralive, sqrt(ttl))); // smooth
//				gc_bgr.fillOval(x,y,r,r);

				double r = this.r, x = this.x, y = this.y;
				Color color = ttl<0.5 ? colordead : coloralive;
				drawFading(game, millis(200), ttl -> {
					gc_bgr.setGlobalAlpha(ttl*ttl);
					gc_bgr.setFill(color);
					gc_bgr.fillOval(x,y,r,r);
					gc_bgr.setGlobalAlpha(1);
					gc.setGlobalAlpha(ttl*ttl);
					gc.setFill(color);
					gc.fillOval(x,y,r,r);
					gc.setGlobalAlpha(1);
				});
			}
		}
	}
	class Energ2 extends Energ {
		public Energ2(double X, double Y, double SPEED, double DIR, double RADIUS) {
			super(X, Y, SPEED, DIR, RADIUS);
			coloralive = rgb(244,48,48);
			hits_max = splits;
		}

		@Override void draw() {
			gc.setGlobalBlendMode(DARKEN);
			gc.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0,coloralive),new Stop(0.2,coloralive),new Stop(0.5,Color.BLACK),new Stop(1,Color.TRANSPARENT)));
			drawOval(gc,x,y,radius);
			gc.setGlobalBlendMode(SRC_OVER);
		}
	}
	class Particler extends Asteroid<OrganelleMover> {
		Color colordead = Color.BLACK;
		Color coloralive = Color.RED;
		double heartbeat = 0;
		double heartbeat_speed = 0.5*D360/ ttl(seconds(1+rand0N(size/30))); // times/sec

		public Particler(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
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
	class PlanetoDisc extends Asteroid<OrganelleMover> {
		public PlanetoDisc(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
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
			gc_bgr.setGlobalAlpha(1);
			gc_bgr.setFill(game.colors.main);
			gc_bgr.fillOval(x-radius,y-radius,d,d);
		}
		@Override void onHitParticles(SO o) {}
	}
	class Stringoid extends Asteroid<OrganelleMover> {
		public Stringoid(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
			createHyperSpaceAnimOut(game,this);
		}

		@Override void draw() {
			double d = radius*2;
			gc_bgr.setStroke(game.colors.main);
			gc_bgr.setLineWidth(3);
			gc_bgr.strokeOval(x-radius,y-radius,d,d);
		}
		@Override void onHitParticles(SO o) {
			repeat((int)(size*4), i -> 
				game.runNext.add(millis(randMN(100,300)), () -> {
					double r = 50+radius*2;
					double d = randAngleRad();
					new FermiGraphics(x+r*cos(d),y+r*sin(d),2).color = game.colors.main;
				})
			);
		}
		@Override void explosion() {
			new FermiGraphics(x,y,4+radius*1.3);
		}
	}
	class Linker extends Asteroid<OrganelleMover> {
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
			gc.setStroke(game.colors.main);
			gc.setLineWidth(1);
			strokeOval(gc, x, y, graphicsRadius);

			double connectionDistMin = 20, connectionDistMax = 100;
			double forceDistMin = 20, forceDistMax = 80, forceDistMid = (forceDistMax-forceDistMin)*2/3;
			stream(game.oss.get(Asteroid.class)).filter(Linker.class::isInstance).map(Linker.class::cast).forEach(l -> {
				double dist = distance(l);
				double dir = dir(l);
				// link
				if (dist>connectionDistMin && dist<connectionDistMax) {
					gc.setGlobalAlpha(1-dist/connectionDistMax);
					drawHudLine(gc, game.field, x,y,0,dist,cos(dir),sin(dir), game.colors.main);
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
				  double r = 50+radius*2*comet.Utils.rand01();
				  double d = randAngleRad();
				  new FermiGraphics(x+r*cos(d),y+r*sin(d),2).color = game.colors.main;
			}));
		}
		@Override void explosion() {
			new FermiGraphics(x,y,4+radius*1.3);
		}
	}
	class Inkoid extends Asteroid<OrganelleMover> {
		double trailTtl = ttl(seconds(0.5+rand0N(2)));

		public Inkoid(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		public void doLoop() {
			super.doLoop();
			trailTtl--;
			if (trailTtl<=0) {
				trailTtl = ttl(seconds(0.5+rand0N(2)));
				new InkoidDebris(x,y,0,0,5,seconds(2));
			}
		}

		@Override void onHit(SO o) {
			super.onHit(o);
			propulsion.dirchange *= 2; // speed rotation up
			propulsion.ttldirchange = -1; // change direction now
		}
		@Override void draw() {
			// Note: Graphics' ttl is function of trailTtl, meaning the trail length (which depends on ttl of the
			// graphics) debris is variable and in a way that makes it spawn the trail debris when it is the longest.
			// This creates an effect of spilling ink droplets on the way.
			new InkoidGraphics(x,y,0,0,radius,seconds(0.35-trailTtl/FPS/10));
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
		@Override void explosion() {
			new InkoidExplosion(x,y,radius*2.5);
		}

		private class InkoidGraphics extends Particle implements Draw2 {
			private final Color COLOR = new Color(0,.1,.1, 1);
			double r;

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
				gc_bgr.setFill(game.colors.main);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			@Override public void drawFront() {
				double rr = max(0,r - (1-ttl)*2);
				double d = rr*2;
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
				gc_bgr.setFill(game.colors.main);
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
		private class InkoidExplosion extends Particle implements Draw2 {
			double r;
			double delayTtl = ttl(seconds(0));  // delay explosion effect
			Color light = game.colors.main.darker();
			Color dark = light.darker().darker();

			public InkoidExplosion(double x, double y, double RADIUS) {
				this(x,y,0,0,RADIUS,seconds(0.4));
			}
			public InkoidExplosion(double x, double y, double dx, double dy, double RADIUS, Duration time) {
				super(x,y,dx,dy, ttl(time));
				radius = RADIUS;
			}

			@Override
			public void doLoop() {
				super.doLoop();
				delayTtl--;
				if (delayTtl >=0) {
					ttl += cacheTtldOld;
				}
			}

			void draw() {
				r = radius*ttl;
			}

			public void drawBack() {
				double rr = 2+r;
				rr *= ttl;  // using nonlinear interpolator here may change/improve the effect
				double d = rr*2;
				gc_bgr.setFill(light);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}
			public void drawFront() {
				double rr = max(0,r - (1-ttl)*2);
				rr *= ttl;  // using nonlinear interpolator here may change/improve the effect
				double d = rr*2;
				gc_bgr.setFill(dark);
				gc_bgr.fillOval(x-rr,y-rr,d,d);
			}

			//
			//		public void drawBack() {
			//			double rr = 2+r;
			//			rr *= ttl;
			//			// this has no effect on graphics, but produces nice radial pattern effect when inkoid dies
			//			//            rr *= ttl;
			//			double d = rr*2;
			//			gc_bgr.setGlobalAlpha(0.5);
			//			gc_bgr.setFill(Color.GREEN);
			//			//			gc_bgr.setFill(game.color);
			//			gc_bgr.fillOval(x-rr,y-rr,d,d);
			//			gc_bgr.setGlobalAlpha(1);
			//		}
			//		public void drawFront() {
			//			//			double rr = max(0,r - (1-ttl)*2);
			//			//			// this has no effect on graphics, but produces nice radial pattern effect when inkoid dies
			//			//			rr *= ttl;
			//			//			double d = rr*2;
			//			//			gc_bgr.setFill(Color.BLACK);
			//			//			gc_bgr.fillOval(x-rr,y-rr,d,d);
			//		}
		}
	}
	class PGon extends Asteroid<OrganelleMover> {
		private final TimeDouble graphicsAngle = new TimeDouble(0, 0.01).setTo(randAngleRad());
		private final TimeDouble radiusPulse = new TimeDouble(0, 1, seconds(1/6d)).oscillating();
		private double radiusMax;

		public PGon(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radiusMax = game.settings.INKOID_SIZE_FACTOR*size;
			radius = radiusMax;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;
		}

		@Override
		public void doLoop() {
			super.doLoop();
			graphicsAngle.run();
			radiusPulse.run();
			long ri = 1+ radiusPulse.cycle%6;
			radius = 10 + (ri<=4 ? 0 : radiusMax * pow(radiusPulse.get(),3));
		}

		@Override
		void onHit(SO o) {
			super.onHit(o);
		}

		@Override
		void onHitParticles(SO o) {}

		@Override
		void explosion() {
			super.explosion();
		}

		@Override
		void draw() {
			int vertices = 3 + (int) (5*size);
			double[] xs = new double[vertices];
			double[] ys = new double[vertices];
			double dAngle = D360 / vertices;
			repeat(vertices, i -> {
				double angle = i * dAngle;
				xs[i] = x+radius*cos(angle+ graphicsAngle.get());
				ys[i] = y+radius*sin(angle+ graphicsAngle.get());
			});

			gc.setFill(Color.WHITE);
			Util.forEachBoth(xs, ys, (x,y) -> drawOval(gc, x, y, 2));

			gc.setFill(color(Color.GREENYELLOW, 0.1));
			gc.setStroke(color(Color.GREENYELLOW, 0.3));
			strokePolygon(gc, xs, ys);
			gc.fillPolygon(xs, ys, vertices);

			gc_bgr.setFill(color(Color.GREENYELLOW, 0.3));
			gc_bgr.setStroke(color(Color.GREENYELLOW, 0.3));
			strokePolygon(gc_bgr, xs, ys);
		}
	}
	class Chargion extends Asteroid<OrganelleMover> {
		final String name;

		public Chargion(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = size>0.5 ? randOf(0.25, 0.125) : size>0.25 ? 0.5 : 1; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? 1 : size>0.25 ? randOf(1,0) : 0;
			name = size>0.5 ? randOf("p+", "n") : size>0.125 ? "e-" : randOf("u", "d", "t");
			hits_max = splits==1 ? 4 : 0;
		}

		@Override
		void draw() {
			game.fillText(name, x, y);

			gc.setStroke(game.colors.main);
			gc.setLineWidth(1);
			strokeOval(gc, x, y, radius);
		}

		@Override
		void onHit(SO o) {
			hits++;
			split(o);
			onHitParticles(o);
		}

		@Override
		void onHitParticles(SO o) {}

		@Override
		void split(SO o) {
			super.split(o);
			dead = hits>=hits_max;
		}
	}
	class Genoid extends Asteroid<OrganelleMover> {
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
			radius = game.settings.INKOID_SIZE_FACTOR*size;
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
				// gc_bgr.setFill(game.color);
				// gc_bgr.fillOval(x-rr,y-rr,d,d);
				gc_bgr.setStroke(game.colors.main);
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
				gc_bgr.setFill(game.colors.main);
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
	class Fermi extends Asteroid<OrganelleMover> {
		final PTtl trail = new PTtl(() -> ttl(seconds(0.5+rand0N(2))), () -> new FermiDebris(x,y,0,0,5,seconds(0.6)));
		double oscillationTtl = 0;
		FermiMove pseudomovement;

		public Fermi(double X, double Y, double SPEED, double DIR, double LIFE) {
			super(X, Y, SPEED, DIR, LIFE);
			propulsion = new OrganelleMover();
			size = LIFE;
			radius = game.settings.INKOID_SIZE_FACTOR*size;
			size_hitdecr = 1;
			size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
			splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
			hits_max = splits>2 ? 1 : 0;

			if (size<=0.5 && randBoolean()) {
				F1<Fermi,FermiMove> m = randOf(FERMI_MOVES);
				pseudomovement = m.apply(this);
			}
		}

		public void doLoop() {
			if (pseudomovement!=null) oscillationTtl += pseudomovement.oscillationIncrement();
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
				double s = sin(oscillationTtl);
				dx += 2*cos(dir+s*D90) ;
				dy += 2*sin(dir+s*D90);
			}
		}
		class ZigZagMove extends FermiMove {
			double oscillationIncrement() {
				return 1;
			}
			public void modifyMovement() {
				if (oscillationTtl %20<10) {
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
				double s = sin(oscillationTtl);
				dx += 2*cos(dir+s*PI) ;
				dy += 2*sin(dir+s*PI);
			}
		}
		class KnobMove extends FermiMove {
			double oscillationIncrement() {
				return 0.5/(10*size);
			}
			public void modifyMovement() {
				double s = sin(oscillationTtl);
				dx += 2*cos(dir+s*D360) ;
				dy += 2*sin(dir+s*D360);
			}
		}
		class SpiralMove extends FermiMove {
			double oscillationIncrement() {
				return .18;
			}
			public void modifyMovement() {
				dx += 2*cos(dir+ oscillationTtl) ;
				dy += 2*sin(dir+ oscillationTtl);
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
				gc_bgr.setFill(game.colors.main);
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
	private static final Set<F1<Fermi,FermiMove>> FERMI_MOVES = set(
		f -> f.new StraightMove(),f -> f.new WaveMove(),f -> f.new FlowerMove(),
		f -> f.new ZigZagMove(),f -> f.new KnobMove(),f -> f.new SpiralMove(),f -> f.new SidespiralMove()
	);
	private class FermiGraphics extends Particle implements Draw2 {
		double r;
		Color color = game.colors.main;

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
			// if (ff.isHyperspace!=o.isHyperspace) return;

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
				Asteroid<?> a = ((Asteroid)o);
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

			{
				// Aura effect
				gc_bgr.setGlobalBlendMode(OVERLAY);
				gc_bgr.setGlobalAlpha(1-life);
				gc_bgr.setEffect(new BoxBlur(100,100,1));
				gc_bgr.setFill(Color.AQUA);
				drawOval(gc_bgr, x,y,20+(1-life)*40);
				gc_bgr.setGlobalBlendMode(SRC_OVER);
				gc_bgr.setGlobalAlpha(1);
				gc_bgr.setEffect(null);
			}

			boolean isCrowded = game.oss.get(Particle.class).size() > game.settings.BLACK_HOLE_PARTICLES_MAX;
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
	 * Rocket enhancer indicator icon moving with player rocket to indicate active enhancers.
	 */
	class EIndicator implements LO {
		double ttl;
		final PO owner;
		final int index;
		final Draw graphics;

		public EIndicator(PO OWNER, Enhancer enhancer) {
			owner = OWNER;
			ttl = ttl(owner instanceof Satellite ? minutes(10) : enhancer.duration);
			index = findFirstInt(0, i -> stream(owner.children)
				.filter(EIndicator.class::isInstance).map(EIndicator.class::cast)
				.noneMatch(o -> o.index==i));
			owner.children.add(this);
			graphics = new Draw(graphics(enhancer.icon, 15, game.colors.humansTech, null));
		}

		@Override
		public void doLoop() {
			ttl--;
			if (ttl<0) game.runNext.add(this::dispose);
			graphics.draw(gc, owner.x+30+20*index, owner.y-30);
		}

		@Override
		public void dispose() {
			owner.children.remove(this);
		}

	}

}