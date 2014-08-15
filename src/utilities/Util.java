/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
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
    
/******************************** GRAPHICS ************************************/
    
    /**
     * Simple black background with no insets or radius. use for debugging (to
     * see pane layout);
     * @return 
     */
    public static Background SIMPLE_BGR() {
        return new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY));
    }
    
/********************************** STRING ************************************/
    
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
     * Converts first letter of the string to upper case.
     */
    public static String capitalize(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    /** 
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
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
     * Convenience method. Equivalent to: loadImage(file, size, size);
     * @param file
     * @param size
     * @return 
     */
    public static Image loadImage(File file, double size) {
        return loadImage(file, size, size);
    }
    
    /**
     * Loads image file. with requested size.
     * <p>
     * Loads File object into Image object of desired size
     * (aspect ratio remains unaffected) to reduce memory consumption.
     * For example it is possible to use {@link Screen} class to find out
     * screen properties to dynamically set optimal resolution or limit it even
     * further for small thumbnails, where intended size is known.
     * 
     * @param file file to load.
     * @param size to resize image's width to. Use 0 to use original image size.
     * The size will be clipped to original if it is greater.
     * @return loaded image or null if file null or not a valid image source.
     */
    public static Image loadImage(File file, double width, double height) {
        if (file == null) return null;
        if (width == 0 && height == 0)
            return new Image(file.toURI().toString());
        else {
            // find out real image file resolution
            int w= Integer.MAX_VALUE;
            int h= Integer.MAX_VALUE;
            try {
                BufferedImage readImage = ImageIO.read(file);
                h = readImage.getHeight();
                w = readImage.getWidth();
            } catch (IOException e) {
                // ignore
            }
            
            // lets not get over real size
            int fin_width = Math.min((int)width,w);
            int fin_height = Math.min((int)height,h);
            return new Image(file.toURI().toString(), fin_width, fin_height, true, true);
        }
    }
    
    /**
     * Compares two Artwork objects.
     * Artwork's equals() method doesnt return true properly. Use this method
     * instead.
     * <p>
     * Method is deprecated as Artwork should not be used anyway. The method
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

    
    
    /**
     * Logarithm
     * @param base of the log
     * @param i number to calculate log for
     * @return base specified logarithm of the number
     */
    static int log(int base, int i) {
        short p = 0;
        while(Math.pow(base, p) <= i)
            p++;
        return p;
    }
    
    /**
     * @param number
     * @return number of digits of a number
     */
    public static int digits(int number) {
        int x = number;
        int cifres = 0;
        while (x > 0) {
            x /= 10;
            cifres++;
        }
        return cifres;
    }
    
    /**
     * Creates zeropadded string - string of a number with '0' added in to
     * maintain consistency in number of length.
     * @param a - to turn onto zeropadded string
     * @param b - number to zeropad into
     * @param ch - character to use. Notable characters are: ' ' or '0'
     * @return 
     */
    public static String zeroPad(int a, int b, char ch) {
        int diff = digits(b) - digits(a);
        String out = "";
        for (int i=1; i<=diff; i++)
            out += ch;
        return out + String.valueOf(a);
    }
    
    /**
     * @return highest possible number of the same decadic length as specified
     * number.
     * Examples:  9 for 1-10, 99 for 10-99, 999 for nubmers 100-999, etc...
     */
    public static int DecMin1(int number) {
        return (int) (Math.pow(10, 1+digits(number))-1);
    }
    
/***************************** REFLECTION *************************************/
    
    /**
     * Returns all declared fields of the class including inherited ones.
     * Equivalent to union of declared fields of the class and all its
     * superclasses.
     */
    public static List<Field> getAllFields(Class clazz) {
       List<Field> fields = new ArrayList();
       // get all fields of the class (but not inherited fields)
       fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

       Class superClazz = clazz.getSuperclass();
       // get super class' fields recursively
       if(superClazz != null) fields.addAll(getAllFields(superClazz));

       return fields;
   }
    
    public static Field getField(Class clazz, String name) throws NoSuchFieldException {
       // get all fields of the class (but not inherited fields)
       Field f = null;
        try {
            f = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException | SecurityException ex) {
            // ignore
        }
       
       if (f!=null) return f;
       
       Class superClazz = clazz.getSuperclass();
       // get super class' fields recursively
       if (superClazz != null) return getField(superClazz, name);
       else throw new NoSuchFieldException();
    }
        
    public static Class unPrimitivize(Class c) {
        if(c.isPrimitive()) {
            if(c.equals(boolean.class)) return Boolean.class;
            if(c.equals(int.class)) return Integer.class;
            if(c.equals(float.class)) return Float.class;
            if(c.equals(double.class)) return Double.class;
            if(c.equals(long.class)) return Long.class;
            if(c.equals(byte.class)) return Byte.class;
            if(c.equals(short.class)) return Short.class;
            if(c.equals(char.class)) return Character.class;
        }
        return c;
    }
}
