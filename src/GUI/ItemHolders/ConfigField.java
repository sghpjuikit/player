
package GUI.ItemHolders;

import Action.Action;
import Configuration.Config;
import GUI.GUI;
import GUI.ItemHolders.ItemTextFields.FileTextField;
import GUI.ItemHolders.ItemTextFields.FontTextField;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.REPEAT;
import java.io.File;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.controlsfx.control.textfield.CustomTextField;
import utilities.FxTimer;
import utilities.Parser.ParserImpl.FileParser;
import utilities.Parser.ParserImpl.FontParser;
import utilities.Password;
import utilities.Util;

/**
 * Editable and setable graphic control for configuring {@Config}.
 * <p>
 * Convenient way to create wide and diverse property sheets, that take 
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author uranium
 */
abstract public class ConfigField<T> {
    private final Label label = new Label();
    private final HBox box = new HBox();
    final Config<T> config;
    private boolean applyOnChange = true;
    Button defB;
    
    private ConfigField(Config<T> c) {
        config = c;
        label.setText(c.getGuiName());
        
        box.setMinSize(0,0);
        box.setPrefSize(HBox.USE_COMPUTED_SIZE,20); // not sure why this needs manual resizing
        box.setSpacing(5);
        box.setAlignment(CENTER_LEFT);
        box.setPadding(new Insets(0, 15, 0, 0)); // space for defB (11+5)(defB.width+box.spacing)
        
        // display default button when hovered for certain time
        box.addEventFilter(MOUSE_ENTERED, e -> {
            // wait delay
            FxTimer.run(270, () -> {
                // no need to do anything if hover ended
                if(box.isHover()) {
                    // lazily build the button when requested
                    // we dont want hundreds of buttons we will never use anyway
                    if(defB==null) {
                        defB = AwesomeDude.createIconButton(REPEAT, "", "11","10",GRAPHIC_ONLY);
                        defB.setOpacity(0);
                        defB.setOnMouseClicked( ee -> setNapplyDefault());
                        defB.getStyleClass().setAll("congfig-field-default-button");
                        Tooltip.install(defB, new Tooltip("Set to default value."));
                        box.getChildren().add(defB);
                        box.setPadding(Insets.EMPTY);
                    }
                    // show it
                    FadeTransition fa = new FadeTransition(Duration.millis(450), defB);
                    fa.stop();
                    fa.setToValue(1);
                    fa.play();
                }
            });
        });
        // hide default button
        box.addEventFilter(MOUSE_EXITED, e-> {
            // return if nothing to hide
            if(defB == null) return;
            // hide it
            FadeTransition fa = new FadeTransition(Duration.millis(450), defB);
            fa.stop();
            fa.setDelay(Duration.ZERO);
            fa.setToValue(0);
            fa.play();
        });
    }
    
    /**
     * Simply compares the current value with the one obtained from Config.
     * Equivalent to: !config.getValue().equals(getItem());
     * @return true if has value that has not been applied
     */
    public boolean hasUnappliedValue() {
        return !config.getValue().equals(getItem());
    }
    
    /**
     * Sets editability by disabling the Nodes responsible for value change
     * @param val 
     */
    public void setEditable(boolean val) {
        getNode().setDisable(val);
    }
    
    /**
     * Use to get the label to attach it to a scene graph.
     * @return label describing this field
     */
    public Label getLabel() {
        return label;
    }
    
    /**
     * Use to get the control node for setting and displaying the value to 
     * attach it to a scene graph.
     * @return setter control for this field
     */
    public Node getControl() {
        if(!box.getChildren().contains(getNode()))
            box.getChildren().add(0, getNode());
        box.setHgrow(getNode(), Priority.ALWAYS);
        return box;
    }
    
    /**
     * Use to get the control node for setting and displaying the value to 
     * attach it to a scene graph.
     * @return setter control for this field
     */
    abstract Node getNode();

