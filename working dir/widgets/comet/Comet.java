package comet;

import java.util.*;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
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
import util.SwitchException;
import util.access.V;
import util.animation.Anim;
import util.animation.Loop;
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

import static comet.Comet.Achievement.achievement1;
import static comet.Comet.Constants.*;
import static comet.Utils.AbilityKind.SHIELD;
import static comet.Utils.AbilityState.*;
import static comet.Utils.*;
import static comet.Utils.GunControl.AUTO;
import static comet.Utils.GunControl.MANUAL;
import static gui.objects.window.stage.UiContext.showSettingsSimple;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.abs;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Pos.*;
import static javafx.scene.effect.BlendMode.*;
import static javafx.scene.effect.BlurType.GAUSSIAN;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;
import static javafx.util.Duration.*;
import static util.Util.clip;
import static util.async.Async.run;
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
                c.setVisible(game.players.stream().anyMatch(p -> p.id==(Integer)c.getUserData()))
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
                new Icon(null,16){{ maintain(game.paused,mapB(MaterialDesignIcon.PLAY,MaterialDesignIcon.PAUSE), this::setIcon); }}.onClick(() -> game.pause(!game.paused.get())),
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
                game.keyPressTimes.put(cc,System.currentTimeMillis());
                game.players.stream().filter(p -> p.alive).forEach(p -> {
                    if (cc==p.keyAbility.getValue()) p.rocket.ability_main.onKeyPress();
                    if (cc==p.keyThrust.getValue()) p.rocket.engine.on();
                    if (cc==p.keyFire.getValue()) p.rocket.gun.fire();
                });
                // cheats
                if (cc==DIGIT1) game.runNext.add(() -> repeat(5, i -> game.mission.spawnPlanetoid()));
                if (cc==DIGIT2) game.runNext.add(() -> repeat(5, i -> new Ufo()));
                if (cc==DIGIT3) game.runNext.add(() -> repeat(5, i -> new Satellite()));
                if (cc==DIGIT4) game.runNext.add(() -> {
                    game.oss.forEach(Asteroid.class,a -> a.dead=true);
                    game.nextMission();
                });
                if (cc==DIGIT5) game.players.stream().filter(p -> p.alive).forEach(p -> game.humans.send(p.rocket, Shuttle::new));
                if (cc==DIGIT6) game.oss.get(Rocket.class).forEach(r -> randOf(game.ROCKET_ENHANCERS).enhance(r));
            }
        });
        playfield.addEventFilter(KEY_RELEASED, e -> {
            game.players.stream().filter(p -> p.alive).forEach(p -> {
                if (e.getCode()==p.keyAbility.getValue()) p.rocket.ability_main.onKeyRelease();
                if (e.getCode()==p.keyThrust.getValue()) p.rocket.engine.off();
            });
            game.pressedKeys.remove(e.getCode());
        });
        playfield.setOnMouseClicked(e -> playfield.requestFocus());
        playfield.focusedProperty().addListener((o,ov,nv) -> game.pause(!nv));

        addEventHandler(Event.ANY, Event::consume);
    }

    @Override
    public void onClose() {
        game.stop();
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
	    double BONUS_LASER_MULTIPLIER_LENGTH = 400; // px
	    double ROTATION_SPEED = 1.3 * PI / FPS; // 540 deg/sec.
	    double RESISTANCE = 0.98; // slow down factor
	    int ROT_LIMIT = 70; // smooths rotation at small scale, see use
	    int ROT_DEL = 7; // smooths rotation at small scale, see use

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
	    double ROCKET_ENGINE_DEBRIS_TTL = ttl(millis(20));
	    double PULSE_ENGINE_PULSEPERIOD_TTL = ttl(millis(20));
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
	    double SHIELD_E_RATE = 30; // energy/frame
	    Duration SHIELD_ACTIVATION_TIME = millis(0);
	    Duration SHIELD_PASSIVATION_TIME = millis(0);
	    double HYPERSPACE_E_ACTIVATION = 1500; // energy
	    double HYPERSPACE_E_RATE = 0; // energy/frame
	    Duration HYPERSPACE_ACTIVATION_TIME = millis(200);
	    Duration HYPERSPACE_PASSIVATION_TIME = millis(200);
	    double DISRUPTOE_E_ACTIVATION = 0; // energy
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
	    double UFO_RADAR_RADIUS = 75;
	    double UFO_DISC_RADIUS = 3;
	    double UFO_DISC_HIT_RADIUS = 9;
	    int UFO_DISC_DECISION_TIME_TTL = (int) ttl(millis(500));
	    double UFO_EXPLOSION_RADIUS = 100;
	    double UFO_DISC_EXPLOSION_RADIUS = 15;

	    static double UFO_TTL() { return ttl(seconds(randMN(30, 80))); }

	    static double UFO_SQUAD_TTL() { return ttl(seconds(randMN(200, 500))); }

	    static double UFO_DISCSPAWN_TTL() { return ttl(seconds(randMN(60, 180))); }

	    double SATELLITE_RADIUS = 15; // energy/frame
	    double SATELLITE_SPEED = 200 / FPS; // ufo speed in px/s/fps

	    static double SATELLITE_TTL() { return ttl(seconds(randMN(10, 25))); }

	    static double SHUTTLE_TTL() { return ttl(seconds(randMN(10, 11))); }

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
    void drawHudLine(double x, double y, double lengthStart, double length, double cosDir, double sinDir, Color color) {
        gc.setFill(color);
        gc.setGlobalAlpha(HUD_OPACITY);

        for (double i=lengthStart; i<length; i+=HUD_DOT_GAP)
            gc.fillOval(modX(x+i*cosDir), modY(y+i*sinDir), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);

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
            gc.fillOval(modX(x+r*cos(a)), modY(y+r*sin(a)), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
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
			if (playfield.contains(x1,y1) || playfield.contains(x2,y2))
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

    /** Encompasses entire game. */
    class Game {
        final V<Boolean> paused = new V<>(false);
        final V<Boolean> running = new V<>(false);
        final V<Boolean> deadly_bullets = new V<>(false);

        final ObservableSet<Player> players  = FXCollections.observableSet();
        final EntityManager entities = new EntityManager();
        final ObjectStore<PO> oss = new ObjectStore<>(o -> o.type);
        final Collection<PO> os = new ArrayDeque<>();
        final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
        final Map<KeyCode,Long> keyPressTimes = new HashMap<>();

        final Loop loop = new Loop(this::doLoop);
        long loopId = 0;   // game loop id, starts at 0, incremented by 1
        final UfoFaction ufos = new UfoFaction();
        final PlayerFaction humans = new PlayerFaction();
        final TTLList runNext = new TTLList();
        final Set<PO> removables = new HashSet<>();

        Grid grid;// = new Grid(gc_bgr, 1000, 500, 50, 50);

        int mission_counter = 0;   // mission counter, starts at 1, increments by 1
        Mission mission = null; // current mission, (they repeat), starts at 1, = mission % missions +1
        boolean isMissionScheduled = false;
        MissionInfoButton mission_button;
        final MapSet<Integer,Mission> missions = new MapSet<>(m -> m.id,

	                                                             new Mission(1, "Energetic fragility","10⁻¹⁵","",
		                                                                        null, Color.DODGERBLUE,Color.rgb(10,10,25,0.08), null,Energ::new
	                                                             ),
//            new Mission(1, "The strange world", "10⁻⁴m", "",
//                null,Color.BLACK, Color.rgb(225,225,225, 0.2),null, PlanetoDisc::new
//            ), //new Glow(0.3)
            new Mission(2, "Sumi-e","10⁻¹⁵","",
                null,Color.LIGHTGREEN, Color.rgb(0, 51, 51, 0.1),null, Inkoid::new
            ), //new Glow(0.3)
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
                null,Color.YELLOW, Color.rgb(10, 11, 1, 0.2),null, PlanetoCircle::new
            ), //new Glow(0.3)
            new Mission(7, "Mother of all branes","10⁻¹⁵","",
                null,Color.DODGERBLUE, Color.rgb(0, 0, 15, 0.08), null, Genoid::new
            ),
            new Mission(8, "Energetic fragility","10⁻¹⁵","",
                null, Color.DODGERBLUE,Color.rgb(10,10,25,0.08), null,Energ::new
            ),
            new Mission(9, "Planc's plancton","10⁻¹⁵","",
                null,Color.GREEN,new Color(1,1,1,0.08),null,Energ2::new
            ),
            new Mission(10, "T duality of a planck boundary","10⁻¹⁵","",
                null,Color.DARKSLATEBLUE,new Color(1,1,1,0.08),null,Energ2::new
            ),
            new Mission(11, "Informative xperience","10⁻¹⁵","",
                bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
            ),
            new Mission(12, "Holographically principled","10⁻¹⁵","",
                bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new
            )
        );

        final Set<Enhancer> ROCKET_ENHANCERS = set(

            // fire upgrades
            new Enhancer("Gun", MaterialDesignIcon.KEY_PLUS, seconds(5),
                r -> r.gun.turrets.inc(), r -> {/*r.gun.turrets.dec()*/},
                "- Mounts additional gun turret",
                "- Increases chance of hitting the target",
                "- Increases maximum possible target damage by 100%"
            ),
            new Enhancer("Rapid fire", MaterialDesignIcon.BLACKBERRY, seconds(12), r -> r.rapidfire.inc(), r -> r.rapidfire.dec(),
                " - Largely increases rate of fire temporarily. Fires constant stream of bullets",
                " - Improved hit efficiency due to bullet spam",
                " - Improved mobility due to less danger of being hit",
                "Tip: Fire constantly. Be on the move. Let the decimating power of countless bullets"
              + " be your shield. The upgrade lasts only a while - being static is a disadvantage."
            ),
            new Enhancer("Long fire", MaterialDesignIcon.DOTS_HORIZONTAL, seconds(50), r -> r.powerfire.inc(), r -> r.powerfire.dec(),
                "- Increases bullet speed",
                "- Increases bullet range",
                "Tip: Aim closer to target. Faster bullet will reach target sooner."
            ),
            new Enhancer("High energy fire", MaterialDesignIcon.MINUS, seconds(25), r -> r.energyfire.inc(), r -> r.energyfire.dec(),
                "- Bullets penetrate the target",
                "- Increases bullet damage, 1 hit kill",
                "- Multiple target damage",
                "Tip: Try lining up targets into a line. "
            ),
            new Enhancer("Split ammo", MaterialIcon.CALL_SPLIT, seconds(15), r -> r.splitfire.inc(), r -> r.splitfire.dec(),
                "- Bullets split into 2 bullets on hit",
                "- Multiple target damage",
                "Tip: Strategic weapon. The damage potential raises exponentially"
              + " with the number of targets. Annihilate the most dense enemy area with ease. "
            ),
            new Enhancer("Black hole cannon", MaterialDesignIcon.CAMERA_IRIS, seconds(5), r -> r.gun.blackhole.inc(),
                "- Fires a bullet generating a black hole",
                "- Lethal to everything, including players",
                "- Player receives partial score for all damage caused by the black hole",
                "Tip: Strategic weapon. Do not endanger yourself or your allies."
            ),
            new Enhancer("Aim enhancer", MaterialDesignIcon.RULER, seconds(35),
                r -> {
                    Ship.LaserSight ls = r.new LaserSight();
                    game.runNext.add(seconds(35),ls::dispose);
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
            new Enhancer("Energizer", MaterialDesignIcon.BATTERY_POSITIVE, seconds(5), r -> r.energy_max *= 1.1,
                "- Increases maximum energy by 10%"
            ),
            new Enhancer("Battery (small)", MaterialDesignIcon.BATTERY_30, seconds(5),
                r -> r.energy = min(r.energy+2000,r.energy_max),
                "- Increases energy by up to 2000"
            ),
            new Enhancer("Battery (medium)", MaterialDesignIcon.BATTERY_60, seconds(5),
                r -> r.energy = min(r.energy+5000,r.energy_max),
                "- Increases energy by up to 5000"
            ),
            new Enhancer("Battery (large)", MaterialDesignIcon.BATTERY, seconds(5),
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
		    ),
		    achievement1(
			    "Control freak", MaterialDesignIcon.ARROW_EXPAND,
			    game -> stream(game.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
			    "Control your nearby area most effectively"
		    ),
		    achievement1(
			    "Reaper's favourite", MaterialDesignIcon.ARROW_EXPAND,
			    game -> stream(game.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
			    "Be the first to die"
		    ),
		    achievement1(
			    "Live and prosper", MaterialDesignIcon.ARROW_EXPAND,
			    game -> stream(game.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
			    "Live the longest"
		    ),
		    achievement1(
			    "Invincible", MaterialDesignIcon.ARROW_EXPAND,
			    game -> stream(game.players).max(by(p -> p.stats.controlAreaCenterDistance.getAverage())).get(),
			    "Don't die"
		    )
	    );
	    Voronoi voronoi = new Voronoi2(
	        (rocket,area) -> rocket.player.stats.controlAreaSize.accept(area),
	        (rocket,areaCenterDistance) -> rocket.player.stats.controlAreaCenterDistance.accept(areaCenterDistance),
	        (centerX,centerY) -> {
	        	if (humans.intelOn.is()) {
			        gc_bgr.fillOval(centerX - 1, centerY - 1, 3, 3);
			        drawHudCircle(centerX, centerY, 10, humans.color);
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

        void start(int player_count) {
            stop();

            // delayed initialization
            if (grid==null) grid = new Grid(gc, playfield.getWidth(), playfield.getHeight(), 35);

            players.addAll(listF(player_count,PLAYERS.list::get));
	        players.forEach(Player::reset);

            running.set(true);
            loopId = 0;
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
            if (loopId%60==0) LOGGER.debug("particle.count= {}", oss.get(Particle.class).size());

            // loop prep
            loopId++;
            long now = System.currentTimeMillis();
            boolean isThird = loopId %3==0;

            players.stream().filter(p -> p.alive).forEach(p -> {
                if (pressedKeys.contains(p.keyLeft.get()))  p.rocket.dir -= p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyLeft.get(), 0L));
                if (pressedKeys.contains(p.keyRight.get())) p.rocket.dir += p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyRight.get(), 0L));
                if (isThird && p.rocket.rapidfire.is() && pressedKeys.contains(p.keyFire.get()))  p.rocket.gun.fire();
            });

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

            mission_button.doLoop();

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

            grid.update();
            grid.draw();

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
            oss.forEach(Rocket.class,UfoDisc.class, (r,ud) -> {
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

            // non-interacting stuff last
            oss.get(Particle.class).forEach(Particle::doLoop);
            stream(oss.get(Particle.class)).select(Draw2.class).forEach(Draw2::drawBack);
            stream(oss.get(Particle.class)).select(Draw2.class).forEach(Draw2::drawFront);

	        voronoi.compute(oss.get(Rocket.class),playfield.getWidth(), playfield.getHeight(), Comet.this);
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

        void nextMission() {
            if (isMissionScheduled) return;
            isMissionScheduled = true;
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
            runNext.add(seconds(5), () -> {
	            Map<Player,List<Achievement>> as =  stream(ACHIEVEMENTS).flatMapToEntry(a -> stream(a.evaluator.apply(this)).toMap(player -> player, player -> a)).grouping();
	            System.out.println(as);
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

        /** Clears resources. No game session will occur after this. */
        void dispose() {
            stop();
        }

        class PlayerFaction {
            final InEffect intelOn = new InEffect();
            final Color color = Color.DODGERBLUE;
            final Color colorTech = Color.AQUAMARINE;
            boolean share_enhancers = false;

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
                double x = s==Side.LEFT ? offset : playfield.getWidth()-offset;
                double y = rand0N(playfield.getHeight());
                if (humans.intelOn.is()) pulseAlert(x,y);
                runNext.add(seconds(1.8), () -> new Satellite(s).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,3,color,false);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-3,color,false);
            }
        }
        class UfoFaction {
            int losses = 0;
            int losses_aggressive = 5;
            int losses_cannon = 20;
            Rocket ufo_enemy = null;
            boolean aggressive = false;
            boolean canSpawnDiscs = false;
            final Color color = Color.rgb(114,208,74);

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
                message("U.F.O. CANNON ALERT");
                repeat(5, () -> runNext.add(seconds(rand0N(15)), this::fireSlipSpaceCannon));
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
                repeat(count, () -> runNext.add(seconds(rand0N(0.5)),() -> sendUfo(side)));
            }
            private void sendUfo(Side side) {
                Side s = side==null ? randEnum(Side.class) : side;
                double offset = 50;
                double x = s==Side.LEFT ? offset : playfield.getWidth()-offset;
                double y = rand0N(playfield.getHeight());
                if (humans.intelOn.is()) pulseAlert(x,y);
                runNext.add(seconds(1.2), () -> new Ufo(s,aggressive).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,3,color,true);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-3,color,true);
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

            void start() {
                ((Pane)playfield.getParent()).setBackground(bgr);
                playfield.setEffect(toplayereffect);
                grid.color = color;

                double size = sqrt(playfield.getWidth()*playfield.getHeight())/1000;
                int planetoids = 3 + (int)(2*(size-1)) + (mission_counter-1) + players.size()/2;
                double delay = ttl(seconds(mission_counter==1 ? 2 : 5));
                runNext.add(delay/2, () -> message("Level " + mission_counter, name));
                runNext.add(delay, () -> repeat(planetoids, i -> spawnPlanetoid()));
                runNext.add(delay, () -> isMissionScheduled = false);
            }

            void spawnPlanetoid() {
                boolean vertical = randBoolean();
                planetoidConstructor.apply(
                    vertical ? 0 : rand01()*playfield.getWidth(),
                    vertical ? rand01()*playfield.getHeight() : 0,
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
    class Player implements Configurable {
        final int id;
        @IsConfig final V<String> name = new V<>("");
        @IsConfig final V<Color> color = new V<>(Color.WHITE);
        @IsConfig final V<KeyCode> keyFire = new V<>(KeyCode.W);
	    @IsConfig final V<KeyCode> keyThrust = new V<>(KeyCode.S);
	    @IsConfig final V<KeyCode> keyLeft = new V<>(KeyCode.A);
	    @IsConfig final V<KeyCode> keyRight = new V<>(KeyCode.D);
	    @IsConfig final V<KeyCode> keyAbility = new V<>(KeyCode.Q);
        boolean alive = false;
        final V<Integer> lives = new V<>(PLAYER_LIVES_INITIAL);
        final V<Integer> score = new V<>(0);
        final V<Double> energy = new V<>(0d);
        final V<Double> energyKS = new V<>(0d);
	    @IsConfig final V<AbilityKind> ability_type = new V<>(AbilityKind.SHIELD);
        Rocket rocket;
	    final StatsPlayer stats = new StatsPlayer();

        public Player(int ID, Color COLOR, KeyCode kfire, KeyCode kthrust, KeyCode kleft, KeyCode kright, KeyCode kability, AbilityKind ABILITY) {
            id = ID;
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
            every200ms.add(() -> { if (rocket!=null) energyKS.set(rocket.kinetic_shield.KSenergy); });
        }

        void die() {
            if (!alive) return;
            alive = false;
            rocket.dead = true;
            if (lives.getValue()>0) {
                game.grid.applyExplosiveForce(100, new Vec(rocket.x,rocket.y), 50);
                run(PLAYER_RESPAWN_TIME.toMillis(),this::spawn);
            } else {
                if (game.players.stream().noneMatch(p -> p.alive))
                    game.over();
            }
        }

        void spawn() {
            if (alive) return;
            alive = true;
            lives.setValueOf(lives -> lives-1);
            rocket = new Rocket(this);
	        rocket.dead = false;
            rocket.x = spawning.get().computeStartingX(playfield.getWidth(),playfield.getHeight(),game.players.size(),id);
            rocket.y = spawning.get().computeStartingY(playfield.getWidth(),playfield.getHeight(),game.players.size(),id);
            rocket.dx = 0;
            rocket.dy = 0;
            rocket.dir = spawning.get().computeStartingAngle(game.players.size(),id);
            rocket.energy = PLAYER_ENERGY_INITIAL;
            rocket.engine.enabled = false; // cant use engine.off() as it could produce unwanted behavior
	        new Enhancer("Super shield", FontAwesomeIcon.SUN_ALT, seconds(5), r -> r.kinetic_shield.large.inc().inc(), r -> r.kinetic_shield.large.dec().dec(), "").enhance(rocket);
	        createHyperSpaceAnimIn(game, rocket);
        }

        void reset() {
	        alive = false;
	        score.setValue(0);
	        lives.setValue(PLAYER_LIVES_INITIAL);
	        if (rocket!=null) rocket.dead = true; // just in case
	        rocket = null;
        }

        double computeRotSpeed(long pressedMsAgo) {
            // Shooting at long distance becomes hard due to 'smallest rotation angle' being too big
            // we slow down rotation in the first ROT_LIMIT ms after key press and reduce rotation
            // limit without decreasing maneuverability. The rotation decrease is nonlinear and
            // continuous
            double r = pressedMsAgo<ROT_LIMIT ? ROTATION_SPEED/((ROT_LIMIT/ROT_DEL+1)-pressedMsAgo/ROT_DEL) : ROTATION_SPEED;
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
            if (x<0) x = playfield.getWidth();
            else if (x>playfield.getWidth()) x = 0;
            if (y<0) y = playfield.getHeight();
            else if (y>playfield.getHeight()) y = 0;
        }
        double distance(PO o) {
            return dist(this.x,this.y,o.x,o.y);
        }
        boolean isDistanceLess(SO o, double dist) {
            return isDistLess(this.x,this.y,o.x,o.y, dist);
        }
        boolean isHitDistance(SO o) {
            return isDistanceLess(o,radius+o.radius);
        }
        /** direction angle between objects. In radians. */
        double dir(SO to) {
            double tx = distXSigned(x,to.x);
            double ty = distYSigned(y,to.y);
            return (tx<0 ? 0 : PI) + atan(ty/tx);
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

        PO(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Image GRAPHICS) {
            type = TYPE;
            x = X; y = Y; dx = DX; dy = DY;
            radius = HIT_RADIUS;
            mass = 2*HIT_RADIUS*HIT_RADIUS; // 4/3d*PI*HIT_RADIUS*HIT_RADIUS*HIT_RADIUS;
            graphics = GRAPHICS;
            init();
        }

        public void doLoop(){
            doLoopBegin();
            super.doLoop();
            doLoopEnd();
            if (children!=null) children.forEach(LO::doLoop);
        }
        void doLoopBegin(){}

	    void doLoopEnd(){}

	    void move() {
            dx *= RESISTANCE;
            dy *= RESISTANCE;
//            if (abs(dx)<1/FPS) dx = 0;
//            if (abs(dy)<1/FPS) dy = 0;
        }

        void draw() {
            if (graphics!=null) {
	            drawImageRotatedScaled(gc, graphics, deg(graphicsDir), x, y, graphicsScale);
            }
        }

        void init() {
            game.os.add(this);
            game.oss.add(this);
        }

        public void dispose() {
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
        double dir = -D90; // up
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

        void doLoopBegin() {
            cache_speed = speed();
            cosdir = cos(dir);
            sindir = sin(dir);
            dx_old = dx;
            dy_old = dy;

            energy = min(energy+energy_buildup_rate,energy_max);
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
                    // for each turret, fire
                    game.runNext.add(() -> {
                        if (blackhole.is()) {
                            blackhole.dec();
                            Bullet b = ammo_type.apply(aimer.apply());
                                   b.isBlackHole = true;
                        } else {
                            for (Double fire_angle : turrets.value()) {
                                Bullet b = ammo_type.apply(aimer.apply()+fire_angle);
                                       b.isHighEnergy = Ship.this instanceof Rocket && ((Rocket)Ship.this).energyfire.is();
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
                dx += cos(dir)*mobility.value()*thrust;
                dy += sin(dir)*mobility.value()*thrust;

                if (!isin_hyperspace) {
                    ttl--;
                    if (ttl<0) {
                        ttl = ROCKET_ENGINE_DEBRIS_TTL;
                        ROCKET_ENGINE_DEBRIS_EMITTER.emit(x,y,dir+PI, mobility.value());
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
                    pulseTTL = PULSE_ENGINE_PULSEPERIOD_TTL;
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
                    double direction = Ship.this.dir+PI;
                    x = Ship.this.x + shipDistance*cos(direction);
                    y = Ship.this.y + shipDistance*sin(direction);
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

                    double distx = distXSigned(x,o.x);
                    double disty = distYSigned(y,o.y);
                    double dist = dist(distx,disty)+1; // +1 avoids /0 " + dist);
                    double f = force(o.mass,dist);

                    // apply force
                    o.dx += distx*f/dist;
                    o.dy += disty*f/dist;
                }

                public double force(double mass, double dist) {
                    return dist==0 ? 0 : -255*mobility_multiplier_effect*ttl*ttl/(dist*dist*dist);
                }
            }
        }

        class Ability implements LO {
            AbilityState state = NO_CHANGE;
            double activation = 0; // 0==passive, 1 == active, 0-1 == transition state
            final double timeActivation; // seconds it takes to activate, 0 = instantanous
            final double timePassivation; // seconds it takes to passivate, 0 = instantanous// seconds it takes to passivate, 0 = instantanous
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
                if (state==NO_CHANGE) {
                    double min_energy_required = e_act+5*e_rate;
                    if (energy >= min_energy_required) {
                        energy -= min(energy,e_act);
                        state=ACTIVATING;
                        onActivateStart();
                    }
                }
            }
            void passivate() {
                if (state==NO_CHANGE) {
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
                        state = NO_CHANGE;
                        onActivateEnd();
                    }
                    onActiveChanged(activation);
                } else
                if (state==PASSSIVATING) {
                    activation = timePassivation==0 ? 0 : max(0,activation-1/(timePassivation*FPS));
                    if (activation==0) {
                        state = NO_CHANGE;
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
                super(true, DISRUPTOR_ACTIVATION_TIME,DISRUPTOR_PASSIVATION_TIME,DISRUPTOE_E_ACTIVATION,DISRUPTOR_E_RATE );
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

                    double distx = distXSigned(x,o.x);
                    double disty = distYSigned(y,o.y);
                    double dist = dist(distx,disty)+1; // +1 avoids /0 " + dist);
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
                    // Makes discruptor vs shield battles more interesting
                    if (o instanceof Rocket && ((Rocket)o).ability_main instanceof Shield && ((Rocket)o).ability_main.isActivated()) {
                        f *= -1;
                    } else
                    if (o instanceof Shuttle || o instanceof Satellite) {
                        noeffect = true;
                    }

                    // apply force
                    if (noeffect) return;
                    o.dx += distx*f/dist;
                    o.dy += disty*f/dist;
                }

                @Override
                public void doLoop() {
                    this.x = Ship.this.x;
                    this.y = Ship.this.y;
                    this.isin_hyperspace = Ship.this.isin_hyperspace; // must always match

                    if (activation==1 && !isin_hyperspace) {
                        // radiation - visually shows disruptor in effect
                        double angle = rand0N(D360);
                        double dist = 30+rand0N(30);
                        new Particle(x+dist*cos(angle),y+dist*sin(angle), dx,dy, ttl(millis(350))) {

                            @Override
                            void draw() {
                                GraphicsContext g = gc_bgr;
                                g.setGlobalAlpha(ttl);
                                g.setFill(game.humans.color);
                                g.fillOval(x-0.5,y-0.5,1,1);
                                g.setGlobalAlpha(1);
                            }

                        };

                        double strength = 17 - 2*cache_speed;
                        game.grid.applyExplosiveForce((float)strength, new Vec(x,y), 60);
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
                game.grid.applyExplosiveForce(90f, new Vec(x,y), 70);
            }
            void onPassivateStart() {
                game.grid.applyExplosiveForce(90f, new Vec(x,y), 70);
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
                    game.grid.applyImplosiveForce((float)strength, new Vec(x,y), 30);
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
                KSenergy_rateInit = KSenergy_max/ ttl(KINETIC_SHIELD_RECHARGE_TIME);
                KSenergy_rate = KSenergy_rateInit;
                KSenergy = KINETIC_SHIELD_INITIAL_ENERGY*KSenergy_max;
                KSradiusInit = RADIUS;
                KSradius = KSradiusInit;
                draw_ring = Ship.this instanceof Shuttle;
                postRadiusChange();
                children.add(this);
                scheduleActivation();

                double sync_reps = Ship.this instanceof Rocket ? ((Rocket)Ship.this).player.id :
                                   Ship.this instanceof Shuttle ? ((Shuttle)Ship.this).owner.player.id :
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
                        game.runNext.add(() -> new KineticShieldPiece(dir+largeLastPiece*piece_angle).max_opacity = 0.4);
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
                    double angle = dir+i*syncs_angle;
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
                    dirOffset = DIR-dir;
                    children.add(this);
                }

                public void doLoop() {
                    super.doLoop();
                    double KSPdir = dir+dirOffset;
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
                double bullet_range = r.calculateBulletRange();
                drawHudLine(x,y, 40, bullet_range, cosdir, sindir, HUD_COLOR);
                // drawHudCircle(x,y,bullet_range, HUD_COLOR); // nah drawing ranges is more cool
                drawHudCircle(x,y,bullet_range,r.dir,D30, HUD_COLOR);
                drawHudCircle(x,y,bullet_range,r.dir+D360/3,PI/8, HUD_COLOR);
                drawHudCircle(x,y,bullet_range,r.dir-D360/3,PI/8, HUD_COLOR);
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
                    gc.fillOval(modX(x+i*cosdir), modY(y+i*sindir), 2,2);
                gc.setGlobalAlpha(1);
            }
        }
    }

    /** Default player ship. */
    class Rocket extends Ship {

        final Player player;
        final InEffect rapidfire = new InEffect();
        final InEffectValue<Double> powerfire = new InEffectValue<>(times -> pow(1.5,times));
        final InEffectValue<Double> energyfire = new InEffectValue<>(times -> pow(2,times));
        final InEffect splitfire = new InEffect();
	    final double randomVoronoiTranslation = randOf(-1,1)*randMN(0.01,0.012);

        Rocket(Player PLAYER) {
            super(
                Rocket.class,
                playfield.getWidth()/2,playfield.getHeight()/2,0,0,PLAYER_HIT_RADIUS,
                graphics(MaterialDesignIcon.ROCKET,34,PLAYER.color.get(),null),
                // graphics(FontAwesomeIcon.ROCKET,40,PLAYER.color.get(),null), alternative
                PLAYER_ENERGY_INITIAL,PLAYER_E_BUILDUP
            );
            player = PLAYER;
//	        maintain(player.color, ((Icon)graphics).fillProperty()); // change color on the fly
            kinetic_shield = new KineticShield(ROCKET_KINETIC_SHIELD_RADIUS,ROCKET_KINETIC_SHIELD_ENERGYMAX);
            changeAbility(player.ability_type.get());
            engine = rand01()<0.5 ? new RocketEngine() : new PulseEngine();

            gun = new Gun(
                MANUAL,
                PLAYER_GUN_RELOAD_TIME,
                () -> dir,
                dir -> splitfire.is()
                    ? new SplitBullet(
                            this,
                            x + PLAYER_BULLET_OFFSET*cos(dir),
                            y + PLAYER_BULLET_OFFSET*sin(dir),
                            dx + energyfire.value()*powerfire.value()*cos(dir)*PLAYER_BULLET_SPEED,
                            dy + energyfire.value()*powerfire.value()*sin(dir)*PLAYER_BULLET_SPEED,
                            0,
                            PLAYER_BULLET_TTL
                        )
                    : new Bullet(
                            this,
                            x + PLAYER_BULLET_OFFSET*cos(dir),
                            y + PLAYER_BULLET_OFFSET*sin(dir),
                            dx + energyfire.value()*powerfire.value()*cos(dir)*PLAYER_BULLET_SPEED,
                            dy + energyfire.value()*powerfire.value()*sin(dir)*PLAYER_BULLET_SPEED,
                            0,
                            PLAYER_BULLET_TTL
                        )
            );
        }

        void draw() {
	        // gravity space contraction effect
	        // bug: interferes with hyperspace animation & graphics scaling
	        // Its also increasing game mechanics complexity wastefully -> disabled for now
	        // setScaleXY(graphics,0.4+0.6*(1-g_potential));

	        graphicsDir = D45 + dir; // add 45 degrees due to the graphics , TODO: fix this

            super.draw();


            if (game.humans.intelOn.is()) {
                double bullet_range = calculateBulletRange();
                drawHudCircle(x,y,bullet_range,HUD_COLOR);
            }

            if (gun.blackhole.is()) {
                double bullet_range = calculateBulletRange();
                gc.setFill(Color.BLACK);
                drawHudCircle(modX(x+bullet_range*cos(dir)),modY(y+bullet_range*sin(dir)), 50, HUD_COLOR);
            }

            if (game.pressedKeys.contains(player.keyLeft.get())) {
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir),y+10*sin(dir),dir+D90,engine.mobility.value());
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir+PI),y+10*sin(dir+PI),dir-D90,engine.mobility.value());
            }
            if (game.pressedKeys.contains(player.keyRight.get())) {
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir),y+10*sin(dir),dir-D90,engine.mobility.value());
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir+PI),y+10*sin(dir+PI),dir+D90,engine.mobility.value());
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
        void changeAbility(AbilityKind type ){
            if (ability_main!=null) ability_main.dispose();
            switch(type) {
                case DISRUPTOR : ability_main = new Disruptor(); break;
                case HYPERSPACE : ability_main =  new Hyperspace(); break;
                case SHIELD : ability_main =  new Shield(); break;
                default: throw new SwitchException(this);
            }
            children.add(ability_main);
        }

        boolean isHitDistance(PO o) {
            if (o instanceof Bullet)
                return isDistanceLess(o,kinetic_shield.KSradius+PLAYER_BULLET_SPEED/2+o.radius);
            if (o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
                return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
            return isDistanceLess(o,radius+o.radius);
        }

        double calculateBulletRange() {
            return energyfire.value()*powerfire.value()*PLAYER_BULLET_RANGE;
        }
    }
    /** Default enemy ship. */
    class Ufo extends Ship {
        boolean aggressive = false;
        Runnable radio = () -> game.ufos.pulseCall(this);
        Runnable discs = () -> {
            if (game.ufos.canSpawnDiscs) {
                game.ufos.canSpawnDiscs = false;
                repeat(5, i -> new UfoDisc(this,i*D360/5));
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

        Ufo() {
            this(randEnum(Side.class),false);
        }
        Ufo(Side side, boolean AGGRESSIVE) {
            super(
                Ufo.class,
                (side==Side.RIGHT ? 1 : 0) * playfield.getWidth(),
	            rand01()*playfield.getHeight(),0,0,UFO_HIT_RADIUS,
	            graphics(MaterialDesignIcon.BIOHAZARD,40,game.ufos.color,null),
                UFO_ENERGY_INITIAL,UFO_E_BUILDUP
            );
            dir = x<playfield.getWidth()/2 ? 0 : PI; // left->right || left<-right
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
                        if (dir==0)            dir = r<0.5 ? D45 : -D45;
                        else if (dir==D45)    dir = r<0.5 ? 0 : -D45;
                        else if (dir==-D45)   dir = r<0.5 ? 0 : D45;
                        else if (dir== PI)     dir = r<0.5 ? 3*D45 : 3*D45;
                        else if (dir== 3*D45) dir = r<0.5 ? PI : -3*D45;
                        else if (dir==-3*D45) dir = r<0.5 ? PI : 3*D45;
                        // preserve speed
                        // this causes movements changes to be abrupt (game is more dificult)
                        double s = speed();
                        dx = s*cos(dir);
                        dy = s*sin(dir);
                    }
                    dx += cos(dir)*UFO_ENGINE_THRUST;
                    dy += sin(dir)*UFO_ENGINE_THRUST;


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
            game.runNext.addPeriodic(() -> ttl(seconds(5)), radio);
            game.runNext.addPeriodic(() -> ttl(seconds(5)), discs);
        }

        @Override
        public void doLoop() {
            super.doLoop();
        }

        void doLoopOutOfField() {
            if (y<0) y = playfield.getHeight();
            if (y>playfield.getHeight()) y = 0;
            if (x<0 || x>playfield.getWidth()) dead = true;
        }

        @Override
        void draw() {
            super.draw();

            drawUfoRadar(x,y);

            // Use jump force to make the disc bounce smoothly only on inner side and bounce
            // instantly on the outer side. Standard force is more natural and 'biological', while
            // jump force loost more mechanical and alien.
            discdspeed += disc_forceJump(discpos);
            discpos += discdspeed;
            double dist = 40+discpos*20;
            drawUfoDisc(x+dist*cos(-3*D30),y+dist*sin(-3*D30));
            drawUfoDisc(x+dist*cos(-7*D30),y+dist*sin(-7*D30));
            drawUfoDisc(x+dist*cos(-11*D30),y+dist*sin(-11*D30));

            if (game.humans.intelOn.is())
                drawHudCircle(x,y,UFO_BULLET_RANGE,game.ufos.color);
        }
        @Override
        public void dispose() {
            super.dispose();
            game.runNext.remove(radio);
            game.runNext.remove(discs);
        }
    }
    /** Ufo heavy projectiles. Autonomous rocket-seekers. */
    class UfoDisc extends Ship {
        Rocket enemy = null;

        public UfoDisc(PO o, double DIR) {
            super(UfoDisc.class, o.x,o.y,0,0, UFO_DISC_HIT_RADIUS, null, UFO_ENERGY_INITIAL,UFO_E_BUILDUP);
            dir = DIR;
            engine = new Engine(){
                {
                    enabled = true;
                }
                @Override
                void onDoLoop() {
                    dx += cos(dir)*0.1;
                    dy += sin(dir)*0.1;
                }
            };
        }

        void move() {
            // prevents overlap using repulsion
            for (UfoDisc ud : game.oss.get(UfoDisc.class)) {
                if (ud==this) continue;
                double f = interUfoDiscForce(ud);
                boolean toright = x<ud.x;
                boolean tobottom = y<ud.y;
                dx += (toright ? 1 : -1) * f;
                dy += (toright ? 1 : -1) * f;
            }

            // look for enemy actively (not every cycle though)
            if (game.loopId %UFO_DISC_DECISION_TIME_TTL==0)
                enemy = findClosestRocketTo(this);

            if (enemy==null || enemy.player.rocket != enemy || enemy.isin_hyperspace) {
                engine.off();   // no enemy -> no pursuit -> no movement
            } else {
                engine.on();   // an enemy -> a pursuit -> a movement
                dir = dir(enemy); // pursuit
            }

            dx *= 0.96;
            dy *= 0.96;
        }

        void draw() {
            drawUfoDisc(x,y);
        }

        double interUfoDiscForce(UfoDisc ud) {
            double d = distance(ud);
            return d>15 ? 0 : -0.5*pow((1-d/15),2);
        }

        void explode() {
            dead = true;
            drawUfoDiscExplosion(x,y);
            for (UfoDisc ud : game.oss.get(UfoDisc.class)) {
                if (distance(ud)<=UFO_DISC_EXPLOSION_RADIUS)
                    game.runNext.add(millis(100),ud::explode);
            }
        }
    }
    /** Non-interactive mission info button. */
    class MissionInfoButton extends PO {

        MissionInfoButton() {
            super(MissionInfoButton.class, 0, 0, 0, 0, 0, graphics(FontAwesomeIcon.INFO,15,game.humans.color,null));
            x = rand0N(playfield.getWidth());
            y = rand0N(playfield.getHeight());
            // graphics.setOnMouseClicked(e -> new MissionPane().show(game.mission));
	        if (playfield.getScene()!=null)
	        playfield.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
	        	if ((x-e.getX())*(x-e.getX())+(y-e.getY())*(y-e.getY())<=10*10) {
			        new MissionPane().show(game.mission);
			        e.consume();
		        }
	        });
        }

        @Override
        void init() {}

        @Override
        public void dispose() {}

        @Override
        public void doLoop() {
            super.doLoop();
            applyPlayerRepulseForce(this,400);
        }
    }

    private void drawUfoDisc(double x, double y) {
        gc.setGlobalAlpha(0.5);
        gc.setStroke(game.ufos.color);
        gc.strokeOval(x-UFO_DISC_RADIUS,y-UFO_DISC_RADIUS,2*UFO_DISC_RADIUS,2*UFO_DISC_RADIUS);
        gc.strokeOval(x-UFO_DISC_RADIUS,y-UFO_DISC_RADIUS,2*UFO_DISC_RADIUS,2*UFO_DISC_RADIUS);
        gc.strokeOval(x-UFO_DISC_RADIUS,y-UFO_DISC_RADIUS,2*UFO_DISC_RADIUS,2*UFO_DISC_RADIUS);
        gc.setGlobalAlpha(1);
        gc.setStroke(null);
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
			// dir += graphicsDir;   // produces interesting effect
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
			// dir += graphicsDir;   // produces interesting effect
			graphicsDir += graphicsDirBy;
			super.draw();
		}
		@Override void die(Object cause) {
			super.die(cause);
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
                (dir==Side.LEFT ? 0 : 1)*playfield.getWidth(), rand01()*playfield.getHeight(),
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
            if (y<0) y = playfield.getHeight();
            if (y>playfield.getHeight()) y = 0;
            if (isLarge) {
                if (x<0 || x>playfield.getWidth()) dead = true;
            } else {
                if (x<0) x = playfield.getWidth();
                if (x>playfield.getWidth()) x = 0;
            }
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

        Bullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL) {
            super(Bullet.class,x,y,dx,dy,hit_radius,null);
            owner = ship;
            color = game.mission.color;
            ttl = 1;
            ttl_d = 1/TTL;
        }

        @Override
        public void doLoop() {
            x += dx;
            y += dy;
            doLoopOutOfField();
            draw();
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
//	        double r = isHighEnergy ?  5 : 2;
//          gc.setFill(color);
//	        gc.fillOval(x-1,y-1,r,r);

//            // line bullets
            GraphicsContext g = gc_bgr;
            g.setGlobalAlpha(0.4);
            g.setStroke(color);
            g.setLineWidth(isHighEnergy ?  5 : 3);
            g.strokeLine(x,y,x+dx*0.9,y+dy*0.9);
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
            double speedsqr = dx*dx+dy*dy;
            if (speedsqr>100) { // if speed > 5px/frame
                double speed = sqrt(speedsqr);
                int iterations = (int) speed/5;
                for (int i=-(iterations-1); i<=0; i++) {
                    boolean washit = checkWithXY(e, x+dx*i/iterations, y+dy*i/iterations);
                    if (washit) break;
                }
            } else {
                checkWithXY(e,x,y);
            }
        }

        boolean checkWithXY(PO e, double X, double Y) {
            if (dead || e.dead) return true;  // dead objects must not participate

	        // hack, we temporarily set old co--ordinates, we must restore this before method returns
	        double tempX = x, tempY = y;
	        x = X; y = Y;

            if (e.isHitDistance(this)) {
                dead = true; // bullet always dies
                if (e instanceof Rocket) {
                    Rocket r = (Rocket)e;
                    if (!game.deadly_bullets.get() && owner instanceof Rocket) {
                        r.kinetic_shield.new KineticShieldPiece(r.dir(this));
                    }
                    if (game.deadly_bullets.get() || !(owner instanceof Rocket)) {
                        if (r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                            r.dx = r.dy = 0;
                            r.engine.off();
                        } else {
                            r.player.die();
                        }
                    }
                } else
                if (e instanceof Asteroid) {
                    Asteroid a = (Asteroid)e;
                    a.onHit(this);

                    if (owner instanceof Rocket)
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_ASTEROID(a));

                    new FermiGraphics(e.x, e.y, e.radius*2.5);
//                            gc_bgr.setGlobalAlpha(0.2);
//                            gc_bgr.setFill(mission.color);
//                            drawOval(gc_bgr,b.x,b.y,100);
//                            gc_bgr.setGlobalAlpha(1);
                } else
                if (e instanceof Ufo) {
                    Ufo u = (Ufo)e;
                    if (!(owner instanceof Ufo)) {
                        u.dead = true;
                        game.ufos.onUfoDestroyed();
                        drawUfoExplosion(u.x,u.y);
                    }
                    if (owner instanceof Rocket)
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_UFO);
                } else
                if (e instanceof UfoDisc) {
                    UfoDisc ud = (UfoDisc)e;
                    if (owner instanceof Rocket) {
                        ud.explode();
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_UFO_DISC);
                    }
                } else
                if (e instanceof Shuttle) {
	                dead = false;   // shoot-through when allies
                } else
                if (e instanceof SuperShield) {
                	// we are assuming its kinetic shield is always active (by game design)
	                // ignore bullets when allies | shooting from inside the shield
	                if (owner instanceof Rocket || owner.distance(e)<((Ship)e).kinetic_shield.KSradius) {
		                dead = false;
	                } else {
		                ((Ship)e).kinetic_shield.new KineticShieldPiece(e.dir(this));
	                }
                } else
                if (e instanceof Satellite) {
                    Satellite s = (Satellite)e;
                    if (s.isLarge) s.explode();
                    else dead = false; // small satellites are shoot-through
                }

                boolean wasHit = dead;
                if (isHighEnergy) dead &= false;
                if (dead) onExpire(e);

	            x = tempX; y = tempY;
                return wasHit;
            }

	        x = tempX; y = tempY;
            return false;
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
                game.runNext.add(() -> {
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

        void draw() {
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

        @Override
        public void doLoop() {
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

        @Override
        public void doLoop() {
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

        @Override
        void draw() {
            super.draw();
            gc.setStroke(game.ufos.color);
            double r = 100 - (100-radius)*(1-ttl);
            gc.strokeRect(x-r, y-r, 2*r, 2*r);
        }

        @Override
        void checkCollision(PO e) {
            // We want the bullet to only hit once so we perform the check only once - when
            // bullet expires
            if (dead && ttl<=0)  {
                dead = false; // needs to be set first
                isin_hyperspace = target!=null && target.isin_hyperspace; // hyperspace targets too
                super.checkCollision(e);
                dead = true; // absolutely preserve the value
            }
        }

        @Override
        void onExpire(PO cause) {}
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

        void init() {
            game.oss.add(this);
        }
        void move(){}
        public void doLoop() {
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
        void draw() {}
        void onExpire() {}
    }
    interface ParticleEmitter {
        void emit(double x, double y, double dir, double param1);
    }


    class RocketEngineDebris extends Particle {

        RocketEngineDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
            super(x,y,dx,dy,ttlmultiplier* ttl(millis(150)));
        }

        @Override
        void draw() {
            GraphicsContext g = gc_bgr;
            g.setGlobalAlpha(ttl);
            g.setFill(game.humans.color);
            g.fillOval(x-1,y-1,1,1);
            g.setGlobalAlpha(1);
        }
    }
    private final ParticleEmitter ROCKET_ENGINE_DEBRIS_EMITTER = (x,y,dir,strength) -> {
        double dispersion_angle = D45;
        double d1 = dir + (rand01())*dispersion_angle;
//        double d4 = dir + .5*(rand01())*dispersion_angle;
//        double d2 = dir - (rand01())*dispersion_angle;
        double d3 = dir - .5*(rand01())*dispersion_angle;
        game.runNext.add(() -> {
            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d1),1*sin(d1),strength);
//            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d2),1*sin(d2),strength);
            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d3),1*sin(d3),strength);
//            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d4),1*sin(d4),strength);
        });
    };
    class PulseEngineDebris extends Particle {

        PulseEngineDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
            super(x,y,dx,dy,ttlmultiplier* ttl(millis(250)));
        }

        void draw() {
            GraphicsContext g = gc_bgr;
            g.setGlobalAlpha(ttl);
            g.setFill(game.humans.color);
            g.fillOval(x-1,y-1,2,2);
            g.setGlobalAlpha(1);
        }
    }
    class UfoEngineDebris extends Particle {

    	@SuppressWarnings("unused")
        public UfoEngineDebris(double x, double y, double radius) {
            super(x,y,0,0, ttl(seconds(0.5)));
        }

        void draw() {
//            double r = 5+radius*(ttl);
//            double d = r*2;
//            gc_bgr.setGlobalAlpha(1);
//            gc_bgr.setGlobalBlendMode(MULTIPLY);
//            gc_bgr.setFill(new RadialGradient(0,0,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0.6*ttl,Color.BLACK),new Stop(0.8*ttl,game.missions.get(game.mission_id).color),new Stop(1,Color.TRANSPARENT)));
//            gc_bgr.fillOval(x-r,y-r,d,d);
//            gc_bgr.setGlobalBlendMode(SRC_OVER);
//            gc_bgr.setGlobalAlpha(1);
        }
    }
    /** Omnidirectional expanding wave. Represents active communication of the ship. */
    class RadioWavePulse extends Particle {
        final double dxy;
        final boolean rect;
        double radius = 0;
        final Color color;
        final boolean inverse;

        RadioWavePulse(double x, double y, double dx, double dy, double EXPANSION_RATE, Color COLOR, boolean RECTANGULAR) {
            super(x,y,dx,dy, ttl(seconds(2)));
            dxy = EXPANSION_RATE;
            inverse = dxy<0;
            color = COLOR;
            rect = RECTANGULAR;
            radius = inverse ? -dxy*ttl/ttld : 0;
        }

        void draw() {
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

        public void doLoop() {
            radius += dxy;
            super.doLoop();
        }
        void move() {
            dx = 0.95*s.dx;
            dy = 0.95*s.dy;
        }

        void draw() {
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

        void move() {}
        void draw() {
            throw new UnsupportedOperationException();
        }
        void onHit(SO o) {
            hits++;
            if (!(o instanceof Bullet) || hits>hits_max) split(o);
            else size *= size_hitdecr;
            onHitParticles(o);
        }
        final void split(SO o) {
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
        abstract void onHitParticles(SO o);
    }

    private interface Mover {
        void calcSpeed(Asteroid o);
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
    private static final Color ffff = Color.valueOf("#1f93ff");
    private class Energ extends Asteroid<OrganelleMover> {
        Color colordead = Color.BLACK;
        Color coloralive;
        double heartbeat = 0;
        double heartbeat_speed = 0.5*D360/ ttl(seconds(1+rand0N(size/30))); // times/sec

        public Energ(double X, double Y, double SPEED, double DIR, double LIFE) {
            super(X, Y, SPEED, DIR, LIFE);
            propulsion = new OrganelleMover();
            coloralive = Color.DODGERBLUE;
            size = LIFE;
            radius = INKOID_SIZE_FACTOR*size;
            size_hitdecr = 0.98;
            size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
            splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
            hits_max = splits>2 ? 1 : 0;
        }

        public void doLoop() {
            heartbeat += heartbeat_speed;
            super.doLoop();
        }

        void draw() {
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
        }

        void onHitParticles(SO o) {
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
            void draw() {
                gc_bgr.setFill(ttl<0.5 ? colordead : coloralive); // crisp
                // gc_bgr.setFill(colordead.interpolate(coloralive, sqrt(ttl))); // smooth
                gc_bgr.fillOval(x,y,r,r);
            }
        }
    }
    private class Energ2 extends Energ {
        public Energ2(double X, double Y, double SPEED, double DIR, double RADIUS) {
            super(X, Y, SPEED, DIR, RADIUS);
            coloralive = Color.rgb(244,48,48);
            hits_max = splits;
        }
        void draw() {
            gc.setGlobalBlendMode(DARKEN);
            gc.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0,coloralive),new Stop(0.2,coloralive),new Stop(0.5,Color.BLACK),new Stop(1,Color.TRANSPARENT)));
            drawOval(gc,x,y,radius);
            gc.setGlobalBlendMode(SRC_OVER);
        }
    }
    private interface Draw2 {
        void drawBack();
        void drawFront();
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

        @Override
        public void doLoop() {
            super.doLoop(); //To change body of generated methods, choose Tools | Templates.
            applyPlayerRepulseForce(this,600);
        }

        void onHit(PO o) {
            super.onHit(o);
        }
        void draw() {
            double d = radius*2;
            gc_bgr.setFill(game.mission.color);
            gc_bgr.fillOval(x-radius,y-radius,d,d);
        }
        void onHitParticles(SO o) {}
    }
    private class PlanetoCircle extends Asteroid<OrganelleMover> {

        public PlanetoCircle(double X, double Y, double SPEED, double DIR, double LIFE) {
            super(X, Y, SPEED, DIR, LIFE);
            size = LIFE;
            radius = INKOID_SIZE_FACTOR*size;
            size_hitdecr = 1;
            size_child = 0.5; // 1 * 1 -> (3-4) * 0.5 -> 2 * 0.25 -> 2 * 0.125
            splits = size>0.5 ? randOf(3,4) : size>0.125 ? 2 : 0;
            hits_max = splits>2 ? 1 : 0;
        }

        void onHit(PO o) {
            super.onHit(o);
        }
        void draw() {
            double d = radius*2;
            gc_bgr.setStroke(game.mission.color);
            gc_bgr.setLineWidth(3);
            gc_bgr.strokeOval(x-radius,y-radius,d,d);
            gc_bgr.setStroke(null);
        }
        void onHitParticles(SO o) {}
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

        void onHit(PO o) {
            super.onHit(o);
            propulsion.dirchange *= 2; // speed rotation up
            propulsion.ttldirchange = -1; // change direction now
        }
        void draw() {
            new InkoidGraphics(x,y,radius);
        }
        void onHitParticles(SO o) {
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

            void draw() {
                r = radius*ttl;
            }

            public void drawBack() {
                double rr = 2+r;
                double d = rr*2;
                gc_bgr.setFill(game.mission.color);
                gc_bgr.fillOval(x-rr,y-rr,d,d);
            }
            public void drawFront() {
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

        public void doLoop() {
            super.doLoop();
            circling += circling_speed;
            circling_mag = sin(circling);
            trail.run();
        }

        @Override
        void doLoopEnd() {
            super.doLoopEnd();
        }

        void onHit(PO o) {
            super.onHit(o);
            propulsion.dirchange *= 2; // speed rotation up
            propulsion.ttldirchange = -1; // change direction now
        }
        void draw() {
            new GenoidGraphics(x,y,radius);
        }
        void onHitParticles(SO o) {
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

            void draw() {
                r = radius*ttl;
            }

            public void drawBack() {
                double rr = ttl*(2+r);
                double d = rr*2;

                // we are only interested in the border & strokeOval performs > fillOval
                // gc_bgr.setFill(game.mission.color);
                // gc_bgr.fillOval(x-rr,y-rr,d,d);
                gc_bgr.setStroke(game.mission.color);
                gc_bgr.setLineWidth(3);
                gc_bgr.strokeOval(x-rr,y-rr,d,d);
            }
            public void drawFront() {
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

        void onHit(PO o) {
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
        public void doLoop(){}
    }
    class BlackHole extends ForceField {
        Player owner = null;
        double life;
        double ttl = 1;
        double ttld = 0;
        double radius_even_horizon_max = 20;
        double radius_even_horizon;
        double radius_ergosphere = 512;
        double radius_gravity = 1000;
        double dir = 0;
        double mass = 0;
        double magnetic_force_dir = rand0N(D360);

        public BlackHole(Player OWNER, Duration TTL, double X, double Y) {
            x=X; y=Y; ttl=1; ttld=1/ ttl(TTL); owner = OWNER;
        }

        public void die() {
            dead = true;
            game.grid.applyExplosiveForce(200, new Vec(x,y), 100);
        }

        @Override
        void apply(PO o) {
            // Gravity affects all dimensions. Hyperspace is irrelevant.
            // if (ff.isin_hyperspace!=o.isin_hyperspace) return;

            double distx = distXSigned(x,o.x);
            double disty = distYSigned(y,o.y);
            double dist = dist(distx,disty)+1; // +1 avoids /0 or /very_small_number);
            double f = force(o.mass,dist);

            double gravity_potential_inv = computeForceInversePotential(dist,radius_gravity);
	        // double gravity_potential = 1-gravity_potential_inv;

            // Used for gravity based effects. E.g. time/space dilatation.
            o.g_potential *= gravity_potential_inv;

            // Bullets are affected more & stop aging
            if (o instanceof Bullet && dist<220)
                f = f*9; // add more force

            boolean isRocket = o instanceof Rocket;

            // Ergosphere. Rockets rotate towards force origin - black hole.
            // This actually simplifies near-BH control (it allows orbiting BH using just 1 key)
            // and its a nice effect.
            if (isRocket && dist<radius_ergosphere) {
                double ergo_potentiall_inv = computeForceInversePotential(dist,radius_ergosphere);
                double ergo_potential = 1-ergo_potentiall_inv;
                double dir = o.dir(this);
                Rocket r = (Rocket)o;
                double angle = r.dir-dir;
//                double angled = 0.1 * dist01*dist01 * sin(angle);
                double angled = 0.06 * ergo_potential * sin(angle);
                r.dir -= angled; // no idea why - and not +
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
            o.dx += distx*f/dist;
            o.dy += disty*f/dist;
        }

        @Override
        public double force(double mass, double dist) {
//            if (dist>220) return 0.03*(1-dist/1000);
//            else return 0.02+0.18*(1-dist/220);

            return 0.3*pow(1-dist/radius_gravity,6);

//            if (dist>220) return 0.02*(1-dist/1000);
//            else return 0.02+0.18*(1-pow(dist/220,2));
        }

        @Override
        public void doLoop() {
            // lifecycle
            ttl -= ttld;
            // life = ttl;              // time limit death
            life = (4000-mass)/4000;    // death when mass==5000
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


            // hawking radiation + magnetic particle acceleration
            double d = rand0N(D360);
            // double speed_base = 4;
            // double magnetic_force = (abs(D90-(d%PI))/(D90)) < PI/10 ? 1 : 0;
            //        magnetic_force = 0;
            // double s = speed_base + 6*magnetic_force*magnetic_force*magnetic_force; // use somehow
            // new RocketEngineDebris(x+50*cos(d),y+50*sin(d), s*cos(d+0.1),s*sin(d+0.1), durToTtl(millis(500)));
            // new RocketEngineDebris(x+100*cos(d),y+100*sin(d), 3*cos(d-D90+0.1),3*sin(d-D90+0.1), d urToTtl(millis(500)));

            // forms acretion disc
            new RocketEngineDebris(x+50*cos(d),y+50*sin(d), 4*cos(d-D90+0.1),4*sin(d-D90+0.1), ttl(millis(500)));

            gc.setFill(Color.BLACK);
            drawOval(gc, x,y,radius_even_horizon);

            if (game.mission.id==4) {
                gc_bgr.setGlobalBlendMode(OVERLAY);
                gc_bgr.setGlobalAlpha(1-life);
    //            gc_bgr.setGlobalAlpha(0.1*(1-ttl));
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

                double distx = distXSigned(x,p.x);
                double disty = distYSigned(y,p.y);
                double dist = dist(distx,disty)+1; // +1 avoids /0
                double f = force(p.mass,dist);

                p.dx *= 0.99;
                p.dy *= 0.99;
                p.dx += distx*f/dist;
                p.dy += disty*f/dist;

                // Overload effect
                // Too many particles cause BH to erupt some.
                // Two reasons:
                //    1) BHs stop particle aging and cause particle count explotion (intended) and
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

                    // dont age near black hole
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

	static class Displayable {
		final String name;
		final String description;
		final GlyphIcons icon;

		public Displayable(String name, GlyphIcons icon, CharSequence... description) {
			this.name = name;
			this.description = String.join("\n", description);
			this.icon = icon;
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
	/**
	 *
	 */
	static class Achievement extends Displayable {
		final Ƒ1<Game,Set<Player>> evaluator;

		private Achievement(String NAME, GlyphIcons ICON, Ƒ1<Game,Set<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			super(NAME, ICON, DESCRIPTION);
			evaluator = EVALUATOR;
		}

		static Achievement achievement1(String NAME, GlyphIcons ICON, Ƒ1<? super Game,? extends Player> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> Collections.singleton(EVALUATOR.apply(game)), DESCRIPTION);
		}

		static Achievement achievement01(String NAME, GlyphIcons ICON, Ƒ1<? super Game,? extends Optional<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, game -> EVALUATOR.apply(game).stream().collect(toSet()), DESCRIPTION);
		}

		static Achievement achievement0N(String NAME, GlyphIcons ICON, Ƒ1<Game,Set<Player>> EVALUATOR, CharSequence... DESCRIPTION) {
			return new Achievement(NAME, ICON, EVALUATOR, DESCRIPTION);
		}
	}

/* ------------------------------------------------------------------------------------------------------------------ */

    /** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.width)*/
    public double modX(double x) {
        if (x<0) return modX(playfield.getWidth()+x);
        else if (x>playfield.getWidth()) return modX(x-playfield.getWidth());
        else return x;
    }
    /** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.height)*/
    public double modY(double y) {
        if (y<0) return modY(playfield.getHeight()+y);
        else if (y>playfield.getHeight()) return modY(y-playfield.getHeight());
        else return y;
    }
    public double distX(double x1, double x2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return abs(x1-x2);

        if (x1<x2) return min(x2-x1, x1+playfield.getWidth()-x2);
        else return min(x1-x2, x2+playfield.getWidth()-x1);
    }
    public double distY(double y1, double y2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return abs(y1-y2);

        if (y1<y2) return min(y2-y1, y1+playfield.getHeight()-y2);
        else return min(y1-y2, y2+playfield.getHeight()-y1);
    }
    public double distXSigned(double x1, double x2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return x1-x2;

        if (x1<x2) {
            double d1 = x2-x1;
            double d2 = x1+playfield.getWidth()-x2;
            return d1<d2 ? -d1 : d2;
        } else {
            double d1 = x1-x2;
            double d2 = x2+playfield.getWidth()-x1;
            return d1<d2 ? d1 : -d2;
        }
    }
    public double distYSigned(double y1, double y2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return y1-y2;

        if (y1<y2) {
            double d1 = y2-y1;
            double d2 = y1+playfield.getHeight()-y2;
            return d1<d2 ? -d1 : d2;
        } else {
            double d1 = y1-y2;
            double d2 = y2+playfield.getHeight()-y1;
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

    /** Finds closest non-hyperspacing rocket to the obejct. */
    Rocket findClosestRocketTo(SO to) {
        return stream(game.oss.get(Rocket.class)).filter(r -> !r.isin_hyperspace)
            .minBy(to::distance).orElse(null);
    }

    /** Applies repulsive force from every player. */
    void applyPlayerRepulseForce(PO o, double maxdist) {
        double fx = 0;
        double fy = 0;
        for (Player p : game.players) {
            if (p.rocket!=null) {
                double distx = distXSigned(o.x,p.rocket.x);
                double disty = distYSigned(o.y,p.rocket.y);
                double dist = dist(distx,disty)+1;
                double f = 1 - min(1,dist/maxdist);
                fx += distx*f*f*f/dist;
                fy += disty*f*f*f/dist;
            }
        }
        o.dx += fx;
        o.dy += fy;
        o.dx *= 0.9;
        o.dy *= 0.9;
    }
}