/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemHolders.ItemTextFields;

import java.io.File;
import utilities.Parser.File.Enviroment;
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
            File f = Enviroment.chooseFile("Choose directory", true, item, getScene().getWindow());
            setItem(f);
        } else {
            File f = Enviroment.chooseFile("Choose file", false, item, getScene().getWindow());
            setItem(f);
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
