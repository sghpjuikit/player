/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.impl;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Config;
import Layout.widget.IsWidget;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import Layout.widget.feature.SongWriter;
import gui.itemnode.*;
import gui.itemnode.ChainValueNode.ConfigPane;
import gui.itemnode.ChainValueNode.ListConfigField;
import gui.itemnode.ItemNode.ValueNode;
import gui.itemnode.StringSplitParser.SplitData;
import gui.objects.combobox.ImprovedComboBox;
import gui.objects.icon.Icon;
import main.App;
import util.File.FileUtil;
import util.access.VarEnum;
import util.access.Ѵ;
import util.async.future.Fut;
import util.collections.map.ClassListMap;
import util.graphics.drag.DragUtil;
import util.Ɽ;

import static Layout.widget.Widget.Group.APP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY_CIRCLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.File.FileUtil.writeFile;
import static util.Util.*;
import static util.functional.Util.*;
import static util.graphics.Util.layAnchor;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layStack;
import static util.graphics.drag.DragUtil.installDrag;

@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Converter",
    description = "Transformation utility. Capable of transforming objects "
        + "and chaining the transforming functions. Capable of file renaming and "
        + "tagging.",
    howto = ""
        + "\tUser can put text in a text area and stack transformations on it "
        + "using available functions. The transformations are applied on each "
        + "line separately. It is possible to manually edit the text to finetune "
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
    notes = "",
    version = "1",
    year = "2015",
    group = APP
)
public class Converter extends ClassController implements SongWriter {

    private final ObservableList source = FXCollections.observableArrayList();
    private final Ta ta_in = new Ta("In");
    private final ObservableList<Ta> tas = FXCollections.observableArrayList(ta_in);
    private final ClassListMap<Act> acts = new ClassListMap<>(act -> act.type);
    private final HBox outTFBox = new HBox(5);
    private final Applier applier = new Applier();
    private final HBox layout = new HBox(5,outTFBox, applier.root);


    public Converter() {
        inputs.create("To convert", Object.class, this::setData);

        // layout
        HBox ll = new HBox(5, ta_in.getNode(),layout);
        HBox.setHgrow(ta_in.getNode(), ALWAYS);
        layAnchor(this, ll,0d);

        // drag&drop
        installDrag(
            this, LIST_ALT, () -> "Set data as input",
            e -> true, e -> false,
            e -> setData(DragUtil.getAny(e)),
            e -> ta_in.getNode().getLayoutBounds()
        );

        tas.addListener((Change<? extends Ta> change) ->
            outTFBox.getChildren().setAll(map(tas,Ta::getNode))
        );

        // on source change run transformation
        source.addListener((Change change) -> {
            ta_in.setData(source);
            Class c = ta_in.transforms.getTypeIn();
            applier.fillActs(c);
        });

        ta_in.onItemChange = lines -> {
            List<Ta> l = null;
            List<Ta> custom_tas = filter(tas,ta -> ta.name.get().contains("Custom"));
            if(!ta_in.output.isEmpty() && ta_in.output.get(0) instanceof SplitData) {
                List<SplitData> s = (List) ta_in.output;
                List<String> names = s.get(0).stream().map(split -> split.parse_key).collect(toList());

                List<List<String>> outs = list(names.size(), ArrayList::new);
                s.forEach(splitdata -> forEachWithI(map(splitdata,split -> split.split), (i,line)->outs.get(i).add(line)));

                List<Ta> li = list(outs.size(), () -> new Ta(""));
                forEachWithI(outs, (i,lins) -> li.get(i).setData(names.get(i), lins));

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
            FileUtil.renameFileNoSuffix(file, name);
        }));
        acts.accumulate(new Act<>("Rename files (and extension)", File.class, 1, list("Filename"), (file, data) -> {
            String name = data.get("Filename");
            FileUtil.renameFile(file, name);
        }));
        acts.accumulate(new Act<>("Edit song tags", Item.class, 100, () -> map(getEnumConstants(Metadata.Field.class),Object::toString), data -> {
            List<Item> songs = list(source);
            if(songs.isEmpty()) return;
            Fut.fut()
               .then(() -> {
                    for(int i=0; i<songs.size(); i++) {
                        int j = i;
                        MetadataWriter.useNoRefresh(songs.get(i), w -> data.forEach((field,vals) -> w.setFieldS(Metadata.Field.valueOf(field), vals.get(j))));
                    }
                    Player.refreshItemsWith(MetadataReader.readMetadata(songs));
               },Player.IO_THREAD)
               .showProgress(getWidget().getWindow().taskAdd())
               .run();
        }));
        acts.accumulate(new WriteFileAct());

