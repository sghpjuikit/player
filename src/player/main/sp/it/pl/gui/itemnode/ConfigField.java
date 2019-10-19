package sp.it.pl.gui.itemnode;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.gui.itemnode.textfield.EffectTextField;
import sp.it.pl.gui.itemnode.textfield.FontTextField;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.textfield.DecoratedTextField;
import sp.it.pl.gui.pane.ConfigPane;
import sp.it.util.access.OrV;
import sp.it.util.access.ref.LazyR;
import sp.it.util.action.Action;
import sp.it.util.animation.Anim;
import sp.it.util.conf.Config;
import sp.it.util.conf.ConfigImpl.ListConfig;
import sp.it.util.conf.ConfigImpl.PropertyConfig;
import sp.it.util.conf.ConfigImpl.ReadOnlyPropertyConfig;
import sp.it.util.conf.Configurable;
import sp.it.util.conf.Constraint.NumberMinMax;
import sp.it.util.conf.OrPropertyConfig;
import sp.it.util.conf.OrPropertyConfig.OrValue;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Try;
import sp.it.util.functional.TryKt;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.access.PropertiesKt.not;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.TryKt.getAny;
import static sp.it.util.functional.TryKt.getOr;
import static sp.it.util.functional.Util.equalsAny;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.sync;
import static sp.it.util.reactive.UtilKt.syncBiFrom;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.reactive.UtilKt.syncFrom;
import static sp.it.util.ui.Util.layHeaderTop;
import static sp.it.util.ui.Util.layHorizontally;

/**
 * Editable and settable graphic control for configuring {@link sp.it.util.conf.Config}.
 * <p/>
 * Convenient way to create wide and diverse property sheets, that take
 * type of configuration into consideration. For example
 * for boolean CheckBox control will be used, for enum ComboBox etc...
 */
abstract public class ConfigField<T> {

    private static final Tooltip okTooltip = appTooltip("Apply value");
    private static final Tooltip warnTooltip = appTooltip("Erroneous value");
    private static final Tooltip defTooltip = appTooltip("Default value");
    private static final Tooltip actTooltip = appTooltip("Run action");
    private static final Tooltip globTooltip = appTooltip("Global shortcut"
            + "\n\nGlobal shortcuts can be used even when the application has no focus or window."
            + "\n\nOnly one application can use this shortcut. If multiple applications use "
            + "the same shortcut, usually the one that was first started will work properly.");
    private static final Tooltip overTooltip = appTooltip("Override value"
            + "\n\nUses specified value if true or inherited value if false.");

    public static final String STYLECLASS_CONFIG_FIELD_PROCEED_BUTTON = "config-field-proceed-button";
    public static final String STYLECLASS_CONFIG_FIELD_OK_BUTTON = "config-field-ok-button";
    public static final String STYLECLASS_CONFIG_FIELD_WARN_BUTTON = "config-field-warn-button";
    public static final String STYLECLASS_TEXT_CONFIG_FIELD = "text-field-config";
    private static final double defBLayoutSize = 15.0;
    private static final double configRootSpacing = 5.0;
    private static Insets paddingNoDefB = new Insets(0.0, defBLayoutSize+configRootSpacing, 0.0, 0.0);
    private static Insets paddingWithDefB = Insets.EMPTY;

    @SuppressWarnings("unchecked")
    private static Map<Class<?>,F1<Config,ConfigField>> CF_BUILDERS = new HashMap<>() {{
        put(boolean.class, BoolCF::new);
        put(Boolean.class, BoolCF::new);
        put(String.class, GeneralCF::new);
        put(Action.class, ShortcutCF::new);
        put(Color.class, ColorCF::new);
        put(File.class, FileCF::new);
        put(Font.class, FontCF::new);
        put(OrValue.class, config -> config instanceof OrPropertyConfig ? new OrCF((OrPropertyConfig) config) : new GeneralCF(config));
        put(Effect.class, config -> new EffectCF(config, Effect.class));
        put(Charset.class, charset -> new EnumerableCF<>(charset, list(ISO_8859_1, US_ASCII, UTF_8, UTF_16, UTF_16BE, UTF_16LE)));
        put(KeyCode.class, KeyCodeCF::new);
        put(Configurable.class, ConfigurableCF::new);
        put(ObservableList.class, config -> {
            if (config instanceof ListConfig) {
                return Configurable.class.isAssignableFrom(((ListConfig)config).a.itemType)
                    ? new PaginatedObservableListCF(config)
                    : new ObservableListCF<>(config);
            } else {
                return new GeneralCF<>(config);
            }
        });
        EffectTextField.EFFECT_TYPES.stream().map(et -> et.getType()).filter(t -> t!=null).forEach(t -> put(t, config -> new EffectCF(config, t)));
    }};

