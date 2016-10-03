package fr.lelouet.stress;

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

import fr.lelouet.stress.StressBenchResult.BenchEntry;
import fr.lelouet.stress.StressBencher.UpTimeMonitor;
import fr.lelouet.stresscloud.BurstStress;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class RunLocalStresser<T extends BurstStress> {

	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(RunLocalStresser.class);

	public static final String LOOPKEY = "loop";

	public static final String HELPKEY = "help";

	public static final String EXPORTKEY = "export=";

	public static final String CLOSEKEY = "close=";

	public static final String BENCHKEY = "bench";

	public static final String BENCHFORKEY = "benchfor=";

	public static final String CALIBRATEKEY = "calibrate";

	public static final String SLEEPKEY = "sleep=";

	public static final String GRAPHKEY = "graph=";

	protected T stress = null;

	public BurstStress getStress() {
		return stress;
	}

	public void setStress(T stress) {
		this.stress = stress;
	}

	public RunLocalStresser() {
	}

	RunLocalStresser(T stress) {
		this.stress = stress;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public void run(String[] args) throws IOException {
		stress.start();
		if (args != null) {
			for (String arg : args) {
				applyToken(arg);
			}
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		do {
			line = br.readLine();
			if (line != null) {
				applyToken(line);
			}
		} while (line != null);
		stress.stop();
		res.closeAll();
		System.exit(0);
	}

	public void applyToken(String line) {
		try {
			if (line.startsWith(LOOPKEY)) {
				setLoopMS(Integer
						.parseInt(line.substring(LOOPKEY.length() + 1)));
			} else if (line.startsWith(BENCHKEY)) {
				if (BENCHKEY.equals(line)) {
					bench(null);
				} else {
					bench(line.substring(BENCHKEY.length() + 1));
				}
			} else if (line.startsWith(CALIBRATEKEY)) {
				double error = 0.1;
				if (line.length() > CALIBRATEKEY.length()) {
					error = Double.parseDouble(line.substring(CALIBRATEKEY
							.length() + 1));
				}
				calibrate(error);
			} else if (line.startsWith(HELPKEY)) {
				System.out.println(getHelp());
			} else if (line.startsWith(EXPORTKEY)) {
				export(line.substring(EXPORTKEY.length()));
			} else if (line.startsWith(BENCHFORKEY)) {
				benchMS = 1000 * Long.parseLong(line.substring(BENCHFORKEY
						.length()));
				System.out.println("bench ms set to " + sleepMS);
			} else if (line.startsWith(CLOSEKEY)) {
				close(line.substring(CLOSEKEY.length()));
			} else if (line.startsWith(GRAPHKEY)) {
				graph(line.substring(GRAPHKEY.length()));
			} else if (line.startsWith(SLEEPKEY)) {
				sleepMS = (long) (1000 * Double.parseDouble(line
						.substring(SLEEPKEY.length())));
				System.out.println("sleep ms set to " + sleepMS);
			} else {
				setLoad(Double.parseDouble(line));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/** @param substring */
	public void graph(String fileName) {
		System.out.println("exporting raph " + fileName);
		// res.graph(fileName);
		System.err.println("not implemented");
	}

	public void setLoopMS(int loopMS) {
		System.out.println("setting internal loop to " + loopMS + "ms");
		stress.setLoopMS(loopMS);

	}

	protected long sleepMS = -1;

	protected long benchMS = 1000;

	public void setLoad(double load) {
		stress.setLoad(load);
		if (sleepMS > -1) {
			try {
				Thread.sleep(sleepMS);
			} catch (InterruptedException e) {
				logger.warn("", e);
			}
			bench(null);
		}
	}

	/**
	 * print the help. Override to add additionnal args description. don't
	 * forget to add newline after the String.
	 */
	public String getHelp() {
		return "This program concatenates the args to the stdio and set the load to the entry doubles.\n"
				+ " optionnal args :\n "
				+ LOOPKEY
				+ "=internaldelay\n "
				+ EXPORTKEY
				+ "file add a file to write bench into\n "
				+ CLOSEKEY
				+ "file close a file we export data to\n "
				+ BENCHKEY
				+ "[=comments] add a bench data to the exports. Optionnal comments\n "
				+ CALIBRATEKEY
				+ "[=errorRate to achieve] find a load that achieve required error rate and print it\n "
				+ SLEEPKEY
				+ "number of s to wait between a load change and a bench. if <0, no bench\n "
				+ HELPKEY + " to print this help";
	}

	protected StressBenchResult res = new StressBenchResult();

	/**
	 * add a file to write bench data into
	 * 
	 * @param fileName
	 */
	public void export(String fileName) {
		res.setProperty("loopms", "" + stress.getLoopMS());
		res.setProperty("stresser", "" + stress.getClass().getSimpleName());
		res.setProperty("benchtime", "" + sleepMS);
		if (res.export(fileName)) {
			System.out.println("exporting to " + fileName);
		}
	}

	public void close(String fileName) {
		if (res.close(fileName)) {
			System.out.println("closed " + fileName);
		}
	}

	protected UpTimeMonitor monitor = new UpTimeMonitor();

	public void bench(String addedData) {
		// System.out.println("benching with aimed external data " + addedData);
		long sleepMS = this.sleepMS == -1 ? 1000 : this.sleepMS;
		monitor.prepareUptimeMXBean();
		long skipped = stress.getSkipped();
		try {
			Thread.sleep(sleepMS);
		} catch (InterruptedException e) {
			logger.warn("", e);
		}
		long upDiff = monitor.getUptimeMXBean();
		// System.err.println("uptime diff : " + upDiff);
		long skippedDiff = stress.getSkipped() - skipped;
		BenchEntry be = new BenchEntry();
		be.date = System.currentTimeMillis();
		be.load = stress.getLoad();
		be.userData = addedData;
		be.cpupct = 0.0001 * upDiff / sleepMS;
		be.errorRate = 100.0 * skippedDiff / sleepMS;
		System.out.println(StressBenchResult.dataDesc + "\n" + be.toPlain());
		res.add(be);
	}

	protected double calibratedLoad = -1;

	public void calibrate(Double errorRate) {
		System.out.println("calibrating to error rate " + errorRate);
		double val = Calibration.calibrate(stress, errorRate);
		calibratedLoad = val * (1 - errorRate);
		System.out.println(calibratedLoad);
	}
}
