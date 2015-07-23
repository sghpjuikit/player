/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unused.hierarchy;

import gui.objects.icon.Icon;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import static java.nio.file.Files.list;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.event.Event;
import static javafx.geometry.Orientation.HORIZONTAL;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import static util.collections.Tuples.tuple;

/**
 <p>
 @author Plutonium_
 */
public class FileHierarchy extends HierarchyView<File> {

    public FileHierarchy() {
        hasUp = f -> f.getParentFile()!=null;
        hasDown = f -> {
            boolean d = f.isDirectory();
            if(d){
                try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(f.toPath())) {
                    return dirStream.iterator().hasNext();
                } catch (IOException ex) {
                    Logger.getLogger(FileHierarchy.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            } else return false;
        };
        childrenSupplier = f -> {
            boolean d = f.isDirectory();
            if(d){
                try {
                    return list(f.toPath()).map(p->p.toFile());
                } catch (IOException ex) {
                    Logger.getLogger(FileHierarchy.class.getName()).log(Level.SEVERE, null, ex);
                    return Stream.empty();
                }
            } else return Stream.empty();
            
        };
        layoutFactory = () -> {
            TilePane tp = new TilePane();
                     tp.setOrientation(HORIZONTAL);
                     tp.setPrefColumns(5);
                     tp.setPrefTileHeight(60);
                     tp.setPrefTileWidth(120);
            ScrollPane sp = new ScrollPane(tp);
                       sp.setContent(tp);
//                       sp.setMinSize(-1, -1);
//                       sp.setPrefSize(555, 555);
//                       sp.setMaxSize(-1, -1);
                       sp.setFitToHeight(false);
                       sp.setFitToWidth(true);
                       sp.setOnScroll(Event::consume);
            StackPane s = new StackPane(new Group(sp));
            return tuple(s,tp);
        };
        graphicFactory = f -> {
            boolean d = f.isDirectory();
            Icon i = new Icon(d ? FOLDER : FILE, 35);
            i.setOnMouseEntered(e -> {i.setScaleX(1.1);i.setScaleY(1.1);});
            i.setOnMouseExited(e -> {i.setScaleX(1);i.setScaleY(1);});
            VBox l = new VBox(5, i,new Label(f.getName()));
                 l.setAlignment(Pos.CENTER);
            return l;
        };
        up = f -> f.getParentFile();
    }
}
