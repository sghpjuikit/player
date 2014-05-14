/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.ItemTextFields;

import Configuration.ConfigManager;
import java.io.File;
import javafx.stage.DirectoryChooser;
import main.App;
import utilities.Parser.FileParser;
import utilities.Parser.StringParser;

/**
 * 
 * <p>
 * @author Plutonium_
 */
public class DirTextField extends ItemTextField<File,StringParser<File>> {
    
    public DirTextField() {
        this(FileParser.class);
    }
    public DirTextField(Class<? extends StringParser<File>> parser_type) {
        super(parser_type);
    }

    @Override
    void onDialogAction() {
        File initf = item==null ? App.getAppLocation() : item;
        DirectoryChooser dc = new DirectoryChooser();
                         dc.setInitialDirectory(initf);
                         dc.setTitle("Choose directory");
        File newfile = dc.showDialog(App.getInstance().getStage());
        setItem(newfile);
    }

    @Override
    String itemToString(File item) {
        if(item!=null && item.isDirectory())
            return item.getPath();
        else
            return "";
    }
    
}
