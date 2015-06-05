/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import gui.ItemNode.TransformableList;
import gui.ItemNode.ItemNode;
import gui.ItemNode.StringSplitGenerator;
import gui.ItemNode.StringSplitParser;
import gui.ItemNode.ConfigField;
import gui.ItemNode.StringListItemNode;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import Configuration.Config;
import gui.ItemNode.ChainConfigField.ListConfigField;
import gui.ItemNode.TransformableList.Transformer;
import static gui.ItemNode.TransformableList.of;
import gui.objects.Icon;
import gui.objects.combobox.ImprovedComboBox;
import Layout.Widgets.ClassWidgetController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.APP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.PLAY_CIRCLE;
import java.io.File;
import static java.lang.Integer.MAX_VALUE;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.cell.ComboBoxListCell;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.Util.*;
import util.access.Accessor;
import util.access.AccessorEnum;
import util.collections.ClassListMap;
import util.collections.PrefList;
import util.collections.Tuple2;
import static util.dev.Util.forbidNull;
import util.functional.Functors;
import util.functional.Functors.PF;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;

@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Converter",
    description = "Text transformation utility. Capable of file renaming and "
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
        + "then be transformed to text (object per by line) using available "
        + "object to text functions, e.g., song to artist or file to filename.\n"
        + "    Output is the final contents of the text area. Some text "
        + "transformations split text into multiple parts (again, each line), "
        + "which produces multiple outputs, each in separate text area. Text "
        + "areas have a name in their header.\n"
        + "    Action is determined by the type of input. User can select which "
        + "output text areas to use. The action then applies the text to the "
        + "input, each line to its respective object (determined by order). "
        + "Number of objects and lines must match.\n"
        + "\n"
        + "Available appliers:\n"
        + "    Save to file - produce a text file with specified content\n"
        + "    Rename files - renames input files\n"
        + "    Tag songs - edits tag of song individually\n"
        + "\n"
        + "Available actions:\n"
        + "    Drag&drop files : Sets files as input\n"
        + "    Drag&drop songs : Sets songs as input\n",
    notes = "",
    version = "0.8",
    year = "2015",
    group = APP
)
public class Converter extends ClassWidgetController implements TaggingFeature {
    
    private final ObservableList source = FXCollections.observableArrayList();
    List<TransformableList> transforms = new ArrayList();
    
    private final ComboBox<PF> transfCB = new ComboBox();
    StringSplitGenerator splitter = new StringSplitGenerator();
    private final TA inTa = new TA("In",()->Functors.getIO(String.class));
    List<TA> outTas;
    private final ObservableList<TA> tas = FXCollections.observableArrayList();
    private final ClassListMap<Act> acts = new ClassListMap<>(act -> act.type);
    private final HBox outTFBox = new HBox(5);
    private final Applier applier = new Applier();
    private final HBox allBox = new HBox(5,outTFBox, applier.root);
    
    public Converter() {
        transfCB.setCellFactory(view -> new ComboBoxListCell<PF>(){
            @Override
            public void updateItem(PF item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.name);
            }
        });
        transfCB.setButtonCell(transfCB.getCellFactory().call(null));

        
        // layout
        HBox l11 = new HBox(5, inTa.getNode(),allBox);
        HBox l12 = new HBox(5, transfCB, splitter.getNode());
        VBox l2 = new VBox(5, l11,l12);
        VBox.setVgrow(l11, ALWAYS);
        getChildren().add(l2);
        setAnchors(l2,0);
        
        
        // behavior
        addEventHandler(DRAG_OVER,DragUtil.audioDragAccepthandler);
        addEventHandler(DRAG_OVER,DragUtil.fileDragAccepthandler);
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles())
                source.setAll(filter(db.getFiles(),File::isFile));
            else if(DragUtil.hasItemList())
                read(DragUtil.getItemsList());
        });
        
        
        // on source change run transformation
        source.addListener((Change change) -> {
            if(source.isEmpty()) {
                transform();            // fires update
            } else {
                Class c = source.get(0).getClass();
                transfCB.getItems().setAll(Functors.getI(c));
                transfCB.getItems().sort((c1,c2) -> c1.name.compareTo(c2.name));
                transfCB.setValue(Functors.getPrefI(c));   // fires update
                transform();
                
                applier.actCB.getItems().setAll(acts.getElementsOfSuper(c));
                applier.actCB.setValue(applier.actCB.getItems().get(0));
            }
        });
        transfCB.valueProperty().addListener((o,ov,nv) -> transform());
        splitter.onItemChange = f -> transform();
        
        inTa.onItemChange = lines -> {
            StringSplitParser p = splitter.getValue();
            List<List<String>> outs = list(p.parse_keys.size(), ArrayList::new);
            lines.forEach(line -> forEachI(p.apply(line), (i,l)->outs.get(i).add(l)));
            outTas = list(p.parse_keys.size(), () -> new TA(()->Functors.getIO(String.class)));
            tas.setAll(gatherTas());
            forEachI(outs, (i,lins) -> outTas.get(i).setData(p.parse_keys.get(i), lins));
            outTFBox.getChildren().setAll(map(outTas,StringListItemNode::getNode));    
        };
        
        // init data
        acts.accumulate(new Act<>("Rename files", File.class, 1, () -> list("Filename"), (file, data) -> {
            String name = data.values().stream().findFirst().get();
            rename_file.accept(file, name);
        }));
        acts.accumulate(new Act<>("Tag songs", Metadata.class, 100, () -> map(getEnumConstants(Metadata.Field.class),Object::toString), (file, data) -> {
//            String 
        }));
        acts.accumulate(new Act<>("Write file", Object.class, 1, () -> list("Contents"), (file, data) -> {
//            String 
        }));
    }
    
    private void transform() {
        String s;
        if(source.isEmpty()) {
            s = "";
        } else {
            PF f = transfCB.getValue();
            forbidNull(f);
            TransformableList in = of(source);
            TransformableList out = in.transform(new Transformer(f.name, f.toFunction()));
            s = (String) out.getListOut().map(toString).collect(joining("\n"));
        }
        
        inTa.setData(list(s.split("\n")));
    }
    
    
    public List<String> gatherTasNames() {
        List<String> l = new ArrayList();
        if(outTas!=null) outTas.forEach(t -> { if(t!=null) l.add(t.name.get()); });
        l.add(inTa.name.get());
        return l;
//        return map((List<TA>)list(outTas == null ? EMPTY_LIST : outTas, inTa),t->t.name.get());
    }    
    public List<TA> gatherTas() {
        List<TA> l = new ArrayList();
        if(outTas!=null) outTas.forEach(t -> { if(t!=null) l.add(t); });
        l.add(inTa);
        return l;
//        return map((List<TA>)list(outTas == null ? EMPTY_LIST : outTas, inTa),t->t.name.get());
    }    

