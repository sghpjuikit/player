/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import Configuration.Config;
import Layout.Widgets.ClassWidgetController;
import Layout.Widgets.Features.SongWriter;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.APP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.PLAY_CIRCLE;
import gui.itemnode.ChainConfigField.ListConfigField;
import gui.itemnode.ConfigField;
import gui.itemnode.ItemNode.ValueNode;
import gui.itemnode.ListAreaNode;
import gui.itemnode.StringSplitGenerator;
import gui.itemnode.StringSplitParser;
import gui.objects.Icon;
import gui.objects.combobox.ImprovedComboBox;
import java.io.File;
import static java.lang.Integer.MAX_VALUE;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toMap;
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
import util.collections.ClassListMap;
import util.collections.Tuple2;
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
        + "    User can put text in a text area and stack transformations on it "
        + "using available functions. The transformations are applied on each "
        + "line separately. It is possible to manually edit the text to finetune "
        + "the result.\n"
        + "    This is useful to edit multiple texts in the same way, e.g., to "
        + "edit filenames or song names. This is done using an 'applier' that "
        + "uses the 'output' and applies it no an input.\n"
        + "    Input is a list of objects and can be set by drag&drop. It can "
        + "then be transformed to text (object per line) using available "
        + "object to text functions, e.g., song to artist or file to filename.\n"
        + "    Output is the final contents of the text area exactly as "
        + "visible. Some text  transformations split text into multiple parts "
        + "(again, each line), which produces multiple outputs, each in separate "
        + "text area. Text areas have a name in their header.\n"
        + "    Action is determined by the type of input. User can select which "
        + "output he wants to use. The action then applies the text to the "
        + "input, each line to its respective object (determined by order). "
        + "Number of objects and lines must match.\n"
        + "\n"
        + "Available appliers:\n"
        + "    Save text to file - produce a text file with specified content\n"
        + "    Rename files - renames input files\n"
        + "    Tag songs - edits tag of song individually\n"
        + "\n"
        + "Available actions:\n"
        + "    Drag&drop files : Sets files as input\n"
        + "    Drag&drop songs : Sets songs as input\n"
        + "    Drag&drop text : Sets text as input\n",
    notes = "",
    version = "0.8",
    year = "2015",
    group = APP
)
public class Converter extends ClassWidgetController implements SongWriter {
    
    private final ObservableList source = FXCollections.observableArrayList();
    StringSplitGenerator splitter = new StringSplitGenerator();
    private final TA inTa = new TA("In");
    private final ObservableList<TA> tas = FXCollections.observableArrayList(inTa);
    private final ClassListMap<Act> acts = new ClassListMap<>(act -> act.type);
    private final HBox outTFBox = new HBox(5);
    private final Applier applier = new Applier();
    private final HBox layout = new HBox(5,outTFBox, applier.root);
    
    public Converter() {
        // layout
        HBox l11 = new HBox(5, inTa.getNode(),layout);
        HBox l12 = new HBox(5, splitter.getNode());
        VBox l2 = new VBox(5, l11,l12);
        VBox.setVgrow(l11, ALWAYS);
        getChildren().add(l2);
        setAnchors(l2,0);
        
        
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
        splitter.onItemChange = f -> transform();
        
        inTa.onItemChange = lines -> {
            StringSplitParser p = splitter.getValue();
            List<List<String>> outs = list(p.parse_keys.size(), ArrayList::new);
            lines.forEach(line -> forEachI(p.apply(line), (i,l)->outs.get(i).add(l)));
            
            List<TA> l = list(outs.size(), () -> new TA(""));
            forEachI(outs, (i,lins) -> l.get(i).setData(p.parse_keys.get(i), lins));
            l.add(0, inTa);
            tas.setAll(l);
            
            outTFBox.getChildren().setAll(map(l,TA::getNode));    
        };
        
        // init data
        acts.accumulate(new Act<>("Rename files", File.class, 1, () -> list("Filename"), (file, data) -> {
            String name = data.get("Filename");
            FileUtil.renameFile(file, name);
        }));
        acts.accumulate(new Act<>("Tag songs", Metadata.class, 100, () -> map(getEnumConstants(Metadata.Field.class),Object::toString), (file, data) -> {
//            String 
        }));
        acts.accumulate(new WriteFileAct());
        applier.fillActs(Void.class);
    }
    
