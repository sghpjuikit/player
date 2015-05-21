/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import GUI.ItemNode.ItemNode;
import GUI.objects.CheckIcon;
import GUI.objects.combobox.ImprovedComboBox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.util.Callback;
import util.collections.PrefList;
import util.collections.Tuple2;
import util.collections.Tuple3;
import static util.collections.Tuples.tuple;
import GUI.ItemNode.FunctionConfigField;
import util.functional.Functors.PF;
import static util.functional.Util.isTRUE;

/**
 *
 * @author Plutonium_
 */
public class FilterGenerator<T> extends ItemNode<Tuple2<Predicate<Object>,T>> {
    
    private static final Tooltip negTooltip = new Tooltip("Negate");
        
    private final ComboBox<Tuple3<String,Class,T>> typeCB = new ImprovedComboBox<>(t->t._1);
    private FunctionConfigField<Object,Boolean> config;
    private final CheckIcon negB = new CheckIcon(false, "filter-negate-icon");
    private final HBox root = new HBox(5,negB,typeCB);
    
    private final Callback<Class,PF<?,Boolean>> ppPool;
    private final Callback<Class,PrefList<PF<?,Boolean>>> pPool;
    private Supplier<Tuple3<String,Class,T>> prefTypeSupplier;
    boolean inconsistentState = false;
    
    public FilterGenerator(Callback<Class,PrefList<PF<?,Boolean>>> predicatePool, Callback<Class,PF<?,Boolean>> prefPredicatePool) {
        pPool = predicatePool;
        ppPool = prefPredicatePool;
        root.setAlignment(CENTER_LEFT);
        typeCB.setVisibleRowCount(25);
        typeCB.valueProperty().addListener((o,ov,nv) -> {System.out.println("tt");
            if(config!=null) root.getChildren().remove(config.getNode());
            config = new FunctionConfigField(() -> pPool.call(nv._2));
            root.getChildren().add(config.getNode());
            HBox.setHgrow(config.getNode(), ALWAYS);
            config.onItemChange = v -> generatePredicate();
            generatePredicate();
        });
        negB.icon_size.set(13);
        negB.selected.addListener((o,nv,ov) -> generatePredicate());
        negB.setTooltip(negTooltip);
    }
    
    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,T>> supplier) {
        prefTypeSupplier = supplier;
    }
    
    /**
     * Sets chosable data specifying what filter can be generated in form of list
     * of tri-tuples : displayed name, class, passed object.
     * <pre>
     * The name is what will be displayed in the combobox to choose from
     * The class specifies the type of object the filter is generated for.
     * The passed object's purpose is to be returned along with the filter, mostly to be used in the generated filter
     * </pre>
     * <p>
     * If there is no object to pass, use null.
     * @param classes 
     */
    public void setData(List<Tuple3<String,Class,T>> classes) {
        List<Tuple3<String,Class,T>> cs = new ArrayList(classes);
        // cs.removeIf(e->pPool.call(unPrimitivize(e._2)).isEmpty()); // remove unsupported
        inconsistentState = true;
        typeCB.getItems().setAll(cs);
        inconsistentState = false;
        
        Tuple3<String,Class,T> v = prefTypeSupplier == null ? null : prefTypeSupplier.get();
        if (v==null) v = cs.isEmpty() ? null : cs.get(0);
        typeCB.setValue(v);
    }
    
    /** 
     * Focuses the filter's first parameter's config field if any.
     * <p>
     * {@inheritDoc }
     */
    @Override
    public void focus() {
        config.focus();
    }
    
    private boolean empty = true;
    
    public boolean isEmpty() {
        return empty;
    }
    
    public void clear() {
        empty = true;
        value = tuple(isTRUE, prefTypeSupplier.get()._3);
    }
    
    private void generatePredicate() {
        if(inconsistentState) return;
        empty = false;
        Function<Object,Boolean> p = config.getValue();
        T o = typeCB.getValue()==null ? null : typeCB.getValue()._3;
        if(p!=null) {
            Predicate<Object> pr = p::apply;
            if(negB.selected.get()) pr = pr.negate();
            if(p!=null) changeValue(tuple(pr,o));
        }
    }

    @Override
    public Node getNode() {
        return root;
    }
    
}