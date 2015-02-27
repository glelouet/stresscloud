/**
 * 
 */
package fr.lelouet.stresscloud.control;

/*
 * #%L
 * StressCloud-API
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

import java.util.LinkedHashMap;

/**
 * @author Guillaume Le Louët
 * 
 */
public class Load extends Work {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Load.class);

	protected LinkedHashMap<Long, Double> skippedVals = new LinkedHashMap<Long, Double>();

	/**
	 * add a new skipped tata
	 * 
	 * @param time
	 *            the time this value was retrieved
	 * @param s
	 *            the value of skipped for the stresser at that time
	 */
	public void addSkipped(long time, Double s) {
		skippedVals.put(time, s);
		endTime = Math.max(time, endTime);
	}

	@Override
	public String toString() {
		return "load(vm" + worker.getParent().getId() + "." + worker.getType()
				+ "+" + requestedLoad + ")[@" + startTime + "-@" + endTime
				+ "]" + skippedVals;
	}
}
