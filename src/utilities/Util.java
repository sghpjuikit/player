/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import javafx.scene.Node;
import static javafx.scene.control.ContentDisplay.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import main.App;
import org.jaudiotagger.tag.images.Artwork;
import utilities.Parser.File.FileUtil;

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
    public static Predicate<Object> NotNULL = Objects::nonNull;
    
    /** Simple Predicate returning true. Use in lambda. */
    public static Predicate<?> TRUE = o -> true;
    
    /** @return runnable that does nothing. */
    public static Runnable DO_NOTHING = () -> {};
    
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
            Dimension d = getImageDim(file);
            int w = d==null ? Integer.MAX_VALUE : d.width;
            int h = d==null ? Integer.MAX_VALUE : d.height;
            
            // lets not get over real size (Image unfortunately does that if we dont stop it)
            int fin_width = Math.min((int)width,w);
            int fin_height = Math.min((int)height,h);
            return new Image(file.toURI().toString(), fin_width, fin_height, true, true);
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
    
    /**
     * Returns file itself if exists or its existing parent recursively. If
     * null or no parent exists, returns application location.
     */
    public static File getExistingParent(File f) {
        if (f==null) return App.getLocation();
        if (f.exists()) return f;
        else return getExistingParent(f.getParentFile());
    }
    
/******************************** GRAPHICS ************************************/
    
    public static void setAPAnchors(Node n, double a) {
        AnchorPane.setTopAnchor(n, a);
        AnchorPane.setRightAnchor(n, a);
        AnchorPane.setBottomAnchor(n, a);
        AnchorPane.setLeftAnchor(n, a);
    }
    
    public static void setAPAnchors(Node n, double top, double right, double bottom, double left) {
        AnchorPane.setTopAnchor(n, top);
        AnchorPane.setRightAnchor(n, right);
        AnchorPane.setBottomAnchor(n, bottom);
        AnchorPane.setLeftAnchor(n, left);
    }
    
    /**
     * Returns copy of the selected items of the table. Because the original list
     * is observable, changes would show up if it was used as a parameter. Often,
     * we need a 'snapshot' of the selected items list at the moment and we dont
     * want that snapshot to mutate.
     * @param <T> type of element in the list
     * @param table_source
     * @return 
     */
    public static<T> List<T> copySelectedItems(TableView<T> table_source) {
        return new ArrayList(table_source.getSelectionModel().getSelectedItems());
    }
    
    /**
     * Creates column that indexes rows from 1 and is right aligned. The column 
     * is general and doesnt need to know what kind of data is in the table.
     * @param name name of the column. For example "#"
     * @return the column
     */
    public static<T,R> TableColumn<T,R> createIndexColumn(String name) {
        TableColumn indexColumn = new TableColumn(name);
        indexColumn.setSortable(false);
        indexColumn.setCellFactory( column -> 
            new TableCell(){
                {
                    // we want to align the index to the right, not left
                    setAlignment(CENTER_RIGHT);
                }
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setText("");
                    else setText(String.valueOf(getIndex()+1)+ ".");
                }
            }
        );
        return indexColumn;
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
    public static<T,O> Callback<TableColumn<T,O>,TableCell<T,O>> DEFAULT_ALIGNED_CELL_FACTORY(Class<O> type) {
        Pos al = type.equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
        return column -> {
            TableCell cell = TableColumn.DEFAULT_CELL_FACTORY.call(column);
                      cell.setAlignment(al);
            return cell;
        };
    }
    
    /**
     * Returns {@link TableColumn.DEFAULT_CELL_FACTORY} (the default factory used
     * when no factory is specified), aligning the cell content to specified value.
     * <p>
     * The factory will need to be cast if it its generic types are declared.
     * @param cell_alignment
     * @return 
     */
    public static<T,O> Callback<TableColumn<T,O>,TableCell<T,O>> DEFAULT_ALIGNED_CELL_FACTORY(Pos cell_alignment) {
        return column -> {
            TableCell cell = TableColumn.DEFAULT_CELL_FACTORY.call(column);
                      cell.setAlignment(cell_alignment);
            return cell;
        };
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
    
    public static MenuItem createmenuItem(String text, EventHandler<ActionEvent> actionHandler) {
        MenuItem i = new MenuItem(text);
                 i.setOnAction(actionHandler);
        return i;
    }
    
    public static Label createIcon(AwesomeIcon icon, int size, String tooltip, EventHandler<MouseEvent> onClick) {
        Label i = AwesomeDude.createIconLabel(icon,"",String.valueOf(size),String.valueOf(GUI.GUI.font.getValue().getSize()),CENTER);
              i.setOnMouseClicked(onClick);
        if(tooltip!=null && !tooltip.isEmpty()) i.setTooltip(new Tooltip(tooltip));
        return i;
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
     * For example Integer for {@code IntegerList extends List<Integer>}
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
    
/********************************** FUNCTIONAL ********************************/
    
    /**
     * Functional alternative to for cycle for collections.
     * <p>
     * Equivalent to Collection.forEach(), with additional parameter - index of
     * the element in the collection.
     * <p>
     * Maps all elements of the collection into index-element pairs and executes
     * the action for each. Indexes start at 0.
     * 
     * @param <T> element type
     * @param c
     * @param action 
     */
    public static<T> void forEachIndexed(Collection<T> c, BiConsumer<Integer,T> action) {
        int i=0;
        for(T item : c) {
            action.accept(i, item);
            i++;
        }
    }
    
    /**
     * Returns stream of elements mapped by the mapper from index-element pairs 
     * of specified collection. Indexes start at 0.
     * <p>
     * Functionally equivalent to: List.stream().map(item->new Pair(item,list.indexOf(item))).map(pair->mapper.map(p))
     * but avoiding the notion of a Pair or Touple, and without any collection
     * traversal to get indexes.
     * 
     * @param <T> element type
     * @param <R> result type
     * @param c
     * @param mapper
     * @return 
     */
    public static<T,R> Stream<R> forEachIndexedStream(Collection<T> c, BiFunction<Integer,T,R> mapper) {
        int i=0;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i++;
        }
        return b.build();
    }
    
    /**
     * More general version of {@link #forEachIndexed(java.util.Collection, utilities.functional.functor.BiCallback)}.
     * The index can now be of any type and how it changes is defined by a parameter.
     * @param <I> key type
     * @param <T> element type
     * @param <R> result type
     * @param c
     * @param initial_val  for example: 0
     * @param operation for example: number -> number++
     * @param mapper maps the key-object pair into another object
     * 
     * @return stream of mapped values by a mapper out of key-element pairs
     */
    public static<I,T,R> Stream<R> forEachIndexedStream(Collection<T> c, I initial_val, Callback<I,I> operation, BiFunction<I,T,R> mapper) {
        I i = initial_val;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i = operation.call(i);
        }
        return b.build();
    }

}
