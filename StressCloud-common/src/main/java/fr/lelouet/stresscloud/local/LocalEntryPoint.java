package fr.lelouet.stresscloud.local;

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

import fr.lelouet.stresscloud.ControlledStresser;
import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.export.AEntryPoint;
import fr.lelouet.stresscloud.export.RegistarEntryPoint;

/**
 * local {@link RegistarEntryPoint} that is programmed to have given
 * virtualMachines. It helps debugging the network layer.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class LocalEntryPoint extends AEntryPoint {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LocalEntryPoint.class);

	public LocalRegisteredVM addVM(final Stresser... stressers) {
		return startExportVM(new LocalExporter(), stressers);
	}

	/**
	 * creates and adds a local VM with empty stressers inside.
	 * 
	 * @param ip
	 *            the p of the vm (if null, set to 127.0.0.1)
	 * @param cores
	 *            number of cores of the vm
	 * @param mem
	 *            size of the available RAM of the VM
	 * @return the created {@link LocalRegisteredVM}
	 */
	public LocalRegisteredVM addVM(String ip, int cores, long mem) {
		LocalExporter ex = new LocalExporter();
		ex.setIp(ip);
		ex.setCores(cores);
		ex.setMem(mem);
		return startExportVM(ex, new ControlledStresser(),
				new ControlledStresser(), new ControlledStresser());
	}

	protected LocalRegisteredVM startExportVM(final LocalExporter ex,
			final Stresser... stressers) {
		LocalRegisteredVM ret = new LocalRegisteredVM();
		ret.id = nextVMid();
		ret.setEntryPoint(this);
		ex.getRegistarCommands().add("" + ret.id);
		ret.setExporter(ex);
		new Thread(ret).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				ex.exportVM(stressers);
			}
		}).start();
		while (ret.getIp() == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
		}
		return ret;
	}
}
