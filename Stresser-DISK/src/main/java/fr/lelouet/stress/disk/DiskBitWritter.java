package fr.lelouet.stress.disk;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.util.ByteArrayBuffer;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import fr.lelouet.stresscloud.BurstStress;
import fr.lelouet.stresscloud.Stresser;

/** @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr > */
public class DiskBitWritter extends BurstStress {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(DiskBitWritter.class);

	public static class HDLoadMonitor extends SystemMonitor {

		@Override
		public String toString() {
			return "HD_W";
		}

		private final Sigar sigar = new Sigar();
		private FileSystem[] list = null;
		{
			mult = 1.0 / 1024;
			try {
				list = sigar.getFileSystemList();
			} catch (SigarException e) {
				logger.warn("", e);
			}
		}

		/** activity of the underlying resource */
		@Override
		protected double getActivityData() {
			long ret = 0;
			try {
				for (FileSystem element : list) {
					FileSystemUsage usage = sigar.getFileSystemUsage(element
							.getDirName());
					long write = usage.getDiskWriteBytes();
					if (write > 0) {
						ret += write;
					}
				}
			} catch (Exception e) {
				logger.warn("", e);
			}
			return ret;
		}

	}

	@Override
	public SystemMonitor getMonitor() {
		return new HDLoadMonitor();
	}

	@Override
	public String getLoadUnit() {
		return "kB to write on the disk per second";
	}

	@Override
	public String getType() {
		return TYPES.DISK.toString();
	}

	protected File fout = null;
	protected FileOutputStream out = null;
	// protected BufferedOutputStream bos = null;

	/** nb of B written to the disk yet */
	protected long writtenFileSize = 0;

	/** max size of a file before we rewrite it in B */
	protected long maxFileSize = 500 * 1024 * 1024; // 500MB

	protected FileOutputStream getOutputStream() {
		if (writtenFileSize > maxFileSize) {
			try {
				out.getChannel().position(0);
				writtenFileSize = 0;
			} catch (IOException e) {
				logger.warn("", e);
			}
		}
		if (out == null) {// || bos == null
			makeOutPutStream();
			// bos = new BufferedOutputStream(out);
		}
		return out;
	}

	protected byte[] byteSource = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.substring(0, 32).getBytes();
	{
		setLoopMS(600);
	}

	public static final int MAXBUFFERBYTES = 1024 * 1024 * 128;

	/**
	 * create an internal buffer to write on each iteration
	 * 
	 * @param kB
	 *            the number of bytes in the buffer to write.
	 */
	protected void setBufferSize(double kB) {
		int requiredsize = (int) (kB * 1024 + 1);
		requiredsize = Math.min(requiredsize, MAXBUFFERBYTES);
		if (requiredsize <= byteSource.length) {
			return;
		}
		ByteArrayBuffer byteArray = new ByteArrayBuffer(requiredsize);
		for (int i = 0; i < requiredsize / byteSource.length; i++) {
			byteArray.append(byteSource, 0, byteSource.length);
		}
		try {
			byteArray.append(byteSource, 0, requiredsize - byteArray.length());
			byteSource = byteArray.buffer();
		} catch (Exception e) {
			logger.error("buffer size=" + byteArray.length() + ", wanted size="
					+ requiredsize, e);
		}
	}

	/**
	 * @return the number of bytes of the buffer to write on each iteration.
	 *         default is 10 kB
	 */
	public int getBufferSize() {
		return byteSource.length / 1024;
	}

	/**
   *
   */
	protected void makeOutPutStream() {
		if (fout == null) {
			fout = new File(".out.tmp");
			fout.deleteOnExit();
			// try {
			// BufferedOutputStream bos = new BufferedOutputStream(
			// new FileOutputStream(fout));
			// setBufferSize(MAXBUFFERBYTES / 1024);
			// for (long i = 0; i < maxFileSize; i += byteSource.length) {
			// bos.write(byteSource);
			// }
			// bos.close();
			// System.err.println("created the file");
			// }
			// catch (Exception e) {
			// logger.warn("", e);
			// }

		}
		try {
			out = new FileOutputStream(fout);
			addOnExit(new StopHook() {

				@Override
				public void onExit(Stresser exited) throws IOException {
					out.close();
				}
			});
		} catch (FileNotFoundException e) {
			logger.warn("", e);
			out = null;
			return;
		}
	}

	@Override
	protected double makeAtomicStress(double kB) {
		FileOutputStream out = getOutputStream();
		int bytes = (int) (kB * 1024);
		int remainingBytes = bytes;
		try {
			while (remainingBytes > byteSource.length) {
				remainingBytes -= byteSource.length;
				out.write(byteSource);
			}
			if (remainingBytes > 0) {
				out.write(byteSource, 0, remainingBytes);
			}
			out.flush();
			out.getFD().sync();
		} catch (IOException e) {
			logger.warn("" + e);
			out = null;
		}
		writtenFileSize += bytes;
		double ret = 1.0 * bytes / 1024;
		return ret;
	}

	@Override
	public void setItPerLoop() {
		super.setItPerLoop();
		setBufferSize(getItPerLoop());
	}

	@Override
	protected void onNewMaxLoad(double load) {
		super.onNewMaxLoad(load);
		setBufferSize(load * 1000 / getLoopMS());
	}
}
