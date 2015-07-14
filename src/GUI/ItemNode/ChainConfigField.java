/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.itemnode;

import Configuration.Config;
import Configuration.Configurable;
import Layout.Widgets.feature.ConfiguringFeature;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.MINUS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.PLUS;
import gui.itemnode.ItemNode.ValueNode;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import util.async.runnable.Run;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public abstract class ChainConfigField<V, IN extends ValueNode<V>> extends ValueNode<V> {

    protected final VBox root = new VBox();
    protected final ObservableList<Chainable<IN>> chain = (ObservableList)root.getChildren();
    public final IntegerProperty maxChainLength = new SimpleIntegerProperty(Integer.MAX_VALUE);
    protected boolean homogeneous = true;
    
    ChainConfigField() {}
    
    /** Creates unlimited chain of 1 initial chained element.  */
    public ChainConfigField(Supplier<IN> chainedFactory) {
        this(1, chainedFactory);
    }
    
    /** Creates unlimited chain of i initial chained elements.  */
    public ChainConfigField(int i, Supplier<IN> chainedFactory) {
        this(i, Integer.MAX_VALUE, chainedFactory);
    }
    
    /** Creates limited chain of i initial chained elements.  */
    public ChainConfigField(int len, int max_len, Supplier<IN> chainedFactory) {
        if(len<1) throw new IllegalArgumentException("Chain length must be positive");
        repeat(len,() -> new Chainable<>(chainedFactory));
        generateValue(); // initializes value, dont fire update yet
        
        maxChainLength.addListener((o,ov,nv) -> {
            int m = nv.intValue();
            if(m<chain.size()) {
                chain.setAll(chain.subList(0, m));
                generateValue();
            }
            chain.forEach(Chainable::updateIcons);
        });
        chain.addListener((Change<? extends Chainable<IN>> c) -> chain.forEach(Chainable::updateIcons));
        maxChainLength.set(max_len);
    }
    
    /** {@inheritDoc} */
    @Override
    public VBox getNode() {
        return root;
    }
    
    /**
     * Invokes focus on last chained element.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void focus() {
        chain.get(chain.size()-1).chained.focus();
    }
    
    protected void generateValue() {
        changeValue(reduce(getValues()));
    }
    
    abstract protected V reduce(Stream<V> values);
    
    /** Return individual chained values that are enabled and non null. */
    public Stream<V> getValues() {
        return chain.stream().filter(g -> g.on.get())
                             .map(g -> g.chained.getValue())
                             .filter(isNotNULL);
    }

    /** 
     * Sets button in the top left corner - instead of remove button of the 
     * first element of the chain (which is more or less disabled by default).
     * 
     * @param icon icon to set, null to revert to default state
     * @param t tooltip
     * @param action on click action
     */
    public void setButton(FontAwesomeIconName icon, Tooltip t, Run action) {
        Chainable<IN> c = chain.get(0);
        Icon i = c.rem;
             i.icon.setValue(icon==null ? MINUS : icon);
             i.setOnMouseClicked(icon==null ? c::onRem : action.toHandlerConsumed());
             i.setTooltip(icon==null ? addTooltip : t);
        c.rem_alt = true;
        c.updateIcons();
    }
    
    
    private static final Tooltip addTooltip = new Tooltip("Add");
    private static final Tooltip remTooltip = new Tooltip("Remove");
    private static final Tooltip onTooltip = new Tooltip("Enabled");
    
    public class Chainable<C extends ValueNode<V>> extends HBox {
        private final CheckIcon onB = new CheckIcon(true);
        private final Icon rem = new Icon(MINUS, 13);
        private final Icon add = new Icon(PLUS, 13);
        public final BooleanProperty on = onB.selected;
        public final C chained;
        private boolean rem_alt = false; // alternative icon, never disable
        
        public Chainable(Supplier<C> chainedFactory) {
            this(chain.size(), chainedFactory);
        }
        public Chainable(int at, Supplier<C> chainedFactory) {
            chained = chainedFactory.get();
            setSpacing(5);
            getChildren().addAll(rem,add,onB,chained.getNode());
            HBox.setHgrow(chained.getNode(), ALWAYS);
            setAlignment(CENTER_LEFT);
            chained.onItemChange = f-> generateValue();
            on.addListener((o,ov,nv) -> generateValue());
            rem.setOnMouseClicked(this::onRem);
            add.setOnMouseClicked(e -> {
                if(chain.size()<maxChainLength.get()) {
                    new Chainable(getIndex()+1,chainedFactory);
                    generateValue();
                }
            });
            rem.setPadding(new Insets(0, 0, 0, 5));
            rem.setTooltip(remTooltip);
            add.setTooltip(addTooltip);
            onB.setTooltip(onTooltip);
            chain.add(at,(Chainable)this);
            updateIcons();
        }
        
        /** @return position index within the chain from 0 to chain.size() */
        public int getIndex() {
            return chain.indexOf(this);
        }
        
        void updateIcons() {
            int l = chain.size();
            boolean h = isHomogeneous();
            
            rem.setDisable(h && !rem_alt && l<=1);
            add.setDisable(h && l>=maxChainLength.get());
            this.setDisable(!h);
        }
        
        void onRem(MouseEvent e) {
            if(chain.size()>1) {
                chain.remove(this);
                generateValue();
            }
        }
        
        private boolean isHomogeneous() {
            int i = getIndex();
            return homogeneous || i>=chain.size()-1;
        }
    }
    
    public static class ListConfigField<V, IN extends ValueNode<V>> extends ChainConfigField<V,IN> {

        public ListConfigField(Supplier<IN> chainedFactory) {
            super(chainedFactory);
        }

        @Override
        protected void generateValue() {
            changeValue(null);
        }

        @Override
        public V getValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected V reduce(Stream<V> values) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    
    public static class ConfigPane<T> implements ConfiguringFeature{
        private final VBox root = new VBox(5);
        private final List<ConfigField<T>> configs = new ArrayList();
        
        public ConfigPane() {}
        public ConfigPane(Collection<Config<T>> configs) {
            this.configs.clear();
            configure((Collection)configs);
        }
        public ConfigPane(Configurable<T> configs) {
            this(configs.getFields());
        }

        @Override
        public final void configure(Collection<Config> configs) {
            root.getChildren().setAll(map(configs,c -> {
                ConfigField cf = ConfigField.create(c);
                this.configs.add(cf);
                Label l = cf.getLabel();
                      l.setMinWidth(100);
                      l.setPrefWidth(100);
                      l.setTextAlignment(TextAlignment.RIGHT);
                      l.setPadding(new Insets(0, 0, 0, 5));
                HBox h = new HBox(5,l,cf.getNode());
                     h.setAlignment(CENTER_LEFT);
                return h;
            }));
        }
        
        public Node getNode() {
            return root;
        }
        
        public Stream<T> getValues() {
            return configs.stream().map(ConfigField::getValue);
        }
        public List<ConfigField<T>> getValuesC() {
            return configs;
        }
        
    }
}
