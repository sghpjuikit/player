package gameLib;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.Interpolator;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import gui.Gui;
import gui.objects.icon.Icon;
import gui.objects.image.Thumbnail;
import gui.objects.image.cover.Cover;
import gui.objects.image.cover.FileCover;
import gui.objects.tree.FileTree;
import gui.objects.tree.TreeItems;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import util.SwitchException;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.Async;
import util.async.executor.FxTimer;
import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.file.Environment;
import util.file.ImageFileFormat;
import util.file.Properties;
import util.functional.Functors.Ƒ1;
import util.validation.Constraint;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAMEPAD;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.WIKIPEDIA;
import static java.util.stream.Collectors.joining;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.SelectionMode.SINGLE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import static javafx.scene.text.TextAlignment.JUSTIFY;
import static javafx.util.Duration.millis;
import static layout.widget.Widget.Group.OTHER;
import static util.animation.Anim.Applier.typeText;
import static util.animation.Anim.par;
import static util.animation.Anim.seq;
import static util.async.Async.runFX;
import static util.validation.Constraint.FileActor.DIRECTORY;
import static util.file.Util.*;
import static util.functional.Util.by;

/**
 *
 * <p/>
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    name = "GameLib",
    description = "Simple game manager.",
    howto = "",
    notes = "",
    version = "0.5",
    year = "2015",
    group = OTHER
)
public class GameLib extends FXMLController {

    @FXML AnchorPane root;
    @FXML StackPane cover_root;
    @FXML ListView<GameItem> game_list;
    @FXML StackPane file_tree_root;
    @FXML TreeView<File> file_tree;
    @FXML StackPane inforootroot;
    @FXML StackPane inforoot;
    @FXML Text info_text;
    @FXML HBox controls;
    final Thumbnail cover = new Thumbnail();
    final TreeView<Object> fh = new TreeView<>();


    @FXML StackPane inner;
    @FXML StackPane outer;
    @FXML Label titleL;
    @FXML Label infoL;
    FxTimer infoLHider = new FxTimer(7000, 1, () -> infoL.setText(null));

	@Constraint.FileType(DIRECTORY)
    @IsConfig(name = "Location", info = "Location of the library.")
    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue(File.class,"File",f));

    GameItem game;
    InfoType at;
    ProgressIndicator gameOpening_progressI; // we reuse same indicator

    public void openGame(GameItem g) {
        if (game!=null && game.equals(g)) return;
        game = g;
        if (g==null) return;

        titleL.setText(g.getName());
        info_text.toFront();
        info_text.setTextAlignment(JUSTIFY);
//        info_text.getChildren().clear();

        info_text.setText("Play");
        if (gameOpening_progressI==null) gameOpening_progressI = getWidget().getWindow().taskAdd();
        gameOpening_progressI.setProgress(-1);
        Async.runNew(() -> {
            File f = new File(g.getLocation(),"play-howto.txt");
            String s = !f.exists() ? "" : readFileLines(f).collect(joining("\n"));
            Cover cv = g.getCover();
            g.loadMetadata();
            if (g==game) runFX(() -> {
                cover.loadImage(cv);
                info_text.setText("Play\n\n" + s);
                gameOpening_progressI.setProgress(1);
            });
        });
        goTo(InfoType.PLAY);

        // animate
            // init values
        inforoot.setMaxSize(USE_PREF_SIZE,USE_PREF_SIZE);
        inforoot.setMinSize(USE_PREF_SIZE,USE_PREF_SIZE);
        inforoot.setPrefSize(50, 50);
            // play
        int name_len = g.getName().length();
        Interpolator i = new ElasticInterpolator();
        Ƒ1<Double,String> ti = typeText(g.getName());
        par(
            seq(
                new Anim(millis(450), i, at -> inforoot.setPrefSize(300*at,50)),
                new Anim(millis(550), i, at -> inforoot.setPrefSize(300,50+(inforootroot.getHeight()-50)*at))
            ),
            new Anim(millis(name_len*30), at -> titleL.setText(ti.apply(at)))
        ).play();
    }

    @Override
    public void init() {
        loadSkin("skin.css",root);

        fh.getSelectionModel().setSelectionMode(MULTIPLE);
        fh.setCellFactory(TreeItems::buildTreeCell);
        fh.setShowRoot(false);
        file_tree_root.getChildren().setAll(fh);

        game_list.setCellFactory(listview -> new ListCell<>(){
            @Override
            protected void updateItem(GameItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });
        game_list.getSelectionModel().setSelectionMode(SINGLE);
        game_list.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> openGame(nv));
        game_list.setOnScroll(Event::consume);

        cover.setBackgroundVisible(false);
        cover.setBorderVisible(false);
        cover.setContextMenuOn(false);
        cover_root.getChildren().add(cover.getPane());

        inforoot.setVisible(false);
        cover_root.addEventFilter(MOUSE_RELEASED, e -> {
            if (e.getButton()==SECONDARY)
                inforoot.setVisible(!inforoot.isVisible());
        });

        Icon playB = new Icon(GAMEPAD, 40, null, () -> {
            if (at!=InfoType.PLAY) goTo(InfoType.PLAY);
            else {
                String s = game.play();
                infoL.setText(s);
                infoLHider.start();
            }
        });
        Icon exploreB = new Icon(FOLDER, 40, null, () -> {
            if (at!=InfoType.EXPLORER) goTo(InfoType.EXPLORER);
            else Environment.browse(game.getLocation());
        });
        Icon wikiB = new Icon(WIKIPEDIA, 40, null, () -> {
            if (game!=null) {
                Environment.browse(new web.WikipediaQBuilder().apply(game.getName()));
                // in-widget browser
                // WebView w = new javafx.scene.web.WebView();
                // file_tree_root.getChildren().add(w);
                // file_tree_root.setVisible(true);
                // w.getEngine().load(new web.WikipediaQBuilder().apply(game.getName()).toString());
            }
        });

        info_text.setWrappingWidth(300-30-16);
        controls.setSpacing(17);
        controls.getChildren().addAll(playB,exploreB,wikiB);


        file_tree.setShowRoot(false);
        file_tree.setFixedCellSize(Gui.font.getValue().getSize() + 5);
        file_tree.getSelectionModel().setSelectionMode(SINGLE);
        FileTree.from(file_tree);
        file_tree.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> {
            if (nv.getValue()!=null && nv.getValue().isDirectory()) {
                File f = new File(nv.getValue(),"readme.txt");
                if (f.exists()) {
                    String s = readFileLines(f).collect(joining("\n"));
                    info_text.setText(getName(f) + "\n\n" + s);
                }
            }
        });

        files.onListInvalid(list -> loadGames());
    }

    @Override
    public void refresh() {
        loadGames();
    }

    @Override
    public void onClose() {}

    private void loadGames() {
        game_list.getItems().clear();
        listFiles(files.list.stream())
                .filter(f -> f.isDirectory() && !f.isHidden())
                .map(GameItem::new).sorted(by(GameItem::getName))
                .forEach(game_list.getItems()::add);
    }

    public void goTo(InfoType to) {
        if (game==null || at==to) return;

        switch(to) {
            case EXPLORER:  cover_root.setVisible(false);
                            file_tree_root.setVisible(true);
                            file_tree.setRoot(FileTree.createTreeItem(game.getLocation()));
                            fh.setRoot((TreeItem)FileTree.createTreeItem(game.getLocation()));
                            break;
            case PLAY:      cover_root.setVisible(true);
                            file_tree_root.setVisible(false);
                            break;
            default: throw new SwitchException(to);
        }
        at = to;
    }

    public enum InfoType {
        PLAY,
        EXPLORER
    }

    public static class GameItem {
        private String name;
        private File location;
        private boolean portable;
        private File installocation;
        private Map<String,String> settings;

        public GameItem(File f) {
            location = f.getAbsoluteFile();
            name = f.getName();
        }

        public String getName() {
            return name;
        }

        public File getLocation() {
            return location;
        }

        public Cover getCover() {
            File dir = getLocation();
            File cf = listFiles(dir).filter(f -> {
                String filename = f.getName();
                int i = filename.lastIndexOf('.');
                if (i == -1) return false;
                String name = filename.substring(0, i);
                return (ImageFileFormat.isSupported(f.toURI()) && name.equalsIgnoreCase("cover"));
            }).findFirst().orElse(null);

            return new FileCover(cf, "");
        }

        /**
         * @return the portable
         */
        public boolean isPortable() {
            return portable;
        }

        /**
         * @return the installocation
         */
        public File getInstallocation() {
            return installocation;
        }

        public File getExe() {
            return new File(location,"play.lnk");
        }

        public Map<String,String> loadMetadata() {
            if (settings==null) {
                File f = new File(location,"game.properties");
                settings = f.exists() ? Properties.load(f) : new HashMap<>();
            }
            return settings;
        }

        public String play() {
            loadMetadata();
            List<String> command = new ArrayList<>();

            try {
                File exe =null ;
                String pathA = settings.get("pathAbs");

                if (pathA!=null) {
                    exe = new File(pathA);
                }

                if (exe==null) {
                    String pathR = settings.get("path");
                    if (pathR==null) return "No path is set up.";
                    exe = new File(location,pathR);
                }

                // run this program
                command.add(exe.getAbsolutePath());

                // with optional parameter
                String arg = settings.get("arguments");
                if (arg!=null) {
                    arg = arg.replaceAll(", ", ",");
                    String[] args = arg.split(",",0);
                    for (String a : args) if (!a.isEmpty()) command.add("-" + a);
                }
                // run
                new ProcessBuilder(command).start();
                return "Starting...";
            } catch (IOException ex) {
                // we might have failed due to the program requiring elevation (run
                // as admin) so we use a little utility we package along
                try {
                    // use elevate.exe to run what we wanted
                    command.add(0, "elevate.exe");
                    new ProcessBuilder(command).start();
                    return "Starting (as administrator)...";
                } catch (IOException ex1) {
                    Logger.getLogger(GameItem.class.getName()).log(Level.SEVERE, null, ex1);
                    return ex.getMessage();
                }
            }

        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof GameItem && name.equals(((GameItem) obj).name));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.name);
            return hash;
        }
    }

    public static class RunParameter {
        String value;
        String description;
    }
}