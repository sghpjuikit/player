
package layout.container.layout;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import layout.container.uncontainer.UniContainer;
import main.App;
import main.AppSerializer;
import main.AppSerializer.SerializationException;
import util.file.Util;

import static main.App.APP;

/**
 * @author Martin Polakovic
 */
public final class Layout extends UniContainer {

    private static final AppSerializer X = App.APP.serializators;
    private static final Logger LOGGER = LoggerFactory.getLogger(Layout.class);

    @XStreamOmitField
    private String name;

    /**
     * Creates new layout with unique name. Use to create completely
     * new layouts.
     * <p/>
     * @see #setName(java.lang.String)
     */
    public Layout() {
        this(uniqueName());
    }

    /**
     * Creates new layout with specified name. Please note that the name must
     * be unique. Use to create layout based on a name parameter, for example
     * when deserializing a file.
     * <p/>
     * Do not use.
     * This method is not intended to be used outside of serialization context.
     * <p/>
     * Note that creating new Layout with specified name is dangerous due to
     * how {@link #equals(java.lang.Object)}
     * is implemented. For example creating layout with a name that of a different
     * layout which is currently active will result both of them being equal.
     * Because of that, application could think the new layout is active and loaded
     * while in fact, it is not connected to any scene graph and has no children at
     * all.
     *
     * @see #setName(java.lang.String)
     * @param _name
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public Layout(String new_name) {
        setName(new_name);
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
     * <p/>
     * Do not use.
     * This method is not intended to be used outside of serialization context.
     * @param new_name - name to put.
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public void setName(String new_name) {
        if (new_name == null || new_name.isEmpty())
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
        if (new_name == null || new_name.isEmpty())
            throw new IllegalArgumentException("Name of the layout must not be null or empty string.");
        name = new_name;
        // save new
        serialize();
    }

    /**
     * Similar to {@link #isActive()} but this method returns true only if this
     * layout is active layout within layout aggregator of the main application
     * application window.
     * @return true if and only if layout is displayed on the screen as main tab
     */
    @Deprecated // remove this
    public boolean isMain() {
        return this == APP.window.getLayout();
    }

    /**
     * Loads or reloads layout. Its effectively equivalent to loading the root
 Container of this layout and assigning it to the parent node of this layout.
 Use to setParentRec layout or to update up to date.
     * @param rootPane root node to load the layout into.
     * @return root node of the this layout
     */
    @Override
    public Node load(AnchorPane rootPane) {
        Objects.requireNonNull(rootPane);

        // load
        Node n = super.load(rootPane);
        setParentRec();
        return n;
    }

    /**
     * Serializes layout into file according to application specifications.
     */
    public void serialize() {
        serialize(getFile());
    }

    public void serialize(File f) {
        if (getChild() == null) return;

        try {
            X.toXML(this,f);
        } catch (SerializationException e) {
            LOGGER.error("Unable to save gui layout '{}' into the file {}. {}", name,f,e);
        }
    }

    /**
     * Deserializes layout from file according to application specifications.
     */
    public void deserialize() {
        deserialize(getFile());
    }

    public Layout deserialize(File f) {
        Layout l;

        try {
            l = X.fromXML(Layout.class,f);
            l.setName(Util.getName(f)); // hmm
        } catch (SerializationException e) {
            LOGGER.error("Unable to deserialize layout from {}. {}", f,e);
            l = new Layout(Util.getName(f));
        }

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
        return new File(APP.DIR_LAYOUTS,name + ".l");
    }

    /**
     * Removes files associated with this layout. Layout lives on in the
     * application.
     */
    public void removeFile() {
       Util.deleteFile(getFile());
    }

     /** @return true if and only if two layouts share the same name. */
    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
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
        return "layout " + UUID.randomUUID().toString();
    }

}