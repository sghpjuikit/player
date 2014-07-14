
package GUI.objects;

import Configuration.Config;
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
import utilities.functional.functor.UnProcedure;

/**
 * Graphical control that configures {@link Configurable} and display an OK
 * button with customizable action.
 * <p>
 * When OK button is clicked all changed {@ConfigField}s with unapplied values
 * will be set and applied. Then the specified behavior is executed.
 * 
 * @author uranium
 */
public class SimpleConfigurator extends AnchorPane {
    
    @FXML private GridPane fields;
    private final List<ConfigField> configFields = new ArrayList<>();
    private final Configurable configurable;
    private final UnProcedure<Configurable>  onOK;

    /**
     * Constructor.
     * @param configurable configurable object
     * @param on_OK behavior executed when OK button is clicked, null if none
     * The procedure provides the Configurable of this configurator as a parameter.
     * It can be used to retrieve its {@link Config}s and their values.
     * When OK button is clicked all changed {@link ConfigField}s rendering
     * the Configs will have their Configs set and applied if they contain
     * unapplied values. After that this specified behavior is executed.
     */
    public SimpleConfigurator(Configurable configurable, UnProcedure<Configurable> on_OK) {
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
        
        for(Config f: configurable.getFields()) {
            ConfigField cf = ConfigField.create(f);                 // create
            
            if (cf == null)// || (!showNonEdit() && !f.editable))
                continue;                                           // ignore on fail || noneditabe
            configFields.add(cf);                                   // add
            fields.add(cf.getLabel(), 0, configFields.size()-1);    // populate
            fields.add(cf.getControl(), 1, configFields.size()-1);
        }
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
