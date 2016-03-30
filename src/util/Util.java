/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.util.Duration;

import org.jaudiotagger.tag.images.Artwork;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Rendering;
import util.dev.TODO;

import static java.lang.Math.*;
import static org.slf4j.LoggerFactory.getLogger;
import static util.dev.TODO.Purpose.BUG;

/**
 * Provides general purpose utility methods.
 *
 * @author _Plutonium
 */
public interface Util {

    /** Golden ratio - {@code 1.6180339887}. */
    double GR = 1.6180339887;

    /**
     * Method equivalent to object's equal method, but if both objects are null
     * they are considered equal as well.
     * <p/>
     * Equivalent to {@code (a==b) || (a!=null && a.equals(b))}
     *
     * @return true iff objects are equal or both null
     */
    static boolean nullEqual(Object a, Object b) {
        return (a==b) || (a!=null && a.equals(b));
    }

    /**
     * Compares two Artwork objects.
     * Artwork's equals() method does not return true properly. Use this method
     * instead.
     * <p/>
     * Method is deprecated as Artwork should not be used anyway. The method
     * works well though.
     *
     * @return true iff artwork are equal
     */
    @Deprecated
    static boolean equals(Artwork art1, Artwork art2) {
        if (art1 == null && art2 == null) return true;
        if (art1 == null || art2 == null) return false;
        return Arrays.equals(art1.getBinaryData(), art2.getBinaryData());
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
     *
     * @return formatted duration
     */
    static String formatDuration(Duration duration) {
        double sec_total = duration.toMillis()/1000;
        int seconds = (int) sec_total %60;
        int minutes = (int)((sec_total-seconds) /60) %60;
        int hours   = (int)(sec_total-seconds-60*minutes) /3600;

        if(hours>99)
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else if (hours>0)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        else if(minutes>0)
            return String.format("%02d:%02d", minutes, seconds);
        else
            return String.format("00:%02d",seconds);
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
     *
     * @return formatted duration
     */
    static String formatDuration(Duration duration, boolean include_zeros) {
        if (include_zeros) return formatDuration(duration);

        double sec_total = duration.toMillis()/1000;
        int seconds = (int) sec_total %60;
        int minutes = (int)((sec_total-seconds) /60) %60;
        int hours   = (int)(sec_total-seconds-60*minutes) /3600;

        if(hours>99)
            return String.format("%3d:%2d:%2d", hours, minutes, seconds);
        else if(hours>0)
            return String.format("%2d:%2d:%2d", hours, minutes, seconds);
        else if (minutes>0)
            return String.format("%2d:%2d", minutes, seconds);
        else
            return String.format("%2d",seconds);
    }

    /**
     * Returns empty string if string meets is-empty criteria according to {@link #shouldBeEmpty(String)}
     * <br>
     * More formally: {@code (shouldBeEmpty(str)) ? "" : str;}
     *
     * @param s input string
     * @return "" if string should be empty or the string itself
     */
    static String emptyOr(String s) {
        return shouldBeEmpty(s) ? "" : s;
    }

    /**
     * Broader check for emptiness of String object.
     * <br/>
     * Criteria:
     * <ul>
     * <li> null
     * <li> ""
     * <li> whitespace only
     * <li> "null"
     * <ul/>
     *
     * @param s string to check.
     * @return true iff any of the criteria is met.
     */
    static boolean shouldBeEmpty(String s) {
        return s == null || s.trim().isEmpty() || s.equalsIgnoreCase("null");
    }

    static boolean equalsNoCase(String text, String phrase) {
        return text.equalsIgnoreCase(phrase);
    }

    static boolean equalsNoCase(String text, String phrase, boolean ignore) {
        return ignore ? text.equals(phrase) : equalsNoCase(text, phrase);
    }

    static boolean startsWithNoCase(String text, String phrase) {
        return text.toLowerCase().startsWith(phrase.toLowerCase());
    }

    static boolean startsWithNoCase(String text, String phrase, boolean ignore) {
        return ignore ? text.startsWith(phrase) : startsWithNoCase(text, phrase);
    }

    static boolean endsWithNoCase(String text, String phrase) {
        return text.toLowerCase().startsWith(phrase.toLowerCase());
    }

    static boolean endsWithNoCase(String text, String phrase, boolean ignore) {
        return ignore ? text.endsWith(phrase) : endsWithNoCase(text, phrase);
    }

    static boolean containsNoCase(String text, String phrase) {
        return text.toLowerCase().contains(phrase.toLowerCase());
    }

    static boolean containsNoCase(String text, String phrase, boolean ignore) {
        return ignore ? text.contains(phrase) : containsNoCase(text, phrase);
    }

    /**
     * Checks and formats String so it can be safely used for naming a File.
     * Replaces all forbidden characters with "_".
     */
    static String filenamizeString(String str) {
        String out = str;
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
    static String capitalize(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
    static String capitalizeStrong(String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Converts enum constant to 'human readable' string.
     * <ul>
     * <li> first letter upper case,
     * <li> other letters lower case,
     * <li> '_' into ' '
     * </ul>
     */
    static String enumToHuman(Enum e) {
        return capitalizeStrong(e.name().replace('_', ' '));
    }

    /** Same as {@link #enumToHuman(java.lang.Enum)}, for String. */
    static String enumToHuman(String s) {
        return capitalizeStrong(s.replace('_', ' '));
    }

    /** @return true if the string is palindrome (empty string is palindrome) */
    static boolean isPalindrome(String s) {
        int n = s.length();
        for( int i = 0; i < n/2; i++ )
            if (s.charAt(i) != s.charAt(n-i-1)) return false;
        return true;
    }

    static boolean isNonEmptyPalindrome(String s) {
        return !s.isEmpty() && isPalindrome(s);
    }

    /** Convenience method. Equivalent to: loadImage(file, size, size); */
    static Image loadImage(File file, double size) {
        return loadImage(file, size, size);
    }

    /**
     * Loads image file. with requested size.
     * <p/>
     * Loads File object into Image object of desired size
     * (aspect ratio remains unaffected) to reduce memory consumption.
     * For example it is possible to use {@link Screen} class to find out
     * screen properties to dynamically set optimal resolution or limit it even
     * further for small thumbnails, where intended size is known.
     *
     * @param file file to load.
     * @param width target width.
     * @param height target height. Use 0 or negative to use original image size.
     * The size will be clipped to original if it is greater.
     * @throws IllegalArgumentException when on fx thread
     * @return loaded image or null if file null or not a valid image source.
     */
    static Image loadImage(File file, double width, double height) {
        if (file == null) return null;

        if(Platform.isFxApplicationThread())
            util.dev.Util.log(Util.class).warn("Loading image on FX thread!");

        if(file.getPath().endsWith("psd")) {
           return loadImageFull(file, width, height, false);
        } else {
            return loadImageThumb(file, width, height);
        }
    }

    static Image loadImageThumb(File file, double width, double height) {
        if (file == null) return null;

        if(Platform.isFxApplicationThread())
            util.dev.Util.log(Util.class).warn("Loading image on FX thread!");

        // negative values have same effect as 0, 0 loads image at its size
        int W = max(0,(int)width);
        int H = max(0,(int)height);
        boolean loadFullSize = W == 0 && H == 0;

        // psd special case
        if(!file.getPath().endsWith("psd")) {
            return imgImplLoadFX(file, W, H, loadFullSize);
        } else {
            ImageReader reader = null;
            try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return null;

                reader = readers.next();
                reader.setInput(input);
                int ii = reader.getMinIndex(); // 1st image index
                boolean thumbHas = imgImplHasThumbnail(reader, ii);
                int thumbW = thumbHas ? 0 : reader.getThumbnailWidth(ii,0),
                    thumbH = thumbHas ? 0 : reader.getThumbnailHeight(ii,0);
                boolean thumbUse = !loadFullSize && thumbHas && (width<=thumbW && height<=thumbH);

                BufferedImage i;
                if(thumbUse) {
                    i = reader.readThumbnail(ii,0);
                } else {
                    ImageReadParam irp = new ImageReadParam();
                    int sw = reader.getWidth(ii)/W;
                    int sh = reader.getHeight(ii)/H;
                    int px = loadFullSize ? 1 : max(1,max(sw,sh)*2/3); // quality == 2/3 == ok, great performance
                    irp.setSourceSubsampling(px,px, 0,0);
                    i = reader.read(ii,irp);

                    // scale, also improves quality, very quick
                    if(!loadFullSize)
                        i = imgImplScale(i, W, H, Rendering.SPEED);
                 }
                 return SwingFXUtils.toFXImage(i, null);

            } catch(IndexOutOfBoundsException | IOException e) {
                return null;
            } finally {
                if(reader!=null) reader.dispose();
            }
        }
    }

    static Image loadImageFull(File file, double width, double height) {
        return loadImageFull(file, width, height, true);
    }

    private static Image loadImageFull(File file, double width, double height, boolean thumbLoadedBefore) {
        if (file == null) return null;

        if(Platform.isFxApplicationThread())
            util.dev.Util.log(Util.class).warn("Loading image on FX thread!");

        // negative values have same effect as 0, 0 loads image at its size
        int W = max(0,(int)width);
        int H = max(0,(int)height);
        boolean loadFullSize = W == 0 && H == 0;

        // psd special case
        if(!file.getPath().endsWith("psd")) {
            return null;
        } else {
            ImageReader reader = null;
            try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return null;

                reader = readers.next();
                reader.setInput(input);
                int ii = reader.getMinIndex(); // 1st image index
                boolean thumbHas = imgImplHasThumbnail(reader, ii);
                int thumbW = thumbHas ? 0 : reader.getThumbnailWidth(ii,0),
                    thumbH = thumbHas ? 0 : reader.getThumbnailHeight(ii,0);
                boolean thumbUse = !loadFullSize && thumbHas && (width<=thumbW && height<=thumbH);

                BufferedImage i;
                if(thumbUse) {
                    if(thumbLoadedBefore) return null;
                    i = reader.readThumbnail(ii,0);
                } else {
                    int sw = reader.getWidth(ii)/W;
                    int sh = reader.getHeight(ii)/H;
                    int px = loadFullSize ? 1 : max(1,max(sw,sh)/3);// quality 3 == great, with ok performance
                    // max quality is px==1, but quality/performance ratio would suck
                    // the only quality issue is with halftone patterns (e.g. manga),
                    // they really ask for max quality
                    ImageReadParam irp = new ImageReadParam();
                    irp.setSourceSubsampling(px,px, 0,0);
                    i = reader.read(ii,irp);

                    // scale, also improves quality, fairly quick
                    if(!loadFullSize)
                        i = imgImplScale(i, W, H, Rendering.QUALITY);
                 }
                 return SwingFXUtils.toFXImage(i, null);

            } catch(IndexOutOfBoundsException | IOException e) {
                return null;
            } finally {
                if(reader!=null) reader.dispose();
            }
        }
    }

    /** Scales image to requested size, returning new image instance and flushing the old. Size must not be 0. */
    private static BufferedImage imgImplScale(BufferedImage image, int W, int H, Rendering rendering) {
        try {
            BufferedImage i = Thumbnails.of(image)
//                    .scalingMode(ScalingMode.BICUBIC) // default == best?, javadoc sux...
                    .size(W,H).keepAspectRatio(true)
                    .rendering(rendering)
                    .asBufferedImage();
            image.flush();
            return i;
        } catch (IOException ex) {
            return image;
        }
    }

    /** Returns true if the image has at least 1 embedded thumbnail of any size. */
    private static boolean imgImplHasThumbnail(ImageReader reader, int ii) {
        try {
            reader.hasThumbnails(ii); // throws exception -> no thumb
            return true;
        } catch(IOException | IndexOutOfBoundsException e){
            return false;
        }
    }

    /** Loads javafx {@link Image}. */
    private static Image imgImplLoadFX(File file, int W, int H, boolean loadFullSize) {
        if (loadFullSize)
            return new Image(file.toURI().toString());
        else {
            // find out real image file resolution
            Dimension d = getImageDim(file);
            int w = d==null ? Integer.MAX_VALUE : d.width;
            int h = d==null ? Integer.MAX_VALUE : d.height;

            // lets not surpass real size (Image unfortunately does that if we do not stop it)
            int fin_width = min(W,w);
            int fin_height = min(H,h);
            return new Image(file.toURI().toString(), fin_width, fin_height, true, true, true);
        }
    }

    /**
     * Returns image size in pixels or null if unable to find out. Does not read whole image into
     * memory. It still involves i/o.
     */
    @TODO(purpose = BUG, note = "suppressed by catching NullPointerException, dirty hack")
    static Dimension getImageDim(File f) {
        // see more at:
        // http://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java
        Dimension result = null;
        String suffix = util.file.Util.getSuffix(f.toURI());
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            try( ImageInputStream stream = ImageIO.createImageInputStream(f) ) {
                reader.setInput(stream);
                int ii = reader.getMinIndex(); // 1st image index
                int width = reader.getWidth(ii);
                int height = reader.getHeight(ii);
                result = new Dimension(width, height);
            } catch (IOException | NullPointerException e) {
                getLogger(Util.class).warn("Problem finding out image size {}", f ,e);
                // we need to catch NullPointerException as well, seems to be a bug, stacktrace below:
                // java.lang.NullPointerException: null
                //	at java.awt.color.ICC_Profile.activateDeferredProfile(ICC_Profile.java:1092) ~[na:na]
                //	at java.awt.color.ICC_Profile$1.activate(ICC_Profile.java:745) ~[na:na]
                //	at sun.java2d.cmm.ProfileDeferralMgr.activateProfiles(ProfileDeferralMgr.java:95) ~[na:na]
                //	at java.awt.color.ICC_Profile.getInstance(ICC_Profile.java:778) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.setImageData(JPEGImageReader.java:658) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.readImageHeader(Native Method) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.readNativeHeader(JPEGImageReader.java:610) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.checkTablesOnly(JPEGImageReader.java:347) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.gotoImage(JPEGImageReader.java:482) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.readHeader(JPEGImageReader.java:603) ~[na:na]
                //	at com.sun.imageio.plugins.jpeg.JPEGImageReader.getWidth(JPEGImageReader.java:717) ~[na:na]
            } finally {
                reader.dispose();
            }
        } else {
            getLogger(Util.class).warn("No reader found for given file: {}", f);
        }

        return result;
    }

    /**
     * Logarithm
     * @param base of the log
     * @param i number to calculate log for
     * @return base specified logarithm of the number
     */
    static int log(int base, int i) {
        short p = 0;
        while(pow(base, p) <= i)
            p++;
        return p;
    }

    /**
     * Return number of digits of the number.
     */
    static int digits(int number) {
        int x = number;
        int cifres = 0;
        while (x > 0) {
            x /= 10;
            cifres++;
        }
        return cifres;
    }

    /**
     * Creates zero-padded string - string of a number with '0' added in to
     * maintain consistency in number of length.
     *
     * @param n number to turn onto zero-padded string
     * @param max number to zero-pad to
     * @param ch character to use. Notable characters are: ' ' or '0'
     */
    static String zeroPad(int n, int max, char ch) {
        int diff = digits(max) - digits(n);
        String prefix = "";
        for (int i=1; i<=diff; i++)
            prefix += ch;
        return prefix + String.valueOf(n);
    }

    /**
     * Examples: 9 for 1-10, 99 for 10-99, 999 for numbers 100-999, etc.
     * @return largest number of the same decimal length as specified number.
     */
    static int decMin1(int n) {
        // normally we would do the below
        // return n==0 ? 0 : (int) (pow(10, 1+digits(n))-1);
        // but why not make this perform faster
        if(n==0) return n;
        n = abs(n);
        if(n<10) return 9;
        else if(n<100) return 99;
        else if(n<1000) return 999;
        else if(n<10000) return 9999;
        else if(n<100000) return 99999;
        else if(n<1000000) return 999999;
        else return (int) (pow(10, 1+digits(n))-1);
    }

    /**
     * Returns file itself if exists or its existing parent recursively. If
     * null or no parent exists, returns application location.
     */
    static File getExistingParent(File f, File defaultFile) {
        if (f==null) return defaultFile;
        if (f.exists()) return f;
        else return getExistingParent(f.getParentFile(), defaultFile);
    }

    /** @return {@code max(min,min(i,max))} */
    static int clip(short min, short i, short max) {
        return max(min,min(i,max));
    }

    /** @return {@code max(min,min(i,max))} */
    static int clip(int min, int i, int max) {
        return max(min,min(i,max));
    }

    /** @return {@code max(min,min(i,max))} */
    static long clip(long min, long i, long max) {
        return max(min,min(i,max));
    }

    /** @return {@code max(min,min(i,max))} */
    static float clip(float min, float i, float max) {
        return max(min,min(i,max));
    }

    /** @return {@code max(min,min(i,max))} */
    static double clip(double min, double i, double max) {
        return max(min,min(i,max));
    }

    /** @return true iff number belongs to the interval inclusive, else false */
    static boolean isInRangeInc(short i, short min, short max) {
        return i>=min && i <=max;
    }

    /** @return true iff number belongs to the interval inclusive, else false */
    static boolean isInRangeInc(int i, int min, int max) {
        return i>=min && i <=max;
    }

    /** @return true iff number belongs to the interval inclusive, else false */
    static boolean isInRangeInc(long i, long min, long max) {
        return i>=min && i <=max;
    }

    /** @return true iff number belongs to the interval inclusive, else false */
    static boolean isInRangeInc(float i, float min, float max) {
        return i>=min && i <=max;
    }

    /** @return true iff number belongs to the interval inclusive, else false */
    static boolean isInRangeInc(double i, double min, double max) {
        return i>=min && i <=max;
    }

    /** @return true iff number belongs to the interval exclusive, else false */
    static boolean isInRangeExc(short i, short min, short max) {
        return i>min && i <max;
    }

    /** @return true iff number belongs to the interval exclusive, else false */
    static boolean isInRangeExc(int i, int min, int max) {
        return i>min && i <max;
    }

    /** @return true iff number belongs to the interval exclusive, else false */
    static boolean isInRangeExc(long i, long min, long max) {
        return i>min && i <max;
    }

    /** @return true iff number belongs to the interval exclusive, else false */
    static boolean isInRangeExc(float i, float min, float max) {
        return i>min && i <max;
    }

    /** @return true iff number belongs to the interval exclusive, else false */
    static boolean isInRangeExc(double i, double min, double max) {
        return i>min && i <max;
    }

    /** @return true iff number is a valid 0-based index of the collection, else false */
    static boolean isInRange(int i, Collection<?> c) {
        return i>=0 && i<c.size();
    }

    /** @return true iff number is a valid 0-based index of the array, else false */
    static <T> boolean isInRange(int i, T[] c) {
        return i>=0 && i<c.length;
    }

    /** @return element at specified index or null if out of bounds or element null */
    static <T> T getAt(int i, List<T> list) {
        return isInRange(i, list) ? list.get(i) : null;
    }

    /** @return element at specified index or null if out of bounds or element null */
    static <T> T getAt(int i, T[] array) {
        return isInRange(i, array) ? array[i] : null;
    }

    /** Returns sum of squares of all numbers. */
    static double sumsqr(double... numbers) {
        double Σ = 0;
        for(double x : numbers)
            Σ += x*x;
        return Σ;
    }

    /** Returns {@code sqrt(a^2 + b^2)}. */
    static double pyth(double a, double b) {
        return Math.sqrt(a*a+b*b);
    }

    /**
     * Returns random element from the list and removes it from the list.
     *
     * @return random element from the list.
     * @throws IndexOutOfBoundsException if list empty
     */
    private static <T> T rand(List<T> list) {
        int i = (int)Math.floor(random()*list.size());
        T t = list.get(i);
        list.remove(t);
        return t;
    }

    /**
     * Returns n random elements from the source list. Source list wont be changed.
     *
     * @return specified number of random elements from the list
     * @throws IndexOutOfBoundsException if list doesnt have enough elements
     */
    private static <T> ArrayList<T> randN(int amount, List<T> source){
        if(amount>=source.size()) return new ArrayList<>(source);
        else {
            ArrayList<T> all = new ArrayList<>(source);
            ArrayList<T> l = new ArrayList<>();
            for(int i=0;i<amount;i++) l.add(rand(all));
            return l;
        }
    }

}