
package Layout.Containers;

import Configuration.PropertyMap;
import GUI.GUI;
import GUI.objects.SimplePositionable;
import Layout.Areas.ContainerNode;
import Layout.BiContainer;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.Widget;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import utilities.TODO;

/**
 * @author uranium
 *
 */
@TODO("resizing when collapsed slow response & boilerplate code")
public final class Splitter implements ContainerNode {
    
    AnchorPane root = new AnchorPane();
    @FXML AnchorPane root_child1;
    @FXML AnchorPane root_child2;
    @FXML SplitPane splitPane;
    @FXML AnchorPane controlsRoot;
    @FXML TilePane controlsBox;
    @FXML TilePane collapseBox;
    
    SimplePositionable controls;
    BiContainer container;
    
    //tmp variables
    private final PropertyMap prop;         // for easy access to container's props
    private static boolean mouse_mov_divider = false;
    private final FadeTransition fadeIn;
    private final FadeTransition fadeOut;

    private void applyPos() {
        splitPane.getDividers().get(0).setPosition(prop.getD("pos"));
    }
    
    public Splitter(BiContainer con) {
        container = con;
        prop = con.properties;
        
        // load graphics
        FXMLLoader fxmlLoader = new FXMLLoader(Splitter.class.getResource("Splitter.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) { 
            throw new RuntimeException(e);
        }
        
        // initialize properties
        prop.initProperty(Double.class, "pos", 0.5d);
        prop.initProperty(Orientation.class, "orient", VERTICAL);
        prop.initProperty(Boolean.class, "abs_size", false); //true=not locked size
        prop.initProperty(Double.class, "collap", 0d);
        prop.initProperty(Integer.class, "col", 0);
       
        // set properties
        splitPane.setOrientation(prop.getOriet("orient"));
        SplitPane.setResizableWithParent(root_child2, prop.getB("abs_size"));
        applyPos();
        
        if (getCollapsed()<0)
            splitPane.setDividerPosition(0,0);
        if (getCollapsed()>0)
            splitPane.setDividerPosition(0,1);
        
        // controls behavior
        controls = new SimplePositionable(controlsRoot, root);

        root.heightProperty().addListener( o -> positionControls());
        positionControls();
        
        splitPane.heightProperty().addListener( o -> {
            if (getCollapsed()<0)
                splitPane.setDividerPositions(0);
            if (getCollapsed()>0)
                splitPane.setDividerPositions(1);
        });
        splitPane.widthProperty().addListener( o -> {
            if (getCollapsed()<0)
                splitPane.setDividerPositions(0);
            if (getCollapsed()>0)
                splitPane.setDividerPositions(1);
        });
        
        // build animations
        fadeIn = new FadeTransition(TIME, controlsRoot);
        fadeIn.setToValue(1);
        fadeOut = new FadeTransition(TIME, controlsRoot);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e->controls.getPane().setMouseTransparent(true));

        // activate animation if mouse close to divider
        final double limit = 10; // distance for activation of the animation
        splitPane.addEventFilter(MOUSE_MOVED, t -> {
            if (splitPane.getOrientation() == HORIZONTAL) {
                double X = splitPane.getDividerPositions()[0] * root.widthProperty().get();
                if (Math.abs(t.getX() - X) < limit)
                    showControls();
                else
                if (Math.abs(t.getX() - X) > limit)
                    hideControls();
            } else {
                double Y = splitPane.getDividerPositions()[0] * root.heightProperty().get();
                if (Math.abs(t.getY() - Y) < limit)
                    showControls();
                else
                if (Math.abs(t.getY() - Y) > limit)
                    hideControls();
            }
        });
        
        // activate animation if mouse if leaving area
        splitPane.addEventFilter(MOUSE_EXITED, t -> {
            if (!splitPane.contains(t.getX(), t.getY())) // the contains check is necessary to avoid mouse over button = splitPane pane mouse exit
                hideControls();
        });
        // collapse on shortcut
//        splitPane.addEventHandler(KEY_PRESSED, e -> {
////            if(e.getText().equalsIgnoreCase(Action.Shortcut_COLAPSE)){
//            if(e.getCode().equals(KeyCode.C)){
//                toggleCollapsed();
//                e.consume();
//            }
//        });
        
        // maintain controls orientation 
        splitPane.orientationProperty().addListener(o->refreshControlsOrientation());
        // init controls orientation
        refreshControlsOrientation();
        
        splitPane.addEventFilter(MOUSE_PRESSED,e-> mouse_mov_divider=true);
        splitPane.addEventFilter(MOUSE_RELEASED,e-> mouse_mov_divider=false);
        splitPane.getDividers().get(0).positionProperty().addListener( (o,ov,nv) -> {
            positionControls();
            if(mouse_mov_divider) {
                prop.set("pos", nv);
            } else {
                if(isCollapsed()) return;
                double p = prop.getD("pos");
                if(nv.doubleValue()<p-0.08||nv.doubleValue()>p+0.08) 
                    Platform.runLater(()->applyPos());
            }

        });
        
        // close container if empty on right click
        splitPane.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY) {
                if (con.getAllWidgets().count()==0)
                    con.close();
            }
        });
        
        positionControls();
        hideControls();
    }

    public void setChild1(Component w) {
        if (w == null) return;
        // repopulate
        if (w instanceof Widget) {
            Node child = w.load();
            root_child1.getChildren().setAll(child);
            // bind child to the area
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
        }
        if (w instanceof Container) {
            Node child = ((Container)w).load(root_child1);
        }
    }
    public void setChild2(Component w) {
        if (w == null) return;
        if (w instanceof Widget) {
            // repopulate
            Node child = w.load();
            root_child2.getChildren().setAll(child);
            // bind child to the area
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
        }
        if (w instanceof Container) {
            Node child = ((Container)w).load(root_child2);
        }
    }

    public AnchorPane getChild1Pane() {
        return root_child1;
    }
    public AnchorPane getChild2Pane() {
        return root_child2;
    }

    @Override
    public Pane getRoot() {
        return root;
    }
    
