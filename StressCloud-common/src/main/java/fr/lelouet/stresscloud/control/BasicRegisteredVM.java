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

import static fr.lelouet.stresscloud.export.AExporter.ANS;
import static fr.lelouet.stresscloud.export.AExporter.STO;
import static fr.lelouet.stresscloud.export.AExporter.unserializeAns;
import static fr.lelouet.stresscloud.export.AExporter.unserializeStore;
import static fr.lelouet.stresscloud.export.VMExporter.CORES_KEY;
import static fr.lelouet.stresscloud.export.VMExporter.FREQ_KEY;
import static fr.lelouet.stresscloud.export.VMExporter.IP_KEY;
import static fr.lelouet.stresscloud.export.VMExporter.MEM_KEY;
import static fr.lelouet.stresscloud.export.VMExporter.TYPES_KEY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.actions.Action;
import fr.lelouet.stresscloud.actions.DelayingAcquire;
import fr.lelouet.stresscloud.actions.SendCommand;
import fr.lelouet.stresscloud.commands.Command;
import fr.lelouet.stresscloud.commands.Ping;
import fr.lelouet.stresscloud.commands.Set;
import fr.lelouet.stresscloud.commands.Sync;
import fr.lelouet.stresscloud.export.RegistarEntryPoint;
import fr.lelouet.stresscloud.export.VMExporter;
import fr.lelouet.stresscloud.tools.DelayingContainer;

