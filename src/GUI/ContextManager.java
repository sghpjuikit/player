
package GUI;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import Configuration.IsConfig;
import GUI.Components.SimpleConfigurator;
import GUI.objects.CircleContextMenu;
import GUI.objects.ContextMenu;
import GUI.objects.VerticalContextMenu;
import Layout.Container;
import Layout.Layout;
import Layout.LayoutManager;
import Layout.Widgets.SupportsTagging;
import Layout.PolyContainer;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import Library.BookmarkItem;
import Library.BookmarkManager;
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
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import main.App;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;

/**
 *
 * @author uranium
 */
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
    
    public static UIController gui;
    
    //menus
    public static ContextMenu playlistMenu;
    public static ContextMenu bookmarkMenu;
    public static ContextMenu widgetsMenu;
    public static ContextMenu imageMenu;
    public static ContextMenu imageFileMenu;
    
    private static final List<ContextMenu> menus = new ArrayList<>();
    private static boolean menu_open = false;
    
    
    public ContextManager(UIController c) {
        gui = c;
        contextPane = c.contextPane;
        
        playlistMenu = makePlaylistContextMenu();
        bookmarkMenu = makeBookmarkCM();
        widgetsMenu = makeWidgetContextMenu();
        imageMenu = makeImageContextMenu();
        imageFileMenu = makeImageFileContextMenu();   
    }

    /** Set last app run x coordinate. */
    public static void setX(double _x) {
        X = _x;
    }
    /** Set last app run y coordinate. */
    public static void setY(double _y) {
        Y = _y;
    }
    
/******************************************************************************/
    
    public static final List<Window> windows = new ArrayList();
    
    /** 
     * @param widget widget to open, does nothing when null.
     * @throws NullPointerException if param null
     */
    public static Window openFloatingWindow(Widget widget) {
        Objects.requireNonNull(widget);
        
        Window w = Window.create();
        w.s.initOwner(App.getInstance().getStage());
        w.setTitle(widget.getName());
        w.setContent(widget);
        w.show();
        w.setLocationCenter();
        
        windows.add(w);
        return w;
    }
    public static void openFloatingWindow(Node content, String title) {
        if (content == null) return;
        
        Window w = Window.create();
               w.setIsPopup(true);
               w.s.initOwner(App.getInstance().getStage());
               w.setTitle(title);
               w.setContent(content);
               w.show();
               w.setLocationCenter();
        
        windows.add(w);
    }
    /**
     * @param widget to open. Null does nothing.
     */
    public static void openFloatingSettingsWindow(Widget widget) {
        if (widget == null) return;
        
        SimpleConfigurator c = new SimpleConfigurator(widget, () -> widget.getController().refresh() );
        
        Window w = Window.create();
               w.s.initOwner(App.getInstance().getStage());
               w.setIsPopup(true);
               w.setTitle(widget.getName() + " Settings");
               w.setContent(c.getPane());
               w.show();
               w.setLocationCenter();
        
        windows.add(w);
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
        if(gui.disable_menus) return;
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
            PlaylistManager.playItem(PlaylistManager.getSelectedItems().get(0));   // NEEDS TWEAK
        });
        cc.add("remove", "Remove item/s from playlist.", () -> {
            PlaylistManager.removeSelectedItems();
        });
        cc.add("tag", "Edit the item/s in tag editor.",  () -> {
             SupportsTagging t = WidgetManager.getTaggerOrCreate();
             if (t != null) t.read(PlaylistManager.getSelectedItems());
        });
        cc.add("crop", "Remove unselected items on playlist.",  () -> {
            PlaylistManager.removeUnselectedItems();
        });
        cc.add("clone(1+)", "Duplicate selected items on playlist as group.",  () -> {
            PlaylistManager.duplicateItemsAsGroup(PlaylistManager.getSelectedItems());
        });
        cc.add("clone(1)", "Duplicate each of selected items on playlist by one.",  () -> {
            PlaylistManager.duplicateItemsByOne(PlaylistManager.getSelectedItems());
        });
        cc.add("folder", "Browse the items in their dictionary.", () -> {
            List<File> files = new ArrayList<>();
            PlaylistManager.getSelectedItems().stream().map(PlaylistItem::getFolder).forEach(files::add);
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
             SupportsTagging t = WidgetManager.getTaggerOrCreate();
             List items = (List<Item>) cc.userData;
             if (t!=null) t.read(items);
        });
        cc.add("folder", "Browse the items in their dictionary.", () -> {
            List files = ((List<Item>) cc.userData).stream()
                         .map(Item::getFolder).collect(Collectors.toList());
            Enviroment.browse(files,true);
        });
        return cc;
    }
    private static ContextMenu makeWidgetContextMenu() {
        final ContextMenu cc = new CircleContextMenu();
        WidgetManager.getFactories().stream().sorted((w1,w2) -> w1.name.compareToIgnoreCase(w2.name)).forEach( w -> {
            cc.add(w.getName(), "Open " + w.getName() + " widget.", () -> {
                Container a = (Container) cc.userData;
                if (a instanceof PolyContainer)
                    a.addChild(a.getChildren().size()+1,w.create());
                else
                    a.addChild(1,w.create());
            });
        });
        LayoutManager.layouts.stream().sorted((l1,l2) -> l1.compareToIgnoreCase(l2)).forEach( l -> {
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
            File f = fc.showSaveDialog(App.getInstance().getStage());
            FileUtil.writeImage(i, f);
        });
        return cc;
    }
    private static ContextMenu makeImageFileContextMenu() {
        final ContextMenu cc = new VerticalContextMenu();
        // populate
        cc.add("folder", "Open directory.", () -> {
            File f = (File) cc.userData;
            Enviroment.browse(f);
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
                fc.getExtensionFilters().addAll(ImageFileFormat.extensions().stream().map(ext->new ExtensionFilter( ext,ext)).collect(Collectors.toList()));
                fc.setTitle("Save image as...");
                fc.setInitialFileName("new_image");
                fc.setInitialDirectory(new File("").getAbsoluteFile());
            File newff = fc.showSaveDialog(App.getInstance().getStage());
            try {
                Files.copy(f.toPath(), newff.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Log.mess("File export failed.");
            }
        });
        return cc;
    }
}