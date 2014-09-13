/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser.ParserImpl;

import java.io.File;
import utilities.Parser.StringParser;

/**
 *
 * @author Plutonium_
 */
public class FileParser implements StringParser<File> {
    
    /** {@inheritDoc} */
    @Override
    public File fromS(String source) {
        return new File(source);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toS(File object) {
        return object.getPath();
    }

}
