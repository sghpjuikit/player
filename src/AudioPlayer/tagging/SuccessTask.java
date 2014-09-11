/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

import java.util.function.BiConsumer;
import javafx.concurrent.Task;

/**
 *
 * @author Plutonium_
 */
public abstract class SuccessTask<T> extends Task<T> {
    
    private BiConsumer<Boolean,T> onEnd;
    protected final StringBuffer sb = new StringBuffer(40);
    
    public SuccessTask() {
        super();
    }
    
    public SuccessTask(BiConsumer<Boolean,T> onEnd) {
        super();
        setOnEnd(onEnd);
    }
    
    public SuccessTask(String title, BiConsumer<Boolean,T> onEnd) {
        super();
        setOnEnd(onEnd);
        updateTitle(title);
    }
    
    
    public final void setOnEnd(BiConsumer<Boolean,T> onEnd) {
        this.onEnd = onEnd;
    }
    
    
    @Override protected void succeeded() {
        super.succeeded();
        updateMessage(getTitle() + " succeeded!");
        if (onEnd!=null) onEnd.accept(true, getValue());
    }

    @Override protected void cancelled() {
        super.cancelled();
        updateMessage(getTitle() + " cancelled!");
        if (onEnd!=null) onEnd.accept(false, getValue());
    }

    @Override protected void failed() {
        super.failed();
        updateMessage(getTitle() + " failed!");
        if (onEnd!=null) onEnd.accept(false, getValue());
    }
    
    protected void updateMessage(int all, int done, int skipped) {
        sb.setLength(0);
        sb.append("Completed ");
        sb.append(done);
        sb.append(" / ");
        sb.append(all);
        sb.append(". ");
        sb.append(skipped);
        sb.append(" skipped.");
        updateMessage(sb.toString());
    }

    @Override
    protected void updateMessage(String message) {
        super.updateMessage(message);
//        System.out.println(message);
    }
}
