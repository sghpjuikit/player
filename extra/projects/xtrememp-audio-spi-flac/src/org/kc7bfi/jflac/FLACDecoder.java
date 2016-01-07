package org.kc7bfi.jflac;

/**
 *  libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;

import org.kc7bfi.jflac.frame.BadHeaderException;
import org.kc7bfi.jflac.frame.ChannelConstant;
import org.kc7bfi.jflac.frame.ChannelFixed;
import org.kc7bfi.jflac.frame.ChannelLPC;
import org.kc7bfi.jflac.frame.ChannelVerbatim;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.frame.Header;
import org.kc7bfi.jflac.io.BitInputStream;
import org.kc7bfi.jflac.io.RandomFileInputStream;
import org.kc7bfi.jflac.metadata.Application;
import org.kc7bfi.jflac.metadata.CueSheet;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.Padding;
import org.kc7bfi.jflac.metadata.Picture;
import org.kc7bfi.jflac.metadata.SeekPoint;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.Unknown;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.util.ByteData;
import org.kc7bfi.jflac.util.CRC16;

/**
 * A Java FLAC decoder.
 * @author kc7bfi
 */
public class FLACDecoder {
    private static final int FRAME_FOOTER_CRC_LEN = 16; // bits
    private static final byte[] ID3V2_TAG = new byte[] { 'I', 'D', '3' };
    
    private BitInputStream bitStream;
    private ChannelData[] channelData = new ChannelData[Constants.MAX_CHANNELS];
    private int outputCapacity;
    private int outputChannels;
    private long samplesDecoded;
    private StreamInfo streamInfo;
    private Frame frame = new Frame();
    private byte[] headerWarmup = new byte[2]; // contains the sync code and reserved bits
    //private int state;
    private int channels;
    private int channelAssignment;
    private int bitsPerSample;
    private int sampleRate; // in Hz
    private int blockSize; // in samples (per channel)
    private InputStream inputStream;
    private int metadataLength;
    
    private int badFrames;
    private boolean eof = false;
    
    private FrameListeners frameListeners = new FrameListeners();
    private PCMProcessors pcmProcessors = new PCMProcessors();
    
    // Decoder states
    //private static final int DECODER_SEARCH_FOR_METADATA = 0;
    //private static final int DECODER_READ_METADATA = 1;
    //private static final int DECODER_SEARCH_FOR_FRAME_SYNC = 2;
    //private static final int DECODER_READ_FRAME = 3;
    //private static final int DECODER_END_OF_STREAM = 4;
    //private static final int DECODER_ABORTED = 5;
    //private static final int DECODER_UNPARSEABLE_STREAM = 6;
    //private static final int STREAM_DECODER_MEMORY_ALLOCATION_ERROR = 7;
    //private static final int STREAM_DECODER_ALREADY_INITIALIZED = 8;
    //private static final int STREAM_DECODER_INVALID_CALLBACK = 9;
    //private static final int STREAM_DECODER_UNINITIALIZED = 10;
    
