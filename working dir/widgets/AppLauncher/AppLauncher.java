package AppLauncher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import layout.widget.Widget;
import layout.widget.controller.ClassController;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.grid.GridCell;
import gui.objects.grid.GridView;
import gui.objects.hierarchy.Item;
import gui.objects.image.Thumbnail;
import main.IconExtractor;
import sun.awt.shell.ShellFolder;
import unused.TriConsumer;
import util.Sort;
import util.SwitchException;
import util.access.fieldvalue.FileField;
import util.access.V;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.dev.Util;
import util.file.Environment;
import util.file.FileSort;
import util.file.FileType;
import util.graphics.drag.DragUtil;
import util.graphics.drag.Placeholder;

import static AppLauncher.AppLauncher.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static AppLauncher.AppLauncher.CellSize.NORMAL;
import static javafx.scene.input.MouseButton.SECONDARY;
import static layout.widget.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.access.fieldvalue.FileField.NAME;
import static util.async.Async.*;
import static util.file.Environment.chooseFile;
import static util.file.FileSort.DIR_FIRST;
import static util.file.FileType.FILE;
import static util.file.Util.getName;
import static util.functional.Util.by;
import static util.graphics.Util.setAnchor;
import static util.graphics.drag.DragUtil.installDrag;

@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "AppLauncher",
    description = "Launches programs",
    howto = "",
    notes = "",
    version = "1",
    year = "2016",
    group = OTHER
)
public class AppLauncher extends ClassController {

    private static final double CELL_TEXT_HEIGHT = 20;

