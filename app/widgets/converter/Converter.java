package converter;

import de.jensd.fx.glyphs.octicons.OctIcon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.SongReadingKt;
import sp.it.pl.gui.itemnode.ChainValueNode.ListConfigField;
import sp.it.pl.gui.itemnode.ConfigField;
import sp.it.pl.gui.itemnode.ListAreaNode;
import sp.it.pl.gui.itemnode.ValueNode;
import sp.it.pl.gui.objects.combobox.ImprovedComboBox;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.pane.ConfigPane;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.Widget.Group;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.feature.Opener;
import sp.it.pl.layout.widget.feature.SongWriter;
import sp.it.pl.main.AppDragKt;
import sp.it.pl.main.Widgets;
import sp.it.util.access.V;
import sp.it.util.access.VarEnum;
import sp.it.util.collections.map.ClassListMap;
import sp.it.util.conf.Config;
import sp.it.util.file.Util;
import sp.it.util.functional.Functors.Ƒ0;
import sp.it.util.text.StringSplitParser.SplitData;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY_CIRCLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.pl.audio.tagging.SongWritingKt.writeNoRefresh;
import static sp.it.pl.main.AppDragKt.getAny;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.AppProgressKt.showAppProgress;
import static sp.it.util.Util.capitalizeStrong;
import static sp.it.util.Util.filenamizeString;
import static sp.it.util.async.future.Fut.fut;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.file.Util.writeFile;
import static sp.it.util.functional.Util.equalBy;
import static sp.it.util.functional.Util.filter;
import static sp.it.util.functional.Util.findFirstInt;
import static sp.it.util.functional.Util.forEachWithI;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.listRO;
import static sp.it.util.functional.Util.map;
import static sp.it.util.functional.Util.mapSlice;
import static sp.it.util.functional.Util.max;
import static sp.it.util.functional.Util.min;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.Util.streamBi;
import static sp.it.util.functional.Util.toS;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.ui.Util.layHorizontally;
import static sp.it.util.ui.Util.layStack;
import static sp.it.util.ui.Util.layVertically;

@SuppressWarnings({"WeakerAccess", "MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal", "unused"})
@Widget.Info(
    author = "Martin Polakovic",
    name = Widgets.CONVERTER,
    description = "Transformation utility. Capable of transforming objects "
        + "and chaining the transforming functions. Capable of text manipulation, file renaming and "
        + "audio tagging.",
    howto = ""
        + "\tUser can put text in an edit area and apply transformations on it "
        + "using available functions. The transformations are applied on each "
        + "line separately. It is possible to manually edit the text to fine-tune "
        + "the result.\n"
        + "\tThis is useful to edit multiple texts in the same way, e.g., to "
        + "edit filenames or song names. This is done using an 'applier' that "
        + "uses the 'output' and applies it no an input.\n"
        + "\tInput is a list of objects and can be set by drag&drop. It can "
        + "then be transformed to text (object per line) or other objects "
        + "using available functions, e.g., song to artist or file to filename.\n"
        + "\tOutput is the final contents of the text area exactly as "
        + "visible. Some transformations can produce multiple outputs "
        + "(again, per line), which produces separate text area for each output. "
        + "To discern different outputs, text areas have a name in their header.\n"
        + "\tAction is determined by the type of input. User can select which "
        + "output he wants to use. The action then applies the text to the "
        + "input, each line to its respective object (determined by order). "
        + "Number of objects and lines must match.\n"
        + "\n"
        + "Available appliers:\n"
        + "\tSave text to file - produce a text file with specified content\n"
        + "\tRename files - renames input files\n"
        + "\tTag songs - edits tag of song individually\n"
        + "\n"
        + "Available actions:\n"
        + "\tDrag&drop files : Sets files as input\n"
        + "\tDrag&drop songs : Sets songs as input\n"
        + "\tDrag&drop text : Sets text as input\n",
    version = "0.7.0",
    year = "2015",
    group = Group.APP
)
@LegacyController
public class Converter extends SimpleController implements Opener, SongWriter {

    public final Input<Object> inputValue;