    /**
     * {@inheritDoc}
     * Returns the currently displayed value. Use to get for custom implementations
     * of setting and applying the value. Usually it is compared to the value obtain
     * from the Config from the {@link #getConfig()} method and then decided whether
     * it should be set or applied or ignored.
     * <p>
     * Current value is value displayed. Because it can be edited in real time
     * by the user and it can be represented visually by a String or differently
     * it doesnt have to be valid at all times - therefore, if the value is not
     * valid (can not be obtained) the method returns currently set value.
     * 
     * @return 
     */
    public abstract T getItem();
    
    /**
     * Refreshes the content of this config field. The content is read from the
     * Config and as such reflects the real value. Using this method after the
     * applying the new value will confirm the success visually to the user.
     */
    public abstract void refreshItem();
    
    /**
     * Returns the {@link Config}. Use for custom implementations of setting and
     * applying new values.
     * @return name of the field
     */
    public Config getConfig() {
        return config;
    }
    
    public boolean isApplyOnChange() {
        return applyOnChange;
    }
    
    public void setApplyOnChange(boolean val) {
        applyOnChange = val;
    }
    
    /**
     * Convenience method and default implementation of set and apply mechanism.
     * Also calls the {@link #refreshItem()} when needed.
     * <p>
     * Checks te current value and compares it with the value obtainable from
     * the config (representing the currently set value) and sets and applies
     * the current value of the values differ.
     * <p>
     * To understand the difference
     * between changing and applying refer to {@link Config}.
     * 
     * @return whether any change occured. Occurs when change needs to be applied.
     * Equivalent to calling {@link #hasUnappliedValue()} method.
     */
    public boolean applyNsetIfAvailable() {
        if(hasUnappliedValue()) {
            config.setNapplyValue(getItem());
            refreshItem();
            return true;
        } else 
            refreshItem();{
            return false;
        }
    }
    
    /**
     * Sets and applies default value of the config if it has different value
     * set.
     */
    public final void setNapplyDefault() {
        T defVal = config.getDefaultValue();
        if(!config.getValue().equals(defVal)) {
            config.setNapplyValue(defVal);
            refreshItem();
        }
    }
    
/******************************************************************************/
    
    /**
     * Creates ConfigFfield best suited for the specified Field.
     * @param f field for which the GUI will be created
     */
    public static ConfigField create(Config f) {        
        String name  = f.getName();
        Class type = f.getType();
        
        ConfigField cf;
        if (name.equals("skin"))
            cf = new SkinField(f);
        else
        if (Boolean.class.equals(Util.unPrimitivize(type)))
            cf = new BooleanField(f);
        else 
        if (String.class.equals(Util.unPrimitivize(type)))
            cf = new GeneralField(f);
        else 
        if (type.isEnum())
            cf = new EnumField(f);
        else
        if (Action.class.equals(Util.unPrimitivize(type)))
            cf = new ShortcutField(f);
        else
        if (f.isMinMax())
            cf = new SliderField(f);
        else
        if (Color.class.equals(Util.unPrimitivize(type)))
            cf = new ColorField(f);
        else
        if (File.class.equals(Util.unPrimitivize(type)))
            cf = new FileField(f);
        else
        if (Font.class.equals(Util.unPrimitivize(type)))
            cf = new FontField(f);
        else
        if (Password.class.equals(Util.unPrimitivize(type)))
            cf = new PasswordField(f);
        else
            cf = new GeneralField(f);
        
        cf.setEditable(!f.isEditable());
        if(!f.getInfo().isEmpty()) {
            Tooltip tooltip = new Tooltip(f.getInfo());
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
            cf.getLabel().setTooltip(tooltip);
            Tooltip.install(cf.getNode(),tooltip);
        }
        
        return cf;
    }
    
/***************************** IMPLEMENTATIONS ********************************/
    
    private static final class PasswordField extends ConfigField<Password>{
        
        javafx.scene.control.PasswordField passF = new javafx.scene.control.PasswordField();
        
        public PasswordField(Config<Password> c) {
            super(c);
            refreshItem();
        }

        @Override
        Node getNode() {
            return passF;
        }

        @Override
        public Password getItem() {
            return new Password(passF.getText());
        }

        @Override
        public void refreshItem() {
            passF.setText(config.getValue().get());
        }
        
    }    
    
