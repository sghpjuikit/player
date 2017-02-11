package gui.objects.image;

import java.io.File;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.validation.Constraint;
import static main.App.APP;

/**
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Images")
public abstract class ImageNode {

    @IsConfig(name="Image caching", info = "Will keep every loaded image in "
       + "memory. Reduces image loading (except for the first time) but "
       + "increases memory. For large images (around 5000px) the effect "
       + "is very noticeable. Not worth it if you do not browse large images "
       + "or want to minimize RAM usage.")
    static boolean cache_images = false;

    /**
     * To spare memory, images are only loaded up to requested size.
     * This defines how many times should images load bigger than requested.
     * Serves 3 purposes:
     * <ul>
     * <li> Loading images in full size would produce artifacts when they would
     * need to be down-scaled to small sizes. So do not use large value.
     * <li> Using value 1 would cause image deformations caused by slight size
     * scaling. Avoid values close to 1.
     * <li> In some cases, user expects the image to be scalable. This removes
     * any potential blur or artifacts from over-scaling low-resolution version.
     * </ul>
     * <p/>
     */
    @IsConfig(name="Image load multiplier", info="To spare "
       + "memory, images are only loaded up to requested size. This defines how many times bigger should the image "
	   + "load compared to requested size. This serves 3 purposes:"
       + "\n\t* Loading images in full size would produce artifacts when they would "
       + "need to be down-scaled to small sizes. So do not use large value. "
       + "\n\t* Using value 1 would cause image deformations caused by slight size scaling. Avoid values close to 1. "
       + "\n\t* You may expect the image to be scalable. This removes potential "
       + "blur or artifacts from over-scaling low-resolution version. ")
    @Constraint.MinMax(min=1, max=2)
    public static double LOAD_COEFFICIENT = 1.2;

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
     * <p/>
     * The image normally loads 1:1 with the resolution size of the file, but it
     * is often wasteful, particularly for big images and even more if the size
     * the image will be used with is rather small.
     * <p/>
     * In order to limit memory consumption
     * the size of the specified component will be assumed to be an upper bound
     * of image's loaded size. The image is assumed to be square and the returned
     * size value will be {@link #LOAD_COEFFICIENT} * component size.
     *
     * @param from component to use to calculate the size based on
     * @return recommended size of the image
     */
    public Point2D calculateImageLoadSize(Region from) {
        // sample both size and prefSize to avoid getting 0 when source not yet initialized (part of scene graph)
        double w = APP.windowManager.screenMaxScaling*Math.max(from.getWidth(),from.getPrefWidth());
        double h = APP.windowManager.screenMaxScaling*Math.max(from.getHeight(),from.getPrefHeight());
        return new Point2D(w,h).multiply(LOAD_COEFFICIENT);
    }

    public abstract Pane getPane();

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
    public abstract ImageView getView();

    /**
     * Decides whether the original image's dimension will be adhered to. Set yes
     * to force original ratio at all times. This will cause the image to not fill
     * all of the thumbnail. Setting to no will stretch the picture to fill the
     * thumbnail accordingly.
     * Defaults to true.
     */
    public void setPreserveRatio(boolean val) {
        getView().setPreserveRatio(val);
    }

    /**
     * Defaults to false.
     */
    public void setSmooth(boolean val) {
        getView().setSmooth(val);
    }

    /**
     * Set caching to true for caching the image of the thumbnail. The performance
     * difference (memory and cpu load) remains untested. Default value - false.
     */
    public void cache(boolean val) {
        getView().setCache(val);
    }
}