package gui.itemnode.textfield;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.icon.Icon;
import java.io.File;
import javafx.geometry.Pos;
import util.access.V;
import util.file.Environment;
import util.file.FileType;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;
import util.validation.Constraint;
import static javafx.scene.input.DragEvent.DRAG_DROPPED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static util.file.FileType.DIRECTORY;
import static util.functional.Util.stream;
import static util.graphics.Util.layHorizontally;
import static util.reactive.Util.maintain;

/**
 * {@link TextFieldItemNode} for {@link File} objects.<br/>
 * Features:
 * <ul>
 * <li> It is possible (but not necessary) to limit the file type to  directory or a file. If the limit is lifted,
 * user can switch the mode to open file or directory dialog.
 * <li> There is also support for drag & drop action.
 * </ul>
 *
 * @author Martin Polakovic
 */
public class FileItemNode extends TextFieldItemNode<File> {
	public final Constraint.FileActor fileActor;
	public final V<FileType> type;

	public FileItemNode(Constraint.FileActor fileActor) {
		super(Parser.DEFAULT.toConverterOf(File.class));
		this.fileActor = fileActor;
		this.type = new V<>(fileActor==Constraint.FileActor.FILE ? FileType.FILE : FileType.DIRECTORY);

		if (fileActor==Constraint.FileActor.ANY) {
			Icon b1 = new Icon(null, 7).tooltip("Switch mode between file and directory");
			maintain(type, t -> t==FileType.FILE ? FontAwesomeIcon.FILE : FontAwesomeIcon.FOLDER, b1::icon);
			b1.setOnMouseClicked(e -> type.setNextValue());
			ArrowDialogButton b2 = (ArrowDialogButton) getRight();
			setRight(layHorizontally(5, Pos.CENTER_RIGHT, b1, b2));
		}

		addEventHandler(DRAG_OVER, DragUtil.accept(e -> DragUtil.getFiles(e).stream().anyMatch(fileActor::isValid)));
		addEventHandler(DRAG_DROPPED, e -> setValue(stream(DragUtil.getFiles(e)).findAny(fileActor::isValid).get()));
	}

	@Override
	void onDialogAction() {
		String title = type.get()==DIRECTORY ? "Choose directory" : "Choose file";
		Environment.chooseFile(title, type.get(), v, getScene().getWindow())
				.ifOk(this::setValue);
	}

	@Override
	String itemToString(File item) {
		return item==null ? "<none>" : item.getPath();
	}

}