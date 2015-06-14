/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @author Plutonium_
 */
public interface TransformableList<I,O> {
      
    public Stream<I> getListIn();
    public Stream<O> getListOut();
    public default <OO> TransformableList<O,OO> transform(Transformer<O,OO> t) {
        return new TLSimple<O,OO>(this, t);
    }    

    
    public static <I> TransformableList<I,I> of(List<I> list) {
        return new TLSource(list);
    }
    public static <I> TransformableList<I,I> of(Supplier<List<I>> list_supplier) {
        return new TLSource(list_supplier);
    }
    
    
    static abstract class TLBase<I,O> implements TransformableList<I,O> {
        final Transformer<I,O> transformer;

        TLBase(Transformer<I, O> transformer) {
            this.transformer = transformer;
        }

        public Transformer<I,O> getTransformer() {
            return transformer;
        }

    }  
    public static class TLSimple<I,O> extends TLBase<I,O> {
        public final TransformableList<?,I> previous;

        private <II> TLSimple(TransformableList<II,I> previous, Transformer<I,O> transformer) {
            super(transformer);
            this.previous = previous;
        }

        @Override
        public Stream<I> getListIn() {
            return previous.getListOut();
        }

        @Override
        public Stream<O> getListOut() {
            return getListIn().map(transformer.transformation);
        }
    }
    public static class TLSource<I> extends TLBase<I,I> {
        private final Supplier<Collection<I>> source;
        
        private TLSource(List<I> list) {
            this(() -> list);
        }
        private TLSource(Supplier<Collection<I>> list_supplier) {
            super(new Transformer<>("Original",x->x));
            this.source = list_supplier;
        }

        @Override
        public Stream<I> getListIn() {
            return source.get().stream();
        }

        @Override
        public Stream<I> getListOut() {
            return source.get().stream();
        } 
    }
    
    public static class Transformer<I,O> {
        public final String name;
        private final Function<I,O> transformation;

        public Transformer(String name, Function<I, O> transformation) {
            this.name = name;
            this.transformation = transformation;
        }

    }
}