    /**
     * The constructor.
     * @param inputStream    The input stream to read data from
     */
    public FLACDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
        this.bitStream = new BitInputStream(inputStream);
        //state = DECODER_SEARCH_FOR_METADATA;
        samplesDecoded = 0;
        //state = DECODER_SEARCH_FOR_METADATA;
    }
    
    /**
     * Return the parsed StreamInfo Metadata record.
     * @return  The StreamInfo
     */
    public StreamInfo getStreamInfo() {
        return streamInfo;
    }
    
    /**
     * Return the ChannelData object.
     * @return  The ChannelData object
     */
    public ChannelData[] getChannelData() {
        return channelData;
    }
    
    /**
     * Return the input bit stream.
     * @return  The bit stream
     */
    public BitInputStream getBitInputStream() {
        return bitStream;
    }
    
    /**
     * Return the input stream.
     * @return  The input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }
    
    /**
     * Add a frame listener.
     * @param listener  The frame listener to add
     */
    public void addFrameListener(FrameListener listener) {
        frameListeners.addFrameListener(listener);
    }
    
    /**
     * Remove a frame listener.
     * @param listener  The frame listener to remove
     */
    public void removeFrameListener(FrameListener listener) {
        frameListeners.removeFrameListener(listener);
    }
    
    /**
     * Add a PCM processor.
     * @param processor  The processor listener to add
     */
    public void addPCMProcessor(PCMProcessor processor) {
        pcmProcessors.addPCMProcessor(processor);
    }
    
    /**
     * Remove a PCM processor.
     * @param processor  The processor listener to remove
     */
    public void removePCMProcessor(PCMProcessor processor) {
        pcmProcessors.removePCMProcessor(processor);
    }
    
    /**
     * return length of metadata, so can be considered as first frame offset
     * @return
     */
    public long getMetadataLength() {
    	return metadataLength;
    }
    
    private boolean callPCMProcessors(Frame frame) {
    	ByteData bd = decodeFrame(frame, null);
        pcmProcessors.processPCM(bd);
        return pcmProcessors.isCanceled() == false;
    }
    
    /**
     * Fill the given ByteData object with PCM data from the frame.
     *
     * @param frame the frame to send to the PCM processors
     * @param pcmData the byte data to be filled, or null if it should be allocated
     * @return the ByteData that was filled (may be a new instance from <code>space</code>) 
     */
    public ByteData decodeFrame(Frame frame, ByteData pcmData) {
    	// required size of the byte buffer
    	int byteSize = frame.header.blockSize * channels * ((streamInfo.getBitsPerSample() + 7) / 2);
    	if (pcmData == null || pcmData.getData().length < byteSize ) {
    		pcmData = new ByteData(byteSize);
    	} else {
    		pcmData.setLen(0);
    	}
        if (streamInfo.getBitsPerSample() == 8) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    pcmData.append((byte) (channelData[channel].getOutput()[i] + 0x80));
                }
            }
        } else if (streamInfo.getBitsPerSample() == 16) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    short val = (short) (channelData[channel].getOutput()[i]);
                    pcmData.append((byte) (val & 0xff));
                    pcmData.append((byte) ((val >> 8) & 0xff));
                }
            }
        } else if (streamInfo.getBitsPerSample() == 24) {
            for (int i = 0; i < frame.header.blockSize; i++) {
                for (int channel = 0; channel < channels; channel++) {
                    int val = (channelData[channel].getOutput()[i]);
                    pcmData.append((byte) (val & 0xff));
                    pcmData.append((byte) ((val >> 8) & 0xff));
                    pcmData.append((byte) ((val >> 16) & 0xff));
                }
            }
        }
        return pcmData;
    }
    
    /**
     * Read the FLAC stream info.
     * @return  The FLAC Stream Info record
     * @throws IOException On read error
     */
    public StreamInfo readStreamInfo() throws IOException {
        readStreamSync();
        Metadata metadata = readNextMetadata();
        if (!(metadata instanceof StreamInfo)) throw new IOException("StreamInfo metadata block missing");
        return (StreamInfo) metadata;
    }
    
    /**
     * Read an array of metadata blocks.
     * @return  The array of metadata blocks
     * @throws IOException  On read error
     */
    public Metadata[] readMetadata() throws IOException {
        readStreamSync();
        Vector<Metadata> metadataList = new Vector<Metadata>();
        metadataLength = 0;
        Metadata metadata;
        do {
            metadata = readNextMetadata();
            metadataList.add(metadata);
            metadataLength += metadata.getLength();
        } while (!metadata.isLast());
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }
    
    /**
     * Read an array of metadata blocks.
     * @param streamInfo    The StreamInfo metadata block previously read
     * @return  The array of metadata blocks
     * @throws IOException  On read error
     */
    public Metadata[] readMetadata(StreamInfo streamInfo) throws IOException {
        if (streamInfo.isLast()) return new Metadata[0];
        Vector<Metadata> metadataList = new Vector<Metadata>();
        metadataLength = 0;
        Metadata metadata;
        do {
            metadata = readNextMetadata();
            metadataList.add(metadata);
        } while (!metadata.isLast());
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }
    
    /**
     * process a single metadata/frame.
     * @return True of one processed
     * @throws IOException  on read error
     */
    /*
     public boolean processSingle() throws IOException {
     
     while (true) {
     switch (state) {
     case DECODER_SEARCH_FOR_METADATA :
     readStreamSync();
     break;
     case DECODER_READ_METADATA :
     readNextMetadata(); // above function sets the status for us
     return true;
     case DECODER_SEARCH_FOR_FRAME_SYNC :
     frameSync(); // above function sets the status for us
     break;
     case DECODER_READ_FRAME :
     readFrame();
     return true; // above function sets the status for us
     //break;
      case DECODER_END_OF_STREAM :
      case DECODER_ABORTED :
      return true;
      default :
      return false;
      }
      }
      }
      */
    
    /**
     * Process all the metadata records.
     * @throws IOException On read error
     */
    /*
     public void processMetadata() throws IOException {
     
     while (true) {
     switch (state) {
     case DECODER_SEARCH_FOR_METADATA :
     readStreamSync();
     break;
     case DECODER_READ_METADATA :
     readNextMetadata(); // above function sets the status for us
     break;
     case DECODER_SEARCH_FOR_FRAME_SYNC :
     case DECODER_READ_FRAME :
     case DECODER_END_OF_STREAM :
     case DECODER_ABORTED :
     default :
     return;
     }
     }
     }
     */
    
    /**
     * Decode the FLAC file.
     * @throws IOException  On read error
     */
    public void decode() throws IOException {
        readMetadata();
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    if (!callPCMProcessors(frame))
                    	throw new EOFException();
                } catch (FrameDecodeException e) {
                	if (__DEBUG)
                		e.printStackTrace();
                    badFrames++;
                }
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    /**
     * Decode the data frames.
     * @throws IOException  On read error
     */
    public void decodeFrames() throws IOException {
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    callPCMProcessors(frame);
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    private final static boolean __SEEK_DEBUG = false;
    /** Seeks for sample and provide seek data
     * 
     * @param sampleOffset
     * @return SeekPoint of best match
     * @throws IOException 
     */
	public SeekPoint seek(long target_sample) throws IOException {
		// Check if it can found using seek table first
		if (inputStream instanceof RandomFileInputStream == false)
			return null;
		RandomFileInputStream rf = (RandomFileInputStream) inputStream;
		long stream_length = ((RandomFileInputStream) inputStream).getLength();
		int first_frame_offset = metadataLength;
		long total_samples = streamInfo.getTotalSamples();
		int min_blocksize = streamInfo.getMinBlockSize();
		int max_blocksize = streamInfo.getMaxBlockSize();
		int min_framesize = streamInfo.getMinFrameSize();
		int max_framesize = streamInfo.getMaxFrameSize();
		int channels = streamInfo.getChannels();
		int bps = streamInfo.getBitsPerSample();

		int approx_bytes_per_frame = 0;
		/* We are just guessing here, but we want to guess high, not low. */
		if (max_framesize > 0)
			approx_bytes_per_frame = max_framesize;
		/* Check if it's a known fixed-blocksize stream. */
		else if (min_blocksize == max_blocksize && min_blocksize > 0)
			approx_bytes_per_frame = min_blocksize * channels * bps / 8 + 64;
		else
			approx_bytes_per_frame = 4608 * channels * bps / 8 + 64;
		if (__SEEK_DEBUG)
			System.err.printf("Seek in ts:%d, mib:%d, mab:%d, mif: %d, maf:%d bpf: %d%n", total_samples, min_blocksize,
					max_blocksize, min_framesize, max_framesize, approx_bytes_per_frame);
		if (min_blocksize == 0)
			min_blocksize = max_blocksize / 2;
		if (min_framesize == 0)
			min_framesize = max_framesize / 2;
		/* Set an upper and lower bound on where in the stream we will search. */
		int lower_bound = first_frame_offset;

		long upper_bound;
		/* Calc the upper_bound, beyond which we never want to seek. */
		if (max_framesize > 0)
			/* 128 for a possible ID3V1 tag, 2 for indexing differences */
			upper_bound = stream_length - (max_framesize + 128 + 2);
		else
			upper_bound = stream_length - ((channels * bps * Constants.MAX_BLOCK_SIZE) / 8 + 128 + 2);

		long pos = -1;
		/* If there's no seek table, we need to use the metadata (if we
		 * have it) and the filelength to estimate the position of the
		 * frame with the correct sample.
		 */
		if (total_samples > 0) {
			/* For max accuracy we should be using
			 * (stream_length-first_frame_offset-1) in the divisor, but the
			 * difference is trivial and (stream_length-first_frame_offset)
			 * has no chance of underflow.
			 */
			pos = first_frame_offset + ((target_sample * (stream_length - first_frame_offset)) / total_samples)
					- approx_bytes_per_frame;
		}
		/* If there's no seek table and total_samples is unknown, we
		 * don't even bother trying to figure out a target, we just use
		 * our current position.
		 */
		int i = 0;
		boolean needs_seek = pos >= 0;
		long this_frame_sample = 0, last_frame_sample = 0;
		int this_block_size = 0;
		int this_jump = 0, last_jump = 0;
		long last_pos = pos;
		int sample_skip = 0;
		/// save current file position
		long savedPos = rf.getPosition();
		BitInputStream savedState = bitStream;
		bitStream = new BitInputStream(rf);
		Frame savedFrame = frame;
		frame = new Frame();

		while (true) {
			/* Clip the position to the bounds, lower bound takes precedence. */
			if (pos >= upper_bound) {
				pos = upper_bound - 1;
				needs_seek = true;
			}
			if (pos < lower_bound) {
				pos = lower_bound;
				needs_seek = true;
			}

			if (needs_seek) {
				bitStream.reset();
				rf.seek(pos);
				needs_seek = false;
				i++;
			}

			/* Now we need to get a frame.  It is possible for our seek
			 * to land in the middle of audio data that looks exactly like
			 * a frame header from a future version of an encoder.  When
			 * that happens, frame_sync() will return false.
			 * But there is a remote possibility that it is properly
			 * synced at such a "future-codec frame", so to make sure,
			 * we wait to see several "unparseable" errors in a row before
			 * bailing out.
			 */
			boolean got_a_frame = false;
			for (int unparseable_count = 0; unparseable_count < 20; unparseable_count++) {
				try {
					findFrameSync();
					readFrame();
					got_a_frame = true;
					break;
				} catch (Exception e) {
					if (__SEEK_DEBUG)
						System.err.printf("iter %d (%s%n", unparseable_count, e);
				}
			}
			if (!got_a_frame) {
				restoreState(savedPos, savedState, savedFrame);
				return null;
			}

			/* Break out if seeking somehow got caught in a loop. */
			if (i >= 30) {
				if (__SEEK_DEBUG)
					System.err.printf("Nothing found after 30 iters ps %d%n", pos);
				//restoreState(savedPos, savedState, savedFrame);
				//return null;
				break;
			}
			this_frame_sample = frame.header.sampleNumber;
			this_block_size = frame.header.blockSize;

			if (target_sample >= this_frame_sample && target_sample < this_frame_sample + this_block_size) {
				/* Found the frame containing the target sample. */
				sample_skip = (int) (target_sample - this_frame_sample);
				break;
			} else if (target_sample < this_frame_sample) {
				if (this_frame_sample - target_sample <= this_block_size * 10) {
					/* Target is no more than 10 frames back,
					 * seek backwards a frame at a time.
					 */
					if (__SEEK_DEBUG)
						System.err.printf("Look back few frames %d to %d%n", approx_bytes_per_frame, target_sample);
					if (this_frame_sample == last_frame_sample && pos < last_pos) {
						/* Our last move backwards wasn't big enough, double it. */
						pos -= (last_pos - pos);
					} else {
						last_pos = pos;
						pos -= approx_bytes_per_frame; // framesize
					}
				} else {
					/* Target may be more than 10 frames back,
					 * calculated new seek position.
					 */
					last_pos = pos;
					long min_bytes_to_frame = ((this_frame_sample - target_sample + min_blocksize - 1) / min_blocksize)
							* min_framesize;
					long max_bytes_to_frame = ((this_frame_sample - target_sample + max_blocksize - 1) / max_blocksize)
							* max_framesize;
					long delta = this_frame_sample - last_frame_sample;
					if (last_frame_sample > 0 && last_jump > 0 && delta != 0) {
						this_jump = (int) (last_jump * (this_frame_sample - target_sample) / delta);
						if (this_jump < 0)
							this_jump = -this_jump;
					} else if (delta == 0)
						this_jump = last_jump + last_jump / 2;
					else
						this_jump = (int) ((min_bytes_to_frame + max_bytes_to_frame) / 2);

					if (last_jump > 0 && this_jump >= last_jump)
						this_jump = last_jump - approx_bytes_per_frame;
					pos = rf.getPosition() - this_jump;
					last_jump = this_jump;
				}
				if (__SEEK_DEBUG)
					System.err.printf("Jump backward samples %d for %d to %d%n", this_frame_sample - target_sample,
							this_jump, target_sample);
				needs_seek = true;
			} else if (target_sample > this_frame_sample) {
				last_pos = pos;
				if (target_sample - this_frame_sample <= min_blocksize * 10) {
					if (__SEEK_DEBUG)
						System.err.printf("Keep reading for %d%n", min_blocksize * 10);
					/* Target is no more than 10 frames ahead,
					 * seek forwards a frame at a time.
					 */

					pos = rf.getPosition();
					//needs_seek = true;
					// just keep reading
					/* If we haven't hit the target frame yet and our position
					 * hasn't changed, it means we're at the end of the stream
					 * and the seek target does not exist.
					 */
				} else {
					/* Target may be more than 10 frames ahead,
					 * calculated new seek position.
					 */

					long min_bytes_to_frame = ((target_sample - this_frame_sample + max_blocksize - 1) / max_blocksize)
							* min_framesize;
					long max_bytes_to_frame = ((target_sample - this_frame_sample + min_blocksize - 1) / min_blocksize)
							* max_framesize;
					long delta = this_frame_sample - last_frame_sample;
					//System.err.printf("del %d, thi %d, las %d lasj %d%n", delta, this_frame_sample,
					//	last_frame_sample, last_jump);
					if (last_frame_sample > 0 && last_jump > 0 && delta != 0) {
						this_jump = (int) (last_jump * (this_frame_sample - target_sample) / delta);
						if (this_jump < 0)
							this_jump = -this_jump;
					} else
						this_jump = (int) ((min_bytes_to_frame + max_bytes_to_frame) / 2);

					if (last_jump > 0 && this_jump >= last_jump)
						this_jump = last_jump - approx_bytes_per_frame;
					pos = rf.getPosition() + this_jump;
					if (__SEEK_DEBUG)
						System.err.printf("Need a jump forward samples  %d for %d to %d%n", target_sample
							- this_frame_sample, this_jump, target_sample);
					last_jump = this_jump;
					needs_seek = true;
				}
			}
			last_frame_sample = this_frame_sample;
		}
		if (__SEEK_DEBUG)
			System.err.printf("Completed in %d%n", i);
		return new SeekPoint(target_sample - sample_skip, last_pos, sample_skip);
	}
    
    private void restoreState(long savedPos, BitInputStream savedState, Frame savedFrame) {
    	if (inputStream instanceof RandomFileInputStream == false)
			return;
    	try {
			((RandomFileInputStream)inputStream).seek(savedPos);
			bitStream = savedState;
	    	frame = savedFrame;
		} catch (IOException e) {
			//e.printStackTrace();
		}
    }
    
    /**
     * Decode the data frames between two seek points.
     * @param from  The starting seek point
     * @param to    The ending seek point (non-inclusive)
     * @throws IOException  On read error
     */
    public void decode(SeekPoint from, SeekPoint to) throws IOException {
        // position random access file
        if (!(inputStream instanceof RandomFileInputStream)) throw new IOException("Not a RandomFileInputStream: " + inputStream.getClass().getName());
        ((RandomFileInputStream)inputStream).seek(from.getStreamOffset());
        bitStream.reset();
        samplesDecoded = from.getSampleNumber();
        
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        try {
            while (true) {
                //switch (state) {
                //case DECODER_SEARCH_FOR_METADATA :
                //    readStreamSync();
                //    break;
                //case DECODER_READ_METADATA :
                //    Metadata metadata = readNextMetadata();
                //    if (metadata == null) break;
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync();
                //    break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    frameListeners.processFrame(frame);
                    callPCMProcessors(frame);
                } catch (FrameDecodeException e) {
                    badFrames++;
                }
                //frameListeners.processFrame(frame);
                //callPCMProcessors(frame);
                //System.out.println(samplesDecoded +" "+ to.getSampleNumber());
                if (to != null && samplesDecoded >= to.getSampleNumber()) return;
                //    break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return;
                //default :
                //    throw new IOException("Unknown state: " + state);
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
    }
    
    /*
     private boolean processUntilEndOfStream() throws IOException {
     //boolean got_a_frame;
      
      while (true) {
      switch (state) {
      case DECODER_SEARCH_FOR_METADATA :
      readStreamSync();
      break;
      case DECODER_READ_METADATA :
      readNextMetadata(); // above function sets the status for us
      break;
      case DECODER_SEARCH_FOR_FRAME_SYNC :
      frameSync(); // above function sets the status for us
      //System.exit(0);
       break;
       case DECODER_READ_FRAME :
       readFrame();
       break;
       case DECODER_END_OF_STREAM :
       case DECODER_ABORTED :
       return true;
       default :
       return false;
       }
       }
       }
       */
    
    /**
     * Read the next data frame.
     * @return  The next frame
     * @throws IOException  on read error
     */
    public Frame readNextFrame() throws IOException {
        //boolean got_a_frame;
        
        try {
            while (true) {
                //switch (state) {
                //case STREAM_DECODER_SEARCH_FOR_METADATA :
                //    findMetadata();
                //    break;
                //case STREAM_DECODER_READ_METADATA :
                //    readMetadata(); /* above function sets the status for us */
                //    break;
                //case DECODER_SEARCH_FOR_FRAME_SYNC :
                findFrameSync(); /* above function sets the status for us */
                //System.exit(0);
                //break;
                //case DECODER_READ_FRAME :
                try {
                    readFrame();
                    return frame;
                } catch (FrameDecodeException e) {
                	//e.printStackTrace();
                	//System.err.print("Error "+e+" at sample "+samplesDecoded+" of "+streamInfo.getTotalSamples());
                    badFrames++;
                }
                //break;
                //case DECODER_END_OF_STREAM :
                //case DECODER_ABORTED :
                //    return null;
                //default :
                //    return null;
                //}
            }
        } catch (EOFException e) {
            eof = true;
        }
        return null;
    }
    
    /**
     * Bytes consumed.
     * @return  The number of bytes read
     */
    //public long getBytesConsumed() {
    //    return is.getConsumedBlurbs();
    //}
    
    /**
     * Bytes read.
     * @return  The number of bytes read
     */
    public long getTotalBytesRead() {
        return bitStream.getTotalBytesRead();
    }
    
    /*
     public int getInputBytesUnconsumed() {
     return is.getInputBytesUnconsumed();
     }
     */
    
    private void allocateOutput(int size, int channels) {
        if (size <= outputCapacity && channels <= outputChannels) return;
        
        Arrays.fill(channelData, null);
        
        for (int i = 0; i < channels; i++) {
            channelData[i] = new ChannelData(size);
        }
        
        outputCapacity = size;
        outputChannels = channels;
    }
    
    /**
     * Read the stream sync string.
     * @throws IOException  On read error
     */
    private void readStreamSync() throws IOException {
        int id = 0;
        for (int i = 0; i < 4;) {
            int x = bitStream.readRawUInt(8);
            if (x == Constants.STREAM_SYNC_STRING[i]) {
                i++;
                id = 0;
            } else if (x == ID3V2_TAG[id]) {
                id++;
                i = 0;
                if (id == 3) {
                    skipID3v2Tag();
                    id = 0;
                }
            } else {
                throw new IOException("Could not find Stream Sync");
                //i = 0;
                //id = 0;
            }
        }
    }
    
    /**
     * Read a single metadata record.
     * @return  The next metadata record
     * @throws IOException  on read error
     */
    public Metadata readNextMetadata() throws IOException {
        Metadata metadata = null;
        
        boolean isLast = (bitStream.readRawUInt(Metadata.STREAM_METADATA_IS_LAST_LEN) != 0);
        int type = bitStream.readRawUInt(Metadata.STREAM_METADATA_TYPE_LEN);
        int length = bitStream.readRawUInt(Metadata.STREAM_METADATA_LENGTH_LEN);
        
        if (type == Metadata.METADATA_TYPE_STREAMINFO) {
        	metadata = new StreamInfo(bitStream, length, isLast);
        	if (((StreamInfo)metadata).getTotalSamples() > 0) {
        		streamInfo = (StreamInfo)metadata;
        		pcmProcessors.processStreamInfo(streamInfo);
        	}
        } else if (type == Metadata.METADATA_TYPE_SEEKTABLE) {
            metadata = new SeekTable(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_APPLICATION) {
            metadata = new Application(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_PADDING) {
            metadata = new Padding(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_VORBIS_COMMENT) {
            metadata = new VorbisComment(bitStream, length, isLast);
        } else if (type == Metadata.METADATA_TYPE_CUESHEET) {
	    metadata = new CueSheet(bitStream, length, isLast);
	} else if (type == Metadata.METADATA_TYPE_PICTURE) {
	    metadata = new Picture(bitStream, length, isLast);
        } else {
            metadata = new Unknown(bitStream, length, isLast);
        }
        frameListeners.processMetadata(metadata);
        //if (isLast) state = DECODER_SEARCH_FOR_FRAME_SYNC;
        return metadata;
    }
    
    
    private void skipID3v2Tag() throws IOException {
        
        // skip the version and flags bytes 
        int verMajor = bitStream.readRawInt(8);
        int verMinor = bitStream.readRawInt(8);
        int flags = bitStream.readRawInt(8);
        
        // get the size (in bytes) to skip
        int skip = 0;
        for (int i = 0; i < 4; i++) {
            int x = bitStream.readRawUInt(8);
            skip <<= 7;
            skip |= (x & 0x7f);
        }
        
        // skip the rest of the tag
        bitStream.readByteBlockAlignedNoCRC(null, skip);
    }
    
    private void findFrameSync() throws IOException {
        boolean first = true;
        //int cnt=0;
        
        // If we know the total number of samples in the stream, stop if we've read that many.
        // This will stop us, for example, from wasting time trying to sync on an ID3V1 tag.
        if (streamInfo != null && (streamInfo.getTotalSamples() > 0)) {
            if (samplesDecoded >= streamInfo.getTotalSamples()) {
                //state = DECODER_END_OF_STREAM;
                return;
            }
        }
        
        // make sure we're byte aligned
        if (!bitStream.isConsumedByteAligned()) {
            bitStream.readRawUInt(bitStream.bitsLeftForByteAlignment());
        }
        
        int x;
        try {
            while (true) {
                x = bitStream.readRawUInt(8);
                if (x == 0xff) { // MAGIC NUMBER for the first 8 frame sync bits
                    headerWarmup[0] = (byte) x;
                    x = bitStream.peekRawUInt(8);
                    
                    /* we have to check if we just read two 0xff's in a row; the second may actually be the beginning of the sync code */
                    /* else we have to check if the second byte is the end of a sync code */
                    if (x >> 2 == 0x3e) { /* MAGIC NUMBER for the last 6 sync bits */
                        headerWarmup[1] = (byte) bitStream.readRawUInt(8);
                        //state = DECODER_READ_FRAME;
                        return;
                    }
                }
                if (first) {
                    frameListeners.processError("FindSync LOST_SYNC: " + Integer.toHexString((x & 0xff)));
                    first = false;
                }
            }
        } catch (EOFException e) {
            if (!first) frameListeners.processError("FindSync LOST_SYNC: Left over data in file");
            //state = DECODER_END_OF_STREAM;
        }
    }
    
    /**
     * Read the next data frame.
     * @throws IOException  On read error
     * @throws FrameDecodeException On frame decoding error
     */
    public void readFrame() throws IOException, FrameDecodeException {
        int channel;
        int i;
        int mid, side, left, right;
        short frameCRC; /* the one we calculate from the input stream */
        //int x;
        
        /* init the CRC */
        frameCRC = 0;
        frameCRC = CRC16.update(headerWarmup[0], frameCRC);
        frameCRC = CRC16.update(headerWarmup[1], frameCRC);
        bitStream.resetReadCRC16(frameCRC);
        
        try {
            frame.header = new Header(bitStream, headerWarmup, streamInfo);
        } catch (BadHeaderException e) {
            frameListeners.processError("Found bad header: " + e);
            throw new FrameDecodeException("Bad Frame Header: " + e, e);
        }
        //if (state == DECODER_SEARCH_FOR_FRAME_SYNC) return false;
        allocateOutput(frame.header.blockSize, frame.header.channels);
        for (channel = 0; channel < frame.header.channels; channel++) {
            // first figure the correct bits-per-sample of the subframe
            int bps = frame.header.bitsPerSample;
            switch (frame.header.channelAssignment) {
            case Constants.CHANNEL_ASSIGNMENT_INDEPENDENT :
                /* no adjustment needed */
                break;
            case Constants.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                if (channel == 1)
                    bps++;
            break;
            case Constants.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                if (channel == 0)
                    bps++;
            break;
            case Constants.CHANNEL_ASSIGNMENT_MID_SIDE :
                if (channel == 1)
                    bps++;
            break;
            default :
            }
            // now read it
            try {
                readSubframe(channel, bps);
            } catch (IOException e) {
                frameListeners.processError("ReadSubframe: " + e);
                throw e;
            }
        }
        readZeroPadding();
        
        // Read the frame CRC-16 from the footer and check
        frameCRC = bitStream.getReadCRC16();
        frame.setCRC((short)bitStream.readRawUInt(FRAME_FOOTER_CRC_LEN));
        if (frameCRC == frame.getCRC()) {
            /* Undo any special channel coding */
            switch (frame.header.channelAssignment) {
            case Constants.CHANNEL_ASSIGNMENT_INDEPENDENT :
                /* do nothing */
                break;
            case Constants.CHANNEL_ASSIGNMENT_LEFT_SIDE :
                for (i = 0; i < frame.header.blockSize; i++)
                    channelData[1].getOutput()[i] = channelData[0].getOutput()[i] - channelData[1].getOutput()[i];
            break;
            case Constants.CHANNEL_ASSIGNMENT_RIGHT_SIDE :
                for (i = 0; i < frame.header.blockSize; i++)
                    channelData[0].getOutput()[i] += channelData[1].getOutput()[i];
            break;
            case Constants.CHANNEL_ASSIGNMENT_MID_SIDE :
                for (i = 0; i < frame.header.blockSize; i++) {
                    mid = channelData[0].getOutput()[i];
                    side = channelData[1].getOutput()[i];
                    mid <<= 1;
                    if ((side & 1) != 0) // i.e. if 'side' is odd...
                        mid++;
                    left = mid + side;
                    right = mid - side;
                    channelData[0].getOutput()[i] = left >> 1;
                    channelData[1].getOutput()[i] = right >> 1;
                }
            //System.exit(1);
            break;
            default :
                break;
            }
        } else {
            // Bad frame, emit error and zero the output signal
            frameListeners.processError("CRC Error: " + Integer.toHexString((frameCRC & 0xffff)) + " vs " + Integer.toHexString((frame.getCRC() & 0xffff)));
            for (channel = 0; channel < frame.header.channels; channel++) {
                for (int j = 0; j < frame.header.blockSize; j++)
                    channelData[channel].getOutput()[j] = 0;
            }
        }
        
        // put the latest values into the public section of the decoder instance
        channels = frame.header.channels;
        channelAssignment = frame.header.channelAssignment;
        bitsPerSample = frame.header.bitsPerSample;
        sampleRate = frame.header.sampleRate;
        blockSize = frame.header.blockSize;
        
        //samplesDecoded = frame.header.sampleNumber + frame.header.blockSize;
        samplesDecoded += frame.header.blockSize;
        //System.out.println(samplesDecoded+" "+frame.header.sampleNumber + " "+frame.header.blockSize);
        
        //state = DECODER_SEARCH_FOR_FRAME_SYNC;
        //return;
    }
    
    private void readSubframe(int channel, int bps) throws IOException, FrameDecodeException {
        int x;
        
        x = bitStream.readRawUInt(8); /* MAGIC NUMBER */
        
        boolean haveWastedBits = ((x & 1) != 0);
        x &= 0xfe;
        
        int wastedBits = 0;
        if (haveWastedBits) {
            wastedBits = bitStream.readUnaryUnsigned() + 1;
            bps -= wastedBits;
        }
        
        // Lots of magic numbers here
        if ((x & 0x80) != 0) {
            frameListeners.processError("ReadSubframe LOST_SYNC: " + Integer.toHexString(x & 0xff));
            //state = DECODER_SEARCH_FOR_FRAME_SYNC;
            throw new FrameDecodeException("ReadSubframe LOST_SYNC: " + Integer.toHexString(x & 0xff));
            //return true;
        } else if (x == 0) {
            frame.subframes[channel] = new ChannelConstant(bitStream, frame.header, channelData[channel], bps, wastedBits);
        } else if (x == 2) {
            frame.subframes[channel] = new ChannelVerbatim(bitStream, frame.header, channelData[channel], bps, wastedBits);
        } else if (x < 16) {
            //state = DECODER_UNPARSEABLE_STREAM;
            throw new FrameDecodeException("ReadSubframe Bad Subframe Type: " + Integer.toHexString(x & 0xff));
        } else if (x <= 24) {
            //FLACSubframe_Fixed subframe = read_subframe_fixed_(channel, bps, (x >> 1) & 7);
            frame.subframes[channel] = new ChannelFixed(bitStream, frame.header, channelData[channel], bps, wastedBits, (x >> 1) & 7);
        } else if (x < 64) {
            //state = DECODER_UNPARSEABLE_STREAM;
            throw new FrameDecodeException("ReadSubframe Bad Subframe Type: " + Integer.toHexString(x & 0xff));
        } else {
            frame.subframes[channel] = new ChannelLPC(bitStream, frame.header, channelData[channel], bps, wastedBits, ((x >> 1) & 31) + 1);
        }
        if (haveWastedBits) {
            int i;
            x = frame.subframes[channel].getWastedBits();
            for (i = 0; i < frame.header.blockSize; i++)
                channelData[channel].getOutput()[i] <<= x;
        }
    }
    
    private void readZeroPadding() throws IOException, FrameDecodeException {
        if (!bitStream.isConsumedByteAligned()) {
            int zero = bitStream.readRawUInt(bitStream.bitsLeftForByteAlignment());
            if (zero != 0) {
                frameListeners.processError("ZeroPaddingError: " + Integer.toHexString(zero));
                //state = DECODER_SEARCH_FOR_FRAME_SYNC;
                throw new FrameDecodeException("ZeroPaddingError: " + Integer.toHexString(zero));
            }
        }
    }
    
    /**
     * Get the number of samples decoded.
     * @return Returns the samples Decoded.
     */
    public long getSamplesDecoded() {
        return samplesDecoded;
    }
    
    public void setSamplesDecoded(long samples) {
        samplesDecoded = samples;
    }
    
    /**
     * @return Returns the number of bad frames decoded.
     */
    public int getBadFrames() {
        return badFrames;
    }
    /**
     * @return Returns true if end-of-file.
     */
    public boolean isEOF() {
        return eof;
    }
    
    static final boolean __DEBUG = false;
}
