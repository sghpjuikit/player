/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import Configuration.AccessorConfig;
import Configuration.Config;
import gui.itemnode.ItemNode.ValueNode;
import gui.objects.combobox.ImprovedComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

import static javafx.scene.layout.Priority.ALWAYS;

import util.access.Var;
import util.collections.PrefList;
import util.functional.Functors.F1;
import util.functional.Functors.PF;

import static util.functional.Util.*;

/**
 * Value node containing function of one parameter {@link F1}.
 * 
 * @param <IN> type of function input
 * @param <OUT> type of function output
 * 
 * @author Plutonium_
 */
public class FunctionItemNode<IN,OUT> extends ValueNode<F1<IN,OUT>> {
    private final HBox root = new HBox(5);
    private final HBox paramB = new HBox(5);
    private final List<ConfigField> configs = new ArrayList();
    private final ComboBox<PF<IN,OUT>> fCB;
    
    public FunctionItemNode(Supplier<PrefList<PF<IN,OUT>>> functionPool) {
        fCB = new ImprovedComboBox<>(f->f.name);
        fCB.getItems().setAll(functionPool.get());
        fCB.getItems().sort(byNC(f->f.name));
        fCB.valueProperty().addListener((o,ov,nv) -> {
            configs.clear();
            paramB.getChildren().clear();
            nv.getParameters().forEach(p -> {
                Var a = new Var(p.defaultValue, v -> generateValue());
                Config cg = new AccessorConfig("",a::setNapplyValue,a::getValue);
                ConfigField cf = ConfigField.create(cg);
                configs.add(cf);
                paramB.getChildren().add(cf.getNode());
            });
            if(!configs.isEmpty()) HBox.setHgrow(configs.get(configs.size()-1).getNode(), ALWAYS);
            generateValue();
        });
        fCB.setValue(functionPool.get().getPrefered()); // generate
        
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
    
    private void generateValue() {
        PF<IN,OUT> f = fCB.getValue();
        changeValue(in -> f.apply(in,configs.stream().map(ConfigField::getValue).toArray()));
    }
    
    public Class getTypeIn() {
        PF<IN,OUT> f = fCB.getValue();
        return f==null ? Void.class : f.in;
    }
    
    public Class getTypeOut() {
        PF<IN,OUT> f = fCB.getValue();
        return f==null ? Void.class : f.out;
    }

}