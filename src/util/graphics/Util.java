/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

import util.dev.TODO;

import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.Modality.APPLICATION_MODAL;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static util.async.Async.runFX;
import static util.dev.TODO.Purpose.BUG;

/**
 * Graphic utility methods.
 *
 * @author Plutonium_
 */
public class Util {

    /** Constructs ordinary {@link HBox)}. Convenience constructor for more fluent style. */
    public static HBox layHorizontally(double gap, Pos align, Node... nodes) {
        HBox l = new HBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }

    /** Constructs ordinary {@link HBox)}. Convenience constructor for more fluent style. */
    public static HBox layHorizontally(double gap, Pos align, List<? extends Node> nodes) {
        HBox l = new HBox(gap);
             l.setAlignment(align);
             l.getChildren().addAll(nodes);
        return l;
    }

    /** Constructs ordinary {@link VBox)}. Convenience constructor for more fluent style. */
    public static VBox layVertically(double gap, Pos align, Node... nodes) {
        VBox l = new VBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }

    /** Constructs ordinary {@link VBox)}. Convenience constructor for more fluent style. */
    public static VBox layVertically(double gap, Pos align, List<? extends Node> nodes) {
        VBox l = new VBox(gap);
             l.setAlignment(align);
             l.getChildren().addAll(nodes);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Header at the top
     * shrinks to its content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p>
     * Constructs ordinary {@link VBox)}. Convenience constructor for more fluent style.
     */
    public static VBox layHeaderTop(double gap, Pos align, Node header, Node content) {
        VBox l = layVertically(gap, align, header,content);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Header at the bottom
     * shrinks to its content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p>
     * Constructs ordinary {@link VBox)}. Convenience constructor for more fluent style.
     */
    public static VBox layHeaderBottom(double gap, Pos align, Node content, Node header) {
        VBox l = layVertically(gap, align, content, header);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Headers at the top
     * and bottom shrink to their content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p>
     * Constructs ordinary {@link VBox)}. Convenience constructor for more fluent style.
     */
    public static VBox layHeaderTopBottom(double gap, Pos align, Node headerTop, Node content, Node headerBottom) {
        VBox l = layVertically(gap, align, headerTop, content, headerBottom);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Constructs ordinary {@link StackPane)} with children aligned to {@link Pos#CENTER}.
     * Convenience constructor for more fluent style.
     */
    public static StackPane layStack(Node... ns) {
        return new StackPane(ns);
    }

    /** Constructs ordinary {@link StackPane)}. Convenience constructor for more fluent style. */
    public static StackPane layStack(Node n,Pos a) {
        StackPane l = new StackPane(n);
        StackPane.setAlignment(n, a);
        return l;
    }

    /** Constructs ordinary {@link StackPane)}. Convenience constructor for more fluent style. */
    public static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2) {
        StackPane l = new StackPane(n1,n2);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        return l;
    }