    private final ObservableList<Object> source = observableArrayList();
    private final EditArea ta_in = new EditArea("In", true);
    private final ObservableList<EditArea> tas = observableArrayList(ta_in);
    private final ClassListMap<Act<?>> acts = new ClassListMap<>(act -> act.type);
    private final HBox outTFBox = new HBox(5);
    private final Applier applier = new Applier();
    private final HBox layout = new HBox(5,outTFBox, applier.root);

    public Converter(Widget widget) {
	    super(widget);
        root.setPrefSize(scaleEM(800), scaleEM(500));

        inputValue = io.i.create("Value", Object.class, null, consumer(v -> source.setAll(unpackData(v))));

        // layout
        HBox ll = new HBox(5, ta_in.getNode(),layout);
        HBox.setHgrow(ta_in.getNode(), ALWAYS);
        root.getChildren().add(ll);

        // drag&drop
        installDrag(
            root, LIST_ALT, () -> "Set data as input",
            e -> true,
            e -> false,
            consumer(e -> inputValue.setValue(getAny(e.getDragboard()))),
            e -> ta_in.getNode().getLayoutBounds()
        );

        tas.addListener((Change<? extends EditArea> change) ->
            outTFBox.getChildren().setAll(map(tas,EditArea::getNode))
        );

        // on source change run transformation
        source.addListener((Change<?> change) -> ta_in.setData(source));

        ta_in.onItemChange = lines -> {
            List<EditArea> l;
            List<EditArea> custom_tas = filter(tas,ta -> ta.name.get().contains("Custom"));
            if (!ta_in.output.isEmpty() && ta_in.output.get(0) instanceof SplitData) {
                List<SplitData> s = ta_in.output.stream().filter(SplitData.class::isInstance).map(SplitData.class::cast).collect(toList());
                List<String> names = s.get(0).stream().map(split -> split.parse_key).collect(toList());

                List<List<String>> outs = list(names.size(), ArrayList::new);
                s.forEach(splits -> forEachWithI(map(splits,split -> split.split), (i,line) -> outs.get(i).add(line)));

                List<EditArea> li = list(outs.size(), () -> new EditArea(""));
                forEachWithI(outs, (i, ls) -> li.get(i).setData(names.get(i),  ls));

                l = li;
                l.add(0, ta_in);
                l.addAll(custom_tas);
                tas.setAll(l);
            } else {
                l = custom_tas;
                l.add(0,ta_in);
                tas.setAll(l);
            }
        };

        // add actions
        // Note that the actions are looked up (per class) in the order they are inserted. Per each
        // class, the first added action will become 'default' and show up as selected when data is
        // set.
        // Therefore, the order below matters.
        acts.accumulate(new Act<>("Rename files", File.class, 1, list("Filename"), (file, data) -> {
            String name = data.get("Filename");
            Util.renameFileNoSuffix(file, name);
        }));
        acts.accumulate(new Act<>("Rename files (and extension)", File.class, 1, list("Filename"), (file, data) -> {
            String name = data.get("Filename");
            Util.renameFile(file, name);
        }));
        acts.accumulate(new Act<>("Edit song tags", Song.class, 100, () -> map(Metadata.Field.FIELDS, f -> f.name()), data -> {
            List<Song> songs = source.stream().filter(Song.class::isInstance).map(Song.class::cast).collect(toList());
            if (songs.isEmpty()) return;
            showAppProgress(
                fut()
                   .useBy(Player.IO_THREAD, it -> {
                        for (int i=0; i<songs.size(); i++) {
                            int j = i;
                            writeNoRefresh(songs.get(i), consumer(w -> data.forEach((field, values) -> w.setFieldS(Metadata.Field.valueOf(field), values.get(j)))));
                        }

                        Player.refreshSongsWith(stream(songs).map(SongReadingKt::read).filter(m -> !m.isEmpty()).collect(toList()));
                   }),
                widget.custom_name.getValue() + "Editing song tags"
            );
        }));
        acts.accumulate(new WriteFileAct());
        acts.accumulate(new ActCreateDirs());

        Output<String> output = io.o.create("Text", String.class, "");
        ta_in.outputText.addListener((o, ov, nv) -> output.setValue(nv));

        // set empty content
        applier.fillActs(Void.class);
    }

    @Override
    public void read(List<? extends Song> items) {
        inputValue.setValue(map(items, Song::toMeta));
    }

    @Override
    public void open(Object data) {
        inputValue.setValue(data);
    }

