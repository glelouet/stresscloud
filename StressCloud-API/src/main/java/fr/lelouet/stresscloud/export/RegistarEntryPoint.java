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

import java.util.ConcurrentModificationException;
import java.util.List;

import fr.lelouet.stresscloud.control.RegisteredVM;
import fr.lelouet.stresscloud.control.VMRegistar;

/**
 * <p>
 * an entry point is responsible for :
 * <ol>
 * <li>receiving incoming registering request, and creating associated
 * {@link RegisteredVM}</li>
 * <li>send the commands back to the registered VMs</li>
 * <li>wait for returned values from the commands.</li>
 * </ol>
 * </p>
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public interface RegistarEntryPoint {

	/** To be called when a new {@link RegisteredVM} has been registered */
	public static interface VMListener {

		void newVM(RegisteredVM vm);

	}

	/**
	 * set the listener which will be notified about new incoming VMs and notify
	 * it of all the already registered VMs
	 * 
	 * @param listener
	 *            the listener to send VMs to.
	 */
	void setVMListener(VMListener listener);

	/**
	 * list the {@link RegisteredVM} already registered
	 * 
	 * @return a copy of the already {@link RegisteredVM} . will not be updated
	 *         due to {@link ConcurrentModificationException}
	 */
	List<RegisteredVM> getRegisteredVMsList();

	/**
	 * to call by a vm when it received its full informations and should be
	 * added to the {@link VMRegistar}s
	 */
	void onNewVM(RegisteredVM vm);

}
