/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import java.util.stream.Stream;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.collections.Tuple2;
import util.collections.Tuple3;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class PredicateChainItemNode<T extends FieldedValue,F extends FieldEnum<T>> extends ChainConfigField<Tuple2<Predicate<Object>,F>,PredicateItemNode<F>> {

    private final List<Tuple3<String,Class,F>> data = new ArrayList();
    private BiFunction<F,Predicate<Object>,Predicate<T>> converter;
    private Predicate<T> conjuction;
    private boolean inconsistent_state = false;
    private Consumer<Predicate<T>> onFilterChange;
    
    public PredicateChainItemNode(Supplier<PredicateItemNode<F>> chainedFactory) {
        this(1,chainedFactory);
    }

    public PredicateChainItemNode(int i, Supplier<PredicateItemNode<F>> chainedFactory) {
        super(i, chainedFactory);
        conjuction = isTRUE;
    }
    

    protected List<Tuple3<String,Class,F>> getData() {
        return data;
    } 

    public void setData(List<Tuple3<String,Class,F>> classes) {
        inconsistent_state = true;
        data.clear(); // causes serious problems, unknown
        data.addAll(classes);
        chain.forEach(g->g.chained.setData(classes));
        clear(); // bug fix, not sure if it doesnt cause problems
    }

    public void setMapper(BiFunction<F,Predicate<Object>,Predicate<T>> mapper) {
        this.converter = mapper;
    }

    public void setOnFilterChange(Consumer<Predicate<T>> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    
    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,F>> supplier) {
        chain.forEach(g->g.chained.setPrefTypeSupplier(supplier));
    }

    public boolean isEmpty() {
        return chain.stream().allMatch(c->c.chained.isEmpty());
    }

    public void clear() {
        inconsistent_state = true;
        chain.setAll(chain.get(0));
        chain.forEach(c->c.chained.clear());
        inconsistent_state = false;
        generateValue();
    }

    @Override
    protected void generateValue() {
        if(inconsistent_state || converter==null) return;
        conjuction = chain.stream().filter(g->g.on.get())
                                    .map(g->g.chained.getValue()).filter(isNotNULL)
                                    .map(g->converter.apply(g._2,g._1))
                                    .reduce(Predicate::and).orElse(isTRUE);
        if(onFilterChange!=null) onFilterChange.accept(conjuction);
    }

    @Override
    protected Tuple2<Predicate<Object>, F> reduce(Stream<Tuple2<Predicate<Object>, F>> values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}