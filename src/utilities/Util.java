/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.util.Callback;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.jaudiotagger.tag.images.Artwork;
import utilities.functional.impl.NotNull;

/**
 * Provides static utility methods for various purposes.
 */
public interface Util {
    
    /** Basic string comparator utilizing String.compareTo(). */
    public static final Comparator<String> STR_COMPARATOR = (a,b) -> a.compareTo(b);
    
    /** Basic string comparator utilizing String.compareToIgnoreCase(). */
    public static final Comparator<String> IGNORE_CASE_STR_COMPARATOR = (a,b) -> a.compareToIgnoreCase(b);
    
    /** 
     * Creates comparator comparing E elements by some associated {@link Comparable}
     * object, mostly their property, Utilizing Comparable.compareTo().
     * <p>
     * Easy and concise way to compare objects without code duplication
     * <p>
     * This method is generic Comparator factory producing comparators comparing
     * the obtained result of the comparable supplier.
     * 
     * @param toC Comparable supplier - E to Comparable mapper. Returns the 
     * Comparable derived from the E.
     */
    public static<E> Comparator<E> cmpareBy(Callback<E,Comparable> toC) {
        return (a,b) -> toC.call(a).compareTo(toC.call(b));
    }
    
    /** 
     * Creates comparator comparing E elements by their string representation
     * obtained by provided implementation. Utilizes String.compareToIgnoreCase().
     * <p>
     * Easy and concise way to compare objects without code duplication.
     * 
     * @param cmpGetter Comparable supplier. Returns the Comparable derived from
     * the object.
     */
    public static<E> Comparator<E> cmpareNoCase(Callback<E,String> strGetter) {
        return (a,b) -> strGetter.call(a).compareToIgnoreCase(strGetter.call(b));
    }
    
    /** Simple Predicate returning true if object is not null. Use in lambda. */
    public static Predicate<Object> NotNULL = new NotNull();
    
    /** Simple Collector concatenating Strings to coma separated list (CSList)
     * by delimiter ", ".  Use in lambda. */
    public static Collector<CharSequence,?,String> toCSList = Collectors.joining(", ");
    
    /**
     * Method equivalent to object's equal method, but if both objects are null
     * they are considered equal as well.
     * @param o1
     * @param o2
     * @return 
     */
    public static boolean nullEqual(Object o1, Object o2) {
        return (o1==null && o2==null) ||
               (o1!=null && o1.equals(o2));
    }
    
    /**
     * Prints out the value of Duration - string representation of the duration
     * in the format h:m:s - 00:00:00. If any of the h,m,s values is single digit,
     * decade digit '0' is still written to retain the correct format.
     * If hours = 0, they are left out.
     * Example:
     * 01:00:06
     *    04:45
     *    00:34
     * @param duration
     * @return 
     */
    public static String formatDuration(Duration duration) {
        Objects.requireNonNull(duration);
        
        double sec_total = duration.toMillis()/1000;
        int seconds = (int) sec_total %  60;
        int minutes = (int)(sec_total /  60) % 60;
        int hours   = (int)(sec_total /3600);
        
        if (minutes < 1) {
            return String.format("00:%02d",seconds);            
        } else if (hours < 1) {
            return String.format("%02d:%02d", minutes, seconds);            
        } else if (hours < 100){
            return String.format("%2d:%02d:%02d", hours, minutes, seconds);            
        } else {
            return String.format("%3d:%02d:%02d", hours, minutes, seconds); 
        }
    }
    
    /**
     * Prints out the value of Duration - string representation of the duration
     * in the format h:m:s - 00:00:00. If any of the h,m,s values is single digit,
     * decade digit '0' is written to retain the correct format only IF 
     * include_zeros = true.
     * If hours = 0, they are left out.
     * Example:
     *  1:00:06
     *     4:45
     *       34
     * @param duration
     * @param include_zeros
     * @return 
     */
    public static String formatDuration(Duration duration, boolean include_zeros) {
        Objects.requireNonNull(duration);
        if (include_zeros) return formatDuration(duration);
        
        double sec_total = duration.toMillis()/1000;
        int seconds = (int) sec_total %  60;
        int minutes = (int)(sec_total /  60) % 60;
        int hours   = (int)(sec_total /3600);
        
        if (minutes < 1) {
            return String.format("%2d",seconds);            
        } else if (hours < 1) {
            return String.format("%2d:%2d", minutes, seconds);            
        } else if (hours < 100) {
            return String.format("%2d:%2d:%2d", hours, minutes, seconds);            
        } else {
            return String.format("%3d:%2d:%2d", hours, minutes, seconds);            
        }
    }
    
