
package Layout.WidgetImpl;

import Layout.Layout;
import Layout.LayoutManager;
import Layout.UniContainer;
import gui.objects.Text;
import gui.objects.Thumbnail.Thumbnail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import main.App;
import util.File.Environment;
import util.dev.Log;
import static util.functional.Util.toCSList;

/**
 *
 * @author uranium
 */
public final class LayoutManagerComponent {
    
    AnchorPane root = new AnchorPane();
    Text infoT = new Text();
    @FXML ComboBox<String> layoutsCB;
    @FXML CheckBox lockedChB;
    @FXML Button nfB;
    @FXML TextField nameF;
    @FXML StackPane imgContainer;
    @FXML VBox box;
    private Thumbnail thumb = new Thumbnail(250,250);
    
    public LayoutManagerComponent() {
        
        FXMLLoader fxmlLoader = new FXMLLoader(LayoutManagerComponent.class.getResource("LayoutManager.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            Log.err("LayoutManager source data coudlnt be read." + e.getMessage());
        }
        
        infoT.setWrappingWidth(200);
        box.getChildren().add(3, infoT);
        
        layoutsCB.valueProperty().addListener((o,oldValue,newValue) -> {
            if (isSelected())
                displayInfo(getSelectedLayout());
            else
                infoT.setText("");
        });
        layoutsCB.setCellFactory( list -> {
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
        });
        
        imgContainer.getChildren().add(thumb.getPane());
        refresh();
    }
    
    public AnchorPane getPane() {
        return root;
    }
    
    /**
     * Completely refreshes layouts - rereads them from files, etc...
     */
    public void refresh() {
        layoutsCB.getItems().setAll(LayoutManager.getAllLayoutsNames().collect(Collectors.toList()));
        layoutsCB.getSelectionModel().select(LayoutManager.getActive().getName());
    }
    
    @FXML
    public void loadSelectedLayout() {
        if (!isSelected()) return;
        
        LayoutManager.changeActiveLayout(getSelectedLayout());
    }
    
    public void saveSelectedLayout() {
        if (!isSelected()) return;
        
        Layout l = getSelectedLayout();
               l.locked.set(lockedChB.isSelected());
               if (!nameF.getText().isEmpty()) l.setName(nameF.getText());
               l.serialize();
               l.makeSnapshot();
        refresh();
    }
    
    @FXML
    public void newLayout() {
        Layout l = new Layout();
               l.serialize();
        refresh();
    }
    @FXML
    public void removeLayout() {
        getSelectedLayout().removeFile();
        refresh();
    }
    @FXML
    public void openLayoutDirectory() {
        Environment.browse(App.LAYOUT_FOLDER().toURI());
    }
    
    private boolean isSelected() {
        return !layoutsCB.getSelectionModel().isEmpty();
    }
    
    private Layout getSelectedLayout() {
        // create 'empty' layout based on name
        String name = layoutsCB.getSelectionModel().getSelectedItem();
        // attempt to get layout from active layouts
        Layout l = LayoutManager.getLayouts().filter(al->al.getName().equals(name)).findAny().orElse(null);
        // attempt to deserialize the layout if not active
        if(l==null) { 
            l = new Layout(name);
            l.deserialize();
        }
        return l;
    }
    
    private void displayInfo(Layout l) {                
        nameF.setText("");
        nameF.setPromptText(l.getName());
        lockedChB.setSelected(l.locked.get());
        
        List<String> w_names = new ArrayList();
        // get children counts by counting leaf Components
            // all widgets (and fetch names while at it to avoid reiterating
        long ws = l.getAllWidgets().peek(w->w_names.add(w.getName())).count();
            // all leaf containers - cs that contain only widgets
        long chs = l.getAllContainers().filter(UniContainer.class::isInstance).count();
            // all empty leaf containers
        long cs = chs-ws;
        
        // show info
        String s;
        s  = "Name: " + l.getName() + "\n";
        s += "Active: " + (l.isMain()) + "\n";
        s += "Children: " + (ws+cs) + "\n";
        s += "Containers: " + cs + "\n";
        s += "Widgets: " + ws + "\n";
        s += "Widgets: " + w_names.stream().collect(toCSList);
        s += "\n";
        infoT.setText(s);
        // show thumbnail
        thumb.loadImage(l.getThumbnail());
    }
}
