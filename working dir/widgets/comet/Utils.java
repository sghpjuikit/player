package comet;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
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
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.objects.Text;
import gui.objects.icon.Icon;
import gui.pane.OverlayPane;
import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;
import one.util.streamex.StreamEx;
import util.R;
import util.SwitchException;
import util.animation.Anim;
import util.collections.Tuple2;
import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ0;
import util.functional.Functors.Ƒ1;
import util.functional.Try;
import util.reactive.SetƑ;

import static comet.Comet.Constants.FPS;
import static comet.Comet.Constants.ROCKET_GUN_TURRET_ANGLE_GAP;
import static gui.objects.icon.Icon.createInfoIcon;
import static java.lang.Double.max;
import static java.lang.Math.*;
import static java.lang.Math.min;
import static javafx.geometry.Pos.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static javafx.util.Duration.millis;
import static util.Util.clip;
import static util.collections.Tuples.tuple;
import static util.dev.Util.no;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.reactive.Util.maintain;

/**
 *
 * @author Martin Polakovic
 */
interface Utils {

    // superscript 	⁰ 	¹ 	²	³	⁴ 	⁵ 	⁶ 	⁷ 	⁸ 	⁹ 	⁺ 	⁻ 	⁼ 	⁽ 	⁾ 	ⁿ
    // subscript 	₀ 	₁ 	₂ 	₃ 	₄ 	₅ 	₆ 	₇ 	₈ 	₉ 	₊ 	₋ 	₌ 	₍ 	₎

	Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    double D360 = 2*PI;
    double D180 = PI;
    double D90 = PI/2;
    double D60 = PI/3;
    double D45 = PI/4;
    double D30 = PI/6;
    double SIN45 = sin(PI/4);
    Random RAND = new Random();
    // Tele-Marines is packed with windows 8.1, but to be sure it works on any version and
    // platform it is packed with the widget.
    Font UI_FONT = Font.loadFont(Utils.class.getResourceAsStream("Tele-Marines.TTF"), 12.0);

    /** Converts radians to degrees. */
    static double deg(double rad) {
        return Math.toDegrees(rad); //360*rad/(2*PI);
    }
    /** Returns angle in rad for given sin and cos. */
    static double dirOf(double x, double y, double dist) {
        double c = x/dist;
        double s = y/dist;
        if (c>0) return asin(s);
        return (s<0) ? acos(c) : acos(c)+PI/2;
    }

    static double computeForceInversePotential(double distance, double maxDistance) {
        return distance >= maxDistance ? 1 : distance/maxDistance;
    }

    /**
     * Creates array of fire angles for specified number of turrets. Angles are a symmetrical
     * sequence with 0 in the middle and consistent angle gap in between each.
     */
    static Double[] calculateGunTurretAngles(int i) {
        if (i<=1) return array(0d);
        return ( i%2==1
            ? range(-i/2d,i/2d)  // ... -3 -2 -1 0 +1 +2 +3 ...
            : stream(range(0.5-i/2,-0.5),range(0.5,i/2-0.5))   // ... -1.5 -0.5 +0.5 +1.5 ...
        ).map(x -> ROCKET_GUN_TURRET_ANGLE_GAP*x)
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

        Label energyKS = new Label();
        installFont(energyKS, UI_FONT);
        p.energyKS.maintain(e -> energyKS.setText("Shield: " + e.intValue()));

        VBox node = layVertically(5, CENTER_LEFT, nameL,score,lives,energy,energyKS);
        node.setMaxHeight(VBox.USE_PREF_SIZE); // fixes alignment in parent by not expanding this box
        node.setPrefWidth(140); // fixes position changing on children resize
        node.setUserData(p.id); // to recognize which belongs to which
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
		return new Anim(millis(200), x -> setScaleXY(n,1-x*x));
	}

	static void createHyperSpaceAnimIn(Game game, PO o) {
		game.runNext.addAnim01(millis(200), x -> o.graphicsScale = x*x);
	}