//    public void setOrientation(Orientation o) {
//        prop.set("orient", o);
//        splitPane.setOrientation(o);
//    }
    /**
     * Toggle orientation between vertical/horizontal.
     */
    @FXML
    public void toggleOrientation() {
        if (splitPane.getOrientation() == HORIZONTAL) {
            prop.set("orient", VERTICAL);
            splitPane.setOrientation(VERTICAL);
        } else {
            prop.set("orient", HORIZONTAL);
            splitPane.setOrientation(HORIZONTAL);
        }
    }
    /**
     * Toggle fixed size on/off.
     */
    @FXML
    public void toggleAbsoluteSize() {
        if (SplitPane.isResizableWithParent(root_child2)) {
            prop.set("abs_size", false);
            SplitPane.setResizableWithParent(root_child2, false);
        } else {
            prop.set("abs_size", true);
            SplitPane.setResizableWithParent(root_child2, true);
        }  
    }
    @FXML
    public void toggleLocked() {
        container.toggleLock();
    }
    /**
     * Switch positions of the children
     */
    @FXML
    public void switchChildren() {
        container.switchCildren();
    }
    public void toggleCollapsed() {
        int c = prop.getI("col");
        if(c==-1) {
            prop.set("col", 0);
            splitPane.setDividerPosition(0, Math.abs(getCollapsed()));
            prop.set("collap", 0d);
        } else if (c==0){
            splitPane.setDividerPosition(0, 1d);
            prop.set("collap", splitPane.getDividerPositions()[0]);
            prop.set("col", 1);
        } else if (c==1){
            prop.set("collap", -splitPane.getDividerPositions()[0]);
            splitPane.setDividerPosition(0, 0d);
            prop.set("col", -1);
        }
    }
    /** Collapse on/off to the left or top depending on the orientation. */
    public void toggleCollapsed1() {
        if (isCollapsed()) {
            splitPane.setDividerPosition(0, Math.abs(getCollapsed()));
            prop.set("collap", 0d);
            prop.set("col", 0);
        } else {
            prop.set("collap", -splitPane.getDividerPositions()[0]);
            prop.set("col", -1);
            splitPane.setDividerPosition(0, 0d);
        }  
    }
    /** Collapse on/off to the right or bottom depending on the orientation. */
    public void toggleCollapsed2() {
        if (isCollapsed()) {
            splitPane.setDividerPosition(0, Math.abs(getCollapsed()));
            prop.set("collap", 0d);
            prop.set("col", 0);
        } else {
            prop.set("collap", splitPane.getDividerPositions()[0]);
            prop.set("col", 1);
            splitPane.setDividerPosition(0, 1d);
        }  
    }
    public boolean isCollapsed() {
        return getCollapsed() != 0;
    }
    public double getCollapsed() {
        return prop.getD("collap");
    }
    
    @FXML
    public void close() {
        container.close();
    }
    
    
    public void showControls() {
        if (!GUI.alt_state) return;
        fadeIn.play();
        controls.getPane().setMouseTransparent(false);
    }
    
    public void hideControls() {
        fadeOut.play();
    }
    
    @Override
    public void show() {
        showControls();
    }

    @Override
    public void hide() {
        hideControls();
    }
    
    
    
    private void positionControls() {
        if (splitPane.getOrientation() == VERTICAL) {
            double X = splitPane.getWidth()/2;
            double Y = splitPane.getDividerPositions()[0] * root.heightProperty().get();
            controls.relocate(X-controls.getWidth()/2, Y-controls.getHeight()/2);
        } else {
            double X = splitPane.getDividerPositions()[0] * root.widthProperty().get();
            double Y = splitPane.getHeight()/2;
            controls.relocate(X-controls.getWidth()/2, Y-controls.getHeight()/2);
        }
    }
    private void refreshControlsOrientation() {
        if(splitPane.getOrientation() == HORIZONTAL) {
            controlsBox.setOrientation(VERTICAL);
            collapseBox.setOrientation(HORIZONTAL);
        } else {
            controlsBox.setOrientation(HORIZONTAL);
            collapseBox.setOrientation(VERTICAL);
        }
    }
}