/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kc7bfi.jflac.sound.spi;

import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 *
 * @author Besmir Beqiri
 */
public class FlacAudioFileFormat extends TAudioFileFormat {

    /**
     * Contructor.
     * @param type
     * @param audioFormat
     * @param nLengthInFrames
     * @param nLengthInBytes
     */
    public FlacAudioFileFormat(Type type, AudioFormat audioFormat, int nLengthInFrames, int nLengthInBytes, Map properties) {
        super(type, audioFormat, nLengthInFrames, nLengthInBytes, properties);
    }

    @Override
    public Map properties() {
        return super.properties();
    }
}
