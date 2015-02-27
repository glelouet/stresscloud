package fr.lelouet.stress.cpu;

/*
 * #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2015 Mines de Nantes
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Unsafe;
import fr.lelouet.stresscloud.BurstStress;

/**
 * Use the {@link Unsafe} class to write directly on the ram. each instance has
 * its own buffer, represented by an in-memory pointer and the size allocated,
 * and loop-writes in it.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
@SuppressWarnings("restriction")
public class UnsafeRamWriterStress extends BurstStress {

	private static final Logger logger = LoggerFactory
			.getLogger(UnsafeRamWriterStress.class);

	private static Unsafe unsafeInstance = null;

	public static Unsafe getUnsafe() {
		if (unsafeInstance == null) {
			try {
				Field f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				unsafeInstance = (Unsafe) f.get(null);
			} catch (Exception e) {
				logger.warn("", e);
			}

		}
		return unsafeInstance;
	}

	private static int CPUCACHESIZE_BYTES = 1024 * 1024 * 8;

	/**
	 * @return the size in bytes of the CPU cache. We need to have a buffer of
	 *         size > this cache size to prevent data caching in the CPU and
	 *         really access the RAM.
	 */
	public long getCPUCacheSize() {
		return CPUCACHESIZE_BYTES;
	}

	/** the buffer of size {@link #getCPUCacheSize()} to write into */
	private long buffAddress = 0;

	protected final long getBuffAddress() {
		return buffAddress;
	}

	/** the size allocated to the buffer */
	private long buffSize;

	protected final long getBuffSize() {
		return buffSize;
	}

	public UnsafeRamWriterStress() {
		// setMaxActionsPerLoop(1E10);
		buffSize = getCPUCacheSize();
		buffAddress = getUnsafe().allocateMemory(buffSize);
	}

	@Override
	protected void finalize() throws Throwable {
		if (buffAddress != 0) {
			getUnsafe().freeMemory(buffAddress);
		}
	}

	protected byte nextByte = 'a';

	/** the next byte to write in the buffer. */
	protected byte nextByte() {
		nextByte++;
		if (nextByte > 'z') {
			nextByte = 'a';
		}
		return nextByte;
	}

	/** the offset we need to write from on the buffer. */
	protected long nextBufferOffset = 0;

	@Override
	protected double makeAtomicStress(double accesses) {
		long bytes = (long) accesses;
		while (bytes > 0) {
			long smallPartSize = Math.min(buffSize - nextBufferOffset, bytes);
			accessBuffer(buffAddress, smallPartSize, nextByte());
			nextBufferOffset = nextBufferOffset + smallPartSize;
			if (nextBufferOffset >= buffSize) {
				nextBufferOffset -= buffSize;
				onEndBuffer();
			}
			bytes -= smallPartSize;
		}
		return accesses - bytes;
	}

	/** called when a full buffer has been written in the ram */
	protected void onEndBuffer() {
	}

	private boolean massiveModeActive = true;

	/** @return the massMode */
	public boolean isMassMode() {
		return massiveModeActive;
	}

	/**
	 * @param massMode
	 *            the massMode to set
	 */
	public void setMassMode(boolean massMode) {
		massiveModeActive = massMode;
	}

	void accessBuffer(long buffAddr, long size, byte value) {
		Unsafe us = getUnsafe();
		if (massiveModeActive) {
			us.setMemory(buffAddr, size, value);
		} else {
			for (long i = buffAddr; i < buffAddr + size; i++) {
				us.putByte(i, value);
			}
		}
	}

	@Override
	public String getLoadUnit() {
		return "number of B to write on ram per sercond";
	}

	@Override
	public String getType() {
		return TYPES.CPU.toString();
	}

	@Override
	public String toString() {
		return getClass().getCanonicalName() + "(" + " enmasse="
				+ massiveModeActive + " buffSize=" + buffSize + ")";
	}

}
