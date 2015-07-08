
package gui.itemnode;

import action.Action;
import Configuration.Config;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.itemnode.ItemNode.ConfigNode;
import gui.itemnode.TextFieldItemNode.FileItemNode;
import gui.itemnode.TextFieldItemNode.FontItemNode;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.combobox.ImprovedComboBox;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import static javafx.css.PseudoClass.getPseudoClass;
import javafx.geometry.Insets;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.textfield.CustomTextField;
import util.Password;
import static util.Util.*;
import static util.async.Async.run;
import static util.functional.Util.*;

/**
 * Editable and setable graphic control for configuring {@Config}.
 * <p>
 * Convenient way to create wide and diverse property sheets, that take 
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author uranium
 */
abstract public class ConfigField<T> extends ConfigNode<T> {
    
    private static final PseudoClass editedPC = getPseudoClass("edited");
    private static final Tooltip okTooltip = new Tooltip("Apply value");
    private static final Tooltip warnTooltip = new Tooltip("Erroneous value");
    private static final Tooltip defTooltip = new Tooltip("Default value");
    private static final Tooltip globTooltip = new Tooltip("Whether shortcut is global (true) or local.");
    
    private final Label label = new Label();
    protected final HBox root = new HBox();
    public boolean applyOnChange = true;
    protected boolean insonsistent_state = false;
    private Icon defB;
    
