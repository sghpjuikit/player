
package GUI.Components;

import Configuration.Config;
import Configuration.Configurable;
import GUI.ItemHolders.ConfigField;
import utilities.functional.functor.Procedure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import utilities.Log;

/**
 *
 * @author uranium
 */
public class SimpleConfigurator {
    
    private static final FXMLLoader fxmlLoader = new FXMLLoader(SimpleConfigurator.class.getResource("SimpleConfigurator.fxml"));
    
    AnchorPane root = new AnchorPane();
    @FXML AnchorPane scrollableArea;
    @FXML GridPane fields;
    
    List<ConfigField> configFields = new ArrayList<>();
    Configurable configurable;
    Procedure onOK;

    /**
     * Constructor.
     * @param p configurable object
     * @param on_OK behavior executed when OK button is clicked, null or empty 
     * if none
     */
    public SimpleConfigurator(Configurable p, Procedure on_OK) {
        configurable = p;
        onOK = on_OK==null ? ()->{} : on_OK;
        
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
        if (configFields.stream().noneMatch(ConfigField::hasValue)) {
            Log.mess("No change");
            return;
        }
        
        for (ConfigField f: configFields)
            if (f.hasValue())
                configurable.setField(f.getName(), f.getValue());
        
        initialize();
        onOK.run();
    }
    
    public AnchorPane getPane() {
        return root;
    }
}
