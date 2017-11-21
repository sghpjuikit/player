/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2011 Besmir Beqiri
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
package audio.playback.player.xtrememp.audio;

import audio.playback.PLAYBACK;
import audio.playback.player.xtrememp.dsp.DigitalSignalSynchronizer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.util.Duration;
import javax.sound.sampled.*;
import javazoom.spi.PropertiesContainer;
import org.kc7bfi.jflac.sound.spi.FlacAudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;
import static util.dev.Util.log;
import static util.dev.Util.throwIfFxThread;

/**
 *
 * @author Besmir Beqiri
 */
public class AudioPlayer implements Callable<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioPlayer.class);

    protected final int READ_BUFFER_SIZE = 4 * 1024;
    protected final Lock lock = new ReentrantLock();
    protected final Condition pauseCondition = lock.newCondition();
    protected Object audioSource;
    protected DigitalSignalSynchronizer dss;
    protected AudioFileFormat audioFileFormat;
    protected AudioInputStream audioInputStream;
    protected SourceDataLine sourceDataLine;
    protected String mixerName;
    protected Set<PlaybackListener> listeners;
    protected ExecutorService execService;
    protected Future<Void> future;
    protected Map<String, Object> properties;
    protected FloatControl gainControl;
    protected BooleanControl muteControl;
    protected FloatControl balanceControl;
    protected FloatControl panControl;
    protected int bufferSize = AudioSystem.NOT_SPECIFIED;
    public static final int INIT = 0;
    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int SEEK = 3;
    public static final int STOP = 4;
    protected volatile int state = AudioSystem.NOT_SPECIFIED;
    protected Map<String, Object> emptyMap = new HashMap<>();
    protected long oldPosition = 0;

    public AudioPlayer() {
        execService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            // daemon==true is imperative, or it pevents application from
            // ever closing
            t.setDaemon(true);
            t.setName("non-fx-playback-thread");
            return t;
        });
        dss = new DigitalSignalSynchronizer();
        listeners = new HashSet<>();
        reset();
    }

    public void addPlaybackListener(PlaybackListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removePlaybackListener(PlaybackListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public Set<PlaybackListener> getPlaybackListeners() {
        return listeners;
    }

    protected void notifyEvent(Playback state) {
        notifyEvent(state, emptyMap);
    }

    protected void notifyEvent(Playback state, Map properties) {
        long position = getPosition() - oldPosition;
        for (PlaybackListener listener : listeners) {
            new PlaybackEventLauncher(this, state, position, properties, listener).start();
        }
        LOGGER.info("{}", state);
    }

    private void reset() {
        if (sourceDataLine != null) {
            throwIfFxThread();
            sourceDataLine.flush();
            sourceDataLine.close();
            sourceDataLine = null;
        }
        audioFileFormat = null;
        gainControl = null;
        panControl = null;
        muteControl = null;
        future = null;
        emptyMap.clear();
        oldPosition = 0;
    }

    /**
     * Open file to play.
     * @param file
     * @throws PlayerException
     */
    public void open(File file) throws PlayerException {
        if (file != null) {
            audioSource = file;
            init();
        }
    }

    /**
     * Open URL to play.
     * @param url
     * @throws PlayerException
     */
    public void open(URL url) throws PlayerException {
        if (url != null) {
            audioSource = url;
            init();
        }
    }

    protected void init() throws PlayerException {
        throwIfFxThread();
        notifyEvent(Playback.BUFFERING);
        int oldState = state;
        state = AudioSystem.NOT_SPECIFIED;
        if (oldState == INIT || oldState == PAUSE) {
            lock.lock();
            try {
                pauseCondition.signal();
            } finally {
                lock.unlock();
            }
        }
        awaitTermination();
        lock.lock();
        try {
            reset();
            initAudioInputStream();
            initSourceDataLine();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inits AudioInputStream and AudioFileFormat from the data source.
     * @throws PlayerException
     */
    @SuppressWarnings("unchecked")
    protected void initAudioInputStream() throws PlayerException {
        throwIfFxThread();
        // Close any previous opened audio stream before creating a new one.
        closeStream();
        if (audioInputStream == null) {
            try {
                LOGGER.info("Data source: {}", audioSource);
                if (audioSource instanceof File) {
                    initAudioInputStream((File) audioSource);
                } else if (audioSource instanceof URL) {
                    initAudioInputStream((URL) audioSource);
                }
                AudioFormat sourceFormat = audioInputStream.getFormat();
                LOGGER.info("Source format: {}", sourceFormat);
                int nSampleSizeInBits = sourceFormat.getSampleSizeInBits();
                if (nSampleSizeInBits <= 0) {
                    nSampleSizeInBits = 16;
                }
                if ((sourceFormat.getEncoding() == AudioFormat.Encoding.ULAW) || (sourceFormat.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    nSampleSizeInBits = 16;
                }
                if (nSampleSizeInBits != 8) {
                    nSampleSizeInBits = 16;
                }
                // this fails for variable bitrate, needs a fix
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(), sourceFormat.getChannels() * (nSampleSizeInBits / 8), sourceFormat.getSampleRate(), false);
                LOGGER.info("Target format: {}", targetFormat);
                // Create decoded stream.
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                if (audioFileFormat instanceof TAudioFileFormat) {
                    // Tritonus SPI compliant audio file format.
                    properties = ((TAudioFileFormat) audioFileFormat).properties();
                    // Clone the Map because it is not mutable.
                    properties = deepCopy(properties);
                } else {
                    properties = new HashMap<String, Object>();
                }

                // Add JavaSound properties.
                if (audioFileFormat.getByteLength() > 0) {
                    properties.put("audio.length.bytes", audioFileFormat.getByteLength());
                }
                if (audioFileFormat.getFrameLength() > 0) {
                    properties.put("audio.length.frames", audioFileFormat.getFrameLength());
                }
                if (audioFileFormat.getType() != null) {
                    properties.put("audio.type", audioFileFormat.getType().toString());
                }
                // Audio format.
                AudioFormat audioFormat = audioFileFormat.getFormat();
                if (audioFormat.getFrameRate() > 0) {
                    properties.put("audio.framerate.fps", audioFormat.getFrameRate());
                }
                if (audioFormat.getFrameSize() > 0) {
                    properties.put("audio.framesize.bytes", audioFormat.getFrameSize());
                }
                if (audioFormat.getSampleRate() > 0) {
                    properties.put("audio.samplerate.hz", audioFormat.getSampleRate());
                }
                if (audioFormat.getSampleSizeInBits() > 0) {
                    properties.put("audio.samplesize.bits", audioFormat.getSampleSizeInBits());
                }
                if (audioFormat.getChannels() > 0) {
                    properties.put("audio.channels", audioFormat.getChannels());
                }
                if (audioFormat instanceof TAudioFormat) {
                    // Tritonus SPI compliant audio format.
                    properties.putAll(((TAudioFormat) audioFormat).properties());
                }
                if (audioFormat instanceof FlacAudioFormat) {
                    // Tritonus SPI compliant audio format.
                    properties.putAll(((FlacAudioFormat) audioFormat).properties());
                }
                for (String key : properties.keySet()) {
                    LOGGER.info("Audio Format Properties: {} = {}", key, properties.get(key));
                }
            } catch (UnsupportedAudioFileException | IOException ex) {
                closeStream();
                throw new PlayerException(ex);
            }
        }
    }
    /**
     * Inits Audio resources from file.
     * @param file
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    protected void initAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        audioInputStream = AudioSystem.getAudioInputStream(file);
        // wrap in buffer to support mark()
        BufferedInputStream bs = new BufferedInputStream(audioInputStream);
        audioInputStream = new AudioInputStream(bs, audioInputStream.getFormat(), audioInputStream.getFrameLength());
        // audioInputStream = AudioSystem.getAudioInputStream(bs);
        // audioInputStream = AudioSystem.getAudioInputStream(file);
        audioInputStream.mark(Integer.MAX_VALUE);   // allows us to seek backwards if mark supported
        audioFileFormat = AudioSystem.getAudioFileFormat(file);
    }

    /**
     * Inits Audio resources from URL.
     * @param url
     * @throws javax.sound.sampled.UnsupportedAudioFileException
     * @throws java.io.IOException
     */
    protected void initAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        audioInputStream = AudioSystem.getAudioInputStream(url);
        BufferedInputStream bs = new BufferedInputStream(audioInputStream);
        audioInputStream = new AudioInputStream(bs, audioInputStream.getFormat(), audioInputStream.getFrameLength());
        audioInputStream.mark(Integer.MAX_VALUE);
        audioFileFormat = AudioSystem.getAudioFileFormat(url);
    }

    /**
     * Inits Audio resources from AudioSystem.
     * @throws PlayerException
     */
    protected void initSourceDataLine() throws PlayerException {
        throwIfFxThread();
        if (sourceDataLine == null) {
            try {
                LOGGER.info("Create Source Data Line");
                AudioFormat audioFormat = audioInputStream.getFormat();
                DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
                if (!AudioSystem.isLineSupported(lineInfo)) {
                    throw new PlayerException(lineInfo + " is not supported");
                }

                if (mixerName == null) {
                    // Primary Sound Driver
                    mixerName = getMixers().get(0);
                }
                Mixer mixer = getMixer(mixerName);
                if (mixer != null) {
                    LOGGER.info("Mixer: {}", mixer.getMixerInfo().toString());
                    sourceDataLine = (SourceDataLine) mixer.getLine(lineInfo);
                } else {
                    sourceDataLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
                    mixerName = null;
                }

                sourceDataLine.addLineListener(dss);

                LOGGER.info("Line Info: {}", sourceDataLine.getLineInfo().toString());
                LOGGER.info("Line AudioFormat: {}", sourceDataLine.getFormat().toString());

                if (bufferSize <= 0) {
                    bufferSize = sourceDataLine.getBufferSize();
                }
                sourceDataLine.open(audioFormat, bufferSize);

                LOGGER.info("Line BufferSize: {}", sourceDataLine.getBufferSize());
                for (Control c : sourceDataLine.getControls()) {
                    LOGGER.info("Line Controls: {}", c);
                }

                if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                }
                if (sourceDataLine.isControlSupported(BooleanControl.Type.MUTE)) {
                    muteControl = (BooleanControl) sourceDataLine.getControl(BooleanControl.Type.MUTE);
                }
                if (sourceDataLine.isControlSupported(FloatControl.Type.BALANCE)) {
                    balanceControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.BALANCE);
                }
                if (sourceDataLine.isControlSupported(FloatControl.Type.PAN)) {
                    panControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.PAN);
                }

                sourceDataLine.start();
                state = INIT;
                future = execService.submit(this);
                notifyEvent(Playback.OPENED);
            } catch (LineUnavailableException ex) {
                throw new PlayerException(ex);
            }
        }
    }

    /**
     * Set SourceDataLine buffer size. It affects audio latency
     * (the delay between SourceDataLine.write(data) and real sound).
     * @param bufferSize if equal to -1 (AudioSystem.NOT_SPECIFIED)
     * means maximum buffer size available.
     */
    public void setBufferSize(int bufferSize) {
        if (bufferSize <= 0) {
            this.bufferSize = AudioSystem.NOT_SPECIFIED;
        } else {
            this.bufferSize = bufferSize;
        }
    }

    /**
     * Return SourceDataLine buffer size.
     * @return -1 (AudioSystem.NOT_SPECIFIED) for maximum buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Deep copy of a Map.
     * @param src
     * @return map
     */
    protected Map<String, Object> deepCopy(Map<String, Object> src) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (src != null) {
            Set<String> keySet = src.keySet();
            for (String key : keySet) {
                Object value = src.get(key);
                map.put(key, value);
//                logger.info("key: {}, value: {}", key, value);
            }
        }
        return map;
    }

    public List<String> getMixers() {
        List<String> mixers = new ArrayList<String>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        if (mixerInfos != null) {
            for (int i = 0, len = mixerInfos.length; i < len; i++) {
                Line.Info lineInfo = new Line.Info(SourceDataLine.class);
                Mixer _mixer = AudioSystem.getMixer(mixerInfos[i]);
                if (_mixer.isLineSupported(lineInfo)) {
                    mixers.add(mixerInfos[i].getName());
                }
            }
        }
        return mixers;
    }

    public Mixer getMixer(String name) {
        Mixer _mixer = null;
        if (name != null) {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            if (mixerInfos != null) {
                for (int i = 0, len = mixerInfos.length; i < len; i++) {
                    if (mixerInfos[i].getName().equals(name)) {
                        _mixer = AudioSystem.getMixer(mixerInfos[i]);
                        break;
                    }
                }
            }
        }
        return _mixer;
    }

    public String getMixerName() {
        return mixerName;
    }

    public void setMixerName(String name) {
        mixerName = name;
    }

    public long getDuration() {
        long duration = AudioSystem.NOT_SPECIFIED;
        if (properties.containsKey("duration")) {
            duration = ((Long) properties.get("duration")).longValue();
        } else {
            duration = getTimeLengthEstimation(properties);
        }
        return duration;
    }

    public int getByteLength() {
        int bytesLength = AudioSystem.NOT_SPECIFIED;
        if (properties != null) {
            if (properties.containsKey("audio.length.bytes")) {
                bytesLength = ((Integer) properties.get("audio.length.bytes")).intValue();
            }
        }
        return bytesLength;
    }

    public int getPositionByte() {
        int positionByte = AudioSystem.NOT_SPECIFIED;
        if (properties != null) {
            if (properties.containsKey("mp3.position.byte")) {
                positionByte = ((Integer) properties.get("mp3.position.byte")).intValue();
                return positionByte;
            }
            if (properties.containsKey("ogg.position.byte")) {
                positionByte = ((Integer) properties.get("ogg.position.byte")).intValue();
                return positionByte;
            }
        }
        return positionByte;
    }

    protected long getTimeLengthEstimation(Map properties) {
        long milliseconds = AudioSystem.NOT_SPECIFIED;
        int byteslength = AudioSystem.NOT_SPECIFIED;
        if (properties != null) {
            if (properties.containsKey("audio.length.bytes")) {
                byteslength = ((Integer) properties.get("audio.length.bytes")).intValue();
            }
            if (properties.containsKey("duration")) {
                milliseconds = (int) (((Long) properties.get("duration")).longValue()) / 1000;
            } else {
                // Try to compute duration
                int bitspersample = AudioSystem.NOT_SPECIFIED;
                int channels = AudioSystem.NOT_SPECIFIED;
                float samplerate = AudioSystem.NOT_SPECIFIED;
                int framesize = AudioSystem.NOT_SPECIFIED;
                if (properties.containsKey("audio.samplesize.bits")) {
                    bitspersample = ((Integer) properties.get("audio.samplesize.bits")).intValue();
                }
                if (properties.containsKey("audio.channels")) {
                    channels = ((Integer) properties.get("audio.channels")).intValue();
                }
                if (properties.containsKey("audio.samplerate.hz")) {
                    samplerate = ((Float) properties.get("audio.samplerate.hz")).floatValue();
                }
                if (properties.containsKey("audio.framesize.bytes")) {
                    framesize = ((Integer) properties.get("audio.framesize.bytes")).intValue();
                }
                if (bitspersample > 0) {
                    milliseconds = (long) (1000.0f * byteslength / (samplerate * channels * (bitspersample / 8)));
                } else {
                    milliseconds = (long) (1000.0f * byteslength / (samplerate * framesize));
                }
            }
        }
        return milliseconds * 1000;
    }


    public void setVolume(double v) { // v = normalized 0-1
        if (gainControl!=null) {
            // float f = (float)v; // logarithmic
            float f = (float) Math.sqrt(Math.sqrt(v)); // approximated linear
            // denormalized
            float n = gainControl.getMinimum()+f*(gainControl.getMaximum()-gainControl.getMinimum());
            gainControl.setValue(n);
        }
    }

    /**
     * Sets Gain value.
     * @param gain a value bitween -1.0 and +1.0
     */
    public void setGain(float gain) {
        if (gainControl != null) {
            double minGain = gainControl.getMinimum();
            double maxGain = gainControl.getMaximum();
            double ampGain = ((10.0f / 20.0f) * maxGain) - minGain;
            double cste = Math.log(10.0) / 20;
            double value = minGain + (1 / cste) * Math.log(1 + (Math.exp(cste * ampGain) - 1) * gain);
            gainControl.setValue((float) value);
            LOGGER.info("{}", gainControl.toString());
        } else {
//            throw new PlayerException("Gain control not supported");
        }
    }

    public void setBalance(double gain) {
        if (balanceControl != null) {
            balanceControl.setValue((float)gain);
        } else {
//            throw new PlayerException("Pan control not supported");
        }
    }

    /**
     * Sets Pan value.
     * @param pan a value bitween -1.0 and +1.0
     */
    public void setPan(float pan) {
        if (panControl != null) {
            panControl.setValue(pan);
            LOGGER.info("{}", panControl.toString());
        } else {
//            throw new PlayerException("Pan control not supported");
        }
    }

    /**
     * Sets Mute value.
     * @param mute a boolean value
     */
    public void setMute(boolean mute) {
        if (muteControl != null) {
            muteControl.setValue(mute);
            LOGGER.info("{}", muteControl.toString());
        } else {
//            throw new PlayerException("Mute control not supported");
        }
    }


    public long getPosition() {
        long pos = 0;
        if (sourceDataLine != null) {
            pos = sourceDataLine.getMicrosecondPosition();
        }
        return pos;
    }

    public int getState() {
        return state;
    }

    public DigitalSignalSynchronizer getDSS() {
        return dss;
    }

    @Override
    public Void call() throws PlayerException {
        throwIfFxThread();
        LOGGER.info("Decoding thread started");
        int nBytesRead = 0;
        int audioDataLength = READ_BUFFER_SIZE;
        ByteBuffer audioDataBuffer = ByteBuffer.allocate(audioDataLength);
        audioDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        lock.lock();
        try {
            while ((nBytesRead != -1) && (state != STOP) && (state != SEEK) && (state != AudioSystem.NOT_SPECIFIED)) {
                try {
                    if (state == PLAY) {
                        int toRead = audioDataLength;
                        int totalRead = 0;
                        while (toRead > 0 && (nBytesRead = audioInputStream.read(audioDataBuffer.array(), totalRead, toRead)) != -1) {
                            totalRead += nBytesRead;
                            toRead -= nBytesRead;
                        }
                        if (totalRead > 0) {
                            byte[] trimBuffer = audioDataBuffer.array();
                            if (totalRead < trimBuffer.length) {
                                trimBuffer = new byte[totalRead];
                                System.arraycopy(audioDataBuffer.array(), 0, trimBuffer, 0, totalRead);
                            }
                            sourceDataLine.write(trimBuffer, 0, totalRead);
                            dss.writeAudioData(trimBuffer, 0, totalRead);
                            for (PlaybackListener pl : listeners) {
                                PlaybackEvent pe = new PlaybackEvent(this, Playback.PLAYING, getPosition() - oldPosition, emptyMap);
                                if (audioInputStream instanceof PropertiesContainer) {
                                    // Pass audio parameters such as instant bitrate, ...
                                    pe.setProperties(((PropertiesContainer) audioInputStream).properties());
                                }
                                pl.playbackProgress(pe);
                            }
                        }
                    } else if (state == INIT || state == PAUSE) {
                        if (sourceDataLine != null && sourceDataLine.isRunning()) {
                            sourceDataLine.flush();
                            sourceDataLine.stop();
                        }
                        pauseCondition.awaitUninterruptibly();
                    }
                } catch (IOException ex) {
                    LOGGER.error("Decoder Exception: ", ex);
                    state = STOP;
                    notifyEvent(Playback.STOPPED);
                    throw new PlayerException(ex);
                }
            }
            if (sourceDataLine != null) {
                sourceDataLine.flush();
                sourceDataLine.stop();
                sourceDataLine.close();
                sourceDataLine = null;
            }
            closeStream();
            if (nBytesRead == -1) {
                notifyEvent(Playback.EOM);
            }
        } finally {
            lock.unlock();
        }
        LOGGER.info("Decoding thread completed");
        return null;
    }

    private void awaitTermination() {
        throwIfFxThread();
        if (future != null && !future.isDone()) {
            try {
                future.get();
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
            } catch (ExecutionException ex) {
                LOGGER.error(ex.getMessage(), ex);
            } finally {
                // Harmless if task already completed
                future.cancel(true); // interrupt if running
            }
        }
    }

    public void play() throws PlayerException {
        throwIfFxThread();
        lock.lock();
        try {
            switch (state) {
                case STOP:
                    initAudioInputStream();
                    initSourceDataLine();
                default:
                    if (sourceDataLine != null && !sourceDataLine.isRunning()) {
                        sourceDataLine.start();
                    }
                    state = PLAY;
                    pauseCondition.signal();
                    notifyEvent(Playback.PLAYING);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        throwIfFxThread();
        if (sourceDataLine != null) {
            if (state == PLAY) {
                state = PAUSE;
                notifyEvent(Playback.PAUSED);
            }
        }
    }

    public void stop() {
        throwIfFxThread();
        if (state != STOP) {
            int oldState = state;
            state = STOP;
            if (oldState == INIT || oldState == PAUSE) {
                lock.lock();
                try {
                    pauseCondition.signal();
                } finally {
                    lock.unlock();
                }
            }
            awaitTermination();
            notifyEvent(Playback.STOPPED);
        }
    }

    public void seek(Duration d) {
        throwIfFxThread();

        // TODO: get rid of this workaround
        if (audioInputStream==null && d.equals(Duration.ZERO)) {
            try {
                state = STOP;
                play();
            } catch (PlayerException e) {
                log(AudioPlayer.class).error("Could not seek, because could not start playing", e);
            }
            return;
        }

        int bytesAll = getByteLength();
        double total = getDuration();
        double tobe = d.toMillis()*1000/total;
        double is = (getPosition())/total;
               is = PLAYBACK.state.currentTime.get().toMillis()/PLAYBACK.state.duration.get().toMillis();

        long bytesTo = (long)(tobe*bytesAll);
        long bytesBy = (long)((tobe-is)*bytesAll);
        try {
            seek(bytesTo, bytesBy);
        } catch (PlayerException ex) {
            LOGGER.error("",ex);
        }
    }

    private long seek(long to, long by) throws PlayerException {
        throwIfFxThread();
        long pos = getPosition();
        long skipped = 0;
        if (audioSource instanceof File) {
            int bytesLength = getByteLength();
            LOGGER.info("Seeking attempt: to: {} by: {} total: {}", to,by,bytesLength);

            if ((bytesLength <= 0) || (to >= bytesLength)) {
                notifyEvent(Playback.EOM);
                LOGGER.info("Seeking - invalid seek position. Pos: {} total: {}", to,bytesLength);
                return skipped;
            }

            oldPosition = getPosition();
            int oldState = state;
            if (state == PLAY) {
                state = PAUSE;
            }
            lock.lock();
            try {
                notifyEvent(Playback.SEEKING);
                skipped = doSeek(to,by);
                oldPosition = getPosition();
                if (skipped == -1) throw new PlayerException("Seek not supported");
                LOGGER.info("Seeking: skipped bytes: {}/{}", skipped, to);
            } catch (IOException ex) {
                throw new PlayerException(ex);
            } finally {
                lock.unlock();
            }
            if (oldState == PLAY) {
                play();
            }
        }
		// System.out.println("POS CHANGED BY " + ((getPosition()-pos)/1000) + "ms");
        return skipped;
    }

    private long doSeek(long to, long by) throws IOException, PlayerException {
        throwIfFxThread();
        if (audioInputStream==null || by ==0)
            return 0;

        // To explain, stream supports 'seeking' forward only, using skip().
        // To seek backwards it needs to support marking, which allows resetting to 0
        // and then skipping  to where we wanted
        boolean isForward = by > 0;
        boolean isForwardOptimizationEnabled = false;
        if (isForward && isForwardOptimizationEnabled) {
            // Unfortunately this is bugged - skips 0. Disabled for now.
            return audioInputStream.skip(to);
        } else {
            // This impl. performs very badly even for single seek. Concecutive seeking is big fail.
			// System.out.println("seeking debug: is mark supported: " + audioInputStream.markSupported());
            if (audioInputStream.markSupported()) {
                audioInputStream.reset();
                return audioInputStream.skip(to);
            } else {
				initAudioInputStream();
				long skipped = audioInputStream.skip(to);
				initSourceDataLine();
				return skipped;
            }
        }
    }

    protected void closeStream() {
        throwIfFxThread();
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
                // TODO: improve
                // Bug fix
                // close() wont release file (file descriptor in FileInputStream) sometimes, this would prevent entire
                // OS from accessing the file until garbage collector is ran or this app terminates
                // apparently a widespread java bug, java devs dont care
                //
                // System.gc(); // call gc manually to release file immediately
                //
                // Calling garbage collector halts the entire application and completely shits on UX (in our case the
                // lag took 1 second and even hanged some system processes, because we were polling mouse position)
                audioInputStream.close();   // This actually works when we call it enough times - yes, i'm serious
                audioInputStream.close();   // This actually works when we call it enough times - yes, i'm serious
                audioInputStream.close();   // This actually works when we call it enough times - yes, i'm serious
                audioInputStream = null;
                LOGGER.info("Stream closed");
            } catch (IOException ex) {
                LOGGER.error("Cannot close stream", ex);
            }
        }
    }
}
