/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.reactive;

import java.util.HashSet;
import org.reactfx.Subscription;
import util.functional.Functors.Ƒ;

/** Set of runnables/functions taking o parameters. Use as a collection of handlers. */
public class SetƑ extends HashSet<Runnable> implements Ƒ {

    public SetƑ() {
        super(2);
    }

    @Override
    public void apply() {
        forEach(Runnable::run);
    }

    public Subscription addS(Runnable r) {
        add(r);
        return () -> remove(r);
    }

}