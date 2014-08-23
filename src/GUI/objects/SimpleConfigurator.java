
package GUI.objects;

import Configuration.Configurable;
import GUI.ItemHolders.ConfigField;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import utilities.Log;
import utilities.Util;
import utilities.functional.functor.UnProcedure;

/**
 * Configurable state transformer graphical control.
 * <p>
 * Generates fields to configure {@link Configurable} and displays an OK
 * button with customizable action.
 * <p>
 * When OK button is clicked all changed {@ConfigField}s with unapplied values
 * will be set and applied. Then the specified behavior is executed.
 * 
 * @param <T> Specifies generic type of Configurable for this component. Only
 * use it for singleton configurables or configurables which contain configs with
 * the same value type.
 * 
 * The advantage of using generic version is in accessing the values in the
 * OK button callback. The configurable is provided as a parameter and if this
 * object is generic it will provide correct configurable returning correct
 * values without casting. 
 * 
 * @see Configurable  
 * @author uranium
 */
public class SimpleConfigurator<T> extends AnchorPane {
    
    @FXML private GridPane fields;
    private final List<ConfigField<T>> configFields = new ArrayList();
    private final Configurable<T> configurable;
    private final UnProcedure<Configurable<T>>  onOK;

    /**
     * Constructor.
     * @param configurable configurable object
     * @param on_OK behavior executed when OK button is clicked, null if none.
     * The procedure provides the Configurable of this configurator as a 
     * parameter to access the configs.
     */
    public SimpleConfigurator(Configurable<T> configurable, UnProcedure<Configurable<T>> on_OK) {
        Objects.requireNonNull(configurable);
        
        this.configurable = configurable;
        this.onOK = on_OK;
        
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleConfigurator.class.getResource("SimpleConfigurator.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            Log.err("SimpleConfiguratorComponent source data coudlnt be read.");
        }
        initialize();
    }
    
    private void initialize() {
        configFields.clear();
        fields.getChildren().clear();
        
        configurable.getFields().stream().sorted(Util.cmpareNoCase( f -> f.getGuiName())).forEach( f -> {
            ConfigField cf = ConfigField.create(f);                 // create
            
            if (f.isEditable()) {                   // ignore noneditabe                    
                configFields.add(cf);                                   // add
                fields.add(cf.getLabel(), 0, configFields.size()-1);    // populate
                fields.add(cf.getControl(), 1, configFields.size()-1);
            }
        });
    }
    
    @FXML
    private void ok() {
        // set and apply values and refresh if needed
        configFields.forEach(ConfigField::applyNsetIfAvailable);
        // always run the onOk procedure 
        if(onOK!=null) onOK.accept(configurable);
    }
    
    public Configurable getConfigurable() {
        return configurable;
    }
}
