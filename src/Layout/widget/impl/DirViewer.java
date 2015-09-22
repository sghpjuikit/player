/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import Configuration.Config;
import Configuration.Config.VarList;
import Configuration.IsConfig;
import Layout.widget.IsWidget;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import gui.objects.Window.stage.Window;
import gui.objects.image.Thumbnail;
import gui.pane.CellPane;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.access.Ѵ;
import util.animation.Anim;
import util.async.future.Fut;

import static Layout.widget.Widget.Group.OTHER;
import static java.lang.Character.isAlphabetic;
import static java.lang.Math.sqrt;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static util.File.FileUtil.getName;
import static util.File.FileUtil.listFiles;
import static util.async.Async.newSingleDaemonThreadExecutor;
import static util.async.Async.runFX;
import static util.dev.Util.log;
import static util.functional.Util.*;
import static util.graphics.Util.layAnchor;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Dir Viewer",
    description = "Displays directory hierarchy and files as thumbnails in a "
            + "vertically scrollable grid. Intended as simple library",
    howto = "",
    notes = "",
    version = "0.5",
    year = "2015",
    group = OTHER
)
public class DirViewer extends ClassController {

    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue("File",f));
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final Ѵ<FFilter> filter = new Ѵ<>(FFilter.ALL, f -> viewDir(new TopCell()));

    Cell item = null;
    CellPane cells = new CellPane(160,220,5);
    ExecutorService executor = newSingleDaemonThreadExecutor();
    boolean initialized = false;

    public DirViewer() {
        files.onListInvalid(list -> viewDir(new TopCell()));

        layAnchor(this,cells.scrollable(),0d);

        setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY)
                visitUp();
        });
        setOnKeyPressed(e -> {
            if(e.getCode()==BACK_SPACE)
                visitUp();
        });
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        initialized = true;
        viewDir(new TopCell());
    }

    @Override
    public void onClose() {
        loading++;
        super.onClose();
    }

    void visitUp() {
        if(item!=null && item.parent!=null)
            viewDir(item.parent);
    }

    long loading = 0; // allows canceling of lloading, still think it should be handled more natively
    public void viewDir(Cell dir) {
        if(!initialized) return; // prevents pointless & inconsistent operations

        loading++;
        item = dir;
        if(item==null) {
            cells.getChildren().clear();
        } if(item!=null) {
            long l = loading;
            Fut.fut(item)
               .map(Cell::children,executor)
               // old implementation, which can overload ui thread
               // .use(newcells ->  cells.getChildren().addAll(map(newcells,Cell::load)),FX)
               .use(newcells ->  {
                   runFX(cells.getChildren()::clear);
                   log(DirViewer.class).info("Cells loading {} started ", l);
                   List<Cell> sorted = list(newcells);
                              sorted.sort(by(c -> c.val.getName()));
                   forEachAfter(10, sorted, c -> {
                       if(l!=loading) throw new InterruptedException();
                       Node n = c.load();
                       runFX(() -> {
                           cells.getChildren().add(n);
                           new Anim(n::setOpacity).dur(500).intpl(x -> sqrt(x)).play(); // animate
                       });
                   });
                   log(DirViewer.class).info("Cells loading {} finished", l);
                },executor)
               .showProgress(Window.windows.get(0).taskAdd())
               .run();
        }
    }

    private boolean filter(File f) {
        return !f.isHidden() && f.canRead() && filter.getValue().filter.test(f);
    }
    private static boolean file_exists(Cell c, File f) {
        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
    }
    public class Cell {

        public final File val;
        public final Cell parent;
        private Set<Cell> children = null;
        private Set<String> all_children = null; // cache

        public Cell(Cell parent, File value) {
            this.val = value;
            this.parent = parent;
        }

        public Set<Cell> children() {
            if (children == null) buildChildren();
            return children;
        }

        boolean isFirstTimeCover = true;
        Image cover = null;
        public File getCoverFile() {

            if(all_children==null) System.out.println("children null " + val);
            if(all_children==null) buildChildren();
            if(val.isDirectory())
                return getImageT(val,"cover");
            else {
                if(ImageFileFormat.isSupported(val))
                    return val;
                else {
                    File i = getImage(val.getParentFile(),FileUtil.getName(val));
                    if(i==null && parent!=null) return parent.getCoverFile();
                    return i;
                }
            }
        }
        public Image getCover() {
            return cover;
        }
        public void setCover(Image i) {
            cover = i;
            isFirstTimeCover=false;
        }

        protected Stream<File> children_files() {
            return listFiles(val).stream();
        }

        private void buildChildren() {
            all_children = new HashSet<>();
            children = new HashSet<>();
            List<Cell> fils = new ArrayList<>();
            children_files().forEach(f -> {
                all_children.add(f.getPath().toLowerCase());
                if(DirViewer.this.filter(f)) {
                    if(!f.isDirectory()) children.add(new Cell(this,f));
                    else                 fils.add(new Cell(this,f));
                }
            });
            children.addAll(fils);
        }

        private File getImage(File dir, String name) {
            if(dir==null) return null;
            for(ImageFileFormat format: ImageFileFormat.values()) {
                if (format.isSupported()) {
                    File f = new File(dir,name + "." + format.toString());
                    if(dir==val ? file_exists(this,f) : file_exists(parent,f)) return f;
                }
            }
            return null;
        }
        private File getImageT(File dir, String name) {
            if(dir==null) return null;

            for(ImageFileFormat format: ImageFileFormat.values()) {
                if (format.isSupported()) {
                    File f = new File(dir,name + "." + format.toString());
                    System.out.println(file_exists(this,f) + " " + f + " " +val);
                    if(file_exists(this,f)) return f;
                }
            }
            return null;
        }

        private VBox root;

        public Node load() {
            if(root==null) {
                File f = val;
                root = new VBox();
                root.setPrefSize(160,220);

                Thumbnail t = new Thumbnail(160,200);
                if(isFirstTimeCover) {
                    File cf = getCoverFile();
                    t.image.addListener((o,ov,nv) -> setCover(nv));
                    t.loadImage(cf!=null && cf.exists() ? cf : null); // the exists() check may need to be in Thumbnail itself
                } else {
                    t.loadImage(getCover());
                }
                t.getPane().setOnMouseClicked(e -> {
                    if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                        if(f.isDirectory()) viewDir(this);
                        else Environment.open(f);
                        e.consume();
                    }
                });

                String n = getName(f);
                if(n.length()>25) {
                   n = n.replaceAll("\\[.*\\]", "")
                        .replaceAll("\\(.*\\)", "")
                        .trim();
                }
                if(n.length()>25) {
                   n = f.isDirectory() ? n : toS(split(n," "),
                       s -> s.length()<=1 || !isAlphabetic(s.charAt(0)) ? s : s.substring(0,1),""
                   );
                }
                Label l = new Label(n);
                root.getChildren().addAll(t.getPane(),l);

                root.setAlignment(Pos.CENTER);
            }
            return root;
        }
    }
    public class TopCell extends Cell {

        public TopCell() {
            super(null,null);
        }

        @Override
        protected Stream<File> children_files() {
            return files.list.stream().flatMap(f -> f.isDirectory() ? stream(listFiles(f)) : stream());
        }

        @Override
        public File getCoverFile() {
            return null;
        }

    }
    public static enum FFilter {
        ALL(IS),
        AUDIO(f -> AudioFileFormat.isSupported(f, Use.APP)),
        IMAGE(ImageFileFormat::isSupported),
        NO_IMAGE(f -> !ImageFileFormat.isSupported(f));

        public final Predicate<? super File> filter;

        FFilter(Predicate<? super File> f) {
            filter = f;
        }
    }
}