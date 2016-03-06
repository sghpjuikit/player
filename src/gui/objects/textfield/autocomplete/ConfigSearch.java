package gui.objects.textfield.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import gui.itemnode.ConfigField;
import gui.objects.textfield.autocomplete.ConfigSearch.Entry;
import util.conf.Config;
import util.graphics.Util;

import static util.functional.Util.map;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.setMinPrefMaxSize;

/**
 * Created by Plutonium_ on 3/6/2016.
 */
public class ConfigSearch extends AutoCompletion<Entry> {

    public static List<String> HISTORY = new ArrayList<>();

    public ConfigSearch(TextField textField, Callback<ISuggestionRequest, Collection<Config>> suggestionProvider) {
        super(textField, sr -> map(suggestionProvider.call(sr), Entry::new));

        hideOnSuggestion.set(false);
        popup.setOnSuggestion(e -> {
            try{
                ignoreInputChanges = true;
                acceptSuggestion(e.getSuggestion());
                fireAutoCompletion(e.getSuggestion());
                if(hideOnSuggestion.get()) hidePopup();
            }finally{
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
                return new AutoCompletePopupSkin<Entry>(this) {
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
            ((Runnable)suggestion.config).run();
    }

    static class Entry {
        public final Config<?> config;

        public Entry(Config<?> config) {
            this.config = config;
        }

        public String getName() {
            return config.getGroup() + "." + config.getGuiName();
        }
    }
    static class EntryListCell extends ListCell<Entry> {
        private Label text = new Label();
        private HBox root = layHorizontally(10, Pos.CENTER_LEFT, text);
        private Config oldconfig = null; // cache
        private Node oldnode = null; // cache

        public EntryListCell() {
            text.setTextAlignment(TextAlignment.LEFT);
            text.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            setMinPrefMaxSize(text,USE_COMPUTED_SIZE);
            HBox.setHgrow(text, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(Entry item, boolean empty) {
            super.updateItem(item, empty);
            if(empty) {
                text.setText("");
                setGraphic(null);
            } else {
                if(getGraphic()!=root) setGraphic(root);

                boolean changed = oldconfig!=item.config;
                oldconfig = item.config;
                Node node = !changed
                        ? oldnode
                        : (item.config.getType()==Boolean.class || item.config.isTypeEnumerable())
                            ? ConfigField.create(item.config).getNode()
                            : null;
                if(oldnode!=null) root.getChildren().remove(oldnode);
                oldnode = node;
                if(node!=null) root.getChildren().add(node);
                text.setText(item.getName());
            }
        }
    }
}
