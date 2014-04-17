
package Layout;

import GUI.Containers.TabbedWidgetArea;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author uranium
 */
public final class TabContainer extends Container {
    private final Map<Integer, Component> children = new HashMap<>();
    @XStreamOmitField
    private TabbedWidgetArea gui;
    
    @Override
    public Node load() {
        if (gui == null) gui = new TabbedWidgetArea(this);
        parent_pane.getChildren().setAll(gui);
        AnchorPane.setBottomAnchor(gui, 0.0);
        AnchorPane.setLeftAnchor(gui, 0.0);
        AnchorPane.setRightAnchor(gui, 0.0);
        AnchorPane.setTopAnchor(gui, 0.0);
        
        
        gui.clearTabs();
        getChildren().values().forEach(w->gui.loadWidget(w));
        return gui;
    }

    /**
     * @param index is ignored
     * @param w 
     */
    @Override
    public void addChild(int index, Component w) {
        if(w==null) {
            children.remove(index);
            gui.removeWidget(w);
        } else {
            if ((w instanceof Container)) ((Container)w).parent = this;
            children.put(index, w);
            gui.loadWidget(w);
        }
    }
    
    @Override
    public void show() {
//        super.show();
        gui.show();
    }

    @Override
    public void hide() {
//        super.hide();
        gui.hide();
    }    

    @Override
    public Map<Integer, Component> getChildren() {
        return children;
    }
}
