package fr.lelouet.stress;

import fr.lelouet.stresscloud.BurstStress;

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
 * 
 */
public class Calibration {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Calibration.class);

	/**
	 * get a load close to a required error rate using dichotomy.
	 * 
	 * @param run
	 *            a stresser to calibrate
	 * @param errorRate
	 *            the error rate of the stresser we want.
	 * @param sleepTime
	 *            the number of ms to sleep to check the error rate.
	 * @return a value of load which generate an error around the one required
	 */
	public static double calibrate(BurstStress run, double errorRate) {
		double relError = 0.05;
		double maxdelta = errorRate * relError;
		/**
		 * if we have an error, then it will be at least this value of error
		 * rate
		 */
		long sleepTime = (long) (run.getLoopMS() / errorRate);
		logger.debug("calibrate sleep time set to " + sleepTime + " ms");
		double min = 0;
		double minErr = 0;
		double max = 0;
		double maxErr = 0;
		evalErrorRate(run, 2 * run.getLoopMS(), 1);
		System.err.println("max load : " + run.getMaxUsagesPerSecond()
				+ "; sleeptime=" + sleepTime);
		// first we try to ge a max value. grow by power of 2
		try {
			for (max = run.getMaxUsagesPerSecond(); maxErr < errorRate; max = Math
					.max(max * 2, run.getMaxUsagesPerSecond() / 4)) {
				maxErr = evalErrorRate(run, sleepTime, max);
			}
		} catch (Exception e) {
			logger.warn("" + e);
		}
		// then we start dichotomy until a value is between erroRate-maxDelta
		// and
		// errorRate+maxDelta
		while (maxErr - errorRate > maxdelta && errorRate - minErr > maxdelta
				&& max != min) {
			double mean = (max + min) / 2;
			if (mean == min || mean == max) {
				min = max = mean;
			}
			double err = evalErrorRate(run, sleepTime, mean);
			logger.debug("dicho for calibrate : (" + min + "," + minErr + ")("
					+ mean + "," + err + ")(" + max + "," + maxErr + ")");
			if (err < errorRate) {
				min = mean;
				minErr = err;
			} else {
				max = mean;
				maxErr = err;
			}
		}
		if (maxErr - errorRate < maxdelta) {
			return max;
		}
		if (errorRate - minErr < maxdelta) {
			return min;
		}
		return (min + max) / 2;
	}

	public static double evalErrorRate(BurstStress run, long sleepTime,
			double load) {
		// System.err.println("evaluating " + run + " for " + sleepTime +
		// "ms and "
		// + load + "load");
		run.setLoad(load);
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < 3; i++) {
			run.setLoad(load);
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.warn("" + e);
			}
			double res = run.getSkipped() * 1.0 / sleepTime;
			if (res < min) {
				min = res;
			}
		}
		logger.debug("load " + load + "gets " + run.getSkipped()
				+ " ms errors in " + sleepTime + "ms : error rate is " + min);
		return min;
	}
}
