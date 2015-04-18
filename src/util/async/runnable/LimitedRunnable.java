/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.runnable;

/**
 * Runnable with an execution count limit. 
 * <p>
 * Guarantees the number of executions (irrelevant of the Executor), a
 * one may wish to execute this runnable at most n times.
 * <p>
 * Wraps the underlying runnable.
 * 
 * @author Plutonium_
 */
public class LimitedRunnable implements Run {
    
    private long executed = 0;
    private final long max;
    private final Runnable x;

    /** 
     * @param limit maximum number of times the runnable can execute
     * @param action action that will execute when this runnable executes
     */
    public LimitedRunnable(long limit, Runnable action) {
        max = limit;
        x = action;
    }
    
    @Override
    public void run() {
        if(executed<max) x.run();
        executed++;
    }

}