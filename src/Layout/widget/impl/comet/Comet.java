/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.impl.comet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import Configuration.Config;
import Configuration.IsConfig;
import Layout.widget.IsWidget;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import Layout.widget.impl.comet.Comet.Ship.Ability;
import Layout.widget.impl.comet.Comet.Ship.Engine;
import Layout.widget.impl.comet.Comet.Ship.KineticShield;
import Layout.widget.impl.comet.Comet.Ship.KineticShield.KineticShieldPiece;
import Layout.widget.impl.comet.Comet.Ship.Shield;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.itemnode.ConfigField;
import gui.objects.Text;
import gui.objects.icon.Icon;
import util.access.V;
import util.animation.Anim;
import util.animation.Loop;
import util.async.executor.FxTimer;
import util.collections.map.ClassMap;
import util.collections.mapset.MapSet;
import util.functional.Functors.Ƒ0;
import util.functional.Functors.Ƒ1;
import util.functional.Functors.Ƒ5;
import util.reactive.RunnableSet;

import static Layout.widget.impl.comet.Comet.AbilityKind.SHIELD;
import static Layout.widget.impl.comet.Comet.AbilityState.ACTIVATING;
import static Layout.widget.impl.comet.Comet.AbilityState.NO_CHANGE;
import static Layout.widget.impl.comet.Comet.AbilityState.PASSSIVATING;
import static Layout.widget.impl.comet.Comet.GunControl.AUTO;
import static Layout.widget.impl.comet.Comet.GunControl.MANUAL;
import static Layout.widget.impl.comet.Comet.PlayerSpawners.CIRCLE;
import static Layout.widget.impl.comet.Comet.Side.LEFT;
import static Layout.widget.impl.comet.Comet.Side.RIGHT;
import static gui.objects.Window.stage.UiContext.showSettings;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.random;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.util.Collections.EMPTY_SET;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.geometry.Pos.BOTTOM_RIGHT;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.geometry.Pos.TOP_RIGHT;
import static javafx.scene.effect.BlendMode.ADD;
import static javafx.scene.effect.BlendMode.DARKEN;
import static javafx.scene.effect.BlendMode.OVERLAY;
import static javafx.scene.effect.BlendMode.SRC_OVER;
import static javafx.scene.effect.BlurType.GAUSSIAN;
import static javafx.scene.input.KeyCode.DIGIT0;
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
import static javafx.scene.paint.CycleMethod.NO_CYCLE;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.minutes;
import static javafx.util.Duration.seconds;
import static util.Util.clip;
import static util.async.Async.run;
import static util.functional.Util.by;
import static util.functional.Util.filter;
import static util.functional.Util.findFirst;
import static util.functional.Util.forEachCartesian;
import static util.functional.Util.forEachCartesianHalfNoSelf;
import static util.functional.Util.isInR;
import static util.functional.Util.list;
import static util.functional.Util.listF;
import static util.functional.Util.listRO;
import static util.functional.Util.mapB;
import static util.functional.Util.repeat;
import static util.functional.Util.set;
import static util.functional.Util.stream;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layStack;
import static util.graphics.Util.layVertically;
import static util.graphics.Util.setAnchor;
import static util.graphics.Util.setScaleXY;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Comet",
    description = "Port of the game Comet.",
    howto = "",
    notes = "",
    version = "0.6",
    year = "2015",
    group = Widget.Group.OTHER
)
public class Comet extends ClassController {

