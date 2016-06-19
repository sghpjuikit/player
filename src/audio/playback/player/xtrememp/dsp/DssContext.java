/**
 * Xtreme Media Player a cross-platform media player. 
 * Copyright (C) 2005-2014 Besmir Beqiri
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package audio.playback.player.xtrememp.dsp;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author Besmir Beqiri
 */
public class DssContext {

    private final SourceDataLine sourceDataLine;
    private final AudioFormat audioFormat;
    private final float[][] audioChannels;
    private int offset;
    private final int blockLength;
    private final int channelCount;
    private final int frameSize;
    private final int ssib;
    private final int channelSize;
    private final float audioSampleSize;

    /**
     * Create a DSS context from a source data line with a fixed sample size.
     *
     * @param sourceDataLine The source data line.
     * @param blockLength The sample size.
     */
    public DssContext(SourceDataLine sourceDataLine, int blockLength) {
        this.sourceDataLine = sourceDataLine;
        this.audioFormat = sourceDataLine.getFormat();
        this.blockLength = blockLength;
        this.audioChannels = new float[2][blockLength];  // will always have audioChannels[0][] and audioChannels[1][]
        
        channelCount = audioFormat.getChannels();   // number of audio channels (1 for mono, 2 for stereo)
        frameSize = audioFormat.getFrameSize();     // number of bytes in each frame
        ssib = audioFormat.getSampleSizeInBits();   // number of bits in each sample
        channelSize = frameSize / channelCount;     // channelSize = 4/2=2 or 2/1=2
        audioSampleSize = (1 << (ssib - 1));        // If ssib = 16 then audioSampleSize = 32768
    }

    /**
     * The method extractData() extracts the desired audio data from the
     * audioDataBuffer provided by the DigitalSignalSynchronizer, performs some
     * necessary reformatting on it, and outputs the audio samples data into a
     * two-dimensional array called audioChannels[][].
     * 
     * @param audioDataBuffer 
     */
    public void extractData(ByteBuffer audioDataBuffer) {
        long lfp = sourceDataLine.getLongFramePosition();  // long frame position
        offset = (int) (lfp * frameSize % (long) (audioDataBuffer.capacity()));
        int channelNum  = 0;    // audio channel number
        int sampleNum   = 0;    // audio sample number
        int cdp         = 0;    // channel data position
        int position    = 0;
        int bit         = 0;    // bit (as in 8th or 16th bit)
        int bytePos     = 0;    // byte position (in the sample)
        float signMask  = 0.0F; // sign mask
        
        // -- Loop through audio data.
        for (sampleNum = 0, position = offset; sampleNum < blockLength; sampleNum++, position += frameSize) {
            if (position >= audioDataBuffer.capacity()) {
                position = 0;
            }

            // -- Loop through channels.
            for (channelNum = 0, cdp = 0; channelNum < channelCount; channelNum++, cdp += channelSize) {

                // -- Sign least significant byte. (PCM_SIGNED)
                signMask = (audioDataBuffer.get(position + cdp) & 0xFF) - 128.0F;

                for (bit = 8, bytePos = 1; bit < ssib; bit += 8) {
                    signMask += audioDataBuffer.get(position + cdp + bytePos) << bit;
                    bytePos++;
                }

                // -- Store normalized data.
                audioChannels[channelNum][sampleNum] = signMask / audioSampleSize;
            }
        }
        // If the input audio signal has only 1 channel then the above loop will
        // produce only the audioChannel[0][] array.  We should create an
        // audioChannels[1][] array containing a replica of the input so that
        // all subsequent processing steps will have 2 channels to work with.
        if(channelCount == 1) {
            System.arraycopy(audioChannels[0], 0, audioChannels[1], 0, blockLength);
        }
    }

    /**
     * Returns a properly formatted audio sample array for each audio channel
     * originally provided by the DSS data buffer.
     *
     * A clone of the audioChannels array is outputted in order to enforce a
     * producer/consumer model of data transfer.  The DSS class is the audio data
     * producer and the DSPs/visualizations are the consumers.  If a visualization
     * modifies its copy of the audioChannels array, it does not effect any
     * subsequent visualizations because they receive a pristine new array clone.
     * 
     * @return A float array for all audio channels.
     */
    public float[][] getAudioData() {
        return audioChannels.clone();
    }

    /**
     * Returns the sample size to read from the data buffer.
     *
     * @return The sample size to read from the data buffer
     */
    public int getSampleSize() {
        return blockLength;
    }

    /**
     * Returns the data buffer offset to start reading from. Please note that
     * the offset + length can be beyond the buffer length. This simply means,
     * the rest of data sample has rolled over to the beginning of the data
     * buffer.
     *
     * @return The data buffer offset to start reading from.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the monitored source data line.
     *
     * @return A {@link SourceDataLine} object.
     */
    public SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }
}
