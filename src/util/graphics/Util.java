/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import java.util.List;
import java.util.function.Consumer;

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
import javafx.stage.WindowEvent;

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

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static void layAnchor(AnchorPane pane, Node n, Double a) {
        pane.getChildren().add(n);
        setAnchors(n, a);
    }

    /** Constructs ordinary {@link AnchorPane)}. Convenience constructor for more fluent style. */
    public static void layAnchor(AnchorPane pane, Node n, Double top, Double right, Double bottom, Double left) {
        pane.getChildren().add(n);
        setAnchors(n, top, right, bottom, left);
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

    public static void add1timeEventHandler(Stage etarget, EventType<WindowEvent> etype, Consumer<WindowEvent> ehandler) {
        etarget.addEventHandler(etype, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
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
