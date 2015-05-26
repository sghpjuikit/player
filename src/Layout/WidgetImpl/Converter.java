/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import GUI.ItemNode.StringListItemNode;
import GUI.ItemNode.StringSplitGenerator;
import GUI.ItemNode.StringSplitParser;
import GUI.ItemNode.TransformableList;
import GUI.ItemNode.TransformableList.Transformer;
import static GUI.ItemNode.TransformableList.of;
import GUI.objects.Icon;
import Layout.Widgets.Controller;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import java.io.File;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.Util.*;
import util.collections.PrefList;
import util.functional.Functors;
import util.functional.Functors.PF;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;

@IsWidget
public class Converter extends AnchorPane implements Controller<Widget> {
    
    private final ObservableList source = FXCollections.observableArrayList();
    List<TransformableList> transforms = new ArrayList();
    
    private final TextArea original = new TextArea();
    private final HBox outTFBox = new HBox(5);
    private final HBox allBox = new HBox(5,outTFBox, new Applier("Input").root);
    private final ComboBox<PF> transfCB = new ComboBox();
    StringSplitGenerator splitter = new StringSplitGenerator();
    List<TA> tas;
    
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
        HBox l11 = new HBox(5, original,allBox);
        HBox l12 = new HBox(5, transfCB, splitter.getNode());
        VBox l2 = new VBox(5, l11,l12);
         VBox.setVgrow(l11, ALWAYS);
        getChildren().add(l2);
        setAnchors(l2,0);
        
        
        // behavior
        addEventHandler(DRAG_OVER,DragUtil.audioDragAccepthandler);
        addEventHandler(DRAG_OVER,e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                e.consume();
            }
        });
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles())
                source.setAll(filter(db.getFiles(),File::isFile));
            else if(DragUtil.hasAudio(db))
                source.setAll(DragUtil.getAudioItems(e));
            
        });
        
        
        // on source change run transformation
        source.addListener((Change change) -> {
            if(source.isEmpty()) {
                transform();            // fires update
            } else {
                Class c = source.get(0).getClass();
                PF f = (PF)Functors.getI(c).get(0); // prefered
                transfCB.getItems().setAll(Functors.getI(c));
                transfCB.getItems().sort((c1,c2) -> c1.name.compareTo(c2.name));
                transfCB.setValue(f);   // fires update
                transform();
            }
        });
        transfCB.valueProperty().addListener((o,ov,nv) -> transform());
        splitter.onItemChange = f -> transform();
        
        original.textProperty().addListener((o,ov,nv) -> {
            StringSplitParser p = splitter.getValue();
            List<List<String>> outs = list(p.parse_keys.size(), ArrayList::new);
            Stream.of(original.getText().split("\n")).forEach(line -> forEachI(p.apply(line), (i,l)->outs.get(i).add(l)));
            tas = list(p.parse_keys.size(), () -> new TA(()->Functors.getIO(String.class)));
            forEachI(outs, (i,lines) -> tas.get(i).setData(p.parse_keys.get(i), lines));
            outTFBox.getChildren().setAll(map(tas,StringListItemNode::getNode));    
        });
    
    }
    
    private void transform() {
        String s;
        if(source.isEmpty()) {
            s = "";
        } else {
            Class c = source.get(0).getClass();
            PF f = transfCB.getValue();
            TransformableList in = of(source);
            TransformableList out = in.transform(new Transformer(f.name, f.toFunction()));
            s = (String) out.getListOut().map(Object::toString).collect(joining("\n"));
        }
        
        original.setText(s);
    }
    
    
    
    private Widget widget;
    
    @Override public void refresh() {
    }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
    
    
    private class TA extends StringListItemNode {
        private final Label nameL = new Label();
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
        private final TextField inTF = new TextField();
        public final StringProperty name = inTF.textProperty();
        BiConsumer<File,String> applier = (f,s) -> {
            File rf = f.getParentFile().getAbsoluteFile();
            int dot = f.getPath().lastIndexOf('.');
            String p = f.getPath();
            String ext = p.substring(dot,p.length());
            f.renameTo(new File(rf, filenamizeString(s)+ext));
        };
        private final Icon okB = new Icon(FontAwesomeIconName.AT, 13, "Apply", () -> {
            if(!source.isEmpty() && source.get(0) instanceof File && tas.stream().anyMatch(t->t.name.get().equals(name.get()))) {
                List<File> files = source;
                List<String> names = tas.stream().filter(t->t.name.get().equals(name.get())).findAny().get().getValue();
                if(files.size()!=names.size()) return;
                for(int i=0; i<files.size(); i++)
                    applier.accept(files.get(i), names.get(i));
            }
        });
        
        VBox root = new VBox(5, inTF, okB);
        
        public Applier(String name) {
            this.name.set(name);
        }
    }
}