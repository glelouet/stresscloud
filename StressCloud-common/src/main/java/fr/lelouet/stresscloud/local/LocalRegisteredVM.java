package fr.lelouet.stresscloud.local;

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

import fr.lelouet.stresscloud.control.BasicRegisteredVM;
import fr.lelouet.stresscloud.control.VMRegistar;
import fr.lelouet.stresscloud.export.VMExporter;

/**
 * VM registered on the same program that the {@link VMRegistar}<br />
 * Not quite developed : does not buffer the commands.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class LocalRegisteredVM extends BasicRegisteredVM {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LocalRegisteredVM.class);

	/** @return the internal local exporter to delegate calls */
	public LocalExporter getExporter() {
		return ex;
	}

	/**
	 * @param ex
	 *            the internal local exporter to delegate the calls
	 */
	public void setExporter(LocalExporter ex) {
		this.ex = ex;
	}

	LocalExporter ex = null;

	/**
	 * directly send a String to the {@link VMExporter}
	 * 
	 * @param string
	 *            the data to send to the distant exporter
	 */
	@Override
	public void sendData(String s) {
		// System.err.println("acquiring lock to write to exporter");
		synchronized (ex.registarCommands) {
			ex.registarCommands.add(s);
			ex.registarCommands.notifyAll();
		}
		// System.err.println("wrote " + s + " to exporter");
	}

	/** @return the incoming data from the distant exporter */
	@Override
	protected String getNextIncommingString() {
		// System.err.println("acquiring lock to read from exporter");
		synchronized (ex.exporterCommands) {
			while (ex.exporterCommands.size() < 1) {
				try {
					ex.exporterCommands.wait();
				} catch (InterruptedException e) {
					logger.warn("", e);
				}
			}
			String ret = ex.exporterCommands.remove(0);
			// System.err.println("acquired command from exporter " + ret);
			return ret;
		}
	}

}
