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

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
@SuppressWarnings("restriction")
public class UnsafeRamReaderStress extends UnsafeRamWriterStress {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(UnsafeRamReaderStress.class);

	/**
	 * 
	 */
	public UnsafeRamReaderStress() {
		/** add the bytes '0' '1' '2' .. '9' '0' '1' etc. */
		for (long l = 0; l < getBuffSize(); l++) {
			getUnsafe().putByte(l + getBuffAddress(), (byte) ('0' + l % 10));
		}
	}

	@Override
	protected void onEndBuffer() {
	}

	@Override
	void accessBuffer(long buffAddr, long size, byte value) {
		for (int i = 0; i < size; i++) {
			getUnsafe().getByte(buffAddr + i);
		}
	}

	@Override
	public String getLoadUnit() {
		return "number of Bytes to read from RAM per second";
	}

}
