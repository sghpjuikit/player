/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import java.io.File;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

/**
 *
 * @author uranium
 */
public abstract class ImageElement {
    /**
     * Images will not load full res, but only the size they need. Preffered
     * size of the GUI container is determined and multiplied by load_coeficient.
     */
    public final static double LOAD_COEFICIENT = 1.7;
    
    
    /**
     * @return image. Null if no image.
     */
    public abstract Image getImage();
    /**
     * Loads image and sets it to show.
     * @param img null to set no image;
     */
    public abstract void loadImage(Image img);
    /**
     * Loads image and sets it to show.
     * @param img null to set no image;
     */
    public abstract void loadImage(File img);
    /**
     * Set file only, dont change the loaded image. Use to add/update file so
     * this object can support file - related activities.
     * @param img null to set no image;
     */
    public abstract void setFile(File img);
    public abstract File getFile();
    /**
     * Calculates size of the image to load.
     * The image normally loads 1:1 with the resolution size, but it is not 
     * necessarily true here. This class will attempt to load the image 
     * smaller than its resolution, taking the estimated future size into
     * consideration.
     * In order to limit memory consumption
     * the size of the specified component will be assumed to be an upper bound
     * of image's future size. The image is assumed to be square and the returned
     * size value will be {@link #LOAD_COEFICIENT} * max(width, height).
     * <p>
     * The sampled size values from the component are preffered size values.
     * @param from component to use to calculate the size based on
     * @return size the image will load in.
     */
    public double calculateImageLoadSize(Region from) {
        return LOAD_COEFICIENT*Math.max(from.getPrefHeight(),from.getPrefWidth());
    }
    public abstract AnchorPane getPane();
    /**
     * For internal use only!
     * This method allows shifting the image methods common for all implementations
     * of this class to be defined here and prevent from need to implement them
     * individually.
     * Note, that implementations using multiple ImageViews or different way of
     * displaying the image will have to use their own implementation of all
     * methods that call this method.
     * @return ImageView displaying the image.
     */
    protected abstract ImageView getImageView();
    /**
     * Decides whether the original image's dimension will be adhered to. Set yes
     * to force original ratio at all times. This will cause the image to not fill
     * all of the thumbnail. Setting to no will stretch the picture to fill the
     * thumbnail accordingly.
     * Defaults to true.
     * @param val 
     */
    public void setPreserveRatio(boolean val) {
        getImageView().setPreserveRatio(val);
    }
    /**
     * Defaults to false
     * @param val 
     */
    public void setSmooth(boolean val) {
        getImageView().setSmooth(val);
    }
    /**
     * Set caching to true for caching the image of the thumbnail. The performance
     * difference (memory and cpu load) remains untested. Default value - false.
     * @param val 
     */
    public void cache(boolean val) {
        getImageView().setCache(val);
    }
}
