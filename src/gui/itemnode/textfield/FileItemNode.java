
package gui.itemnode.textfield;

import java.io.File;

import util.file.Environment;
import util.file.FileType;
import util.parsing.Parser;

import static util.file.FileType.DIRECTORY;
import static util.file.FileType.FILE;

/**
 * {@link TextFieldItemNode} for {@link File} objects denoting directories
 * specifically, not files.
 *
 * @author Martin Polakovic
 */
public class FileItemNode extends TextFieldItemNode<File> {

    public FileItemNode() {
        super(Parser.DEFAULT.toConverter(File.class));
    }

    @Override
    void onDialogAction() {
        FileType type = v==null || v.isDirectory() ? DIRECTORY : FILE;
        File f = Environment.chooseFile(type==DIRECTORY ? "Choose directory" : "Choose file", type, v, getScene().getWindow());
        if (f!=null) setValue(f);
    }

    @Override
    String itemToString(File item) {
        return item==null ? "<none>" : item.getPath();
    }

}