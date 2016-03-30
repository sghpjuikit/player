package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.filechooser.FileSystemView;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import util.file.Util;

// http://stackoverflow.com/questions/15629069/extract-application-icons-from-desktop-folder-to-application
// http://stackoverflow.com/questions/15149565/how-does-jtree-display-file-name/15150756#15150756
// http://stackoverflow.com/questions/28034432/javafx-file-listview-with-icon-and-file-name
// http://stackoverflow.com/questions/26192832/java-javafx-set-swing-icon-for-javafx-label
public class ImageThumbIconTest extends Application {

    ListView<String> list = new ListView<String>();
    ObservableList<String> data = FXCollections.observableArrayList(
            "C:\\software\\CCleaner\\CCleaner.exe",
            "C:\\software\\Sublime Text 2.0.2 (P)\\sublime_text.exe",
            "a.msg", "a1.msg", "b.txt", "c.pdf",
            "d.html", "e.png", "f.zip",
            "g.docx", "h.xlsx", "i.pptx");

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
                Image fxImage = getFileIcon(new File(item));
                ImageView imageView = new ImageView(fxImage);
                setGraphic(imageView);
                setText(item);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }


    static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<>();

    private static javax.swing.Icon getJSwingIconFromFileSystem(File file) {

        // Windows {
        FileSystemView view = FileSystemView.getFileSystemView();
        javax.swing.Icon icon = view.getSystemIcon(file);
        // }

        // OS X {
        //final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        //javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
        // }

        return icon;
    }

    public static Image getFileIcon(File file) {
        // FIXME: !work for windows shortcuts
        String ext = Util.getSuffix(file.getPath()).toLowerCase();
//        file.i
        String key = "exe".equals(ext) ? Util.getName(file) : ext;

        return mapOfFileExtToSmallIcon.computeIfAbsent(key, key_ -> {
            javax.swing.Icon jswingIcon = null;
            if (file.exists()) {
                jswingIcon = getJSwingIconFromFileSystem(file);
            } else {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("icon", ext);
                    jswingIcon = getJSwingIconFromFileSystem(tempFile);
                }
                catch (IOException ignored) {
                    // Cannot create temporary file.
                }
                finally {
                    if (tempFile != null) tempFile.delete();
                }
            }
            return jswingIconToImage(jswingIcon);
        });
    }

    private static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
        BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

}