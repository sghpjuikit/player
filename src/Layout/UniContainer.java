
package Layout;

import GUI.Components.Layouter;
import GUI.Containers.WidgetArea;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;


/**
 * @author uranium
 * 
 * Container with one child.
 */
public class UniContainer extends Container {
    Component child;
    @XStreamOmitField
    private WidgetArea gui;
 
    public UniContainer() {
        gui = new WidgetArea(this);
    }
    public UniContainer(AnchorPane _parent) {
        parent_pane = _parent;
        gui = new WidgetArea(this);
    }
    
    @Override
    public Node load() {
        // lazy load fields
        if (gui == null) gui = new WidgetArea(this);
        
        // although Layouter is legit Widget, dont add it as child, it would wrap
        // it in Widget Gui Container which is not desirable
        
        Node out;
        if (child == null)
            out = new Layouter(this).load();
        else
        if (child instanceof Container)
            out = ((Container)child).load(parent_pane);
        else
        if (child instanceof Widget) {
            gui.loadWidget((Widget)child);
            out = gui.getPane();
        }
        else out = new Layouter(this).load();
        
        parent_pane.getChildren().setAll(out);
        AnchorPane.setBottomAnchor(out, 0.0);
        AnchorPane.setLeftAnchor(out, 0.0);
        AnchorPane.setRightAnchor(out, 0.0);
        AnchorPane.setTopAnchor(out, 0.0);
        return out;
    }
    
    
    @Override
    public Map<Integer, Component> getChildren() {
        // override with more effective implementation
        return Collections.singletonMap(1, child);
    }
    
    /**
     * Returns child. Convenience method. Equal to getChildreg.get(1). It is
     * recommended to use this method if standard Map format is not necessary.
     * @return child or null if none present.
     */
    public Component getChild() {
        return child;
    }
    
    /**
     * Adds the widget as child.
     * Since there is only one child, the index parameter is ignored.
     * @param w widget or container. Null value will clear content.
     * @param index must be of value 1. Other value will be ignored
     */
    @Override
    public void addChild(int index, Component w) {
        if (index != 1) return;
        if (w instanceof Container)
            ((Container)w).parent = this;
        child = w;
        load();
    }
    
    /**
     * Adds child to this container. Convenience method. Equal to addChild(1, w);
     * @param w 
     */
    public void setChild(Component w) {
        addChild(1, w);
    }
    
    @Override
    public int indexOf(Component c) {
        if (Objects.equals(c, child)) return 1;
        else return -1;
    }
    
    @Override
    public void show() {
        super.show();
        gui.show();
    }
    @Override
    public void hide() {
        super.hide();
        gui.hide();
    }
}