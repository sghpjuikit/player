
package sp.it.pl.layout.container;

import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.Layouter;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetUi;
import static java.util.Collections.singletonMap;
import static sp.it.util.ui.Util.setAnchors;


/**
 * Implementation of {@link Container Container} storing one child component. It
 * is expected the child will be non-Container component as putting any container
 * within this would turn this into unnecessary intermediary.
 */
public class UniContainer extends Container<ContainerUi> {

    protected Component child;
    public boolean isStandalone = false;    // !need to be serialized as standalone widgets die with app

    public UniContainer() {}

    public UniContainer(AnchorPane root_pane) {
        root = root_pane;
    }

    @Override
    public Node load() {
        Node out;

        if (child instanceof Container) {
            removeGraphicsFromSceneGraph();
            ui = null;
            out = Container.class.cast(child).load(root);
        } else
        if (child instanceof Widget) {
            if (!(ui instanceof WidgetUi)) {
                removeGraphicsFromSceneGraph();
                ui = new WidgetUi(this,1,(Widget)child);
	            if (isStandalone) ((WidgetUi) ui).setStandaloneStyle();
            }
            out = ui.getRoot();
        } else {
            ui = new Layouter(this,1);
            out = ui.getRoot();
        }

        root.getChildren().setAll(out);
        setAnchors(out,0d);

        return out;
    }


    @Override
    public Map<Integer, Component> getChildren() {
        return singletonMap(1, child);
    }

    /**
     * Convenience method. Equal to getChildreg.get(1). It is
     * recommended to use this method if standard Map format is not necessary.
     * @return child or null if none present.
     */
    public Component getChild() {
        return child;
    }

    /**
     * {@inheritDoc}
     * This implementation considers all index values valid, except for null,
     * which will be ignored.
     */
    @Override
    public void addChild(Integer index, Component c) {
        if (index==null) return;

        if (c instanceof Container) ((Container)c).setParent(this);
        child = c;
        load();
        setParentRec();
    }

    /**
     * Convenience method. Equal to addChild(1, w);
     * @param w
     */
    public void setChild(Component w) {
        addChild(1, w);
    }

    @Override
    public Integer indexOf(Component c) {
        return c==child ? 1 : null;
    }

    @Override
    public Integer getEmptySpot() {
        return child==null ? 1 : null;
    }
}