/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.collections.transformation.FilteredList;
import static util.Util.getEnumConstants;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.collections.Tuple3;
import static util.collections.Tuples.tuple;
import util.functional.Functors;
import static util.functional.Util.cmpareBy;

/**
 *
 * @author Plutonium_
 */
public class TableFilterGenerator<T extends FieldedValue,F extends FieldEnum<T>> extends FilterGeneratorChain<T,F> {

    public TableFilterGenerator(FilteredList<T> table_list, F prefFilterType) {
        super(() -> {
            FilterGenerator<F> g = new FilterGenerator<>(
                in -> Functors.getIO(in, Boolean.class),
                in -> Functors.getPrefIO(in, Boolean.class)
            );
            g.setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
            g.setData(d(prefFilterType));
            return g;
        });
        setMapper((field,filter) -> element -> filter.test(element.getField(field)));
        setOnFilterChange(table_list::setPredicate);
        setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
        if(prefFilterType instanceof Enum) {
            setData(d(prefFilterType));
        } else
            throw new IllegalArgumentException("Initial value - field type must be an enum");
    }
    
    private static <F extends FieldEnum> List<Tuple3<String,Class,F>> d(F prefFilterType) {
        F[] es = (F[]) getEnumConstants(prefFilterType.getClass());
        return Stream.of(es)
                .filter(FieldEnum::isTypeStringRepresentable)
                .map(mf->tuple(mf.toString(),mf.getType(),mf))
                .sorted(cmpareBy(e->e._1))
                .collect(toList());
    }
}