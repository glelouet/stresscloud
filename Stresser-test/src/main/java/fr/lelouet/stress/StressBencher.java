package fr.lelouet.stress;

/* #%L
 * stresscloud
 * %%
 * Copyright (C) 2012 - 2015 Mines de Nantes
 * %%
 * This program
 * is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License
 * for more details. You should have received a copy of the GNU General Lesser
 * Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L% */

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import fr.lelouet.stresscloud.BurstStress;
import fr.lelouet.stresscloud.BurstStress.SystemMonitor;

/**
 * run a stresser with various parameters <br />
 * set its {@link #target} to bench<br />
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class StressBencher {

	public static class UpTimeMonitor {

		long[] watchedThreadsUpTimes = null;
		ThreadMXBean bean = null;

		public void prepareUptimeMXBean() {
			if (bean == null) {
				bean = ManagementFactory.getThreadMXBean();
			}
			long[] watchedThreadsIds = bean.getAllThreadIds();
			long maxThreadId = -1;
			for (long id : watchedThreadsIds) {
				if (id > maxThreadId) {
					maxThreadId = id;
				}
			}
			watchedThreadsUpTimes = new long[(int) (maxThreadId + 1)];
			for (long id : watchedThreadsIds) {
				long up = bean.getThreadCpuTime(id);
				// System.err.println("up of pid " + id + " = " + up);
				watchedThreadsUpTimes[(int) id] = up;
			}
		}

		/**
		 * get the uptime for threads since last {@link #prepareUptimeMXBean()}
		 * 
		 * @return the sumn of the uptimes of the threads since last
		 *         {@link #prepareUptimeMXBean()}, in ns
		 */
		public long getUptimeMXBean() {
			long ret = 0;
			if (bean == null) {
				bean = ManagementFactory.getThreadMXBean();
			}
			long[] watchedThreadsIds = bean.getAllThreadIds();
			for (long id : watchedThreadsIds) {
				long up = bean.getThreadCpuTime(id);
				long idUp = up
						- (id < watchedThreadsUpTimes.length
								? watchedThreadsUpTimes[(int) id]
								: 0);
				ret += idUp;
				// System.err.println("up of pid " + id + " = " + up);
			}
			return ret;
		}
	}

	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(StressBencher.class);

	UpTimeMonitor monitor = new UpTimeMonitor();

	/** runnable called every time we perform a bench observation */
	public SystemMonitor sysloadMonitor = null;

	public BurstStress target = null;

	public OutputStream output = System.err;

	/**
	 * time in ms to wait between the modification of the load and the execution
	 * of monitoring routine
	 */
	public long adaptationPeriod = 10 * 1000;

	/** time in ms to observe the errors durng monitoring */
	public long monitoringPeriod = 5 * 1000;

	void writeln(String s) {
		try {
			output.write((s + "\n").getBytes());
		} catch (IOException e) {
			logger.warn("while writting " + s, e);
		}
	}

	long nanoBeforeMonitor;
	long faultsBefore;
	long upBefore;

	/**
	 * start a CPU Monitoring, so that next {@link #endMonitor()} will consider
	 * the monitor from now on.
	 */
	void startMonitor() {
		nanoBeforeMonitor = System.nanoTime();
		faultsBefore = target.getSkipped();
		monitor.prepareUptimeMXBean();
		if (sysloadMonitor != null) {
			sysloadMonitor.prepareMonitor();
		}
	}

	public long monitoredNanos, monitoredFaults, monitoredUpTime;
	public double monitoredCPULoadPC, monitoredMsFaultPerS, monitoredMaxLoad,
			monitoredSysload;

	/** end of the monitoring period */
	void endMonitor() {
		monitoredFaults = target.getSkipped() - faultsBefore;
		monitoredUpTime = monitor.getUptimeMXBean();
		monitoredNanos = System.nanoTime() - nanoBeforeMonitor;
		monitoredCPULoadPC = 100.0 * monitoredUpTime / monitoredNanos;
		monitoredMsFaultPerS = monitoredFaults * 1E9 / monitoredNanos;
		monitoredMaxLoad = target.getLoad() * 1000
				/ (1000 + monitoredMsFaultPerS);
		if (sysloadMonitor != null) {
			monitoredSysload = sysloadMonitor.retrieveData();
		}
	}

	void writeHeader() {
		writeln("stresser " + target);
		writeln("loopms " + target.getLoopMS());
		writeln("adaptation(ms) " + adaptationPeriod);
		writeln("monitoring(ms) " + monitoringPeriod);
		writeln("");
		OperatingSystemMXBean osmb = ManagementFactory
				.getOperatingSystemMXBean();
		writeln("arch " + osmb.getArch());
		writeln("processors " + osmb.getAvailableProcessors());
		// writeln("processorId " + System.getenv("PROCESSOR_IDENTIFIER"));
		// //does not work on linux
		writeln("OS " + osmb.getName());
		writeln("version " + osmb.getVersion());
		writeln("");
		RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
		writeln("vmName \"" + rmxb.getVmName() + "\"");
		writeln("vmVersion " + rmxb.getVmVersion());
		// writeln("bootClassPath " + rmxb.getBootClassPath().replaceAll(":",
		// " "));
		// writeln("libPath " + rmxb.getLibraryPath().replaceAll(":", " "));
	}

	protected double[] loads = null;

	public void setLoads(double[] loads) {
		this.loads = loads;
	}

	/**
	 * sets the loads to use, when we want to use a fixed number of loads and a
	 * maximum value of load
	 * 
	 * @param maxLoad
	 *            maximum load to reach
	 * @param nbLoads
	 *            number of steps.
	 */
	public void makeLoads(double maxLoad, int nbLoads) {
		loads = new double[nbLoads];
		for (int i = 0; i < nbLoads; i++) {
			loads[i] = maxLoad * i / (nbLoads - 1);
		}
	}

	private final Iterator<Double> loadsIterator = new Iterator<Double>() {

		int nextId = 0;
		double nextVal = 0;

		@Override
		public void remove() {
			throw new UnsupportedOperationException("impossible");
		}

		@Override
		public Double next() {
			if (loads != null) {
				nextId++;
				return loads[nextId - 1];
			}
			double ret = nextVal;
			nextVal += target.getMaxUsagesPerSecond() / 20;
			return ret;
		}

		@Override
		public boolean hasNext() {
			if (loads != null) {
				return nextId < loads.length;
			}
			return nextVal < target.getMaxUsagesPerSecond()
					&& monitoredMsFaultPerS < 300;
		}
	};

	Iterable<Double> loadsIterable = new Iterable<Double>() {

		@Override
		public Iterator<Double> iterator() {
			return loadsIterator;
		}
	};

	public void run() {
		target.start();
		sysloadMonitor = target.getMonitor();
		writeHeader();
		writeln("");
		writeln("benchstart");
		writeln("load uptimeCPU(%) msFaultPerS nano faults uptime(cs) deducedMaxLoad "
				+ (sysloadMonitor == null ? "" : sysloadMonitor));
		target.waitForRunning(1000);
		for (Double load : loadsIterable) {
			target.setLoad(load);
			try {
				Thread.sleep(adaptationPeriod);
				startMonitor();
				Thread.sleep(monitoringPeriod);
				endMonitor();
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
			writeln("" + load + " " + monitoredCPULoadPC + " "
					+ monitoredMsFaultPerS + " " + monitoredNanos + " "
					+ monitoredFaults + " " + monitoredUpTime + " "
					+ monitoredMaxLoad
					+ (sysloadMonitor == null ? "" : " " + monitoredSysload));
		}
		target.stop();
	}

	public void setFile(String fileName) {
		try {
			File f = new File(fileName);
			File parent = f.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			output = new FileOutputStream(f);
		} catch (Exception e) {
			logger.warn("", e);
		}
	}

	public void setFile() {
		String name = "benches/"
				+ new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss")
						.format(new Date()) + ".csv";
		logger.info("writting to file " + name);
		setFile(name);
	}

	public static long getPROCSTATUptime(int pid) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("/proc/"
					+ pid + "/stat"));
			String line = reader.readLine();
			reader.close();
			return Long.parseLong(line.split(" ")[13]);
		} catch (Exception e) {
			logger.warn("", e);
			return -1;
		}
	}

	public static int getPID() {
		try {
			return Integer.parseInt(new File("/proc/self").getCanonicalFile()
					.getName());
		} catch (Exception e) {
			logger.warn("", e);
			return -1;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		System.out.println("pid is : " + getPID());
	}
}
