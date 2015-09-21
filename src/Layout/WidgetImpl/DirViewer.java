/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.ClassController;
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

import static Layout.Widgets.Widget.Group.OTHER;
import static java.lang.Character.isAlphabetic;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toList;
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
                   forEachAfter(10, newcells, c -> {
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

    public class Cell {

        public final File val;
        public final Cell parent;

        private boolean isLeaf;
        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private final List<Cell> childr = new ArrayList();

        public Cell(Cell parent, File value) {
            this.val = value;
            this.parent = parent;
        }

        public List<Cell> children() {
            if (isFirstTimeChildren) {
                childr.clear();
                childr.addAll(buildChildren());
                isFirstTimeChildren = false;
            }
            return childr;
        }

        boolean isFirstTimeCover = true;
        Image cover = null;
        public File getCoverFile() {
            File f = val;
            File i = f.isDirectory() ? new File(f,"cover.jpg") : filImage(f);
            return i;
        }
        public Image getCover() {
            return cover;
        }
        public void setCover(Image i) {
            cover = i;
            isFirstTimeCover=false;
        }

        private File filImage(File f) {
            File i = new File(f.getParent(),FileUtil.getName(f)+".jpg");
            if(!i.exists()) return parent.getCoverFile();
            else return i;
        }

        protected List<Cell> buildChildren() {
            // we want to sort the items : directories first
            // we make use of the fact that listFiles() gives us already
            // sorted list
            List<Cell> dirs = new ArrayList<>();
            List<Cell> fils = new ArrayList<>();
            listFiles(val).stream().filter(DirViewer.this::filter).forEach(f -> {
                if(!f.isDirectory()) dirs.add(new Cell(this,f));
                else                 fils.add(new Cell(this,f));
            });
                   dirs.addAll(fils);
            return dirs;
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
        protected List<Cell> buildChildren() {
            return files.list.stream()
                      .flatMap(f->f.isDirectory() ? stream(listFiles(f)) : Stream.empty())
                      .filter(DirViewer.this::filter)
                      .sorted(by(File::getName))
                      .map(f -> new Cell(this,f))
                      .collect(toList());
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