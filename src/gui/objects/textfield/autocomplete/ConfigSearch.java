package gui.objects.textfield.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import gui.itemnode.ConfigField;
import gui.objects.textfield.autocomplete.ConfigSearch.Entry;
import util.action.Action;
import util.conf.Config;

import static util.functional.Util.map;
import static util.graphics.Util.layStack;
import static util.graphics.Util.setMinPrefMaxSize;
import static util.graphics.Util.setMinPrefMaxWidth;

/**
 * Created by Plutonium_ on 3/6/2016.
 */
public class ConfigSearch extends AutoCompletion<Entry> {

    private final TextField textField;
    private final History history;
    private boolean ignoreevent = false;

    public ConfigSearch(TextField textField, Callback<ISuggestionRequest, Collection<Config>> suggestionProvider) {
        this(textField, new History(), suggestionProvider);
    }

    public ConfigSearch(TextField textField, History history, Callback<ISuggestionRequest, Collection<Config>> suggestionProvider) {
        super(textField, sr -> map(suggestionProvider.call(sr), Entry::new));
        this.history = history;

        this.textField = textField;
        this.textField.setPrefWidth(350); // note this dictates the popup width

        hideOnSuggestion.set(false);
        popup.setOnSuggestion(e -> {
            try {
                ignoreInputChanges = true;
                acceptSuggestion(e.getSuggestion());
                fireAutoCompletion(e.getSuggestion());
                if(hideOnSuggestion.get()) hidePopup();
            } finally {
                // Ensure that ignore is always set back to false
                ignoreInputChanges = false;
            }
        });
    }

