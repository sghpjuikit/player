
package gui.itemnode;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javafx.animation.FadeTransition;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import com.sun.javafx.scene.traversal.Direction;

import gui.itemnode.ChainValueNode.ConfigPane;
import gui.itemnode.ChainValueNode.ListConfigField;
import gui.itemnode.ItemNode.ConfigNode;
import gui.itemnode.textfield.EffectItemNode;
import gui.itemnode.textfield.FileItemNode;
import gui.itemnode.textfield.FontItemNode;
import gui.objects.combobox.ImprovedComboBox;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.textfield.DecoratedTextField;
import util.Password;
import util.access.Vo;
import util.action.Action;
import util.conf.Config;
import util.conf.Config.ListConfig;
import util.conf.Config.OverridablePropertyConfig;
import util.conf.Config.PropertyConfig;
import util.conf.Config.ReadOnlyPropertyConfig;
import util.dev.Dependency;
import util.functional.Functors.Ƒ1;
import util.parsing.Parser;
import util.type.Util;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RECYCLE;
import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.toList;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.Priority.ALWAYS;
import static main.App.Build.appTooltip;
import static util.Util.enumToHuman;
import static util.async.Async.run;
import static util.functional.Util.*;
import static util.reactive.Util.maintain;

/**
 * Editable and setable graphic control for configuring {@Config}.
 * <p/>
 * Convenient way to create wide and diverse property sheets, that take
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 *
 * @author Martin Polakovic
 */
abstract public class ConfigField<T> extends ConfigNode<T> {

    private static final PseudoClass editedPC = getPseudoClass("edited");
    private static final Tooltip okTooltip = appTooltip("Apply value");
    private static final Tooltip warnTooltip = appTooltip("Erroneous value");
    private static final Tooltip defTooltip = appTooltip("Default value");
    private static final Tooltip globTooltip = appTooltip("Global shortcut"
            + "\n\nGlobal shortcut can be used even when application doesn't have focus. Note, that "
            + "only one application can use this shortcut. If multiple applications use the same "
            + "shortcut, the one started later will have it disabled.");
    private static final Tooltip overTooltip = appTooltip("Override value"
            + "\n\nUses local value if true and global value if false.");

    protected final HBox root = new HBox();
    public boolean applyOnChange = true;
    protected boolean inconsistentState = false;
    private Icon defB;

