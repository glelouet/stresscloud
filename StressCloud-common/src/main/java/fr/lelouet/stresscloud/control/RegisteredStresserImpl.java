package fr.lelouet.stresscloud.control;

/* #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2016 Mines de Nantes
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
import java.util.HashSet;
import java.util.List;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.commands.Get;
import fr.lelouet.stresscloud.commands.Set;
import fr.lelouet.stresscloud.commands.Sync;
import fr.lelouet.stresscloud.tools.DelayingContainer;
import groovy.lang.Closure;

/**
 * simple {@link RegisteredVM} that delegates all its {@link #get(String)} and
 * {@link #set(String, String)} to the network layer {@link #parent}
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class RegisteredStresserImpl implements RegisteredStresser {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RegisteredStresserImpl.class);

	/**
	 * set it to the element to return on {@link #setLoad(double)},
	 * {@link #add(double)}
	 */
	public BasicRegisteredVM parent = null;

	@Override
	public RegisteredVM getParent() {
		return parent;
	}

	@Override
	public RegisteredVM add(double totalLoad) {
		set(Stresser.WORK_KEY, "" + totalLoad);
		return parent;
	}

	/** call {@link #add(double)} */
	public RegisteredVM plus(double work) {
		return add(work);
	}

	@Override
	public double getWork() {
		if (presentWork == null) {
			// System.err.println("work is null");
			return 0;
		}
		String val = get(Stresser.WORK_KEY);
		return val != null && !val.equals("null")
				? Double.parseDouble(val)
				: 0.0;
	}

	@Override
	public RegisteredVM setLoad(double load) {
		set(Stresser.LOAD_KEY, "" + load);
		return parent;
	}

	@Override
	public double getLoad() {
		String val = get(Stresser.LOAD_KEY);
		double ret = val != null && !val.equals("null") ? Double
				.parseDouble(val) : 0.0;
		if (presentLoad != null && ret != 0 && parent.getRegistar() != null) {
			presentLoad.endTime = parent.getRegistar().getTime();
		}
		return ret;
	}

	@Override
	public long getSkipped() {
		if (presentLoad == null) {
			return 0;
		}
		String val = get(Stresser.SKIPPED_KEY);
		long ret = val != null && !val.equals("null")
				? Long.parseLong(val)
				: 0L;
		updateSkipped(ret);
		return ret;
	}

	@Override
	public boolean updateActivityResult() {
		if (presentLoad == null && presentWork == null) {
			return false;
		}
		getSkipped();
		getWork();
		return true;
	}

	List<Closure<?>> onLoadChange = new ArrayList<Closure<?>>();

	@Override
	public void onLoadChange(Closure<?> cl) {
		synchronized (onLoadChange) {
			onLoadChange.add(cl);
		}
	}

	@Override
	public List<Closure<?>> onLoadChange() {
		return onLoadChange;
	}

	private HashSet<String> recursiveSetInvocation = new HashSet<String>();

	Work presentWork = null;

	Load presentLoad = null;

	/**
	 * call the objects that listen to property modifications
	 * 
	 * @param propKey
	 *            the modified property
	 */
	protected void callObjectListener(String propKey, String value) {
		// System.err.println("prop " + propKey + " updated to " + value);
		if (recursiveSetInvocation.contains(propKey)) {
			return;
		}
		recursiveSetInvocation.add(propKey);
		if (Stresser.LOAD_KEY.equals(propKey)) {
			synchronized (onLoadChange) {
				for (Closure<?> cl : onLoadChange) {
					cl.call(this);
				}
			}
			updateLoad(value);
		} else if (Stresser.WORK_KEY.equals(propKey)) {
			// System.err.println("updating work");
			updateWork(value);
		}
		recursiveSetInvocation.remove(propKey);
	}

	/**
	 * add a new work request if a registar is known
	 * 
	 * @param value
	 *            the double value of the work request
	 */
	protected void updateWork(String value) {
		if (parent.getRegistar() == null) {
			return;
		}
		long time = parent.getRegistar().getTime();
		if (presentWork != null) {
			presentWork.end(time);
		}
		if (value != null) {
			try {
				Double w = Double.parseDouble(value);
				if (w > 0) {
					presentWork = new Work();
					presentWork.start(time, w, this);
					parent.getRegistar().addWork(presentWork);
				}
			} catch (Exception e) {
				logger.debug("could not apply update of work to value " + value);
			}
		}
	}

	/**
	 * add a new load request if a registar is known
	 * 
	 * @param value
	 *            the double value of the load request
	 */
	protected void updateLoad(String value) {
		if (parent.getRegistar() == null) {
			return;
		}
		long time = parent.getRegistar().getTime();
		if (presentLoad != null) {
			presentLoad.end(time);
			presentLoad = null;
		}
		if (value != null) {
			try {
				Double l = Double.parseDouble(value);
				if (l > 0) {
					presentLoad = new Load();
					presentLoad.start(time, l, this);
					parent.getRegistar().addLoad(presentLoad);
				}
			} catch (Exception e) {
				logger.debug("could not apply update of load to value " + value);
			}
		}
	}

	/**
	 * add a skipped information at present time, if a registar is known.
	 * 
	 * @param value
	 *            the double value (double.tostring) to use as skipped
	 *            information
	 */
	protected void updateSkipped(long value) {
		if (parent.getRegistar() == null) {
			return;
		}
		long time = parent.getRegistar().getTime();
		if (presentLoad != null) {
			presentLoad.addSkipped(time, (double) value);
		}
	}

	protected void updateLoadTime() {
		if (parent.getRegistar() == null) {
			return;
		}
		if (presentLoad != null) {
			presentLoad.endTime = parent.getRegistar().getTime();
		}
	}

	protected String type = null;

	@Override
	public String getType() {
		return type;
	}

	/**
	 * @param registeredVM
	 *            the registeredVM that contains the network layer. The commands
	 *            will be sent to the real stresser through this.
	 * @param type
	 *            the type of the stresser.
	 */
	public RegisteredStresserImpl(BasicRegisteredVM registeredVM, String type) {
		this.type = type;
		parent = registeredVM;
	}

	@Override
	public DelayingContainer<String> set(final String key, final String value) {
		final DelayingContainer<String> ret = parent.addCommand(new Set(key,
				value, type));
		Runnable r = new Runnable() {
			@Override
			public void run() {
				ret.get();
				// System.err.println("returning from " + key + "=" + value);
				callObjectListener(key, value);
			}
		};
		new Thread(r).start();
		return ret;
	}

	@Override
	public String get(String key) {
		return parent.addCommand(new Get(key, type)).get();
	}

	@Override
	public String toString() {
		return "vm" + parent.getId() + "." + type;
	}

	@Override
	public RegisteredVM after() {
		final DelayingContainer<String> lock = parent
				.addCommand(new Sync(type));
		new Thread(new Runnable() {
			@Override
			public void run() {
				lock.get();
				RegisteredStresserImpl.this.updateWork(null);
			}
		}).start();
		return parent;
	}

	@Override
	public void callAfter(Runnable run) {
		parent.callAfter(type, run);
	}
}
