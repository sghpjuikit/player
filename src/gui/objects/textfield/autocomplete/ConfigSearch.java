package gui.objects.textfield.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import gui.itemnode.ConfigField;
import gui.objects.textfield.autocomplete.ConfigSearch.Entry;
import gui.objects.window.stage.UiContext;
import layout.widget.WidgetFactory;
import util.action.Action;
import util.conf.Config;
import util.functional.Functors.Ƒ0;

import static main.App.Build.appTooltip;
import static util.Util.containsNoCase;
import static util.functional.Util.by;
import static util.functional.Util.stream;
import static util.graphics.Util.layStack;
import static util.graphics.Util.setMinPrefMaxSize;

/**
 *
 *
 * @author Martin Polakovic
 */
public class ConfigSearch extends AutoCompletion<Entry> {

    private final TextField textField;
    private final History history;
    private boolean ignoreEvent = false;

	@SafeVarargs
    public ConfigSearch(TextField textField, Supplier<Stream<Entry>>...searchTargets) {
        this(textField, new History(), searchTargets);
    }

	@SafeVarargs
    public ConfigSearch(TextField textField, History history, Supplier<Stream<Entry>>... searchTargets) {
        super(
        	textField,
	        s -> {
        		String text = s.getUserText();
		        String[] phrases = text.split(" ");
        		return stream(searchTargets).flatMap(Supplier::get)
			               .filter(f -> stream(phrases).allMatch(phrase -> containsNoCase(f.getSearchText(), phrase)))
			               .sorted(by(Entry::getName))
			               .toList();
	        },
	        AutoCompletion.defaultStringConverter()
        );
        this.history = history;
        this.textField = textField;
        this.textField.setPrefWidth(450); // dictates the popup width
    }

