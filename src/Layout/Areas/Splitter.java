
package Layout.Areas;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;

import Layout.Component;
import Layout.container.Container;
import Layout.container.bicontainer.BiContainer;
import Layout.widget.Widget;
import gui.GUI;
import gui.objects.icon.Icon;
import util.access.Ѵ;
import util.collections.map.PropertyMap;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static gui.GUI.closeAndDo;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static util.access.SequentialValue.next;
import static util.async.Async.run;
import static util.async.Async.runFX;
import static util.graphics.Util.layAnchor;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 * @author uranium
 */
public final class Splitter extends ContainerNodeBase<BiContainer> {

    private static final PseudoClass COLLAPSED_PC = getPseudoClass("collapsed");

    AnchorPane rt = new AnchorPane();
    @FXML AnchorPane root_child1;
    @FXML AnchorPane root_child2;
    @FXML SplitPane splitPane;

    private final PropertyMap prop;
    private boolean initialized = false;

    private void applyPos() {
        splitPane.getDividers().get(0).setPosition(prop.getD("pos"));
    }

    public Splitter(BiContainer c) {
        super(c);
        layAnchor(root, rt,0d);
        prop = c.properties;

        Icon orienB = new Icon(MAGIC, 12, "Change orientation", this::toggleOrientation);
        maintain(c.orientation,o->o==VERTICAL ? ELLIPSIS_V : ELLIPSIS_H, orienB::icon);
        icons.getChildren().add(2,orienB);

        Icon coll1B = new Icon(ARROW_RIGHT, 10, "Collapse", this::toggleCollapsed1);
        maintain(c.orientation,o -> o==HORIZONTAL ? ARROW_RIGHT : ARROW_DOWN, coll1B::icon);
        Icon coll2B = new Icon(ARROW_LEFT, 10, "Collapse", this::toggleCollapsed2);
        maintain(c.orientation,o -> o==HORIZONTAL ? ARROW_LEFT : ARROW_UP, coll2B::icon);

        // load graphics
        FXMLLoader fxmlLoader = new FXMLLoader(Splitter.class.getResource("Splitter.fxml"));
        fxmlLoader.setRoot(rt);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        new ConventionFxmlLoader(Splitter.class, rt,this)

        // setParentRec properties
        prop.initProperty(Double.class, "pos", 0.5d);
        prop.initProperty(Integer.class, "abs_size", 0); // 0 none, 1 child1, 2 child2
        prop.initProperty(Integer.class, "col", 0);

        // maintain proper orientation
        maintain(container.orientation, splitPane.orientationProperty());
        // put properties
        setAbsoluteSize(prop.getI("abs_size"));
        applyPos();
        setupCollapsed(getCollapsed());

        // activate animation if mouse if leaving area
        splitPane.setOnMouseClicked(root.getOnMouseClicked());

        Ѵ<Double> position = new Ѵ<>(splitPane.getDividers().get(0).getPosition());
        splitPane.setOnMouseReleased(e -> {
            double v = position.getValue();
            if(v>0.01 && v<0.99)
                // Remember non collapsed value
                // This works so if user drags slider to the edge and causes collapse, no value
                // is remembered. Then when user decollapses we can restore last value.
                prop.put("pos", v);
            else {
                setCollapsed(v<0.5 ? -1 : 1);
            }
        });
        // decollapse on user drag
        // to make sure the above drag executes only on divider drag, we consume drag on content
        splitPane.setOnMouseDragged(e -> {
            if(getCollapsed()!=0) setCollapsed(0);
        });
        root_child1.setOnMouseDragged(Event::consume);
        root_child2.setOnMouseDragged(Event::consume);

        splitPane.addEventHandler(MOUSE_CLICKED, e -> {
            System.out.println("click " + e.getClickCount());
            boolean dividerclicked = false;
            if(splitPane.getOrientation()==VERTICAL && (e.getY()<5 || e.getY()>splitPane.getHeight()-5)) dividerclicked = true;
            if(splitPane.getOrientation()==HORIZONTAL && (e.getX()<5 || e.getX()>splitPane.getWidth()-5)) dividerclicked = true;System.out.println(dividerclicked);
            if(e.getClickCount()==2 && isCollapsed()) {System.out.println("decoll");
                setCollapsed(0);
            }
        });

        splitPane.getDividers().get(0).positionProperty().addListener((o,ov,nv) -> {
            double v = nv.doubleValue();

            // occurs when user drags the divider, manual -> remember it
            if(splitPane.isPressed()) {
                if(v<0.01) v=0;
                if(v>0.99) v=1;
                position.setValue(v);
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
                    if(v<p-0.08||v>p+0.08)
                        runFX(this::applyPos);
                    else
                        run(5000, ()->initialized=true);
                }
            }

            if(v<0.01) v=0;
            if(v>0.99) v=1;
            // maintain collapsed pseudoclass
            splitPane.pseudoClassStateChanged(COLLAPSED_PC, v==0 || v==1);
        });
    }

    Layouter layouter1,layouter2;
    WidgetArea wa1,wa2;

    public void setComponent(int i, Component c) {
        if(i!=1 && i!=2) throw new IllegalArgumentException("Only 1 or 2 supported as index.");

        AnchorPane r = i==1 ? root_child1 : root_child2;

        Node n = null;
        if (c instanceof Widget) {
            WidgetArea wa = new WidgetArea(container, i);
            wa.loadWidget((Widget)c);
            if(i==1) wa1 = wa; else wa2 = wa;
            n = wa.root;
        } else if (c instanceof Container) {
            n = ((Container)c).load(r);
        } else {
            Layouter l = i==1 ? layouter1 : layouter2;
            if(l==null) l = new Layouter(container, i);
            if(i==1) layouter1 = l; else layouter2 = l;
            if(GUI.isLayoutMode()) l.show();
            n = l.getRoot();
        }

        r.getChildren().setAll(n);
        setAnchors(n,0d);
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
        updateAbsB();
    }

    public int getAbsoluteSize() {
        return prop.getI("abs_size");
    }

    @Override
    protected void updateAbsB() {
        super.updateAbsB();
        if(wa1!=null) wa1.controls.updateAbsB();
        if(wa2!=null) wa2.controls.updateAbsB();
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

    private final ChangeListener<Orientation> orientL = (o,ov,nv) -> setupCollapsed(getCollapsed());
    public void setCollapsed(int i) {
        prop.put("col", i);
        if(i==-1) {
            splitPane.orientationProperty().removeListener(orientL);
            splitPane.orientationProperty().addListener(orientL);
        } else if (i==0) {
            splitPane.orientationProperty().removeListener(orientL);
        } else if (i==1) {
            splitPane.orientationProperty().removeListener(orientL);
            splitPane.orientationProperty().addListener(orientL);
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
        closeAndDo(rt, container::close);
    }

    @FXML
    public void toggleLocked() {
        container.locked.set(!container.locked.get());
    }

    @Override
    public void show() {
        super.show();
        if(layouter1!=null) layouter1.show();
        if(layouter2!=null) layouter2.show();
    }

    @Override
    public void hide() {
        super.hide();
        if(layouter1!=null) layouter1.hide();
        if(layouter2!=null) layouter2.hide();
    }

}