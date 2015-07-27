/*
 *
 */
package org.kc7bfi.jflac.sound.spi;

import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.tritonus.share.sampled.TAudioFormat;

/**
 *
 * @author Besmir Beqiri
 */
public class FlacAudioFormat extends TAudioFormat {

    /**
     * Constructor.
     * @param encoding
     * @param nFrequency
     * @param SampleSizeInBits
     * @param nChannels
     * @param FrameSize
     * @param FrameRate
     * @param isBigEndian
     * @param properties
     */
    public FlacAudioFormat(AudioFormat.Encoding encoding, float nFrequency, 
            int SampleSizeInBits, int nChannels, int FrameSize, float FrameRate,
            boolean isBigEndian, Map properties) {
        super(encoding, nFrequency, SampleSizeInBits, nChannels, FrameSize,
                FrameRate, isBigEndian, properties);
    }

    /**
     * Flac audio format parameters.
     * Some parameters might be unavailable. So availability test is required
     * before reading any parameter.
     *
     * <br>AudioFormat parameters.
     */
    @Override
    public Map properties() {
        return super.properties();
    }
}
