
package Layout.Areas;

import util.graphics.drag.DragUtil;
import gui.GUI;
import static gui.GUI.closeAndDo;
import gui.objects.icon.Icon;
import static Layout.Areas.Area.DRAGGED_PSEUDOCLASS;
import Layout.BiContainer;
import Layout.Component;
import Layout.Container;
import Layout.Widgets.Widget;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import unused.SimplePositionable;
import static util.Util.setAnchors;
import static util.access.SequentialValue.next;
import static util.async.Async.run;
import static util.async.Async.runFX;
import util.collections.map.PropertyMap;
import util.dev.TODO;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Severity.MEDIUM;
import static util.functional.Util.mapB;
import static util.reactive.Util.maintain;

/**
 * @author uranium
 */
@TODO(purpose = UNIMPLEMENTED, severity = MEDIUM,
      note = "resizing when collapsed slow response & boilerplate code")
public final class Splitter implements ContainerNode {
    
    AnchorPane root = new AnchorPane();
    @FXML AnchorPane root_child1;
    @FXML AnchorPane root_child2;
    @FXML SplitPane splitPane;
    @FXML AnchorPane controlsRoot;
    @FXML TilePane controlsBox;
    @FXML TilePane collapseBox;
    
    SimplePositionable controls;
    private final BiContainer container;
    
    private final PropertyMap prop;
    private final FadeTransition fadeIn;
    private final FadeTransition fadeOut;

    private boolean initialized = false;
    private EventHandler<MouseEvent> aaa;
    
    private void applyPos() {
        splitPane.getDividers().get(0).setPosition(prop.getD("pos"));
    }
    
    public Splitter(BiContainer c) {
        container = c;
        prop = c.properties;
        
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
        prop.initProperty(Integer.class, "abs_size", 0); // 0 none, 1 child1, 2 child2
        prop.initProperty(Integer.class, "col", 0);
       
        // maintain proper orientation
        maintain(container.orientation, splitPane.orientationProperty());
        // put properties
        setAbsoluteSize(prop.getI("abs_size"));
        applyPos();
        setupCollapsed(getCollapsed());
        
        // controls behavior
        controls = new SimplePositionable(controlsRoot, root);

        // maintain controls position
        // note: ideally we would reposition ontorls when show() gets called
        // but we need them to react on resizing when visible too
        // so at least reposition lazily - only when visible
        ChangeListener<Number> repositioner = (o,ov,nv) -> {
            if(controls.getPane().getOpacity() != 0) positionControls();
        };
        root.widthProperty().addListener(repositioner);
        root.heightProperty().addListener(repositioner);
        controls.getPane().opacityProperty().addListener(repositioner);
        splitPane.getDividers().get(0).positionProperty().addListener(repositioner);
                
        // build animations
        fadeIn = new FadeTransition(TIME, controlsRoot);
        fadeIn.setToValue(1);
        fadeOut = new FadeTransition(TIME, controlsRoot);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> controls.getPane().setMouseTransparent(true));
        // additional animations bound to the previous one
        controlsRoot.opacityProperty().addListener((o,ov,nv) -> {
            double d = nv.doubleValue();
            // we use absolute positioning, to always produce gap of the width 2*30
            if(splitPane.getOrientation()==HORIZONTAL) {
                root_child1.setPadding(new Insets(0, d*20, 0, 0));
                root_child2.setPadding(new Insets(0, 0, 0, d*20));
            } else {
                root_child1.setPadding(new Insets(0, 0, d*20, 0));
                root_child2.setPadding(new Insets(d*20, 0, 0, 0));
            }
        });
        
        // maintain controls position show if mouse close to divider
        // activate the handler only if this visible
        final double limit = 15; // distance for activation of the animation
        final double act_width = 70; // length for activation area
        aaa = e -> {
            double W = root.widthProperty().get();
            double H = root.heightProperty().get();
            if (splitPane.getOrientation() == HORIZONTAL) {
                double gap = (H-act_width)/2d;
                double X = splitPane.getDividerPositions()[0] * W;
                if(e.getY()>gap && e.getY()<H-gap) {
                    if (Math.abs(e.getX() - X) < limit)
                        showControls();
                    else
                    if (Math.abs(e.getX() - X) > limit)
                        hideControls();
                }
            } else {
                double gap = (W-act_width)/2d;
                if(e.getX()>gap && e.getX()<W-gap) {
                    double Y = splitPane.getDividerPositions()[0] * H;
                    if (Math.abs(e.getY() - Y) < limit)
                        showControls();
                    else
                    if (Math.abs(e.getY() - Y) > limit)
                        hideControls();
                }
            }
        };
        
