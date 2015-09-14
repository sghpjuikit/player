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

import AudioPlayer.tagging.Metadata;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.collections.Tuple2;
import util.collections.Tuple3;

import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class PredicateChainItemNode<T extends FieldedValue,F extends FieldEnum<T>> extends ChainValueNode<Tuple2<Predicate<Object>,F>,PredicateItemNode<F>> {

    private final List<Tuple3<String,Class,F>> data = new ArrayList();
    // normally we would use this predicate builder:
    //     (field,filter) -> element -> filter.test(element.getField(field));
    // but getField(field) can return null!, which Predicate can not handle on its own
    // we end up with nullsafe alternative:
    //     private final BiFunction<F,Predicate<Object>,Predicate<T>> converter = (field,filter) -> element -> {
    //         Object o = element.getField(field);
    //         return o==null ? false : filter.test(o);
    //     };
    // which has 1 problem:
    //    o -> o==null (isNull) predicate will get bypassed and wont have any effect
    // leading us to predicate identity preservation and ultimate below.
    //
    // One could argue predicate isNull is useless in OOP, particularly for filtering, like here,
    // but ultimately, it has it's place and we should'nt ignore it out of convenience.
    // In this particular case, where we are filtering FieldedTable, isNull shouldnt be used, rather
    // isEmpty() predicate should check: element.getField(field).equals(EMPTY_ELEMENT.getField(field))
    // where null.equals(null) would return true.
    // However, in my opinion, isNull predicate does not lose its value completely. I'll remain
    // relying on predicate identity, despite introducing ~spagetti code dependency here, as I
    // dont see any other way (not without subclassing Predicate across whole functionality)
    private final BiFunction<F,Predicate<Object>,Predicate<T>> converter = (field,filter) -> {
            if(field==Metadata.Field.FIRST_PLAYED) {
                System.out.println((filter==ISØ) + " " + (filter==ISNTØ) + " " + (filter==ALL) + " " + (filter==NONE));
            }
            return isInR(filter, ISØ,ISNTØ,ALL,NONE)
                    ? element -> filter.test(element.getField(field))
                    : element -> {
                          Object o = element.getField(field);
                          return o==null ? false : filter.test(o);
                      };
    };
//    private final BiFunction<F,Predicate<Object>,Predicate<T>> converter = (field,filter) ->
//            isInR(filter, ISØ,ISNTØ,ALL,NONE)
//                    ? element -> filter.test(element.getField(field))
//                    : element -> {
//                          Object o = element.getField(field);
//                          return o==null ? false : filter.test(o);
//                      };
    private Predicate<T> conjuction;
    private boolean inconsistent_state = false;
    private Consumer<Predicate<T>> onFilterChange;

    public PredicateChainItemNode(Supplier<PredicateItemNode<F>> chainedFactory) {
        this(1,chainedFactory);
    }

    public PredicateChainItemNode(int i, Supplier<PredicateItemNode<F>> chainedFactory) {
        super(i, chainedFactory);
        conjuction = (Predicate)ALL;
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
        conjuction = chain.stream().filter(g->g.on.getValue())
                                   .map(g->g.chained.getValue()).filter(ISNTØ)
                                   .map(g->converter.apply(g._2,g._1))
                                   .reduce(Predicate::and).orElse((Predicate)ALL);
        if(onFilterChange!=null) onFilterChange.accept(conjuction);
    }

    @Override
    protected Tuple2<Predicate<Object>, F> reduce(Stream<Tuple2<Predicate<Object>, F>> values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}