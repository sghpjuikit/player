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

    @Override
    public boolean supports(Class type) {
        return type.equals(File.class);
    }

    @Override
    public File fromS(String source) {
        return new File(source);
    }

    @Override
    public String toS(File object) {
        return object.getName();
    }

}
