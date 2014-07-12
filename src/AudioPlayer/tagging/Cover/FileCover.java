/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging.Cover;

import java.io.File;
import java.util.Objects;
import javafx.scene.image.Image;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Denotes Cover represented by a {@link File}.
 * <p>
 * This class is fully polymorphic
 * Should never be used directly but instead use the {@Cover} interface
 * and leverage polymorphism.
 * 
 * @author Plutonium_
 */
@Immutable
public class FileCover implements Cover {
    private final File image;
    private final String info;
    
    public FileCover(File image, String description) {
        Objects.requireNonNull(description);
        
        this.image = image;
        this.info = description;
    }

    /** {@inheritDoc} */
    @Override
    public Image getImage() {
        return image==null ? null : new Image(image.toURI().toString());
    }

    /** {@inheritDoc} */
    @Override
    public File getFile() {
        return image;
    }

    @Override
    public boolean isEmpty() {
        return image != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDestription() {
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if(o != null && o instanceof FileCover) {
            FileCover other = (FileCover)o;
            return image.equals(other.image);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.image);
        return hash;
    }
    
    
}