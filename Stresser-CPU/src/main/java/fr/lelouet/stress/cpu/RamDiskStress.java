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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.lelouet.stresscloud.BurstStress;

/**
 * load = ko/s to write on the ram
 * 
 * @see BurstStress
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class RamDiskStress extends BurstStress {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RamDiskStress.class);

	public final static String MAXFILESIZE_KEY = "maxfileBytes";

	/** default max size of a file is 1Mo */
	private int maxFileSize = 1024 * 1024;

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public int setMaxFileSize(int maxFileSize) {
		int ret = this.maxFileSize;
		this.maxFileSize = maxFileSize;
		setItPerLoop();
		return ret;
	}

	/** the chunk of data to write from on memory. should be &ge; itPerLoop */
	private char[] toWrite = new char[]{};

	/** the size of the buffers to write on each atomic stress */
	private int buffSize = 0;

	protected int getBuffSize() {
		return buffSize;
	}

	/**
	 * generate the internal buffer and its size, according to the required
	 * {@link #getItPerLoop()} and {@link #getMaxFileSize()}
	 */
	protected void makeBuffer() {
		if (maxFileSize >= itPerLoop) {
			buffSize = (int) itPerLoop;
		} else {
			int numbBuff = (int) Math.floor(itPerLoop / maxFileSize);
			buffSize = (int) (itPerLoop / numbBuff);
		}
		if (buffSize <= toWrite.length) {
			return;
		}
		char[] newbuffer = new char[buffSize];
		for (int i = 0; i < newbuffer.length; i++) {
			newbuffer[i] = i % 2 == 0 ? 'a' : 'b';
		}
		toWrite = newbuffer;
	}

	@Override
	public void setItPerLoop() {
		super.setItPerLoop();
		itPerLoop *= 1024;
		makeBuffer();
		logger.debug("size of buffer to write : " + buffSize
				+ ", size per loop : " + itPerLoop + ", max file size : "
				+ maxFileSize);
	}

	@Override
	protected double makeAtomicStress(double actions) {
		double remaining = actions;
		while (remaining > getBuffSize()) {
			remaining -= writeOneBuffer();
		}
		return actions - remaining;
	}

	File output = null;
	FileWriter fw = null;

	long byteswritten = 0;

	/**
	 * write one buffer on the ram, manage the file to do so.
	 */
	protected long writeOneBuffer() {
		if (output != null && byteswritten + getBuffSize() > getMaxFileSize()) {
			output.delete();
			output = null;
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					logger.warn("", e);
				}
			}
		}
		try {
			if (output == null) {
				byteswritten = 0;
				output = File.createTempFile("stress", null);
				output.deleteOnExit();
				fw = new FileWriter(output);
			}
			fw.write(toWrite, 0, getBuffSize());
			byteswritten += getBuffSize();
		} catch (Exception e) {
			logger.warn("", e);
		}
		return getBuffSize();
	}

	@Override
	public String getType() {
		return TYPES.CPU.toString();
	}

	@Override
	public String getLoadUnit() {
		return "kB to write on ram";
	}

	@Override
	public String getParams() {
		return super.getParams() + ";" + MAXFILESIZE_KEY;
	}

	@Override
	public String get(String key) {
		if (MAXFILESIZE_KEY.equals(key)) {
			return "" + getMaxFileSize();
		}
		return super.get(key);
	}

	@Override
	public String set(String key, String value) {
		if (MAXFILESIZE_KEY.equals(key)) {
			return "" + setMaxFileSize(Integer.parseInt(value));
		}
		return super.set(key, value);
	}
}
