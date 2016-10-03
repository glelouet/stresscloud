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

import java.util.List;

import fr.lelouet.stresscloud.tools.DelayingContainer;
import groovy.lang.Closure;

/**
 * a stresser in a {@link RegisteredVM}, corresponding to a distant Stresser
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface RegisteredStresser extends RegisteredWorker {

	/**
	 * set a key, value couple as an option of the distant stresser. The call is
	 * asynchronous, that is the value will be set later
	 * 
	 * @param key
	 *            the option name
	 * @param value
	 *            the value to associate to the key.
	 * @return a container to wait for the set to be performed.
	 */
	DelayingContainer<String> set(String key, String value);

	/**
	 * @param load
	 */
	RegisteredVM setLoad(double load);

	/**
	 * @return the load set in the stresser
	 */
	double getLoad();

	/** @return the work remaining in the stresser */
	double getWork();

	/** @return the total ms failed to burn since last load modification */
	long getSkipped();

	/**
	 * @param key
	 * @return the value stored under the required key, or null if not present
	 */
	String get(String key);

	/**
	 * add an amount of work to execute. The load is also set
	 * {@link #setLoad(double)} to 0
	 * 
	 * @param totalLoad
	 *            the total number of work to execute.
	 */
	RegisteredVM add(double totalLoad);

	/**
	 * add a closure to call each time the load is set by the controller. The
	 * closure's "it" will be this stresser
	 * 
	 * @param cl
	 *            the closure to call on stresser load change
	 */
	void onLoadChange(Closure<?> cl);

	/**
	 * get the list of closure to call each time the load of the stresser is
	 * modified by the controller
	 * 
	 * @return the internal list of closures to call upon load modification.
	 *         Modification of that list must be synced on this list.
	 */
	List<Closure<?>> onLoadChange();

	/**
	 * request the vm to wait until the end of this stresser's work specifically
	 * 
	 * @return the {@link RegisteredVM} this belongs to
	 */
	@Override
	RegisteredVM after();

	/**
	 * get the parent of this.
	 * 
	 * @return The registeredVM this belongs to
	 */
	public RegisteredVM getParent();

	/**
	 * get the string description of the type of this stresser.
	 * 
	 * @return the type the distant stresser declares.
	 */
	public String getType();

	/**
	 * require the stresser to retrieve the remainingwork and skipped values if
	 * an activity has been sepcified.
	 * 
	 * @return true if an activity was specified, false if no load and no work.
	 */
	boolean updateActivityResult();

}
