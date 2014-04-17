/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistCategorizer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author uranium
 * 
 */
public class Category {
    /**
     * Unique.
     */
    private String name;
    /**
     * Lazy initialized.
     */
    private CategoryPath path;
    /**
     * Lazy initialized. Never null.
     */
    private List<Category> children;
    
    public Category(String _name) {
        name = _name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the children
     */
    public List<Category> getChildren() {
        if (children == null) { children = new ArrayList<>(); }
        return children;
    }
    public void addChild(Category c) {
        getChildren().add(c);
        List<String> p = new ArrayList<>();
        p.addAll(p);
        p.add(name);
        c.setPath(new CategoryPath(p));
    }
    /**
     * @param children the children to set
     */
    public void setChildren(List<Category> children) {
        this.children = children;
    }

    /**
     * @return the path
     */
    public CategoryPath getPath() {
        if(path == null) { path = new CategoryPath(new ArrayList<String>()); }
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(CategoryPath p) {
        getPath().set(p.get());
    }
    
    /**
     * Constructs String representation of the path. Full path = path + 
     * category name.
     * Example:
     * .photos.nature.flowers
     * Never null. At most "";
     * @return
     */
    public String getPathString() {
        return path.getPathAsString();
    }
    
     /**
     * Constructs String representation of the full path. Full path = path + 
     * category name.
     * Example:
     * .photos.nature.flowers
     * Never null. At most "";
     * @return
     */
    public String getFullPathString(String categoryName) {
        return path.getFullPath(name);
    }
    
    /**
     * Returns all playlists belonging to this category. This method considers only
     * loaded playlists. If there is possibility that from the last time playlists
     * were read, data changed, read playlists first.
     * @param allPlaylists
     * @return 
     */
    public List<Playlist> getPlaylists() {
        List<Playlist> allPlaylists = PlaylistCategorizer.getPlaylists();
        List<Playlist> out = new ArrayList<>();
        if (allPlaylists == null || allPlaylists.isEmpty()) { return out; }
        
        for (Playlist p: allPlaylists) {
            if (p.getCategories().contains(getName())) { out.add(p); }
        }
        return out;
    }
    
    @Override
    public String toString() {
        return name;
    }

}
