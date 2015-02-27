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

import java.util.LinkedList;
import java.util.List;

import fr.lelouet.stresscloud.export.AExporter;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class LocalExporter extends AExporter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(LocalExporter.class);

	public List<String> exporterCommands = new LinkedList<String>();
	public List<String> registarCommands = new LinkedList<String>();

	/** @return the exporterCommands */
	public List<String> getExporterCommands() {
		return exporterCommands;
	}

	/**
	 * @param exporterCommands
	 *            the exporterCommands to set
	 */
	public void setExporterCommands(List<String> exporterCommands) {
		this.exporterCommands = exporterCommands;
	}

	/** @return the registarCommands */
	public List<String> getRegistarCommands() {
		return registarCommands;
	}

	/**
	 * @param registarCommands
	 *            the registarCommands to set
	 */
	public void setRegistarCommands(List<String> registarCommands) {
		this.registarCommands = registarCommands;
	}

	public LocalExporter() {
	}

	@Override
	protected void send(String s) {
		// System.err.println("acquiring lock to write to registar");
		synchronized (exporterCommands) {
			exporterCommands.add(s);
			exporterCommands.notifyAll();
		}
		// System.err.println("wrote " + s + " to registar");
	}

	@Override
	protected String readNext() {
		// System.err.println("acquiring lock to read from registar");
		synchronized (registarCommands) {
			while (registarCommands.size() < 1) {
				try {
					registarCommands.wait();
				} catch (InterruptedException e) {
					logger.warn("", e);
				}
			}
			String ret = registarCommands.remove(0);
			// System.err.println("acquired command from registar" + ret);
			return ret;
		}
	}

}
