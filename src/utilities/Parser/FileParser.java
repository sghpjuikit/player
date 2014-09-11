/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import java.io.File;
import static java.util.Collections.singletonList;
import java.util.List;

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
    
    /** {@inheritDoc} */
    @Override
    public List<Class> getSupportedClasses() {
        return singletonList(File.class);
    }

}
