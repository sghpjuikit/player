/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Graphic utility methods.
 * 
 * @author Plutonium_
 */
public class Util {
    
    /** Constructs ordinary {@link HBox)}. */
    public static HBox layHorizontally(double gap, Pos align, Node... nodes) {
        HBox l = new HBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }
    
    /** Constructs ordinary {@link VBox)}. */
    public static VBox layVertically(double gap, Pos align, Node... nodes) {
        VBox l = new VBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }
    
    /** Creates most simple bacground with solid bgr color as specified.*/
    public static Background bgr(Color c) {
        return new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY));
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

    public static void setScaleXY(Node n, double s) {
        n.setScaleX(s);
        n.setScaleY(s);
    }

    public static void setScaleXY(Node n, double x, double y) {
        n.setScaleX(x);
        n.setScaleY(y);
    }
}
