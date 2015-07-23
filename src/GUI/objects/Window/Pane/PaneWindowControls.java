/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.Window.Pane;

import gui.LayoutAggregators.SwitchPane;
import gui.objects.Window.Resize;
import static gui.objects.Window.Resize.*;
import Layout.Component;
import Layout.Layout;
import static de.jensd.fx.glyphs.GlyphsDude.setIcon;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import java.io.*;
import java.net.URL;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import static javafx.scene.paint.Color.BLACK;
import javafx.scene.shape.Rectangle;
import static util.Util.setAnchors;
import util.dev.Log;

/**
 */
public class PaneWindowControls extends WindowPane {

    /** Psududoclass active when this window is resized. Applied on root as '.window'. */
    public static final PseudoClass pcResized = PseudoClass.getPseudoClass("resized");
    /** Psududoclass active when this window is moved. Applied on root as '.window'. */
    public static final PseudoClass pcMoved = PseudoClass.getPseudoClass("moved");
    /** Psududoclass active when this window is focused. Applied on root as '.window'. */
    public static final PseudoClass pcFocused = PseudoClass.getPseudoClass("focused");
    
    @FXML public AnchorPane borders;
    @FXML public AnchorPane content;
    @FXML private HBox controls;
    @FXML Button minimizeB;
    @FXML Button maximizeB;
    @FXML Button closeB;

    public PaneWindowControls(AnchorPane own) {
	super(own);
        
	try {
	    URL fxml = PaneWindowControls.class.getResource("PaneWindowControls.fxml");
	    FXMLLoader l = new FXMLLoader(fxml);
                       l.setRoot(root);
                       l.setController(this);
                       l.load();

	} catch (IOException ex) {
	    Log.err("Couldnt create Window. " + ex.getMessage());
	}
        

	// clip the content to its bounds to prevent leaking out
	Rectangle mask = new Rectangle(1, 1, BLACK);
	mask.widthProperty().bind(content.widthProperty());
	mask.heightProperty().bind(content.heightProperty());
	content.setClip(mask);


	// maintain custom pseudoclasses for .window styleclass
	resizing.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcResized, nv!=NONE));
	moving.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcMoved, nv));
	focused.addListener((o, ov, nv) -> root.pseudoClassStateChanged(pcFocused, nv));

//        moveOnDragOf(header);
        
	// header double click maximize, show header on/off
//	root.setOnMouseClicked(e -> {
//	    if (e.getButton() == MouseButton.PRIMARY)
//		if (e.getClickCount() == 2)
//		    max();
//	    if (e.getButton() == MouseButton.SECONDARY)
//		if (e.getClickCount() == 2)
//		    setHeaderVisible(!headerVisible);
//	});
        
        
        // controls
	setIcon(minimizeB,MINUS_SQUARE_ALT, "13", GRAPHIC_ONLY);
        minimizeB.setOnMouseClicked(e -> close());
	setIcon(maximizeB,PLUS_SQUARE_ALT, "13", GRAPHIC_ONLY);
        maximizeB.setOnMouseClicked(e -> close());
	setIcon(closeB,CLOSE, "13", GRAPHIC_ONLY);
        closeB.setOnMouseClicked(e -> close());
        controls.getChildren().addAll();
    }

    /**
     ***************************** CONTENT *************************************
     */
    public void setContent(Node n) {
	content.getChildren().clear();
	content.getChildren().add(n);
	setAnchors(n, 0);
    }

    public void setContent(Component c) {
	Layout l = new Layout();
	SwitchPane la = new SwitchPane();
	la.addTab(0, l);
	la.setAlwaysAlignTabs(SwitchPane.align_tabs);
	l.setChild(c);
    }
    
    /**
     Blocks input to content, but not to root.
     <p>
     Use when any input to content is not desirable, for example during
     window manipulation like animations.
     <p>
     Sometimes content could consume or interfere with the input directed
     towards the window (root), in such situations this method will help.
     <p>
     @param val
     */
    public void setContentMouseTransparent(boolean val) {
	content.setMouseTransparent(val);
    }