    /**
     * Convenience method to clean String objects.
     * Assigns string empty value if it should be empty according to shouldBeEmpty()
     * method. Use as a convenient filter for suspicious String objects.
     * More formally returns:
     * (shouldBeEmpty(str)) ? "" : str;
     * @see shouldBeEmpty(String str)
     * @param str String to emptify.
     * @return "" if String should be empty, otherwise does nothing..
     */
    public static String emptifyString(String str) {
        return (shouldBeEmpty(str)) ? "" : str;
    }
    
    /**
     * Broader check for emptiness of String object.
     * Checks for: 
     * - null
     * - "null", "NULL" and other combinations
     * - ""
     * - whitespaceOnly.
     * 
     * @param str String to check.
     * @return true if any of the above is met.
     */
    public static boolean shouldBeEmpty(String str) {
        return str == null || str.equalsIgnoreCase("null") || str.trim().isEmpty();       
    }
    
    /**
     * Checks and formats String so it can be safely used for naming a File.
     * Replaces all forbidden characters and " " with "_".
     * @param str
     * @return 
     */
    public static String filenamizeString(String str) {
        String out = str;
        out = out.replace(" ", "_");
        out = out.replace("/", "_");
        out = out.replace("\\", "_");
        out = out.replace(":", "_");
        out = out.replace("*", "_");
        out = out.replace("?", "_");
        out = out.replace("<", "_");
        out = out.replace(">", "_");
        out = out.replace("|", "_");
        return out;
    }
    
    /** 
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
    public static String capitalize(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    public static String capitalizeStrong(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    /**
     * Anytime you are asked for STRING REPRESENTATION OF THE FILE you want to 
     * use this method if you are not sure what to do.
     * Converts file to String url. Use to safely get file's url as String from
     * file in a File object.
     *
     * This method is identical to the following codes:
     * File f.toURI().toString();
     * File f.toURI().toURL().toString();
     * 
     * The above is not the same as File.toString() !
     * 
     * For example use when creating Image from File.
     * @param f
     * @return String representation of file's url.
     */
    public static String FileToUrlString(File f) {
        return f.toURI().toString();
    }
    
    /**
     * Loads image file.
     * Loads File object into Image object. Resizes the image to desired size
     * (aspect ratio remains) to reduce memory consumption. It is recommended
     * to use smaller size when possible, for example for thumbnails. For big
     * images, this can also make a difference. Loading picture into bigger res
     * as screen is pointless. It is possible to use Screen class to find out
     * screen properties to dynamically set optimal resolution.
     * @param file file to load.
     * @param size to resize image's width to. Retains ratio. Set to 0 to
     * prevent resizing and load full image. Smaller size reduces memory.
     * @return loaded image or null if file null or not a valid image source.
     */
    public static Image loadImage(File file, double size) {
        if (file == null) return null;
        if (size == 0)
            return new Image(file.toURI().toString());
        else {
            int w= Integer.MAX_VALUE;
            try {
                BufferedImage readImage = ImageIO.read(file);
//                int h = readImage.getHeight();
                w = readImage.getWidth();
            } catch (IOException e) {
            }
            
            int width = Math.min((int)size,w);
            return new Image(file.toURI().toString(), width, 0, true, true);
        }
    }
    
    /**
     * Compares two Artwork objects.
     * Artwork's equals() method doesnt return true properly. Use this method
     * instead.
     * <p>
     * Method is deprecated as it Artwork should not be used anyway. The method
     * works well though.
     * @param art1
     * @param art2
     * @return 
     */
    @Deprecated
    public static boolean equals(Artwork art1, Artwork art2) {
        if (art1 == null && art2 == null) { return true; }
        if (art1 == null || art2 == null) { return false; }
        return Arrays.equals(art1.getBinaryData(), art2.getBinaryData());
    }
}
