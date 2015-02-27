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

import java.util.ArrayList;
import java.util.List;

import fr.lelouet.stresscloud.control.RegisteredVM;

/**
 * Abstract Registar Entry point that stores the commands, and responses.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public abstract class AEntryPoint implements RegistarEntryPoint {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(AEntryPoint.class);

	private VMListener vmlistener = null;

	/** lock on the {@link VMListener} notifications */
	private final Object registrationLock = new Object();

	@Override
	public void setVMListener(VMListener listener) {
		vmlistener = listener;
		if (listener != null) {
			synchronized (registrationLock) {
				logger.debug("resending " + registeredVMs.size()
						+ " vms to the listener : " + listener);
				for (RegisteredVM vm : registeredVMs) {
					listener.newVM(vm);
				}
			}
		}
	}

	public VMListener getVMListener() {
		return vmlistener;
	}

	/** force to send the VM to registered Listener */
	@Override
	public void onNewVM(RegisteredVM vm) {
		logger.info("new vm : " + vm);
		synchronized (registrationLock) {
			registeredVMs.add(vm);
			if (vmlistener != null) {
				vmlistener.newVM(vm);
			}
		}
	}

	List<RegisteredVM> registeredVMs = new ArrayList<RegisteredVM>();

	@Override
	public List<RegisteredVM> getRegisteredVMsList() {
		synchronized (registrationLock) {
			return new ArrayList<RegisteredVM>(registeredVMs);
		}
	}

	private long lastVMId = 0;

	public synchronized long nextVMid() {
		lastVMId++;
		return lastVMId;
	}

}
