/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.time.Duration;
import java.util.function.Consumer;
import org.reactfx.EventSink;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class AccessibleStream<T> implements EventStream<T>, EventSink<T>, AccessibleValue<T> {
    private T value;
    private final EventStream<T> source;
    private final EventStream<T> stream;
    
    public AccessibleStream(T initialValue) {
        this(initialValue, new EventSource());
    }
    
    public AccessibleStream(T initialValue, EventStream<T> source_stream) {
        value = initialValue;
        source = source_stream;
        stream = source instanceof EventSource ? source.successionEnds(Duration.ofMillis(100)) : source;
    }
    

    /** @retuen last value emitted by this stream. */
    @Override
    public T getValue() {
        return value;
    }

    /**
     * Set last value emitted by this stream.
     * <p>
     * The value is set automatically only when the backing EventStream is an
     * EventSource, by calling {@link #push()}. Other streams must manually set
     * the value when they emit it.
     */
    @Override
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public Subscription subscribe(Consumer<? super T> subscriber) {
        return stream.subscribe(subscriber);
    }

    @Override
    public Subscription monitor(Consumer<? super Throwable> monitor) {
        return stream.monitor(monitor);
    }

    /**
     * Causes immediate emit of the pushed value. The value is stored for
     * retrieval with {@link #getValue()}.
     * 
     * @param value 
     * @throws UnsupportedOperationException when backing stream not an event
     * source.
     */
    @Override
    public void push(T value) {
        this.value = value;
        if (source instanceof EventSource) {
            EventSource.class.cast(source).push(value);
        } else {
            throw new UnsupportedOperationException("Can not push value. Event stream is not an Event Source.");
        }
    }
}
