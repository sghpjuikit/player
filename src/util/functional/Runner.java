/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

/**
 *  Runnable executor with an execution count limit.
 * 
 * @author Plutonium_
 */
public class Runner {
    
    private long executed = 0;
    private final long max;

    public Runner(long limit) {
        max = limit;
    }
    
    public void run(Runnable r) {
        if(executed<max) r.run();
        executed++;
    }
    
}