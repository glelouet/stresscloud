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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import fr.lelouet.stress.cpu.CPUMIPSStress;
import fr.lelouet.stress.disk.DiskBitWritter;
import fr.lelouet.stress.net.UDPSenderStress;
import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.export.AExporter;

/**
 * exports a VM's resources over the TCP network. When the socket is closed, the
 * VM is stopped from registering.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class TCPExporter extends AExporter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TCPExporter.class);

	protected Socket socket = null;
	protected BufferedReader reader;
	protected String host;
	protected int port;

	protected long id = -1;

	public TCPExporter(String host, int port) {
		this.host = host;
		this.port = port;
	}

	protected Stresser cpu, disk, net;

	@Override
	protected void send(String s) {
		try {
			socket.getOutputStream().write((s + "\n").getBytes());
		} catch (IOException e) {
			logger.warn("could not send " + s, e);
		}
	}

	@Override
	protected String readNext() {
		try {
			return reader.readLine();
		} catch (IOException e) {
			logger.warn("", e);
			return null;
		}
	}

	@Override
	public void exportVM(Stresser... stressers) {
		socket = null;
		do {
			try {
				socket = new Socket(host, port);
				reader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
			} catch (Exception e) {
				logger.info("could not connect to host=" + host + ", port="
						+ port + " ; retrying in 5 s");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					logger.warn("while waiting to get the host", e);
				}
			}
		} while (socket == null);
		super.exportVM(stressers);
	}

	@Override
	public void stop() {
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			logger.warn("", e);
		}
		super.stop();
	}

	@Override
	protected String guessIp() {
		if (socket != null && socket.getLocalAddress() != null) {
			return socket.getLocalAddress().getHostAddress();
		}
		return AExporter.getHostIpConnect(host, port);
	}

	public static void main(String[] args) {
		if (args == null || args.length < 1) {
			printMainHelp();
			System.exit(0);
		}
		String address = args[0];
		String ip = null;
		int port = 8080;
		for (String s : args) {
			if (s.startsWith("--target=")) {
				address = s.substring("--target=".length());
			} else if (s.startsWith("--IP=")) {
				ip = s.substring("--IP=".length());
			} else if (s.startsWith("--port=")) {
				port = Integer.parseInt(s.substring("--port=".length()));
			} else {
				String[] split = s.split(":");
				address = split[0];
				if (split.length > 1) {
					port = Integer.parseInt(split[1]);
				}
			}
		}
		logger.info("connecting to registar : " + address + ":" + port);
		TCPExporter ex = new TCPExporter(address, port);
		ex.setIp(ip);
		ex.exportVM(new CPUMIPSStress(), new DiskBitWritter(),
				new UDPSenderStress());
		System.exit(0);
	}

	public static void printMainHelp() {
		System.out
				.println("args=TARGET:PORT [--IP=localIP] [--port=targetPort] [--target=targetIP]\n connect a stresser to the distant registar, to stress local resources");
	}
}
