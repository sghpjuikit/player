
package unused;

import Configuration.Config;
import Configuration.Configurable;
import gui.itemnode.ConfigField;
import com.sun.glass.ui.Screen;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import static util.functional.Util.byNC;
import util.graphics.fxml.ConventionFxmlLoader;

/**
 * Configurable state transformer graphical control. Graphics to configure 
 * {@link Configurable}.
 * <p>
 * When OK button is clicked all changed {@ConfigField}s with unapplied values
 * will be set and applied. Then the specified behavior is executed.
 * 
 * @param <T> Specifies generic type of Configurable for this component. Only
 * use it for singleton configurables or configurables that contain configs with
 * the same value type.
 * <p>
 * The advantage of using generic version is in accessing the values in the
 * OK button callback. The configurable is provided as a parameter and if this
 * object is generic it will provide correct configurable returning correct
 * values without type casting.
 * @see Configurable  
 * @author uranium
 */
public class SimpleConfigurator<T> extends AnchorPane {
    
    @FXML private GridPane fields;
    @FXML private BorderPane buttonPane;
    @FXML private ScrollPane fieldsPane;
    
    private final double anchor;
    private final List<ConfigField<T>> configFields = new ArrayList();
    private final Configurable<T> configurable;
    /**
     * Procedure executed when user finishes the configuring. 
     * Invoked when ok button is pressed.
     * Default implementation does nothing. Must not be null.
     * <p>
     * For example one might want to close this control when no item is selected.
     */
    public Consumer<Configurable<T>> onOK = c -> {};

    /**
     * @param c configurable object
     * @param on_OK OK button click action. Null if none. Affects visibility
     * of the OK button. It is only visible if there is an action to execute.
     */
    public SimpleConfigurator(Configurable<T> c, Consumer<Configurable<T>> on_OK) {
        Objects.requireNonNull(c);
        
        configurable = c;
        onOK = on_OK==null ? onOK : on_OK;
        
        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();
        
        fieldsPane.setMaxHeight(Screen.getMainScreen().getVisibleHeight()*0.7);
        anchor = AnchorPane.getBottomAnchor(fieldsPane);
        setOkButtonVisible(on_OK!=null);
        
        // set configs
        configFields.clear();
        fields.getChildren().clear();
        c.getFields().stream()
         .sorted(byNC(Config::getGuiName))
         .filter(Config::isEditable)
         .forEach( f -> { 
            ConfigField cf = ConfigField.create(f);                 // create
            configFields.add(cf);                                   // add
            fields.add(cf.createLabel(), 0, configFields.size()-1);    // populate
            fields.add(cf.getNode(), 1, configFields.size()-1);
        });
        
        // prevent scroll even leak
        fieldsPane.setOnScroll(Event::consume);
    }
    
    public SimpleConfigurator(Configurable<T> configurable) {
        this(configurable, null);
    }

/******************************** PUBLIC API **********************************/
    
    @FXML
    public void ok() {
        // set and apply values and refresh if needed
        configFields.forEach(ConfigField::apply);
        onOK.accept(configurable);
    }
    
    public Configurable getConfigurable() {
        return configurable;
    }
    
    public final void setOkButtonVisible(boolean val) {
        buttonPane.setVisible(val);
        AnchorPane.setBottomAnchor(fieldsPane, val ? anchor : 0d);
    }
}
