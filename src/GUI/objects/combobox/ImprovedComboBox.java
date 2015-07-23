/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.combobox;

import static java.util.Objects.requireNonNull;
import java.util.function.Function;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * ComboBox with to string converter and default text for empty value.
 * 
 * @author Plutonium_
 */
public class ImprovedComboBox<T> extends ComboBox<T> {

    /** String converter for cell value factory. Default is Object::toString */
    public final Function<T,String> toStringConverter;
    /** Text for when no value is selected. Default {@code "<none>"} */
    public final String emptyText;
    
    public ImprovedComboBox() {
        this(Object::toString, "<none>");
    }

    public ImprovedComboBox(Function<T, String> toS) {
        this(toS, "<none>");
    }
    public ImprovedComboBox(Function<T, String> toS, String empty_text) {
        requireNonNull(toS);
        toStringConverter = toS;
        setCellFactory(view -> new ListCell<T>(){ // do not use ComboBoxListCell! causes problems!
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "<none>" : toStringConverter.apply(item));
            }
        });
        setButtonCell(getCellFactory().call(null));        
        emptyText = empty_text;
    }
}
