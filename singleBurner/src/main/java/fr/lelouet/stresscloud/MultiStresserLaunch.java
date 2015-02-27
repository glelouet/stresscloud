package fr.lelouet.stresscloud;

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

import org.apache.commons.cli.*;

/**
 * @author Guillaume Le Louët < guillaume.le-louet@mines-nantes.fr >
 */
public class MultiStresserLaunch {

	@SuppressWarnings("unused")
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(MultiStresserLaunch.class);

	protected static Option CPU = new Option("CPU", null, true,
			"CPU actions per second");

	protected static Option RAM = new Option("RAM", null, true,
			"RAM actions per second");

	protected static Option NET = new Option("NET", null, true,
			"NET actions per second");

	protected static Option PERCENTAGE = new Option("percent",
			"set the actions in percentage of the max capacity");

	protected static final Option HELP = new Option("help", false,
			"print this help and exit");

	/** option to parse from the main args */
	public static final Option[] MAINOPTIONS = new Option[]{CPU, RAM, NET,
			HELP, PERCENTAGE};

	public static void main(String[] args) throws ParseException {
		CommandLineParser clp = new PosixParser();
		Options options = new Options();
		for (Option opt : MAINOPTIONS) {
			options.addOption(opt);
		}
		CommandLine cl = clp.parse(options, args);
		// for (Option o : cl.getOptions()) {
		// logger.debug("option " + o + " parsed to " + o.getValue());
		// }
		if (cl.hasOption(HELP.getOpt())) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(MultiStresserLaunch.class.getSimpleName(),
					options);
			System.exit(0);
		}
		MultipleStresser stress = new MultipleStresser();
		stress.setLoadCPU(Long.parseLong(cl.getOptionValue(CPU.getOpt(), "0")));
		stress.setLoadRAM(Long.parseLong(cl.getOptionValue(RAM.getOpt(), "0")));
		stress.setLoadNET(Long.parseLong(cl.getOptionValue(NET.getOpt(), "0")));
		if (cl.hasOption(PERCENTAGE.getOpt())) {
			stress.setPercentLoad(true);
		}
		stress.setLoopMS(100);
		stress.launch();
	}
}
