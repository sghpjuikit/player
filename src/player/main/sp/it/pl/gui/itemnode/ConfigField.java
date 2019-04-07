package sp.it.pl.gui.itemnode;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import sp.it.pl.gui.itemnode.ChainValueNode.ListConfigField;
import sp.it.pl.gui.itemnode.textfield.EffectTextField;
import sp.it.pl.gui.itemnode.textfield.FileTextField;
import sp.it.pl.gui.itemnode.textfield.FontTextField;
import sp.it.pl.gui.objects.combobox.ImprovedComboBox;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.textfield.DecoratedTextField;
import sp.it.pl.gui.pane.ConfigPane;
import sp.it.pl.util.access.Vo;
import sp.it.pl.util.action.Action;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.Config.ListConfig;
import sp.it.pl.util.conf.Config.OverridablePropertyConfig;
import sp.it.pl.util.conf.Config.PropertyConfig;
import sp.it.pl.util.conf.Config.ReadOnlyPropertyConfig;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.functional.Functors.Ƒ1;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.text.Password;
import sp.it.pl.util.type.Util;
import sp.it.pl.util.validation.Constraint;
import sp.it.pl.util.validation.Constraint.HasNonNullElements;
import sp.it.pl.util.validation.Constraint.NumberMinMax;
import sp.it.pl.util.validation.Constraint.ReadOnlyIf;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.util.Util.enumToHuman;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.conf.ConfigurationUtilKt.isEditableByUserRightNow;
import static sp.it.pl.util.functional.Try.Java.ok;
import static sp.it.pl.util.functional.TryKt.getAny;
import static sp.it.pl.util.functional.Util.IS;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.equalsAny;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.reactive.UtilKt.maintain;
import static sp.it.pl.util.ui.Util.layHeaderTop;
import static sp.it.pl.util.ui.Util.layHorizontally;
import static sp.it.pl.util.ui.UtilKt.pseudoclass;

/**
 * Editable and settable graphic control for configuring {@link sp.it.pl.util.conf.Config}.
 * <p/>
 * Convenient way to create wide and diverse property sheets, that take
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 */
abstract public class ConfigField<T> extends ConfigNode<T> {

    private static final PseudoClass editedPC = pseudoclass("edited");
    private static final Tooltip okTooltip = appTooltip("Apply value");
    private static final Tooltip warnTooltip = appTooltip("Erroneous value");
    private static final Tooltip defTooltip = appTooltip("Default value");
    private static final Tooltip globTooltip = appTooltip("Global shortcut"
            + "\n\nGlobal shortcuts can be used even when the application doesn't have focus.\n"
            + "Note that only one application can use this shortcut. If multiple applications use "
            + "the same shortcut, only the one that was first started will work.");
    private static final Tooltip overTooltip = appTooltip("Override value"
            + "\n\nUses local value if true and global value if false.");

    public static final String STYLECLASS_CONFIG_FIELD_PROCEED_BUTTON = "config-field-proceed-button";
    public static final String STYLECLASS_CONFIG_FIELD_OK_BUTTON = "config-field-ok-button";
    public static final String STYLECLASS_CONFIG_FIELD_WARN_BUTTON = "config-field-warn-button";
    public static final String STYLECLASS_TEXT_CONFIG_FIELD = "text-field-config";

    @SuppressWarnings("unchecked")
    private static Map<Class<?>,Ƒ1<Config,ConfigField>> CF_BUILDERS = new HashMap<>() {{
        put(boolean.class, BooleanField::new);
        put(Boolean.class, BooleanField::new);
        put(String.class, GeneralField::new);
        put(Action.class, ShortcutField::new);
        put(Color.class, ColorField::new);
        put(File.class, FileField::new);
        put(Font.class, FontField::new);
        put(Effect.class, config -> new EffectField(config, Effect.class));
        put(Password.class, PasswordField::new);
        put(Charset.class, charset -> new EnumerableField<>(charset, list(ISO_8859_1, US_ASCII, UTF_8, UTF_16, UTF_16BE, UTF_16LE)));
        put(KeyCode.class, KeyCodeField::new);
        put(Configurable.class, ConfigurableField::new);
        put(ObservableList.class, config -> {
            if (config instanceof ListConfig) {
                return Configurable.class.isAssignableFrom(((ListConfig)config).a.itemType)
                    ? new ListFieldPaginated(config)
                    : new ListField<>(config);
            } else {
                return new GeneralField<>(config);
            }
        });
        EffectTextField.EFFECT_TYPES.stream().map(et -> et.getType()).filter(t -> t!=null).forEach(t -> put(t, config -> new EffectField(config, t)));
    }};

