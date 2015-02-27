package fr.lelouet.stresscloud.commands;

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

import java.io.Serializable;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.Stresser.TYPES;
import fr.lelouet.stresscloud.export.VMExporter;

/**
 * a command to send to a VM
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface Command extends Serializable {

	/**
	 * apply the command to a {@link Stresser}
	 * 
	 * @param target
	 *            the {@link Stresser} to apply the command to.
	 */
	Object apply(VMExporter target);

	/**
	 * the type this should apply to. null means the VM or all stressers
	 * 
	 * @return the {@link Stresser} of the VM wich should be applied the
	 *         command.
	 */
	String getTypeTarget();

	/**
	 * @param t
	 *            the type of resource this stresser uses.
	 * @see {@link TYPES}
	 */
	void setTypeTarget(String t);

	/**
	 * @return the id of the command
	 */
	long getId();

	/**
	 * @param id
	 *            the id of the command to set.
	 */
	void setId(long id);

	/**
	 * get the synchronization requirement of the command. Most commands should
	 * be synced to prevent commands interlacement.
	 * 
	 * @return true if the command should be sent after the last synced command
	 *         added has returned.
	 */
	boolean synced();

	/**
	 * specifies wether next command should wait for this command to return
	 * before being sent. Most commands will specify true
	 */
	boolean blocking();

}