    @IsConfig(name = "Location", info = "Add program")
    final VarList<File> files = new VarList<>(() -> new File("X:\\"),f -> Config.forValue(File.class,"File",f));

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, NORMAL.width,NORMAL.height,5,5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    private final ExecutorService executorImage = newSingleDaemonThreadExecutor(); // 2 threads perform better, but cause bugs
    boolean initialized = false;
    private volatile boolean isResizing = false;
    private volatile long visitId = 0;
    private final Placeholder placeholder = new Placeholder(
        FOLDER_PLUS, "Click to add launcher or drag & drop a file",
        () -> {
            File dir = chooseFile("Choose program or file", FILE, APP.DIR_HOME, APP.windowOwner.getStage());
            if(dir!=null) files.list.add(dir);
        }
    );

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, s -> s.apply(grid));
    @IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::resort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::resort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final V<FileField> sortBy = new V<>(NAME, this::resort);
    @IsConfig(name = "Close on launch", info = "Close this widget when it launches a program.")
    final V<Boolean> closeOnLaunch = new V<>(false);
    @IsConfig(name = "Close on right click", info = "Close this widget when right click is detected.")
    final V<Boolean> closeOnRightClick = new V<>(false);

    public AppLauncher() {
        setPrefSize(500,500);

        files.onListInvalid(list -> visit());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.setCellFactory(grid -> new Cell());
        setAnchor(this,grid,0d);
        placeholder.showFor(this);

        // delay cell loading when content is being resized (increases resize performance)
        double delay = 200; // ms
        FxTimer resizeTimer = new FxTimer(delay, 1, () -> isResizing = false);
        grid.widthProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.heightProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.widthProperty().addListener((o,ov,nv) -> resizeTimer.start(300));
        grid.heightProperty().addListener((o,ov,nv) -> resizeTimer.start(300));
        grid.setOnKeyPressed(e -> {
            if(e.getCode()==ENTER) {
                Item si = grid.selectedItem.get();
                if(si!=null) doubleClickItem(si);
            }
        });
        grid.setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY && closeOnRightClick.get())
                widget.areaTemp.close();
        });
        setOnScroll(Event::consume);

        installDrag(
            this, FontAwesomeIcon.PLUS_SQUARE_ALT, "Add launcher",
            DragUtil::hasFiles,
            e -> files.list.addAll(DragUtil.getFiles(e))
        );
    }

    @Override
    public void refresh() {
        initialized = true;
        cellSize.applyValue();
        visit();
    }

    private void visit() {
        if(!initialized) return;
        Item item = new TopItem();
//        item.last_gridposition = grid.implGetSkin().getFlow().getPosition(); // can cause nullpointer here
        visitId++;
        if(item==null) {
            grid.getItemsRaw().clear();
            grid.implGetSkin().getFlow().requestFocus(); // fixes focus problem
        } if(item!=null) {
            Fut.fut(item)
                    .map(Item::children,executorIO)
                    .use(newcells -> newcells.sort(buildSortComparator()),executorIO)
                    .use(newcells -> {
                        grid.getItemsRaw().setAll(newcells);
                        if(item.last_gridposition>=0)
                            grid.implGetSkin().getFlow().setPosition(item.last_gridposition);
                        grid.implGetSkin().getFlow().requestFocus(); // fixes focus problem
                    },FX)
                    .run();
        }
    }

    private void doubleClickItem(Item i) {
        if(closeOnLaunch.get()) {
            widget.areaTemp.close();
            run(250, () -> Environment.open(i.val));
        } else {
            Environment.open(i.val);
        }
    }
    private static FileSystemView fileUtils = FileSystemView.getFileSystemView();
    /** Resorts grid's items according to current sort criteria. */
    private void resort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    private Comparator<Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort, // sorts Files to files and directories
                sortHomo = sort.get(); // sorts each group separately
        FileField field = sortBy.get(); // precompute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.cmp(by(i -> i.valtype)),
                cmpHomo = sortHomo.cmp(by(i -> i.val, field.comparator()));
        return cmpHetero.thenComparing(cmpHomo);
    }

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
            Util.log(AppLauncher.class).warn("Couldnt load icon for {}", file);
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
        BufferedImage bimg = new BufferedImage(
                swingIcon.getIconWidth(),
                swingIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        swingIcon.paintIcon(null, bimg.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(bimg, null);
    }

    private static void imageSwingToFx(javax.swing.Icon swingIcon, Consumer<Image> then) {
        if(swingIcon==null) {
            then.accept(null);
            return;
        }
        BufferedImage bimg = new BufferedImage(
                swingIcon.getIconWidth(),
                swingIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        swingIcon.paintIcon(null, bimg.getGraphics(), 0, 0);
        runFX(() -> then.accept(SwingFXUtils.toFXImage(bimg, null)));
    }

    /**
     * Graphics representing the file. Cells are virtualized just like ListView or TableView does
     * it, but both vertically & horizontally. This avoids loading all files at once and allows
     * unlimited scaling.
     */
    private class Cell extends GridCell<Item,File> {
        Pane root;
        Label name;
        Thumbnail thumb;
        EventReducer<Item> setCoverLater = EventReducer.toLast(500, item -> executorThumbs.execute(task(() -> {
            sleep(10); // gives FX thread some space to avoid lag under intense workload
            runFX(() -> {
                if(item==getItem())
                    setCover(item);
            });
        })));

        @Override
        protected void updateItem(Item item, boolean empty) {
            super.updateItem(item, empty);

            if(item==null) {
                // empty cell has no graphics
                // we do not clear the content of the graphics however
                setGraphic(null);
            } else {
                if(root==null) {
                    // we create graphics only once and only when frst requested
                    createGraphics();
                    // we set graphics only once (when creating it)
                    setGraphic(root);
                }
                // if cell was previously empty, we set graphics back
                // this improves performance compared to setting it every time
                if(getGraphic()!=root) setGraphic(root);

                // set name
                name.setText(getName(item.val));
                // set cover
                // The item caches the cover Image, so it is only loaded once. That is a heavy op.
                // This method can be called very frequently and:
                //     - block ui thread when scrolling
                //     - reduce ui performance when resizing
                // We solve this by delaying the image loading & drawing. We reduce subsequent
                // invokes into single update (last).
                if(item.cover_loadedFull && !isResizing)
                    setCover(item);
                else {
                    thumb.loadImage((File)null); // prevent displaying old content before cover loads
                    // this should call setCoverPost() to allow animations
                    setCoverLater.push(item);
                }
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if(thumb!=null && thumb.image.get()!=null) thumb.animationPlayPause(selected);
        }

        private void createGraphics() {
            name = new Label();
            name.setAlignment(Pos.CENTER);
            thumb = new Thumbnail() {
                @Override
                protected Object getRepresentant() {
                    return getItem()==null ? null : getItem().val;
                }
            };
            thumb.getPane().setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                    doubleClickItem(getItem());
                    e.consume();
                }
            });
            thumb.getPane().hoverProperty().addListener((o,ov,nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0,0,0.2,0) : null));
            root = new Pane(thumb.getPane(),name) {
                // Cell layouting should be fast - gets called multiple times when grid resizes.
                // Why not use custom pane for more speed if we can.
                @Override
                protected void layoutChildren() {
                    double w = getWidth();
                    double h = getHeight();
                    thumb.getPane().resizeRelocate(0,0,w,h-CELL_TEXT_HEIGHT);
                    name.resizeRelocate(0,h-CELL_TEXT_HEIGHT,w,CELL_TEXT_HEIGHT);
                }
            };
            root.setPadding(Insets.EMPTY);
            root.setMinSize(-1,-1);
            root.setPrefSize(-1,-1);
            root.setMaxSize(-1,-1);
        }

        /**
         * Begins loading cover for the item. If item changes meanwhile, the result is stored
         * (it will not need to load again) to the old item, but not showed.
         * <p/>
         * Thumbnail quality may be decreased to achieve good performance, while loading high
         * quality thumbnail in the bgr. Each phase uses its own executor.
         */
        private void setCover(Item item) {
            // load thumbnail
            double width = cellSize.get().width,
                   height = cellSize.get().height-CELL_TEXT_HEIGHT;
//            executorThumbs.execute(task(() ->
                    item.loadCover(false,width,height, (was_loaded,file,img) -> setCoverPost(item,was_loaded,file,img));
//            ));
        }

        private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if(item==getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this,imgAlreadyLoaded,imgFile,img);
                    thumb.loadImage(img, imgFile);
                    if(animate)
                        new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
                }
            });
        }
    }
    private class FItem extends Item {

        public FItem(Item parent, File value, FileType type) {
            super(parent, value, type);
        }

        @Override
        protected FItem createItem(Item parent, File value, FileType type) {
            return new FItem(parent, value, type);
        }

        @Override
        public void loadCover(boolean full, double width, double height, TriConsumer<Boolean, File, Image> action) {
            if(!cover_loadedFull) {
                cover_loadedFull = true;
                cover_loadedThumb = true;
//                cover = getIcon(val);
                cover = IconExtractor.getFileIcon(val);
                action.accept(false, null, cover);
            }
        }
    }
    private class TopItem extends FItem {

        public TopItem() {
            super(null,null,null);
        }

        @Override
        protected Stream<File> children_files() {
            return files.list.stream();
        }

        @Override
        protected File getCoverFile() {
            return null;
        }
    }


    private Runnable task(Runnable r) {
        final long id = visitId;
        return () -> {
            if(id==visitId)
                r.run();
        };
    }

    enum CellSize {
        SMALL (80, 100+CELL_TEXT_HEIGHT),
        NORMAL(160,200+CELL_TEXT_HEIGHT),
        LARGE (240,300+CELL_TEXT_HEIGHT),
        GIANT (400,500+CELL_TEXT_HEIGHT);

        final double width;
        final double height;

        CellSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        void apply(GridView<?,?> grid) {
            grid.setCellWidth(width);
            grid.setCellHeight(height);
        }
    }
    enum AnimateOn {
        IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

        public boolean needsAnimation(Cell cell, boolean imgAlreadyLoaded, File imgFile, Image img) {
            if(this== IMAGE_CHANGE) return cell.thumb.image.get()!=img;
            else if(this==IMAGE_CHANGE_FROM_EMPTY) return cell.thumb.image.get()==null && img!=null;
            else if(this==IMAGE_CHANGE_1ST_TIME) return !imgAlreadyLoaded && img!=null;
            else throw new SwitchException(this);
        }
    }
}