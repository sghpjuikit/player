/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

import util.conf.AccessorConfig;
import util.conf.Config;
import gui.itemnode.ItemNode.ValueNode;
import gui.objects.combobox.ImprovedComboBox;
import util.access.V;
import util.collections.list.PrefList;
import util.functional.Functors.PƑ;
import util.functional.Functors.Ƒ1;

import static javafx.scene.layout.Priority.ALWAYS;
import static util.functional.Util.*;

/**
 * Value node containing function of one parameter {@link Ƒ1}.
 *
 * @param <I> type of function input
 * @param <O> type of function output
 *
 * @author Martin Polakovic
 */
public class FItemNode<I,O> extends ValueNode<Ƒ1<I,O>> {
    private final HBox root = new HBox(5);
    private final HBox paramB = new HBox(5);
    private final List<ConfigField> configs = new ArrayList<>();
    private final ComboBox<PƑ<I,O>> fCB;

    public FItemNode(Supplier<PrefList<PƑ<I,O>>> functionPool) {
        fCB = new ImprovedComboBox<>(f -> f.name);
        fCB.getItems().setAll(functionPool.get());
        fCB.getItems().sort(byNC(f -> f.name));
        fCB.valueProperty().addListener((o,ov,nv) -> {
            configs.clear();
            paramB.getChildren().clear();
            nv.getParameters().forEach(p -> {
                V a = new V<>(p.defaultValue, v -> generateValue());
                Config cg = new AccessorConfig(p.type,p.name,p.description,a::setNapplyValue,a::getValue);
                ConfigField cf = ConfigField.create(cg);
                configs.add(cf);
                paramB.getChildren().add(cf.getNode());
            });
            if (!configs.isEmpty()) HBox.setHgrow(configs.get(configs.size()-1).getNode(), ALWAYS);
            generateValue();
        });
        fCB.setValue(functionPool.get().getPreferedOrFirst()); // generate

        root.getChildren().addAll(fCB,paramB);
    }

    @Override
    public HBox getNode() {
        return root;
    }

    /**
     * Focuses the first parameter's config field if any.
     * <p/>
     * {@inheritDoc }
     */
    @Override
    public void focus() {
        if (!configs.isEmpty())
            configs.get(0).focus();
    }

    private void generateValue() {
        PƑ<I,O> f = fCB.getValue();
        changeValue(f.toƑ1(configs.stream().map(ConfigField::getValue).toArray()));
    }

    public Class getTypeIn() {
        PƑ<I,O> f = fCB.getValue();
        return f==null ? Void.class : f.in;
    }

    public Class getTypeOut() {
        PƑ<I,O> f = fCB.getValue();
        return f==null ? Void.class : f.out;
    }

}