    /**
     * Creates ConfigFfield best suited for the specified Field.
     *
     * @param config field for which the GUI will be created
     */
    @SuppressWarnings("unchecked")
    public static <T> ConfigField<T> create(Config<T> config) {
        Config c = config;
        ConfigField cf;
        if (c instanceof OverridablePropertyConfig) cf = new OverridableField((OverridablePropertyConfig) c);
        else if (c.isTypeEnumerable()) cf = c.getType()==KeyCode.class ? new KeyCodeField(c) : new EnumerableField(c);
        else if (isMinMax(c)) cf = new SliderField(c);
        else cf = CF_BUILDERS.computeIfAbsent(c.getType(), key -> GeneralField::new).apply(c);

	    disableIfReadOnly(cf, c);

        return cf;
    }

    public static boolean isMinMax(Config<?> c) {
        return Number.class.isAssignableFrom(c.getType()) && c.getConstraints().stream().anyMatch(l -> l.getClass()==NumberMinMax.class);
    }

    public static <T> ConfigField<T> createForProperty(Class<T> type, String name, Object property) {
        return create(Config.forProperty(type, name, property));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObservableValue<T> getObservableValue(Config<T> c) {
        return c instanceof PropertyConfig && ((PropertyConfig)c).getProperty() instanceof ObservableValue
                   ? (ObservableValue)((PropertyConfig)c).getProperty()
                   : c instanceof ReadOnlyPropertyConfig
                         ? ((ReadOnlyPropertyConfig)c).getProperty()
                         : null;
    }

/* ------------------------------------------------------------------------------------------------------------------ */

    protected final HBox root = new HBox();
    public boolean applyOnChange = true;
    protected boolean inconsistentState = false;
    private Icon defB;
    public Try<T,String> value = ok(null);
    public Consumer<? super Try<T,String>> observer;

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
            if (!isEditableByUserRightNow(config)) return;
            runFX(millis(270), () -> {
                if (root.isHover()) {
                    if (defB==null && isEditableByUserRightNow(c)) {
                        defB = new Icon(null, 11, null, this::setNapplyDefault);
                        defB.tooltip(defTooltip);
                        defB.styleclass("config-field-default-button");
                        defB.setOpacity(0);
                        root.getChildren().add(defB);
                        root.setPadding(Insets.EMPTY);
                    }
                    FadeTransition fa = new FadeTransition(millis(450), defB);
                    fa.stop();
                    fa.setToValue(1);
                    fa.play();
                }
            });
        });
        // hide default button
        root.addEventFilter(MOUSE_EXITED, e-> {
            // return if nothing to hide
            if (defB == null) return;
            // hide it
            FadeTransition fa = new FadeTransition(millis(450), defB);
            fa.stop();
            fa.setDelay(Duration.ZERO);
            fa.setToValue(0);
            fa.play();
        });
    }

    /**
     * Simply compares the current value with the one obtained from Config.
     * Equivalent to: {@code !Objects.equals(config.getValue(), getValid())}
     *
     * @return true if has value that has not been applied
     */
    public boolean hasUnappliedValue() {
        return !Objects.equals(config.getValue(), getValid());
    }

    /**
     * Creates label with config field name and tooltip with config field description.
     *
     * @return label describing this field
     */
    public Label createLabel() {
        Label l = new Label(config.getGuiName());

        String tooltip_text = getTooltipText();
        if (!tooltip_text.isEmpty()) {
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
     *
     * @return setter control for this field
     */
    @Override
    public final HBox getNode() {
        Node config = getControl();
        if (!root.getChildren().contains(config))
            root.getChildren().add(0, config);
        HBox.setHgrow(config, ALWAYS);
        HBox.setHgrow(config.getParent(), ALWAYS);
        return root;
    }

    /**
     * Use to get the control node for setting and displaying the value to
     * attach it to a scene graph.
     *
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

    protected abstract Try<T,String> get();

    @SuppressWarnings("unchecked")
    public Try<T,String> getValid() {
        value = get().and(v -> config.getConstraints().stream().map(c -> c.validate(v)).reduce(ok(),Try::and));
        if (observer!=null) observer.accept(value);
        return value;
    }

    /**
     * Refreshes the content of this config field. The content is read from the
     * Config and as such reflects the real value. Using this method after the
     * applying the new value will confirm the success visually to the user.
     */
    public abstract void refreshItem();

    /** Sets and applies default value of the config if it has different value set and if editable by user. */
    public void setNapplyDefault() {
        if (isEditableByUserRightNow(config)) {
            T t = config.getDefaultValue();
            if (!Objects.equals(config.getValue(), t)) {
                config.setValue(t);
                refreshItem();
                if (onChange!=null) onChange.run();
            }
        }
    }

    public Runnable onChange;

    public void apply() {
        apply(true);
    }

    protected void apply(boolean user) {
        if (inconsistentState) return;
        getValid().ifOk(v -> {
            boolean needsapply = !Objects.equals(v, config.getValue());
            if (!needsapply) return;
            inconsistentState = true;
            config.setValue(v);
            refreshItem();
            if (onChange!=null) onChange.run();
            inconsistentState = false;
        });

    }

/* ---------- IMPLEMENTATIONS --------------------------------------------------------------------------------------- */

    private static class PasswordField extends ConfigField<Password> {
        javafx.scene.control.PasswordField graphics = new javafx.scene.control.PasswordField();

        public PasswordField(Config<Password> c) {
            super(c);
            graphics.setPromptText(c.getGuiName());
            refreshItem();
        }

        @Override
        Node getControl() {
            return graphics;
        }

        @Override
        public Try<Password,String> get() {
            return ok(new Password(graphics.getText()));
        }

        @Override
        public void refreshItem() {
            graphics.setText(config.getValue().getValue());
        }

    }
    private static class GeneralField<T> extends ConfigField<T> {
        private final DecoratedTextField n = new DecoratedTextField();
        private final boolean isObservable;
        private final Icon okI= new Icon();
        private final Icon warnB = new Icon();
        private final AnchorPane okB = new AnchorPane(okI);

        private GeneralField(Config<T> c) {
            super(c);

            ObservableValue<T> obv = getObservableValue(c);
            isObservable = obv!=null;
            if (isObservable) obv.addListener((o,ov,nv) -> refreshItem());

            okB.setPrefSize(11, 11);
            okB.setMinSize(11, 11);
            okB.setMaxSize(11, 11);
            okI.styleclass(STYLECLASS_CONFIG_FIELD_OK_BUTTON);
            okI.tooltip(okTooltip);
            warnB.styleclass(STYLECLASS_CONFIG_FIELD_WARN_BUTTON);
            warnB.tooltip(warnTooltip);

            n.getStyleClass().setAll("text-field","text-input");
            n.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            n.setPromptText(c.getGuiName());
            n.setText(c.getValueS());

            n.focusedProperty().addListener((o,ov,nv) -> {
                if (nv) {
                    n.pseudoClassStateChanged(editedPC, true);
                } else {
                    n.pseudoClassStateChanged(editedPC, false);
//                    // the timer solves a little bug where the focus shift from
//                    // txtF to okB has a delay which we need to jump over
//                    run(80, () -> {
//                        if (!okBL.isFocused() && !okB.isFocused()) {
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
                Try<T,String> t = getValid();
                boolean applicable = t.map(v -> !Objects.equals(config.getValue(), v)).getOr(false);
                showOkButton(!applyOnChange && applicable && t.isOk());
                showWarnButton(t);
                if (applyOnChange) apply(false);
            });
            okI.setOnMouseClicked(e -> {
                apply(true);
                e.consume();
            });
            n.setOnKeyPressed(e -> {
                if (e.getCode()==ENTER) {
                    apply(true);
                    e.consume();
                }
            });

            Try<T,String> t = getValid();
            showWarnButton(t);
        }

        @Override
        public Control getControl() {
            return n;
        }

        @Override
        public void focus() {
            n.requestFocus();
            n.selectAll();
        }

        @Override
        public Try<T,String> get() {
            return config.ofS(n.getText());
        }

        @Override
        public void refreshItem() {
            n.setText(config.getValueS());
            showOkButton(false);
            showWarnButton(ok());
        }

        @Override
        protected void apply(boolean user) {
            if (inconsistentState) return;
            getValid().ifOk(v -> {
                boolean applicable = !Objects.equals(config.getValue(), v);
                if (!applicable) return;

                inconsistentState = true;
                config.setValue(v);
                if (onChange!=null) onChange.run();
                inconsistentState = false;
            });
        }

        private void showOkButton(boolean val) {
            n.left.setValue(val ? okI : null);
            okI.setVisible(val);
        }

        private void showWarnButton(Try<?,String> value) {
            n.right.setValue(value.isError() ? warnB : null);
            warnB.setVisible(value.isError());
            warnTooltip.setText(getAny(value.map(v -> "")));
        }

        private void showWarnButton(boolean val) {
            n.right.setValue(val ? warnB : null);
            warnB.setVisible(val);
        }

    }
    private static class BooleanField extends ConfigField<Boolean> {
        protected final CheckIcon graphics;
        private final boolean isObservable;

        private BooleanField(Config<Boolean> c) {
            super(c);

            ObservableValue<Boolean> v = getObservableValue(c);
            isObservable = v!=null;

            graphics = new CheckIcon();
            graphics.styleclass("boolean-config-field");
            graphics.selected.setValue(config.getValue());
            if (isObservable) v.addListener((o,ov,nv) -> graphics.selected.setValue(nv));
            graphics.selected.addListener((o,ov,nv) -> config.setValue(nv));
        }

        @Override
        public CheckIcon getControl() {
            return graphics;
        }

        @Override
        public Try<Boolean,String> get() {
            return ok(graphics.selected.getValue());
        }

        @Override
        public void refreshItem() {
            if (!isObservable)
                graphics.selected.setValue(config.getValue());
        }
    }
    private static class OverrideField extends BooleanField {
        private OverrideField(Config<Boolean> c) {
            super(c);
            graphics.styleclass("override-config-field");
            graphics.tooltip(overTooltip);
        }
    }
    private static class SliderField extends ConfigField<Number> {
        private final Slider slider;
        private final Label cur, min, max;
        private final HBox box;
        private final boolean isObservable;

        private SliderField(Config<Number> c) {
            super(c);

            ObservableValue<Number> v = getObservableValue(c);
            isObservable = v!=null;
            if (isObservable) v.addListener((o,ov,nv) -> refreshItem());

            double val = c.getValue().doubleValue();
            NumberMinMax range = stream(c.getConstraints())
                .filter(NumberMinMax.class::isInstance).map(NumberMinMax.class::cast)
                .findAny().get();

            min = new Label(String.valueOf(range.getMin()));
            max = new Label(String.valueOf(range.getMax()));

            slider = new Slider(range.getMin(),range.getMax(),val);
            cur = new Label(computeLabelText());
            cur.setPadding(new Insets(0, 5, 0, 0)); // add gap
            // there is a slight bug where isValueChanging is false even if it should not. It appears when mouse clicks
            // NOT on the thumb but on the slider track instead and keeps dragging. valueChanging does not activate
            slider.valueProperty().addListener((o,ov,nv) -> {
                // also bug with snap to tick, which does not work on mouse drag so we use get() which returns correct value
                cur.setText(computeLabelText());
                if (!slider.isValueChanging())
                    apply(false);
            });
            slider.setOnMouseReleased(e -> apply(false));
            slider.setBlockIncrement((range.getMax()-range.getMin())/20);
            slider.setMinWidth(-1);
            slider.setPrefWidth(-1);
            slider.setMaxWidth(-1);

            box = new HBox(min,slider,max);
            box.setAlignment(CENTER_LEFT);
            box.setSpacing(5);

            Class type = config.getType();
            if (equalsAny(type, Integer.class,Short.class,Long.class)) {
                box.getChildren().add(0,cur);
                slider.setMajorTickUnit(1);
                slider.setSnapToTicks(true);
            }
        }

        @Override
        public Node getControl() {
            return box;
        }

        @Override
        public Try<Number,String> get() {
            Double d = slider.getValue();
            Class type = config.getType();
            if (Integer.class==type) return ok(d.intValue());
            if (Double.class==type) return ok(d);
            if (Float.class==type) return ok(d.floatValue());
            if (Long.class==type) return ok(d.longValue());
            if (Short.class==type) return ok(d.shortValue());
            throw new IllegalStateException("wrong number type: " + type);
        }

        @Override
        public void refreshItem() {
            slider.setValue(config.getValue().doubleValue());
        }

        private String computeLabelText() {
            return getValid().map(Object::toString).getOr("");
        }
    }
    private static class EnumerableField<T> extends ConfigField<T> {
        ComboBox<T> n;

        private EnumerableField(Config<T> c) {
            this(c, c.enumerateValues());
        }

        private EnumerableField(Config<T> c, Collection<T> enumeration) {
            super(c);

            boolean sortable = c.getConstraints().stream().noneMatch(con -> con instanceof Constraint.PreserveOrder);

            n = new ImprovedComboBox<>(item -> enumToHuman(c.toS(item)));

            if (enumeration instanceof ObservableList) n.setItems((ObservableList<T>)enumeration);
            else n.getItems().setAll(enumeration);
            if (sortable) n.getItems().sort(by(c::toS));
            n.setValue(c.getValue());
            n.valueProperty().addListener((o,ov,nv) -> apply(false));
            n.getStyleClass().add("combobox-field-config");
            n.focusedProperty().addListener((o,ov,nv) -> n.pseudoClassStateChanged(editedPC, nv));
        }

        @Override
        public Try<T,String> get() {
            return ok(n.getValue());
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

                e.consume();
            });
        }

    }
    private static class ShortcutField extends ConfigField<Action> {
        DecoratedTextField txtF;
        CheckIcon globB;
        HBox group;
        String t="";
        Action a;
        Icon runB;

        private ShortcutField(Config<Action> con) {
            super(con);
            a = con.getValue();

            globB = new CheckIcon();
            globB.styleclass("config-shortcut-global-icon");
            globB.selected.setValue(a.isGlobal());
            globB.tooltip(globTooltip);
            globB.selected.addListener((o,ov,nv) -> apply(false));

            txtF = new DecoratedTextField();
            txtF.setPromptText(computePromptText());
            txtF.setOnKeyReleased(e -> {
                KeyCode c = e.getCode();
                // handle subtraction
                if (c==BACK_SPACE || c==DELETE) {
                    txtF.setPromptText("<none>");
                    if (!txtF.getText().isEmpty()) txtF.setPromptText(computePromptText());

                    if (t.isEmpty()) {  // set back to empty
                        txtF.setPromptText(computePromptText());
                    } else {            // subtract one key
                        if (t.indexOf('+') == -1) t="";
                        else t=t.substring(0,t.lastIndexOf('+'));
                        txtF.setText(t);
                    }
                } else if (c==ENTER) {
                    apply(true);
                } else if (c==ESCAPE) {
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
                if (nv) {
                    txtF.setText(a.getKeys());
                } else {
                    // prevent 'deselection' if we txtF lost focus because glob
                    // received click
                    if (!globB.isFocused())
                        txtF.setText("");
                }
            });
            txtF.left.setValue(globB);

            runB = new Icon();
            runB.styleclass("config-shortcut-run-icon");
            runB.onClick(a);
            runB.tooltip("Run " + a.getGuiName());

            group = new HBox(5, runB, txtF);
            group.setAlignment(CENTER_LEFT);
            group.setPadding(Insets.EMPTY);
        }

        private String computePromptText() {
            String keys = a.getKeys();
            return keys.isEmpty() ? "<none>" : keys;
        }

        @Override
        public Node getControl() {
            return group;
        }

        @Override
        public boolean hasUnappliedValue() {
            Action a = config.getValue();
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) ||
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }

        @Override
        protected void apply(boolean b) {
            // its pointless to make new Action just for this
            // config.applyValue(get());
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = txtF.getText().equals(a.getKeys()) ||
                    (txtF.getText().isEmpty() && txtF.getPromptText().equals(a.getKeys()));

            if (!sameglobal && !sameKeys)
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

        @Override
        public Try<Action,String> get() {
            return ok(a);
        }

        @Override
        public void refreshItem() {
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
        @Override public Try<Color,String> get() {
            return ok(picker.getValue());
        }
        @Override public void refreshItem() {
            picker.setValue(config.getValue());
        }
    }
    private static class FontField extends ConfigField<Font> {
        FontTextField txtF = new FontTextField();

        private FontField(Config<Font> c) {
            super(c);
            refreshItem();
            txtF.setOnValueChange((ov, nv) -> apply(false));
        }

        @Override public Control getControl() {
            return txtF;
        }
        @Override public Try<Font,String> get() {
            return ok(txtF.getValue());
        }
        @Override public void refreshItem() {
            txtF.setValue(config.getValue());
        }
    }
    private static class FileField extends ConfigField<File> {
        FileTextField editor;
        boolean isObservable;

        public FileField(Config<File> c) {
            super(c);
            ObservableValue<File> v = getObservableValue(c);
            isObservable = v!=null;
            Constraint.FileActor constraint = stream(c.getConstraints())
                .filter(Constraint.FileActor.class::isInstance).map(Constraint.FileActor.class::cast)
                .findFirst().orElse(Constraint.FileActor.ANY);

            editor = new FileTextField(constraint);
            editor.setValue(config.getValue());
            editor.setOnKeyPressed(e -> {
                if (e.getCode()==ENTER) {
                    e.consume();
                }
            });

            if (isObservable) v.addListener((o,ov,nv) -> editor.setValue(nv));
            editor.setOnValueChange((ov, nv) -> apply(false));
        }

        @Override
        public Control getControl() {
            return editor;
        }

        @Override
        public Try<File,String> get() {
            return ok(editor.getValue());
        }

        @Override
        public void refreshItem() {
            if (!isObservable)
                editor.setValue(config.getValue());
        }
    }
    private static class EffectField extends ConfigField<Effect> {

        private final EffectTextField editor;

        public EffectField(Config<Effect> c, Class<? extends Effect> effectType) {
            super(c);
            editor = new EffectTextField(effectType);
            refreshItem();
            editor.setOnValueChange((ov, nv) -> apply(false));
        }

        @Override public Control getControl() {
            return editor;
        }
        @Override public Try<Effect,String> get() {
            return ok(editor.getValue());
        }
        @Override public void refreshItem() {
            editor.setValue(config.getValue());
        }
        @Override protected void apply(boolean user) {
            getValid().ifOk(v -> {
                config.setValue(v);
                refreshItem();
                if (onChange != null) onChange.run();
            });
        }
    }
    private static class ConfigurableField extends ConfigField<Configurable<?>> {
        private final ConfigPane<Object> configPane = new ConfigPane<>();

        private ConfigurableField(Config<Configurable<?>> c) {
            super(c);
            configPane.configure(c.getValue().getFields());
        }

        @Override
        public Node getControl() {
            return configPane;
        }

        @Override
        public Try<Configurable<?>,String> get() {
            return ok(config.getValue());
        }

        @Override
        public void refreshItem() {}
    }
    private static class ListField<T> extends ConfigField<ObservableList<T>> {

        private final ListConfig<T> lc;
        private final ListConfigField<T,ConfigurableField> chain;
        private final Runnable changeHandler;

        @SuppressWarnings("unchecked")
        public ListField(Config<ObservableList<T>> c) {
            super(c);
            lc = (ListConfig) c;
            Predicate<T> p = c.getConstraints().stream().anyMatch(HasNonNullElements.class::isInstance)
                    ? Objects::nonNull
                    : (Predicate) IS;

            // create chain
            chain = new ListConfigField<>(0, () -> new ConfigurableField(lc.a.itemType, lc.a.factory.get()));
            changeHandler = () -> lc.a.list.setAll(chain.getValues().filter(p).collect(toList()));

            // initialize chain - add existing list values to chain
            lc.a.list.forEach(v -> chain.addChained(new ConfigurableField(lc.a.itemType, v)));
            chain.growTo1();

            // bind list to the chain values (after it was initialized above)
            chain.onItemChange = it -> changeHandler.run();
        }

        @Override
        Node getControl() {
            return chain.getNode();
        }

        @Override
        protected Try<ObservableList<T>,String> get() {
            return ok(config.getValue()); // return the ever-same observable list
        }

        @Override
        public void refreshItem() {}

        class ConfigurableField extends ValueNode<T> {
            private final Class<T> type;
            private final ConfigPane<T> pane = new ConfigPane<>();

            public ConfigurableField(Class<T> type, T value) {
                super(value);
                this.type = type;
                pane.setOnChange(changeHandler);
                pane.configure(lc.toConfigurable.apply(this.value));
            }

            @Override
            public Node getNode() {
                return pane;
            }

            @Override
            public T getVal() {
                // TODO: use Type instead of Class for Config.type or add list type support to Config
                Class<? extends T> oType = pane.getConfigFields().get(0).config.getType();
                T o = pane.getConfigFields().get(0).getVal();
                if (type==oType) return o;
                else return value;
            }

        }
    }
    private static class ListFieldPaginated extends ConfigField<ObservableList<Configurable<?>>> {

        private int at = -1;
        private final ListConfig<Configurable<?>> lc;
        Icon prevB = new Icon(FontAwesomeIcon.ANGLE_LEFT, 16, "Previous item", this::prev);
        Icon nextB = new Icon(FontAwesomeIcon.ANGLE_RIGHT, 16, "Next item", this::next);
        ConfigPane<Object> configPane = new ConfigPane<>();
        Node graphics = layHeaderTop(10, Pos.CENTER_RIGHT,
            layHorizontally(5, Pos.CENTER_RIGHT, prevB,nextB),
            configPane
        );

        public ListFieldPaginated(Config<ObservableList<Configurable<?>>> c) {
            super(c);
            lc = (ListConfig<Configurable<?>>) c;
            next();
        }

        private void prev() {
            int size = lc.a.list.size();
            if (size<=0) at=-1;
            if (size<=0) return;

            at = at==-1 || at==0 ? size-1 : at-1;
            configPane.configure(lc.a.list.get(at));
        }

        private void next() {
            int size = lc.a.list.size();
            if (size<=0) at=-1;
            if (size<=0) return;

            at = at==-1 || at==size-1 ? 0 : at+1;
            configPane.configure(lc.a.list.get(at));
        }

        @Override
        Node getControl() {
            return graphics;
        }

        @Override
        protected Try<ObservableList<Configurable<?>>,String> get() {
            return ok(config.getValue()); // return the ever-same observable list
        }

        @Override
        public void refreshItem() {}
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
            maintain(vo.override, it -> cf.getControl().setDisable(!it));
            root.getChildren().addAll(cf.getNode(),bf.getNode());
        }
        @Override
        Node getControl() {
            return root;
        }

        @Override
        protected Try<T,String> get() {
            return ok(config.getValue());
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

/* ---------- HELPER --------------------------------------------------------------------------------------- */

    static void disableIfReadOnly(ConfigField<?> control, Config<?> config) {
    	if (!config.isEditable().isByUser()) {
    		control.getControl().setDisable(true);
	    } else {
	        config.getConstraints().stream()
	            .filter(Constraint.ReadOnlyIf.class::isInstance)
	            .map(Constraint.ReadOnlyIf.class::cast)
		        .map(ReadOnlyIf::getCondition)
                .reduce(Bindings::and)
                .ifPresent(control.getControl().disableProperty()::bind);
	    }
    }
}

