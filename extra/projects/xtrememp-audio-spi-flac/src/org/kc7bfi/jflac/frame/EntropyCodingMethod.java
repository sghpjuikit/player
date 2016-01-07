package org.kc7bfi.jflac.frame;

import java.io.IOException;

import org.kc7bfi.jflac.io.BitInputStream;

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
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

public abstract class EntropyCodingMethod {
	protected int order; // The partition order, i.e. # of contexts = 2 ^ order.
    protected EntropyPartitionedRiceContents contents; // The context's Rice parameters and/or raw bits.

    /**
     * Read compressed signal residual data.
     * 
     * @param is                The InputBitStream
     * @param predictorOrder    The predicate order
     * @param partitionOrder    The partition order
     * @param header            The FLAC Frame Header
     * @param residual          The residual signal (output)
     * @throws IOException      On error reading from InputBitStream
     */
    abstract void readResidual(BitInputStream is, int predictorOrder, int partitionOrder, Header header, int[] residual) throws IOException; 
}
