/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.combobox;

import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import static java.util.Objects.requireNonNull;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.util.Duration.millis;
import static util.type.Util.getFieldValue;

/**
 * ComboBox with extra functions.
 * <ul>
 * <li> String converter
 * <li> Default text for empty value
 * <li> Searching & scrolling when typing
 * </ul>
 *
 * @author Martin Polakovic
 */
public class ImprovedComboBox<T> extends ComboBox<T> {

    /** String converter for cell value factory. Default is Object::toString */
    public final Function<T,String> toStringConverter;
    /** Text for when no value is selected. Default {@code "<none>"} */
    public final String emptyText;

    public ImprovedComboBox() {
        this(Object::toString);
    }

    public ImprovedComboBox(Function<T, String> toS) {
        this(toS, "<none>");
    }

    public ImprovedComboBox(Function<T, String> toS, String empty_text) {
        requireNonNull(toS);
        toStringConverter = toS;
        // we need to set the converter specifically or the combobox cell wont get updated sometimes
        setConverter(new javafx.util.StringConverter<T>() {

            @Override
            public String toString(T object) {
                return toStringConverter.apply(object);
            }

            @Override
            public T fromString(String string) {
                return (T) string;
            }
        });
        setCellFactory(view -> new ListCell<T>(){ // do not use ComboBoxListCell! causes problems!
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "<none>" : toStringConverter.apply(item));
            }
        });
        setButtonCell(getCellFactory().call(null));
        setValue(null);
        emptyText = empty_text;


        // we need this to obtains listView
        skinProperty().addListener(new ChangeListener<Skin<?>>() {
            @Override
            public void changed(ObservableValue<? extends Skin<?>> o, Skin<?> ov, Skin<?> nv) {
                listView = getFieldValue(getSkin(), ListView.class, "listView");
                skinProperty().removeListener(this);
            }
        });
        addEventHandler(KEY_PRESSED, e -> {
            KeyCode k = e.getCode();
            if(e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
            // typing -> scroll to
            if (k.isDigitKey() || k.isLetterKey()){
                String st = e.getText().toLowerCase();
                // update scroll text
                long now = System.currentTimeMillis();
                boolean append = searchTime==-1 || now-searchTime<searchTimeMax.toMillis();
                searchQuery.set(append ? searchQuery.get()+st : st);
                searchTime = now;
                search(searchQuery.get());
            }
        });
    }




    private ListView<T> listView;
    private long searchTime = -1;
    private static Duration searchTimeMax = millis(500);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    public void search(String s) {
        searchQuery.set(s);
        // scroll to match
        if(!getItems().isEmpty()) {
            for(int i=0; i<getItems().size(); i++) {
                T e = getItems().get(i);
                String es = toStringConverter.apply(e);
                if(matches(es,searchQuery.get())) {
                    listView.scrollTo(i);
                    listView.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }

    private static boolean matches(String text, String query) {
        return text.toLowerCase().contains(query.toLowerCase());
    }
}