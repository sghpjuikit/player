/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import util.functional.Functors.F1;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR_STR;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/**
 *
 * @author Plutonium_
 */
@StringParseStrategy(from = CONSTRUCTOR_STR, to = TO_STRING_METHOD, ex = {IllegalStateException.class,java.lang.IllegalArgumentException.class})
public class SDF implements F1<Double,Double> {
    
    private final String ex;
    private final Expression e;
    
    public SDF(String s) {
        try {
            ex = s;
            e = new ExpressionBuilder(s).variables("x").build();
            e.setVariable("x", 0).evaluate();
        } catch(Exception e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Double apply(Double i) {
        return e.setVariable("x", i).evaluate();
    }

    @Override
    public String toString() {
        return ex;
    }
    
}
