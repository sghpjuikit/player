
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.objects.ContextMenu;
import GUI.objects.PopOver.PopOver;
import GUI.objects.SimpleConfigurator;
import GUI.objects.VerticalContextMenu;
import Layout.Container;
import Layout.Layout;
import Layout.LayoutManager;
import Layout.PolyContainer;
import Layout.UniContainer;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.Widget_Source;
import Library.BookmarkItem;
import Library.BookmarkManager;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COGS;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import static javafx.stage.WindowEvent.WINDOW_HIDING;
import main.App;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;

/**
 *
 * @author uranium
 */
@IsConfigurable
public final class ContextManager {
    
    @IsConfig(name="Layout mode on for menus", info="Transition into layout mode when context menus open.")
    public static boolean transitForMenu = true;
    @IsConfig(name="Close menu on action", info="Close context menus when item on menus is chosen.")
    public static boolean closeMenuOnAction = true;
    @IsConfig(name="Allow multiple open menus", info="Allow multiple open context menus simultaneously."
            + " If false previously open menus will be closed when new menu is open.")
    public static boolean allowMultipleMenus = true;
    @IsConfig(name="Context menu item type", info = "Preffered context menu item type.")
    public static ContextMenu.ElementType contextMenuItemType = ContextMenu.ElementType.CIRCLE;
    
    private static double X;
    private static double Y;
    /** Context menus. */
    public static AnchorPane contextPane;
    public static AnchorPane overlayPane;
    
    //menus
    public static ContextMenu playlistMenu;
    public static ContextMenu bookmarkMenu;
    public static ContextMenu widgetsMenu;
    public static ContextMenu imageMenu;
    public static ContextMenu imageFileMenu;
    
    private static final List<ContextMenu> menus = new ArrayList<>();
    private static boolean menu_open = false;
    
    
    public ContextManager(AnchorPane o, AnchorPane c) {
        contextPane = c;
        overlayPane = o;
        
        playlistMenu = makePlaylistContextMenu();
        bookmarkMenu = makeBookmarkCM();
        widgetsMenu = makeWidgetContextMenu();
        imageMenu = makeImageContextMenu();
        imageFileMenu = makeImageFileContextMenu();   
    }

    /** Set last mouse click x coordinate. */
    static void setX(double x) {
        X = x;
    }
    /** Set last mouse click y coordinate. */
    static void setY(double y) {
        Y = y;
    }
    /** Get last mouse click x coordinate. */
    public static double getX() {
        return Window.getFocused().getX()+X;
    }
    /** Get last mouse click y coordinate. */
    public static double getY() {
        return Window.getFocused().getY()+Y;
    }
/******************************************************************************/
    
    public static final ArrayList<Window> windows = new ArrayList();
    
    /** 
     * @param widget widget to open, does nothing when null.
     * @throws NullPointerException if param null
     */
    public static Window showWindow(Widget widget) {
        Objects.requireNonNull(widget);
        
        Window w = Window.create();
        w.setTitle(widget.getName());
        w.setContent(widget);
        w.show();
        w.setLocationCenter();
        
        return w;
    }
    
    public static PopOver showFloating(Widget w) {
        Objects.requireNonNull(w);
        
        // build popup content
        Label propB = AwesomeDude.createIconLabel(COGS,"","12","12",CENTER);
              propB.setTooltip(new Tooltip("Settings"));
              propB.setOnMouseClicked( e -> {
                  SimpleConfigurator c = new SimpleConfigurator(w,()->w.getController().refresh());
                  PopOver ph = new PopOver();
                          ph.setContentNode(c);
                          ph.setTitle(w.getName() + " Settings");
                          ph.setAutoFix(false);
                          ph.setAutoHide(true);
                          ph.show(propB);
                  e.consume();
              });
        // build popup
        PopOver p = new PopOver(w.load());
                p.setTitle(w.name);
                p.setAutoFix(false);
                p.getHeaderIcons().addAll(propB);
                p.show(Window.getFocused().getStage(),getX(),getY());
                // unregister the widget from active eidgets manually
                p.addEventFilter(WINDOW_HIDING, we -> WidgetManager.standaloneWidgets.remove(w));
        return p;
    }
    