/******************************** features ************************************/
    
    /** {@inheritDoc} */
    @Override
    public void read(List<? extends Item> items) {
        source.setAll(map(items,Item::toMeta));
    }
    
/******************************* helper classes *******************************/
    
    private class TA extends StringListItemNode {
        private final Label nameL = new Label("");
        public final StringProperty name = nameL.textProperty();
        
        public TA(Supplier<PrefList<PF<String, String>>> functionPool) {
            this("",functionPool);
        }
        public TA(String name, Supplier<PrefList<PF<String, String>>> functionPool) {
            super(functionPool);
            getNode().getChildren().add(0,new StackPane(nameL));
            setData(name, EMPTY_LIST);
        }

        public void setData(String name, List<String> input) {
            super.setData(input);
            this.name.set(capitalizeStrong(name));
        }
    }
    private class Applier {
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
            boolean empty = action==null || source.isEmpty() || ins.getValues().count()==0;
            if(empty) return;
            
            boolean in_out_type_match = action!=null && action.type.isInstance(source.get(0));
            boolean same_size = equalBy(tas, ta->ta.getValue().size());
            if(!in_out_type_match || !same_size) return;
            
            Map<String,List<String>> m = tas.stream().collect(toMap(ta->ta.name.get(),ta ->ta.getValue()));
//                Map<String,List<String>> mm = mapKeys(m, in_name -> ins.getValues().filter(in->in._2.equals(in_name)).findFirst().get()._1);
            for(int i=0; i<source.size(); i++)
                action.action.accept(source.get(i), mapSlice(m,i));
        });
        VBox root = new VBox(5, new StackPane(new Label("Action")),actCB, new StackPane(new Label("Apply", okB)));
        
        public Applier() {
            okB.setPadding(new Insets(8));
            actCB.valueProperty().addListener((o,ov,nv) -> {
                if(ins!=null) root.getChildren().remove(ins.getNode());
                ins = new ListConfigField<>(() -> new In(nv.names,() -> gatherTasNames()));
                ins.maxChainLength.set(nv.max);
                root.getChildren().add(2,ins.getNode());
            });
        }
    }
    private static class Act<V> {
        String name;
        int max = MAX_VALUE;
        Supplier<List<String>> names;
        Class type;
        BiConsumer<V,Map<String,String>> action;

        public Act(String name, Class<V> type, int max, Supplier<List<String>> names, BiConsumer<V,Map<String,String>> action) {
            this.name = name;
            this.type = type;
            this.max = max;
            this.names = names;
            this.action = action;
        }
    }
    private static class In extends ItemNode<Tuple2<String,String>> {
        Accessor<String> name;
        Accessor<String> input;
        ConfigField<String> configfieldA;
        ConfigField<String> configfieldB;
        HBox root;
        
        public In(Supplier<Collection<String>> actions, Supplier<Collection<String>> inputs) {
            name = new AccessorEnum<>(actions.get().stream().findFirst().get(),actions);
            input = new AccessorEnum<>(inputs.get().stream().findFirst().get(),inputs);
            configfieldA = ConfigField.create(Config.fromProperty("", name));
            configfieldB = ConfigField.create(Config.fromProperty("", input));
            root = new HBox(5, configfieldA.getNode(),configfieldB.getNode());
        }
        @Override
        public Tuple2<String,String> getValue() {
            return util.collections.Tuples.tuple(configfieldA.getItem(), configfieldB.getItem());
        }

        @Override
        public Node getNode() {
            return root;
        }
    }
    
    private static BiConsumer<File,String> rename_file = (f,s) -> {
        File rf = f.getParentFile().getAbsoluteFile();
        int dot = f.getPath().lastIndexOf('.');
        String p = f.getPath();
        String ext = p.substring(dot,p.length());
        f.renameTo(new File(rf, filenamizeString(s)+ext));
    };
}