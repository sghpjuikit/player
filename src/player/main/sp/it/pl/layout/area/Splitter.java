package sp.it.pl.layout.area;

import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.layout.AltState;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.util.access.V;
import sp.it.util.collections.map.PropertyMap;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_UP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_H;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MAGIC;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.access.SequentialValue.next;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.reactive.UtilKt.syncTo;
import static sp.it.util.ui.Util.setAnchor;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.pseudoclass;

public final class Splitter extends ContainerNodeBase<BiContainer> {

    private static final PseudoClass COLLAPSED_PC = pseudoclass("collapsed");

    private final AnchorPane root_child1 = new AnchorPane();
    private final AnchorPane root_child2 = new AnchorPane();
    private final SplitPane splitPane = new SplitPane(root_child1,root_child2);

    private final PropertyMap<String> prop;
    private boolean initialized = false;

    private void applyPos() {
        splitPane.getDividers().get(0).setPosition(prop.getD("pos"));
    }

    public Splitter(BiContainer c) {
        super(c);

        setAnchor(root, splitPane,0d);
        splitPane.setMinSize(0,0);
        root_child1.setMinSize(0,0);
        root_child2.setMinSize(0,0);

        prop = c.properties;

        Icon orienB = new Icon(MAGIC, 12, "Change orientation", this::toggleOrientation);
        maintain(c.orientation, it -> orienB.icon(it==VERTICAL ? ELLIPSIS_V : ELLIPSIS_H));
        icons.getChildren().add(2,orienB);

        Icon coll1B = new Icon(ARROW_RIGHT, 10, "Collapse", this::toggleCollapsed1);
        maintain(c.orientation, it -> coll1B.icon(it==HORIZONTAL ? ARROW_RIGHT : ARROW_DOWN));
        Icon coll2B = new Icon(ARROW_LEFT, 10, "Collapse", this::toggleCollapsed2);
        maintain(c.orientation, it -> coll2B.icon(it==HORIZONTAL ? ARROW_LEFT : ARROW_UP));

        // setParentRec properties
        prop.getOrPut(Double.class, "pos", 0.5d);
        prop.getOrPut(Integer.class, "abs_size", 0); // 0 none, 1 child1, 2 child2
        prop.getOrPut(Integer.class, "col", 0);

        // maintain proper orientation
        syncTo(container.orientation, splitPane.orientationProperty());
        // put properties
        setAbsoluteSize(prop.getI("abs_size"));
        applyPos();
        setupCollapsed(getCollapsed());

        // activate animation if mouse if leaving area
        splitPane.setOnMouseClicked(root.getOnMouseClicked());

        V<Double> position = new V<>(splitPane.getDividers().get(0).getPosition());
        splitPane.setOnMouseReleased(e -> {
            double v = position.getValue();
            if (v>0.01 && v<0.99)
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
            if (getCollapsed()!=0) setCollapsed(0);
        });
        root_child1.setOnMouseDragged(Event::consume);
        root_child2.setOnMouseDragged(Event::consume);

        splitPane.addEventHandler(MOUSE_CLICKED, e -> {
            boolean dividerclicked = false;
            if (splitPane.getOrientation()==VERTICAL && (e.getY()<5 || e.getY()>splitPane.getHeight()-5)) dividerclicked = true;
            if (splitPane.getOrientation()==HORIZONTAL && (e.getX()<5 || e.getX()>splitPane.getWidth()-5)) dividerclicked = true;
            if (e.getClickCount()==2 && isCollapsed()) {
                setCollapsed(0);
            }
        });

        splitPane.getDividers().get(0).positionProperty().addListener((o,ov,nv) -> {
            double v = nv.doubleValue();

            // occurs when user drags the divider, manual -> remember it
            if (splitPane.isPressed()) {
                if (v<0.01) v=0;
                if (v>0.99) v=1;
                // It is important we do this only when the value changes manually - by user (hence
                // the isPressed()).
                // This way initialization and rounding errors will never affect the value
                // and can not produce a domino effect. No matter how badly the value gets
                // distorted when applying to the layout, its stored value will be always exact.
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
                    if (v<p-0.08 || v>p+0.08)
                        runFX(this::applyPos);
                    else
                        runFX(millis(5000), () -> initialized=true);
                }
            }

            if (v<0.01) v=0;
            if (v>0.99) v=1;
            // maintain collapsed pseudoclass
            splitPane.pseudoClassStateChanged(COLLAPSED_PC, v==0 || v==1);
        });
    }

    private Layouter layouter1, layouter2;
    private WidgetArea wa1, wa2;

    public void setComponent(int i, Component c) {
        if (i!=1 && i!=2) throw new IllegalArgumentException("Only 1 or 2 supported as index.");

        AnchorPane r = i==1 ? root_child1 : root_child2;

        Node n;
        AltState as;
        if (c instanceof Widget) {
            WidgetArea wa = new WidgetArea(container,i,(Widget)c);
            if (i==1) wa1 = wa; else wa2 = wa;
            n = wa.getRoot();
            as = wa;
        } else if (c instanceof Container) {
            n = ((Container)c).load(r);
            as = (Container)c;
        } else { // ==null
            Layouter l = i==1 ? layouter1 : layouter2;
            if (l==null) l = new Layouter(container, i);
            if (i==1) layouter1 = l; else layouter2 = l;
            n = l.getRoot();
            as = l;
        }
        if (APP.ui.isLayoutMode()) as.show();

        r.getChildren().setAll(n);
        setAnchors(n, 0d);
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
        if (i<0 || i>2)
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
        if (wa1!=null) wa1.controls.updateAbsB();
        if (wa2!=null) wa2.controls.updateAbsB();
    }

/********************************** COLLAPSING ********************************/

    /**
     * Switch positions of the children
     */
    @FXML
    public void switchChildren() {
        container.switchChildren();
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
        if (i==-1) {
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
        if (i==-1) {
            if (splitPane.getOrientation()==VERTICAL) {
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
            if (splitPane.getOrientation()==VERTICAL) {
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

    @Override
    public void show() {
        super.show();
        if (layouter1!=null) layouter1.show();
        if (layouter2!=null) layouter2.show();
    }

    @Override
    public void hide() {
        super.hide();
        if (layouter1!=null) layouter1.hide();
        if (layouter2!=null) layouter2.hide();
    }

}