    /** @return config field best suited for the specified config */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull ConfigField<T> create(Config<T> config) {
        Config c = config;
        ConfigField cf;
        if (c.isTypeEnumerable()) cf = c.getType()==KeyCode.class ? new KeyCodeCF(c) : new EnumerableCF(c);
        else if (isMinMax(c)) cf = new SliderCF(c);
        else cf = CF_BUILDERS.computeIfAbsent(c.getType(), key -> GeneralCF::new).apply(c);

        syncFrom(cf.getEditor().disableProperty(), not(cf.isEditableByUser));

        return cf;
    }

    public static boolean isMinMax(Config<?> c) {
        return Number.class.isAssignableFrom(c.getType()) && c.getConstraints().stream().anyMatch(l -> l.getClass()==NumberMinMax.class);
    }

    public static <T> ConfigField<T> createForProperty(Class<T> type, String name, Object property) {
        return create(Config.forProperty(type, name, property));
    }

    @SuppressWarnings("unchecked")
    protected static <T> ObservableValue<T> getObservableValue(Config<T> c) {
        return c instanceof PropertyConfig && ((PropertyConfig)c).getProperty() instanceof ObservableValue
                   ? (ObservableValue)((PropertyConfig)c).getProperty()
                   : c instanceof ReadOnlyPropertyConfig
                         ? ((ReadOnlyPropertyConfig)c).getProperty()
                         : null;
    }

/* ------------------------------------------------------------------------------------------------------------------ */
    public final Config<T> config;
    public boolean applyOnChange = true;
    protected boolean inconsistentState = false;
    public Try<T,String> value = ok(null);
    public Consumer<? super Try<T,String>> observer;
    private Icon defB;
    private Anim defBA;
    public final ObservableValue<Boolean> isEditableByUser;

