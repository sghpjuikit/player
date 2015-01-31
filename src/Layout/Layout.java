
package Layout;

import Serialization.Serializattion;
import Serialization.Serializes;
import Serialization.SerializesFile;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.File;
import java.util.Objects;
import java.util.UUID;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import main.App;
import util.Parser.File.FileUtil;

/**
 * @author uranium
 * 
 */
public final class Layout extends UniContainer implements Serializes, SerializesFile {
    @XStreamOmitField
    private String name;
    
    /**
     * Creates new layout with unique name. Use to create completely
     * new layouts.
     * <p>
     * @see #setName(java.lang.String)
     */
    public Layout() {
        this(uniqueName());
    }
    
    /**
     * Creates new layout with specified name. Please note that the name must
     * be unique. Use to create layout based on a name parameter, for example
     * when deserializing a file.
     * <p>
     * Do not use.
     * This method is not intended to be used outside of serialization context.
     * <p>
     * Note that creating new Layout with specified name is dangerous due to 
     * how {@link #equals(java.lang.Object)}
     * is implemented. For example creating layout with a name that of a different
     * layout which is currently active will result both of them being equal.
     * Because of that, application could think the new layout is active and loaded
     * while in fact, it isnt connected to any scene graph and has no children at
     * all.
     * 
     * @see #setName(java.lang.String)
     * @param _name 
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public Layout(String new_name) {
        setName(new_name);
        properties.initProperty(Boolean.class, "locked", false);
    }
    
    /** 
     * {@inheritDoc} 
     * @see #setName(java.lang.String)
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Sets name.
     * <p>
     * Do not use.
     * This method is not intended to be used outside of serialization context.
     * @param new_name - name to put.
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public void setName(String new_name) {
        if(new_name == null || new_name.isEmpty())
            throw new IllegalArgumentException("Name of the layout must not be null or empty string.");
        name = new_name;
    }
    
    /**
     * Change name. This method immediately takes care of all file operations
     * needed to maintain consistency -saves layout to the new file, old
     * file will deleted, etc
     * @param new_name. Same as old one, empty or null will do nothing.
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public void setNameAndSave(String new_name) {
        if(new_name == null || new_name.isEmpty())
            throw new IllegalArgumentException("Name of the layout must not be null or empty string.");
        // put name
        String old = name;
        name = new_name;
        // save new
        Serializattion.serializeLayout(this);
        // delete old file
        FileUtil.deleteFile(new File(App.LAYOUT_FOLDER(), old + ".l"));
        // rename thumb
        File thumb = new File(App.LAYOUT_FOLDER(), old + ".png");
        if (thumb.exists())
            thumb.renameTo(new File(App.LAYOUT_FOLDER(), name + ".png"));
    }
    
    /** 
     * Similar to {@link #isActive()} but this method returns true only if this
     * layout is active layout within layout aggregator of the main application
     * application window.
     * @return true if and only if layout is displayed on the screen as main tab */
    public boolean isMain() {
        return this == App.getWindow().getLayoutAggregator().getActive();
    }
    /** 
     * Returns whether this layout is active. Layout is active if its root is not
     * null and is attached to the scene graph - if the layout is loaded.
     * @return true if and only if layout is active.
     */
    public boolean isActive() {
        return LayoutManager.getLayouts().anyMatch(l->l.equals(this));
    }
    
    @Override
    public boolean isLocked() {
        return properties.getB("locked");
    }
    
    @Override
    public void setLocked(boolean val) {
        properties.put("locked", val);
    }
    
    /**
     * Get the thumbnail image. If not available it wil be attempted to create
     * a new one. It is only possible to create new thumbnail if the layout is
     * active;
     * @return file of the image of the layout preview or null if layout is not
     * active.
     */
    public File getThumbnail() {
        // get thumbnail file
        File file = new File(App.LAYOUT_FOLDER(), name + ".png");
        // if thumbnail not available attempt to make it
        if (!file.exists()) makeSnapshot();
        return (file.exists()) ? file : null;
    }
    
    /**
     * Takes preview/thumbnail/snapshot of the active layout and saves it as .png
     * under same name.
     * <p>
     * If the root is null this method is a no-op.
     */
    public void makeSnapshot() {
        if(root!=null) {
            WritableImage i = root.snapshot(new SnapshotParameters(),null);
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
        
        // load
        Node n = super.load(rootPane);
        initialize();
        return n;
    }    

    /**
     * Serializes layout into file according to application specifications.
     */
    public void serialize() {
       if(!getChildren().isEmpty())
           Serializattion.serializeLayout(this);
    }
    public void serialize(File f) {
       if(!getChildren().isEmpty())
           Serializattion.serialize(this,f);
    }
    /**
     * Deserializes layout from file according to application specifications.
     */
    public void deserialize() {
        deserialize(getFile());
    }
    public Layout deserialize(File f) {
        Layout l = Serializattion.deserializeLayout(f);
        l.properties.forEach(properties::put);
        child = l.child;
        return this;
    }
    
    /**
     * Get file this layout will sarialize as if not put otherwise. The file
     * should always be derived from the layouts name, Specifically name+".l".
     * 
     * @return 
     */
    @Deprecated
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
        if(this==o) return true; // this line can make a difference
        
        return ( o instanceof Layout && name.equals(((Layout)o).name));
    }

    @Override
    public int hashCode() {
        return 59 * 3 + Objects.hashCode(this.name);
    }
    
    /**
     * This implementation returns name of the layout.
     * @return name
     */
    @Override
    public String toString() {
        return name;
    }
    
    // create unique name
    private static String uniqueName() {
        return "Layout " + UUID.randomUUID().toString();
    }    
    
}