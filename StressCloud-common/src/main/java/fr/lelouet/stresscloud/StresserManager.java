package fr.lelouet.stresscloud;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * abstract class controlling a burn loop.This class takes care of the
 * {@link #run()}, {@link #waitForRunning(long...)},
 * {@link #addOnExit(fr.lelouet.stress.Stresser.StopHook)} parts.<br />
 * implement the {@link #burnLoop()} to make the real burn process
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public abstract class StresserManager implements Stresser, Runnable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(StresserManager.class);

	protected static enum STATES {
		stopped, running
	}

	private STATES wantedstate = STATES.stopped;
	private STATES actualstate = STATES.stopped;

	protected STATES wantedState() {
		return wantedstate;
	}

	/** to sync on when modifying the state */
	private Object stateLock = new Object();

	@Override
	public void stop() {
		logger.trace("getting the stop lock");
		synchronized (stateLock) {
			wantedstate = STATES.stopped;
			for (StopHook sh : onStop) {
				try {
					sh.onExit(this);
				} catch (Exception e) {
					logger.warn("", e);
				}
			}
			onStop.clear();
		}
		logger.trace("stopping " + this + " wantedstate=" + wantedstate);
	}

	@Override
	public void run() {
		/** set the mode to running, if stopped */
		boolean changedDone = false;
		while (!changedDone) {
			logger.trace("wantedState=" + wantedstate + ", actualstate="
					+ actualstate);
			synchronized (stateLock) {
				if (wantedstate == STATES.running
						&& actualstate == STATES.running) {
					logger.trace("skipping the burn : already burning");
					return;
				}
				if (wantedstate == STATES.stopped
						&& actualstate == STATES.stopped) {
					changedDone = true;
					wantedstate = STATES.running;
					actualstate = STATES.running;
					for (Semaphore sem : waitingForRunSemaphores) {
						sem.release();
					}
					waitingForRunSemaphores.clear();
				}
			}
			Thread.yield();
		}
		logger.trace("entering burn loop");
		while (wantedstate == STATES.running) {
			burnLoop();
		}
		actualstate = STATES.stopped;
		logger.debug("stopping the burn");
	}

	@Override
	public void start() {
		new Thread(this).start();
	}

	protected abstract void burnLoop();

	/**
	 * semaphores for threads waiting for a start. Acces should be synchronized
	 * with {@link #stateLock}
	 */
	protected List<Semaphore> waitingForRunSemaphores = new ArrayList<Semaphore>();

	@Override
	public boolean waitForRunning(long... timeoutms) {
		Semaphore sem = new Semaphore(0);
		synchronized (stateLock) {
			if (actualstate == STATES.running) {
				return true;
			} else {
				waitingForRunSemaphores.add(sem);
			}
		}
		try {
			if (timeoutms != null && timeoutms.length > 0) {
				return sem.tryAcquire(1, timeoutms[0], TimeUnit.MILLISECONDS);
			} else {
				sem.acquire();
			}
		} catch (InterruptedException e) {
			logger.warn("", e);
			return false;
		}
		return true;
	}

	@Override
	public boolean keepsRunning() {
		synchronized (stateLock) {
			return actualstate == STATES.running
					&& wantedstate == STATES.running;
		}
	}

	List<StopHook> onStop = new ArrayList<Stresser.StopHook>();

	/** set the object to notify on this Object's call to {@link #stop()} */
	@Override
	public void addOnExit(StopHook onStop) {
		this.onStop.add(onStop);
	}

	@Override
	public String get(String key) {
		if (STATE_KEY.equals(key)) {
			return actualstate.name();
		} else if (LOAD_KEY.equals(key)) {
			return "" + getLoad();
		} else if (PARAMS_KEY.equals(key)) {
			return getParams();
		} else if (TYPE_KEY.equals(key)) {
			return getType().toString();
		} else if (LOADUNIT_KEY.equals(key)) {
			return getLoadUnit();
		} else if (AFTER_KEY.equals(key)) {
			after();
			return "synced";
		} else if (SKIPPED_KEY.equals(key)) {
			return "" + getSkipped();
		}
		return null;
	}

	@Override
	public String set(String key, String val) {
		if (STATE_KEY.equals(key)) {
			STATES state = actualstate;
			if (val.equals(STATES.stopped)) {
				stop();
			}
			return state.name();
		} else if (LOAD_KEY.equals(key)) {
			double l = getLoad();
			setLoad(Double.parseDouble(val));
			return "" + l;
		} else if (WORK_KEY.equals(key)) {
			double work = Double.parseDouble(val);
			addWork(work);
			return "" + work;
		} else if (SKIPPED_KEY.equals(key)) {
			if (getWork() == 0) {
				setLoad(getLoad());
			}
		}
		return null;
	}

	@Override
	public String getParams() {
		return PARAMS_KEY + ";" + LOAD_KEY + ";" + STATE_KEY + ";" + TYPE_KEY
				+ ";" + LOADUNIT_KEY;
	}
}
