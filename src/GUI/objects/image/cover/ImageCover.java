/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.image.cover;

import gui.objects.image.Thumbnail;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Denotes Cover represented by a {@link Image} or {@link BufferedImage}.
 * <p>
 * This class is fully polymorphic
 * Should never be used directly but instead use the {@Cover} interface
 * and leverage polymorphism.
 * 
 * @author Plutonium_
 */
@Immutable
public class ImageCover implements Cover {
    private final Image imageI;
    private final BufferedImage imageB;
    private final String info;
    
    public ImageCover(Image image, String description) {
        Objects.requireNonNull(description);
        
        this.imageI = image;
        this.imageB = null;
        this.info = description;
    }
    
    public ImageCover(BufferedImage image, String description) {
        Objects.requireNonNull(description);
        
        this.imageB = image;
        this.imageI = null;
        this.info = description;
    }

    /** {@inheritDoc} */
    @Override
    public Image getImage() {
        return imageB==null ? imageI : SwingFXUtils.toFXImage(imageB, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public Image getImage(double width, double height) {
        return getImage();
    }

    /** {@inheritDoc} */
    @Override
    public File getFile() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return imageB!=null && imageI!=null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDestription() {
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if(o==this) return true; // this line can make a difference
        
        if(o != null && o instanceof ImageCover) {
            ImageCover other = (ImageCover)o;
            if(imageB!=null && other.imageB!=null)
                return imageB.equals(other.imageB);
            if(imageI!=null && other.imageI!=null)
                return imageI.equals(other.imageI);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.imageI);
        hash = 37 * hash + Objects.hashCode(this.imageB);
        return hash;
    }
    
    
}
