
package Layout;

import GUI.Components.Layouter;
import Layout.Containers.WidgetArea;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;


/**
 * 
 * Implementation of {@link Container Container} storing one child component. It
 * is expected the child will be non-Container component as putting any container
 * within this would turn this into unnecessary intermediary.
 * <p>
 * @author uranium
 */
public class UniContainer extends Container {
    Component child;
    @XStreamOmitField
    private WidgetArea gui;
 
    public UniContainer() {
    }
    public UniContainer(AnchorPane _parent) {
        parent_pane = _parent;
    }
    
    @Override
    public Node load() {
        // lazy load fields
        if (gui == null) gui = new WidgetArea(this);
        
        // although Layouter is legit Widget, dont add it as child, it would wrap
        // it in Widget Gui Container which is not desirable
        
        Node out;
        if (child == null)
            out = new Layouter(this,1).load();
        else
        if (child instanceof Container)
            out = ((Container)child).load(parent_pane);
        else
        if (child instanceof Widget) {
            gui.loadWidget((Widget)child);
            out = gui.root;
        }
        else out = new Layouter(this,1).load();
        
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
        super.addChild(index, c);
        if(index==null) return;
        if (c instanceof Container)
            ((Container)c).parent = this;
        child = c;
        load();
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
        if (Objects.equals(c, child)) return 1;
        else return null;
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

    @Override
    public void close() {
        super.close();
        
        setChild(null);     // have to call this or the gui change wont
                            // take effect, now i have to override the method...
                            // it wouldnt hurt figuring this out
    }
    
}