    public static PopOver showFloating(Node content, String title) {
        Objects.requireNonNull(content);
        Objects.requireNonNull(title);  // we could use null, but disallow
        
        PopOver p = new PopOver(content);
                p.setTitle(title);
                p.setAutoFix(false);
                p.show(Window.getFocused().getStage(),getX(),getY());
        return p;
    }
    
    /**
     * @param exception optional parameter. Specified window will not be closed.
     * Use if running this method from concrete window that shouldnt close as a
     * result of this method. Generally the parameter is active window with
     * autoClose property set to true. In other case, the parameter doesnt serve
     * a purpose.
     */
    public static void closeFloatingWindows(Window exception) {
        List<Window> rem = new ArrayList<>(windows); // tmp list to avoid concurrent modific. error
        rem.forEach(w-> { 
            if(w!=exception)
                w.closeWeak();// windows auto-remove themselves on close
        });
        rem.clear();
    }

    
/******************************************************************************/
    
    private static boolean changed = true;
    
    /** Shows specified menu at specified coordinates. */
    public static void showMenu(ContextMenu menu, double x, double y, Object o) {
        if(!allowMultipleMenus) closeMenus();
        menu.show(x,y,o);
        menu_open = true;
        if (!menus.contains(menu)) menus.add(menu);
        changed = !GUI.isLayoutMode();
        if(transitForMenu) GUI.setLayoutMode(true);
    }
    
    /** Shows specified menu at the centre of the given Node. Use for menus based
     * on polar coordinates. */
    public static void showMenu(ContextMenu menu, Node source, Object o) {
        Bounds b = source.localToScene(source.getLayoutBounds());
        double x = b.getMinX() + b.getWidth()/2;
        double y = b.getMinY() + b.getHeight()/2;
        showMenu(menu,x,y,o);
    }
    
    /** Shows specified menu at last active mouse coordinates */
    public static void showMenu(ContextMenu menu, Object o) {
        showMenu(menu, X, Y, o);
    }
    
    public static void closeMenus() {
        if (menu_open) {
            menus.forEach(m->m.close());
            menu_open = false;
        }
    }
    
    /** Additional customary behavior that takes place when menu closes. */
    public static void onMenuClose() {
        if(transitForMenu && changed)
            GUI.setLayoutMode(false);
    }
    
/******************************************************************************/   
    
    public static WritableImage makeSnapshot(Node n) {
        return n.snapshot(new SnapshotParameters(), null);
    }
    
/******************************************************************************/    
    
