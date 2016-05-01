package layouts;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import layout.Component;
import layout.container.layout.Layout;
import layout.container.switchcontainer.SwitchContainer;
import layout.widget.Widget;
import layout.widget.Widget.Group;
import layout.widget.controller.ClassController;
import gui.objects.Text;
import gui.objects.image.Thumbnail;
import util.file.Environment;
import util.graphics.fxml.ConventionFxmlLoader;

import static java.util.stream.Collectors.toList;
import static main.App.APP;
import static util.functional.Util.toCSList;

/**
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Layouts",
    description = "",
    howto = "",
    notes = "",
    version = "0.4",
    year = "2014",
    group = Group.APP
)
public final class Layouts extends ClassController {

    Text infoT = new Text();
    @FXML ComboBox<String> layoutsCB;
    @FXML CheckBox lockedChB;
    @FXML Button nfB;
    @FXML TextField nameF;
    @FXML StackPane imgContainer;
    @FXML VBox box;
    Thumbnail thumb = new Thumbnail(250,250);

    public Layouts() {

        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();

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

    /**
     * Completely refreshes layouts - rereads them from files, etc...
     */
    public void refresh() {
        layoutsCB.getItems().setAll(APP.widgetManager.getAllLayoutsNames().collect(toList()));
        layoutsCB.getSelectionModel().select(APP.widgetManager.getActive().getName());
    }

    @FXML
    public void loadSelectedLayout() {
        if (!isSelected()) return;

        SwitchContainer c = APP.windowManager.getActive().getTopContainer();
        Component toLoad = getSelectedLayout().getChild();
        int i = c.getEmptySpot(); // this can normally return null, but not SwitchContainer
        c.addChild(i, toLoad);
        c.ui.alignTab(i);
    }

    public void saveSelectedLayout() {
        if (!isSelected()) return;

        Layout l = getSelectedLayout();
               l.locked.set(lockedChB.isSelected());
               if (!nameF.getText().isEmpty()) l.setName(nameF.getText());
               l.serialize();
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
        Environment.browse(APP.DIR_LAYOUTS);
    }

    private boolean isSelected() {
        return !layoutsCB.getSelectionModel().isEmpty();
    }

    private Layout getSelectedLayout() {
        // create 'empty' layout based on name
        String name = layoutsCB.getSelectionModel().getSelectedItem();
        // attempt to get layout from active layouts
        Layout l = APP.widgetManager.getLayouts().filter(al->al.getName().equals(name)).findAny().orElse(null);
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
        long chs = l.getAllContainers(true).filter(c -> c.getChildren().isEmpty()).count();
            // all empty leaf containers
        long cs = ws-chs;

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
//        thumb.loadImage(l.getThumbnail());
    }
}