    private ConfigField(Config<T> c) {
        super(c);

        root.setMinSize(0,20);   // min height actually required to get consistent look
        root.setPrefSize(-1,-1); // support variable content height
        root.setMaxSize(-1,-1);  // support variable content height
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
                    // we do not want hundreds of buttons we will never use anyway
                    if(defB==null) {
                        defB = new Icon(RECYCLE, 11, null, this::setNapplyDefault);
                        defB.tooltip(defTooltip);
                        defB.styleclass("config-field-default-button");
                        defB.setOpacity(0);
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
     * Creates label with config field name and tooltip with config field description.
     * @return label describing this field
     */
    public Label createLabel() {
        Label l = new Label(config.getGuiName());

        String tooltip_text = getTooltipText();
        if(!tooltip_text.isEmpty()) {
            Tooltip t = appTooltip(tooltip_text);
                    t.setWrapText(true);
                    t.setMaxWidth(300);
            l.setTooltip(t);
        }

        return l;
    }

    /**
     * Use to get the control node for setting and displaying the value to
     * attach it to a scene graph.
     * @return setter control for this field
     */
    @Override
    public final HBox getNode() {
        Node config = getControl();
        if(!root.getChildren().contains(config))
            root.getChildren().add(0, config);
        HBox.setHgrow(config, ALWAYS);
        HBox.setHgrow(config.getParent(), ALWAYS);
        return root;
    }

    /**
     * Use to get the control node for setting and displaying the value to
     * attach it to a scene graph.
     * @return setter control for this field
     */
    abstract Node getControl();

    protected String getTooltipText() {
        return config.getInfo();
    }

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
    public void setNapplyDefault() {
        T t = config.getDefaultValue();
        if(!config.getValue().equals(t)) {
            config.setNapplyValue(t);
            refreshItem();
            if(onChange!=null) onChange.run();//System.out.println("changed config " + config.getName());
        }
    }

    Runnable onChange;

    public void apply() {
        apply(true);
    }

    protected void apply(boolean user) {
        if(inconsistentState) return;
        T t = get();
        boolean erroneous = t==null;
        if(erroneous) return;
        boolean needsapply = !Objects.equals(t, config.getValue());
        if(!needsapply) return;

        inconsistentState = true;
        if(applyOnChange || user) config.setNapplyValue(t);
        else config.setValue(t);
        refreshItem();
        if(onChange!=null) onChange.run();//System.out.println("changed config " + config.getName());
        inconsistentState = false;
    }

/******************************************************************************/

    private static Map<Class<?>,Ƒ1<Config,ConfigField>> m = new HashMap<>();

    static {
        m.put(boolean.class, BooleanField::new);
        m.put(Boolean.class, BooleanField::new);
        m.put(String.class, GeneralField::new);
        m.put(Action.class, ShortcutField::new);
        m.put(Color.class, ColorField::new);
        m.put(File.class, FileField::new);
        m.put(Font.class, FontField::new);
        m.put(Effect.class, config -> new EffectField(config,Effect.class));
        m.put(Password.class, PasswordField::new);
        m.put(Charset.class, charset -> new EnumerableField<Charset>(charset,list(ISO_8859_1,US_ASCII,UTF_8,UTF_16,UTF_16BE,UTF_16LE)));
        m.put(String.class, StringField::new);
        m.put(KeyCode.class, KeyCodeField::new);
        m.put(ObservableList.class, ListField::new);
        EffectItemNode.EFFECT_TYPES.stream().filter(et -> et.type!=null)
                      .forEach(et -> m.put(et.type,config -> new EffectField(config,et.type)));
    }

    /**
     * Creates ConfigFfield best suited for the specified Field.
     * @param config field for which the GUI will be created
     */
    @SuppressWarnings("unchecked")
    public static <T> ConfigField<T> create(Config<T> config) {
        Config c = config;
        ConfigField cf;
        if (c instanceof OverridablePropertyConfig) cf = new OverridableField((OverridablePropertyConfig) c);
        else if (c.isTypeEnumerable()) cf = c.getType()==KeyCode.class ? new KeyCodeField(c) : new EnumerableField(c);
        else if(c.isMinMax()) cf = new SliderField(c);
        else cf = m.getOrDefault(c.getType(), GeneralField::new).apply(c);

        cf.setEditable(c.isEditable());

        return cf;
    }

    public static <T> ConfigField<T> createForProperty(Class<T> type, String name, Object property) {
        return create(Config.forProperty(type, name, property));
    }

    private static <T> ObservableValue<T> getObservableValue(Config<T> c) {
        return c instanceof PropertyConfig && ((PropertyConfig)c).getProperty() instanceof ObservableValue
                ? (ObservableValue)((PropertyConfig)c).getProperty()
                : c instanceof ReadOnlyPropertyConfig
                    ? ((ReadOnlyPropertyConfig)c).getProperty()
                    : null;
    }

/***************************** IMPLEMENTATIONS ********************************/

    private static class PasswordField extends ConfigField<Password>{

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
    private static class StringField extends ConfigField<String> {
        private DecoratedTextField n = new DecoratedTextField();

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
                if (isAny(e.getCode(), BACK_SPACE,DELETE)) {
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
            if(inconsistentState) return;
            n.setText(config.getValueS());
        }

    }
    private static class GeneralField extends ConfigField<Object> {
        DecoratedTextField n = new DecoratedTextField();
        Icon okI= new Icon();
        Icon warnB = new Icon();
        AnchorPane okB = new AnchorPane(okI);

        private GeneralField(Config c) {
            super(c);

            // does not work because of CustomTextField instead f TextField
            // restrict input
//            if(c.isTypeNumber())
//                InputConstraints.numbersOnly(txtF, !c.isTypeNumberNonegative(), c.isTypeFloatingNumber());

            okB.setPrefSize(11, 11);
            okB.setMinSize(11, 11);
            okB.setMaxSize(11, 11);
            okI.styleclass("config-field-ok-button");
            okI.size(11);
            okI.tooltip(okTooltip);
            warnB.size(11);
            warnB.styleclass("config-field-warn-button");
            warnB.tooltip(warnTooltip);

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
            if(inconsistentState) return;
            Object t = get();

            boolean erroneous = t==null;
            if(erroneous) return;
            boolean applicable = !config.getValue().equals(t);
            if(!applicable) return;

            inconsistentState = true;
            if(applyOnChange || user) config.setNapplyValue(t);
            else config.setValue(t);
            inconsistentState = false;
        }
        private void showOkButton(boolean val) {
            n.setLeft(val ? okI : null);
            okI.setVisible(val);
        }
        private void showWarnButton(boolean val) {
            n.setRight(val ? warnB : null);
            warnB.setVisible(val);
            if(val) warnTooltip.setText(Parser.DEFAULT.getError());
        }

    }
    private static class BooleanField extends ConfigField<Boolean> {
        final CheckIcon cBox;
        final boolean observable;

        private BooleanField(Config<Boolean> c) {
            super(c);

            ObservableValue<Boolean> v = getObservableValue(c);
            observable = v!=null;

            cBox = new CheckIcon();
            cBox.styleclass("boolean-config-field");
            cBox.selected.setValue(config.getValue());
            // bind config -> config field (if possible)
            if(observable) v.addListener((o,ov,nv) -> cBox.selected.setValue(nv));
            // bind config field -> config
            cBox.selected.addListener((o,ov,nv) -> config.setNapplyValue(nv));
        }

        @Override public CheckIcon getControl() {
            return cBox;
        }
        @Override public Boolean get() {
            return cBox.selected.getValue();
        }
        @Override public void refreshItem() {
            if(!observable)
                cBox.selected.setValue(config.getValue());
        }
    }
    private static class OverrideField extends BooleanField {
        private OverrideField(Config<Boolean> c) {
            super(c);
            cBox.styleclass("override-config-field");
            cBox.tooltip(overTooltip);
        }
    }
    private static class SliderField extends ConfigField<Number> {
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
                // also bug with snap to tick, which does not work on mouse drag
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

            Class type = config.getType();
            if(isContainedIn(type, Integer.class,Short.class,Long.class)) {
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
            Class type = config.getType();
            if(Integer.class==type) return d.intValue();
            if(Double.class==type) return d;
            if(Float.class==type) return d.floatValue();
            if(Long.class==type) return d.longValue();
            if(Short.class==type) return d.shortValue();
            throw new IllegalStateException("wrong number type: " + type);
        }
        @Override public void refreshItem() {
            slider.setValue(config.getValue().doubleValue());
        }
    }
    private static class EnumerableField<T> extends ConfigField<T> {
        ComboBox<T> n;

        private EnumerableField(Config<T> c) {
            this(c, c.enumerateValues());
        }

        private EnumerableField(Config<T> c, Collection<T> enumeration) {
            super(c);
            n = new ImprovedComboBox<>(item -> enumToHuman(c.toS(item)));
            if(enumeration instanceof ObservableList) n.setItems((ObservableList<T>)enumeration);
            else n.getItems().setAll(enumeration);
            n.getItems().sort(by(c::toS));
            n.setValue(c.getValue());
            n.valueProperty().addListener((o,ov,nv) -> apply(false));
            n.getStyleClass().add("combobox-field-config");
            n.focusedProperty().addListener((o,ov,nv) -> n.pseudoClassStateChanged(editedPC, nv));
        }

        @Override
        public T get() {
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
    private static class KeyCodeField extends EnumerableField<KeyCode> {

	    @Dependency("requires access to com.sun.javafx.scene.traversal.Direction")
	    private KeyCodeField(Config<KeyCode> c) {
            super(c);

            n.setOnKeyPressed(Event::consume);
            n.setOnKeyReleased(Event::consume);
            n.setOnKeyTyped(Event::consume);
            n.addEventFilter(KeyEvent.ANY, e -> {
                // Note that in case of UP, DOWN, LEFT, RIGHT arrow keys and potentially others (any
                // which cause selection change) the KEY_PRESSED event will not get fired!
                //
                // Hence we set the value in case of key event of any type. This causes the value to
                // be set twice, but should be all right since the value is the same anyway.
                n.setValue(e.getCode());

	            // TODO: jigsaw
                if(e.getEventType()==KEY_RELEASED) {
                    // conveniently traverse focus by simulating TAB behavior
                    // currently only hacks allow this
                    //((BehaviorSkinBase)n.getSkin()).getBehavior().traverseNext(); // !work since java9
	                 n.impl_traverse(Direction.NEXT);
                }
                e.consume();
            });
        }

    }
    private static class ShortcutField extends ConfigField<Action> {
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
                e.consume();
            });
            txtF.setEditable(false);
            txtF.setTooltip(appTooltip(a.getInfo()));
            txtF.focusedProperty().addListener((o,ov,nv) -> {
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
            globB.styleclass("shortcut-global-config-field");
            globB.selected.setValue(a.isGlobal());
            globB.tooltip(globTooltip);
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
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) ||
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }
        @Override protected void apply(boolean b) {
            // its pointless to make new Action just for this
            // config.applyValue(get());
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) ||
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));

            if(!sameglobal && !sameKeys)
                a.set(globB.selected.getValue(), txtF.getText());
            else if (!sameKeys)
                a.setKeys(txtF.getText());
            else if (!sameglobal)
                a.setGlobal(globB.selected.getValue());
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
            globB.selected.setValue(a.isGlobal());
        }
    }
    private static class ColorField extends ConfigField<Color> {
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
    private static class FontField extends ConfigField<Font> {
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
    private static class FileField extends ConfigField<File> {
        FileItemNode editor = new FileItemNode();
        boolean observable;