    final Pane playfield = new Pane();  // playfield, contains scenegraph game graphics
    final GraphicsContext gc; // draws canvas game graphics on canvas
    final GraphicsContext gc_bgr; // draws canvas game graphics on bgrcanvas
    final Text message = new Text();
    final Game game = new Game();
    final RunnableSet every200ms = new RunnableSet();
    final FxTimer timer200ms = new FxTimer(200,-1,every200ms);
    Bloom be = new Bloom(0.2);
    public Comet() {
        // message
        message.setOpacity(0);

        // playfield
        Rectangle playfieldMask = new Rectangle();
        playfieldMask.widthProperty().bind(playfield.widthProperty());
        playfieldMask.heightProperty().bind(playfield.heightProperty());
        playfield.setClip(playfieldMask);
        playfield.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        playfield.getStyleClass().add("comet-bgr");
        // the below can produce wrap effect and draw object on both sides of the screen when
        // near edge, but produces graphical artefacts, investigate
        // DisplacementMap ef = new DisplacementMap();
        // ef.setWrap(true);
        // ef.setOffsetX(0.1);
        // ef.setOffsetY(0.1);
        // playfield.setEffect(ef);

        // canvas
        Canvas canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        canvas.widthProperty().bind(playfield.widthProperty());
        canvas.heightProperty().bind(playfield.heightProperty());
        canvas.setManaged(false);
        Canvas canvas_bgr = new Canvas();
        canvas_bgr.widthProperty().bind(playfield.widthProperty());
        canvas_bgr.heightProperty().bind(playfield.heightProperty());
        gc_bgr = canvas_bgr.getGraphicsContext2D();
        canvas_bgr.setManaged(false);
        canvas_bgr.setEffect(be);

        // player stats
        double G = 10; // padding
        StackPane playerStats = layStack(
            layHorizontally(G,TOP_LEFT,     createPlayerStat(PLAYERS.get(0)),createPlayerStat(PLAYERS.get(4))),TOP_LEFT,
            layHorizontally(G,TOP_RIGHT,    createPlayerStat(PLAYERS.get(1)),createPlayerStat(PLAYERS.get(5))),TOP_RIGHT,
            layHorizontally(G,BOTTOM_LEFT,  createPlayerStat(PLAYERS.get(2)),createPlayerStat(PLAYERS.get(6))),BOTTOM_LEFT,
            layHorizontally(G,BOTTOM_RIGHT, createPlayerStat(PLAYERS.get(3)),createPlayerStat(PLAYERS.get(7))),BOTTOM_RIGHT
        );
        playerStats.setPadding(new Insets(G,0,G,G));
        playerStats.setMouseTransparent(true);
        game.players.addListener((Change<? extends Player> change) -> {
            playerStats.getChildren().forEach(cc -> ((Pane)cc).getChildren().forEach(c ->
                c.setVisible(game.players.stream().anyMatch(p -> p.id==(Integer)c.getUserData()))
            ));
        });

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
                new Icon<>(FontAwesomeIcon.GEARS,14,"Settings").onClick(e -> showSettings(getWidget(),e))
            ),
            0d,0d,null,0d,
            layStack(canvas_bgr, canvas, playfield, playerStats, message),
            20d,0d,0d,0d
        );

        // keys
        playfield.addEventFilter(KEY_PRESSED, e -> {
            KeyCode cc = e.getCode();
            boolean first_time = game.pressedKeys.add(cc);
            if(first_time) {
                game.keyPressTimes.put(cc,System.currentTimeMillis());
                game.players.stream().filter(p -> p.alive).forEach(p -> {
                    if(cc==p.keyAbility.getValue()) p.rocket.ability_main.onKeyPress();
                    if(cc==p.keyThrust.getValue()) p.rocket.engine.on();
                    if(cc==p.keyFire.getValue()) p.rocket.gun.fire();
                });
                // cheats
                if(cc==DIGIT1) game.nextFrameJobs.add(() -> repeat(5, i -> game.mission.spawnPlanetoid()));
                if(cc==DIGIT2) game.nextFrameJobs.add(() -> repeat(5, i -> new Ufo()));
                if(cc==DIGIT3) game.nextFrameJobs.add(() -> repeat(5, i -> new Satellite()));
                if(cc==DIGIT4) game.nextFrameJobs.add(() -> {
                    game.oss.forEach(Asteroid.class,a -> a.canDispose=true);
                    game.nextMission();
                });
                if(cc==DIGIT5) game.players.stream().filter(p -> p.alive).map(p -> p.rocket).forEach(game.humans::sendShuttle);
                if(cc==DIGIT6) ;
                if(cc==DIGIT7) ;
                if(cc==DIGIT8) ;
                if(cc==DIGIT9) ;
                if(cc==DIGIT0) ;
            }
        });
        playfield.addEventFilter(KEY_RELEASED, e -> {
            game.players.stream().filter(p -> p.alive).forEach(p -> {
                if(e.getCode()==p.keyAbility.getValue()) p.rocket.ability_main.onKeyRelease();
                if(e.getCode()==p.keyThrust.getValue()) p.rocket.engine.off();
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

    static double SIN45 = sin(PI/4);
    static double FPS = 60; // frames per second (locked)
    static double FPS_KEY_PRESSED = 40; // frames per second
    static double FPS_KEY_PRESSED_PERIOD = 1000/FPS_KEY_PRESSED; // ms

    static int PLAYER_LIVES_INITIAL = 5; // lives at the beginning of the game
    static int PLAYER_SCORE_NEW_LIFE = 10000; // we need int since we make use of int division
    static double SCORE_ASTEROID(Asteroid a) { return 30 + 2000/(4*a.radius); };
    static double SCORE_UFO = 350;
    static double BONUS_MOBILITY_MULTIPLIER = 1.25; // coeficient
    static double BONUS_LASER_MULTIPLIER_LENGTH = 400; // px
    static Duration PLAYER_RESPAWN_TIME = seconds(3); // die -> respawn time
    static double ROTATION_SPEED = 1.3*PI/FPS; // 540 deg/sec.
    static double RESISTANCE = 0.98; // slow down factor
    static int ROT_LIMIT = 70; // smooths rotation at small scale, see use
    static int ROT_DEL = 7; // smooths rotation at small scale, see use

    static double PLAYER_BULLET_SPEED = 380/FPS; // bullet speed in px/s/fps
    static double PLAYER_BULLET_TTL = 0.8*FPS; // bullet time to live == 1s
    static double PLAYER_BULLET_RANGE = PLAYER_BULLET_SPEED*PLAYER_BULLET_TTL;
    static double PLAYER_BULLET_OFFSET = 10; // px
    static double PLAYER_ENERGY_INITIAL = 5000;
    static double PLAYER_E_BUILDUP = 1; // energy/frame
    static double PLAYER_HIT_RADIUS = 15; // energy/frame
    static Duration PLAYER_GUN_RELOAD_TIME = millis(100); // default ability
    static AbilityKind PLAYER_ABILITY_INITIAL = SHIELD; // rocket fire-fire time period
    static double PLAYER_GRAPHICS_ANGLE_OFFSET = PI/4;

    static double ROCKET_ENGINE_THRUST = 0.16; // px/s/frame
    static double ROCKET_ENGINE_DEBRIS_TTL = durToTtl(millis(20));
    static double PULSE_ENGINE_PULSEPERIOD_TTL = durToTtl(millis(20));
    static double PULSE_ENGINE_PULSE_TTL = durToTtl(millis(400));
    static double PULSE_ENGINE_PULSE_TTL1 = 1/PULSE_ENGINE_PULSE_TTL; // saves us computation

    // kinetic shield is very effective and can disrupt game balance, thus
    // it should have very low energy accumulation to prevent overuse
    // 1) it should NOT make player prefer being static (which by design it does)
    // 2) it should be able to handle multiple simultaneous hits (e.g. most of entire big asteroid explosion spawn)
    // 3) player should NEVER want to rely on it intentionally
    // 4) it should only prevent player to die accidental death
    static double KINETIC_SHIELD_INITIAL_ENERGY = 0.5; // 0-1 coeficient
    static Duration KINETIC_SHIELD_RECHARGE_TIME = minutes(4);
    static double ROCKET_KINETIC_SHIELD_RADIUS = 25; // px
    static double ROCKET_KINETIC_SHIELD_ENERGYMAX = 5000; // energy
    static double SHUTTLE_KINETIC_SHIELD_RADIUS = 180; // px
    static double SHUTTLE_KINETIC_SHIELD_ENERGYMAX = 1000000; // energy
    static double SHIELD_E_ACTIVATION = 0; // energy
    static double SHIELD_E_RATE = 30; // energy/frame
    static double SHIELD_RADIUS = 20; // px
    static Duration SHIELD_ACTIVATION_TIME = millis(0);
    static Duration SHIELD_PASSIVATION_TIME = millis(0);
    static double HYPERSPACE_E_ACTIVATION = 1500; // energy
    static double HYPERSPACE_E_RATE = 0; // energy/frame
    static Duration HYPERSPACE_ACTIVATION_TIME = millis(200);
    static Duration HYPERSPACE_PASSIVATION_TIME = millis(200);
    static double DISRUPTOE_E_ACTIVATION = 0; // energy
    static double DISRUPTOR_E_RATE = 5; // energy/frame
    static Duration DISRUPTOR_ACTIVATION_TIME = millis(0);
    static Duration DISRUPTOR_PASSIVATION_TIME = millis(0);

    static double UFO_ENERGY_INITIAL = 3000;
    static double UFO_E_BUILDUP = 1; // energy/frame
    static double UFO_HIT_RADIUS = 15; // energy/frame
    static double UFO_SPEED = 150/FPS; // ufo speed in px/s/fps
    static Duration UFO_GUN_RELOAD_TIME = seconds(2); // ufo fire-fire time period
    static double UFO_BULLET_SPEED = 380/FPS; // bullet speed in px/s/fps
    static double UFO_BULLET_TTL = 0.8*FPS; // bullet time to live == 1s
    static double UFO_BULLET_RANGE = PLAYER_BULLET_SPEED*PLAYER_BULLET_TTL;
    static double UFO_BULLET_OFFSET = 10; // pc
    static double UFO_TTL() { return durToTtl(seconds(randMN(30,80))); }
    static double UFO_SQUAD_TTL() { return durToTtl(seconds(randMN(20,500))); }
    static double SATELLITE_RADIUS = 15; // energy/frame
    static double SATELLITE_SPEED = 200/FPS; // ufo speed in px/s/fps
    static double SATELLITE_TTL() { return durToTtl(seconds(randMN(10,25))); }
    static double SHUTTLE_TTL() { return durToTtl(seconds(randMN(10,11))); }

    static double ARBITRARY_CONSTANT1 = 1.2;

    static Image ASTEROID_GRAPHICS100 = graphics(MaterialDesignIcon.EARTH,100, Color.AQUA, new GaussianBlur(1)); // use blur as anti-scaling-aliasing
    static Image ASTEROID_GRAPHICS50 = graphics(MaterialDesignIcon.EARTH,50, Color.AQUA, new GaussianBlur(1));
    static Image ASTEROID_GRAPHICS20 = graphics(MaterialDesignIcon.EARTH,20, Color.AQUA, new GaussianBlur(1));
    static Image KINETIC_SHIELD_PIECE_GRAPHICS = graphics(MaterialDesignIcon.MINUS,13, Color.AQUA, new DropShadow(GAUSSIAN, Color.DODGERBLUE.deriveColor(1,1,1,0.6), 8,0.3,0,0));
    static double INKOID_SIZE_FACTOR = 50;
    static double ENERG_SIZE_FACTOR = 50;

    @IsConfig(min = 0, max = 1) final DoubleProperty b1 = be.thresholdProperty();
    @IsConfig final V<PlayerSpawners> spawning = new V<>(CIRCLE);
    @IsConfig final V<String> p1name = new V<>("Player 1");
    @IsConfig final V<String> p2name = new V<>("Player 2");
    @IsConfig final V<String> p3name = new V<>("Player 3");
    @IsConfig final V<String> p4name = new V<>("Player 4");
    @IsConfig final V<String> p5name = new V<>("Player 5");
    @IsConfig final V<String> p6name = new V<>("Player 6");
    @IsConfig final V<String> p7name = new V<>("Player 7");
    @IsConfig final V<String> p8name = new V<>("Player 8");
    @IsConfig final V<Color> p1color = new V<>(Color.CORNFLOWERBLUE);
    @IsConfig final V<Color> p2color = new V<>(Color.GREY);
    @IsConfig final V<Color> p3color = new V<>(Color.GREEN);
    @IsConfig final V<Color> p4color = new V<>(Color.RED);
    @IsConfig final V<Color> p5color = new V<>(Color.SANDYBROWN);
    @IsConfig final V<Color> p6color = new V<>(Color.YELLOW);
    @IsConfig final V<Color> p7color = new V<>(Color.CADETBLUE);
    @IsConfig final V<Color> p8color = new V<>(Color.MAGENTA);
    @IsConfig final V<KeyCode> p1fire = new V<>(KeyCode.W);
    @IsConfig final V<KeyCode> p1thrust = new V<>(KeyCode.S);
    @IsConfig final V<KeyCode> p1left = new V<>(KeyCode.A);
    @IsConfig final V<KeyCode> p1right = new V<>(KeyCode.D);
    @IsConfig final V<KeyCode> p1ability = new V<>(KeyCode.Q);
    @IsConfig final V<KeyCode> p2fire = new V<>(KeyCode.UP);
    @IsConfig final V<KeyCode> p2thrust = new V<>(KeyCode.DOWN);
    @IsConfig final V<KeyCode> p2left = new V<>(KeyCode.LEFT);
    @IsConfig final V<KeyCode> p2right = new V<>(KeyCode.RIGHT);
    @IsConfig final V<KeyCode> p2ability = new V<>(KeyCode.CONTROL);
    @IsConfig final V<KeyCode> p3fire = new V<>(KeyCode.T);
    @IsConfig final V<KeyCode> p3thrust = new V<>(KeyCode.G);
    @IsConfig final V<KeyCode> p3left = new V<>(KeyCode.F);
    @IsConfig final V<KeyCode> p3right = new V<>(KeyCode.H);
    @IsConfig final V<KeyCode> p3ability = new V<>(KeyCode.R);
    @IsConfig final V<KeyCode> p4fire = new V<>(KeyCode.I);
    @IsConfig final V<KeyCode> p4thrust = new V<>(KeyCode.K);
    @IsConfig final V<KeyCode> p4left = new V<>(KeyCode.J);
    @IsConfig final V<KeyCode> p4right = new V<>(KeyCode.L);
    @IsConfig final V<KeyCode> p4ability = new V<>(KeyCode.U);
    @IsConfig final V<KeyCode> p5fire = new V<>(KeyCode.W);
    @IsConfig final V<KeyCode> p5thrust = new V<>(KeyCode.S);
    @IsConfig final V<KeyCode> p5left = new V<>(KeyCode.A);
    @IsConfig final V<KeyCode> p5right = new V<>(KeyCode.D);
    @IsConfig final V<KeyCode> p5ability = new V<>(KeyCode.Q);
    @IsConfig final V<KeyCode> p6fire = new V<>(KeyCode.W);
    @IsConfig final V<KeyCode> p6thrust = new V<>(KeyCode.S);
    @IsConfig final V<KeyCode> p6left = new V<>(KeyCode.A);
    @IsConfig final V<KeyCode> p6right = new V<>(KeyCode.D);
    @IsConfig final V<KeyCode> p6ability = new V<>(KeyCode.Q);
    @IsConfig final V<KeyCode> p7fire = new V<>(KeyCode.W);
    @IsConfig final V<KeyCode> p7thrust = new V<>(KeyCode.S);
    @IsConfig final V<KeyCode> p7left = new V<>(KeyCode.A);
    @IsConfig final V<KeyCode> p7right = new V<>(KeyCode.D);
    @IsConfig final V<KeyCode> p7ability = new V<>(KeyCode.Q);
    @IsConfig final V<KeyCode> p8fire = new V<>(KeyCode.W);
    @IsConfig final V<KeyCode> p8thrust = new V<>(KeyCode.S);
    @IsConfig final V<KeyCode> p8left = new V<>(KeyCode.A);
    @IsConfig final V<KeyCode> p8right = new V<>(KeyCode.D);
    @IsConfig final V<KeyCode> p8ability = new V<>(KeyCode.Q);
    final List<Player> PLAYERS = listRO(
        new Player(1, p1name, p1color, p1fire, p1thrust, p1left, p1right, p1ability),
        new Player(2, p2name, p2color, p2fire, p2thrust, p2left, p2right, p2ability),
        new Player(3, p3name, p3color, p3fire, p3thrust, p3left, p3right, p3ability),
        new Player(4, p4name, p4color, p4fire, p4thrust, p4left, p4right, p4ability),
        new Player(5, p5name, p5color, p5fire, p5thrust, p5left, p5right, p5ability),
        new Player(6, p6name, p6color, p6fire, p6thrust, p6left, p6right, p6ability),
        new Player(7, p7name, p7color, p7fire, p7thrust, p7left, p7right, p7ability),
        new Player(8, p8name, p8color, p8fire, p8thrust, p8left, p8right, p8ability)
    );

    /** Encompasses entire game. */
    private class Game {
        final V<Boolean> paused = new V<>(false);
        final V<Boolean> running = new V<>(false);
        final V<Boolean> deadly_bullets = new V<>(false);

        final ObservableSet<Player> players  = FXCollections.observableSet();
        final ObjectStore<PO> oss = new ObjectStore<>(o -> o.type);
        final Collection<PO> os = new ArrayDeque<>();
        final Set<ForceField> forceFields = new HashSet<>();
        final EnumSet<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
        final Map<KeyCode,Long> keyPressTimes = new HashMap<>();

        final Loop loop = new Loop(this::doLoop);
        long loopid = 0;   // game loop id, starts at 0, incremented by 1
        int mission_counter = 0;   // mission counter, starts at 1, increments by 1
        Mission mission = null; // current mission, (they repeat), starts at 1, = mission % missions +1
        final MapSet<Integer,Mission> missions = new MapSet<>(m -> m.id,
            new Mission(1,null,Color.DODGERBLUE, Color.rgb(0, 0, 15, 0.08),null, Inkoid::new),
            new Mission(2,null, Color.DODGERBLUE,Color.rgb(10,10,25,0.08),null,Energ::new),
            new Mission(3,null,Color.GREEN, Color.rgb(0, 15, 0, 0.08), null, Fermi::new),
            new Mission(4,null,Color.DODGERBLUE, Color.rgb(0, 0, 15, 0.08), null, Genoid::new),
//            new Mission(4,bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new),
            new Mission(5,null,Color.RED,new Color(1,1,1,0.08),new ColorAdjust(0,-0.6,-0.7,0),Energ2::new)
        );
        final UfoFaction ufos = new UfoFaction();
        final PlayerFaction humans = new PlayerFaction();
        final TTLList nextFrameJobs = new TTLList();
        final Set<PO> removables = new HashSet<>();
        final PoolMap<Node> graphicsPool = new PoolMap<Node>(){{
            registerPool(Bullet.class, () -> new Pool<>(60,() -> new Icon(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE,5)));
            registerPool(PulseEngineDebris.class, () -> new Pool<>(200,() -> new Icon(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE,3)));
            registerPool(RocketEngineDebris.class, () -> new Pool<>(200,() -> new Icon(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE,3)));
        }};

        final Ƒ0<RocketEnhancer> SHUTTLE_CALL_ENHANCER = ShuttleCall::new; // we need specific instance
        final Set<Ƒ0<? extends RocketEnhancer>> ROCKET_ENHANCERS = set(
            RapidFire::new,PowerFire::new,Mobility::new,Intel::new,
            LaserSight::new, EnergyMaximizer::new,SHUTTLE_CALL_ENHANCER,
            KineticShieldEnergizer::new, EnergizerSmall::new,BaterryMedium::new,BaterryLarge::new
        );

        public Game() {}

        void pause(boolean v) {
            if(!running.get() || paused.get()==v) return;
            paused.set(v);
            if(v) loop.stop();
            else loop.start();
        }

        void start(int player_count) {
            stop();

            players.addAll(listF(player_count,PLAYERS::get));
            players.forEach(p -> {
                p.alive = false;
                p.score.setValue(0);
                p.lives.setValue(PLAYER_LIVES_INITIAL);
                p.spawn();
            });

            running.set(true);
            loopid = 0;
            mission_counter = 0;
            ufos.init();
            humans.init();
            loop.start();

            timer200ms.start();
            playfield.requestFocus();
            nextMission();
        }

        void doLoop() {
            loopid++;
            long now = System.currentTimeMillis();
            boolean isHalf = loopid%2==0;

            players.stream().filter(p -> p.alive).forEach(p -> {
                if(pressedKeys.contains(p.keyLeft.get()))  p.rocket.dir -= p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyLeft.get(),0l));
                if(pressedKeys.contains(p.keyRight.get())) p.rocket.dir += p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyRight.get(),0l));
                if(isHalf && p.rocket.rapidfire.is() && pressedKeys.contains(p.keyFire.get()))  p.rocket.gun.fire();
            });

            nextFrameJobs.run();

            // remove inactive objects
            for(PO o : os) if(o.canDispose) removables.add(o);
            os.removeIf(o -> o.canDispose);
            for(PO o : oss.get(Particle.class)) if(o.canDispose) removables.add(o);
            oss.forEachSet(set -> set.removeIf(o -> o.canDispose));
            removables.forEach(PO::dispose);
            removables.clear();

            // apply forces
            forEachCartesian(forceFields, os, (ff,o) -> {
                if(ff.isInHyperspace()==o.isin_hyperspace) {
                    double ax = ff.getX() - o.x;
                    double ay = ff.getY() - o.y;
                    double dist = 1 + sqrt(ax*ax+ay*ay); // apprxmt of max(1,sqrt(ax*ax+ay*ay));
                    double f = ff.force(o.mass,dist);
                    o.dx += ax*f/dist;
                    o.dy += ay*f/dist;
                }
            });

            // canvas clearing
            // we use optional "fade" effect. Filling canvas with transparent color repeatedly
            // will serve instead of clearing & spare drawing complex stuff & produce bgr & interesting effect
            if(mission.color_canvasFade==null) {
                gc_bgr.clearRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
            } else {
                gc_bgr.setFill(mission.color_canvasFade);
                gc_bgr.fillRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
            }
            gc.clearRect(0,0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());

            // update & redraw active objects
            forceFields.stream().filter(ff -> ff instanceof LO).forEach(ff -> ((LO)ff).doLoop());
            os.forEach(PO::doLoop);

            // guns & firing
            stream(oss.get(Rocket.class).stream(),oss.get(Ufo.class).stream()).forEach(ship -> {
                if(ship.gun!=null) {
                    ship.gun.fireTTL--;
                }
                if(ship.gun!=null && ship.gun.control==AUTO && ship.gun.fireTTL<0) {
                    ship.gun.fireTTL = durToTtl(ship.gun.time_reload);
                    nextFrameJobs.add(() -> ship.gun.ammo_type.apply(ship.gun.aimer.apply()));
                }
            });

            // collisions
            forEachCartesian(oss.get(Bullet.class),filter(os,e -> !(e instanceof Bullet)), (b,e) -> {
                if(b.dead==false && !(e instanceof Ship && ((Ship)e).isin_hyperspace) && b.owner!=e) { // avoid self-hits
                    if(!(e instanceof Satellite) && e.isHitDistance(b)) {
                        b.dead = true;
                        b.canDispose = true; // bullet always dies
                        if(e instanceof Rocket) {
                            Rocket r = (Rocket)e;
                            if(!game.deadly_bullets.get() && b.owner instanceof Rocket) {
                                r.kinetic_shield.new KineticShieldPiece(r.dir(b));
                            }
                            if(game.deadly_bullets.get() || !(b.owner instanceof Rocket)) {
                                if(r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                                    r.dx = r.dy = 0;
                                    r.engine.off();
                                } else {
                                    r.player.die();
                                }
                            }
                        } else
                        if(e instanceof Asteroid) {
                            ((Asteroid)e).onHit(b);
                            onPlanetoidDestroyed();

                            new FermiGraphics(e.x, e.y, e.radius*2.5);
//                            gc_bgr.setGlobalAlpha(0.2);
//                            gc_bgr.setFill(mission.color);
//                            drawOval(gc_bgr,b.x,b.y,100);
//                            gc_bgr.setGlobalAlpha(1);


                        } else
                        if(e instanceof Ufo) {
                            Ufo u = (Ufo)e;
                            if(!(b.owner instanceof Ufo)) {
                                u.canDispose = true;
                                ufos.onUfoDestroyed(u);
                                new FermiGraphics(e.x, e.y, 100).color = ufos.color;
                            }
                        } else
                        if(e instanceof Shuttle) { // we are assuming its kinetic shield is always active (by game design)
                            // ignore bullets when allies | shooting from inside the shield
                            if(b.owner instanceof Rocket || b.owner.distance(e)<((Ship)e).kinetic_shield.KSradius) {
                                b.canDispose = false;
                                b.dead = false;
                            } else {
                                ((Ship)e).kinetic_shield.new KineticShieldPiece(e.dir(b));
                            }
                        }

                        // score
                        if(b.owner instanceof Rocket) {
                            double bonus;
                            if(e instanceof Asteroid) bonus = SCORE_ASTEROID((Asteroid)e);
                            else if(e instanceof Ufo) bonus = SCORE_UFO;
                            else bonus = 0;
                            ((Rocket)b.owner).player.score.setValueOf(s -> s + (int)bonus);
                        }
                    }
                }
            });

            oss.forEach(Rocket.class,Rocket.class, (r1,r2) -> {
                if(!r1.isin_hyperspace && !r2.isin_hyperspace && r1.isHitDistance(r2)) {
                    if(r1.ability_main instanceof Shield && r1.ability_main.isActivated()) {
                        r1.dx = r1.dy = 0;
                        r1.engine.off();
                        r2.engine.off();
                    } else {
                        r1.player.die();
                    }
                    if(r2.ability_main instanceof Shield && r2.ability_main.isActivated()) {
                        r2.dx = r2.dy = 0;
                    } else {
                        r2.player.die();
                    }
                }
            });
            oss.forEach(Rocket.class,Satellite.class, (r,s) -> {
                if(!r.isin_hyperspace && r.isHitDistance(s)) {
                    s.pickUpBy(r);
                }
            });
            oss.forEach(Rocket.class,Ufo.class, (r,u) -> {
                if(!r.isin_hyperspace && r.isHitDistance(u)) {
                    if(r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                        r.dx = r.dy = 0;
                    } else {
                        r.player.die();
                    }
                    u.canDispose = true;
                    ufos.onUfoDestroyed(u);
                }
            });
            oss.forEach(Rocket.class,Asteroid.class, (r,a) -> {
                if(!r.isin_hyperspace && r.isHitDistance(a)) {
                    if(r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                        ((Shield)r.ability_main).onHit(a);
                    } else {
                        if(r.kineticEto(a)<r.kinetic_shield.KSenergy) {
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
                if(s.isHitDistance(a)) {
                    if(s.kineticEto(a)<s.kinetic_shield.KSenergy) {
                        s.kinetic_shield.onShieldHit(a);
                        a.onHit(s);
                        onPlanetoidDestroyed();
                    }
                }
            });

            // noninteracting stuff last
            oss.get(Particle.class).forEach(Particle::doLoop);
            oss.get(Particle.class).stream().filter(Draw2.class::isInstance).map(Draw2.class::cast).forEach(Draw2::drawBack);
            oss.get(Particle.class).stream().filter(Draw2.class::isInstance).map(Draw2.class::cast).forEach(Draw2::drawFront);
        }

        void stop() {
            running.set(false);
            timer200ms.stop();
            loop.stop();
            players.clear();
            os.clear();
            oss.clear();
            forceFields.clear();
            nextFrameJobs.clear();
            playfield.getChildren().clear();
        }

        void nextMission() {
            mission_counter = mission_counter==0 ? 1 : mission_counter+1;
            int id = mission_counter%missions.size();
            int mission_id = id==0 ? missions.size() : mission_counter%missions.size(); // modulo mission count, but start at 1
            mission = missions.get(mission_id);
            mission.start();
        }
        void onPlanetoidDestroyed() {
            // it may take a cycle or two for asteroids to get disposed, hence the delay
            nextFrameJobs.add(10, () -> {
                if(oss.get(Asteroid.class).isEmpty()) nextMission();
            });
        }
        void over() {
            nextFrameJobs.add(seconds(5),this::stop);
        }
        void message(String s) {
            message.setText(s);
            message.setFont(new Font(message.getFont().getName(), 50));
            Anim a = new Anim(millis(500),x -> message.setOpacity(x*x));
            a.playOpen();
            nextFrameJobs.add(seconds(2),a::playClose);
        }

        /** Clears resources. No game session will occur after this. */
        void dispose() {
            stop();
            graphicsPool.clear();
        }

        class PlayerFaction {
            final InEffect intelOn = new InEffect();
            final Color color = Color.DODGERBLUE;

            void init() {
                intelOn.reset();
                nextFrameJobs.addPeriodic(() -> SATELLITE_TTL()/sqrt(players.size()), humans::sendSatellite);
            }

            void sendShuttle(Rocket r) {
                game.nextFrameJobs.add(seconds(2),() -> pulseCall(r));
                game.nextFrameJobs.add(seconds(4),() -> pulseCall(r));
                game.nextFrameJobs.add(seconds(6),() -> new Shuttle(r));
            }
            void sendSatellite() {
                sendSatellite(randEnum(Side.class));
            }
            private void sendSatellite(Side side) {
                Side s = side==null ? randEnum(Side.class) : side;
                double offset = 50;
                double x = s==LEFT ? offset : playfield.getWidth()-offset;
                double y = rand0N(playfield.getHeight());
                if(humans.intelOn.is()) pulseAlert(x,y);
                nextFrameJobs.add(seconds(1.8), () -> new Satellite(s).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,8,color,false);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-8,color,false);
            }
        }
        class UfoFaction {
            int losses = 0;
            int losses_aggressive = 5;
            int losses_cannon = 10;
            Rocket ufo_enemy = null;
            boolean aggressive = false;
            final Color color = Color.rgb(114,208,74);

            void init() {
                losses = 0;
                ufo_enemy = null;
                aggressive = false;
                nextFrameJobs.addPeriodic(() -> UFO_TTL()/sqrt(players.size()), ufos::sendUfo);
                nextFrameJobs.addPeriodic(() -> UFO_SQUAD_TTL()/sqrt(players.size()), ufos::sendUfoSquadron);
            }

            void onUfoDestroyed(Ufo u) {
                losses++;
                if(losses>losses_aggressive) {
                    aggressive = losses%2==0;
                }
                if(losses%losses_cannon==losses_cannon-1) {
                    activateSlipSpaceCannon();
                }
            }
            void activateSlipSpaceCannon() {
                message("U.F.O. SLIP SPACE CANNON ALERT");
            }
            void sendUfo() {
                sendUfo(randEnum(Side.class));
            }
            void sendUfoSquadron() {
                ufo_enemy = players.isEmpty() ? null : randOf(players).rocket;
                Side side = randEnum(Side.class);
                int count = (int)(2+random()*8);
                repeat(count, () -> nextFrameJobs.add(seconds(rand0N(0.5)),() -> sendUfo(side)));
            }
            private void sendUfo(Side side) {
                Side s = side==null ? randEnum(Side.class) : side;
                double offset = 50;
                double x = s==LEFT ? offset : playfield.getWidth()-offset;
                double y = rand0N(playfield.getHeight());
                if(humans.intelOn.is()) pulseAlert(x,y);
                nextFrameJobs.add(seconds(1.2), () -> new Ufo(s,aggressive).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,8,color,true);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-8,color,true);
            }
        }
        class Mission {
            final int id;
            final Background bgr;
            final Ƒ5<Double,Double,Double,Double,Double,Asteroid> planetoidConstructor;
            final Color color;
            final Color color_canvasFade; // normally null, canvas fade effect
            final Effect toplayereffect;

            public Mission(int ID, Background BGR, Color COLOR, Color CANVAS_REDRAW, Effect effect, Ƒ5<Double,Double,Double,Double,Double,Asteroid> planetoidFactory) {
                id = ID;
                bgr = BGR;
                color = COLOR;
                planetoidConstructor = planetoidFactory;
                color_canvasFade = CANVAS_REDRAW;
                toplayereffect = effect;
            }

            void start() {
                ((Pane)playfield.getParent()).setBackground(bgr);
                playfield.setEffect(toplayereffect);

                int size = (int) sqrt(playfield.getWidth()*playfield.getHeight())/1000;
                int planetoids = 4 + 3*(size-1) + 2*(mission_counter-1) + players.size()/4;
                double delay = durToTtl(seconds(mission_counter==1 ? 2 : 5));
                nextFrameJobs.add(delay/2, () -> message("Level " + mission_counter));
                nextFrameJobs.add(delay, () -> repeat(planetoids, i -> spawnPlanetoid()));
            }

            void spawnPlanetoid() {
                boolean vertical = randBoolean();
                planetoidConstructor.apply(
                    vertical ? 0 : random()*playfield.getWidth(),
                    vertical ? random()*playfield.getHeight() : 0,
                    randMN(-1,1)*0.7,rand0N(2*PI), 1d
                );
            }
        }
    }

    /** Game player. Survives game sessions. */
    private class Player {
        final int id;
        V<String> name;
        V<Color> color;
        V<KeyCode> keyFire;
        V<KeyCode> keyThrust;
        V<KeyCode> keyLeft;
        V<KeyCode> keyRight;
        V<KeyCode> keyAbility;
        boolean alive = false;
        final V<Integer> lives = new V<>(PLAYER_LIVES_INITIAL);
        final V<Integer> score = new V<>(0);
        final V<Double> energy = new V<>(0d);
        final V<AbilityKind> ability_type = new V<>(PLAYER_ABILITY_INITIAL);
        Rocket rocket;

        public Player(int ID, V<String> NAME, V<Color> COLOR, V<KeyCode> kfire, V<KeyCode> kthrust, V<KeyCode> kleft, V<KeyCode>kright, V<KeyCode> kability) {
            id = ID;
            name = NAME;
            color = COLOR;
            keyFire = kfire;
            keyThrust = kthrust;
            keyLeft = kleft;
            keyRight = kright;
            keyAbility = kability;
            ability_type.onChange(v -> {
                if(rocket!=null) rocket.changeAbility(v);
            });
            score.onChange((os,ns) -> {
                if(os/PLAYER_SCORE_NEW_LIFE<ns/PLAYER_SCORE_NEW_LIFE) lives.setValueOf(l -> l+1);
            });
//            every200ms.add(() -> { if(rocket!=null) energy.set(rocket.energy); });
            every200ms.add(() -> { if(rocket!=null) energy.set(rocket.kinetic_shield.KSenergy); });
        }

        void die() {
            if(!alive) return; // bugfix
            alive = false;
            rocket.canDispose = true;
            if(lives.getValue()>0) {
                run(PLAYER_RESPAWN_TIME.toMillis(),this::spawn);
            } else {
                if(game.players.stream().filter(p -> p.alive).count()==0)
                    game.over();
            }
        }
        void spawn() {
            alive = true;
            lives.setValueOf(lives -> lives-1);
            rocket = new Rocket(this);
            rocket.x = spawning.get().computeStartingX(playfield.getWidth(),playfield.getHeight(),game.players.size(),id);
            rocket.y = spawning.get().computeStartingY(playfield.getWidth(),playfield.getHeight(),game.players.size(),id);
            rocket.dx = 0;
            rocket.dy = 0;
            rocket.dir = spawning.get().computeStartingAngle(game.players.size(),id);
            rocket.energy = PLAYER_ENERGY_INITIAL;
            rocket.engine.enabled = false; // cant use engine.off() as it could produce unwanted behaviot
            createHyperSpaceAnim(rocket.graphics).playClose();
        }

        double computeRotSpeed(long pressedMsAgo) {
            // Shooting at long distance becomes hard due to 'smallest rotation angle' being too big
            // we slow down rotation in the first ROT_LIMIT ms after key press and reduce rotation
            // limit without decreasing maneuverability. The rotation decrease is nonlinear and
            // continuous
            double r = pressedMsAgo<ROT_LIMIT ? ROTATION_SPEED/((ROT_LIMIT/ROT_DEL+1)-pressedMsAgo/ROT_DEL) : ROTATION_SPEED;
            return rocket.engine.mobility_multiplier*r;
        }


    }
    static enum PlayerSpawners {
        CIRCLE, LINE, RECTANGLE;

        double computeStartingAngle(int ps, int p) {
            switch(this) {
                case CIRCLE : return ps==0 ? 0 : p*2*PI/ps;
                case LINE :
                case RECTANGLE : return -PI/2;
            }
            throw new AssertionError("Illegal switch case");
        }
        double computeStartingX(double w, double h, int ps, int p) {
            switch(this) {
                case CIRCLE : return w/2 + 50*cos(computeStartingAngle(ps, p));
                case LINE : return w/(ps+1)*p;
                case RECTANGLE : {
                    int a = ((int)ceil(sqrt(ps)));
                    return w/(a+1)*(1+(p-1)/a);
                }
            }
            throw new AssertionError("Illegal switch case");
        }
        double computeStartingY(double w, double h, int ps, int p) {
            switch(this) {
                case CIRCLE : return h/2 + 50*sin(computeStartingAngle(ps, p));
                case LINE : return h/2;
                case RECTANGLE : {
                    int a = ((int)ceil(sqrt(ps)));
                    return h/(a+1)*(1+(p-1)%a);
                }
            }
            throw new AssertionError("Illegal switch case");
        }
    }

    /** Loop object - object with per loop behavior. Executes once per loop. */
    static interface LO {
        void doLoop();
        default void dispose() {};
    }
    abstract class SO implements LO {
        double x = 0;
        double y = 0;
        double dx = 0;
        double dy = 0;

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
            if(x<0) x = playfield.getWidth();
            else if(x>playfield.getWidth()) x = 0;
            if(y<0) y = playfield.getHeight();
            else if(y>playfield.getHeight()) y = 0;
        }
    }
    /** Object with physical properties. */
    abstract class PO extends SO {
        double mass = 0;
        Engine engine = null;
        Class type;
        Node graphics;
        double radius;
        boolean isin_hyperspace = false;
        Set<LO> children = null;
        boolean canDispose = false;

        PO(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Node GRAPHICS) {
            type = TYPE;
            x = X; y = Y; dx = DX; dy = DY;
            radius = HIT_RADIUS;
            mass = 2*HIT_RADIUS*HIT_RADIUS; // 4/3d*PI*HIT_RADIUS*HIT_RADIUS*HIT_RADIUS;
            graphics = GRAPHICS;
            if(graphics!=null) {
                graphics.setManaged(false);
                graphics.setCache(true);
                graphics.setCacheHint(CacheHint.SPEED);
            }
            init();
        }

        public void doLoop(){
            doLoopBegin();
            super.doLoop();
            doLoopEnd();
            if(children!=null) children.forEach(LO::doLoop);
        }
        void doLoopBegin(){};
        void doLoopEnd(){};
        void move() {
            dx *= RESISTANCE;
            dy *= RESISTANCE;
            if(abs(dx)<1/FPS) dx = 0;
            if(abs(dy)<1/FPS) dy = 0;
        }

        void draw() {
            if(graphics!=null) {
                relocateCenter(graphics,x,y);
            }
        }

        void init() {
            if(graphics!=null) playfield.getChildren().add(graphics);
            game.os.add(this);
            game.oss.add(this);
        }

        public void dispose() {
            if(graphics!=null) playfield.getChildren().remove(graphics);
            if(children!=null) list(children).forEach(LO::dispose);
        }

        boolean isDistanceLess(PO o, double dist) {
            return dist*dist > (o.x-x)*(o.x-x) + (o.y-y)*(o.y-y);
        }

        boolean isHitDistance(PO o) {
            return isDistanceLess(o,radius+o.radius);
        }

        double distance(PO o) {
            return sqrt((o.x-x)*(o.x-x) + (o.y-y)*(o.y-y));
        }

        /** Returns direction angle between objects. In radians. */
        double dir(PO to) {
            double tx = x-to.x;
            return (tx<0 ? 0 : PI) + atan((y-to.y)/tx);
        }

        double kineticE() {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
            return mass;// * (dx*dy);
        }

        double speed() {
            return sqrt(dx*dx+dy*dy);
        }

        double speedTo(PO o) {
            double sx = dx-o.dx;
            double sy = dy-o.dy;
            return sqrt(sx*sx+sy*sy);
        }

        double kineticEto(PO o) {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
            return mass;// * (dx*dy);
        }

    }
    /** Object with engine, gun and other space ship characteristics. */
    abstract class Ship extends PO {
        double dir = -PI/2; // up
        double energy = 0;
        double energy_buildup_rate;
        double energy_max = 10000;
        Gun gun = null;
        Ability ability_main;
        KineticShield kinetic_shield = null;

        public Ship(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Node GRAPHICS, double E, double dE) {
            super(TYPE, X, Y, DX, DY, HIT_RADIUS, GRAPHICS);
            energy = E;
            energy_buildup_rate = dE;
            children = new HashSet<>(10);
        }
        void doLoopBegin() {
            energy = min(energy+energy_buildup_rate,energy_max);
            if(engine!=null) engine.doLoop();
        }
        class Engine {
            boolean enabled = false;
            InEffect mobility = new InEffect();
            double mobility_multiplier = 1;

            final void on() { enabled = true; onOn();};
            final void off() { enabled = false; onOff();}
            void onOn(){};
            void onOff(){};
            final void doLoop(){
                if(enabled) {
                    onDoLoop();
                }
            };
            void onDoLoop(){};
            final void mobilityInc(){
                mobility.inc();
                mobility_multiplier = pow(BONUS_MOBILITY_MULTIPLIER,mobility.isTimes());
            }
            final void mobilityDec(){
                if(mobility.is()) {
                    mobility.dec();
                    mobility_multiplier = pow(BONUS_MOBILITY_MULTIPLIER,mobility.isTimes());
                }
            }
        }
        class RocketEngine extends Engine {
            double ttl = 0;
            double thrust = ROCKET_ENGINE_THRUST;
            final double particle_speed = 1/1/FPS;
            final double particle_dispersion_angle = PI/4;

            void onDoLoop() {
                dx += cos(dir)*mobility_multiplier*thrust;
                dy += sin(dir)*mobility_multiplier*thrust;

                if(!isin_hyperspace) {
                    ttl--;
                    if(ttl<0) {
                        ttl = ROCKET_ENGINE_DEBRIS_TTL;
                        double d = dir+PI;
                        double d1 = d + (random())*particle_dispersion_angle;
                        double d4 = d + .5*(random())*particle_dispersion_angle;
                        double d2 = d - (random())*particle_dispersion_angle;
                        double d3 = d - .5*(random())*particle_dispersion_angle;
                        game.nextFrameJobs.add(() -> {
                            new RocketEngineDebris(x+20*cos(d), y+20*sin(d), 1*cos(d1),1*sin(d1));
                            new RocketEngineDebris(x+20*cos(d), y+20*sin(d), 1*cos(d2),1*sin(d2));
                            new RocketEngineDebris(x+20*cos(d), y+20*sin(d), 1*cos(d3),1*sin(d3));
                            new RocketEngineDebris(x+20*cos(d), y+20*sin(d), 1*cos(d4),1*sin(d4));
                        });
                    }
                }
            }
        }
        class PulseEngine extends Engine {
            private double pulseTTL = 0;

            void onOn() { pulseTTL = 0; }
            void onOff() {}
            void onDoLoop() {
                pulseTTL--;
                if(pulseTTL<0) {
                    pulseTTL = PULSE_ENGINE_PULSEPERIOD_TTL;
                    pulse();
                }
            }
            void pulse() {
                final double x = Ship.this.x + 9*cos(Ship.this.dir+PI);
                final double y = Ship.this.y + 9*sin(Ship.this.dir+PI);
                final boolean hyper = isin_hyperspace;
                final double mobility_multiplier_effect = mobility_multiplier;
                ForceField ff = new ForceField() {
                    private double ttl = 1;
                    private boolean debris_done = false; // prevents spawning debris multiple times

                    public void doLoop() {
                        ttl -= PULSE_ENGINE_PULSE_TTL1;
                        if(!debris_done && ttl<0.7) {
                            debris_done = true;
                            if(!isInHyperspace()) {
                                game.nextFrameJobs.add(() -> {
                                    new PulseEngineDebris(x-2,y+2,-.5d, .5d);
                                    new PulseEngineDebris(x+2,y+2, .5d, .5d);
                                    new PulseEngineDebris(x+2,y-2, .5d,-.5d);
                                    new PulseEngineDebris(x-2,y-2,-.5d,-.5d);
                                });
                            }
                        }
                        if(ttl<0) game.nextFrameJobs.add(() -> game.forceFields.remove(this));
                    }
                    public boolean isInHyperspace() { return hyper; }
                    public double getX() { return x; }
                    public double getY() { return y; }
                    public double force(double mass, double dist) {
                        return dist==0 ? 0 : -255*mobility_multiplier_effect*ttl*ttl/(dist*dist*dist);
                    }
                };
                game.nextFrameJobs.add(() -> game.forceFields.add(ff));
            }
        }
        class Gun {
            final GunControl control;
            final Ƒ0<Double> aimer; // determines bullet direction
            final Ƒ1<Double,Bullet> ammo_type; // bullet factory
            final Duration time_reload;
            double fireTTL; // frames till next fire

            public Gun(GunControl CONTROL, Duration TIME_RELOAD, Ƒ0<Double> AIMER, Ƒ1<Double,Bullet> AMMO_TYPE) {
                control = CONTROL;
                time_reload = TIME_RELOAD;
                aimer = AIMER;
                ammo_type = AMMO_TYPE;
                fireTTL = durToTtl(time_reload);
            }

            void fire() {
                if(!isin_hyperspace)
                    game.nextFrameJobs.add(() -> ammo_type.apply(aimer.apply()));
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

            void init(){};
            public void dispose(){
                passivate(); // forcefully deactivate
                onPassivateStart(); // forcefully deactivate
                onPassivateEnd(); // forcefully deactivate
                if(ability_main==this) ability_main = null;
                children.remove(this);
            };

            void onKeyPress(){
                if(onHold) {
                    activate();
                } else {
                    if(state==ACTIVATING || activation==1) passivate(); else activate();
                }
            }
            void onKeyRelease(){
                if(onHold) {
                    passivate();
                } else {}
            }
            void activate() {
                if(state==NO_CHANGE) {
                    double min_energy_required = e_act+5*e_rate;
                    if(energy >= min_energy_required) {
                        energy -= min(energy,e_act);
                        state=ACTIVATING;
                        onActivateStart();
                    }
                }
            }
            void passivate() {
                if(state==NO_CHANGE) {
                    state=PASSSIVATING;
                    onPassivateStart();
                }
            }
            void onActivateStart(){};
            void onActivateEnd(){};
            void onActive(){};
            void onPassivateStart(){};
            void onPassivateEnd(){};
            void onPassive(){};
            void onActiveChanged(double activation){};
            boolean isActivated() { return activation==1; }
            public void doLoop() {
                if(state==ACTIVATING) {
                    activation = timeActivation==0 ? 1 : min(1,activation+1/(timeActivation*FPS));
                    if(activation==1) {
                        state = NO_CHANGE;
                        onActivateEnd();
                    }
                    onActiveChanged(activation);
                } else
                if(state==PASSSIVATING) {
                    activation = timePassivation==0 ? 0 : max(0,activation-1/(timePassivation*FPS));
                    if(activation==0) {
                        state = NO_CHANGE;
                        onPassivateEnd();
                    }
                    onActiveChanged(activation);
                } else if(activation==0) onPassive();
                else if (activation==1) onActive();

                if(activation==1) energy -= min(energy,e_rate);
                if(energy==0) passivate();

            }
        }
        class AbilityWithSceneGraphics extends Ability {
            final Node graphicsA;
            final boolean poolGraphics;

            public AbilityWithSceneGraphics(boolean ONHOLD, Duration timeToAct, Duration timeToPass, double E_ACT, double E_RATE) {
                super(ONHOLD, timeToAct, timeToPass, E_ACT, E_RATE);
                graphicsA = game.graphicsPool.get(getClass());
                poolGraphics = true;
            }
            public AbilityWithSceneGraphics(boolean ONHOLD, Duration timeToAct, Duration timeToPass, double E_ACT, double E_RATE, Node GRAPHICS) {
                super(ONHOLD, timeToAct, timeToPass, E_ACT, E_RATE);
                graphicsA = GRAPHICS;
                poolGraphics = false;
            }

            public void dispose() {
                super.dispose();
                playfield.getChildren().remove(graphicsA);
                if(poolGraphics) game.graphicsPool.add(getClass(),graphicsA);
            }
        }
        class Disruptor extends Ability {
            final ForceField field = new ForceField() {
                public boolean isInHyperspace() { return isInHyperspace(); }
                public double getX() { return x; }
                public double getY() { return y; }
                public double force(double mass, double dist) {
                    return dist==0 ? 0 : -75/dist/dist;
                }
                public void doLoop() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };

            Disruptor() {
                super(true, DISRUPTOR_ACTIVATION_TIME,DISRUPTOR_PASSIVATION_TIME,DISRUPTOE_E_ACTIVATION,DISRUPTOR_E_RATE );
            }

            public void doLoop() { super.doLoop(); } // technical matter (inheriting this method twice)
            public boolean isInHyperspace() {
                return isin_hyperspace;
            }
            void onActivateStart() {
                game.forceFields.add(field);
            }
            void onPassivateStart() {
                game.forceFields.remove(field);
            }
        }
        class Hyperspace extends AbilityWithSceneGraphics {
            Hyperspace() {
                super(
                    false, HYPERSPACE_ACTIVATION_TIME,HYPERSPACE_PASSIVATION_TIME,HYPERSPACE_E_ACTIVATION,HYPERSPACE_E_RATE,
                    new Icon(MaterialDesignIcon.PLUS,30)
                );
            }

            void onActivateStart() {
                isin_hyperspace = true;
                playfield.getChildren().add(graphicsA);
            }
            void onPassivateEnd() {
                isin_hyperspace = false;
                playfield.getChildren().remove(graphicsA);
            }
            void onActiveChanged(double activation) {
                 setScaleXY(graphics,1-activation);
                 setScaleXY(graphicsA,activation);
            }
            public void doLoop() {
                super.doLoop();
                relocateCenter(graphicsA, x, y);
            }

        }
        class Shield extends AbilityWithSceneGraphics {
            final Anim animation;

            Shield() {
                super(
                    true, SHIELD_ACTIVATION_TIME,SHIELD_PASSIVATION_TIME,SHIELD_E_ACTIVATION,SHIELD_E_RATE,
                    new Icon(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE,50)
                );
                playfield.getChildren().add(graphicsA);
                animation = new Anim(millis(100),x -> graphicsA.setOpacity(x*x*x));
                animation.affector.accept(activation); // initialize graphics
            }

            void onActivateStart() {
                animation.playOpen();
            }
            void onPassivateStart() {
                animation.playClose();
            }
            public void doLoop() {
                super.doLoop();
                relocateCenter(graphicsA, x,y);
            }
            boolean isActivated() {
                return activation!=0;
            }
            void onHit(Asteroid a) {
                // makes shield hardly usable, instead we drain energy constantly while active
                // energy -= min(energy,kineticEto(a));
            }

        }
        class KineticShield extends Ability {
            double KSenergy_max;
            double KSenergy;
            double KSenergy_rate;
            double KSradius;
            int pieces;
            double piece_angle;
            final Runnable activationRun = this::showActivation;
            final LO ksemitter = new ShieldPulseEmitter();

            public KineticShield(double RADIUS, double ENERGY_MAX) {
                super(true, Duration.ZERO,Duration.ZERO,0,0);
                KSenergy_max = ENERGY_MAX;
                KSenergy_rate = KSenergy_max/durToTtl(KINETIC_SHIELD_RECHARGE_TIME);
                KSenergy = KINETIC_SHIELD_INITIAL_ENERGY*KSenergy_max;
                KSradius = RADIUS;
                pieces = ((int)(2*PI*KSradius))/11;
                piece_angle = 2*PI/pieces;
                children.add(this);
                scheduleActivation();
            }

            void init() {}    // no init
            public void dispose() {} // no dispose
            public void doLoop() {
                KSenergy = min(KSenergy_max,KSenergy+KSenergy_rate);
                ksemitter.doLoop();
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
            private void scheduleActivation() {
                if(KSenergy<KSenergy_max) {
                    game.nextFrameJobs.remove(activationRun);
                    game.nextFrameJobs.add((KSenergy_max-KSenergy)/KSenergy_rate, activationRun);
                }
            }
            void changeKSenergyBy(double e){
                KSenergy = clip(0,KSenergy+e,KSenergy_max);
            }
            void changeKSenergyToMax(){
                if(KSenergy<KSenergy_max) {
                    KSenergy = KSenergy_max;
                    showActivation();
                }
            }


            /** Not an ability, simply a graphics for an ability. Extends Ability to avoid code duplicity. */
            class KineticShieldPiece extends Ability {
                final double dirOffset;
                double ttl2;
                double ttl = 1;
                double ttl_frac = 1/durToTtl(seconds(1));


                KineticShieldPiece(double DIR) {
                    this(false,DIR);
                }
                KineticShieldPiece(boolean delayed, double DIR) {
                    super(true, Duration.ZERO,Duration.ZERO,0,0);
                    ttl2 = durToTtl(seconds(delayed ? 0.2 : 1));
                    dirOffset = DIR-dir;
                    children.add(this);
                }

                public void doLoop() {
                    super.doLoop();
                    double KSPdir = dir+dirOffset;
                    double KSPx = cos(KSPdir)*KSradius;
                    double KSPy = sin(KSPdir)*KSradius;


                    gc.setGlobalAlpha(ttl*ttl);
                    gc.setGlobalBlendMode(ADD);
                    // gc.setGlobalBlendMode(ADD);
                    drawRotatedImage(gc, KINETIC_SHIELD_PIECE_GRAPHICS, deg(PI/2+KSPdir), KSPx+x-KINETIC_SHIELD_PIECE_GRAPHICS.getWidth()/2, KSPy+y-KINETIC_SHIELD_PIECE_GRAPHICS.getHeight()/2);
                    gc.setGlobalAlpha(1);
                    gc.setGlobalBlendMode(SRC_OVER);

                    ttl2--;
                    if(ttl2<0) {
                        ttl -= ttl_frac;
                        if(ttl<0) game.nextFrameJobs.add(this::dispose);
                    }
                }
                public void dispose() {
                    passivate(); // forcefully deactivate
                    onPassivateStart(); // forcefully deactivate
                    onPassivateEnd(); // forcefully deactivate
                    if(ability_main==this) ability_main = null;
                    children.remove(this);
                }
            }
            /** Emits shield pulses. */
            class ShieldPulseEmitter implements LO {
                double ttl = 0;

                public void doLoop() {
                    ttl--;
                    if(ttl<0) {
                        ttl = durToTtl(seconds(1+kinetic_shield.KSradius/100*0.7));
                        ShieldPulse p = new ShieldPulse(Ship.this,x,y);
                                    p.dxy = 0.4;
                                    p.ttld = 1/(1.3*KSradius/0.4);
                    }
                }
            }
        }
    }
    /** Default player ship. */
    class Rocket extends Ship {

        final Player player;
        InEffect rapidfire = new InEffect();
        int powerfire = 1; // stacks up multipliers


        Rocket(Player PLAYER) {
            super(
                Rocket.class,
                playfield.getWidth()/2,playfield.getHeight()/2,0,0,PLAYER_HIT_RADIUS,
                new Icon(MaterialDesignIcon.ROCKET,40),
                PLAYER_ENERGY_INITIAL,PLAYER_E_BUILDUP
            );
            player = PLAYER;
            ((Icon)graphics).setFill(player.color.getValue());
            ((Icon)graphics).styleclass("comet-rocket");
            kinetic_shield = new KineticShield(ROCKET_KINETIC_SHIELD_RADIUS,ROCKET_KINETIC_SHIELD_ENERGYMAX);
            changeAbility(player.ability_type.get());
            engine = random()<0.5 ? new RocketEngine() : new PulseEngine();
            gun = new Gun(
                MANUAL,
                PLAYER_GUN_RELOAD_TIME,
                () -> dir,
                dir -> new Bullet(
                    this,
                    x + PLAYER_BULLET_OFFSET*cos(dir),
                    y + PLAYER_BULLET_OFFSET*sin(dir),
                    dx + pow(1.5,powerfire)*cos(dir)*PLAYER_BULLET_SPEED,
                    dy + pow(1.5,powerfire)*sin(dir)*PLAYER_BULLET_SPEED,
                    0,
                    PLAYER_BULLET_TTL,
                    game.graphicsPool.get(Bullet.class)
                )
            );
        }

        void draw() {
            super.draw();
            graphics.setRotate(deg(PLAYER_GRAPHICS_ANGLE_OFFSET + dir));
        }
        void changeAbility(AbilityKind type ){
            if(ability_main!=null) ability_main.dispose();
            switch(type) {
                case DISRUPTOR : ability_main = new Disruptor(); break;
                case HYPERSPACE : ability_main =  new Hyperspace(); break;
                case SHIELD : ability_main =  new Shield(); break;
                default: throw new AssertionError("Illegal switch case " + this);
            }
            children.add(ability_main);
        }

        boolean isHitDistance(PO o) {
            if(ability_main instanceof Shield && ability_main.isActivated()) {
                return isDistanceLess(o,SHIELD_RADIUS+(o.dx+o.dy)/2+o.radius);
            }
            if(o instanceof Bullet)
                return isDistanceLess(o,kinetic_shield.KSradius+PLAYER_BULLET_SPEED/2+o.radius);
            if(o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
                return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
            return isDistanceLess(o,radius+o.radius);
        }

    }
    /** Default enemy ship. */
    class Ufo extends Ship {

        long dirChangeTTL = 0;
        boolean aggressive = false;
        Runnable radio = () -> game.ufos.pulseCall(this);

        Ufo() {
            this(randEnum(Side.class),false);
        }
        Ufo(Side side, boolean AGGRESSIVE) {
            super(
                Ufo.class,
                (side==RIGHT ? 1 : 0) * playfield.getWidth(),
                random()*playfield.getHeight(),0,0,UFO_HIT_RADIUS,
                new Icon(MaterialDesignIcon.BIOHAZARD,40){{ setFill(game.ufos.color); }},
                UFO_ENERGY_INITIAL,UFO_E_BUILDUP
            );
            dir = x<playfield.getWidth()/2 ? 0 : PI; // left->right || left<-right
            aggressive = AGGRESSIVE;
            gun = new Gun(
                AUTO,
                UFO_GUN_RELOAD_TIME,
                () -> {
                    if(!aggressive || game.ufos.ufo_enemy==null) return rand0N(2*PI);
                    Rocket enemy = isDistanceLess(game.ufos.ufo_enemy, UFO_BULLET_RANGE)
                                        ? game.ufos.ufo_enemy
                                        : game.oss.get(Rocket.class).stream().sorted(by(this::distance)).findFirst().orElse(null);
                    return enemy==null ? rand0N(2*PI) : dir(enemy) + randMN(-PI/6,PI/6);
                },
                dir -> new Bullet(
                    this,
                    x + UFO_BULLET_OFFSET*cos(dir),
                    y + UFO_BULLET_OFFSET*sin(dir),
                    dx + cos(dir)*UFO_BULLET_SPEED,
                    dy + sin(dir)*UFO_BULLET_SPEED,
                    0,
                    UFO_BULLET_TTL,
                    game.graphicsPool.get(Bullet.class)
                )
            );
            game.nextFrameJobs.addPeriodic(() -> durToTtl(seconds(5)), radio);
        }

        void doLoopBegin() {
            super.doLoopBegin();
            dirChangeTTL--;
            if(dirChangeTTL<0) {
                dirChangeTTL=(long)((long)(2+random()*2)*FPS);
                // generate new direction
                double r = random();
                if(dir==0)            dir = r<0.5 ? PI/4 : -PI/4;
                else if(dir==PI/4)    dir = r<0.5 ? 0 : -PI/4;
                else if(dir==-PI/4)   dir = r<0.5 ? 0 : PI/4;
                else if(dir== PI)     dir = r<0.5 ? 3*PI/4 : 3*PI/4;
                else if(dir== 3*PI/4) dir = r<0.5 ? PI : -3*PI/4;
                else if(dir==-3*PI/4) dir = r<0.5 ? PI : 3*PI/4;
            }
        }
        void move() {
            dx = cos(dir)*UFO_SPEED;
            dy = sin(dir)*UFO_SPEED;
        }
        void doLoopOutOfField() {
            if(y<0) y = playfield.getHeight();
            if(y>playfield.getHeight()) y = 0;
            if(x<0 || x>playfield.getWidth()) canDispose = true;
        }

        @Override
        void draw() {
            super.draw();

            gc.setGlobalBlendMode(OVERLAY);
            gc.setFill(new RadialGradient(deg(dir),0,0.5,0.5,1,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(1,Color.rgb(114,208,74))));
            gc.fillOval(x-125,y-125,250,250);
            gc.setGlobalBlendMode(SRC_OVER);
        }
        @Override
        public void dispose() {
            super.dispose();
            game.nextFrameJobs.remove(radio);
        }
    }
    class Shuttle extends Ship {
        double ttl = durToTtl(seconds(50));
        final double rotationAngle = randOf(-1,1)*deg(2*PI/durToTtl(seconds(20)));

        public Shuttle(Rocket r) {
            super(
                Shuttle.class, r.x+50,r.y-50,0,0,PLAYER_HIT_RADIUS,
                new Icon(FontAwesomeIcon.SPACE_SHUTTLE,40), 0,0
            );
            kinetic_shield = new KineticShield(SHUTTLE_KINETIC_SHIELD_RADIUS,SHUTTLE_KINETIC_SHIELD_ENERGYMAX);
            createHyperSpaceAnim(graphics).playClose();
            game.nextFrameJobs.add(3*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(4*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(5*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(6*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(7*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(8*ttl/10, () -> new Satellite(this,rand0N(2*PI)));
            game.nextFrameJobs.add(ttl, () -> createHyperSpaceAnim(graphics).playOpenDo(() -> canDispose=true));
        }
        boolean isHitDistance(PO o) {
            if(o instanceof Bullet)
                return isDistanceLess(o,kinetic_shield.KSradius+PLAYER_BULLET_SPEED/2+o.radius);
            if(o instanceof Asteroid && kineticEto(o)<kinetic_shield.KSenergy)
                return isDistanceLess(o,kinetic_shield.KSradius+o.radius);
            return isDistanceLess(o,radius+o.radius);
        }
        void draw() {
            super.draw();
            graphics.setRotate(graphics.getRotate()+rotationAngle);
        }
    }
    /** Represents defunct ship. Gives upgrades. */
    class Satellite extends PO {
        final RocketEnhancer e;

        public Satellite(Shuttle s, double DIR) {
            super(Satellite.class,
                s.x,s.y, 0.2*cos(DIR), 0.2*sin(DIR),
                SATELLITE_RADIUS/2, new Icon(MaterialDesignIcon.SATELLITE_VARIANT,40)
            );
            e = randomOfExcept(game.ROCKET_ENHANCERS,game.SHUTTLE_CALL_ENHANCER).apply();
            children = new HashSet<>(2);
            setScaleXY(graphics,0.5);
            if(game.humans.intelOn.is()) ((Icon)graphics).icon(e.icon);
        }
        public Satellite() {
            this(randEnum(Side.class));
        }
        public Satellite(Side dir) {
            super(Satellite.class,
                (dir==LEFT ? 0 : 1)*playfield.getWidth(), random()*playfield.getHeight(),
                (dir==LEFT ? 1 : -1)*SATELLITE_SPEED, 0,
                SATELLITE_RADIUS, new Icon(MaterialDesignIcon.SATELLITE_VARIANT,40)
            );
            children = new HashSet<>(2);
            e = randOf(game.ROCKET_ENHANCERS).apply();
            if(game.humans.intelOn.is()) new REIndicator(this,e);
        }

        void move() {}
        void doLoopOutOfField() {
            if(y<0) y = playfield.getHeight();
            if(y>playfield.getHeight()) y = 0;
            if(x<0 || x>playfield.getWidth()) canDispose = true;
        }
        void pickUpBy(Rocket r) {
            e.start(r);
            game.nextFrameJobs.add(e.duration,() -> e.stop(r));
            new REIndicator(r,e);

            canDispose = true;
        }
    }
    /** Gun projectile. */
    class Bullet extends PO {
        final Ship owner;
        double ttl = PLAYER_BULLET_TTL;
        boolean dead = false;  // avoids multiple collisions

        Bullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL, Node graphics) {
            super(Bullet.class,x,y,dx,dy,hit_radius,graphics);
            owner = ship;
            ttl = TTL;
        }

        public void doLoop() {
            x += dx;
            y += dy;
            doLoopOutOfField();
            relocateCenter(graphics,x,y);
            ttl--;
            if(ttl<0) canDispose = true;
        }

        public void dispose() {
            super.dispose();
            game.graphicsPool.add(Bullet.class,graphics);
        }

    }
    class Particle extends PO {
        double ttld;
        double ttl;

        Particle(double x, double y, double dx, double dy, double TTL, Node graphics) {
            super(Particle.class, x,y,dx,dy,0,graphics);
            ttl = 1;
            ttld = 1/TTL;
        }

        void init() {
            if(graphics!=null) playfield.getChildren().add(graphics);
            game.oss.add(this);
        }
        void move(){}
        public void doLoop() {
            move();
            x += dx;
            y += dy;
            doLoopOutOfField();
            draw();

            ttl -= ttld;
            if(ttl<0) canDispose = true;
        }
        void draw() {
            if(graphics!=null) {
                relocateCenter(graphics,x,y);
                graphics.setOpacity(ttl);
            }
        }
    }
    class PulseEngineDebris extends Particle {
        PulseEngineDebris(double x, double y, double dx, double dy) {
            super(x,y,dx,dy,13d,game.graphicsPool.get(PulseEngineDebris.class));
        }

        public void dispose() {
            super.dispose();
            game.graphicsPool.add(PulseEngineDebris.class,graphics);
        }
    }
    class UfoEngineDebris extends Particle {

        public UfoEngineDebris(double x, double y, double radius) {
            super(x,y,0,0,durToTtl(seconds(0.5)),null);
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

        void drawBack() {
            double r = 5+(radius-5)*(ttl);
            double d = r*2;
            gc_bgr.setFill(game.mission.color);
            gc_bgr.fillOval(x-r,y-r,d,d);
        }
        void drawFront() {
            double r = 5+(radius-5)*(ttl);
            double d = r*2;
            gc_bgr.setFill(Color.BLACK);
            gc_bgr.fillOval(x-r,y-r,d,d);
        }
    }
    /** Omnidirectional expanding wave. Represents active communication of the ship. */
    class RadioWavePulse extends Particle {
        final double dxy;
        final boolean rect;
        double radius = 0;
        final Color color;
        final boolean inverse;

        RadioWavePulse(double x, double y, double dx, double dy, double EPANSION_RATE, Color COLOR, boolean RECTANGULAR) {
            super(x,y,dx,dy,durToTtl(seconds(2)),null);
            dxy = EPANSION_RATE;
            inverse = dxy<0;
            color = COLOR;
            rect = RECTANGULAR;
            radius = inverse ? -dxy*ttl/ttld : 0;
        }

        void draw() {
            radius += dxy;
            gc.setGlobalAlpha(inverse ? 1-ttl : ttl);
            gc.setStroke(color);
            gc.setLineWidth(2);
            if(rect) {
                gc.save();
                rotate(gc, 360*ttl/3, x,y);
                gc.strokeRect(x-radius,y-radius, 2*radius,2*radius);
                gc.restore();
                gc.rotate(0);
            } else {
                gc.strokeOval(x-radius,y-radius, 2*radius,2*radius);
            }
            gc.setGlobalAlpha(1);
        }
    }

    static Color COLOR_DB = new Color(0.11764706f,0.5647059f,1.0f,0.2);
    class ShieldPulse extends Particle {
        double dxy;
        Color color;
        final Ship s;

        ShieldPulse(Ship ship, double x, double y) {
            super(x,y,0,0,0,null);
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
            if(s.canDispose) return;
//            gc.setGlobalBlendMode(OVERLAY);
            gc.setGlobalAlpha((s.kinetic_shield.KSenergy/s.kinetic_shield.KSenergy_max)*sqrt(ttl));
            gc.setStroke(null);
            gc.setFill(new RadialGradient(0,0,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0.3,Color.TRANSPARENT),new Stop(0.85,COLOR_DB),new Stop(1,Color.TRANSPARENT)));
            drawOval(gc,x,y,radius);
            gc.setGlobalAlpha(1);
//            gc.setGlobalBlendMode(SRC_OVER);
        }
    }
    class RocketEngineDebris extends Particle {
        RocketEngineDebris(double x, double y, double dx, double dy) {
            super(x,y,dx,dy,3d,game.graphicsPool.get(RocketEngineDebris.class));
        }

        public void dispose() {
            super.dispose();
            game.graphicsPool.add(RocketEngineDebris.class,graphics);
        }
    }
    /** Default floating space object. */
    class Asteroid<M extends Mover> extends PO {
        final Image graphicsImage;
        final double graphicsScale;
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
//            super(Asteroid.class, X, Y, SPEED*cos(DIR), SPEED*sin(DIR), RADIUS*(50+15*random()), null);
            super(Asteroid.class, X, Y, SPEED*cos(DIR), SPEED*sin(DIR), RADIUS, null);
            size = radius;
            dir = DIR;
            speed = SPEED;
            graphicsImage = size<=10 ? ASTEROID_GRAPHICS20 : size<=25 ? ASTEROID_GRAPHICS50 : ASTEROID_GRAPHICS100;
            graphicsScale = ARBITRARY_CONSTANT1*2*size/graphicsImage.getWidth();
            splits = size>size_min ? 2 : 0;
        }

        void move() {}
        void draw() {
            drawScaledImage(gc, graphicsImage,x,y,graphicsScale);
        }
        void onHit(PO o) {
            hits++; // hits are checked only for bullets
            if(!(o instanceof Bullet) || hits>hits_max) split();
            else size *= size_hitdecr;
            onHitParticles(dir(o));
        }
        void split() {
            canDispose = true;
            game.nextFrameJobs.add(() ->
                repeat(splits, i -> {
                    double h = random();
                    double v = random();
                    double dxnew = dx+randMN(-1,1.1);
                    double dynew = dy+randMN(-1,1.1);
                    double speednew = sqrt(dxnew*dxnew+dynew*dynew);
                    double dirnew = dirOf(dxnew,dynew,speednew);
                    game.mission.planetoidConstructor.apply(x+h*0.2*size,y+v*0.2*size,speednew,dirnew, size_child*size);
                })
            );
        }
        void onHitParticles(double direction) {}
    }

    private static interface Mover {
        void calcSpeed(Asteroid o);
    }
    private static class OrganelleMover implements Mover {
            double dirchange = rand0N(2*PI)/5/FPS;
            double ttldirchange = durToTtl(seconds(rand0N(12)));
            double ttldirchanging = durToTtl(seconds(rand0N(3)));

            public void calcSpeed(Asteroid o){
                // rotate at random time for random time period by random angle
                ttldirchange--;
                if(ttldirchange<0) {
                    o.dir += dirchange;
                    ttldirchanging--;
                    if(ttldirchanging<0) {
                        ttldirchange = durToTtl(seconds(rand0N(10)));
                        ttldirchanging = durToTtl(seconds(rand0N(3)));
                    }
                }
                o.dx = o.speed*cos(o.dir);
                o.dy = o.speed*sin(o.dir);
            }
        }

    class Energ extends Asteroid<OrganelleMover> {
        Color colordead = Color.BLACK;
        Color coloralive;
        double heartbeat = 0;
        double heartbeat_speed = 0.5*2*PI/durToTtl(seconds(1+rand0N(size/30))); // times/sec

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
            double d = radius*2;
//            gc_bgr.setGlobalBlendMode(DARKEN);
            gc_bgr.setEffect(null);
            gc_bgr.setStroke(null);
            gc_bgr.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0+abs(0.3*sin(heartbeat)),colordead),new Stop(0.5,coloralive),new Stop(1,Color.TRANSPARENT)));
            drawOval(gc_bgr,x,y,radius);
//            gc_bgr.setGlobalBlendMode(SRC_OVER);
        }

        void onHitParticles(double hitdir) {
            int particles = (int)radius/2;
            repeat(5*particles, i -> new EnergParticle(hitdir));
        }

        class EnergParticle extends Particle {
            final double r = randMN(0.5,2.5+Energ.this.size);

            public EnergParticle(double hitdir) {
                super(
                    Energ.this.x+Energ.this.radius*cos(hitdir),
                    Energ.this.y+Energ.this.radius*sin(hitdir),
                    Energ.this.dx + randMN(-1,1) + 1.5*random()*cos(hitdir),
                    Energ.this.dy + randMN(-1,1) + 1.5*random()*sin(hitdir),
                    durToTtl(seconds(0.5+rand0N(1)+rand0N(size))),null
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
            double d = radius*2;
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
    private class Inkoid extends Asteroid<OrganelleMover> {
        double trail_ttl = durToTtl(seconds(0.5+rand0N(2)));

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
            if(trail_ttl<0) {
                trail_ttl = durToTtl(seconds(0.5+rand0N(2)));
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
        void onHitParticles(double hitdir) {
            int particles = (int)randMN(1,3);
            repeat(particles, i ->
                new InkoidDebris(
                    x,y,
                    randMN(-2,2),randMN(-2,2),
                    2 + random()*size_child*radius/4,
                    seconds(0.5+rand0N(1)+rand0N(size))
                )
            );
        }
    }
    private class InkoidGraphics extends Particle implements Draw2 {
        double r;

        public InkoidGraphics(double x, double y, double RADIUS) {
            this(x,y,0,0,RADIUS,seconds(0.4));
        }
        public InkoidGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
            super(x,y,dx,dy,durToTtl(time),null);
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
            gc_bgr.setFill(Color.BLACK);
            gc_bgr.fillOval(x-rr,y-rr,d,d);
        }
    }
    private class InkoidDebris extends InkoidGraphics {
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
            gc_bgr.setFill(Color.BLACK);
            gc_bgr.fillOval(x-rr,y-rr,d,d);
        }
    }
    private class Genoid extends Asteroid<OrganelleMover> {
        double circling = 0;
        double circling_speed = 0.5*2*PI/durToTtl(seconds(0.5)); // times/sec
        double circling_mag = 0;
        final PTTL trail = new PTTL(() -> durToTtl(seconds(0.5+rand0N(2))),() -> {
            if(0.9>size && size >0.4) {
                new GenoidDebris(x+3*radius*cos(circling),y+2*radius*sin(circling),0,0,2,seconds(1.6));
                new GenoidDebris(x+3*radius*cos(circling),y+2*radius*sin(circling),0,0,2,seconds(1.6));
            } else {
                new GenoidDebris(x+circling_mag*2*radius*cos(dir+PI/2),y+circling_mag*2*radius*sin(dir+PI/2),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
                new GenoidDebris(x+circling_mag*2*radius*cos(dir-PI/2),y+circling_mag*2*radius*sin(dir-PI/2),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
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

        void onHit(PO o) {
            super.onHit(o);
            propulsion.dirchange *= 2; // speed rotation up
            propulsion.ttldirchange = -1; // change direction now
        }
        void draw() {
            new GenoidGraphics(x,y,radius);
        }
        void onHitParticles(double hitdir) {
            int particles = (int)randMN(1,3);
            repeat(particles, i ->
                new GenoidDebris(
                    x,y,
                    randMN(-2,2),randMN(-2,2),
                    2 + random()*size_child*radius/4,
                    seconds(0.5+rand0N(1)+rand0N(size))
                )
            );
        }
    }
    private class GenoidGraphics extends Particle implements Draw2 {
        double r;

        public GenoidGraphics(double x, double y, double RADIUS) {
            this(x,y,0,0,RADIUS,seconds(0.4));
        }
        public GenoidGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
            super(x,y,dx,dy,durToTtl(time),null);

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
            gc_bgr.setFill(game.mission.color);
            gc_bgr.fillOval(x-rr,y-rr,d,d);
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
    private class Fermi extends Asteroid<OrganelleMover> {
        final PTTL trail = new PTTL(() -> durToTtl(seconds(0.5+rand0N(2))), () -> new FermiDebris(x,y,0,0,5,seconds(2)));

        public Fermi(double X, double Y, double SPEED, double DIR, double LIFE) {
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
            trail.run();
        }

        void onHit(PO o) {
            super.onHit(o);
            propulsion.dirchange *= 2; // speed rotation up
            propulsion.ttldirchange = -1; // change direction now
        }
        void draw() {
            new FermiGraphics(x,y,radius);
        }
        void onHitParticles(double hitdir) {
            int particles = (int)randMN(1,3);
            repeat(particles, i ->
                new FermiDebris(
                    x,y,
                    randMN(-2,2),randMN(-2,2),
                    2 + random()*size_child*radius/4,
                    seconds(0.5+rand0N(1)+rand0N(size))
                )
            );
        }
    }
    private class FermiGraphics extends Particle implements Draw2 {
        double r;
        Color color = game.mission.color;

        public FermiGraphics(double x, double y, double RADIUS) {
            this(x,y,0,0,RADIUS,seconds(0.4));
        }
        public FermiGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
            super(x,y,dx,dy,durToTtl(time),null);
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
    /** Generates force field affecting objects. */
    interface ForceField extends LO {
        boolean isInHyperspace();
        double getX();
        double getY();
        double force(double mass, double dist);
        default void doLoop(){}
    }

    static abstract class RocketEnhancer {
        final GlyphIcons icon;
        final Duration duration;
        RocketEnhancer(GlyphIcons ICON, Duration DURATION) {
            icon = ICON;
            duration = DURATION;
        }
        abstract void start(Rocket r);
        abstract void stop(Rocket r);
    }
    static class RapidFire extends RocketEnhancer {
        RapidFire() {
            super(MaterialDesignIcon.BLACKBERRY, seconds(12));
        }
        void start(Rocket r) {
            r.rapidfire.inc();
        }
        void stop(Rocket r) {
            r.rapidfire.dec();
        }
    }
    static class PowerFire extends RocketEnhancer {
        PowerFire() {
            super(MaterialDesignIcon.DOTS_HORIZONTAL, seconds(25));
        }
        void start(Rocket r) {
            r.powerfire++;
        }
        void stop(Rocket r) {
            r.powerfire--;
        }
    }
    static class Mobility extends RocketEnhancer {
        Mobility() {
            super(MaterialDesignIcon.TRANSFER, seconds(25));
        }
        void start(Rocket r) { r.engine.mobilityInc(); }
        void stop(Rocket r) { r.engine.mobilityDec(); }
    }
    static class ProximityAlert extends RocketEnhancer {
        public ProximityAlert() {
            super(MaterialDesignIcon.ALERT_OCTAGON, seconds(30));
        }
        public void start(Rocket r) {
        }
        void stop(Rocket r) {
        }
    }
    class Intel extends RocketEnhancer {
        public Intel() {
            super(MaterialDesignIcon.EYE, minutes(1));
        }
        public void start(Rocket r) {
            game.humans.intelOn.inc();
        }
        void stop(Rocket r) {
            game.humans.intelOn.dec();
        }
    }
    class LaserSight extends RocketEnhancer {
        LO laser;
        public LaserSight() {
            super(MaterialDesignIcon.RULER, seconds(20));
        }
        public void start(Rocket r) {
            laser = new LO() {
                final Node graphics = new Icon(MaterialDesignIcon.MINUS,7);
                final double w;
                final double h;
                {
                    playfield.getChildren().add(graphics);
                    graphics.setScaleX(BONUS_LASER_MULTIPLIER_LENGTH/7d);
                    w = graphics.getLayoutBounds().getWidth()*graphics.getScaleX();
                    h = graphics.getLayoutBounds().getHeight();
                }
                public void doLoop() {
                    double c = cos(r.dir);
                    double s = sin(r.dir);
                    double cr = cos(r.dir-PI);
                    double sr = sin(r.dir-PI);
                    graphics.setRotate(deg(r.dir));
                    graphics.relocate(
                        r.x + 20*c - (-0.5*w*cr + w*cr),
                        r.y + 20*s - 0.5*w*sr
                    );
                }

                public void dispose() {
                    playfield.getChildren().remove(graphics);
                }
            };
            r.children.add(laser);
        }
        void stop(Rocket r) {
            r.children.remove(laser);
            laser.dispose();
        }
    }
    static class KineticShieldEnergizer extends RocketEnhancer {
        public KineticShieldEnergizer() {
            super(MaterialDesignIcon.IMAGE_FILTER_TILT_SHIFT, seconds(5));
        }
        public void start(Rocket r) {
            r.kinetic_shield.KSenergy_max *= 1.1;
            r.kinetic_shield.changeKSenergyToMax();
        }
        void stop(Rocket r) {}
    }
    static class EnergyMaximizer extends RocketEnhancer {
        public EnergyMaximizer() {
            super(MaterialDesignIcon.BATTERY_POSITIVE, seconds(5));
        }
        public void start(Rocket r) {
            r.energy_max += 1000;
        }
        void stop(Rocket r) {}
    }
    class AimEnhancer extends RocketEnhancer {
        AimEnhancer() {
            super(MaterialDesignIcon.CROSSHAIRS, seconds(20));
        }
        public void start(Rocket r) {
        }
        void stop(Rocket r) {
        }
    }
    class ShuttleCall extends RocketEnhancer {
        ShuttleCall() {
            super(FontAwesomeIcon.SPACE_SHUTTLE, seconds(5));
        }
        public void start(Rocket r) {
            game.humans.sendShuttle(r);
        }
        void stop(Rocket r) {
        }
    }
    static abstract class Energizer extends RocketEnhancer {
        final double energy;
        Energizer(GlyphIcons ICON, double ENERGY) {
            super(ICON, seconds(5));
            energy = ENERGY;
        }
        public void start(Rocket r) {
            r.energy = min(r.energy+energy,r.energy_max);
        }
        void stop(Rocket r) {}
    }
    static class EnergizerSmall extends Energizer {
        public EnergizerSmall() { super(MaterialDesignIcon.BATTERY_30,2000); }
    }
    static class BaterryMedium extends Energizer {
        public BaterryMedium() { super(MaterialDesignIcon.BATTERY_60,5000); }
    }
    static class BaterryLarge extends Energizer {
        public BaterryLarge() { super(MaterialDesignIcon.BATTERY,10000); }
    }
    /** Rocket enhancer indicator icon moving with player rocket to indicate active enhancers. */
    class REIndicator implements LO {
        double ttl;
        final PO owner;
        final Node graphics;
        final int index;

        public REIndicator(PO OWNER, RocketEnhancer enhancer) {
            owner = OWNER;
            ttl = durToTtl(owner instanceof Satellite ? minutes(10) : enhancer.duration);
            index = findFirst(i -> owner.children.stream().filter(REIndicator.class::isInstance).noneMatch(o -> ((REIndicator)o).index==i),0);
            owner.children.add(this);
            graphics = new Icon(enhancer.icon,15);
            playfield.getChildren().add(graphics);
        }

        public void doLoop() {
            ttl--;
            if(ttl<0) game.nextFrameJobs.add(this::dispose);
            relocateCenter(graphics, owner.x+30+20*index, owner.y-30); // javafx inverts y, hence minus
        }

        public void dispose() {
            owner.children.remove(this);
            playfield.getChildren().remove(graphics);
        }

    }

/**************************************************************************************************/

    static enum Side { LEFT,RIGHT; }
    static enum GunControl { AUTO,MANUAL; }
    static enum AbilityState { ACTIVATING, PASSSIVATING, NO_CHANGE; }
    static enum AbilityKind { HYPERSPACE,DISRUPTOR,SHIELD; }
    static enum Faction {
        HumanSpaceForce,
        UnidentifiedFrontlineOpposition,
        ENERGION;

        String initials() { return this==HumanSpaceForce ? "H.S.F" : "U.F.O."; }
        boolean isEnemy() {
            return true;
        }
    }
    static enum Relations { ALLY, NEUTRAL, ENEMY; }

    /** Weighted boolean - stores how many times it is. False if not once. True if at least once. */
    static class InEffect {
        private int times = 0;

        boolean is() {
            return times>0;
        }

        int isTimes() {
            return times;
        }

        void inc() {
            times++;
        }

        void dec() {
            times--;
        }

        void reset() {
            times = 0;
        }
    }
    static class ObjectStore<O> {
        private final Map<Class,Set<O>> m = new HashMap<>();
        private final Ƒ1<O,Class> mapper;

        public ObjectStore(Ƒ1<O,Class> classMapper) {
            mapper = classMapper;
        }

        public void add(O o) {
            m.computeIfAbsent(mapper.apply(o), c -> new HashSet<>()).add(o);
        }

        public void remove(O o) {
            Set l = m.get(mapper.apply(o));
            if(l!=null) l.remove(o);
        }

        public <T extends O> Set<T> get(Class<T> c) {
            return m.getOrDefault(c,EMPTY_SET);
        }

        public void clear() {
            m.values().forEach(Set::clear); m.clear();
        }

        public <T extends O> void forEach(Class<T> c, Consumer<? super T> action) {
            Set<T> l = (Set) m.get(c);
            if(l!=null) l.forEach(action);
        }

        public void forEach(Consumer<? super O> action) {
            m.forEach((k,set) -> set.forEach(action));
        }

        public <T extends O,E extends O> void forEach(Class<T> t, Class<E> e, BiConsumer<? super T,? super E> action) {
            if(t==e) forEachCartesianHalfNoSelf(get(t), (BiConsumer)action);
            else forEachCartesian(get(t),get(e), action);
        }

        public void forEachSet(Consumer<? super Set<O>> action) {
            m.values().forEach(action);
        }
    }
    static class Pool<P> {
        private final List<P> pool;
        public final int max;
        private final Ƒ0<P> fac;

        public Pool(int max_size, Ƒ0<P> factory) {
            max = max_size;
            fac = factory;
            pool = new ArrayList<>(max_size);
        }

        void add(P p) {
            if(pool.size()<max) pool.add(p);
        }
        P get() {
            P n = pool.isEmpty() ? null : pool.get(0);
            if(n!=null) pool.remove(0);
            return n==null ? fac.apply() : n;
        }

        int sizeNow(){ return pool.size(); }
        int sizeMax(){ return max; }
        void clear() { pool.clear(); }
    }
    static class PoolMap<P> {
        private final ClassMap<Pool<P>> pools = new ClassMap<>();
        private final ClassMap<Ƒ1<Class,Pool<P>>> factories = new ClassMap<>();

        public PoolMap() {
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
    static class SpatialHash {
        private final int xmax;
        private final int ymax;
        private final int bucketspan;
        private final Set<PO>[][] a;

        public SpatialHash(int sizex, int sizey, int bucket_SPAN) {
            xmax = sizex;
            ymax = sizey;
            bucketspan = bucket_SPAN;
            a = new Set[xmax][ymax];
            for(int i=0; i<xmax; i++)
                for(int j=0; j<ymax; j++)
                    a[i][j] = new HashSet<>();
        }

        void add(Collection<PO> os) {
            for(PO o : os) {
                int i = (int) o.x/bucketspan;
                int j = (int) o.y/bucketspan;
                a[i][j].add(o);
            }
//            for(PO o : os) {
//                int mini = (int) (o.x-o.hit_radius)/bucketspan;
//                int minj = (int) (o.y-o.hit_radius)/bucketspan;
//                int maxi = (int) (o.x+o.hit_radius)/bucketspan;
//                int maxj = (int) (o.y+o.hit_radius)/bucketspan;
//                for(int i=mini; i<=maxi; i++)
//                    for(int j=minj; j<maxj; j++)
//                        a[i][j].add(o);
//            }
        }
        void forEachClose(PO o, BiConsumer<? super PO, ? super PO> action) {
            int mini = (int) (o.x-o.radius)/bucketspan;
            int minj = (int) (o.y-o.radius)/bucketspan;
            int maxi = (int) (o.x+o.radius)/bucketspan;
            int maxj = (int) (o.y+o.radius)/bucketspan;
            for(int i=mini; i<=maxi; i++)
                for(int j=minj; j<maxj; j++)
                    for(PO e : a[i][j])
                        action.accept(o,e);
        }
    }
    static class TTLList implements Runnable {
        final List<TTL> lt = new ArrayList<>();
        final List<PTTL> lpt = new ArrayList<>();
        final RunnableSet lr = new RunnableSet();
        final Set<Runnable> temp = new HashSet<>();

        void add(Runnable r) {
            lr.add(r);
        }
        void add(double ttl, Runnable r) {
            lt.add(new TTL(ttl, r));
        }
        void add(Duration delay, Runnable r) {
            lt.add(new TTL(durToTtl(delay), r));
        }
        void addPeriodic(Duration delay, Runnable r) {
            lpt.add(new PTTL(() -> durToTtl(delay), r));
        }
        void addPeriodic(Ƒ0<Double> ttl, Runnable r) {
            lpt.add(new PTTL(ttl, r));
        }

        void remove(Runnable r) {
            lt.removeIf(t -> t.r==r);
            lpt.removeIf(t -> t.r==r);
            lr.remove(r);
        }

        public void run() {
            for(int i=lt.size()-1; i>=0; i--) {
                TTL t = lt.get(i);
                if(t.ttl>1) t.ttl--;
                else {
                    lt.remove(i);
                    temp.add(t);
                }
            }
            temp.forEach(Runnable::run);
            temp.clear();

            for(int i=lpt.size()-1; i>=0; i--) {
                PTTL t = lpt.get(i);
                if(t.ttl>1) t.ttl--;
                else t.run();
            }

            lr.run();
            lr.clear();
        }

        public void clear() {
            lt.clear();
            lr.clear();
            lpt.clear();
        }
    }
    static class TTL implements Runnable {
        double ttl;
        final Runnable r;

        public TTL(double TTL, Runnable R) {
            ttl = TTL;
            r = R;
        }

        public void run() {
            r.run();
        }
    }
    static class PTTL extends TTL {
        final Ƒ0<Double> ttlperiod;

        public PTTL(Ƒ0<Double> TTL, Runnable R) {
            super(0, R);
            ttlperiod = TTL;
            ttl = TTL.get();
        }

        public void run() {
            r.run();
            ttl = ttlperiod.get();
        }

    }

    /** Relocates node such the center of the node is at the coordinates. */
    private static void relocateCenter(Node n, double x, double y) {
        n.relocate(x-n.getLayoutBounds().getWidth()/2,y-n.getLayoutBounds().getHeight()/2);
    }
    /** Converts radians to degrees. */
    private static double deg(double rad) {
        return Math.toDegrees(rad); //360*rad/(2*PI);
    }
    /** Returns angle in rad for given sin and cos. */
    private static double dirOf(double x, double y, double dist) {
        double c = x/dist;
        double s = y/dist;
        double ac = acos(c);
        double as = asin(s);
        if(c>0) return as;
        return (s<0) ? acos(c) : acos(c)+PI/2;
    }
    private Node createPlayerStat(Player p) {
        Label score = new Label();
        p.score.maintain(s -> score.setText("Score: " + s));

        Label nameL = new Label();
        maintain(p.name,nameL::setText);

        Ƒ0<Icon> liveIconFactory = () -> new Icon(MaterialDesignIcon.ROCKET);
        HBox lives = layHorizontally(5, CENTER_LEFT);
        repeat(p.lives.get(), () -> lives.getChildren().add(liveIconFactory.apply()));
        p.lives.onChange((ol,nl) -> {
            repeat(ol-nl, i -> {
                Icon ic = (Icon)lives.getChildren().get(lives.getChildren().size()-1-i);
                createHyperSpaceAnim(ic).playOpenDo(() -> lives.getChildren().remove(ic));
            });
            repeat(nl-ol, () -> {
                Icon ic = liveIconFactory.apply();
                lives.getChildren().add(ic);
                createHyperSpaceAnim(ic).intpl(x -> 1-x).playOpen();
            });
        });

        Label energy = new Label();
        p.energy.maintain(e -> energy.setText("Energy: " + e.intValue()));

        VBox node = layVertically(5, CENTER_LEFT,
            nameL, score,lives,energy,
            ConfigField.create(Config.forProperty("Ability",p.ability_type)).getNode()
        );
        node.setMaxHeight(VBox.USE_PREF_SIZE); // fixes alignment in parent by not expanding this box
        node.setUserData(p.id); // to recognize which belongs to which
        return node;
    }
    private static Anim createHyperSpaceAnim(Node n) {
        return new Anim(millis(200), x -> setScaleXY(n,1-x*x));
    }
    /** Snapshot an image out of a node, consider transparency. */
    private static Image createImage(Node n) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        int imageWidth = (int) n.getBoundsInLocal().getWidth();
        int imageHeight = (int) n.getBoundsInLocal().getHeight();

        WritableImage wi = new WritableImage(imageWidth, imageHeight);
        n.snapshot(parameters, wi);

        return wi;

    }
    /** Creates image from icon. */
    private static Image graphics(GlyphIcons icon, double radius, Color c, Effect effect) {
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
    private void rotate(GraphicsContext gc, double angle, double px, double py) {
        Rotate r = new Rotate(angle, px, py);
        gc.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
    }
    /**
     * Draws an image on a graphics context.
     *
     * The image is drawn at (tlpx, tlpy) rotated by angle pivoted around the point:
     *   (tlpx + image.getWidth() / 2, tlpy + image.getHeight() / 2)
     *
     * @param gc the graphics context the image is to be drawn on.
     * @param angle the angle of rotation.
     * @param tlpx the top left x co-ordinate where the image will be plotted (in canvas co-ordinates).
     * @param tlpy the top left y co-ordinate where the image will be plotted (in canvas co-ordinates).
     */
    private void drawRotatedImage(GraphicsContext gc, Image i, double angle, double tlpx, double tlpy) {
        gc.save();
        rotate(gc, angle, tlpx + i.getWidth() / 2, tlpy + i.getHeight() / 2);
        gc.drawImage(i, tlpx, tlpy);
        gc.restore();
    }
    private void drawImage(GraphicsContext gc, Image i, double x, double y) {
        gc.drawImage(i, x+i.getWidth()/2, y+i.getHeight()/2, i.getWidth(), i.getHeight());
    }
    private void drawScaledImage(GraphicsContext gc, Image i, double x, double y, double scale) {
        gc.drawImage(i, x-scale*(i.getWidth()/2), y-scale*(i.getHeight()/2), scale*i.getWidth(), scale*i.getHeight());
    }
    private static void drawOval(GraphicsContext g, double x, double y, double r) {
        double d = 2*r;
        g.fillOval(x-r,y-r,d,d);
    }
    private static void drawRect(GraphicsContext g, double x, double y, double r) {
        double d = 2*r;
        g.fillOval(x-r,y-r,d,d);
    }

    private static double durToTtl(Duration d) {
        return d.toSeconds()*FPS;
    }

    public static double randMN(double m, double n) {
        return m+random()*(n-m);
    }
    public static double rand0N(double n) {
        return RAND.nextDouble()*n;
    }
    public static int rand0or1() {
        return randBoolean() ? 0 : 1;
    }
    public static int randInt(int n) {
        return RAND.nextInt(n);
    }
    public static boolean randBoolean() {
        return RAND.nextBoolean();
    }
    public static <E extends Enum> E randEnum(Class<E> enumtype) {
        return randOf(enumtype.getEnumConstants());
    }
    public static <T> T randOf(T a, T b) {
        return randBoolean() ? a : b;
    }
    public static <T> T randOf(T... c) {
        int l = c.length;
        return l==0 ? null : c[randInt(c.length)];
    }
    public static <T> T randOf(Collection<T> c) {
        int s = c.size();
        return s==0 ? null : c.stream().skip((long)(random()*(max(0,s)))).findAny().orElse(null);
    }
    /**
     * Returns random element from collection C except for those elements listen as exceptions.
     * Be sure not to cause infinite loop by excluding all elements.
     */
    public static <T> T randomOfExcept(Collection<T> c, T... excluded) {
        T t;
        do {
           t = randOf(c);
        } while(isInR(t,excluded));
        return t;
    }
    private static Random RAND = new Random();
}