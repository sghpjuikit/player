/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import GUI.ItemNode.StringSplitParser;
import GUI.ItemNode.TextListArea;
import GUI.ItemNode.TransformableList;
import GUI.ItemNode.StringSplitGenerator;
import util.functional.Functors;
import Layout.Widgets.Controller;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.ComboBoxListCell;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import static util.Util.setAnchors;
import util.functional.Functors.PF;
import GUI.ItemNode.TransformableList.Transformer;
import static GUI.ItemNode.TransformableList.of;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;

@IsWidget
public class Converter extends AnchorPane implements Controller<Widget> {
    
    private final ObservableList source = FXCollections.observableArrayList();
    List<TransformableList> transforms = new ArrayList();
    
    private final TextArea original = new TextArea();
    private final HBox outTFBox = new HBox(5);
    private final ComboBox<PF> transfCB = new ComboBox();
    StringSplitGenerator splitter = new StringSplitGenerator();
    
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
        HBox l11 = new HBox(5, original,outTFBox);
        HBox l12 = new HBox(5, transfCB, splitter.getNode());
        VBox l2 = new VBox(5, l11,l12);
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
        
        try {
            StringSplitParser p = splitter.getValue();
            List<List<String>> outs = list(p.parse_keys.size(), ArrayList::new);
            Stream.of(original.getText().split("\n"))
                  .forEach(line -> forEachI(p.apply(line), (i,l)->outs.get(i).add(l)));
            List<TextListArea> tas = list(p.parse_keys.size(), () -> new TextListArea(()->Functors.getIO(String.class)));
            forEachI(outs, (i,lines) -> tas.get(i).setData(p.parse_keys.get(i), lines));
            outTFBox.getChildren().setAll(map(tas,TextListArea::getNode));            
        } catch(Exception e) {
//        } catch(ParseException e) {
            System.out.println(e);
        }
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
}