	static void createHyperSpaceAnimOut(Game game, PO o) {
		game.runNext.addAnim01(millis(200), x -> o.graphicsScale = 1-x*x);
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
    static void drawRect(GraphicsContext g, double x, double y, double r) {
        double d = 2*r;
        g.fillRect(x-r,y-r,d,d);
    }

    static double durToTtl(Duration d) {
        return d.toSeconds()*FPS;
    }

    static double randMN(double m, double n) {
        return m+random()*(n-m);
    }
    static double rand0N(double n) {
        return RAND.nextDouble()*n;
    }
    static int rand0or1() {
        return randBoolean() ? 0 : 1;
    }
    static int randInt(int n) {
        return RAND.nextInt(n);
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
	    no(c.length==0);
        return c[randInt(c.length)];
    }
    static <T> T randOf(Collection<T> c) {
    	no(c.isEmpty());
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
        ACTIVATING, PASSSIVATING, NO_CHANGE
    }
    enum AbilityKind {
        HYPERSPACE,DISRUPTOR,SHIELD
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

    /** How to play help pane. */
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

        @Deprecated
        @Override
        public void show() {
            super.show();
        }

        public void show(Game game) {
            super.show();
            build(game);
        }

        @Override
        public void hide() {
            super.hide();
        }

        private void build(Game game) {
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
    class MissionPane extends OverlayPane {
	    private final Text text = new Text();
	    private final Icon helpI = createInfoIcon("Mission details");

	    public MissionPane() {
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

	    @Deprecated
	    @Override
	    public void show() {
		    super.show();
	    }

	    public void show(Mission mission) {
		    super.show();

		    text.setText(
			    "Name: " + mission.name + "\n" +
				    "Scale: " + mission.scale + "\n\n" +
				    mission.details
		    );
	    }

	    @Override
	    public void hide() {
		    super.hide();
	    }
    }
	/** How to play help pane. */
	class EndGamePane extends OverlayPane {
		private final GridPane g = new GridPane();
		private final Icon helpI = createInfoIcon("How to play");

		public EndGamePane() {
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

		@Deprecated
		@Override
		public void show() {
			super.show();
		}

		public void show(Map<Player,List<Achievement>> game) {
			super.show();
			build(game);
		}

		@Override
		public void hide() {
			super.hide();
		}

		private void build(Map<Player,List<Achievement>> game) {
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

        @SuppressWarnings("unchecked")
        <T extends O> void forEach(Class<T> c, Consumer<? super T> action) {
            Set<T> l = (Set<T>) m.get(c);
            if (l!=null) l.forEach(action);
        }

        void forEach(Consumer<? super O> action) {
            m.forEach((k,set) -> set.forEach(action));
        }

        @SuppressWarnings("unchecked")
        <T extends O,E extends O> void forEach(Class<T> t, Class<E> e, BiConsumer<? super T,? super E> action) {
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
            lt.add(new Ttl(durToTtl(delay), r));
        }

        /**
         * Adds an animation.
         * <p/>
         * Adds runnable that will consume double every time this runs during the specified time from now on.
         * The double is interpolated from 0 to 1 by pre-calculated step from duration.
         */
        void addAnim01(Duration dur, DoubleConsumer r) {
            ltc.add(new TtlC(0,1,1/durToTtl(dur), r));
        }

        /**
         * Adds an animation.
         * <p/>
         * Adds runnable that will consume double every time this runs during the specified time from now on.
         * The double is interpolated between specified values by pre-calculated step from duration.
         */
        void addAnim(double from, double to, Duration dur, DoubleConsumer r) {
            ltc.add(new TtlC(from,to,(to-from)/durToTtl(dur), r));
        }

        void addPeriodic(Duration delay, Runnable r) {
            lpt.add(new PTtl(() -> durToTtl(delay), r));
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
        void accumulate(T t);
	    void calculate();
	    void clear();
    }
    class StatSet<T> extends HashSet<T> {
    	private final ToDoubleFunction<? super T> valueExtractor;

	    public StatSet(ToDoubleFunction<? super T> valueExtractor) {
		    this.valueExtractor = valueExtractor;
	    }

	    public void avg() {
			this.stream().mapToDouble(valueExtractor).average();
	    }
    }
	class StatsGame implements Stats<Game> {

		@Override
		public void accumulate(Game game) {

		}

		@Override
		public void calculate() {

		}

		@Override
		public void clear() {

		}
	}
	class StatsPlayer implements Stats<Player> {
		public DoubleSummaryStatistics controlAreaSize = new DoubleSummaryStatistics();
		public DoubleSummaryStatistics controlAreaCenterDistance = new DoubleSummaryStatistics();

		@Override
		public void accumulate(Player player) {
//			controlAreaSize.accept();
//			controlAreaCenterDistance = new DoubleSummaryStatistics();
		}

		@Override
		public void calculate() {
//			controlAreaSize = controlAreaSizes.stream().mapToDouble()
		}

		@Override
		public void clear() {
			controlAreaSize = new DoubleSummaryStatistics();
			controlAreaCenterDistance = new DoubleSummaryStatistics();
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
	abstract class Voronoi {
		protected final BiConsumer<Rocket,Double> areaAction;
		protected final BiConsumer<Rocket,Double> distAction;
		protected final BiConsumer<Double,Double> centerAction;
		protected final Consumer<Stream<Lin>> edgesAction;

		public Voronoi(BiConsumer<Rocket, Double> areaAction, BiConsumer<Rocket, Double> distAction, BiConsumer<Double,Double> centerAction, Consumer<Stream<Lin>> edgesAction) {
			this.areaAction = areaAction;
			this.distAction = distAction;
			this.edgesAction = edgesAction;
			this.centerAction = centerAction;
		}

		public void compute(Set<Rocket> rockets, double W, double H, Comet game) {
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

		protected abstract void doCompute(Set<Rocket> rockets, double W, double H, Comet game);
	}
	class Voronoi1 extends Voronoi {
		public Voronoi1(BiConsumer<Rocket, Double> areaAction, BiConsumer<Rocket, Double> distAction, BiConsumer<Double,Double> centerAction, Consumer<Stream<Lin>> edgesAction) {
			super(areaAction, distAction, centerAction, edgesAction);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void doCompute(Set<Rocket> rockets, double W, double H, Comet game) {
			Set<Site> cells = stream(rockets)
	            .flatMap(rocket -> {
	                Vec r = new Vec(rocket.x+rocket.randomVoronoiTranslation, rocket.y+rocket.randomVoronoiTranslation);
	                Site sMain = new Site(r.x, r.y);
	                sMain.setData(tuple(rocket,true));
	                return stream(new Site(r.x + W, r.y), new Site(r.x, r.y + H), new Site(r.x - W, r.y), new Site(r.x, r.y - H),
		                new Site(r.x + W, r.y + H), new Site(r.x + W, r.y - H), new Site(r.x - W, r.y + H), new Site(r.x - W, r.y - H))
		                       .peek(s -> s.setData(tuple(rocket,false)))
		                       .append(sMain);
                })
                .toSet();
			OpenList sites = new OpenList();
			cells.forEach(sites::add);

			PolygonSimple clip = new PolygonSimple();
			clip.add(-W, -H);
			clip.add(2*W, -H);
			clip.add(2*W, 2*H);
			clip.add(-W, 2*H);
			PowerDiagram diagram = new PowerDiagram();
			diagram.setSites(sites);
			diagram.setClipPoly(clip);

			// Unfortunately the computation can fail under some circumstances, so lets defend against it with Try
			Try.tryR(diagram::computeDiagram, Exception.class)
				.ifError(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOk(noValue ->
					edgesAction.accept(
						StreamEx.of(sites.iterator())
							.groupingBy(site -> ((Tuple2<Comet.Rocket,Boolean>) site.getData())._1)
							.values().stream()
							.flatMap(ss -> {
									List<Lin> lines = stream(ss)
						                  .map(site -> {
							                  Rocket rocket = ((Tuple2<Rocket,Boolean>) site.getData())._1;
							                  Boolean isMain = ((Tuple2<Rocket,Boolean>) site.getData())._2;
							                  PolygonSimple polygon = site.getPolygon();
							                  if (polygon != null && polygon.getNumPoints() > 1) {
								                  if (isMain) {
									                  Point2D c = polygon.getCentroid();
									                  areaAction.accept(rocket, polygon.getArea());
									                  distAction.accept(rocket, game.dist(c.x, c.y, rocket.x, rocket.y));
									                  centerAction.accept(c.x,c.y);
								                  }
								                  return game.game.humans.intelOn.is() ? polygon : null; // todo: refactor out
							                  }
							                  return null;
						                  })
						                  .filter(ISNTØ)
						                  // optimization: return edges -> draw edges instead of polygons, we can improve performance
						                  .flatMap(polygon -> {
							                  Stream.Builder<Lin> s = Stream.builder();
							                  for (int j=0; j<polygon.getNumPoints(); j++) {
								                  int k = j==polygon.getNumPoints()-1 ? 0 : j+1;
								                  double x1 = polygon.getXPoints()[j], x2 = polygon.getXPoints()[k],
									                     y1 = polygon.getYPoints()[j], y2 = polygon.getYPoints()[k];
								                  if ((x1>=0 && y1>=0 && x1<=W && y1<=H) || (x2>=0 && y2>=0 && x2<=W && y2<=H))
									                  s.add(new Lin(x1,y1,x2,y2));
							                  }
							                  return s.build();
						                  }).toList();
//								        .groupingBy(x -> x, counting())
//								        .entrySet().stream()
//								        .peek(e -> System.out.println(e.getValue()))
//								        .filter(e -> e.getValue()==1)
//								        .map(Entry::getKey)
									Set<Lin> linesUnique = new HashSet<>();
									Set<Lin> linesDuplicate = stream(lines).filter(n -> !linesUnique.add(n)).toSet();
									linesUnique.removeAll(linesDuplicate);
									return linesUnique.stream();
								}
							)
							// optimization: draw each edge only once by removing duplicates with Set and proper hashCode()
							.distinct()
					)
				);
		}
	}
	class Voronoi2 extends Voronoi {
		public Voronoi2(BiConsumer<Rocket, Double> areaAction, BiConsumer<Rocket, Double> distAction, BiConsumer<Double, Double> centerAction, Consumer<Stream<Lin>> edgesAction) {
			super(areaAction, distAction, centerAction, edgesAction);
		}

		@Override
		protected void doCompute(Set<Rocket> rockets, double W, double H, Comet game) {
			List<Coordinate> cells = stream(rockets).flatMap(c -> stream(new Vec(c.x, c.y),
				new Vec(c.x + W, c.y), new Vec(c.x, c.y + H), new Vec(c.x - W, c.y), new Vec(c.x, c.y - H),
				new Vec(c.x + W, c.y + H), new Vec(c.x + W, c.y - H), new Vec(c.x - W, c.y + H), new Vec(c.x - W, c.y - H)))
				                         .peek(c -> c.x += randMN(0.01, 0.012)).map(c -> new Coordinate(c.x, c.y))
				                         .toList();

			VoronoiDiagramBuilder voronoi = new VoronoiDiagramBuilder();
			voronoi.setClipEnvelope(new Envelope(0, W, 0, H));
			voronoi.setSites(cells);
			Try.tryS(() -> voronoi.getDiagram(new GeometryFactory()), Exception.class)
				.ifError(e -> LOGGER.warn("Computation of Voronoi diagram failed", e))
				.ifOk(g -> {
					IntStream.range(0, g.getNumGeometries())
						.mapToObj(g::getGeometryN)
						.peek(polygon -> {
							Point c = polygon.getCentroid();
							centerAction.accept(c.getX(),c.getY());
						})
						.flatMap(polygon -> {
							Coordinate[] cs = polygon.getCoordinates();
							double[] xs = new double[cs.length];
							double[] ys = new double[cs.length];
							for (int j = 0; j < cs.length; j++) {
								xs[j] = cs[j].x;
								ys[j] = cs[j].y;
							}

							Stream.Builder s = Stream.builder();
							for (int j=0; j<cs.length; j++) {
								int k = j==cs.length-1 ? 0 : j+1;
								double x1 = xs[j], x2 = xs[k], y1 = ys[j], y2 = ys[k];
								if ((x1>=0 && y1>=0 && x1<=W && y1<=H) || (x2>=0 && y2>=0 && x2<=W && y2<=H))
									s.add(new Lin(x1,y1,x2,y2));
							}
							return s.build();
						});
				});
		}
	}

    /**
     * Graphical 2D grid with warp effects.
     * Based on:
     * <a href="http://gamedevelopment.tutsplus.com/tutorials/make-a-neon-vector-shooter-in-xna-the-warping-grid--gamedev-9904">neon-vector-shooter-in-xna</a>
     *
     */
    class Grid {
        Spring[] springs;
        PointMass[][] points;
        GraphicsContext gc;
	    Color color = Color.rgb(30, 30, 139, 0.85);
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
            for (Spring s : springs)
                s.update();

            for (PointMass[] ps : points)
                for (PointMass p : ps)
                    p.update();
        }

         void applyDirectedForce(Vec force, Vec position, double radius) {
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
            gc.save();
	        gc.setStroke(color);
	        double opacityMin = 0.02, opacityMax = 0.3;

            int width = points.length;
            int height = points[0].length;

            for (int y = 1; y < height; y++) {
                for (int x = 1; x < width; x++) {
                    double px = points[x][y].position.x;
                    double py = points[x][y].position.y;

	                Vec p = points[x][y].position;
	                Vec piInit = points[x][y].positionInitial;
                    double warpDist = 1.5*p.distX(piInit)*p.distY(piInit);
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

                    if (x > 1 && y > 1) {
                        Vec left = points[x - 1][y].position;
                        Vec up =  points[x][y - 1].position;
                        Vec upLeft = points[x - 1][y - 1].position;
                        drawLine(0.5*(upLeft.x + up.x),0.5*(upLeft.y + up.y), 0.5*(left.x + px),0.5*(left.y + py), 0.5f);   // vertical line
                        drawLine(0.5*(upLeft.x + left.x),0.5*(upLeft.y + left.y), 0.5*(up.x + px),0.5*(up.y + py), 0.5f);   // horizontal line
                    }
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
}