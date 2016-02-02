/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.itemnode.TextFieldItemNode;

import java.io.File;

import util.file.Environment;
import util.parsing.Parser;

/**
 * {@link AbstractFileField} for {@link File} objects denoting directories
 * specifically, not files.
 * <p>
 * @author Plutonium_
 */
public class FileItemNode extends TextFieldItemNode<File> {

    public FileItemNode() {
        super(Parser.DEFAULT.toConverter(File.class));
    }

    @Override
    void onDialogAction() {
        boolean isDir = v==null || v.isDirectory();
        File f = Environment.chooseFile(isDir ? "Choose directory" : "Choose file", isDir, v, getScene().getWindow());
        if(f!=null) setValue(f);
    }

    @Override
    String itemToString(File item) {
        if(item!=null && item.isDirectory())
            return item.getPath();
        else
            return "";
    }

}
