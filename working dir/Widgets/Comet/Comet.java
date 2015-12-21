/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Comet;

import java.io.IOException;
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.BoxBlur;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
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

import Comet.Comet.Ship.Engine;
import Comet.Comet.Ship.Shield;
import Configuration.IsConfig;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import gui.objects.Text;
import gui.objects.icon.Icon;
import gui.pane.OverlayPane;
import util.R;
import util.SwitchException;
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

import static Comet.Comet.AbilityKind.*;
import static Comet.Comet.AbilityState.*;
import static Comet.Comet.GunControl.*;
import static Comet.Comet.Side.*;
import static gui.objects.Window.stage.UiContext.showSettings;
import static gui.objects.icon.Icon.createInfoIcon;
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
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.geometry.Pos.TOP_RIGHT;
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
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.minutes;
import static javafx.util.Duration.seconds;
import static util.Util.clip;
import static util.async.Async.run;
import static util.dev.Util.log;
import static util.functional.Util.array;
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
import static util.functional.Util.minBy;
import static util.functional.Util.range;
import static util.functional.Util.repeat;
import static util.functional.Util.set;
import static util.functional.Util.stream;
import static util.graphics.Util.bgr;
import static util.graphics.Util.layHeaderTop;
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
    GraphicsContext gc_bgr; // draws canvas game graphics on bgrcanvas
    final Text message = new Text();
    final Game game = new Game();
    final RunnableSet every200ms = new RunnableSet();
    final FxTimer timer200ms = new FxTimer(200,-1,every200ms);

    public Comet() {
        // message
        message.setOpacity(0);

        // playfield
        Rectangle playfieldMask = new Rectangle();
        playfieldMask.widthProperty().bind(playfield.widthProperty());
        playfieldMask.heightProperty().bind(playfield.heightProperty());
        playfield.setClip(playfieldMask);
        playfield.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

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

        // player stats
        double G = 10; // padding
        StackPane playerStats = layStack(
            layHorizontally(G,TOP_LEFT,     createPlayerStat(PLAYERS.get(0)),createPlayerStat(PLAYERS.get(4))),TOP_LEFT,
            layHorizontally(G,TOP_RIGHT,    createPlayerStat(PLAYERS.get(5)),createPlayerStat(PLAYERS.get(1))),TOP_RIGHT,
            layHorizontally(G,BOTTOM_LEFT,  createPlayerStat(PLAYERS.get(2)),createPlayerStat(PLAYERS.get(6))),BOTTOM_LEFT,
            layHorizontally(G,BOTTOM_RIGHT, createPlayerStat(PLAYERS.get(7)),createPlayerStat(PLAYERS.get(3))),BOTTOM_RIGHT
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
                new Icon<>(FontAwesomeIcon.GEARS,14,"Settings").onClick(e -> showSettings(getWidget(),e)),
                new Icon<>(FontAwesomeIcon.INFO,14,"How to play").onClick(() -> new HowToPane().show())
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
                if(cc==DIGIT1) game.runNext.add(() -> repeat(5, i -> game.mission.spawnPlanetoid()));
                if(cc==DIGIT2) game.runNext.add(() -> repeat(5, i -> new Ufo()));
                if(cc==DIGIT3) game.runNext.add(() -> repeat(5, i -> new Satellite()));
                if(cc==DIGIT4) game.runNext.add(() -> {
                    game.oss.forEach(Asteroid.class,a -> a.dead=true);
                    game.nextMission();
                });
                if(cc==DIGIT5) game.players.stream().filter(p -> p.alive).map(p -> p.rocket).forEach(game.humans::sendShuttle);
                if(cc==DIGIT6) game.oss.get(Rocket.class).forEach(r -> randOf(game.ROCKET_ENHANCERS).apply().enhance(r));
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


    static Font UI_FONT; // loaded in sttic block, we pack the font with the widget

    static double SIN45 = sin(PI/4);
    static double FPS = 60; // frames per second (locked)
    static double FPS_KEY_PRESSED = 40; // frames per second
    static double FPS_KEY_PRESSED_PERIOD = 1000/FPS_KEY_PRESSED; // ms

    static int PLAYER_LIVES_INITIAL = 5; // lives at the beginning of the game
    static int PLAYER_SCORE_NEW_LIFE = 10000; // we need int since we make use of int division
    static double SCORE_ASTEROID(Asteroid a) { return 30 + 2000/(4*a.radius); };
    static double SCORE_UFO = 250;
    static double SCORE_UFO_DISC = 100;
    static double BONUS_MOBILITY_MULTIPLIER = 1.25; // coeficient
    static double BONUS_LASER_MULTIPLIER_LENGTH = 400; // px
    static Duration PLAYER_RESPAWN_TIME = seconds(3); // die -> respawn time
    static double ROTATION_SPEED = 1.3*PI/FPS; // 540 deg/sec.
    static double RESISTANCE = 0.98; // slow down factor
    static int ROT_LIMIT = 70; // smooths rotation at small scale, see use
    static int ROT_DEL = 7; // smooths rotation at small scale, see use

    static double PLAYER_BULLET_SPEED = 420/FPS; // bullet speed in px/s/fps
    static double PLAYER_BULLET_TTL = durToTtl(seconds(0.7)); // bullet time of living
    static double PLAYER_BULLET_RANGE = PLAYER_BULLET_SPEED*PLAYER_BULLET_TTL;
    static double PLAYER_BULLET_OFFSET = 10; // px
    static double PLAYER_ENERGY_INITIAL = 5000;
    static double PLAYER_E_BUILDUP = 1; // energy/frame
    static double PLAYER_HIT_RADIUS = 13; // energy/frame
    static Duration PLAYER_GUN_RELOAD_TIME = millis(100); // default ability
    static AbilityKind PLAYER_ABILITY_INITIAL = SHIELD; // rocket fire-fire time period
    static double PLAYER_GRAPHICS_ANGLE_OFFSET = PI/4;
    static double ROCKET_GUN_TURRET_ANGLE_GAP = 2*PI/180;

    static double ROCKET_ENGINE_THRUST = 0.16; // px/s/frame
    static double ROCKET_ENGINE_DEBRIS_TTL = durToTtl(millis(20));
    static double PULSE_ENGINE_PULSEPERIOD_TTL = durToTtl(millis(20));
    static double PULSE_ENGINE_PULSE_TTL = durToTtl(millis(400));
    static double PULSE_ENGINE_PULSE_TTL1 = 1/PULSE_ENGINE_PULSE_TTL; // saves us computation

    // kinetic shield is very effective and can disrupt game balance, thus
    // it should have very low energy accumulation to prevent overuse
    // 1) it should NOT make player prefer not move
    // 2) it should be able to handle multiple simultaneous hits (e.g. most of entire big asteroid explosion spawn)
    // 3) player should NEVER want to rely on it intentionally
    // 4) it should only prevent player to die accidental death
    static double KINETIC_SHIELD_INITIAL_ENERGY = 0.5; // 0-1 coeficient
    static Duration KINETIC_SHIELD_RECHARGE_TIME = minutes(4);
    static double ROCKET_KINETIC_SHIELD_RADIUS = 25; // px
    static double ROCKET_KINETIC_SHIELD_ENERGYMAX = 5000; // energy
    static double KINETIC_SHIELD_LARGE_E_RATE = 50; // 50 times
    static double KINETIC_SHIELD_LARGE_RADIUS_INC = 10; // by 10 px
    static double KINETIC_SHIELD_LARGE_E_MAX_INC = 1; // by 100%
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
    static double UFO_RADAR_RADIUS = 75;
    static double UFO_DISC_RADIUS = 3;
    static double UFO_DISC_HIT_RADIUS = 9;
    static double UFO_EXPLOSION_RADIUS = 100;
    static double UFO_DISC_EXPLOSION_RADIUS = 15;
    static double UFO_TTL() { return durToTtl(seconds(randMN(30,80))); }
    static double UFO_SQUAD_TTL() { return durToTtl(seconds(randMN(200,500))); }
    static double UFO_DISCSPAWN_TTL() { return durToTtl(seconds(randMN(60,180))); }
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
    static double BLACKHOLE_PARTICLES_MAX = 5000;

    @IsConfig
    final V<Color> ccolor = new V<>(Color.BLACK, c -> game.mission.color_canvasFade = new Color(c.getRed(), c.getGreen(), c.getBlue(), game.mission.color_canvasFade.getOpacity()));
    @IsConfig(min=0,max=0.1)
    final V<Double> copac = new V<>(0.05, c -> game.mission.color_canvasFade = new Color(game.mission.color_canvasFade.getRed(), game.mission.color_canvasFade.getGreen(), game.mission.color_canvasFade.getBlue(), c));
//    @IsConfig final V<Effect> b1 = new V<>(new Glow(0.3), e -> gc_bgr.getCanvas().setEffect(e));
    @IsConfig final V<PlayerSpawners> spawning = new V<>(PlayerSpawners.CIRCLE);
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
    @IsConfig final V<AbilityKind> p1ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p2ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p3ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p4ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p5ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p6ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p7ability_type = new V<>(PLAYER_ABILITY_INITIAL);
    @IsConfig final V<AbilityKind> p8ability_type = new V<>(PLAYER_ABILITY_INITIAL);
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
        new Player(1, p1name, p1color, p1fire, p1thrust, p1left, p1right, p1ability, p1ability_type),
        new Player(2, p2name, p2color, p2fire, p2thrust, p2left, p2right, p2ability, p2ability_type),
        new Player(3, p3name, p3color, p3fire, p3thrust, p3left, p3right, p3ability, p3ability_type),
        new Player(4, p4name, p4color, p4fire, p4thrust, p4left, p4right, p4ability, p4ability_type),
        new Player(5, p5name, p5color, p5fire, p5thrust, p5left, p5right, p5ability, p5ability_type),
        new Player(6, p6name, p6color, p6fire, p6thrust, p6left, p6right, p6ability, p6ability_type),
        new Player(7, p7name, p7color, p7fire, p7thrust, p7left, p7right, p7ability, p7ability_type),
        new Player(8, p8name, p8color, p8fire, p8thrust, p8left, p8right, p8ability, p8ability_type)
    );

    static {
        try {
            UI_FONT = Font.loadFont(Comet.class.getResource("Tele-Marines.TTF").openStream(), 12.0);
            if(UI_FONT==null) UI_FONT = Font.getDefault();
        } catch (IOException e) {
            log(Comet.class).error("Couldnt load font",e);
        }
    }

    private static final Color HUD_COLOR = Color.AQUA;
    private static final double HUD_OPACITY = 0.25;
    private static final double HUD_DOT_GAP = 3;
    private static final double HUD_DOT_DIAMETER = 1;
    void drawHudLine(double x, double y, double lengthstart, double length, double cosdir, double sindir, Color color) {
        gc.setFill(color);
        gc.setGlobalAlpha(HUD_OPACITY);

        for(double i=lengthstart; i<length; i+=HUD_DOT_GAP)
            gc.fillOval(modX(x+i*cosdir), modY(y+i*sindir), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);

        gc.setGlobalAlpha(1);
    }
    void drawHudCircle(double x, double y, double r, double angle, double angleWidth, Color color) {
        gc.setFill(color);
        gc.setGlobalAlpha(HUD_OPACITY);

        double length = angleWidth*r;
        int pieces = (int)(length/HUD_DOT_GAP);
        double angleStart = angle-angleWidth/2;
        double angleBy = angleWidth/pieces;
        for(int p=0; p<pieces; p++) {
            double a = angleStart+p*angleBy;
            gc.fillOval(modX(x+r*cos(a)), modY(y+r*sin(a)), HUD_DOT_DIAMETER,HUD_DOT_DIAMETER);
        }

        gc.setGlobalAlpha(1);

    }
    void drawHudCircle(double x, double y, double r, Color color) {
        drawHudCircle(x, y, r, 0, 2*PI, color);

//        gc.setGlobalAlpha(0.3);
//        gc.setStroke(Color.AQUA);
//        gc.setLineWidth(1);
//        gc.strokeOval(x-r,y-r,r*2,r*2);
//        gc.setStroke(null);
//        gc.setGlobalAlpha(1);
    }




    /** Encompasses entire game. */
    private class Game {
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
        long loopid = 0;   // game loop id, starts at 0, incremented by 1
        final UfoFaction ufos = new UfoFaction();
        final PlayerFaction humans = new PlayerFaction();
        final TTLList runNext = new TTLList();
        final Set<PO> removables = new HashSet<>();

        int mission_counter = 0;   // mission counter, starts at 1, increments by 1
        Mission mission = null; // current mission, (they repeat), starts at 1, = mission % missions +1
        boolean isMissionScheduled = false;
        final MapSet<Integer,Mission> missions = new MapSet<>(m -> m.id,
            new Mission(1,null,Color.LIGHTGREEN, Color.rgb(0, 51, 51, 0.1),null, Inkoid::new), //new Glow(0.3)
            new Mission(2,null,Color.YELLOW, Color.rgb(0, 8, 0, 0.08), null, Fermi::new),
            new Mission(3,null, Color.DODGERBLUE,Color.rgb(10,10,25,0.08), null,Energ::new),
            new Mission(4,null,Color.GREEN, Color.rgb(0, 15, 0, 0.08), null, Fermi::new),
            new Mission(5,null,Color.DODGERBLUE, Color.rgb(0, 0, 15, 0.08), null, Genoid::new),
            new Mission(6,bgr(Color.WHITE), Color.DODGERBLUE,new Color(1,1,1,0.02),new ColorAdjust(0,-0.6,-0.7,0),Energ::new),
            new Mission(7,null,Color.RED,new Color(1,1,1,0.08),new ColorAdjust(0,-0.6,-0.7,0),Energ2::new)
        );

        // we need specific instance
        final Ƒ0<RocketEnhancer> SHUTTLE_CALL_ENHANCER = () -> new RocketEnhancer("Shuttle support", FontAwesomeIcon.SPACE_SHUTTLE, seconds(5),
            r -> humans.sendShuttle(r), r -> {},
            "- Calls in supply shuttle",
            "- provides large and powerful stationary kinetic shield",
            "- provides upgrade supply"
        );
        final Set<Ƒ0<RocketEnhancer>> ROCKET_ENHANCERS = set(

            // fire upgrades
            () -> new RocketEnhancer("Gun", MaterialDesignIcon.KEY_PLUS, seconds(5),
                r -> r.gun.turrets.inc(), r -> {/**r.gun.turrets.dec()*/},
                "- Mounts additional gun turret",
                "- Increases chance of hitting the target",
                "- Increases maximum possible target damage by 100%"
            ),
            () -> new RocketEnhancer("Rapid fire", MaterialDesignIcon.BLACKBERRY, seconds(12), r -> r.rapidfire.inc(), r -> r.rapidfire.dec(),
                " - Largely increases rate of fire temporarily. Fires constant stream of bullets",
                " - Improved hit efficiency due to bullet spam",
                " - Improved mobility due to less danger of being hit",
                "Tip: Hold the fire button. Be on the move. Let the decimating power of countless bullets"
              + " be your shield. The upgrade lasts only a while, but being static is a disadvatage."
            ),
            () -> new RocketEnhancer("Long fire", MaterialDesignIcon.DOTS_HORIZONTAL, seconds(25), r -> r.powerfire.inc(), r -> r.powerfire.dec(),
                "- Increases bullet speed",
                "- Increases bullet range",
                "Tip: Aim closer to target. Faster bullet will reach target sooner."
            ),
            () -> new RocketEnhancer("High energy fire", MaterialDesignIcon.MINUS, seconds(25), r -> r.energyfire.inc(), r -> r.energyfire.dec(),
                "- Bullets penetrate the target",
                "- Increases bullet damage, 1 hit kill",
                "- Multiple target damage",
                "Tip: Try lining up targets into a line or move to space with more higher density. "
            ),
            () -> new RocketEnhancer("Split ammo", MaterialIcon.CALL_SPLIT, seconds(15), r -> r.splitfire.inc(), r -> r.splitfire.dec(),
                "- Bullets split into 2 bullets on hit",
                "- Multiple target damage",
                "Tip: Strategic weapon. The damage potential raises exponentially"
              + " depending on the number of targets. Annihilate the most dense enemy area with easy. "
            ),
            () -> new RocketEnhancer("Black hole cannon", MaterialDesignIcon.CAMERA_IRIS, seconds(5), r -> r.gun.blackhole.inc(),
                "- Fires a bullet generating a black hole",
                "- Lethal to everything, including players",
                "- Player receives partial score for all damage caused by the black hole",
                "Tip: Strategic weapon. Do not endanger yourself or your allies."
            ),
            () -> new RocketEnhancer("Aim enhancer", MaterialDesignIcon.RULER, seconds(35),
                r -> {
                    Ship.LaserSight ls = r.new LaserSight();
                    game.runNext.add(seconds(35),ls::dispose);
                },
                "- Displays bullet path",
                "- Displays bullet range"
            ),

            () -> new RocketEnhancer("Mobility", MaterialDesignIcon.TRANSFER, seconds(25), r -> r.engine.mobility.inc(), r -> r.engine.mobility.dec(),
                "- Increases propulsion efficiency, i.e., speed",
                "- Increases meneuverability",
                "Tip: If there is ever time to move, it is now. Dont idle around."
            ),
//            () -> new RocketEnhancer("Proximity alert", MaterialDesignIcon.ALERT_OCTAGON, seconds(30), r -> {}, r -> {}),
            () -> new RocketEnhancer("Intel", MaterialDesignIcon.EYE, minutes(2), r ->  humans.intelOn.inc(), r -> humans.intelOn.dec(),
                "- Reports incoming ufo time & location",
                "- Reports incoming upgrade time & location",
                "- Reports exact upgrade type",
                "- Displays bullet range",
                "Tip: This upgrade is shared."
            ),
            () -> new RocketEnhancer("Share upgrades", MaterialDesignIcon.SHARE_VARIANT, minutes(2),
                r -> humans.share_enhancers=true, r -> humans.share_enhancers=false,
                "- Applies upgrades to all allies",
                "Tip: The more allies, the bigger the gain. Beware of strategic weapons in a team - "
              + "they can do more harm than good."
            ),
            SHUTTLE_CALL_ENHANCER,
//            () -> new RocketEnhancer("U.F.O. technology", MaterialDesignIcon.RADIOACTIVE, seconds(5), r -> r.energy_buildup_rate *= 1.1
//            ),

            // kinetic shield upgrades
            () -> new RocketEnhancer("Shield energizer", MaterialDesignIcon.IMAGE_FILTER_TILT_SHIFT, seconds(5),
                r -> {
                    r.kinetic_shield.KSenergy_max *= 1.1;
                    r.kinetic_shield.changeKSenergyToMax();
                },
                "- Sets kinetic shield energy to max",
                "- Increases maximum kinetic shield energy by 10%"
            ),
            () -> new RocketEnhancer("Super shield", FontAwesomeIcon.SUN_ALT, seconds(25), r -> r.kinetic_shield.large.inc(),r -> r.kinetic_shield.large.dec(),
                "- Increases kinetic shield range by " + KINETIC_SHIELD_LARGE_RADIUS_INC + "px",
                "- Increases maximum kinetic shield energy by" + (KINETIC_SHIELD_LARGE_E_MAX_INC*100)+"%",
                "- Increases kinetic shield energy accumulation " + (KINETIC_SHIELD_LARGE_E_RATE)+" times",
                "Tip: You are not invincible, but anyone will think twice about colliding with you. Go on the offensive. Move."
            ),

            // energy upgrades
            () -> new RocketEnhancer("Charger", MaterialDesignIcon.BATTERY_CHARGING_100, seconds(5), r -> r.energy_buildup_rate *= 1.1,
                "- Increases energy accumulation by 10%"
            ),
            () -> new RocketEnhancer("Energizer", MaterialDesignIcon.BATTERY_POSITIVE, seconds(5), r -> r.energy_max *= 1.1,
                "- Increases maximum energy by 10%"
            ),
            () -> new RocketEnhancer("Baterry (small)", MaterialDesignIcon.BATTERY_30, seconds(5),
                r -> r.energy = min(r.energy+2000,r.energy_max),
                "- Increases energy by up to 2000"
            ),
            () -> new RocketEnhancer("Baterry (medium)", MaterialDesignIcon.BATTERY_60, seconds(5),
                r -> r.energy = min(r.energy+5000,r.energy_max),
                "- Increases energy by up to 5000"
            ),
            () -> new RocketEnhancer("Baterry (large)", MaterialDesignIcon.BATTERY, seconds(5),
                r -> r.energy = min(r.energy+10000,r.energy_max),
                "- Increases energy by up to 10000"
            )
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
            isMissionScheduled = false;
            ufos.init();
            humans.init();
            loop.start();

            timer200ms.start();
            playfield.requestFocus();
            nextMission();
        }

        void doLoop() {
            // debug
             if(loopid%60==0) System.out.println("particle count: " + oss.get(Particle.class).size());

            // loop prep
            loopid++;
            long now = System.currentTimeMillis();
            boolean isThird = loopid%3==0;

            players.stream().filter(p -> p.alive).forEach(p -> {
                if(pressedKeys.contains(p.keyLeft.get()))  p.rocket.dir -= p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyLeft.get(),0l));
                if(pressedKeys.contains(p.keyRight.get())) p.rocket.dir += p.computeRotSpeed(now-keyPressTimes.getOrDefault(p.keyRight.get(),0l));
                if(isThird && p.rocket.rapidfire.is() && pressedKeys.contains(p.keyFire.get()))  p.rocket.gun.fire();
            });

            runNext.run();

            // remove inactive objects
            for(PO o : os) if(o.dead) removables.add(o);
            os.removeIf(o -> o.dead);
            for(PO o : oss.get(Particle.class)) if(o.dead) removables.add(o);
            oss.forEachSet(set -> set.removeIf(o -> o.dead));
            removables.forEach(PO::dispose);
            removables.clear();

            entities.addPending();
            entities.removePending();

            // apply forces
            forEachCartesian(entities.forceFields, os, ForceField::apply);

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
            entities.forceFields.stream().filter(ff -> ff instanceof LO).forEach(ff -> ((LO)ff).doLoop());
            os.forEach(PO::doLoop);

            // guns & firing
            stream(oss.get(Rocket.class).stream(),oss.get(Ufo.class).stream()).forEach(ship -> {
                if(ship.gun!=null) {
                    ship.gun.fireTTL--;
                }
                if(ship.gun!=null && ship.gun.control==AUTO && ship.gun.fireTTL<0) {
                    ship.gun.fireTTL = durToTtl(ship.gun.time_reload);
                    runNext.add(() -> ship.gun.ammo_type.apply(ship.gun.aimer.apply()));
                }
            });

            // collisions
            forEachCartesian(oss.get(Bullet.class),filter(os,e -> !(e instanceof Bullet)), Bullet::checkCollision);

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
                    u.dead = true;
                    ufos.onUfoDestroyed(u);
                }
            });
            oss.forEach(Rocket.class,UfoDisc.class, (r,ud) -> {
                if(!r.isin_hyperspace && r.isHitDistance(ud)) {
                    if(r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                        r.dx = r.dy = 0;
                    } else {
                        r.player.die();
                    }
                    ud.dead = true;
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
            entities.clear();
            runNext.clear();
            playfield.getChildren().clear();
        }

        void nextMission() {
            if(isMissionScheduled) return;
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
                if(oss.get(Asteroid.class).isEmpty()) nextMission();
            });
        }
        void over() {
            runNext.add(seconds(5),this::stop);
        }
        void message(String s) {
            message.setText(s);
            message.setFont(new Font(message.getFont().getName(), 50));
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
            boolean share_enhancers = false;

            void init() {
                intelOn.reset();
                runNext.addPeriodic(() -> SATELLITE_TTL()/sqrt(players.size()), humans::sendSatellite);
            }

            void sendShuttle(Rocket r) {
                game.runNext.add(seconds(2),() -> pulseCall(r));
                game.runNext.add(seconds(4),() -> pulseCall(r));
                game.runNext.add(seconds(6),() -> new Shuttle(r));
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
                runNext.add(seconds(1.8), () -> new Satellite(s).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,4,color,false);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-4,color,false);
            }
        }
        class UfoFaction {
            int losses = 0;
            int losses_aggressive = 5;
            int losses_cannon = 10;
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
                repeat(count, () -> runNext.add(seconds(rand0N(0.5)),() -> sendUfo(side)));
            }
            private void sendUfo(Side side) {
                Side s = side==null ? randEnum(Side.class) : side;
                double offset = 50;
                double x = s==LEFT ? offset : playfield.getWidth()-offset;
                double y = rand0N(playfield.getHeight());
                if(humans.intelOn.is()) pulseAlert(x,y);
                runNext.add(seconds(1.2), () -> new Ufo(s,aggressive).y = y );
            }

            void pulseCall(PO o) { pulseCall(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y) { pulseCall(x,y,0,0); }
            void pulseAlert(double x, double y) { pulseAlert(x,y,0,0); }
            void pulseAlert(PO o) { pulseAlert(o.x,o.y,o.dx,o.dy); }
            void pulseCall(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,4,color,true);
            }
            void pulseAlert(double x, double y, double dx, double dy) {
                new RadioWavePulse(x,y,dx,dy,-4,color,true);
            }
        }
        class Mission {
            final int id;
            final Background bgr;
            final Ƒ5<Double,Double,Double,Double,Double,Asteroid> planetoidConstructor;
            final Color color;
            Color color_canvasFade; // normally null, canvas fade effect
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

                double size = sqrt(playfield.getWidth()*playfield.getHeight())/1000;
                int planetoids = 3 + (int)(2*(size-1)) + (mission_counter-1) + players.size()/2;
                double delay = durToTtl(seconds(mission_counter==1 ? 2 : 5));
                runNext.add(delay/2, () -> message("Level " + mission_counter));
                runNext.add(delay, () -> repeat(planetoids, i -> spawnPlanetoid()));
                runNext.add(delay, () -> isMissionScheduled = false);
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
            };
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
        final V<Double> energyKS = new V<>(0d);
        final V<AbilityKind> ability_type;
        Rocket rocket;

        public Player(int ID, V<String> NAME, V<Color> COLOR, V<KeyCode> kfire, V<KeyCode> kthrust, V<KeyCode> kleft, V<KeyCode>kright, V<KeyCode> kability, V<AbilityKind> ability_) {
            id = ID;
            name = NAME;
            color = COLOR;
            ability_type = ability_;
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
            every200ms.add(() -> { if(rocket!=null) energy.set(rocket.energy); });
            every200ms.add(() -> { if(rocket!=null) energyKS.set(rocket.kinetic_shield.KSenergy); });
        }

        void die() {
            if(!alive) return; // bugfix
            alive = false;
            rocket.dead = true;
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
            return rocket.engine.mobility.value()*r;
        }


    }
    private static enum PlayerSpawners {
        CIRCLE, LINE, RECTANGLE;

        double computeStartingAngle(int ps, int p) {
            switch(this) {
                case CIRCLE : return ps==0 ? 0 : p*2*PI/ps;
                case LINE :
                case RECTANGLE : return -PI/2;
            }
            throw new SwitchException(this);
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
            throw new SwitchException(this);
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
            throw new SwitchException(this);
        }
    }

    /** Loop object - object with per loop behavior. Executes once per loop. */
    private static interface LO {
        void doLoop();
        default void dispose() {};
    }
    private abstract class SO implements LO {
        double x = 0;
        double y = 0;
        double dx = 0;
        double dy = 0;
        boolean isin_hyperspace = false;
        boolean dead = false;
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
            if(x<0) x = playfield.getWidth();
            else if(x>playfield.getWidth()) x = 0;
            if(y<0) y = playfield.getHeight();
            else if(y>playfield.getHeight()) y = 0;
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
    }
    /** Object with physical properties. */
    abstract class PO extends SO {
        double mass = 0;
        Engine engine = null;
        Class type;
        Node graphics;
        double blackhole_nearby = 0;
        Set<LO> children = null;

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

        double kineticE() {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
            return mass;// * (dx*dy);
        }

        double kineticEto(PO o) {
//            return 0.5 * mass * (dx*dx+dy*dy); // 0.5mv^2
            return mass;// * (dx*dy);
        }

    }
    /** Object with engine, gun and other space ship characteristics. */
    abstract class Ship extends PO {
        double dir = -PI/2; // up
        double cosdir = 0; // cos(dir), to avoid multiple calculations/loop
        double sindir = 0; // sin(dir), to avoid multiple calculations/loop
        double energy = 0;
        double energy_buildup_rate;
        double energy_max = 10000;
        Gun gun = null;
        Ability ability_main;
        KineticShield kinetic_shield = null;
        double dx_old = 0; // allows us calculate ddx (2nd derivation - acceleration)
        double dy_old = 0;

        public Ship(Class TYPE, double X, double Y, double DX, double DY, double HIT_RADIUS, Node GRAPHICS, double E, double dE) {
            super(TYPE, X, Y, DX, DY, HIT_RADIUS, GRAPHICS);
            energy = E;
            energy_buildup_rate = dE;
            children = new HashSet<>(10);
        }

        void doLoopBegin() {
            cosdir = cos(dir);
            sindir = sin(dir);
            energy = min(energy+energy_buildup_rate,energy_max);
            dx_old = dx;
            dy_old = dy;
            if(engine!=null) engine.doLoop();
        }

        class Engine {
            boolean enabled = false;
            final InEffectValue<Double> mobility = new InEffectValue<>(times -> pow(BONUS_MOBILITY_MULTIPLIER,times));

            final void on() {
                if(!enabled) {
                    enabled = true;
                    onOn();
                }
            };
            final void off() {
                if(enabled) {
                    enabled = false;
                    onOff();
                }
            }
            void onOn(){};
            void onOff(){};
            final void doLoop(){
                if(enabled) {
                    onDoLoop();
                }
            };
            void onDoLoop(){};
        }
        class RocketEngine extends Engine {
            double ttl = 0;
            double thrust = ROCKET_ENGINE_THRUST;
            final double particle_speed = 1/1/FPS;

            void onDoLoop() {
                dx += cos(dir)*mobility.value()*thrust;
                dy += sin(dir)*mobility.value()*thrust;

                if(!isin_hyperspace) {
                    ttl--;
                    if(ttl<0) {
                        ttl = ROCKET_ENGINE_DEBRIS_TTL;
                        ROCKET_ENGINE_DEBRIS_EMITTER.emit(x,y,dir+PI, mobility.value());
                    }
                }
            }
        }
        class PulseEngine extends Engine {
            private double pulseTTL = 0;
            private double shipDistance = 9;

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
                    if(ttl<0) dead = true;

                    if(!debris_done && ttl<0.7) {
                        double m = mobility.value();
                        debris_done = true;
                        if(!isin_hyperspace) {
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
                    if(!(o instanceof Rocket)) return;  // too much performance costs for no benefits
                    if(isin_hyperspace!=o.isin_hyperspace) return;

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
        class Gun {
            final GunControl control;
            final Ƒ0<Double> aimer; // determines bullet direction
            final Ƒ1<Double,Bullet> ammo_type; // bullet factory
            final InEffectValue<Double[]> turrets = new InEffectValue<>(1, count -> calculateGunTurretAngles(count));
            final Duration time_reload;
            final InEffect blackhole = new InEffect();
            double fireTTL; // frames till next fire

            public Gun(GunControl CONTROL, Duration TIME_RELOAD, Ƒ0<Double> AIMER, Ƒ1<Double,Bullet> AMMO_TYPE) {
                control = CONTROL;
                time_reload = TIME_RELOAD;
                aimer = AIMER;
                ammo_type = AMMO_TYPE;
                fireTTL = durToTtl(time_reload);
            }

            void fire() {
                if(!isin_hyperspace) {
                    // for each turret, fire
                    game.runNext.add(() -> {
                        if(blackhole.is()) {
                            blackhole.dec();
                            Bullet b = ammo_type.apply(aimer.apply());
                                   b.isBlackHole = true;
                        } else {
                            for(Double fire_angle : turrets.value()) {
                                Bullet b = ammo_type.apply(aimer.apply()+fire_angle);
                                       b.isHighEnergy = Ship.this instanceof Rocket && ((Rocket)Ship.this).energyfire.is();
                            }
                        }
                    });
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

            public AbilityWithSceneGraphics(boolean ONHOLD, Duration timeToAct, Duration timeToPass, double E_ACT, double E_RATE, Node GRAPHICS) {
                super(ONHOLD, timeToAct, timeToPass, E_ACT, E_RATE);
                graphicsA = GRAPHICS;
            }

            public void dispose() {
                super.dispose();
                playfield.getChildren().remove(graphicsA);
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
                void apply(PO o) {
                    if(o==Ship.this) return; // must not affect itself
                    if(!isin_hyperspace) return;

                    double distx = distXSigned(x,o.x);
                    double disty = distYSigned(y,o.y);
                    double dist = dist(distx,disty)+1; // +1 avoids /0 " + dist);
                    double f = force(o.mass,dist);

                    // apply force
                    o.dx += distx*f/dist;
                    o.dy += disty*f/dist;
                }
                public void doLoop() {
                    this.x = Ship.this.x;
                    this.y = Ship.this.y;
                    this.isin_hyperspace = Ship.this.isin_hyperspace; // must always match
                }
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
            double radius_shield = SHIELD_RADIUS;

            Shield() {
                super(
                    true, SHIELD_ACTIVATION_TIME,SHIELD_PASSIVATION_TIME,SHIELD_E_ACTIVATION,SHIELD_E_RATE,
                    new Icon(MaterialDesignIcon.HEXAGON_OUTLINE,50)
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

                if(game.humans.intelOn.is()) {
                    drawHudCircle(x,y,radius_shield,HUD_COLOR);
                }
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
            final LO ksemitter = new ShieldPulseEmitter();
            final InEffect large = new InEffect(times -> {
                KSenergy_rate = (times>0 ? KINETIC_SHIELD_LARGE_E_RATE : 1)*KSenergy_rateInit;
                KSradius = KSradiusInit + KINETIC_SHIELD_LARGE_RADIUS_INC*times;
                KSenergy_max = KSenergy_maxInit*(1 + KINETIC_SHIELD_LARGE_E_MAX_INC*times);
                postRadiusChange();
            });
            double largeTTL = 1;
            double largeTTLd;
            double largeLastPiece = 0;

            int syncs_radius = 60;
            int syncs_amount = 100;
            double syncs_angle = 2*PI/syncs_amount;
            double[] syncs = new double[syncs_amount];
            int sync_index = 0;

            public KineticShield(double RADIUS, double ENERGY_MAX) {
                super(true, Duration.ZERO,Duration.ZERO,0,0);
                KSenergy_maxInit = ENERGY_MAX;
                KSenergy_max = KSenergy_maxInit;
                KSenergy_rateInit = KSenergy_max/durToTtl(KINETIC_SHIELD_RECHARGE_TIME);
                KSenergy_rate = KSenergy_rateInit;
                KSenergy = KINETIC_SHIELD_INITIAL_ENERGY*KSenergy_max;
                KSradiusInit = RADIUS;
                KSradius = KSradiusInit;
                postRadiusChange();
                children.add(this);
                scheduleActivation();

                double syncs_range = (1+randInt(5))*2*PI;
                double syncs_range_d = syncs_range/syncs_amount;
                for(int i=0; i<syncs_amount; i++)
                    syncs[i] = 2+15+15*sin(i*syncs_range_d);
            }

            @Override
            void init() {}    // no init
            public void dispose() {} // no dispose
            public void doLoop() {
                KSenergy = min(KSenergy_max,KSenergy+KSenergy_rate);
                ksemitter.doLoop();

                if(large.is()) {
                    largeTTL -= 0.3;
                    if(largeTTL<0) {
                        largeTTL = 1;
                        largeLastPiece = largeLastPiece%pieces;
                        game.runNext.add(() -> new KineticShieldPiece(dir+largeLastPiece*piece_angle).max_opacity = 0.4);
                        largeLastPiece++;
                    }
                }

                if(game.humans.intelOn.is()) {
                    drawHudCircle(x,y,KSradius,HUD_COLOR);
                }

                sync_index++;
                for(int i=0; i<syncs_amount; i++) {
                    double angle = dir+i*syncs_angle;
                    double acos = cos(angle);
                    double asin = sin(angle);
                    double alen = syncs[Math.floorMod(i+sync_index/2,syncs_amount)];
                    gc.setStroke(COLOR_DB);
                    gc.setLineWidth(1);
                    gc.strokeLine(x+syncs_radius*acos,y+syncs_radius*asin,x+(syncs_radius-alen)*acos,y+(syncs_radius-alen)*asin);
                }

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
                    game.runNext.remove(activationRun);
                    game.runNext.add((KSenergy_max-KSenergy)/KSenergy_rate, activationRun);
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
            private void postRadiusChange() {
                pieces = ((int)(2*PI*KSradius))/11;
                piece_angle = 2*PI/pieces;
                largeTTLd = 1/FPS/seconds(1/60/pieces).toSeconds();
            }


            /** Not an ability, simply a graphics for an ability. Extends Ability to avoid code duplicity. */
            class KineticShieldPiece extends Ability {
                final double dirOffset;
                double delay_ttl;
                double ttl = 1;
                double ttld = 1/durToTtl(seconds(1));
                double max_opacity = 1;

                KineticShieldPiece(double DIR) {
                    this(false,DIR);
                }
                KineticShieldPiece(boolean delayed, double DIR) {
                    super(true, Duration.ZERO,Duration.ZERO,0,0);
                    delay_ttl = durToTtl(seconds(delayed ? 0.2 : 1));
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
                    drawRotatedImage(gc, KINETIC_SHIELD_PIECE_GRAPHICS, deg(PI/2+KSPdir), KSPx+x-KINETIC_SHIELD_PIECE_GRAPHICS.getWidth()/2, KSPy+y-KINETIC_SHIELD_PIECE_GRAPHICS.getHeight()/2);
                    gc.setGlobalAlpha(1);
//                    gc.setGlobalBlendMode(SRC_OVER);

                    delay_ttl--;
                    if(delay_ttl<0) {
                        ttl -= ttld;
                        if(ttl<0) game.runNext.add(this::dispose);
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
                drawHudCircle(x,y,bullet_range,r.dir,PI/6, HUD_COLOR);
                drawHudCircle(x,y,bullet_range,r.dir+2*PI/3,PI/8, HUD_COLOR);
                drawHudCircle(x,y,bullet_range,r.dir-2*PI/3,PI/8, HUD_COLOR);
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
                for(int i=20; i<500; i+=5)
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

        Rocket(Player PLAYER) {
            super(
                Rocket.class,
                playfield.getWidth()/2,playfield.getHeight()/2,0,0,PLAYER_HIT_RADIUS,
                new Icon(null,40),
                PLAYER_ENERGY_INITIAL,PLAYER_E_BUILDUP
            );
            player = PLAYER;
            ((Icon)graphics).setFill(player.color.getValue());
            ((Icon)graphics).styleclass("comet-rocket");
            kinetic_shield = new KineticShield(ROCKET_KINETIC_SHIELD_RADIUS,ROCKET_KINETIC_SHIELD_ENERGYMAX);
            changeAbility(player.ability_type.get());
            engine = random()<0.5 ? new RocketEngine() : new PulseEngine();
            ((Icon)graphics).icon(engine instanceof RocketEngine ? MaterialDesignIcon.ROCKET : FontAwesomeIcon.ROCKET);
            ((Icon)graphics).size(engine instanceof RocketEngine ? 40 : 34);

            gun = new Gun(
                MANUAL,
                PLAYER_GUN_RELOAD_TIME,
                () -> dir,
                dir -> splitfire.is()
                    ? new SplitBullet(
                            this,
                            x + PLAYER_BULLET_OFFSET*cos(dir),
                            y + PLAYER_BULLET_OFFSET*sin(dir),
                            dx + energyfire.value*powerfire.value()*cos(dir)*PLAYER_BULLET_SPEED,
                            dy + energyfire.value*powerfire.value()*sin(dir)*PLAYER_BULLET_SPEED,
                            0,
                            PLAYER_BULLET_TTL,HUD_COLOR
                        )
                    : new Bullet(
                            this,
                            x + PLAYER_BULLET_OFFSET*cos(dir),
                            y + PLAYER_BULLET_OFFSET*sin(dir),
                            dx + energyfire.value*powerfire.value()*cos(dir)*PLAYER_BULLET_SPEED,
                            dy + energyfire.value*powerfire.value()*sin(dir)*PLAYER_BULLET_SPEED,
                            0,
                            PLAYER_BULLET_TTL,HUD_COLOR
                        )
            );
        }

        void draw() {
            super.draw();
            graphics.setRotate(deg(PLAYER_GRAPHICS_ANGLE_OFFSET + dir));

            if(game.humans.intelOn.is()) {
                double bullet_range = calculateBulletRange();
                drawHudCircle(x,y,bullet_range,HUD_COLOR);
            }

            if(gun.blackhole.is()) {
                double bullet_range = calculateBulletRange();
                gc.setFill(Color.BLACK);
                drawHudCircle(modX(x+bullet_range*cos(dir)),modY(y+bullet_range*sin(dir)), 50, HUD_COLOR);
            }

            if(game.pressedKeys.contains(player.keyLeft.get())) {
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir),y+10*sin(dir),dir+PI/2,engine.mobility.value());
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir+PI),y+10*sin(dir+PI),dir-PI/2,engine.mobility.value());
            }
            if(game.pressedKeys.contains(player.keyRight.get())) {
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir),y+10*sin(dir),dir-PI/2,engine.mobility.value());
                ROCKET_ENGINE_DEBRIS_EMITTER.emit(x+10*cos(dir+PI),y+10*sin(dir+PI),dir+PI/2,engine.mobility.value());
            }

            // rocket-rocket 'quark entanglement' formation force
            // Repells at short distance, pulls at long distance.
            //
            // Nice idea, but the force requires some tuning. Its needs to be fairly strong to shape
            // the formation properly and player movement can be disrupted or itself disruptive.
             double mid = 150;
             game.oss.get(Rocket.class).forEach(r -> {
                 if(this==r) return;
                 double d = dir(r);
                 double cd = cos(d);
                 double sd = sin(d);
                 double dist = distance(r);
                 double f = dist<mid ? -8*pow((1-dist/mid),2) : 6*pow((dist-mid)/2000,2);
                 dx += f*cd;
                 dy += f*sd;
                 r.dx -= f*cd;
                 r.dy -= f*sd;
                 drawHudLine(x,y,20,dist-2*20,cd,sd,COLOR_DB);
             });
        }
        void changeAbility(AbilityKind type ){
            if(ability_main!=null) ability_main.dispose();
            switch(type) {
                case DISRUPTOR : ability_main = new Disruptor(); break;
                case HYPERSPACE : ability_main =  new Hyperspace(); break;
                case SHIELD : ability_main =  new Shield(); break;
                default: throw new SwitchException(this);
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

        double calculateBulletRange() {
            return energyfire.value()*powerfire.value()*PLAYER_BULLET_RANGE;
        }
    }
    /** Default enemy ship. */
    class Ufo extends Ship {
        long dirChangeTTL = 0;
        boolean aggressive = false;
        Runnable radio = () -> game.ufos.pulseCall(this);
        Runnable discs = () -> {
            if(game.ufos.canSpawnDiscs) {
                game.ufos.canSpawnDiscs = false;
                repeat(3, i -> new UfoDisc(this,i*2*PI/3));
            }
        };

        double discpos = 0; // 0-1, 0=close, 1=far
        double discdspeed = 0;
        double disc_forceJump(double pos) { return pos>=1 ? -2*discdspeed : 0.01; }; // jump force
        double disc_forceBio(double pos) { return pos<0.5 ? 0.01 : -0.01; }; // standard force

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
                        : game.oss.get(Rocket.class).stream().filter(r -> !r.isin_hyperspace).collect(minBy(this::distance)).orElse(null);
                    return enemy==null ? rand0N(2*PI) : dir(enemy) + randMN(-PI/6,PI/6);
                },
                dir -> new UfoBullet(
                    this,
                    x + UFO_BULLET_OFFSET*cos(dir),
                    y + UFO_BULLET_OFFSET*sin(dir),
                    dx + cos(dir)*UFO_BULLET_SPEED,
                    dy + sin(dir)*UFO_BULLET_SPEED,
                    0,
                    UFO_BULLET_TTL,game.ufos.color
                )
            );
            game.runNext.addPeriodic(() -> durToTtl(seconds(5)), radio);
            game.runNext.addPeriodic(() -> durToTtl(seconds(5)), discs);
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
            for(Ufo u : game.oss.get(Ufo.class)) {
                if(u==this) continue;
                double f = interUfoForce(u);
                boolean toright = x<u.x;
                boolean tobottom = y<u.y;
                dx += (toright ? 1 : -1) * f;
                dy += (toright ? 1 : -1) * f;
//                u2.dx += (tobottom ? -1 : 1) * f;
//                u2.dy += (tobottom ? -1 : 1) * f;
            }
        }
        void doLoopOutOfField() {
            if(y<0) y = playfield.getHeight();
            if(y>playfield.getHeight()) y = 0;
            if(x<0 || x>playfield.getWidth()) dead = true;
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
            drawUfoDisc(x+dist*cos(-3*PI/6),y+dist*sin(-3*PI/6));
            drawUfoDisc(x+dist*cos(-7*PI/6),y+dist*sin(-7*PI/6));
            drawUfoDisc(x+dist*cos(-11*PI/6),y+dist*sin(-11*PI/6));

            if(game.humans.intelOn.is())
                drawHudCircle(x,y,UFO_BULLET_RANGE,game.ufos.color);
        }
        @Override
        public void dispose() {
            super.dispose();
            game.runNext.remove(radio);
            game.runNext.remove(discs);
        }
    }
    static int UFO_DISC_DECISION_TIME_TTL = (int) durToTtl(millis(500)); // time it takes to perform logic, such as aiming
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
            for(UfoDisc ud : game.oss.get(UfoDisc.class)) {
                if(ud==this) continue;
                double f = interUfoDiscForce(ud);
                boolean toright = x<ud.x;
                boolean tobottom = y<ud.y;
                dx += (toright ? 1 : -1) * f;
                dy += (toright ? 1 : -1) * f;
            }

            // look for enemy, we dont do this every cycle, 2 reasons:
            // 1) performance
            // 2) the delay allows for a 'drift' effect. For example if player turns behind screen
            //    edge, the ufo disc will still pursue it in correct direction
            if(game.loopid%UFO_DISC_DECISION_TIME_TTL==0) {
                game.oss.get(Rocket.class).stream().filter(r -> !r.isin_hyperspace)
                    .collect(minBy(this::distance))
                    .ifPresent(r -> enemy = r);
            }
            if(enemy==null || enemy.player.rocket != enemy || enemy.isin_hyperspace) {
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
            for(UfoDisc ud : game.oss.get(UfoDisc.class)) {
                if(distance(ud)<=UFO_DISC_EXPLOSION_RADIUS)
                    game.runNext.add(millis(100),ud::explode);
            }
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
        double ttl = durToTtl(seconds(50));
        final double rotationAngle = randOf(-1,1)*deg(2*PI/durToTtl(seconds(20)));

        public Shuttle(Rocket r) {
            super(
                Shuttle.class, r.x+50,r.y-50,0,0,PLAYER_HIT_RADIUS,
                new Icon(FontAwesomeIcon.SPACE_SHUTTLE,40), 0,0
            );
            kinetic_shield = new KineticShield(SHUTTLE_KINETIC_SHIELD_RADIUS,SHUTTLE_KINETIC_SHIELD_ENERGYMAX);
            createHyperSpaceAnim(graphics).playClose();
            game.runNext.add(3*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(4*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(5*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(6*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(7*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(8*ttl/10, () -> { if(!dead) new Satellite(this,rand0N(2*PI)); });
            game.runNext.add(ttl, () -> { if(!dead) createHyperSpaceAnim(graphics).playOpenDo(() -> dead=true); });
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
        final boolean isLarge;

        /** Creates small satellite out of Shuttle or large Satellite. */
        public Satellite(PO s, double DIR) {
            super(Satellite.class,
                s.x,s.y,
                s instanceof Shuttle ? 0.2*cos(DIR) : s.dx,
                s instanceof Shuttle ? 0.2*sin(DIR) : s.dy,
                SATELLITE_RADIUS/2, new Icon(MaterialDesignIcon.SATELLITE_VARIANT,20)
            );
            e = s instanceof Shuttle ? randomOfExcept(game.ROCKET_ENHANCERS,game.SHUTTLE_CALL_ENHANCER).apply() : ((Satellite)s).e;
            children = new HashSet<>(2);
            if(game.humans.intelOn.is()) ((Icon)graphics).icon(e.icon);
            isLarge = false;
        }
        /** Creates large Satellite. */
        public Satellite() {
            this(randEnum(Side.class));
        }
        /** Creates large Satellite. */
        public Satellite(Side dir) {
            super(Satellite.class,
                (dir==LEFT ? 0 : 1)*playfield.getWidth(), random()*playfield.getHeight(),
                (dir==LEFT ? 1 : -1)*SATELLITE_SPEED, 0,
                SATELLITE_RADIUS, new Icon(MaterialDesignIcon.SATELLITE_VARIANT,40)
            );
            children = new HashSet<>(2);
            e = randOf(game.ROCKET_ENHANCERS).apply();
            if(game.humans.intelOn.is()) new REIndicator(this,e);
            isLarge = true;
        }

        void move() {}
        void doLoopOutOfField() {
            if(y<0) y = playfield.getHeight();
            if(y>playfield.getHeight()) y = 0;
            if(isLarge) {
                if(x<0 || x>playfield.getWidth()) dead = true;
            } else {
                if(x<0) x = playfield.getWidth();
                if(x>playfield.getWidth()) x = 0;
            }
        }
        void pickUpBy(Rocket r) {
            e.enhance(r);
            dead = true;
        }
        void explode() {
            if(isLarge) {
                dead = true;
                game.runNext.add(() -> new Satellite(this, -1));
            }
        }
    }

    /** Gun projectile. */
    class Bullet extends PO {
        final Ship owner;
        Color color;
        double ttl = 0;
        double ttl_d = 0;
        boolean isBlackHole = false;
        boolean isHighEnergy = false;

        Bullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL, Color COLOR) {
            super(Bullet.class,x,y,dx,dy,hit_radius,null);
            owner = ship;
            color = COLOR;
            ttl = 1;
            ttl_d = 1/TTL;
        }

        public void doLoop() {
            x += dx;
            y += dy;
            doLoopOutOfField();
            draw();
            ttl -= ttl_d;
            if(ttl<0) {
                dead = true;
                onExpire(null);
            }
        }

        void draw() {
            // the classic point bullet
//            gc_bgr.setFill(color);
//            gc_bgr.fillOval(x-1,y-1,2,2);

            // line bullets
            GraphicsContext g = gc_bgr;
            g.setGlobalAlpha(0.4);
            g.setStroke(color);
            g.setLineWidth(2);
            g.strokeLine(x,y,x+dx,y+dy);
            g.setStroke(null);
            g.setGlobalAlpha(1);
        }

        // cause == null => natural expiration, else hit object
        void onExpire(PO cause) {
            if(isBlackHole && !isin_hyperspace) {
                Player own = owner instanceof Rocket ?((Rocket)owner).player : null;
                game.entities.addForceField(new BlackHole(own, seconds(20),x,y));
            }
        }

        void checkCollision(PO e) {
            if(owner==e) return; // avoid self-hits (bugfix)
            if(isin_hyperspace!=e.isin_hyperspace) return;   // forbid space-hyperspace interaction

            // Fast bullets need interpolating (we check inter-frame collisions)
            // Im still not sure this is the best implementation as far as performance goes and
            // it may require some tuning, but otherwise helps a lot.
            double speedsqr = dx*dx+dy*dy;
            if(speedsqr>100) { // if speed > 5px/frame
                double speed = sqrt(speedsqr);
                int iterations = (int) speed/5;
                for(int i=-(iterations-1); i<=0; i++) {
                    boolean washit = checkWithXY(e, x+dx*i/iterations, y+dy*i/iterations);
                    if(washit) break;
                }
            } else {
                checkWithXY(e,x,y);
            }
        }

        boolean checkWithXY(PO e, double X, double Y) {
            if(dead || e.dead) return true;  // dead objects must not participate

            SO past_state = new SO() {};
               past_state.x = X;
               past_state.y = Y;

            if(e.isHitDistance(past_state)) {
                dead = true; // bullet always dies
                if(e instanceof Rocket) {
                    Rocket r = (Rocket)e;
                    if(!game.deadly_bullets.get() && owner instanceof Rocket) {
                        r.kinetic_shield.new KineticShieldPiece(r.dir(past_state));
                    }
                    if(game.deadly_bullets.get() || !(owner instanceof Rocket)) {
                        if(r.ability_main instanceof Shield && r.ability_main.isActivated()) {
                            r.dx = r.dy = 0;
                            r.engine.off();
                        } else {
                            r.player.die();
                        }
                    }
                } else
                if(e instanceof Asteroid) {
                    Asteroid a = (Asteroid)e;
                    a.onHit(past_state);
                    game.onPlanetoidDestroyed();

                    if(owner instanceof Rocket)
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_ASTEROID(a));

                    new FermiGraphics(e.x, e.y, e.radius*2.5);

//                            gc_bgr.setGlobalAlpha(0.2);
//                            gc_bgr.setFill(mission.color);
//                            drawOval(gc_bgr,b.x,b.y,100);
//                            gc_bgr.setGlobalAlpha(1);
                } else
                if(e instanceof Ufo) {
                    Ufo u = (Ufo)e;
                    if(!(owner instanceof Ufo)) {
                        u.dead = true;
                        game.ufos.onUfoDestroyed(u);
                        drawUfoExplosion(u.x,u.y);
                    }
                    if(owner instanceof Rocket)
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_UFO);
                } else
                if(e instanceof UfoDisc) {
                    UfoDisc ud = (UfoDisc)e;
                    if(owner instanceof Rocket) {
                        ud.explode();
                        ((Rocket)owner).player.score.setValueOf(s -> s + (int)SCORE_UFO_DISC);
                    }
                } else
                if(e instanceof Shuttle) { // we are assuming its kinetic shield is always active (by game design)
                    // ignore bullets when allies | shooting from inside the shield
                    if(owner instanceof Rocket || owner.distance(e)<((Ship)e).kinetic_shield.KSradius) {
                        dead = false;
                    } else {
                        ((Ship)e).kinetic_shield.new KineticShieldPiece(e.dir(past_state));
                    }
                } else
                if (e instanceof Satellite) {
                    Satellite s = (Satellite)e;
                    if(s.isLarge) s.explode();
                    else dead = false; // small satellites are shoot-through
                }

                boolean washit = dead;
                if(isHighEnergy) dead = false;
                if(dead) onExpire(e);
                return washit;
            }
            return false;
        }
    }
    class SplitBullet extends Bullet {
        private int splits = 6; // results in 2^splits bullets
        public SplitBullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL, Color COLOR) {
            super(ship, x, y, dx, dy, hit_radius, TTL, COLOR);
        }

        @Override
        void onExpire(PO cause) {
            super.onExpire(cause);
//            if(splits==0) return;
            if(isBlackHole || isin_hyperspace) return;
            if(cause!=null && !isBlackHole && !isin_hyperspace) {
                double life_degradation = 0.9;
                double s = speed();
                double d = cause==null ? dirOf(dx,dy,s) : dir(cause);
                double d1 = d + PI/6;
                double d2 = d - PI/6;
                game.runNext.add(() -> {
                    new SplitBullet(owner, x, y, s*cos(d1), s*sin(d1), radius, life_degradation*1/ttl_d, color).splits = splits-1;
                    new SplitBullet(owner, x, y, s*cos(d2), s*sin(d2), radius, life_degradation*1/ttl_d, color).splits = splits-1;
                });
            }
        }
    }
    class UfoBullet extends Bullet {

        public UfoBullet(Ship ship, double x, double y, double dx, double dy, double hit_radius, double TTL, Color COLOR) {
            super(ship, x, y, dx, dy, hit_radius, TTL, COLOR);
        }

        void draw() {
            gc_bgr.setGlobalAlpha(0.8*(1-ttl));
            gc_bgr.setStroke(color);
            // gc_bgr.setFill(color); produces different effect
            gc_bgr.setLineWidth(2);
            double r = 15*(ttl*ttl);
            gc_bgr.strokeOval(x-r,y-r,2*r,2*r);
            gc_bgr.fillOval(x-r,y-r,2*r,2*r);
            gc_bgr.setStroke(null);
            gc_bgr.setGlobalAlpha(1);
        }

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

            ttl -= ttld;
            if(ttl<0) {
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
            super(x,y,dx,dy,ttlmultiplier*durToTtl(millis(150)));
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
        double dispersion_angle = PI/4;
        double d1 = dir + (random())*dispersion_angle;
        double d4 = dir + .5*(random())*dispersion_angle;
        double d2 = dir - (random())*dispersion_angle;
        double d3 = dir - .5*(random())*dispersion_angle;
        game.runNext.add(() -> {
            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d1),1*sin(d1),strength);
//            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d2),1*sin(d2),strength);
            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d3),1*sin(d3),strength);
//            new RocketEngineDebris(x+20*cos(dir), y+20*sin(dir), 1*cos(d4),1*sin(d4),strength);
        });
    };
    class PulseEngineDebris extends Particle {

        PulseEngineDebris(double x, double y, double dx, double dy, double ttlmultiplier) {
            super(x,y,dx,dy,ttlmultiplier*durToTtl(millis(250)));
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

        public UfoEngineDebris(double x, double y, double radius) {
            super(x,y,0,0,durToTtl(seconds(0.5)));
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

        RadioWavePulse(double x, double y, double dx, double dy, double EPANSION_RATE, Color COLOR, boolean RECTANGULAR) {
            super(x,y,dx,dy,durToTtl(seconds(2)));
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
            if(s.dead) return;
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
            mass = size*100;
        }

        void move() {}
        void draw() {
            drawScaledImage(gc, graphicsImage,x,y,graphicsScale);
        }
        void onHit(SO o) {
            hits++; // hits are checked only for bullets
            if(!(o instanceof Bullet) || hits>hits_max) split(o);
            else size *= size_hitdecr;
            onHitParticles(o);
        }
        final void split(SO o) {
            boolean spontaneous = o instanceof BlackHole;
            dead = true;
            game.runNext.add(() ->
                repeat(splits, i -> {
                    double h = random();
                    double v = random();
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

    private class Energ extends Asteroid<OrganelleMover> {
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
//            gc_bgr.setGlobalBlendMode(DARKEN);
            gc_bgr.setEffect(null);
            gc_bgr.setStroke(null);
            gc_bgr.setFill(new RadialGradient(deg(dir),0.6,0.5,0.5,0.5,true,NO_CYCLE,new Stop(0+abs(0.3*sin(heartbeat)),colordead),new Stop(0.5,coloralive),new Stop(1,Color.TRANSPARENT)));
            drawOval(gc_bgr,x,y,radius);
//            gc_bgr.setGlobalBlendMode(SRC_OVER);
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
                    Energ.this.dx + randMN(-1,1) + 1.5*random()*cos(hitdir),
                    Energ.this.dy + randMN(-1,1) + 1.5*random()*sin(hitdir),
                    durToTtl(seconds(0.5+rand0N(1)+rand0N(size)))
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
        void onHitParticles(SO o) {
            double hitdir = dir(o);
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
            super(x,y,dx,dy,durToTtl(time));
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
            gc_bgr.setFill(new Color(0,.1,.1, 1));
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
//            gc_bgr.setFill(Color.BLACK);
            gc_bgr.setFill(new Color(0,.1,.1, 1));
            gc_bgr.fillOval(x-rr,y-rr,d,d);
        }
    }
    private class Genoid extends Asteroid<OrganelleMover> {
        double circling = 0;
        double circling_speed = 0.5*2*PI/durToTtl(seconds(0.5)); // times/sec
        double circling_mag = 0;

        final PTTL trail = new PTTL(() -> durToTtl(seconds(0.5+rand0N(2))),() -> {
            if(0.9>size && size >0.4) {
                new GenoidDebris(x+1.5*radius*cos(circling),y+1.5*radius*sin(circling),0,0,2,seconds(1.6));
                new GenoidDebris(x+1.5*radius*cos(circling),y+1.5*radius*sin(circling),0,0,2,seconds(1.6));
            } else {
                new GenoidDebris(x+circling_mag*1.5*radius*cos(dir+PI/2),y+circling_mag*1.5*radius*sin(dir+PI/2),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
                new GenoidDebris(x+circling_mag*1.5*radius*cos(dir-PI/2),y+circling_mag*1.5*radius*sin(dir-PI/2),dx*0.8,dy*0.8,1.5+size*2,seconds(2));
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
            double hitdir = dir(o);
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
            super(x,y,dx,dy,durToTtl(time));
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
    private class Fermi extends Asteroid<OrganelleMover> {
        final PTTL trail = new PTTL(() -> durToTtl(seconds(0.5+rand0N(2))), () -> new FermiDebris(x,y,0,0,5,seconds(2)));
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

            Ƒ0<FermiMove> m = randOf(StraightMove::new,WaveMove::new,FlowerMove::new,ZigZagMove::new,KnobMove::new,SpiralMove::new);
            pseudomovement = m.apply();
        }

        public void doLoop() {
            if(pseudomovement!=null) ttlocillation += pseudomovement.oscillationIncrement();
            double dxtemp = dx;
            double dytemp = dy;

            if(pseudomovement!=null) pseudomovement.modifyMovement();
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
            double hitdir = dir(o);
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
                dx += 2*cos(dir+s*PI/2) ;
                dy += 2*sin(dir+s*PI/2);
            }
        }
        class ZigZagMove extends FermiMove {
            double oscillationIncrement() {
                return 1;
            }
            public void modifyMovement() {
                if(ttlocillation%20<10) {
                    dx += cos(dir-PI/2) ;
                    dy += sin(dir-PI/2);
                } else {
                    dx += 3*cos(dir+PI/2) ;
                    dy += 3*sin(dir+PI/2);
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
                dx += 2*cos(dir+s*2*PI) ;
                dy += 2*sin(dir+s*2*PI);
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
    }

    private class FermiGraphics extends Particle implements Draw2 {
        double r;
        Color color = game.mission.color;

        public FermiGraphics(double x, double y, double RADIUS) {
            this(x,y,0,0,RADIUS,seconds(0.4));
        }
        public FermiGraphics(double x, double y, double dx, double dy, double RADIUS, Duration time) {
            super(x,y,dx,dy,durToTtl(time));
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
    abstract class ForceField extends SO {
        abstract void apply(PO o);
        abstract double force(double mass, double dist);
        public void doLoop(){}
    }
    class BlackHole extends ForceField {
        double ttl = 1;
        double ttld = 0;
        Player owner = null;
        double radius_even_horizon = 25;
        double consumed = 0;

        public BlackHole(Player OWNER, Duration TTL, double X, double Y) {
            x=X; y=Y; ttl=1; ttld=1/durToTtl(TTL); owner = OWNER;
        }

        @Override
        void apply(PO o) {
            // Gravity affects all dimensions. Hyperspace is irrelevant.
            // if(ff.isin_hyperspace!=o.isin_hyperspace) return;

            double distx = distXSigned(x,o.x);
            double disty = distYSigned(y,o.y);
            double dist = dist(distx,disty)+1; // +1 avoids /0 " + dist);
            double f = force(o.mass,dist);

            // Bullets are affected more & stop aging. Gravity-based bullets?
            if(o instanceof Bullet && dist<220) {
                Bullet b = ((Bullet)o);
                double dist01 = 1-dist/220;
                b.ttl = min(1,b.ttl + b.ttl_d*0.75*(0.5+dist01)*0.8); // dont age
                f = f*8; // add more force
            }
            boolean isRocket = o instanceof Rocket;

            // Rockets rotate towards black hole. Ergosphere.
            // This actually simplifies near-BH control (it allows orbiting BH using just 1 key)
            // and its a nice effect.
            if(isRocket && dist<520) {
                double dist01 = 1-dist/520;
                double dir = o.dir(this);
                Rocket r = (Rocket)o;
                double angle = r.dir-dir;
                double angled = 0.1 * dist01*dist01 * sin(angle);
                r.dir -= angled; // no idea why - and not +
            }

            // Space resistance. Ether exists!
            // Rises chance (otherwise very low) of things falling into BH.
            // Do note this affects rockets too (makes BH escape)
            o.dx *= 0.995;
            o.dy *= 0.995;

            // Roche limit.
            // Asteroids disintegrate due to BH tidal forces.
            // It would be ugly for asteroids not do this...
            if(o instanceof Asteroid) {
                Asteroid a = ((Asteroid)o);
                if(dist/220<a.size) {
                    a.onHit(this);
                }
            }

            // Consumed debris.
            // Instead of counting all particles (debris), we approximate to 1 particle
            // per near-BH object per loop, using 0.1 mass per particle.
            // Normally debris would take time to get consumed (if at all), this way it
            // happens instantly.
            if(dist<220) {
                consumed += 0.1;
            }

            // Consuming objects
            if(dist<radius_even_horizon) {
                if(o instanceof Rocket) ((Rocket)o).player.die();
                o.dead = true;
                if(owner!=null) owner.score.setValueOf(score -> score + 20);
                consumed += o.mass;
                System.out.println("consumed " + this.hashCode() + " " + consumed);
            }

//            double dir = o.dir(this)+PI/2;
//            o.x += (1-dist/2000)*1*cos(dir);
//            o.y += (1-dist/2000)*1*sin(dir);

//            if(dist<220) {
//                double dir = o.dir(ff)+PI/2;
//                o.dx += (1-dist/220)*0.05*cos(dir);
//                o.dy += (1-dist/220)*0.05*sin(dir);
//            }

            // apply force
            o.dx += distx*f/dist;
            o.dy += disty*f/dist;
        }

        @Override
        public double force(double mass, double dist) {
//            if(dist>220) return 0.03*(1-dist/1000);
//            else return 0.02+0.18*(1-dist/220);
            if(dist>220) return 0.02*(1-dist/1000);
            else return 0.02+0.18*(1-pow(dist/220,2));
        }

        @Override
        public void doLoop() {
            ttl -= ttld;
            if(ttl<0) dead = true;

            gc.setFill(Color.BLACK);
            drawOval(gc, x,y,radius_even_horizon);

            if(game.mission.id==4) {
                gc_bgr.setGlobalBlendMode(OVERLAY);
                gc_bgr.setGlobalAlpha(1-ttl);
    //            gc_bgr.setGlobalAlpha(0.1*(1-ttl));
                gc_bgr.setEffect(new BoxBlur(100,100,1));
                gc_bgr.setFill(Color.AQUA);
                drawOval(gc_bgr, x,y,100);
                gc_bgr.setGlobalBlendMode(SRC_OVER);
                gc_bgr.setGlobalAlpha(1);
                gc_bgr.setEffect(null);
            }

            boolean isCrowded = game.oss.get(Particle.class).size() > BLACKHOLE_PARTICLES_MAX;
            for(Particle p : game.oss.get(Particle.class)) {
                if(p.ignore_blackholes) continue;

                double distx = distXSigned(x,p.x);
                double disty = distYSigned(y,p.y);
                double dist = dist(distx,disty)+1; // +1 avoids /0
                double f = force(p.mass,dist);
                p.dx += distx*f/dist;
                p.dy += disty*f/dist;

                // Time dilatation
                // Particles live longer in strong gravity fields
                //
                // Bug: when affected by multiple BHs, the particles can age backwards, which
                // is not as good as it sounds, as their size depends on life, causing ugly effects
                if(dist<220) {
//                    p.ttl = min(1,p.ttl + p.ttld*0.8);    // particles dont age near black hole
                    p.ttl += p.ttld/2;
                }

                // Overload effect
                // Too many particles cause BH to erupt some.
                // Two reasons:
                //    1) BHs stop particle aging and cause particle count explotion (intended) and
                //       if got out of hand, performance suffers significantly. This solves the
                //       problem rather elegantly
                //          a) system agnostic - no tuning is necessary except for PARTICLE LIMIT
                //          b) no particle lifetime management overhead
                //    2) Cool effect.
                if(isCrowded && dist<2*radius_even_horizon) {
                    p.ignore_blackholes = true;
                }

//                if(dist<220) {
//                    double dir = p.dir(this)+PI/2;
//                    p.dx += (1-dist/220)*0.05*cos(dir);
//                    p.dy += (1-dist/220)*0.05*sin(dir);
//                }

                // very interesting spacetime 'wind' effect
//                double dir = p.dir(this)+PI/2;
//                p.dx += (1-dist/220)*0.05*cos(dir);
//                p.dy += (1-dist/220)*0.05*sin(dir);

                // very interesting spacetime constant rotation effect
//                double dir = p.dir(this)+PI/2;
//                p.x += (1-dist/220)*5*cos(dir);
//                p.y += (1-dist/220)*5*sin(dir);


//                double dir = p.dir(this)+PI/2;
//                p.x += (1-dist/2000)*0.5*cos(dir);
//                p.y += (1-dist/2000)*0.5*sin(dir);
            }
        }

    }

    private class RocketEnhancer {
        final String name;
        final String description;
        final GlyphIcons icon;
        final Duration duration;
        final Consumer<Rocket> starter;
        final Consumer<Rocket> stopper;

        RocketEnhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, Consumer<Rocket> STOPPER, String... DESCRIPTION) {
            name = NAME;
            icon = ICON;
            duration = DURATION;
            starter = STARTER;
            stopper = STOPPER;
            description = String.join("\n", DESCRIPTION);
        }

        RocketEnhancer(String NAME, GlyphIcons ICON, Duration DURATION, Consumer<Rocket> STARTER, String... DESCRIPTION) {
            this(NAME, ICON, DURATION, STARTER, r -> {}, DESCRIPTION);
        }

//        RocketEnhancer(String NAME, GlyphIcons ICON, Duration DURATION, Ƒ1<Rocket,InEffect> effect_supplier, String... DESCRIPTION) {
//            this(NAME, ICON, DURATION, r -> effect_supplier.apply(r).inc(), r -> effect_supplier.apply(r).dec(), DESCRIPTION);
//        }

        void enhance(Rocket r) {
            start(r);
            game.runNext.add(duration,() -> stop(r));
        }

        void start(Rocket r) {
            if(game.humans.share_enhancers) {
                game.oss.get(Rocket.class).forEach(starter);
                game.oss.get(Rocket.class).forEach(rk -> new REIndicator(rk,this));
            }
            else {
                starter.accept(r);
                new REIndicator(r,this);
            }
        }

        void stop(Rocket r) {
            if(game.humans.share_enhancers) game.oss.get(Rocket.class).forEach(stopper);
            else stopper.accept(r);
        }
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
            if(ttl<0) game.runNext.add(this::dispose);
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
    static class InEffect extends InEffectValue<Void> {
        public InEffect() {
            super(t -> null);
        }
        public InEffect(Consumer<Integer> onChange) {
            super(0, t -> null, onChange);
        }
    }
    static class InEffectValue<T> {
        private int times = 0;
        private T value;
        private final Ƒ1<Integer,T> valueCalc;
        private final Consumer<Integer> changeApplier;

        public InEffectValue(int times_init, Ƒ1<Integer,T> valueCalculator, Consumer<Integer> onChange) {
            times = times_init;
            valueCalc = valueCalculator;
            changeApplier = onChange;
            value = valueCalc.apply(times);
        }
        public InEffectValue(int times_init, Ƒ1<Integer,T> valueCalculator) {
            this(times_init, valueCalculator, null);
        }
        public InEffectValue(Ƒ1<Integer,T> valueCalculator) {
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

        void inc() {
            times++;
            value = valueCalc.apply(times);
            if(changeApplier!=null) changeApplier.accept(times);
        }

        void dec() {
            times--;
            value = valueCalc.apply(times);
            if(changeApplier!=null) changeApplier.accept(times);
        }

        void reset() {
            if(times!=0) {
                times = 0;
                value = valueCalc.apply(times);
                if(changeApplier!=null) changeApplier.accept(times);
            }
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

    /** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.width)*/
    private double modX(double x) {
        if(x<0) return playfield.getWidth()+x;
        else if(x>playfield.getWidth()) return x-playfield.getWidth();
        return x;
    }
    /** Modular coordinates. Maps coordinates of (-inf,+inf) to (0,map.height)*/
    private double modY(double y) {
        if(y<0) return playfield.getHeight()+y;
        else if(y>playfield.getHeight()) return y-playfield.getHeight();
        return y;
    }
    private double distX(double x1, double x2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return abs(x1-x2);

        if(x1<x2) return min(x2-x1, x1+playfield.getWidth()-x2);
        else return min(x1-x2, x2+playfield.getWidth()-x1);
    }
    private double distY(double y1, double y2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return abs(y1-y2);

        if(y1<y2) return min(y2-y1, y1+playfield.getHeight()-y2);
        else return min(y1-y2, y2+playfield.getHeight()-y1);
    }
    private double distXSigned(double x1, double x2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return x1-x2;

        if(x1<x2) {
            double d1 = x2-x1;
            double d2 = x1+playfield.getWidth()-x2;
            return d1<d2 ? -d1 : d2;
        } else {
            double d1 = x1-x2;
            double d2 = x2+playfield.getWidth()-x1;
            return d1<d2 ? d1 : -d2;
        }
    }
    private double distYSigned(double y1, double y2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return y1-y2;

        if(y1<y2) {
            double d1 = y2-y1;
            double d2 = y1+playfield.getHeight()-y2;
            return d1<d2 ? -d1 : d2;
        } else {
            double d1 = y1-y2;
            double d2 = y2+playfield.getHeight()-y1;
            return d1<d2 ? d1 : -d2;
        }
    }
    private double dist(double x1, double y1, double x2, double y2) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));

        double dx = distX(x1, x2);
        double dy = distY(y1, y2);
        return sqrt(dx*dx+dy*dy);
    }
    private double dist(double distX, double distY) {
        return sqrt(distX*distX+distY*distY);
    }
    private boolean isDistLess(double x1, double y1, double x2, double y2, double as) {
        // because we use modular coordinates (infinite field connected by borders), distance
        // calculation needs a tweak
        // return (x1-x2)*(x1-x2)+(y1-y2)*(y1-y2) < as*as;

        double dx = distX(x1, x2);
        double dy = distY(y1, y2);
        return dx*dx+dy*dy < as*as;
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
    /**
     * Creates array of fire angles for specified number of turrets. Angles are a symmetrical
     * sequence with 0 in the middle and consistent angle gap in between each.
     */
    private static final Double[] calculateGunTurretAngles(int i) {
        if(i<=1) return array(0d);
        return ( i%2==1
            ? range(-i/2d,i/2d)  // ... -3 -2 -1 0 +1 +2 +3 ...
            : stream(range(0.5-i/2,-0.5),range(0.5,i/2-0.5))   // ... -1.5 -0.5 +0.5 +1.5 ...
        ).map(x -> ROCKET_GUN_TURRET_ANGLE_GAP*x)
         .toArray(size -> new Double[size]);
    }
    /** Relocates node such the center of the node is at the coordinates. */
    private static void relocateCenter(Node n, double x, double y) {
        n.relocate(x-n.getLayoutBounds().getWidth()/2,y-n.getLayoutBounds().getHeight()/2);
    }
    private static Node createPlayerStat(Player p) {
        Label score = new Label();
        score.setFont(UI_FONT);
        p.score.maintain(s -> score.setText("Score: " + s));

        Label nameL = new Label();
        nameL.setFont(UI_FONT);
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
        energy.setFont(UI_FONT);
        p.energy.maintain(e -> energy.setText("Energy: " + e.intValue()));

        Label energyKS = new Label();
        energyKS.setFont(UI_FONT);
        p.energyKS.maintain(e -> energyKS.setText("Shield: " + e.intValue()));

        VBox node = layVertically(5, CENTER_LEFT, nameL,score,lives,energy,energyKS);
        node.setMaxHeight(VBox.USE_PREF_SIZE); // fixes alignment in parent by not expanding this box
        node.setPrefWidth(140); // fixes position changing on children resize
        node.setUserData(p.id); // to recognize which belongs to which
        return node;
    }
    private static Icon createPlayerLiveIcon() {
        return new Icon(MaterialDesignIcon.ROCKET,15);
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
    private static void rotate(GraphicsContext gc, double angle, double px, double py) {
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
    private static void drawRotatedImage(GraphicsContext gc, Image i, double angle, double tlpx, double tlpy) {
        gc.save();
        rotate(gc, angle, tlpx + i.getWidth() / 2, tlpy + i.getHeight() / 2);
        gc.drawImage(i, tlpx, tlpy);
        gc.restore();
    }
    private static void drawImage(GraphicsContext gc, Image i, double x, double y) {
        gc.drawImage(i, x+i.getWidth()/2, y+i.getHeight()/2, i.getWidth(), i.getHeight());
    }
    private static void drawScaledImage(GraphicsContext gc, Image i, double x, double y, double scale) {
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



    class HowToPane extends OverlayPane {
        private final GridPane g = new GridPane();
        private final Icon helpI = createInfoIcon("How to play");

        public HowToPane() {
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
        public void show() {
            super.show();
            build();
        }

        @Override
        public void hide() {
            super.hide();
        }

        private void build() {
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
                .map(Ƒ0::apply) // instantiate
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
}