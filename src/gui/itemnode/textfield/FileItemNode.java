
package gui.itemnode.textfield;

import java.io.File;

import javafx.geometry.Pos;

import util.file.Environment;
import util.file.FileType;
import util.parsing.Parser;
import util.validation.Constraint;

import static util.file.FileType.DIRECTORY;
import static util.graphics.Util.layHorizontally;

/**
 * {@link TextFieldItemNode} for {@link File} objects denoting directories
 * specifically, not files.
 *
 * @author Martin Polakovic
 */
public class FileItemNode extends TextFieldItemNode<File> {
	public final Constraint.FileActor fileActor;
	private FileType type;

    public FileItemNode(Constraint.FileActor fileActor) {
        super(Parser.DEFAULT.toConverter(File.class));
	    this.fileActor = fileActor;
	    this.type = fileActor== Constraint.FileActor.FILE ? FileType.FILE : FileType.DIRECTORY;

	    if (fileActor== Constraint.FileActor.ANY) {
		    ArrowDialogButton b1 = new ArrowDialogButton();
		    b1.setOnMouseClicked(e -> type = type.next());
		    ArrowDialogButton b2 = (ArrowDialogButton) getRight();
		    setRight(layHorizontally(5, Pos.CENTER_RIGHT, b1,b2));
	    }
    }

    @Override
    void onDialogAction() {
        File f = Environment.chooseFile(type==DIRECTORY ? "Choose directory" : "Choose file", type, v, getScene().getWindow());
        if (f!=null) setValue(f);
    }

    @Override
    String itemToString(File item) {
        return item==null ? "<none>" : item.getPath();
    }

}