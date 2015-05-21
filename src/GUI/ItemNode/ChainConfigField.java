/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import GUI.objects.CheckIcon;
import GUI.objects.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.PLUS;
import java.util.function.Supplier;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import util.async.runnable.Run;
import static util.functional.Util.rep;

/**
 *
 * @author Plutonium_
 */
public abstract class ChainConfigField<V, IN extends ItemNode<V>> extends ItemNode<V> {

    private final VBox root = new VBox();
    protected final ObservableList<Chainable<IN>> generators = (ObservableList)root.getChildren();
    
    public ChainConfigField(Supplier<IN> chainedFactory) {
        this(1, chainedFactory);
    }
    
    public ChainConfigField(int i, Supplier<IN> chainedFactory) {
        if(i<1) throw new IllegalArgumentException("Chain length must be positive");
        rep(i,() -> new Chainable<>(chainedFactory));
        generateValue(); // initializes value, dont fire update yet
    }
    
    @Override
    public VBox getNode() {
        return root;
    }
    
    /**
     * Invokes focus on last chained.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void focus() {
        generators.get(generators.size()-1).chained.focus();
    }
    
    protected void generateValue() {}
    

    /** Sets button in the empty spot in the top left corner. Empty by default. */
    public void setButton(FontAwesomeIconName icon, Tooltip t, Run action) {
        Icon i = generators.get(0).rem;
             i.icon.setValue(icon);
             i.setOnMouseClicked(action.toHandlerConsumed());
             i.setTooltip(t);
             i.setDisable(false);
    }
    
    
    private static final Tooltip addTooltip = new Tooltip("Add");
    private static final Tooltip remTooltip = new Tooltip("Remove");
    private static final Tooltip onTooltip = new Tooltip("Enabled");
    
    class Chainable<C extends ItemNode<V>> extends HBox {
        CheckIcon on = new CheckIcon(true);
        Icon rem = new Icon(MINUS, 13);
        Icon add = new Icon(PLUS, 13);
        C chained;
        
        public Chainable(Supplier<C> chainedFactory) {
            this(generators.size(), chainedFactory);
        }
        public Chainable(int at, Supplier<C> chainedFactory) {
            chained = chainedFactory.get();
            setSpacing(5);
            getChildren().addAll(rem,add,on,chained.getNode());
            HBox.setHgrow(chained.getNode(), ALWAYS);
            setAlignment(CENTER_LEFT);
            chained.onItemChange = f-> generateValue();
            on.selected.addListener((o,ov,nv) -> generateValue());
            rem.setOnMouseClicked(e -> {
                generators.remove(this);
                generateValue();
            });
            add.setOnMouseClicked(e -> {
                new Chainable(generators.indexOf(this)+1,chainedFactory);
                generateValue();
            });
            rem.setPadding(new Insets(0, 0, 0, 5));
            rem.setDisable(at==0);
            rem.setTooltip(remTooltip);
            add.setTooltip(addTooltip);
            on.setTooltip(onTooltip);
            generators.add(at,(Chainable)this);
        }
    }
}