    @SuppressWarnings("unchecked")
    private static List<?> unpackData(Object o) {
        if (o instanceof String)
            return split((String) o, "\n", x->x);
        else if (o instanceof Collection)
            return list((Collection) o);
        else return listRO(o);
    }

/******************************* helper classes *******************************/

    /* Generates unique name in format 'CustomN', where N is integer. */
    String taName() {
        return "Custom" + findFirstInt(1, i -> tas.stream().noneMatch(ta -> ta.name.get().equals("Custom"+i)));
    }

    private class EditArea extends ListAreaNode {
        public final StringProperty name;
        public final boolean isMain;

        EditArea() {
            this(taName());
        }

        EditArea(String name) {
            this(name, false);
        }

        EditArea(String name, boolean isMain) {
            super();
            this.isMain = isMain;

            // graphics
            Label nameL = new Label("");
            Icon applyI = new Icon(OctIcon.DATABASE)
                    .tooltip("Set as input\n\nSet transformed (visible) data as input. The original data will be lost."
                            + (isMain ? "\n\nThis edit area is main, so the new input data will update the available actions." : ""))
                    .onClick(() -> setData(output)); // this will update applier if it is main
            Icon remI = new Icon(MINUS)
                    .tooltip("Remove\n\nRemove this edit area.")
                    .onClick(() -> tas.remove(this));
            Icon addI = new Icon(PLUS)
                    .tooltip("Add\n\nCreate new edit area with no data.")
                    .onClick(() -> tas.add(tas.indexOf(this)+1, new EditArea()));
            Icon copyI = new Icon(ANGLE_DOUBLE_RIGHT)
                    .tooltip("Copy data\n\nCopy transformed (visible) data into new edit area."
                            + "\n\nManual text changes will be ignored unless the type of transformation output is "
                            + "text. Use a transformation to text to achieve that."
                            + "")
                    .onClick(() -> {
                        EditArea t = new EditArea();
                        t.setData(output);
                        tas.add(tas.indexOf(this)+1,t);
                     });

            // layout
            getNode().getChildren().add(0,
                layStack(
                    nameL,Pos.CENTER,
                    layHorizontally(5,Pos.CENTER_RIGHT, applyI,new Label(),remI,addI,copyI),Pos.CENTER_RIGHT
                )
            );

            this.name = nameL.textProperty();
            remI.setDisable(isMain);   // disallow removing main edit area

            // drag & drop (main area is handled globally)
            if (!isMain) {
                installDrag(
                    getNode(), OctIcon.DATABASE, () -> "Set data to " + this.name.get() + " edit area",
                    e -> true,
                    consumer(e -> setData(unpackData(AppDragKt.getAny(e.getDragboard()))))
                );
            }

            textArea.addEventHandler(KEY_PRESSED, e -> {
                if (e.getCode()==KeyCode.V && e.isControlDown()) {
                    String pasted_text = Clipboard.getSystemClipboard().getString();
                    if (pasted_text!=null) {
                        String[] areaLines = textArea.getText().split("\\n");
                        String[] pastedLines = pasted_text.split("\\n");
                        String text;
                        int min = min(areaLines.length,pastedLines.length);
                        int max = max(areaLines.length,pastedLines.length);
                        if (min==max) {
                            text = streamBi(areaLines,pastedLines, (a,p) -> a+p).collect(joining("\n"));
                        } else {
                            // not implemented
                            text = "";
                        }
                        textArea.setText(text);
                    }
                    e.consume();
                }
            });

            setData(name, list());
        }

        // Weird reasons for needing this method, just call it bad design. Not worth 'fixing'.
        public void setData(String name, List<?> input) {
            this.name.set(capitalizeStrong(name));
            setData(input);
        }

        @Override
        public void setData(List<?> input) {
            super.setData(input);
            fillActionData();
        }

        public void fillActionData(){
            if (isMain && applier!=null) {
                applier.fillActs(transforms.getTypeIn());
            }
        }

