
package GUI.ItemHolders;

import Configuration.Action;
import Configuration.Config;
import Configuration.StringEnum;
import GUI.GUI;
import GUI.objects.ItemTextFields.DirTextField;
import GUI.objects.ItemTextFields.FontTextField;
import java.io.File;
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

/**
 * Its a convenient way to create wide and diverse property sheets, that take 
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author uranium
 */
abstract public class ConfigField<T extends Object> {
    private final Label label = new Label();
    private final String field_name;
    private final T old;

    private ConfigField(String name, T item) {
        field_name = name; 
        old = item;
    }
    
    /**@return name of the field*/
    public String getName() {
        return field_name;
    }
    
    /** sets name of the field displayed in gui*/
    public void setName(String val) {
        if (!val.isEmpty()) label.setText(val);
    }
    
    /**@return name of the field*/
    abstract public String getValue();
    
    /**@return true if has value*/
    public boolean hasValue() {
        return !getValue().isEmpty();
    }
    
    /**@return label describing this field*/
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
            cf = new BooleanField(name, (boolean)value);
        else 
        if (value instanceof Enum) {
            cf = new EnumField(name, value);}
        else
        if (type.equals(Action.class))
            cf = new ShortcutField(name, (String)value);
        else
        if (type.equals(Color.class))
            cf = new ColorField(name, (Color)value);
        else
        if (type.equals(File.class))
            cf = new DirectoryField(name, (File)value);
        else
        if (value instanceof StringEnum)
            cf = new EnumListField(name, (StringEnum)value);
        else
        if (type.equals(Font.class))
            cf = new FontField(name, (Font)value);
        else
        if (f.isMinMax())
            cf = new SliderField(name, (Number) value, f.min, f.max);
        else 
            cf = new GeneralField(name, value);

        
        cf.setName(f.gui_name);
        cf.getLabel().setTooltip(new Tooltip(f.info));
        cf.getControl().setTooltip(new Tooltip(f.info));
        cf.getControl().setDisable(!f.editable);

        
        return cf;
    }
    
/******************************************************************************/
    
    private static class GeneralField extends ConfigField<Object> {
        TextField control;
        final boolean allow_empty; // only for string
        
        private GeneralField(String name, Object value) {
            super(name,value);
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
                            control.setPromptText(super.old.toString());
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
                return !control.getText().equals(super.old);
            else
                return !control.getText().isEmpty() && 
                       !control.getText().equals(super.old);
        }
    }
    
    private static class BooleanField extends ConfigField<Boolean> {
        CheckBox control;
        
        private BooleanField(String name, Boolean value) {
            super(name,value);
            control = new CheckBox();
            control.setSelected(value);
        }
        @Override public String getValue() {
            return String.valueOf(control.isSelected());
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return super.old != control.isSelected();
        }
        
    }
    
    private static class EnumField extends ConfigField<Object> {
        ChoiceBox<Object> control;
        
        private EnumField(String name, Object value) {
            super(name,value);
            control = new ChoiceBox<>();
            ObservableList<Object> constants = FXCollections.observableArrayList();
            
            // get enum constants
            if(value.getClass().getEnclosingClass()!=null && value.getClass().getEnclosingClass().isEnum())
                // handle enums with class method bodies
                constants.setAll(value.getClass().getEnclosingClass().getEnumConstants());
            else
                // handle normal enums
                constants.setAll(value.getClass().getEnumConstants());
            
            control.setItems(constants);
            control.getSelectionModel().select(value);
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !super.old.toString().equals(getValue());
        }
    }
    
    private static class EnumListField extends ConfigField {
        ChoiceBox<StringEnum> control;
        
        private EnumListField(String name, StringEnum value) {
            super(name,value);
            control = new ChoiceBox<>();
            control.getItems().setAll(value.valuesOrig());
            control.getSelectionModel().select(value);
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !super.old.toString().equals(getValue());
        }
    }
    
    private static class SliderField extends ConfigField<Number> {
        Slider control;
        String curr = "";
        private SliderField(String name, Number value, double min, double max) {
            super(name,value);
            control = new Slider(min, max, value.doubleValue());
            control.valueProperty().addListener(o -> {
                curr = String.valueOf(control.getValue());
            });
//            control.setShowTickMarks(true);
//            control.setShowTickLabels(true);
//            control.setMajorTickUnit((max-min)/2);
//            control.setSnapToTicks(true);
        }
        @Override public String getValue() {
            return curr;
        }
        @Override public Control getControl() {
            return control;
        }
    }
    
    /** Specifically for listing out available skins. */
    private static class SkinField extends ConfigField<Object> {
        ChoiceBox<Object> control;
        
        private SkinField(String name, Object value) {
            super(name,value);
            control = new ChoiceBox<>();
            ObservableList<Object> items = FXCollections.observableArrayList();
                                   items.setAll(GUI.getSkins());
            control.setItems(items);
            control.getSelectionModel().select(value);
        }
        @Override public String getValue() {
            return control.getSelectionModel().getSelectedItem().toString();
        }
        @Override public Control getControl() {
            return control;
        }
        @Override public boolean hasValue() {
            return !super.old.toString().equals(getValue());
        }
    }
    
    private static class ShortcutField extends ConfigField<String> {
        TextField control;
        Class<?> type;
        String t="";
        
        private ShortcutField(String name, String value) {
            super(name,value);
            
            control = new TextField();
            control.setPromptText(super.old);
            control.setOnKeyReleased((KeyEvent e) -> {
                if (e.getCode().equals(KeyCode.BACK_SPACE) || e.getCode().equals(KeyCode.DELETE)) {
                    control.setPromptText("");
                    if (!control.getText().isEmpty()) control.setPromptText(super.old);
                    
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
                return !control.getText().equals(super.old);
            else
                return !control.getText().isEmpty() && 
                       !control.getText().equals(super.old);
        }
    }
    
    private static final class ColorField extends ConfigField<Color> {
        ColorPicker picker = new ColorPicker();
        
        private ColorField(String name, Color value) {
            super(name,value);
            picker.setValue(value);
        }

        @Override public String getValue() { 
            return picker.getValue().toString();
        }
        @Override public boolean hasValue() {
            return getValue() != null && !picker.getValue().equals(super.old);
        }
        @Override public Control getControl() {
            return picker;
        }
    }
      
    private static final class FontField extends ConfigField<Font> {
        FontTextField control = new FontTextField();
        
        private FontField(String name, Font value) {
            super(name,value);
            
            control.setOnItemChange(newFont -> {
                if(newFont.equals(super.old)) {
                    control.setText("");
                }
            });
            control.setItem(value);
        }

        @Override public String getValue() { 
            return control.getText();
        }
        @Override public boolean hasValue() {
            return !getValue().isEmpty() && !control.getItem().equals(super.old);
        }
        @Override public Control getControl() {
            return control;
        }
    }
    
    private static final class DirectoryField extends ConfigField<File> {
        DirTextField control = new DirTextField();
        
        public DirectoryField(String name, File value) {
            super(name,value);
            
            control.setOnItemChange(newFont -> {
                if(newFont.equals(super.old)) {
                    control.setText("");
                }
            });
            control.setItem(value);
        }
        
        @Override public String getValue() {
            return control.getText();
        }
        @Override public boolean hasValue() {
            return !getValue().isEmpty() && !control.getItem().equals(super.old);
        }
        @Override public Control getControl() {
            return control;
        }
        
    }
}
