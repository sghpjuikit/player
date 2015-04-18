/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.ItemNode.ItemTextFields;

import java.io.File;
import util.File.Environment;
import util.parsing.impl.FileParser;
import util.parsing.StringParser;

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
        if(v==null || v.isDirectory()) {
            File f = Environment.chooseFile("Choose directory", true, v, getScene().getWindow());
            setValue(f);
        } else {
            File f = Environment.chooseFile("Choose file", false, v, getScene().getWindow());
            setValue(f);
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
