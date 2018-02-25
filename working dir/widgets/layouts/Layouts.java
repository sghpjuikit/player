package layouts;

import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sp.it.pl.gui.objects.Text;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.layout.Layout;
import sp.it.pl.layout.container.switchcontainer.SwitchContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.Widget.Group;
import sp.it.pl.layout.widget.controller.ClassController;
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.functional.Util.toCSList;
import static sp.it.pl.util.system.EnvironmentKt.browse;

@SuppressWarnings({"WeakerAccess", "unused"})
@Widget.Info(
    author = "Martin Polakovic",
    name = "Layouts",
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

        thumb.setBorderVisible(true);
        infoT.setWrappingWidth(200);
        box.getChildren().add(3, infoT);

        layoutsCB.valueProperty().addListener((o,ov,nv) -> {
            if (isSelected())
                displayInfo(getSelectedLayout());
            else
                infoT.setText("");
        });
        layoutsCB.setCellFactory( list -> new ListCell<>() {
            @Override protected void updateItem(String l, boolean empty) {
                super.updateItem(l, empty);
                setText(empty ? "" : l);
            }
        });

        imgContainer.getChildren().add(thumb.getPane());
        refresh();
    }

    /**
     * Completely refreshes layouts - rereads them from files, etc...
     */
    public void refresh() {
        layoutsCB.getItems().setAll(APP.widgetManager.layouts.getAllNames().collect(toList()));
        layoutsCB.getSelectionModel().select(APP.widgetManager.layouts.findActive().getName());
    }

    @FXML
    public void loadSelectedLayout() {
        if (!isSelected()) return;

        SwitchContainer c = APP.windowManager.getActiveOrNew().getTopContainer();
        Component toLoad = getSelectedLayout().getChild();
        int i = c.getEmptySpot(); // this can usually return null, but never in case of SwitchContainer
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
        browse(APP.DIR_LAYOUTS);
    }

    private boolean isSelected() {
        return !layoutsCB.getSelectionModel().isEmpty();
    }

    private Layout getSelectedLayout() {
        // create 'empty' layout based on name
        String name = layoutsCB.getSelectionModel().getSelectedItem();
        // attempt to get layout from active layouts
        Layout l = APP.widgetManager.layouts.findAllActive().filter(al->al.getName().equals(name)).findAny().orElse(null);
        // attempt to deserialize the layout if not active
        if (l==null) {
            l = new Layout(name);
            l.deserialize();
        }
        return l;
    }

    private void displayInfo(Layout l) {
        nameF.setText("");
        nameF.setPromptText(l.getName());
        lockedChB.setSelected(l.locked.get());

        List<String> w_names = new ArrayList<>();
        // get children counts by counting leaf Components
            // all widgets (and fetch names while at it to avoid reiterating
        long ws = l.getAllWidgets().peek(w->w_names.add(w.getName())).count();
        long chs = l.getAllContainers(true).filter(c -> c.getChildren().isEmpty()).count();
            // all empty leaf containers
        long cs = ws-chs;

        // show info
        String s;
        s  = "Name: " + l.getName() + "\n";
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