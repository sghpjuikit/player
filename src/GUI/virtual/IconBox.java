/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.virtual;

import static java.util.Collections.reverseOrder;
import javafx.geometry.NodeOrientation;
import static javafx.geometry.NodeOrientation.*;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/**
 Simple HBox wrapper for creating icon headers. Lines up the children centered 
 vertically and horizontally to the left or right side, by given node 
 orientation. 
 */
public final class IconBox {

    public final HBox box;

    public IconBox(HBox box, NodeOrientation o) {
	this.box = box;
	setNodeOrientation(o);
    }

    public IconBox(HBox box) {
        this(box, LEFT_TO_RIGHT);
    }
    
    public IconBox() {
	this(new HBox(), LEFT_TO_RIGHT);
    }
    
    /** Sets node orientation to layout children to the left or right. */
    public final void setNodeOrientation(NodeOrientation o) {
	box.setAlignment(o==INHERIT||o==LEFT_TO_RIGHT ? CENTER_RIGHT : CENTER_LEFT);
    }
    
    /**
    Convenience method. Equivalent to box.getChildren().addAll(n); 
    */
    public void add(Node... n) {
	box.getChildren().addAll(n);
	if(box.getNodeOrientation() == RIGHT_TO_LEFT)
	    box.getChildren().sort(reverseOrder());
    }
}
