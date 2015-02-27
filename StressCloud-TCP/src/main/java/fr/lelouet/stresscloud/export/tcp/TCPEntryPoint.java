package fr.lelouet.stresscloud.export.tcp;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import fr.lelouet.stresscloud.HttpExecutor;
import fr.lelouet.stresscloud.RestExecutor;
import fr.lelouet.stresscloud.control.BasicVMRegistar;
import fr.lelouet.stresscloud.export.AEntryPoint;
import fr.lelouet.stresscloud.export.AExporter;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 * 
 */
public class TCPEntryPoint extends AEntryPoint implements Runnable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TCPEntryPoint.class);
	ServerSocket listeningSocket = null;

	/**
	 * creates a TCP entry point and start it in a new thread.
	 * 
	 * @param port
	 *            the port to accept incoming VM registrations on.
	 */
	public TCPEntryPoint(int port) {
		try {
			listeningSocket = new ServerSocket();
			listeningSocket.bind(new InetSocketAddress(port));
		} catch (IOException e) {
			logger.warn("while opening socket to listen on port " + port, e);
		}
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				logger.debug("waiting for incomming VMs connexions");
				Socket vmSocket = listeningSocket.accept();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(vmSocket.getInputStream()));
				PrintWriter writer = new PrintWriter(
						vmSocket.getOutputStream(), true);
				String line = reader.readLine();
				if (!AExporter.REG.equals(line)) {
					logger.debug("dicarding line : " + line);
				}
				TCPRegisteredVM ret = new TCPRegisteredVM();
				ret.id = nextVMid();
				ret.setIO(reader, writer);
				ret.setEntryPoint(this);
				new Thread(ret).start();
			} catch (IOException e) {
				logger.warn("", e);
			}
		}
	}

	public static final String REGISTAR_PORT_ARG = "registar";
	public static final String CONTROL_PORT_ARG = "control";

	public static final int DEFAULT_REG_PORT = 8080;
	public static final int DEFAULT_EXEC_PORT = 8090;

	public static final String MAIN_HELP = "start a registart with this entryy point and a controller. args :\n "
			+ REGISTAR_PORT_ARG
			+ "=X to change the entry point port to X (the port the registered VMs connect to)\n "
			+ CONTROL_PORT_ARG
			+ "=Y to change the controller port to Y(the port the client connects in http to)";

	/**
	 * @see #MAIN_HELP
	 * @param args
	 */
	public static void main(String[] args) {
		int regPort = DEFAULT_REG_PORT;
		int execPort = DEFAULT_EXEC_PORT;
		for (String arg : args) {
			if ("help".equals(arg)) {
				System.out.println(MAIN_HELP);
				return;
			} else if (arg.startsWith(REGISTAR_PORT_ARG + "=")) {
				regPort = Integer.parseInt(arg.substring(REGISTAR_PORT_ARG
						.length() + 1));
			} else if (arg.startsWith(CONTROL_PORT_ARG + "=")) {
				execPort = Integer.parseInt(arg.substring(CONTROL_PORT_ARG
						.length() + 1));
			} else {
				System.err.println("what to do of arg " + arg + " ?");
			}
		}
		RestExecutor executor = new RestExecutor();
		executor.port = execPort;
		executor.publish();
		TCPEntryPoint entry = new TCPEntryPoint(regPort);
		logger.info("listening for VMs reg on port " + regPort
				+ ", http client on port " + execPort);
		BasicVMRegistar reg = new BasicVMRegistar();
		entry.setVMListener(reg);
		executor.setRegistar(reg);
		HttpExecutor h = new HttpExecutor();
		h.setRegistar(reg);
		h.publish();
	}
}