    @Override
    protected AutoCompletePopup<Entry> buildPopup() {
        return new AutoCompletePopup<Entry>(){
            @Override
            protected Skin<AutoCompletePopup<Entry>> createDefaultSkin() {
                return new AutoCompletePopupSkin<>(this, 2) {
                    {
                        // set keys & allow typing
                        getSkinnable().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                            if (!ignoreEvent)
                                if (e.isControlDown() && (e.getCode()==KeyCode.UP || e.getCode()==KeyCode.DOWN)) {
                                    switch (e.getCode()) {
                                        case UP   : history.up(ConfigSearch.this); break;
                                        case DOWN : history.down(ConfigSearch.this); break;
                                        default   : break;
                                    }
                                } else if (e.getCode()==KeyCode.BACK_SPACE) {
	                                textField.deletePreviousChar();
	                                e.consume();
                                } else if (e.getCode()==KeyCode.DELETE) {
	                                textField.deleteNextChar();
	                                e.consume();
                                } else if (!e.getCode().isNavigationKey()) {
                                	// TODO: remove
                                    // We re-fire event on text field so we can type even though it
                                    // does not have focus. This causes event stack overflow, so we
                                    // defend against it with a boolean flag.
                                	if (!textField.isFocused()) {
	                                    ignoreEvent = true;
	                                    completionTarget.fireEvent(e);
	                                }
                                }
                            ignoreEvent = false;
                            // e.consume(); // may brake functionality
                        });
                        getNode().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                            if (!ignoreEvent)
                                if (e.isControlDown() && (e.getCode()==KeyCode.UP || e.getCode()==KeyCode.DOWN)) {
                                    switch (e.getCode()) {
                                        case UP   : history.up(ConfigSearch.this); break;
                                        case DOWN : history.down(ConfigSearch.this); break;
                                        default   : break;
                                    }
                                    e.consume();
                                } else if (e.isControlDown() && e.getCode()==KeyCode.A) {
                                    textField.selectAll();
                                    e.consume();
                                // } else if (e.getCode()==KeyCode.BACK_SPACE) {
                                ///    textField.deletePreviousChar(); // doesn't work here
                                //     e.consume();
                                } else if (e.getCode()==KeyCode.END) {
                                    if (e.isShiftDown()) textField.selectEnd();
                                    else textField.positionCaret(textField.getLength());
                                    e.consume();
                                } else if (e.getCode()==KeyCode.HOME) {
                                    if (e.isShiftDown()) textField.selectHome();
                                    else textField.positionCaret(0);
                                    e.consume();
                                } else if (e.getCode()==KeyCode.LEFT) {
                                    if (e.isControlDown()) textField.selectPreviousWord(); else textField.selectBackward();
                                    if (!e.isShiftDown()) textField.deselect();
                                    e.consume();
                                } else if (e.getCode()==KeyCode.RIGHT) {
                                    if (e.isControlDown()) textField.selectNextWord(); else textField.selectForward();
                                    if (!e.isShiftDown()) textField.deselect();
                                    e.consume();
                                } // else if (!e.getCode().isNavigationKey()) {}
                            ignoreEvent = false;
                            // e.consume(); // may brake functionality
                        });
                    }
                    @Override
                    protected ListCell<Entry> buildListViewCellFactory(ListView<Entry> view) {
                        return new EntryListCell();
                    }
                };
            }
        };
    }

    @Override
    protected void acceptSuggestion(Entry suggestion) {
		suggestion.run();
        history.add(this);
    }

    public static class History {
        private int history_at = 0;
        private final List<String> history = new ArrayList<>();

        private void up(ConfigSearch search) {
            if (history.isEmpty()) return;
            history_at = history_at==0 ? history.size()-1 : history_at-1;
            ((TextField)search.completionTarget).setText(history.get(history_at));
        }

        private void down(ConfigSearch search) {
            if (history.isEmpty()) return;
            history_at = history_at==history.size()-1 ? 0 : history_at+1;
            ((TextField)search.completionTarget).setText(history.get(history_at));
        }

        private void add(ConfigSearch search) {
            String last = history.isEmpty() ? null : history.get(history.size()-1);
            String curr = ((TextField)search.completionTarget).getText();
            boolean isDiff = last!=null && curr!=null && last.equalsIgnoreCase(curr);
            if (isDiff) {
                history.add(curr);
                history_at = history.size()-1;
            }
        }
    }
    public interface Entry extends Runnable {

	    static Entry of(Ƒ0<String> nameΛ, Runnable runΛ) {
		    return new ΛEntry(nameΛ, nameΛ, () -> null, runΛ);
	    }

	    static Entry of(Ƒ0<String> nameΛ, Ƒ0<String> infoΛ, Runnable runΛ) {
		    return new ΛEntry(nameΛ, infoΛ, () -> null, runΛ);
	    }

	    static Entry of(Ƒ0<String> nameΛ, Runnable runΛ, Ƒ0<Node> graphicsΛ) {
		    return new ΛEntry(nameΛ, nameΛ, graphicsΛ, runΛ);
	    }

	    static Entry of(Ƒ0<String> nameΛ, Ƒ0<String> infoΛ, Runnable runΛ, Ƒ0<Node> graphicsΛ) {
		    return new ΛEntry(nameΛ, infoΛ, graphicsΛ, runΛ);
	    }

    	static Entry of(Config<?> config) {
    	    return new ConfigEntry(config);
	    }

    	static Entry of(WidgetFactory f) {
    	    return new SimpleEntry(
                "Open widget " + f.nameGui(),
				"Open widget " + f.nameGui() + "\n\n" + "Opens the widget in new window.",
                () -> UiContext.launchComponent(f.name())
	        );
	    }

	    String getName();
	    default String getSearchText() {
	        return getName();
	    }
	    default String getInfo() {
		    return getName();
	    }
	    default Node getGraphics() {
	    	return null;
	    }

	    class ΛEntry implements Entry {
		    private final Ƒ0<String> nameΛ;
		    private final Ƒ0<String> infoΛ;
		    private final Ƒ0<Node> graphicsΛ;
		    private final Runnable runΛ;

		    public ΛEntry(Ƒ0<String> nameΛ, Ƒ0<String> infoΛ, Ƒ0<Node> graphicsΛ, Runnable runΛ) {
			    this.nameΛ = nameΛ;
			    this.infoΛ = infoΛ;
			    this.graphicsΛ = graphicsΛ;
			    this.runΛ = runΛ;
		    }

		    @Override
		    public String getName() {
			    return nameΛ.get();
		    }

		    @Override
		    public void run() {
			    runΛ.run();
		    }

		    @Override
		    public String getInfo() {
			    return infoΛ.get();
		    }

		    @Override
		    public Node getGraphics() {
			    return graphicsΛ.get();
		    }
	    }
	    class SimpleEntry implements Entry {
		    private final String name;
		    private final String info;
		    private final Runnable action;

		    SimpleEntry(String name, String info, Runnable action) {
			    this.name = name;
			    this.info = info;
			    this.action = action;
		    }

		    @Override
		    public String getName() {
			    return name;
		    }

		    @Override
		    public void run() {
			    if (action!=null) action.run();
		    }
	    }
	    class ConfigEntry implements Entry {
		    public final Config<?> config;
		    private boolean loaded = false;
		    private Node graphics;

		    ConfigEntry(Config<?> config) {
			    this.config = config;
		    }

		    @Override
		    public String getName() {
			    return config.getGroup() + "." + config.getGuiName();
		    }

		    @Override
		    public String getSearchText() {
			    return Action.class.isAssignableFrom(config.getType())
				           ? getName() + Action.class.cast(config).getKeys()
				           : getName();
		    }

		    @Override
		    public String getInfo() {
			    return getName() + "\n\n" + config.getInfo();
		    }

		    @Override
		    public Node getGraphics() {
			    if (loaded) return graphics;

			    if (config.getType()==Action.class && ((Action)config).hasKeysAssigned()) {
				    graphics = new Label(((Action)config).getKeys());
				    ((Label)graphics).setTextAlignment(TextAlignment.RIGHT);
			    } else {
				    graphics = config.getType()==Boolean.class || config.isTypeEnumerable()
					               ? ConfigField.create(config).getNode()
					               : null;
			    }

			    loaded = true;
			    return graphics;
		    }

		    @Override
		    public void run() {
			    if (Runnable.class.isAssignableFrom(config.getClass()))
				    ((Runnable)config).run();
			    else
			    if (Runnable.class.isAssignableFrom(config.getType()) && config.getValue() != null)
				    ((Runnable)config.getValue()).run();
		    }
	    }
    }
    private static class EntryListCell extends ListCell<Entry> {
        private final Label text = new Label();
        private final StackPane configNodeRoot = new StackPane();
        private final StackPane root = layStack(text,Pos.CENTER_LEFT, configNodeRoot,Pos.CENTER_RIGHT);
        private final Tooltip tooltip = appTooltip();

        public EntryListCell() {
            text.setTextAlignment(TextAlignment.LEFT);
            text.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            setMinPrefMaxSize(text,USE_COMPUTED_SIZE);
            text.prefWidthProperty().bind(root.widthProperty().subtract(configNodeRoot.widthProperty()).subtract(10));
            text.setMinWidth(200);
            text.maxWidthProperty().bind(root.widthProperty().subtract(100));
            text.setPadding(new Insets(5,0,0,10));
            Tooltip.install(root, tooltip);
        }

        @Override
        protected void updateItem(Entry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                text.setText("");
                setGraphic(null);
            } else {
                if (getGraphic()!=root) setGraphic(root);

                tooltip.setText(item.getInfo());

                text.setText(item.getName());
	            Node node = item.getGraphics();
	            if (node instanceof HBox) ((HBox)node).setAlignment(Pos.CENTER_RIGHT);
	            if (node==null) configNodeRoot.getChildren().clear();
	            else {
	            	configNodeRoot.getChildren().setAll(node);
		            StackPane.setAlignment(node, Pos.CENTER_RIGHT);
	            }
            }
        }
    }
}