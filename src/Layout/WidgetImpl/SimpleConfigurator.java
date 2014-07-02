
package Layout.WidgetImpl;

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
import utilities.functional.functor.Procedure;

/**
 *
 * @author uranium
 */
public class SimpleConfigurator {
    
    AnchorPane root = new AnchorPane();
    @FXML AnchorPane scrollableArea;
    @FXML GridPane fields;
    
    List<ConfigField> configFields = new ArrayList<>();
    Configurable configurable;
    Procedure onOK;

    /**
     * Constructor.
     * @param configurable configurable object
     * @param on_OK behavior executed when OK button is clicked, null or empty 
     * if none
     */
    public SimpleConfigurator(Configurable configurable, Procedure on_OK) {
        Objects.requireNonNull(configurable);
        
        this.configurable = configurable;
        this.onOK = on_OK;
        
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleConfigurator.class.getResource("SimpleConfigurator.fxml"));
        fxmlLoader.setRoot(root);
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
        // set and apply values if possible
        configFields.forEach(ConfigField::applyNsetIfAvailable);
        // refresh
        initialize();
        // always run the onOk procedure 
        if(onOK!=null) onOK.run();
    }
    
    public AnchorPane getPane() {
        return root;
    }
}
