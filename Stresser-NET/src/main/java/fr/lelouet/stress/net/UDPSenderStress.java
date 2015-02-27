package fr.lelouet.stress.net;

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

import java.net.*;
import java.util.Collections;

import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;

import fr.lelouet.stresscloud.BurstStress;

/**
 * Send data over UDP. The load is the average number of Byte to send per
 * second.
 * <p>
 * this implementation has two byte[] buffer to write, according to the size of
 * the data so send on each iteration:<br />
 * <ol>
 * <li>size&le; {@link #DATAGRAM_HEADER_SIZE} : we store the data in a remaining
 * field, when the remaining is &ge; {@link #DATAGRAM_HEADER_SIZE} we write an
 * empty datagram and reduce remaing from its size.</li>
 * <li>{@link #DATAGRAM_HEADER_SIZE}&le; size &le;
 * {@link #MAX_DATAGRAM_DATASIZE}+{@link #DATAGRAM_HEADER_SIZE} : we create a
 * {@link #smallBuffer} of size <it>size-{@link #DATAGRAM_HEADER_SIZE}</i> and
 * write it each time</li>
 * <li>{@link #MAX_DATAGRAM_DATASIZE}+{@link #DATAGRAM_HEADER_SIZE}&lt; size :
 * we write N times the biggest datagram available, then we have a remaining
 * value : we falll back to one of the two case above.</li>
 * </ol>
 * </p>
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr > Le Louët <
 *         guillaume.le-louet@mines-nantes.fr >
 */
public class UDPSenderStress extends BurstStress {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(UDPSenderStress.class);

	class UDPUPMonitor extends SystemMonitor {

		@Override
		public String toString() {
			return itfName + "_Tx";
		}

		private final Sigar sigar = new Sigar();
		private final String itfName = findAdequateInterface().getName();

		{
			mult = 1.0 / 1024;
		}

		@Override
		protected double getActivityData() {
			try {
				double ret = 0;
				NetInterfaceStat stat = sigar.getNetInterfaceStat(itfName);
				ret += stat.getTxBytes();
				return ret;
			} catch (Exception e) {
				logger.warn("", e);
			}
			return -1;
		}

	}

	@Override
	public SystemMonitor getMonitor() {
		return new UDPUPMonitor();
	}

	/**
	 * find the best network interface. criterion are :
	 * <ul>
	 * <li>up:4 pts</li>
	 * <li>not virtual : 2 point</li>
	 * <li>not loopback: 1 points</li>
	 * </ul>
	 * 
	 * @return
	 */
	public static NetworkInterface findAdequateInterface() {
		NetworkInterface best = null;
		int bestscore = 0;
		try {
			for (NetworkInterface nic : Collections.list(NetworkInterface
					.getNetworkInterfaces())) {
				int score = (nic.isUp() ? 4 : 0) + (nic.isVirtual() ? 0 : 2)
						+ (nic.isLoopback() ? 0 : 1);
				if (score > bestscore) {
					bestscore = score;
					best = nic;
				}
			}
		} catch (SocketException e) {
			logger.warn("", e);
		}
		return best;
	}

	public static InterfaceAddress findAdequateAddress(NetworkInterface nic) {
		InterfaceAddress best = null;
		int bestscore = 0;
		for (InterfaceAddress address : nic.getInterfaceAddresses()) {
			int score = 100 - address.getNetworkPrefixLength();
			if (score > bestscore) {
				best = address;
				bestscore = score;
			}
		}
		return best;
	}

