package fr.lelouet.stresscloud.export;

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

import java.util.Set;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.Stresser.TYPES;
import fr.lelouet.stresscloud.commands.Command;

/**
 * exports ONE VM to a distant {@link RegistarEntryPoint}. The network layer.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface VMExporter {

	/**
	 * export a VM to a distant access point.
	 * 
	 * @param stressers
	 *            the stressers to export
	 * @param cores
	 *            the number of cores available
	 * @param freq
	 *            the frequency of a core(should be 0 most of time since there
	 *            is no VM CPU limitation)
	 * @param mem
	 *            the memory allocated to the VM
	 */
	void exportVM(Stresser... stressers);

	/**
	 * retrieve the next command from the distant registar
	 * 
	 * @return null if no command, timeout; or the String of the command.
	 */
	Command acquireNextCommand();

	/**
	 * export a couple key-val to the server.
	 * 
	 * @param key
	 *            the key to export
	 * @param value
	 *            the value associated to the key.
	 */
	void store(String key, String value);

	/**
	 * send a response to the last command acquired with
	 * {@link #acquireNextCommand()}
	 * 
	 * @param response
	 *            the response to the command, or null.
	 */
	void sendResponse(String response, long commandId);

	/**
	 * stop export of any exported of being exported stressers.
	 */
	void stop();

	/** get the associated stresser for a given {@link TYPES}. */
	Stresser getStresser(String type);

	/** @return the list of stressers types present on the VM */
	Set<String> getStressersTypes();

	public static final String CORES_KEY = "cores";
	public static final String MEM_KEY = "mem";
	public static final String FREQ_KEY = "freq";
	public static final String IP_KEY = "ip";
	public static final String TYPES_KEY = "types";

}
