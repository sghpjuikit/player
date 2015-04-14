/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.async.executor;

import java.util.concurrent.Executor;

/**
 * Executor with an execution count limit.
 * <p>
 * Guarantees the number of executions, for example one may wish to execute an
 * action at most once.
 * 
 * @author Plutonium_
 */
public class Runner implements Executor {
    
    private long executed = 0;
    private final long max;

    /** @param limit maximum number of times this executor can execute any 
        runnable */
    public Runner(long limit) {
        max = limit;
    }
    
    @Override
    public void execute(Runnable r) {
        if(executed<max) r.run();
        executed++;
    }

}