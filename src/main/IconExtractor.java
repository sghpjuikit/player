package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import javafx.application.Application;
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

import AppLauncher.AppLauncher;
import sun.awt.shell.ShellFolder;
import util.LazyR;
import util.R;
import util.file.Util;
import util.file.WindowsShortcut;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static javafx.collections.FXCollections.observableArrayList;
import static util.async.Async.runFX;

/**
 * Extracts an icon for a file type of specific file.
 * <p/>
 * http://stackoverflow.com/questions/15629069/extract-application-icons-from-desktop-folder-to-application
 * <br/>
 * http://stackoverflow.com/questions/15149565/how-does-jtree-display-file-name/15150756#15150756
 * <br/>
 * http://stackoverflow.com/questions/28034432/javafx-file-listview-with-icon-and-file-name
 * <br/>
 * http://stackoverflow.com/questions/26192832/java-javafx-set-swing-icon-for-javafx-label
 */
public class IconExtractor extends Application {

    ListView<String> list = new ListView<>();
    ObservableList<String> data = observableArrayList(
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


	private static R<FileSystemView> helperFileSystemView = new LazyR<>(FileSystemView::getFileSystemView);
    private static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<>();

    private static javax.swing.Icon getJSwingIconFromFileSystem(File file) {

        // Windows
        FileSystemView view = helperFileSystemView.get();
        javax.swing.Icon icon = view.getSystemIcon(file);

        // OS X
        //final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        //javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);

        return icon;
    }

    public static Image getFileIcon(File file) {
        String ext = Util.getSuffix(file.getPath()).toLowerCase();

	    // Handle windows shortcut files (we need to resolve the target file)
        if("lnk".equals(ext))
            return WindowsShortcut.targetedFile(file).map(IconExtractor::getFileIcon).orElse(null);

	    // Handle windows executable files (we need to handle each individually)
        String key = "exe".equals(ext) ? Util.getName(file) : ext;

        return mapOfFileExtToSmallIcon.computeIfAbsent(key, k -> {
            javax.swing.Icon jswingIcon = null;
            if (file.exists()) {
                jswingIcon = getJSwingIconFromFileSystem(file);
            } else {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("icon", ext);
                    jswingIcon = getJSwingIconFromFileSystem(tempFile);
                } catch (IOException ignored) {
                    // Cannot create temporary file.
                } finally {
                    if (tempFile != null) tempFile.delete();
                }
            }
            return jswingIconToImage(jswingIcon);
        });
    }

    private static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
        BufferedImage image = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(), TYPE_INT_ARGB);
        jswingIcon.paintIcon(null, image.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(image, null);
    }



/************************* EXPERIMENTAL IMPLEMENTATION */

	private static final FileSystemView fs = FileSystemView.getFileSystemView();
	private static final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();

	private static final Image getIcon(File file) {
		javax.swing.Icon i = fc.getUI().getFileView(fc).getIcon(file);
		return i==null ? null : imageSwingToFx(i);

		//        ImageIcon i = getLargeIcon(file);
		//        return i==null ? null : imageAwtToFx(i.getImage());
	}

	private static final void getIcon(File file, Consumer<Image> action) {
		//        javax.swing.Icon i = fc.getUI().getFileView(fc).getIcon(file);
		//        return i==null ? null : imageSwingToFx(i);

		javax.swing.Icon i = fc.getUI().getFileView(fc).getIcon(file);
		if(i==null) action.accept(null);
		else imageSwingToFx(i, action);


		//        ImageIcon i = getLargeIcon(file);
		//        return i==null ? null : imageAwtToFx(i.getImage());
	}

	private static ImageIcon getLargeIcon(File file) {
		try {
			if(file==null) throw new FileNotFoundException("File is null");
			ShellFolder sf = ShellFolder.getShellFolder(file);
			return new ImageIcon(sf.getIcon(true), sf.getFolderType());
		} catch (FileNotFoundException e) {
			util.dev.Util.log(AppLauncher.class).warn("Could not load icon for {}", file);
			return null;
		}
	}

	private static Image imageAwtToFx(java.awt.Image awtImage) {
		if(awtImage==null) return null;
		BufferedImage bimg;
		if (awtImage instanceof BufferedImage) {
			bimg = (BufferedImage) awtImage ;
		} else {
			bimg = new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = bimg.createGraphics();
			graphics.drawImage(awtImage, 0, 0, null);
			graphics.dispose();
		}
		return SwingFXUtils.toFXImage(bimg, null);
	}

	private static void imageAwtToFx(java.awt.Image awtImage, Consumer<Image> then) {
		if(awtImage==null) {
			then.accept(null);
			return;
		}
		BufferedImage bimg;
		if (awtImage instanceof BufferedImage) {
			bimg = (BufferedImage) awtImage ;
		} else {
			bimg = new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = bimg.createGraphics();
			graphics.drawImage(awtImage, 0, 0, null);
			graphics.dispose();
		}
		runFX(() -> then.accept(SwingFXUtils.toFXImage(bimg, null)));
	}

	private static Image imageSwingToFx(javax.swing.Icon swingIcon) {
		if(swingIcon==null) return null;
		BufferedImage bimg = new BufferedImage(swingIcon.getIconWidth(),swingIcon.getIconHeight(),BufferedImage.TYPE_INT_ARGB);
		swingIcon.paintIcon(null, bimg.getGraphics(), 0, 0);
		return SwingFXUtils.toFXImage(bimg, null);
	}

	private static void imageSwingToFx(javax.swing.Icon swingIcon, Consumer<Image> then) {
		if(swingIcon==null) {
			then.accept(null);
			return;
		}
		BufferedImage bimg = new BufferedImage(swingIcon.getIconWidth(),swingIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		swingIcon.paintIcon(null, bimg.getGraphics(), 0, 0);
		runFX(() -> then.accept(SwingFXUtils.toFXImage(bimg, null)));
	}

}