package util.demo;

import java.io.File;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.graphics.IconExtractor;

import static javafx.collections.FXCollections.observableArrayList;

public class IconExtractorDemo extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	private final ListView<String> list = new ListView<>();
	private final ObservableList<String> data = observableArrayList(
		// "C:\\CCleaner\\CCleaner.exe", // an example to test out .exe (windows executable) file
		// "C:\\CCleaner\\CCleaner.lnk", // an example to test out .lnk (windows shortcut) file
		"a.msg", "a1.msg", "b.txt", "c.pdf",
		"d.html", "e.png", "f.zip",
		"g.docx", "h.xlsx", "i.pptx"
	);

	@Override
	public void start(Stage stage) {
		VBox box = new VBox();
		Scene scene = new Scene(box, 200, 200);
		stage.setScene(scene);
		stage.setTitle("ListViewSample");
		box.getChildren().addAll(list);
		VBox.setVgrow(list, Priority.ALWAYS);

		list.setItems(data);
		list.setCellFactory(list1 -> new AttachmentListCell());

		stage.show();
	}

	private static class AttachmentListCell extends ListCell<String> {
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				Image fxImage = IconExtractor.getFileIcon(new File(item));
				ImageView imageView = new ImageView(fxImage);
				setGraphic(imageView);
				setText(item);
			}
		}
	}

}