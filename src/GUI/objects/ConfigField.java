
package GUI.objects;

import Configuration.Action;
import Configuration.Config;
import Configuration.StringEnum;
import GUI.GUI;
import GUI.objects.TextFields.FontTextField;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import main.App;
import org.controlsfx.dialog.Dialogs;
import utilities.Parser.Parser;

/**
 * Its a convenient way to create wide and diverse settings
 * windows, that take type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author uranium
 */
abstract public class ConfigField {
    private final Label label = new Label();
    private final String name;
    
    private ConfigField(String _name) {
        label.setText(_name);
        name = _name;        
    }
    
    /**@return name of the field*/
    public String getName() {
        return name;
    }
    
    /** sets name of the field */
    public void setName(String val) {
        if (!val.isEmpty()) label.setText(val);
    }
    
    /**@return name of the field*/
    abstract public String getValue();
    
    /**@return true if has value*/
    public boolean hasValue() {
        return !getValue().isEmpty();
    }
    
    /**@return label set to name of this field*/
    public Label getLabel() {
        return label;
    }
    
    /**@return setter control for this field*/
    abstract public Control getControl();
    
/******************************************************************************/
    
    /**
     * Creates config field best suited for the specified Field.
     * @param f field for which the GUI will be created
     * @return null if errors out
     */
    public static ConfigField create(Config f) {
        if (!f.visible) return null;
        
        String name = f.name;
        Class<?> type = f.getType();
        Object value = f.value;
        
        ConfigField cf;
        if (name.equals("skin"))
            cf = new SkinField(name, value);
        else
        if (type.equals(boolean.class) || type.equals(Boolean.class))
            cf = new BooleanField(name, value);
        else 
        if (type.isEnum())
            cf = new EnumField(name, value);
        else
        if (type.equals(Action.class))
            cf = new ShortcutField(name, value);
        else
        if (type.equals(Color.class))
            cf = new ColorField(name, (Color)value);
        else
        if (value instanceof StringEnum)
            cf = new EnumListField(name, (StringEnum)value);
        else
        if (type.equals(Font.class))
            cf = new FontField(name, (Font)value);
        else
        if (f.isMinMax())
            cf = new SliderField(name, (double) value, f.min, f.max);
        else 
            cf = new GeneralField(name, value);

        
        cf.setName(f.gui_name);
        cf.getLabel().setTooltip(new Tooltip(f.info));
        cf.getControl().setTooltip(new Tooltip(f.info));
        cf.getControl().setDisable(!f.editable);

        
        return cf;
    }

    /**
     * Creates config field. This method can create any type of field specified
     * by type.
     * @param name
     * @param value
     * @param type
     * @return null if unable to create
     */
    public static ConfigField create(String name, Object value, Class type) {
        ConfigField f;
        
        if (type.equals(boolean.class) || type.equals(Boolean.class))
            f = new BooleanField(name, value);
        else if (type.isEnum())
            f = new EnumField(name, value);
        else
            f = new GeneralField(name, value);

        return f;
    }
    
/******************************************************************************/
    
    private static class GeneralField extends ConfigField {
        TextField control;
        String old;
        final boolean allow_empty; // only for string
        private GeneralField(String name, Object value) {
            super(name);
            old = value.toString();
            allow_empty = value instanceof String;
            control = new TextField();
            control.setPromptText(value.toString());
            control.setOnMouseClicked((MouseEvent t) -> {
                if (control.getText().isEmpty())
                    control.setText(control.getPromptText());
            });
            if (allow_empty)
                control.setOnKeyReleased((KeyEvent e) -> {
                    if (e.getCode().equals(KeyCode.BACK_SPACE) || e.getCode().equals(KeyCode.DELETE)) {
                        if (control.getPromptText().isEmpty())
                            control.setPromptText(old);
                        else
                            control.setPromptText("");
                    }
                });
        }
        @Override public String getValue() {
            return control.getText();
        }
        @Override public Control getControl() {
            return control;
        }     
        @Override public boolean hasValue() {
            if (control.getPromptText().isEmpty())
                return !control.getText().equals(old);
            else
                return !control.getText().isEmpty() && 
                       !control.getText().equals(old);
        }
    }
    
    private static class BooleanField extends ConfigField {
        CheckBox control;
        boolean old;
        private BooleanField(String name, Object value) {
            super(name);
            control = new CheckBox();
            control.setSelected((boolean)value);
            old = (boolean)value;
        }
        @Override public String getValue() {
            return String.valueOf(control.isSelected());
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return old != control.isSelected();
        }
        
    }
    
    private static class EnumField extends ConfigField {
        ChoiceBox<Object> control;
        Object old;
        private EnumField(String name, Object value) {
            super(name);
            control = new ChoiceBox<>();
            ObservableList<Object> items = FXCollections.observableArrayList();
                                   items.setAll(value.getClass().getEnumConstants());
            control.setItems(items);
            control.getSelectionModel().select(value);
            old = value;
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !old.toString().equals(getValue());
        }
    }
    