    private ConfigField(Config<T> c) {
        super(c);
        label.setText(config.getGuiName());
        
        root.setMinSize(0,0);
        root.setPrefSize(HBox.USE_COMPUTED_SIZE,20); // not sure why this needs manual resizing
        root.setSpacing(5);
        root.setAlignment(CENTER_LEFT);
        root.setPadding(new Insets(0, 15, 0, 0)); // space for defB (11+5)(defB.width+box.spacing)
        
        // display default button when hovered for certain time
        root.addEventFilter(MOUSE_ENTERED, e -> {
            if(!config.isEditable()) return;
            // wait delay
            run(270, () -> {
                // no need to do anything if hover ended
                if(root.isHover()) {
                    // lazily build the button when requested
                    // we dont want hundreds of buttons we will never use anyway
                    if(defB==null) {
                        defB = new Icon(RECYCLE, 11, null, this::setNapplyDefault);
                        defB.setTooltip(defTooltip);
                        defB.setOpacity(0);
                        defB.getStyleClass().setAll("congfig-field-default-button");
                        root.getChildren().add(defB);
                        root.setPadding(Insets.EMPTY);
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
        root.addEventFilter(MOUSE_EXITED, e-> {
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
     * Equivalent to: !config.getValue().equals(get());
     * @return true if has value that has not been applied
     */
    public boolean hasUnappliedValue() {
        return !config.getValue().equals(get());
    }
    
    /**
     * Sets editability by disabling the Nodes responsible for value change
     * @param val 
     */
    public void setEditable(boolean val) {
        getControl().setDisable(!val);
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
    @Override
    public Node getNode() {
        if(!root.getChildren().contains(getControl()))
            root.getChildren().add(0, getControl());
        HBox.setHgrow(getControl(), ALWAYS);
        return root;
    }
    
    /**
     * Use to get the control node for setting and displaying the value to 
     * attach it to a scene graph.
     * @return setter control for this field
     */
    abstract Node getControl();
    
    @Override
    public void focus() {
        getControl().requestFocus();
    }

    protected abstract T get();
    
    /**
     * Refreshes the content of this config field. The content is read from the
     * Config and as such reflects the real value. Using this method after the
     * applying the new value will confirm the success visually to the user.
     */
    public abstract void refreshItem();
    
    /**
     * Sets and applies default value of the config if it has different value
     * set.
     */
    public final void setNapplyDefault() {
        T t = config.getDefaultValue();
        if(!config.getValue().equals(t)) {
            config.setNapplyValue(t);
            refreshItem();
        }
    }
    public void apply() {
        apply(true);
    }
    protected void apply(boolean user) {
        if(insonsistent_state) return;
        T t = get();
        
        boolean erroneous = t==null;
        if(erroneous) return;
        boolean applicable = !config.getValue().equals(t);
        if(!applicable) return;
        
        insonsistent_state = true;
        if(applyOnChange || user) config.setNapplyValue(t);
        else config.setValue(t);
        refreshItem();
        insonsistent_state = false;
    }
    
/******************************************************************************/
    
    private static Map<Class,Callback<Config,ConfigField>> m = new HashMap();
    
    static {
        m.put(boolean.class, f -> new BooleanField(f));
        m.put(Boolean.class, f -> new BooleanField(f));
        m.put(String.class, f -> new GeneralField(f));
        m.put(Action.class, f -> new ShortcutField(f));
        m.put(Color.class, f -> new ColorField(f));
        m.put(File.class, f -> new FileField(f));
        m.put(Font.class, f -> new FontField(f));
        m.put(Password.class, f -> new PasswordField(f));
        m.put(String.class, f -> new StringField(f));
    }
    
    /**
     * Creates ConfigFfield best suited for the specified Field.
     * @param f field for which the GUI will be created
     */
    public static ConfigField create(Config f) {
        
        ConfigField cf = null;
        if (f.isTypeEnumerable()) cf = new EnumerableField(f);
        else if(f.isMinMax()) cf = new SliderField(f);
        else cf = m.getOrDefault(f.getType(), GeneralField::new).call(f);
        
        cf.setEditable(f.isEditable());
        
        if(!f.getInfo().isEmpty()) {
            Tooltip t = new Tooltip(f.getInfo());
                    t.setWrapText(true);
                    t.setMaxWidth(300);
            cf.getLabel().setTooltip(t);
            if(!cf.getClass().isInstance(ShortcutField.class))
                Tooltip.install(cf.getControl(),t);
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
        Node getControl() {
            return passF;
        }

        @Override
        public Password get() {
            return new Password(passF.getText());
        }

        @Override
        public void refreshItem() {
            passF.setText(config.getValue().get());
        }
        
    }    
    private static final class StringField extends ConfigField<String> {
        private CustomTextField n = new CustomTextField();
        
        private StringField(Config c) {
            super(c);
            
            n.getStyleClass().setAll("text-field","text-input");
            n.getStyleClass().add("text-field-config");
            n.setPromptText(c.getName());
            n.setText(c.getValueS());

            n.focusedProperty().addListener((o,ov,nv) -> {
                if(nv) {
                    n.pseudoClassStateChanged(editedPC, true);
                } else {
                    n.pseudoClassStateChanged(editedPC, false);
                    refreshItem();
                }
            });
            n.addEventHandler(KEY_RELEASED, e -> {
                if (isInR(e.getCode(), BACK_SPACE,DELETE)) {
                    boolean firsttime = !n.getPromptText().isEmpty();
                    n.setPromptText(firsttime ? "" : config.getName());
                }
            });
            n.addEventHandler(KEY_RELEASED, e -> {
                if (e.getCode()==ESCAPE)
                    root.requestFocus();
            });
            n.textProperty().addListener((o,ov,nv)-> apply(false));
        }
        
        @Override public Control getControl() {
            return n;
        }

        @Override public void focus() {
            n.requestFocus();
            n.selectAll();
        }
        
        @Override public String get() {
            return n.getText();
        }
        
        @Override public void refreshItem() {
            if(insonsistent_state) return;
            n.setText(config.getValueS());
        }
        
    }
    private static final class GeneralField extends ConfigField<Object> {
        CustomTextField n = new CustomTextField();
        Icon okI= new Icon();
        Icon warnI = new Icon();
        AnchorPane okB = new AnchorPane(okI);
        AnchorPane warnB = new AnchorPane(warnI);
        
        private GeneralField(Config c) {
            super(c);
            
            // doesnt work because of CustomTextField instead f TextField
            // restrict input
//            if(c.isTypeNumber())
//                InputConstraints.numbersOnly(txtF, !c.isTypeNumberNonegative(), c.isTypeFloatingNumber());
            
            okB.setPrefSize(11, 11);
            okB.setMinSize(11, 11);
            okB.setMaxSize(11, 11);
            warnB.setPrefSize(11, 11);
            warnB.setMinSize(11, 11);
            warnB.setMaxSize(11, 11);
            okI.getStyleClass().setAll("congfig-field-ok-button");
            okI.icon_size.set(11);
            okI.setTooltip(okTooltip);
            setAnchors(okI,0,0,0,8);       // fix alignment
            warnI.icon_size.set(11);
            warnI.getStyleClass().setAll("congfig-field-warn-button");
            warnI.setTooltip(warnTooltip);
            
            n.getStyleClass().setAll("text-field","text-input");
            n.getStyleClass().add("text-field-config");
            n.setPromptText(c.getName());
            n.setText(c.getValueS());
            
            n.focusedProperty().addListener((o,ov,nv) -> {
                if(nv) {
                    n.pseudoClassStateChanged(editedPC, true);
                } else {
                    n.pseudoClassStateChanged(editedPC, false);
//                    // the timer solves a little bug where the focus shift from
//                    // txtF to okB has a delay which we need to jump over
//                    run(80, () -> {
//                        if(!okBL.isFocused() && !okB.isFocused()) {
//                            txtF.setText("");
//                            showOkButton(false);
//                        }
//                    });
                    refreshItem();
                }
            });
            
            n.addEventHandler(KEY_RELEASED, e -> {
                if (e.getCode()==ESCAPE)
                    root.requestFocus();
            });
            // applying value
            n.textProperty().addListener((o,ov,nv)-> {
                Object i = get();
                boolean erroneous = i==null;
                boolean applicable = !config.getValue().equals(i);
                showOkButton(!applyOnChange && applicable && !erroneous);
                showWarnButton(erroneous);
                if(nv.isEmpty()) return;
                if(applyOnChange) apply(false);
            });
            okI.setOnMouseClicked( e -> apply(true));
            n.setOnKeyPressed( e -> { if(e.getCode()==ENTER) apply(true); });
        }
        
        @Override public Control getControl() {
            return n;
        }

        @Override
        public void focus() {
            n.requestFocus();
            n.selectAll();
        }
        
        @Override public Object get() {
            return config.fromS(n.getText());
        }
        @Override public void refreshItem() {
            n.setText(config.getValueS());
            showOkButton(false);
            showWarnButton(false);
        }
        @Override
        protected void apply(boolean user) {
            if(insonsistent_state) return;
            Object t = get();

            boolean erroneous = t==null;
            if(erroneous) return;
            boolean applicable = !config.getValue().equals(t);
            if(!applicable) return;

            insonsistent_state = true;
            if(applyOnChange || user) config.setNapplyValue(t);
            else config.setValue(t);
            insonsistent_state = false;
        }
        private void showOkButton(boolean val) {
            if (val) n.setLeft(okB);
            else n.setLeft(null);
            okB.setVisible(val);
        }
        private void showWarnButton(boolean val) {
            if (val) n.setRight(warnB);
            else n.setRight(null);
            warnB.setVisible(val);
        }
        
    }
    private static final class BooleanField extends ConfigField<Boolean> {
        CheckIcon cBox;
        
        private BooleanField(Config<Boolean> c) {
            super(c);
            cBox = new CheckIcon();
            refreshItem();
            cBox.selected.addListener((o,ov,nv)-> apply(false));
        }
        
        @Override public Node getControl() {
            return cBox;
        }
        @Override public Boolean get() {
            return cBox.selected.get();
        }
        @Override public void refreshItem() {
            cBox.selected.set(config.getValue());
        }
    }
    private static final class SliderField extends ConfigField<Number> {
        Slider slider;
        Label cur, min, max;
        HBox box;
        private SliderField(Config<Number> c) {
            super(c);
            double v = c.getValue().doubleValue();
            
            min = new Label(String.valueOf(c.getMin()));
            max = new Label(String.valueOf(c.getMax()));
            
            slider = new Slider(c.getMin(),c.getMax(),v);
            cur = new Label(get().toString());
            cur.setPadding(new Insets(0, 5, 0, 0)); // add gap
            // there is a slight bug where isValueChanging is false even if it
            // shouldnt. It appears when mouse clicks NOT on the thumb but on
            // the slider track instead and keeps dragging. valueChanging doesn
            // activate
            slider.valueProperty().addListener((o,ov,nv) -> {
                // also bug with snap to tick, which doesnt work on mouse drag
                // so we use get() which returns correct value
                cur.setText(get().toString());
                if(!slider.isValueChanging())
                    apply(false);
            });
            slider.setOnMouseReleased(e -> {
                if(applyOnChange) apply(false);
            });
            slider.setBlockIncrement((c.getMax()-c.getMin())/20);
            slider.setMinWidth(-1);
            slider.setPrefWidth(-1);
            slider.setMaxWidth(-1);
            
            
            box = new HBox(min,slider,max);
            box.setAlignment(CENTER_LEFT);
            box.setSpacing(5);
            
            Class type = unPrimitivize(config.getType());
            if(isIn(type, Integer.class,Short.class,Long.class)) {
                box.getChildren().add(0,cur);
                slider.setMajorTickUnit(1);
                slider.setSnapToTicks(true);
            }
        }
        
        @Override public Node getControl() {
            return box;
        }
        @Override public Number get() {
            Double d = slider.getValue();
            Class type = unPrimitivize(config.getType());
            if(Integer.class.equals(type)) return d.intValue();
            if(Double.class.equals(type)) return d;
            if(Float.class.equals(type)) return d.floatValue();
            if(Long.class.equals(type)) return d.longValue();
            if(Short.class.equals(type)) return d.shortValue();
            throw new IllegalStateException("wrong number type: " + type);
        }
        @Override public void refreshItem() {
            slider.setValue(config.getValue().doubleValue());
        }
    }
    private static final class EnumerableField extends ConfigField<Object> {
        ComboBox<Object> n;
        
        private EnumerableField(Config<Object> c) {
            super(c);
            Collection e = c.enumerateValues();
            n = new ImprovedComboBox(item -> enumToHuman(c.toS(item)));
            if(e instanceof ObservableList) n.setItems((ObservableList)e);
            else n.getItems().setAll(e);
            n.getItems().sort(by(v->c.toS(v)));
            n.setValue(c.getValue());
            n.valueProperty().addListener((o,ov,nv) -> apply(false));
            n.getStyleClass().add("combobox-field-config");
            n.focusedProperty().addListener((o,ov,nv) -> n.pseudoClassStateChanged(editedPC, nv));
        }
        
        @Override 
        public Object get() {
            return n.getValue();
        }

        @Override 
        public void refreshItem() {
            n.setValue(config.getValue());
        }

        @Override Node getControl() {
            return n;
        }
    }
    private static final class ShortcutField extends ConfigField<Action> {
        TextField txtF;
        CheckIcon globB;
        HBox group;
        String t="";
        Action a;
        
        private ShortcutField(Config<Action> con) {
            super(con);
            a = con.getValue();
            txtF = new TextField();
            txtF.setPromptText(a.getKeys());
            txtF.setOnKeyReleased(e -> {
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
                    apply(true);
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
                    if(!globB.isFocused())
                        txtF.setText("");
                }
            });
            
            globB = new CheckIcon();
            globB.selected.set(a.isGlobal());
            globB.setTooltip(globTooltip);
            globB.selected.addListener((o,ov,nv) -> apply(false));
            group = new HBox(5, globB,txtF);
            group.setAlignment(CENTER_LEFT);
            group.setPadding(Insets.EMPTY);
        }
        
        @Override public Node getControl() {
            return group;
        }
        @Override public boolean hasUnappliedValue() {
            Action a = config.getValue();
            boolean sameglobal = globB.selected.get()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }
        @Override protected void apply(boolean b) {
            // its pointless to make new Action just for this
            // config.applyValue(get()); 
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = globB.selected.get()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) || 
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            
            if(!sameglobal && !sameKeys)
                a.set(globB.selected.get(), txtF.getText());
            else if (!sameKeys)
                a.setKeys(txtF.getText());
            else if (!sameglobal)
                a.setGlobal(globB.selected.get());
            else {
                refreshItem();
            }
            refreshItem();
        }
        @Override public Action get() {
            return a;
        }
        @Override public void refreshItem() {
            Action a = config.getValue();
            txtF.setPromptText(a.getKeys());
            txtF.setText("");
            globB.selected.set(a.isGlobal());
        }
    }
    private static final class ColorField extends ConfigField<Color> {
        ColorPicker picker = new ColorPicker();
        
        private ColorField(Config<Color> c) {
            super(c);
            refreshItem();
            picker.valueProperty().addListener((o,ov,nv) -> apply(false));
        }
        
        @Override public Control getControl() {
            return picker;
        }
        @Override public Color get() {
            return picker.getValue();
        }
        @Override public void refreshItem() {
            picker.setValue(config.getValue());
        }
    }
    private static final class FontField extends ConfigField<Font> {
        FontItemNode txtF = new FontItemNode();
        
        private FontField(Config<Font> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((ov,nv) -> apply(false));
        }
        
        @Override public Control getControl() {
            return txtF;
        }
        @Override public Font get() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
    private static final class FileField extends ConfigField<File> {
        FileItemNode txtF = new FileItemNode();
        
        public FileField(Config<File> c) {
            super(c);
            refreshItem();
            txtF.setOnItemChange((ov,nv) -> apply(false));
        }
        
        @Override public Control getControl() {
            return txtF;
        }
        @Override public File get() {
            return txtF.getValue();
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
}