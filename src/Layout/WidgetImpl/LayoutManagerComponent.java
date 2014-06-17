
package Layout.WidgetImpl;

import GUI.objects.Thumbnail;
import Layout.Layout;
import Layout.LayoutManager;
import Layout.Widgets.Widget;
import java.io.IOException;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import main.App;
import utilities.Enviroment;
import utilities.Log;

/**
 *
 * @author uranium
 */
public final class LayoutManagerComponent {
    
    AnchorPane root = new AnchorPane();
    @FXML TextArea infoT;
    @FXML ComboBox<String> layoutsCB;
    @FXML CheckBox lockedChB;
    @FXML Button nfB;
    @FXML TextField nameF;
    @FXML BorderPane borderpane;
    private final Thumbnail thumb = new Thumbnail(250);
    
    public LayoutManagerComponent() {
        
        FXMLLoader fxmlLoader = new FXMLLoader(LayoutManagerComponent.class.getResource("LayoutManager.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            Log.err("LayoutManager source data coudlnt be read." + e.getMessage());
        }
        
        
        layoutsCB.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (isSelected())
                displayInfo(getSelectedLayout());
            else
                infoT.setText("");
        });
        layoutsCB.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override public void updateItem(String l, boolean empty) {
                        super.updateItem(l, empty);
                        if (empty)
                            setText("");
                        else
                            if (new Layout(l).isMain())
                                setText(l + " (active)");
                            else
                                setText(l);
                    }
                };
            }
        });
        
        borderpane.setRight(thumb.getPane());
        BorderPane.setAlignment(thumb.getPane(), Pos.CENTER_RIGHT);
        refresh();
    }
    
    public AnchorPane getPane() {
        return root;
    }
    
    /**
     * Completely refreshes layouts - rereads them from files, etc...
     */
    public void refresh() {
        LayoutManager.findLayouts();
        layoutsCB.getItems().setAll(LayoutManager.layouts);
        layoutsCB.getSelectionModel().select(LayoutManager.getActive().getName());
    }
    
    public void loadSelectedLayout() {
        if (isSelected())
            LayoutManager.changeActiveLayout(getSelectedLayout());
    }
    
    public void saveSelectedLayout() {
        if (!isSelected()) return;
        Layout l = getSelectedLayout();
               l = l.equals(LayoutManager.active.get(0)) ? LayoutManager.active.get(0) : l;
        l.setLocked(lockedChB.isSelected());
        if (!nameF.getText().isEmpty())
            l.setNameAndSave(nameF.getText());
        else
            l.serialize();
        LayoutManager.makeSnapshot();
        refresh();
    }
    
    @FXML
    public void newLayout() {
        Layout l = new Layout();
        LayoutManager.layouts.add(l.getName());
        l.serialize();
        layoutsCB.getItems().setAll(LayoutManager.layouts);
        layoutsCB.getSelectionModel().select(layoutsCB.getItems().size()-1);
    }
    @FXML
    public void removeLayout() {
        getSelectedLayout().removeFile();
        refresh();
    }
    @FXML
    public void openLayoutDirectory() {
        Enviroment.browse(App.LAYOUT_FOLDER());
    }
    
    private boolean isSelected() {
        return !layoutsCB.getSelectionModel().isEmpty();
    }
    
    private Layout getSelectedLayout() {
        Layout l =  new Layout(layoutsCB.getSelectionModel().getSelectedItem());
               l.deserialize();
        return l;
    }
    
    private void displayInfo(Layout l) {
        l.deserialize();
                
        nameF.setText("");
        nameF.setPromptText(l.getName());
        lockedChB.setSelected(l.isLocked());
        
        // show info
        String s;
        s  = "Active: " + (l.isMain()) + "\n\n\n";
        s += "Name: " + l.getName() + "\n\n\n";
        s += "Children: " + l.getAllChildren().size() + "\n";
        s += "Containers: " + l.getAllContainers().size() + "\n";
        s += "Widgets: " + l.getAllWidgets().size() + "\n";
        s += "Widgets:";
        s += l.getAllWidgets().stream().map(Widget::getName).collect(Collectors.joining(", "));
        s += "\n";
        infoT.setPromptText(s);
        
        // show thumbnail
        thumb.loadImage(l.getThumbnail());
    }
}
