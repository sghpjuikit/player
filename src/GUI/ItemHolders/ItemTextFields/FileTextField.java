/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemHolders.ItemTextFields;

import java.io.File;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import main.App;
import utilities.Parser.ParserImpl.FileParser;
import utilities.Parser.StringParser;

/**
 * {@link AbstractFileField} for {@link File} objects denoting directories
 * specifically, not files.
 * <p>
 * @author Plutonium_
 */
public class FileTextField extends ItemTextField<File> {
    
    public FileTextField() {
        this(FileParser.class);
    }
    public FileTextField(Class<? extends StringParser<File>> parser_type) {
        super(parser_type);
    }

    @Override
    void onDialogAction() {
        if(item==null || item.isDirectory()) {
            File initf = item==null ? App.getLocation() : item;
            DirectoryChooser dc = new DirectoryChooser();
                             dc.setInitialDirectory(initf);
                             dc.setTitle("Choose directory");
            File newfile = dc.showDialog(getScene().getWindow());
            setItem(newfile);
        } else {
            File initf = item==null ? App.getLocation() : item;
            FileChooser fc = new FileChooser();
                        fc.setInitialDirectory(initf);
                        fc.setTitle("Choose file");
            File newfile = fc.showOpenDialog(getScene().getWindow());
            setItem(newfile);
        }
    }

    @Override
    String itemToString(File item) {
        if(item!=null && item.isDirectory())
            return item.getPath();
        else
            return "";
    }
    
}
