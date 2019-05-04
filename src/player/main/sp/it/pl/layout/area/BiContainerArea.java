package sp.it.pl.layout.area;

import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.layout.AltState;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.util.access.V;
import sp.it.util.collections.map.PropertyMap;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_H;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MAGIC;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.access.Values.next;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.SubscriptionKt.on;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.reactive.UtilKt.sync1IfInScene;
import static sp.it.util.reactive.UtilKt.syncTo;
import static sp.it.util.ui.Util.setAnchor;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.pseudoclass;

public final class BiContainerArea extends ContainerNodeBase<BiContainer> {

    private static final PseudoClass COLLAPSED_PC = pseudoclass("collapsed");
    private static final double grabberSize = 20.0;

    private final AnchorPane root_child1 = new AnchorPane();
    private final AnchorPane root_child2 = new AnchorPane();
    private final SplitPane splitPane = new SplitPane(root_child1,root_child2);
    private final PropertyMap<String> prop;

    public BiContainerArea(BiContainer c) {
        super(c);

        setAnchor(root_, splitPane, 0d);
        splitPane.setMinSize(0,0);
        root_child1.setMinSize(0,0);
        root_child2.setMinSize(0,0);

        prop = c.properties;

        // setParentRec properties
        prop.getOrPut(Double.class, "pos", 0.5d);
        prop.getOrPut(Integer.class, "abs_size", 0); // 0 none, 1 child1, 2 child2
        prop.getOrPut(Integer.class, "col", 0);

        syncTo(container.orientation, splitPane.orientationProperty());
        setAbsoluteSize(prop.getI("abs_size"));
        setupCollapsed(getCollapsed());

        splitPane.setOnMouseClicked(root_.getOnMouseClicked());

        // initialize position
        sync1IfInScene(splitPane, runnable(() -> applyPos()));
        attach(getRoot().parentProperty(), consumer(it -> applyPos()));

        // maintain position in resize (SplitPane position is affected by distortion in case of small sizes)
        sync(splitPane.layoutBoundsProperty(), consumer(it -> {
            if (prop.getI("abs_size")==0)
                applyPos();
        }));

        V<Double> position = new V<>(prop.getD("pos"));

        // remember position for persistence
        splitPane.getDividers().get(0).positionProperty().addListener((o,ov,nv) -> {
            // only when the value changes manually - by user (hence the isPressed()).
            // This way initialization and rounding errors will never affect the value
            // and can not produce a domino effect. No matter how badly the value gets
            // distorted when applying to the layout, its stored value will be always exact.
            if (splitPane.isPressed())
                position.setValue(nv.doubleValue());
        });

        // apply/persist position (cant do this in positionProperty().addListener()) because it corrupts collapsed state restoration
        splitPane.addEventFilter(MOUSE_RELEASED, e -> {
            double v = position.getValue();
            if (v>0.01 && v<0.99) {
                // remember position (only when not collapsed to be able to restore)
                if (!isCollapsed())
                    prop.put("pos", v);
            } else {
                var collapsed = v<0.5 ? -1 : 1;
                if (e.getClickCount()==1 && getCollapsed()!=collapsed)
                    setCollapsed(collapsed);
            }
        });

        // maintain collapsed pseudoclass
        syncC(position, v -> splitPane.pseudoClassStateChanged(COLLAPSED_PC, v<0.01 || v>0.99));
    }

    @Override
    protected ContainerAreaControls buildControls() {
        var c = super.buildControls();

        Icon orientB = new Icon(MAGIC, -1, "Change orientation", this::toggleOrientation).styleclass("header-icon");
        on(syncC(container.orientation, it -> orientB.icon(it==VERTICAL ? ELLIPSIS_V : ELLIPSIS_H)), c.disposer);
        c.addExtraIcon(orientB);

        return c;
    }

    private void applyPos() {
        splitPane.getDividers().get(0).setPosition(prop.getD("pos"));
    }