    private static final class GeneralField extends ConfigField<Object> {
        CustomTextField txtF = new CustomTextField();
        final boolean allow_empty; // only for string
        Button okBL= AwesomeDude.createIconButton(AwesomeIcon.CHECK, "", "15","15",ContentDisplay.GRAPHIC_ONLY);
        AnchorPane okB = new AnchorPane(okBL);
   
        
        private GeneralField(Config c) {
            super(c);
            allow_empty = c.getType().equals(String.class);
            
            // doesnt work because of CustomTextField instead f TextField
            // restrict input
//            if(c.isTypeNumber())
//                InputConstraints.numbersOnly(txtF, !c.isTypeNumberNonegative(), c.isTypeFloatingNumber());
            
            okBL.getStyleClass().setAll("congfig-field-ok-button");
            Tooltip.install(okB, new Tooltip("Apply value."));
            // unfortunately the icon buttonis not aligned well, need to fix that
            AnchorPane.setBottomAnchor(okBL, 3d);
            AnchorPane.setLeftAnchor(okBL, 8d);
            
            txtF.setContextMenu(null);
            txtF.getStyleClass().setAll("text-field","text-input");
            txtF.setPromptText(c.getValueS());
            // start edit
            txtF.setOnMouseClicked( e -> {
                if (txtF.getText().isEmpty())
                    txtF.setText(txtF.getPromptText());
                e.consume();
            });
            
            txtF.focusedProperty().addListener((o,ov,nv) -> {
                if(nv) {
                    if (txtF.getText().isEmpty()) 
                        txtF.setText(txtF.getPromptText());
                } else {
                    // the timer solves a little bug where the focus shift from
                    // txtF to okB has a delay which we need to jump over
                    FxTimer.run(Duration.millis(80), () -> {
                        if(!okBL.isFocused() && !okB.isFocused()) {
                            txtF.setText("");
                            showOkButton(false);
                        }
                    });
                }
            });
            
            if (allow_empty)
                txtF.addEventHandler(KEY_RELEASED, e -> {
                    if (e.getCode()==BACK_SPACE || e.getCode()==DELETE) {
                        if (txtF.getPromptText().isEmpty())
                            txtF.setPromptText(config.getValueS());
                        else
                            txtF.setPromptText("");
                    }
                });
            // applying value
            txtF.textProperty().addListener((o,ov,nv)-> {
                boolean applicable = !nv.isEmpty() && !nv.equals(txtF.getPromptText());
                showOkButton(applicable);
            });
            okBL.setOnMouseClicked( e -> apply());
            txtF.setOnKeyPressed( e -> { if(e.getCode()==ENTER) apply(); });
        }
        
        @Override public Control getNode() {
            return txtF;
        }
        @Override public Object getItem() {
            String text = txtF.getText();
            if(allow_empty) {
                return text.isEmpty() ? txtF.getPromptText() : text;
            } else {
                return text.isEmpty() ? config.getValue() : config.fromS(text);
            }
        }
        @Override public void refreshItem() {
            txtF.setPromptText(config.getValueS());
            txtF.setText("");
            showOkButton(false);
        }
        private void apply() {
            if(isApplyOnChange()) applyNsetIfAvailable();
        }
        private void showOkButton(boolean val) {
            if (val) txtF.setLeft(okB);
            else txtF.setLeft(new Region());
            okB.setVisible(val);
        }
        
    }
    
    private static final class BooleanField extends ConfigField<Boolean> {
        CheckBox cBox;
        
        private BooleanField(Config<Boolean> c) {
            super(c);
            cBox = new CheckBox();
            refreshItem();
            cBox.selectedProperty().addListener((o,ov,nv)->{
                if(isApplyOnChange()) applyNsetIfAvailable();
            });
        }
        
        @Override public Control getNode() {
            return cBox;
        }
        @Override public Boolean getItem() {
            return cBox.isSelected();
        }
        @Override public void refreshItem() {
            cBox.setSelected(config.getValue());
        }
    }
    
    private static final class EnumField extends ConfigField<Object> {
        ChoiceBox<Object> cBox;
        
