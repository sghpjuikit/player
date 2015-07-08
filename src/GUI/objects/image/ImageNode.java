/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.image;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import java.io.File;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

/**
 *
 * @author uranium
 */
@IsConfigurable("Images")
public abstract class ImageNode {

    @IsConfig(name="Image caching", info = "Will keep every loaded image in "
       + "memory. Reduces image loading (except for the first time) but "
       + "increases memory. For large images (around 5000px) the effect "
       + "is very noticable. Not worth it if you dont browse large images "
       + "or want to minimize RAM usage.")
    static boolean cache_images = false;
    
    /**
     * To spare memory, images are only loaded up to requested size.
     * This defines how many times should images load bigger than requested.
     * Serves 3 purposes:
     * <ul>
     * <li> Loading images in full size would produce artifacts when they would
     * need to be down-scaled to small sizes. So dont use large value.
     * <li> Using value 1 would cause image deformations caused by slight size
     * scaling. Avoid values close to 1.
     * <li> In some cases, user expects the image to be scalable. This removes
     * any potential blur or artifacts from over-scaling low-resolution version.
     * </ul>
     * <p>
     */
    @IsConfig(name="Image load multiplier", min = 1, max = 2, info="To spare "
       + "memory, images are only loaded up to requested size. "
       + "This defines how many times bigger should the image load compared to "
       + "requested size. This serves 3 purposes:\n"
       + "\n\tLoading images in full size would produce artifacts when they would "
       + "need to be down-scaled to small sizes. So dont use large value. "
       + "\n\tUsing value 1 would cause image deformations caused by slight size "
       + "scaling. Avoid values close to 1. "
       + "\n\tYou may expect the image to be scalable. This removes potential "
       + "blur or artifacts from over-scaling low-resolution version. ")
    public static double LOAD_COEFICIENT = 1.2;
    
    
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
    
    public abstract File getFile();
    
    /**
     * Calculates size of the image to load. Returns recommended size for the
     * image.
     * <p>
     * The image normally loads 1:1 with the resolution size of the file, but it
     * is often wasteful, particlarly for big images and even more if the size
     * the image will be used with is rather small.
     * <p>
     * In order to limit memory consumption
     * the size of the specified component will be assumed to be an upper bound
     * of image's loaded size. The image is assumed to be square and the returned
     * size value will be {@link #LOAD_COEFICIENT} * component size.
     * 
     * @param from component to use to calculate the size based on
     * @return recommended size of the image
     */
    public Point2D calculateImageLoadSize(Region from) {
        // sample both height and prefHeight to avoid getting 0 when source not
        // yet initialized (part of scene graph)
        double w = Math.max(from.getWidth(),from.getPrefWidth());
        double h = Math.max(from.getHeight(),from.getPrefHeight());
        return new Point2D(w,h).multiply(LOAD_COEFICIENT);
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
    protected abstract ImageView getView();
    
    /**
     * Decides whether the original image's dimension will be adhered to. Set yes
     * to force original ratio at all times. This will cause the image to not fill
     * all of the thumbnail. Setting to no will stretch the picture to fill the
     * thumbnail accordingly.
     * Defaults to true.
     * @param val 
     */
    public void setPreserveRatio(boolean val) {
        getView().setPreserveRatio(val);
    }
    
    /**
     * Defaults to false
     * @param val 
     */
    public void setSmooth(boolean val) {
        getView().setSmooth(val);
    }
    
    /**
     * Set caching to true for caching the image of the thumbnail. The performance
     * difference (memory and cpu load) remains untested. Default value - false.
     * @param val 
     */
    public void cache(boolean val) {
        getView().setCache(val);
    }
}
