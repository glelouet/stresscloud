package fr.lelouet.stresscloud.control;

/*
 * #%L
 * StressCloud-common
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
 * base of a sequence of loads to apply
 * 
 * @author guillaume
 */
public class LoadBase {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LoadBase.class);

	double loadMultiplier;
	long duration;

	/** construct a LoadBase with duration 1s and base multiplier=1 */
	public LoadBase() {
		this(1, 1);
	}

	public LoadBase(long duration) {
		this(duration, 1);
	}

	/**
	 * @param duration
	 *            the duration of the stresses before changing the load
	 * @param base
	 *            the base multiplier of each load
	 */
	public LoadBase(long duration, double base) {
		this.duration = duration;
		loadMultiplier = base;
	}

	/** @return the base load */
	public double getLoadMultiplier() {
		return loadMultiplier;
	}

	/**
	 * @param base
	 *            the base load to set
	 */
	public void setLoadMultiplier(double base) {
		loadMultiplier = base;
	}

	/** @return the duration */
	public long getDuration() {
		return duration;
	}

	/**
	 * @param duration
	 *            the duration to set
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * call a sequence of loads on a stresser.
	 * 
	 * @param stresser
	 *            the stresser to set the load
	 * @param loads
	 *            the weights of the base load to apply
	 */
	public void to(RegisteredStresser stresser, double... loads) {
		if (loads != null) {
			for (double l : loads) {
				stresser.setLoad(l * loadMultiplier);
				stresser.getParent().after(duration);
			}
		}
		stresser.setLoad(0);
	}

}
