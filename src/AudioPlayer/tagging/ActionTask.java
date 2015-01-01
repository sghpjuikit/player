/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import java.util.function.Supplier;

/**
 *
 * @author Plutonium_
 */
public final class ActionTask<T> extends SuccessTask<T,ActionTask<T>> {

    private String title;
    private Supplier<T> r;
    
    
    public ActionTask() {
        super();
    }
    
    public ActionTask(String title) {
        this();
        setTitle(title);
    }
    
    public ActionTask(String title, Supplier<T> action) {
        this(title);
        setAction(action);
    }
    
    
    public ActionTask<T> setTitle(String title) {
        this.title = title;
        return this;
    }
    
    public ActionTask<T> setAction(Supplier<T> action) {
        this.r = action;
        return this;
    }
    
    public ActionTask<Void> setAction(Runnable action) {
        this.r = ()-> {
            action.run();
            return null;
        };
        return (ActionTask<Void>) this;
    }
    
    @Override
    protected T call() throws Exception {
        updateTitle(title);
        updateMessage(title + " ...");
        updateProgress(-1, 1);
        T t = r.get();
        updateProgress(1, 1);
        return t;
    }
}