    @Override
    protected AutoCompletePopup buildPopup() {
        return new AutoCompletePopup(){
            @Override
            protected Skin<?> createDefaultSkin() {
                return new AutoCompletePopupSkin<Entry>(this,2) {
                    {
                        // set keys & allow typing
                        getSkinnable().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                            if(!ignoreevent)
                                if(e.isControlDown() && (e.getCode()==KeyCode.UP || e.getCode()==KeyCode.DOWN)) {
//                                    switch (e.getCode()) {
//                                        case UP   : up(); break;
//                                        case DOWN : down(); break;
//                                        default   : break;
//                                    }
                                } else if(e.getCode()==KeyCode.BACK_SPACE) {
                                    textField.deletePreviousChar();
                                    e.consume();
                                } else if(!e.getCode().isNavigationKey()) {
                                    // We refire event on text field so we can type even though it
                                    // does not have focus. This causes event stack overflow, so we
                                    // defend against it with a boolean flag.
                                    ignoreevent = true;
                                    completionTarget.fireEvent(e);
                                }
                            ignoreevent = false;
                            // e.consume(); // may brake functionality
                        });
                        getNode().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                            if(!ignoreevent)
                                if(e.isControlDown() && (e.getCode()==KeyCode.UP || e.getCode()==KeyCode.DOWN)) {
                                    switch (e.getCode()) {
                                        case UP   : history.up(ConfigSearch.this); break;
                                        case DOWN : history.down(ConfigSearch.this); break;
                                        default   : break;
                                    }
                                    e.consume();
                                } else if(e.getCode()==KeyCode.BACK_SPACE) {
                                    // textField.deletePreviousChar(); // doenst work here
                                    // e.consume();
                                } else if(e.getCode()==KeyCode.END) {
                                    if (e.isShiftDown()) textField.selectEnd();
                                    else textField.positionCaret(textField.getLength());
                                    e.consume();
                                } else if(e.getCode()==KeyCode.HOME) {
                                    if (e.isShiftDown()) textField.selectHome();
                                    else textField.positionCaret(0);
                                    e.consume();
                                } else if(e.getCode()==KeyCode.LEFT) {
                                    if (e.isControlDown()) textField.selectPreviousWord(); else textField.selectBackward();
                                    if(!e.isShiftDown()) textField.deselect();
                                    e.consume();
                                } else if(e.getCode()==KeyCode.RIGHT) {
                                    if(e.isControlDown()) textField.selectNextWord(); else textField.selectForward();
                                    if(!e.isShiftDown()) textField.deselect();
                                    e.consume();
                                } else if(!e.getCode().isNavigationKey()) {

                                }
                            ignoreevent = false;
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
        if(Runnable.class.isAssignableFrom(suggestion.config.getType()))
            ((Runnable)suggestion.config).run();Node n;
        history.add(this);
    }

    private void acceptSuggestionNhide(Entry suggestion) {
        acceptSuggestion(suggestion);
        popup.hide();
    }

    public static class History {
        private int history_at = 0;
        private final List<String> history = new ArrayList<>();

        private void up(ConfigSearch search) {
            if(history.isEmpty()) return;
            history_at = history_at==0 ? history.size()-1 : history_at-1;
            ((TextField)search.completionTarget).setText(history.get(history_at));
        }

        private void down(ConfigSearch search) {
            if(history.isEmpty()) return;
            history_at = history_at==history.size()-1 ? 0 : history_at+1;
            ((TextField)search.completionTarget).setText(history.get(history_at));
        }

        private void add(ConfigSearch search) {
            String last = history.isEmpty() ? null : history.get(history.size()-1);
            String curr = ((TextField)search.completionTarget).getText();
            boolean isDiff = last!=null & last!=null && last.equalsIgnoreCase(curr);
            if(isDiff) {
                history.add(curr);
                history_at = history.size()-1;
            }
        }
    }
    public static class Entry {
        public final Config<?> config;
        private boolean loaded = false;
        private Node graphics;

        public Entry(Config<?> config) {
            this.config = config;
        }

        public String getName() {
            return config.getGroup() + "." + config.getGuiName();
        }

        public Node getGraphics() {
            if(loaded) return graphics;

            if(config.getType()==Action.class) {
                graphics = new Label(((Action)config).getKeys());
                graphics.setCursor(Cursor.HAND);
                graphics.setMouseTransparent(true);
                ((Label)graphics).setTextAlignment(TextAlignment.RIGHT);
            } else {
                graphics = config.getType()==Boolean.class || config.isTypeEnumerable()
                        ? ConfigField.create(config).getNode()
                        : null;
            }

            loaded = true;
            return graphics;
        }
    }
    static class EntryListCell extends ListCell<Entry> {
        private final Label text = new Label();
        private final StackPane root = layStack(text,Pos.CENTER_LEFT, new StackPane(),Pos.CENTER_RIGHT);
        private final StackPane congigNodeRoot = (StackPane) root.getChildren().get(1);
        private final Tooltip tooltip = new Tooltip();
        private Config oldconfig = null; // cache
        private Node oldnode = null; // cache

        public EntryListCell() {
            text.setTextAlignment(TextAlignment.LEFT);
            text.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            setMinPrefMaxSize(text,USE_COMPUTED_SIZE);
            text.prefWidthProperty().bind(root.widthProperty().subtract(congigNodeRoot.widthProperty()).subtract(10));
            text.setMinWidth(200);
            text.maxWidthProperty().bind(root.widthProperty().subtract(100));
            text.setPadding(new Insets(5,0,0,10));
            Tooltip.install(root, tooltip);
        }

        @Override
        protected void updateItem(Entry item, boolean empty) {
            super.updateItem(item, empty);
            if(empty) {
                text.setText("");
                setGraphic(null);
            } else {
                if(getGraphic()!=root) setGraphic(root);
                tooltip.setText(item.config.getGroup() + item.config.getGuiName() + "\n\n" + item.config.getInfo());

                boolean changed = oldconfig!=item.config;
                oldconfig = item.config;
                Node node = !changed ? oldnode : item.getGraphics();
                if(node instanceof HBox) ((HBox)node).setAlignment(Pos.CENTER_RIGHT);
                if(oldnode!=null) congigNodeRoot.getChildren().remove(oldnode);
                oldnode = node;
                if(node!=null) congigNodeRoot.getChildren().add(node);
                text.setText(item.getName());
            }
        }
    }
}