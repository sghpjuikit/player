package fileDownloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import gui.itemnode.ConfigField;
import gui.objects.icon.Icon;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import main.App;
import util.access.V;
import util.conf.Config;
import util.conf.IsConfig;
import util.file.Environment;
import util.file.Util;
import util.graphics.drag.DragUtil;

import static javafx.geometry.Pos.CENTER;
import static main.App.APP;
import static util.async.Async.newSingleDaemonThreadExecutor;
import static util.async.future.Fut.fut;
import static util.graphics.Util.*;

/**
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "FileDownloader",
    description = "Displays real time audio spectrum of playback",
    howto = "",
    notes = "",
    version = "0.5",
    year = "2015",
    group = Widget.Group.VISUALISATION
)
public class FileDownloader extends ClassController  {

    @IsConfig(name = "Download directory", info = "Destination for the files")
    private final V<File> downloadDir = new V<>(APP.DIR_TEMP);
    private final Config<File> downloadDirConfig = Config.forProperty(File.class, "Download directory", downloadDir);
    private final ProgressIndicator progress = App.Build.appProgressIndicator();
    private final ExecutorService downloader = newSingleDaemonThreadExecutor();

    public FileDownloader() {
        progress.setProgress(1);
        setAnchor(
            this,
            layHeaderTop(10, CENTER,
                layHorizontally(10, CENTER,
                    progress,
                    new Icon<>(MaterialIcon.GET_APP)
                            .tooltip("Open download directory")
                            .onClick(() -> Environment.open(downloadDir.get())),
                    ConfigField.create(downloadDirConfig).getNode()
                ),
                new Pane()
            ),
            0d
        );

        // drag&drop
        DragUtil.installDrag(
            this, FontAwesomeIcon.FILE_ALT, "Add to download queue",
            e -> e.getDragboard().hasUrl() || e.getDragboard().hasFiles(),
            e -> {
                if (e.getDragboard().hasFiles())
                    e.getDragboard().getFiles().forEach(this::process);
                else if (e.getDragboard().hasUrl())
                    process(e.getDragboard().getUrl());
            }
        );

    }

    private void process(File f) {
        try {
            process(f.toURI().toURL().toString());
        } catch (MalformedURLException ex) {
                        ex.printStackTrace();
        }
    }

    private void process(String url) {
        if (url!=null) {
            File dest = downloadDir.get();
            fut().then(() -> {
                    try {
                        Util.saveFileTo(url, dest);
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }, downloader)
            .showProgress(progress);
        }
    }

}