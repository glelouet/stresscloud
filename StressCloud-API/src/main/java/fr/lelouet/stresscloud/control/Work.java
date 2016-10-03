/**
 * 
 */
package fr.lelouet.stresscloud.control;

/*
 * #%L
 * StressCloud-API
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
 * a work requested on a stresser. it is a load, sent at a time, and a state :
 * started, then finished or cancelled. if finished or cancelled, the end time
 * is specified.
 * 
 * @author Guillaume Le Louët
 */
public class Work {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(Work.class);

	protected long startTime;

	protected long endTime;

	protected RegisteredStresser worker;

	protected double requestedLoad;

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @return the worker
	 */
	public RegisteredWorker getWorker() {
		return worker;
	}

	/**
	 * @return the requestedLoad
	 */
	public double getRequestedLoad() {
		return requestedLoad;
	}

	/**
	 * 
	 * @return the duration in ms of the activity
	 */
	public long getDuration() {
		return endTime - startTime;
	}

	void start(long time, double requestedLoad, RegisteredStresser worker) {
		startTime = time;
		this.requestedLoad = requestedLoad;
		this.worker = worker;
	}

	void end(long time) {
		endTime = time;
	}

	void end() {
		end(System.currentTimeMillis());
	}

	@Override
	public String toString() {
		return "work(vm" + worker.getParent().getId() + "." + worker.getType()
				+ "+" + requestedLoad + ")[@" + startTime + "-@" + endTime
				+ "]";
	}

}
