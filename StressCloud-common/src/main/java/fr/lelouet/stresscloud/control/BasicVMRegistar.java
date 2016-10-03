package fr.lelouet.stresscloud.control;

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

import fr.lelouet.stresscloud.Stresser.TYPES;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

/**
 * an implementation of the {@link VMRegistar}, to perform simple actions on a
 * set of registered VMs.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class BasicVMRegistar implements VMRegistar {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BasicVMRegistar.class);

	public BasicVMRegistar() {
	}

	public static final Comparator<RegisteredVM> VMCMP = new Comparator<RegisteredVM>() {
		@Override
		public int compare(RegisteredVM o1, RegisteredVM o2) {
			int mem = (int) (o1.getMem() - o2.getMem());
			int cores = o1.getCores() - o2.getCores();
			if (mem != 0) {
				return mem;
			}
			if (cores != 0) {
				return cores;
			}
			String ip1 = o1.getIp(), ip2 = o2.getIp();
			if (ip1 == null || ip2 == null) {
				throw new UnsupportedOperationException(
						"cannot add VMs with null ip");
			}
			int ip = ip1.compareTo(ip2);
			if (ip != 0) {
				return ip;
			}
			return (int) (o1.getId() - o2.getId());
		}
	};

	TreeSet<RegisteredVM> availableVMs = new TreeSet<RegisteredVM>(VMCMP);

	public void setAvailableVMs(TreeSet<RegisteredVM> availableVMs) {
		synchronized (listsLock) {
			this.availableVMs = availableVMs;
		}
		onNewAvailable();
	}

	@Override
	public Collection<RegisteredVM> getAvailableVMs() {
		return availableVMs;
	}

	List<RegisteredVM> reservedVMs = new ArrayList<RegisteredVM>();

	public void setReservedVMs(List<RegisteredVM> reservedVMs) {
		synchronized (listsLock) {
			this.reservedVMs = reservedVMs;
		}
		onNewAvailable();
	}

	/** lock on {@link #reservedVMs} and {@link #availableVMs} */
	Object listsLock = new Object();

	public void setVMs(List<RegisteredVM> available, List<RegisteredVM> reserved) {
		synchronized (listsLock) {
			reservedVMs = reserved;
			availableVMs.clear();
			availableVMs.addAll(available);
		}
		onNewAvailable();
	}

	protected long requireTimeout = -1;

	/**
	 * set the timeout mode of {@link #require(Closure, int)}.<br />
	 * If negative, require will return null if the required constraints are not
	 * available. If zero, it will try again when a new VM is available. If
	 * positive, it will return null if it could not success after given timeout<br />
	 * This invocation of require is called by all the other invocations
	 */
	public void setRequireTimeout(long milliseconds) {
		requireTimeout = milliseconds;
	}

	@Override
	public RegisteredVM require(Closure<?> filter) {
		List<RegisteredVM> res;
		res = require(filter, 1);
		return res == null || res.size() == 0 ? null : res.get(0);
	}

	/**
	 * check now if the required VMs are available and reserve them.
	 * 
	 * @param filter
	 *            filter to check on the vms.
	 * @param number
	 *            required number of VMs respecting that filter
	 * @return null if could not find those VMs, or the list of reserved VMs
	 */
	protected List<RegisteredVM> findPresentVMs(Closure<?> filter, int number) {
		List<RegisteredVM> ret = new ArrayList<RegisteredVM>();
		for (RegisteredVM elem : availableVMs) {
			boolean passes = false;
			if (filter == null) {
				passes = true;
			} else {
				filter.setDelegate(elem);
				passes = (Boolean) filter.call(elem);
			}
			if (passes) {
				ret.add(elem);
				if (ret.size() == number) {
					break;
				}
			}
		}
		if (ret.size() != number) {
			logger.debug("could not find enough VMs for require : requiring "
					+ number + ", got " + ret.size());
			return null;
		} else {
			for (RegisteredVM vm : ret) {
				boolean reserved = reserve(vm) == vm;
				if (!reserved) {
					logger.warn("cannot reserve selected vm " + vm
							+ " , returning null");
					release(ret);
					return null;
				}
			}
		}
		return ret;
	}

	@Override
	public List<RegisteredVM> require(Closure<?> filter, int number) {
		long timeEnd = System.currentTimeMillis() + requireTimeout;
		int nb = availableVMs.size();
		List<RegisteredVM> ret = findPresentVMs(filter, number);
		while (ret == null
				&& (requireTimeout == 0 || requireTimeout > 0
						&& System.currentTimeMillis() < timeEnd)) {
			waitNewVM(timeEnd - System.currentTimeMillis(), nb);
			nb = availableVMs.size();
			ret = findPresentVMs(filter, number);
		}
		return ret;
	}

	@Override
	public List<RegisteredVM> require(String filter, int number) {
		return require(
				filter == null ? null : GroovyTooling.makeClosure(filter),
				number);
	}

	@Override
	public RegisteredVM require(String filter) {
		List<RegisteredVM> res = require(
				filter == null ? null : GroovyTooling.makeClosure(filter), 1);
		return res != null && res.size() == 1 ? res.get(0) : null;
	}

	protected long lastReserveTime = -1;

	@Override
	public long getScriptStart() {
		return lastReserveTime;
	}

	@Override
	public long getTime() {
		return System.currentTimeMillis() - lastReserveTime;
	}

	@Override
	public RegisteredVM reserve(RegisteredVM target) {
		synchronized (listsLock) {
			boolean removed = availableVMs.remove(target);
			if (removed) {
				reservedVMs.add(target);
			} else {
				logger.debug("cannot remove " + target + " from "
						+ availableVMs);
			}
			lastReserveTime = System.currentTimeMillis();
			return removed ? target : null;
		}
	}

	@Override
	public List<RegisteredVM> getReservedVMs() {
		return reservedVMs;
	}

	@Override
	public void release() {
		synchronized (listsLock) {
			for (RegisteredVM vm : reservedVMs) {
				vm.clear();
			}
			availableVMs.addAll(reservedVMs);
			reservedVMs.clear();
		}
		lastReserveTime = -1;
		works.clear();
		loads.clear();
		clearScripts();
		onNewAvailable();
	}

	/**
	 * wait until a new VM is available, or timeout is reached, or the VM gets
	 * shaken, or a licorn eats a lolilop.<br />
	 * relies on the {@link Object.#wait()}, so is not very reliable. ensures at
	 * least to be awaken when a new VM appears
	 * 
	 * @param milliseconds
	 *            the number of milliseconds to wait
	 * @param nbVMs
	 *            the number of vms present last time we checked. if it is
	 *            different from the number of vms, return at once
	 */
	protected void waitNewVM(long milliseconds, int nbVMs) {
		synchronized (newVMNotification) {
			try {
				if (nbVMs != availableVMs.size()) {
					return;
				}
				if (milliseconds >= 0) {
					newVMNotification.wait(milliseconds);
				} else {
					newVMNotification.wait();
				}
				return;
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		}
	}

	/** notify all the threads waiting for {@link #waitNewVM(long)} */
	protected void onNewAvailable() {
		synchronized (newVMNotification) {
			newVMNotification.notifyAll();
		}
	}

	public void release(RegisteredVM vm) {
		clearScripts();
		synchronized (listsLock) {
			if (reservedVMs.remove(vm)) {
				vm.clear();
				availableVMs.add(vm);
			}
		}
		onNewAvailable();
	}

	public void release(Collection<RegisteredVM> vms) {
		for (RegisteredVM e : vms) {
			release(e);
		}
	}

	/** is notified each time a new VM is made available */
	private final Object newVMNotification = new Object();

	@Override
	public void need(int nbVMs) {
		while (true) {
			int present = availableVMs.size();
			if (present >= nbVMs) {
				return;
			}
			logger.debug("need(" + nbVMs + ") : has " + present);
			waitNewVM(-1, present);
		}
	}

	@Override
	public void newVM(RegisteredVM vm) {
		logger.trace("new VM registered : " + vm);
		vm.setRegistar(this);
		synchronized (listsLock) {
			availableVMs.add(vm);
		}
		onNewAvailable();
	}

	GroovyShell executor = makeShell();

	protected GroovyShell makeShell() {
		GroovyShell ret = new GroovyShell();
		for (TYPES t : TYPES.values()) {
			ret.setProperty(t.name(), t);
		}
		ret.setProperty("system", null);
		// ExpandoMetaClass.enableGlobally();
		return ret;
	}

	/**
	 * structure to store the executed scripts, their start time, end time, and
	 * the result
	 * 
	 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com]2013
	 * 
	 */
	public class ScriptDetail {
		public String script;
		public long start;
		public Object res = null;
		public long finished = -1;

		@Override
		public String toString() {
			return "@" + start + "-@" + finished + ":" + script;
		}
	}

	ArrayList<ScriptDetail> scriptsResults = new ArrayList<ScriptDetail>();

	public ArrayList<ScriptDetail> getScriptsResults() {
		return scriptsResults;
	}

	protected void clearScripts() {
		ScriptDetail last = null;
		for (ScriptDetail s : scriptsResults) {
			last = s;
		}
		scriptsResults.clear();
		if (last != null) {
			scriptsResults.add(last);
		}
	}

	public Object evaluate(String s) {
		Closure<?> cl = (Closure<?>) executor.evaluate("{->" + s + "}");
		cl.setDelegate(this);
		cl.setResolveStrategy(Closure.DELEGATE_FIRST);
		ScriptDetail det = null;
		if (!(null == s || "availableVMs" == s || s.length() == 0)) {
			det = new ScriptDetail();
			det.start = System.currentTimeMillis();
			det.script = s;
			scriptsResults.add(det);
		}
		Object o = cl.call();
		if (det != null) {
			det.res = o;
			det.finished = System.currentTimeMillis();
		}
		return o;
	}

	@Override
	public BasicVMRegistar after(RegisteredWorker... workers) {
		final Semaphore waiting = new Semaphore(0);
		Runnable callback = new Runnable() {

			@Override
			public void run() {
				waiting.release();
			}
		};
		for (final RegisteredWorker worker : workers) {
			worker.callAfter(callback);
		}
		try {
			waiting.acquire(workers.length);
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
		logger.debug("finished sync over " + Arrays.asList(workers));
		return this;
	}

	@Override
	public BasicVMRegistar after(List<RegisteredWorker> workers) {
		after(workers.toArray(new RegisteredVM[]{}));
		return this;
	}

	@Override
	public BasicVMRegistar after(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			logger.debug("while sleeping for " + seconds + "s", e);
		}
		return this;
	}

	@Override
	public SyncedExecutor delay(RegisteredWorker... sync) {
		return new SyncedExecutor(this, sync);
	}

	public void after(Closure<?> cl, RegisteredWorker... sync) {
		new SyncedExecutor(this, sync).call(cl);
	}

	protected ArrayList<Work> works = new ArrayList<Work>();

	@Override
	public WorkList works(RegisteredWorker... workers) {
		return new WorkList(works, workers);
	}

	@Override
	public void addWork(Work w) {
		synchronized (works) {
			works.add(w);
		}
	}

	protected ArrayList<Load> loads = new ArrayList<Load>();

	@Override
	public LoadList loads(RegisteredWorker... workers) {
		return new LoadList(loads, workers);
	}

	@Override
	public void addLoad(Load presentLoad) {
		synchronized (loads) {
			loads.add(presentLoad);
		}
	}

	LoadBase loadBase(long duration, double baseMultiplier) {
		return new LoadBase(duration, baseMultiplier);
	}
}