/***************************    HEADER & BORDER    ****************************/
    @FXML private StackPane header;
    @FXML private ImageView iconI;
    @FXML private Label titleL;
    @FXML private HBox leftHeaderBox;
    private boolean headerVisible = true;
    private boolean headerAllowed = true;

    /**
     Sets visibility of the window header, including its buttons for control
     of the window (close, etc).
     */
    public void setHeaderVisible(boolean val) {
	// prevent pointless operation
	if (!headerAllowed) return;
	headerVisible = val;
	showHeader(val);
    }

    public boolean isHeaderVisible() {
	return headerVisible;
    }

    private void showHeader(boolean val) {
	header.setVisible(val);
	if (val) {
	    AnchorPane.setTopAnchor(content, 25d);
	    setBorderless(!val);
	} else {
	    AnchorPane.setTopAnchor(content, 0d);
	}
    }

    /**
     Set false to permanently hide header.
     */
    public void setHeaderAllowed(boolean val) {
	setHeaderVisible(val);
	headerAllowed = val;
    }

    /**
     Set title for this window shown in the header.
     */
    public void setTitle(String text) {
	titleL.setText(text);
    }

    /**
     Set title alignment.
     */
    public void setTitlePosition(Pos align) {
	BorderPane.setAlignment(titleL, align);
    }

    public boolean isBorderless() {
	return AnchorPane.getBottomAnchor(content) == 0;
    }

    public void setBorderless(boolean v) {
	if (v) setAnchors(content, 0);
	else setAnchors(content, 25, 5, 5, 5);
	borders.setVisible(!v);
    }

/*******************************    RESIZING  *********************************/
    
    @FXML
    private void border_onDragStart(MouseEvent e) {
        // start resize if allowed
        if(resizable.get()) {
            Point2D b = root.getParent().screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
            double X = b.getX()-x.get();
            double Y = b.getY()-y.get();
            double WW = w.get();
            double WH = h.get();
            double L = 18; // corner treshold

            Resize r = NONE;
            if ((X > WW - L) && (Y > WH - L)) r = SE;
            else if ((X < L) && (Y > WH - L)) r = SW;
            else if ((X < L) && (Y < L))      r = NW;
            else if ((X > WW - L) && (Y < L)) r = NE;
            else if ((X > WW - L))            r = Resize.E;
            else if ((Y > WH - L))            r = S;
            else if ((X < L))                 r = W;
            else if ((Y < L))                 r = N;
            
            if(r!=NONE) {
                _resizing.set(r);
                e.consume();
            }
        }
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        // end resizing if active
	if(_resizing.get()!=NONE) {
            _resizing.set(NONE);
            e.consume();
        }
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if(_resizing.get()!=NONE) {
            Point2D b = root.getParent().screenToLocal(new Point2D(e.getScreenX(), e.getScreenY()));
            double X = x.get();
            double Y = y.get();
            Resize r = _resizing.get();
            if (r == SE) {
                w.set(b.getX() - X);
                h.set(b.getY() - Y);
            } else if (r == S) {
                h.set(b.getY() - Y);
            } else if (r == Resize.E) {
                w.set(b.getX() - X);
            } else if (r == SW) {
                w.set(X + w.get() - b.getX());
                h.set(b.getY() - Y);
                x.set(b.getX());
            } else if (r == W) {
                w.set(X + w.get() - b.getX());
                x.set(b.getX());
            } else if (r == NW) {
                w.set(X + w.get()- b.getX());
                h.set(Y + h.get() - b.getY());
                x.set(b.getX());
                y.set(b.getY());
            } else if (r == N) {
                h.set(Y + h.get() - b.getY());
                y.set(b.getY());
            } else if (r == NE) {
                w.set(b.getX() - X);
                h.set(Y + h.get() - b.getY());
                y.set(b.getY());
            }
            e.consume();
        }
    }
    
    @FXML
    private void consumeMouseEvent(MouseEvent e) {
	e.consume();
    }

}