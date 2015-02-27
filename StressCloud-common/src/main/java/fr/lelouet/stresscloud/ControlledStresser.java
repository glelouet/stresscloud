package fr.lelouet.stresscloud;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * a shallow stresser for tests. does not work, just expose its attributes.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class ControlledStresser extends Properties implements Stresser {

	private static final long serialVersionUID = 1L;

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ControlledStresser.class);

	public List<String> accessesTraces = new ArrayList<String>();

	public ControlledStresser() {
		set("type", TYPES.CPU.toString());
	}

	@Override
	public String set(String key, String value) {
		if (key.startsWith(WORK_KEY)) {
			String ret = "" + getWork();
			addWork(Double.parseDouble(value));
			return ret;
		}
		if (key.startsWith(LOAD_KEY)) {
			String ret = "" + getLoad();
			setLoad(Double.parseDouble(value));
			return ret;
		}
		return "" + setProperty(key, value);
	}

	@Override
	public String get(String key) {
		return getProperty(key);
	}

	@Override
	public String getProperty(String key) {
		accessesTraces.add("get " + key);
		return super.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		accessesTraces.add("get " + key);
		String res = super.getProperty(key, defaultValue);
		// System.err.println("getting prop " + key + "(" + defaultValue +
		// ") = "
		// + res);
		return res;
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		accessesTraces.add("set " + key + " = " + value);
		return super.setProperty(key, value);
	}

	@Override
	public String getParams() {
		return get("params");
	}

	@Override
	public String getLoadUnit() {
		return get("loadUnit");
	}

	@Override
	public void setLoad(double load) {
		setProperty("load", "" + load);
	}

	@Override
	public double getLoad() {
		return Double.parseDouble(getProperty("load", "0"));
	}

	@Override
	public double getWork() {
		return Double.parseDouble(getProperty("work", "0"));
	}

	@Override
	public void addWork(double work) {
		setLoad(0);
		setProperty("work", "" + (getWork() + work));
	}

	@Override
	public long getSkipped() {
		return Long.parseLong(get("skipped"));
	}

	Object startWaitLock = new Object();

	/** set started to the value and return previous value */
	boolean setStarted(boolean value) {
		return Boolean.valueOf(""
				+ setProperty("started", Boolean.toString(value)));
	}

	boolean isStarted() {
		String val = getProperty("started", Boolean.toString(false));
		boolean ret = Boolean.parseBoolean(val);
		// System.err.println("started[" + val + "] : " + ret);
		return ret;
	}

	@Override
	public void stop() {
		synchronized (startWaitLock) {
			if (setStarted(false)) {
				for (StopHook e : onStop) {
					try {
						e.onExit(this);
					} catch (Exception e1) {
						logger.warn("", e);
					}
				}
				onStop.clear();
			}
		}
	}

	@Override
	public void start() {
		synchronized (startWaitLock) {
			setStarted(true);
			startWaitLock.notifyAll();
		}
	}

	@Override
	public boolean keepsRunning() {
		return isStarted();
	}

	@Override
	public boolean waitForRunning(long... timeoutms) {
		synchronized (startWaitLock) {
			if (isStarted()) {
				return true;
			}
			try {
				if (timeoutms != null && timeoutms.length > 0) {
					startWaitLock.wait(timeoutms[0]);
				} else {
					startWaitLock.wait();
				}
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
			return isStarted();
		}

	}

	List<StopHook> onStop = new ArrayList<Stresser.StopHook>();

	@Override
	public void addOnExit(StopHook onStop) {
		this.onStop.add(onStop);
	}

	@Override
	public String getType() {
		return get("type");
	}

	/** to notify on each clearWork() */
	Object workLock = new Object();

	@Override
	public void after() {
		if (getWork() == 0) {
			return;
		}
		synchronized (workLock) {
			try {
				workLock.wait();
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		}
	}

	public void clearWork() throws InterruptedException {
		setProperty("work", "0");
		synchronized (workLock) {
			workLock.notifyAll();
		}
	}
}