        @Override
        public String toString() {
            return name.get();
        }

    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    private class Applier {
        private final ImprovedComboBox<Act<?>> actCB = new ImprovedComboBox<>(act -> act.name, "<none>");
        Ins ins;
        BiConsumer<File,String> applier = (f, s) -> {
            File rf = f.getParentFile().getAbsoluteFile();
            int dot = f.getPath().lastIndexOf('.');
            String p = f.getPath();
            String ext = p.substring(dot,p.length());
            f.renameTo(new File(rf, filenamizeString(s)+ext));
        };
        @SuppressWarnings("unchecked")
        private final Icon runB = new Icon(PLAY_CIRCLE, 20, "Apply", () -> {
            Act<Object> action = (Act) actCB.getValue();
            if (action==null) return;

            if (action.isPartial()) {
                boolean empty = source.isEmpty() || ins.values().count()==0;
                if (empty) return;

                boolean in_out_type_match = true;   //action.type.isInstance(source.get(0));    // TODO: implement properly
                boolean same_size = equalBy(tas, ta -> ta.getVal().size());
                if (!in_out_type_match || !same_size) return;

                Map<String,List<? extends String>> m = ins.values().collect(toMap(in -> in.name, in -> in.ta.getVal()));
                for (int i=0; i<source.size(); i++)
                    action.actionPartial.accept(source.get(i), mapSlice(m,i));
            } else {
                Map<String,List<? extends String>> m = ins.values().collect(toMap(in -> in.name, in -> in.ta.getVal()));
                action.actionImpartial.accept(m);
            }
        });
        VBox root = new VBox(5,
            new StackPane(new Label("Action")),         // title
            actCB,                                      // action chooser
            new StackPane(new Label("Apply", runB))     // action run button
        );

        public Applier() {
            actCB.valueProperty().addListener((o,ov,nv) -> {
                if (nv==null) return;
                if (ins!=null) root.getChildren().remove(ins.node());
                if (root.getChildren().size()==4) root.getChildren().remove(2);
                ins = nv.isNamesDeterminate ? new InsSimple(nv) : new InsComplex(nv);
                root.getChildren().add(2,ins.node());
                Node n = nv.getNode();
                if (n!=null) root.getChildren().add(3,n);
            });
        }

        public void fillActs(Class<?> c) {
            List<Act<?>> l = acts.getElementsOfSuperV(c);
            actCB.getItems().setAll(l);
            if (!l.isEmpty()) actCB.setValue(l.get(0));
        }
    }
    private class Act<Y> {
        String name;
        int max = MAX_VALUE;
        Ƒ0<List<String>> names;
        Class<Y> type;
        boolean isNamesDeterminate;
        BiConsumer<Y,Map<String,? extends String>> actionPartial = null;
        Consumer<? super Map<String,List<? extends String>>> actionImpartial = null;

        private Act(String name, Class<Y> type, int max, BiConsumer<Y,Map<String,? extends String>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionPartial = action;
        }
        private Act(String name, Class<Y> type, int max, Consumer<Map<String,List<? extends String>>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionImpartial = action;
        }
        Act(String name, Class<Y> type, int max, Ƒ0<List<String>> names, BiConsumer<Y,Map<String,? extends String>> action) {
            this(name, type, max, action);
            this.names = names;
            isNamesDeterminate = false;
        }
        Act(String name, Class<Y> type, int max, List<String> names, BiConsumer<Y,Map<String,? extends String>> action) {
            this(name, type, max, action);
            this.names = () -> names;
            isNamesDeterminate = true;
        }
        Act(String name, Class<Y> type, int max, Ƒ0<List<String>> names, Consumer<Map<String,List<? extends String>>> action) {
            this(name, type, max, action);
            this.names = names;
            isNamesDeterminate = false;
        }
        Act(String name, Class<Y> type, int max, List<String> names, Consumer<Map<String,List<? extends String>>> action) {
            this(name, type, max, action);
            this.names = () -> names;
            isNamesDeterminate = true;
        }

        Node getNode() {
            return null;
        }

        public boolean isPartial() {
            return actionPartial!=null;
        }
    }
    private class WriteFileAct extends Act<Void> {
        V<String> nam = new V<>("new_file");
        V<String> ext = new V<>("txt");
        V<File> loc = new V<>(APP.DIR_APP);

        public WriteFileAct() {
            super("Write file", Void.class, 1, list("Contents"), (Consumer<Map<String,List<? extends String>>>) null);
            actionImpartial = data -> {
                String filepath = new File(loc.get(), nam.get()+"."+ext.get()).getPath();
                String contents = toS(data.get("Contents"),"\n");
                writeFile(filepath, contents);
            };
        }

