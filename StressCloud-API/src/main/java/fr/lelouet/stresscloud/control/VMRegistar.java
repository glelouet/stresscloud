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

import java.util.Collection;
import java.util.List;

import fr.lelouet.stresscloud.export.RegistarEntryPoint.VMListener;
import groovy.lang.Closure;

/**
 * indexes and filters the {@link RegisteredVM} available.<br />
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface VMRegistar extends VMListener {

	/**
	 * list all the VMs that have not been reserved yet.
	 * 
	 * @return the list of VMs that have not been reserved yet
	 */
	Collection<RegisteredVM> getAvailableVMs();

	/**
	 * reserve and get the first {@link RegisteredVM} that respect the filter
	 * 
	 * @param filter
	 *            a function over a {@link RegisteredVM} that returns true if
	 *            the vm respects the requirements
	 * @return the first available {@link RegisteredVM} that make filter return
	 *         true, or null.
	 */
	RegisteredVM require(Closure<?> filter);

	/**
	 * reserve and get the first {@link RegisteredVM}s that respect the filter
	 * 
	 * @param filter
	 *            a function over a {@link RegisteredVM} that returns true if
	 *            the vm respects the requirements
	 * @param number
	 *            the number of VMs required
	 * @return the first available {@link RegisteredVM} that make filter return
	 *         true, or null.
	 */
	List<RegisteredVM> require(Closure<?> filter, int number);

	/**
	 * reserve and return the first {@link RegisteredVM} available that
	 * correspond to the filter specified in the string.
	 * 
	 * @param filter
	 * @param number
	 * @return the first available {@link RegisteredVM}
	 */
	List<RegisteredVM> require(String filter, int number);

	/**
	 * reserve and return the first {@link RegisteredVM} available that
	 * correspond to the filter specified in the string.
	 * 
	 * @param filter
	 * @param number
	 * @return the first available {@link RegisteredVM}
	 */
	RegisteredVM require(String filter);

	/**
	 * request to reserve a VMStresser
	 * 
	 * @param target
	 *            the VMStresser we want to reserve
	 * @return target or null if it was already reserved
	 */
	RegisteredVM reserve(RegisteredVM target);

	/**
	 * get the {@link RegisteredVM} that have been reserved by this.
	 * 
	 * @return the list of {@link RegisteredVM} that this has reserved.
	 */
	List<RegisteredVM> getReservedVMs();

	/**
	 * blocks until a number of {@link RegisteredVM} is available
	 * 
	 * @param nbVMs
	 *            the number of VMs we want.
	 */
	void need(int nbVMs);

	/**
	 * tell this it is no more used. The reserved VMStressers are all available
	 * to new reserve(), and the stressers are all set back to a load of 0.0.
	 */
	void release();

	/**
	 * blocks until specified VMs finish their work.
	 * 
	 * @param vms
	 *            the {@link RegisteredVM}s we want to wait for their work to be
	 *            finished.
	 */
	VMRegistar after(RegisteredWorker... workers);

	/**
	 * call to sync with a list instead of an array.
	 * 
	 * @param vms
	 * @see #after(RegisteredVM...)
	 */
	VMRegistar after(List<RegisteredWorker> workers);

	/**
	 * make the system sleep for some seconds
	 * 
	 * @param seconds
	 *            the number of seconds to wait.
	 */
	VMRegistar after(long seconds);

	class SyncedExecutor implements Runnable {

		RegisteredWorker[] workers = null;
		VMRegistar reg = null;
		Closure<?> e = null;

		/**
		 *
		 */
		public SyncedExecutor(VMRegistar reg, RegisteredWorker[] workers) {
			this.reg = reg;
			this.workers = workers;
		}

		void call(Closure<?> e) {
			this.e = e;
			new Thread(this).start();
		}

		void plus(Closure<?> e) {
			call(e);
		}

		@Override
		public void run() {
			reg.after(workers);
			e.call();
		}
	}

	/**
	 * @return an executor to execute the following closure after the required
	 *         workers have finished
	 */
	SyncedExecutor delay(RegisteredWorker... sync);

	/**
	 * @return a copy of all the works added since last require()
	 * @param workers
	 *            workers to filter the works on
	 */
	WorkList works(RegisteredWorker... workers);

	/**
	 * @param workers
	 *            workers to filter the loads on
	 * @return a copy of internal loads added corresponding to selected workers
	 */
	LoadList loads(RegisteredWorker... workers);

	/**
	 * @param presentWork
	 *            a work created to add to the list of works
	 */
	void addWork(Work presentWork);

	/**
	 * 
	 * @param presentLoad
	 *            a Load created to add to the list of loads
	 */
	void addLoad(Load presentLoad);

	/** @return the time of the last vm reservation */
	long getScriptStart();

	/** @return the present time in ms since last vm reservation */
	long getTime();

}
