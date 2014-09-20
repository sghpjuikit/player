/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.transformation.FilteredList;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuples;
import utilities.access.FieldValue.FieldEnum;
import utilities.access.FieldValue.FieldedValue;
import utilities.filtering.Predicates;

/**
 *
 * @author Plutonium_
 */
public class TableFilterGenerator<T extends FieldedValue,F extends FieldEnum<T>> extends FilterGeneratorChain<T,F> {
    
    public TableFilterGenerator(FilteredList<T> table_list) {
        this(table_list, null);
    }
    
    public TableFilterGenerator(FilteredList<T> table_list, F prefFilterType) {
        setMapper((elementField,filter) -> element -> filter.test(element.getField(elementField)));
        setOnFilterChange(table_list::setPredicate);
        setPredicateSupplier(Predicates::getPredicates);
        setPrefPredicateSupplier(Predicates::getPrefPredicate);
        
        if(prefFilterType!=null) {
            setPrefTypeSupplier(() -> Tuples.t(prefFilterType.toStringEnum(), prefFilterType.getType(), prefFilterType));
            if(prefFilterType instanceof Enum) {
                F[] es = (F[]) prefFilterType.getClass().getEnumConstants();
                List<Tuple3<String,Class,F>> data = Arrays.asList(es).stream()
                    .map(mf->Tuples.t(mf.toStringEnum(),mf.getType(),mf))
                    .collect(Collectors.toList());
                setData(data);
            } else
                throw new IllegalArgumentException("Initial value - field type must be an enum");
        }
    }
}