	/**
	 * copy bytes in a newly created array, keeping only the mask first bits of
	 * bytes and putting the remaining bits to 0
	 */
	static byte[] maskSubAddress(byte[] bytes, int mask) {
		byte[] ret = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			if (mask <= 0) {
				ret[i] = 0;
			} else {
				int byteMask = 0xff00 >> (mask > 8 ? 8 : mask) & 0xff;
				ret[i] = (byte) (byteMask & bytes[i]);
				mask -= 8;
			}
		}
		return ret;
	}

	/**
	 * finds the network IP address of the interface address with the best
	 * probability to be good
	 */
	public static InetAddress findAdequateTarget() {
		InterfaceAddress addr = findAdequateAddress(findAdequateInterface());
		byte[] bytes = maskSubAddress(addr.getAddress().getAddress(),
				addr.getNetworkPrefixLength());
		try {
			return InetAddress.getByAddress(bytes);
		} catch (UnknownHostException e) {
			logger.warn("", e);
		}
		return null;
	}

	private String targetURI = findAdequateTarget().getHostAddress();
	InetAddress address = null;

	private int targetPort = 8080;

	public String getTargetURI() {
		return targetURI;
	}

	public String setTargetURI(String targetURI) {
		String old = this.targetURI;
		String[] split = targetURI.split(":");
		this.targetURI = split[0];
		if (split.length > 1) {
			targetPort = Integer.parseInt(split[1]);
		}
		address = null;
		return old;
	}

	public int getTargetPort() {
		return targetPort;
	}

	public int setTargetPort(int targetPort) {
		int old = this.targetPort;
		this.targetPort = targetPort;
		address = null;
		return old;
	}

	@Override
	public String getType() {
		return TYPES.NET.toString();
	}

	DatagramSocket dsocket;

	/** a datagram header adds 20 Bytes to its data. */
	public static final int DATAGRAM_HEADER_SIZE = 44;

	/** max size in bytes of the data of an UDP datagram */
	public static final int MAX_DATAGRAM_DATASIZE = 65507;
	private static byte[] MAXDATAGRAM;
	static {
		MAXDATAGRAM = new byte[MAX_DATAGRAM_DATASIZE];
		for (int i = 0; i < MAXDATAGRAM.length; i++) {
			MAXDATAGRAM[i] = (byte) (i % 2 == 0 ? 'a' : 'b');
		}
	}

	{
		setLoopMS(200);
	}

	@Override
	protected double makeAtomicStress(double kB) {
		double packetBytes = kB * 1024;
		if (packetBytes < DATAGRAM_HEADER_SIZE) {
			return 0;
		}
		/** copy it to prevent concurrent modification */
		DatagramSocket dsocket = this.dsocket;
		double remaining = packetBytes;
		try {
			if (dsocket == null) {
				this.dsocket = new DatagramSocket();
				dsocket = this.dsocket;
			}
			if (address == null) {
				address = InetAddress.getByName(targetURI);
			}
		} catch (Exception e) {
			logger.warn("", e);
			return kB;
		}
		try {
			while (remaining >= DATAGRAM_HEADER_SIZE) {
				int size = (int) (remaining - DATAGRAM_HEADER_SIZE);
				if (size > MAX_DATAGRAM_DATASIZE) {
					size = MAX_DATAGRAM_DATASIZE;
				}
				DatagramPacket packet = new DatagramPacket(MAXDATAGRAM, size,
						address, targetPort);
				dsocket.send(packet);
				remaining -= size + DATAGRAM_HEADER_SIZE;
			}
		} catch (Exception e) {
			logger.warn("", e);
		}
		return (packetBytes - remaining) / 1024;
	}

	public static final String TARGETURI_KEY = "target";

	public static final String TARGETPORT_KEY = "port";

	@Override
	public String set(String key, String value) {
		if (TARGETURI_KEY.equals(key)) {
			return setTargetURI(value);
		} else if (TARGETPORT_KEY.equals(key)) {
			return "" + setTargetPort(Integer.parseInt(value));
		}
		return super.set(key, value);
	}

	@Override
	public String get(String key) {
		if (TARGETURI_KEY.equals(key)) {
			return getTargetURI();
		} else if (TARGETPORT_KEY.equals(key)) {
			return "" + getTargetPort();
		}
		return super.get(key);
	}

	@Override
	public String getLoadUnit() {
		return "kB to send on UPD per second";
	}

	@Override
	public String getParams() {
		return super.getParams() + ";" + TARGETPORT_KEY + ";" + TARGETURI_KEY;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (dsocket != null && !dsocket.isClosed()) {
			dsocket.close();
		}
	}

}
