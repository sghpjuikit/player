/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.collections.transformation.FilteredList;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuples;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.filtering.Predicates;
import static util.functional.FunctUtil.cmpareBy;

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
                List<Tuple3<String,Class,F>> data = Stream.of(es)
                    .filter(FieldEnum::isTypeStringRepresentable)
                    .map(mf->Tuples.t(mf.toStringEnum(),mf.getType(),mf))
                    .sorted(cmpareBy(e->e._1))
                    .collect(toList());
                setData(data);
            } else
                throw new IllegalArgumentException("Initial value - field type must be an enum");
        }
    }
}