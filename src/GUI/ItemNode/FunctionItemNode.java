/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import Configuration.AccessorConfig;
import Configuration.Config;
import GUI.ItemNode.ConfigField;
import GUI.ItemNode.ItemNode;
import GUI.objects.combobox.ImprovedComboBox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import util.access.Accessor;
import util.collections.PrefList;
import util.functional.Functors.PF;
import static util.functional.Util.cmpareNoCase;

/**
 *
 * @author Plutonium_
 */
public class FunctionItemNode<IN,OUT> extends ItemNode<Function<IN,OUT>> {
    private final HBox root = new HBox(5);
    private final HBox paramB = new HBox(5);
    private final List<ConfigField> configs = new ArrayList();
    private final ComboBox<PF<IN,OUT>> fCB;
    
    public FunctionItemNode(Supplier<PrefList<PF<IN,OUT>>> functionPool) {
        fCB = new ImprovedComboBox<>(f->f.name);
        fCB.valueProperty().addListener((o,ov,nv) -> {
            configs.clear();
            paramB.getChildren().clear();
            nv.getParameters().forEach(p -> {
                Accessor a = new Accessor(p.defaultValue, v -> generateValue(nv));
                Config cg = new AccessorConfig("",a::setNapplyValue,a::getValue);
                ConfigField cf = ConfigField.create(cg);
                configs.add(cf);
                paramB.getChildren().add(cf.getNode());
            });
            if(!configs.isEmpty()) HBox.setHgrow(configs.get(configs.size()-1).getNode(), ALWAYS);
            generateValue(nv);
        });
        
        fCB.getItems().setAll(functionPool.get());
        fCB.getItems().sort(cmpareNoCase(f->f.name));
        generateValue(functionPool.get().getPrefered());
        changeValue(null);   // initializes value, dont fire update yet
        
        root.getChildren().addAll(fCB,paramB);
    }
    
    @Override
    public HBox getNode() {
        return root;
    }

    /** 
     * Focuses the first parameter's config field if any.
     * <p>
     * {@inheritDoc }
     */
    @Override
    public void focus() {
        if(!configs.isEmpty())
            configs.get(0).focus();
    }
    
    private void generateValue(PF<IN,OUT> f) {
        fCB.setValue(f);
        changeValue(in -> f.apply(in,configs.stream().map(ConfigField::getItem).toArray()));
    }
}