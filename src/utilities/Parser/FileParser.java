/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.Parser;

import java.io.File;

/**
 *
 * @author Plutonium_
 */
public class FileParser implements StringParser<File> {

    /**
     * @param type
     * @return true if and only if the class is a File
     */
    @Override
    public boolean supports(Class<?> type) {
        return File.class.isAssignableFrom(type);
    }
    
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