        private EnumField(Config c) {
            super(c);
            cBox = new ChoiceBox();
            ObservableList<Object> constants = FXCollections.observableArrayList();
            // get enum constants
            //      of enums with class method bodies
            Class clazz = c.getType();
            if(clazz.getEnclosingClass()!=null && clazz.getEnclosingClass().isEnum())
                constants.setAll(clazz.getEnclosingClass().getEnumConstants());
            //      of normal enums
            else constants.setAll(clazz.getEnumConstants());
            
            cBox.setItems(constants);
            cBox.setValue(config.getValue());
            cBox.getSelectionModel().selectedItemProperty().addListener((o,ov,nv)->{
                if(isApplyOnChange()) applyNsetIfAvailable();
            });
        }
        
        @Override public Control getNode() {
            return cBox;
        }
        @Override public Object getItem() {
            return cBox.getValue();
        }
        @Override public void refreshItem() {
            cBox.setValue(config.getValue());
        }
    }
    
    private static final class SliderField extends ConfigField<Number> {
        Slider slider;
        private SliderField(Config<Number> c) {
            super(c);
            slider = new Slider(c.getMin(),c.getMax(),c.getValue().doubleValue());
            // there is a slight bug where isValueChanging is false even it if 
            // shouldnt. It appears when mouse clicks NOT on the thumb but on
            // the slider track instead and keeps dragging. valueChanging doesn
            // activate - fill out JIRA bug?
            slider.valueProperty().addListener( (o,ov,nv) -> {
                if(isApplyOnChange() && !slider.isValueChanging())
                    applyNsetIfAvailable();
            });
            slider.setOnMouseReleased( e -> {
                if(isApplyOnChange()) applyNsetIfAvailable();
            });
            
            // add scrolling support
            slider.setBlockIncrement((c.getMax()-c.getMin())/20);
            slider.setOnScroll( e -> {
                if (e.getDeltaY()>0) slider.increment();
                else slider.decrement();
                e.consume();
            });

//            control.setShowTickMarks(true);
//            control.setShowTickLabels(true);
//            control.setMajorTickUnit((max-min)/2);
//            control.setSnapToTicks(true);
        }
        
        @Override public Control getNode() {
            return slider;
        }
        @Override public Number getItem() {
            return slider.getValue();
        }
        @Override public void refreshItem() {
            slider.setValue(config.getValue().doubleValue());
        }
    }
    
    
    /** Specifically for listing out available skins. */
    private static final class SkinField extends ConfigField<String> {
        ChoiceBox<String> cBox;
        
