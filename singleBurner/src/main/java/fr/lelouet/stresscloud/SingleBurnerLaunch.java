package fr.lelouet.stresscloud;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.*;

import fr.lelouet.stress.cpu.CPUMIPSStress;
import fr.lelouet.stress.net.UDPSenderStress;
import fr.lelouet.stresscloud.BurstStress;
import fr.lelouet.stresscloud.StresserManager;
import fr.lelouet.stresscloud.Stresser.TYPES;

/**
 * a main to select an stresser implementation and burn it. The available
 * options are in {@link #MAINOPTIONS}.
 * 
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
@SuppressWarnings("static-access")
public class SingleBurnerLaunch {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(SingleBurnerLaunch.class);

	public static final long DEFAULT_LOAD = 50;
	public static final char LOAD_SEPARATOR = ',';
	protected static Option LOADS = OptionBuilder
			.withArgName("loads")
			.hasArgs()
			.withValueSeparator(LOAD_SEPARATOR)
			.withDescription(
					"list of loads to burn the resource, separated by '"
							+ LOAD_SEPARATOR + "' (" + DEFAULT_LOAD + ")")
			.create("load");

	public static final int DEFAULTPERIOD = 10;
	protected static Option PERIOD = new Option("period",
			"seconds between different usage values(" + DEFAULTPERIOD + ")");

	public static final Long DEFAULTGRANULARITY = BurstStress.DEFAULT_LOOPMS;
	protected static final Option GRANULARITY = new Option("delay", true,
			"burning-sleep period seconds(" + DEFAULTGRANULARITY + ")");

	public static final int DEFAULT_NB = 1;
	protected static final Option THREADS = new Option("threads", true,
			"number of process to spawn(" + DEFAULT_NB + ")");

	/** alias to stresser implementations */
	protected static final HashMap<String, String> STRESSERS_ALIAS = new HashMap<String, String>();
	static {
		STRESSERS_ALIAS.put(TYPES.NET.name(),
				UDPSenderStress.class.getCanonicalName());
		STRESSERS_ALIAS.put(TYPES.CPU.name(),
				CPUMIPSStress.class.getCanonicalName());
	}
	public static final Class<? extends StresserManager> DEFAULTSTRESSER = CPUMIPSStress.class;
	protected static final Option STRESSERCLASS = new Option("stress", true,
			"implementation of the stresserManager to use("
					+ DEFAULTSTRESSER.getCanonicalName()
					+ "). can be an alias : " + STRESSERS_ALIAS);

	protected static final Option HELP = new Option("help", false,
			"print this help and exit");

	/** option to parse from the main args */
	public static final Option[] MAINOPTIONS = new Option[]{LOADS, PERIOD,
			GRANULARITY, THREADS, STRESSERCLASS, HELP};

	protected long granularity = DEFAULTGRANULARITY;
	protected List<Long> loads = Arrays.asList(new Long[]{DEFAULT_LOAD});
	protected int threads = DEFAULT_NB;
	protected long period = DEFAULTPERIOD;
	protected Class<? extends BurstStress> stressClass = CPUMIPSStress.class;

	/**
	 * set the internal parameters according to the arguments parsed in a
	 * {@link CommandLine}
	 * 
	 * @param cl
	 *            the parsed comandline.
	 */
	@SuppressWarnings("unchecked")
	public void acceptArgs(CommandLine cl) {
		if (cl.hasOption(LOADS.getOpt())) {
			loads = new ArrayList<Long>();
			for (String s : cl.getOptionValues(LOADS.getOpt())) {
				if (s != null && s.length() > 0) {
					loads.add(Long.parseLong(s));
				}
			}
		}
		if (cl.hasOption(PERIOD.getOpt())) {
			period = Long.parseLong(cl.getOptionValue(PERIOD.getOpt()));
		}
		if (cl.hasOption(GRANULARITY.getOpt())) {
			granularity = Long
					.parseLong(cl.getOptionValue(GRANULARITY.getOpt()));
		}
		if (cl.hasOption(THREADS.getOpt())) {
			threads = Integer.parseInt(cl.getOptionValue(THREADS.getOpt()));
		}
		if (cl.hasOption(STRESSERCLASS.getOpt())) {
			String className = cl.getOptionValue(STRESSERCLASS.getOpt());
			if (STRESSERS_ALIAS.containsKey(className)) {
				logger.info("stresser alias " + className + " resolved to "
						+ STRESSERS_ALIAS.get(className));
				className = STRESSERS_ALIAS.get(className);
			}
			try {
				stressClass = (Class<? extends BurstStress>) Class
						.forName(className);
			} catch (ClassNotFoundException e) {
				logger.warn("will fail, cannot find stresser class : "
						+ className, e);
			}
		}
	}

	/**
	 * effectiveley run the stresser(s) as specified.
	 */
	public void run() {
		StresserManager[] stressers = new StresserManager[threads];
		for (int i = 0; i < threads; i++) {
			StresserManager stress;
			try {
				stress = stressClass.newInstance();
			} catch (Exception e) {
				throw new UnsupportedOperationException(e);
			}
			if (stress instanceof BurstStress) {
				((BurstStress) stress).setLoopMS(granularity);
			}
			stressers[i] = stress;
			new Thread(stress).start();
		}
		while (true) {
			for (double pc : loads) {
				System.out.println("load=" + pc);
				for (StresserManager stress : stressers) {
					stress.setLoad(pc);
				}
				do {
					try {
						Thread.sleep(period * 1000);
					} catch (InterruptedException e) {
						logger.debug("", e);
					}
				} while (loads.size() < 2);
			}
		}
	}

	/**
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		CommandLineParser clp = new PosixParser();
		Options options = new Options();
		for (Option opt : MAINOPTIONS) {
			options.addOption(opt);
		}
		CommandLine cl = clp.parse(options, args);
		if (cl.hasOption(HELP.getOpt())) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(SingleBurnerLaunch.class.getSimpleName(),
					options);
			System.exit(0);
		}
		SingleBurnerLaunch launch = new SingleBurnerLaunch();
		launch.acceptArgs(cl);
		launch.run();
	}
}
