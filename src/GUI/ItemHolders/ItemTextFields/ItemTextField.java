/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemHolders.ItemTextFields;

import GUI.ItemHolders.ItemHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.controlsfx.control.textfield.CustomTextField;
import utilities.Parser.StringParser;
import utilities.functional.functor.UnProcedure;

/**
 * Customized {@link TextField} that stores an item. Normally a non-editable text
 * field that brings up a popup picker for tits item type.
 * <p>
 * Text field stores the item by providing String converting mechanism - a value
 * factory. The parsing doesn't have to be reversible as the original item is
 * contained. Although only Object -> String parsing is necessary, the class
 * still requires full fledged parser, although String -> Object parsing doesnt
 * have to be implemented.
 * <p>
 * It is recommended for classes to provide default parser implementations to
 * allow simple instantiation hiding any parsing related matters from the 
 * programmer. Different parser can be used to specify different string
 * representation of the objects.
 * <p>
 * It is possible to specify the converting mechanism in several ways. One can
 * provide default (if implemented) or custom string-item parser in the
 * constructor. Or create anonymous class and override {@link itemToString}
 * method. Or provide custom value factory which will always override the above.
 * <p>
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 * <p>
 * Useful for displaying property sheets and bring easy object selection
 * feature.
 * <p>
 * @param <T> type of item
 * <p>
 * @author Plutonium_
 */
public abstract class ItemTextField<T, P extends StringParser<T>> extends CustomTextField implements ItemHolder<T>{
    
    T item;
    private Class parser_class;
    private UnProcedure<T> onItemChange;
    private Callback<T, String> valueFactory;
    
    /**
     * Constructor. Creates instance of the item text field utilizing parser
     * of the provided type.
     */
    public ItemTextField() {
        // default implementation of value factory
        valueFactory = this::itemToString;   
        
        // set the button to the right & action
        setRight(new DialogButton());
        getRight().setOnMouseClicked( e -> onDialogAction());
        
        setEditable(false);
        
        //set same cass style as TextField
        getStyleClass().setAll(STYLECLASS());
    }
    
    /**
     * Constructor. Creates instance of the item text field utilizing parser
     * of the provided type. */
    public ItemTextField(Class<? extends P> parser_type) {
        this();
        parser_class = parser_type;
    }
    
    
    /** 
     * Behavior to be executed on dialog button click. Executes specified
     * method that gets ahold of new Item. */
    abstract void onDialogAction();
   
    /** 
     * Sets item for this text field. Sets text and prompt text according to
     * provided implementation.*/
    public void setItem(T _item) {
         if(_item!=null) {
             this.item = _item;
             String text = valueFactory.call(_item);    // use factory to convert
             setText(text);
             setPromptText(text);
             if(onItemChange!=null) onItemChange.accept(_item);
         }
    }
    
    /** 
     * Sets behavior to execute when item changes. The item change ignores
     * equality check and will fire even for same object to be set. */
    public void setOnItemChange(UnProcedure<T> _onFontChange) {
        onItemChange=_onFontChange;
    }

    /** @return current value displayed in this text field. */
    @Override
    public T getItem() {
        return item;
    }
    
    /**
     * Sets value factory that specifies how the item will be parsed to
     * String. This an alternative to using String Parser.
     * Default factory invokes {@link #itemToString} method. Invoking this method
     * will override that behavior.
     * @param value factory for converting item to string. It takes item as
     * a parameter and returns the String.
     */
    public void setValueFactory(Callback<T,String> factory) {
        valueFactory = factory;
    }
   
    /**
     * Default implementation uses provided parser. Default implementation of the
     * value factory invokes this method.
     * @param item
     * @return String as a representation of the item.
     * @throws RuntimeException if no parser is provided
     */
    String itemToString(T item) {
        // throw runtime exception if implementation incomplete
        // forces developer to provide one or the other
        if(parser_class == null) {
            throw new RuntimeException("No provided parsing implementation. At "
                    + "least one - parser or value factory - must be specified.");
        }
        
        // instantiate parser
        StringParser<T> parser;
        try {
            parser = (StringParser<T>) parser_class.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            return Objects.toString(item); // if parser fails to instantiate () shouldnt throw exception?
        }
        
        // parse
        if(item!=null)
            return parser.toS(item);
        else
            return "";
    }
    
/******************************************************************************/
   
   
    /** 
     * Returns style class as text field.
     * <p>
     * Should be: text-input, text-field.
     */
    private static List<String> STYLECLASS() {
        // debug (prints: text-input, text-field.)
//        new TextField().getStyleClass().forEach(System.out::println); 
        
        // general solution, but not optimal
//        return new TextField().getStyleClass();
        
        // manually
        List<String> out = new ArrayList();
                     out.add("text-input");
                     out.add("text-field");
        return out;
    }   
    
    
/******************************************************************************/

    /**
     * Button for calling dialogs, from within {@link ItemTextField}.
     * The button has its own css style class "dialog-button".
     * <p>
     * @author Plutonium_
     */
    public static class DialogButton extends StackPane {

        private static final String STYLE_CLASS = "dialog-button";

        public DialogButton() {
            this("");
        }

        public DialogButton(String text) {
            Region r = new Region();
                   r.getStyleClass().add(STYLE_CLASS);
                   r.setMinSize(0, 0);
                   r.setPrefSize(7, 6);
                   r.setMaxSize(7, 6);
            
            setPrefSize(22,22);
            getChildren().add(r);
        }
    }
}