        private SkinField(Config c) {
            super(c);
            cBox = new ChoiceBox<>();
            ObservableList<String> items = FXCollections.observableArrayList();
                                   items.setAll(GUI.getSkins());
            cBox.setItems(items);
            cBox.getSelectionModel().select(GUI.getSkins().indexOf(GUI.skin));
            cBox.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
                if(isApplyOnChange()) {
                    GUI.skin.setNapplyValue(getItem());
//                    applyNsetIfAvailable(); // causes StackOverflow sometimes !
                }
            });
        }
        @Override public final String getItem() {
            return cBox.getValue();
        }

        @Override
        public void refreshItem() {
//            ObservableList<String> items = FXCollections.observableArrayList();
//                                   items.setAll(GUI.getSkins());
//            cBox.setItems(items);
//            cBox.getSelectionModel().select(GUI.getSkins().indexOf(GUI.skin));
        }

        @Override
        Node getNode() {
            return cBox;
        }
    }
    
    private static final class ShortcutField extends ConfigField<Action> {
        TextField txtF;
        CheckBox glob;
        HBox group;
        String t="";
        Action a;
        
        private ShortcutField(Config<Action> con) {
            super(con);
            a = con.getValue();
            txtF = new TextField();
            txtF.setPromptText(a.getKeys());
            txtF.setOnKeyReleased( e -> {
                KeyCode c = e.getCode();
                // handle substraction
                if (c==BACK_SPACE || c==DELETE) {
                    txtF.setPromptText("");
                    if (!txtF.getText().isEmpty()) txtF.setPromptText(a.getKeys());
                    
                    
                    if (t.isEmpty()) {  // set back to empty
                        txtF.setPromptText(a.getKeys());
                    } else {            // substract one key
                        if (t.indexOf('+') == -1) t="";
                        else t=t.substring(0,t.lastIndexOf('+'));
                        txtF.setText(t);
                    }
                } else if(c==ENTER) {
                    if (isApplyOnChange()) applyNsetIfAvailable();
                } else if(c==ESCAPE) {
                    refreshItem();
                // handle addition
                } else {
                    t += t.isEmpty() ? c.getName() : "+" + c.getName();
                    txtF.setText(t);
                }
            });
            txtF.setEditable(false);
            txtF.setTooltip(new Tooltip(a.getInfo()));
            txtF.focusedProperty().addListener( (o,ov,nv) -> {
                if(nv) {
                    txtF.setText(txtF.getPromptText());
                } else {
                    // prevent 'deselection' if we txtF lost focus because glob
                    // received click
                    if(!glob.isFocused())
                        txtF.setText("");
                }
            });
            
            glob = new CheckBox();
            glob.setSelected(a.isGlobal());
            glob.setTooltip(new Tooltip("Whether shortcut is global (true) or local."));
            glob.selectedProperty().addListener((o,ov,nv) -> {
                if (isApplyOnChange()) applyNsetIfAvailable();
            });
            group = new HBox(glob,txtF);
            group.setAlignment(CENTER_LEFT);
            group.setPadding(Insets.EMPTY);
        }
        
        @Override public Node getNode() {
            return group;
        }
        @Override public boolean hasUnappliedValue() {
            Action a = config.getValue();
            boolean sameglobal = glob.isSelected()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }
        @Override public final boolean applyNsetIfAvailable() {
            // its pointless to make new Action just for this
            // config.setNapplyValue(getItem()); 
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = glob.isSelected()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            
            if(!sameglobal && !sameKeys)
                a.set(glob.isSelected(), txtF.getText());
            else if (!sameKeys)
                a.setKeys(txtF.getText());
            else if (!sameglobal)
                a.setGlobal(glob.isSelected());
            else {
                refreshItem();
                return false;
            }

            refreshItem();
            return true;
        }
        @Override public Action getItem() {
            return a;
        }
        @Override public void refreshItem() {
            Action a = config.getValue();
            txtF.setPromptText(a.getKeys());
            txtF.setText("");
            glob.setSelected(a.isGlobal());
        }
    }
    
    private static final class ColorField extends ConfigField<Color> {
        ColorPicker picker = new ColorPicker();
        
        private ColorField(Config<Color> c) {
            super(c);
            refreshItem();
            picker.valueProperty().addListener( (o,ov,nv) -> {
                if(isApplyOnChange()) applyNsetIfAvailable();
            });
        }
        
        @Override public Control getNode() {
            return picker;
        }
        @Override public Color getItem() {
            return picker.getValue();
        }
        @Override public void refreshItem() {
            picker.setValue(config.getValue());
        }
    }
      
    private static final class FontField extends ConfigField<Font> {
        FontTextField txtF = new FontTextField();
        
        private FontField(Config<Font> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((oldFont,newFont) -> {
                if(!newFont.equals(oldFont)) {  // we shouldnt rely on Font.equals here
                    applyNsetIfAvailable();
                    txtF.setPromptText(new FontParser().toS(newFont));
                }
                txtF.setText(""); // always stay in prompt text more
            });
        }
        
        @Override public Control getNode() {
            return txtF;
        }
        @Override public Font getItem() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
    
    private static final class FileField extends ConfigField<File> {
        FileTextField txtF = new FileTextField();
        
        public FileField(Config<File> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((oldFile,newFile) -> {
                if(!newFile.equals(oldFile)) {
                    applyNsetIfAvailable();
                    txtF.setPromptText(new FileParser().toS(newFile));
                }
                txtF.setText(""); // always stay in prompt text more
            });
        }
        
        @Override public Control getNode() {
            return txtF;
        }
        @Override public File getItem() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
}  
