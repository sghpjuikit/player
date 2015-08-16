
package Layout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javafx.scene.Node;

import org.atteo.classindex.IndexSubclasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import main.App;
import util.collections.map.PropertyMap;

/**
 * @author uranium
 * 
 * Defines wrapper of loadable graphical component.
 * Basis for wrappers - containers or wrapped widgets.
 */
@IndexSubclasses()
public abstract class Component {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Component.class);
    private static final XStream X = App.INSTANCE.serialization.x;
    
    /** Unique ID. Permanent. Persists application life cycle. */
    public final UUID id = UUID.randomUUID();
    /** Simple storage. Persists application life cycle. */
    public final PropertyMap properties = new PropertyMap();

    
    /** @return name */
    abstract public String getName();
    
    /**
     * Loads the graphical element this container wraps.
     * @return root node of the loaded container
     */
    abstract public Node load();
    
    /** @return whether this component is currently open*/
    public boolean isOpen() {
        // check if this isnt standalone widget (not in a layout)
        if(WidgetManager.standaloneWidgets.contains(this)) return true;
        
        Component c = this;
        Component p = this;
        do {
            p = c instanceof Widget ? null : ((Container)c).getParent();
            if(p!=null) c = p;
        } while(p!=null);
        
        // top container is always layout
        return c instanceof Layout;
    }
    
    /** 
     * Creates a launcher for this component in given directory. Launcher is a
     * file, opening which by this application opens this component with its
     * current settings.
     */
    public void exportFxwl(File dir) {
        File f = new File(dir,getName() + ".fxwl");
        try {
            X.toXML(this, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            LOGGER.error("Unable to export component launcher for {} into {}", getName(),f);
        }
    }
}
