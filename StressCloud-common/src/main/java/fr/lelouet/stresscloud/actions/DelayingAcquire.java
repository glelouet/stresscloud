package fr.lelouet.stresscloud.actions;

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

import fr.lelouet.stresscloud.control.BasicRegisteredVM;

/**
 * wait until given time elapses before ending next commands to the vm.
 * 
 * @author guillaume
 */
public class DelayingAcquire implements Action {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(DelayingAcquire.class);

	long seconds = 0;

	/** set to true to sleep(seconds), false to waitUntil(seconds) */
	boolean absolute = true;

	public DelayingAcquire(long seconds) {
		this.seconds = seconds;
	}

	/**
	 * set whether the wait should be absolute(eg. sleep(30)) or relative(eg.
	 * wait the 30th seconds of the script)
	 * 
	 * @param abs
	 *            the valueof {@link #absolute} to set.
	 * @return this
	 */
	public DelayingAcquire toAbsoluteWait(boolean abs) {
		absolute = abs;
		return this;
	}

	@Override
	public void apply(BasicRegisteredVM target) {
		// System.err.println("applying " + this + " to " + target);
		try {
			if (absolute) {
				Thread.sleep(seconds * 1000);
			} else {
				Thread.sleep(seconds * 1000 - target.getRegistar().getTime());
			}
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
		target.removeWaitingResponseId(syncedId());
		target.requestStressersUpdate();
	}

	@Override
	public long syncedId() {
		return hashCode() + 1;
	}

	@Override
	public String toString() {
		return "delay(" + (absolute ? "for" : "till") + seconds + "s)";
	}

}
