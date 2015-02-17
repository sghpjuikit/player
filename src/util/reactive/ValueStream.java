/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.reactive;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import static org.reactfx.EventStreams.merge;
import org.reactfx.Subscription;
import util.access.AccessibleValue;

/**
 */
    public class ValueStream<T> extends EventStreamBase<T> implements AccessibleValue<T>{
        private T v;
        private final EventStream<T> source;
        
        public ValueStream(T initialValue, EventStream<? extends T>... sources) {
            v = initialValue;
            this.source = merge(sources);
        }
        
        @Override
        public T getValue() {
            return v;
        }

        @Override
        public void setValue(T event) {
            emit(event);
        }

        @Override
        public void emit(T value) {
            v = value;
            super.emit(value);
        }

        @Override
        protected Subscription observeInputs() {
            return source.subscribe(this::emit);
        }
    }
