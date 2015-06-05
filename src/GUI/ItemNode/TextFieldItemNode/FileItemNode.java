/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.ItemNode.TextFieldItemNode;

import java.io.File;
import util.File.Environment;
import util.parsing.Parser;

/**
 * {@link AbstractFileField} for {@link File} objects denoting directories
 * specifically, not files.
 * <p>
 * @author Plutonium_
 */
public class FileItemNode extends TextFieldItemNode<File> {
    
    public FileItemNode() {
        super(Parser.toConverter(File.class));
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
