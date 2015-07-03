/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Config;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.APP;
import Layout.Widgets.controller.ClassController;
import Layout.Widgets.feature.SongWriter;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.PLAY_CIRCLE;
import gui.itemnode.*;
import gui.itemnode.ChainConfigField.ConfigPane;
import gui.itemnode.ChainConfigField.ListConfigField;
import gui.itemnode.ItemNode.ValueNode;
import gui.itemnode.StringSplitParser.SplitData;
import gui.objects.combobox.ImprovedComboBox;
import gui.objects.icon.Icon;
import java.io.File;
import static java.lang.Integer.MAX_VALUE;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.*;
import java.util.stream.Stream;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.*;
import static javafx.scene.layout.Priority.ALWAYS;
import main.App;
import util.File.FileUtil;
import static util.File.FileUtil.writeFile;
import static util.Util.*;
import util.access.Accessor;
import util.access.AccessorEnum;
import static util.async.Async.runNew;
import util.async.future.Fut;
import util.collections.ClassListMap;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;

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
    
    public Converter(ClassWidget widget) {
        super(widget);
        
        // layout
        HBox ll = new HBox(5, ta_in.getNode(),layout);
        HBox.setHgrow(ta_in.getNode(), ALWAYS);
        getChildren().add(ll);
        setAnchors(ll,0);
        
        
        // behavior
        addEventHandler(DRAG_OVER,DragUtil.audioDragAccepthandler);
        addEventHandler(DRAG_OVER,DragUtil.fileDragAccepthandler);
        addEventHandler(DRAG_OVER,DragUtil.textDragAccepthandler);
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles())
                source.setAll(filter(db.getFiles(),File::isFile));
            else if(DragUtil.hasItemList())
                read(DragUtil.getItemsList());
            else if(DragUtil.hasText(e))
                source.setAll(split(DragUtil.getText(e), "\n", x->x));
        });
        
        
        // on source change run transformation
        source.addListener((Change change) -> {
            Class c = source.isEmpty() ? Void.class : source.get(0).getClass();
            applier.fillActs(c);
            transform();
        });
        
        ta_in.onItemChange = lines -> {
            List<Ta> l = null;
            if(!ta_in.output.isEmpty() && ta_in.output.get(0) instanceof SplitData) {
                List<SplitData> s = (List) ta_in.output;
                List<String> names = s.get(0).stream().map(split->split.parse_key).collect(toList());
                
                List<List<String>> outs = list(names.size(), ArrayList::new);
                s.forEach(splitdata -> forEachI(map(splitdata,split->split.split), (i,line)->outs.get(i).add(line)));

                List<Ta> li = list(outs.size(), () -> new Ta(""));
                forEachI(outs, (i,lins) -> li.get(i).setData(names.get(i), lins));
                
                l = li;
                l.add(0, ta_in);
                tas.setAll(l);
            } else {
                l = listRO(ta_in);
                tas.setAll(ta_in);
            }
            
            outTFBox.getChildren().setAll(map(l,Ta::getNode));    
        };
        
        // init data
        acts.accumulate(new Act<>("Rename files", File.class, 1, list("Filename"), (file, data) -> {
            String name = data.get("Filename");
            FileUtil.renameFile(file, name);
        }));
        acts.accumulate(new Act<>("Edit song tags", Metadata.class, 100, () -> map(getEnumConstants(Metadata.Field.class),Object::toString), data -> {
            List<Item> songs = list(source);
            if(songs.isEmpty()) return;
            Fut.fut()
               .thenR(() -> {
                    for(int i=0; i<songs.size(); i++) {
                        int j = i;
                        MetadataWriter.useNoRefresh(songs.get(i), w -> data.forEach((field,vals) -> w.setFieldS(Metadata.Field.valueOf(field), vals.get(j))));
                    }
                    MetadataReader.readMetadata(songs, (ok,metas) -> {
                        if (ok) runNew(() -> Player.refreshItemsWithUpdatedBgr(metas));
                    });
               })
               .showProgress(App.getWindow().taskAdd())
               .run();
        }));
        acts.accumulate(new WriteFileAct());
        applier.fillActs(Void.class);
    }
    
    private void transform() {
        ta_in.setData(source);
    }

/******************************** features ************************************/
    
    /** {@inheritDoc} */
    @Override
    public void read(List<? extends Item> items) {
        source.setAll(map(items,Item::toMeta));
    }
    
/******************************* helper classes *******************************/

    class Ta extends ListAreaNode {
        private final Label nameL = new Label("");
        public final StringProperty name = nameL.textProperty();

        public Ta(String name) {
            super();
            getNode().getChildren().add(0,new StackPane(nameL));
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
            okB.setPadding(new Insets(8));
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
        Accessor<String> nam = new Accessor("new_file");
        Accessor<String> ext = new Accessor("txt");
        Accessor<File> loc = new Accessor(App.getLocation());
            
        public WriteFileAct() {
            super("Write file", Void.class, 1, list("Contents"), (Consumer)null);
            actionImpartial = data -> {
                String filepath = new File(loc.get(), nam.get()+"."+ext.get()).getPath();
                String contents = toS(data.get("Contents"),"\n");
                writeFile(filepath, contents);
            };
        }
        @Override public Node getNode() {
            HBox hb = new HBox(5,ConfigField.create(Config.fromProperty("File name", nam)).getNode(),
                                 new Label("."),
                                 ConfigField.create(Config.fromProperty("Extension", ext)).getNode());
            VBox vb = new VBox(5,hb,ConfigField.create(Config.fromProperty("Location", loc)).getNode());
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
        Accessor<String> name;
        Accessor<Ta> input;
        ConfigField<String> configfieldA;
        ConfigField<Ta> configfieldB;
        HBox root;
        
        public InPane(Supplier<Collection<String>> actions) {
            name = new AccessorEnum<>(actions.get().stream().findFirst().get(),actions);
            input = new AccessorEnum<>(find(tas,ta->ta.name.get().equalsIgnoreCase("out")).orElse(ta_in),tas);
            configfieldA = ConfigField.create(Config.fromProperty("", name));
            configfieldB = ConfigField.create(Config.fromProperty("", input));
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
                Accessor<Ta> input = new AccessorEnum(find(tas,ta->ta.name.get().equalsIgnoreCase("out")).orElse(ta_in),tas);
                return Config.fromProperty(name, input);
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