        public FileField(Config<File> c) {
            super(c);
            ObservableValue<File> v = getObservableValue(c);
            observable = v!=null;
            editor.setValue(config.getValue());
            if(observable) v.addListener((o,ov,nv) -> editor.setValue(nv));
            editor.setOnItemChange((ov,nv) -> apply(false));
        }

        @Override public Control getControl() {
            return editor;
        }
        @Override public File get() {
            return editor.getValue();
        }
        @Override public void refreshItem() {
            if(!observable)
                editor.setValue(config.getValue());
        }
    }
    private static class EffectField extends ConfigField<Effect> {

        private final EffectItemNode editor;

        public EffectField(Config<Effect> c, Class<? extends Effect> effect_type) {
            super(c);
            editor = new EffectItemNode(effect_type);
            refreshItem();
            editor.setOnItemChange((ov,nv) -> apply(false));
        }

        @Override public Control getControl() {
            return editor;
        }
        @Override public Effect get() {
            return editor.getValue();
        }
        @Override public void refreshItem() {
            editor.setValue(config.getValue());
        }
        // The problem here is that officially Configs and ConfigFields do not support null value
        // but obviously there are cases null makes sense, such as with Effect. For now we add
        // support for null here
        @Override
        protected void apply(boolean user) {
            Effect t = get();
            if(applyOnChange || user) config.setNapplyValue(t);
            else config.setValue(t);
            refreshItem();
            if(onChange!=null) onChange.run();
        }
    }
    private static class ListField<T> extends ConfigField<ObservableList<T>> {