        // activate animation if mouse if leaving area
        splitPane.addEventFilter(MOUSE_EXITED, e -> {
            if (!splitPane.contains(e.getX(), e.getY())) // the containsKey check is necessary to avoid mouse over button = splitPane pane mouse exit
                hideControls();
            e.consume();
        });
        
        // maintain controls orientation 
        maintain(splitPane.orientationProperty(), this::refreshControlsOrientation);
                
        splitPane.getDividers().get(0).positionProperty().addListener((o,ov,nv) -> {
            // occurs when user drags the divider, manual -> remember it
            if(splitPane.isPressed()) {
                prop.put("pos", nv);
            // occurs as a result of node parent resizing
            } else {
                // bug fix
                // when layout starts the position is not applied correctly
                // either because of a bug (only when orientation==vertical) or
                // layout not being sized properly yet, it is difficult to say
                // so for now, the initialisation phase (5s) is handled differently
                if (initialized) {
                    // the problem still remains though - the position value gets
                    // changes when restored from near zero & reapplication of
                    // the value from prop.getD("pos") is not working because of a bug
                    // which requires it to be in Platform.runlater() which causes
                    // a major unersponsiveness of the divider during resizing
                    
                    // the stored value is not affected which is good. But the gui
                    // might not be be properly put on consequent resizes or on
                    // near edge restoration (major problem)

                    
                    // because we really need to load the layout to proper
                    // position, use the workaround for initialisation
                } else {
                    double p = prop.getD("pos");
                    if(nv.doubleValue()<p-0.08||nv.doubleValue()>p+0.08)
                        runFX(this::applyPos);
                    else
                        run(5000, ()->initialized=true);
                }
            }
        });

        // close container if on right click it is empty
        splitPane.setOnMouseClicked( e -> {
            if(e.getButton()==SECONDARY && GUI.isLayoutMode()) {
                if (c.getAllWidgets().count()==0)
                    closeAndDo(root, c::close);
                e.consume();
            }
        });
        
