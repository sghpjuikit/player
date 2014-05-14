/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package unused;

import java.util.List;
import java.util.stream.Stream;
import utilities.functional.functor.UnProcedure;

/**
 *
 * @author Plutonium_
 */
public class For<E extends Object> implements Loop<List<E>,UnProcedure<E>>{
    private E[] els;

    
    public For(){
        els = (E[])Stream.empty().toArray();
    }
    public For(E... els) {
        this.els = els;
    }
    
    
    public void run(UnProcedure<E> procedure) {
        Stream.of(els).forEach(procedure::accept);
    }
    public void run(List<E> elements, UnProcedure<E> procedure) {
        eachRun(elements, procedure);
    }
    public void run(UnProcedure<E> procedure, E... elements) {
        eachRun(procedure, elements);
    }
    public Stream<E> stream() {
        return Stream.of(els);
    }
    
    
    public static<E> For<E> each(E... els) {
        return new For(els);
    }
    public static <E> void eachRun(List<E> elements, UnProcedure<E> procedure) {
        elements.forEach(procedure::accept);
    }
    public static <E> void run(E[] elements, UnProcedure<E> procedure) {
        Stream.of(elements).forEach(procedure::accept);  
    }
    public static <E> void eachRun(UnProcedure<E> procedure, E... elements) {
        Stream.of(elements).forEach(procedure::accept);
    } 
}
