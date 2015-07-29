/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import org.jaudiotagger.tag.images.Artwork;
import util.File.FileUtil;
import unused.Log;
import static util.functional.Util.list;
import util.functional.functor.FunctionC;
import static util.functional.functor.FunctionC.composable;

/** 
 * Provides static utility methods for various purposes. 
 * 
 * @author _Plutonium
 */
public class Util {
    
    /**
     * Method equivalent to object's equal method, but if both objects are null
     * they are considered equal as well.
     * Equivalent to {@code (o1==null && o2==null) || (o1!=null && o1.equals(o2));}
     */
    public static boolean nullEqual(Object o1, Object o2) {
        return (o1==null && o2==null) || (o1!=null && o1.equals(o2));
    }
    
/********************************** DEBUG *************************************/
    
    /**
     * Simple black background with no insets or radius. use for layout debugging
     * Equivalent to {@code new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY));}
     */
    public static Background SIMPLE_BGR = new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY));
    
    /**
     * Simple black border with no radius
     * Equivalent to {@code new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));}
     */
    public static Border BORDER_SIMPLE = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
    
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
     * @param duration
     * @param include_zeros
     * @return 
     */
    public static String formatDuration(Duration duration, boolean include_zeros) {
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
        - null
        - "null", "isNULL" and other combinations
        - ""
        - whitespaceOnly.
     * 
     * @param str String to check.
     * @return true if any of the above is met.
     */
    public static boolean shouldBeEmpty(String str) {
        return str == null || str.equalsIgnoreCase("null") || str.trim().isEmpty();       
    }
    
    /**
     * Checks and formats String so it can be safely used for naming a File.
     * Replaces all forbidden characters with "_".
     * @param str
     * @return 
     */
    public static String filenamizeString(String str) {
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
     * Converts enum constant to 'human readable' string.
     * <ul>
     * <li> first letter upper case, 
     * <li> other letters lower case, 
     * <li> '_' into ' '
     * </ul>
     */
    public static String enumToHuman(Enum e) {
        return capitalizeStrong(e.name().replace('_', ' '));
    }
    
    /** Same as {@link #enumToHuman(java.lang.Enum)}, for String. */
    public static String enumToHuman(String s) {
        return capitalizeStrong(s.replace('_', ' '));
    }
    
    /** @return true if the string is palindrome (empty string is palindrome) */
    public static boolean isPalindrome(String s) {
        int n = s.length();
        for( int i = 0; i < n/2; i++ )
            if (s.charAt(i) != s.charAt(n-i-1)) return false;
        return true;    
    }
    public static boolean isNonEmptyPalindrome(String s) {    
        return s.isEmpty() ? false : isPalindrome(s);
    }
    
    /** Convenience method. Equivalent to: loadImage(file, size, size); */
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
     * @param width 
     * @param height to resize image's width to. Use 0 or negative to use original image size.
     * The size will be clipped to original if it is greater.
     * @return loaded image or null if file null or not a valid image source.
     */
    public static Image loadImage(File file, double width, double height) {
        if (file == null) return null;
        width = max(0,width);
        height = max(0,height);
        if (width == 0 && height == 0)
            return new Image(file.toURI().toString());
        else {
            // find out real image file resolution
            Dimension d = getImageDim(file);
            int w = d==null ? Integer.MAX_VALUE : d.width;
            int h = d==null ? Integer.MAX_VALUE : d.height;

            // lets not surpass real size (Image unfortunately does that if we dont stop it)
            int fin_width = min((int)width,w);
            int fin_height = min((int)height,h);
            return new Image(file.toURI().toString(), fin_width, fin_height, true, true, true);
        }
    }
    
    // thx: http://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java
    public static Dimension getImageDim(File f) {
        Dimension result = null;
        String suffix = FileUtil.getSuffix(f.toURI());
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            try {
                ImageInputStream stream = new FileImageInputStream(f);
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                result = new Dimension(width, height);
            } catch (IOException e) {
                Log.warn("Problem finding out image size" + e.getMessage());
            } finally {
                reader.dispose();
            }
        } else
            throw new RuntimeException("No reader found for given file: " + f.getPath());

        return result;
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
        while(pow(base, p) <= i)
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
     * @param n number to turn onto zeropadded string
     * @param max number to zeropad to
     * @param ch character to use. Notable characters are: ' ' or '0'
     * @return 
     */
    public static String zeroPad(int n, int max, char ch) {
        int diff = digits(max) - digits(n);
        String out = "";
        for (int i=1; i<=diff; i++)
            out += ch;
        return out + String.valueOf(n);
    }
    
    /**
     * @return highest possible number of the same decadic length as specified
     * number in absolute value.
     * Examples:  9 for 1-10, 99 for 10-99, 999 for nubmers 100-999, etc...
     */
    public static int decMin1(int n) {
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
    public static File getExistingParent(File f, File defaultFile) {
        if (f==null) return defaultFile;
        if (f.exists()) return f;
        else return getExistingParent(f.getParentFile(), defaultFile);
    }
    
    /** @return {@code max(min,min(i,max))} */
    public static int clip(int min, int i, int max) {
        return max(min,min(i,max));
    }
    /** @return {@code max(min,min(i,max))} */
    public static long clip(long min, long i, long max) {
        return max(min,min(i,max));
    }
    /** @return {@code max(min,min(i,max))} */
    public static float clip(float min, float i, float max) {
        return max(min,min(i,max));
    }
    /** @return {@code max(min,min(i,max))} */
    public static double clip(double min, double i, double max) {
        return max(min,min(i,max));
    }
    
    
    /** Returns random element from the list and removes it from the list. 
    @return random element from the list. 
    @throws IndexOutOfBoundsException if list empty
    */
    private static <T> T rand(List<T> list) {
        int i = (int)Math.floor(random()*list.size());
        T t = list.get(i);
        list.remove(t);
        return t;
    }
    
    /** Returns n random elements from the source list. Source list wont be changed.
    @return specified number of random elements from the list
    @throws IndexOutOfBoundsException if list doesnt have enough elements */
    private static <T> ArrayList<T> randN(int amount, List<T> source){
        if(amount>=source.size()) return new ArrayList<>(source);
        else {
            ArrayList<T> all = new ArrayList<>(source);
            ArrayList<T> l = new ArrayList<>();
            for(int i=0;i<amount;i++) l.add(rand(all));
            return l;
        }
    }
    
/******************************** GRAPHICS ************************************/
    
    /** Shortcut for:
        <pre>{@code
        AnchorPane.setTopAnchor(n, a);
        AnchorPane.setRightAnchor(n, a);
        AnchorPane.setBottomAnchor(n, a);
        AnchorPane.setLeftAnchor(n, a);
        }</pre>
    */
    public static void setAnchors(Node n, double a) {
        AnchorPane.setTopAnchor(n, a);
        AnchorPane.setRightAnchor(n, a);
        AnchorPane.setBottomAnchor(n, a);
        AnchorPane.setLeftAnchor(n, a);
    }
    
    /** Shortcut for:
        <pre>{@code
            AnchorPane.setTopAnchor(n, top);
            AnchorPane.setRightAnchor(n, right);
            AnchorPane.setBottomAnchor(n, bottom);
            AnchorPane.setLeftAnchor(n, left);
        }</pre>
    */
    public static void setAnchors(Node n, double top, double right, double bottom, double left) {
        AnchorPane.setTopAnchor(n, top);
        AnchorPane.setRightAnchor(n, right);
        AnchorPane.setBottomAnchor(n, bottom);
        AnchorPane.setLeftAnchor(n, left);
    }
    
    public static void setScaleXY(Node n, double s) {
        n.setScaleX(s);
        n.setScaleY(s);
    }
    
    public static void setScaleXY(Node n, double x, double y) {
        n.setScaleX(x);
        n.setScaleY(y);
    }
    
    /**
     * Creates column that indexes rows from 1 and is right aligned. The column 
     * is of type Void - table data type agnostic.
     * @param name name of the column. For example "#"
     * @return the column
     */
    public static<T> TableColumn<T,Void> createIndexColumn(String name) {
        TableColumn<T,Void> c = new TableColumn(name);
               c.setSortable(false);
               c.setCellFactory( column -> new TableCell<T,Void>(){
                {
                    setAlignment(CENTER_RIGHT);
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText(null);
                    else setText(String.valueOf(getIndex()+1)+ ".");
                }
            });
        return c;
    }
    
    /**
     * Creates default cell factory, which sets cell text to provided text when 
     * cells text equals "". This is to differentiate between empty cell and nonempty
     * cell with 'empty' value.
     * For example: '<empty cell>'
     * @param empty_value
     * @return 
     */
    public static Callback<TableColumn<?,?>, TableCell<?,?>> EMPTY_TEXT_DEFAULT_CELL_FACTORY(String empty_value) { 
       return new Callback<TableColumn<?,?>, TableCell<?,?>>() {
            @Override public TableCell<?,?> call(TableColumn<?,?> param) {
                return new TableCell<Object,Object>() {
                    @Override protected void updateItem(Object item, boolean empty) {
                        if (item == getItem()) return;

                        super.updateItem(item, empty);

                        if (item == null) {
                            super.setText(null);
                            super.setGraphic(null);
                        } else if ("".equals(item)) {
                            super.setText(empty_value);
                            super.setGraphic(null);
                        } else if (item instanceof Node) {
                            super.setText(null);
                            super.setGraphic((Node)item);
                        } else {
                            super.setText(item.toString());
                            super.setGraphic(null);
                        }
                    }
                };
            }
        };
    }
    
    /**
     * Same as {@link #DEFAULT_ALIGNED_CELL_FACTORY(javafx.geometry.Pos)}, but
     * the alignment is inferred from the type of element in the cell (not table
     * or column, because we are aligning cell content) in the following way: 
     * String content is aligned to CENTER_LEFT and the rest CENTER_RIGHT.
     * <p>
     * The factory will need to be cast if it its generic types are declared.
     * 
     * @param type for cell content.
     */
    public static<T,O> FunctionC<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Class<O> type, String no_val_text) {
        Pos a = type.equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
        return cellFactoryAligned(a, no_val_text);
    }
    
    /**
     * Returns {@link TableColumn.DEFAULT_CELL_FACTORY} (the default factory used
     * when no factory is specified), aligning the cell content to specified value.
     * <p>
     * The factory will need to be cast if it its generic types are declared.
     * @param a cell alignment
     * @return 
     */
    public static<T,O> FunctionC<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Pos a, String no_val_text) {
        return (FunctionC) composable(EMPTY_TEXT_DEFAULT_CELL_FACTORY(no_val_text))
                           .andApply(cell -> cell.setAlignment(a));
    }
    
    /**
     * Convenience method to make it easier to select given rows of the
     * TableView via its SelectionModel.
     * This methods provides alternative to TableViewSelectionModel.selectIndices()
     * that requires array parameter. This method makes the appropriate conversions
     * and selects the items using List parameter
     * <p>
     * After the method is invoked only the provided rows will be selected - it
     * clears any previously selected rows.
     * @param selectedIndexes
     * @param selectionModel
     */
    public static void selectRows(List<Integer> selectedIndexes, TableViewSelectionModel<?> selectionModel) {
        selectionModel.clearSelection();
        int[] newSelected = new int[selectedIndexes.size()];
        for (int i = 0; i < selectedIndexes.size(); i++) {
            newSelected[i] = selectedIndexes.get(i);
        }
        if (newSelected.length != 0) {
            selectionModel.selectIndices(newSelected[0], newSelected);
        }
    }
    
    public static final EventHandler<MouseEvent> consumeOnSecondaryButton = e-> {
        if (e.getButton()==MouseButton.SECONDARY) e.consume();
    };
    
    public static MenuItem menuItem(String text, EventHandler<ActionEvent> action) {
        MenuItem i = new MenuItem(text);
                 if(action!=null) i.setOnAction(action);
        return i;
    }
    public static MenuItem menuItem(String text, Runnable action) {
        return menuItem(text, action==null ? null : a -> action.run());
    }
    /** 
    * Generates menu items from list of services or actions. Use to populate
    * context menu dynamically.
    * <p>
    * For example context menu that provides menu items for searching given
    * text on the web, using different search engines. What we want is to
    * generate menu items each executing the same type action.
    * 
    * @param <A> service or action that can facilitates the action
    * @param from list of service
    * @param toStr service to string converter for menu item text
    * @param menu item click action, uses service A possibly on some parameter
    * like selected table item
    * @return 
    */
    public static <A> MenuItem[] menuItems(List<A> from, Function<A,String> toStr, Consumer<A> action) {
        return from.stream()
            .map(t -> menuItem(toStr.apply(t), e -> action.accept(t)))
            .toArray(MenuItem[]::new);
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
    
    /**
     * Returns all declared methods of the class including inherited ones.
     * Equivalent to union of declared fields of the class and all its
     * superclasses.
     */
    public static List<Method> getAllMethods(Class clazz) {
       List<Method> methods = new ArrayList();
       // get all fields of the class (but not inherited fields)
       methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));

       Class superClazz = clazz.getSuperclass();
       // get super class' fields recursively
       if(superClazz != null) methods.addAll(getAllMethods(superClazz));

       return methods;
   }
    
    public static <A extends Annotation> Method getMethodAnnotated(Class c, Class<A> ca) {
        for(Method m: c.getDeclaredMethods()) {
            A a = m.getAnnotation(ca);
            if(a!=null) return m;
        }
        return null;
    }
    
    /** 
     * Returns all superclasses and interfaces.
     * @return list containing all superclasses
     * @see #getSuperClassesInc(java.lang.Class)
     */
    public static List<Class> getSuperClasses(Class c) {
        return getSuperClasses(c, new ArrayList());
    }
    /** 
     * Returns all superclasses and interfaces and the class.
     * @return list containing the class and all its superclasses
     * @see #getSuperClasses(java.lang.Class) 
     */
    public static List<Class> getSuperClassesInc(Class c) {
        return getSuperClasses(c, list(c));
    }
    
    private static List<Class> getSuperClasses(Class c, List<Class> cs) {
        Class sc = c.getSuperclass();
        if(sc!=null) {
            cs.add(sc);
            getSuperClasses(sc, cs);
        }
        Class[] is = c.getInterfaces();
        for(Class i : is) {
            cs.add(i);
            getSuperClasses(i, cs);
        }
        return cs;
    }
    
    /**
     * Converts primitive to wrappers, otherwise does nothing. 
     * @param c
     * @return Object class of given class or class itself if not primitive.
     */
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
    
    /**
     * Returns i-th generic parameter of the field starting from 0. 
     * For example {@code Integer for List<Integer>}
     * @param f
     * @return 
     */
    public static Class getGenericType(Field f, int i) {
        ParameterizedType pType = (ParameterizedType) f.getGenericType();
        Class<?> genericType = (Class<?>) pType.getActualTypeArguments()[i];
        return genericType;
    }
    
    /**
     * Returns i-th generic parameter of the class starting from 0.
     * For example Integer for class {@code IntegerList extends List<Integer>}
     * <p>
     * Will NOT work on variables, using getClass() method on them.
     * 
     * @param c
     * @param i
     * @return 
     */
    public static Class getGenericClass(Class c, int i) {
        return (Class) ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments()[i];
    }
    
    /**
     * Same as {@link #getGenericClass(java.lang.Class, int)} but for interfaces.
     * Returns p-th generic parameter of the i-th interface of c class starting from 0.
     * 
     * @param c
     * @param i
     * @param p
     * @return 
     */
    public static Class getGenericInterface(Class c, int i, int p) {
        return (Class) ((ParameterizedType) c.getGenericInterfaces()[i]).getActualTypeArguments()[p];
    }
    
    /** 
     * Returns field named n declared in class c.
     * 
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class 
     */
    public static Field getField(Class c, String n) throws NoSuchFieldException {
       // get all fields of the class (but not inherited fields)
       Field f = null;
        try {
            f = c.getDeclaredField(n);
        } catch (NoSuchFieldException | SecurityException ex) {
            // ignore
        }
       
       if (f!=null) return f;
       
       Class superClazz = c.getSuperclass();
       // get super class' fields recursively
       if (superClazz != null) return getField(superClazz, n);
       else throw new NoSuchFieldException();
    }
    
    /**
     * Set field named f of the object o to value v.
     * 
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class 
     * @throws RuntimeException if reflection error occurs
     */
    public static void setField(Object o, String f, Object v) {
        setField(o.getClass(), o, f, v);
    }
    
    /**
     * Set field named f of the object o declared in class c to value v.

     * 
     * @implSpec the field can be declared in the class or any of its supoerclasses
     * as opposed to standard reflection behavior which checks only the specified class 
     * @throws RuntimeException if reflection error occurs
     */
    public static void setField(Class c, Object o, String f, Object v) {
        try {
            Field fl = getField(c,f);
            fl.setAccessible(true);
            fl.set(o,v);
            fl.setAccessible(false);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
    
    /**
     * Renames declared enum sonstant using the mapper function on the enum
     * constant string.
     * <p>
     * This method effectively overrides both enum's toString() and valueOf() 
     * methods. It allows using arbitrary string values for enum constants,
     * but in toString/valueOf cpliant way.
     * <p>
     * Use in enum constructor. For example:
     * <p>
     * <pre>
     * {@code
     *  class MyEnum {
     *      A,
     *      B;
     * 
     *      public MuEnum() {
     *          mapEnumConstant(MyEnum.class, this, String::toLowerCase);
     *      }
     *  }
     * }
     * </pre>
     * <p>
     * @param <E>
     * @param c class
     * @param e enum constant/instance
     * @param mapper function to apply on the constant
     * @throws RuntimeException if reflection error occurs
     */
    public static<E extends Enum>void mapEnumConstant(E e, Function<E, String> mapper) {
        setField(e.getClass().getSuperclass(), e, "name", mapper.apply(e));
    }
    
    /**
     * Returns whether class is an enum. Works for 
     * enums with class method bodies (where Class.isEnum) does not work.
     * 
     * @return true if class is enum or false otherwise
     * @see #getEnumConstants(java.lang.Class)
     */
    public static boolean isEnum(Class c) {
        return c.isEnum() || (c.getEnclosingClass()!=null && c.getEnclosingClass().isEnum());
    }

    /**
     * Returns enum constants of an enum class in declared order. Works for 
     * enums with class method bodies (where Enum.getEnumConstants) does not work.
     * <p>
     * Always use {@link #isEnum(java.lang.Class)} before this method.
     * 
     * @param c
     * @return array of enum constants, never null
     * @throws IllegalArgumentException if class not an enum
     */
    public static <T> T[] getEnumConstants(Class c) {
        // handle enums
        if(c.isEnum()) return (T[]) c.getEnumConstants();

        // handle enum with class method bodies (they are not recognized as enums)
        else {
            Class ec = c.getEnclosingClass();
            if(ec!=null && ec.isEnum()) 
                return (T[]) ec.getEnumConstants();
            else 
                throw new IllegalArgumentException("Class " + c + " is not an Enum.");
        }
    }
}
