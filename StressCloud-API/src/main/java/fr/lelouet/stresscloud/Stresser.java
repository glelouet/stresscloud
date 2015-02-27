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

import fr.lelouet.stresscloud.control.RegisteredVM;

/**
 * <p>
 * an access to a resource.
 * </p>
 * <p>
 * it can either be used in a main launcher, or controlled by a
 * {@link RegisteredVM}
 * </p>
 * <p>
 * Its {@link #getLoad()} represent the amount of resource it uses per time
 * unit.
 * </p>
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface Stresser {

	/**
	 * the different types of stresser known:
	 * <ul>
	 * <li>The {@link #CPU} uses the processing unit. Its load is expressed in
	 * BOGOMIPS</li>
	 * <li>The {@link #DISK} uses the local hard drive. Its load is expressed in
	 * B to write on it per second.</li>
	 * <li>The {@link #NET} uses the network card(s). Its load is the number of
	 * B to write to a target per second ; plus, it can be set a target to write
	 * data to.</li>
	 * </ul>
	 * 
	 */
	public enum TYPES {
		CPU, NET, DISK;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		};
	}

	/**
	 * set a property
	 * 
	 * @param key
	 *            the property name
	 * @param value
	 *            the value associated to the property
	 * @return the previous value associated to that property name
	 */
	String set(String key, String value);

	/** get a property */
	String get(String key);

	/**
	 * get the list of parameters that are modifiable with {@link #get(String)}
	 * and {@link #set(String, String)},semicolon separated
	 */
	String getParams();

	/** get the meaning of the load, that is, number of unit per second */
	String getLoadUnit();

	/** the basic keys that are available. */
	public static final String AFTER_KEY = "after";
	public static final String CLASS_KEY = "class";
	public static final String LOAD_KEY = "load";
	public static final String LOADUNIT_KEY = "loadunit";
	public static final String PARAMS_KEY = "params";
	public static final String SETWORK_KEY = "setwork";
	public static final String SKIPPED_KEY = "skipped";
	public static final String STATE_KEY = "state";
	public static final String TYPE_KEY = "type";
	public static final String WORK_KEY = "work";

	/**
	 * set the load of the resource to use. The meaning of the value depends on
	 * the implementation.
	 */
	void setLoad(double load);

	/** get resource load */
	double getLoad();

	/** get resource work */
	double getWork();

	/**
	 * add an absolute work to perform. once the work is performed, the load is
	 * set to 0.
	 * 
	 * @param work
	 *            the number of accesses to perform
	 */
	void addWork(double work);

	/**
	 * @return the sum of the time skipped, in ms, since last load affectation,
	 *         due to underburns. Can be used to detect overloads. This is reset
	 *         to 0 whenever the load changes.
	 */
	public long getSkipped();

	/** requires to stop the burn */
	void stop();

	/**
	 * start a new Thread to access the resource ; do nothing if already started
	 */
	public void start();

	/**
	 * @return true if this is already {@link #start() started}, and no call to
	 *         {@link #stop()} has been given since last call to
	 *         {@link #start()}. Needs to be synced to prevent a call to
	 *         {@link #stop()} to modify the internal state.
	 */
	boolean keepsRunning();

	/**
	 * active wait for the {@link #run()} to be in running mode. Note that this
	 * may be in infinite loop if a stop is set between the call to run() and
	 * the internal return of this method. Thus this needs a synchronization.
	 * 
	 * @param timeoutms
	 *            an optional long for the maximum time to wait.
	 * @return true if this has been put in running mode. false if the timeout
	 *         expired or this has been asked to stop before being run.
	 */
	public boolean waitForRunning(long... timeoutms);

	/**
	 * @param onStop
	 *            the object to call on Exit() on when this received a
	 *            {@link #stop()}
	 */
	public void addOnExit(StopHook onStop);

	public static interface StopHook {
		void onExit(Stresser exited) throws Exception;
	}

	/** @return the type of the stresser : CPU, RAM, etc. */
	public String getType();

	/** block untill the work is finished. call it after {@link #addWork(double)} */
	public void after();
}
