/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.FilterGenerator;

import javafx.collections.transformation.FilteredList;
import utilities.access.FieldValue.FieldEnum;
import utilities.access.FieldValue.FieldedValue;

/**
 *
 * @author Plutonium_
 */
public class TableFilterGenerator<T extends FieldedValue,F extends FieldEnum<T>> extends FilterGeneratorChain<T,F> {
    
    public TableFilterGenerator(FilteredList<T> table_list) {
        setMapper((elementField,filter) -> element -> filter.test(element.getField(elementField)));
        setOnFilterChange(table_list::setPredicate);
    }
}
