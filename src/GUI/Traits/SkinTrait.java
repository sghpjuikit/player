
package GUI.Traits;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.IntegerProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import utilities.FileUtil;

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
    default String getSkinCurrent() {
        return getSkins().get(skinIndex());
    }
    
    default void setSkinCurrent(String skincss) {
        getSkinOwner().getStylesheets().setAll(skincss);
    }
    
    default int skinIndex() {
        return skinIndexProperty().get();
    }
    
    default public void toggleSkin() {
        skinIndexProperty().set(skinIndexProperty().get()+1);
        if (skinIndex()==getSkins().size()) skinIndexProperty().set(0);
        
        getSkinOwner().getStylesheets().setAll(getSkins().get(skinIndex()));
        
        if (getOnSkinChanged() != null) getOnSkinChanged().handle(null);
    }
    
    
    
   /* @return Handler used when skin changes.*/
    public EventHandler<Event> getOnSkinChanged();
    
    /**
     * Fired when skin of this control changes - more specifically on right
     * mouse click (after new skin value is set).
     * @param handler 
     */
    public void setOnSkinChanged(EventHandler<Event> handler);
}
