package fr.lelouet.stresscloud;

/* #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2015 Mines de Nantes
 * %%
 * This program
 * is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License
 * for more details. You should have received a copy of the GNU General Lesser
 * Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L% */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * A stresser that will periodically run a small atomic burn loop.<br />
 * In this implementation, the load is the number of action to do per second;
 * however, an action can have any meaning. <br />
 * gives the layer of granularity and self synchronization with the time<br />
 * to use it, implement the {@link #makeAtomicStress(long)} to realize an atomic
 * stress of given number of actions
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public abstract class BurstStress extends StresserManager {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BurstStress.class);

	/** retrieve, externally, the effective accesses done to the resource */
	public static abstract class SystemMonitor {

		/**
		 * number of accesses performed on the resource at he moment of
		 * {@link #prepareMonitor()}
		 */
		protected double activitybefore = 0;

		/** multiplicator to the number of activities */
		protected double mult = 1.0;

		/** activity of the underlying resource */
		protected double getActivityData() {
			return -1.0;
		}

		/** we want to start monitoring from now on */
		public void prepareMonitor() {
			activitybefore = getActivityData();
		}

		/**
		 * end monitoring and retrieve the number of accesses since last
		 * {@link #prepareMonitor()}
		 */
		public double retrieveData() {
			return mult * (getActivityData() - activitybefore);
		}

	}

	public SystemMonitor getMonitor() {
		return null;
	}

	public BurstStress() {
	}

	private double load = 0;

	@Override
	public void setLoad(double load) {
		synchronized (workLock) {
			remainingWork = 0;
		}
		if (load < 0) {
			load = 0;
		}
		this.load = load;
		timeSkipped = 0;
		if (load > 0) {
			setItPerLoop();
			nextWakeUp = System.currentTimeMillis() + loopMS;
		}
	}

	@Override
	public double getLoad() {
		return load;
	}

	protected final Object workLock = new Object();

	protected double remainingWork = 0;

	@Override
	public void addWork(double work) {
		logger.debug("adding work: " + work);
		synchronized (workLock) {
			remainingWork += work;
			load = 0;
		}
	}

	@Override
	public double getWork() {
		return remainingWork;
	}

	protected void setWork(long work) {
		logger.debug("setting work to " + work);
		synchronized (workLock) {
			remainingWork = work;
		}
	}

	protected double itPerLoop = 0;

	public void setItPerLoop() {
		hasLimitedActionRate = false;
		itPerLoop = getLoad() * getLoopMS() / 1000;
		logger.debug("{}.setItPerLoop : load={}, loopMS={}, iterPerLoop={} ",
				new Object[]{this.getClass().getSimpleName(), getLoad(),
						getLoopMS(), getItPerLoop()});
	}

	public double getItPerLoop() {
		return itPerLoop;
	}

	public static final long DEFAULT_LOOPMS = 5;

	public static final String LOOPMS_KEY = "loopms";

	public static final String MAXUSAGE_KEY = "maxUsage";

	long loopMS = DEFAULT_LOOPMS;

	/**
	 * Set the granularity of the stress. The ms is set to at least 10 ms, or
	 * the sleep() may have no meaning
	 * 
	 * @param ms
	 *            number of ms to loop over
	 */
	public void setLoopMS(long ms) {
		if (ms < 0) {
			ms = 0;
		}
		loopMS = ms;
		setItPerLoop();
	}

	/** @return the number of seconds a burning loops lasts */
	public long getLoopMS() {
		return loopMS;
	}

	@Override
	public String get(String key) {
		if (LOOPMS_KEY.equals(key)) {
			return "" + getLoopMS();
		}
		if (Stresser.WORK_KEY.equals(key)) {
			return "" + getWork();
		}
		if (MAXUSAGE_KEY.equals(key)) {
			return "" + getMaxUsagesPerSecond();
		}
		return super.get(key);
	}

	@Override
	public String set(String key, String value) {
		if (LOOPMS_KEY.equals(key)) {
			String old = "" + getLoopMS();
			setLoopMS(Long.parseLong(value));
			return old;
		}
		if (Stresser.SETWORK_KEY.equals(key)) {
			String old = "" + getWork();
			setWork(Long.parseLong(value));
			return old;
		}
		if (MAXUSAGE_KEY.equals(key)) {
			resetMaxUsagesPerSecond();
			return "0";
		}
		return super.set(key, value);
	}

	@Override
	public String getParams() {
		return super.getParams() + ";" + LOOPMS_KEY;
	}

	long nextWakeUp = System.currentTimeMillis();

	/** time between two monitor of the time taken to burn the resource */
	public static final int monitorPeriod = 10;

	int remainingBeforeMonitor = monitorPeriod;

	double remainingActions = 0;

	private long timeSkipped = 0;

	@Override
	public long getSkipped() {
		return timeSkipped;
	}

	/**
	 * set to true whether the number of actions in a loop has already been
	 * limited by the max load.
	 */
	protected boolean hasLimitedActionRate = false;

	@Override
	protected void burnLoop() {
		if (!waitForLoad()) {
			return;
		}
		long current = System.currentTimeMillis();
		long period = loopMS;
		if (remainingWork > 0) {
			double actions = Math.floor(Math.min(remainingWork, 2
					* getMaxUsagesPerSecond() * getLoopMS() / 1000));
			double done;
			if (remainingBeforeMonitor > 0 && maxUsagesPerSecond > 0) {
				done = makeAtomicStress(actions);
			} else {
				done = makeMonitoredAtomicStress(actions);
			}
			remainingBeforeMonitor--;
			synchronized (workLock) {
				remainingWork -= done;
				if (remainingWork <= 0) {
					remainingWork = 0;
					unlockAfter();
				} else {
					// System.err.println("remaining : " + remainingWork +
					// ", done " +
					// done
					// + " wanted " + actions);
				}
			}
		} else if (load > 0) {
			// check if we skipped a full period
			if (current > nextWakeUp + period) {
				// logger.debug("underburn : expected " + nextWakeUp +
				// " , delayed by "
				// + (current - nextWakeUp) + " ms, load=" + getLoad() +
				// ", loopMS="
				// + getLoopMS() + ", class=" + getClass().getSimpleName());
				timeSkipped += current - nextWakeUp;
				nextWakeUp = current;
				remainingActions = 0;
			} else if (nextWakeUp > current) {
				try {
					Thread.sleep(nextWakeUp - current);
				} catch (InterruptedException e) {
					logger.warn("", e);
				}
			}
			nextWakeUp += period;
			remainingActions += itPerLoop;
			if (remainingActions > 0) {
				double actions = remainingActions;
				// if we already calibrated the stress, we don't want the next
				// stress
				// cycle to last more than 2 times the cycle period.
				if (maxUsagesPerSecond > 0) {
					double maxActions = maxUsagesPerSecond * loopMS / 100;
					if (actions > maxActions) {
						if (!hasLimitedActionRate) {
							logger.debug("preventing from doing more than "
									+ maxActions
									+ " actions per loop, maxUsagesPerSecond="
									+ maxUsagesPerSecond);
							hasLimitedActionRate = true;
						}
						actions = maxActions;
					}
				}
				if (remainingBeforeMonitor > 0 && maxUsagesPerSecond > 0) {
					remainingActions -= makeAtomicStress(actions);
				} else {
					remainingActions -= makeMonitoredAtomicStress(actions);
				}
				remainingBeforeMonitor--;
			}
			// in case we modified it while the addWork was called.
			if (remainingWork > 0) {
				load = 0;
			}
		} else {
			try {
				Thread.sleep(period);
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		}
	}

	/**
	 * wait untill load is set to >0 or work is added, or should stop
	 * 
	 * @return true if work was added or load was added ; false if was asked to
	 *         stop.
	 */
	protected boolean waitForLoad() {
		if (load > 0 || remainingWork > 0) {
			return true;
		}
		try {
			Thread.sleep(getLoopMS());
		} catch (InterruptedException e) {
			throw new UnsupportedOperationException(e);
		}
		return load > 0 || remainingWork > 0;
	}

	List<Semaphore> afterSemaphores = new ArrayList<Semaphore>();

	@Override
	public void after() {
		if (remainingWork <= 0) {
			logger.debug(getClass().getName() + " skipping the after lock");
			return;
		}
		Semaphore sem = new Semaphore(0);
		synchronized (workLock) {
			if (remainingWork <= 0) {
				logger.debug(getClass().getName() + " skipping the after lock");
				return;
			}
			logger.debug(getClass().getName() + "getting the after work");
			synchronized (afterSemaphores) {
				afterSemaphores.add(sem);
			}
		}
		try {
			sem.acquire();
			logger.debug(getClass().getName() + "got the semaphore after work");
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
	}

	protected void unlockAfter() {
		logger.debug("unlocking the after() : " + afterSemaphores.size());
		synchronized (afterSemaphores) {
			for (Semaphore s : afterSemaphores) {
				s.release();
			}
			afterSemaphores.clear();
		}
	}

	/**
	 * really use the resource during a burning loop.
	 * 
	 * @param iterations
	 *            the number of usage to realize.
	 * @return the number of access really performed
	 */
	protected abstract double makeAtomicStress(double iterations);

	/** make an atomic stress and check the time it takes */
	protected double makeMonitoredAtomicStress(double iterations) {
		long start = System.nanoTime();
		double actions = makeAtomicStress(iterations);
		long delay = System.nanoTime() - start;
		double usagesPerSecond = iterations * 1000000000.0 / delay;
		if (usagesPerSecond > maxUsagesPerSecond) {
			logger.debug("" + printUnit(iterations, "i") + " in "
					+ printUnit(1E-9 * delay, "s") + " maxload="
					+ printUnit(usagesPerSecond, "Loop/s"));
			onNewMaxLoad(usagesPerSecond);
			remainingBeforeMonitor = 0;
		} else {
			remainingBeforeMonitor = monitorPeriod;
		}
		return actions;
	}

	// ***********************************
	// MAX LOAD OF THE STRESSER
	// ***********************************

	/**
	 * the maximum of usage per second that has been reached. It is not set
	 * until the first {@link #makeMonitoredAtomicStress(double)}
	 */
	double maxUsagesPerSecond = -1;

	/**
	 * get the highest value of usage per second already reached ; monitor the
	 * usage of 1 resource access to get it if non existent yet.
	 * 
	 * @return the max value of usage per second that has been reached.
	 */
	public double getMaxUsagesPerSecond() {
		while (maxUsagesPerSecond == -1) {
			if (getLoad() == 0) {
				makeMonitoredAtomicStress(1);
			} else {
				try {
					remainingBeforeMonitor = 0;
					Thread.sleep(getLoopMS());
				} catch (InterruptedException e) {
					logger.warn("", e);
				}
			}
		}
		return maxUsagesPerSecond;
	}

	/**
	 * reset the value of max usage per second to "undefined", so it will be
	 * monitored on next {@link #getMaxUsagesPerSecond()}
	 */
	public void resetMaxUsagesPerSecond() {
		maxUsagesPerSecond = -1;
	}

	public static interface MaxLoadListener {
		public void onNewMaxLoad(BurstStress stress, double maxLoad);
	}

	/** copy it on modification to avoid synchronization lags */
	List<MaxLoadListener> maxLoadListeners = new ArrayList<BurstStress.MaxLoadListener>();

	protected void onNewMaxLoad(double load) {
		maxUsagesPerSecond = load;
		hasLimitedActionRate = false;
		// logger.debug("new max load for " + this + " : " + load);
		List<MaxLoadListener> maxLoadListeners = this.maxLoadListeners;
		for (MaxLoadListener mll : maxLoadListeners) {
			mll.onNewMaxLoad(this, load);
		}
	}

	public void registerMaxLoadListener(MaxLoadListener maxLoadListener) {
		synchronized (maxLoadListeners) {
			List<MaxLoadListener> newList = new ArrayList<BurstStress.MaxLoadListener>(
					maxLoadListeners);
			newList.add(maxLoadListener);
			maxLoadListeners = newList;
		}
	}

	public double benchMaxLoad() {
		start();
		long wantedTime = 2000;
		int successiveTries = 3;
		double work = 1;
		double bestlongLoad = 0;
		long deltaT = 0;
		int remaining = successiveTries;
		waitForRunning(1000);
		while (deltaT < wantedTime || remaining > 0) {
			if (deltaT < wantedTime) {
				work *= Math.max(2, deltaT > 0 ? wantedTime / deltaT : 1);
				bestlongLoad = 0;
			}
			deltaT = -System.currentTimeMillis();
			addWork(work);
			after();
			deltaT += System.currentTimeMillis();
			if (deltaT >= wantedTime) {
				remaining--;
				double longload = work * 1000 / deltaT;
				bestlongLoad = Math.max(bestlongLoad, longload);
			} else {
				remaining = successiveTries;
				bestlongLoad = 0;
			}
		}
		return bestlongLoad;
	}

	protected static String[] unitPrefixes = {"n", "µ", "m", "", "k", "M", "G",
			"T", "E", "P", "Z"};

	/**
	 * put the correct SI prefix
	 * 
	 * @param unit
	 *            the value to print, in standard unit(meter, gramme, etc.)
	 * @param unitName
	 *            the name of the unit to print
	 * @return the concatenation of the value with the unit, prefixed by the
	 *         correct prefix.<br />
	 *         eg, printUnit(5000, "g") will return "5kg"
	 */
	public static String printUnit(double unit, String unitName) {
		int decal = 3;
		while ((unit >= 1000 || unit < -1000) && decal < unitPrefixes.length) {
			decal++;
			unit /= 1000;
		}
		while (-1 < unit && unit < 1 && decal > 0) {
			decal--;
			unit *= 1000;
		}
		return "" + unit + unitPrefixes[decal]
				+ (unitName == null ? "" : unitName);
	}

}
