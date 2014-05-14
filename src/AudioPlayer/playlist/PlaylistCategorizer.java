
package AudioPlayer.playlist;

import Configuration.ConfigManager;
import Configuration.Configuration;
import PseudoObjects.Category;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 *
 * @author uranium
 */
public class PlaylistCategorizer {
    private static final List<Playlist> playlists = new ArrayList<>();
    private static List<Category> categories;
    private static List<Category> paths;
    
    public static Playlist getPlaylist(String name) {
        for (Playlist p : playlists) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns all playlists.
     * findPlaylists() needs to be called only once in
     * the app run. But up to date state of the playlist list is guaranteed only
     * right after the findPlaylists() method call.
     * @return 
     */
    public static List<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * Returns playlists in given category. Use null as parameter to get
     * playlists in no category.
     *
     * @param category
     * @return 
     */
    public static List<Playlist> getPlaylists(Category category) {
        List<Playlist> out = new ArrayList<>();
        if (playlists == null || playlists.isEmpty()) {
            return out;
        }

        if (category == null) {
            for (Playlist p : playlists) {
                if (p.getCategories().isEmpty()) {
                    out.add(p);
                }
            }
            return out;
        } else {
            return category.getPlaylists();
        }
    }

    /**
     * Reads previously stored playlists. Searches for .pfx files in playlist
     * folder and registers them as available. Can be relatively I/O operation
     * costly depending on the mount of stored playlists.
     *
     * @TODO Make the performance impact smaller.
     */
    private static void findPlaylists() {
        Log.mess("Attempting to load playlists...");
        File dir = new File(App.PLAYLIST_FOLDER());
        if (FileUtil.isValidatedDirectory(dir)) {
            Log.err("Loading playlists failed.");
            return;
        }
        File[] files;
        files = dir.listFiles((File pathname) -> pathname.getName().endsWith(".pfx"));

        if (files.length == 0) {
            Log.mess("Folder '" + dir.getAbsolutePath() + "' is empty. No playlists loaded.");
            return;
        }
        playlists.clear();
        for (File f : files) {
            Playlist p = Playlist.deserialize(f);
            if (p != null) {
                playlists.add(p);
                Log.mess("Playlist " + p.getName() + " loaded.");
            }
        }
    }

    /**
     * Constructs list of all existing playlist categories. Existing category is
     * defined as one with non-zero number of playlists associated with it.
     *
     * The list is constructed from all currently available playlists' data.
     * Playlists are freshly loaded from the playlist folder before category
     * list starts to be populated to guarantee up to date data. That makes this
     * method I/O costly, but it is a trade off, gaining the overall app
     * performance by avoiding constant folder monitoring. Performance cost
     * should be linear - O(n).
     *
     * There are alternatives to this method utilizing the other side of the
     * trade off = not up to date data + no I/O. This method may be used only
     * once or at specific times to populate the category list stored globally
     * in PlaylistManager object and doesnt need to be used to access category
     * list. Depending on the use strategy, this can be considered merely an
     * update method.
     *
     * This method overwrites the category list and deletes any empty
     * categories.
     *
     * @return up to date list of existing Categories.
     */
    public static List<Category> findCategories() {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        findPlaylists();
        if (playlists == null || playlists.isEmpty()) {
            return new ArrayList<>();
        }
        Log.mess("Loading playlist categories.");

        categories.clear();
        for (Playlist p : playlists) {
            for (String categoryName : p.getCategories()) {
                if (!categoryExist(categoryName)) {
                    categories.add(new Category(categoryName));
                }
            }
        }
        return categories;
    }

    /**
     * Returns playlist category list.
     *
     * Note that category list follows lazy initialization. This means that
     * without manually calling findCategories() method prior to this one, empty
     * list will be returned. findCategories() needs to be called only once in
     * the app run.
     * @return 
     */
    public static List<Category> getCategories() {
        return categories;
    }
    
    public static List<Category> getHierarchy() {
        if (paths == null) { paths = new ArrayList<>(); }
        Category c1 = new Category("hhhhh");
        c1.addChild(new Category("dsds"));
        c1.addChild(new Category("dgfddd"));
        c1.addChild(new Category("fff"));
        Category c2 = new Category("hhhhhgtrtretretre");
        c2.addChild(new Category("dsds"));
        c2.addChild(new Category("dgfddd"));
        c2.addChild(new Category("ggggggggggggg"));
        return paths;
    }

    /**
     * Checks whether category with given name already exists. As category
     * name must be unique, there cant be two categories with the same name.
     *
     * @param name
     * @return
     */
    private static boolean categoryExist(String name) {
        for (Category c : categories) {
            if (name.equals(c.getName()))
                return true;
        }
        return false;
    }
}
