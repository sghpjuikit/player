/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging.Cover;

import java.io.File;
import javafx.scene.image.Image;

/**
 <p>
 @author Plutonium_
 */
public class EmptyCover implements Cover {

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public Image getImage(double width, double height) {
        return null;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public String getDestription() {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
    
}