    private void transform() {
        inTa.setData(source);
    }

/******************************** features ************************************/
    
    /** {@inheritDoc} */
    @Override
    public void read(List<? extends Item> items) {
        source.setAll(map(items,Item::toMeta));
    }
    
/******************************* helper classes *******************************/

    class TA extends ListAreaNode {
        private final Label nameL = new Label("");
        public final StringProperty name = nameL.textProperty();

        public TA(String name) {
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
        ListConfigField<Tuple2<String,String>, In> ins;
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
            
            if(action.isPartial) {
                boolean empty = source.isEmpty() || ins.getValues().count()==0;
                if(empty) return;

                boolean in_out_type_match = true;//action.type.isInstance(source.get(0));
                boolean same_size = equalBy(tas, ta->ta.getValue().size());
                if(!in_out_type_match || !same_size) return;
                
                Map<String,List<String>> m = ins.getValues().collect(toMap(in->in._1,in ->findOrDie(tas,ta->ta.name.get().equalsIgnoreCase(in._2)).getValue()));
                for(int i=0; i<source.size(); i++)
                    action.action.accept(source.get(i), mapSlice(m,i));
            } else {
                Map<String,String> m = ins.getValues().collect(toMap(in->in._1,in->toS(tas.stream().filter(ta->ta.name.get().equals(in._2)).findFirst().get().getValue(),x->x,"\n")));
                action.actionImpartial.accept(m);
            }
        });
        VBox root = new VBox(5, new StackPane(new Label("Action")),actCB, new StackPane(new Label("Apply", okB)));
        
        public Applier() {
            okB.setPadding(new Insets(8));
            actCB.valueProperty().addListener((o,ov,nv) -> {
                if(ins!=null) root.getChildren().remove(ins.getNode());
                if(root.getChildren().size()==4) root.getChildren().remove(2);
                ins = new ListConfigField<>(() -> new In(nv.names,tas));
                ins.maxChainLength.set(nv.max);
                root.getChildren().add(2,ins.getNode());
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
        BiConsumer<V,Map<String,String>> action;
        Consumer<Map<String,String>> actionImpartial;
        boolean isPartial = true;

        public Act(String name, Class<V> type, int max, Supplier<List<String>> names, BiConsumer<V,Map<String,String>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.names = names;
            this.action = action;
        }
        
        public Node getNode() {
            return null;
        }
    }
    class WriteFileAct extends Act<Void> {
        Accessor<String> nam = new Accessor("new_file");
        Accessor<String> ext = new Accessor("txt");
        Accessor<File> loc = new Accessor(App.getLocation());
            
        public WriteFileAct() {
            super("Write file", Void.class, 1, () -> list("Contents"), null);
            actionImpartial=this::makeFile;
            isPartial = false;
        }
        @Override public Node getNode() {
            HBox hb = new HBox(5,ConfigField.create(Config.fromProperty("File name", nam)).getNode(),
                                 new Label("."),
                                 ConfigField.create(Config.fromProperty("Extension", ext)).getNode());
            VBox vb = new VBox(5,hb,ConfigField.create(Config.fromProperty("Location", loc)).getNode());
            return vb;
        }
        void makeFile(Map<String,String> outputs) {
            String filepath = new File(loc.get(), nam.get()+"."+ext.get()).getPath();
            String contents = outputs.get("Contents");
            writeFile(filepath, contents);
        }
    }
    class In extends ValueNode<Tuple2<String,String>> {
        Accessor<String> name;
        Accessor<TA> input;
        ConfigField<String> configfieldA;
        ConfigField<TA> configfieldB;
        HBox root;
        
        public In(Supplier<Collection<String>> actions, ObservableList<TA> inputs) {
            name = new AccessorEnum<>(actions.get().stream().findFirst().get(),actions);
            input = new AccessorEnum<>(find(tas,ta->ta.name.get().equalsIgnoreCase("out")).orElse(inTa),inputs);
            configfieldA = ConfigField.create(Config.fromProperty("", name));
            configfieldB = ConfigField.create(Config.fromProperty("", input));
            root = new HBox(5, configfieldA.getNode(),configfieldB.getNode());
        }
        
        @Override
        public Tuple2<String,String> getValue() {
            return util.collections.Tuples.tuple(configfieldA.getValue(), configfieldB.getValue().name.get());
        }

        @Override
        public Node getNode() {
            return root;
        }
    }

}