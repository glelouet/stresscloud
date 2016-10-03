package fr.lelouet.stress.cpu;

/*
 * #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2016 Mines de Nantes
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
 * 
 */
public class UnsafeWritterStressWithPercentage extends UnsafeRamWriterStress {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(UnsafeWritterStressWithPercentage.class);

	protected double percentRam = 0;

	/**
	 * @return the percentRam
	 */
	public double getPercentRam() {
		return percentRam;
	}

	/**
	 * @param percentRam
	 *            the percentRam to set
	 */
	public void setPercentRam(double percentRam) {
		this.percentRam = percentRam;
	}

	public static final String PERCENTRAM_KEY = "percentram";

	@Override
	public String get(String key) {
		if (PERCENTRAM_KEY.equals(key)) {
			return "" + getPercentRam();
		}
		return super.get(key);
	}

	@Override
	public String set(String key, String value) {
		if (PERCENTRAM_KEY.equals(key)) {
			String ret = "" + getPercentRam();
			setPercentRam(Double.parseDouble(value));
			return ret;
		}
		return super.set(key, value);
	}

	@Override
	public String getParams() {
		return super.getParams() + ";" + PERCENTRAM_KEY;
	}

	@Override
	protected double makeAtomicStress(double iterations) {
		long ramIt = (long) (percentRam * iterations / 100);
		long cpuIt = (long) (iterations - ramIt);
		for (; cpuIt > 0; cpuIt--) {
			Math.sqrt(cpuIt / 1000);
		}
		return super.makeAtomicStress(ramIt) + cpuIt;
	}

}