    private Layouter layouter1, layouter2;
    private WidgetArea wa1, wa2;
    private ContainerNodeBase<?> ca1, ca2;

    public void setComponent(int i, Component c) {
        if (i!=1 && i!=2) throw new IllegalArgumentException("Only 1 or 2 supported as index.");

        AnchorPane r = i==1 ? root_child1 : root_child2;

        Node n;
        AltState as;
        if (c instanceof Widget) {
            var wa = new WidgetArea(container,i,(Widget)c);
            if (i==1) wa1 = wa; else wa2 = wa;
            n = wa.getRoot();
            as = wa;
        } else if (c instanceof Container) {
            n = ((Container)c).load(r);
            var caa = ((Container) c).ui;
            var ca = caa instanceof ContainerNodeBase<?> ? (ContainerNodeBase<?>) caa : null;
            if (i==1) ca1 = ca; else ca2 = ca;
            as = (Container) c;
        } else { // ==null
            var l = i==1 ? layouter1 : layouter2;
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

    public void toggleOrientation() {
        container.orientation.set(next(container.orientation.getValue()));
    }

    /**
     * Toggle fixed size on for different children and off.
     */
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
            throw new IllegalArgumentException("Only values 0,1,2 allowed here.");

        SplitPane.setResizableWithParent(root_child1, i!=1);
        SplitPane.setResizableWithParent(root_child2, i!=2);

        prop.put("abs_size", i);
        if (wa1!=null) wa1.getControls().updateAbsB();
        if (wa2!=null) wa2.getControls().updateAbsB();
        if (ca1!=null && ca1.controls.isSet()) ca1.controls.get().updateIcons();
        if (ca2!=null && ca2.controls.isSet()) ca2.controls.get().updateIcons();
    }

    public int getAbsoluteSize() {
        return prop.getI("abs_size");
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
    private final EventHandler<MouseEvent> decollapserClick = e -> {
        if (e.getClickCount()==2 && e.getButton()==PRIMARY && isCollapsed()) {
            var so = splitPane.getOrientation();
            var isGrabber =
                (so==VERTICAL && getCollapsed()==-1 && e.getY()<grabberSize) ||
                    (so==VERTICAL && getCollapsed()==1 && e.getY()>splitPane.getHeight()-grabberSize) ||
                    (so==HORIZONTAL && getCollapsed()==-1 && e.getX()<grabberSize) ||
                    (so==HORIZONTAL && getCollapsed()==1 && e.getX()>splitPane.getWidth()-grabberSize);

            if (isGrabber) {
                setCollapsed(0);
                applyPos();
            }
        }
    };
    private final EventHandler<MouseEvent> decollapserDrag = e -> {
        if (isCollapsed()) {
            var so = splitPane.getOrientation();
            var isGrabber =
                (so==VERTICAL && getCollapsed()==-1 && e.getY()<grabberSize) ||
                    (so==VERTICAL && getCollapsed()==1 && e.getY()>splitPane.getHeight()-grabberSize) ||
                    (so==HORIZONTAL && getCollapsed()==-1 && e.getX()<grabberSize) ||
                    (so==HORIZONTAL && getCollapsed()==1 && e.getX()>splitPane.getWidth()-grabberSize);

            if (isGrabber)
                setCollapsed(0);
        }
    };

    public void setCollapsed(int i) {
        prop.put("col", i);
        splitPane.orientationProperty().removeListener(orientL);
        if (i!=0) splitPane.orientationProperty().addListener(orientL);
        splitPane.removeEventFilter(MOUSE_RELEASED, decollapserClick);
        if (i!=0) splitPane.addEventFilter(MOUSE_RELEASED, decollapserClick);
        splitPane.removeEventFilter(MOUSE_DRAGGED, decollapserDrag);
        if (i!=0) splitPane.addEventFilter(MOUSE_DRAGGED, decollapserDrag);
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