        private final ListConfigField<T,ConfigurableField> chain;
        private final ListConfig<T> lc;

        public ListField(Config<ObservableList<T>> c) {
            super(c);
            lc = (ListConfig)c;

            // create chain
            chain = new ListConfigField<>(0,() -> new ConfigurableField(lc.a.factory.get()));
            // initialize chain - add existing list values to chain
            lc.a.list.forEach(v -> chain.addChained(new ConfigurableField(v)));
            chain.growTo1();
            // bind list to the chain values (after it was initialized above)
            chain.onItemChange = ignored -> lc.a.list.setAll(chain.getValues().collect(toList()));
        }

        @Override
        Node getControl() {
            return chain.getNode();
        }

        @Override
        protected ObservableList<T> get() {
            return config.getValue(); // return the ever-same observable list
        }

        @Override
        public void refreshItem() {}


        class ConfigurableField extends ValueNode<T> {
            ConfigPane<Object> p = new ConfigPane<>();

            public ConfigurableField(T t) {
                value = t;
                p.configure(lc.a.toConfigurable.apply(value));
                p.onChange = () -> chain.onItemChange.accept(null);
            }

            @Override
            public Node getNode() {
                return p.getNode();
            }

            @Override
            public T getValue() {
                Object o = p.getValuesC().get(0).getValue();
                if(value.getClass().equals(o.getClass())) return (T)o;
                else return super.getValue();
            }

        }
    }
    private static class OverridableField<T> extends ConfigField<T> {
        final FlowPane root = new FlowPane(5,5);

        public OverridableField(OverridablePropertyConfig<T> c) {
            super(c);
            Vo<T> vo = c.getProperty();

//            root.setMinSize(100,20);
//            root.setPrefSize(-1,-1);
//            root.setMaxSize(-1,-1);

            BooleanField bf = new OverrideField(Config.forProperty(Boolean.class, "Override", vo.override)) {
                @Override
                public void setNapplyDefault() {
                    vo.override.setValue(c.getDefaultOverrideValue());
                }
            };
            ConfigField cf = create(Config.forProperty(c.getType(),"", vo.real));
            Util.setField(cf.config, "defaultValue", c.getDefaultValue());
            maintain(vo.override, b -> !b, cf.getControl().disableProperty());
            root.getChildren().addAll(cf.getNode(),bf.getNode());
        }
        @Override
        Node getControl() {
            return root;
        }

        @Override
        protected T get() {
            return config.getValue();
        }

        @Override
        public void refreshItem() {}

        @Override
        public void setNapplyDefault() {
            config.setDefaultValue();
        }

        @Override
        protected String getTooltipText() {
            return config.getInfo() + "\n\nThis value must override global "
                    + "value to take effect.";
        }
    }
}