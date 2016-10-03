package fr.lelouet.stresscloud.export.tcp;

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

import java.io.BufferedReader;
import java.io.PrintWriter;

import fr.lelouet.stresscloud.control.BasicRegisteredVM;

/**
 * access to a VM that registered in the entrypoint
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class TCPRegisteredVM extends BasicRegisteredVM implements Runnable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TCPRegisteredVM.class);

	protected BufferedReader reader = null;

	protected PrintWriter writer = null;

	/**
	 * @param vmSocket
	 */
	public void setIO(BufferedReader reader, PrintWriter writer) {
		this.reader = reader;
		this.writer = writer;
		sendData("" + getId());
	}

	@Override
	protected String getNextIncommingString() {
		String exec = null;
		try {
			exec = reader.readLine();
		} catch (Exception e) {
			logger.warn("", e);
		}
		return exec;
	}

	@Override
	public void sendData(String s) {
		writer.write(s + "\n");
		writer.flush();
	}

}