    /** Constructs ordinary {@link StackPane)}. Convenience constructor for more fluent style. */
    public static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2, Node n3,Pos a3) {
        StackPane l = new StackPane(n1,n2,n3);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        StackPane.setAlignment(n3, a3);
        return l;
    }

    /** Constructs ordinary {@link StackPane)}. Convenience constructor for more fluent style. */
    public static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2, Node n3,Pos a3, Node n4,Pos a4) {
        StackPane l = new StackPane(n1,n2,n3,n4);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        StackPane.setAlignment(n3, a3);
        StackPane.setAlignment(n4, a4);
        return l;
    }

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static AnchorPane layAnchor(Node n, Double a) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n, a);
        return p;
    }

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static AnchorPane layAnchor(Node n, Double top, Double right, Double bottom, Double left) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n, top, right, bottom, left);
        return p;
    }

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2);
        return p;
    }

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2,
                                                  Node n3, Double top3, Double right3, Double bottom3, Double left3) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2, n3, top3, right3, bottom3, left3);
        return p;
    }

    /** Sets {@link AnchorPane} anchors to the same value. Null clears all anchors. */
    public static void setAnchors(Node n, Double a) {
        if (a == null) {
            AnchorPane.clearConstraints(n);
        } else {
            AnchorPane.setTopAnchor(n, a);
            AnchorPane.setRightAnchor(n, a);
            AnchorPane.setBottomAnchor(n, a);
            AnchorPane.setLeftAnchor(n, a);
        }
    }

    /** Sets {@link AnchorPane} anchors. Null clears the respective anchor. */
    public static void setAnchors(Node n, Double top, Double right, Double bottom, Double left) {
        AnchorPane.clearConstraints(n);
        if (top != null) AnchorPane.setTopAnchor(n, top);
        if (right != null) AnchorPane.setRightAnchor(n, right);
        if (bottom != null) AnchorPane.setBottomAnchor(n, bottom);
        if (left != null) AnchorPane.setLeftAnchor(n, left);
    }

    /** Sets {@link AnchorPane)} anchors for node. Convenience method for more fluent style. */
    public static void setAnchor(AnchorPane pane, Node n, Double a) {
        pane.getChildren().add(n);
        setAnchors(n, a);
    }

    /** Sets {@link AnchorPane)} anchors for node. Convenience method for more fluent style. */
    public static void setAnchor(AnchorPane pane, Node n, Double top, Double right, Double bottom, Double left) {
        pane.getChildren().add(n);
        setAnchors(n, top, right, bottom, left);
    }

    /** Sets {@link AnchorPane)} anchors for nodes. Convenience method for more fluent style. */
    public static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2) {
        setAnchor(pane, n1, top1, right1, bottom1, left1);
        setAnchor(pane, n2, top2, right2, bottom2, left2);
    }

    /** Sets {@link AnchorPane)} anchors for nodes. Convenience method for more fluent style. */
    public static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2,
                                                  Node n3, Double top3, Double right3, Double bottom3, Double left3) {
        setAnchor(pane, n1, top1, right1, bottom1, left1);
        setAnchor(pane, n2, top2, right2, bottom2, left2);
        setAnchor(pane, n3, top3, right3, bottom3, left3);
    }

    /** Gives the text shape wrapping to its width and scrollable functionalities. */
    @Deprecated
    @TODO(purpose = BUG)
    public static ScrollPane layScrollVText(Text t) {
        // This is how it should be done, but there is a bug.
        // Unfortunately the pane resizes with the text so we cant bind
        // t.wrappingWidthProperty().bind(sa.widthProperty());
        // The only (to me) known solution is to make the text t not manageable, but that
        // causes the height caculation of the pane sa fail and consequently breaks the
        // scrolling behavior
        // I dont know what to do anymore, believe me I have tried...
//        Pane sa = new StackPane(t);
//        ScrollPane s = new ScrollPane(sa);
//                   s.setPannable(false);
//                   s.setFitToWidth(true);
//                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
//                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
//        t.wrappingWidthProperty().bind(sa.widthProperty());

        // Scrollbar width hardcoded!
        double reserve = 5;
        ScrollPane s = new ScrollPane(t);
                   s.setOnScroll(Event::consume);
                   s.setPannable(false);
                   s.setFitToWidth(false);
                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
        t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
        return s;
    }

    @Deprecated // the behavior may be unpredictable/change in the future
    @TODO(purpose = BUG)
    public static ScrollPane layScrollVTextCenter(Text t) {
        double reserve = 5;
        ScrollPane s = new ScrollPane(new StackPane(t));
                   s.setOnScroll(Event::consume);
                   s.setPannable(false);
                   s.setFitToWidth(false);
                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
        t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
        return s;
    }

    /** Creates most simple background with solid bgr color fill and no radius or insets.*/
    public static Background bgr(Color c) {
        return new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static void setScaleXY(Node n, double s) {
        n.setScaleX(s);
        n.setScaleY(s);
    }

    public static void setScaleXY(Node n, double x, double y) {
        n.setScaleX(x);
        n.setScaleY(y);
    }

    public static <E extends Event> void add1timeEventHandler(Stage etarget, EventType<E> etype, Consumer<E> ehandler) {
        etarget.addEventHandler(etype, new EventHandler<>() {
            @Override
            public void handle(E e) {
                ehandler.accept(e);
                etarget.removeEventHandler(etype, this);
            }
        });
    }

/**************************************************************************************************/

    /**
     * Sets minimal, preferred and maximal width and height of the node to provided value.
     * Any bound property will be ignored. Null value will be ignored.
     * If node isnt a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    public static void setMinPrefMaxSize(Node n, Double widthheight) {
        setMinPrefMaxSize(n, widthheight,widthheight);
    }

    /**
     * Sets minimal, preferred and maximal width and height of the node to provided values.
     * Any bound property will be ignored. Null value will be ignored.
     * If node isnt a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    public static void setMinPrefMaxSize(Node n, Double width, Double height) {
        if(n instanceof Region) {
            Region r = (Region) n;
            boolean wmin  = width!=null && !r.minWidthProperty().isBound();
            boolean wpref = width!=null && !r.prefWidthProperty().isBound();
            boolean wmax  = width!=null && !r.maxWidthProperty().isBound();
            boolean hmin  = height!=null && !r.minHeightProperty().isBound();
            boolean hpref = height!=null && !r.prefHeightProperty().isBound();
            boolean hmax  = height!=null && !r.maxHeightProperty().isBound();


            if(hmin && wmin)        r.setMinSize(width,height);
            else if (hmin && !wmin) r.setMinHeight(height);
            else if (!hmin && wmin) r.setMinWidth(height);

            if(hpref && wpref)        r.setPrefSize(width,height);
            else if (hpref && !wpref) r.setPrefHeight(height);
            else if (!hpref && wpref) r.setPrefWidth(height);

            if(hmax && wmax)        r.setMaxSize(width,height);
            else if (hmax && !wmax) r.setMaxHeight(height);
            else if (!hmax && wmax) r.setMaxWidth(height);
        }
    }

    /**
     * Sets minimal, preferred and maximal width of the node to provided value.
     * Any bound property will be ignored. Null value will be ignored.
     * If node isnt a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    public static void setMinPrefMaxWidth(Node n, Double width) {
        if(width!=null && n instanceof Region) {
            Region r = (Region) n;
            if(!r.minWidthProperty().isBound()) r.setMinWidth(width);
            if(!r.prefWidthProperty().isBound()) r.setPrefWidth(width);
            if(!r.maxWidthProperty().isBound()) r.setMaxWidth(width);
        }
    }

    /**
     * Sets minimal, preferred and maximal height of the node to provided value.
     * If property bound, value null or node not a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    public static void setMinPrefMaxHeight(Node n, Double height) {
        if(height!=null && n instanceof Region) {
            Region r = (Region) n;
            if(!r.minHeightProperty().isBound()) r.setMinHeight(height);
            if(!r.prefHeightProperty().isBound()) r.setPrefHeight(height);
            if(!r.maxHeightProperty().isBound()) r.setMaxHeight(height);
        }
    }

    public static void removeFromParent(Node parent, Node child) {
        if(parent==null || child==null) return;
        if(parent instanceof Pane) {
            ((Pane)parent).getChildren().remove(child);
        }
    }

    public static void removeFromParent(Node child) {
        if(child==null) return;
        removeFromParent(child.getParent(),child);
    }

/**************************************************************************************************/

    /**
     * Create fullscreen modal no taskbar stage on given screen. The stage will have its owner,
     * style and modality initialized and be prepared to be shown.
     * <p>
     * Use: just set your scene on it and call show().
     * <p>
     * Easy way to get popup like behavior that:
     * <ul>
     * <li> is always fullscreen
     * <li> is modal - doesnt lose focus and is always on top of other application windows
     * <li> has no taskbar icon (for your information, javafx normally disallows this, but it is
     * doable using owner stage with UTILITY style).
     * </ul>
     */
    public static Stage createFMNTStage(Screen screen) {
        // Using owner stage of UTILITY style is the only way to get a 'top level'
        // window with no taskbar.
        Stage owner = new Stage(UTILITY);
              owner.setOpacity(0); // make sure it will never be visible
              owner.setWidth(5); // stay small to leave small footprint, just in case
              owner.setHeight(5);
              owner.show(); // must be 'visible' for the hack to work

        Stage s = new Stage(UNDECORATED); // no OS header & buttons, we want full space
        s.initOwner(owner);
        s.initModality(APPLICATION_MODAL); // eliminates focus stealing form other apps
        s.setX(screen.getVisualBounds().getMinX()); // screen doesnt necessarily start at [0,0] !!
        s.setY(screen.getVisualBounds().getMinY());
        s.setWidth(screen.getVisualBounds().getWidth()); // fullscreen...
        s.setHeight(screen.getVisualBounds().getHeight());
        s.setAlwaysOnTop(true); // maybe not needed, but just in case
        // Going fullscreen actually breaks things.
        // We dont need fullscreen, we use UNECORATED stage of maximum size. Fullscreen
        // was supposed to be more of a final nail to prevent possible focus stealing.
        //
        // In reality, using fullscreen actually causes this issue! - focus stealing
        // and the consequent disappearance of the window (nearly impossible to bring it
        // back). This is even when using modality on the window or even its owneer stage.
        //
        // Fortunately things work as they should using things like we do. Pray the moment
        // they dont wont occur though.
        //
        // s.setFullScreen(true); // just in case
        // s.setFullScreenExitHint(""); // completely annoying, remove
        // // not always desired! and if we dont use fullscreen it wont work or we could just
        // // introduce nconsistent behavior. Let the dev implement his own hide if he needs.
        // s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // The owner must not escape garbage collection or remain visible forever
//        s.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> owner.hide());

        return s;
    }

/**************************************************************************************************/

    /** Returns screen containing the given coordinates. Never null. */
    public static Screen getScreen(Point2D p) {
        return getScreen(p.getX(), p.getY());
    }

    /** Returns screen containing the given coordinates. Never null. */
    public static Screen getScreen(double x, double y) {
        // rely on official util (someone hid it..., good work genius)
        return com.sun.javafx.util.Utils.getScreenForPoint(x, y);

//        for (Screen scr : Screen.getScreens())
//            if (scr.getBounds().intersects(x,y,1,1)) {
//                return scr;
//                // unknown whether this affects functionality
////                break;
//            }
//        // avoid null
//        return Screen.getPrimary();
    }

    /** Captures screenshot of the entire screen and runs custom action on fx thread. */
    public static void screenCaptureAndDo(Screen screen, Consumer<Image> action) {
        Rectangle2D r = screen.getBounds();
        screenCaptureAndDo((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight(), action);
    }

    /**
     * Captures screenshot of the screen of given size and position and runs custom
     * action on fx thread.
     */
    public static void screenCaptureAndDo(int x, int y, int w, int h, Consumer<Image> action) {
        screenCaptureRawAndDo(
            x, y, w, h,
            img -> {
                Image i = img==null ? null : SwingFXUtils.toFXImage(img, new WritableImage(img.getWidth(), img.getHeight()));
                runFX(() -> action.accept(i));
            }
        );
    }

    /** Captures screenshot of the entire screen and runs custom action on non fx thread. */
    public static void screenCaptureRawAndDo(Screen screen, Consumer<BufferedImage> action) {
        Rectangle2D r = screen.getBounds();
        screenCaptureRawAndDo((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight(), action);
    }

    /**
     * Captures screenshot of the screen of given size and position and runs custom
     * action on non fx thread.
     * <p>
     * Based on:
     * <a href="http://www.aljoscha-rittner.de/blog/archive/2011/03/09/javafxdev-screen-capture-tool-with-200-lines-and-500ms-startup-time/">javafxdev-screen-capture-tool</a>
     */
    public static void screenCaptureRawAndDo(int x, int y, int w, int h, Consumer<BufferedImage> action) {
        EventQueue.invokeLater(() -> {
            try {
                Robot robot = new Robot();
                BufferedImage img = robot.createScreenCapture(new Rectangle(x,y,w,h));
                action.accept(img);
            } catch (Exception ex) {
                util.dev.Util.log(Util.class).error("Failed to screenshot the screen {}");
                action.accept(null);
            }
        });
    }

}
