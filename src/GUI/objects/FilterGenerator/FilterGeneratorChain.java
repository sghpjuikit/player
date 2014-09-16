/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.reactfx.util.Tuple3;
import utilities.access.FieldValue.FieldEnum;
import utilities.access.FieldValue.FieldedValue;
import utilities.functional.functor.BiCallback;

/**
 *
 * @author Plutonium_
 */
public class FilterGeneratorChain<T extends FieldedValue,F extends FieldEnum<T>> extends VBox {
    
    private final List<Tuple3<String,Class,F>> data = new ArrayList();
    private final List<FilterGenerator<F>> generators = new ArrayList();
    private Consumer<Predicate<T>> onFilterChange;
    private Predicate<T> conjuction;
    private BiCallback<F,Predicate<Object>,Predicate<T>> converter;
    
    public FilterGeneratorChain() {
        this(1);
    }
    
    public FilterGeneratorChain(int i) {
        IntStream.range(1, i+1).forEach( index -> generators.add(buildG()));
        
        
    }
    
    public void setData(List<Tuple3<String,Class,F>> classes) {
        data.addAll(classes);
        generators.forEach(g->g.setData(classes));
    }
    
    public void setOnFilterChange(Consumer<Predicate<T>> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    public void setMapper(BiCallback<F,Predicate<Object>,Predicate<T>> mapper) {
        this.converter = mapper;
    }
    
    private FilterGenerator<F> buildG() {
        FilterGenerator<F> g = new FilterGenerator();
        g.setData(data);
        g.setOnFilterChange((a,b) -> generatePredicate());
        Label add = AwesomeDude.createIconLabel(AwesomeIcon.PLUS, "13");
        Label rem = AwesomeDude.createIconLabel(AwesomeIcon.MINUS, "13");
        add.setOnMouseClicked(e -> {
            int i = generators.indexOf(g);
            generators.add(i+1, buildG());
            getChildren().setAll(generators);
        });
        rem.setOnMouseClicked(e -> {
            generators.remove(g);
            getChildren().setAll(generators);
        });
        g.getChildren().add(0, add);
        g.getChildren().add(0, rem);
        add.setPadding(new Insets(0, 3, 0, 3));
        rem.setPadding(new Insets(0, 3, 0, 5));
        rem.setDisable(generators.isEmpty());
        getChildren().add(generators.indexOf(g)+1,g);
        return g;
    }
    
    private void generatePredicate() {
        conjuction = o -> true;
        for(FilterGenerator<F> g : generators) {
            if(g.predicate==null) return;
            else conjuction=conjuction.and(converter.call(g.val,g.predicate));
        }
        if(onFilterChange!=null) onFilterChange.accept(conjuction);
    }
}
