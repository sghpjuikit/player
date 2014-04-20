
package Layout;

import Layout.Containers.TabArea;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * Implementation of {@link Container Container} containing multiple components.
 * <p>
 * @author uranium
 */
public final class PolyContainer extends Container {
    
    private final Map<Integer, Component> children = new HashMap<>();
    @XStreamOmitField
    private TabArea gui;
    
    /** {@inheritDoc} */
    @Override
    public Node load() {
        // lazy-init gui
        if (gui == null) gui = new TabArea(this);
        
        // load gui
        parent_pane.getChildren().setAll(gui.root);
        AnchorPane.setBottomAnchor(gui.root, 0.0);
        AnchorPane.setLeftAnchor(gui.root, 0.0);
        AnchorPane.setRightAnchor(gui.root, 0.0);
        AnchorPane.setTopAnchor(gui.root, 0.0);
        
        gui.removeAllComponents();
        getChildren().values().forEach(w->gui.addComponent(w));
        gui.showComponentAt(properties.getI("selected"));
        return gui.root;
    }

    /** {@inheritDoc} */
    @Override
    public void addChild(Integer index, Component c) {
        if(index==null) return;
        if(c==null) {
            children.remove(index);
            gui.removeComponent(c);
        } else {
            if ((c instanceof Container)) ((Container)c).parent = this;
            children.put(index, c);
            gui.addComponent(c);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void show() {
        super.show();
        gui.show();
    }
    
    /** {@inheritDoc} */
    @Override
    public void hide() {
        super.hide();
        gui.hide();
    }  
    
    /** {@inheritDoc} */
    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }
}
