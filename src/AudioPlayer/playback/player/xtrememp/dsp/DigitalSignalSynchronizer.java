/**
 * Xtreme Media Player a cross-platform media player. Copyright (C) 2005-2014
 * Besmir Beqiri
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
package AudioPlayer.playback.player.xtrememp.dsp;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides synchronization between a digital signal processor and
 * speaker output.
 *
 * Based on the KJ-DSS project by Kris Fudalewski at http://fudcom.com/main/libs/kjdss/.
 *
 * @author Besmir Beqiri
 */
public class DigitalSignalSynchronizer implements LineListener, Runnable {

    private static Logger logger = LoggerFactory.getLogger(DigitalSignalSynchronizer.class);
    public static final int DEFAULT_BLOCK_LENGTH = 8192;
    public static final double DEFAULT_BLOCK_RATE = 44100.0 / 1024.0; // 44100/1024 = 43 bps
    private final List<DigitalSignalProcessor> dspList;
    private final ExecutorService execService;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rLock = lock.readLock();
    private final Lock wLock = lock.writeLock();
    private final Condition writeCondition = wLock.newCondition();
    private Future future;
    private int blockLength = DEFAULT_BLOCK_LENGTH;
    private double blockRate = DEFAULT_BLOCK_RATE;  // 43 blocks per second
    private SourceDataLine sourceDataLine;
    private ByteBuffer audioDataBuffer;
    private DssContext dssContext;

    // The following variables are used for "active rendering":
    private long blockPeriod = 0L;      // nanoseconds
    private long beforeTime = 0L;       // nanoseconds
    private long afterTime = 0L;        // nanoseconds
    private long timeDiff = 0L;         // nanoseconds
    private long desiredSleepTime = 0L; // nanoseconds
    private long actualSleepTime = 0L;  // nanoseconds
    private long overSleepTime = 0L;    // nanoseconds

    // The following variables are used for performance monitoring and reporting:
    private float measuredLinePeriod = (float) 0.0;
    private float cumulativeLinePeriod = (float) 0.0;
    private float cumulativeComputationTime = (float) 0.0;
    private float cumulativeActualSleepTime = (float) 0.0;
    private final int numLinesToAverage = 100;
    private int lineCount = 0;
    private int numMissedCycles = 0;

    /**
     * Default constructor.
     */
    public DigitalSignalSynchronizer() {
        this(DEFAULT_BLOCK_LENGTH, DEFAULT_BLOCK_RATE);
    }

    /**
     * @param blockLength The sample size to extract from audio data sent to the
     * SourceDataLine.
     * @param framesPerSecond The desired refresh rate per second of registered
     * DSP's.
     */
    public DigitalSignalSynchronizer(int blockLength, double framesPerSecond) {
        this.blockLength = blockLength;
        this.blockRate = framesPerSecond;
        this.dspList = new CopyOnWriteArrayList<>();
        this.execService = Executors.newFixedThreadPool(1, r->{
            Thread t = new Thread(r);
            // daemon==true is imperative, or it pevents application from
            // ever closing
            t.setDaemon(true);
            t.setName("DigitalSignalSynchronizer");
            return t;
        });
    }

    /**
     * Adds a DSP to the DSS and forwards any audio data to it at the specified
     * frame rate.
     *
     * @param dsp A class implementing the DigitalSignalProcessor interface.
     */
    public void add(DigitalSignalProcessor dsp) {
        if (dsp == null) {
            throw new IllegalArgumentException();
        }
        if (sourceDataLine != null) {
            dsp.init(blockLength, sourceDataLine);
        }
        if (dspList.add(dsp)) {
            logger.info("DSP added");
        }
        wLock.lock();
        try {
            writeCondition.signal();
        } finally {
            wLock.unlock();
        }
    }

    /**
     * Removes the specified DSP from this DSS if it exists.
     *
     * @param dsp A class implementing the DigitalSignalProcessor interface.
     */
    public void remove(DigitalSignalProcessor dsp) {
        if (dsp == null) {
            throw new IllegalArgumentException();
        }
        if (dspList.remove(dsp)) {
            logger.info("DSP removed");
        }
    }

    /**
     * Start monitoring the specified SourceDataLine.
     *
     * @param sdl a SourceDataLine.
     */
    protected void open(SourceDataLine sdl) {
        //Stop processing previous source data line.
        if (future != null && !future.isCancelled()) {
            stop();
        }

        sourceDataLine = sdl;
        dssContext = new DssContext(sourceDataLine, blockLength);
        audioDataBuffer = ByteBuffer.allocate(sdl.getBufferSize());

        //Initialize DSP registered with this DSS.
        for (DigitalSignalProcessor dsp : dspList) {
            dsp.init(blockLength, sourceDataLine);
        }
    }

