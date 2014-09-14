
package GUI.Traits;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Parent;
import utilities.Parser.File.FileUtil;

/**
 *
 * @author uranium
 */
public interface SkinTrait {
    
    Parent getSkinOwner();
    IntegerProperty skinIndexProperty();
    
    
    default File getSkinDirectory() {
        return new File("controls"+File.separator+getClass().getSimpleName());
    }
    
    default List<String> getSkins() {
        List<String> s = new ArrayList<>();
        if (!FileUtil.isValidatedDirectory(getSkinDirectory())) return s;
        
        for (File f: getSkinDirectory().listFiles((dir, name) -> name.endsWith(".css"))) {
            if (FileUtil.isValidFile(f))
                try {
                    s.add(f.toURI().toURL().toExternalForm());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(SkinTrait.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        return s;
    }
    
    /**
     * @return name of the skin in use or "" if default skin.
     */
    default String getSkinCurrent() {
        int i = skinIndexProperty().get();
        return i==-1 ? "" : getSkins().get(i);
    }
    
    /**
     * Set skin by its name (filename of the stylesheet). Use "" to set default
     * skin.
     * <p>
     * Fires skin change event.
     * 
     * @param skincss 
     */
    default void setSkinCurrent(String skincss) {
        if(skincss.isEmpty()) {
            getSkinOwner().getStylesheets().clear();
            skinIndexProperty().set(-1);
        }
        else {
            List<String> skins = getSkins();
            getSkinOwner().getStylesheets().setAll(skincss);
            skinIndexProperty().set(skins.indexOf(skincss));
        }
        
        if (getOnSkinChanged() != null) getOnSkinChanged().accept(skincss);
        System.out.println(skincss);
    }
    
    /** Loops through the skins by one. */
    default public void toggleSkin() {
        List<String> skins = getSkins();
        
        int skinIndex = skinIndexProperty().get()+1;
        if (skinIndex>=skins.size()) skinIndex=-1;
        
        String skin = skinIndex==-1 ? "" : skins.get(skinIndex);
        setSkinCurrent(skin);
    }
    
    
    
   /* @return skin change handler.*/
    public Consumer<String> getOnSkinChanged();
    
    /**
     * Fired when skin of this control changes - more specifically on right
     * mouse click (after new skin value is set).
     * @param handler 
     */
    public void setOnSkinChanged(Consumer<String> handler);
}
