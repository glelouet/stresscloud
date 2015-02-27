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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import fr.lelouet.stresscloud.control.RegisteredVM;
import fr.lelouet.stresscloud.export.RegistarEntryPoint.VMListener;

/**
 * basic operations on an set of VMs that are provided by an {@link EntityPoint}
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class BasicVMListener implements VMListener {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(BasicVMListener.class);

	protected List<RegisteredVM> vms = new ArrayList<RegisteredVM>();

	@Override
	public void newVM(RegisteredVM vm) {
		vms.add(vm);
		checkWaiters();
	}

	protected void checkWaiters() {
		int nb = vms.size();
		synchronized (WaitersLock) {
			List<Semaphore> todelete = new ArrayList<Semaphore>();
			for (Entry<Semaphore, Integer> e : waitersSems.entrySet()) {
				if (e.getValue() <= nb) {
					todelete.add(e.getKey());
				}
			}
			for (Semaphore s : todelete) {
				waitersSems.remove(s);
				s.release();
			}
		}
	}

	private Object WaitersLock = new Object();

	private Map<Semaphore, Integer> waitersSems = new HashMap<Semaphore, Integer>();

	public void need(int nbVMs) {
		Semaphore sem = new Semaphore(0);
		synchronized (WaitersLock) {
			waitersSems.put(sem, nbVMs);
		}
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			throw new UnsupportedOperationException(e);
		}
	}
}
