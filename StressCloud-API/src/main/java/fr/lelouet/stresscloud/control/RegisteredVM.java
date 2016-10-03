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
import java.util.Set;

import fr.lelouet.stresscloud.commands.Command;
import fr.lelouet.stresscloud.tools.DelayingContainer;
import fr.lelouet.tools.containers.BlockingContainer;

/**
 * Access to the different resources access on a Virtual Machine. Stands for a
 * VM that has registered the acces to its resoures
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface RegisteredVM extends RegisteredWorker {

	/**
	 * @return the RAM capacity of the VM, in MB
	 */
	long getMem();

	/**
	 * @return the CPU frequency, in MHz, of a core
	 */
	long getFreq();

	/**
	 * @return the number of cores of the CPU of the VM
	 */
	int getCores();

	/**
	 * get the internal id in the registar.
	 * 
	 * @return
	 */
	long getId();

	/**
	 * @return the ip the VM knows itself as
	 */
	String getIp();

	/** send network data to another stresser */
	void send(RegisteredVM target, double load);

	/** set a VM to rest mode. calls cancel() first. */
	void clear();

	/** cancel any waiting command, and ready for next command */
	void cancel();

	/**
	 * add a command to the list of command to send, and prepares a
	 * {@link BlockingContainer} to put the return value of the command.
	 * 
	 * @param command
	 *            the command to send, its id will be modified.
	 * @return the {@link BlockingContainer} that will be put the return value.
	 */
	DelayingContainer<String> addCommand(Command command);

	/**
	 * release a semaphore after given type of stresser has ended its work. does
	 * not lock the mesage transmission as after() does
	 * 
	 * @return
	 */
	DelayingContainer<String> callAfter(String type, Runnable run);

	/**
	 * delays next synced commands until the given workers have finished their
	 * work.
	 * 
	 * @param workers
	 *            the workers to wait after
	 * @return this.
	 */
	RegisteredVM after(RegisteredWorker... workers);

	/**
	 * order the vm to wait for a given time
	 * 
	 * @param seconds
	 *            the number of seconds to wait after last command and before
	 *            next commande
	 * @return this
	 */
	RegisteredVM after(long seconds);

	/**
	 * order the vm to wait till a given time in the script
	 * 
	 * @param seconds
	 *            the time in seconds the VM should wait before sending next
	 *            activity modifications
	 * @return this
	 */
	RegisteredVM till(long seconds);

	/**
	 * @return the set of types of resources used by the stressers of this VM.
	 *         corresponding stressers can be retrieved using
	 *         {@link #getStresser(String)}
	 */
	Set<String> getTypes();

	/** @return the stresser associated to the given resource type, or null */
	RegisteredStresser getStresser(String stresserType);

	/** get the list of stressers associated to this vm */
	public Collection<RegisteredStresser> getStressers();

	/** @param basicVMRegistar */
	void setRegistar(VMRegistar registar);

	/**
	 * set the time between to pings. Any time the VM returns a value is
	 * considered as a ping result. When a ping is supposed to be done, the VM
	 * will request the error rate and the remaining work of all its stressers.
	 * 
	 * @param seconds
	 *            the seconds between two pings
	 */
	void ping(int seconds);
}