        // set empty content
        applier.fillActs(Void.class);
    }

    public void setData(Object o) {
        source.setAll(unpackData(o));
    }

    public static List<?> unpackData(Object o) {
        if(o instanceof String)
            return split((String) o, "\n", x->x);
        else if(o instanceof Collection)
            return list((Collection) o);
        else return listRO(o);
    }

/******************************** features ************************************/

    /** {@inheritDoc} */
    @Override
    public void read(List<? extends Item> items) {
        source.setAll(map(items,Item::toMeta));
    }

/******************************* helper classes *******************************/

    // generates unique name in format 'CustomN', where N is integer number
    String taname() {
        Ɽ<Integer> i = new Ɽ<>(0);
        do {
            i.setOf(x -> x+1);
        }while(tas.stream().map(t->t.name.get()).anyMatch(n -> n.equals("Custom"+i.get())));
        return "Custom"+i.get();
    }

    class Ta extends ListAreaNode {
        public final StringProperty name;

        public Ta() {
            this(taname());
        }

        public Ta(String name) {
            super();

            Label nameL = new Label("");
            Icon remI = new Icon(MINUS)
                    .tooltip("Remove\n\nRemove this edit area.")
                    .onClick(() -> tas.remove(this));
            Icon addI = new Icon(PLUS)
                    .tooltip("Add\n\nCreate new edit area with no data.")
                    .onClick(() -> tas.add(tas.indexOf(this)+1, new Ta()));
            Icon copyI = new Icon(ANGLE_DOUBLE_RIGHT)
                    .tooltip("Copy\n\nCopy data of edit area into new one. Data will retain its transformations.")
                    .onClick(() -> {
                        Ta t = new Ta();
                        t.setData(output);
                        tas.add(t);
                     });
            getNode().getChildren().add(0,
                layStack(
                    nameL,Pos.CENTER,
                    layHorizontally(5,Pos.CENTER_RIGHT, remI,addI,copyI),Pos.CENTER_RIGHT
                )
            );

            this.name = nameL.textProperty();
            boolean is1st = tas==null;

            // disallow removing 1st edit area
            remI.setDisable(is1st);

            // drag & drop (1st area is handled globally)
            if(!is1st) {
                installDrag(
                    getNode(), LIST_ALT, () -> "Set data to " + this.name.get() + " edit area",
                    e -> true,
                    e -> setData(unpackData(DragUtil.getAny(e)))
                );
            }

            setData(name, EMPTY_LIST);
        }

        public void setData(String name, List<? extends Object> input) {
            this.name.set(capitalizeStrong(name));
            super.setData(input);
        }

        @Override
        public String toString() {
            return name.get();
        }
    }
    class Applier {
        private final ImprovedComboBox<Act> actCB = new ImprovedComboBox<>(act -> act.name, "<none>");
        Ins ins;
        BiConsumer<File,String> applier = (f,s) -> {
            File rf = f.getParentFile().getAbsoluteFile();
            int dot = f.getPath().lastIndexOf('.');
            String p = f.getPath();
            String ext = p.substring(dot,p.length());
            f.renameTo(new File(rf, filenamizeString(s)+ext));
        };
        private final Icon okB = new Icon(PLAY_CIRCLE, 20, "Apply", () -> {
            Act action = actCB.getValue();
            if(action==null) return;

            if(action.isPartial()) {
                boolean empty = source.isEmpty() || ins.vals().count()==0;
                if(empty) return;

                boolean in_out_type_match = true;//action.type.isInstance(source.get(0));
                boolean same_size = equalBy(tas, ta->ta.getValue().size());
                if(!in_out_type_match || !same_size) return;

                Map<String,List<String>> m = ins.vals().collect(toMap(in->in.name,in ->in.ta.getValue()));
                for(int i=0; i<source.size(); i++)
                    action.actionPartial.accept(source.get(i), mapSlice(m,i));
            } else {
                Map<String,List<String>> m = ins.vals().collect(toMap(in->in.name,in ->in.ta.getValue()));
                action.actionImpartial.accept(m);
            }
        });
        VBox root = new VBox(5, new StackPane(new Label("Action")),actCB, new StackPane(new Label("Apply", okB)));

        public Applier() {
            actCB.valueProperty().addListener((o,ov,nv) -> {
                if(ins!=null) root.getChildren().remove(ins.node());
                if(root.getChildren().size()==4) root.getChildren().remove(2);
                ins = nv.isNamesDeterminate ? new InsSimple(nv) : new InsComplex(nv);
                root.getChildren().add(2,ins.node());
                Node n = nv.getNode();
                if(n!=null) root.getChildren().add(3,n);
            });
        }

        public void fillActs(Class c) {
            List<Act> l = acts.getElementsOfSuperV(c);
            actCB.getItems().setAll(l);
            if(!l.isEmpty()) actCB.setValue(l.get(0));
        }
    }
    class Act<V> {
        String name;
        int max = MAX_VALUE;
        Supplier<List<String>> names;
        Class type;
        boolean isNamesDeterminate;
        BiConsumer<V,Map<String,String>> actionPartial = null;
        Consumer<Map<String,List<String>>> actionImpartial = null;

        private Act(String name, Class<V> type, int max, BiConsumer<V,Map<String,String>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionPartial = action;
        }
        private Act(String name, Class<V> type, int max, Consumer<Map<String,List<String>>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.actionImpartial = action;
        }
        Act(String name, Class<V> type, int max, Supplier<List<String>> names, BiConsumer<V,Map<String,String>> action) {
            this(name, type, max, action);
            this.names = names;
            isNamesDeterminate = false;
        }
        Act(String name, Class<V> type, int max, List<String> names, BiConsumer<V,Map<String,String>> action) {
            this(name, type, max, action);
            this.names = () -> names;
            isNamesDeterminate = true;
        }
        Act(String name, Class<V> type, int max, Supplier<List<String>> names, Consumer<Map<String,List<String>>> action) {
            this(name, type, max, action);
            this.names = names;
            isNamesDeterminate = false;
        }
        Act(String name, Class<V> type, int max, List<String> names, Consumer<Map<String,List<String>>> action) {
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
    class WriteFileAct extends Act<Void> {
        Ѵ<String> nam = new Ѵ("new_file");
        Ѵ<String> ext = new Ѵ("txt");
        Ѵ<File> loc = new Ѵ(App.getLocation());

        public WriteFileAct() {
            super("Write file", Void.class, 1, list("Contents"), (Consumer)null);
            actionImpartial = data -> {
                String filepath = new File(loc.get(), nam.get()+"."+ext.get()).getPath();
                String contents = toS(data.get("Contents"),"\n");
                writeFile(filepath, contents);
            };
        }
        @Override public Node getNode() {
            HBox hb = new HBox(5,ConfigField.create(Config.forProperty("File name", nam)).getNode(),
                                 new Label("."),
                                 ConfigField.create(Config.forProperty("Extension", ext)).getNode());
            VBox vb = new VBox(5,hb,ConfigField.create(Config.forProperty("Location", loc)).getNode());
            return vb;
        }
    }
    class In {
        String name;
        Ta ta;

        public In(String name, Ta ta) {
            this.name = name;
            this.ta = ta;
        }
    }
    class InPane extends ValueNode<In> {
        Ѵ<String> name;
        Ѵ<Ta> input;
        ConfigField<String> configfieldA;
        ConfigField<Ta> configfieldB;
        HBox root;

        public InPane(Supplier<Collection<String>> actions) {
            name = new VarEnum<>(actions.get().stream().findFirst().get(),actions);
            input = new VarEnum<>(find(tas,ta->ta.name.get().equalsIgnoreCase("out")).orElse(ta_in),tas);
            configfieldA = ConfigField.create(Config.forProperty("", name));
            configfieldB = ConfigField.create(Config.forProperty("", input));
            root = new HBox(5, configfieldA.getNode(),configfieldB.getNode());
        }

        @Override
        public In getValue() {
            return new In(configfieldA.getValue(), configfieldB.getValue());
        }

        @Override
        public Node getNode() {
            return root;
        }
    }
    interface Ins {
        Node node();
        Stream<In> vals();
    }
    class InsSimple implements Ins {
        ConfigPane<Ta> ins;
        public InsSimple(Act<?> a) {
            ins = new ConfigPane(map(a.names.get(), name -> {
                Ѵ<Ta> input = new VarEnum(find(tas,ta->ta.name.get().equalsIgnoreCase("out")).orElse(ta_in),tas);
                return Config.forProperty(name, input);
            }));
        }

        public Node node() {
            return ins.getNode();
        }

        public Stream<In> vals() {
            return ins.getValuesC().stream().map(c -> new In(c.getConfig().getName(),c.getValue()));
        }

    }
    class InsComplex implements Ins {
        ListConfigField<In, InPane> ins;

        public InsComplex(Act a) {
            ins = new ListConfigField<>(() -> new InPane(a.names));
            ins.maxChainLength.set(a.max);
        }

        public VBox node() {
            return ins.getNode();
        }

        public Stream<In> vals() {
            return ins.getValues();
        }
    }
}