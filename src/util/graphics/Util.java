/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import java.util.List;
import java.util.function.Consumer;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static javafx.scene.layout.Priority.ALWAYS;

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
        etarget.addEventHandler(etype, new EventHandler<E>() {
            @Override
            public void handle(E e) {
                ehandler.accept(e);
                etarget.removeEventHandler(etype, this);
            }
        });
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
}
