package sp.it.pl.gui.itemnode.textfield;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import javafx.geometry.Pos;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.util.access.V;
import sp.it.pl.util.file.FileType;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.validation.Constraint;
import static javafx.scene.input.DragEvent.DRAG_DROPPED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.layHorizontally;
import static sp.it.pl.util.reactive.Util.maintain;
import static sp.it.pl.util.system.EnvironmentKt.chooseFile;

/**
 * {@link TextFieldItemNode} for {@link File} objects.<br/>
 * Features:
 * <ul>
 * <li> Optional file type constraint to directory or a file.
 * <li> User can switch the mode to open file or directory dialog (if the limit is lifted).
 * <li> Support for drag & drop action.
 * </ul>
 */
public class FileItemNode extends TextFieldItemNode<File> {

	/**
	 * File type constraint.
	 */
	public final Constraint.FileActor fileActor;
	private final V<FileType> type;

	public FileItemNode(Constraint.FileActor fileActor) {
		super(f -> APP.converter.general.toS(f));
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
		addEventHandler(DRAG_DROPPED, e -> setValue(stream(DragUtil.getFiles(e)).filter(fileActor::isValid).findAny().get()));
	}

	@Override
	void onDialogAction() {
		String title = type.get()==DIRECTORY ? "Choose directory" : "Choose file";
		chooseFile(title, type.get(), v, getScene().getWindow())
				.ifOk(this::setValue);
	}

}