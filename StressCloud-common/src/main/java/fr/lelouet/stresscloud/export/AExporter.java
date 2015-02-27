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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lelouet.stresscloud.Stresser;
import fr.lelouet.stresscloud.commands.ACommand;
import fr.lelouet.stresscloud.commands.Command;

/**
 * main exporting tools.<br />
 * Some values to export are the {@link #ip}, the number of CPU {@link #cores},
 * the available {@link #mem}ory. If those fields are not specified, they are
 * guessed using {@link #getHostIpIfconfig()}, {@link #getNbCores()} and
 * {@link #getInstalledMemorySize()}
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public abstract class AExporter implements VMExporter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(AExporter.class);

	/** ask to register */
	public static final String REG = "REG";

	/** ask next command */
	public static final String COM = "COM";

	/**
	 * serialize a request for the next command of one VM id, to send to the
	 * registar
	 */
	public static String serializeComm(long vmId) {
		return COM + " " + vmId;
	}

	protected static Pattern PCOM = Pattern.compile(COM + " (\\d+)");

	/** unserialize the id from a command string */
	public static long unserializeComm(String serial) {
		Matcher m = PCOM.matcher(serial);
		if (!m.matches()) {
			return -1;
		}
		return Long.parseLong(m.group(1));
	}

	/** store information */
	public static final String STO = "STO";

	public static String serializeStore(String key, String value) {
		return STO + " " + key + "=" + value;
	}

	protected static Pattern PSTO = Pattern.compile(STO + " (.*?)=(.*)");

	public static String[] unserializeStore(String serial) {
		Matcher m = PSTO.matcher(serial);
		if (!m.matches()) {
			return null;
		}
		String[] ret = new String[]{m.group(1), m.group(2)};
		return ret;
	}

	/** answer to a command */
	public static final String ANS = "ANS";

	/**
	 * transform a response to a String, to send to the registar
	 * 
	 * @param commandId
	 *            the id of the command we want to response to
	 * @param value
	 *            the value of the response, or null
	 * @return the String to send to the server.
	 */
	public static String serializeAns(long commandId, String value) {
		return ANS + " " + commandId + "=" + value;
	}

	protected static Pattern PANS = Pattern.compile(ANS + " (\\d+)=(.*)");

	/**
	 * unserializes a String to the args of an answer
	 * 
	 * @param serial
	 *            the received serialized answer
	 * @return { commandId, result } or null if incorrect.
	 */
	public static String[] unserializeAns(String serial) {
		Matcher m = PANS.matcher(serial);
		if (!m.matches()) {
			return null;
		}
		return new String[]{m.group(1), m.group(2)};
	}

	/**
	 * generate an IP from an ID<br />
	 * 256*256*5 + 256*8 + 12 => 0.5.8.12
	 */
	public static String makeIP(long id) {
		return new StringBuilder().append(id >> 8 * 3 & 255).append('.')
				.append(id >> 8 * 2 & 255).append('.').append(id >> 8 & 255)
				.append('.').append(id & 255).toString();
	}

	protected long id = -1;

	protected int cores = -1;

	protected long mem = -1;

	protected String ip = null;

	/** @return the id */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/** @return the cores */
	public int getCores() {
		return cores;
	}

	/**
	 * @param cores
	 *            the cores to set
	 */
	public void setCores(int cores) {
		this.cores = cores;
	}

	/** @return the mem */
	public long getMem() {
		return mem;
	}

	/**
	 * @param mem
	 *            the mem to set
	 */
	public void setMem(long mem) {
		this.mem = mem;
	}

	/** @return the ip */
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip
	 *            the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	protected HashMap<String, Stresser> stressers = new HashMap<String, Stresser>();

	@Override
	public Stresser getStresser(String type) {
		// System.err.println("getting stresser of type " + type + " among "
		// + stressers);
		return stressers.get(type);
	}

	@Override
	public Set<String> getStressersTypes() {
		return stressers.keySet();
	}

	/**
	 * send data to the distant registeredVM.
	 * 
	 * @param s
	 *            the data to send
	 */
	protected abstract void send(String s);

	/** get next String from distant registeredVM, or null if connexion closed */
	protected abstract String readNext();

	@Override
	public void sendResponse(String response, long commandId) {
		logger.debug("sending ans : " + response + " to command id "
				+ commandId);
		String serial = serializeAns(commandId, response);
		send(serial);
	}

	@Override
	public Command acquireNextCommand() {
		String s = null;
		logger.trace("vm" + id + ".readNext()");
		s = readNext();
		Command ret = ACommand.parse(s);
		logger.debug("vm" + id + ".read()=" + s + "; parse()=" + ret);
		return ret;
	}

	protected void guessMissingParameters() {
		if (ip == null) {
			ip = guessIp();
		}
		if (cores == -1) {
			cores = getNbCores();
		}
		if (mem == -1) {
			mem = getInstalledMemorySize();
		}
	}

	/**
	 * guess the ip of the VM. To override when using different protocols.<br />
	 * eg. when you have a target IP, you can use
	 * {@link #getHostIpConnect(String, int)}
	 */
	protected String guessIp() {
		String ret = getHostIpEnumerate();
		if (ret == null) {
			ret = getHostIpIfconfig();
		}
		return ret;
	}

	/**
	 * Start and export three stressers as the local VM to a distant registar.
	 * also send correct VM informations, such as number of cores, available
	 * ram, IP address.
	 * 
	 * @param exp
	 *            the exporter
	 * @param cpu
	 *            the stresser of CPU
	 * @param disk
	 *            the stresser of the disk
	 * @param net
	 *            the network stresser.
	 */
	@Override
	public void exportVM(Stresser... mstressers) {
		send(REG);
		id = Long.parseLong(readNext());
		String typesarg = null;
		for (Stresser s : mstressers) {
			if (s == null) {
				continue;
			}
			String type = s.getType();
			if (!stressers.containsKey(type)) {
				stressers.put(type, s);
				typesarg = (typesarg == null ? "" : typesarg + ",") + type;
				s.start();
			}
		}
		guessMissingParameters();
		// System.err.println("sending store types " + typesarg);
		store(TYPES_KEY, typesarg == null ? "" : typesarg);
		store(CORES_KEY, "" + cores);
		store(MEM_KEY, "" + mem);
		store(IP_KEY, ip);
		Command c = null;
		do {
			c = acquireNextCommand();
			final Command c2 = c;
			if (c != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Object ret = c2.apply(AExporter.this);
						sendResponse("" + ret, c2.getId());
					}
				}).start();
			}
		} while (c != null);
		logger.info("export finished, leaving");
		stop();
	}

	/** get the memory available to the machine on linux system. */
	public static long getInstalledMemorySize() {
		Process pr = null;
		try {
			String[] commands = new String[]{"/bin/bash", "-c",
					"cat /proc/meminfo | grep \"MemTotal\" | sed \"s/MemTotal: *//\""};
			pr = Runtime.getRuntime().exec(commands);
			pr.waitFor();
			String ret = new BufferedReader(new InputStreamReader(
					pr.getInputStream())).readLine();
			long mult = 1;
			String val = ret.split(" ")[0];
			String unit = ret.split(" ")[1];
			if ("kB".equals(unit)) {
				mult = 1024;
			}
			return Long.parseLong(val) * mult;
		} catch (Exception e) {
			logger.warn("return -1 free B", e);
			return -1;
		}
	}

	/** get the number of cores available on a linux system. */
	public static int getNbCores() {
		Process pr = null;
		try {
			String[] commands = new String[]{"/bin/bash", "-c",
					"cat /proc/cpuinfo | grep processor | grep \": \" | wc -l"};
			pr = Runtime.getRuntime().exec(commands);
			pr.waitFor();
			String ret = new BufferedReader(new InputStreamReader(
					pr.getInputStream())).readLine();
			String err = new BufferedReader(new InputStreamReader(
					pr.getErrorStream())).readLine();
			if (err != null) {
				logger.debug("error : " + err);
			}
			return Integer.parseInt(ret);
		} catch (Exception e) {
			logger.warn("return -1 core", e);
			return -1;
		}
	}

	public static final String[] netDrivers = {"eth", "wlan"};

	/**
	 * get the first available IP address from "ifconfig proto" on linux
	 * systems, proto being in {@link #netDrivers}
	 */
	public static String getHostIpIfconfig() {
		for (String driver : netDrivers) {
			Process pr = null;
			try {
				String[] commands = new String[]{
						"/bin/bash",
						"-c",
						"ifconfig "
								+ driver
								+ " | grep \"inet\" | sed \"s/^[^:]*://\" | sed \"s/ .*//\""};
				pr = Runtime.getRuntime().exec(commands);
				pr.waitFor();
				String ret = new BufferedReader(new InputStreamReader(
						pr.getInputStream())).readLine();
				String err = new BufferedReader(new InputStreamReader(
						pr.getErrorStream())).readLine();
				if (err != null) {
					logger.debug("error : " + err);
				}
				if (ret != null) {
					return ret;
				}
			} catch (Exception e) {
				logger.warn("while getting ifconfig", e);
				return null;
			}
		}
		return null;
	}

	/** get this host's IP by connecting to a distant server. */
	public static String getHostIpConnect(String host, int port) {
		try {
			Socket s = new Socket(host, port);
			String ret = s.getLocalAddress().getHostAddress();
			s.close();
			return ret;
		} catch (Exception e) {
			logger.warn("", e);
			return null;
		}
	}

	public static String getHostIpLocalhost() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return null;
		}
	}

	public static String getHostIpEnumerate() {
		try {
			Enumeration<NetworkInterface> interfaces;
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface current = interfaces.nextElement();
				try {
					if (!current.isUp() || current.isLoopback()
							|| current.isVirtual()) {
						continue;
					}
				} catch (SocketException e) {
					logger.warn("", e);
					continue;
				}
				Enumeration<InetAddress> addresses = current.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress current_addr = addresses.nextElement();
					if (current_addr.isLoopbackAddress()) {
						continue;
					}
					return current_addr.getHostAddress();
				}
			}
		} catch (SocketException e) {
			logger.warn("", e);
		}
		return null;
	}

	@Override
	public void stop() {
		for (Stresser s : stressers.values()) {
			s.stop();
		}
	}

	@Override
	public void store(String key, String value) {
		send(serializeStore(key, value));
	}
}
