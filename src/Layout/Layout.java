
package Layout;

import Serialization.Serializator;
import Serialization.Serializes;
import Serialization.SerializesFile;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.File;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import main.App;
import utilities.FileUtil;

/**
 * @author uranium
 * 
 */
public class Layout extends UniContainer implements Serializes, SerializesFile {
    @XStreamOmitField
    private String name;
    
    public Layout() {
        this(uniqueName("new_layout"));
    }
    public Layout(String _name) {
        name = _name;
        properties.initProperty(Boolean.class, "locked", false);
    }
    
    @Override
    public String getName() {
        return name;
    }
    /**
     * @Note: Name of layout directly affects file it will serialize into. Avoid
     * this method if you want to avoid leaving out old file
     * @param new_name - name to set.
     */
    public void setName(String new_name) {
        name = new_name;
    }
    /**
     * Change name. This method immediately takes care of all file operations
     * needed to maintain consistency -saves layout to the new file, old
     * file will deleted, etc
     * @param _name. Same as old one, empty or null will do nothing.
     */
    public void setNameAndSave(String _name) {
        if (_name == null || _name.isEmpty() || _name.equals(name)) return;
        // set name
        String old = name;
        name = _name;
        // save new
        Serializator.serialize(this);
        // delete old file
        FileUtil.deleteFile(new File(App.LAYOUT_FOLDER()+File.separator+old+".l"));
        // rename thumb
        File thumb = new File(App.LAYOUT_FOLDER() + File.separator + old + ".png");
        if (thumb.exists())
            thumb.renameTo(new File(App.LAYOUT_FOLDER() + File.separator + name + ".png"));
    }
    
    /** @return true if and only if layout is displayed on the screen as main tab */
    public boolean isMain() {
        return LayoutManager.active.get(0).equals(this);
    }
    /** @return true if and only if layout is loaded within the aplication */
    public boolean isActive() {
        return LayoutManager.active.containsValue(this);
    }
    public void setActive() {
        LayoutManager.changeActiveLayout(this);
    }
    public boolean isLocked() {
        return properties.getB("locked");
    }
    public void setLocked(boolean val) {
        properties.set("locked", val);
    }
    
    /**
     * @return file of the image of the layout preview or null if none exists.
     */
    public File getThumbnail() {
        File file = new File(App.LAYOUT_FOLDER() + File.separator + name + ".png");
        if (!file.exists() && isMain()) LayoutManager.makeSnapshot();
        return file;
    }
    
    /**
     * Takes preview/thumbnail/snapshot of the active layout and saves it as .png
     * under same name.
     */
    public void makeSnapshot() {
        if(parent_pane!=null) {
            WritableImage i = parent_pane.snapshot(new SnapshotParameters(),null);
            File out = new File(App.LAYOUT_FOLDER(),name + ".png");
            FileUtil.writeImage(i, out);
        }
    } 
    
    /**
     * Loads or reloads layout. Its effectively equivalent to loading the root
     * Container of this layout and assigning it to the parent node of this layout.
     * Use to initialize layout or to update up to date.
     * @param rootPane root node to load the layout into.
     * @return root node of the this layout
     */
    @Override
    public Node load(AnchorPane rootPane) {
        Objects.requireNonNull(rootPane);
        
        try {
            getAllWidgets().forEach(w->w.getController().OnClosing());
            System.out.println("widgets reloaded");;
        }catch(NullPointerException e) {
            // do nothing
            System.out.println("not initialized ");
        }
        
        Node n = super.load(rootPane);
        initialize();
        return n;
    }    
    

     /**
      * Serializes layout into file according to application specifications.
      */
     public void serialize() {                                                  // Log.deb("serializing"+name);
        if(!getChildren().isEmpty())
            Serializator.serialize(this);
     }
     /**
      * Deserializes layout from file according to application specifications.
      */
     public void deserialize() {                                                // Log.deb("deserializing"+name);
         Layout l = Serializator.deserializeLayout(getFile());
         l.properties.getMap().forEach(properties::set);
//         l.getChildren().forEach(this::addChild);
         child = l.child;
     }
     
     public File getFile() {
         return new File(App.LAYOUT_FOLDER(),name+".l");
     }
     
     /**
      * Removes files associated with this layout. Layout lives on in the
      * application.
      */
     public void removeFile() {
        FileUtil.deleteFile(getFile());
        FileUtil.deleteFile(getThumbnail());
     }
     
     /** @return true if and only if two layouts share the same name. */
    @Override
     public boolean equals(Object o) {
         if (this==o) return true;
         if ( o instanceof Layout) return name.equals(((Layout)o).name);
         return false;
     }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.name);
        return hash;
    }
    /**
     * This implementation returns name of the layout.
     * @return name
     */
    @Override
    public String toString() {
        return name;
    }
    
    // avoid name collisions (it would overwrite files)
    private static String uniqueName(String unique_name) {
        String out = unique_name;
        int i = 0;
        while (LayoutManager.layouts.contains(out)){
            out = unique_name + String.valueOf(++i);
        }
        return out;
    }    
    
}