    protected void start() {
        blockPeriod = Math.round(1000000000.0 / blockRate); // 23,219,955 nanoseconds
        future = execService.submit(this);
    }

    protected boolean isRunning() {
        if (future != null) {
            return !future.isDone();
        }
        return false;
    }

    /**
     * Stop monitoring the current SourceDataLine and release resources.
     */
    protected void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }

    protected void close() {
        if (future != null) {
            future.cancel(true);
        }
        if (audioDataBuffer != null) {
            audioDataBuffer.clear();
        }
    }

    /**
     * Writes part of specified buffer to the monitored source data line an any
     * registered DSPs.
     *
     * @param audioData Data to write.
     * @param offset Offset to start reading from the buffer.
     * @param length The length from the specified offset to read.
     */
    public void writeAudioData(byte[] audioData, int offset, int length) {
        if (audioDataBuffer == null) {
            return;
        }

        wLock.lock();
        try {
            if (audioDataBuffer.remaining() < length) {
                removeBytesFromStart(audioDataBuffer, length - audioDataBuffer.remaining());
            }
            audioDataBuffer.put(audioData, offset, length);
        } finally {
            wLock.unlock();
        }
    }

    private void removeBytesFromStart(ByteBuffer byteBuffer, int n) {
        int index = 0;
        for (int i = n; i < byteBuffer.position(); i++) {
            byteBuffer.put(index++, byteBuffer.get(i));
            byteBuffer.put(i, (byte) 0);
        }
        byteBuffer.position(index);
    }

    @Override
    public void run() {
        while (isRunning()) {
            beforeTime = System.nanoTime();  // nanoseconds

            if (!dspList.isEmpty()) {
                rLock.lock();
                try {
                    dssContext.extractData(audioDataBuffer);
                } finally {
                    rLock.unlock();
                }
                //Dispatch sample data to digital signal processors
                for (DigitalSignalProcessor dsp : dspList) {
                    dsp.process(dssContext);
                }
            } else {
                wLock.lock();
                try {
                    writeCondition.awaitUninterruptibly();
                } finally {
                    wLock.unlock();
                }
            }

            afterTime = System.nanoTime();      // nanoseconds
            timeDiff = afterTime - beforeTime;  // This is the "computation time"
            desiredSleepTime = blockPeriod - timeDiff - overSleepTime;  // nanoseconds

            // logger.info("beforeTime       = "  + beforeTime );
            // logger.info("afterTime        = "  + afterTime );
            // logger.info("timeDiff         = "  + timeDiff );
            // logger.info("desiredSleepTime = "  + desiredSleepTime );
            // logger.info("blockPeriod      = "  + blockPeriod );
            
            if (desiredSleepTime > 0L) {   // some time left in this cycle.  This is good.
                try {
                    TimeUnit.NANOSECONDS.sleep(desiredSleepTime);
                } catch (InterruptedException ex) {
                }
                actualSleepTime = System.nanoTime() - afterTime; // nanoseconds
                overSleepTime = actualSleepTime - desiredSleepTime; // corrects for sleep inaccuracies
                // logger.info("actualSleepTime      = "  + actualSleepTime );
                // logger.info("overSleepTime        = " + overSleepTime);
            } else {
                // sleepTime <= 0; The computation took longer than the blockPeriod. This is bad.
                overSleepTime = 0;
                numMissedCycles++;
            }
            /*
            // This section computes some performance statics and reports them:
            measuredLinePeriod = timeDiff + desiredSleepTime + overSleepTime;
            cumulativeLinePeriod += measuredLinePeriod;
            cumulativeComputationTime += timeDiff;
            cumulativeActualSleepTime += actualSleepTime;

            if (lineCount == numLinesToAverage - 1) {
                logger.info("Average Line Period      = " + cumulativeLinePeriod / numLinesToAverage);
                logger.info("Average Computation Time =  " + cumulativeComputationTime / numLinesToAverage);
                logger.info("Average Actual Sleep Time= " + cumulativeActualSleepTime / numLinesToAverage);
                logger.info("Total Missed Cycles      =  " + numMissedCycles);
                lineCount = 0;
                cumulativeLinePeriod = 0;
                cumulativeComputationTime = 0;
                cumulativeActualSleepTime = 0;
                numMissedCycles = 0;
            }
            lineCount++;
            */
        }
    }

    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();
        wLock.lock();
        try {
            if (type.equals(LineEvent.Type.OPEN)) {
                open((SourceDataLine) event.getLine());
            } else if (type.equals(LineEvent.Type.START)) {
                start();
            } else if (type.equals(LineEvent.Type.STOP)) {
                stop();
            } else if (type.equals(LineEvent.Type.CLOSE)) {
                close();
            }
        } finally {
            wLock.unlock();
        }
    }
}
