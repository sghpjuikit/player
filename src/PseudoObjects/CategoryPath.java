/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

import java.util.ArrayList;
import java.util.List;

/**
 * @author uranium
 * 
 * Path string always starts with separator character. May or may not end with
 * separator character, depending on type of the path. See example below.
 * Full path is unobtainable without the respective category name.
 * 
 * Example:
 * path:        .photos.nature.
 * full path:   .photos.nature.flowers
 */
public class CategoryPath {
    public static String separator = ".";
    
    /**
     * Ordered list of categories making up the path.
     */
    List<String> path;
    
    public CategoryPath(List<String> p) {
        path = p;
    }
    /**
     * Constructs String representation of the path. Full path = path + 
     * category name.
     * Example:
     * .photos.nature.flowers
     * Never null. At most "";
     * @return
     */
    public String getPathAsString() {
        String pth = "";
        for (String str: path) { pth = getSeparator() + pth + str; }
        pth = pth + separator;
        
        return pth;
    }
    public void set(List<String> p) {
        path = p;
    }
    public List<String> get() {
        if (path == null) { path = new ArrayList<>(); }
        return path;
    }
    
     /**
     * Constructs String representation of the full path. Full path = path + 
     * category name.
     * Example:
     * .photos.nature.flowers
     * Never null. At most "";
     * @return
     */
    public String getFullPath(String categoryName) {
        String pth = "";
        for (String str: path) { pth = getSeparator() + pth + str; }
        pth = pth + separator + categoryName;
        return pth;
    }

            
    /**
     * Returns string representation of the path separator.
     * ".";
     */
    public static String getSeparator() {
        return separator;
    }
}