    private static ContextMenu makePlaylistContextMenu() {
        final ContextMenu cc = new VerticalContextMenu();
        // populate
        cc.add("play", "Play the item/s on playlist.",() -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            PlaylistManager.playItem(items.get(0));
        });
        cc.add("remove", "Remove item/s from playlist.", () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            PlaylistManager.removeItems(items);
        });
        cc.add("tag", "Edit the item/s in tag editor.",  () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            Widget w = WidgetManager.getWidget(TaggingFeature.class,Widget_Source.FACTORY);
            if (w!=null) {
                TaggingFeature t = (TaggingFeature) w.getController();
                               t.read(items);
            }
        });
        cc.add("crop", "Remove unselected items on playlist.",  () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            PlaylistManager.retainItems(items);
        });
        cc.add("clone(1+)", "Duplicate selected items on playlist as group.",  () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            PlaylistManager.duplicateItemsAsGroup(items);
        });
        cc.add("clone(1)", "Duplicate each of selected items on playlist by one.",  () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            PlaylistManager.duplicateItemsByOne(items);
        });
        cc.add("folder", "Browse the items in their dictionary.", () -> {
            List<PlaylistItem> items = (List<PlaylistItem>)cc.userData;
            List<File> files = items.stream()
                    .filter(PlaylistItem::isFileBased)
                    .map(PlaylistItem::getLocation).collect(Collectors.toList());
            Enviroment.browse(files,true);
        });
        return cc;
    }
    private static ContextMenu makeBookmarkCM() {
        final ContextMenu cc = new VerticalContextMenu();
        // populate
        cc.add("enqueue", "Add items to playlist.", () -> {
            List<Item> i = (List<Item>) cc.userData;
            PlaylistManager.addItems(i);
        });
        cc.add("remove", "Unbookmark items.", () -> {
            List<BookmarkItem> i = (List<BookmarkItem>) cc.userData;
            BookmarkManager.removeBookmarks(i);
        });
        cc.add("tag", "Edit the item/s in tag editor.", () -> {
            List items = (List<Item>) cc.userData;
            Widget w = WidgetManager.getWidget(TaggingFeature.class,Widget_Source.FACTORY);
            if (w!=null) {
                TaggingFeature t = (TaggingFeature) w.getController();
                               t.read(items);
            }
        });
        cc.add("folder", "Browse the items in their dictionary.", () -> {
            List files = ((List<Item>) cc.userData).stream()
                         .filter(Item::isFileBased)
                         .map(Item::getLocation).collect(Collectors.toList());
            Enviroment.browse(files,true);
        });
        return cc;
    }
    private static ContextMenu makeWidgetContextMenu() {
        final ContextMenu cc = new VerticalContextMenu();
        WidgetManager.getFactories().stream().sorted((w1,w2) -> w1.name.compareToIgnoreCase(w2.name)).forEach( w -> {
            cc.add(w.getName(), "Open " + w.getName() + " widget.", () -> {
                Container a = (Container) cc.userData;
                if (a instanceof PolyContainer)
                    a.addChild(a.getChildren().size()+1,w.create());
                else
                    a.addChild(1,w.create());
            });
        });
        Container c = new UniContainer();
            cc.add(c.getName(), "Add sub-layout.", () -> {
                Container a = (Container) cc.userData;
                if (a instanceof PolyContainer)
                    a.addChild(a.getChildren().size()+1,c);
                else
                    a.addChild(1,c);
            });
        LayoutManager.getAllLayoutsNames()
                .sorted((l1,l2) -> l1.compareToIgnoreCase(l2))
                .forEach( l -> {
            cc.add(l, "Open " + l + " layout.", () -> {
                Container a = (Container) cc.userData;
                Layout ll = new Layout(l);
                       ll.deserialize();
                if (a instanceof PolyContainer)
                    a.addChild(a.getChildren().size()+1,ll);
                else
                    a.addChild(1,ll);
            });
        });
        return cc;
    }   
    private static ContextMenu makeImageContextMenu() {
        final ContextMenu cc = new VerticalContextMenu();
        // populate
        cc.add("edit", "Edit the image in editor.", () -> {
            Image i = (Image) cc.userData;
        });
        cc.add("export", "Save the image as ...", () -> {
            Image i = (Image) cc.userData;
            FileChooser fc = new FileChooser();
                fc.getExtensionFilters().addAll(ImageFileFormat.extensions().stream().map(ext->new ExtensionFilter( ext,ext)).collect(Collectors.toList()));
                fc.setTitle("Save image as...");
                fc.setInitialFileName("new_image");
                fc.setInitialDirectory(new File("").getAbsoluteFile());
            File f = fc.showSaveDialog(App.getWindowOwner().getStage());
            FileUtil.writeImage(i, f);
        });
        cc.add("copy", "Copy the image to clipboard", () -> {
            Image i = (Image) cc.userData;
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
                             content.putImage(i);
            clipboard.setContent(content);
        });
        
        
        return cc;
    }
    private static ContextMenu makeImageFileContextMenu() {
        final ContextMenu cc = new VerticalContextMenu();
        // populate
        cc.add("folder", "Open directory.", () -> {
            File f = (File) cc.userData;
            Enviroment.browse(f.toURI());
        });
        cc.add("edit", "Edit the image in editor.", () -> {
            File f = (File) cc.userData;
            Enviroment.edit(f);
        });
        cc.add("open", "Open the image.", () -> {
            File f = (File) cc.userData;
            Enviroment.open(f);
        });
        cc.add("delete", "Delete the image from disc.", () -> {
            File f = (File) cc.userData;
            FileUtil.deleteFile(f);
        });
        cc.add("export", "Save the image as ...", () -> {
            File f = (File) cc.userData;
            FileChooser fc = new FileChooser();
                fc.getExtensionFilters().addAll(ImageFileFormat.extensions().stream().map(ext->new ExtensionFilter(ext,ext)).collect(Collectors.toList()));
                fc.setTitle("Save image as...");
                fc.setInitialFileName("new_image");
                fc.setInitialDirectory(new File("").getAbsoluteFile()); // ?
            File newff = fc.showSaveDialog(App.getWindowOwner().getStage());
            try {
                Files.copy(f.toPath(), newff.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Log.mess("File export failed.");
            }
        });
        return cc;
    }
}