        @Override
        public Node getNode() {
            return layVertically(5,TOP_CENTER,
                layHorizontally(5,CENTER_LEFT,
                    ConfigField.create(Config.forProperty(String.class, "File name", nam)).getNode(),
                    new Label("."),
                    ConfigField.create(Config.forProperty(String.class, "Extension", ext)).getNode()
                ),
                ConfigField.create(Config.forProperty(File.class, "Location", loc)).getNode()
            );
        }
    }
    private class ActCreateDirs extends Act<Void> {
        V<Boolean> use_loc = new V<>(false);
        V<File> loc = new V<>(APP.DIR_HOME);

        public ActCreateDirs() {
            super("Create directories", Void.class, 1, list("Names (Paths)"), (Consumer<Map<String,List<? extends String>>>) null);
            actionImpartial = data ->
                fut(data.get("Names (Paths)"))
                   .useBy(names -> {
                       File dir = loc.get();
                       names.forEach((String name) -> {
                           try {
                               File newFile;
                               if (use_loc.get()) {
                                   name = name.startsWith(File.separator) ? name.substring(1) : name;
                                   newFile = new File(dir, filenamizeString(name));
                               } else {
                                   newFile = new File(name);
                               }
                               Files.createDirectories(newFile.toPath());
                           } catch (IOException e) {
                               logger(Converter.class).info("Couldn't create file/directory",e);
                           }
                       });
                   });
        }

        @Override
        public Node getNode() {
            Node n = ConfigField.create(Config.forProperty(File.class, "Location", loc)).getNode();
            use_loc.syncC(v -> n.setDisable(!v));
            return layVertically(5,TOP_CENTER,
                layHorizontally(5,CENTER_LEFT,
                    new Label("In directory"),
                    ConfigField.create(Config.forProperty(Boolean.class, "In directory", use_loc)).getNode()
                ),
                n
            );
        }
    }

    private class In {
        public String name;
        public EditArea ta;

        In(String name, EditArea ta) {
            this.name = name;
            this.ta = ta;
        }
    }
    private class InPane extends ValueNode<In> {
        V<String> name;
        V<EditArea> input;
        ConfigField<String> configFieldA;
        ConfigField<EditArea> configFieldB;
        HBox root;

        InPane(Ƒ0<? extends Collection<? extends String>> actions) {
            super(null);
            name = new VarEnum<>(actions.get().stream().findFirst().orElse(null), actions);
            input = new VarEnum<>(stream(tas).filter(ta -> ta.name.get().equalsIgnoreCase("out")).findAny().orElse(ta_in),tas);
            configFieldA = ConfigField.create(Config.forProperty(String.class, "", name));
            configFieldB = ConfigField.create(Config.forProperty(EditArea.class, "", input));
            root = new HBox(5, configFieldA.getNode(), configFieldB.getNode());
        }

        @Override
        public In getVal() {
            return new In(configFieldA.getVal(), configFieldB.getVal());
        }

        @Override
        public Node getNode() {
            return root;
        }
    }
    private interface Ins {
        Node node();
        Stream<In> values();
    }
    private class InsSimple implements Ins {
        ConfigPane<EditArea> ins;

        InsSimple(Act<?> a) {
            ins = new ConfigPane<>();
            ins.configure(
                map(a.names.get(), name -> {
                    V<EditArea> input = new VarEnum<>(stream(tas).filter(ta -> ta.name.get().equalsIgnoreCase("out")).findAny().orElse(ta_in), tas);
                    return Config.forProperty(EditArea.class, name, input);
                })
            );
        }

        public Node node() {
            return ins;
        }

        public Stream<In> values() {
            return ins.getConfigFields().stream().map(c -> new In(c.config.getName(),c.getVal()));
        }

    }
    private class InsComplex implements Ins {
        ListConfigField<In, InPane> ins;

        InsComplex(Act<?> a) {
            ins = new ListConfigField<>(() -> new InPane(a.names));
            ins.maxChainLength.set(a.max);
        }

        public VBox node() {
            return ins.getNode();
        }

        public Stream<In> values() {
            return ins.getValues();
        }
    }
}