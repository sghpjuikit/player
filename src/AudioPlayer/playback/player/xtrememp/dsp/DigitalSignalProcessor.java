/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2010 Besmir Beqiri
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package AudioPlayer.playback.player.xtrememp.dsp;

import javax.sound.sampled.SourceDataLine;

/**
 * Classes must implement this interface in order to be registered with the 
 * KJDigitalSignalSynchronizer class.
 *
 * Based on the KJ-DSS project by Kris Fudalewski at http://fudcom.com/main/libs/kjdss/.
 *
 * @author Besmir Beqiri
 */
public interface DigitalSignalProcessor {

    /**
     * Called by the DigitalSignalSynchronizer during the call of the 'start'
     * method. Allows a DSP to prepare any necessary buffers or objects according
     * to the audio format of the source data line.
     *
     * @param blockLength     The sample size that this DSP should be prepared to handle.
     * @param sourceDataLine The source data line that will be monitored.
     */
    void init(int blockLength, SourceDataLine sourceDataLine);

    /**
     * Called by the DigitalSignalSynchronizer while the SourceDataLine is active.
     *
     * @param dssContext A context object containing a reference to the sample 
     *                   data to be processed as well as other useful references
     *                   during processing time.
     */
    void process(DssContext dssContext);
}
