/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Comet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import Comet.Comet.PO;
import Comet.Comet.Player;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.objects.icon.Icon;
import util.animation.Anim;
import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ0;
import util.functional.Functors.Ƒ1;
import util.reactive.RunnableSet;

import static Comet.Comet.*;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.random;
import static java.lang.Math.sqrt;
import static java.util.Collections.EMPTY_SET;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.util.Duration.millis;
import static util.functional.Util.array;
import static util.functional.Util.forEachCartesian;
import static util.functional.Util.forEachCartesianHalfNoSelf;
import static util.functional.Util.isInR;
import static util.functional.Util.range;
import static util.functional.Util.repeat;
import static util.functional.Util.stream;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layVertically;
import static util.graphics.Util.setScaleXY;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class Utils {


    static Font UI_FONT;

    static {
        // Tele-Marines is packed with windows 8.1, but to be sure it works on any version and
        // platform it is packed with the widget.
        UI_FONT = Font.loadFont(Utils.class.getResourceAsStream("Tele-Marines.TTF"), 12.0);
    }


    /** Converts radians to degrees. */
    public static double deg(double rad) {
        return Math.toDegrees(rad); //360*rad/(2*PI);
    }
    /** Returns angle in rad for given sin and cos. */
    public static double dirOf(double x, double y, double dist) {
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
    public static final Double[] calculateGunTurretAngles(int i) {
        if(i<=1) return array(0d);
        return ( i%2==1
            ? range(-i/2d,i/2d)  // ... -3 -2 -1 0 +1 +2 +3 ...
            : stream(range(0.5-i/2,-0.5),range(0.5,i/2-0.5))   // ... -1.5 -0.5 +0.5 +1.5 ...
        ).map(x -> ROCKET_GUN_TURRET_ANGLE_GAP*x)
         .toArray(size -> new Double[size]);
    }

    /** Relocates node such the center of the node is at the coordinates. */
    public static void relocateCenter(Node n, double x, double y) {
        n.relocate(x-n.getLayoutBounds().getWidth()/2,y-n.getLayoutBounds().getHeight()/2);
    }

    public static Node createPlayerStat(Player p) {
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

    private static void installFont(Labeled l, Font f) {
        Font ft = f==null ? Font.getDefault() : f;
        l.setFont(ft);
        l.setStyle("{ -fx-font: \"" + ft.getFamily() + "\"; }"); // bugfix, suddenly !work without this...
    }

    public static Icon createPlayerLiveIcon() {
        return new Icon(MaterialDesignIcon.ROCKET,15);
    }

    public static Anim createHyperSpaceAnim(Node n) {
        return new Anim(millis(200), x -> setScaleXY(n,1-x*x));
    }

    /** Snapshot an image out of a node, consider transparency. */
    public static Image createImage(Node n) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        int imageWidth = (int) n.getBoundsInLocal().getWidth();
        int imageHeight = (int) n.getBoundsInLocal().getHeight();

        WritableImage wi = new WritableImage(imageWidth, imageHeight);
        n.snapshot(parameters, wi);

        return wi;
    }
    /** Creates image from icon. */
    public static Image graphics(GlyphIcons icon, double radius, Color c, Effect effect) {
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
    public static void rotate(GraphicsContext gc, double angle, double px, double py) {
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
    public static void drawRotatedImage(GraphicsContext gc, Image i, double angle, double tlpx, double tlpy) {
        gc.save();
        rotate(gc, angle, tlpx + i.getWidth() / 2, tlpy + i.getHeight() / 2);
        gc.drawImage(i, tlpx, tlpy);
        gc.restore();
    }
    public static void drawImage(GraphicsContext gc, Image i, double x, double y) {
        gc.drawImage(i, x+i.getWidth()/2, y+i.getHeight()/2, i.getWidth(), i.getHeight());
    }
    public static void drawScaledImage(GraphicsContext gc, Image i, double x, double y, double scale) {
        gc.drawImage(i, x-scale*(i.getWidth()/2), y-scale*(i.getHeight()/2), scale*i.getWidth(), scale*i.getHeight());
    }
    public static void drawOval(GraphicsContext g, double x, double y, double r) {
        double d = 2*r;
        g.fillOval(x-r,y-r,d,d);
    }
    public static void drawRect(GraphicsContext g, double x, double y, double r) {
        double d = 2*r;
        g.fillOval(x-r,y-r,d,d);
    }

    public static double durToTtl(Duration d) {
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
    public static Random RAND = new Random();





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
            if(times<=0) return;
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

    /** 2d vector. Mutable. */
    static class Vec2 {
        public double x;
        public double y;

        public Vec2(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Vec2(Vec2 v) {
            set(v);
        }

        public void set(Vec2 v) {
            this.x = v.x;
            this.y = v.y;
        }

        public void set(double x, double y, double z) {
            this.x = x;
            this.y = y;
        }

        /**
         * Multiplies this vector by the specified scalar value.
         * @param scale the scalar value
         */
        public void mul(double scale) {
            x *= scale;
            y *= scale;
        }

        /**
         * Sets the value of this vector to the difference
         * of vectors t1 and t2 (this = t1 - t2).
         * @param t1 the first vector
         * @param t2 the second vector
         */
        public void setSub(Vec2 t1, Vec2 t2) {
            this.x = t1.x - t2.x;
            this.y = t1.y - t2.y;
        }

        /**
         * Sets the value of this vector to the difference of
         * itself and vector t1 (this = this - t1) .
         * @param t1 the other vector
         */
        public void sub(Vec2 t1) {
            this.x -= t1.x;
            this.y -= t1.y;
        }

        /**
         * Sets the value of this vector to the sum
         * of vectors t1 and t2 (this = t1 + t2).
         * @param t1 the first vector
         * @param t2 the second vector
         */
        public void setAdd(Vec2 t1, Vec2 t2) {
            this.x = t1.x + t2.x;
            this.y = t1.y + t2.y;
        }

        public void setResult(DoubleUnaryOperator f) {
            this.x = f.applyAsDouble(x);
            this.y = f.applyAsDouble(y);
        }

        /**
         * Sets the value of this vector to the sum of
         * itself and vector t1 (this = this + t1) .
         * @param t1 the other vector
         */
        public void add(Vec2 t1) {
            this.x += t1.x;
            this.y += t1.y;
        }
        public void addMul(double s, Vec2 t1) {
            this.x += s*t1.x;
            this.y += s*t1.y;
        }

        /**
         * Sets the value of this vector to the mul
         * of vectors t1 and t2 (this = t1 * t2).
         * @param t1 the first vector
         * @param t2 the second vector
         */
        public void setMul(Vec2 t1, Vec2 t2) {
            this.x = t1.x * t2.x;
            this.y = t1.y * t2.y;
        }

        /**
         * Sets the value of this vector to the mul
         * of vectors t1 and scalar s (this = s * t1).
         * @param t1 the first vector
         * @param s scalar
         */
        public void setMul(double s, Vec2 t2) {
            this.x = s * t2.x;
            this.y = s * t2.y;
        }

        /**
         * Returns the length of this vector.
         * @return the length of this vector
         */
        public double length() {
            return Math.sqrt(x*x + y*y);
        }

        /**
         * Returns the length of this vector squared.
         * @return the length of this vector squared
         */
        public double lengthSqr() {
            return x*x + y*y;
        }

        public double dist(Vec2 to) {
            return sqrt((x-to.x)*(x-to.x) + (y-to.y)*(y-to.y));
        }

        public double distSqr(Vec2 to) {
            return (x-to.x)*(x-to.x) + (y-to.y)*(y-to.y);
        }

        public Vec2 diff(Vec2 to) {
            return new Vec2(x-to.x, y-to.y);
        }

        public void normalize() {
            double norm = 1.0 / length();
            this.x = x * norm;
            this.y = y * norm;
        }

        public double dot(Vec2 v1) {
            return this.x * v1.x + this.y * v1.y;
        }

        @Override
        public int hashCode() {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(x);
            bits = 31L * bits + Double.doubleToLongBits(y);
            return (int) (bits ^ (bits >> 32));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Vec2) {
                Vec2 v = (Vec2) obj;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Vec3[" + x + ", " + y + "]";
        }
    }

    /**
     * Graphical 2D grid with warp effects.
     * Based on:
     * <a href="http://gamedevelopment.tutsplus.com/tutorials/make-a-neon-vector-shooter-in-xna-the-warping-grid--gamedev-9904">neon-vector-shooter-in-xna</a>
     *
     */
    static class Grid {
        Spring[] springs;
        PointMass[][] points;
        GraphicsContext gc;
        public Color color = Color.rgb(30, 30, 139, 0.85);
        double WIDTH;
        double HEIGHT;
        int thick_frequency = 5;

        public Grid(GraphicsContext gc, double width, double height, double gap) {
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
                    points[column][row] = new PointMass(new Vec2(x, y), 1);
                    fixedPoints[column][row] = new PointMass(new Vec2(x, y), 0);
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
            for(Spring s : springs)
                s.update();

            for(PointMass[] ps : points)
                for(PointMass p : ps)
                    p.update();
        }

        public void applyDirectedForce(Vec2 force, Vec2 position, float radius) {
            for(PointMass[] ps : points)
                for(PointMass p : ps) {
                    double distsqr = position.distSqr(p.position);
                    double dist = sqrt(distsqr);
                    if (distsqr < radius*radius) {
                        Vec2 f = new Vec2(force);
                             f.setResult(v -> 10*v / (10+dist));
                        p.applyForce(f);
                    }
                }
        }

        public void applyImplosiveForce(float force, Vec2 position, float radius) {
            for(PointMass[] ps : points)
                for(PointMass p : ps) {
                double dist2 = position.distSqr(p.position);
                if (dist2 < radius*radius) {
                    Vec2 f = new Vec2(position);
                         f.sub(p.position);
                         f.setResult(v -> 10*force*v / (100+dist2));
                    p.applyForce(f);
                    p.incDamping(0.6f);
                }
            }
        }

        public void applyExplosiveForce(float force, Vec2 position, float radius) {
            for(PointMass[] ps : points)
                for(PointMass p : ps) {
                double dist2 = position.distSqr(p.position);
                if (dist2 < radius*radius) {
                    Vec2 f = new Vec2(p.position);
                         f.sub(position);
                         f.setResult(v -> 100*force*v / (10000+dist2));
                    p.applyForce(f);
                    p.incDamping(0.6f);
                }
            }
        }

        public void draw() {
            int width = points.length;
            int height = points[0].length;

            for (int y = 1; y < height; y++) {
                for (int x = 1; x < width; x++) {
                    double px = points[x][y].position.x;
                    double py = points[x][y].position.y;

                    // Im not sure why, but the opacity is not distributed uniformly across direction
                    // multiples of 90deg (PI/2) have less opacity than in diagonal directions.
                    double warp_factor = points[x][y].positionInitial.distSqr(points[x][y].position);
                           warp_factor = warp_factor/1000;
                           warp_factor = min(warp_factor,1);
                    double opacity = 0.02 + 0.6*warp_factor; // musst be 0-1, else Exception
                    gc.setGlobalAlpha(opacity);

                    if (x > 1) {
                        Vec2 left = points[x - 1][y].position;
                        float thickness = y % thick_frequency == 1 ? 1f : 0.5f;
                        drawLine(left.x,left.y,px,py, color, thickness);
                    }
                    if (y > 1) {
                        Vec2 up =  points[x][y - 1].position;
                        float thickness = x % thick_frequency == 1 ? 1f : 0.5f;
                        drawLine(up.x,up.y,px,py, color, thickness);
                    }

                    if (x > 1 && y > 1) {
                        Vec2 left = points[x - 1][y].position;
                        Vec2 up =  points[x][y - 1].position;
                        Vec2 upLeft = points[x - 1][y - 1].position;
                        drawLine(0.5*(upLeft.x + up.x),0.5*(upLeft.y + up.y), 0.5*(left.x + px),0.5*(left.y + py), color, 0.5f);   // vertical line
                        drawLine(0.5*(upLeft.x + left.x),0.5*(upLeft.y + left.y), 0.5*(up.x + px),0.5*(up.y + py), color, 0.5f);   // horizontal line
                    }
                }
            }
            gc.setGlobalAlpha(1);
        }

        private void drawLine(double x1, double y1, double x2, double y2, Color c, double thickness) {
            gc.setStroke(c);
            gc.setLineWidth(thickness);
            gc.strokeLine(x1, y1, x2, y2);
        }

        class PointMass {

            private static final double DAMPING_INIT = 0.97;

            Vec2 position;
            Vec2 positionInitial;
            Vec2 velocity;
            double massI; // inverse mass == 1/mass
            Vec2 acceleration;
            double damping = DAMPING_INIT;

            public PointMass(Vec2 position, double inverse_mass) {
                this.position = position;
                this.positionInitial = new Vec2(position);
                this.massI = inverse_mass;
                this.velocity = new Vec2(0,0);
                this.acceleration = new Vec2(0,0);
            }

            public void applyForce(Vec2 force) {
               acceleration.addMul(massI, force);
            }

            public void incDamping(double factor) {
               damping *= factor;
            }

            public void update() {
                velocity.add(acceleration);
                position.add(velocity);
                acceleration = new Vec2(0,0);
                if(velocity.lengthSqr() < 0.000001) // forbids small values, perf optimization
                    velocity = new Vec2(0,0);

                velocity.mul(damping);
                damping = DAMPING_INIT;
            }
        }
        class Spring {
            PointMass end1;
            PointMass end2;
            double length;
            double stiffness;
            double damping;

            public Spring(PointMass end1, PointMass end2, double stiffness, double damping) {
                this.end1 = end1;
                this.end2 = end2;
                this.length = end1.position.dist(end2.position)*0.95;
                this.stiffness = stiffness;
                this.damping = damping;
            }

            void update() {
                Vec2 posDiff = end1.position.diff(end2.position);
                double posDiffLen = posDiff.length();
                if(posDiffLen <= length) // we will only pull, not push
                    return;

                posDiff.setResult(v -> (v/posDiffLen) * (posDiffLen-length));
                Vec2 velDiff = end2.velocity.diff(end1.velocity);
                     velDiff.mul(damping);
                posDiff.mul(stiffness);
                posDiff.setSub(posDiff,velDiff);
                Vec2 force = posDiff;
//                force.mul(-1);
                end2.applyForce(force);
                force.mul(-1);
                end1.applyForce(force);
            }
        }
    }
}