    protected ConfigField(Config<T> config) {
        this.config = config;
        this.isEditableByUser = config.isEditableByUserRightNowProperty();
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

    public T getConfigValue() {
        return config.getValue();
    }

    /**
     * Creates label with config field name and tooltip with config field description.
     *
     * @return label describing this field
     */
    public Label buildLabel() {
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

    public final HBox buildNode() {
        return buildNode(true);
    }

    /**
     * Use to get the control node for setting and displaying the value to
     * attach it to a scene graph.
     *
     * @param managedControl default true, set false to avoid controls affecting size measurement (particularly helpful
     * with text fields, which can 'expand' layout beyond expected width due to
     * {@link javafx.scene.control.TextField#prefColumnCountProperty()}. I.e., use true to expand and false to shrink.
     *
     * @return setter control for this field
     */
    public final HBox buildNode(boolean managedControl) {
        HBox root = new HBox(configRootSpacing);
        root.getStyleClass().add("config-field");
        root.setMinSize(0,20);   // min height actually required to get consistent look
        root.setPrefSize(-1,-1); // support variable content height
        root.setMaxSize(-1,-1);  // support variable content height
        root.setAlignment(CENTER_LEFT);

        root.addEventFilter(MOUSE_ENTERED, e -> {
            if (!isEditableByUser.getValue()) return;
            runFX(millis(270), () -> {
                if (root.isHover()) {
                    if (defB==null && isEditableByUser.getValue()) {
                        defB = new Icon(null, -1, null, this::setNapplyDefault);
                        defB.tooltip(defTooltip);
                        defB.styleclass("config-field-default-button");
                        defB.setManaged(false);
                        defB.setOpacity(0);

                        var defBRoot = new StackPane(defB) {
                            @Override
                            protected void layoutChildren() {
                                defB.relocate(
                                    getWidth()/2d-defB.getLayoutBounds().getWidth()/2,
                                    getHeight()/2d-defB.getLayoutBounds().getHeight()/2
                                );
                            }
                        };
                        defBRoot.setPrefSize(defBLayoutSize, defBLayoutSize);
                        root.getChildren().add(defBRoot);
                        root.setPadding(paddingWithDefB);

                        defBA = Anim.anim(millis(450), consumer(it -> {
                            if (defB!=null)
                                defB.setOpacity(it*it);
                        }));
                    }
                    if (defBA!=null)
                        defBA.playOpenDo(null);
                }
            });
        });
        root.addEventFilter(MOUSE_EXITED, e -> {
            if (defBA!=null)
                defBA.playCloseDo(runnable(() -> {
                    root.getChildren().remove(defB.getParent());
                    defB = null;
                    defBA = null;
                    root.setPadding(paddingNoDefB);
                }));
        });

        var isHardToAutoResize = getEditor() instanceof TextField;
        var config = !isHardToAutoResize
            ? getEditor()
            : new StackPane(getEditor()) {
                    {
                        getEditor().setManaged(managedControl);
                    }
                    @Override
                    protected void layoutChildren() {
                        getChildren().get(0).resizeRelocate(0, 0, getWidth(), getHeight());
                    }
                };
        root.getChildren().add(0, config);
        root.setPadding(paddingNoDefB);
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
    public abstract Node getEditor();

    public void focusEditor() {
        getEditor().requestFocus();
    }

    protected String getTooltipText() {
        return config.getInfo();
    }

    protected abstract Try<T,String> get();

    @SuppressWarnings("unchecked")
    public Try<T,String> getValid() {
        value = TryKt.and(get(), v -> config.getConstraints().stream().map(c -> c.validate(v)).reduce(ok(),TryKt::and));
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
        if (isEditableByUser.getValue()) {
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
        getValid().ifOkUse(v -> {
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

    private static class GeneralCF<T> extends ConfigField<T> {
        private final DecoratedTextField editor = new DecoratedTextField();
        private final boolean isObservable;
        private final StackPane okB = new StackPane();
        private final LazyR<Icon> okI = new LazyR<>(() -> {
            var i = new Icon();
            i.styleclass(STYLECLASS_CONFIG_FIELD_OK_BUTTON);
            i.tooltip(okTooltip);
            i.onClick(() -> apply(true));
            okB.getChildren().setAll(i);
            return i;
        });
        private final LazyR<Icon> warnI = new LazyR<>(() -> {
            var i = new Icon();
            i.styleclass(STYLECLASS_CONFIG_FIELD_WARN_BUTTON);
            i.tooltip(warnTooltip);
            return i;
        });

        private GeneralCF(Config<T> c) {
            super(c);

            ObservableValue<T> obv = getObservableValue(c);
            isObservable = obv!=null;
            if (isObservable) {
                obv.addListener((o,ov,nv) -> refreshItem());
            }

            if (Observable.class.isAssignableFrom(c.getType())) {
                var v = (Observable) c.getValue();
                if (v!=null) v.addListener(it -> refreshItem());
            }

            okB.setPrefSize(11, 11);
            okB.setMinSize(11, 11);
            okB.setMaxSize(11, 11);

            editor.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            editor.setPromptText(c.getGuiName());
            editor.setText(toS(getConfigValue()));

            // refreshing value
            editor.focusedProperty().addListener((o, ov, nv) -> {
                if (!nv)
                    refreshItem();
            });
            editor.addEventHandler(KEY_RELEASED, e -> {
                if (e.getCode()==ESCAPE) {
                    refreshItem();
                    e.consume();
                }
            });

            // applying value
            editor.textProperty().addListener((o, ov, nv)-> {
                Try<T,String> t = getValid();
                boolean applicable = getOr(t.map(v -> !Objects.equals(config.getValue(), v)), false);
                showOkButton(!applyOnChange && applicable && t.isOk());
                showWarnButton(t);
                if (applyOnChange) apply(false);
            });
            editor.setOnKeyPressed(e -> {
                if (e.getCode()==ENTER) {
                    apply(true);
                    e.consume();
                }
            });

            syncC(isEditableByUser, it -> showWarnButton(getValid()));
        }

        @Override
        public Control getEditor() {
            return editor;
        }

        @Override
        public void focusEditor() {
            editor.requestFocus();
            editor.selectAll();
        }

        @Override
        public Try<T,String> get() {
            return Config.convertValueFromString(config, editor.getText());
        }

        @Override
        public void refreshItem() {
            editor.setText(toS(getConfigValue()));
            showOkButton(false);
            showWarnButton(ok());
        }

        @Override
        protected void apply(boolean user) {
            if (inconsistentState) return;
            getValid().ifOkUse(v -> {
                boolean applicable = !Objects.equals(config.getValue(), v);
                if (!applicable) return;

                inconsistentState = true;
                config.setValue(v);
                if (onChange!=null) onChange.run();
                inconsistentState = false;
            });
        }

        private void showOkButton(boolean val) {
            editor.left.setValue(val ? okI.get() : null);
            okI.ifSet(it ->  it.setVisible(false));
        }

        private void showWarnButton(Try<?,String> value) {
            var shouldBeVisible = value.isError() && isEditableByUser.getValue();
            editor.right.setValue(shouldBeVisible ? warnI.get() : null);
            warnI.ifSet(it -> it.setVisible(shouldBeVisible));
            warnTooltip.setText(getAny(value.map(v -> "")));
        }

        private String toS(Object o) {
            if (o instanceof Collection<?>) {
                return ((Collection<?>) o).stream().map(x -> toS(x)).collect(joining(", ", "[", "]"));
            } else if (o instanceof Map<?,?>) {
                return ((Map<?,?>) o).entrySet().stream()
                    .map(x -> toS(x.getKey()) + " -> " + toS(x.getValue()))
                    .collect(joining(", ", "[", "]"));
            } else {
                return config.isEditable().isByUser()
                    ? APP.converter.ui.toS(o)
                    : APP.converter.general.toS(o);
            }
        }

    }
    private static class BoolCF extends ConfigField<Boolean> {
        protected final CheckIcon graphics;
        private final boolean isObservable;

        private BoolCF(Config<Boolean> c) {
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
        public CheckIcon getEditor() {
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
    private static class OrBoolCF extends BoolCF {
        private OrBoolCF(Config<Boolean> c) {
            super(c);
            graphics.styleclass("override-config-field");
            graphics.tooltip(overTooltip);
        }
    }
    private static class SliderCF extends ConfigField<Number> {
        private final Slider slider;
        private final Label cur, min, max;
        private final HBox box;
        private final boolean isObservable;

        private SliderCF(Config<Number> c) {
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
            slider.getStyleClass().add("slider-config-field");
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
        public Node getEditor() {
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
            return getOr(getValid().map(Object::toString), "");
        }
    }
    private static class ShortcutCF extends ConfigField<Action> {
        TextField editor;
        CheckIcon globB;
        HBox group;
        String t="";
        Action a;
        Icon runB;

        private ShortcutCF(Config<Action> con) {
            super(con);
            a = con.getValue();

            globB = new CheckIcon();
            globB.styleclass("config-shortcut-global-icon");
            globB.selected.setValue(a.isGlobal());
            globB.tooltip(globTooltip);
            globB.selected.addListener((o,ov,nv) -> apply(false));

            editor = new TextField();
            editor.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            editor.getStyleClass().add("shortcut-config-field");
            editor.setPromptText(computePromptText());
            editor.setOnKeyReleased(e -> {
                KeyCode c = e.getCode();
                // handle subtraction
                if (c==TAB) {
                } else if (c==BACK_SPACE || c==DELETE) {
                    editor.setPromptText("<none>");
                    if (!editor.getText().isEmpty()) editor.setPromptText(computePromptText());

                    if (t.isEmpty()) {  // set back to empty
                        editor.setPromptText(computePromptText());
                    } else {            // subtract one key
                        if (t.indexOf('+') == -1) t="";
                        else t=t.substring(0,t.lastIndexOf('+'));
                        editor.setText(t);
                    }
                } else if (c==ENTER) {
                    apply(true);
                } else if (c==ESCAPE) {
                    refreshItem();
                // handle addition
                } else {
                    t += t.isEmpty() ? c.getName() : "+" + c.getName();
                    editor.setText(t);
                }
                e.consume();
            });
            editor.setEditable(false);
            editor.setTooltip(appTooltip(a.getInfo()));
            editor.focusedProperty().addListener((o, ov, nv) -> editor.setText(a.getKeys()));

            runB = new Icon();
            runB.styleclass("config-shortcut-run-icon");
            runB.onClick(a);
            runB.tooltip(actTooltip);

            group = new HBox(5, runB, globB, editor);
            group.setAlignment(CENTER_LEFT);
            group.setPadding(Insets.EMPTY);
        }

        private String computePromptText() {
            String keys = a.getKeys();
            return keys.isEmpty() ? "<none>" : keys;
        }

        @Override
        public Node getEditor() {
            return group;
        }

        @Override
        public boolean hasUnappliedValue() {
            Action a = config.getValue();
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = editor.getText().equals(a.getKeys()) ||
                    (editor.getText().isEmpty() && editor.getPromptText().equals(a.getKeys()));
            return !sameKeys || !sameglobal;
        }

        @Override
        protected void apply(boolean b) {
            // its pointless to make new Action just for this
            // config.applyValue(get());
            // rather operate on the Action manually

            Action a = config.getValue();
            boolean sameglobal = globB.selected.getValue()==a.isGlobal();
            boolean sameKeys = editor.getText().equals(a.getKeys()) ||
                    (editor.getText().isEmpty() && editor.getPromptText().equals(a.getKeys()));

            if (!sameglobal && !sameKeys)
                a.set(globB.selected.getValue(), editor.getText());
            else if (!sameKeys)
                a.setKeys(editor.getText());
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
            editor.setPromptText(a.getKeys());
            editor.setText("");
            globB.selected.setValue(a.isGlobal());
        }
    }
    private static class ColorCF extends ConfigField<Color> {
        private ColorPicker editor = new ColorPicker();

        private ColorCF(Config<Color> c) {
            super(c);
            refreshItem();
            editor.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            editor.valueProperty().addListener((o,ov,nv) -> apply(false));
        }

        @Override public Control getEditor() {
            return editor;
        }
        @Override public Try<Color,String> get() {
            return ok(editor.getValue());
        }
        @Override public void refreshItem() {
            editor.setValue(config.getValue());
        }
    }
    private static class FontCF extends ConfigField<Font> {
        private FontTextField editor = new FontTextField();

        private FontCF(Config<Font> c) {
            super(c);
            refreshItem();
            editor.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            editor.setOnValueChange((ov, nv) -> apply(false));
        }

        @Override public Control getEditor() {
            return editor;
        }
        @Override public Try<Font,String> get() {
            return ok(editor.getValue());
        }
        @Override public void refreshItem() {
            editor.setValue(config.getValue());
        }
    }
    private static class EffectCF extends ConfigField<Effect> {

        private final EffectTextField editor;

        public EffectCF(Config<Effect> c, Class<? extends Effect> effectType) {
            super(c);
            editor = new EffectTextField(effectType);
            editor.getStyleClass().add(STYLECLASS_TEXT_CONFIG_FIELD);
            refreshItem();
            editor.setOnValueChange((ov, nv) -> apply(false));
        }

        @Override public Control getEditor() {
            return editor;
        }
        @Override public Try<Effect,String> get() {
            return ok(editor.getValue());
        }
        @Override public void refreshItem() {
            editor.setValue(config.getValue());
        }
        @Override protected void apply(boolean user) {
            getValid().ifOkUse(v -> {
                config.setValue(v);
                refreshItem();
                if (onChange != null) onChange.run();
            });
        }
    }
    private static class ConfigurableCF extends ConfigField<Configurable<?>> {
        private final ConfigPane<Object> configPane = new ConfigPane<>();

        private ConfigurableCF(Config<Configurable<?>> c) {
            super(c);
            configPane.configure(c.getValue());
        }

        @Override
        public Node getEditor() {
            return configPane;
        }

        @Override
        public Try<Configurable<?>,String> get() {
            return ok(config.getValue());
        }

        @Override
        public void refreshItem() {}
    }

    private static class PaginatedObservableListCF extends ConfigField<ObservableList<Configurable<?>>> {

        private int at = -1;
        private final ListConfig<Configurable<?>> lc;
        Icon prevB = new Icon(FontAwesomeIcon.ANGLE_LEFT, 16, "Previous item", this::prev);
        Icon nextB = new Icon(FontAwesomeIcon.ANGLE_RIGHT, 16, "Next item", this::next);
        ConfigPane<Object> configPane = new ConfigPane<>();
        Node graphics = layHeaderTop(10, Pos.CENTER_RIGHT,
            layHorizontally(5, Pos.CENTER_RIGHT, prevB,nextB),
            configPane
        );

        public PaginatedObservableListCF(Config<ObservableList<Configurable<?>>> c) {
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
        public Node getEditor() {
            return graphics;
        }

        @Override
        protected Try<ObservableList<Configurable<?>>,String> get() {
            return ok(config.getValue()); // return the ever-same observable list
        }

        @Override
        public void refreshItem() {}
    }
    private static class OrCF<T> extends ConfigField<OrValue<T>> {
        final FlowPane root = new FlowPane(5,5);

        public OrCF(OrPropertyConfig<T> c) {
            super(c);
            OrV<T> orV = c.getProperty();

            var op = new SimpleBooleanProperty(c.getDefaultValue().getOverride());
            var oc = new OrBoolCF(Config.forProperty(Boolean.class, "Override", op));
            syncBiFrom(orV.override, op);

            var vp = new SimpleObjectProperty<>(c.getDefaultValue().getValue());
            var vc = create(Config.forProperty(c.getValueType(), "", vp));
            syncBiFrom(orV.real, vp);

            syncC(orV.override, it -> vc.getEditor().setDisable(!it));
            root.getChildren().addAll(vc.buildNode(), oc.buildNode());
        }

        @Override
        public Node getEditor() {
            return root;
        }

        @Override
        protected Try<OrValue<T>,String> get() {
            return ok(config.getValue());
        }

        @Override
        public void refreshItem() {}

        @Override
        public void setNapplyDefault() {
            config.setValueToDefault();
        }

        @Override
        protected String getTooltipText() {
            return config.getInfo() + "\n\nThis value must override global value to take effect.";
        }
    }

}