    private static class EnumListField extends ConfigField {
        ChoiceBox<StringEnum> control;
        StringEnum old;
        private EnumListField(String name, StringEnum value) {
            super(name);
            control = new ChoiceBox<>();
            control.getItems().setAll(value.valuesOrig());
            control.getSelectionModel().select(value);
            old = value;
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !old.toString().equals(getValue());
        }
    }
    
    private static class SliderField extends ConfigField {
        Slider control;
        String val = "";
        private SliderField(String name, double value, double min, double max) {
            super(name);
            control = new Slider(min, max, value);
            control.valueProperty().addListener((Observable o) -> {
                val = String.valueOf(control.getValue());
            });
//            control.setShowTickMarks(true);
//            control.setShowTickLabels(true);
//            control.setMajorTickUnit((max-min)/2);
//            control.setSnapToTicks(true);
        }
        @Override public String getValue() {
            return val;
        }
        @Override public Control getControl() {
            return control;
        }
    }
    
    /** Specifically for listing out available skins. */
    private static class SkinField extends ConfigField {
        ChoiceBox<Object> control;
        Object old;
        private SkinField(String name, Object value) {
            super(name);
            control = new ChoiceBox<>();
            ObservableList<Object> items = FXCollections.observableArrayList();
                                   items.setAll(GUI.getSkins());
            control.setItems(items);
            control.getSelectionModel().select(value);
            old = value;
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !old.toString().equals(getValue());
        }
    }
    
    private static class ShortcutField extends ConfigField {
        TextField control;
        Class<?> type;
        String t="";
        String old;
        private ShortcutField(String name, Object value) {
            super(name);
            old = value.toString();
            control = new TextField();
            control.setPromptText(old);
            control.setOnKeyReleased((KeyEvent e) -> {
                if (e.getCode().equals(KeyCode.BACK_SPACE) || e.getCode().equals(KeyCode.DELETE)) {
                    control.setPromptText("");
                    if (!control.getText().isEmpty()) control.setPromptText(old);
                    
                    if (t.isEmpty()) {
                        return;
                    }
                    if (t.indexOf('+') == -1) t="";
                    else t=t.substring(0,t.lastIndexOf('+'));
                    control.setText(t);
                    return;
                }
                if(t.isEmpty()) t += e.getCode().getName();
                else t += "+"+e.getText();
                control.setText(t);
            });
            control.setEditable(false);
        }
        @Override public String getValue() {
            return t;
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            if (control.getPromptText().isEmpty())
                return !control.getText().equals(old);
            else
                return !control.getText().isEmpty() && 
                       !control.getText().equals(old);
        }
    }
    
    private static class ColorField extends ConfigField {
        ColorPicker picker = new ColorPicker();
        Color old;
        private ColorField(String name, Color value) {
            super(name);
            old = value;
            picker.setValue(value);
        }

        @Override public String getValue() { 
            return picker.getValue().toString();
        }

        @Override public boolean hasValue() {
            return getValue() != null && !picker.getValue().equals(old);
        }

        @Override public Control getControl() {
            return picker;
        }
    }
    
//    private static class FontField extends ConfigField {
//        CustomTextField control = new CustomTextField();
//        Font old;
//        Font curr;
//        private FontField(String name, Font value) {
//            super(name);
//            old = value;
//            curr = value;
//            control.setRight(new DialogButton("..."));
//            control.setPromptText(Parser.toS(value));
//            control.getRight().setOnMouseClicked(e-> {
//                 Font tmp = Dialogs.create().owner(App.getInstance().getScene().getWindow()).showFontSelector(curr);
//                 if (tmp != null) {
//                    curr = tmp;
//                    if(curr != old)
//                       control.setText(Parser.toS(curr));
//                    else
//                       control.setText("");
//                 }
//            });
//        }
//
//        @Override public String getValue() { 
//            return Parser.toS(curr);
//        }
//
//        @Override public boolean hasValue() {
//            return !getValue().isEmpty() && !curr.equals(old);
//        }
//
//        @Override public Control getControl() {
//            return control;
//        }
//        
//    }        
    private static class FontField extends ConfigField {
        FontTextField control = new FontTextField();
        Font old;
        Font curr;
        private FontField(String name, Font value) {
            super(name);
            old = value;
            curr = value;
            control.setPromptFontText(value);
            control.getRight().setOnMouseClicked( e -> {
                 Font tmp = Dialogs.create().owner(App.getInstance().getScene().getWindow()).showFontSelector(curr);
                 if (tmp != null) {
                    curr = tmp;
                    if(curr != old)
                       control.setText(Parser.toS(curr));
                    else
                       control.setText("");
                 }
            });
        }

        @Override public String getValue() { 
            return Parser.toS(curr);
        }

        @Override public boolean hasValue() {
            return !getValue().isEmpty() && !curr.equals(old);
        }

        @Override public Control getControl() {
            return control;
        }
        
    }
}