/**
 * basic bean {@link RegisteredVM}. A VM that has been registered on the
 * {@link VMRegistar}.
 * <p>
 * this is used for test : the {@link #sendData(String)} and
 * {@link #getNextIncommingString()} will throw an exception. you need to
 * override them in order for a subclass to work.
 * </p>
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class BasicRegisteredVM implements RegisteredVM, Runnable {

	static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BasicRegisteredVM.class);

	public BasicRegisteredVM() {
		this(null);
	}

	public BasicRegisteredVM(RegistarEntryPoint reg) {
		super();
		this.reg = reg;
	}

	/**
	 * the registar to call {@link VMRegistar.#newVM(RegisteredVM)} on when full
	 * data has arrived
	 */
	protected RegistarEntryPoint reg = null;

	/**
	 * set the entrypoint to call {@link
	 * RegistarEntryPoint.#onNewVM(RegisteredVM)} when this VM has all its
	 * information ready
	 */
	public void setEntryPoint(RegistarEntryPoint reg) {
		this.reg = reg;
	}

	long mem = 0;

	@Override
	public long getMem() {
		return mem;
	}

	/**
	 * @param mem
	 *            the mem to set
	 */
	public void setMem(long mem) {
		this.mem = mem;
	}

	long coreFreq = 0;

	@Override
	public long getFreq() {
		return coreFreq;
	}

	/**
	 * @param coreFreq
	 *            the coreFreq to set
	 */
	public void setCoreFreq(long coreFreq) {
		this.coreFreq = coreFreq;
	}

	int cores = 1;

	@Override
	public int getCores() {
		return cores;
	}

	/**
	 * @param cores
	 *            the cores to set
	 */
	public void setCores(int cores) {
		this.cores = cores;
	}

	String ip = null;

	@Override
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip
	 *            the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	public long id;

	@Override
	public long getId() {
		return id;
	}

	/**
	 * the waiting commands. modifications should be synced on it, and followed
	 * by a notify() ; the same when a response is received.<br />
	 * possible deadlock, do not sync w/o looking at code.
	 */
	protected ArrayList<Action> awaitingActions = new ArrayList<Action>();

	public boolean hasRemainingActions() {
		return awaitingActions.size() == 0;
	}

	/**
	 * set to an id to prevent from sending commands until specified id response
	 * returns. <br />
	 * access should be synchronized on both {@link #awaitingCommands} and
	 * {@link #awaitingResponses}
	 */
	long waitingResponseId = -1;

	/** tell the VM that a action with given Id has stopped blocking */
	public boolean removeWaitingResponseId(long id) {
		synchronized (awaitingActions) {
			if (waitingResponseId == id) {
				waitingResponseId = -1;
				awaitingActions.notify();
				return true;
			}
		}
		return false;
	}

	/**
	 * the container for each command sent. Once the response has been returned,
	 * the container associated to that response should be discarded.
	 * Modifications should be synchronized on it.
	 */
	protected HashMap<Long, DelayingContainer<String>> awaitingResponses = new HashMap<Long, DelayingContainer<String>>();

	@Override
	public DelayingContainer<String> addCommand(Command command) {
		long commandId = getNextCommandId();
		command.setId(commandId);
		logger.debug("vm" + id + ".add(" + command + "+synced="
				+ command.synced() + ")");
		DelayingContainer<String> ret = new DelayingContainer<String>();
		synchronized (awaitingActions) {
			synchronized (awaitingResponses) {
				awaitingResponses.put(commandId, ret);
			}
			if (command.synced()) {
				awaitingActions.add(new SendCommand(command));
				awaitingActions.notify();
			} else {
				sendData(command.toString());
			}
			return ret;
		}
	}

	/** wait for an action to be put in {@link #awaitingActions} */
	public Action getNextAwaitingAction() {
		synchronized (awaitingActions) {
			while (waitingResponseId > -1 || awaitingActions.size() == 0) {
				try {
					awaitingActions.wait();
				} catch (InterruptedException e) {
					throw new UnsupportedOperationException(e);
				}
			}
			Action c = awaitingActions.remove(0);
			// System.err.println("returning action " + c);
			if (c.syncedId() > -1) {
				synchronized (awaitingResponses) {
					waitingResponseId = c.syncedId();
				}
			}
			return c;
		}
	}

	private long lastCommandId = 0;

	/** get the next id to be used.Each call gets a different value. */
	protected synchronized long getNextCommandId() {
		lastCommandId++;
		return lastCommandId;
	}

	/**
	 * to call when this has received an answer to a command
	 * 
	 * @param commandId
	 *            the id of the command which receives this answer
	 * @param value
	 *            the response to that command.
	 */
	protected void answer(long commandId, String value) {
		synchronized (awaitingActions) {
			synchronized (awaitingResponses) {
				DelayingContainer<String> ctn = awaitingResponses
						.remove(commandId);
				if (ctn != null) {
					ctn.set(value);
					logger.trace("vm" + id + ".anwer(" + commandId + "="
							+ value + ")");
				} else {
					logger.debug("vm" + id + ".ans(" + commandId + "=" + value
							+ "):unregistred command id");
				}
				if (waitingResponseId == commandId) {
					waitingResponseId = -1;
				}
				awaitingActions.notify();
			}
		}
	}

	/**
	 * to call when this received an order to store data
	 * 
	 * @param key
	 *            the name of the property
	 * @param val
	 *            the value to associate to the name.
	 */
	protected void store(String key, String val) {
		try {
			if (CORES_KEY.equals(key)) {
				cores = Integer.parseInt(val);
			} else if (MEM_KEY.equals(key)) {
				mem = Long.parseLong(val);
			} else if (FREQ_KEY.equals(key)) {
				coreFreq = Integer.parseInt(val);
			} else if (IP_KEY.equals(key)) {
				ip = val;
			} else if (TYPES_KEY.equals(key)) {
				// System.err.println("storing types : " + val);
				setStressersTypes(val.split(","));
			} else {
				logger.debug("vm" + id + " discarded store : " + key + "->"
						+ val);
			}
		} catch (Exception e) {
			logger.warn("", e);
		}
	}

	boolean stop = false;

	public void stop() {
		stop = true;
		awaitingActions.notify();
		pinger.interrupt();
	}

	/**
	 * Runnable to wait and send next actions added
	 * 
	 * @author Guillaume Le Louët
	 */
	protected class ActionSender implements Runnable {
		@Override
		public void run() {
			while (!stop) {
				Action c = getNextAwaitingAction();
				logger.debug("vm" + id + ".action(" + c + ")");
				c.apply(BasicRegisteredVM.this);
			}
		}
	}

	protected Thread pinger = null;

	/**
	 * runnable to request ping on a periodic basis
	 * 
	 * @author Guillaume Le Louët
	 */
	protected class Pinger implements Runnable {
		@Override
		public void run() {
			while (!stop) {
				try {
					Thread.sleep(ping());
				} catch (InterruptedException e) {
					// do nothing. we are interrupted each time ping(long)
					// is called
				}
			}
		}
	}

	protected class StringReceiver implements Runnable {
		@Override
		public void run() {
			boolean available = false;
			while (!stop) {
				// logger.debug("running the vm " + getId()
				// + ", waiting for strings from the client");
				String incomming = getNextIncommingString();
				logger.debug("vm" + id + ".rec(" + incomming + ")");
				lastPingTime = System.currentTimeMillis();
				if (incomming == null) {
					logger.info("stopping the VM "
							+ this
							+ " after receiving null string from the distant client");
					return;
				} else if (incomming.startsWith(ANS)) {
					String[] data = unserializeAns(incomming);
					answer(Long.parseLong(data[0]), data[1]);
				} else if (incomming.startsWith(STO)) {
					String[] data = unserializeStore(incomming);
					if (data != null) {
						store(data[0], data[1]);
					} else {
						logger.debug("could not decode store from ["
								+ incomming + "]");
					}
					if (!available && ip != null) {
						available = true;
						if (reg != null) {
							reg.onNewVM(BasicRegisteredVM.this);
						}
					}
				}
			}
		}
	}

	@Override
	public void run() {
		stop = false;
		new Thread(new ActionSender()).start();
		lastPingTime = System.currentTimeMillis();
		pinger = new Thread(new Pinger());
		pinger.start();
		new StringReceiver().run();
	}

	/**
	 * directly send a String to the {@link VMExporter}
	 * 
	 * @param string
	 *            the data to send to the distant exporter
	 */
	public void sendData(String string) {
		throw new UnsupportedOperationException("implement this !");
	}

	/** @return the incoming data from the distant exporter */
	protected String getNextIncommingString() {
		throw new UnsupportedOperationException("implement this !");
	}

	@Override
	public String toString() {
		return "VM{id=" + id + ", ip=" + ip + ", cores=" + cores + "}";
	}

	@Override
	public BasicRegisteredVM after() {
		Sync s = new Sync();
		addCommand(s);
		return this;
	}

	public BasicRegisteredVM after(List<RegisteredWorker> l) {
		return after(l.toArray(new RegisteredWorker[]{}));
	}

	@Override
	public BasicRegisteredVM after(RegisteredWorker... workers) {
		if (workers == null || workers.length == 0) {
			return after();
		}
		final Semaphore lock = new Semaphore(0);
		Runnable run = new Runnable() {

			@Override
			public void run() {
				lock.release();
			}
		};
		for (RegisteredWorker w : workers) {
			w.callAfter(run);
		}
		try {
			lock.acquire(workers.length);
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
		return this;
	}

	@Override
	public void send(RegisteredVM target, double load) {
		RegisteredStresser net = getStresser("net");
		net.set("target", target.getIp());
		net.add(load);
	}

	@Override
	public void clear() {
		cancel();
		addCommand(new Set(Stresser.LOAD_KEY, "0", null));
		for (RegisteredStresser r : presentStressers.values()) {
			r.onLoadChange().clear();
		}
	}

	@Override
	public void cancel() {
		synchronized (awaitingActions) {
			synchronized (awaitingResponses) {
				awaitingActions.clear();
				for (DelayingContainer<String> m : awaitingResponses.values()) {
					m.set(null);
				}
				awaitingResponses.clear();
				if (waitingResponseId > -1) {
					waitingResponseId = -1;
					awaitingActions.notify();
				}
			}
		}
		logger.debug("vm" + id + ".cancel()");
	}

	@Override
	public DelayingContainer<String> callAfter(final String type,
			final Runnable run) {
		Sync s = new Sync(type);
		s.setBlocking(false);
		final DelayingContainer<String> ret = addCommand(s);
		new Thread(new Runnable() {

			@Override
			public void run() {
				ret.get();
				if (run != null) {
					run.run();
				}
				BasicRegisteredVM.this.hasSynced(type);
			}
		}).start();
		return ret;
	}

	/** declares the stressers they have finished their works */
	public void hasSynced(String type) {
		if (type != null) {
			RegisteredStresserImpl stresser = getStresser(type);
			if (stresser != null) {
				stresser.updateWork(null);
			}
		} else {
			for (RegisteredStresserImpl s : presentStressers.values()) {
				s.updateWork(null);
			}
		}
	}

	@Override
	public void callAfter(Runnable run) {
		callAfter(null, run);
	}

	HashMap<String, RegisteredStresserImpl> presentStressers = new HashMap<String, RegisteredStresserImpl>();

	RegisteredStresserImpl propertyMissing(String name) {
		return getStresser(name);
	}

	protected void propertyMissing(String name, Object value) {
		RegisteredStresser stress = getStresser(name);
		if (stress != null) {
			stress.set(Stresser.LOAD_KEY, "" + value);
		}
	}

	protected void setStressersTypes(String... types) {
		presentStressers.clear();
		for (String t : types) {
			// System.err.println("adding stresser of type " + t);
			presentStressers.put(t, new RegisteredStresserImpl(this, t));
		}
	}

	@Override
	public java.util.Set<String> getTypes() {
		return presentStressers.keySet();
	}

	@Override
	public RegisteredStresserImpl getStresser(String stresserType) {
		RegisteredStresserImpl ret = presentStressers.get(stresserType);
		if (ret == null) {
			logger.debug("no stresser of type " + stresserType + ", only "
					+ presentStressers.keySet());
		}
		return ret;
	}

	@Override
	public Collection<RegisteredStresser> getStressers() {
		return new ArrayList<RegisteredStresser>(presentStressers.values());
	}

	private VMRegistar registar = null;

	@Override
	public void setRegistar(VMRegistar reg) {
		registar = reg;
	}

	/** @return the registar this is linked to */
	public VMRegistar getRegistar() {
		return registar;
	}

	@Override
	public RegisteredVM after(long seconds) {
		DelayingAcquire da = new DelayingAcquire(seconds);
		synchronized (awaitingActions) {
			// System.err.println("adding action " + da);
			awaitingActions.add(da);
			awaitingActions.notify();
		}
		return this;
	}

	@Override
	public RegisteredVM till(long seconds) {
		DelayingAcquire da = new DelayingAcquire(seconds).toAbsoluteWait(false);
		synchronized (awaitingActions) {
			awaitingActions.add(da);
		}
		return this;
	}

	/**
	 * request all stressers to retrieve their skipped load and remaining work
	 * values
	 * 
	 * @return true if one stresser at least had an activity to monitor
	 */
	public boolean requestStressersUpdate() {
		boolean success = false;
		for (RegisteredStresser s : presentStressers.values()) {
			if (s.updateActivityResult()) {
				success = true;
			}
		}
		return success;
	}

	/** time in ms of the last retrieved information */
	long lastPingTime = 0;

	/** time between pings */
	int pingdelay = 60;

	@Override
	public void ping(int seconds) {
		pingdelay = seconds;
		if (pinger != null) {
			pinger.interrupt();
		}
	}

	/**
	 * request a ping
	 * 
	 * @return the delay till next ping in ms
	 */
	protected long ping() {
		long time = System.currentTimeMillis();
		if (lastPingTime + pingdelay * 1000 - time <= 0) {
			lastPingTime = time;
			if (!requestStressersUpdate()) {
				addCommand(new Ping());
			}
		}
		long ret = lastPingTime + pingdelay * 1000 - time;
		return ret;
	}
}