        // controls
        Icon infoB = new Icon(INFO, 12, "Help");
        Icon dragB = new Icon(MAIL_REPLY, 12, "Move widget by dragging");
        dragB.setOnDragDetected(e -> {
            if (e.getButton()==PRIMARY) { //// primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(container.getParent(),container,db);
                // signal dragging graphically with css
                root.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, true);
                e.consume();
            }
        });
        Icon lockB = new Icon(LOCK, 12, "Lock container", this::toggleLocked);
        maintain(c.locked,mapB(LOCK,UNLOCK), lockB.icon);
        Icon absB = new Icon(CHAIN, 12, "Switch proportionally resizable content", this::toggleAbsoluteSize);absB.setText("1");
        Icon switchB = new Icon(EXCHANGE, 12, "Swap content", this::switchChildren);
        Icon orienB = new Icon(MAGIC, 12, "Change orientation", this::toggleOrientation);
        maintain(c.orientation,o->o==VERTICAL ? ELLIPSIS_V : ELLIPSIS_H, orienB.icon);
        Icon closeB = new Icon(CLOSE, 12, "Close container", ()->closeAndDo(root, container::close));
        Icon coll1B = new Icon(ARROW_RIGHT, 10, "Collapse", this::toggleCollapsed1);
        maintain(c.orientation,o->o==VERTICAL ? ARROW_RIGHT : ARROW_DOWN, coll1B.icon);
        Icon coll2B = new Icon(ARROW_LEFT, 10, "Collapse", this::toggleCollapsed2);
        maintain(c.orientation,o->o==VERTICAL ? ARROW_LEFT : ARROW_UP, coll2B.icon);
        collapseBox.getChildren().setAll(coll1B,coll2B);
        controlsBox.getChildren().setAll(infoB,dragB,absB,collapseBox,lockB,orienB,switchB,closeB);
        
        hideControls();
    }
    
    public void setComponent(int i, Component c) {
        if(i!=1 && i!=2) throw new IllegalArgumentException("Only 1 or 2 supported as index.");
        
        AnchorPane r = i==1 ? root_child1 : root_child2;
        
        if (c == null) {
            r.getChildren().clear();
            return;
        }
        Node content = null;
        if (c instanceof Widget) content = c.load();
        else if (c instanceof Container) content = ((Container)c).load(r);
            
            
        r.getChildren().setAll(content);
        setAnchors(content,0);
    }
    
    public void setChild1(Component w) {
        setComponent(1, w);
    }
    public void setChild2(Component w) {
        setComponent(2, w);
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
    
    /**
     * Toggle orientation between vertical/horizontal.
     */
    @FXML
    public void toggleOrientation() {
        container.orientation.set(next(splitPane.getOrientation()));
    }
    
/*********************************** ABS SIZE *********************************/
    
    /**
     * Toggle fixed size on for different children and off.
     */
    @FXML
    public void toggleAbsoluteSize() {
        int i = getAbsoluteSize();
            i = i==2 ? 0 : i+1;
        setAbsoluteSize(i);
        
    }
    public void toggleAbsoluteSizeFor(int i) {
        setAbsoluteSize(getAbsoluteSize()==i ? 0 : i);
    }
    public void setAbsoluteSize(int i) {
        if(i<0 || i>2)
            throw new IllegalArgumentException("Only valiues 0,1,2 allowed here.");
        
        SplitPane.setResizableWithParent(root_child1, i!=1);
        SplitPane.setResizableWithParent(root_child2, i!=2);
        
        prop.put("abs_size", i);
        
        updateAbsB(container.getChildren().get(1));
        updateAbsB(container.getChildren().get(2));
    }
    public int getAbsoluteSize() {
        return prop.getI("abs_size");
    }
    
    private void updateAbsB(Component cmp) {
        if(cmp instanceof Container) {
            Container c = (Container)cmp;
            Component w = ((Container)cmp).getChildren().get(1);
            if(w instanceof Widget && c.getGraphics() instanceof Area) {
                ((Area)c.getGraphics()).controls.updateAbsB();
            }
        }
    }
    
/********************************** COLLAPSING ********************************/
    
    /**
     * Switch positions of the children
     */
    @FXML
    public void switchChildren() {
        container.switchCildren();
    }
    public void toggleCollapsed() {
        int c = getCollapsed();
            c = c==1 ? -1 : c+1;
        setCollapsed(c);
    }
    /** Collapse on/off to the left or top depending on the orientation. */
    public void toggleCollapsed1() {
        setCollapsed(isCollapsed() ? 0 : -1);
    }
    /** Collapse on/off to the right or bottom depending on the orientation. */
    public void toggleCollapsed2() {
        setCollapsed(isCollapsed() ? 0 : 1);
    }
    public boolean isCollapsed() {
        return getCollapsed() != 0;
    }
    public int getCollapsed() {
        return prop.getI("col");
    }
    
    private final ChangeListener<Orientation> orientListener = (o,ov,nv) -> {
        setupCollapsed(getCollapsed());
    };
    public void setCollapsed(int i) {
        prop.put("col", i);
        if(i==-1) {
            splitPane.orientationProperty().removeListener(orientListener);
            splitPane.orientationProperty().addListener(orientListener);
        } else if (i==0) {
            splitPane.orientationProperty().removeListener(orientListener);
        } else if (i==1) {
            splitPane.orientationProperty().removeListener(orientListener);
            splitPane.orientationProperty().addListener(orientListener);
        }
        setupCollapsed(i);
    }
    private void setupCollapsed(int i) {
        if(i==-1) {
            if(splitPane.getOrientation()==VERTICAL) {
                root_child1.setMaxHeight(0);
                root_child1.setMaxWidth(-1);
                root_child2.setMaxHeight(-1);
                root_child2.setMaxWidth(-1);
            } else {
                root_child1.setMaxHeight(-1);
                root_child1.setMaxWidth(0);
                root_child2.setMaxHeight(-1);
                root_child2.setMaxWidth(-1);
            }
        } else if (i==0) {
                root_child1.setMaxHeight(-1);
                root_child1.setMaxWidth(-1);
                root_child2.setMaxHeight(-1);
                root_child2.setMaxWidth(-1);
        } else if (i==1) {
            if(splitPane.getOrientation()==VERTICAL) {
                root_child1.setMaxHeight(-1);
                root_child1.setMaxWidth(-1);
                root_child2.setMaxHeight(0);
                root_child2.setMaxWidth(-1);
            } else {
                root_child1.setMaxHeight(-1);
                root_child1.setMaxWidth(-1);
                root_child2.setMaxHeight(-1);
                root_child2.setMaxWidth(0);
            }
        }
    }
    
    @FXML
    public void closeContainer() {
        closeAndDo(root, container::close);
    }
    
    
    public void showControls() {
        if (!GUI.isLayoutMode()) return;
        fadeIn.play();
        controls.getPane().setMouseTransparent(false);
    }
    
    public void hideControls() {
        fadeOut.play();
    }
    
    @FXML
    public void toggleLocked() {
        container.locked.set(!container.locked.get());
    }
    
    @Override
    public void show() {
//        showControls();
        splitPane.addEventFilter(MOUSE_MOVED,aaa);
    }

    @Override
    public void hide() {
//        hideControls();
        splitPane.removeEventFilter(MOUSE_MOVED,aaa);
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
    private void refreshControlsOrientation(Orientation o) {
        if(o == HORIZONTAL) {
            controlsBox.setOrientation(VERTICAL);
            collapseBox.setOrientation(HORIZONTAL);
        } else {
            controlsBox.setOrientation(HORIZONTAL);
            collapseBox.setOrientation(VERTICAL);
        }
    }
}