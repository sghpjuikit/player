
package sp.it.pl.layout.container.layout;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.File;
import java.util.Objects;
import java.util.UUID;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.layout.container.uncontainer.UniContainer;
import sp.it.pl.util.file.Util;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.dev.Fail.noNull;
import static sp.it.pl.util.file.UtilKt.getNameWithoutExtensionOrRoot;

public final class Layout extends UniContainer {

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
     * @param name name of this layout
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public Layout(String name) {
        setName(name);
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
     *
     * @param name name of this layout
     * @throws IllegalArgumentException if name parameter null or empty
     */
    public void setName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name of the layout must not be null or empty string.");
        this.name = name;
    }

    /**
     * Change name. This method immediately takes care of all file operations
     * needed to maintain consistency -saves layout to the new file, old
     * file will deleted, etc.
     *
     * @param new_name same as old one, empty or null will do nothing.
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
     * Loads or reloads layout. Its effectively equivalent to loading the root
 Container of this layout and assigning it to the parent node of this layout.
 Use to setParentRec layout or to update up to date.
     * @param parentPane root node to load the layout into.
     * @return root node of the this layout
     */
    @Override
    public Node load(AnchorPane parentPane) {
        noNull(parentPane);

        // load
        Node n = super.load(parentPane);
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
        APP.serializerXml.toXML(this, f);
    }

    /**
     * Deserializes layout from file according to application specifications.
     */
    public void deserialize() {
        deserialize(getFile());
    }

    public Layout deserialize(File f) {
        Layout l = APP.serializerXml.fromXML(Layout.class, f).getOrSupply(() -> new Layout(getNameWithoutExtensionOrRoot(f)));
        l.properties.forEach(properties::put);
        l.setName(name);    // TODO: dangerous
        child = l.child;

        return this;
    }

    private File getFile() {
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
        return o instanceof Layout && name.equals(((Layout)o).name);
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

    public static Layout openStandalone(AnchorPane root) {
        Layout l = new Layout();
        l.isStandalone = true;
        l.load(root);
        return l;
    }
}